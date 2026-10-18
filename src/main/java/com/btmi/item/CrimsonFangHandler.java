package com.btmi.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.entity.LaserBeam;
import com.btmi.entity.LaserBolt;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 猩红之牙的服务端逻辑：三种开火模式 + 能量环技能。
 */
@EventBusSubscriber(modid = BTMI.MODID)
public final class CrimsonFangHandler {
    private static final int BURST_COOLDOWN = 4;
    private static final int LASER_COOLDOWN = 14;
    private static final int SHOTGUN_COOLDOWN = 18;

    private static final double BURST_SPEED = 3.0D;
    private static final double LASER_SPEED = 6.0D;
    private static final float LASER_RANGE = 40.0F;
    private static final float LASER_DAMAGE = 18.0F;
    private static final double SHOTGUN_RANGE = 7.0D;
    private static final float SHOTGUN_DAMAGE = 16.0F;
    private static final double SHOTGUN_COS = 0.86D;

    private static final int RING_SPIN_TICKS = 14;
    private static final double RING_RADIUS = 2.2D;
    private static final double RING_DAMAGE_RADIUS = 4.5D;
    private static final float RING_DAMAGE = 24.0F;

    private static final double MUZZLE_FORWARD = 0.95D;
    private static final double MUZZLE_RIGHT = 0.18D;
    private static final double MUZZLE_DROP = 0.12D;
    /** 激光模式额外再压低一点，蓝白光柱从枪口下沿射出更自然 */
    private static final double LASER_EXTRA_DROP = 0.22D;

    /** 正在转圈放能量环的玩家 -> 已经过的 tick */
    private static final Map<UUID, Integer> RINGS = new HashMap<>();

    private CrimsonFangHandler() {
    }

    public static String modeName(int mode) {
        return switch (mode) {
            case CrimsonFangItem.MODE_LASER -> "item.btmi.crimson_fang.mode.laser";
            case CrimsonFangItem.MODE_SHOTGUN -> "item.btmi.crimson_fang.mode.shotgun";
            default -> "item.btmi.crimson_fang.mode.burst";
        };
    }

    public static void cycleMode(Player player, ItemStack stack) {
        int next = (CrimsonFangItem.mode(stack) + 1) % CrimsonFangItem.MODE_COUNT;
        CrimsonFangItem.setMode(stack, next);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable("item.btmi.crimson_fang.mode", Component.translatable(modeName(next))), true);
            serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7F, 1.6F);
        }
    }

    public static void fire(ServerPlayer player) {
        ItemStack gun = player.getMainHandItem();
        if (!gun.is(BTMI.CRIMSON_FANG.get())) {
            return;
        }

        int mode = CrimsonFangItem.mode(gun);
        int cooldown = switch (mode) {
            case CrimsonFangItem.MODE_LASER -> LASER_COOLDOWN;
            case CrimsonFangItem.MODE_SHOTGUN -> SHOTGUN_COOLDOWN;
            default -> BURST_COOLDOWN;
        };
        if (player.getCooldowns().isOnCooldown(gun.getItem())) {
            return;
        }
        player.getCooldowns().addCooldown(gun.getItem(), cooldown);

        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 muzzle = muzzle(player, look);

        switch (mode) {
            case CrimsonFangItem.MODE_LASER -> fireLaser(level, player, muzzle.add(0.0D, -LASER_EXTRA_DROP, 0.0D), look);
            case CrimsonFangItem.MODE_SHOTGUN -> fireShotgun(level, player, muzzle, look);
            default -> fireBurst(level, player, muzzle, look);
        }
        gun.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    private static Vec3 muzzle(Player player, Vec3 look) {
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        return player.getEyePosition()
                .add(look.scale(MUZZLE_FORWARD))
                .add(right.scale(MUZZLE_RIGHT))
                .add(0.0D, -MUZZLE_DROP, 0.0D);
    }

    private static void fireBurst(ServerLevel level, ServerPlayer player, Vec3 muzzle, Vec3 look) {
        LaserBolt bolt = new LaserBolt(level, player);
        bolt.setPos(muzzle.x, muzzle.y, muzzle.z);
        bolt.shoot(look.x, look.y, look.z, (float) BURST_SPEED, 0.0F);
        level.addFreshEntity(bolt);

        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 3, 0.04D, 0.04D, 0.04D, 0.01D);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.05D, 0.05D, 0.05D, 0.01D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    private static void fireLaser(ServerLevel level, ServerPlayer player, Vec3 muzzle, Vec3 look) {
        // 瞬间命中的光束：先算出射线被打断的位置
        Vec3 end = muzzle.add(look.scale(LASER_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(muzzle, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double distance = blockHit.getType() == HitResult.Type.MISS
                ? LASER_RANGE
                : muzzle.distanceTo(blockHit.getLocation());

        // 沿射线找最近的目标
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player,
                muzzle, muzzle.add(look.scale(distance)),
                player.getBoundingBox().inflate(distance + 1.0D),
                entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player);
        if (entityHit != null) {
            distance = muzzle.distanceTo(entityHit.getLocation());
            if (entityHit.getEntity() instanceof LivingEntity target) {
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().playerAttack(player), LASER_DAMAGE);
                player.setLastHurtMob(target);
            }
        }

        // 光束实体（带体积的发光柱，/ 7 tick 后消失）
        level.addFreshEntity(new LaserBeam(level, muzzle, look, (float) distance));



        // 命中点爆点
        Vec3 impact = muzzle.add(look.scale(distance));
        level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z, 20,
                0.25D, 0.25D, 0.25D, 0.15D);
        level.sendParticles(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.7F, 1.8F);
    }

    private static void fireShotgun(ServerLevel level, ServerPlayer player, Vec3 muzzle, Vec3 look) {
        AABB area = player.getBoundingBox().inflate(SHOTGUN_RANGE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive())) {
            Vec3 toTarget = target.getEyePosition().subtract(muzzle);
            if (toTarget.length() > SHOTGUN_RANGE) {
                continue;
            }
            if (toTarget.normalize().dot(look) < SHOTGUN_COS) {
                continue;
            }
            target.hurt(level.damageSources().playerAttack(player), SHOTGUN_DAMAGE);
        }

        // 枪口扇面闪电
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();

        for (int ray = -4; ray <= 4; ray++) {
            double spread = ray * 0.10D;
            for (double d = 0.6D; d < 6.0D; d += 0.45D) {
                double wobble = Math.sin(d * 2.0D + ray) * 0.12D;
                Vec3 point = muzzle
                        .add(look.scale(d))
                        .add(right.scale(spread * d + wobble))
                        .add(up.scale(wobble * 0.6D));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.02D);
            }
        }

        level.sendParticles(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.7F, 1.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.8F);
    }

    // ------------------------------------------------------------------
    // 能量环
    // ------------------------------------------------------------------

    public static void triggerRing(ServerPlayer player) {
        if (!player.getMainHandItem().is(BTMI.CRIMSON_FANG.get())) {
            return;
        }
        UUID id = player.getUUID();
        if (RINGS.containsKey(id)) {
            return;
        }
        RINGS.put(id, 0);

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Integer age = RINGS.get(player.getUUID());
        if (age == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);

        // 枪口画出的能量环
        double baseAngle = age * 0.7D;
        for (int i = 0; i < 16; i++) {
            double angle = baseAngle + i * (Math.PI * 2.0D / 16.0D);
            double x = center.x + Math.cos(angle) * RING_RADIUS;
            double z = center.z + Math.sin(angle) * RING_RADIUS;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, center.y, z, 1, 0.0D, 0.02D, 0.0D, 0.01D);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, x, center.y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        if (age < RING_SPIN_TICKS) {
            RINGS.put(player.getUUID(), age + 1);
            return;
        }

        // 脉冲：伤害 + 向外扩散的光环
        AABB area = player.getBoundingBox().inflate(RING_DAMAGE_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive())) {
            target.hurt(level.damageSources().playerAttack(player), RING_DAMAGE);
        }
        for (int step = 0; step < 4; step++) {
            double radius = RING_RADIUS + step * 0.7D;
            for (int i = 0; i < 24; i++) {
                double angle = i * (Math.PI * 2.0D / 24.0D);
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, center.y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.8F);
        RINGS.remove(player.getUUID());
    }

    public static boolean isRinging(Entity entity) {
        return RINGS.containsKey(entity.getUUID());
    }
}
