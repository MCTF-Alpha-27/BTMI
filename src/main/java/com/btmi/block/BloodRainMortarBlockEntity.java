package com.btmi.block;

import com.btmi.BTMI;
import com.btmi.entity.BloodRainShell;
import com.btmi.menu.BloodRainMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BloodRainMortarBlockEntity extends BaseContainerBlockEntity {
    public static final int ARM_DELAY = 20;
    private static final int SHELL_SLOT = 0;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private long fireAt = -1L;

    public BloodRainMortarBlockEntity(BlockPos pos, BlockState state) {
        super(BTMI.BLOOD_RAIN_MORTAR_BE.get(), pos, state);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.btmi.blood_rain_mortar");
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(BTMI.BLOOD_RAIN_SHELL.get());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new BloodRainMenu(containerId, inventory, this.worldPosition);
    }

    public void onMenuOpened() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (this.fireAt < 0L) {
            return;
        }
        this.fireAt = -1L;
        this.setChanged();
    }

    public void onMenuClosed() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (this.getItem(SHELL_SLOT).isEmpty()) {
            return;
        }
        if (!this.level.getBlockState(this.worldPosition).is(BTMI.BLOOD_RAIN_MORTAR.get())) {
            return;
        }
        this.fireAt = this.level.getGameTime() + ARM_DELAY;
        this.level.playSound(null, this.worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.7F);
        this.setChanged();
    }

    public void tick() {
        if (this.level == null || this.level.isClientSide || this.fireAt < 0L) {
            return;
        }
        if (this.level.getGameTime() < this.fireAt) {
            return;
        }
        this.fireAt = -1L;
        ItemStack shell = this.getItem(SHELL_SLOT);
        if (shell.isEmpty()) {
            this.setChanged();
            return;
        }
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.getBlockState(this.worldPosition).is(BTMI.BLOOD_RAIN_MORTAR.get())) {
            return;
        }
        this.setItem(SHELL_SLOT, ItemStack.EMPTY);
        double muzzleX = this.worldPosition.getX() + 0.5D;
        double muzzleY = this.worldPosition.getY() + 1.3D;
        double muzzleZ = this.worldPosition.getZ() + 0.5D;
        BloodRainShell shellEntity = new BloodRainShell(serverLevel, muzzleX, muzzleY, muzzleZ);
        shellEntity.setItem(shell.copyWithCount(1));
        shellEntity.shoot(0.0D, 1.0D, 0.0D, 1.3F, 0.0F);
        serverLevel.addFreshEntity(shellEntity);
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, muzzleX, muzzleY, muzzleZ, 36, 0.4D, 0.4D, 0.4D, 0.03D);
        serverLevel.sendParticles(ParticleTypes.FLAME, muzzleX, muzzleY, muzzleZ, 24, 0.3D, 0.3D, 0.3D, 0.06D);
        serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE, muzzleX, muzzleY, muzzleZ, 18, 0.5D, 0.4D, 0.5D, 0.03D);
        serverLevel.playSound(null, this.worldPosition, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 3.0F, 0.45F);
        serverLevel.playSound(null, this.worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.2F, 0.6F);
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putLong("FireAt", this.fireAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.fireAt = tag.contains("FireAt") ? tag.getLong("FireAt") : -1L;
    }
}
