package com.btmi.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.network.DashActionPayload;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class DashAbilityHandler {
    private static final int UNSHEATH_DURATION = 45;
    private static final int SLASH_DELAY = 5;

    private static final Map<UUID, Integer> UNSHEATH = new HashMap<>();
    private static final Map<UUID, PendingSlash> PENDING = new HashMap<>();

    private DashAbilityHandler() {
    }

    // ------------------------------------------------------------------
    // 刀刃 / 刀鞘 状态
    // ------------------------------------------------------------------

    public static void markUnsheathed(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(BTMI.DASH.get())) {
            return;
        }
        if (!Boolean.TRUE.equals(stack.get(BTMI.UNSHEATHED.get()))) {
            stack.set(BTMI.UNSHEATHED.get(), Boolean.TRUE);
        }
        UNSHEATH.put(player.getUUID(), UNSHEATH_DURATION);
    }

    private static void clearUnsheathed(ServerPlayer player) {
        reset(player.getMainHandItem());
        reset(player.getOffhandItem());
        for (ItemStack stack : player.getInventory().items) {
            reset(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            reset(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            reset(stack);
        }
    }

    private static void reset(ItemStack stack) {
        if (stack.is(BTMI.DASH.get()) && Boolean.TRUE.equals(stack.get(BTMI.UNSHEATHED.get()))) {
            stack.set(BTMI.UNSHEATHED.get(), Boolean.FALSE);
        }
    }

    // ------------------------------------------------------------------
    // 客户端上报的动作
    // ------------------------------------------------------------------

    public static void handleAction(ServerPlayer player, int action, int chargeTicks) {
        if (!player.getMainHandItem().is(BTMI.DASH.get())) {
            return;
        }
        if (action == DashActionPayload.ACTION_ATTACK) {
            markUnsheathed(player);
        } else if (action == DashActionPayload.ACTION_PROJECTION) {
            DashProjectionSkill.tryActivate(player);
        } else if (action == DashActionPayload.ACTION_SLASH) {
            // 只要按住过就发动劈砍：轻点一下是最弱的一击，按住则逐渐增强
            if (chargeTicks >= 1) {
                startDashSlash(player, chargeTicks);
            }
        }
    }

    // ------------------------------------------------------------------
    // 蓄力劈砍
    // ------------------------------------------------------------------

    private static void startDashSlash(ServerPlayer player, int chargeTicks) {
        int min = Config.DASH_MIN_CHARGE_TICKS.get();
        int max = Math.max(min + 1, Config.DASH_MAX_CHARGE_TICKS.get());
        // 达到 min 之前强度为 0（最弱一击），满蓄力时为 1.0
        float intensity = Mth.clamp((chargeTicks - min) / (float) (max - min), 0.0F, 1.0F);

        markUnsheathed(player);

        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();

        // 向前上方腾空：明显的冲刺位移，让技能效果一眼可见
        double forward = 1.1D + 0.9D * intensity;
        double upward = 0.62D + 0.30D * intensity;
        player.setDeltaMovement(look.x * forward, upward, look.z * forward);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.swing(InteractionHand.MAIN_HAND, true);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 20, 0.3D, 0.1D, 0.3D, 0.15D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.5D, player.getZ(),
                15, 0.4D, 0.4D, 0.4D, 0.1D);

        // 真正的挥砍延迟几刻执行，先跃再劈更有感觉（
        PENDING.put(player.getUUID(), new PendingSlash(SLASH_DELAY, intensity));
    }

    private static void executeSlash(ServerPlayer player, float intensity) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!player.getMainHandItem().is(BTMI.DASH.get())) {
            return;
        }
        // 冲刺落地时不会因为下落距离而受伤
        player.resetFallDistance();

        Vec3 look = player.getLookAngle();
        double radius = 1.5D + Config.DASH_MAX_SLASH_RADIUS.get() * intensity;
        Vec3 center = player.position().add(look.scale(1.5D + 1.5D * intensity));
        float damage = (float) (DashItem.ATTACK_DAMAGE + 1.0D) * (float) (1.0D + (Config.DASH_MAX_DAMAGE_MULTIPLIER.get() - 1.0D) * intensity);

        AABB area = new AABB(center, center).inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            if (target.position().subtract(center).length() > radius) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), damage);
            target.push(look.x * (1.2D + 0.8D * intensity), 0.45D, look.z * (1.2D + 0.8D * intensity));
            target.hurtMarked = true;
        }

        // 玩家身前的一道弧形挥砍特效
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-6D) {
            // 正上方/正下方视线时水平分量为零，退回默认朝向
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }
        flat = flat.normalize();
        int points = Mth.ceil(radius * 8.0F);
        for (int i = 0; i <= points; i++) {
            double angle = -Math.PI * 0.45D + (Math.PI * 0.9D) * i / points;
            Vec3 dir = flat.yRot((float) angle);
            Vec3 point = center.add(dir.scale(radius));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, player.getY() + 0.8D, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.CRIT, point.x, player.getY() + 0.8D, point.z, 1, 0.0D, 0.0D, 0.0D, 0.05D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.3F, 0.8F);
        player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    // ------------------------------------------------------------------
    // 事件
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID id = player.getUUID();

        // 倒计时：出鞘状态
        Integer unsheatheLeft = UNSHEATH.get(id);
        if (unsheatheLeft != null) {
            if (unsheatheLeft <= 1) {
                UNSHEATH.remove(id);
                clearUnsheathed(player);
            } else {
                UNSHEATH.put(id, unsheatheLeft - 1);
            }
        }

        // 倒计时：待执行的劈砍
        PendingSlash pending = PENDING.get(id);
        if (pending != null) {
            if (pending.delay <= 1) {
                PENDING.remove(id);
                if (player.isAlive()) {
                    executeSlash(player, pending.intensity);
                }
            } else {
                pending.delay--;
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isCharging(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isCharging(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean isCharging(Player player) {
        return player.isShiftKeyDown() && player.getMainHandItem().is(BTMI.DASH.get());
    }

    private static final class PendingSlash {
        int delay;
        final float intensity;

        PendingSlash(int delay, float intensity) {
            this.delay = delay;
            this.intensity = intensity;
        }
    }
}
