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
import net.minecraft.world.entity.Mob;
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

    private final Map<UUID, McaVampireLifecycleBridge.Snapshot> snapshots = new HashMap<>();
    private final Map<UUID, Integer> initialObservationTick = new HashMap<>();
    private final Map<UUID, BirthContext> births = new HashMap<>();
    private final LinkedHashMap<UUID, Observation> latest = new LinkedHashMap<>();

    private McaVampireLifecycleEngine() {}

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!FolkloreConfig.MCA_VAMPIRE_LIFECYCLE.get() || !(event.getLevel() instanceof ServerLevel level)
                || !isMca(event.getEntity())) return;
        initialObservationTick.put(event.getEntity().getUUID(), level.getServer().getTickCount() + 1);
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        UUID id = event.getEntity().getUUID();
        snapshots.remove(id);
        initialObservationTick.remove(id);
    }

    @SubscribeEvent
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!FolkloreConfig.MCA_VAMPIRE_LIFECYCLE.get()) return;
        AgeableMob child = event.getChild();
        if (child == null || !isMca(child) || !(child.level() instanceof ServerLevel)) return;
        births.put(child.getUUID(), new BirthContext(event.getParentA().getUUID(), event.getParentB().getUUID(),
                child.level().getGameTime()));
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!FolkloreConfig.MCA_VAMPIRE_LIFECYCLE.get() || !(event.getEntity() instanceof LivingEntity entity)
                || !(entity.level() instanceof ServerLevel level) || !isMca(entity)) return;
        McaVampireLifecycleBridge bridge = CompatibilityManager.INSTANCE.mcaVampireLifecycle();
        if (!bridge.runtimeAvailable()) return;

        int serverTick = level.getServer().getTickCount();
        Integer initialTick = initialObservationTick.get(entity.getUUID());
        boolean initialReady = initialTick != null && serverTick >= initialTick;
        if (!initialReady && Math.floorMod(entity.tickCount + entity.getId(), SAMPLE_INTERVAL) != 0) return;
        if (initialReady) initialObservationTick.remove(entity.getUUID());
        observe(level, entity, bridge);
    }

    private void observe(ServerLevel level, LivingEntity entity, McaVampireLifecycleBridge bridge) {
        McaVampireLifecycleBridge.Snapshot current = bridge.snapshot(entity);
        if (!current.available() || !current.mcaVillager()) return;
        long now = level.getGameTime();
        BirthContext birth = births.get(entity.getUUID());
        boolean recentBirth = birth != null && now - birth.gameTime() <= BIRTH_CONTEXT_TTL;
        McaVampireLifecycleBridge.Snapshot previous = snapshots.put(entity.getUUID(), current);

        if (current.converted() && !current.curing() && !current.aiGoalsAdded()) {
            bridge.ensureNativeAi(entity);
            current = bridge.snapshot(entity);
            snapshots.put(entity.getUUID(), current);
        }

        McaVampireLifecycleTransition transition;
        if (previous == null) {
            transition = recentBirth && current.converted() && current.inheritanceProcessed() && current.source().isEmpty()
                    ? McaVampireLifecycleTransition.INHERITED_VAMPIRE
                    : McaVampireLifecycleTransition.NONE;
        } else {
            transition = McaVampireLifecycleClassifier.transition(previous, current, recentBirth);
        }
        if (transition != McaVampireLifecycleTransition.NONE) {
            handleTransition(level.getServer(), entity, current, transition, birth, now);
        }
        if (recentBirth && (current.inheritanceProcessed() || now - birth.gameTime() > BIRTH_CONTEXT_TTL / 2)) {
            births.remove(entity.getUUID());
        }
    }

    private void handleTransition(MinecraftServer server, LivingEntity entity,
                                  McaVampireLifecycleBridge.Snapshot current,
                                  McaVampireLifecycleTransition transition,
                                  BirthContext birth, long now) {
        latest.remove(entity.getUUID());
        latest.put(entity.getUUID(), new Observation(entity.getUUID(), McaVampireLifecycleClassifier.state(current),
                transition, current.source(), Optional.ofNullable(birth), now));
        while (latest.size() > 512) latest.remove(latest.keySet().iterator().next());

        switch (transition) {
            case NATIVE_BITE_CONVERTED, CONVERTED -> current.source().ifPresent(source ->
                    FolkloreSavedData.get(server).addLineage(new LineageRecord(entity.getUUID(), source,
                            SecretType.VAMPIRE, now)));
            case CURE_STARTED, CURED, VAMPIRISM_CLEARED, INFECTION_CLEARED -> {
                // Historical beliefs deliberately remain. Only transient predatory intent is stopped.
                if (entity instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }
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
        while (latest.size() > 512) latest.remove(latest.keySet().iterator().next());
    }

    public Optional<Observation> latest(UUID entity) { return Optional.ofNullable(latest.get(entity)); }
    public int trackedEntities() { return snapshots.size(); }
    public int pendingBirths() { return births.size(); }

    private static boolean isMca(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
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
