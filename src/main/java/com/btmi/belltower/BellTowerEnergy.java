package com.btmi.belltower;

import com.btmi.BTMI;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BellTowerEnergy {
    public static final int MAX = 100;

    private BellTowerEnergy() {
    }

    private static ItemStack find(Player player) {
        ItemStack best = ItemStack.EMPTY;
        int bestEnergy = -1;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(BTMI.DIGAMMA.get())) {
                int energy = stack.getMaxDamage() - stack.getDamageValue();
                if (energy > bestEnergy) {
                    best = stack;
                    bestEnergy = energy;
                }
            }
        }
        if (player.getOffhandItem().is(BTMI.DIGAMMA.get())) {
            int energy = player.getOffhandItem().getMaxDamage() - player.getOffhandItem().getDamageValue();
            if (energy > bestEnergy) {
                best = player.getOffhandItem();
            }
        }
        return best;
    }

    public static boolean hasItem(Player player) {
        return !find(player).isEmpty();
    }

    public static int get(Player player) {
        ItemStack stack = find(player);
        return stack.isEmpty() ? 0 : Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    public static boolean has(Player player, int amount) {
        return get(player) >= amount;
    }

    public static boolean consume(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        ItemStack stack = find(player);
        if (stack.isEmpty()) {
            return false;
        }
        int energy = stack.getMaxDamage() - stack.getDamageValue();
        if (energy < amount) {
            return false;
        }
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + amount));
        sync(player);
        return true;
    }

    public static void drain(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack stack = find(player);
        if (stack.isEmpty()) {
            return;
        }
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + amount));
        sync(player);
    }

    public static boolean set(Player player, int value) {
        ItemStack stack = find(player);
        if (stack.isEmpty()) {
            return false;
        }
        int clamped = Math.max(0, Math.min(MAX, value));
        stack.setDamageValue(stack.getMaxDamage() - clamped);
        sync(player);
        return true;
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
        }
    }
}
