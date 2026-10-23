package com.btmi.client;

import com.btmi.BTMI;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 3D cross mesh for the death_sign. The body uses a dedicated atlas instead of
 * vanilla cuboid models, so the shape and glow state stay under our control.
 */
public final class DeathSignRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/death_sign_model.png");
    private static final ResourceLocation ICON =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/death_sign_icon.png");
    private static final ResourceLocation LIT_ICON =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/death_sign_lit_icon.png");

    private static final float DARK_U = 0.0F;
    private static final float GOLD_U = 0.25F;
    private static final float GEM_U = 0.5F;
    private static final float LIT_U = 0.75F;
    private static final float REGION = 0.25F;

    public DeathSignRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        if (context == ItemDisplayContext.GUI) {
            renderIcon(poseStack, buffer, light, overlay, isLit(stack) ? LIT_ICON : ICON);
            return;
        }
        if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        if (!context.firstPerson()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
        renderCross(poseStack, buffer, light, overlay, isLit(stack));
        poseStack.popPose();
    }

    public static boolean isLit(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(BTMI.LIT.get()));
    }

    public static void renderCross(PoseStack poseStack, MultiBufferSource buffer,
                                   int light, int overlay, boolean lit) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        float bodyU = lit ? LIT_U : DARK_U;

        // vertical shaft
        box(consumer, pose, -1.2F, 0.0F, -0.7F, 1.2F, 22.0F, 0.7F, bodyU, light, overlay);
        // gold strips along the shaft
        box(consumer, pose, -1.45F, 0.0F, -0.75F, -1.2F, 22.0F, 0.75F, GOLD_U, light, overlay);
        box(consumer, pose, 1.2F, 0.0F, -0.75F, 1.45F, 22.0F, 0.75F, GOLD_U, light, overlay);
        // bottom and top bands
        box(consumer, pose, -1.65F, -0.8F, -0.9F, 1.65F, 0.2F, 0.9F, GOLD_U, light, overlay);
        box(consumer, pose, -1.65F, 21.8F, -0.9F, 1.65F, 22.6F, 0.9F, GOLD_U, light, overlay);
        // crossbar
        box(consumer, pose, -6.0F, 14.0F, -0.7F, 6.0F, 17.0F, 0.7F, bodyU, light, overlay);
        // crossbar gold edges
        box(consumer, pose, -6.3F, 16.8F, -0.75F, 6.3F, 17.25F, 0.75F, GOLD_U, light, overlay);
        box(consumer, pose, -6.3F, 13.75F, -0.75F, 6.3F, 14.2F, 0.75F, GOLD_U, light, overlay);
        box(consumer, pose, -6.5F, 13.7F, -0.8F, -6.0F, 17.3F, 0.8F, GOLD_U, light, overlay);
        box(consumer, pose, 6.0F, 13.7F, -0.8F, 6.5F, 17.3F, 0.8F, GOLD_U, light, overlay);
        // center gem
        box(consumer, pose, -1.7F, 14.4F, -0.95F, 1.7F, 16.6F, 0.95F,
                lit ? LIT_U : GEM_U, light, overlay);
        // top finial
        box(consumer, pose, -1.4F, 22.6F, -0.8F, 1.4F, 24.0F, 0.8F,
                lit ? LIT_U : GOLD_U, light, overlay);
    }

    private static void box(VertexConsumer consumer, PoseStack.Pose pose,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float u, int light, int overlay) {
        float u0 = u;
        float u1 = u + REGION;
        float v0 = 0.0F;
        float v1 = REGION;

        float ax0 = x0 / 16.0F, ay0 = y0 / 16.0F, az0 = z0 / 16.0F;
        float ax1 = x1 / 16.0F, ay1 = y1 / 16.0F, az1 = z1 / 16.0F;

        // north (-Z)
        quad(consumer, pose, ax0, ay0, az0, ax1, ay0, az0, ax1, ay1, az0, ax0, ay1, az0,
                u0, v0, u1, v1, 0.0F, 0.0F, -1.0F, light, overlay);
        // south (+Z)
        quad(consumer, pose, ax1, ay0, az1, ax0, ay0, az1, ax0, ay1, az1, ax1, ay1, az1,
                u0, v0, u1, v1, 0.0F, 0.0F, 1.0F, light, overlay);
        // west (-X)
        quad(consumer, pose, ax0, ay0, az1, ax0, ay0, az0, ax0, ay1, az0, ax0, ay1, az1,
                u0, v0, u1, v1, -1.0F, 0.0F, 0.0F, light, overlay);
        // east (+X)
        quad(consumer, pose, ax1, ay0, az0, ax1, ay0, az1, ax1, ay1, az1, ax1, ay1, az0,
                u0, v0, u1, v1, 1.0F, 0.0F, 0.0F, light, overlay);
        // down (-Y)
        quad(consumer, pose, ax0, ay0, az0, ax1, ay0, az0, ax1, ay0, az1, ax0, ay0, az1,
                u0, v0, u1, v1, 0.0F, -1.0F, 0.0F, light, overlay);
        // up (+Y)
        quad(consumer, pose, ax0, ay1, az1, ax1, ay1, az1, ax1, ay1, az0, ax0, ay1, az0,
                u0, v0, u1, v1, 0.0F, 1.0F, 0.0F, light, overlay);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float u0, float v0, float u1, float v1,
                             float nx, float ny, float nz, int light, int overlay) {
        vertex(consumer, pose, x0, y0, z0, u0, v1, nx, ny, nz, light, overlay);
        vertex(consumer, pose, x1, y1, z1, u1, v1, nx, ny, nz, light, overlay);
        vertex(consumer, pose, x2, y2, z2, u1, v0, nx, ny, nz, light, overlay);
        vertex(consumer, pose, x3, y3, z3, u0, v0, nx, ny, nz, light, overlay);
    }

    private static void renderIcon(PoseStack poseStack, MultiBufferSource buffer, int light,
                                   int overlay, ResourceLocation texture) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        float z = 0.5F;
        vertex(consumer, pose, 0.0F, 0.0F, z, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, light, overlay);
        vertex(consumer, pose, 1.0F, 0.0F, z, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, light, overlay);
        vertex(consumer, pose, 1.0F, 1.0F, z, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, light, overlay);
        vertex(consumer, pose, 0.0F, 1.0F, z, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, light, overlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light, int overlay) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
