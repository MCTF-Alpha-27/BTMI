package com.btmi.item;

import java.util.List;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.network.ShakePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    public static void perform(ServerLevel level, Player player, ItemStack stack, int chargeTicks) {        int minTicks = Config.MIN_CHARGE_TICKS.get();
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

    // ------------------------------------------------------------------
    // 全垒打
    // ------------------------------------------------------------------

    public static void homeRun(ServerLevel level, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(BTMI.EXCLAMATION_MARK.get()) || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        player.getCooldowns().addCooldown(stack.getItem(), Config.EXCLAMATION_HOMERUN_COOLDOWN.get());

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
        if (flatLook.lengthSqr() < 1.0E-6D) {
            flatLook = new Vec3(0.0D, 0.0D, 1.0D);
        }
        flatLook = flatLook.normalize();

        double radius = Config.EXCLAMATION_HOMERUN_RADIUS.get();
        double cosHalf = Math.cos(Math.toRadians(80.0D));
        float damage = Config.EXCLAMATION_HOMERUN_DAMAGE.get().floatValue();
        double knockback = Config.EXCLAMATION_HOMERUN_KNOCKBACK.get();

        AABB area = player.getBoundingBox().inflate(radius, 3.0D, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive())) {
            Vec3 toTarget = target.getEyePosition().subtract(eye);
            if (toTarget.length() > radius) {
                continue;
            }
            Vec3 flat = new Vec3(toTarget.x, 0.0D, toTarget.z);
            if (flat.lengthSqr() < 1.0E-6D) {
                flat = flatLook;
            }
            flat = flat.normalize();
            if (flat.dot(flatLook) < cosHalf) {
                continue;
            }

            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), damage);
            target.push(flat.x * knockback, 1.1D, flat.z * knockback);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                    16, 0.35D, 0.35D, 0.35D, 0.28D);
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8D, target.getZ(),
                    8, 0.3D, 0.3D, 0.3D, 0.02D);
        }

        spawnHomeRunEffects(level, player, eye, flatLook, radius);
    }

    private static void spawnHomeRunEffects(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 flatLook, double radius) {
        Vec3 right = new Vec3(-flatLook.z, 0.0D, flatLook.x);
        Vec3 center = eye.add(0.0D, -0.35D, 0.0D);

        // 三层挥击弧线，外圈更大更亮
        for (int layer = 0; layer < 3; layer++) {
            double arcRadius = radius * (0.55D + layer * 0.2D);
            double yOffset = layer * 0.28D;
            int points = 36;
            for (int i = 0; i <= points; i++) {
                double angle = Math.toRadians(-82.0D + 164.0D * (i / (double) points));
                Vec3 direction = flatLook.scale(Math.cos(angle)).add(right.scale(Math.sin(angle)));
                Vec3 point = center.add(direction.scale(arcRadius)).add(0.0D, yOffset, 0.0D);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z,
                            1, 0.0D, 0.0D, 0.0D, 0.03D);
                }
            }
        }

        // 地面扩散的冲击环
        for (int ring = 0; ring < 2; ring++) {
            double ringRadius = 1.8D + ring * 1.4D;
            for (int i = 0; i < 40; i++) {
                double angle = i * (Math.PI * 2.0D / 40.0D);
                level.sendParticles(ring == 0 ? ParticleTypes.CLOUD : ParticleTypes.POOF,
                        player.getX() + Math.cos(angle) * ringRadius,
                        player.getY() + 0.12D,
                        player.getZ() + Math.sin(angle) * ringRadius,
                        1, 0.0D, 0.02D, 0.0D, 0.01D);
            }
        }

        level.sendParticles(ParticleTypes.FLASH, center.x + flatLook.x * 1.2D, center.y, center.z + flatLook.z * 1.2D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x + flatLook.x * 2.0D, center.y,
                center.z + flatLook.z * 2.0D, 6, 0.6D, 0.2D, 0.6D, 0.0D);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.6F, 0.65F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.45F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.7F);

        if (Config.SCREEN_SHAKE.get()) {
            float strength = (float) (Config.SHAKE_STRENGTH.get() * 0.85D);
            PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 40.0D,
                    new ShakePayload(player.getX(), player.getY(), player.getZ(), strength, 12));
        }
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
