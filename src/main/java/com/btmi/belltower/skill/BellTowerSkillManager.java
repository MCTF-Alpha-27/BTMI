package com.btmi.belltower.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.belltower.BellTowerClasses;
import com.btmi.belltower.BellTowerEnergy;
import com.btmi.network.BellTowerSkillPayload;
import com.btmi.network.BellTowerSkillStatePayload;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BellTowerSkillManager {
    private static final Map<UUID, Map<String, Long>> READY_AT = new HashMap<>();

    private BellTowerSkillManager() {
    }

    public static void handle(BellTowerSkillPayload payload, ServerPlayer player) {
        if (!BellTowerSkills.unlocked(player)) {
            sendState(player);
            return;
        }
        switch (payload.action()) {
            case BellTowerSkillPayload.ACTION_TOGGLE -> {
                BellTowerSkills.togglePassive(player, payload.skillId());
                sendState(player);
            }
            case BellTowerSkillPayload.ACTION_SELECT -> {
                BellTowerSkills.select(player, payload.skillId());
                sendState(player);
            }
            case BellTowerSkillPayload.ACTION_CAST -> cast(player);
            case BellTowerSkillPayload.ACTION_SET_CLASS -> {
                BellTowerClasses.choose(player, payload.skillId());
                sendState(player);
            }
            default -> {
            }
        }
    }

    public static void cast(ServerPlayer player) {
        BellTowerSkill skill = BellTowerSkills.selectedActive(player);
        if (skill == null) {
            sendState(player);
            return;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < readyAt(player, skill.id())) {
            sendState(player);
            return;
        }
        if (!BellTowerEnergy.consume(player, skill.energyCost())) {
            player.displayClientMessage(Component.translatable(BellTowerEnergy.hasItem(player)
                    ? "message.btmi.energy.low" : "message.btmi.energy.no_item").withStyle(ChatFormatting.RED), true);
            return;
        }
        BellTowerSkillEffects.cast(player, skill);
        READY_AT.computeIfAbsent(player.getUUID(), key -> new HashMap<>())
                .put(skill.id(), now + skill.cooldownTicks());
        sendState(player);
    }

    public static int cooldown(ServerPlayer player, BellTowerSkill skill) {
        long remaining = readyAt(player, skill.id()) - player.level().getGameTime();
        if (remaining <= 0L) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    public static void sendState(ServerPlayer player) {
        BellTowerSkill active = BellTowerSkills.selectedActive(player);
        int cooldown = active == null ? 0 : cooldown(player, active);
        PacketDistributor.sendToPlayer(player, new BellTowerSkillStatePayload(
                player.getData(BTMI.BELL_TOWER_DISABLED_PASSIVES.get()),
                player.getData(BTMI.BELL_TOWER_ACTIVE.get()),
                cooldown,
                player.getData(BTMI.BELL_TOWER_CLASS.get())));
    }

    public static void clear(UUID playerId) {
        READY_AT.remove(playerId);
    }

    private static long readyAt(ServerPlayer player, String skillId) {
        Map<String, Long> skills = READY_AT.get(player.getUUID());
        return skills == null ? 0L : skills.getOrDefault(skillId, 0L);
    }
}
