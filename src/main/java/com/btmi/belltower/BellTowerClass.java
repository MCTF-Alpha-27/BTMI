package com.btmi.belltower;

public enum BellTowerClass {
    NONE("none"),
    HARBINGER("harbinger"),
    AGENT("agent");

    private final String id;

    BellTowerClass(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public String translationKey() {
        return "class.btmi." + this.id;
    }

    public String descriptionKey() {
        return "class.btmi." + this.id + ".desc";
    }

    public boolean isChosen() {
        return this != NONE;
    }

    public static BellTowerClass byId(String id) {
        for (BellTowerClass value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return NONE;
    }
}
