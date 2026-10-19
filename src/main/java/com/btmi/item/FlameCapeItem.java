package com.btmi.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class FlameCapeItem extends ArmorItem {
    public FlameCapeItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.btmi.flame_cape.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.btmi.flame_cape.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.btmi.flame_cape.tooltip3",
                Component.keybind("key.btmi.weapon_skill"),
                Component.keybind("key.btmi.butterfly_strike")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.btmi.flame_cape.lore")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
