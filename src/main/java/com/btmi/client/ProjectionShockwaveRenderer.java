package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.entity.ProjectionShockwave;
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

public class ProjectionShockwaveRenderer extends EntityRenderer<ProjectionShockwave> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/projection_shockwave.png");

    public ProjectionShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ProjectionShockwave entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float t = Mth.clamp(age / ProjectionShockwave.LIFE, 0.0F, 1.0F);
        float scale = entity.size() * (0.55F + t * 0.95F);
        float alpha = (1.0F - t) * (1.0F - t) * 0.9F;
        if (alpha <= 0.01F) {
            return;
        }

        pose.pushPose();
        if (entity.horizontal()) {
            pose.translate(0.0F, 0.03F, 0.0F);
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else {
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        }
        pose.scale(scale, scale, 1.0F);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f matrix = pose.last().pose();
        int a = (int) (alpha * 255.0F);
        vertex(consumer, matrix, -1.0F, -1.0F, 0.0F, 1.0F, a);
        vertex(consumer, matrix, 1.0F, -1.0F, 1.0F, 1.0F, a);
        vertex(consumer, matrix, 1.0F, 1.0F, 1.0F, 0.0F, a);
        vertex(consumer, matrix, -1.0F, 1.0F, 0.0F, 0.0F, a);
        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y,
                               float u, float v, int alpha) {
        consumer.addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ProjectionShockwave entity) {
        return TEXTURE;
    }
}
