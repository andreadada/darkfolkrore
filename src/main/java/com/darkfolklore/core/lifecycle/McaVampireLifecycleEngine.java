package com.darkfolklore.core.lifecycle;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.McaVampireLifecycleBridge;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.bloodline.LineageRecord;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * Observes the exact provider lifecycle without becoming another infection/cure/inheritance authority.
 * All caches are transient observations; MCA Vamp Compat remains the factual owner.
 */
public final class McaVampireLifecycleEngine {
    public static final McaVampireLifecycleEngine INSTANCE = new McaVampireLifecycleEngine();
    private static final int SAMPLE_INTERVAL = 40;
    private static final long BIRTH_CONTEXT_TTL = 200L;
    private static final int MAX_TRACKED_ENTITIES = 4096;
    private static final int MAX_BIRTH_CONTEXTS = 2048;

    private final LinkedHashMap<UUID, McaVampireLifecycleBridge.Snapshot> snapshots = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Integer> initialObservationTick = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, BirthContext> births = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Observation> latest = new LinkedHashMap<>();

    private McaVampireLifecycleEngine() {}

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!FolkloreConfig.MCA_VAMPIRE_LIFECYCLE.get() || !(event.getLevel() instanceof ServerLevel level)
                || !isMca(event.getEntity())) return;
        putBounded(initialObservationTick, event.getEntity().getUUID(), level.getServer().getTickCount() + 1,
                MAX_TRACKED_ENTITIES);
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        UUID id = event.getEntity().getUUID();
        snapshots.remove(id);
        initialObservationTick.remove(id);
        births.remove(id);
    }

    @SubscribeEvent
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!FolkloreConfig.MCA_VAMPIRE_LIFECYCLE.get()) return;
        AgeableMob child = event.getChild();
        if (child == null || !isMca(child) || !(child.level() instanceof ServerLevel)) return;
        putBounded(births, child.getUUID(), new BirthContext(event.getParentA().getUUID(), event.getParentB().getUUID(),
                child.level().getGameTime()), MAX_BIRTH_CONTEXTS);
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!FolkloreConfig.MCA_VAMPIRE_LIFECYCLE.get() || !(event.getEntity() instanceof LivingEntity entity)
                || !(entity.level() instanceof ServerLevel level) || !isMca(entity)) return;
        McaVampireLifecycleBridge bridge = CompatibilityManager.INSTANCE.mcaVampireLifecycle();
        if (!bridge.runtimeAvailable()) return;

        int serverTick = level.getServer().getTickCount();
        Integer initialTick = initialObservationTick.get(entity.getUUID());
        boolean sampledTick = Math.floorMod(entity.tickCount + entity.getId(), SAMPLE_INTERVAL) == 0;
        if (initialTick == null && !sampledTick) return;
        if (initialTick != null && serverTick < initialTick) return;
        if (initialTick != null && !InitialObservationPolicy.shouldAttempt(serverTick, initialTick, sampledTick)) {
            initialObservationTick.remove(entity.getUUID());
            return;
        }
        boolean observed = observe(level, entity, bridge);
        if (initialTick != null && !InitialObservationPolicy.shouldRetain(serverTick, initialTick, observed)) {
            initialObservationTick.remove(entity.getUUID());
        }
    }

    private boolean observe(ServerLevel level, LivingEntity entity, McaVampireLifecycleBridge bridge) {
        McaVampireLifecycleBridge.Snapshot current = bridge.snapshot(entity);
        if (!current.available() || !current.mcaVillager()) return false;
        long now = level.getGameTime();
        BirthContext birth = births.get(entity.getUUID());
        boolean recentBirth = birth != null && now - birth.gameTime() <= BIRTH_CONTEXT_TTL;
        McaVampireLifecycleBridge.Snapshot previous = snapshots.remove(entity.getUUID());
        putBounded(snapshots, entity.getUUID(), current, MAX_TRACKED_ENTITIES);

        if (current.converted() && !current.curing() && !current.aiGoalsAdded()) {
            bridge.ensureNativeAi(entity);
            current = bridge.snapshot(entity);
            snapshots.remove(entity.getUUID());
            putBounded(snapshots, entity.getUUID(), current, MAX_TRACKED_ENTITIES);
        }

        // Provider provenance is a durable factual datum, not merely a transition edge. Recover it after world
        // load as well as during a live conversion so event-listener order cannot make a valid source disappear.
        if (current.converted()) {
            ensureProviderLineage(level.getServer(), entity.getUUID(), current.source(), now);
        }

        McaVampireLifecycleTransition transition;
        if (previous == null) {
            transition = McaVampireLifecycleClassifier.initialTransition(current, recentBirth);
        } else {
            transition = McaVampireLifecycleClassifier.transition(previous, current, recentBirth);
        }
        if (transition != McaVampireLifecycleTransition.NONE) {
            handleTransition(level.getServer(), entity, current, transition, birth, now);
        }
        if (recentBirth && (current.inheritanceProcessed() || now - birth.gameTime() > BIRTH_CONTEXT_TTL / 2)) {
            births.remove(entity.getUUID());
        }
        return true;
    }

    private static void ensureProviderLineage(MinecraftServer server, UUID descendant,
                                              Optional<UUID> providerSource, long now) {
        ProviderLineagePolicy.validSource(descendant, providerSource).ifPresent(source ->
                FolkloreSavedData.get(server).addLineage(new LineageRecord(descendant, source,
                        SecretType.VAMPIRE, now)));
    }

    private void handleTransition(MinecraftServer server, LivingEntity entity,
                                  McaVampireLifecycleBridge.Snapshot current,
                                  McaVampireLifecycleTransition transition,
                                  BirthContext birth, long now) {
        putBounded(latest, entity.getUUID(), new Observation(entity.getUUID(), McaVampireLifecycleClassifier.state(current),
                transition, current.source(), Optional.ofNullable(birth), now), 512);

        switch (transition) {
            case CURE_STARTED, CURED, VAMPIRISM_CLEARED, INFECTION_CLEARED -> {
                // Historical beliefs deliberately remain. Only transient predatory intent is stopped.
                // Provider owns generic combat/command targets and navigation; cancel only Core's session.
                com.darkfolklore.core.predation.VampirePredationEngine.INSTANCE.cancelSession(entity);
            }
            case INHERITED_VAMPIRE -> {
                // Provider inheritance intentionally has no conversion source. Preserve both parents only in
                // diagnostic birth context instead of fabricating a one-parent conversion lineage record.
            }
            default -> { }
        }

        if (FolkloreConfig.DEBUG_LOGGING.get()) {
            DarkFolkloreCore.LOGGER.info("[mca-vamp-lifecycle] {} {} source={} birth={}", entity.getUUID(),
                    transition, current.source().orElse(null), birth);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 1200 != 0) return;
        long now = event.getServer().overworld().getGameTime();
        births.entrySet().removeIf(entry -> now - entry.getValue().gameTime() > BIRTH_CONTEXT_TTL);
        initialObservationTick.entrySet().removeIf(entry -> event.getServer().getTickCount() - entry.getValue() > 1200);
        trimOldest(births, MAX_BIRTH_CONTEXTS);
        trimOldest(initialObservationTick, MAX_TRACKED_ENTITIES);
        trimOldest(snapshots, MAX_TRACKED_ENTITIES);
        trimOldest(latest, 512);
    }

    public Optional<Observation> latest(UUID entity) { return Optional.ofNullable(latest.get(entity)); }
    public int trackedEntities() { return snapshots.size(); }
    public int pendingBirths() { return births.size(); }

    public void clearRuntimeState() {
        snapshots.clear();
        initialObservationTick.clear();
        births.clear();
        latest.clear();
    }

    private static boolean isMca(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
    }

    private static <K, V> void putBounded(LinkedHashMap<K, V> map, K key, V value, int maximum) {
        map.remove(key);
        map.put(key, value);
        trimOldest(map, maximum);
    }

    private static <K, V> void trimOldest(LinkedHashMap<K, V> map, int maximum) {
        while (map.size() > maximum) {
            Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }

    public record BirthContext(UUID parentA, UUID parentB, long gameTime) {}

    public record Observation(UUID entity, McaVampireLifecycleState state,
                              McaVampireLifecycleTransition transition, Optional<UUID> source,
                              Optional<BirthContext> birth, long gameTime) {
        public Observation {
            source = source == null ? Optional.empty() : source;
            birth = birth == null ? Optional.empty() : birth;
        }
    }
}
