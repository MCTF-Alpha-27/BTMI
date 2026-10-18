package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.item.ObeliskHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ObeliskActionPayload(int hand, int action) implements CustomPacketPayload {
    public static final int ACTION_ATTACK = 0;
    public static final int ACTION_DETONATE = 1;

    public static final CustomPacketPayload.Type<ObeliskActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "obelisk_action"));

    public static final StreamCodec<FriendlyByteBuf, ObeliskActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.hand());
                buffer.writeVarInt(payload.action());
            },
            buffer -> new ObeliskActionPayload(buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ObeliskActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ObeliskHandler.handleAction(player, payload.hand(), payload.action());
            }
        });
    }
}
