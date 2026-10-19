package com.btmi.entity;

import com.btmi.BTMI;
import com.btmi.item.ObeliskHandler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ObeliskSentinel extends Entity implements ItemSupplier {
    private static final int MODE_IDLE = 0;
    private static final int MODE_RAM = 1;
    private static final int MODE_DETONATE = 2;

    private static final double RAM_SPEED = 1.4D;
    private static final double DETONATE_SPEED = 2.2D;
    private static final int RAM_DURATION = 10;
    private static final int DETONATE_DURATION = 120;

    private UUID ownerUUID;
    private int ownerHand;
    private int mode = MODE_IDLE;
    private int timer;
    private Vec3 dashDir = Vec3.ZERO;
    private final Set<UUID> ramHit = new HashSet<>();

    public ObeliskSentinel(EntityType<? extends ObeliskSentinel> entityType, Level level) {
        super(entityType, level);
    }

    public ObeliskSentinel(Level level, Player owner, int hand) {
        super(BTMI.OBELISK_SENTINEL.get(), level);
        this.ownerUUID = owner.getUUID();
        this.ownerHand = hand;
        this.setPos(owner.getX(), owner.getY() + 1.2D, owner.getZ());
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("Owner")) {
            this.ownerUUID = compound.getUUID("Owner");
        }
        this.ownerHand = compound.getInt("Hand");
    }

    @Override
    public boolean save(CompoundTag compound) {
        // 悬浮实体是召唤物，不写入存档，避免重新进游戏后残留
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.ownerUUID != null) {
            compound.putUUID("Owner", this.ownerUUID);
        }
        compound.putInt("Hand", this.ownerHand);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(BTMI.OBELISK.get());
    }

    public int getOwnerHand() {
        return this.ownerHand;
    }

    public boolean isOwnedBy(Player player) {
        return this.ownerUUID != null && this.ownerUUID.equals(player.getUUID());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ------------------------------------------------------------------
    // 行为
    // ------------------------------------------------------------------

    private void faceDirection(Vec3 direction) {
        Vec3 dir = normalizeOr(direction, Vec3.ZERO);
        if (dir.lengthSqr() < 1.0E-6D) {
            return;
        }
        // Minecraft 中 yaw 对应的前方 = (sin yaw, 0, -cos yaw)，反解即 yaw = atan2(dir.x, -dir.z)
        this.setYRot((float) (Mth.atan2(dir.x, -dir.z) * (180.0D / Math.PI)));
    }

    public void startRam(Vec3 direction) {
        this.mode = MODE_RAM;
        this.timer = RAM_DURATION;
        this.dashDir = normalizeOr(direction, Vec3.ZERO);
        this.faceDirection(this.dashDir);
        this.ramHit.clear();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    public void startDetonate(Vec3 direction) {
        this.mode = MODE_DETONATE;
        this.timer = DETONATE_DURATION;
        this.dashDir = normalizeOr(direction, Vec3.ZERO);
        this.faceDirection(this.dashDir);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.5F, 1.6F);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // 客户端只负责一点点环绕光点
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getRandomX(0.5D), this.getY() + 0.35D, this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        ServerLevel level = (ServerLevel) this.level();
        Player owner = this.resolveOwner(level);
        switch (this.mode) {
            case MODE_RAM -> this.tickRam(level, owner);
            case MODE_DETONATE -> this.tickDetonate(level, owner);
            default -> this.tickHover(level, owner);
        }
    }

    private Player resolveOwner(ServerLevel level) {
        if (this.ownerUUID == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(this.ownerUUID);
    }

    private void tickHover(ServerLevel level, Player owner) {
        this.noPhysics = true;
        if (owner == null || !owner.isAlive()) {
            // 主人不在线或已死亡：直接消失
            this.discard();
            return;
        }
        // 手里已经没有召回示亡号了，就不要再留着
        InteractionHand hand = this.ownerHand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!owner.getItemInHand(hand).is(BTMI.OBELISK_RECALL.get())) {
            this.discard();
            return;
        }

        Vec3 look = owner.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize().scale(this.ownerHand == 1 ? -1.0D : 1.0D);

        double bob = Math.sin(this.tickCount * 0.18D) * 0.15D;
        Vec3 target = owner.position()
                .add(right.scale(1.2D))
                .add(look.x * 0.5D, 1.15D + bob, look.z * 0.5D);

        // 朝向跟随玩家视线，让十字架的最顶端始终指向前方
        this.faceDirection(look);

        Vec3 delta = target.subtract(this.position()).scale(0.35D);
        this.move(MoverType.SELF, delta);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void tickRam(ServerLevel level, Player owner) {
        this.noPhysics = false;
        this.move(MoverType.SELF, this.dashDir.scale(RAM_SPEED));

        AABB area = this.getBoundingBox().inflate(1.2D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target == owner || (this.ownerUUID != null && this.ownerUUID.equals(target.getUUID()))) {
                continue;
            }
            if (this.ramHit.add(target.getUUID())) {
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().indirectMagic(this, owner),
                        com.btmi.Config.OBELISK_RAM_DAMAGE.get().floatValue());
                Vec3 push = this.dashDir.normalize().scale(1.2D);
                target.push(push.x, 0.4D, push.z);
                target.hurtMarked = true;
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                        10, 0.3D, 0.3D, 0.3D, 0.15D);
            }
        }

        level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(),
                6, 0.2D, 0.2D, 0.2D, 0.05D);

        if (--this.timer <= 0 || this.horizontalCollision || this.verticalCollision) {
            this.mode = MODE_IDLE;
            this.ramHit.clear();
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void tickDetonate(ServerLevel level, Player owner) {
        this.noPhysics = false;
        this.move(MoverType.SELF, this.dashDir.scale(DETONATE_SPEED));

        boolean hitBlock = this.horizontalCollision || this.verticalCollision || this.onGround();
        LivingEntity hitEntity = null;
        AABB area = this.getBoundingBox().inflate(0.8D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target == owner || (this.ownerUUID != null && this.ownerUUID.equals(target.getUUID()))) {
                continue;
            }
            hitEntity = target;
            break;
        }

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(),
                12, 0.25D, 0.25D, 0.25D, 0.1D);

        if (hitBlock || hitEntity != null || --this.timer <= 0) {
            this.detonate(level, owner, hitEntity);
        }
    }

    private void detonate(ServerLevel level, Player owner, LivingEntity directHit) {
        float radius = com.btmi.Config.OBELISK_EXPLOSION_RADIUS.get().floatValue();
        Level.ExplosionInteraction interaction = com.btmi.Config.OBELISK_EXPLOSION_BREAKS_BLOCKS.get()
                ? Level.ExplosionInteraction.BLOCK
                : Level.ExplosionInteraction.NONE;

        level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 3, 1.0D, 1.0D, 1.0D, 0.0D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0F, 0.8F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 1.2F);

        level.explode(this, this.getX(), this.getY(), this.getZ(), radius, interaction);

        if (directHit != null) {
            directHit.invulnerableTime = 0;
            directHit.hurt(level.damageSources().indirectMagic(this, owner), radius * 2.0F);
        }

        this.discard();
    }

    private static Vec3 normalizeOr(Vec3 value, Vec3 fallback) {
        if (value == null || value.lengthSqr() < 1.0E-6D) {
            return fallback;
        }
        return value.normalize();
    }
}
