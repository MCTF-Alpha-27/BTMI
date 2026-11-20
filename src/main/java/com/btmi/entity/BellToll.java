package com.btmi.entity;

import java.util.UUID;

import com.btmi.BTMI;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class BellToll extends Entity {
    public static final int IMPACT = 20;
    public static final int LIFE = 48;

    private UUID owner;

    public BellToll(EntityType<? extends BellToll> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public BellToll(Level level, ServerPlayer player) {
        this(BTMI.BELL_TOLL_ENTITY.get(), level);
        this.owner = player.getUUID();
        this.setPos(player.getX(), player.getY(), player.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount < IMPACT && this.owner != null && this.level() instanceof ServerLevel serverLevel) {
            ServerPlayer ownerPlayer = serverLevel.getServer().getPlayerList().getPlayer(this.owner);
            if (ownerPlayer != null && ownerPlayer.isAlive() && !ownerPlayer.isSpectator()) {
                this.setPos(ownerPlayer.getX(), ownerPlayer.getY(), ownerPlayer.getZ());
            }
        }
        if (this.tickCount > LIFE) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.owner = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
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
