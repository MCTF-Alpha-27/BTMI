package com.btmi;

import com.btmi.client.DashStaffItemRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import yesman.epicfight.api.client.event.EpicFightClientEventHooks;

@Mod(value = BTMI.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public class BTMIClient {
    public BTMIClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        EpicFightClientEventHooks.Registry.PATCHED_ITEM.registerEvent(registered ->
                registered.addItemRenderer(
                        ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "dash"),
                        DashStaffItemRenderer::new));
        BTMI.LOGGER.info("BTMI 客户端初始化完成，你好 {}！", Minecraft.getInstance().getUser().getName());
    }
}
