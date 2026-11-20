package com.btmi.belltower.skill;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.btmi.BTMI;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class BellTowerSkillScheduler {
    private static final List<Task> TASKS = new ArrayList<>();

    private BellTowerSkillScheduler() {
    }

    public static void later(ServerPlayer player, int delay, Consumer<ServerPlayer> action) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        long due = server.overworld().getGameTime() + Math.max(0, delay);
        TASKS.add(new Task(player.getUUID(), due, action));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (TASKS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        List<Task> due = null;
        Iterator<Task> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (now < task.due()) {
                continue;
            }
            iterator.remove();
            if (due == null) {
                due = new ArrayList<>();
            }
            due.add(task);
        }
        if (due == null) {
            return;
        }
        for (Task task : due) {
            ServerPlayer player = server.getPlayerList().getPlayer(task.playerId());
            if (player != null && player.isAlive() && !player.isSpectator()) {
                task.action().accept(player);
            }
        }
    }

    private record Task(UUID playerId, long due, Consumer<ServerPlayer> action) {
    }
}
