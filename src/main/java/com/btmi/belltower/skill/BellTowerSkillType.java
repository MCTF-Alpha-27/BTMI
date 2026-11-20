package com.btmi.belltower.skill;

public enum BellTowerSkillType {
    PASSIVE("skill.btmi.type.passive"),
    ACTIVE("skill.btmi.type.active");

    private final String translationKey;

    BellTowerSkillType(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }
}
