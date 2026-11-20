package com.btmi.belltower;

import com.btmi.belltower.skill.BellTowerSkills;
import com.btmi.bloodrain.BloodRainHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BellTowerShield {
    public static final double RADIUS = 1.08D;
    public static final double CENTER_HEIGHT = 0.9D;

    private BellTowerShield() {
    }

    public static boolean isActive(Player player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return BloodRainHandler.isActive(level) && BellTowerPermission.has(player)
                && BellTowerEnergy.has(player, 1) && BellTowerSkills.bloodRainEnabled(player);
    }

    public static Vec3 center(Entity entity) {
        return entity.position().add(0.0D, CENTER_HEIGHT, 0.0D);
    }
}
