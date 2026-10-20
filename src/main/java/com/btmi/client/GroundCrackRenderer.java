package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.GroundCrack;
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

public class GroundCrackRenderer extends EntityRenderer<GroundCrack> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/ground_crack.png");

    private static final float WIDTH = 1.8F;

    public GroundCrackRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GroundCrack entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        if (age >= GroundCrack.LIFE_TICKS) {
            return;
        }
        float fade;
        if (age < 3.0F) {
            fade = age / 3.0F;
        } else if (age > GroundCrack.LIFE_TICKS - 8.0F) {
            fade = (GroundCrack.LIFE_TICKS - age) / 8.0F;
        } else {
            fade = 1.0F;
        }
        fade = Mth.clamp(fade, 0.0F, 1.0F);
        if (fade <= 0.01F) {
            return;
        }

        Vec3 direction = entity.getViewVector(partialTick);
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        if (flat.lengthSqr() < 1.0E-6D) {
            return;
        }
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x).scale(WIDTH * 0.5D);
        float length = entity.length();

        Vec3 a = side;
        Vec3 b = side.scale(-1.0D);
        Vec3 c = flat.scale(length).subtract(side);
        Vec3 d = flat.scale(length).add(side);

        pose.pushPose();
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

        vertex(consumer, matrix, a, 0.0F, 0.0F, fade);
        vertex(consumer, matrix, b, 0.0F, 1.0F, fade);
        vertex(consumer, matrix, c, 1.0F, 1.0F, fade);
        vertex(consumer, matrix, d, 1.0F, 0.0F, fade);

        vertex(consumer, matrix, d, 1.0F, 0.0F, fade);
        vertex(consumer, matrix, c, 1.0F, 1.0F, fade);
        vertex(consumer, matrix, b, 0.0F, 1.0F, fade);
        vertex(consumer, matrix, a, 0.0F, 0.0F, fade);

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
    public ResourceLocation getTextureLocation(GroundCrack entity) {
        return TEXTURE;
    }
}
