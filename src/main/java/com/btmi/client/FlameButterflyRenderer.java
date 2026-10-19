package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.FlameButterfly;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class FlameButterflyRenderer extends EntityRenderer<FlameButterfly> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/flame_butterfly.png");

    private static final float SIZE = 0.55F;

    public FlameButterflyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlameButterfly entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float flap = 0.62F + 0.38F * Mth.abs(Mth.sin((entity.tickCount + partialTick) * 0.55F));
        pose.scale(SIZE * flap, SIZE, SIZE);

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

    @Override
    public ResourceLocation getTextureLocation(FlameButterfly entity) {
        return TEXTURE;
    }
}
