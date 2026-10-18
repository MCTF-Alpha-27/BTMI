package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.ProjectionPortal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class ProjectionPortalRenderer extends EntityRenderer<ProjectionPortal> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/projection_portal.png");

    private static final float RADIUS = 1.15F;
    private static final int CLOSE_TICKS = 10;

    public ProjectionPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ProjectionPortal entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float openness = openness(entity, partialTick);
        if (openness <= 0.02F) {
            return;
        }

        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees(entity.tickCount * 6.0F + partialTick * 6.0F - entity.openDelay() * 6.0F));
        pose.scale(RADIUS * openness, RADIUS * openness, 1.0F);

        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(consumer, matrix, -1.0F, -1.0F, 0.0F, 1.0F);
        vertex(consumer, matrix, 1.0F, -1.0F, 1.0F, 1.0F);
        vertex(consumer, matrix, 1.0F, 1.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, -1.0F, 1.0F, 0.0F, 0.0F);

        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float u, float v) {
        consumer.addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    private static float openness(ProjectionPortal entity, float partialTick) {
        // 相对开门时刻的年龄：还没轮到它开门时是负数
        float age = entity.tickCount + partialTick - entity.openDelay();
        if (age < 0.0F) {
            return 0.0F;
        }

        float open = Mth.clamp(age / ProjectionPortal.EMERGE_TICKS, 0.0F, 1.0F);
        open = 1.0F - (1.0F - open) * (1.0F - open);

        int end = ProjectionPortal.fireStartDelay() + ProjectionPortal.ACTIVE_TICKS;
        float close = age > end - CLOSE_TICKS
                ? Mth.clamp((end - age) / CLOSE_TICKS, 0.0F, 1.0F)
                : 1.0F;

        return open * close;
    }

    @Override
    public ResourceLocation getTextureLocation(ProjectionPortal entity) {
        return TEXTURE;
    }
}
