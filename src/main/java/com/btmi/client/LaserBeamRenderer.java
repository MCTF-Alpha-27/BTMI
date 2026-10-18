package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.LaserBeam;
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
import net.minecraft.world.phys.Vec3;

/**
 * 激光光束渲染：沿射线画 3 个滚转不同的发光面片，形成有体积感的光柱。
 */
public class LaserBeamRenderer extends EntityRenderer<LaserBeam> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/laser_beam.png");

    private static final float BASE_WIDTH = 0.11F;

    public LaserBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LaserBeam entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float length = entity.length();
        if (length <= 0.05F) {
            return;
        }

        Vec3 direction = entity.getViewVector(partialTick);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();

        float age = entity.tickCount + partialTick;
        float fade = Mth.clamp(1.0F - age / (LaserBeam.LIFE_TICKS + 1.0F), 0.0F, 1.0F);
        if (fade <= 0.01F) {
            return;
        }

        // 与光束垂直的两个基向量
        Vec3 helper = Math.abs(direction.y) > 0.98D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 sideA = direction.cross(helper).normalize();
        Vec3 sideB = direction.cross(sideA).normalize();

        float width = BASE_WIDTH * (0.6F + 0.4F * fade);
        Vec3 end = direction.scale(length);

        pose.pushPose();
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

        for (int i = 0; i < 3; i++) {
            double roll = Math.toRadians(60.0D * i);
            Vec3 side = sideA.scale(Math.cos(roll)).add(sideB.scale(Math.sin(roll))).normalize();
            Vec3 offset = side.scale(width);

            Vec3 p0 = offset.scale(-1.0D);
            Vec3 p1 = offset;
            Vec3 p2 = end.add(offset);
            Vec3 p3 = end.subtract(offset);

            vertex(consumer, matrix, p0, 0.0F, 0.0F, fade);
            vertex(consumer, matrix, p1, 1.0F, 0.0F, fade);
            vertex(consumer, matrix, p2, 1.0F, 1.0F, fade);
            vertex(consumer, matrix, p3, 0.0F, 1.0F, fade);

            // 背面
            vertex(consumer, matrix, p3, 0.0F, 1.0F, fade);
            vertex(consumer, matrix, p2, 1.0F, 1.0F, fade);
            vertex(consumer, matrix, p1, 1.0F, 0.0F, fade);
            vertex(consumer, matrix, p0, 0.0F, 0.0F, fade);
        }

        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position,
                               float u, float v, float alpha) {
        consumer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, (int) (alpha * 255.0F))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LaserBeam entity) {
        return TEXTURE;
    }
}
