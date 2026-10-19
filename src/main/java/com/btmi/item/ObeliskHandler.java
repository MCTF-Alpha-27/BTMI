package com.btmi.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.entity.LaserBolt;
import com.btmi.entity.ObeliskSentinel;
import com.btmi.network.ObeliskActionPayload;
import com.btmi.network.ShakePayload;

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

@EventBusSubscriber(modid = BTMI.MODID)
public final class ObeliskHandler {
    private static final double SEARCH_RADIUS = 160.0D;

    private static final int KNELL_WINDUP = 16;
    private static final int KNELL_MARK_TICKS = 160;
    private static final double KNELL_RANGE = 16.0D;

    private static final Map<UUID, Integer> LIT_TICKS = new HashMap<>();
    private static final Map<UUID, Knell> KNELLS = new HashMap<>();
    private static final Map<UUID, Long> JUDGED = new HashMap<>();

    private record Knell(Vec3 target, int age) {
    }

    private ObeliskHandler() {
    }

    public static InteractionHand handOf(int hand) {
        return hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    // 一次性处理双手。
    public static void toggleBoth(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(BTMI.OBELISK.get())) {
                deploy(player, hand);
            } else if (stack.is(BTMI.OBELISK_RECALL.get())) {
                recall(player, hand);
            }
        }
    }

    // ------------------------------------------------------------------
    // 召唤 / 召回 / 摧毁
    // ------------------------------------------------------------------

    public static void deploy(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(BTMI.OBELISK.get())) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (find(level, player, hand) != null) {
            return;
        }

        ObeliskSentinel sentinel = new ObeliskSentinel(level, player, hand.ordinal());
        level.addFreshEntity(sentinel);
        player.setItemInHand(hand, new ItemStack(BTMI.OBELISK_RECALL.get()));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.sendParticles(ParticleTypes.ENCHANT, sentinel.getX(), sentinel.getY(), sentinel.getZ(),
                30, 0.5D, 0.5D, 0.5D, 0.4D);
    }

    public static void recall(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(BTMI.OBELISK_RECALL.get())) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        ObeliskSentinel sentinel = find(level, player, hand);
        if (sentinel != null) {
            level.sendParticles(ParticleTypes.ENCHANT, sentinel.getX(), sentinel.getY(), sentinel.getZ(),
                    30, 0.5D, 0.5D, 0.5D, 0.4D);
            sentinel.discard();
        }
        player.setItemInHand(hand, new ItemStack(BTMI.OBELISK.get()));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    public static void destroyRecallItem(ServerPlayer player, int hand) {
        InteractionHand interactionHand = handOf(hand);
        ItemStack stack = player.getItemInHand(interactionHand);
        if (stack.is(BTMI.OBELISK_RECALL.get())) {
            player.setItemInHand(interactionHand, ItemStack.EMPTY);
        }
    }

    public static ObeliskSentinel find(ServerLevel level, Player owner, InteractionHand hand) {
        AABB area = owner.getBoundingBox().inflate(SEARCH_RADIUS);
        for (ObeliskSentinel sentinel : level.getEntitiesOfClass(ObeliskSentinel.class, area)) {
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
            if (stack.is(BTMI.OBELISK.get()) && !Boolean.TRUE.equals(stack.get(BTMI.LIT.get()))) {
                stack.set(BTMI.LIT.get(), Boolean.TRUE);
                changed = true;
            }
        }
        if (changed || !LIT_TICKS.containsKey(player.getUUID())) {
            LIT_TICKS.put(player.getUUID(), Math.max(1, Config.OBELISK_SABER_TICKS.get()));
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
        if (stack.is(BTMI.OBELISK.get()) && Boolean.TRUE.equals(stack.get(BTMI.LIT.get()))) {
            stack.set(BTMI.LIT.get(), Boolean.FALSE);
        }
    }

    // ------------------------------------------------------------------
    // 激光
    // ------------------------------------------------------------------

    public static void fireLasers(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        for (InteractionHand hand : InteractionHand.values()) {
            if (!player.getItemInHand(hand).is(BTMI.OBELISK_RECALL.get())) {
                continue;
            }
            ObeliskSentinel sentinel = find(level, player, hand);
            if (sentinel == null) {
                continue;
            }
            LaserBolt bolt = new LaserBolt(level, player);
            // 从十字架前端稍上方射出，避免激光看起来贴在自己身上
            Vec3 muzzle = new Vec3(sentinel.getX(), sentinel.getY(), sentinel.getZ())
                    .add(look.scale(0.7D))
                    .add(0.0D, 0.4D, 0.0D);
            bolt.setPos(muzzle.x, muzzle.y, muzzle.z);
            bolt.shoot(look.x, look.y, look.z, 2.6F, 0.0F);
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
        boolean holding = main.is(BTMI.OBELISK.get()) || main.is(BTMI.OBELISK_RECALL.get())
                || off.is(BTMI.OBELISK.get()) || off.is(BTMI.OBELISK_RECALL.get());
        if (!holding) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(BTMI.OBELISK.get())
                || player.getCooldowns().isOnCooldown(BTMI.OBELISK_RECALL.get())
                || KNELLS.containsKey(player.getUUID())) {
            return;
        }

        int cooldown = Config.OBELISK_KNELL_COOLDOWN.get();
        player.getCooldowns().addCooldown(BTMI.OBELISK.get(), cooldown);
        player.getCooldowns().addCooldown(BTMI.OBELISK_RECALL.get(), cooldown);

        ServerLevel level = (ServerLevel) player.level();
        KNELLS.put(player.getUUID(), new Knell(knellTarget(player, level), 0));
        markLit(player);

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

        Vec3 target = knell.target();
        float progress = knell.age() / (float) KNELL_WINDUP;
        double angle = knell.age() * 0.42D;

        // 脚下的双重法阵，内外圈反向旋转
        for (int ring = 0; ring < 2; ring++) {
            double ringRadius = 1.15D + ring * 0.6D;
            int count = 14 + ring * 8;
            double spin = ring == 0 ? angle : -angle * 0.8D;
            for (int i = 0; i < count; i++) {
                double a = spin + i * (Math.PI * 2.0D / count);
                level.sendParticles(ring == 0 ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT,
                        player.getX() + Math.cos(a) * ringRadius,
                        player.getY() + 0.1D,
                        player.getZ() + Math.sin(a) * ringRadius,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        // 螺旋上升的光点
        for (int i = 0; i < 12; i++) {
            double a = angle * 1.8D + i * 0.62D;
            double radius = 1.5D - i * 0.08D;
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX() + Math.cos(a) * radius,
                    player.getY() + 0.25D + i * 0.18D,
                    player.getZ() + Math.sin(a) * radius,
                    1, 0.0D, 0.02D, 0.0D, 0.0D);
        }

        // 落点上空逐渐成形的光之十字
        double height = target.y + 13.0D - progress * 2.5D;
        int half = 2 + Mth.floor(progress * 3.0F);
        for (int i = -half; i <= half; i++) {
            double offset = i * 0.55D;
            level.sendParticles(ParticleTypes.END_ROD, target.x + offset, height, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, target.x, height + offset, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.END_ROD, target.x, height, target.z, 3, 0.15D, 0.15D, 0.15D, 0.03D);
        level.sendParticles(ParticleTypes.FIREWORK, target.x, height, target.z, 1, 0.2D, 0.2D, 0.2D, 0.02D);

        // 从上往下扫过的引导光
        double scanY = target.y + 12.0D - progress * 10.0D;
        for (int i = -2; i <= 2; i++) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.x + i * 0.5D, scanY, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        // 落点地面的预兆法阵
        for (int i = 0; i < 16; i++) {
            double a = i * (Math.PI * 2.0D / 16.0D) + angle * 0.3D;
            level.sendParticles(ParticleTypes.ENCHANT,
                    target.x + Math.cos(a) * 1.6D, target.y + 0.15D, target.z + Math.sin(a) * 1.6D,
                    1, 0.0D, 0.03D, 0.0D, 0.0D);
        }

        // 逐渐加速的钟声
        if (knell.age() == 4 || knell.age() == 8 || knell.age() == 12) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BELL_RESONATE, SoundSource.PLAYERS,
                    0.8F + knell.age() * 0.05F, 0.95F - knell.age() * 0.03F);
        }
    }

    private static void strikeKnell(ServerLevel level, ServerPlayer player, Vec3 target) {
        double radius = Config.OBELISK_KNELL_RADIUS.get();
        double armLength = Config.OBELISK_KNELL_ARM_LENGTH.get();

        // 贯穿天地的光柱
        for (double y = 0.0D; y <= 20.0D; y += 0.3D) {
            level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + y, target.z,
                    1, 0.06D, 0.0D, 0.06D, 0.0D);
            if (y % 1.5D < 0.31D) {
                level.sendParticles(ParticleTypes.FIREWORK, target.x, target.y + y, target.z,
                        1, 0.18D, 0.18D, 0.18D, 0.02D);
            }
        }

        // 四条双线光臂，臂端各立起一根小光柱
        for (int direction = 0; direction < 4; direction++) {
            double dx = direction == 0 ? 1.0D : direction == 1 ? -1.0D : 0.0D;
            double dz = direction == 2 ? 1.0D : direction == 3 ? -1.0D : 0.0D;
            double px = -dz;
            double pz = dx;
            for (double d = 0.4D; d <= radius + armLength; d += 0.45D) {
                for (int side = -1; side <= 1; side += 2) {
                    double x = target.x + dx * d + px * side * 0.45D;
                    double z = target.z + dz * d + pz * side * 0.45D;
                    level.sendParticles(ParticleTypes.END_ROD, x, target.y + 0.12D, z,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                if (Math.round(d * 10.0D) % 4 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            target.x + dx * d, target.y + 0.5D, target.z + dz * d,
                            1, 0.04D, 0.12D, 0.04D, 0.02D);
                }
            }

            double tipX = target.x + dx * (radius + armLength);
            double tipZ = target.z + dz * (radius + armLength);
            for (double y = 0.4D; y <= 5.0D; y += 0.35D) {
                level.sendParticles(ParticleTypes.END_ROD, tipX, target.y + y, tipZ,
                        1, 0.03D, 0.0D, 0.03D, 0.0D);
            }
            level.sendParticles(ParticleTypes.FLASH, tipX, target.y + 1.0D, tipZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        // 三层扩散冲击环
        for (int ring = 0; ring < 3; ring++) {
            double ringRadius = radius * 0.5D + ring * 2.2D;
            int count = 28 + ring * 12;
            for (int i = 0; i < count; i++) {
                double a = i * (Math.PI * 2.0D / count);
                double x = target.x + Math.cos(a) * ringRadius;
                double z = target.z + Math.sin(a) * ringRadius;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, target.y + 0.15D, z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (ring == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, x, target.y + 0.3D, z,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        // 圣光爆发
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.x, target.y + 1.2D, target.z,
                60, 1.2D, 1.2D, 1.2D, 0.35D);
        level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 1.0D, target.z,
                40, 1.5D, 1.5D, 1.5D, 0.25D);
        level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 0.5D, target.z, 2, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 3.0D, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

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
                    ? Config.OBELISK_KNELL_DAMAGE.get()
                    : Config.OBELISK_KNELL_ARM_DAMAGE.get()).floatValue();
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

    public static boolean holdsObeliskLaser(Player player) {
        return player.getMainHandItem().is(BTMI.OBELISK_RECALL.get())
                || player.getOffhandItem().is(BTMI.OBELISK_RECALL.get());
    }

    // ------------------------------------------------------------------
    // 客户端上报的动作
    // ------------------------------------------------------------------

    public static void handleAction(ServerPlayer player, int hand, int action) {
        ServerLevel level = (ServerLevel) player.level();

        if (action == ObeliskActionPayload.ACTION_KNELL) {
            castKnell(player);
            return;
        }

        if (action == ObeliskActionPayload.ACTION_ATTACK) {
            markLit(player);
        }

        for (InteractionHand interactionHand : InteractionHand.values()) {
            if (!player.getItemInHand(interactionHand).is(BTMI.OBELISK_RECALL.get())) {
                continue;
            }
            ObeliskSentinel sentinel = find(level, player, interactionHand);
            if (sentinel == null) {
                continue;
            }
            if (action == ObeliskActionPayload.ACTION_ATTACK) {
                sentinel.startRam(player.getLookAngle());
            } else if (action == ObeliskActionPayload.ACTION_DETONATE) {
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
