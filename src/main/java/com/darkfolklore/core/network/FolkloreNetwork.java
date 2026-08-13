package com.darkfolklore.core.network;

import com.darkfolklore.core.client.ClientLoreToast;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class FolkloreNetwork {
    private FolkloreNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                LoreToastPayload.TYPE,
                LoreToastPayload.STREAM_CODEC,
                (payload, context) -> ClientLoreToast.show(payload));
    }

    public static void sendLoreToast(ServerPlayer player, String concept, String stage) {
        PacketDistributor.sendToPlayer(player, new LoreToastPayload(concept, stage));
    }
}
