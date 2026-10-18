package com.btmi.client;

import com.btmi.BTMI;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

// 冒号的后坐力：开火瞬间视角向上抬一点，随后快速回落。
@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class ColonRecoilHandler {
    // 初始抬升角度（度）
    private static final float KICK = 2.2F;
    // 每刻的衰减系数
    private static final float DECAY = 0.55F;

    private static float offset;

    private ColonRecoilHandler() {
    }

    public static void kick() {
        offset = KICK;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        offset *= DECAY;
        if (offset < 0.02F) {
            offset = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (offset <= 0.0F) {
            return;
        }
        // 俯仰角减小即为向上看
        event.setPitch(event.getPitch() - offset);
    }
}
