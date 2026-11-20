package com.btmi.menu;

import com.btmi.BTMI;
import com.btmi.block.BloodRainMortarBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BloodRainMenu extends AbstractContainerMenu {
    public static final int SHELL_X = 80;
    public static final int SHELL_Y = 36;
    public static final int INV_Y = 86;
    public static final int HOTBAR_Y = 144;

    private final Container container;
    private final BlockPos pos;

    public BloodRainMenu(int windowId, Inventory inventory, BlockPos pos) {
        super(BTMI.BLOOD_RAIN_MORTAR_MENU.get(), windowId);
        this.pos = pos;
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        this.container = blockEntity instanceof BloodRainMortarBlockEntity mortar ? mortar : new SimpleContainer(1);

        if (this.container instanceof BloodRainMortarBlockEntity mortar) {
            mortar.onMenuOpened();
        }

        this.addSlot(new Slot(this.container, 0, SHELL_X, SHELL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(BTMI.BLOOD_RAIN_SHELL.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    public BlockPos pos() {
        return this.pos;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && this.container instanceof BloodRainMortarBlockEntity mortar) {
            mortar.onMenuClosed();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(BTMI.BLOOD_RAIN_SHELL.get())) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
