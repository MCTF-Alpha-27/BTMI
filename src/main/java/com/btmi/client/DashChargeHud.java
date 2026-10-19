package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.item.DashProjectionSkill;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class DashChargeHud {
    private static final int SEGMENT_WIDTH = 9;
    private static final int SEGMENT_HEIGHT = 5;
    private static final int GAP = 2;

    private static final int COLOR_EMPTY = 0x66000000;
    private static final int COLOR_FILLED = 0xFFD9A441;
    private static final int COLOR_READY = 0xFFFFF0A0;

    private static int charge;

    private DashChargeHud() {
    }

    public static void setCharge(int value) {
        charge = value;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        boolean holdingDash = minecraft.player.getMainHandItem().is(BTMI.DASH.get())
                || minecraft.player.getOffhandItem().is(BTMI.DASH.get());
        // 只在手持破折号时显示
        if (!holdingDash) {
            return;
        }

        int max = DashProjectionSkill.REQUIRED_HITS;
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        int totalWidth = max * SEGMENT_WIDTH + (max - 1) * GAP;
        int startX = (screenWidth - totalWidth) / 2;
        int y = screenHeight - 62;
        boolean ready = charge >= max;

        for (int i = 0; i < max; i++) {
            int x = startX + i * (SEGMENT_WIDTH + GAP);
            graphics.fill(x, y, x + SEGMENT_WIDTH, y + SEGMENT_HEIGHT, COLOR_EMPTY);
            if (i < charge) {
                graphics.fill(x + 1, y + 1, x + SEGMENT_WIDTH - 1, y + SEGMENT_HEIGHT - 1,
                        ready ? COLOR_READY : COLOR_FILLED);
            }
        }

        Component text = Component.translatable("hud.btmi.dash_projection", charge, max)
                .withStyle(ready ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        graphics.drawCenteredString(minecraft.font, text, screenWidth / 2, y - 11, 0xFFFFFFFF);
    }
}
