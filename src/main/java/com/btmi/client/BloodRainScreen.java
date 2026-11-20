package com.btmi.client;

import com.btmi.menu.BloodRainMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class BloodRainScreen extends AbstractContainerScreen<BloodRainMenu> {
    private static final int FRAME = 0xFF6B2A32;
    private static final int PANEL = 0xF0141012;
    private static final int SLOT_BORDER = 0xFF3A1118;
    private static final int SLOT_INNER = 0xFF1A1A20;

    public BloodRainScreen(BloodRainMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = BloodRainMenu.INV_Y - 12;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        int right = left + this.imageWidth;
        int bottom = top + this.imageHeight;

        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 2, FRAME);
        graphics.fill(left, bottom - 2, right, bottom, FRAME);
        graphics.fill(left, top, left + 2, bottom, FRAME);
        graphics.fill(right - 2, top, right, bottom, FRAME);

        int centerX = left + BloodRainMenu.SHELL_X + 8;
        int centerY = top + BloodRainMenu.SHELL_Y + 8;
        int radius = 26;
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(radius * radius - dy * dy);
            graphics.fill(centerX - half, centerY + dy, centerX - half + 1, centerY + dy + 1, 0xFF541C24);
            graphics.fill(centerX + half, centerY + dy, centerX + half + 1, centerY + dy + 1, 0xFF541C24);
        }
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(radius * radius - dy * dy);
            graphics.fill(centerX - half + 3, centerY + dy, centerX - half + 4, centerY + dy + 1, 0xFF2A1116);
            graphics.fill(centerX + half - 3, centerY + dy, centerX + half - 2, centerY + dy + 1, 0xFF2A1116);
        }

        int slotLeft = left + BloodRainMenu.SHELL_X;
        int slotTop = top + BloodRainMenu.SHELL_Y;
        graphics.fill(slotLeft - 2, slotTop - 2, slotLeft + 18, slotTop + 18, SLOT_BORDER);
        graphics.fill(slotLeft - 1, slotTop - 1, slotLeft + 17, slotTop + 17, SLOT_INNER);

        int hatchTop = top + this.imageHeight - 26;
        for (int x = left + 6; x < right - 6; x += 10) {
            graphics.fill(x, hatchTop, x + 5, hatchTop + 1, 0xFF43151B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        ItemStack shell = this.menu.getSlot(0).getItem();
        Component status = shell.isEmpty()
                ? Component.translatable("gui.btmi.blood_rain.empty")
                : shell.getHoverName();
        graphics.drawCenteredString(this.font, status, BloodRainMenu.SHELL_X + 8, BloodRainMenu.SHELL_Y - 14, 0xFFE8C8CE);
        graphics.drawCenteredString(this.font, Component.translatable("gui.btmi.blood_rain.hint"),
                BloodRainMenu.SHELL_X + 8, BloodRainMenu.SHELL_Y + 24, 0xFFB08088);
    }
}
