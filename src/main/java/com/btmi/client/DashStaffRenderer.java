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

public final class DashStaffRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/dash_staff.png");
    private static final ResourceLocation ICON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/dash_icon.png");

    private static final int SIDES = 12;
    private static final float BOTTOM = -11.0F / 16.0F;
    private static final float TOP = 11.0F / 16.0F;
    private static final float RADIUS = 1.1F / 16.0F;

    private static final float SIDE_U_MAX = 0.5F;
    private static final float CAP_U = 0.5F;
    private static final float CAP_V = 0.0F;
    private static final float CAP_SIZE = 0.25F;

    public DashStaffRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        if (context == ItemDisplayContext.GUI) {
            renderIcon(poseStack, buffer, light, overlay);
            return;
        }

        if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }

        renderStaffMesh(poseStack, buffer, light, overlay);
    }

    public static void renderStaffMesh(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < SIDES; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / SIDES);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / SIDES);
            float x0 = (float) Math.cos(a0) * RADIUS;
            float z0 = (float) Math.sin(a0) * RADIUS;
            float x1 = (float) Math.cos(a1) * RADIUS;
            float z1 = (float) Math.sin(a1) * RADIUS;

            float u0 = SIDE_U_MAX * i / SIDES;
            float u1 = SIDE_U_MAX * (i + 1) / SIDES;
            float nx = (float) Math.cos((a0 + a1) * 0.5D);
            float nz = (float) Math.sin((a0 + a1) * 0.5D);

            vertex(consumer, pose, x0, BOTTOM, z0, u0, 1.0F, nx, 0.0F, nz, light, overlay);
            vertex(consumer, pose, x1, BOTTOM, z1, u1, 1.0F, nx, 0.0F, nz, light, overlay);
            vertex(consumer, pose, x1, TOP, z1, u1, 0.0F, nx, 0.0F, nz, light, overlay);
            vertex(consumer, pose, x0, TOP, z0, u0, 0.0F, nx, 0.0F, nz, light, overlay);
        }

        cap(consumer, pose, BOTTOM, -1.0F, light, overlay);
        cap(consumer, pose, TOP, 1.0F, light, overlay);
    }

    private static void renderIcon(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ICON_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        float z = 0.5F;
        vertex(consumer, pose, 0.0F, 0.0F, z, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, light, overlay);
        vertex(consumer, pose, 1.0F, 0.0F, z, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, light, overlay);
        vertex(consumer, pose, 1.0F, 1.0F, z, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, light, overlay);
        vertex(consumer, pose, 0.0F, 1.0F, z, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, light, overlay);
    }

    private static void cap(VertexConsumer consumer, PoseStack.Pose pose, float y, float normalY,
                            int light, int overlay) {
        float centerU = CAP_U + CAP_SIZE * 0.5F;
        float centerV = CAP_V + CAP_SIZE * 0.5F;
        float uvRadius = CAP_SIZE * 0.5F;

        for (int i = 0; i < SIDES; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / SIDES);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / SIDES);
            float x0 = (float) Math.cos(a0) * RADIUS;
            float z0 = (float) Math.sin(a0) * RADIUS;
            float x1 = (float) Math.cos(a1) * RADIUS;
            float z1 = (float) Math.sin(a1) * RADIUS;

            float u0 = centerU + (float) Math.cos(a0) * uvRadius;
            float v0 = centerV + (float) Math.sin(a0) * uvRadius;
            float u1 = centerU + (float) Math.cos(a1) * uvRadius;
            float v1 = centerV + (float) Math.sin(a1) * uvRadius;

            vertex(consumer, pose, 0.0F, y, 0.0F, centerU, centerV, 0.0F, normalY, 0.0F, light, overlay);
            vertex(consumer, pose, x0, y, z0, u0, v0, 0.0F, normalY, 0.0F, light, overlay);
            vertex(consumer, pose, x1, y, z1, u1, v1, 0.0F, normalY, 0.0F, light, overlay);
        }
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
