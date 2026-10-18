package com.btmi.client;

import com.btmi.entity.ColonBullet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 子弹渲染器。
 */
public class ColonBulletRenderer extends EntityRenderer<ColonBullet> {
    private static final ResourceLocation PLACEHOLDER =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private final ItemRenderer itemRenderer;
    private final float scale;

    public ColonBulletRenderer(EntityRendererProvider.Context context, float scale) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.scale = scale;
    }

    @Override
    public void render(ColonBullet entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-8D) {
            motion = entity.getViewVector(1.0F);
        }
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();

        // 世界方向 -> 相机空间：+X 屏幕右，+Y 屏幕上，-Z 相机视线方向
        Vector3f velocity = new Vector3f((float) motion.x, (float) motion.y, (float) motion.z);
        velocity.rotate(camera.conjugate(new Quaternionf()));

        float roll = 0.0F;
        if (velocity.x() * velocity.x() + velocity.y() * velocity.y() > 1.0E-4F) {
            roll = (float) Math.atan2(-velocity.x(), velocity.y());
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(camera);
        poseStack.mulPose(Axis.ZP.rotation(roll));
        poseStack.scale(this.scale, this.scale, this.scale);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ColonBullet entity) {
        return PLACEHOLDER;
    }
}
