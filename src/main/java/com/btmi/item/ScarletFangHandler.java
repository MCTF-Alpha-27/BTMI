package com.btmi.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.entity.LaserBeam;
import com.btmi.entity.LaserBolt;
import com.btmi.network.ShakePayload;

import net.minecraft.core.particles.DustParticleOptions;
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
import net.neoforged.neoforge.network.PacketDistributor;

import org.joml.Vector3f;

@EventBusSubscriber(modid = BTMI.MODID)
public final class ScarletFangHandler {
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

    private ScarletFangHandler() {
    }

    public static String modeName(int mode) {
        return switch (mode) {
            case ScarletFangItem.MODE_LASER -> "item.btmi.scarlet_fang.mode.laser";
            case ScarletFangItem.MODE_SHOTGUN -> "item.btmi.scarlet_fang.mode.shotgun";
            default -> "item.btmi.scarlet_fang.mode.burst";
        };
    }

    public static void cycleMode(Player player, ItemStack stack) {
        int next = (ScarletFangItem.mode(stack) + 1) % ScarletFangItem.MODE_COUNT;
        ScarletFangItem.setMode(stack, next);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable("item.btmi.scarlet_fang.mode", Component.translatable(modeName(next))), true);
            serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7F, 1.6F);
        }
    }

    public static void fire(ServerPlayer player) {
        ItemStack gun = player.getMainHandItem();
        if (!gun.is(BTMI.SCARLET_FANG.get())) {
            return;
        }

        int mode = ScarletFangItem.mode(gun);
        int cooldown = switch (mode) {
            case ScarletFangItem.MODE_LASER -> LASER_COOLDOWN;
            case ScarletFangItem.MODE_SHOTGUN -> SHOTGUN_COOLDOWN;
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
            case ScarletFangItem.MODE_LASER -> fireLaser(level, player, muzzle.add(0.0D, -LASER_EXTRA_DROP, 0.0D), look);
            case ScarletFangItem.MODE_SHOTGUN -> fireShotgun(level, player, muzzle, look);
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
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();

        // 锥形命中判定
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
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), SHOTGUN_DAMAGE);

            // 命中反馈：红色电火花与爆闪
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                    22, 0.35D, 0.45D, 0.35D, 0.28D);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.62F, 0.03F, 0.08F), 1.5F),
                    center.x, center.y, center.z, 18, 0.3D, 0.4D, 0.3D, 0.06D);
        }

        // 枪口爆闪、硝烟与猩红粉尘
        level.sendParticles(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 2, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
                28, 0.22D, 0.22D, 0.22D, 0.09D);
        level.sendParticles(ParticleTypes.FIREWORK, muzzle.x, muzzle.y, muzzle.z,
                34, 0.32D, 0.32D, 0.32D, 0.2D);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.6F, 0.02F, 0.07F), 1.9F),
                muzzle.x, muzzle.y, muzzle.z, 30, 0.28D, 0.28D, 0.28D, 0.14D);

        // 枪口前方的冲击圆盘
        for (int ring = 0; ring < 2; ring++) {
            double ringRadius = 0.3D + ring * 0.35D;
            Vec3 ringCenter = muzzle.add(look.scale(0.35D + ring * 0.3D));
            for (int i = 0; i < 20; i++) {
                double angle = i * (Math.PI * 2.0D / 20.0D);
                Vec3 point = ringCenter
                        .add(right.scale(Math.cos(angle) * ringRadius))
                        .add(up.scale(Math.sin(angle) * ringRadius));
                level.sendParticles(ring == 0 ? ParticleTypes.FLASH : ParticleTypes.SWEEP_ATTACK,
                        point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        // 五束分叉闪电，铺满整个扇面
        for (int ray = -2; ray <= 2; ray++) {
            double spread = ray * 0.28D;
            double phase = ray * 1.7D;
            for (int step = 0; step <= 10; step++) {
                double distance = 0.6D + step * 0.62D;
                double wobble = Math.sin(distance * 1.5D + phase) * 0.08D
                        + (level.random.nextDouble() - 0.5D) * 0.09D;
                Vec3 point = muzzle
                        .add(look.scale(distance))
                        .add(right.scale(spread * distance + wobble))
                        .add(up.scale(wobble * 0.6D + (level.random.nextDouble() - 0.5D) * 0.07D));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
                        1, 0.01D, 0.01D, 0.01D, 0.01D);
                if (step % 3 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                if (level.random.nextFloat() < 0.14F) {
                    Vec3 fork = point
                            .add(right.scale((level.random.nextDouble() - 0.5D) * 1.2D))
                            .add(up.scale((level.random.nextDouble() - 0.5D) * 0.8D))
                            .add(look.scale(0.45D));
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, fork.x, fork.y, fork.z,
                            2, 0.02D, 0.02D, 0.02D, 0.03D);
                }
            }
        }

        // 扇面内漂浮的猩红雾与硝烟
        for (int layer = 0; layer < 4; layer++) {
            double distance = 1.2D + layer * 1.35D;
            double spread = 0.32D * distance;
            Vec3 center = muzzle.add(look.scale(distance));
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, center.x, center.y, center.z,
                    16, spread, spread * 0.6D, spread, 0.03D);
            level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                    6, spread, spread * 0.5D, spread, 0.03D);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.9F, 1.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.55F, 1.75F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.3F);

        if (Config.SCREEN_SHAKE.get()) {
            float strength = (float) (Config.SHAKE_STRENGTH.get() * 0.55D);
            PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 24.0D,
                    new ShakePayload(player.getX(), player.getY(), player.getZ(), strength, 8));
        }
    }

    // ------------------------------------------------------------------
    // 能量环
    // ------------------------------------------------------------------

    public static void triggerRing(ServerPlayer player) {
        if (!player.getMainHandItem().is(BTMI.SCARLET_FANG.get())) {
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
