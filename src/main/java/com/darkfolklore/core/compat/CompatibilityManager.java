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
            new Spec("mca_vamp_compat", "MCA Reborn x Vampirism Compat", "2.0.12",
                    "exact-version state + native predation/lifecycle bridge"),
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

        // Phase one is immutable version discovery. Component failures below must not disable unrelated bridges.
        for (Spec spec : SPECS) {
            Optional<? extends net.neoforged.fml.ModContainer> container = mods.getModContainerById(spec.modId());
            if (container.isEmpty()) {
                nextReports.add(new CompatibilityReport(spec.modId(), spec.name(), spec.testedVersion(), "-",
                        spec.mechanism(), CompatibilityStatus.DISABLED, "Optional mod is not installed"));
                continue;
            }
            String actual = container.get().getModInfo().getVersion().toString();
            CompatibilityStatus status = actual.equals(spec.testedVersion())
                    ? CompatibilityStatus.ACTIVE : CompatibilityStatus.UNTESTED_VERSION;
            String detail = status == CompatibilityStatus.ACTIVE ? "Exact audited version"
                    : "Adapter internals disabled until this version is audited";

            nextReports.add(new CompatibilityReport(spec.modId(), spec.name(), spec.testedVersion(), actual,
                    spec.mechanism(), status, detail));
        }

        boolean vampirismExact = isExact(nextReports, "vampirism");
        boolean werewolvesExact = isExact(nextReports, "werewolves");
        boolean mcaExact = isExact(nextReports, McaSocialAdapter.MOD_ID);
        boolean mcaCapitalsExact = isExact(nextReports, McaCapitalsCompat.MOD_ID);
        boolean providerExact = isExact(nextReports, McaVampCompatAdapter.MOD_ID);
        boolean fieldGuideExact = isExact(nextReports, "fieldguide");
        boolean exactMcaVampStack = vampirismExact && mcaExact && providerExact;

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

        if (exactMcaVampStack) {
            CompatibilityStatus facts = CompatibilityStatus.ERROR;
            CompatibilityStatus predation = CompatibilityStatus.ERROR;
            CompatibilityStatus lifecycle = CompatibilityStatus.ERROR;
            try {
                McaVampCompatAdapter adapter = new McaVampCompatAdapter();
                adapter.initialize();
                mcaFactAdapter = adapter;
                facts = CompatibilityStatus.ACTIVE;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                DarkFolkloreCore.LOGGER.error("[compat/mca_vamp] Exact factual bridge failed to load", exception);
            }
            try {
                Object adapter = Class.forName("com.darkfolklore.core.compat.vampirism.VampirePredationCompat", true,
                        CompatibilityManager.class.getClassLoader()).getConstructor().newInstance();
                if (!(adapter instanceof VampirePredationBridge bridge)) {
                    throw new LinkageError("VampirePredationCompat does not implement VampirePredationBridge");
                }
                vampirePredationBridge = bridge;
                NeoForge.EVENT_BUS.register(adapter);
                predation = CompatibilityStatus.ACTIVE;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                vampirePredationBridge = VampirePredationBridge.DISABLED;
                DarkFolkloreCore.LOGGER.error("[compat/vampire_predation] Exact provider bridge failed to load", exception);
            }

            try {
                Object adapter = Class.forName("com.darkfolklore.core.compat.mca.McaVampireLifecycleCompat", true,
                        CompatibilityManager.class.getClassLoader()).getConstructor().newInstance();
                if (!(adapter instanceof McaVampireLifecycleBridge bridge)) {
                    throw new LinkageError("McaVampireLifecycleCompat does not implement McaVampireLifecycleBridge");
                }
                mcaVampireLifecycleBridge = bridge;
                lifecycle = CompatibilityStatus.ACTIVE;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                mcaVampireLifecycleBridge = McaVampireLifecycleBridge.DISABLED;
                DarkFolkloreCore.LOGGER.error("[compat/mca_vamp_lifecycle] Exact provider bridge failed to load", exception);
            }

            mcaFactStatus = facts;
            mcaVampComponents = new ProviderComponents(facts, predation, lifecycle);
            CompatibilityStatus combined = mcaVampComponents.combinedStatus();
            replaceStatus(nextReports, McaVampCompatAdapter.MOD_ID, combined,
                    "Exact audited stack; facts=" + facts + ", predation=" + predation + ", lifecycle=" + lifecycle);
        } else {
            CompatibilityStatus unavailable = unavailableMcaAuthorityStatus(nextReports);
            mcaFactStatus = unavailable;
            mcaVampComponents = new ProviderComponents(unavailable, unavailable, unavailable);
            if (providerExact) {
                replaceStatus(nextReports, McaVampCompatAdapter.MOD_ID, unavailable,
                        "Exact provider requires audited Vampirism + MCA versions; components disabled fail-closed");
            }
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
        reports.forEach(report -> DarkFolkloreCore.LOGGER.info("[compat/{}] tested={} actual={} status={}",
                report.modId(), report.testedVersion(), report.actualVersion(), report.status()));
    }

    public List<CompatibilityReport> reports() { return reports; }

    public Optional<CompatibilityReport> report(String modId) {
        return reports.stream().filter(report -> report.modId().equals(modId)).findFirst();
    }

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
            FactResult provider = mcaFactAdapter == null ? FactResult.NOT_APPLICABLE
                    : query(mcaFactAdapter, entity, query);
            return SupernaturalFactResolver.resolveMca(mcaFactStatus, provider);
        }
        List<FactResult> results = new ArrayList<>();
        for (SupernaturalStateAdapter adapter : stateAdapters) {
            results.add(query(adapter, entity, query));
        }
        return SupernaturalFactResolver.resolveGeneric(results);
    }

    private static FactResult query(SupernaturalStateAdapter adapter, Entity entity, Query query) {
        return switch (query) {
            case VAMPIRE -> adapter.isVampire(entity);
            case WEREWOLF -> adapter.isWerewolf(entity);
            case HUNTER -> adapter.isHunter(entity);
        };
    }

    private static boolean isExact(List<CompatibilityReport> reports, String modId) {
        return reports.stream().anyMatch(report -> report.modId().equals(modId)
                && report.status() == CompatibilityStatus.ACTIVE);
    }

    private static String actualVersion(List<CompatibilityReport> reports, String modId) {
        return reports.stream().filter(report -> report.modId().equals(modId))
                .map(CompatibilityReport::actualVersion).findFirst().orElse("-");
    }

    private static CompatibilityStatus unavailableMcaAuthorityStatus(List<CompatibilityReport> reports) {
        CompatibilityStatus provider = status(reports, McaVampCompatAdapter.MOD_ID);
        if (provider == CompatibilityStatus.DISABLED) return CompatibilityStatus.DISABLED;
        if (provider == CompatibilityStatus.UNTESTED_VERSION
                || status(reports, "vampirism") == CompatibilityStatus.UNTESTED_VERSION
                || status(reports, "mca") == CompatibilityStatus.UNTESTED_VERSION) {
            return CompatibilityStatus.UNTESTED_VERSION;
        }
        return CompatibilityStatus.UNSUPPORTED;
    }

    private static CompatibilityStatus status(List<CompatibilityReport> reports, String modId) {
        return reports.stream().filter(report -> report.modId().equals(modId))
                .map(CompatibilityReport::status).findFirst().orElse(CompatibilityStatus.DISABLED);
    }

    private static boolean isMca(Entity entity) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
    }

    private static void replaceStatus(List<CompatibilityReport> reports, String modId,
                                      CompatibilityStatus status, String detail) {
        for (int i = 0; i < reports.size(); i++) {
            CompatibilityReport report = reports.get(i);
            if (report.modId().equals(modId)) {
                reports.set(i, new CompatibilityReport(report.modId(), report.displayName(),
                        report.testedVersion(), report.actualVersion(), report.mechanism(), status, detail));
                return;
            }
        }
    }

    private enum Query { VAMPIRE, WEREWOLF, HUNTER }
    public record ProviderComponents(CompatibilityStatus facts, CompatibilityStatus predation,
                                     CompatibilityStatus lifecycle) {
        public static ProviderComponents disabled() {
            return new ProviderComponents(CompatibilityStatus.DISABLED, CompatibilityStatus.DISABLED,
                    CompatibilityStatus.DISABLED);
        }

        public CompatibilityStatus combinedStatus() {
            int active = (facts == CompatibilityStatus.ACTIVE ? 1 : 0)
                    + (predation == CompatibilityStatus.ACTIVE ? 1 : 0)
                    + (lifecycle == CompatibilityStatus.ACTIVE ? 1 : 0);
            return active == 3 ? CompatibilityStatus.ACTIVE
                    : active > 0 ? CompatibilityStatus.PARTIAL : CompatibilityStatus.ERROR;
        }

        public boolean factualAuthorityAvailable() { return facts == CompatibilityStatus.ACTIVE; }
        public boolean predationAvailable() { return predation == CompatibilityStatus.ACTIVE; }
        public boolean lifecycleAvailable() { return lifecycle == CompatibilityStatus.ACTIVE; }
    }
    private record Spec(String modId, String name, String testedVersion, String mechanism) {}
}
