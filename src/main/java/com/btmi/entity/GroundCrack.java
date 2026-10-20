package com.btmi.entity;

import com.btmi.BTMI;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GroundCrack extends Entity {
    public static final int LIFE_TICKS = 40;

    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(GroundCrack.class, EntityDataSerializers.FLOAT);

    public GroundCrack(EntityType<? extends GroundCrack> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public GroundCrack(Level level, Vec3 start, Vec3 direction, float length) {
        this(BTMI.GROUND_CRACK.get(), level);
        this.setPos(start.x, start.y, start.z);
        this.setYRot((float) (Mth.atan2(-direction.x, direction.z) * (180.0D / Math.PI)));
        this.setXRot(0.0F);
        this.getEntityData().set(DATA_LENGTH, length);
    }

    public float length() {
        return this.getEntityData().get(DATA_LENGTH);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LENGTH, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > LIFE_TICKS) {
            this.discard();
        }
    }

    @Override
    public boolean save(CompoundTag tag) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.getEntityData().set(DATA_LENGTH, tag.getFloat("Length"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Length", this.length());
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
