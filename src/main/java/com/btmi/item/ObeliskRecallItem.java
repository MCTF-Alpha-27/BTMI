package com.btmi.item;

import java.util.List;

import com.btmi.Config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class ObeliskRecallItem extends Item {
    public ObeliskRecallItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 潜行右键：召回悬浮实体（双手一并处理）
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                ObeliskHandler.toggleBoth(serverPlayer);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
        }

        // 普通右键：进入持续射击状态
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int used = getUseDuration(stack, livingEntity) - remainingUseDuration;
        int interval = Math.max(1, Config.OBELISK_LASER_INTERVAL.get());
        if (used > 0 && used % interval == 0) {
            ObeliskHandler.fireLasers(serverLevel, player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.btmi.obelisk_recall.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.obelisk_recall.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.obelisk_recall.tooltip3",
                Component.keybind("key.btmi.obelisk_detonate")).withStyle(ChatFormatting.GOLD));
    }
}
