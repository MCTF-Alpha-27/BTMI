package com.btmi.item;

import java.util.List;

import com.btmi.belltower.BellTowerQuests;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ChairmanFileItem extends Item {
    private final String fileId;

    public ChairmanFileItem(String fileId, Properties properties) {
        super(properties);
        this.fileId = fileId;
    }

    public String fileId() {
        return this.fileId;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            BellTowerQuests.onDocumentFound(player, this.fileId);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.btmi.chairman_file.desc").withStyle(ChatFormatting.RED));
    }
}
