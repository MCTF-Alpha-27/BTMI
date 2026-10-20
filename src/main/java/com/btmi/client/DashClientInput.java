package com.btmi.client;

import com.btmi.BTMI;
import com.btmi.item.ColonAmmo;
import com.btmi.network.ColonActionPayload;
import com.btmi.network.CrimsonFangPayload;
import com.btmi.network.DualWeaponPayload;
import com.btmi.network.ExclamationPayload;
import com.btmi.network.FlameCapePayload;
import com.btmi.network.DashActionPayload;
import com.btmi.network.ObeliskActionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
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
        boolean holdingExclamation = minecraft.player.getMainHandItem().is(BTMI.EXCLAMATION_MARK.get());
        boolean holdingDual = minecraft.player.getMainHandItem().is(BTMI.SLASH.get())
                && minecraft.player.getOffhandItem().is(BTMI.EQUAL_SIGN.get());
        boolean holdingAxe = minecraft.player.getMainHandItem().is(BTMI.EQUAL_SIGN.get())
                || minecraft.player.getOffhandItem().is(BTMI.EQUAL_SIGN.get());
        boolean holdingSpear = minecraft.player.getMainHandItem().is(BTMI.SLASH.get())
                || minecraft.player.getOffhandItem().is(BTMI.SLASH.get());
        boolean holdingColon = minecraft.player.getMainHandItem().is(BTMI.COLON.get())
                || minecraft.player.getOffhandItem().is(BTMI.COLON.get());
        boolean holdingObelisk = minecraft.player.getMainHandItem().is(BTMI.OBELISK.get())
                || minecraft.player.getMainHandItem().is(BTMI.OBELISK_RECALL.get())
                || minecraft.player.getOffhandItem().is(BTMI.OBELISK.get())
                || minecraft.player.getOffhandItem().is(BTMI.OBELISK_RECALL.get());
        boolean wearingCape = minecraft.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(BTMI.FLAME_CAPE.get());

        // 蝴蝶出击：单独按键，避免和攻击动作冲突
        while (ModKeyMappings.BUTTERFLY_STRIKE.consumeClick()) {
            if (minecraft.screen == null && wearingCape) {
                PacketDistributor.sendToServer(new FlameCapePayload(FlameCapePayload.ACTION_STRIKE));
                DashClientAnimations.playPointGesture(minecraft.player);
            }
        }
        while (ModKeyMappings.WEAPON_SKILL.consumeClick()) {
            if (minecraft.screen != null) {
                continue;
            }
            if (holdingDual) {
                PacketDistributor.sendToServer(new DualWeaponPayload(DualWeaponPayload.ACTION_STRIKETHROUGH));
            } else if (holdingAxe) {
                PacketDistributor.sendToServer(new DualWeaponPayload(DualWeaponPayload.ACTION_EQUAL_SLASH));
            } else if (holdingSpear) {
                PacketDistributor.sendToServer(new DualWeaponPayload(DualWeaponPayload.ACTION_SLASH_THRUST));
            } else if (holdingDash) {
                PacketDistributor.sendToServer(new DashActionPayload(DashActionPayload.ACTION_PROJECTION, 0));
            } else if (holdingFang) {
                PacketDistributor.sendToServer(new CrimsonFangPayload(CrimsonFangPayload.ACTION_RING));
                DashClientAnimations.playWeaponSpin(minecraft.player);
            } else if (holdingExclamation) {
                PacketDistributor.sendToServer(new ExclamationPayload(ExclamationPayload.ACTION_HOMERUN));
                DashClientAnimations.playHomeRun(minecraft.player);
            } else if (holdingColon) {
                PacketDistributor.sendToServer(new ColonActionPayload(ColonActionPayload.ACTION_FAN, 0));
                ItemStack gun = minecraft.player.getMainHandItem().is(BTMI.COLON.get())
                        ? minecraft.player.getMainHandItem()
                        : minecraft.player.getOffhandItem();
                int loaded = 0;
                for (ItemStack bullet : ColonAmmo.read(gun)) {
                    if (!bullet.isEmpty()) {
                        loaded++;
                    }
                }
                GunFirstPersonHandler.startFan(Math.max(1, loaded) * 2 + 3);
            } else if (holdingObelisk) {
                PacketDistributor.sendToServer(new ObeliskActionPayload(0, ObeliskActionPayload.ACTION_KNELL));
                DashClientAnimations.playPointGesture(minecraft.player);
            } else if (wearingCape) {
                PacketDistributor.sendToServer(new FlameCapePayload(FlameCapePayload.ACTION_SUMMON));
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
