package com.darkfolklore.core.client;

import com.darkfolklore.core.network.LoreToastPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class ClientLoreToast {
    private ClientLoreToast() {}

    public static void show(LoreToastPayload payload) {
        String stage = payload.stage().toLowerCase(Locale.ROOT);
        Component subject = Component.literal(displayName(payload.concept()));
        Component body = Component.translatable("toast.darkfolklore.lore." + stage, subject);
        SystemToast.add(Minecraft.getInstance().getToasts(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.translatable("toast.darkfolklore.lore.title"), body);
    }

    static String displayName(String concept) {
        ResourceLocation id = ResourceLocation.tryParse(concept);
        String path = id == null ? concept : id.getPath();
        String[] words = path.replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) result.append(word.substring(1));
        }
        return result.isEmpty() ? concept : result.toString();
    }
}
