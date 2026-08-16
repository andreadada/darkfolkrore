package com.darkfolklore.core.encounter;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.spawn.SpawnDirector;
import com.darkfolklore.core.spawn.SpawnProfile;
import com.darkfolklore.core.spawn.SpawnRarity;
import com.darkfolklore.core.world.WorldEventDirector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dark Folklore owns encounter rarity and narrative pressure only.
 * Combat scaling is delegated exclusively to L2 Hostility levels when the audited integration is available.
 */
public final class ThreatPolicyRuntime {
    public static final ThreatPolicyRuntime INSTANCE = new ThreatPolicyRuntime();
    private static final int MAX_PENDING_L2 = 4096;
    private static final int MAX_NATURAL_CANDIDATES = 4096;
    private static final long NATURAL_CANDIDATE_TTL = 20L;
    private final LinkedHashMap<UUID, PendingL2> pendingL2 = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Long> naturalSpawnCandidates = new LinkedHashMap<>();

    private ThreatPolicyRuntime() {}

    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        ServerLevel level = event.getLevel().getLevel();
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        EncounterPolicy policy = ThreatPolicyManager.INSTANCE.forEntity(entityId).orElse(null);
        SpawnProfile profile = FolkloreDataManager.INSTANCE.spawns().get(entityId).orElse(null);

        if (FolkloreConfig.SPAWN_DIRECTOR.get()) {
            if (profile != null && !SpawnDirector.hardGateAllows(profile, level.isNight())) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }

            double profileContext = 1.0D;
            if (profile != null) {
                if (!WorldEventDirector.INSTANCE.active(level).isEmpty()) profileContext *= profile.eventMultiplier();
                if (FolkloreConfig.ENCOUNTER_DIRECTOR.get()) {
                    var nearestPlayer = level.getNearestPlayer(event.getX(), event.getY(), event.getZ(), 128, false);
                    if (nearestPlayer != null) {
                        int pressure = FolkloreSavedData.get(level.getServer()).encounterPressure(nearestPlayer.getUUID());
                        profileContext *= Math.max(0.2D, 1.0D - pressure / 125.0D);
                    }
                }
            }

            NaturalSpawnRarityPolicy.Decision decision = NaturalSpawnRarityPolicy.resolve(
                    policy == null ? null : policy.naturalSpawnMultiplier(),
                    profile == null ? null : (double) profile.rarity().naturalChance(),
                    event.getEntity() instanceof Monster,
                    FolkloreConfig.HOSTILE_NATURAL_SPAWN_MULTIPLIER.get(),
                    FolkloreConfig.SPAWN_MULTIPLIER.get(),
                    profileContext);
            if (NaturalSpawnRarityPolicy.reject(decision, event.getLevel().getRandom().nextDouble())) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }

        // PositionCheck is the reliable 1.21.1 source for NATURAL provenance. Do not mutate SavedData or L2 from
        // EntityJoinLevelEvent: NeoForge may fire that event before the chunk reaches FULL and it can still be
        // cancelled by another listener. A mob that was actually admitted will tick shortly afterwards; that first
        // tick consumes this bounded marker and is the commit point for pressure/L2 side effects.
        if (FolkloreConfig.ENCOUNTER_DIRECTOR.get()) {
            rememberNaturalCandidate(event.getEntity().getUUID(), level.getGameTime());
        }
    }

    /**
     * Applies the non-rarity part of an explicit Dark Folklore encounter exactly when the Core itself manifests it.
     * Core-owned ritual/legendary callers invoke this after addFreshEntity succeeds. Provider summons and entities
     * merely reloaded from disk therefore never inherit a Dark Folklore encounter policy by registry id alone.
     */
    public L2HostilityAdapter.ApplyResult applyCuratedEncounter(ServerLevel level, LivingEntity living,
                                                                EncounterPolicy policy) {
        if (!FolkloreConfig.ENCOUNTER_DIRECTOR.get()) {
            return new L2HostilityAdapter.ApplyResult(L2HostilityAdapter.Status.DISABLED, 0,
                    "encounter director disabled");
        }
        if (level == null || living == null || policy == null) {
            return new L2HostilityAdapter.ApplyResult(L2HostilityAdapter.Status.DISABLED, 0,
                    "missing curated encounter context");
        }
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        if (!policy.entityId().equals(entityId)) {
            return new L2HostilityAdapter.ApplyResult(L2HostilityAdapter.Status.DISABLED, 0,
                    "encounter policy/entity mismatch");
        }

        if (policy.minimumEncounterPressure() > 0) {
            ensureEncounterPressure(level, living, policy.minimumEncounterPressure());
        }
        return requestL2Floor(level, living, policy.l2MinimumLevel());
    }

    /** Resolve and apply the current datapack policy for a Core-owned manifestation. */
    public L2HostilityAdapter.ApplyResult applyCuratedEncounter(ServerLevel level, LivingEntity living) {
        if (living == null) {
            return new L2HostilityAdapter.ApplyResult(L2HostilityAdapter.Status.DISABLED, 0,
                    "missing curated encounter entity");
        }
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        EncounterPolicy policy = ThreatPolicyManager.INSTANCE.forEntity(entityId).orElse(null);
        if (policy == null) {
            return new L2HostilityAdapter.ApplyResult(L2HostilityAdapter.Status.DISABLED, 0,
                    "no curated encounter policy for " + entityId);
        }
        return applyCuratedEncounter(level, living, policy);
    }

    public void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !(living.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();

        Long naturalExpiry = naturalSpawnCandidates.remove(living.getUUID());
        if (naturalExpiry != null && now <= naturalExpiry && FolkloreConfig.ENCOUNTER_DIRECTOR.get()) {
            applyNaturalSpawn(level, living);
        }

        PendingL2 pending = pendingL2.get(living.getUUID());
        if (pending == null) return;
        if (now > pending.expiresAt() || living.isRemoved()) {
            pendingL2.remove(living.getUUID());
            return;
        }
        if (living.tickCount % 10 != 0) return;
        L2HostilityAdapter.ApplyResult result = L2HostilityAdapter.INSTANCE.applyMinimum(living, pending.minimumLevel());
        if (!result.retry()) pendingL2.remove(living.getUUID());
    }

    public void clearRuntimeState() {
        pendingL2.clear();
        naturalSpawnCandidates.clear();
        L2HostilityAdapter.INSTANCE.clearRuntimeState();
    }

    /**
     * Requests a minimum through L2 and retains the strongest pending request while L2's attachment initializes.
     * Lower-priority systems can therefore never overwrite a stronger legendary/story request made in the same tick.
     */
    public L2HostilityAdapter.ApplyResult requestL2Floor(ServerLevel level, LivingEntity living, int minimumLevel) {
        if (minimumLevel <= 0) {
            return new L2HostilityAdapter.ApplyResult(L2HostilityAdapter.Status.DISABLED, 0, "no level requested");
        }
        L2HostilityAdapter.ApplyResult result = L2HostilityAdapter.INSTANCE.applyMinimum(living, minimumLevel);
        if (!result.retry()) return result;

        long now = level.getGameTime();
        prunePendingL2(now);
        PendingL2 existing = pendingL2.remove(living.getUUID());
        int strongest = existing == null ? minimumLevel : Math.max(minimumLevel, existing.minimumLevel());
        long expiresAt = Math.max(now + 200L, existing == null ? 0L : existing.expiresAt());
        while (pendingL2.size() >= MAX_PENDING_L2) {
            pendingL2.remove(pendingL2.keySet().iterator().next());
        }
        // Reinsert even for an existing request so map order remains compatible with expiry/oldest-first pruning.
        pendingL2.put(living.getUUID(), new PendingL2(strongest, expiresAt));
        return result;
    }

    private void applyNaturalSpawn(ServerLevel level, LivingEntity living) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        SpawnProfile profile = FolkloreDataManager.INSTANCE.spawns().get(entityId).orElse(null);
        if (profile != null && profile.rarity().ordinal() >= SpawnRarity.RARE.ordinal()) {
            addEncounterPressure(level, living, 15);
        }

        EncounterPolicy policy = ThreatPolicyManager.INSTANCE.forEntity(entityId).orElse(null);
        if (policy != null) {
            applyCuratedEncounter(level, living, policy);
        } else if (living instanceof Monster) {
            requestL2Floor(level, living, FolkloreConfig.L2_GENERIC_MIN_LEVEL.get());
        }
    }

    private static ServerPlayer nearestPlayer(ServerLevel level, LivingEntity living) {
        return level.players().stream()
                .filter(player -> player.distanceToSqr(living) < 16384.0D)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(living))).orElse(null);
    }

    private static void addEncounterPressure(ServerLevel level, LivingEntity living, int amount) {
        if (amount <= 0) return;
        ServerPlayer nearest = nearestPlayer(level, living);
        if (nearest == null) return;
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        data.setEncounterPressure(nearest.getUUID(), data.encounterPressure(nearest.getUUID()) + amount);
    }

    private static void ensureEncounterPressure(ServerLevel level, LivingEntity living, int minimum) {
        if (minimum <= 0) return;
        ServerPlayer nearest = nearestPlayer(level, living);
        if (nearest == null) return;
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        if (data.encounterPressure(nearest.getUUID()) < minimum) {
            data.setEncounterPressure(nearest.getUUID(), minimum);
        }
    }

    private void rememberNaturalCandidate(UUID entityId, long now) {
        pruneNaturalCandidates(now);
        // Remove before re-adding so insertion order remains expiry order even if a provider fires PositionCheck more
        // than once for the same entity instance.
        naturalSpawnCandidates.remove(entityId);
        while (naturalSpawnCandidates.size() >= MAX_NATURAL_CANDIDATES) {
            naturalSpawnCandidates.remove(naturalSpawnCandidates.keySet().iterator().next());
        }
        naturalSpawnCandidates.put(entityId, now + NATURAL_CANDIDATE_TTL);
    }

    private void pruneNaturalCandidates(long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = naturalSpawnCandidates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() >= now) break;
            iterator.remove();
        }
    }

    private void prunePendingL2(long now) {
        Iterator<Map.Entry<UUID, PendingL2>> iterator = pendingL2.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingL2> entry = iterator.next();
            if (entry.getValue().expiresAt() >= now) break;
            iterator.remove();
        }
    }

    private record PendingL2(int minimumLevel, long expiresAt) {}
}
