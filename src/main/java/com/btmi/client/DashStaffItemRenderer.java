package com.btmi.client;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Draws the dash staff in the patched item layer, using the spear hand pose but
 * replacing the item model with our own cylindrical mesh.
 */
public class DashStaffItemRenderer extends RenderItemBase {
    public DashStaffItemRenderer(JsonElement json) {
        super(json);
    }

    @Override
    public void renderItemInHand(ItemStack stack, LivingEntityPatch<?> entitypatch, InteractionHand hand,
                                 OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack,
                                 int packedLight, float partialTicks) {
        OpenMatrix4f modelMatrix = this.getCorrectionMatrix(entitypatch, hand, poses);
        boolean left = hand == InteractionHand.OFF_HAND;

        poseStack.pushPose();
        poseStack.translate(-0.14F, 0.24F, 0.0F);
        MathUtils.mulStack(poseStack, modelMatrix);
        poseStack.translate(0.0F, 4.0F / 16.0F, 0.5F / 16.0F);
        poseStack.mulPose(new Quaternionf().rotationXYZ(
                0.0F,
                (float) Math.toRadians(left ? 90.0F : -90.0F),
                (float) Math.toRadians(left ? 55.0F : -55.0F)));
        poseStack.scale(0.85F, 1.35F, 0.85F);
        DashStaffRenderer.renderStaffMesh(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
