package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.network.CrimsonFangPayload;
import com.btmi.network.DashActionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class DashClientInput {
    private static final int MAX_TRACKED_CHARGE = 400;

    private static boolean attackWasDown;
    private static boolean charging;
    private static int chargeTicks;

    private DashClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }

        // 只有在没有打开任何界面、手持破折号且按下攻击键时
        boolean holdingDash = minecraft.player.getMainHandItem().is(BTMI.DASH.get());
        boolean attackKeyDown = minecraft.options.keyAttack.isDown() || minecraft.mouseHandler.isLeftPressed();
        boolean attackDown = minecraft.screen == null && holdingDash && attackKeyDown;

        // 武器技能键（H）
        boolean holdingFang = minecraft.player.getMainHandItem().is(BTMI.CRIMSON_FANG.get());
        while (ModKeyMappings.WEAPON_SKILL.consumeClick()) {
            if (minecraft.screen != null) {
                continue;
            }
            if (holdingDash) {
                PacketDistributor.sendToServer(new DashActionPayload(DashActionPayload.ACTION_PROJECTION, 0));
            } else if (holdingFang) {
                PacketDistributor.sendToServer(new CrimsonFangPayload(CrimsonFangPayload.ACTION_RING));
                DashClientAnimations.playWeaponSpin(minecraft.player);
            }
        }

        if (attackDown && !attackWasDown) {
            // 刚刚按下攻击键：长棍分离为刀刃与刀鞘
            PacketDistributor.sendToServer(new DashActionPayload(DashActionPayload.ACTION_ATTACK, 0));
            chargeTicks = 0;
            charging = false;
        }

        if (attackDown) {
            if (minecraft.player.isShiftKeyDown()) {
                charging = true;
            }
            if (charging && chargeTicks < MAX_TRACKED_CHARGE) {
                chargeTicks++;
                spawnChargeParticles(minecraft, chargeTicks);
            }
        } else if (attackWasDown) {
            // 松开攻击键：只要这次是按着潜行蓄过力的，就发动劈砍
            if (charging && chargeTicks > 0) {
                PacketDistributor.sendToServer(new DashActionPayload(DashActionPayload.ACTION_SLASH, chargeTicks));
            }
            chargeTicks = 0;
            charging = false;
        }

        attackWasDown = attackDown;
    }

    private static void spawnChargeParticles(Minecraft minecraft, int charge) {
        if (minecraft.player == null || minecraft.level == null || charge % 2 != 0) {
            return;
        }
        double angle = charge * 0.45D;
        double radius = 0.9D;
        double x = minecraft.player.getX() + Math.cos(angle) * radius;
        double z = minecraft.player.getZ() + Math.sin(angle) * radius;
        minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                x, minecraft.player.getY() + 1.0D, z, 0.0D, 0.02D, 0.0D);
        // 每 10 刻播放一次提示音，音调随蓄力升高
        if (charge % 10 == 0) {
            float pitch = 0.8F + Math.min(charge, 60) * 0.01F;
            minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, pitch, false);
        }
    }

    public static int getChargeTicks() {
        return chargeTicks;
    }

    private static void reset() {
        attackWasDown = false;
        charging = false;
        chargeTicks = 0;
    }
}
