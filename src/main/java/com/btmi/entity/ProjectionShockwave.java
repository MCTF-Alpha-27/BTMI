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

public class ProjectionShockwave extends Entity {
    public static final int LIFE = 16;

    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(ProjectionShockwave.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HORIZONTAL =
            SynchedEntityData.defineId(ProjectionShockwave.class, EntityDataSerializers.BOOLEAN);

    public ProjectionShockwave(EntityType<? extends ProjectionShockwave> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ProjectionShockwave(Level level, Vec3 position, float size, boolean horizontal) {
        this(BTMI.PROJECTION_SHOCKWAVE.get(), level);
        this.setPos(position.x, position.y, position.z);
        this.entityData.set(DATA_SIZE, size);
        this.entityData.set(DATA_HORIZONTAL, horizontal);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, 1.0F);
        builder.define(DATA_HORIZONTAL, false);
    }

    public float size() {
        return this.entityData.get(DATA_SIZE);
    }

    public boolean horizontal() {
        return this.entityData.get(DATA_HORIZONTAL);
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
        this.entityData.set(DATA_SIZE, tag.getFloat("Size"));
        this.entityData.set(DATA_HORIZONTAL, tag.getBoolean("Horizontal"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Size", this.size());
        tag.putBoolean("Horizontal", this.horizontal());
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
