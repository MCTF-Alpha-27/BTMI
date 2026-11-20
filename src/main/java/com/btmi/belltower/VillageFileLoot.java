package com.btmi.belltower;

import com.btmi.BTMI;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class VillageFileLoot {
    private static final String POOL_NAME = "btmi_chairman_file";

    private VillageFileLoot() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (!"minecraft".equals(name.getNamespace()) || !name.getPath().startsWith("chests/village/")) {
            return;
        }
        LootTable table = event.getTable();
        if (table.getPool(POOL_NAME) != null) {
            return;
        }
        LootPool pool = LootPool.lootPool()
                .name(POOL_NAME)
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(BTMI.CHAIRMAN_FILE.get()))
                .build();
        table.addPool(pool);
    }
}
