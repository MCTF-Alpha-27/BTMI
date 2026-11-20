package com.btmi.belltower;

import com.btmi.BTMI;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class BellTowerNameFormat {
    private BellTowerNameFormat() {
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        BellTowerClass towerClass = BellTowerClasses.get(event.getEntity());
        if (!towerClass.isChosen()) {
            return;
        }
        Component prefix = Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.RED))
                .append(Component.translatable(towerClass.translationKey()).withStyle(ChatFormatting.RED))
                .append(Component.literal("] ").withStyle(ChatFormatting.RED));
        event.setDisplayname(Component.empty().append(prefix).append(event.getUsername()));
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        BellTowerClass towerClass = BellTowerClasses.get(event.getEntity());
        if (!towerClass.isChosen()) {
            return;
        }
        Component prefix = Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.RED))
                .append(Component.translatable(towerClass.translationKey()).withStyle(ChatFormatting.RED))
                .append(Component.literal("] ").withStyle(ChatFormatting.RED));
        Component name = Component.literal(event.getEntity().getGameProfile().getName());
        event.setDisplayName(Component.empty().append(prefix).append(name));
    }
}
