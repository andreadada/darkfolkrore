package com.darkfolklore.core.compat.mca;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.CompatCapabilityCircuit;
import com.darkfolklore.core.compat.FactResult;
import com.darkfolklore.core.compat.SupernaturalStateAdapter;
import com.darkfolklore.core.knowledge.social.SecretType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fail-closed provider adapter with independent vampire, werewolf, hunter and provenance capabilities. */
public final class McaVampCompatAdapter implements SupernaturalStateAdapter {
    public static final String MOD_ID = "mca_vamp_compat";
    public static final String TESTED_VERSION = "2.0.29";
    public static final Set<String> SUPPORTED_VERSIONS = Set.of("2.0.12", "2.0.29", "3.0.29");
    private static final Pattern VERSION_TRIPLE = Pattern.compile("(?:^|[^0-9])(\\d+)\\.(\\d+)\\.(\\d+)(?:$|[^0-9])");

    private final CompatCapabilityCircuit vampireFacts = new CompatCapabilityCircuit("vampire-facts");
    private final CompatCapabilityCircuit werewolfFacts = new CompatCapabilityCircuit("werewolf-facts");
    private final CompatCapabilityCircuit hunterFacts = new CompatCapabilityCircuit("hunter-facts");
    private final CompatCapabilityCircuit provenance = new CompatCapabilityCircuit("provenance");
    private Method vampireQuery;
    private Method werewolfQuery;
    private Method hunterQuery;
    private Method stateQuery;
    private Method vampireSource;
    private Method werewolfSource;

    public static String normalizeVersion(String version) {
        if (version == null) return "";
        String trimmed = version.trim();
        Matcher matcher = VERSION_TRIPLE.matcher(trimmed);
        if (!matcher.find()) return trimmed;
        return matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
    }

    public static boolean supportsVersion(String version) {
        return SUPPORTED_VERSIONS.contains(normalizeVersion(version));
    }

    public static boolean runtimeProbeEligible(String version) {
        String normalized = normalizeVersion(version);
        if (SUPPORTED_VERSIONS.contains(normalized)) return true;
        Matcher matcher = VERSION_TRIPLE.matcher(normalized);
        if (!matcher.find()) return false;
        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));
            return major == 2 && minor == 0 && patch >= 12;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Retains the historical checked contract while every sub-probe handles its own failure internally. */
    public void initialize() throws ReflectiveOperationException {
        ClassLoader loader = McaVampCompatAdapter.class.getClassLoader();
        probeFact(loader, vampireFacts, "vampire",
                "com.guilh.mca_vampirism_compat.service.McaVampireStateService", "isVampire");
        probeFact(loader, werewolfFacts, "werewolf",
                "com.guilh.mca_vampirism_compat.service.McaWerewolfStateService", "isWerewolf");
        probeFact(loader, hunterFacts, "hunter",
                "com.guilh.mca_vampirism_compat.service.McaHunterAlignmentService", "isMcaHunterAligned");

        try {
            Class<?> capabilities = Class.forName("com.guilh.mca_vampirism_compat.capability.ModCapabilities", false, loader);
            Class<?> state = Class.forName("com.guilh.mca_vampirism_compat.VampiricVillagerState", false, loader);
            stateQuery = capabilities.getMethod("get", Entity.class);
            vampireSource = state.getMethod("getSource");
            werewolfSource = state.getMethod("getWerewolfSourceUuid");
            provenance.markReady("state provenance members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            provenance.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp] Provenance reads disabled while factual detection remains independent: {}",
                    exception.getClass().getSimpleName());
        }
    }

    private void probeFact(ClassLoader loader, CompatCapabilityCircuit circuit, String name,
                           String className, String methodName) {
        try {
            Class<?> service = Class.forName(className, false, loader);
            Method method = service.getMethod(methodName, Entity.class);
            switch (name) {
                case "vampire" -> vampireQuery = method;
                case "werewolf" -> werewolfQuery = method;
                case "hunter" -> hunterQuery = method;
                default -> throw new IllegalArgumentException("Unknown factual probe " + name);
            }
            circuit.markReady(methodName + " resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            circuit.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp/{}] Factual probe unavailable; unrelated facts remain active: {}",
                    name, exception.getClass().getSimpleName());
        }
    }

    @Override public String modId() { return MOD_ID; }
    @Override public FactResult isVampire(Entity entity) { return query(entity, vampireQuery, vampireFacts); }
    @Override public FactResult isWerewolf(Entity entity) { return query(entity, werewolfQuery, werewolfFacts); }
    @Override public FactResult isHunter(Entity entity) { return query(entity, hunterQuery, hunterFacts); }

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

    public boolean factsAvailable() {
        return vampireFacts.available() || werewolfFacts.available() || hunterFacts.available();
    }

    public boolean provenanceAvailable() { return provenance.available(); }

    public Map<String, Boolean> factualCircuitStatus() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("vampire", vampireFacts.available());
        result.put("werewolf", werewolfFacts.available());
        result.put("hunter", hunterFacts.available());
        result.put("provenance", provenance.available());
        return Map.copyOf(result);
    }

    public String diagnosticDetail() {
        return vampireFacts.detail() + ", " + werewolfFacts.detail() + ", " + hunterFacts.detail()
                + ", " + provenance.detail();
    }

    /** Re-arm only members that successfully resolved during initialization; missing signatures remain failed. */
    @Override
    public void clearRuntimeState() {
        rearm(vampireFacts, vampireQuery, "isVampire resolved");
        rearm(werewolfFacts, werewolfQuery, "isWerewolf resolved");
        rearm(hunterFacts, hunterQuery, "isMcaHunterAligned resolved");
        if (stateQuery != null && vampireSource != null && werewolfSource != null) {
            provenance.reset();
            provenance.markReady("state provenance members resolved");
        }
    }

    private static void rearm(CompatCapabilityCircuit circuit, Method method, String detail) {
        if (method == null) return;
        circuit.reset();
        circuit.markReady(detail);
    }

    private FactResult query(Entity entity, Method method, CompatCapabilityCircuit circuit) {
        if (!applies(entity)) return FactResult.NOT_APPLICABLE;
        if (!circuit.available() || method == null) return FactResult.UNKNOWN;
        try {
            return FactResult.of((boolean) method.invoke(null, entity));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            circuit.fail(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_vamp] {} failed; unrelated factual capabilities remain independent",
                    circuit.detail(), exception);
            return FactResult.UNKNOWN;
        }
    }

    private static boolean applies(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
    }
}
