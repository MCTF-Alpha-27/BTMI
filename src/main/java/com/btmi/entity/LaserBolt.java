package com.btmi.entity;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.item.DeathSignHandler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class LaserBolt extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 60;

    public LaserBolt(EntityType<? extends LaserBolt> entityType, Level level) {
        super(entityType, level);
    }

    public LaserBolt(Level level, LivingEntity shooter) {
        super(BTMI.LASER_BOLT_ENTITY.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return BTMI.LASER_BOLT.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(), 2, 0.03D, 0.03D, 0.03D, 0.0D);
        }
        if (this.tickCount > MAX_LIFE_TICKS) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        float damage = Config.DEATH_SIGN_LASER_DAMAGE.get().floatValue();
        if (target instanceof LivingEntity living && owner instanceof ServerPlayer serverPlayer
                && DeathSignHandler.isJudged(living) && DeathSignHandler.holdsDeathSignLaser(serverPlayer)) {
            damage *= 1.0F + Config.DEATH_SIGN_KNELL_MARK_BONUS.get().floatValue();
        }
        target.hurt(this.damageSources().thrown(this, owner), damage);
        if (owner instanceof LivingEntity livingOwner) {
            livingOwner.setLastHurtMob(target);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(), 12, 0.2D, 0.2D, 0.2D, 0.1D);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.8F);
        }
        this.discard();
    }
}
