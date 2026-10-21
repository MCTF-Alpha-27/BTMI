package com.btmi.entity;

import java.util.UUID;

import com.btmi.BTMI;
import com.btmi.entity.ProjectionShockwave;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ProjectionPortal extends Entity {
    /** 传送门打开的时长 */
    public static final int EMERGE_TICKS = 14;
    /** 全部开门之后持续射击的时长 */
    public static final int ACTIVE_TICKS = 80;
    /** 传送门个数 */
    public static final int PORTAL_COUNT = 6;
    /** 相邻两个传送门开门的间隔（开得快一点） */
    public static final int OPEN_INTERVAL = 3;

    /** 从第 1 个门开始打开，到全部开完所需的时间 */
    public static int fireStartDelay() {
        return (PORTAL_COUNT - 1) * OPEN_INTERVAL + EMERGE_TICKS;
    }

    private static final int FIRE_INTERVAL = 14;
    private static final int FIRE_STAGGER = 2;
    /** 光球从门面稍前方射出 */
    private static final double MUZZLE_OFFSET = 0.55D;

    private static final EntityDataAccessor<Integer> DATA_OPEN_DELAY =
            SynchedEntityData.defineId(ProjectionPortal.class, EntityDataSerializers.INT);

    private int index;
    private Vec3 target = Vec3.ZERO;
    private UUID ownerId;

    public ProjectionPortal(EntityType<? extends ProjectionPortal> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ProjectionPortal(Level level, Vec3 position, Vec3 target, int index, UUID ownerId, int openDelay) {
        this(BTMI.PROJECTION_PORTAL.get(), level);
        this.setPos(position.x, position.y, position.z);
        this.target = target;
        this.index = index;
        this.ownerId = ownerId;
        this.getEntityData().set(DATA_OPEN_DELAY, openDelay);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OPEN_DELAY, 0);
    }

    /** 开门延迟（tick），用来让多个传送门依次打开 */
    public int openDelay() {
        return this.getEntityData().get(DATA_OPEN_DELAY);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        int age = this.tickCount - this.openDelay();
        if (age < 0) {
            // 还没轮到它开门
            return;
        }

        Vec3 direction = this.fireDirection();
        this.setYRot((float) (Mth.atan2(direction.x, direction.z) * (180.0D / Math.PI)));
        this.setXRot((float) (-Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D)) * (180.0D / Math.PI)));

        if (age == 0) {
            level.addFreshEntity(new ProjectionShockwave(level, this.position(), 1.8F, false));
        }

        if (age <= EMERGE_TICKS) {
            return;
        }

        int active = age - fireStartDelay();
        if (active < 0) {
            return;
        }
        if (active >= ACTIVE_TICKS) {
            this.discard();
            return;
        }

        int offset = this.index * FIRE_STAGGER;
        if (active >= offset && (active - offset) % FIRE_INTERVAL == 0) {
            this.fire(level, direction);
        }
    }

    private Vec3 fireDirection() {
        Vec3 direction = this.target.subtract(this.position());
        return direction.lengthSqr() < 1.0E-4D ? this.getLookAngle() : direction.normalize();
    }

    private void fire(ServerLevel level, Vec3 direction) {
        Vec3 muzzle = this.position().add(direction.scale(MUZZLE_OFFSET));

        LightOrb orb = new LightOrb(BTMI.LIGHT_ORB_ENTITY.get(), level);
        orb.setOwner(this.ownerId == null ? null : level.getPlayerByUUID(this.ownerId));
        orb.setPos(muzzle.x, muzzle.y, muzzle.z);
        orb.shoot(direction.x, direction.y, direction.z, 2.2F, 0.0F);
        level.addFreshEntity(orb);

        level.addFreshEntity(new ProjectionShockwave(level, muzzle, 0.9F, false));
        level.playSound(null, muzzle.x, muzzle.y, muzzle.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.index = tag.getInt("Index");
        this.getEntityData().set(DATA_OPEN_DELAY, tag.getInt("OpenDelay"));
        this.target = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Index", this.index);
        tag.putInt("OpenDelay", this.openDelay());
        tag.putDouble("TargetX", this.target.x);
        tag.putDouble("TargetY", this.target.y);
        tag.putDouble("TargetZ", this.target.z);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
