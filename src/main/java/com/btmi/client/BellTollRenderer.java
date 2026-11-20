package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.BellToll;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BellTollRenderer extends EntityRenderer<BellToll> {
    private static final ResourceLocation CLOCK =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/bell_toll_clock.png");
    private static final ResourceLocation BELL =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/bell_toll_bell.png");
    private static final ResourceLocation RING =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/bell_toll_ring.png");
    private static final ResourceLocation BEAM =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/knell_beam.png");

    public BellTollRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BellToll entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        if (age < BellToll.IMPACT) {
            renderCharge(pose, buffer, age);
        } else {
            renderImpact(pose, buffer, age - BellToll.IMPACT, (BellToll.LIFE - BellToll.IMPACT));
        }
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    private void renderCharge(PoseStack pose, MultiBufferSource buffer, float age) {
        float p = Mth.clamp(age / BellToll.IMPACT, 0.0F, 1.0F);
        int clockAlpha = (int) ((0.30F + 0.62F * p) * 255.0F);

        pose.pushPose();
        pose.translate(0.0D, 0.06D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-age * 1.6F));
        float clockScale = 3.0F + p * 2.1F;
        pose.scale(clockScale, clockScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(CLOCK)),
                pose.last().pose(), clockAlpha);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0D, 0.09D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 4.2F));
        float innerScale = 1.7F + p * 1.9F;
        pose.scale(innerScale, innerScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(RING)),
                pose.last().pose(), (int) ((0.25F + 0.55F * p) * 255.0F));
        pose.popPose();

        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(i * 90.0D + age * 5.0D);
            float x = (float) (Math.cos(angle) * 3.5D);
            float z = (float) (Math.sin(angle) * 3.5D);
            pose.pushPose();
            pose.translate(x, 0.0D, z);
            KnellChargeRenderer.beam(buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM)),
                    pose.last().pose(), 0.35F, 2.0F + p * 5.5F, (int) ((0.15F + 0.5F * p) * 255.0F));
            pose.popPose();
        }

        pose.pushPose();
        KnellChargeRenderer.beam(buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM)),
                pose.last().pose(), 0.7F, 13.0F, (int) ((0.18F + 0.35F * p) * 255.0F));
        pose.popPose();

        float sway = Mth.sin(age * 0.7F) * 0.10F;
        float hover = 7.4F - p * 3.5F;
        pose.pushPose();
        pose.translate(sway, hover, 0.0D);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float bellScale = 2.3F + p * 0.6F;
        pose.scale(bellScale, bellScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(BELL)),
                pose.last().pose(), (int) ((0.1F + 0.85F * p) * 255.0F));
        pose.popPose();
    }

    private void renderImpact(PoseStack pose, MultiBufferSource buffer, float age, float span) {
        float q = Mth.clamp(age / span, 0.0F, 1.0F);
        float fade = (1.0F - q) * (1.0F - q);

        pose.pushPose();
        pose.translate(0.0D, 0.07D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        float flashScale = 5.0F + q * 3.5F;
        pose.scale(flashScale, flashScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(CLOCK)),
                pose.last().pose(), (int) (Mth.clamp(1.0F - q * 2.6F, 0.0F, 1.0F) * 255.0F));
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0D, 0.10D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(q * 24.0F));
        float ringScale = 2.2F + q * 8.5F;
        pose.scale(ringScale, ringScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(RING)),
                pose.last().pose(), (int) (fade * 245.0F));
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0D, 0.12D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-q * 30.0F));
        float echoScale = 1.6F + q * 5.2F;
        pose.scale(echoScale, echoScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(RING)),
                pose.last().pose(), (int) (fade * 170.0F));
        pose.popPose();

        int beamAlpha = (int) (fade * 200.0F);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0D + q * 18.0D);
            float x = (float) (Math.cos(angle) * (1.2D + q * 1.6D));
            float z = (float) (Math.sin(angle) * (1.2D + q * 1.6D));
            pose.pushPose();
            pose.translate(x, 0.0D, z);
            KnellChargeRenderer.beam(buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM)),
                    pose.last().pose(), 0.30F, 9.0F * (1.0F - q * 0.75F), beamAlpha);
            pose.popPose();
        }

        float shakeX = Mth.sin(age * 2.7F) * 0.22F * (1.0F - q);
        float shakeZ = Mth.cos(age * 3.1F) * 0.20F * (1.0F - q);
        pose.pushPose();
        pose.translate(shakeX, 1.1F - q * 0.5F, shakeZ);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float bellScale = 1.9F + q * 0.7F;
        pose.scale(bellScale, bellScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(BELL)),
                pose.last().pose(), (int) (fade * 235.0F));
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BellToll entity) {
        return BELL;
    }
}
