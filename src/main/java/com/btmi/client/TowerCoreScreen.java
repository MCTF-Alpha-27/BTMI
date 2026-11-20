package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.belltower.BellTowerEnergy;
import com.btmi.block.TowerCoreMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class TowerCoreScreen extends AbstractContainerScreen<TowerCoreMenu> {
    public TowerCoreScreen(TowerCoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("block.btmi.tower_core"));
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xE8100C10);
        graphics.fill(left, top, left + this.imageWidth, top + 1, 0xFF8C2A32);
        graphics.fill(left, top + this.imageHeight - 1, left + this.imageWidth, top + this.imageHeight, 0xFF8C2A32);
        graphics.fill(left, top, left + 1, top + this.imageHeight, 0xFF8C2A32);
        graphics.fill(left + this.imageWidth - 1, top, left + this.imageWidth, top + this.imageHeight, 0xFF8C2A32);
        graphics.fill(left + 79, top + 34, left + 99, top + 54, 0xFF3A0E14);
        graphics.fill(left + 80, top + 35, left + 98, top + 53, 0xFF160A0E);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable("gui.btmi.tower_core.hint"),
                8, 54, 0xFFBFAEB2, false);

        ItemStack stack = this.menu.getSlot(0).getItem();
        if (!stack.isEmpty() && stack.is(BTMI.DIGAMMA.get())) {
            int energy = stack.getMaxDamage() - stack.getDamageValue();
            int width = 56;
            int x = 60;
            int y = 76;
            int fill = Math.round(width * (energy / (float) BellTowerEnergy.MAX));
            graphics.fill(x, y, x + width, y + 6, 0xFF160A0E);
            graphics.fill(x, y, x + fill, y + 6, 0xFFB01C2A);
            graphics.drawString(this.font, Component.translatable("hud.btmi.energy", energy, BellTowerEnergy.MAX),
                    x, y - 10, 0xFFFF8080, false);
        }
    }
}
