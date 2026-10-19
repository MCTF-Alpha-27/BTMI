package com.btmi.item;

import java.util.List;

import com.btmi.menu.ColonMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ColonItem extends Item {
    public ColonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (windowId, inventory, p) -> new ColonMenu(windowId, inventory),
                    Component.translatable("container.btmi.colon")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.btmi.colon.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.colon.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.colon.tooltip3",
                Component.keybind("key.btmi.ammo_wheel")).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.colon.tooltip4").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.colon.tooltip5",
                Component.keybind("key.btmi.weapon_skill")).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("item.btmi.colon.lore")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
