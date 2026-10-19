package com.btmi.entity;

import java.util.UUID;

import com.btmi.BTMI;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FlameButterfly extends Entity {
    public static final int ORBIT_TICKS = 600;

    private static final double BASE_ORBIT_RADIUS = 2.3D;
    private static final double ORBIT_HEIGHT = 1.25D;
    private static final double CHASE_SPEED = 0.8D;
    private static final float DAMAGE = 4.0F;
    /** 锁定目标后最多追多久，超时就消失 */
    private static final int TARGET_TIMEOUT = 120;

    private UUID ownerId;
    private int targetId = -1;
    private int targetSetAt = -1;
    private int spawnIndex;
    private double orbitPhase;
    private double orbitRadius = BASE_ORBIT_RADIUS;
    private Vec3 lastTargetPos = Vec3.ZERO;
    private boolean hasLastTargetPos;

    public FlameButterfly(EntityType<? extends FlameButterfly> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public FlameButterfly(Level level, Player owner, double orbitPhase, int spawnIndex) {
        this(BTMI.FLAME_BUTTERFLY.get(), level);
        this.ownerId = owner.getUUID();
        this.orbitPhase = orbitPhase;
        this.spawnIndex = spawnIndex;
        // 每只的环绕半径略有差异，避免全部挤在同一圈上
        this.orbitRadius = BASE_ORBIT_RADIUS + (spawnIndex % 3) * 0.45D;
        this.setPos(owner.getX(), owner.getY() + ORBIT_HEIGHT, owner.getZ());
    }

    public boolean isOwnedBy(Player player) {
        return this.ownerId != null && this.ownerId.equals(player.getUUID());
    }

    public void setTargetEntity(Entity target) {
        if (this.targetId < 0) {
            // 记录第一次锁定目标的时刻，用来错开飞出的顺序
            this.targetSetAt = this.tickCount;
        }
        this.targetId = target.getId();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();

        if (this.tickCount > ORBIT_TICKS) {
            this.discard();
            return;
        }

        boolean assigned = this.targetId >= 0;
        Entity target = assigned ? level.getEntity(this.targetId) : null;
        if (assigned) {
            if (target != null && target.isAlive()) {
                this.lastTargetPos = target.position();
                this.hasLastTargetPos = true;
            }
            boolean ready = this.tickCount >= this.targetSetAt + this.spawnIndex * 4;
            if (ready) {
                if (target != null && target.isAlive()) {
                    this.chase(level, target);
                } else if (this.hasLastTargetPos) {
                    // 目标已死：飞向它最后的位置再消失
                    this.chasePosition(level, this.lastTargetPos);
                } else {
                    this.discard();
                }
                return;
            }
            if (this.tickCount > this.targetSetAt + TARGET_TIMEOUT) {
                this.discard();
                return;
            }
        }
        this.orbit(level);
    }

    private void orbit(ServerLevel level) {
        Player owner = this.ownerId == null ? null : level.getPlayerByUUID(this.ownerId);
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        double angle = owner.tickCount * 0.10D + this.orbitPhase;
        double height = ORBIT_HEIGHT
                + (this.spawnIndex % 4 - 1.5D) * 0.12D
                + Math.sin(owner.tickCount * 0.18D + this.orbitPhase * 1.7D) * 0.35D;
        this.setPos(owner.getX() + Math.cos(angle) * this.orbitRadius,
                owner.getY() + height,
                owner.getZ() + Math.sin(angle) * this.orbitRadius);

        if (this.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.SMALL_FLAME,
                    this.getX(), this.getY(), this.getZ(), 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private void chasePosition(ServerLevel level, Vec3 goal) {
        Vec3 toTarget = goal.add(0.0D, 0.8D, 0.0D).subtract(this.position());
        double distance = toTarget.length();
        if (distance < 0.9D) {
            this.explodeAt(level);
            return;
        }
        Vec3 step = toTarget.scale(CHASE_SPEED / distance);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(),
                2, 0.05D, 0.05D, 0.05D, 0.01D);
    }

    private void explodeAt(ServerLevel level) {
        level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(),
                10, 0.2D, 0.2D, 0.2D, 0.05D);
        this.discard();
    }

    private void chase(ServerLevel level, Entity target) {
        Vec3 toTarget = target.getEyePosition().subtract(this.position());
        double distance = toTarget.length();
        if (distance < 0.9D) {
            this.explode(level, target);
            return;
        }
        Vec3 step = toTarget.scale(CHASE_SPEED / distance);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
        level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(),
                2, 0.05D, 0.05D, 0.05D, 0.01D);
    }

    private void explode(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity living) {
            Player owner = this.ownerId == null ? null : level.getPlayerByUUID(this.ownerId);
            living.invulnerableTime = 0;
            living.hurt(owner != null ? level.damageSources().playerAttack(owner) : level.damageSources().magic(), DAMAGE);
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 60));
        }
        level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(),
                12, 0.2D, 0.2D, 0.2D, 0.05D);
        level.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(),
                2, 0.15D, 0.15D, 0.15D, 0.0D);
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
        this.orbitPhase = tag.getDouble("Phase");
        this.targetId = tag.getInt("TargetId");
        this.spawnIndex = tag.getInt("SpawnIndex");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putDouble("Phase", this.orbitPhase);
        tag.putInt("TargetId", this.targetId);
        tag.putInt("SpawnIndex", this.spawnIndex);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
