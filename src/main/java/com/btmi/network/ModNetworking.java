package com.btmi.network;

import com.btmi.BTMI;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BTMI.MODID)
public final class ModNetworking {
    private ModNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // 服务端 -> 客户端：屏幕震动
        registrar.playToClient(ShakePayload.TYPE, ShakePayload.STREAM_CODEC, ShakePayload::handle);
        // 客户端 -> 服务端：破折号的按键动作
        // 服务端 -> 客户端：破折号万象投影层数
        registrar.playToClient(DashChargePayload.TYPE, DashChargePayload.STREAM_CODEC, DashChargePayload::handle);
        registrar.playToServer(DashActionPayload.TYPE, DashActionPayload.STREAM_CODEC, DashActionPayload::handle);
        // 客户端 -> 服务端：感叹号的技能
        registrar.playToServer(ExclamationPayload.TYPE, ExclamationPayload.STREAM_CODEC, ExclamationPayload::handle);
        // 客户端 -> 服务端：等号 + 斜杠的联动技
        registrar.playToServer(DualWeaponPayload.TYPE, DualWeaponPayload.STREAM_CODEC, DualWeaponPayload::handle);
        // 客户端 -> 服务端：猩红之牙的开火与技能
        registrar.playToServer(ScarletFangPayload.TYPE, ScarletFangPayload.STREAM_CODEC, ScarletFangPayload::handle);
        // 客户端 -> 服务端：烈焰蝶技能
        registrar.playToServer(FlameCapePayload.TYPE, FlameCapePayload.STREAM_CODEC, FlameCapePayload::handle);
        // 客户端 -> 服务端：示亡号的按键动作
        registrar.playToServer(DeathSignActionPayload.TYPE, DeathSignActionPayload.STREAM_CODEC, DeathSignActionPayload::handle);
        // 客户端 -> 服务端：冒号的发射与选弹
        registrar.playToServer(ColonActionPayload.TYPE, ColonActionPayload.STREAM_CODEC, ColonActionPayload::handle);
    }
}
