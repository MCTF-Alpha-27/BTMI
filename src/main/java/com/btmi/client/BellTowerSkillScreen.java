package com.btmi.client;

import java.util.List;

import com.btmi.BTMI;
import com.btmi.belltower.BellTowerClass;
import com.btmi.belltower.BellTowerClasses;
import com.btmi.belltower.BellTowerPermission;
import com.btmi.belltower.skill.BellTowerSkill;
import com.btmi.belltower.skill.BellTowerSkills;
import com.btmi.network.BellTowerSkillPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class BellTowerSkillScreen extends Screen {
    private static final int PANEL_WIDTH = 270;
    private static final int ROW_HEIGHT = 26;
    private static final int ROW_STEP = 28;
    private static final int FOOTER = 44;
    private static final int CLASS_HEIGHT = 18;
    private static final int CLASS_GAP = 4;

    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int headerHeight;

    public BellTowerSkillScreen() {
        super(Component.translatable("gui.btmi.skills.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        Player player = this.minecraft == null ? null : this.minecraft.player;
        boolean unlocked = player != null && BellTowerSkills.unlocked(player);
        boolean permitted = player != null && BellTowerPermission.has(player);
        BellTowerClass towerClass = player == null ? BellTowerClass.NONE : BellTowerClasses.get(player);
        boolean choosingClass = permitted && !towerClass.isChosen();
        List<BellTowerSkill> skills = player == null ? List.of() : BellTowerSkills.available(player);

        this.headerHeight = choosingClass ? 60 : towerClass.isChosen() ? 44 : 30;
        this.panelHeight = this.headerHeight + Math.max(1, skills.size()) * ROW_STEP + FOOTER;
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;
        int left = this.panelLeft;
        int top = this.panelTop;
        int right = left + PANEL_WIDTH;
        int bottom = top + this.panelHeight;

        graphics.fill(left, top, right, bottom, 0xE8100C10);
        graphics.fill(left, top, right, top + 1, 0xFF8C2A32);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF8C2A32);
        graphics.fill(left, top, left + 1, bottom, 0xFF8C2A32);
        graphics.fill(right - 1, top, right, bottom, 0xFF8C2A32);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFF0D9A0);

        BellTowerClass hoveredClass = null;
        if (towerClass.isChosen()) {
            Component className = Component.translatable(towerClass.translationKey()).withStyle(ChatFormatting.RED);
            graphics.drawCenteredString(this.font, Component.translatable("gui.btmi.skills.class", className),
                    this.width / 2, top + 24, 0xFFD8C8CC);
        } else if (choosingClass) {
            hoveredClass = renderClassButtons(graphics, mouseX, mouseY, left + 10, top + 26);
        }

        BellTowerSkill selected = BellTowerSkillClient.active();
        BellTowerSkill hoveredSkill = null;
        int rowX = left + 10;
        int rowWidth = PANEL_WIDTH - 20;
        if (!unlocked) {
            graphics.drawCenteredString(this.font,
                    Component.translatable(player != null && BellTowerPermission.has(player)
                            ? "gui.btmi.skills.locked"
                            : "gui.btmi.skills.find_book"),
                    this.width / 2, top + this.headerHeight + 12, 0xFFD06060);
        } else {
            for (int i = 0; i < skills.size(); i++) {
                BellTowerSkill skill = skills.get(i);
                int y = top + this.headerHeight + i * ROW_STEP;
                boolean over = mouseX >= rowX && mouseX < rowX + rowWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
                if (over) {
                    hoveredSkill = skill;
                }
                boolean chosen = skill.isActive() && selected != null && selected.id().equals(skill.id());
                int background = chosen ? 0xF03A1218 : over ? 0xE0261A20 : 0xC0181218;
                graphics.fill(rowX, y, rowX + rowWidth, y + ROW_HEIGHT, background);
                if (chosen) {
                    graphics.fill(rowX, y, rowX + rowWidth, y + 1, 0xFFE0A84C);
                    graphics.fill(rowX, y + ROW_HEIGHT - 1, rowX + rowWidth, y + ROW_HEIGHT, 0xFFE0A84C);
                    graphics.fill(rowX, y, rowX + 1, y + ROW_HEIGHT, 0xFFE0A84C);
                    graphics.fill(rowX + rowWidth - 1, y, rowX + rowWidth, y + ROW_HEIGHT, 0xFFE0A84C);
                }

                graphics.blit(skill.icon(), rowX + 5, y + 5, 0, 0, 16, 16, 16, 16);
                graphics.drawString(this.font, Component.translatable(skill.nameKey()), rowX + 27, y + 4,
                        0xFFF2E8EA);
                Component type = Component.translatable(skill.type().translationKey());
                if (skill.allowedClass().isChosen()) {
                    type = type.copy().append(" · ").append(Component.translatable(skill.allowedClass().translationKey()));
                }
                if (skill.energyCost() > 0) {
                    type = type.copy().append(" · ").append(
                            Component.translatable("gui.btmi.skills.energy_cost", skill.energyCost()));
                }
                graphics.drawString(this.font, type, rowX + 27, y + 14, 0xFFA88E94);

                Component state = stateOf(skill, chosen, selected);
                int stateColor = chosen ? 0xFFE0A84C : 0xFFC8B8BC;
                if (state != null) {
                    graphics.drawString(this.font, state, rowX + rowWidth - 8 - this.font.width(state), y + 9,
                            stateColor);
                }
            }
        }

        int footerY = top + this.headerHeight + Math.max(1, skills.size()) * ROW_STEP + 6;
        if (hoveredClass != null) {
            drawDescription(graphics, Component.translatable(hoveredClass.descriptionKey()), rowX, footerY, rowWidth);
        } else if (hoveredSkill != null) {
            drawDescription(graphics, Component.translatable(hoveredSkill.descriptionKey()), rowX, footerY, rowWidth);
        } else if (!unlocked) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.btmi.book.hint"),
                    this.width / 2, footerY, 0xFF8C7C80);
        }

        Component hint;
        if (selected == null) {
            hint = Component.translatable("gui.btmi.skills.no_active");
        } else {
            hint = Component.translatable("gui.btmi.skills.hint",
                    ModKeyMappings.SKILL_CAST.getTranslatedKeyMessage(),
                    ModKeyMappings.SKILL_MENU.getTranslatedKeyMessage());
        }
        graphics.drawCenteredString(this.font, hint, this.width / 2, top + this.panelHeight - 15, 0xFF8C7C80);
    }

    private BellTowerClass renderClassButtons(GuiGraphics graphics, int mouseX, int mouseY, int left, int y) {
        int inner = PANEL_WIDTH - 20;
        int width = (inner - CLASS_GAP) / 2;
        BellTowerClass hovered = null;
        BellTowerClass[] values = {BellTowerClass.HARBINGER, BellTowerClass.AGENT};
        for (int i = 0; i < values.length; i++) {
            BellTowerClass value = values[i];
            int x = left + i * (width + CLASS_GAP);
            boolean over = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + CLASS_HEIGHT;
            if (over) {
                hovered = value;
            }
            graphics.fill(x, y, x + width, y + CLASS_HEIGHT, over ? 0xE03A1218 : 0xC0181218);
            graphics.fill(x, y, x + width, y + 1, over ? 0xFFE0A84C : 0xFF5A2A30);
            graphics.drawCenteredString(this.font,
                    Component.translatable(value.translationKey()).withStyle(ChatFormatting.RED),
                    x + width / 2, y + 5, 0xFFFF6060);
        }
        return hovered;
    }

    private void drawDescription(GuiGraphics graphics, Component description, int x, int y, int width) {
        int lineY = y;
        for (var line : this.font.split(description, width)) {
            graphics.drawString(this.font, line, x, lineY, 0xFFBFAEB2);
            lineY += 10;
        }
    }

    private Component stateOf(BellTowerSkill skill, boolean chosen, BellTowerSkill selected) {
        if (skill.isPassive()) {
            boolean enabled = BellTowerSkillClient.passiveEnabled(skill);
            return Component.translatable(enabled ? "gui.btmi.skills.on" : "gui.btmi.skills.off");
        }
        if (!chosen) {
            return null;
        }
        int cooldown = BellTowerSkillClient.cooldown();
        if (cooldown > 0) {
            return Component.translatable("gui.btmi.skills.cooldown",
                    BellTowerSkills.cooldownSeconds(skill, cooldown));
        }
        return Component.translatable("gui.btmi.skills.equipped");
    }

    private BellTowerSkill rowAt(double mouseX, double mouseY, List<BellTowerSkill> skills) {
        int rowX = this.panelLeft + 10;
        int rowWidth = PANEL_WIDTH - 20;
        for (int i = 0; i < skills.size(); i++) {
            int y = this.panelTop + this.headerHeight + i * ROW_STEP;
            if (mouseX >= rowX && mouseX < rowX + rowWidth && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                return skills.get(i);
            }
        }
        return null;
    }

    private BellTowerClass classAt(double mouseX, double mouseY) {
        int left = this.panelLeft + 10;
        int y = this.panelTop + 26;
        int inner = PANEL_WIDTH - 20;
        int width = (inner - CLASS_GAP) / 2;
        BellTowerClass[] values = {BellTowerClass.HARBINGER, BellTowerClass.AGENT};
        for (int i = 0; i < values.length; i++) {
            int x = left + i * (width + CLASS_GAP);
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + CLASS_HEIGHT) {
                return values[i];
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null || !BellTowerSkills.unlocked(player)) {
            return true;
        }
        BellTowerClass towerClass = BellTowerClasses.get(player);
        if (BellTowerPermission.has(player) && !towerClass.isChosen()) {
            BellTowerClass hovered = classAt(mouseX, mouseY);
            if (hovered != null) {
                PacketDistributor.sendToServer(new BellTowerSkillPayload(
                        BellTowerSkillPayload.ACTION_SET_CLASS, hovered.id()));
                return true;
            }
        }
        BellTowerSkill skill = rowAt(mouseX, mouseY, BellTowerSkills.available(player));
        if (skill == null) {
            return true;
        }
        if (skill.isPassive()) {
            BellTowerSkillClient.localTogglePassive(skill.id());
            PacketDistributor.sendToServer(new BellTowerSkillPayload(
                    BellTowerSkillPayload.ACTION_TOGGLE, skill.id()));
        } else {
            BellTowerSkill current = BellTowerSkillClient.active();
            boolean chosen = current != null && current.id().equals(skill.id());
            String next = chosen ? "" : skill.id();
            BellTowerSkillClient.localSelect(next);
            PacketDistributor.sendToServer(new BellTowerSkillPayload(
                    BellTowerSkillPayload.ACTION_SELECT, next));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.SKILL_MENU.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
