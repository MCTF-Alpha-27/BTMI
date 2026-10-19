package com.btmi.client;

import com.btmi.BTMI;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class FlameCapeClient {
    private FlameCapeClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(BTMI.FLAME_CAPE.get())) {
            return;
        }

        Vec3[][] grid = FlameCapeShape.grid(player, 1.0F);
        RandomSource random = player.getRandom();

        for (int n = 0; n < 10; n++) {
            int i = random.nextInt(FlameCapeShape.COLS);
            int j = random.nextInt(FlameCapeShape.ROWS);
            float fu = random.nextFloat();
            float fv = random.nextFloat();

            Vec3 top = lerp(grid[i][j], grid[i + 1][j], fu);
            Vec3 bottom = lerp(grid[i][j + 1], grid[i + 1][j + 1], fu);
            Vec3 point = lerp(top, bottom, fv);
            Vec3 down = grid[i][j + 1].subtract(grid[i][j]).normalize();

            double speed = 0.05D + random.nextDouble() * 0.06D;
            minecraft.level.addParticle(
                    random.nextFloat() < 0.3F ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                    point.x + (random.nextDouble() - 0.5D) * 0.06D,
                    point.y,
                    point.z + (random.nextDouble() - 0.5D) * 0.06D,
                    down.x * speed + (random.nextDouble() - 0.5D) * 0.01D,
                    down.y * speed,
                    down.z * speed + (random.nextDouble() - 0.5D) * 0.01D);
        }

        if (player.tickCount % 4 == 0) {
            int i = random.nextInt(FlameCapeShape.COLS);
            Vec3 hem = grid[i][FlameCapeShape.ROWS];
            minecraft.level.addParticle(ParticleTypes.LAVA,
                    hem.x, hem.y, hem.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, float delta) {
        return new Vec3(
                from.x + (to.x - from.x) * delta,
                from.y + (to.y - from.y) * delta,
                from.z + (to.z - from.z) * delta);
    }
}
