package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.network.ObeliskActionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class ObeliskClientInput {
    private static boolean attackWasDown;

    private ObeliskClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            attackWasDown = false;
            return;
        }

        // 攻击键按下（含最原始的鼠标左键状态作为兜底）
        boolean attackDown = minecraft.screen == null
                && (minecraft.options.keyAttack.isDown() || minecraft.mouseHandler.isLeftPressed());
        if (attackDown && !attackWasDown) {
            sendForHeldItem(minecraft.player, ObeliskActionPayload.ACTION_ATTACK);
        }
        attackWasDown = attackDown;

        // 引爆键
        while (ModKeyMappings.DETONATE_OBELISK.consumeClick()) {
            sendForHeldItem(minecraft.player, ObeliskActionPayload.ACTION_DETONATE);
        }
    }

    private static void sendForHeldItem(Player player, int action) {
        if (isObelisk(player.getMainHandItem())) {
            PacketDistributor.sendToServer(new ObeliskActionPayload(InteractionHand.MAIN_HAND.ordinal(), action));
        } else if (isObelisk(player.getOffhandItem())) {
            PacketDistributor.sendToServer(new ObeliskActionPayload(InteractionHand.OFF_HAND.ordinal(), action));
        }
    }

    private static boolean isObelisk(net.minecraft.world.item.ItemStack stack) {
        return stack.is(BTMI.OBELISK.get()) || stack.is(BTMI.OBELISK_RECALL.get());
    }
}
