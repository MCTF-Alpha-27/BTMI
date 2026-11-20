package com.btmi.item;

import java.util.List;

import com.btmi.BTMI;
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

public class FixedChairmanFileItem extends Item {
    public FixedChairmanFileItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        int count = 0;
        for (ItemStack item : player.getInventory().items) {
            if (item.is(this)) {
                count += item.getCount();
            }
        }
        if (player.getOffhandItem().is(this)) {
            count += player.getOffhandItem().getCount();
        }
        if (count > 0) {
            BellTowerQuests.onFileCount(player, count);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.btmi.chairman_file.desc").withStyle(ChatFormatting.RED));
    }
}
