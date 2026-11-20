package com.btmi.belltower;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.btmi.BTMI;
import com.btmi.network.BellTowerOverlayPayload;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BellTowerQuests {
    public static final String BOOK = "book";
    public static final String FILES = "files";
    public static final int FILE_TARGET = 3;

    private BellTowerQuests() {
    }

    public static boolean hasStarted(Player player) {
        return progress(player).getOrDefault(BOOK, 0) >= 1;
    }

    public static void onReadBook(ServerPlayer player) {
        Map<String, Integer> progress = progress(player);
        if (progress.getOrDefault(BOOK, 0) < 1) {
            progress.put(BOOK, 1);
            player.setData(BTMI.BELL_TOWER_QUESTS.get(), encode(progress));
        }
        sendQuestOverlay(player);
    }

    public static void onDocumentFound(ServerPlayer player, String file) {
    }

    public static void onFileCount(ServerPlayer player, int count) {
        int current = progress(player).getOrDefault(FILES, 0);
        int next = Math.min(FILE_TARGET, Math.max(current, count));
        if (next <= current) {
            return;
        }
        Map<String, Integer> progress = progress(player);
        progress.put(FILES, next);
        player.setData(BTMI.BELL_TOWER_QUESTS.get(), encode(progress));

        if (next >= FILE_TARGET && !BellTowerPermission.has(player)) {
            BellTowerPermission.grant(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            sendRecognition(player);
            return;
        }
        sendQuestOverlay(player);
    }

    public static void completeAll(ServerPlayer player) {
        Map<String, Integer> progress = progress(player);
        progress.put(FILES, FILE_TARGET);
        player.setData(BTMI.BELL_TOWER_QUESTS.get(), encode(progress));
        if (!BellTowerPermission.has(player)) {
            BellTowerPermission.grant(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        sendQuestOverlay(player);
    }

    public static void sendQuestOverlay(ServerPlayer player) {
        int found = progress(player).getOrDefault(FILES, 0);
        List<String> lines = new ArrayList<>();
        lines.add(json(player, Component.translatable("gui.btmi.book.line.files", found, FILE_TARGET)));
        lines.add(json(player, Component.translatable("gui.btmi.book.file.hint")));
        if (found >= FILE_TARGET) {
            lines.add(json(player, Component.translatable("gui.btmi.book.done")));
        }
        PacketDistributor.sendToPlayer(player, new BellTowerOverlayPayload(
                json(player, Component.translatable("gui.btmi.book.title")), lines, 180));
    }

    public static void sendStatus(ServerPlayer player) {
        sendQuestOverlay(player);
    }

    public static void reset(ServerPlayer player) {
        player.setData(BTMI.BELL_TOWER_QUESTS.get(), "");
    }

    private static void sendRecognition(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BellTowerOverlayPayload(
                json(player, Component.translatable("gui.btmi.book.recognized")),
                List.of(
                        json(player, Component.translatable("gui.btmi.book.recognized.line1")),
                        json(player, Component.translatable("gui.btmi.book.recognized.line2"))),
                200));
    }

    private static Map<String, Integer> progress(Player player) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String data = player.getData(BTMI.BELL_TOWER_QUESTS.get());
        for (String part : data.split(",")) {
            int index = part.indexOf('=');
            if (index <= 0) {
                continue;
            }
            try {
                result.put(part.substring(0, index), Integer.parseInt(part.substring(index + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static String encode(Map<String, Integer> progress) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : progress.entrySet()) {
            if (entry.getValue() > 0) {
                parts.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return String.join(",", parts);
    }

    private static String json(ServerPlayer player, Component component) {
        if (player.getServer() == null) {
            return "";
        }
        return Component.Serializer.toJson(component, player.getServer().registryAccess());
    }
}
