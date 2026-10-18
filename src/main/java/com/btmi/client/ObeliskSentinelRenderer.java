package com.btmi.client;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.btmi.BTMI;
import com.btmi.entity.ObeliskSentinel;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ObeliskSentinelRenderer extends EntityRenderer<ObeliskSentinel> {
    private static final ResourceLocation PLACEHOLDER =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final float SCALE = 0.9F;
    private static final float DIAG = 0.70710678F;

    private final ItemRenderer itemRenderer;
    private ItemStack renderStack;

    public ObeliskSentinelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    private ItemStack renderStack() {
        if (this.renderStack == null) {
            this.renderStack = new ItemStack(BTMI.OBELISK.get());
        }
        return this.renderStack;
    }

    @Override
    public void render(ObeliskSentinel entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        pose.scale(SCALE, SCALE, SCALE);

        // 实体朝向（水平）：Minecraft 中 yaw 对应的前方 = (sin yaw, 0, -cos yaw)
        float yaw = entity.getViewYRot(partialTick);
        double yawRad = Math.toRadians(yaw);
        Vec3 direction = new Vec3(Math.sin(yawRad), 0.0D, -Math.cos(yawRad));

        // 让贴图平面尽量正对镜头：取实体和镜头中垂直于朝向的分量作为平面法线
        Vec3 toCamera = this.entityRenderDispatcher.camera.getPosition().subtract(entity.getPosition(partialTick));
        Vec3 normal = toCamera.subtract(direction.scale(toCamera.dot(direction)));
        if (normal.lengthSqr() < 1.0E-6D) {
            normal = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (normal.lengthSqr() < 1.0E-6D) {
                normal = new Vec3(0.0D, 1.0D, 0.0D);
            }
        }
        normal = normal.normalize();

        // w = R(横臂方向)，由右手关系推出：R(横臂) = 朝向 * 法线
        Vec3 crossbar = direction.cross(normal);

        // (1,0,0) = DIAG * (顶端方向 + 横臂方向)
        // (0,1,0) = DIAG * (顶端方向 - 横臂方向)
        // (0,0,1) = 法线
        Vec3 c0 = direction.add(crossbar).scale(DIAG);
        Vec3 c1 = direction.subtract(crossbar).scale(DIAG);
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
    public ResourceLocation getTextureLocation(ObeliskSentinel entity) {
        return PLACEHOLDER;
    }
}
