package com.btmi.client;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class DeathSignItemRenderer extends RenderItemBase {
    public DeathSignItemRenderer(JsonElement json) {
        super(json);
    }

    @Override
    public void renderItemInHand(ItemStack stack, LivingEntityPatch<?> entitypatch, InteractionHand hand,
                                 OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack,
                                 int packedLight, float partialTicks) {
        OpenMatrix4f modelMatrix = this.getCorrectionMatrix(entitypatch, hand, poses);
        boolean left = hand == InteractionHand.OFF_HAND;

        poseStack.pushPose();
        MathUtils.mulStack(poseStack, modelMatrix);
        poseStack.translate(left ? -0.032F : 0.032F, -0.489F, -0.098F);
        poseStack.translate(0.0F, 4.0F / 16.0F, 0.5F / 16.0F);
        poseStack.mulPose(new Quaternionf().rotationXYZ(
                0.0F,
                (float) Math.toRadians(left ? 90.0F : -90.0F),
                (float) Math.toRadians(left ? -25.0F : 25.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        DeathSignRenderer.renderCross(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY,
                DeathSignRenderer.isLit(stack));
        poseStack.popPose();
    }
}
