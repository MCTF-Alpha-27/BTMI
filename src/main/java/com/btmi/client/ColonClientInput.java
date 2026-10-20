package com.btmi.client;

import java.util.List;

import com.btmi.BTMI;
import com.btmi.item.ColonAmmo;
import com.btmi.network.ColonActionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;

@EventBusSubscriber(modid = BTMI.MODID, value = Dist.CLIENT)
public final class ColonClientInput {
    private static boolean attackWasDown;

    private ColonClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            attackWasDown = false;
            return;
        }

        boolean holding = heldGun(player) != null;
        boolean down = minecraft.screen == null && holding
                && (minecraft.options.keyAttack.isDown() || minecraft.mouseHandler.isLeftPressed());

        if (down && !attackWasDown) {
            PacketDistributor.sendToServer(new ColonActionPayload(ColonActionPayload.ACTION_FIRE, 0));
            // 空弹时不开火，也就没有后坐
            if (hasAmmo(player)) {
                ColonRecoilHandler.kick();
                GunFirstPersonHandler.kick();
            }
        }
        attackWasDown = down;
    }

    // 手持冒号时拦截攻击键
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || heldGun(player) == null) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
    }


    private static ItemStack heldGun(Player player) {
        if (player.getMainHandItem().is(BTMI.COLON.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(BTMI.COLON.get())) {
            return player.getOffhandItem();
        }
        return null;
    }

    private static boolean hasAmmo(Player player) {
        ItemStack gun = heldGun(player);
        if (gun == null) {
            return false;
        }
        List<ItemStack> ammo = ColonAmmo.read(gun);
        return !ammo.get(ColonAmmo.selected(gun)).isEmpty()
                || ammo.stream().anyMatch(stack -> !stack.isEmpty());
    }
}
