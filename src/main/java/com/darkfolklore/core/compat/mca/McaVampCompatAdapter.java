package com.darkfolklore.core.compat.mca;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.CompatCapabilityCircuit;
import com.darkfolklore.core.compat.FactResult;
import com.darkfolklore.core.compat.SupernaturalStateAdapter;
import com.darkfolklore.core.knowledge.social.SecretType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Fail-closed adapter with independent factual and provenance capabilities. */
public final class McaVampCompatAdapter implements SupernaturalStateAdapter {
    public static final String MOD_ID = "mca_vamp_compat";
    /** Current reference version used by the intended 1.21.1 pack. */
    public static final String TESTED_VERSION = "2.0.29";
    /**
     * Versions whose integration surface is accepted. Every useful member is still resolved at runtime and each
     * capability fails closed independently, so accepting a version never bypasses the reflection probes.
     */
    public static final Set<String> SUPPORTED_VERSIONS = Set.of("2.0.12", "2.0.29", "3.0.29");

    private final CompatCapabilityCircuit facts = new CompatCapabilityCircuit("facts");
    private final CompatCapabilityCircuit provenance = new CompatCapabilityCircuit("provenance");
    private Method vampireQuery;
    private Method werewolfQuery;
    private Method hunterQuery;
    private Method stateQuery;
    private Method vampireSource;
    private Method werewolfSource;

    public static boolean supportsVersion(String version) {
        return version != null && SUPPORTED_VERSIONS.contains(version);
    }

    public void initialize() throws ReflectiveOperationException {
        ClassLoader loader = McaVampCompatAdapter.class.getClassLoader();
        Class<?> entity = Entity.class;
        Class<?> vampireService = Class.forName("com.guilh.mca_vampirism_compat.service.McaVampireStateService", false, loader);
        Class<?> werewolfService = Class.forName("com.guilh.mca_vampirism_compat.service.McaWerewolfStateService", false, loader);
        Class<?> hunterService = Class.forName("com.guilh.mca_vampirism_compat.service.McaHunterAlignmentService", false, loader);
        vampireQuery = vampireService.getMethod("isVampire", entity);
        werewolfQuery = werewolfService.getMethod("isWerewolf", entity);
        hunterQuery = hunterService.getMethod("isMcaHunterAligned", entity);
        facts.markReady("service detection members resolved");

        try {
            Class<?> capabilities = Class.forName("com.guilh.mca_vampirism_compat.capability.ModCapabilities", false, loader);
            Class<?> state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);
            stateQuery = capabilities.getMethod("get", entity);
            vampireSource = state.getMethod("getSource");
            werewolfSource = state.getMethod("getWerewolfSourceUuid");
            provenance.markReady("state provenance members resolved");
        } catch (ReflectiveOperationException | LinkageError exception) {
            provenance.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp] Provenance reads disabled while factual detection remains active: {}",
                    exception.getClass().getSimpleName());
        }
    }

    @Override public String modId() { return MOD_ID; }
    @Override public FactResult isVampire(Entity entity) { return query(entity, vampireQuery); }
    @Override public FactResult isWerewolf(Entity entity) { return query(entity, werewolfQuery); }
    @Override public FactResult isHunter(Entity entity) { return query(entity, hunterQuery); }

    @Override
    public Optional<UUID> conversionSource(Entity entity, SecretType type) {
        if (!provenance.available() || !applies(entity) || (type != SecretType.VAMPIRE && type != SecretType.WEREWOLF)) {
            return Optional.empty();
        }
        try {
            Object value = stateQuery.invoke(null, entity);
            if (!(value instanceof Optional<?> state) || state.isEmpty()) return Optional.empty();
            Object sourceValue = (type == SecretType.VAMPIRE ? vampireSource : werewolfSource).invoke(state.get());
            if (sourceValue instanceof Optional<?> optional && optional.orElse(null) instanceof UUID uuid) {
                return Optional.of(uuid);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            provenance.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp] Provenance capability failed; factual detection remains independent",
                    exception);
        }
        return Optional.empty();
    }

    public boolean factsAvailable() { return facts.available(); }
    public boolean provenanceAvailable() { return provenance.available(); }
    public String diagnosticDetail() { return facts.detail() + ", " + provenance.detail(); }

    private FactResult query(Entity entity, Method method) {
        if (!applies(entity)) return FactResult.NOT_APPLICABLE;
        if (!facts.available() || method == null) return FactResult.UNKNOWN;
        try {
            return FactResult.of((boolean) method.invoke(null, entity));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            facts.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp] Factual detection failed; provenance capability remains independent",
                    exception);
            return FactResult.UNKNOWN;
        }
    }

    private static boolean applies(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
    }
}
