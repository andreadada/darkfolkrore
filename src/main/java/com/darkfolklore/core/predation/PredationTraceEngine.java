package com.darkfolklore.core.predation;

import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.FactResult;
import com.darkfolklore.core.compat.VampirePredationBridge;
import com.darkfolklore.core.compat.mca.McaRelationshipCategory;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeKey;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import com.darkfolklore.core.persistence.FolkloreSavedData;
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
        VampireBehaviorResolver.Resolution behavior = director == null
                ? behaviorFor(predator, kind)
                : new VampireBehaviorResolver.Resolution(director.behaviorProfile(), director.profileDetail());
        VampirePredationIntent intent = VampirePredationEngine.INSTANCE.sessionIntent(predator.getUUID())
                .orElse(director == null ? VampirePredationIntent.NONE : director.intent());
        double localRisk = director == null ? 0.0D : director.localRisk();
        double personalRisk = director == null ? 0.0D : director.personalRisk();
        Optional<UUID> selected = director == null ? Optional.empty() : director.target();
        PredationPhase phase = VampirePredationEngine.INSTANCE.sessionPhase(predator.getUUID())
                .orElseGet(() -> derivedPhase(level, predator, selected, director, wantsBlood, environmentAllowed));
        String detail = VampirePredationEngine.INSTANCE.sessionDetail(predator.getUUID())
                .orElse(director == null ? "director has not evaluated this predator yet" : director.reason());

        List<PredationTrace.Candidate> candidates = candidates(level, predator, kind, bridge, behavior, wantsBlood,
                environmentAllowed, localRisk, personalRisk);
        put(new PredationTrace(predator.getUUID(), kind, behavior.profile(), intent, behavior.detail(), phase,
                day, skyVisible, environmentAllowed, wantsBlood, localRisk, personalRisk, selected,
                detail, candidates, level.getGameTime()));
    }

    private static PredationPhase derivedPhase(ServerLevel level, Mob predator, Optional<UUID> selected,
                                                VampirePredationEngine.Diagnostic director,
                                                boolean wantsBlood, boolean environmentAllowed) {
        if (!environmentAllowed) return PredationPhase.IDLE;
        String detail = director == null ? "" : director.reason().toLowerCase(Locale.ROOT);
        if (detail.contains("cooldown") || detail.contains("budget exhausted")) return PredationPhase.COOLDOWN;
        if (selected.isEmpty()) {
            if (!wantsBlood && !detail.contains("non-feeding")) return PredationPhase.IDLE;
            return detail.contains("no socially") ? PredationPhase.SEARCHING : PredationPhase.IDLE;
        }
        Entity target = level.getEntity(selected.get());
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return PredationPhase.ABORTED;
        if (predator.distanceToSqr(living) <= 3.5D) return PredationPhase.ATTACKING;
        if (predator.getTarget() == living) return PredationPhase.PURSUING;
        return predator.getSensing().hasLineOfSight(living) ? PredationPhase.TARGET_SELECTED : PredationPhase.STALKING;
    }

    private static List<PredationTrace.Candidate> candidates(ServerLevel level, Mob predator, PredatorKind kind,
                                                              VampirePredationBridge bridge,
                                                              VampireBehaviorResolver.Resolution behavior,
                                                              boolean hungry, boolean environmentAllowed,
                                                              double localRisk, double personalRisk) {
        int radius = FolkloreConfig.VAMPIRE_PREDATION_RADIUS.get();
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                predator.getBoundingBox().inflate(radius, Math.max(4, radius / 2.0D), radius),
                value -> value != predator && value.isAlive());
        List<PredationTrace.Candidate> result = new ArrayList<>();
        long worldDay = Math.floorDiv(level.getGameTime(), 24000L);
        VampireBehaviorPolicy.Rates rates = behaviorRates();
        boolean behaviorEnabled = FolkloreConfig.VAMPIRE_BEHAVIOR_PROFILES.get();
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
                    result.add(rejected(target, animal, mca, false, false, 0, predator,
                            VampirePredationIntent.PROVIDER_OWNED,
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
            boolean isolated = witnesses == 0;
            boolean knowsIdentity = victimKnowsIdentity(level, target, predator);
            PredationPolicy.Candidate candidate = new PredationPolicy.Candidate(animal, mca, target.isAlive(), child,
                    closeFamily, supernatural, hunter, target.hasCustomName() && !mca, providerEligible,
                    witnesses, distance, isolated);
            PredationPolicy.Decision base = tamedAnimal
                    ? PredationPolicy.Decision.rejected("tamed animals are protected from autonomous feeding")
                    : !factsKnown
                    ? PredationPolicy.Decision.rejected("one or more supernatural provider facts are unknown")
                    : environmentAllowed
                    ? PredationPolicy.score(new PredationPolicy.Context(kind, true, localRisk, personalRisk), candidate)
                    : PredationPolicy.Decision.rejected("daylight exposure blocks autonomous predation");

            double adjustment = 0.0D;
            VampirePredationIntent predictedIntent = VampirePredationIntent.NONE;
            String reason = base.reason();
            boolean eligible = base.eligible();
            double score = base.score();
            if (eligible) {
                if (kind == PredatorKind.MCA_VAMPIRE) {
                    predictedIntent = VampirePredationIntent.PROVIDER_OWNED;
                    reason += "; behavior is observational because MCA target/AI is provider-owned";
                } else if (!behaviorEnabled) {
                    predictedIntent = hungry ? VampirePredationIntent.FEED : VampirePredationIntent.NONE;
                    if (predictedIntent == VampirePredationIntent.NONE) {
                        eligible = false;
                        score = Double.NEGATIVE_INFINITY;
                        reason = "behavior profiles disabled and provider reports no feeding pressure";
                    }
                } else {
                    VampireBehaviorPolicy.CandidateContext context = new VampireBehaviorPolicy.CandidateContext(
                            animal, mca, isolated, witnesses, knowsIdentity, localRisk, personalRisk);
                    VampireBehaviorPolicy.Preference preference = VampireBehaviorPolicy.preference(behavior.profile(), context);
                    adjustment = preference.scoreAdjustment();
                    predictedIntent = VampireBehaviorPolicy.intent(behavior.profile(), animal, knowsIdentity, hungry,
                            predator.getUUID(), target.getUUID(), worldDay, rates);
                    if (predictedIntent == VampirePredationIntent.NONE) {
                        eligible = false;
                        score = Double.NEGATIVE_INFINITY;
                        reason = preference.detail() + "; no feeding/non-feeding motive in current context";
                    } else {
                        score += adjustment;
                        eligible = score >= 10.0D;
                        reason += "; " + preference.detail() + "; intent=" + predictedIntent;
                        if (!eligible) reason += "; adjusted score below threshold";
                    }
                }
            }
            result.add(new PredationTrace.Candidate(target.getUUID(), target.getName().getString(), animal, mca,
                    providerEligible, knowsIdentity, witnesses, distance, adjustment, predictedIntent,
                    score, eligible, reason));
        }
        return List.copyOf(result);
    }

    private static PredationTrace.Candidate rejected(LivingEntity target, boolean animal, boolean mca,
                                                       boolean providerEligible, boolean knowsIdentity,
                                                       int witnesses, Mob predator,
                                                       VampirePredationIntent intent, String reason) {
        return new PredationTrace.Candidate(target.getUUID(), target.getName().getString(), animal, mca,
                providerEligible, knowsIdentity, witnesses, Math.sqrt(predator.distanceToSqr(target)),
                0.0D, intent, Double.NEGATIVE_INFINITY, false, reason);
    }

    private static boolean victimKnowsIdentity(ServerLevel level, LivingEntity victim, LivingEntity predator) {
        SocialKnowledgeKey key = new SocialKnowledgeKey(victim.getUUID(), predator.getUUID(), SecretType.VAMPIRE);
        return FolkloreSavedData.get(level.getServer()).social(key)
                .map(record -> record.state().strength() >= SocialKnowledgeState.CONFIRMED.strength())
                .orElse(false);
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

    private static VampireBehaviorResolver.Resolution behaviorFor(Mob predator, PredatorKind kind) {
        if (!FolkloreConfig.VAMPIRE_BEHAVIOR_PROFILES.get()) {
            return new VampireBehaviorResolver.Resolution(VampireBehaviorProfile.CONTROLLED,
                    "behavior profiles disabled by config");
        }
        Optional<String> personality = kind == PredatorKind.MCA_VAMPIRE
                ? CompatibilityManager.INSTANCE.mcaSocial().personality(predator) : Optional.empty();
        return VampireBehaviorResolver.resolve(kind, predator.getUUID(), personality,
                FolkloreConfig.PERSONALITY_MODIFIERS.get());
    }

    private static VampireBehaviorPolicy.Rates behaviorRates() {
        return new VampireBehaviorPolicy.Rates(
                FolkloreConfig.VAMPIRE_PREDATOR_KILL_CHANCE.get(),
                FolkloreConfig.VAMPIRE_RIPPER_OVERFEED_CHANCE.get(),
                FolkloreConfig.VAMPIRE_RIPPER_SPORT_KILL_CHANCE.get(),
                FolkloreConfig.VAMPIRE_VENGEFUL_KILL_CHANCE.get(),
                FolkloreConfig.VAMPIRE_RIPPER_MAX_EXTRA_FEEDS.get());
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
