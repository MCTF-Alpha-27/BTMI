package com.btmi.client;

import org.lwjgl.glfw.GLFW;

import com.btmi.BTMI;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final KeyMapping DETONATE_OBELISK = new KeyMapping(
            "key.btmi.obelisk_detonate",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.btmi");

    public static final KeyMapping AMMO_WHEEL = new KeyMapping(
            "key.btmi.ammo_wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.btmi");

    public static final KeyMapping WEAPON_SKILL = new KeyMapping(
            "key.btmi.weapon_skill",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.btmi");

    public static final KeyMapping BUTTERFLY_STRIKE = new KeyMapping(
            "key.btmi.butterfly_strike",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.btmi");

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DETONATE_OBELISK);
        event.register(AMMO_WHEEL);
        event.register(WEAPON_SKILL);
        event.register(BUTTERFLY_STRIKE);
    }
}
