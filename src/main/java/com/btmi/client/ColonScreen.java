package com.btmi.client;

import com.btmi.item.ColonAmmo;
import com.btmi.menu.ColonMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ColonScreen extends AbstractContainerScreen<ColonMenu> {
    public ColonScreen(ColonMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = ColonMenu.INV_Y - 12;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int cx = this.leftPos + ColonMenu.DISC_CENTER_X;
        int cy = this.topPos + ColonMenu.DISC_CENTER_Y;
        int radius = 44;

        // 实心圆作为弹巢盘
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(radius * radius - dy * dy);
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xFF2A2A33);
        }
        // 外环
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(radius * radius - dy * dy);
            graphics.fill(cx - half, cy + dy, cx - half + 2, cy + dy + 1, 0xFF6E6E80);
            graphics.fill(cx + half - 2, cy + dy, cx + half, cy + dy + 1, 0xFF6E6E80);
        }

        // 每个槽位的底色
        for (Slot slot : this.menu.slots) {
            graphics.fill(this.leftPos + slot.x - 1, this.topPos + slot.y - 1,
                    this.leftPos + slot.x + 17, this.topPos + slot.y + 17, 0xFF15151A);
            graphics.fill(this.leftPos + slot.x, this.topPos + slot.y,
                    this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0xFF3A3A46);
        }

        int selected = ColonAmmo.selected(this.menu.gun());
        if (selected >= 0 && selected < ColonAmmo.SLOTS) {
            Slot slot = this.menu.slots.get(selected);
            graphics.fill(this.leftPos + slot.x - 2, this.topPos + slot.y - 2,
                    this.leftPos + slot.x + 18, this.topPos + slot.y + 18, 0x80FFD24A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        // 圆盘正中显示鼠标所指弹巢的子弹名称，未指向槽位时显示当前选中的弹巢
        int index = hoveredAmmoSlot();
        if (index < 0) {
            index = ColonAmmo.selected(this.menu.gun());
        }
        ItemStack stack = this.menu.ammo().getItem(index);
        Component name = stack.isEmpty()
                ? Component.translatable("gui.btmi.colon.empty")
                : stack.getHoverName();
        graphics.drawCenteredString(this.font, name,
                ColonMenu.DISC_CENTER_X, ColonMenu.DISC_CENTER_Y - 4, 0xFFE8E8F0);
    }

    private int hoveredAmmoSlot() {
        Slot slot = this.hoveredSlot;
        if (slot == null || slot.index >= ColonAmmo.SLOTS) {
            return -1;
        }
        return slot.index;
    }
}
