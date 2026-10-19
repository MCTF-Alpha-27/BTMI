package com.btmi.client;

import com.btmi.BTMI;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class DashClientAnimations {
    private static final int AIM_HOLD_TICKS = 7;
    private static final int SPIN_TICKS = 12;
    private static final int POINT_TICKS = 10;
    private static final int HOMERUN_TICKS = 15;

    private static int holdTicks = -1;
    private static int spinTicks = -1;
    private static int pointTicks = -1;
    private static int homeRunTicks = -1;

    private DashClientAnimations() {
    }

    public static void playFire(Player player) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }
        LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(localPlayer);
        if (patch == null) {
            return;
        }
        patch.playAnimationInClientSide(Animations.SPEAR_TWOHAND_AUTO1, 0.05F);
        holdTicks = AIM_HOLD_TICKS;
        BTMI.LOGGER.debug("[dash] aim pose for {} ticks", AIM_HOLD_TICKS);
    }

    /** 猩红之牙的能量环：用长柄的旋风斩动作表现旋转一周 */
    public static void playWeaponSpin(Player player) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }
        LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(localPlayer);
        if (patch != null) {
            // 只转前半圈，12 tick 后主动停掉
            patch.playAnimationInClientSide(Animations.STEEL_WHIRLWIND, 0.0F);
            spinTicks = SPIN_TICKS;
        }
    }

    /** 烈焰蝶召唤：伸手施法的动作 */
    public static void playPointGesture(Player player) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }
        LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(localPlayer);
        if (patch != null) {
            patch.playAnimationInClientSide(Animations.EVOKER_CAST_SPELL, 0.0F);
            pointTicks = POINT_TICKS;
        }
    }

    /** 感叹号全垒打：借一次大幅横向挥击 */
    public static void playHomeRun(Player player) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }
        LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(localPlayer);
        if (patch != null) {
            patch.playAnimationInClientSide(Animations.SWEEPING_EDGE, 0.05F);
            homeRunTicks = HOMERUN_TICKS;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (homeRunTicks > 0) {
            homeRunTicks--;
            if (homeRunTicks == 0) {
                LocalPlayerPatch patch = EpicFightCapabilities.getCachedLocalPlayerPatch();
                if (patch != null) {
                    patch.stopPlaying(Animations.SWEEPING_EDGE);
                }
            }
        }
        if (pointTicks > 0) {
            pointTicks--;
            if (pointTicks == 0) {
                LocalPlayerPatch patch = EpicFightCapabilities.getCachedLocalPlayerPatch();
                if (patch != null) {
                    patch.stopPlaying(Animations.EVOKER_CAST_SPELL);
                }
            }
        }

        if (spinTicks > 0) {
            spinTicks--;
            if (spinTicks == 0) {
                LocalPlayerPatch patch = EpicFightCapabilities.getCachedLocalPlayerPatch();
                if (patch != null) {
                    patch.stopPlaying(Animations.STEEL_WHIRLWIND);
                }
            }
        }
        if (holdTicks <= 0) {
            return;
        }
        holdTicks--;
        if (holdTicks == 0) {
            LocalPlayerPatch patch = EpicFightCapabilities.getCachedLocalPlayerPatch();
            if (patch != null) {
                patch.stopPlaying(Animations.SPEAR_TWOHAND_AUTO1);
            }
        }
    }
}
