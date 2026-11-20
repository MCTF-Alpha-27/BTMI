package com.btmi.belltower.skill;

import java.util.ArrayList;
import java.util.List;

import com.btmi.BTMI;
import com.btmi.belltower.BellTowerClass;
import com.btmi.belltower.BellTowerClasses;
import com.btmi.belltower.BellTowerPermission;
import com.btmi.belltower.BellTowerQuests;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class BellTowerSkills {
    public static final String BLOOD_RAIN = "blood_rain";
    public static final String BELL_TOLL = "bell_toll";
    public static final String BLOOD_RUSH = "blood_rush";
    public static final String HARBINGER_REVELATION = "harbinger_revelation";
    public static final String AGENT_RUIN = "agent_ruin";

    private static final List<BellTowerSkill> ALL = List.of(
            new BellTowerSkill(BLOOD_RAIN, BellTowerSkillType.PASSIVE, BellTowerClass.NONE, 0, 0, icon("blood_rain")),
            new BellTowerSkill(BELL_TOLL, BellTowerSkillType.ACTIVE, BellTowerClass.NONE, 200, 25, icon("bell_toll")),
            new BellTowerSkill(BLOOD_RUSH, BellTowerSkillType.ACTIVE, BellTowerClass.NONE, 140, 20, icon("blood_rush")),
            new BellTowerSkill(HARBINGER_REVELATION, BellTowerSkillType.ACTIVE, BellTowerClass.HARBINGER, 400, 15,
                    icon("harbinger_revelation")),
            new BellTowerSkill(AGENT_RUIN, BellTowerSkillType.ACTIVE, BellTowerClass.AGENT, 300, 35,
                    icon("agent_ruin")));

    private BellTowerSkills() {
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/gui/skill/" + name + ".png");
    }

    public static List<BellTowerSkill> all() {
        return ALL;
    }

    public static List<BellTowerSkill> available(Player player) {
        if (!unlocked(player)) {
            return List.of();
        }
        BellTowerClass playerClass = BellTowerClasses.get(player);
        List<BellTowerSkill> result = new ArrayList<>();
        for (BellTowerSkill skill : ALL) {
            if (skill.availableTo(playerClass)) {
                result.add(skill);
            }
        }
        return result;
    }

    public static BellTowerSkill byId(String id) {
        for (BellTowerSkill skill : ALL) {
            if (skill.id().equals(id)) {
                return skill;
            }
        }
        return null;
    }

    public static BellTowerSkill bloodRain() {
        return byId(BLOOD_RAIN);
    }

    public static boolean unlocked(Player player) {
        return BellTowerPermission.has(player) || BellTowerQuests.hasStarted(player);
    }

    public static BellTowerSkill selectedActive(Player player) {
        if (!unlocked(player)) {
            return null;
        }
        BellTowerSkill skill = byId(player.getData(BTMI.BELL_TOWER_ACTIVE.get()));
        if (skill == null || !skill.isActive()) {
            return null;
        }
        return skill.availableTo(BellTowerClasses.get(player)) ? skill : null;
    }

    public static boolean passiveEnabled(Player player, BellTowerSkill skill) {
        if (skill == null || !skill.isPassive() || !unlocked(player)) {
            return false;
        }
        return !isDisabled(player.getData(BTMI.BELL_TOWER_DISABLED_PASSIVES.get()), skill.id());
    }

    public static boolean bloodRainEnabled(Player player) {
        return passiveEnabled(player, bloodRain());
    }

    public static boolean isDisabled(String disabled, String id) {
        for (String part : disabled.split(",")) {
            if (part.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static String toggleDisabled(String disabled, String id) {
        List<String> parts = new ArrayList<>();
        for (String part : disabled.split(",")) {
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        if (!parts.remove(id)) {
            parts.add(id);
        }
        return String.join(",", parts);
    }

    public static void select(Player player, String id) {
        BellTowerSkill skill = byId(id);
        boolean allowed = skill != null && skill.isActive()
                && skill.availableTo(BellTowerClasses.get(player));
        player.setData(BTMI.BELL_TOWER_ACTIVE.get(), allowed ? skill.id() : "");
    }

    public static void togglePassive(Player player, String id) {
        BellTowerSkill skill = byId(id);
        if (skill == null || !skill.isPassive()) {
            return;
        }
        String current = player.getData(BTMI.BELL_TOWER_DISABLED_PASSIVES.get());
        player.setData(BTMI.BELL_TOWER_DISABLED_PASSIVES.get(), toggleDisabled(current, skill.id()));
    }

    public static int cooldownSeconds(BellTowerSkill skill, int ticks) {
        if (skill == null || ticks <= 0) {
            return 0;
        }
        return (int) Math.ceil(ticks / 20.0D);
    }
}
