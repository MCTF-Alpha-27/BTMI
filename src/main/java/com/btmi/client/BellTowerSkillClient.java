package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.belltower.skill.BellTowerSkill;
import com.btmi.belltower.skill.BellTowerSkills;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class BellTowerSkillClient {
    private static String disabledPassives = "";
    private static String activeId = "";
    private static String classId = "none";
    private static int cooldownTicks;

    private BellTowerSkillClient() {
    }

    public static void apply(String disabled, String active, int cooldown, String towerClass) {
        disabledPassives = disabled;
        activeId = active;
        cooldownTicks = Math.max(0, cooldown);
        classId = towerClass;
    }

    public static void reset() {
        disabledPassives = "";
        activeId = "";
        classId = "none";
        cooldownTicks = 0;
    }

    public static String classId() {
        return classId;
    }

    public static boolean passiveEnabled(BellTowerSkill skill) {
        if (skill == null) {
            return false;
        }
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            return BellTowerSkills.passiveEnabled(player, skill);
        }
        return !BellTowerSkills.isDisabled(disabledPassives, skill.id());
    }

    public static BellTowerSkill active() {
        BellTowerSkill skill = BellTowerSkills.byId(activeId);
        if (skill != null && skill.isActive()) {
            return skill;
        }
        Player player = Minecraft.getInstance().player;
        return player == null ? null : BellTowerSkills.selectedActive(player);
    }

    public static int cooldown() {
        return cooldownTicks;
    }

    public static void startCooldown(int ticks) {
        cooldownTicks = Math.max(cooldownTicks, ticks);
    }

    public static void localTogglePassive(String id) {
        disabledPassives = BellTowerSkills.toggleDisabled(disabledPassives, id);
    }

    public static void localSelect(String id) {
        BellTowerSkill skill = BellTowerSkills.byId(id);
        activeId = skill != null && skill.isActive() ? skill.id() : "";
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }
}
