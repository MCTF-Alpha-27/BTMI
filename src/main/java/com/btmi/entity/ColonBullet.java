package com.btmi.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.Config;
import com.btmi.item.BulletType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ColonBullet extends ThrowableItemProjectile {
    private static final int MAX_LIFE_TICKS = 80;
    private static final double SEEK_RANGE = 24.0D;

    private final Set<UUID> pierced = new HashSet<>();

    public ColonBullet(EntityType<? extends ColonBullet> entityType, Level level) {
        super(entityType, level);
    }

    public ColonBullet(Level level, LivingEntity shooter, BulletType bulletType) {
        super(BTMI.COLON_BULLET.get(), shooter, level);
        this.setItem(bulletType.createStack());
    }

    @Override
    protected Item getDefaultItem() {
        return BTMI.HIGH_EXPLOSIVE_BULLET.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    private BulletType bulletType() {
        return BulletType.of(this.getItem());
    }

    @Override
    public void tick() {
        super.tick();
        BulletType type = this.bulletType();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (type == BulletType.SEEKER) {
                this.steerTowardsTarget(serverLevel);
            }
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(), 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        if (this.tickCount > MAX_LIFE_TICKS) {
            this.discard();
        }
    }

    private void steerTowardsTarget(ServerLevel level) {
        LivingEntity best = null;
        double bestDistance = SEEK_RANGE * SEEK_RANGE;
        AABB area = this.getBoundingBox().inflate(SEEK_RANGE);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (candidate == this.getOwner()) {
                continue;
            }
            double distance = candidate.distanceToSqr(this);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best == null) {
            return;
        }
        double speed = this.getDeltaMovement().length();
        Vec3 desired = best.getEyePosition().subtract(this.position()).normalize().scale(speed);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.55D).add(desired.scale(0.45D)));
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !this.pierced.contains(target.getUUID());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity target)) {
            return;
        }
        BulletType type = this.bulletType();
        if (type == null) {
            return;
        }

        float damage = type == BulletType.PIERCING
                ? Config.COLON_PIERCING_DAMAGE.get().floatValue()
                : Config.COLON_BULLET_DAMAGE.get().floatValue();

        target.invulnerableTime = 0;
        target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        if (this.getOwner() instanceof LivingEntity owner) {
            owner.setLastHurtMob(target);
        }
        if (type == BulletType.PARALYSIS) {
            this.paralyze(target);
        }
        this.pierced.add(target.getUUID());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        BulletType type = this.bulletType();
        if (type != null) {
            this.impact(type, result.getLocation());
        }
        // 穿甲弹命中生物后继续飞行，其余情况立即消失
        if (type != BulletType.PIERCING || result.getType() != HitResult.Type.ENTITY) {
            this.discard();
        }
    }

    private void impact(BulletType type, Vec3 pos) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        switch (type) {
            case HIGH_EXPLOSIVE -> {
                float radius = Config.COLON_EXPLOSION_RADIUS.get().floatValue();
                Level.ExplosionInteraction interaction = Config.COLON_EXPLOSION_BREAKS_BLOCKS.get()
                        ? Level.ExplosionInteraction.BLOCK
                        : Level.ExplosionInteraction.NONE;
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 2, 0.6D, 0.6D, 0.6D, 0.0D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 1.0F);
                level.explode(this, pos.x, pos.y, pos.z, radius, interaction);
            }
            case INCENDIARY -> {
                int radius = Config.COLON_INCENDIARY_RADIUS.get();
                BlockPos center = BlockPos.containing(pos);
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (dx * dx + dy * dy + dz * dz > radius * radius) {
                                continue;
                            }
                            BlockPos firePos = center.offset(dx, dy, dz);
                            if (!level.getBlockState(firePos).isAir()) {
                                continue;
                            }
                            BlockState below = level.getBlockState(firePos.below());
                            if (below.isAir() || !below.isSolidRender(level, firePos.below())) {
                                continue;
                            }
                            level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
                AABB area = new AABB(center).inflate(radius);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
                    if (target != this.getOwner()) {
                        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 80));
                    }
                }
                level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 40, 0.8D, 0.5D, 0.8D, 0.05D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.4F, 0.8F);
            }
            case CRYO -> {
                double radius = Config.COLON_CRYO_RADIUS.get();
                AABB area = new AABB(pos, pos).inflate(radius);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
                    if (target == this.getOwner()) {
                        continue;
                    }
                    target.setRemainingFireTicks(0);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true, true));
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, false, true, true));
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 60, 0.9D, 0.6D, 0.9D, 0.02D);
                level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.6F);
            }
            default -> level.sendParticles(ParticleTypes.CRIT,
                    pos.x, pos.y, pos.z, 12, 0.2D, 0.2D, 0.2D, 0.1D);
        }
    }

    private void paralyze(LivingEntity target) {
        int ticks = Config.COLON_PARALYSIS_TICKS.get();
        // 缓慢 6 级即可把移动速度压到 0，实现原地僵直
        target.addEffect(effect(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
        target.addEffect(effect(MobEffects.WEAKNESS, ticks, 2));
        target.addEffect(effect(MobEffects.DIG_SLOWDOWN, ticks, 2));

        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                    40, 0.4D, 0.6D, 0.4D, 0.15D);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.7F);
        }
    }

    private static MobEffectInstance effect(Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier, false, true, true);
    }
}
