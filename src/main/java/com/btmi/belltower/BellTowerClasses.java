package com.btmi.belltower;

import com.btmi.BTMI;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BellTowerClasses {
    private BellTowerClasses() {
    }

    public static BellTowerClass get(Player player) {
        return BellTowerClass.byId(player.getData(BTMI.BELL_TOWER_CLASS.get()));
    }

    public static BellTowerClass get(String id) {
        return BellTowerClass.byId(id);
    }

    public static boolean choose(ServerPlayer player, String id) {
        if (!BellTowerPermission.has(player)) {
            return false;
        }
        if (get(player).isChosen()) {
            return false;
        }
        BellTowerClass choice = BellTowerClass.byId(id);
        if (!choice.isChosen()) {
            return false;
        }
        set(player, choice);
        return true;
    }

    public static void set(ServerPlayer player, BellTowerClass choice) {
        player.setData(BTMI.BELL_TOWER_CLASS.get(), choice.id());
        if (choice == BellTowerClass.HARBINGER) {
            boolean hasBox = player.getInventory().items.stream().anyMatch(stack -> stack.is(BTMI.MESSENGER_BOX_ITEM.get()));
            if (!hasBox) {
                ItemStack box = new ItemStack(BTMI.MESSENGER_BOX_ITEM.get());
                if (!player.getInventory().add(box)) {
                    player.drop(box, false);
                }
            }
        }
    }
}
