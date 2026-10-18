package com.btmi.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.entity.LaserBolt;
import com.btmi.entity.ObeliskSentinel;
import com.btmi.network.ObeliskActionPayload;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class ObeliskHandler {
    private static final double SEARCH_RADIUS = 160.0D;

    private static final Map<UUID, Integer> LIT_TICKS = new HashMap<>();

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
    // 客户端上报的动作
    // ------------------------------------------------------------------

    public static void handleAction(ServerPlayer player, int hand, int action) {
        ServerLevel level = (ServerLevel) player.level();

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
