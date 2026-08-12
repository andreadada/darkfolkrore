package com.darkfolklore.core.contracts;

import com.darkfolklore.core.api.event.ConfirmedLivingDeathEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/** Central fail-closed finality gate for every death-dependent Dark Folklore mutation. */
public final class ConfirmedDeathDispatcher {
    public static final ConfirmedDeathDispatcher INSTANCE = new ConfirmedDeathDispatcher();

    private final Map<MinecraftServer, Map<UUID, PendingDeath>> pendingByServer = new IdentityHashMap<>();

    private ConfirmedDeathDispatcher() {}

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        pendingByServer.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
                .put(event.getEntity().getUUID(), new PendingDeath(event, level.getServer().getTickCount() + 1));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        Map<UUID, PendingDeath> pending = pendingByServer.get(event.getServer());
        if (pending == null) return;
        for (LivingDeathEvent confirmed : collectReady(event.getServer(), pending,
                event.getServer().getTickCount())) {
            NeoForge.EVENT_BUS.post(new ConfirmedLivingDeathEvent(event.getServer(), confirmed.getEntity(),
                    confirmed.getSource()));
        }
        if (pending.isEmpty()) pendingByServer.remove(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        pendingByServer.remove(event.getServer());
    }

    static List<LivingDeathEvent> collectReady(MinecraftServer server, Map<UUID, PendingDeath> pending,
                                               int currentTick) {
        List<LivingDeathEvent> confirmed = new ArrayList<>();
        Iterator<Map.Entry<UUID, PendingDeath>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingDeath value = iterator.next().getValue();
            if (currentTick < value.verifyAfterTick()) continue;
            iterator.remove();
            LivingEntity entity = value.event().getEntity();
            boolean entityOrReplacementAlive = entity.isAlive() || loadedReplacementAlive(server, entity);
            if (shouldDispatch(currentTick, value.verifyAfterTick(), value.event().isCanceled(),
                    entityOrReplacementAlive)) {
                confirmed.add(value.event());
            }
        }
        return confirmed;
    }

    static boolean shouldDispatch(int currentTick, int verifyAfterTick,
                                  boolean eventCanceled, boolean entityAlive) {
        return currentTick >= verifyAfterTick && DeathFinality.confirmed(eventCanceled, entityAlive);
    }

    private static boolean loadedReplacementAlive(MinecraftServer server, LivingEntity original) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity loaded = level.getEntity(original.getUUID());
            if (loaded != null && loaded != original && loaded.isAlive()) return true;
        }
        return false;
    }

    record PendingDeath(LivingDeathEvent event, int verifyAfterTick) {}
}
