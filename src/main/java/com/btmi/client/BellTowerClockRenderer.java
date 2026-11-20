package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.BellTowerClock;
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

public class BellTowerClockRenderer extends EntityRenderer<BellTowerClock> {
    private static final ResourceLocation FACE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/bell_tower_clock.png");
    private static final ResourceLocation HAND =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/bell_tower_clock_hand.png");
    private static final float SIZE = 42.0F;

    public BellTowerClockRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BellTowerClock entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float t = Mth.clamp(age / BellTowerClock.LIFE, 0.0F, 1.0F);
        float fade = Math.min(1.0F, age / 20.0F) * Math.min(1.0F, (BellTowerClock.LIFE - age) / 10.0F);
        int alpha = (int) (Mth.clamp(fade, 0.0F, 1.0F) * 255.0F);
        if (alpha <= 2) {
            return;
        }

        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.scale(SIZE, SIZE, 1.0F);

        quad(buffer.getBuffer(RenderType.entityTranslucentEmissive(FACE)), pose.last().pose(), alpha);
        drawHand(pose, buffer, -240.0F, 0.78F, 0.042F, alpha);
        drawHand(pose, buffer, -290.0F, 0.52F, 0.06F, alpha);
        int tickStep = Math.min(10, entity.tickCount / 20);
        drawHand(pose, buffer, -300.0F - tickStep * 6.0F, 0.9F, 0.018F, alpha, 255, 70, 70);
        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    private static void drawHand(PoseStack pose, MultiBufferSource buffer, float angle, float length,
                                 float width, int alpha) {
        drawHand(pose, buffer, angle, length, width, alpha, 255, 235, 190);
    }

    private static void drawHand(PoseStack pose, MultiBufferSource buffer, float angle, float length,
                                 float width, int alpha, int red, int green, int blue) {
        pose.pushPose();
        pose.translate(0.0D, 0.0D, 0.02D);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(HAND));
        Matrix4f matrix = pose.last().pose();
        vertex(consumer, matrix, -width, 0.0F, 0.0F, 0.0F, 1.0F, alpha, red, green, blue);
        vertex(consumer, matrix, width, 0.0F, 0.0F, 1.0F, 1.0F, alpha, red, green, blue);
        vertex(consumer, matrix, width * 0.22F, length, 0.0F, 1.0F, 0.0F, alpha, red, green, blue);
        vertex(consumer, matrix, -width * 0.22F, length, 0.0F, 0.0F, 0.0F, alpha, red, green, blue);
        pose.popPose();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, int alpha) {
        vertex(consumer, matrix, -1.0F, -1.0F, 0.0F, 0.0F, 1.0F, alpha);
        vertex(consumer, matrix, 1.0F, -1.0F, 0.0F, 1.0F, 1.0F, alpha);
        vertex(consumer, matrix, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, alpha);
        vertex(consumer, matrix, -1.0F, 1.0F, 0.0F, 0.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                               float u, float v, int alpha) {
        vertex(consumer, matrix, x, y, z, u, v, alpha, 255, 235, 190);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                               float u, float v, int alpha, int red, int green, int blue) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(BellTowerClock entity) {
        return FACE;
    }
}
