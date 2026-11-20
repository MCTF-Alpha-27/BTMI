package com.btmi.client;

import com.btmi.BTMI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class BloodRainClient {
    private static final ResourceLocation VANILLA_RAIN =
            ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
    private static final ResourceLocation BLOOD_RAIN =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/environment/blood_rain.png");

    private static int remainingTicks;
    private static int elapsedTicks;
    private static boolean swapped;

    private BloodRainClient() {
    }

    public static void apply(boolean active, int durationTicks) {
        if (!active || durationTicks <= 0) {
            remainingTicks = 0;
            restore();
            return;
        }
        remainingTicks = durationTicks;
        elapsedTicks = 0;
        swap();
    }

    public static void reset() {
        remainingTicks = 0;
        elapsedTicks = 0;
        restore();
    }

    public static boolean isActive() {
        return remainingTicks > 0;
    }

    public static float fade() {
        float in = Math.min(1.0F, elapsedTicks / 20.0F);
        float out = Math.min(1.0F, remainingTicks / 20.0F);
        return Math.min(in, out);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            reset();
            return;
        }
        if (remainingTicks > 0) {
            remainingTicks--;
            elapsedTicks++;
            if (remainingTicks == 0) {
                restore();
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void swap() {
        if (swapped) {
            return;
        }
        Minecraft.getInstance().getTextureManager().register(VANILLA_RAIN, new SimpleTexture(BLOOD_RAIN));
        swapped = true;
    }

    private static void restore() {
        if (!swapped) {
            return;
        }
        Minecraft.getInstance().getTextureManager().register(VANILLA_RAIN, new SimpleTexture(VANILLA_RAIN));
        swapped = false;
    }
}
