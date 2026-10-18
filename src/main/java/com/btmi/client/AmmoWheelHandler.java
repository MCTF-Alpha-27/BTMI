package com.btmi.client;

import org.jetbrains.annotations.Nullable;

import com.btmi.BTMI;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// 长按选弹键达到一定时长后打开轮盘界面。
@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class AmmoWheelHandler {
    private static final int HOLD_TICKS = 6;

    private static int heldTicks;
    // 轮盘被关闭后，必须松开按键才能再次唤出
    private static boolean suppressUntilRelease;

    private AmmoWheelHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            heldTicks = 0;
            suppressUntilRelease = false;
            return;
        }

        ItemStack gun = heldColon(player);
        boolean down = gun != null && ModKeyMappings.AMMO_WHEEL.isDown();

        if (!down) {
            heldTicks = 0;
            suppressUntilRelease = false;
            return;
        }
        if (suppressUntilRelease) {
            return;
        }
        if (minecraft.screen != null || gun == null) {
            heldTicks = 0;
            return;
        }

        heldTicks++;
        if (heldTicks >= HOLD_TICKS) {
            suppressUntilRelease = true;
            minecraft.setScreen(new AmmoWheelScreen(gun));
        }
    }

    @Nullable
    private static ItemStack heldColon(Player player) {
        if (player.getMainHandItem().is(BTMI.COLON.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(BTMI.COLON.get())) {
            return player.getOffhandItem();
        }
        return null;
    }
}
