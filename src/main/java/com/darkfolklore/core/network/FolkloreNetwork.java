package com.darkfolklore.core.network;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.client.ClientLoreToast;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = DarkFolkloreCore.MOD_ID)
public final class FolkloreNetwork {
    private FolkloreNetwork() {}

    @SubscribeEvent
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
