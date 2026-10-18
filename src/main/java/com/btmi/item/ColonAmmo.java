package com.btmi.item;

import java.util.ArrayList;
import java.util.List;

import com.btmi.BTMI;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 冒号的六个弹巢在物品上的存取。
 *
 * <p>用固定长度的 {@code List<ItemStack>} 数据组件保存，保证槽位不会因空格而前移。
 */
public final class ColonAmmo {
    public static final int SLOTS = 6;

    private ColonAmmo() {
    }

    public static List<ItemStack> read(ItemStack gun) {
        List<ItemStack> stored = gun.get(BTMI.AMMO.get());
        List<ItemStack> result = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            result.add(stored != null && i < stored.size() ? stored.get(i).copy() : ItemStack.EMPTY);
        }
        return result;
    }

    public static void write(ItemStack gun, List<ItemStack> items) {
        gun.set(BTMI.AMMO.get(), List.copyOf(items));
    }

    public static int selected(ItemStack gun) {
        Integer value = gun.get(BTMI.SELECTED_AMMO.get());
        return Mth.clamp(value == null ? 0 : value, 0, SLOTS - 1);
    }

    public static void setSelected(ItemStack gun, int slot) {
        gun.set(BTMI.SELECTED_AMMO.get(), Mth.clamp(slot, 0, SLOTS - 1));
    }

    public static boolean isBullet(ItemStack stack) {
        return BulletType.of(stack) != null;
    }
}
