package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.item.DashAbilityHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DashActionPayload(int action, int chargeTicks) implements CustomPacketPayload {
    public static final int ACTION_ATTACK = 0;
    public static final int ACTION_SLASH = 1;
    public static final int ACTION_PROJECTION = 2;

    public static final CustomPacketPayload.Type<DashActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "dash_action"));

    public static final StreamCodec<FriendlyByteBuf, DashActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.action());
                buffer.writeVarInt(payload.chargeTicks());
            },
            buffer -> new DashActionPayload(buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DashActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DashAbilityHandler.handleAction(player, payload.action(), payload.chargeTicks());
            }
        });
    }
}
