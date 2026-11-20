package com.btmi.belltower;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.entity.BloodRainShell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class BellTowerShieldEvents {
    private static final Map<ServerLevel, Long> CACHED_TICK = new IdentityHashMap<>();
    private static final Map<ServerLevel, List<ServerPlayer>> CACHED_PLAYERS = new IdentityHashMap<>();
    private static final Map<UUID, Integer> DRAIN_TICKS = new HashMap<>();
    private static final Map<UUID, Long> HEAVY_UNTIL = new HashMap<>();

    private BellTowerShieldEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!BellTowerShield.isActive(player)) {
            return;
        }
        UUID id = player.getUUID();
        int ticks = DRAIN_TICKS.merge(id, 1, Integer::sum);
        if (ticks % 20 != 0) {
            return;
        }
        boolean heavy = player.level().getGameTime() < HEAVY_UNTIL.getOrDefault(id, 0L);
        BellTowerEnergy.drain(player, heavy ? 2 : 1);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        DRAIN_TICKS.remove(id);
        HEAVY_UNTIL.remove(id);
    }

    private static void markHeavy(ServerPlayer player) {
        HEAVY_UNTIL.put(player.getUUID(), player.level().getGameTime() + 40L);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (entity instanceof Player) {
            return;
        }
        List<ServerPlayer> shielded = shieldedPlayers(level);
        if (shielded.isEmpty()) {
            return;
        }
        if (entity instanceof Projectile projectile) {
            if (projectile.isRemoved() || projectile instanceof BloodRainShell) {
                return;
            }
            blockProjectile(projectile, shielded);
        } else if (entity instanceof LivingEntity living && !living.isSpectator()) {
            repelLiving(living, shielded);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        if (!(hit.getEntity() instanceof ServerPlayer player) || !BellTowerShield.isActive(player)) {
            return;
        }
        if (projectile.getOwner() == player) {
            return;
        }
        event.setCanceled(true);
        shieldHit(level, projectile, BellTowerShield.center(player));
        markHeavy(player);
        projectile.discard();
    }

    private static List<ServerPlayer> shieldedPlayers(ServerLevel level) {
        long time = level.getGameTime();
        Long cached = CACHED_TICK.get(level);
        if (cached != null && cached.longValue() == time) {
            return CACHED_PLAYERS.get(level);
        }
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (BellTowerShield.isActive(player)) {
                players.add(player);
            }
        }
        CACHED_TICK.put(level, time);
        CACHED_PLAYERS.put(level, players);
        return players;
    }

    private static void blockProjectile(Projectile projectile, List<ServerPlayer> shielded) {
        double limit = BellTowerShield.RADIUS + 0.15D;
        Vec3 previous = new Vec3(projectile.xo, projectile.yo, projectile.zo);
        Vec3 current = projectile.position();
        for (ServerPlayer player : shielded) {
            if (projectile.getOwner() == player) {
                continue;
            }
            Vec3 center = BellTowerShield.center(player);
            if (!segmentHitsSphere(previous, current, center, limit)) {
                continue;
            }
            shieldHit((ServerLevel) projectile.level(), projectile, center);
            markHeavy(player);
            projectile.discard();
            return;
        }
    }

    private static boolean segmentHitsSphere(Vec3 from, Vec3 to, Vec3 center, double radius) {
        Vec3 direction = to.subtract(from);
        double lengthSquared = direction.lengthSqr();
        if (lengthSquared < 1.0E-6D) {
            return to.distanceToSqr(center) <= radius * radius;
        }
        Vec3 offset = from.subtract(center);
        double t = -offset.dot(direction) / lengthSquared;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vec3 closest = from.add(direction.scale(t));
        return closest.distanceToSqr(center) <= radius * radius;
    }

    private static void shieldHit(ServerLevel level, Entity projectile, Vec3 center) {
        level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                10, 0.2D, 0.2D, 0.2D, 0.02D);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 1.5F);
    }

    private static void repelLiving(LivingEntity living, List<ServerPlayer> shielded) {
        for (ServerPlayer player : shielded) {
            if (player.getVehicle() == living) {
                continue;
            }
            Vec3 center = BellTowerShield.center(player);
            Vec3 point = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
            Vec3 offset = point.subtract(center);
            double distance = offset.length();
            double limit = BellTowerShield.RADIUS + living.getBbWidth() * 0.5D;
            if (distance >= limit) {
                continue;
            }
            Vec3 direction = distance > 1.0E-3D
                    ? offset.scale(1.0D / distance)
                    : new Vec3(0.0D, 1.0D, 0.0D);
            double strength = Math.min(0.6D, 0.18D + (limit - distance) * 0.4D);
            Vec3 motion = living.getDeltaMovement();
            living.setDeltaMovement(
                    motion.x + direction.x * strength,
                    motion.y + Math.max(0.0D, direction.y) * strength * 0.25D,
                    motion.z + direction.z * strength);
            living.hasImpulse = true;
            markHeavy(player);
            return;
        }
    }
}
