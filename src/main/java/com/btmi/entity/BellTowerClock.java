package com.btmi.entity;

import com.btmi.BTMI;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class BellTowerClock extends Entity {
    public static final int LIFE = 200;

    public BellTowerClock(EntityType<? extends BellTowerClock> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public BellTowerClock(Level level, double x, double y, double z) {
        this(level, x, y, z, 0.0F);
    }

    public BellTowerClock(Level level, double x, double y, double z, float yaw) {
        this(BTMI.BELL_TOWER_CLOCK.get(), level);
        this.setPos(x, y, z);
        this.setYRot(yaw);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > LIFE) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 640.0D * 640.0D;
    }
}
