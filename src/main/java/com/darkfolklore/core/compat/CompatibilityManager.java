package com.darkfolklore.core.compat;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.mca.McaSocialAdapter;
import com.darkfolklore.core.compat.mca.McaVampCompatAdapter;
import com.darkfolklore.core.compat.mcacapitals.McaCapitalsCompat;
import com.darkfolklore.core.knowledge.social.SecretType;
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
            new Spec("mca_vamp_compat", "MCA Reborn x Vampirism Compat", "2.0.12", "exact-version reflective service bridge"),
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
    private final McaSocialAdapter mcaSocial = new McaSocialAdapter();
    private final McaCapitalsCompat mcaCapitals = new McaCapitalsCompat();

    private CompatibilityManager() {}

    public synchronized void initialize() {
        List<CompatibilityReport> nextReports = new ArrayList<>();
        List<SupernaturalStateAdapter> nextAdapters = new ArrayList<>();
        ModList mods = ModList.get();
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
            CompatibilityStatus status = actual.equals(spec.testedVersion())
                    ? CompatibilityStatus.ACTIVE : CompatibilityStatus.UNTESTED_VERSION;
            String detail = status == CompatibilityStatus.ACTIVE ? "Exact audited version"
                    : "Adapter internals disabled until this version is audited";

            if (spec.modId().equals(McaVampCompatAdapter.MOD_ID) && status == CompatibilityStatus.ACTIVE) {
                McaVampCompatAdapter adapter = new McaVampCompatAdapter();
                try {
                    adapter.initialize();
                    nextAdapters.add(adapter);
                } catch (ReflectiveOperationException | LinkageError exception) {
                    status = CompatibilityStatus.ERROR;
                    detail = "Expected 2.0.12 signatures were not found: " + exception.getClass().getSimpleName();
                    DarkFolkloreCore.LOGGER.error("[compat/mca_vamp] Exact-version signature validation failed", exception);
                }
            }
            if (spec.modId().equals(McaSocialAdapter.MOD_ID) && status == CompatibilityStatus.ACTIVE
                    && !mcaSocial.initialize(actual)) {
                status = CompatibilityStatus.ERROR;
                detail = mcaSocial.statusDetail();
            }
            if (spec.modId().equals(McaCapitalsCompat.MOD_ID) && status == CompatibilityStatus.ACTIVE
                    && !mcaCapitals.initialize(actual)) {
                status = CompatibilityStatus.ERROR;
                detail = mcaCapitals.statusDetail();
            }
            nextReports.add(new CompatibilityReport(spec.modId(), spec.name(), spec.testedVersion(), actual,
                    spec.mechanism(), status, detail));
        }
        boolean vampirismExact = isExact(nextReports, "vampirism");
        boolean werewolvesExact = isExact(nextReports, "werewolves");
        if (vampirismExact) {
            try {
                Class<?> type = Class.forName("com.darkfolklore.core.compat.vampirism.VampirismAdapter", true,
                        CompatibilityManager.class.getClassLoader());
                nextAdapters.add((SupernaturalStateAdapter) type.getConstructor(boolean.class)
                        .newInstance(werewolvesExact));
            } catch (ReflectiveOperationException | LinkageError exception) {
                replaceStatus(nextReports, "vampirism", CompatibilityStatus.ERROR,
                        "Public API adapter failed to load: " + exception.getClass().getSimpleName());
                DarkFolkloreCore.LOGGER.error("[compat/vampirism] Public API adapter failed to load", exception);
            }
        }
        if (isExact(nextReports, "fieldguide")) {
            try {
                Object adapter = Class.forName("com.darkfolklore.core.compat.fieldguide.FieldGuideAdapter", true,
                        CompatibilityManager.class.getClassLoader()).getConstructor().newInstance();
                NeoForge.EVENT_BUS.register(adapter);
            } catch (ReflectiveOperationException | LinkageError exception) {
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

    public List<CompatibilityReport> reports() {
        return reports;
    }

    public Optional<CompatibilityReport> report(String modId) {
        return reports.stream().filter(report -> report.modId().equals(modId)).findFirst();
    }

    public McaSocialAdapter mcaSocial() { return mcaSocial; }

    public McaCapitalsCompat mcaCapitals() { return mcaCapitals; }

    public void clearRuntimeCaches() { mcaCapitals.clearCache(); }

    public FactResult isVampire(Entity entity) {
        return aggregate(entity, Query.VAMPIRE);
    }

    public FactResult isWerewolf(Entity entity) {
        return aggregate(entity, Query.WEREWOLF);
    }

    public FactResult isHunter(Entity entity) {
        return aggregate(entity, Query.HUNTER);
    }

    public Optional<UUID> conversionSource(Entity entity, SecretType type) {
        return stateAdapters.stream().map(adapter -> adapter.conversionSource(entity, type))
                .flatMap(Optional::stream).findFirst();
    }

    private FactResult aggregate(Entity entity, Query query) {
        boolean hadFalse = false;
        boolean hadUnknown = false;
        for (SupernaturalStateAdapter adapter : stateAdapters) {
            FactResult value = switch (query) {
                case VAMPIRE -> adapter.isVampire(entity);
                case WEREWOLF -> adapter.isWerewolf(entity);
                case HUNTER -> adapter.isHunter(entity);
            };
            if (value == FactResult.TRUE) return value;
            if (value == FactResult.FALSE) hadFalse = true;
            if (value == FactResult.UNKNOWN) hadUnknown = true;
        }
        if (hadUnknown) return FactResult.UNKNOWN;
        return hadFalse ? FactResult.FALSE : FactResult.NOT_APPLICABLE;
    }

    private static boolean isExact(List<CompatibilityReport> reports, String modId) {
        return reports.stream().anyMatch(report -> report.modId().equals(modId)
                && report.status() == CompatibilityStatus.ACTIVE);
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
    private record Spec(String modId, String name, String testedVersion, String mechanism) {}
}
