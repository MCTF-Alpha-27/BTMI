package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.KnellCharge;
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

public class KnellChargeRenderer extends EntityRenderer<KnellCharge> {
    private static final ResourceLocation SIGIL =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/knell_sigil.png");
    private static final ResourceLocation CROSS =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/knell_cross.png");
    private static final ResourceLocation BEAM =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/knell_beam.png");

    public KnellChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(KnellCharge entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float progress = Mth.clamp(age / KnellCharge.LIFE, 0.0F, 1.0F);
        int alpha = (int) ((0.35F + 0.65F * progress) * 255.0F);

        pose.pushPose();
        pose.translate(0.0D, 0.07D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 5.0F));
        float sigil = 2.2F + progress * 2.4F;
        pose.scale(sigil, sigil, 1.0F);
        flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(SIGIL)), pose.last().pose(), alpha);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0D, 13.0D - progress * 3.0F, 0.0D);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float cross = 1.0F + progress * 1.8F;
        pose.scale(cross, cross, 1.0F);
        flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(CROSS)), pose.last().pose(), alpha);
        pose.popPose();

        pose.pushPose();
        beam(buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM)), pose.last().pose(),
                0.7F + progress * 0.35F, 18.0F, (int) ((0.35F + 0.45F * progress) * 255.0F));
        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    static void flatQuad(VertexConsumer consumer, Matrix4f matrix, int alpha) {
        consumer.addVertex(matrix, -1.0F, -1.0F, 0.0F)
                .setColor(255, 240, 210, alpha)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, 1.0F, -1.0F, 0.0F)
                .setColor(255, 240, 210, alpha)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, 1.0F, 1.0F, 0.0F)
                .setColor(255, 240, 210, alpha)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
        consumer.addVertex(matrix, -1.0F, 1.0F, 0.0F)
                .setColor(255, 240, 210, alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    static void beam(VertexConsumer consumer, Matrix4f matrix, float width, float height, int alpha) {
        quad(consumer, matrix, -width, 0.0F, 0.0F, width, 0.0F, 0.0F,
                width, height, 0.0F, -width, height, 0.0F, alpha);
        quad(consumer, matrix, 0.0F, 0.0F, -width, 0.0F, 0.0F, width,
                0.0F, height, width, 0.0F, height, -width, alpha);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int alpha) {
        vertex(consumer, matrix, x0, y0, z0, 0.0F, 1.0F, alpha);
        vertex(consumer, matrix, x1, y1, z1, 1.0F, 1.0F, alpha);
        vertex(consumer, matrix, x2, y2, z2, 1.0F, 0.0F, alpha);
        vertex(consumer, matrix, x3, y3, z3, 0.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z, float u, float v, int alpha) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 240, 210, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(KnellCharge entity) {
        return SIGIL;
    }
}
