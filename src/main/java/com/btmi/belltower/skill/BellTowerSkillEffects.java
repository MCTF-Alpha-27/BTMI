package com.btmi.belltower.skill;

import java.util.ArrayList;
import java.util.List;

import com.btmi.Config;
import com.btmi.belltower.BellTowerQuests;
import com.btmi.entity.BellToll;
import com.btmi.entity.GroundCrack;
import com.btmi.entity.ProjectionShockwave;
import com.btmi.entity.RedLine;
import com.btmi.network.BellTowerOverlayPayload;
import com.btmi.network.ShakePayload;
import com.btmi.weapon.ModAnimations;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class BellTowerSkillEffects {
    private static final double TOLL_RADIUS = 7.0D;
    private static final float TOLL_DAMAGE = 8.0F;
    private static final int TOLL_IMPACT_DELAY = 20;
    private static final double RUSH_DISTANCE = 7.0D;
    private static final float RUSH_DAMAGE = 7.0F;
    private static final int RUSH_IMPACT_DELAY = 6;
    private static final double TAU = Math.PI * 2.0D;

    private BellTowerSkillEffects() {
    }

    public static void cast(ServerPlayer player, BellTowerSkill skill) {
        if (skill == null) {
            return;
        }
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            switch (skill.id()) {
                case BellTowerSkills.BELL_TOLL, BellTowerSkills.AGENT_RUIN ->
                        patch.playAnimationSynchronized(ModAnimations.BELL_TOLL, 0.0F);
                case BellTowerSkills.BLOOD_RUSH ->
                        patch.playAnimationSynchronized(ModAnimations.BLOOD_RUSH, 0.0F);
                case BellTowerSkills.HARBINGER_REVELATION -> {
                    patch.playAnimationSynchronized(Animations.EVOKER_CAST_SPELL, 0.0F);
                    BellTowerSkillScheduler.later(player, 12, p -> {
                        ServerPlayerPatch later = EpicFightCapabilities.getServerPlayerPatch(p);
                        if (later != null) {
                            later.playAnimationSynchronized(Animations.BIPED_IDLE, 0.15F);
                        }
                    });
                }
                default -> {
                }
            }
        }
        switch (skill.id()) {
            case BellTowerSkills.BELL_TOLL -> beginToll(player);
            case BellTowerSkills.BLOOD_RUSH -> beginRush(player);
            case BellTowerSkills.HARBINGER_REVELATION -> staffRevelation(player);
            case BellTowerSkills.AGENT_RUIN -> beginRuin(player);
            default -> {
            }
        }
    }

    private static void beginToll(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.5F, 0.72F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.9F, 1.35F);
        level.addFreshEntity(new BellToll(level, player));

        for (int step = 0; step < 10; step++) {
            int index = step;
            BellTowerSkillScheduler.later(player, index * 2, p -> {
                ServerLevel l = p.serverLevel();
                Vec3 base = p.position();
                double progress = index / 10.0D;
                double radius = 5.6D * (1.0D - progress) + 0.7D;
                double y = base.y + 0.18D + progress * 2.6D;
                for (int i = 0; i < 10; i++) {
                    double angle = i * TAU / 10.0D + index * 0.45D;
                    double x = base.x + Math.cos(angle) * radius;
                    double z = base.z + Math.sin(angle) * radius;
                    l.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0D, 0.02D, 0.0D, 0.01D);
                    if (i % 2 == 0) {
                        l.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.02D);
                    }
                    if (index % 3 == 0) {
                        l.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y - 0.1D, z, 1, 0.02D, 0.04D, 0.02D, 0.0D);
                    }
                }
            });
        }
        BellTowerSkillScheduler.later(player, TOLL_IMPACT_DELAY, BellTowerSkillEffects::tollImpact);
    }

    private static void tollImpact(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 3.0F, 0.55F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.2F, 0.62F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 0.66F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.5F, 0.6F);

        level.addFreshEntity(new ProjectionShockwave(level, center.add(0.0D, 0.05D, 0.0D), 6.5F, true));
        BellTowerSkillScheduler.later(player, 3, p -> p.serverLevel().addFreshEntity(
                new ProjectionShockwave(p.serverLevel(), center.add(0.0D, 0.05D, 0.0D), 4.2F, true)));
        BellTowerSkillScheduler.later(player, 6, p -> p.serverLevel().addFreshEntity(
                new ProjectionShockwave(p.serverLevel(), center.add(0.0D, 0.05D, 0.0D), 8.5F, true)));

        for (int i = 0; i < 8; i++) {
            double angle = i * TAU / 8.0D + 0.18D;
            Vec3 towards = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            level.addFreshEntity(new GroundCrack(level, center.add(0.0D, 0.02D, 0.0D), towards, 5.6F));
        }

        for (int i = 0; i < 8; i++) {
            double angle = i * TAU / 8.0D;
            double x = center.x + Math.cos(angle) * 3.4D;
            double z = center.z + Math.sin(angle) * 3.4D;
            for (double y = 0.0D; y < 7.0D; y += 0.45D) {
                level.sendParticles(ParticleTypes.END_ROD, x, center.y + y, z, 1, 0.0D, 0.05D, 0.0D, 0.01D);
                if (y > 2.0D && i % 2 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, center.y + y, z,
                            1, 0.05D, 0.08D, 0.05D, 0.03D);
                }
            }
        }

        level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.1D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 1.0D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        for (int i = 0; i < 150; i++) {
            double angle = level.random.nextDouble() * TAU;
            double radius = 0.8D + level.random.nextDouble() * 6.4D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + 0.25D + level.random.nextDouble() * 2.0D;
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, 0.0D, 0.35D, 0.0D, 0.08D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z, 1, 0.05D, 0.4D, 0.05D, 0.02D);
            }
            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0D, 0.5D, 0.0D, 0.05D);
            }
        }

        for (int i = 0; i < 52; i++) {
            double angle = i * 0.55D;
            double radius = Math.max(0.15D, 1.3D - i * 0.023D);
            double y = center.y + 0.2D + i * 0.16D;
            level.sendParticles(ParticleTypes.END_ROD,
                    center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.01D);
        }

        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI * 0.5D + 0.4D;
            level.sendParticles(ParticleTypes.SONIC_BOOM,
                    center.x + Math.cos(angle) * 2.6D, center.y + 1.1D, center.z + Math.sin(angle) * 2.6D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        int hits = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(TOLL_RADIUS + 1.0D))) {
            if (target == player || !target.isAlive() || target.isSpectator()) {
                continue;
            }
            if (target instanceof Player other && other.isCreative()) {
                continue;
            }
            if (target instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
                continue;
            }
            Vec3 offset = target.position().subtract(center);
            if (offset.length() > TOLL_RADIUS + target.getBbWidth() * 0.5D) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), TOLL_DAMAGE);
            Vec3 push = new Vec3(offset.x, 0.0D, offset.z);
            if (push.lengthSqr() < 1.0E-4D) {
                push = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            }
            if (push.lengthSqr() > 1.0E-4D) {
                push = push.normalize();
            }
            target.push(push.x * 1.1D, 0.6D, push.z * 1.1D);
            target.hurtMarked = true;
            hits++;
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                    12, 0.35D, 0.45D, 0.35D, 0.25D);
        }

        shake(level, center, 1.8F, 16);
    }

    private static void beginRush(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.position();
        Vec3 direction = player.getLookAngle();
        level.playSound(null, start.x, start.y, start.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.1F, 1.35F);
        level.playSound(null, start.x, start.y, start.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.3F, 1.15F);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                start.x, start.y + 1.0D, start.z, 40, 0.45D, 0.6D, 0.45D, 0.06D);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                start.x, start.y + 1.0D, start.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.addFreshEntity(new RedLine(level, start.add(0.0D, 1.05D, 0.0D), direction, 7.0F));
        BellTowerSkillScheduler.later(player, RUSH_IMPACT_DELAY, BellTowerSkillEffects::rushImpact);
    }

    private static void rushImpact(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
            flat = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        Vec3 direction = flat.normalize();
        AABB origin = player.getBoundingBox();
        Vec3 destination = start;
        for (double distance = 0.6D; distance <= RUSH_DISTANCE; distance += 0.4D) {
            Vec3 offset = direction.scale(distance);
            AABB moved = origin.move(offset);
            if (level.noCollision(player, moved)) {
                destination = start.add(offset);
                continue;
            }
            AABB raised = moved.move(0.0D, 1.0D, 0.0D);
            if (level.noCollision(player, raised)) {
                destination = start.add(offset).add(0.0D, 1.0D, 0.0D);
                continue;
            }
            break;
        }
        if (destination.distanceToSqr(start) < 0.25D) {
            Vec3 forced = start.add(direction.scale(1.2D));
            if (level.noCollision(player, origin.move(direction.scale(1.2D)).move(0.0D, 0.5D, 0.0D))) {
                destination = forced.add(0.0D, 0.5D, 0.0D);
            }
        }
        Vec3 travel = destination.subtract(start);
        Vec3 from = start.add(0.0D, 1.0D, 0.0D);
        Vec3 to = destination.add(0.0D, 1.0D, 0.0D);

        int hits = 0;
        AABB area = new AABB(start, destination).inflate(2.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player || !target.isAlive() || target.isSpectator()) {
                continue;
            }
            if (target instanceof Player other && other.isCreative()) {
                continue;
            }
            if (target instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
                continue;
            }
            Vec3 point = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (distanceToSegment(point, from, to) > 1.4D + target.getBbWidth() * 0.5D) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), RUSH_DAMAGE);
            target.push(direction.x * 0.85D, 0.35D, direction.z * 0.85D);
            target.hurtMarked = true;
            hits++;
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                    8, 0.3D, 0.35D, 0.3D, 0.12D);
        }

        int steps = Math.max(1, (int) Math.ceil(travel.length() / 0.3D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(travel.scale(i / (double) steps));
            level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    point.x, point.y + 1.0D, point.z, 4, 0.18D, 0.5D, 0.18D, 0.02D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        point.x, point.y + 0.9D, point.z, 1, 0.1D, 0.2D, 0.1D, 0.0D);
            }
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                        point.x, point.y + 1.1D, point.z, 1, 0.05D, 0.25D, 0.05D, 0.04D);
            }
        }

        player.teleportTo(destination.x, destination.y, destination.z);
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.FLASH,
                destination.x, destination.y + 1.0D, destination.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.addFreshEntity(new ProjectionShockwave(level,
                destination.add(0.0D, 0.05D, 0.0D), 3.4F, true));
        level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 1.6F);
        level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.1F, 1.25F);
        shake(level, destination, 1.1F, 10);
    }

    private static void beginRuin(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.8F, 0.55F);
        for (int i = 0; i < 40; i++) {
            double angle = level.random.nextDouble() * TAU;
            double radius = 2.0D + level.random.nextDouble() * 4.0D;
            level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    center.x + Math.cos(angle) * radius, center.y + 0.2D, center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.3D, 0.0D, 0.02D);
        }
        BellTowerSkillScheduler.later(player, 14, BellTowerSkillEffects::ruinImpact);
    }

    private static void ruinImpact(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 3.2F, 0.42F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 2.0F, 0.45F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.2F, 0.5F);

        level.addFreshEntity(new ProjectionShockwave(level, center.add(0.0D, 0.05D, 0.0D), 8.0F, true));
        BellTowerSkillScheduler.later(player, 4, p -> p.serverLevel().addFreshEntity(
                new ProjectionShockwave(p.serverLevel(), center.add(0.0D, 0.05D, 0.0D), 11.0F, true)));
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        for (int i = 0; i < 220; i++) {
            double angle = level.random.nextDouble() * TAU;
            double radius = 0.5D + level.random.nextDouble() * 7.5D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + 0.2D + level.random.nextDouble() * 2.5D;
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, 0.0D, 0.4D, 0.0D, 0.1D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z, 1, 0.08D, 0.5D, 0.08D, 0.03D);
            }
            if (i % 7 == 0) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, x, y + 0.6D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        int hits = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(8.5D))) {
            if (target == player || !target.isAlive() || target.isSpectator()) {
                continue;
            }
            if (target instanceof Player other && other.isCreative()) {
                continue;
            }
            if (target instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
                continue;
            }
            Vec3 offset = target.position().subtract(center);
            if (offset.length() > 7.5D + target.getBbWidth() * 0.5D) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), 18.0F);
            Vec3 push = new Vec3(offset.x, 0.0D, offset.z);
            if (push.lengthSqr() < 1.0E-4D) {
                push = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            }
            if (push.lengthSqr() > 1.0E-4D) {
                push = push.normalize();
            }
            target.push(push.x * 1.6D, 0.8D, push.z * 1.6D);
            target.hurtMarked = true;
            hits++;
        }

        shake(level, center, 2.4F, 20);
    }

    private static void staffRevelation(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.6F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6F, 1.2F);

        int hostiles = 0;
        int players = 0;
        double nearest = -1.0D;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(32.0D))) {
            if (target == player || !target.isAlive() || target.isSpectator()) {
                continue;
            }
            if (target instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0), player);
            double distance = target.distanceTo(player);
            if (nearest < 0.0D || distance < nearest) {
                nearest = distance;
            }
            if (target instanceof Enemy) {
                hostiles++;
            } else if (target instanceof Player) {
                players++;
            }
            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.8D, target.getZ(),
                    6, 0.25D, 0.4D, 0.25D, 0.03D);
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0), player);

        for (int i = 0; i < 120; i++) {
            double angle = i * TAU / 120.0D;
            double radius = 6.0D + (i % 3) * 4.0D;
            level.sendParticles(ParticleTypes.END_ROD,
                    center.x + Math.cos(angle) * radius, center.y + 0.4D, center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.15D, 0.0D, 0.02D);
        }

        List<String> lines = new ArrayList<>();
        lines.add(overlayJson(player, Component.translatable("gui.btmi.revelation.hostiles", hostiles)));
        lines.add(overlayJson(player, Component.translatable("gui.btmi.revelation.players", players)));
        lines.add(overlayJson(player, Component.translatable("gui.btmi.revelation.nearest",
                nearest < 0.0D ? "-" : String.format("%.1f", nearest))));
        PacketDistributor.sendToPlayer(player, new BellTowerOverlayPayload(
                overlayJson(player, Component.translatable("gui.btmi.revelation.title")), lines, 160));
    }

    public static void summonStrike(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        HitResult hit = player.pick(40.0D, 1.0F, false);
        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? eye.add(player.getLookAngle().scale(30.0D))
                : hit.getLocation();

        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.6F, 0.6F);
        for (int step = 0; step < 5; step++) {
            int index = step;
            BellTowerSkillScheduler.later(player, index * 4, p -> {
                ServerLevel l = p.serverLevel();
                double y = target.y + 2.0D + (4 - index) * 5.0D;
                for (int i = 0; i < 16; i++) {
                    double x = target.x + (l.random.nextDouble() - 0.5D) * 3.0D;
                    double z = target.z + (l.random.nextDouble() - 0.5D) * 3.0D;
                    l.sendParticles(ParticleTypes.END_ROD, x, y + l.random.nextDouble() * 2.0D, z,
                            1, 0.0D, -0.2D, 0.0D, 0.05D);
                    l.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y, z,
                            1, 0.05D, -0.3D, 0.05D, 0.02D);
                }
            });
        }
        BellTowerSkillScheduler.later(player, 20, p -> strikeImpact(p, target));
    }

    private static void strikeImpact(ServerPlayer player, Vec3 target) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8F, 0.6F);
        level.addFreshEntity(new ProjectionShockwave(level, target.add(0.0D, 0.05D, 0.0D), 5.0F, true));
        level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 1.0D, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SONIC_BOOM, target.x, target.y + 1.0D, target.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);

        int hits = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(target, target).inflate(4.5D))) {
            if (entity == player || !entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            if (entity instanceof Player other && other.isCreative()) {
                continue;
            }
            if (entity instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
                continue;
            }
            entity.hurt(player.damageSources().playerAttack(player), 14.0F);
            Vec3 offset = entity.position().subtract(target);
            if (offset.lengthSqr() > 1.0E-4D) {
                Vec3 push = offset.normalize();
                entity.push(push.x * 1.0D, 0.5D, push.z * 1.0D);
            }
            entity.hurtMarked = true;
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), player);
            BellTowerMarks.mark(player, entity, 200);
            hits++;
        }
        shake(level, target, 1.6F, 14);
    }

    private static String overlayJson(ServerPlayer player, Component component) {
        if (player.getServer() == null) {
            return "";
        }
        return Component.Serializer.toJson(component, player.getServer().registryAccess());
    }

    private static double distanceToSegment(Vec3 point, Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        double lengthSquared = direction.lengthSqr();
        if (lengthSquared < 1.0E-6D) {
            return point.distanceTo(from);
        }
        double t = point.subtract(from).dot(direction) / lengthSquared;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return point.distanceTo(from.add(direction.scale(t)));
    }

    private static void shake(ServerLevel level, Vec3 center, float scale, int duration) {
        if (!Config.SCREEN_SHAKE.get()) {
            return;
        }
        float strength = (float) (Config.SHAKE_STRENGTH.get() * scale);
        PacketDistributor.sendToPlayersNear(level, null, center.x, center.y, center.z, 20.0D,
                new ShakePayload(center.x, center.y, center.z, strength, duration));
    }
}
