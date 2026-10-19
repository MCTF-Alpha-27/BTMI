package com.btmi.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.entity.ColonBullet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@EventBusSubscriber(modid = BTMI.MODID)
public final class ColonHandler {
    private static final int FIRE_COOLDOWN_TICKS = 6;
    private static final double BULLET_SPEED = 3.2D;

    private static final Map<UUID, Fan> FANS = new HashMap<>();

    private static final class Fan {
        private final InteractionHand hand;
        private final List<Integer> slots;
        private int index;
        private int delay;

        private Fan(InteractionHand hand, List<Integer> slots) {
            this.hand = hand;
            this.slots = slots;
        }
    }

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

    private static AnimationManager.AnimationAccessor<? extends StaticAnimation> fanAnimation;

    private static AnimationManager.AnimationAccessor<? extends StaticAnimation> fanAnimation() {
        if (fanAnimation == null) {
            AnimationManager.AnimationAccessor<? extends StaticAnimation> resolved = null;
            try {
                AnimationManager.AnimationAccessor<StaticAnimation> custom =
                        AnimationManager.byKey("btmi:biped/combat/colon_fan");
                if (custom != null && !AnimationManager.checkNull(custom)) {
                    resolved = custom;
                }
            } catch (RuntimeException exception) {
                BTMI.LOGGER.warn("无法加载冒号的齐射动画 btmi:biped/combat/colon_fan", exception);
            }
            fanAnimation = resolved != null ? resolved : shootAnimation();
        }
        return fanAnimation;
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
        if (hand == null || FANS.containsKey(player.getUUID())) {
            return;
        }
        ItemStack gun = player.getItemInHand(hand);
        ServerLevel level = (ServerLevel) player.level();

        List<ItemStack> ammo = ColonAmmo.read(gun);
        int slot = ColonAmmo.selected(gun);
        if (ammo.get(slot).isEmpty()) {
            slot = firstLoaded(ammo);
            if (slot < 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.6F);
                player.getCooldowns().addCooldown(gun.getItem(), FIRE_COOLDOWN_TICKS);
                return;
            }
            ColonAmmo.setSelected(gun, slot);
        }

        if (shoot(player, gun, slot, 0.0D, true)) {
            player.getCooldowns().addCooldown(gun.getItem(), FIRE_COOLDOWN_TICKS);
            gun.hurtAndBreak(1, player,
                    hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
    }

    public static void fan(ServerPlayer player) {
        InteractionHand hand = findHand(player);
        if (hand == null) {
            return;
        }
        ItemStack gun = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(gun.getItem()) || FANS.containsKey(player.getUUID())) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        List<ItemStack> ammo = ColonAmmo.read(gun);
        List<Integer> loaded = new ArrayList<>();
        for (int i = 0; i < ColonAmmo.SLOTS; i++) {
            if (!ammo.get(i).isEmpty()) {
                loaded.add(i);
            }
        }

        if (loaded.isEmpty()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.getCooldowns().addCooldown(gun.getItem(), 10);
            return;
        }

        // 从当前选中的弹巢开始依次打空
        int selected = ColonAmmo.selected(gun);
        loaded.sort(Comparator.comparingInt(slot -> Math.floorMod(slot - selected, ColonAmmo.SLOTS)));

        player.getCooldowns().addCooldown(gun.getItem(), Config.COLON_FAN_COOLDOWN.get());
        FANS.put(player.getUUID(), new Fan(hand, loaded));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 1.1F, 1.35F);
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.25D, player.getZ(),
                12, 0.25D, 0.2D, 0.25D, 0.02D);

        // 整段齐射只播放一次抬枪动作，避免每发都从垂手姿势重新起手
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            patch.playAnimationSynchronized(fanAnimation(), 0.08F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Fan fan = FANS.get(player.getUUID());
        if (fan == null) {
            return;
        }
        if (fan.delay > 0) {
            fan.delay--;
            return;
        }

        ItemStack gun = player.getItemInHand(fan.hand);
        if (!gun.is(BTMI.COLON.get())) {
            FANS.remove(player.getUUID());
            return;
        }
        if (fan.index >= fan.slots.size()) {
            finishFan(player);
            return;
        }

        int shot = fan.index;
        int slot = fan.slots.get(fan.index++);
        double spread = Config.COLON_FAN_SPREAD.get() * (0.35D + shot * 0.8D);
        shoot(player, gun, slot, spread, false);

        fan.delay = Math.max(0, Config.COLON_FAN_INTERVAL.get() - 1);
        if (fan.index >= fan.slots.size()) {
            finishFan(player);
        }
    }

    private static void finishFan(ServerPlayer player) {
        FANS.remove(player.getUUID());
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0F, 1.2F);

        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            patch.playAnimationSynchronized(Animations.BIPED_CROSSBOW_RELOAD, 0.1F);
        }
    }

    private static boolean shoot(ServerPlayer player, ItemStack gun, int slot, double spread, boolean animate) {
        List<ItemStack> ammo = ColonAmmo.read(gun);
        if (slot < 0 || slot >= ColonAmmo.SLOTS || ammo.get(slot).isEmpty()) {
            return false;
        }
        BulletType type = BulletType.of(ammo.get(slot));
        if (type == null) {
            return false;
        }
        ammo.get(slot).shrink(1);
        ColonAmmo.write(gun, ammo);

        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = spreadDirection(player.getLookAngle(), spread, player.getRandom());
        Vec3 muzzle = player.getEyePosition().add(look.scale(0.6D));

        ColonBullet bullet = new ColonBullet(level, player, type);
        bullet.setPos(muzzle.x, muzzle.y, muzzle.z);
        bullet.shoot(look.x, look.y, look.z, (float) BULLET_SPEED, 0.0F);
        level.addFreshEntity(bullet);

        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (animate && patch != null) {
            patch.playAnimationSynchronized(shootAnimation(), 0.0F);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2F, 1.3F);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.05D, 0.05D, 0.05D, 0.02D);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 3, 0.04D, 0.04D, 0.04D, 0.01D);
        return true;
    }

    private static int firstLoaded(List<ItemStack> ammo) {
        for (int i = 0; i < ColonAmmo.SLOTS; i++) {
            if (!ammo.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static Vec3 spreadDirection(Vec3 look, double spread, RandomSource random) {
        if (spread <= 1.0E-6D) {
            return look;
        }
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        double yaw = (random.nextDouble() - 0.5D) * spread * 2.0D;
        double pitch = (random.nextDouble() - 0.5D) * spread * 2.0D;
        return look.add(right.scale(yaw)).add(up.scale(pitch)).normalize();
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
