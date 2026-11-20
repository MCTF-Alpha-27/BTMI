package com.btmi.belltower;

import com.btmi.BTMI;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class EnergyCoreLoot {
    private static final String POOL_NAME = "btmi_energy_core";
    private static final int EMPTY_WEIGHT = 78;
    private static final int CORE_WEIGHT = 22;

    private EnergyCoreLoot() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        String path = name.getPath();
        if (!path.startsWith("chests/")) {
            return;
        }
        if (BTMI.MODID.equals(name.getNamespace())) {
            return;
        }
        if (path.startsWith("chests/village/") || path.equals("chests/spawn_bonus_chest")) {
            return;
        }
        LootTable table = event.getTable();
        if (table.getPool(POOL_NAME) != null) {
            return;
        }
        LootPool pool = LootPool.lootPool()
                .name(POOL_NAME)
                .setRolls(ConstantValue.exactly(1.0F))
                .add(EmptyLootItem.emptyItem().setWeight(EMPTY_WEIGHT))
                .add(LootItem.lootTableItem(BTMI.ENERGY_CORE.get()).setWeight(CORE_WEIGHT))
                .build();
        table.addPool(pool);
    }
}
