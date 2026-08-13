package com.darkfolklore.core.network;

import com.darkfolklore.core.DarkFolkloreCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Small clientbound payload used only when a persisted lore stage changes. */
public record LoreToastPayload(String concept, String stage) implements CustomPacketPayload {
    public static final Type<LoreToastPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DarkFolkloreCore.MOD_ID, "lore_toast"));
    public static final StreamCodec<ByteBuf, LoreToastPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LoreToastPayload::concept,
            ByteBufCodecs.STRING_UTF8, LoreToastPayload::stage,
            LoreToastPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
