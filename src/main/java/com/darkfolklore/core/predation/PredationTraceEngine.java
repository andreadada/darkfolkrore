package com.darkfolklore.core.predation;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.FactResult;
import com.darkfolklore.core.compat.VampirePredationBridge;
import com.darkfolklore.core.compat.mca.McaRelationshipCategory;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.society.SocialEntityClassifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.*;

/**
 * Read-only observability layer for the predation director. It never creates targets, paths, bites or provider
 * facts; it only explains the current decision in enough detail to debug a real modpack instance.
 */
public final class PredationTraceEngine {
    public static final PredationTraceEngine INSTANCE = new PredationTraceEngine();
    private static final int MAX_TRACES = 128;
    private static final int MAX_CANDIDATES = 16;
    private static final Set<McaRelationshipCategory> CLOSE_FAMILY = EnumSet.of(
            McaRelationshipCategory.SPOUSE, McaRelationshipCategory.SOURCE_IS_PARENT,
            McaRelationshipCategory.SOURCE_IS_CHILD, McaRelationshipCategory.SIBLING);
    private final LinkedHashMap<UUID, PredationTrace> traces = new LinkedHashMap<>();

    private PredationTraceEngine() {}

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!FolkloreConfig.VAMPIRE_PREDATION.get() || !(event.getEntity() instanceof Mob predator)
                || !(predator.level() instanceof ServerLevel level) || !predator.isAlive()) return;
        int interval = FolkloreConfig.VAMPIRE_PREDATION_SCAN_INTERVAL.get();
        if (Math.floorMod(predator.tickCount + predator.getId(), interval) != 0) return;

        VampirePredationBridge bridge = CompatibilityManager.INSTANCE.vampirePredation();
        if (!bridge.runtimeAvailable()) return;
        PredatorKind kind = bridge.predatorKind(predator);
        if (kind == PredatorKind.NONE) return;

        boolean day = level.isDay();
        boolean skyVisible = level.canSeeSky(predator.blockPosition());
        boolean environmentAllowed = PredationPolicy.environmentAllowsPredation(day, skyVisible);
        boolean wantsBlood = bridge.wantsBlood(predator);
        VampirePredationEngine.Diagnostic director = VampirePredationEngine.INSTANCE
                .diagnostic(predator.getUUID()).orElse(null);
        double localRisk = director == null ? 0.0D : director.localRisk();
        double personalRisk = director == null ? 0.0D : director.personalRisk();
        Optional<UUID> selected = director == null ? Optional.empty() : director.target();
        PredationPhase phase = VampirePredationEngine.INSTANCE.sessionPhase(predator.getUUID())
                .orElseGet(() -> derivedPhase(level, predator, selected, director, wantsBlood, environmentAllowed));
        String detail = VampirePredationEngine.INSTANCE.sessionDetail(predator.getUUID())
                .orElse(director == null ? "director has not evaluated this predator yet" : director.reason());

        List<PredationTrace.Candidate> candidates = candidates(level, predator, kind, bridge, environmentAllowed,
                localRisk, personalRisk);
        put(new PredationTrace(predator.getUUID(), kind, phase, day, skyVisible, environmentAllowed, wantsBlood,
                localRisk, personalRisk, selected, detail, candidates, level.getGameTime()));
    }

    private static PredationPhase derivedPhase(ServerLevel level, Mob predator, Optional<UUID> selected,
                                                VampirePredationEngine.Diagnostic director,
                                                boolean wantsBlood, boolean environmentAllowed) {
        if (!environmentAllowed || !wantsBlood) return PredationPhase.IDLE;
        String detail = director == null ? "" : director.reason().toLowerCase(Locale.ROOT);
        if (detail.contains("cooldown") || detail.contains("budget exhausted")) return PredationPhase.COOLDOWN;
        if (selected.isEmpty()) return detail.contains("no socially") ? PredationPhase.SEARCHING : PredationPhase.IDLE;
        Entity target = level.getEntity(selected.get());
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return PredationPhase.ABORTED;
        if (predator.distanceToSqr(living) <= 3.5D) return PredationPhase.ATTACKING;
        if (predator.getTarget() == living) return PredationPhase.PURSUING;
        return predator.getSensing().hasLineOfSight(living) ? PredationPhase.TARGET_SELECTED : PredationPhase.STALKING;
    }

    private static List<PredationTrace.Candidate> candidates(ServerLevel level, Mob predator, PredatorKind kind,
                                                              VampirePredationBridge bridge,
                                                              boolean environmentAllowed, double localRisk,
                                                              double personalRisk) {
        int radius = FolkloreConfig.VAMPIRE_PREDATION_RADIUS.get();
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                predator.getBoundingBox().inflate(radius, Math.max(4, radius / 2.0D), radius),
                value -> value != predator && value.isAlive());
        List<PredationTrace.Candidate> result = new ArrayList<>();
        for (LivingEntity target : nearby) {
            if (result.size() >= MAX_CANDIDATES) break;
            boolean animal = target instanceof Animal;
            boolean mca = isMca(target);
            if (!animal && !mca) continue;
            boolean tamedAnimal = animal && target instanceof TamableAnimal tame && tame.isTame();
            boolean child = target instanceof AgeableMob ageable && ageable.isBaby();
            boolean closeFamily = false;
            if (kind == PredatorKind.MCA_VAMPIRE && mca) {
                McaRelationshipCategory relationship = CompatibilityManager.INSTANCE.mcaSocial()
                        .relationship(target, predator).relationship();
                if (relationship == McaRelationshipCategory.UNKNOWN
                        || relationship == McaRelationshipCategory.NOT_APPLICABLE) {
                    result.add(rejected(target, animal, mca, false, 0, predator,
                            "MCA relationship is unknown/not applicable"));
                    continue;
                }
                closeFamily = CLOSE_FAMILY.contains(relationship);
            }
            FactResult vampire = CompatibilityManager.INSTANCE.isVampire(target);
            FactResult werewolf = CompatibilityManager.INSTANCE.isWerewolf(target);
            FactResult hunterFact = CompatibilityManager.INSTANCE.isHunter(target);
            boolean factsKnown = PredationPolicy.factsKnown(vampire, werewolf, hunterFact);
            boolean supernatural = vampire == FactResult.TRUE || werewolf == FactResult.TRUE;
            boolean hunter = hunterFact == FactResult.TRUE;
            boolean providerEligible = kind == PredatorKind.WILD_VAMPIRISM
                    ? bridge.canWildFeed(predator, target)
                    : animal ? bridge.canMcaAnimalFeed(predator, target) : bridge.canMcaVampireTarget(predator, target);
            int witnesses = visibleSocialWitnesses(level, predator, target);
            double distance = Math.sqrt(predator.distanceToSqr(target));
            PredationPolicy.Candidate candidate = new PredationPolicy.Candidate(animal, mca, target.isAlive(), child,
                    closeFamily, supernatural, hunter, target.hasCustomName() && !mca, providerEligible,
                    witnesses, distance, witnesses == 0);
            PredationPolicy.Decision decision = tamedAnimal
                    ? PredationPolicy.Decision.rejected("tamed animals are protected from autonomous feeding")
                    : !factsKnown
                    ? PredationPolicy.Decision.rejected("one or more supernatural provider facts are unknown")
                    : environmentAllowed
                    ? PredationPolicy.score(new PredationPolicy.Context(kind, true, localRisk, personalRisk), candidate)
                    : PredationPolicy.Decision.rejected("daylight exposure blocks autonomous predation");
            result.add(new PredationTrace.Candidate(target.getUUID(), target.getName().getString(), animal, mca,
                    providerEligible, witnesses, distance, decision.score(), decision.eligible(), decision.reason()));
        }
        return List.copyOf(result);
    }

    private static PredationTrace.Candidate rejected(LivingEntity target, boolean animal, boolean mca,
                                                       boolean providerEligible, int witnesses, Mob predator,
                                                       String reason) {
        return new PredationTrace.Candidate(target.getUUID(), target.getName().getString(), animal, mca,
                providerEligible, witnesses, Math.sqrt(predator.distanceToSqr(target)),
                Double.NEGATIVE_INFINITY, false, reason);
    }

    private static int visibleSocialWitnesses(ServerLevel level, LivingEntity predator, LivingEntity target) {
        int count = 0;
        int radius = Math.min(16, FolkloreConfig.WITNESS_RADIUS.get());
        for (LivingEntity observer : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius),
                value -> value.isAlive() && value != predator && value != target && !value.isSleeping()
                        && SocialEntityClassifier.isSocial(value))) {
            if (observer.hasLineOfSight(predator) && ++count >= 8) break;
        }
        return count;
    }

    private static boolean isMca(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.getNamespace().equals("mca");
    }

    private void put(PredationTrace trace) {
        traces.remove(trace.predator());
        traces.put(trace.predator(), trace);
        while (traces.size() > MAX_TRACES) traces.remove(traces.keySet().iterator().next());
    }

    public Optional<PredationTrace> trace(UUID predator) { return Optional.ofNullable(traces.get(predator)); }
    public int tracked() { return traces.size(); }
    public void clearRuntimeState() { traces.clear(); }
}
