package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.network.DeathSignActionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class DeathSignClientInput {
    private static boolean attackWasDown;

    private DeathSignClientInput() {
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
            sendForHeldItem(minecraft.player, DeathSignActionPayload.ACTION_ATTACK);
        }
        attackWasDown = attackDown;

        // 引爆键
        while (ModKeyMappings.DETONATE_DEATH_SIGN.consumeClick()) {
            sendForHeldItem(minecraft.player, DeathSignActionPayload.ACTION_DETONATE);
        }
    }

    private static void sendForHeldItem(Player player, int action) {
        if (isDeathSign(player.getMainHandItem())) {
            PacketDistributor.sendToServer(new DeathSignActionPayload(InteractionHand.MAIN_HAND.ordinal(), action));
        } else if (isDeathSign(player.getOffhandItem())) {
            PacketDistributor.sendToServer(new DeathSignActionPayload(InteractionHand.OFF_HAND.ordinal(), action));
        }
    }

    private static boolean isDeathSign(net.minecraft.world.item.ItemStack stack) {
        return stack.is(BTMI.DEATH_SIGN.get()) || stack.is(BTMI.DEATH_SIGN_RECALL.get());
    }
}
