package com.btmi.client;

import com.btmi.BTMI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class ModClientSetup {
    private ModClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 光球
        event.registerEntityRenderer(BTMI.LIGHT_ORB_ENTITY.get(), LightOrbRenderer::new);
        // 示亡号的悬浮实体
        event.registerEntityRenderer(BTMI.DEATH_SIGN_SENTINEL.get(), DeathSignSentinelRenderer::new);
        // 激光弹
        event.registerEntityRenderer(BTMI.LASER_BOLT_ENTITY.get(), LaserBoltRenderer::new);
        event.registerEntityRenderer(BTMI.LASER_BEAM.get(), LaserBeamRenderer::new);
        event.registerEntityRenderer(BTMI.RED_LINE.get(), RedLineRenderer::new);
        event.registerEntityRenderer(BTMI.SPEAR_BEAM.get(), SpearBeamRenderer::new);
        event.registerEntityRenderer(BTMI.GROUND_CRACK.get(), GroundCrackRenderer::new);
        event.registerEntityRenderer(BTMI.PROJECTION_PORTAL.get(), ProjectionPortalRenderer::new);
        event.registerEntityRenderer(BTMI.PROJECTION_SHOCKWAVE.get(), ProjectionShockwaveRenderer::new);
        event.registerEntityRenderer(BTMI.FLAME_BUTTERFLY.get(), FlameButterflyRenderer::new);
        event.registerEntityRenderer(BTMI.COLON_BULLET.get(),
                context -> new ColonBulletRenderer(context, 0.35F));
    }


    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(BTMI.COLON_MENU.get(), ColonScreen::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions dashExtensions = new IClientItemExtensions() {
            private DashStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    this.renderer = new DashStaffRenderer(
                            minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return this.renderer;
            }
        };
        IClientItemExtensions deathSignExtensions = new IClientItemExtensions() {
            private DeathSignRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    this.renderer = new DeathSignRenderer(
                            minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return this.renderer;
            }
        };
        event.registerItem(dashExtensions, BTMI.DASH.get());
        event.registerItem(deathSignExtensions, BTMI.DEATH_SIGN.get());
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
                    BTMI.DEATH_SIGN.get(),
                    ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "lit"),
                    (stack, level, entity, seed) -> Boolean.TRUE.equals(stack.get(BTMI.LIT.get())) ? 1.0F : 0.0F);
        });
    }
}
