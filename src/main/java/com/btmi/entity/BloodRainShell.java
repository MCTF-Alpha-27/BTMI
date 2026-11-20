package com.btmi.entity;

import com.btmi.BTMI;
import com.btmi.bloodrain.BloodRainHandler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class BloodRainShell extends ThrowableItemProjectile {
    private static final int MAX_FLIGHT_TICKS = 200;

    public BloodRainShell(EntityType<? extends BloodRainShell> entityType, Level level) {
        super(entityType, level);
    }

    public BloodRainShell(Level level, double x, double y, double z) {
        super(BTMI.BLOOD_RAIN_SHELL_ENTITY.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return BTMI.BLOOD_RAIN_SHELL.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) {
            return;
        }
        if (this.level().isClientSide) {
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.01D, 0.0D);
            }
            return;
        }
        if (this.tickCount >= MAX_FLIGHT_TICKS
                || (this.tickCount > 2 && this.getDeltaMovement().y <= 0.0D)) {
            this.impact(this.position());
            return;
        }
        if (this.getY() < this.level().getMinBuildHeight() - 16) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.impact(result.getLocation());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        this.impact(result.getLocation());
    }

    private void impact(Vec3 at) {
        if (this.isRemoved()) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            BloodRainHandler.start(serverLevel);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 4, 0.5D, 0.5D, 0.5D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE, at.x, at.y, at.z, 90, 2.6D, 2.0D, 2.6D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 40, 1.4D, 1.2D, 1.4D, 0.04D);
            serverLevel.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.WEATHER, 4.0F, 0.4F);
            serverLevel.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.WEATHER, 2.0F, 0.55F);
        }
        this.discard();
    }
}
