package com.btmi.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FlameCapeShape {
    public static final int COLS = 8;
    public static final int ROWS = 10;
    public static final float TOP_Y = 1.44F;
    public static final float LENGTH = 1.14F;

    private FlameCapeShape() {
    }

    public static Vec3[][] grid(Player player, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float time = player.tickCount + partialTick;
        Vec3 motion = player.getDeltaMovement();
        double speed = motion.horizontalDistance();
        double fallSpeed = Math.max(0.0D, -motion.y);
        float lag = (float) Mth.clamp(speed * 0.9D + fallSpeed * 1.1D + player.fallDistance * 0.06D, 0.0D, 1.0D);

        double yawRad = Math.toRadians(bodyYaw);
        Vec3 back = new Vec3(Math.sin(yawRad), 0.0D, -Math.cos(yawRad));
        Vec3 right = new Vec3(-Math.cos(yawRad), 0.0D, -Math.sin(yawRad));

        Vec3 origin = new Vec3(
                Mth.lerp(partialTick, player.xOld, player.getX()),
                Mth.lerp(partialTick, player.yOld, player.getY()),
                Mth.lerp(partialTick, player.zOld, player.getZ()));

        Vec3[][] grid = new Vec3[COLS + 1][ROWS + 1];
        for (int i = 0; i <= COLS; i++) {
            float u = i / (float) COLS;
            float f = (u - 0.5F) * 2.0F;
            for (int j = 0; j <= ROWS; j++) {
                float v = j / (float) ROWS;
                float widthAtV = 0.34F + 0.28F * v;
                float wave = Mth.sin(f * 2.6F + time * 0.20F) * 0.05F * v * v;
                float flutter = Mth.sin(f * 1.7F - time * 0.31F) * 0.03F * v;
                float backDist = (0.24F + 0.28F * v + lag * 0.34F * v * v) * (1.0F - 0.14F * f * f);
                float y = TOP_Y - LENGTH * v + lag * 0.16F * v * v + flutter;

                grid[i][j] = origin
                        .add(right.scale(f * widthAtV + wave))
                        .add(back.scale(backDist))
                        .add(0.0D, y, 0.0D);
            }
        }
        return grid;
    }
}
