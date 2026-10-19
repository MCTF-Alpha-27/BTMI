package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.network.CrimsonFangPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class CrimsonFangClientInput {
    private CrimsonFangClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        if (!minecraft.player.getMainHandItem().is(BTMI.CRIMSON_FANG.get())) {
            return;
        }
        boolean attackDown = minecraft.options.keyAttack.isDown() || minecraft.mouseHandler.isLeftPressed();
        if (attackDown) {
            PacketDistributor.sendToServer(new CrimsonFangPayload(CrimsonFangPayload.ACTION_FIRE));
        }
    }

    // 手持猩红之牙时屏蔽左键的原版挥击动作，避免开火时出现挥手
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().is(BTMI.CRIMSON_FANG.get())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
