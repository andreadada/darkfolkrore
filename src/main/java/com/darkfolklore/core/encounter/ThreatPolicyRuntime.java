package com.darkfolklore.core.encounter;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.spawn.SpawnDirector;
import com.darkfolklore.core.spawn.SpawnProfile;
import com.darkfolklore.core.world.WorldEventDirector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Dark Folklore owns encounter rarity and narrative pressure only.
 * Combat scaling is delegated exclusively to L2 Hostility levels when the audited integration is available.
 */
public final class ThreatPolicyRuntime {
    public static final ThreatPolicyRuntime INSTANCE = new ThreatPolicyRuntime();
    private static final int MAX_PENDING_L2 = 4096;
    private static final int MAX_NATURAL_CANDIDATES = 4096;
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

        // NeoForge 21.1.x exposes the natural spawn reason here, while EntityJoinLevelEvent does not. Retain a
        // short-lived, bounded marker so generic L2 scaling can distinguish ambient natural monsters from another
        // mod's summoned minions, scripted bosses and command-created entities without depending on provider APIs.
        if (FolkloreConfig.ENCOUNTER_DIRECTOR.get() && event.getEntity() instanceof Monster) {
            rememberNaturalCandidate(event.getEntity().getUUID(), level.getGameTime());
        }
    }

    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!FolkloreConfig.ENCOUNTER_DIRECTOR.get() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity living)) return;
        boolean observedNaturalSpawn = naturalSpawnCandidates.remove(living.getUUID()) != null;
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        EncounterPolicy policy = ThreatPolicyManager.INSTANCE.forEntity(entityId).orElse(null);
        if (policy == null) {
            if (observedNaturalSpawn && living instanceof Monster) {
                requestL2Floor(level, living, FolkloreConfig.L2_GENERIC_MIN_LEVEL.get());
            }
            return;
        }

        var nearest = level.players().stream()
                .filter(player -> player.distanceToSqr(living) < 16384.0D)
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(living))).orElse(null);
        if (nearest != null && policy.minimumEncounterPressure() > 0) {
            FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
            if (data.encounterPressure(nearest.getUUID()) < policy.minimumEncounterPressure()) {
                data.setEncounterPressure(nearest.getUUID(), policy.minimumEncounterPressure());
            }
        }
        // Explicit policies are intentional Dark Folklore encounter semantics, so ritual/story/non-natural spawns
        // still receive their curated L2 floor while ordinary unlisted provider summons do not.
        requestL2Floor(level, living, policy.l2MinimumLevel());
    }

    public void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !(living.level() instanceof ServerLevel level)) return;
        PendingL2 pending = pendingL2.get(living.getUUID());
        if (pending == null) return;
        long now = level.getGameTime();
        if (now > pending.expiresAt()) {
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

    public L2HostilityAdapter.ApplyResult requestL2Floor(ServerLevel level, LivingEntity living, int minimumLevel) {
        if (minimumLevel <= 0) {
            return new L2HostilityAdapter.ApplyResult(L2HostilityAdapter.Status.DISABLED, 0, "no level requested");
        }
        L2HostilityAdapter.ApplyResult result = L2HostilityAdapter.INSTANCE.applyMinimum(living, minimumLevel);
        if (!result.retry()) return result;

        PendingL2 existing = pendingL2.get(living.getUUID());
        int strongest = existing == null ? minimumLevel : Math.max(minimumLevel, existing.minimumLevel());
        long expiresAt = Math.max(level.getGameTime() + 200L, existing == null ? 0L : existing.expiresAt());
        if (existing == null && pendingL2.size() >= MAX_PENDING_L2) {
            pendingL2.remove(pendingL2.keySet().iterator().next());
        }
        pendingL2.put(living.getUUID(), new PendingL2(strongest, expiresAt));
        return result;
    }

    private void rememberNaturalCandidate(UUID entityId, long now) {
        naturalSpawnCandidates.entrySet().removeIf(entry -> entry.getValue() < now);
        if (!naturalSpawnCandidates.containsKey(entityId) && naturalSpawnCandidates.size() >= MAX_NATURAL_CANDIDATES) {
            naturalSpawnCandidates.remove(naturalSpawnCandidates.keySet().iterator().next());
        }
        naturalSpawnCandidates.put(entityId, now + 200L);
    }

    private record PendingL2(int minimumLevel, long expiresAt) {}
}
