package com.btmi.item;

import java.util.List;

import com.btmi.belltower.BellTowerClass;
import com.btmi.belltower.BellTowerClasses;
import com.btmi.belltower.skill.BellTowerSkillEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class MessengerBoxItem extends Item {
    private static final int COOLDOWN_TICKS = 600;

    public MessengerBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (BellTowerClasses.get(serverPlayer) != BellTowerClass.HARBINGER) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.btmi.messenger_box.class").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (serverPlayer.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.fail(stack);
            }
            serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            BellTowerSkillEffects.summonStrike(serverPlayer);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.btmi.messenger_box.desc").withStyle(ChatFormatting.RED));
    }
}
