package com.btmi.item;

import com.btmi.BTMI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.btmi.entity.FlameButterfly;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class FlameCapeHandler {
    private static final int BUTTERFLY_COUNT = 8;
    /** 每只蝴蝶生成之间的间隔（tick） */
    private static final int SPAWN_INTERVAL = 4;
    private static final float FIRE_BONUS = 4.0F;
    private static final int FIRE_TICKS = 60;
    /** 蝴蝶出击的最远距离 */
    private static final double STRIKE_RANGE = 24.0D;

    /** 正在逐只生成的蝴蝶：玩家 -> [剩余数量, 冷却 tick] */
    private static final Map<UUID, int[]> PENDING = new HashMap<>();

    private FlameCapeHandler() {
    }

    public static boolean isWearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(BTMI.FLAME_CAPE.get());
    }

    public static void summon(ServerPlayer player) {
        if (!isWearing(player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();

        // 重新召唤时先清理旧的一批
        level.getEntitiesOfClass(FlameButterfly.class, player.getBoundingBox().inflate(8.0D),
                butterfly -> butterfly.isOwnedBy(player)).forEach(Entity::discard);

        // 不一次性全部生成，交给 onPlayerTick 每隔几 tick 放出一只
        PENDING.put(player.getUUID(), new int[] { BUTTERFLY_COUNT, 0 });

        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.2D, player.getZ(),
                40, 1.2D, 0.6D, 1.2D, 0.05D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    /** 让场上的蝴蝶扑向准星指向的目标 */
    public static void strike(ServerPlayer player) {
        if (!isWearing(player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(STRIKE_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double distance = blockHit.getType() == HitResult.Type.MISS
                ? STRIKE_RANGE
                : eye.distanceTo(blockHit.getLocation());

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player,
                eye, eye.add(look.scale(distance)),
                player.getBoundingBox().inflate(distance + 1.0D),
                entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player);

        if (entityHit == null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7F, 1.4F);
            return;
        }

        boolean any = false;
        for (FlameButterfly butterfly : level.getEntitiesOfClass(FlameButterfly.class,
                player.getBoundingBox().inflate(8.0D), butterfly -> butterfly.isOwnedBy(player))) {
            butterfly.setTargetEntity(entityHit.getEntity());
            any = true;
        }

        if (!any) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int[] pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return;
        }
        if (!player.isAlive() || !isWearing(player)) {
            PENDING.remove(player.getUUID());
            return;
        }
        if (pending[1] > 0) {
            pending[1]--;
            return;
        }

        int index = BUTTERFLY_COUNT - pending[0];
        ServerLevel level = (ServerLevel) player.level();
        level.addFreshEntity(new FlameButterfly(level, player,
                index * Math.PI * 2.0D / BUTTERFLY_COUNT, index));

        pending[0]--;
        pending[1] = SPAWN_INTERVAL;
        if (pending[0] <= 0) {
            PENDING.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Projectile projectile) {
            attacker = projectile.getOwner();
        }
        if (!(attacker instanceof ServerPlayer player) || !isWearing(player) || victim == player) {
            return;
        }

        // 附加火焰伤害并点燃
        event.setAmount(event.getAmount() + FIRE_BONUS);
        victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), FIRE_TICKS));

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.FLAME,
                victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                8, 0.3D, 0.3D, 0.3D, 0.02D);


    }
}
