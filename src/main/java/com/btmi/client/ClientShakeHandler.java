package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.network.ShakePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class ClientShakeHandler {
    private static final double MAX_DISTANCE = 48.0D;

    private static int ticks;
    private static int maxTicks = 1;
    private static float strength;

    private ClientShakeHandler() {
    }

    public static void handle(ShakePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // 按距离衰减：越远震得越轻
        double distance = minecraft.player.position().distanceTo(new Vec3(payload.x(), payload.y(), payload.z()));
        float falloff = (float) Mth.clamp(1.0D - distance / MAX_DISTANCE, 0.0D, 1.0D);
        float value = payload.intensity() * falloff;
        if (value <= 0.0F) {
            return;
        }
        // 更强的震动会覆盖较弱的震动，但较弱的震动不会打断仍在进行中的强震动
        if (value >= strength || ticks <= 0) {
            strength = value;
            maxTicks = Math.max(1, payload.duration());
            ticks = maxTicks;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ticks > 0) {
            ticks--;
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ticks <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float remaining = ticks / (float) maxTicks;
        float amplitude = strength * remaining * remaining;
        float time = (float) (minecraft.level.getGameTime() + event.getPartialTick());

        // 用三条不同频率的正弦波分别扰动 yaw / pitch / roll
        event.setYaw(event.getYaw() + (float) Math.sin(time * 37.0F) * amplitude);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 41.0F) * amplitude);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 53.0F) * amplitude * 0.7F);
    }
}
