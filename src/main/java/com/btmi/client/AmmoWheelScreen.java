package com.btmi.client;

import java.util.List;

import com.btmi.BTMI;
import com.btmi.item.ColonAmmo;
import com.btmi.network.ColonActionPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class AmmoWheelScreen extends Screen {
    private static final int RADIUS = 62;

    private final ItemStack gun;
    private int hovered;

    public AmmoWheelScreen(ItemStack gun) {
        super(Component.translatable("gui.btmi.colon.wheel"));
        this.gun = gun;
        this.hovered = ColonAmmo.selected(gun);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 轮盘自绘背景，不使用原版的暗色遮罩
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = slotAt(mouseX, mouseY);

        List<ItemStack> ammo = ColonAmmo.read(this.gun);
        int selected = ColonAmmo.selected(this.gun);
        int cx = this.width / 2;
        int cy = this.height / 2;

        int backdrop = RADIUS + 24;
        for (int dy = -backdrop; dy <= backdrop; dy++) {
            int half = (int) Math.sqrt(Math.max(0, backdrop * backdrop - dy * dy));
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xC8101016);
        }
        graphics.drawCenteredString(this.font, this.title, cx, cy - backdrop - 14, 0xFFFFFFFF);

        for (int i = 0; i < ColonAmmo.SLOTS; i++) {
            double angle = Math.toRadians(-90.0D + i * 60.0D);
            int ix = cx + (int) Math.round(Math.cos(angle) * RADIUS);
            int iy = cy + (int) Math.round(Math.sin(angle) * RADIUS);

            graphics.fill(ix - 12, iy - 12, ix + 12, iy + 12, i == this.hovered ? 0xFFFFD24A : 0xFF3A3A46);
            graphics.fill(ix - 11, iy - 11, ix + 11, iy + 11, 0xFF15151A);

            ItemStack bullet = ammo.get(i);
            if (!bullet.isEmpty()) {
                graphics.renderItem(bullet, ix - 8, iy - 8);
            }
            if (i == selected) {
                graphics.fill(ix - 13, iy - 13, ix - 11, iy + 13, 0xFF6EE7FF);
                graphics.fill(ix + 11, iy - 13, ix + 13, iy + 13, 0xFF6EE7FF);
                graphics.fill(ix - 13, iy - 13, ix + 13, iy - 11, 0xFF6EE7FF);
                graphics.fill(ix - 13, iy + 11, ix + 13, iy + 13, 0xFF6EE7FF);
            }
        }

        // 圆盘中心显示当前悬停（或已选中）弹巢里的子弹名称
        int shown = this.hovered >= 0 ? this.hovered : selected;
        ItemStack shownStack = ammo.get(shown);
        Component name = shownStack.isEmpty()
                ? Component.translatable("gui.btmi.colon.empty")
                : shownStack.getHoverName();
        graphics.drawCenteredString(this.font, name, cx, cy - 4, 0xFFE8E8F0);
    }

    private int slotAt(int mouseX, int mouseY) {
        double dx = mouseX - this.width / 2.0D;
        double dy = mouseY - this.height / 2.0D;
        if (dx * dx + dy * dy < 196.0D) {
            return ColonAmmo.selected(this.gun);
        }
        // 正上方为 0 号弹巢，顺时针每 60 度一个
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0D;
        if (angle < 0.0D) {
            angle += 360.0D;
        }
        return (int) Math.round(angle / 60.0D) % ColonAmmo.SLOTS;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.AMMO_WHEEL.matches(keyCode, scanCode)) {
            this.confirm();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.hovered = slotAt((int) mouseX, (int) mouseY);
            this.confirm();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void confirm() {
        PacketDistributor.sendToServer(new ColonActionPayload(ColonActionPayload.ACTION_SELECT, this.hovered));
        this.onClose();
    }
}
