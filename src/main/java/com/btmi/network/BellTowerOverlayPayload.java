package com.btmi.network;

import java.util.ArrayList;
import java.util.List;

import com.btmi.BTMI;
import com.btmi.client.BellTowerOverlay;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BellTowerOverlayPayload(String titleJson, List<String> linesJson, int durationTicks)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BellTowerOverlayPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "bell_tower_overlay"));

    public static final StreamCodec<FriendlyByteBuf, BellTowerOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.titleJson());
                buffer.writeVarInt(payload.linesJson().size());
                for (String line : payload.linesJson()) {
                    buffer.writeUtf(line);
                }
                buffer.writeVarInt(payload.durationTicks());
            },
            buffer -> {
                String title = buffer.readUtf();
                int count = buffer.readVarInt();
                List<String> lines = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    lines.add(buffer.readUtf());
                }
                return new BellTowerOverlayPayload(title, lines, buffer.readVarInt());
            });

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BellTowerOverlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BellTowerOverlay.show(payload));
    }
}
