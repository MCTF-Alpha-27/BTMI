package com.btmi.client;

import com.btmi.BTMI;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class GunFirstPersonHandler {
    private static final float FANG_UP = 0.14F;
    private static final float FANG_INWARD = 0.10F;
    private static final float FANG_PITCH = 7.0F;
    private static final float FANG_YAW = 6.0F;

    private static int fanTicks;
    private static float kick;

    private GunFirstPersonHandler() {
    }

    public static void startFan(int ticks) {
        fanTicks = Math.max(1, ticks);
        kick = 0.0F;
    }

    public static void kick() {
        kick = 1.0F;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (fanTicks > 0) {
            fanTicks--;
        }
        kick *= 0.6F;
        if (kick < 0.02F) {
            kick = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!event.getItemStack().is(BTMI.CRIMSON_FANG.get())) {
            return;
        }
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float recoil = kick * 0.5F;

        PoseStack pose = event.getPoseStack();
        pose.translate(-FANG_INWARD * side, FANG_UP, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(FANG_PITCH + 6.0F * recoil));
        pose.mulPose(Axis.YP.rotationDegrees(-FANG_YAW * side));
    }
}
