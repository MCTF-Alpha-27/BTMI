package com.btmi.belltower;

import com.btmi.BTMI;
import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class BellTowerPermission {
    private BellTowerPermission() {
    }

    public static AttachmentType<Boolean> create() {
        return AttachmentType.builder(() -> Boolean.FALSE)
                .serialize(Codec.BOOL, Boolean::booleanValue)
                .copyOnDeath()
                .sync(ByteBufCodecs.BOOL)
                .build();
    }

    public static boolean has(Player player) {
        return Boolean.TRUE.equals(player.getData(BTMI.BELL_TOWER.get()));
    }

    public static void grant(Player player) {
        player.setData(BTMI.BELL_TOWER.get(), Boolean.TRUE);
    }

    public static void revoke(Player player) {
        player.setData(BTMI.BELL_TOWER.get(), Boolean.FALSE);
    }
}
