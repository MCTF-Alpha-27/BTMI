package com.btmi.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class StateAffairsChronicleScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 240;
    private static final int BODY_WIDTH = PANEL_WIDTH - 40;
    private static final List<Page> PAGES = List.of(
            new Page("gui.btmi.chronicle.d1", List.of(
                    "gui.btmi.chronicle.p1.a",
                    "gui.btmi.chronicle.p1.b")),
            new Page("gui.btmi.chronicle.d2", List.of(
                    "gui.btmi.chronicle.p2.a",
                    "gui.btmi.chronicle.p2.b")),
            new Page("gui.btmi.chronicle.d3", List.of(
                    "gui.btmi.chronicle.p3.a",
                    "gui.btmi.chronicle.p3.b")));

    private int page;

    public StateAffairsChronicleScreen() {
        super(Component.translatable("gui.btmi.chronicle.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        graphics.fill(left, top, right, bottom, 0xF0140A0E);
        graphics.fill(left, top, right, top + 1, 0xFF8C2A32);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF8C2A32);
        graphics.fill(left, top, left + 1, bottom, 0xFF8C2A32);
        graphics.fill(right - 1, top, right, bottom, 0xFF8C2A32);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 14, 0xFFF0D9A0);

        Page current = PAGES.get(this.page);
        graphics.drawString(this.font, Component.translatable(current.titleKey()), left + 20, top + 38, 0xFFE0A84C,
                false);

        int bodyTop = top + 54;
        for (String key : current.bodyKeys()) {
            for (FormattedCharSequence line : this.font.split(Component.translatable(key), BODY_WIDTH)) {
                graphics.drawString(this.font, line, left + 20, bodyTop, 0xFFE8DCC0, false);
                bodyTop += 10;
            }
            bodyTop += 4;
        }

        int arrowY = bottom - 24;
        int pageY = bottom - 20;
        if (this.page > 0) {
            graphics.drawString(this.font, "‹", left + 20, arrowY, 0xFFE0A84C, false);
        }
        if (this.page < PAGES.size() - 1) {
            graphics.drawString(this.font, "›", right - 28, arrowY, 0xFFE0A84C, false);
        }
        graphics.drawCenteredString(this.font, (this.page + 1) + " / " + PAGES.size(),
                this.width / 2, pageY, 0xFF8C7C80);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = (this.width - PANEL_WIDTH) / 2;
            int top = (this.height - PANEL_HEIGHT) / 2;
            int right = left + PANEL_WIDTH;
            int bottom = top + PANEL_HEIGHT;
            if (mouseY >= bottom - 30) {
                if (mouseX >= left + 12 && mouseX <= left + 40 && this.page > 0) {
                    this.page--;
                    return true;
                }
                if (mouseX >= right - 44 && mouseX <= right - 12 && this.page < PAGES.size() - 1) {
                    this.page++;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263 && this.page > 0) {
            this.page--;
            return true;
        }
        if (keyCode == 262 && this.page < PAGES.size() - 1) {
            this.page++;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private record Page(String titleKey, List<String> bodyKeys) {
    }
}
