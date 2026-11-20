package com.btmi.block;

import com.btmi.belltower.BellTowerClass;
import com.btmi.belltower.BellTowerClasses;
import com.btmi.belltower.BellTowerEnergy;
import com.btmi.entity.BellTowerClock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MessengerBoxBlockEntity extends BlockEntity {
    public static final int COUNTDOWN = 200;
    public static final int COOLDOWN = 600;
    public static final int ENERGY_COST = 50;
    public static final double CLOCK_HEIGHT = 72.0D;

    private boolean active;
    private int ticks;
    private int cooldown;

    public MessengerBoxBlockEntity(BlockPos pos, BlockState state) {
        super(com.btmi.BTMI.MESSENGER_BOX_BE.get(), pos, state);
    }

    public boolean activate(ServerPlayer player) {
        if (BellTowerClasses.get(player) != BellTowerClass.HARBINGER) {
            player.displayClientMessage(
                    Component.translatable("message.btmi.messenger_box.class").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (this.active) {
            player.displayClientMessage(
                    Component.translatable("message.btmi.messenger_box.active").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (this.cooldown > 0) {
            player.displayClientMessage(
                    Component.translatable("message.btmi.messenger_box.cooldown").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!BellTowerEnergy.consume(player, ENERGY_COST)) {
            player.displayClientMessage(
                    Component.translatable("message.btmi.energy.low").withStyle(ChatFormatting.RED), true);
            return false;
        }
        this.active = true;
        this.ticks = 0;
        this.setChanged();
        this.sync();
        if (this.level != null) {
            this.level.setBlock(this.worldPosition,
                    this.getBlockState().setValue(MessengerBoxBlock.LIT, Boolean.TRUE), 3);
            this.level.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2F, 0.8F);
            this.level.playSound(null, this.worldPosition, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(new BellTowerClock(serverLevel,
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + CLOCK_HEIGHT,
                    this.worldPosition.getZ() + 0.5D));
        }
        return true;
    }

    public void tick() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (!this.active) {
            return;
        }
        this.ticks++;
        if (this.ticks % 5 == 0) {
            BlockState state = this.getBlockState();
            this.level.setBlock(this.worldPosition, state.setValue(MessengerBoxBlock.LIT,
                    !state.getValue(MessengerBoxBlock.LIT)), 3);
        }
        if (this.ticks >= COUNTDOWN) {
            this.active = false;
            this.ticks = 0;
            this.cooldown = COOLDOWN;
            this.setChanged();
            this.sync();
            this.level.setBlock(this.worldPosition,
                    this.getBlockState().setValue(MessengerBoxBlock.LIT, Boolean.FALSE), 3);
            MessengerStrike.strike((ServerLevel) this.level, this.worldPosition);
            return;
        }
    }

    public boolean isActive() {
        return this.active;
    }

    private void sync() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Active", this.active);
        tag.putInt("Ticks", this.ticks);
        tag.putInt("Cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.active = tag.getBoolean("Active");
        this.ticks = tag.getInt("Ticks");
        this.cooldown = tag.getInt("Cooldown");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Active", this.active);
        tag.putInt("Ticks", this.ticks);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
