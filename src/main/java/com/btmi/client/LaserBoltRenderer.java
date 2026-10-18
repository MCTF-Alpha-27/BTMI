package com.btmi.client;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.btmi.BTMI;
import com.btmi.entity.LaserBolt;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class LaserBoltRenderer extends EntityRenderer<LaserBolt> {
    private static final ResourceLocation PLACEHOLDER =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final float SCALE = 0.85F;

    private final ItemRenderer itemRenderer;
    private ItemStack renderStack;

    public LaserBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    private ItemStack renderStack() {
        if (this.renderStack == null) {
            this.renderStack = new ItemStack(BTMI.LASER_BOLT.get());
        }
        return this.renderStack;
    }

    @Override
    public void render(LaserBolt entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.scale(SCALE, SCALE, SCALE);

        Vec3 position = entity.getPosition(partialTick);

        // 飞行方向（取实体的速度方向）
        Vec3 direction = entity.getDeltaMovement();
        if (direction.lengthSqr() < 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();

        // 镜头方向
        Vec3 toCamera = this.entityRenderDispatcher.camera.getPosition().subtract(position);
        if (toCamera.lengthSqr() < 1.0E-8D) {
            toCamera = new Vec3(0.0D, 1.0D, 0.0D);
        }

        // 建立一个正交基：X = 屏幕横向，Y = 飞行方向，Z = 贴图平面法线（尽量朝向镜头）
        Vec3 right = direction.cross(toCamera);
        if (right.lengthSqr() < 1.0E-6D) {
            // 视线与飞行方向几乎平行时退化，随便取一个垂直方向即可
            right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (right.lengthSqr() < 1.0E-6D) {
                right = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
        }
        right = right.normalize();
        Vec3 normal = right.cross(direction).normalize();

        Matrix3f basis = new Matrix3f().set(
                new Vector3f((float) right.x, (float) right.y, (float) right.z),
                new Vector3f((float) direction.x, (float) direction.y, (float) direction.z),
                new Vector3f((float) normal.x, (float) normal.y, (float) normal.z));
        pose.mulPose(new Quaternionf().setFromUnnormalized(basis));

        // 激光自身发光，直接使用全亮度
        this.itemRenderer.renderStatic(renderStack(), ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LaserBolt entity) {
        return PLACEHOLDER;
    }
}
