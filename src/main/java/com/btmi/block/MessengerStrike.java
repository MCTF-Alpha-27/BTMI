package com.btmi.block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.btmi.BTMI;
import com.btmi.entity.KnellStrike;
import com.btmi.entity.ProjectionShockwave;
import com.btmi.network.ShakePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BTMI.MODID)
public final class MessengerStrike {
    private static final int RADIUS = 30;
    private static final int HEIGHT = 14;
    private static final int COLUMN_RADIUS = 10;
    private static final int COLUMN_HEIGHT = 72;
    private static final int BATCH = 1800;
    private static final float EFFECT_SCALE = 3.5F;
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Destruction> DESTRUCTIONS = new ArrayList<>();

    private MessengerStrike() {
    }

    public static void strike(ServerLevel level, BlockPos centerPos) {
        TASKS.add(new Task(level, centerPos.immutable(), level.getGameTime() + 1));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        if (!TASKS.isEmpty()) {
            List<Task> due = null;
            Iterator<Task> iterator = TASKS.iterator();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (now < task.due()) {
                    continue;
                }
                iterator.remove();
                if (due == null) {
                    due = new ArrayList<>();
                }
                due.add(task);
            }
            if (due != null) {
                for (Task task : due) {
                    strikeNow(task.level(), task.pos());
                }
            }
        }
        if (DESTRUCTIONS.isEmpty()) {
            return;
        }
        Iterator<Destruction> iterator = DESTRUCTIONS.iterator();
        while (iterator.hasNext()) {
            Destruction destruction = iterator.next();
            destruction.process(BATCH);
            if (destruction.done()) {
                iterator.remove();
            }
        }
    }

    private static void strikeNow(ServerLevel level, BlockPos centerPos) {
        Vec3 center = Vec3.atCenterOf(centerPos);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 5.0F, 0.38F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.BLOCKS, 3.0F, 0.4F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 3.0F, 0.45F);

        level.addFreshEntity(new KnellStrike(level, center, EFFECT_SCALE));
        level.addFreshEntity(new ProjectionShockwave(level, center.add(0.0D, 0.05D, 0.0D), 20.0F, true));
        level.addFreshEntity(new ProjectionShockwave(level, center.add(0.0D, 0.05D, 0.0D), 13.0F, true));

        AABB area = new AABB(center, center).inflate(RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!target.isAlive() || target.isSpectator()) {
                continue;
            }
            if (target instanceof Player player && player.isCreative()) {
                continue;
            }
            Vec3 offset = target.position().subtract(center);
            if (offset.length() > RADIUS + target.getBbWidth() * 0.5D) {
                continue;
            }
            target.hurt(level.damageSources().magic(), 24.0F);
            Vec3 push = new Vec3(offset.x, 0.0D, offset.z);
            if (push.lengthSqr() > 1.0E-4D) {
                push = push.normalize();
            }
            target.push(push.x * 1.6D, 0.9D, push.z * 1.6D);
            target.hurtMarked = true;
        }

        level.setBlock(centerPos, Blocks.AIR.defaultBlockState(), 2);

        Set<BlockPos> positions = new LinkedHashSet<>();
        for (int y = COLUMN_HEIGHT; y >= 0; y--) {
            for (int x = -COLUMN_RADIUS; x <= COLUMN_RADIUS; x++) {
                for (int z = -COLUMN_RADIUS; z <= COLUMN_RADIUS; z++) {
                    if (x * x + z * z > COLUMN_RADIUS * COLUMN_RADIUS) {
                        continue;
                    }
                    BlockPos pos = centerPos.offset(x, y, z);
                    if (!pos.equals(centerPos)) {
                        addDestroyable(level, pos, positions);
                    }
                }
            }
        }

        for (int y = -HEIGHT; y <= HEIGHT; y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (x * x + y * y + z * z > RADIUS * RADIUS) {
                        continue;
                    }
                    BlockPos pos = centerPos.offset(x, y, z);
                    if (!pos.equals(centerPos)) {
                        addDestroyable(level, pos, positions);
                    }
                }
            }
        }
        if (!positions.isEmpty()) {
            DESTRUCTIONS.add(new Destruction(level, new ArrayList<>(positions)));
        }

        PacketDistributor.sendToPlayersNear(level, null, center.x, center.y, center.z, 64.0D,
                new ShakePayload(center.x, center.y, center.z, 14.0F, 30));
    }

    private static void addDestroyable(ServerLevel level, BlockPos pos, Set<BlockPos> positions) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F) {
            positions.add(pos.immutable());
        }
    }

    private record Task(ServerLevel level, BlockPos pos, long due) {
    }

    private static final class Destruction {
        private final ServerLevel level;
        private final List<BlockPos> positions;
        private int cursor;

        private Destruction(ServerLevel level, List<BlockPos> positions) {
            this.level = level;
            this.positions = positions;
        }

        private void process(int limit) {
            int end = Math.min(this.positions.size(), this.cursor + limit);
            for (; this.cursor < end; this.cursor++) {
                this.level.setBlock(this.positions.get(this.cursor), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        private boolean done() {
            return this.cursor >= this.positions.size();
        }
    }
}
