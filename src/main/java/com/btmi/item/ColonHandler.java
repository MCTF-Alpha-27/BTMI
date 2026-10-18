package com.btmi.item;

import java.util.List;

import com.btmi.BTMI;
import com.btmi.entity.ColonBullet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class ColonHandler {
    private static final int FIRE_COOLDOWN_TICKS = 6;

    private ColonHandler() {
    }

    // 开火动作：借用史诗战斗的弩射击回弹动画（无伤害，纯动作），直接用静态访问器。

    // 数据包动画：idle 站姿 + 45% 弩后坐量；解析失败时退回内置弩射击动画。
    private static AnimationManager.AnimationAccessor<? extends StaticAnimation> shootAnimation;

    private static AnimationManager.AnimationAccessor<? extends StaticAnimation> shootAnimation() {
        if (shootAnimation == null) {
            AnimationManager.AnimationAccessor<? extends StaticAnimation> resolved = null;
            try {
                AnimationManager.AnimationAccessor<StaticAnimation> custom =
                        AnimationManager.byKey("btmi:biped/combat/colon_shot");
                if (custom != null && !AnimationManager.checkNull(custom)) {
                    resolved = custom;
                }
            } catch (RuntimeException exception) {
                BTMI.LOGGER.warn("无法加载冒号的开火动画 btmi:biped/combat/colon_shot", exception);
            }
            shootAnimation = resolved != null ? resolved : Animations.BIPED_CROSSBOW_SHOT;
        }
        return shootAnimation;
    }

    public static InteractionHand findHand(ServerPlayer player) {
        if (player.getMainHandItem().is(BTMI.COLON.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(BTMI.COLON.get())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static void fire(ServerPlayer player) {
        InteractionHand hand = findHand(player);
        if (hand == null) {
            return;
        }
        ItemStack gun = player.getItemInHand(hand);
        ServerLevel level = (ServerLevel) player.level();

        List<ItemStack> ammo = ColonAmmo.read(gun);
        int slot = ColonAmmo.selected(gun);
        if (ammo.get(slot).isEmpty()) {
            slot = -1;
            for (int i = 0; i < ColonAmmo.SLOTS; i++) {
                if (!ammo.get(i).isEmpty()) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.6F);
                player.getCooldowns().addCooldown(gun.getItem(), FIRE_COOLDOWN_TICKS);
                return;
            }
            ColonAmmo.setSelected(gun, slot);
        }

        BulletType type = BulletType.of(ammo.get(slot));
        if (type == null) {
            return;
        }
        ammo.get(slot).shrink(1);
        ColonAmmo.write(gun, ammo);

        Vec3 look = player.getLookAngle();
        Vec3 muzzle = player.getEyePosition().add(look.scale(0.6D));

        ColonBullet bullet = new ColonBullet(level, player, type);
        bullet.setPos(muzzle.x, muzzle.y, muzzle.z);
        bullet.shoot(look.x, look.y, look.z, 3.2F, 0.0F);
        level.addFreshEntity(bullet);

        // 开火动作 + 枪口反馈 + 镜头后坐
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            patch.playAnimationSynchronized(shootAnimation(), 0.0F);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2F, 1.3F);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.05D, 0.05D, 0.05D, 0.02D);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 3, 0.04D, 0.04D, 0.04D, 0.01D);

        player.getCooldowns().addCooldown(gun.getItem(), FIRE_COOLDOWN_TICKS);
        gun.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    public static void select(ServerPlayer player, int slot) {
        InteractionHand hand = findHand(player);
        if (hand == null) {
            return;
        }
        ItemStack gun = player.getItemInHand(hand);
        ColonAmmo.setSelected(gun, slot);
    }
}
