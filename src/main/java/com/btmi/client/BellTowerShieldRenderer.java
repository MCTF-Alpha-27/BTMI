package com.btmi.client;

import org.joml.Matrix4f;

import com.btmi.BTMI;
import com.btmi.belltower.BellTowerEnergy;
import com.btmi.belltower.BellTowerPermission;
import com.btmi.belltower.BellTowerShield;
import com.btmi.belltower.skill.BellTowerSkills;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class BellTowerShieldRenderer {
    private static final float RADIUS = (float) BellTowerShield.RADIUS;
    private static final float CENTER_HEIGHT = (float) BellTowerShield.CENTER_HEIGHT;
    private static final int LON = 32;
    private static final int LAT = 16;
    private static final int STREAKS = 28;
    private static final float TAU = (float) (Math.PI * 2.0D);

    private static final RenderType SHELL = RenderType.create(
            "btmi_bell_tower_shield",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType STREAK = RenderType.create(
            "btmi_bell_tower_shield_rain",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType SHEEN = RenderType.create(
            "btmi_bell_tower_shield_sheen",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private BellTowerShieldRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        if (!BloodRainClient.isActive()) {
            return;
        }
        float fade = BloodRainClient.fade();
        if (fade <= 0.01F) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        float time = (minecraft.level.getGameTime() + partialTick) / 20.0F;
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        for (Player player : minecraft.level.players()) {
            if (firstPerson && player == minecraft.player) {
                continue;
            }
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (!BellTowerPermission.has(player) || !BellTowerSkills.bloodRainEnabled(player)
                    || BellTowerEnergy.get(player) <= 0) {
                continue;
            }
            Vec3 position = player.getPosition(partialTick);
            double dx = position.x - camera.x;
            double dy = position.y - camera.y;
            double dz = position.z - camera.z;
            if (dx * dx + dy * dy + dz * dz > 96.0D * 96.0D) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(dx, dy + CENTER_HEIGHT, dz);
            Matrix4f matrix = poseStack.last().pose();
            renderShell(buffers.getBuffer(SHELL), matrix, (float) -dx, (float) -(dy + CENTER_HEIGHT),
                    (float) -dz, time, fade);
            renderSheen(buffers.getBuffer(SHEEN), matrix, (float) -dx, (float) -(dy + CENTER_HEIGHT),
                    (float) -dz, time, fade);
            renderStreaks(buffers.getBuffer(STREAK), matrix, time, fade);
            poseStack.popPose();
        }

        buffers.endBatch(SHELL);
        buffers.endBatch(SHEEN);
        buffers.endBatch(STREAK);
    }

    private static void renderShell(VertexConsumer buffer, Matrix4f matrix, float camX, float camY, float camZ,
            float time, float fade) {
        float pulse = 0.85F + 0.15F * Mth.sin(time * 2.2F);
        for (int lat = 0; lat < LAT; lat++) {
            float phi0 = Mth.PI * lat / LAT;
            float phi1 = Mth.PI * (lat + 1) / LAT;
            for (int lon = 0; lon < LON; lon++) {
                float theta0 = TAU * lon / LON;
                float theta1 = TAU * (lon + 1) / LON;

                float[] p0 = spherePoint(phi0, theta0);
                float[] p1 = spherePoint(phi0, theta1);
                float[] p2 = spherePoint(phi1, theta1);
                float[] p3 = spherePoint(phi1, theta0);

                shellVertex(buffer, matrix, p0, camX, camY, camZ, pulse, fade, time);
                shellVertex(buffer, matrix, p1, camX, camY, camZ, pulse, fade, time);
                shellVertex(buffer, matrix, p2, camX, camY, camZ, pulse, fade, time);
                shellVertex(buffer, matrix, p3, camX, camY, camZ, pulse, fade, time);
            }
        }
    }

    private static void renderSheen(VertexConsumer buffer, Matrix4f matrix, float camX, float camY, float camZ,
            float time, float fade) {
        float angle = time * 0.32F;
        float lightX = Mth.cos(angle) * 0.92F;
        float lightY = 0.3F;
        float lightZ = Mth.sin(angle) * 0.92F;
        float length = Mth.sqrt(lightX * lightX + lightY * lightY + lightZ * lightZ);
        lightX /= length;
        lightY /= length;
        lightZ /= length;

        for (int lat = 0; lat < LAT; lat++) {
            float phi0 = Mth.PI * lat / LAT;
            float phi1 = Mth.PI * (lat + 1) / LAT;
            for (int lon = 0; lon < LON; lon++) {
                float theta0 = TAU * lon / LON;
                float theta1 = TAU * (lon + 1) / LON;

                sheenVertex(buffer, matrix, spherePoint(phi0, theta0), camX, camY, camZ,
                        lightX, lightY, lightZ, fade);
                sheenVertex(buffer, matrix, spherePoint(phi0, theta1), camX, camY, camZ,
                        lightX, lightY, lightZ, fade);
                sheenVertex(buffer, matrix, spherePoint(phi1, theta1), camX, camY, camZ,
                        lightX, lightY, lightZ, fade);
                sheenVertex(buffer, matrix, spherePoint(phi1, theta0), camX, camY, camZ,
                        lightX, lightY, lightZ, fade);
            }
        }
    }

    private static void sheenVertex(VertexConsumer buffer, Matrix4f matrix, float[] point, float camX, float camY,
            float camZ, float lightX, float lightY, float lightZ, float fade) {
        float nx = point[0] / RADIUS;
        float ny = point[1] / RADIUS;
        float nz = point[2] / RADIUS;
        float vx = camX - point[0];
        float vy = camY - point[1];
        float vz = camZ - point[2];
        float length = Mth.sqrt(vx * vx + vy * vy + vz * vz);
        float face = 0.0F;
        if (length > 1.0E-4F) {
            face = Math.max(0.0F, (nx * vx + ny * vy + nz * vz) / length);
        }
        float diffuse = Math.max(0.0F, nx * lightX + ny * lightY + nz * lightZ);
        float wide = diffuse * diffuse * diffuse * diffuse;

        float alpha = fade * wide * 0.1F * face;
        buffer.addVertex(matrix, point[0], point[1], point[2]).setColor(1.0F, 0.8F, 0.82F, alpha);
    }

    private static float[] spherePoint(float phi, float theta) {
        float sinPhi = Mth.sin(phi);
        return new float[] {
                RADIUS * sinPhi * Mth.cos(theta),
                RADIUS * Mth.cos(phi),
                RADIUS * sinPhi * Mth.sin(theta)
        };
    }

    private static void shellVertex(VertexConsumer buffer, Matrix4f matrix, float[] point, float camX, float camY,
            float camZ, float pulse, float fade, float time) {
        float nx = point[0] / RADIUS;
        float ny = point[1] / RADIUS;
        float nz = point[2] / RADIUS;
        float vx = camX - point[0];
        float vy = camY - point[1];
        float vz = camZ - point[2];
        float length = Mth.sqrt(vx * vx + vy * vy + vz * vz);
        float rim = 0.0F;
        if (length > 1.0E-4F) {
            float facing = Math.abs((nx * vx + ny * vy + nz * vz) / length);
            float edge = 1.0F - Math.min(1.0F, facing);
            rim = edge * edge * edge;
        }
        float flow = flowAt(nx, ny, nz, time);
        float alpha = Mth.clamp(fade * pulse * (0.05F + 0.34F * rim + 0.58F * flow), 0.0F, 0.9F);
        float red = Math.min(1.0F, 0.3F + 0.55F * rim + 0.52F * flow);
        float green = Math.min(1.0F, 0.02F + 0.09F * rim + 0.3F * flow);
        float blue = Math.min(1.0F, 0.04F + 0.11F * rim + 0.31F * flow);
        buffer.addVertex(matrix, point[0], point[1], point[2]).setColor(red, green, blue, alpha);
    }

    private static float flowAt(float nx, float ny, float nz, float time) {
        float swirl = noise(nx * 1.25F + time * 0.13F, ny * 1.6F - time * 0.11F, nz * 1.25F + 5.0F) - 0.5F;
        float angle = time * 0.36F + ny * 0.5F + swirl * 1.1F;
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float wideX = nx * cos - nz * sin;
        float wideZ = nx * sin + nz * cos;
        float bands = noise(wideX * 1.6F, ny * 5.2F + time * 0.26F + swirl * 2.4F, wideZ * 1.6F);

        float fastAngle = -time * 0.58F + ny * 0.3F + 1.9F - swirl * 0.7F;
        float fastCos = Mth.cos(fastAngle);
        float fastSin = Mth.sin(fastAngle);
        float fineX = nx * fastCos - nz * fastSin;
        float fineZ = nx * fastSin + nz * fastCos;
        float filaments = noise(fineX * 3.7F, ny * 9.6F - time * 0.46F + 4.2F, fineZ * 3.7F);

        float value = bands * 0.66F + filaments * 0.34F;
        value = Mth.clamp((value - 0.38F) * 3.4F, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static float noise(float x, float y, float z) {
        int x0 = Mth.floor(x);
        int y0 = Mth.floor(y);
        int z0 = Mth.floor(z);
        float fx = x - x0;
        float fy = y - y0;
        float fz = z - z0;
        float u = fx * fx * (3.0F - 2.0F * fx);
        float v = fy * fy * (3.0F - 2.0F * fy);
        float w = fz * fz * (3.0F - 2.0F * fz);

        float c000 = hash3(x0, y0, z0);
        float c100 = hash3(x0 + 1, y0, z0);
        float c010 = hash3(x0, y0 + 1, z0);
        float c110 = hash3(x0 + 1, y0 + 1, z0);
        float c001 = hash3(x0, y0, z0 + 1);
        float c101 = hash3(x0 + 1, y0, z0 + 1);
        float c011 = hash3(x0, y0 + 1, z0 + 1);
        float c111 = hash3(x0 + 1, y0 + 1, z0 + 1);

        return Mth.lerp(w,
                Mth.lerp(v, Mth.lerp(u, c000, c100), Mth.lerp(u, c010, c110)),
                Mth.lerp(v, Mth.lerp(u, c001, c101), Mth.lerp(u, c011, c111)));
    }

    private static float hash3(int x, int y, int z) {
        int value = x * 0x27D4EB2D + y * 0x165667B1 + z * 0x9E3779B1;
        value ^= value >>> 15;
        value *= 0x2545F491;
        value ^= value >>> 13;
        value *= 0x85EBCA77;
        value ^= value >>> 16;
        return (value & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    private static void renderStreaks(VertexConsumer buffer, Matrix4f matrix, float time, float fade) {
        for (int i = 0; i < STREAKS; i++) {
            float theta = ((i + hash(i, 11) * 0.6F) / STREAKS) * TAU;
            float speed = 1.5F + hash(i, 23) * 1.5F;
            float phase = hash(i, 37);
            float travel = (phase + time * speed) % 1.0F;
            float span = 0.2F + hash(i, 53) * 0.3F;

            float headY = RADIUS - 2.0F * RADIUS * travel;
            float tailY = Math.min(RADIUS, headY + span * 2.0F * RADIUS);
            if (tailY - headY < 0.02F) {
                continue;
            }
            float headRadius = radiusAt(headY);
            float tailRadius = radiusAt(tailY);
            float cos = Mth.cos(theta);
            float sin = Mth.sin(theta);
            float halfWidth = RADIUS * (0.0045F + hash(i, 71) * 0.0065F);
            float tangentX = -sin * halfWidth;
            float tangentZ = cos * halfWidth;

            float headAlpha = fade * (0.45F + 0.3F * hash(i, 89));
            float tailAlpha = 0.0F;

            buffer.addVertex(matrix, headRadius * cos - tangentX, headY, headRadius * sin - tangentZ)
                    .setColor(1.0F, 0.14F, 0.17F, headAlpha);
            buffer.addVertex(matrix, headRadius * cos + tangentX, headY, headRadius * sin + tangentZ)
                    .setColor(1.0F, 0.14F, 0.17F, headAlpha);
            buffer.addVertex(matrix, tailRadius * cos + tangentX, tailY, tailRadius * sin + tangentZ)
                    .setColor(0.45F, 0.03F, 0.06F, tailAlpha);
            buffer.addVertex(matrix, tailRadius * cos - tangentX, tailY, tailRadius * sin - tangentZ)
                    .setColor(0.45F, 0.03F, 0.06F, tailAlpha);
        }
    }

    private static float radiusAt(float y) {
        float squared = RADIUS * RADIUS - y * y;
        return squared <= 0.0F ? 0.0F : Mth.sqrt(squared);
    }

    private static float hash(int index, int salt) {
        int value = index * 0x9E3779B1 + salt * 0x85EBCA77;
        value ^= value >>> 15;
        value *= 0x2545F491;
        value ^= value >>> 13;
        return (value & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }
}
