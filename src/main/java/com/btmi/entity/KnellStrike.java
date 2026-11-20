package com.btmi.entity;

import com.btmi.BTMI;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class KnellStrike extends Entity {
    public static final int LIFE = 24;

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(KnellStrike.class, EntityDataSerializers.FLOAT);

    public KnellStrike(EntityType<? extends KnellStrike> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public KnellStrike(Level level, Vec3 position) {
        this(level, position, 1.0F);
    }

    public KnellStrike(Level level, Vec3 position, float scale) {
        this(BTMI.KNELL_STRIKE.get(), level);
        this.setPos(position.x, position.y, position.z);
        this.entityData.set(DATA_SCALE, scale);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SCALE, 1.0F);
    }

    public float scale() {
        return this.entityData.get(DATA_SCALE);
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
        return distance < 96.0D * 96.0D;
    }
}
