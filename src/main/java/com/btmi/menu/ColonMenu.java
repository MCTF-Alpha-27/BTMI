package com.btmi.menu;

import com.btmi.BTMI;
import com.btmi.item.ColonAmmo;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

public class ColonMenu extends AbstractContainerMenu {
    public static final int DISC_CENTER_X = 88;
    public static final int DISC_CENTER_Y = 60;
    public static final int DISC_RADIUS = 30;
    public static final int INV_Y = 104;
    public static final int HOTBAR_Y = 162;

    private final AmmoContainer ammo;
    private final ItemStack gun;

    public ColonMenu(int windowId, Inventory inventory) {
        super(BTMI.COLON_MENU.get(), windowId);
        Player player = inventory.player;
        ItemStack main = player.getMainHandItem();
        this.gun = main.is(BTMI.COLON.get()) ? main : player.getOffhandItem();
        this.ammo = new AmmoContainer(this.gun);

        for (int i = 0; i < ColonAmmo.SLOTS; i++) {
            double angle = Math.toRadians(-90.0D + i * 60.0D);
            int x = (int) Math.round(DISC_CENTER_X + Math.cos(angle) * DISC_RADIUS) - 8;
            int y = (int) Math.round(DISC_CENTER_Y + Math.sin(angle) * DISC_RADIUS) - 8;
            this.addSlot(new Slot(this.ammo, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ColonAmmo.isBullet(stack);
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    public AmmoContainer ammo() {
        return this.ammo;
    }

    public ItemStack gun() {
        return this.gun;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < ColonAmmo.SLOTS) {
            if (!this.moveItemStackTo(stack, ColonAmmo.SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (ColonAmmo.isBullet(stack)) {
            if (!this.moveItemStackTo(stack, 0, ColonAmmo.SLOTS, false)) {
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
        return player.getMainHandItem().is(BTMI.COLON.get()) || player.getOffhandItem().is(BTMI.COLON.get());
    }

    public static class AmmoContainer implements Container {
        private final ItemStack gun;
        private final NonNullList<ItemStack> items = NonNullList.withSize(ColonAmmo.SLOTS, ItemStack.EMPTY);

        public AmmoContainer(ItemStack gun) {
            this.gun = gun;
            for (int i = 0; i < ColonAmmo.SLOTS; i++) {
                this.items.set(i, ColonAmmo.read(gun).get(i));
            }
        }

        @Override
        public int getContainerSize() {
            return ColonAmmo.SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return this.items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.items.get(Mth.clamp(slot, 0, ColonAmmo.SLOTS - 1));
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ContainerHelper.removeItem(this.items, slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(this.items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.items.set(Mth.clamp(slot, 0, ColonAmmo.SLOTS - 1), stack);
        }

        @Override
        public void setChanged() {
            ColonAmmo.write(this.gun, this.items);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            this.items.clear();
        }
    }
}
