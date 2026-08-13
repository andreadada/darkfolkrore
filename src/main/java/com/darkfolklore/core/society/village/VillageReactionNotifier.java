package com.darkfolklore.core.society.village;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.api.event.WitnessEvent;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Emits human-facing village reactions only after the persisted reaction band actually changes. */
@EventBusSubscriber(modid = DarkFolkloreCore.MOD_ID)
public final class VillageReactionNotifier {
    private static final Map<String, Pending> PENDING = new LinkedHashMap<>();

    private VillageReactionNotifier() {}

    @SubscribeEvent
    public static synchronized void onWitness(WitnessEvent event) {
        if (!FolkloreConfig.VILLAGE_SOCIETY.get()) return;
        VillageKey key = VillageKey.at(event.level(), event.actor().blockPosition());
        long now = event.level().getGameTime();
        String incident = event.actor().getUUID() + ":" + event.secret() + ":" + (now / 20L);
        PENDING.putIfAbsent(key.serialized() + "|" + incident,
                new Pending(event.level(), event.actor().blockPosition(), key, incident, now));
        if (PENDING.size() > 1024) {
            PENDING.entrySet().removeIf(entry -> now - entry.getValue().observedAt() > 20L);
        }
    }

    @SubscribeEvent
    public static synchronized void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        var pending = PENDING.values().toArray(Pending[]::new);
        PENDING.clear();

        for (Pending entry : pending) {
            ServerLevel level = entry.level();
            if (level.getServer() != event.getServer()) continue;
            VillageSocietyState state = FolkloreSavedData.get(event.getServer()).village(entry.key().serialized());
            VillageReactionTracker.INSTANCE.record(entry.key().serialized(), entry.incident(),
                    level.getGameTime(), state).ifPresent(transition -> notifyPlayers(entry, transition));
        }
    }

    private static void notifyPlayers(Pending pending, VillageReactionTracker.Transition transition) {
        String publicText = switch (transition.after()) {
            case CALM -> "la tensione soprannaturale del villaggio si placa";
            case WATCHFUL -> "il villaggio comincia a sospettare che qualcosa non vada";
            case ALARMED -> "il villaggio è in allarme per eventi soprannaturali";
            case CRISIS -> "il villaggio è in crisi e reagirà apertamente al soprannaturale";
        };
        double radiusSq = 96.0D * 96.0D;
        for (ServerPlayer player : pending.level().players()) {
            if (player.distanceToSqr(pending.position().getX() + 0.5D,
                    pending.position().getY() + 0.5D, pending.position().getZ() + 0.5D) > radiusSq) continue;
            player.displayClientMessage(Component.literal("Dark Folklore: " + publicText + "."), false);
            if (FolkloreConfig.DEBUG_LOGGING.get()) {
                player.displayClientMessage(Component.literal("[DF debug] village=" + pending.key().serialized()
                        + " state=" + transition.before() + "->" + transition.after()
                        + " score=" + transition.score()), false);
            }
        }
    }

    @SubscribeEvent
    public static synchronized void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        VillageReactionTracker.INSTANCE.clear();
    }

    private record Pending(ServerLevel level, BlockPos position, VillageKey key, String incident, long observedAt) {}
}
