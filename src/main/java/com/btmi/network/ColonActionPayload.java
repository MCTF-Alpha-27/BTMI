package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.item.ColonHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ColonActionPayload(int action, int slot) implements CustomPacketPayload {
    public static final int ACTION_FIRE = 0;
    public static final int ACTION_SELECT = 1;
    public static final int ACTION_FAN = 2;

    public static final CustomPacketPayload.Type<ColonActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "colon_action"));

    public static final StreamCodec<FriendlyByteBuf, ColonActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.action());
                buffer.writeVarInt(payload.slot());
            },
            buffer -> new ColonActionPayload(buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ColonActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.action() == ACTION_FIRE) {
                    ColonHandler.fire(player);
                } else if (payload.action() == ACTION_FAN) {
                    ColonHandler.fan(player);
                } else {
                    ColonHandler.select(player, payload.slot());
                }
            }
        });
    }
}
