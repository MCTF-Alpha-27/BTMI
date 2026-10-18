package com.btmi.item;

import java.util.List;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.client.DashClientAnimations;
import com.btmi.entity.LightOrb;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import com.btmi.weapon.ModAnimations;

public class DashItem extends Item {
    public static final double ATTACK_DAMAGE = 6.0D;
    public static final double ATTACK_SPEED = -2.6D;
    public static final float ORB_SPEED = 1.8F;

    // 枪口位置：从眼睛沿视线方向前移到破折号末端，再稍微偏向右下（武器在右手）
    private static final double MUZZLE_FORWARD = 1.8D;
    private static final double MUZZLE_RIGHT = 0.22D;
    private static final double MUZZLE_DROP = 0.12D;

    public DashItem(Properties properties) {
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
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            // 本地立即播放开火姿势（客户端类只在客户端加载）
            DashClientAnimations.playFire(player);
        }

        if (level instanceof ServerLevel serverLevel) {
            // 从破折号末端射出：视线前方 1.8 格，再偏向右下与武器末端对齐；
            // 若前方有方块则把枪口收回，避免光球生成在墙里
            Vec3 look = player.getLookAngle();
            Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (right.lengthSqr() < 1.0E-6D) {
                right = new Vec3(1.0D, 0.0D, 0.0D);
            }
            right = right.normalize();

            double reach = MUZZLE_FORWARD;
            HitResult obstacle = player.pick(reach, 1.0F, false);
            if (obstacle.getType() != HitResult.Type.MISS) {
                reach = Math.max(0.3D, player.getEyePosition().distanceTo(obstacle.getLocation()) - 0.1D);
            }

            Vec3 muzzle = player.getEyePosition()
                    .add(look.scale(reach))
                    .add(right.scale(MUZZLE_RIGHT))
                    .add(0.0D, -MUZZLE_DROP, 0.0D);

            LightOrb orb = new LightOrb(serverLevel, player);
            orb.setPos(muzzle.x, muzzle.y, muzzle.z);
            orb.shoot(look.x, look.y, look.z, ORB_SPEED, 0.0F);
            serverLevel.addFreshEntity(orb);
            // 施法音效与枪口粒子（在末端而不是脸前）
            serverLevel.sendParticles(ParticleTypes.ENCHANT, muzzle.x, muzzle.y, muzzle.z,
                    12, 0.15D, 0.15D, 0.15D, 0.35D);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.5F);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCooldowns().addCooldown(this, Config.DASH_ORB_COOLDOWN_TICKS.get());
                // 双手前伸的瞄准姿势 + 后坐
                ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(serverPlayer);
                if (patch != null && patch.isVanillaMode()) {
                    // 史诗战斗只在 EPICFIGHT 模式下渲染自定义姿势
                    patch.toEpicFightMode(true);
                }
                BTMI.LOGGER.info("[dash] fire muzzle=({}, {}, {}) reach={} patch={} mode={}",
                        muzzle.x, muzzle.y, muzzle.z, reach,
                        patch != null, patch != null ? patch.getPlayerMode() : "null");
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer serverPlayer) {
            DashAbilityHandler.markUnsheathed(serverPlayer);
            DashProjectionSkill.addCharge(serverPlayer);
        }
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.btmi.dash.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.dash.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.dash.tooltip3").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.btmi.dash.tooltip4",
                Component.keybind("key.btmi.weapon_skill")).withStyle(ChatFormatting.GOLD));
    }
}
