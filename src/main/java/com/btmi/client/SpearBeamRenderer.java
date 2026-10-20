package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.SpearBeam;
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

public class SpearBeamRenderer extends EntityRenderer<SpearBeam> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/spear_beam.png");

    private static final float START_WIDTH = 0.55F;
    private static final float END_WIDTH = 0.06F;
    private static final int SEGMENTS = 4;
    private static final int RIBBONS = 3;
    private static final float HOLD_END = 38.0F;

    public SpearBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpearBeam entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        if (entity.tickCount == 0) {
            BTMI.LOGGER.info("[dual] spear beam render length={} age={}", entity.length(), age);
        }
        if (age >= SpearBeam.LIFE_TICKS) {
            return;
        }
        float progress = Mth.clamp(age / SpearBeam.GROW_TICKS, 0.0F, 1.0F);
        float length = entity.length() * progress;
        if (length < 0.05F) {
            return;
        }

        Vec3 direction = entity.getViewVector(partialTick);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();

        float retract = age <= HOLD_END
                ? 0.0F
                : Mth.clamp((age - HOLD_END) / (SpearBeam.LIFE_TICKS - HOLD_END), 0.0F, 1.0F);
        float fade = age <= HOLD_END
                ? 1.0F
                : Mth.clamp(1.0F - retract * 0.7F, 0.0F, 1.0F);

        Vec3 helper = Math.abs(direction.y) > 0.98D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 sideA = direction.cross(helper).normalize();
        Vec3 sideB = direction.cross(sideA).normalize();

        pose.pushPose();
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

        for (int ribbon = 0; ribbon < RIBBONS; ribbon++) {
            double roll = Math.toRadians(60.0D * ribbon);
            Vec3 side = sideA.scale(Math.cos(roll)).add(sideB.scale(Math.sin(roll))).normalize();

            for (int i = 0; i < SEGMENTS; i++) {
                float t0 = retract + (1.0F - retract) * (i / (float) SEGMENTS);
                float t1 = retract + (1.0F - retract) * ((i + 1) / (float) SEGMENTS);
                Vec3 from = direction.scale(length * t0);
                Vec3 to = direction.scale(length * t1);
                float width0 = Mth.lerp(t0, START_WIDTH, END_WIDTH);
                float width1 = Mth.lerp(t1, START_WIDTH, END_WIDTH);
                if (i == 0 && retract > 0.0F) {
                    width0 = 0.02F;
                }
                quad(consumer, matrix, side, from, to, width0, width1, t0, t1, fade);
            }
        }

        long seed = entity.getId() * 31L + entity.tickCount / 2L;
        for (int arc = 0; arc < 3; arc++) {
            lightning(consumer, matrix, direction, sideA, sideB, length, retract, fade, age,
                    (float) (arc * Math.PI * 2.0D / 3.0D), seed + arc * 7919L);
        }

        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    private static void lightning(VertexConsumer consumer, Matrix4f matrix, Vec3 direction,
                                   Vec3 sideA, Vec3 sideB, float length, float retract,
                                   float alpha, float time, float arcOffset, long seed) {
        long state = seed * 6364136223846793005L + 1442695040888963407L;
        int points = 26;
        double turns = 2.3D;
        double phase = time * 0.85D + arcOffset;
        Vec3 previous = null;
        for (int i = 0; i <= points; i++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            float t = retract + (1.0F - retract) * (i / (float) points);
            double radius = Mth.lerp(t, START_WIDTH, END_WIDTH) * 0.9D
                    + 0.035D + ((state >>> 8 & 31L) / 31.0D) * 0.05D;
            double angle = phase + t * turns * Math.PI * 2.0D
                    + ((state >>> 16 & 63L) / 63.0D - 0.5D) * 0.45D;
            Vec3 point = direction.scale(length * t)
                    .add(sideA.scale(Math.cos(angle) * radius))
                    .add(sideB.scale(Math.sin(angle) * radius));
            if (previous != null) {
                Vec3 segment = point.subtract(previous);
                if (segment.lengthSqr() > 1.0E-8D) {
                    Vec3 segSide = segment.normalize().cross(sideA);
                    if (segSide.lengthSqr() < 1.0E-6D) {
                        segSide = sideB;
                    }
                    quad(consumer, matrix, segSide.normalize(), previous, point,
                            0.02F, 0.02F, 0.35F, 0.65F, alpha);
                }
            }
            previous = point;
        }
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
    public ResourceLocation getTextureLocation(SpearBeam entity) {
        return TEXTURE;
    }
}
