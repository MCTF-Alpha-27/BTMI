package com.btmi.client;

import net.minecraft.client.Minecraft;

public final class StateAffairsChronicleClient {
    private StateAffairsChronicleClient() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new StateAffairsChronicleScreen());
    }
}
