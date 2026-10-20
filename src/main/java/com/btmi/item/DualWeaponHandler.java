package com.btmi.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.entity.GroundCrack;
import com.btmi.entity.RedLine;
import com.btmi.entity.SpearBeam;
import com.btmi.network.ShakePayload;
import com.btmi.weapon.ModAnimations;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import org.joml.Vector3f;

@EventBusSubscriber(modid = BTMI.MODID)
public final class DualWeaponHandler {
    private static final int LEVITATE_TICKS = 34;
    private static final int WINDUP_TICKS = 24;
    private static final int CHARGE_TICKS = 40;
    private static final int CHARGE_DELAY = 8;
    private static final double LEVITATE_SPEED = 0.075D;

    private static final Map<UUID, Ritual> RITUALS = new HashMap<>();
    private static final Map<UUID, Integer> THRUSTS = new HashMap<>();

    private static final class Ritual {
        private final List<UUID> targets = new ArrayList<>();
        private final Map<UUID, Vec3> lineDirections = new HashMap<>();
        private int age;
        private int phase;
        private int phaseTick;
        private int kills;
    }

    private DualWeaponHandler() {
    }

    // ------------------------------------------------------------------
    // 等号：断界
    // ------------------------------------------------------------------

    public static void equalSlash(ServerPlayer player) {
        boolean mainAxe = player.getMainHandItem().is(BTMI.EQUAL_SIGN.get());
        boolean offAxe = player.getOffhandItem().is(BTMI.EQUAL_SIGN.get());
        if (!mainAxe && !offAxe) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(BTMI.EQUAL_SIGN.get())) {
            return;
        }
        player.getCooldowns().addCooldown(BTMI.EQUAL_SIGN.get(), Config.EQUAL_SLASH_COOLDOWN.get());

        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-6D) {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }
        flat = flat.normalize();

        double range = Config.EQUAL_SLASH_RADIUS.get() + 3.5D;
        double width = 1.5D;
        float damage = Config.EQUAL_SLASH_DAMAGE.get().floatValue();
        Vec3 start = player.position();

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range + 1.0D, 3.0D, range + 1.0D),
                entity -> entity != player && entity.isAlive())) {
            Vec3 delta = target.position().subtract(start);
            Vec3 flatDelta = new Vec3(delta.x, 0.0D, delta.z);
            double along = flatDelta.dot(flat);
            if (along < 0.0D || along > range) {
                continue;
            }
            Vec3 perpendicular = flatDelta.subtract(flat.scale(along));
            if (perpendicular.length() > width || Math.abs(delta.y) > 3.0D) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), damage);
            target.push(flat.x * 0.35D, 0.85D, flat.z * 0.35D);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                    14, 0.3D, 0.3D, 0.3D, 0.25D);
        }

        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            patch.playAnimationSynchronized(
                    mainAxe ? Animations.VINDICATOR_SWING_AXE2 : ModAnimations.EQUAL_SLASH, 0.2F);
        }

        level.addFreshEntity(new GroundCrack(level, start, flat, (float) range));
        double groundY = player.getY() + 0.12D;
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
        for (double d = 0.6D; d <= range; d += 0.35D) {
            double x = start.x + flat.x * d;
            double z = start.z + flat.z * d;
            level.sendParticles(ParticleTypes.SQUID_INK, x, groundY, z, 2, 0.15D, 0.05D, 0.15D, 0.02D);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.02F, 0.05F), 1.4F),
                    x, groundY + 0.05D, z, 2, 0.12D, 0.05D, 0.12D, 0.02D);
            if (Math.round(d * 10.0D) % 3 == 0) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, groundY + 0.1D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        Vec3 endPoint = start.add(flat.scale(range));
        level.sendParticles(ParticleTypes.FLASH, start.x + flat.x * 1.2D, groundY + 0.5D,
                start.z + flat.z * 1.2D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SQUID_INK, endPoint.x, groundY + 0.3D, endPoint.z,
                24, 0.4D, 0.3D, 0.4D, 0.06D);
        level.playSound(null, start.x, start.y, start.z,
                SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.4F, 0.8F);
        level.playSound(null, endPoint.x, endPoint.y, endPoint.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        shake(level, endPoint, 0.9F, 12);
    }

    // ------------------------------------------------------------------
    // 斜杠：贯穿线
    // ------------------------------------------------------------------

    public static void slashThrust(ServerPlayer player) {
        boolean mainSpear = player.getMainHandItem().is(BTMI.SLASH.get());
        boolean offSpear = player.getOffhandItem().is(BTMI.SLASH.get());
        if (!mainSpear && !offSpear) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(BTMI.SLASH.get()) || THRUSTS.containsKey(player.getUUID())) {
            return;
        }
        player.getCooldowns().addCooldown(BTMI.SLASH.get(), Config.SLASH_THRUST_COOLDOWN.get());

        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            patch.playAnimationSynchronized(ModAnimations.SPEAR_CHARGE, 0.2F);
        }
        THRUSTS.put(player.getUUID(), 8);
    }

    private static void castThrust(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = Config.SLASH_THRUST_RANGE.get();
        float damage = Config.SLASH_THRUST_DAMAGE.get().floatValue();
        Vec3 end = eye.add(look.scale(range));

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), entity -> entity != player && entity.isAlive())) {
            Vec3 toTarget = target.getEyePosition().subtract(eye);
            double distance = toTarget.dot(look);
            if (distance < 0.0D || distance > range) {
                continue;
            }
            Vec3 perpendicular = toTarget.subtract(look.scale(distance));
            if (perpendicular.length() > 1.3D) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), damage);
            target.push(look.x * 0.7D, 0.35D, look.z * 0.7D);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                    12, 0.25D, 0.25D, 0.25D, 0.2D);
        }

        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            patch.stopPlaying(ModAnimations.SPEAR_CHARGE);
            patch.playAnimationSynchronized(Animations.BIPED_JAVELIN_THROW, 0.15F);
        }

        spawnRedLine(level, eye.add(look.scale(0.6D)), end, 1.2F, 24);
        level.sendParticles(ParticleTypes.FLASH, eye.x + look.x * 1.2D, eye.y + look.y * 1.2D,
                eye.z + look.z * 1.2D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SQUID_INK, end.x, end.y, end.z, 24, 0.5D, 0.5D, 0.5D, 0.08D);
        level.playSound(null, eye.x, eye.y, eye.z,
                SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2F, 0.9F);
        level.playSound(null, end.x, end.y, end.z,
                SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.0F, 1.1F);
        shake(level, end, 0.5F, 8);
    }

    // ------------------------------------------------------------------
    // 删除线：联动技
    // ------------------------------------------------------------------

    public static void strikethrough(ServerPlayer player) {
        if (!player.getMainHandItem().is(BTMI.SLASH.get())
                || !player.getOffhandItem().is(BTMI.EQUAL_SIGN.get())) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(BTMI.EQUAL_SIGN.get())
                || player.getCooldowns().isOnCooldown(BTMI.SLASH.get())
                || RITUALS.containsKey(player.getUUID())) {
            return;
        }
        int cooldown = Config.DUAL_STRIKETHROUGH_COOLDOWN.get();
        player.getCooldowns().addCooldown(BTMI.EQUAL_SIGN.get(), cooldown);
        player.getCooldowns().addCooldown(BTMI.SLASH.get(), cooldown);

        ServerLevel level = (ServerLevel) player.level();
        Ritual ritual = new Ritual();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(Config.DUAL_STRIKETHROUGH_RADIUS.get()),
                entity -> entity != player && entity.isAlive())) {
            ritual.targets.add(target.getUUID());
            target.setNoGravity(true);
            target.setDeltaMovement(0.0D, 0.02D, 0.0D);
            target.hurtMarked = true;
        }
        RITUALS.put(player.getUUID(), ritual);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0D, player.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (RITUALS.isEmpty() && THRUSTS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();

        if (!THRUSTS.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> thrustIterator = THRUSTS.entrySet().iterator();
            while (thrustIterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = thrustIterator.next();
                int left = entry.getValue() - 1;
                if (left <= 0) {
                    thrustIterator.remove();
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null && player.isAlive()) {
                        castThrust((ServerLevel) player.level(), player);
                    }
                } else {
                    entry.setValue(left);
                }
            }
        }

        if (RITUALS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Ritual>> iterator = RITUALS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Ritual> entry = iterator.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            Ritual ritual = entry.getValue();
            if (caster == null || !caster.isAlive()) {
                if (caster != null) {
                    restoreGravity((ServerLevel) caster.level(), ritual);
                }
                iterator.remove();
                continue;
            }

            ServerLevel level = (ServerLevel) caster.level();
            ritual.age++;
            switch (ritual.phase) {
                case 0 -> {
                    if (ritual.age <= LEVITATE_TICKS) {
                        tickLevitate(level, ritual);
                    } else {
                        ritual.phase = 1;
                        ritual.phaseTick = 0;
                        prepareLines(level, caster, ritual);
                    }
                }
                case 1 -> {
                    ritual.phaseTick++;
                    tickWindup(level, caster, ritual);
                }
                case 2 -> {
                    ritual.phaseTick++;
                    tickCharge(level, caster, ritual);
                }
                default -> {
                }
            }
            if (ritual.phase == 3) {
                restoreGravity(level, ritual);
                iterator.remove();
            }
        }
    }

    private static void tickLevitate(ServerLevel level, Ritual ritual) {
        for (UUID targetId : ritual.targets) {
            Entity entity = level.getEntities().get(targetId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            living.setDeltaMovement(0.0D, LEVITATE_SPEED, 0.0D);
            living.hurtMarked = true;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    living.getX(), living.getY(), living.getZ(), 2, 0.25D, 0.15D, 0.25D, 0.02D);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.65F, 0.03F, 0.08F), 1.2F),
                    living.getX(), living.getY() + 0.5D, living.getZ(), 2, 0.2D, 0.4D, 0.2D, 0.01D);
        }
    }

    private static void prepareLines(ServerLevel level, ServerPlayer caster, Ritual ritual) {
        for (UUID targetId : ritual.targets) {
            Entity entity = level.getEntities().get(targetId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            living.setDeltaMovement(Vec3.ZERO);
            living.hurtMarked = true;
            Vec3 delta = living.position().subtract(caster.position());
            Vec3 direction = new Vec3(delta.x, 0.0D, delta.z);
            if (direction.lengthSqr() < 1.0E-6D) {
                direction = new Vec3(1.0D, 0.0D, 0.0D);
            }
            direction = direction.normalize()
                    .add(0.0D, (level.random.nextDouble() - 0.5D) * 0.35D, 0.0D)
                    .normalize();
            ritual.lineDirections.put(targetId, direction);
        }
        level.playSound(null, caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void tickWindup(ServerLevel level, ServerPlayer caster, Ritual ritual) {
        float progress = Math.min(1.0F, ritual.phaseTick / (float) WINDUP_TICKS);

        if (ritual.phaseTick == 1) {
            for (UUID targetId : ritual.targets) {
                Entity entity = level.getEntities().get(targetId);
                if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                    continue;
                }
                Vec3 center = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
                Vec3 direction = ritual.lineDirections.getOrDefault(targetId, new Vec3(1.0D, 0.0D, 0.0D));
                level.addFreshEntity(new RedLine(level, center, direction, 3.2F));
                level.addFreshEntity(new RedLine(level, center, direction.scale(-1.0D), 3.2F));
            }
        }

        if (ritual.phaseTick == WINDUP_TICKS - 6) {
            ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(caster);
            if (patch != null) {
                patch.playAnimationSynchronized(ModAnimations.EQUAL_SLASH, 0.2F);
            }
        }
        if (ritual.phaseTick % 4 == 0) {
            shake(level, caster.position(), 0.2F + progress * 0.25F, 6);
        }

        if (ritual.phaseTick >= WINDUP_TICKS) {
            applyAxeDamage(level, caster, ritual);
            ritual.kills = (int) ritual.targets.stream().filter(id -> isDead(level, id)).count();
            if (ritual.kills <= 0) {
                ritual.phase = 3;
            } else {
                ritual.phase = 2;
                ritual.phaseTick = -CHARGE_DELAY;
                ServerPlayerPatch aimPatch = EpicFightCapabilities.getServerPlayerPatch(caster);
                if (aimPatch != null) {
                    aimPatch.playAnimationSynchronized(ModAnimations.SPEAR_CHARGE, 0.3F);
                }
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2F, 1.2F);
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
        }
    }

    private static void applyAxeDamage(ServerLevel level, ServerPlayer caster, Ritual ritual) {
        float damage = Config.DUAL_STRIKETHROUGH_DAMAGE.get().floatValue();
        for (UUID targetId : ritual.targets) {
            Entity entity = level.getEntities().get(targetId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            Vec3 center = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
            living.invulnerableTime = 0;
            living.hurt(level.damageSources().playerAttack(caster), damage);

            level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z,
                    18, 0.4D, 0.5D, 0.4D, 0.08D);
        }

        Vec3 look = caster.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-6D) {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }
        flat = flat.normalize();
        Vec3 arcCenter = caster.position().add(0.0D, 1.6D, 0.0D);
        spawnArc(level, arcCenter, flat, 5.0D, 4, 0.6D);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8F, 0.6F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 0.9F);
        shake(level, arcCenter, 1.0F, 14);
    }

    private static void tickCharge(ServerLevel level, ServerPlayer caster, Ritual ritual) {
        if (ritual.phaseTick < 0) {
            Vec3 pos = caster.position().add(0.0D, 1.0D, 0.0D);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.02F, 0.05F), 1.2F),
                    pos.x, pos.y, pos.z, 6, 0.8D, 0.5D, 0.8D, 0.02D);
            level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z,
                    4, 0.6D, 0.4D, 0.6D, 0.02D);
            return;
        }
        Vec3 look = caster.getLookAngle();
        Vec3 eye = caster.getEyePosition();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 tip = eye.add(look.scale(1.0D)).add(right.scale(0.15D)).add(0.0D, -0.25D, 0.0D);

        float progress = ritual.phaseTick / (float) CHARGE_TICKS;
        double radius = 0.8D - Math.min(0.6D, progress * 0.6D);
        double angleBase = ritual.phaseTick * 0.7D;
        for (int i = 0; i < 6; i++) {
            double angle = angleBase + i * (Math.PI * 2.0D / 6.0D);
            Vec3 point = tip.add(Math.cos(angle) * radius,
                    Math.sin(ritual.phaseTick * 0.5D) * 0.2D, Math.sin(angle) * radius);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.8F, 0.02F, 0.05F), 1.6F),
                    point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.02D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        level.sendParticles(ParticleTypes.SQUID_INK, tip.x, tip.y, tip.z, 3, 0.3D, 0.3D, 0.3D, 0.02D);
        if (ritual.phaseTick % (progress > 0.6F ? 2 : 4) == 0) {
            level.sendParticles(ParticleTypes.FLASH, tip.x, tip.y, tip.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            shake(level, caster.position(), 0.25F + progress * 0.6F, 5);
        }
        if (ritual.phaseTick % 5 == 0) {
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    0.6F, 0.8F + progress * 0.7F);
        }

        if (ritual.phaseTick >= CHARGE_TICKS) {
            castSpearThrow(level, caster, ritual.kills);
            ritual.phase = 3;
        }
    }

    private static void castSpearThrow(ServerLevel level, ServerPlayer caster, int kills) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(caster);
        if (patch != null) {
            patch.stopPlaying(ModAnimations.SPEAR_CHARGE);
            patch.playAnimationSynchronized(Animations.BIPED_JAVELIN_THROW, 0.15F);
        }

        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        double range = Config.DUAL_SPEAR_RANGE.get();
        double width = 1.6D;
        Vec3 end = eye.add(look.scale(range));
        float damage = Config.DUAL_SPEAR_DAMAGE.get().floatValue()
                + Config.DUAL_SPEAR_DAMAGE_PER_KILL.get().floatValue() * kills;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(range + 2.0D), entity -> entity != caster && entity.isAlive())) {
            Vec3 toTarget = target.getEyePosition().subtract(eye);
            double distance = toTarget.dot(look);
            if (distance < 0.0D || distance > range) {
                continue;
            }
            Vec3 perpendicular = toTarget.subtract(look.scale(distance));
            if (perpendicular.length() > width) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(caster), damage);
            target.push(look.x * 1.2D, 0.5D, look.z * 1.2D);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                    16, 0.3D, 0.3D, 0.3D, 0.25D);
        }

        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 beamStart = eye.add(look.scale(0.5D)).add(right.scale(0.35D)).add(0.0D, -0.3D, 0.0D);
        Vec3 beamEnd = eye.add(look.scale(range));
        Vec3 beamDir = beamEnd.subtract(beamStart).normalize();
        level.addFreshEntity(new SpearBeam(level, beamStart, beamDir, (float) beamEnd.distanceTo(beamStart)));
        level.sendParticles(new DustParticleOptions(new Vector3f(0.85F, 0.02F, 0.05F), 1.8F),
                beamStart.x, beamStart.y, beamStart.z, 20, 0.22D, 0.22D, 0.22D, 0.12D);
        level.sendParticles(ParticleTypes.FLASH, beamStart.x, beamStart.y, beamStart.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, end.x, end.y, end.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.4F, 1.5F);
        level.playSound(null, end.x, end.y, end.z,
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.2F, 1.0F);
        shake(level, end, 1.2F, 16);
    }

    private static void restoreGravity(ServerLevel level, Ritual ritual) {
        for (UUID targetId : ritual.targets) {
            Entity entity = level.getEntities().get(targetId);
            if (entity != null && entity.isAlive()) {
                entity.setNoGravity(false);
            }
        }
    }

    private static boolean isDead(ServerLevel level, UUID targetId) {
        Entity entity = level.getEntities().get(targetId);
        return !(entity instanceof LivingEntity living) || !living.isAlive();
    }

    private static void spawnArc(ServerLevel level, Vec3 center, Vec3 flat, double radius, int layers, double yOffset) {
        Vec3 right = new Vec3(-flat.z, 0.0D, flat.x);
        for (int layer = 0; layer < layers; layer++) {
            double layerRadius = radius * (0.55D + layer * 0.15D);
            double layerY = yOffset + layer * 0.25D;
            for (int i = 0; i <= 30; i++) {
                double angle = Math.toRadians(-80.0D + 160.0D * (i / 30.0D));
                Vec3 direction = flat.scale(Math.cos(angle)).add(right.scale(Math.sin(angle)));
                Vec3 point = center.add(direction.scale(layerRadius)).add(0.0D, layerY, 0.0D);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (i % 3 == 0) {
                    level.sendParticles(new DustParticleOptions(new Vector3f(0.68F, 0.03F, 0.08F), 1.4F),
                            point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.02D);
                }
            }
        }
    }

    private static void spawnRedLine(ServerLevel level, Vec3 from, Vec3 to, float scale, int steps) {
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, i / (double) steps);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.78F, 0.02F, 0.06F), scale),
                    point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.03D);
            }
        }
    }

    private static void shake(ServerLevel level, Vec3 center, float factor, int duration) {
        if (!Config.SCREEN_SHAKE.get()) {
            return;
        }
        PacketDistributor.sendToPlayersNear(level, null, center.x, center.y, center.z, 40.0D,
                new ShakePayload(center.x, center.y, center.z,
                        (float) (Config.SHAKE_STRENGTH.get() * factor), duration));
    }
}
