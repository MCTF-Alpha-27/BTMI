package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.KnellStrike;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class KnellStrikeRenderer extends EntityRenderer<KnellStrike> {
    private static final ResourceLocation SIGIL =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/knell_sigil.png");
    private static final ResourceLocation CROSS =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/knell_cross.png");
    private static final ResourceLocation RING =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/knell_ring.png");
    private static final ResourceLocation BEAM =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/knell_beam.png");

    public KnellStrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(KnellStrike entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float t = Mth.clamp(age / KnellStrike.LIFE, 0.0F, 1.0F);
        float fade = (1.0F - t) * (1.0F - t);
        int alpha = (int) (fade * 255.0F);
        if (alpha <= 2) {
            return;
        }
        float scale = entity.scale();
        pose.pushPose();
        pose.scale(scale, scale, scale);

        pose.pushPose();
        pose.translate(0.0D, 0.08D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 3.5F));
        float crossScale = 2.0F + t * 8.0F;
        pose.scale(crossScale, crossScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(CROSS)),
                pose.last().pose(), alpha);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0D, 0.11D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-age * 5.0F));
        float ringScale = 3.0F + t * 9.0F;
        pose.scale(ringScale, ringScale, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(RING)),
                pose.last().pose(), (int) (fade * 210.0F));
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0D, 0.0D, 0.0D);
        KnellChargeRenderer.beam(buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM)),
                pose.last().pose(), 1.4F * (1.0F - t * 0.55F), 22.0F, alpha);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0D, 2.2D, 0.0D);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float cross = 2.0F + t * 3.5F;
        pose.scale(cross, cross, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(CROSS)),
                pose.last().pose(), (int) (fade * 230.0F));
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0D, 1.4D, 0.0D);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float flash = 6.0F * (1.0F - t);
        pose.scale(flash, flash, 1.0F);
        KnellChargeRenderer.flatQuad(buffer.getBuffer(RenderType.entityTranslucentEmissive(SIGIL)),
                pose.last().pose(), (int) ((1.0F - t) * (1.0F - t) * 220.0F));
        pose.popPose();

        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KnellStrike entity) {
        return SIGIL;
    }
}
