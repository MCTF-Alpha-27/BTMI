package com.btmi.weapon;

import com.btmi.BTMI;

import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

// 冒号的武器类型由数据包定义（data/btmi/capabilities/weapons/types/colon.json），
// 这里只保留手持冒号时取消史诗战斗近战基本攻击的钩子。
public final class ModWeaponTypes {
    private ModWeaponTypes() {
    }

    public static void register() {
        EpicFightEventHooks.Player.COMBO_ATTACK.registerEvent(event -> {
            ServerPlayerPatch patch = event.getPlayerPatch();
            if (patch == null) {
                return;
            }
            ServerPlayer player = patch.getOriginal();
            if (player.getMainHandItem().is(BTMI.COLON.get())
                    || player.getOffhandItem().is(BTMI.COLON.get())
                    || player.getMainHandItem().is(BTMI.SCARLET_FANG.get())) {
                event.cancel();
            }
        });
    }
}
