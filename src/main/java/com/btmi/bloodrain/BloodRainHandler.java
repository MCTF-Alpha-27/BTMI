package com.btmi.bloodrain;

import java.util.HashMap;
import java.util.Map;

import com.btmi.BTMI;
import com.btmi.network.BloodRainPayload;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BTMI.MODID)
public final class BloodRainHandler {
    public static final int DURATION_TICKS = 3 * 60 * 20;
    private static final Map<ResourceKey<Level>, Long> EXPIRY = new HashMap<>();

    private BloodRainHandler() {
    }

    public static void start(ServerLevel level) {
        long expiry = level.getGameTime() + DURATION_TICKS;
        EXPIRY.put(level.dimension(), expiry);
        level.setWeatherParameters(0, DURATION_TICKS, true, false);
        PacketDistributor.sendToPlayersInDimension(level, new BloodRainPayload(true, DURATION_TICKS));
    }

    public static boolean isActive(ServerLevel level) {
        Long expiry = EXPIRY.get(level.dimension());
        return expiry != null && level.getGameTime() < expiry;
    }

    public static boolean stop(ServerLevel level) {
        if (EXPIRY.remove(level.dimension()) == null) {
            return false;
        }
        level.setWeatherParameters(12000, 0, false, false);
        PacketDistributor.sendToPlayersInDimension(level, new BloodRainPayload(false, 0));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            Long expiry = EXPIRY.get(level.dimension());
            if (expiry == null || level.getGameTime() < expiry) {
                continue;
            }
            stop(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        EXPIRY.clear();
    }

    private static void syncTo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Long expiry = EXPIRY.get(level.dimension());
        int remaining = expiry == null ? 0 : (int) Math.max(0L, expiry - level.getGameTime());
        PacketDistributor.sendToPlayer(player, new BloodRainPayload(remaining > 0, remaining));
    }
}
