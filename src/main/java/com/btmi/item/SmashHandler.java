package com.btmi.item;

import java.util.List;

import com.btmi.Config;
import com.btmi.network.ShakePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SmashHandler {
    private static final double BASE_REACH = 3.0D;
    private static final double CHARGE_REACH = 3.0D;
    private static final double BASE_HIT_RADIUS = 2.0D;
    private static final double CHARGE_HIT_RADIUS = 2.5D;
    private static final double SHAKE_RANGE = 48.0D;

    private SmashHandler() {
    }

    public static void perform(ServerLevel level, Player player, ItemStack stack, int chargeTicks) {
        int minTicks = Config.MIN_CHARGE_TICKS.get();
        int maxTicks = Math.max(minTicks + 1, Config.MAX_CHARGE_TICKS.get());
        // 最小蓄力时为 0.0，满蓄力（或更久）时为 1.0
        float intensity = Mth.clamp((chargeTicks - minTicks) / (float) (maxTicks - minTicks), 0.0F, 1.0F);

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = BASE_REACH + CHARGE_REACH * intensity;

        BlockHitResult blockHit = level.clip(new ClipContext(
                eye, eye.add(look.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
        Vec3 impact = hitBlock ? blockHit.getLocation() : eye.add(look.scale(reach));

        damageEntities(level, player, impact, look, intensity);

        if (hitBlock && Config.BREAK_BLOCKS.get()) {
            double radius = Mth.lerp(intensity, Config.MIN_CHARGE_RADIUS.get(), Config.MAX_CHARGE_RADIUS.get());
            destroyBlocks(level, player, impact, radius);
        }

        spawnShockwave(level, impact, look, intensity);
        playSounds(level, impact, intensity);
        sendScreenShake(level, impact, intensity);

        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    private static void damageEntities(ServerLevel level, Player player, Vec3 impact, Vec3 look, float intensity) {
        double radius = BASE_HIT_RADIUS + CHARGE_HIT_RADIUS * intensity;
        double multiplier = Mth.lerp(intensity,
                Config.MIN_CHARGE_DAMAGE_MULTIPLIER.get(), Config.MAX_CHARGE_DAMAGE_MULTIPLIER.get());
        float damage = (float) (ExclamationMarkItem.ATTACK_DAMAGE + 1.0D) * (float) multiplier;

        AABB area = new AABB(impact, impact).inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            Vec3 offset = target.position().subtract(impact);
            double distance = offset.length();
            if (distance > radius) {
                continue;
            }
            // 把目标从冲击点向外推开（若正好位于冲击点上则改用视线方向）
            Vec3 direction = distance < 1.0E-4D ? look : offset.normalize();
            double strength = 0.6D + 0.6D * intensity;

            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), damage);
            target.push(direction.x * strength, 0.35D + 0.45D * intensity, direction.z * strength);
            target.hurtMarked = true;
        }
    }

    private static void destroyBlocks(ServerLevel level, Player player, Vec3 impact, double radius) {
        int range = Mth.ceil(radius);
        double radiusSq = radius * radius;
        BlockPos center = BlockPos.containing(impact);
        boolean dropItems = Config.DROP_BLOCKS.get();
        int maxBlocks = Config.MAX_BLOCKS_BROKEN.get();
        int broken = 0;

        for (int dx = -range; dx <= range && broken < maxBlocks; dx++) {
            for (int dy = -range; dy <= range && broken < maxBlocks; dy++) {
                for (int dz = -range; dz <= range && broken < maxBlocks; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (state.getDestroySpeed(level, pos) < 0.0F) {
                        continue;
                    }
                    if (level.destroyBlock(pos, dropItems, player)) {
                        broken++;
                    }
                }
            }
        }
    }

    private static void spawnShockwave(ServerLevel level, Vec3 impact, Vec3 look, float intensity) {
        level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        // 粒子环
        Vec3 up = Math.abs(look.y) > 0.99D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(up).normalize();
        Vec3 vertical = right.cross(look).normalize();

        int points = 24 + Mth.floor(intensity * 24.0F);
        double ringRadius = 0.8D + 2.2D * intensity;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            Vec3 direction = right.scale(Math.cos(angle)).add(vertical.scale(Math.sin(angle)));
            Vec3 point = impact.add(direction.scale(ringRadius));
            level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.02D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.05D);
            }
        }

        // 冲击点附近的尘土与碎屑
        int dust = 15 + Mth.floor(intensity * 30.0F);
        level.sendParticles(ParticleTypes.POOF, impact.x, impact.y, impact.z, dust, 0.6D, 0.4D, 0.6D, 0.05D);
        level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, dust, 0.8D, 0.6D, 0.8D, 0.15D);
    }

    private static void playSounds(ServerLevel level, Vec3 impact, float intensity) {
        float pitch = 0.8F + 0.3F * intensity;
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.4F, pitch);
        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 0.7F + 0.2F * intensity);
    }

    private static void sendScreenShake(ServerLevel level, Vec3 impact, float intensity) {
        if (!Config.SCREEN_SHAKE.get()) {
            return;
        }
        float strength = (float) (Config.SHAKE_STRENGTH.get() * (0.4D + 0.6D * intensity));
        int duration = 10 + Mth.floor(intensity * 12.0F);
        PacketDistributor.sendToPlayersNear(level, null, impact.x, impact.y, impact.z, SHAKE_RANGE,
                new ShakePayload(impact.x, impact.y, impact.z, strength, duration));
    }
}
