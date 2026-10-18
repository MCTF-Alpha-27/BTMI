package com.btmi.entity;

import com.btmi.BTMI;
import com.btmi.Config;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LightOrb extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 100;

    public LightOrb(EntityType<? extends LightOrb> entityType, Level level) {
        super(entityType, level);
    }

    public LightOrb(Level level, LivingEntity shooter) {
        super(BTMI.LIGHT_ORB_ENTITY.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return BTMI.LIGHT_ORB.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        // 飞行拖尾（由服务端发送给附近客户端）
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(), 2, 0.02D, 0.02D, 0.02D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(), 1, 0.05D, 0.05D, 0.05D, 0.0D);
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
        target.hurt(this.damageSources().thrown(this, owner), Config.DASH_ORB_DAMAGE.get().floatValue());
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
        // 命中时：召唤闪电 + 粒子 + 音效
        this.strikeLightning();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(), 40, 0.6D, 0.6D, 0.6D, 0.25D);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.2F, 1.1F);
        }
        this.discard();
    }

    private void strikeLightning() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(Vec3.atBottomCenterOf(this.blockPosition()));
        // 纯视觉：不造成伤害、不点燃方块、不转化实体
        bolt.setVisualOnly(true);
        if (this.getOwner() instanceof ServerPlayer serverPlayer) {
            bolt.setCause(serverPlayer);
        }
        serverLevel.addFreshEntity(bolt);
    }
}
