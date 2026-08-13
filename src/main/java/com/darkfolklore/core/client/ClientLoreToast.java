package com.darkfolklore.core.client;

import com.darkfolklore.core.network.LoreToastPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class ClientLoreToast {
    private ClientLoreToast() {}

    public static void show(LoreToastPayload payload) {
        SystemToast.add(Minecraft.getInstance().getToasts(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("Dark Folklore"),
                Component.literal(payload.concept() + " — " + payload.stage()));
    }
}
