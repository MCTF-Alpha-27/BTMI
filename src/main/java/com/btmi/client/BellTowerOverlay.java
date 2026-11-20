package com.btmi.client;

import java.util.ArrayList;
import java.util.List;

import com.btmi.BTMI;
import com.btmi.network.BellTowerOverlayPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class BellTowerOverlay {
    private static final int MAX_WIDTH = 280;

    private static Component title = Component.empty();
    private static final List<Component> LINES = new ArrayList<>();
    private static int remainingTicks;

    private BellTowerOverlay() {
    }

    public static void show(BellTowerOverlayPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        title = parse(payload.titleJson(), minecraft).copy().withStyle(ChatFormatting.DARK_RED);
        LINES.clear();
        for (String json : payload.linesJson()) {
            LINES.add(parse(json, minecraft).copy().withStyle(ChatFormatting.RED));
        }
        remainingTicks = Math.max(20, payload.durationTicks());
    }

    private static Component parse(String json, Minecraft minecraft) {
        try {
            Component component = Component.Serializer.fromJson(json, minecraft.level.registryAccess());
            if (component != null) {
                return component;
            }
        } catch (Exception ignored) {
        }
        return Component.literal(json);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (remainingTicks <= 0 || minecraft.player == null || minecraft.options.hideGui || LINES.isEmpty()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        List<FormattedCharSequence> rendered = new ArrayList<>();
        for (Component line : LINES) {
            rendered.addAll(minecraft.font.split(line, MAX_WIDTH));
        }
        int width = Math.max(minecraft.font.width(title), rendered.stream()
                .mapToInt(minecraft.font::width).max().orElse(0)) + 16;
        int height = 20 + rendered.size() * 10;
        int left = (screenWidth - width) / 2;
        int bottom = screenHeight - 58;
        int top = bottom - height;

        graphics.fill(left, top, left + width, bottom, 0xB0100810);
        graphics.fill(left, top, left + width, top + 1, 0xFF8C1C24);
        graphics.fill(left, bottom - 1, left + width, bottom, 0xFF8C1C24);
        graphics.drawCenteredString(minecraft.font, title, screenWidth / 2, top + 4, 0xFFFF8080);

        int y = top + 16;
        for (FormattedCharSequence line : rendered) {
            graphics.drawString(minecraft.font, line, (screenWidth - minecraft.font.width(line)) / 2, y, 0xFFFF4040);
            y += 10;
        }
    }
}
