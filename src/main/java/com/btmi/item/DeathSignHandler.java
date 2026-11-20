package com.btmi.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.entity.LaserBolt;
import com.btmi.entity.DeathSignSentinel;
import com.btmi.entity.KnellCharge;
import com.btmi.entity.KnellStrike;
import com.btmi.network.DeathSignActionPayload;
import com.btmi.network.ShakePayload;
import com.btmi.weapon.ModAnimations;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@EventBusSubscriber(modid = BTMI.MODID)
public final class DeathSignHandler {
    private static final double SEARCH_RADIUS = 160.0D;

    private static final int KNELL_WINDUP = 16;
    private static final int KNELL_MARK_TICKS = 160;
    private static final double KNELL_RANGE = 16.0D;

    private static final Map<UUID, Integer> LIT_TICKS = new HashMap<>();
    private static final Map<UUID, Knell> KNELLS = new HashMap<>();
    private static final Map<UUID, Long> JUDGED = new HashMap<>();

    private record Knell(Vec3 target, int age) {
    }

    private DeathSignHandler() {
    }

    public static InteractionHand handOf(int hand) {
        return hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    // 一次性处理双手。
    public static void toggleBoth(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(BTMI.DEATH_SIGN.get())) {
                deploy(player, hand);
            } else if (stack.is(BTMI.DEATH_SIGN_RECALL.get())) {
                recall(player, hand);
            }
        }
    }

    // ------------------------------------------------------------------
    // 召唤 / 召回 / 摧毁
    // ------------------------------------------------------------------

    public static void deploy(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(BTMI.DEATH_SIGN.get())) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (find(level, player, hand) != null) {
            return;
        }

        DeathSignSentinel sentinel = new DeathSignSentinel(level, player, hand.ordinal());
        level.addFreshEntity(sentinel);
        player.setItemInHand(hand, new ItemStack(BTMI.DEATH_SIGN_RECALL.get()));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.sendParticles(ParticleTypes.ENCHANT, sentinel.getX(), sentinel.getY(), sentinel.getZ(),
                30, 0.5D, 0.5D, 0.5D, 0.4D);
    }

    public static void recall(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(BTMI.DEATH_SIGN_RECALL.get())) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        DeathSignSentinel sentinel = find(level, player, hand);
        if (sentinel != null) {
            level.sendParticles(ParticleTypes.ENCHANT, sentinel.getX(), sentinel.getY(), sentinel.getZ(),
                    30, 0.5D, 0.5D, 0.5D, 0.4D);
            sentinel.discard();
        }
        player.setItemInHand(hand, new ItemStack(BTMI.DEATH_SIGN.get()));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    public static void destroyRecallItem(ServerPlayer player, int hand) {
        InteractionHand interactionHand = handOf(hand);
        ItemStack stack = player.getItemInHand(interactionHand);
        if (stack.is(BTMI.DEATH_SIGN_RECALL.get())) {
            player.setItemInHand(interactionHand, ItemStack.EMPTY);
        }
    }

    public static DeathSignSentinel find(ServerLevel level, Player owner, InteractionHand hand) {
        AABB area = owner.getBoundingBox().inflate(SEARCH_RADIUS);
        for (DeathSignSentinel sentinel : level.getEntitiesOfClass(DeathSignSentinel.class, area)) {
            if (sentinel.isOwnedBy(owner) && sentinel.getOwnerHand() == hand.ordinal()) {
                return sentinel;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 光剑
    // ------------------------------------------------------------------

    public static void markLit(ServerPlayer player) {
        boolean changed = false;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(BTMI.DEATH_SIGN.get()) && !Boolean.TRUE.equals(stack.get(BTMI.LIT.get()))) {
                stack.set(BTMI.LIT.get(), Boolean.TRUE);
                changed = true;
            }
        }
        if (changed || !LIT_TICKS.containsKey(player.getUUID())) {
            LIT_TICKS.put(player.getUUID(), Math.max(1, Config.DEATH_SIGN_SABER_TICKS.get()));
        }
    }

    private static void clearLit(ServerPlayer player) {
        clearLit(player.getMainHandItem());
        clearLit(player.getOffhandItem());
        for (ItemStack stack : player.getInventory().items) {
            clearLit(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            clearLit(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            clearLit(stack);
        }
    }

    private static void clearLit(ItemStack stack) {
        if (stack.is(BTMI.DEATH_SIGN.get()) && Boolean.TRUE.equals(stack.get(BTMI.LIT.get()))) {
            stack.set(BTMI.LIT.get(), Boolean.FALSE);
        }
    }

    // ------------------------------------------------------------------
    // 激光
    // ------------------------------------------------------------------

    public static void fireLasers(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(SEARCH_RADIUS);
        for (DeathSignSentinel sentinel : level.getEntitiesOfClass(DeathSignSentinel.class, area)) {
            if (!sentinel.isOwnedBy(player)) {
                continue;
            }
            if (!player.getInventory().getItem(sentinel.getOwnerSlot()).is(BTMI.DEATH_SIGN_RECALL.get())) {
                continue;
            }
            LaserBolt bolt = new LaserBolt(level, player);
            Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 tip = look.normalize();
            Vec3 horizontal = new Vec3(tip.x, 0.0D, tip.z);
            if (horizontal.lengthSqr() < 1.0E-6D) {
                double yawRad = Math.toRadians(player.getYRot());
                horizontal = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
            } else {
                horizontal = horizontal.normalize();
            }
            Vec3 normal = horizontal.cross(up);
            Vec3 crossbar = normal.cross(tip);
            Vec3 muzzle = sentinel.position()
                    .add(0.0D, sentinel.getBbHeight() * 0.5D, 0.0D)
                    .add(tip.scale(2.05D))
                    .subtract(crossbar.scale(0.65D));
            if (sentinel.getOwnerHand() == 1) {
                muzzle = muzzle.add(normal.scale(-1.10D))
                        .add(tip.scale(0.2D))
                        .subtract(crossbar.scale(0.10D));
            }
            bolt.setPos(muzzle.x, muzzle.y, muzzle.z);
            bolt.shoot(tip.x, tip.y, tip.z, 2.6F, 0.0F);
            level.addFreshEntity(bolt);

            level.playSound(null, sentinel.getX(), sentinel.getY(), sentinel.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.9F);
        }
    }

    // ------------------------------------------------------------------
    // 丧钟
    // ------------------------------------------------------------------

    public static void castKnell(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean holding = main.is(BTMI.DEATH_SIGN.get()) || main.is(BTMI.DEATH_SIGN_RECALL.get())
                || off.is(BTMI.DEATH_SIGN.get()) || off.is(BTMI.DEATH_SIGN_RECALL.get());
        if (!holding) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(BTMI.DEATH_SIGN.get())
                || player.getCooldowns().isOnCooldown(BTMI.DEATH_SIGN_RECALL.get())
                || KNELLS.containsKey(player.getUUID())) {
            return;
        }

        int cooldown = Config.DEATH_SIGN_KNELL_COOLDOWN.get();
        player.getCooldowns().addCooldown(BTMI.DEATH_SIGN.get(), cooldown);
        player.getCooldowns().addCooldown(BTMI.DEATH_SIGN_RECALL.get(), cooldown);

        ServerLevel level = (ServerLevel) player.level();
        Vec3 target = knellTarget(player, level);
        KNELLS.put(player.getUUID(), new Knell(target, 0));
        level.addFreshEntity(new KnellCharge(level, target));
        markLit(player);

        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            patch.playAnimationSynchronized(ModAnimations.DEATH_KNELL, 0.0F);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.4F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static Vec3 knellTarget(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(KNELL_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        int x = Mth.floor(end.x);
        int z = Mth.floor(end.z);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(end.x, y, end.z);
    }

    private static void tickKnell(ServerLevel level, ServerPlayer player, Knell knell) {
        if (knell.age() >= KNELL_WINDUP) {
            KNELLS.remove(player.getUUID());
            strikeKnell(level, player, knell.target());
            return;
        }
        KNELLS.put(player.getUUID(), new Knell(knell.target(), knell.age() + 1));

        // 逐渐加速的钟声
        if (knell.age() == 4 || knell.age() == 8 || knell.age() == 12) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BELL_RESONATE, SoundSource.PLAYERS,
                    0.8F + knell.age() * 0.05F, 0.95F - knell.age() * 0.03F);
        }
    }

    private static void strikeKnell(ServerLevel level, ServerPlayer player, Vec3 target) {
        double radius = Config.DEATH_SIGN_KNELL_RADIUS.get();
        double armLength = Config.DEATH_SIGN_KNELL_ARM_LENGTH.get();

        level.addFreshEntity(new KnellStrike(level, target));
        level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 1.2D, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 1.2D, target.z,
                8, 0.6D, 1.0D, 0.6D, 0.06D);

        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.6F, 1.2F);
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.5F);
        level.playSound(null, target.x, target.y, target.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 1.4F);

        if (Config.SCREEN_SHAKE.get()) {
            float strength = (float) (Config.SHAKE_STRENGTH.get() * 0.9D);
            PacketDistributor.sendToPlayersNear(level, null, target.x, target.y, target.z, 32.0D,
                    new ShakePayload(target.x, target.y, target.z, strength, 14));
        }

        double reach = radius + armLength;
        AABB area = new AABB(target.x - reach, target.y - 4.0D, target.z - reach,
                target.x + reach, target.y + 6.0D, target.z + reach);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (entity == player) {
                continue;
            }
            Vec3 delta = entity.position().subtract(target);
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            boolean center = horizontal <= radius && Math.abs(delta.y) <= 4.0D;
            boolean arm = (Math.abs(delta.x) <= 1.2D || Math.abs(delta.z) <= 1.2D)
                    && horizontal <= reach && Math.abs(delta.y) <= 3.0D;
            if (!center && !arm) {
                continue;
            }

            float damage = (center
                    ? Config.DEATH_SIGN_KNELL_DAMAGE.get()
                    : Config.DEATH_SIGN_KNELL_ARM_DAMAGE.get()).floatValue();
            entity.invulnerableTime = 0;
            entity.hurt(level.damageSources().indirectMagic(player, player), damage);
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, KNELL_MARK_TICKS, 0, false, false));
            JUDGED.put(entity.getUUID(), level.getGameTime() + KNELL_MARK_TICKS);

            Vec3 push = horizontal > 1.0E-4D ? new Vec3(delta.x, 0.0D, delta.z).normalize() : Vec3.ZERO;
            entity.push(push.x * 0.7D, center ? 0.55D : 0.35D, push.z * 0.7D);
            entity.hurtMarked = true;
        }
    }

    public static boolean isJudged(LivingEntity entity) {
        Long until = JUDGED.get(entity.getUUID());
        if (until == null) {
            return false;
        }
        if (entity.level().getGameTime() > until) {
            JUDGED.remove(entity.getUUID());
            return false;
        }
        return true;
    }

    public static boolean holdsDeathSignLaser(Player player) {
        return player.getMainHandItem().is(BTMI.DEATH_SIGN_RECALL.get())
                || player.getOffhandItem().is(BTMI.DEATH_SIGN_RECALL.get());
    }

    // ------------------------------------------------------------------
    // 客户端上报的动作
    // ------------------------------------------------------------------

    public static void handleAction(ServerPlayer player, int hand, int action) {
        ServerLevel level = (ServerLevel) player.level();

        if (action == DeathSignActionPayload.ACTION_KNELL) {
            castKnell(player);
            return;
        }

        if (action == DeathSignActionPayload.ACTION_ATTACK) {
            markLit(player);
        }

        for (InteractionHand interactionHand : InteractionHand.values()) {
            if (!player.getItemInHand(interactionHand).is(BTMI.DEATH_SIGN_RECALL.get())) {
                continue;
            }
            DeathSignSentinel sentinel = find(level, player, interactionHand);
            if (sentinel == null) {
                continue;
            }
            if (action == DeathSignActionPayload.ACTION_ATTACK) {
                sentinel.startRam(player.getLookAngle());
            } else if (action == DeathSignActionPayload.ACTION_DETONATE) {
                sentinel.startDetonate(player.getLookAngle());
                // 发射即消耗：即使之后什么都没撞到，武器也应立刻消失
                destroyRecallItem(player, interactionHand.ordinal());
            }
        }
    }

    // ------------------------------------------------------------------
    // 每刻维护
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID id = player.getUUID();

        Knell knell = KNELLS.get(id);
        if (knell != null) {
            tickKnell((ServerLevel) player.level(), player, knell);
        }

        Integer left = LIT_TICKS.get(id);
        if (left == null) {
            return;
        }
        if (left <= 1) {
            LIT_TICKS.remove(id);
            clearLit(player);
        } else {
            LIT_TICKS.put(id, left - 1);
        }
    }
}
