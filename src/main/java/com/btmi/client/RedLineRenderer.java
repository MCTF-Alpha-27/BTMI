package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.RedLine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RedLineRenderer extends EntityRenderer<RedLine> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/red_line.png");

    private static final float START_WIDTH = 0.32F;
    private static final float END_WIDTH = 0.05F;

    public RedLineRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RedLine entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        if (age >= RedLine.LIFE_TICKS) {
            return;
        }
        float progress = Mth.clamp(age / RedLine.GROW_TICKS, 0.0F, 1.0F);
        float length = entity.length() * progress;
        if (length < 0.03F) {
            return;
        }

        Vec3 direction = entity.getViewVector(partialTick);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();

        Vec3 helper = Math.abs(direction.y) > 0.98D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 sideA = direction.cross(helper).normalize();
        Vec3 sideB = direction.cross(sideA).normalize();

        float fade = age <= RedLine.GROW_TICKS
                ? 1.0F
                : Mth.clamp(1.0F - (age - RedLine.GROW_TICKS) / (RedLine.LIFE_TICKS - RedLine.GROW_TICKS), 0.0F, 1.0F);
        if (fade <= 0.01F) {
            return;
        }

        float startWidth = Mth.lerp(progress, START_WIDTH, END_WIDTH);
        float midWidth = startWidth * 0.8F;
        float endWidth = startWidth * 0.15F;

        Vec3 start = Vec3.ZERO;
        Vec3 end = direction.scale(length);

        pose.pushPose();
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

        for (int ribbon = 0; ribbon < 3; ribbon++) {
            double roll = Math.toRadians(60.0D * ribbon);
            Vec3 side = sideA.scale(Math.cos(roll)).add(sideB.scale(Math.sin(roll))).normalize();
            Vec3 mid = direction.scale(length * 0.5D).add(side.scale((1.0D - progress) * 0.22D));
            quad(consumer, matrix, side, start, mid, startWidth, midWidth, 0.0F, 0.5F, fade);
            quad(consumer, matrix, side, mid, end, midWidth, endWidth, 0.5F, 1.0F, fade);
        }

        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Vec3 side, Vec3 from, Vec3 to,
                             float fromWidth, float toWidth, float u0, float u1, float alpha) {
        Vec3 a = from.add(side.scale(fromWidth));
        Vec3 b = from.subtract(side.scale(fromWidth));
        Vec3 c = to.subtract(side.scale(toWidth));
        Vec3 d = to.add(side.scale(toWidth));

        vertex(consumer, matrix, a, u0, 0.0F, alpha);
        vertex(consumer, matrix, b, u0, 1.0F, alpha);
        vertex(consumer, matrix, c, u1, 1.0F, alpha);
        vertex(consumer, matrix, d, u1, 0.0F, alpha);

        vertex(consumer, matrix, d, u1, 0.0F, alpha);
        vertex(consumer, matrix, c, u1, 1.0F, alpha);
        vertex(consumer, matrix, b, u0, 1.0F, alpha);
        vertex(consumer, matrix, a, u0, 0.0F, alpha);
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
    public ResourceLocation getTextureLocation(RedLine entity) {
        return TEXTURE;
    }
}
