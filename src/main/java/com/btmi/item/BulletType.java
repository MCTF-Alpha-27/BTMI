package com.btmi.item;

import java.util.List;

import com.btmi.BTMI;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

public enum BulletType {
    HIGH_EXPLOSIVE("high_explosive"),
    INCENDIARY("incendiary"),
    PARALYSIS("paralysis"),
    PIERCING("piercing"),
    CRYO("cryo"),
    SEEKER("seeker");

    private final String id;

    BulletType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public String translationKey() {
        return "item.btmi." + this.id + "_bullet";
    }

    // 弹药类型直接由投射物携带的物品推导，因此客户端/服务端不需要额外同步枚举。
    public static BulletType of(ItemStack stack) {
        List<DeferredItem<Item>> items = BTMI.BULLET_ITEMS;
        for (int i = 0; i < items.size(); i++) {
            if (stack.is(items.get(i).get())) {
                return values()[i];
            }
        }
        return null;
    }

    public ItemStack createStack() {
        return new ItemStack(BTMI.BULLET_ITEMS.get(this.ordinal()).get());
    }
}
