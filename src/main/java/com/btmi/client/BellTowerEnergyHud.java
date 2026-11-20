package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.belltower.BellTowerEnergy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class BellTowerEnergyHud {
    private static final int WIDTH = 104;
    private static final int HEIGHT = 8;

    private BellTowerEnergyHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !BellTowerEnergy.hasItem(player)) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int energy = BellTowerEnergy.get(player);
        int x = screenWidth - WIDTH - 12;
        int y = screenHeight - 38;
        int fill = Math.round(WIDTH * (energy / (float) BellTowerEnergy.MAX));

        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFF2A0A0C);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF160A0E);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + HEIGHT, 0xFFB01C2A);
            graphics.fill(x, y, x + fill, y + 2, 0xFFF06060);
        }
        for (int i = 1; i < 5; i++) {
            int tickX = x + WIDTH * i / 5;
            graphics.fill(tickX, y, tickX + 1, y + HEIGHT, 0x66000000);
        }

        Component text = Component.translatable("hud.btmi.energy", energy, BellTowerEnergy.MAX);
        graphics.drawString(minecraft.font, text, x, y - 10, 0xFFFF9090, false);
    }
}
