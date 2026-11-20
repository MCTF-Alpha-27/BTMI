package com.btmi.belltower.skill;

import com.btmi.belltower.BellTowerClass;

import net.minecraft.resources.ResourceLocation;

public record BellTowerSkill(String id, BellTowerSkillType type, BellTowerClass allowedClass, int cooldownTicks,
        int energyCost, ResourceLocation icon) {
    public String nameKey() {
        return "skill.btmi." + this.id + ".name";
    }

    public String descriptionKey() {
        return "skill.btmi." + this.id + ".desc";
    }

    public boolean isActive() {
        return this.type == BellTowerSkillType.ACTIVE;
    }

    public boolean isPassive() {
        return this.type == BellTowerSkillType.PASSIVE;
    }

    public boolean availableTo(BellTowerClass playerClass) {
        return this.allowedClass == BellTowerClass.NONE || this.allowedClass == playerClass;
    }
}
