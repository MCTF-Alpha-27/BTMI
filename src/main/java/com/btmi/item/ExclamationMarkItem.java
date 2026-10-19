package com.btmi.item;

import java.util.List;

import com.btmi.Config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public class ExclamationMarkItem extends Item {
    public static final double ATTACK_DAMAGE = 7.0D;
    public static final double ATTACK_SPEED = -2.4D;
    public static final int MAX_USE_DURATION = 72000;

    public ExclamationMarkItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createWeaponAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int charge = getUseDuration(stack, livingEntity) - remainingUseDuration;
        int maxCharge = Math.max(1, Config.MAX_CHARGE_TICKS.get());
        float ratio = Mth.clamp(charge / (float) maxCharge, 0.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel && charge % 2 == 0) {
            // 蓄力时武器周围汇聚能量，越接近满蓄力粒子越密集
            Vec3 look = player.getLookAngle();
            Vec3 focus = player.getEyePosition().add(look.scale(0.9D)).add(0.0D, -0.35D, 0.0D);
            int count = 1 + Mth.floor(ratio * 6.0F);
            double spread = 0.35D + ratio * 0.35D;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    focus.x, focus.y, focus.z, count, spread, spread, spread, 0.0D);
            if (ratio >= 1.0F && charge % 6 == 0) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.6F);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int charge = getUseDuration(stack, livingEntity) - timeLeft;
        if (charge < Config.MIN_CHARGE_TICKS.get()) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            SmashHandler.perform(serverLevel, player, stack, charge);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.btmi.exclamation_mark.tooltip").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.exclamation_mark.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.exclamation_mark.tooltip3",
                Component.keybind("key.btmi.weapon_skill")).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("item.btmi.exclamation_mark.lore")
                .withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }
}
