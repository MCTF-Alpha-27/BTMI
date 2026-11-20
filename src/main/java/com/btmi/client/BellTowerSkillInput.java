package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.belltower.BellTowerEnergy;
import com.btmi.belltower.skill.BellTowerSkill;
import com.btmi.belltower.skill.BellTowerSkills;
import com.btmi.network.BellTowerSkillPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class BellTowerSkillInput {
    private BellTowerSkillInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        while (ModKeyMappings.SKILL_MENU.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new BellTowerSkillScreen());
            }
        }
        while (ModKeyMappings.SKILL_CAST.consumeClick()) {
            if (minecraft.screen == null) {
                tryCast(player);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getButton() < GLFW.GLFW_MOUSE_BUTTON_4) {
            return;
        }
        if (!castBoundToSideButton()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        tryCast(minecraft.player);
    }

    private static boolean castBoundToSideButton() {
        InputConstants.Key key = ModKeyMappings.SKILL_CAST.getKey();
        return key.getType() == InputConstants.Type.MOUSE && key.getValue() >= GLFW.GLFW_MOUSE_BUTTON_4;
    }

    private static void tryCast(Player player) {
        if (player.isSpectator() || !BellTowerSkills.unlocked(player)) {
            return;
        }
        BellTowerSkill skill = BellTowerSkillClient.active();
        if (skill == null || BellTowerSkillClient.cooldown() > 0) {
            return;
        }
        if (!BellTowerEnergy.hasItem(player)) {
            player.displayClientMessage(
                    Component.translatable("message.btmi.energy.no_item").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (BellTowerEnergy.get(player) < skill.energyCost()) {
            player.displayClientMessage(
                    Component.translatable("message.btmi.energy.low").withStyle(ChatFormatting.RED), true);
            return;
        }
        BellTowerSkillClient.startCooldown(skill.cooldownTicks());
        PacketDistributor.sendToServer(new BellTowerSkillPayload(
                BellTowerSkillPayload.ACTION_CAST, skill.id()));
    }
}
