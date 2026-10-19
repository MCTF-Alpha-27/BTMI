package com.btmi.client;

import com.btmi.BTMI;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class ModClientSetup {
    private ModClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 光球
        event.registerEntityRenderer(BTMI.LIGHT_ORB_ENTITY.get(),
                context -> new ThrownItemRenderer<>(context, 1.6F, true));
        // 示亡号的悬浮实体
        event.registerEntityRenderer(BTMI.OBELISK_SENTINEL.get(), ObeliskSentinelRenderer::new);
        // 激光弹
        event.registerEntityRenderer(BTMI.LASER_BOLT_ENTITY.get(), LaserBoltRenderer::new);
        event.registerEntityRenderer(BTMI.LASER_BEAM.get(), LaserBeamRenderer::new);
        event.registerEntityRenderer(BTMI.PROJECTION_PORTAL.get(), ProjectionPortalRenderer::new);
        event.registerEntityRenderer(BTMI.FLAME_BUTTERFLY.get(), FlameButterflyRenderer::new);
        event.registerEntityRenderer(BTMI.COLON_BULLET.get(),
                context -> new ColonBulletRenderer(context, 0.35F));
    }


    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(BTMI.COLON_MENU.get(), ColonScreen::new);
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    BTMI.DASH.get(),
                    ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "unsheathed"),
                    (stack, level, entity, seed) -> Boolean.TRUE.equals(stack.get(BTMI.UNSHEATHED.get())) ? 1.0F : 0.0F);
            ItemProperties.register(
                    BTMI.OBELISK.get(),
                    ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "lit"),
                    (stack, level, entity, seed) -> Boolean.TRUE.equals(stack.get(BTMI.LIT.get())) ? 1.0F : 0.0F);
        });
    }
}
