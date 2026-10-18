package com.btmi.item;

import java.util.List;

import com.btmi.BTMI;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * 猩红之牙：黑底红纹的短管步枪。
 *
 * <p>左键开火，右键切换开火模式（连发 / 激光 / 霰弹），H 键放能量环。</p>
 */
public class CrimsonFangItem extends Item {
    public static final int MODE_BURST = 0;
    public static final int MODE_LASER = 1;
    public static final int MODE_SHOTGUN = 2;
    public static final int MODE_COUNT = 3;

    public CrimsonFangItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createWeaponAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static int mode(ItemStack stack) {
        Integer value = stack.get(BTMI.FIRE_MODE.get());
        return value == null ? MODE_BURST : Math.floorMod(value, MODE_COUNT);
    }

    public static void setMode(ItemStack stack, int mode) {
        stack.set(BTMI.FIRE_MODE.get(), Math.floorMod(mode, MODE_COUNT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        CrimsonFangHandler.cycleMode(player, stack);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.btmi.crimson_fang.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.btmi.crimson_fang.tooltip2",
                Component.keybind("key.btmi.weapon_skill")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.btmi.crimson_fang.tooltip3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.btmi.crimson_fang.mode",
                Component.translatable(CrimsonFangHandler.modeName(mode(stack)))).withStyle(ChatFormatting.DARK_RED));
    }
}
