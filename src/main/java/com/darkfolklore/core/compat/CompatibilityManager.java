package com.darkfolklore.core.compat;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.mca.McaSocialAdapter;
import com.darkfolklore.core.compat.mca.McaVampCompatAdapter;
import com.darkfolklore.core.compat.mcacapitals.McaCapitalsCompat;
import com.darkfolklore.core.knowledge.social.SecretType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

public final class CompatibilityManager {
    public static final CompatibilityManager INSTANCE = new CompatibilityManager();

    private static final List<Spec> SPECS = List.of(
            new Spec("vampirism", "Vampirism", "1.10.12", "official/public API"),
            new Spec("werewolves", "Werewolves", "2.0.3.3", "public API + tags"),
            new Spec("mca", "MCA Reborn", "7.7.32+1.21.1", "isolated public implementation reads"),
            new Spec("mcacapitals", "MCA Capitals", "1.1.0", "exact-version cached read-only bridge"),
            new Spec(McaVampCompatAdapter.MOD_ID, "MCA Reborn x Vampirism Compat",
                    McaVampCompatAdapter.TESTED_VERSION,
                    "runtime-probed facts/provenance + independently gated predation/lifecycle"),
            new Spec("enchanted", "Enchanted", "4.2.7", "tags and recipes"),
            new Spec("occultism", "Occultism", "1.224.2", "tags and recipes"),
            new Spec("malum", "Malum", "1.8.2", "tags and recipes"),
            new Spec("eidolon_repraised", "Eidolon: Repraised", "0.5.0.2", "tags and datapack recipes"),
            new Spec("feywild", "Feywild", "5.5.5", "tags and recipes"),
            new Spec("fieldguide", "Field Guide", "1.14.0", "exact 1.14.0 server progress bridge"),
            new Spec("betterarcheology", "Better Archeology", "1.21.1-1.3.8", "item tags and pickup events"),
            new Spec("quest_giver", "Quest Giver", "1.5.1", "optional frontend; Core owns backend"),
            new Spec("almostunified", "AlmostUnified", "1.21.1-1.4.2", "delegated material unification")
    );

    private volatile List<CompatibilityReport> reports = List.of();
    private volatile List<SupernaturalStateAdapter> stateAdapters = List.of();
    private volatile SupernaturalStateAdapter mcaFactAdapter;
    private volatile CompatibilityStatus mcaFactStatus = CompatibilityStatus.DISABLED;
    private volatile ProviderComponents mcaVampComponents = ProviderComponents.disabled();
    private volatile FieldGuideBridge fieldGuideBridge;
    private volatile VampirePredationBridge vampirePredationBridge = VampirePredationBridge.DISABLED;
    private volatile McaVampireLifecycleBridge mcaVampireLifecycleBridge = McaVampireLifecycleBridge.DISABLED;
    private final McaSocialAdapter mcaSocial = new McaSocialAdapter();
    private final McaCapitalsCompat mcaCapitals = new McaCapitalsCompat();

    private CompatibilityManager() {}

    public synchronized void initialize() {
        List<CompatibilityReport> nextReports = new ArrayList<>();
        List<SupernaturalStateAdapter> nextAdapters = new ArrayList<>();
        ModList mods = ModList.get();
        fieldGuideBridge = null;
        vampirePredationBridge = VampirePredationBridge.DISABLED;
        mcaVampireLifecycleBridge = McaVampireLifecycleBridge.DISABLED;
        mcaFactAdapter = null;
        mcaFactStatus = CompatibilityStatus.DISABLED;
        mcaVampComponents = ProviderComponents.disabled();
        mcaSocial.initialize("not-installed-or-untested");
        mcaCapitals.initialize("not-installed-or-untested");

        for (Spec spec : SPECS) {
            Optional<? extends net.neoforged.fml.ModContainer> container = mods.getModContainerById(spec.modId());
            if (container.isEmpty()) {
                nextReports.add(new CompatibilityReport(spec.modId(), spec.name(), spec.testedVersion(), "-",
                        spec.mechanism(), CompatibilityStatus.DISABLED, "Optional mod is not installed"));
                continue;
            }
            String actual = container.get().getModInfo().getVersion().toString();
            boolean supported = versionSupported(spec, actual);
            CompatibilityStatus status = supported ? CompatibilityStatus.ACTIVE : CompatibilityStatus.UNTESTED_VERSION;
            String detail;
            if (status != CompatibilityStatus.ACTIVE) {
                detail = "Adapter internals disabled until this version line is admitted for runtime probing";
            } else if (spec.modId().equals(McaVampCompatAdapter.MOD_ID)) {
                String normalized = McaVampCompatAdapter.normalizeVersion(actual);
                detail = McaVampCompatAdapter.supportsVersion(actual)
                        ? "Known provider version " + normalized + "; capabilities are probed independently at runtime"
                        : "Compatible 2.0.x provider candidate " + normalized + "; no capability is enabled until its runtime probe succeeds";
            } else {
                detail = "Exact audited version";
            }
            nextReports.add(new CompatibilityReport(spec.modId(), spec.name(), spec.testedVersion(), actual,
                    spec.mechanism(), status, detail));
        }

        boolean vampirismExact = isExact(nextReports, "vampirism");
        boolean werewolvesExact = isExact(nextReports, "werewolves");
        boolean mcaExact = isExact(nextReports, McaSocialAdapter.MOD_ID);
        boolean mcaCapitalsExact = isExact(nextReports, McaCapitalsCompat.MOD_ID);
        boolean providerProbeEligible = isExact(nextReports, McaVampCompatAdapter.MOD_ID);
        boolean fieldGuideExact = isExact(nextReports, "fieldguide");
        boolean factualMcaStack = mcaExact && providerProbeEligible;
        boolean fullMcaVampStack = PredationBridgePolicy.enableMcaProbe(vampirismExact, mcaExact, providerProbeEligible);

        if (mcaExact && !mcaSocial.initialize(actualVersion(nextReports, McaSocialAdapter.MOD_ID))) {
            replaceStatus(nextReports, McaSocialAdapter.MOD_ID, CompatibilityStatus.ERROR, mcaSocial.statusDetail());
        }
        if (mcaCapitalsExact && !mcaCapitals.initialize(actualVersion(nextReports, McaCapitalsCompat.MOD_ID))) {
            replaceStatus(nextReports, McaCapitalsCompat.MOD_ID, CompatibilityStatus.ERROR, mcaCapitals.statusDetail());
        }

        if (vampirismExact) {
            try {
                Class<?> type = Class.forName("com.darkfolklore.core.compat.vampirism.VampirismAdapter", true,
                        CompatibilityManager.class.getClassLoader());
                nextAdapters.add((SupernaturalStateAdapter) type.getConstructor(boolean.class)
                        .newInstance(werewolvesExact));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                replaceStatus(nextReports, "vampirism", CompatibilityStatus.ERROR,
                        "Public API adapter failed to load: " + exception.getClass().getSimpleName());
                DarkFolkloreCore.LOGGER.error("[compat/vampirism] Public API adapter failed to load", exception);
            }
        }

        CompatibilityStatus facts = componentDependencyStatus(nextReports, McaVampCompatAdapter.MOD_ID, "mca");
        CompatibilityStatus predation = componentDependencyStatus(nextReports,
                McaVampCompatAdapter.MOD_ID, "mca", "vampirism");
        CompatibilityStatus lifecycle = predation;
        String factsDetail = "not initialized";

        if (factualMcaStack) {
            facts = CompatibilityStatus.ERROR;
            try {
                McaVampCompatAdapter adapter = new McaVampCompatAdapter();
                adapter.initialize();
                if (adapter.factsAvailable()) {
                    mcaFactAdapter = adapter;
                    facts = CompatibilityStatus.ACTIVE;
                }
                factsDetail = adapter.diagnosticDetail();
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                factsDetail = "fact capability initialization failed: " + exception.getClass().getSimpleName();
                DarkFolkloreCore.LOGGER.error("[compat/mca_vamp] Factual bridge failed to load", exception);
            }
        }

        /*
         * Critical isolation rule: ordinary Vampirism predation depends only on Vampirism. MCA version/probe
         * failures must never prevent IVampireMob recognition, blood hunger, targeting or feeding.
         */
        if (PredationBridgePolicy.loadWildBridge(vampirismExact)) {
            try {
                Object adapter = Class.forName("com.darkfolklore.core.compat.vampirism.VampirePredationCompat", true,
                                CompatibilityManager.class.getClassLoader())
                        .getConstructor(boolean.class).newInstance(fullMcaVampStack);
                if (!(adapter instanceof VampirePredationBridge bridge)) {
                    throw new LinkageError("VampirePredationCompat does not implement VampirePredationBridge");
                }
                vampirePredationBridge = bridge;
                NeoForge.EVENT_BUS.register(adapter);
                if (fullMcaVampStack) {
                    predation = bridge.mcaRuntimeAvailable() ? CompatibilityStatus.ACTIVE : CompatibilityStatus.ERROR;
                }
                if (!bridge.wildRuntimeAvailable()) {
                    DarkFolkloreCore.LOGGER.error("[compat/vampire_predation] Wild Vampirism circuit failed to initialize: {}",
                            bridge.runtimeDetail());
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                vampirePredationBridge = VampirePredationBridge.DISABLED;
                if (fullMcaVampStack) predation = CompatibilityStatus.ERROR;
                DarkFolkloreCore.LOGGER.error("[compat/vampire_predation] Wild predation bridge failed to load", exception);
            }
        }

        if (fullMcaVampStack) {
            lifecycle = CompatibilityStatus.ERROR;
            try {
                Object adapter = Class.forName("com.darkfolklore.core.compat.mca.McaVampireLifecycleCompat", true,
                        CompatibilityManager.class.getClassLoader()).getConstructor().newInstance();
                if (!(adapter instanceof McaVampireLifecycleBridge bridge)) {
                    throw new LinkageError("McaVampireLifecycleCompat does not implement McaVampireLifecycleBridge");
                }
                mcaVampireLifecycleBridge = bridge;
                lifecycle = bridge.runtimeAvailable() ? CompatibilityStatus.ACTIVE : CompatibilityStatus.ERROR;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                mcaVampireLifecycleBridge = McaVampireLifecycleBridge.DISABLED;
                DarkFolkloreCore.LOGGER.error("[compat/mca_vamp_lifecycle] Provider lifecycle bridge failed to load", exception);
            }
        }

        mcaFactStatus = facts;
        mcaVampComponents = new ProviderComponents(facts, predation, lifecycle);
        if (status(nextReports, McaVampCompatAdapter.MOD_ID) != CompatibilityStatus.DISABLED && providerProbeEligible) {
            replaceStatus(nextReports, McaVampCompatAdapter.MOD_ID, mcaVampComponents.combinedStatus(),
                    "Capabilities: facts=" + facts + ", predation=" + predation + ", lifecycle=" + lifecycle
                            + "; " + factsDetail + "; predationBridge=" + vampirePredationBridge.runtimeDetail());
        }

        if (fieldGuideExact) {
            try {
                Object adapter = Class.forName("com.darkfolklore.core.compat.fieldguide.FieldGuideAdapter", true,
                        CompatibilityManager.class.getClassLoader()).getConstructor().newInstance();
                if (!(adapter instanceof FieldGuideBridge bridge)) {
                    throw new LinkageError("FieldGuideAdapter does not implement FieldGuideBridge");
                }
                fieldGuideBridge = bridge;
                NeoForge.EVENT_BUS.register(adapter);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                fieldGuideBridge = null;
                replaceStatus(nextReports, "fieldguide", CompatibilityStatus.ERROR,
                        "1.14.0 progress bridge failed to load: " + exception.getClass().getSimpleName());
                DarkFolkloreCore.LOGGER.error("[compat/fieldguide] Progress bridge failed to load", exception);
            }
        }

        reports = List.copyOf(nextReports);
        stateAdapters = List.copyOf(nextAdapters);
        reports.forEach(report -> DarkFolkloreCore.LOGGER.info("[compat/{}] tested={} actual={} status={} detail={}",
                report.modId(), report.testedVersion(), report.actualVersion(), report.status(), report.detail()));
    }

    public List<CompatibilityReport> reports() { return reports; }
    public Optional<CompatibilityReport> report(String modId) { return reports.stream().filter(report -> report.modId().equals(modId)).findFirst(); }
    public McaSocialAdapter mcaSocial() { return mcaSocial; }
    public McaCapitalsCompat mcaCapitals() { return mcaCapitals; }
    public VampirePredationBridge vampirePredation() { return vampirePredationBridge; }
    public McaVampireLifecycleBridge mcaVampireLifecycle() { return mcaVampireLifecycleBridge; }
    public ProviderComponents mcaVampComponents() { return mcaVampComponents; }

    public boolean unlockFieldGuideImplementation(ServerPlayer player, String registryId) {
        FieldGuideBridge bridge = fieldGuideBridge;
        return bridge != null && bridge.runtimeAvailable() && bridge.unlockObservedImplementation(player, registryId);
    }

    public boolean fieldGuideRuntimeAvailable() {
        FieldGuideBridge bridge = fieldGuideBridge;
        return bridge != null && bridge.runtimeAvailable();
    }

    public void clearRuntimeCaches() {
        mcaCapitals.clearCache();
        vampirePredationBridge.clearRuntimeState();
    }

    public FactResult isVampire(Entity entity) { return aggregate(entity, Query.VAMPIRE); }
    public FactResult isWerewolf(Entity entity) { return aggregate(entity, Query.WEREWOLF); }
    public FactResult isHunter(Entity entity) { return aggregate(entity, Query.HUNTER); }

    public Optional<UUID> conversionSource(Entity entity, SecretType type) {
        if (isMca(entity)) {
            return mcaFactStatus == CompatibilityStatus.ACTIVE && mcaFactAdapter != null
                    ? mcaFactAdapter.conversionSource(entity, type) : Optional.empty();
        }
        return stateAdapters.stream().map(adapter -> adapter.conversionSource(entity, type))
                .flatMap(Optional::stream).findFirst();
    }

    private FactResult aggregate(Entity entity, Query query) {
        if (isMca(entity)) {
            FactResult provider = mcaFactAdapter == null ? FactResult.NOT_APPLICABLE : query(mcaFactAdapter, entity, query);
            return SupernaturalFactResolver.resolveMca(mcaFactStatus, provider);
        }
        List<FactResult> results = new ArrayList<>();
        for (SupernaturalStateAdapter adapter : stateAdapters) results.add(query(adapter, entity, query));
        return SupernaturalFactResolver.resolveGeneric(results);
    }

    private static FactResult query(SupernaturalStateAdapter adapter, Entity entity, Query query) {
        return switch (query) {
            case VAMPIRE -> adapter.isVampire(entity);
            case WEREWOLF -> adapter.isWerewolf(entity);
            case HUNTER -> adapter.isHunter(entity);
        };
    }

    private static boolean versionSupported(Spec spec, String actual) {
        if (spec.modId().equals(McaVampCompatAdapter.MOD_ID)) {
            return McaVampCompatAdapter.runtimeProbeEligible(actual);
        }
        return actual.equals(spec.testedVersion());
    }

    private static boolean isExact(List<CompatibilityReport> reports, String modId) {
        return reports.stream().anyMatch(report -> report.modId().equals(modId) && report.status() == CompatibilityStatus.ACTIVE);
    }

    private static String actualVersion(List<CompatibilityReport> reports, String modId) {
        return reports.stream().filter(report -> report.modId().equals(modId)).map(CompatibilityReport::actualVersion).findFirst().orElse("-");
    }

    private static CompatibilityStatus componentDependencyStatus(List<CompatibilityReport> reports, String... modIds) {
        boolean allActive = true;
        boolean anyUntested = false;
        for (String modId : modIds) {
            CompatibilityStatus status = status(reports, modId);
            if (status == CompatibilityStatus.DISABLED) return CompatibilityStatus.DISABLED;
            if (status == CompatibilityStatus.UNTESTED_VERSION) anyUntested = true;
            if (status != CompatibilityStatus.ACTIVE) allActive = false;
        }
        if (allActive) return CompatibilityStatus.ACTIVE;
        return anyUntested ? CompatibilityStatus.UNTESTED_VERSION : CompatibilityStatus.UNSUPPORTED;
    }

    private static CompatibilityStatus status(List<CompatibilityReport> reports, String modId) {
        return reports.stream().filter(report -> report.modId().equals(modId)).map(CompatibilityReport::status).findFirst().orElse(CompatibilityStatus.DISABLED);
    }

    private static boolean isMca(Entity entity) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
    }

    private static void replaceStatus(List<CompatibilityReport> reports, String modId, CompatibilityStatus status, String detail) {
        for (int i = 0; i < reports.size(); i++) {
            CompatibilityReport report = reports.get(i);
            if (report.modId().equals(modId)) {
                reports.set(i, new CompatibilityReport(report.modId(), report.displayName(), report.testedVersion(),
                        report.actualVersion(), report.mechanism(), status, detail));
                return;
            }
        }
    }

    private enum Query { VAMPIRE, WEREWOLF, HUNTER }

    public record ProviderComponents(CompatibilityStatus facts, CompatibilityStatus predation, CompatibilityStatus lifecycle) {
        public static ProviderComponents disabled() {
            return new ProviderComponents(CompatibilityStatus.DISABLED, CompatibilityStatus.DISABLED, CompatibilityStatus.DISABLED);
        }

        public CompatibilityStatus combinedStatus() {
            if (facts == CompatibilityStatus.ACTIVE && predation == CompatibilityStatus.ACTIVE && lifecycle == CompatibilityStatus.ACTIVE) return CompatibilityStatus.ACTIVE;
            if (facts == CompatibilityStatus.ACTIVE || predation == CompatibilityStatus.ACTIVE || lifecycle == CompatibilityStatus.ACTIVE) return CompatibilityStatus.PARTIAL;
            if (facts == CompatibilityStatus.DISABLED && predation == CompatibilityStatus.DISABLED && lifecycle == CompatibilityStatus.DISABLED) return CompatibilityStatus.DISABLED;
            if (facts == CompatibilityStatus.UNTESTED_VERSION || predation == CompatibilityStatus.UNTESTED_VERSION || lifecycle == CompatibilityStatus.UNTESTED_VERSION) return CompatibilityStatus.UNTESTED_VERSION;
            if (facts == CompatibilityStatus.ERROR || predation == CompatibilityStatus.ERROR || lifecycle == CompatibilityStatus.ERROR) return CompatibilityStatus.ERROR;
            return CompatibilityStatus.UNSUPPORTED;
        }

        public boolean factualAuthorityAvailable() { return facts == CompatibilityStatus.ACTIVE; }
        public boolean predationAvailable() { return predation == CompatibilityStatus.ACTIVE; }
        public boolean lifecycleAvailable() { return lifecycle == CompatibilityStatus.ACTIVE; }
    }

    private record Spec(String modId, String name, String testedVersion, String mechanism) {}
}
