package com.btmi.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.entity.ProjectionPortal;
import com.btmi.entity.ProjectionShockwave;
import com.btmi.network.DashChargePayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public final class DashProjectionSkill {
    public static final int REQUIRED_HITS = 10;

    // 随机散布范围
    private static final double LANE_SPREAD = 5.0D;
    private static final double HEIGHT_SPREAD = 1.6D;
    private static final double DEPTH_SPREAD = 2.5D;


    private static final Map<UUID, Integer> CHARGES = new HashMap<>();

    private DashProjectionSkill() {
    }

    public static void addCharge(ServerPlayer player) {
        int current = CHARGES.getOrDefault(player.getUUID(), 0);
        if (current >= REQUIRED_HITS) {
            return;
        }
        int charge = current + 1;
        CHARGES.put(player.getUUID(), charge);
        PacketDistributor.sendToPlayer(player, new DashChargePayload(charge));
    }

    public static int charge(ServerPlayer player) {
        return CHARGES.getOrDefault(player.getUUID(), 0);
    }

    public static void tryActivate(ServerPlayer player) {
        if (!player.getMainHandItem().is(BTMI.DASH.get())) {
            return;
        }
        if (charge(player) < REQUIRED_HITS) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7F, 1.4F);
            return;
        }
        CHARGES.put(player.getUUID(), 0);
        PacketDistributor.sendToPlayer(player, new DashChargePayload(0));
        activate(player);
    }

    private static void activate(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();

        // 指向位置：准星打到的点，没打到就取前方 40 格
        HitResult hit = player.pick(40.0D, 1.0F, false);
        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? eye.add(look.scale(40.0D))
                : hit.getLocation();

        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();

        // 在身后偏上随机散开：横向先分成若干区间再打乱，保证既分散又不重叠
        Vec3 base = eye.add(look.scale(-2.4D)).add(0.0D, 1.9D, 0.0D);
        List<Integer> lanes = new ArrayList<>(ProjectionPortal.PORTAL_COUNT);
        for (int i = 0; i < ProjectionPortal.PORTAL_COUNT; i++) {
            lanes.add(i);
        }
        // RandomSource 没有 shuffle，自己洗牌
        for (int i = lanes.size() - 1; i > 0; i--) {
            Collections.swap(lanes, i, level.random.nextInt(i + 1));
        }

        double laneStep = LANE_SPREAD * 2.0D / ProjectionPortal.PORTAL_COUNT;
        for (int i = 0; i < ProjectionPortal.PORTAL_COUNT; i++) {
            int slot = lanes.get(i);
            double lane = -LANE_SPREAD + laneStep * (slot + 0.5D)
                    + (level.random.nextDouble() - 0.5D) * laneStep * 0.5D;
            double height = (level.random.nextDouble() - 0.5D) * HEIGHT_SPREAD * 2.0D;
            double depth = -level.random.nextDouble() * DEPTH_SPREAD;

            Vec3 position = base
                    .add(right.scale(lane))
                    .add(look.scale(depth))
                    .add(0.0D, height, 0.0D);
            // 依次开门的延迟
            level.addFreshEntity(new ProjectionPortal(level, position, target, i, player.getUUID(), i * ProjectionPortal.OPEN_INTERVAL));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.4F, 0.6F);

        level.addFreshEntity(new ProjectionShockwave(level,
                player.position().add(0.0D, 0.05D, 0.0D), 4.5F, true));
        level.addFreshEntity(new ProjectionShockwave(level, base, 2.4F, false));
        if (hit.getType() != HitResult.Type.MISS) {
            level.addFreshEntity(new ProjectionShockwave(level, target, 2.2F, false));
        }

        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) {
            patch.playAnimationSynchronized(Animations.EVOKER_CAST_SPELL, 0.0F);
        }
    }
}
