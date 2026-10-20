package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.item.DualWeaponHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DualWeaponPayload(int action) implements CustomPacketPayload {
    public static final int ACTION_STRIKETHROUGH = 0;
    public static final int ACTION_EQUAL_SLASH = 1;
    public static final int ACTION_SLASH_THRUST = 2;

    public static final CustomPacketPayload.Type<DualWeaponPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "dual_weapon_action"));

    public static final StreamCodec<FriendlyByteBuf, DualWeaponPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.action()),
            buffer -> new DualWeaponPayload(buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DualWeaponPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.action() == ACTION_STRIKETHROUGH) {
                    DualWeaponHandler.strikethrough(player);
                } else if (payload.action() == ACTION_EQUAL_SLASH) {
                    DualWeaponHandler.equalSlash(player);
                } else if (payload.action() == ACTION_SLASH_THRUST) {
                    DualWeaponHandler.slashThrust(player);
                }
            }
        });
    }
}
