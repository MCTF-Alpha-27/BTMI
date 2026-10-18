package com.btmi.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// 六种子弹共用的物品实现，悬停提示由所属弹药类型决定。
public class BulletItem extends Item {
    public BulletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        BulletType type = BulletType.of(stack);
        if (type != null) {
            tooltipComponents.add(Component.translatable(type.translationKey() + ".tooltip").withStyle(ChatFormatting.GRAY));
        }
    }
}
