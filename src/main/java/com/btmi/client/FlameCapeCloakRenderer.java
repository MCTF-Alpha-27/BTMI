package com.btmi.client;

import com.btmi.BTMI;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class FlameCapeCloakRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "textures/entity/flame_cape_elytra.png");

    private FlameCapeCloakRenderer() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(BTMI.FLAME_CAPE.get())) {
            return;
        }

        Vec3[][] grid = FlameCapeShape.grid(player, event.getPartialTick());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        MultiBufferSource buffer = event.getMultiBufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f matrix = new Matrix4f();

        for (int i = 0; i < FlameCapeShape.COLS; i++) {
            for (int j = 0; j < FlameCapeShape.ROWS; j++) {
                float u0 = i / (float) FlameCapeShape.COLS;
                float u1 = (i + 1) / (float) FlameCapeShape.COLS;
                float v0 = j / (float) FlameCapeShape.ROWS;
                float v1 = (j + 1) / (float) FlameCapeShape.ROWS;

                Vec3 a = grid[i][j].subtract(camera);
                Vec3 b = grid[i + 1][j].subtract(camera);
                Vec3 c = grid[i + 1][j + 1].subtract(camera);
                Vec3 d = grid[i][j + 1].subtract(camera);

                Vec3 normal = b.subtract(a).cross(d.subtract(a)).normalize();
                vertex(consumer, matrix, a, u0, v0, normal);
                vertex(consumer, matrix, b, u1, v0, normal);
                vertex(consumer, matrix, c, u1, v1, normal);
                vertex(consumer, matrix, d, u0, v1, normal);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position,
                               float u, float v, Vec3 normal) {
        consumer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }
}
