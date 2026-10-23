package com.btmi.client;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.btmi.BTMI;
import com.btmi.entity.DeathSignSentinel;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class DeathSignSentinelRenderer extends EntityRenderer<DeathSignSentinel> {
    private static final ResourceLocation PLACEHOLDER =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final float SCALE = 0.9F;

    private final ItemRenderer itemRenderer;
    private ItemStack renderStack;

    public DeathSignSentinelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    private ItemStack renderStack() {
        if (this.renderStack == null) {
            this.renderStack = new ItemStack(BTMI.DEATH_SIGN.get());
        }
        return this.renderStack;
    }

    @Override
    public void render(DeathSignSentinel entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        pose.scale(SCALE, SCALE, SCALE);

        // 实体朝向（水平）：Minecraft 中 yaw 对应的前方 = (sin yaw, 0, -cos yaw)
        float yaw = entity.getViewYRot(partialTick);
        double yawRad = Math.toRadians(yaw);
        Vec3 direction = new Vec3(Math.sin(yawRad), 0.0D, -Math.cos(yawRad));

        // 十字架尖端随视角俯仰，横臂与正面一起绕右侧轴旋转
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 normal = direction.cross(up);
        float pitch = entity.getTipPitch(partialTick);
        float cos = Mth.cos(pitch);
        float sin = Mth.sin(pitch);
        Vec3 c0 = direction.scale(cos).add(up.scale(sin));
        Vec3 c1 = up.scale(cos).subtract(direction.scale(sin));
        Vec3 c2 = normal;

        Matrix3f basis = new Matrix3f().set(
                new Vector3f((float) c0.x, (float) c0.y, (float) c0.z),
                new Vector3f((float) c1.x, (float) c1.y, (float) c1.z),
                new Vector3f((float) c2.x, (float) c2.y, (float) c2.z));
        pose.mulPose(new Quaternionf().setFromUnnormalized(basis));

        this.itemRenderer.renderStatic(renderStack(), ItemDisplayContext.FIXED,
                packedLight, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DeathSignSentinel entity) {
        return PLACEHOLDER;
    }
}
