package com.darkfolklore.core.predation;

import com.darkfolklore.core.api.event.ConfirmedLivingDeathEvent;
import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.FactResult;
import com.darkfolklore.core.compat.VampirePredationBridge;
import com.darkfolklore.core.compat.mca.McaRelationshipCategory;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.investigation.EvidenceRecord;
import com.darkfolklore.core.investigation.IncidentFact;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.persistence.InvestigationSavedData;
import com.darkfolklore.core.persistence.WorldPosition;
import com.darkfolklore.core.society.SocialEntityClassifier;
import com.darkfolklore.core.society.organization.OrganizationType;
import com.darkfolklore.core.society.rumor.RumorEngine;
import com.darkfolklore.core.society.story.*;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.society.village.VillageSocietyState;
import com.darkfolklore.core.society.witness.WitnessEngine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * Bounded social director for vampire predation. It chooses risk-aware prey and, for wild Vampirism mobs only,
 * can add a stable behavioral motive. Factual vampire state, infection, conversion and MCA-vampire AI remain
 * provider-owned.
 */
public final class VampirePredationEngine {
    public static final VampirePredationEngine INSTANCE = new VampirePredationEngine();
    private static final Set<McaRelationshipCategory> CLOSE_FAMILY = EnumSet.of(
            McaRelationshipCategory.SPOUSE, McaRelationshipCategory.SOURCE_IS_PARENT,
            McaRelationshipCategory.SOURCE_IS_CHILD, McaRelationshipCategory.SIBLING);

    private final Map<UUID, PredationSession> sessions = new HashMap<>();
    private final Map<UUID, Long> predatorCooldowns = new HashMap<>();
    private final Map<UUID, Long> victimCooldowns = new HashMap<>();
    private final Map<String, ArrayDeque<Long>> regionalFeeds = new HashMap<>();
    private final Map<FeedKey, Long> observedFeeds = new HashMap<>();
    private final Map<FeedKey, PendingLethalIntent> lethalIntents = new HashMap<>();
    private final LinkedHashMap<UUID, Diagnostic> diagnostics = new LinkedHashMap<>();

    private VampirePredationEngine() {}

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!FolkloreConfig.VAMPIRE_PREDATION.get() || !(event.getEntity() instanceof Mob predator)
                || !(predator.level() instanceof ServerLevel level) || !predator.isAlive()) return;
        if (predator instanceof AgeableMob ageablePredator && ageablePredator.isBaby()) return;
        int interval = FolkloreConfig.VAMPIRE_PREDATION_SCAN_INTERVAL.get();
        if (Math.floorMod(predator.tickCount + predator.getId(), interval) != 0) return;
        VampirePredationBridge bridge = CompatibilityManager.INSTANCE.vampirePredation();
        if (!bridge.runtimeAvailable()) return;
        PredatorKind kind = bridge.predatorKind(predator);
        if (kind == PredatorKind.NONE) return;
        long now = level.getGameTime();
        boolean environmentAllowed = PredationPolicy.environmentAllowsPredation(
                level.isDay(), level.canSeeSky(predator.blockPosition()));
        VampireBehaviorResolver.Resolution behavior = behaviorFor(predator, kind);

        PredationSession current = sessions.get(predator.getUUID());
        if (current != null) {
            if (continueSession(level, predator, current, bridge, now, environmentAllowed)) return;
            endSession(predator, current, bridge);
        }

        if (!environmentAllowed) {
            remember(predator, kind, behavior, VampirePredationIntent.NONE, null, 0, 0,
                    "daylight exposure blocks autonomous predation", now);
            return;
        }

        boolean hungry = bridge.wantsBlood(predator);
        boolean mayActWithoutHunger = FolkloreConfig.VAMPIRE_BEHAVIOR_PROFILES.get()
                && kind == PredatorKind.WILD_VAMPIRISM
                && VampireBehaviorPolicy.mayActWithoutHunger(behavior.profile());
        if (!hungry && !mayActWithoutHunger) {
            remember(predator, kind, behavior, VampirePredationIntent.NONE, null, 0, 0,
                    "provider reports no feeding pressure and profile has no non-feeding motive", now);
            return;
        }
        if (now < predatorCooldowns.getOrDefault(predator.getUUID(), 0L)) {
            remember(predator, kind, behavior, VampirePredationIntent.NONE, null, 0, 0,
                    "predator cooldown", now);
            return;
        }
        String region = VillageKey.at(level, predator.blockPosition()).serialized();
        if (!regionalBudgetAvailable(region, now)) {
            remember(predator, kind, behavior, VampirePredationIntent.NONE, null, 0, 0,
                    "local anti-chaos feeding/violence budget exhausted", now);
            return;
        }

        double localRisk = localRisk(level, predator);
        double personalRisk = personalRisk(level, predator);
        Choice choice = choose(level, predator, kind, bridge, behavior, hungry,
                localRisk, personalRisk, now, environmentAllowed);
        if (choice == null) {
            remember(predator, kind, behavior, VampirePredationIntent.NONE, null, localRisk, personalRisk,
                    hungry ? "no socially/provider-valid prey"
                            : "no socially/provider-valid prey with a non-feeding behavioral motive", now);
            return;
        }

        boolean directedTarget = false;
        if (kind == PredatorKind.WILD_VAMPIRISM && !choice.animal()) {
            LivingEntity existing = predator.getTarget();
            boolean alreadyOwnsChosenTarget = existing != null && existing.isAlive()
                    && existing.getUUID().equals(choice.target().getUUID());
            if (!bridge.requestWildHuntTarget(predator, choice.target())) {
                remember(predator, kind, behavior, choice.intent(), choice.target(), localRisk, personalRisk,
                        "selected mca_civilian but another live combat target/provider guard refused hunt steering", now);
                return;
            }
            directedTarget = !alreadyOwnsChosenTarget;
        }

        long lifetime = choice.intent().lethal() ? 480L : 240L;
        PredationSession session = new PredationSession(choice.target().getUUID(), choice.animal(), kind,
                directedTarget, now, now + lifetime, behavior.profile(), choice.intent());
        if (choice.intent() == VampirePredationIntent.KILL_FOR_SPORT) {
            session.transition(PredationPhase.KILLING, "behavioral hunt does not require a blood drain");
            long sportCooldown = Math.max(6000L, FolkloreConfig.VAMPIRE_PREDATION_COOLDOWN.get() * 4L);
            predatorCooldowns.put(predator.getUUID(), now + sportCooldown);
        }
        sessions.put(predator.getUUID(), session);
        if (choice.intent().lethal()) rememberLethalIntent(predator, choice.target(), session, now);
        remember(predator, kind, behavior, choice.intent(), choice.target(), localRisk, personalRisk,
                (directedTarget ? "directed " : "selected ") + choice.reason()
                        + " behaviorAdjustment=" + Math.round(choice.behaviorAdjustment())
                        + " score=" + Math.round(choice.score()), now);
    }

    private boolean continueSession(ServerLevel level, Mob predator, PredationSession session,
                                    VampirePredationBridge bridge, long now, boolean environmentAllowed) {
        if (!environmentAllowed) return abort(session, "predator became exposed to daylight");
        if (now > session.expiresAt() || !predator.isAlive()) return abort(session, "session expired or predator died");
        Entity loaded = level.getEntity(session.target());
        if (!(loaded instanceof LivingEntity target) || !target.isAlive()) return abort(session, "target is no longer alive/loaded");

        if (session.phase() == PredationPhase.KILLING) {
            if (session.kind() != PredatorKind.WILD_VAMPIRISM || !session.directedTarget()) {
                return abort(session, "lethal target steering is valid only for a directed wild-vampire session");
            }
            if (!bridge.requestWildCombatTarget(predator, target)) {
                return abort(session, "lethal wild target steering was revoked by a different live combat target");
            }
            if (predator.distanceToSqr(target) <= 3.5D) {
                session.note("lethal combat target is in melee range; native Vampirism combat owns damage");
            } else if (predator.getSensing().hasLineOfSight(target)) {
                session.note("lethal combat pursuit");
            } else {
                session.note("lethal combat stalking hidden target");
            }
            return true;
        }

        if (session.confirmedFeeds() == 0 && session.intent() != VampirePredationIntent.KILL_FOR_SPORT
                && now < victimCooldowns.getOrDefault(target.getUUID(), 0L)) {
            return abort(session, "victim cooldown");
        }

        if (session.kind() == PredatorKind.MCA_VAMPIRE && !session.animal()) {
            VampirePredationBridge.ProviderSnapshot snapshot = bridge.providerSnapshot(predator);
            boolean providerOwns = predator.getTarget() == target;
            boolean allowed = PredationPolicy.mayContinueMcaSession(snapshot.available(), snapshot.converted(),
                    snapshot.curing(), bridge.canMcaVampireTarget(predator, target), providerOwns);
            if (!allowed) return abort(session, "waiting for/provider-native MCA target no longer owns prey");
            updateMovementPhase(predator, target, session);
            return true;
        }

        boolean canFeed = session.kind() == PredatorKind.WILD_VAMPIRISM
                ? bridge.canWildFeed(predator, target) : bridge.canMcaAnimalFeed(predator, target);
        if (!canFeed) {
            if (session.kind() == PredatorKind.WILD_VAMPIRISM && session.phase() == PredationPhase.OVERFEEDING) {
                long sinceFeed = now - session.lastFeedAt();
                if (sinceFeed >= 0L && sinceFeed < 80L) {
                    if (!bridge.requestWildCombatTarget(predator, target)) {
                        return abort(session, "ripper lost its post-feed target while waiting for another bite window");
                    }
                    session.note("ripper is holding the victim while waiting for another provider bite window");
                    return true;
                }
                session.transition(PredationPhase.KILLING,
                        "victim is no longer provider-biteable; ripper continues as lethal combat");
                session.extendUntil(now + 240L);
                return bridge.requestWildCombatTarget(predator, target);
            }
            return abort(session, "provider rejected feeding target");
        }

        if (session.kind() == PredatorKind.WILD_VAMPIRISM && session.directedTarget()
                && !bridge.requestWildHuntTarget(predator, target)) {
            return abort(session, "wild target steering was revoked");
        }

        boolean lineOfSight = predator.getSensing().hasLineOfSight(target);
        if (!lineOfSight) {
            if (session.kind() == PredatorKind.WILD_VAMPIRISM && session.directedTarget()) {
                if (session.phase() == PredationPhase.OVERFEEDING) {
                    session.note("ripper is pathing toward the same hidden victim for another feed");
                } else {
                    session.transition(PredationPhase.STALKING, "pathing toward hidden target");
                }
                return true;
            }
            return abort(session, "opportunistic prey left line of sight");
        }
        if (predator.distanceToSqr(target) > 3.5D) {
            if (session.phase() == PredationPhase.OVERFEEDING) {
                session.note("ripper is closing distance for another feed");
            } else {
                session.transition(PredationPhase.PURSUING, "closing distance");
            }
            return true;
        }

        session.transition(PredationPhase.ATTACKING, session.phase() == PredationPhase.OVERFEEDING
                ? "ripper reached the victim for another feed" : "in feeding range");
        boolean fed = session.kind() == PredatorKind.WILD_VAMPIRISM
                ? bridge.performWildFeed(predator, target)
                : bridge.performMcaAnimalFeed(predator, target);
        if (!fed && sessions.containsKey(predator.getUUID())) {
            session.transition(PredationPhase.ABORTED, "provider feed action returned false");
        }
        return fed && sessions.get(predator.getUUID()) == session;
    }

    private static void updateMovementPhase(Mob predator, LivingEntity target, PredationSession session) {
        if (predator.distanceToSqr(target) <= 3.5D) {
            session.transition(PredationPhase.ATTACKING, "provider-native MCA AI reached target");
        } else if (predator.getSensing().hasLineOfSight(target)) {
            session.transition(PredationPhase.PURSUING, "provider-native MCA AI pursuing target");
        } else {
            session.transition(PredationPhase.STALKING, "provider-native MCA AI tracking hidden target");
        }
    }

    private static boolean abort(PredationSession session, String reason) {
        session.transition(PredationPhase.ABORTED, reason);
        return false;
    }

    private Choice choose(ServerLevel level, Mob predator, PredatorKind kind, VampirePredationBridge bridge,
                          VampireBehaviorResolver.Resolution behavior, boolean hungry,
                          double localRisk, double personalRisk, long now, boolean environmentAllowed) {
        int radius = FolkloreConfig.VAMPIRE_PREDATION_RADIUS.get();
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                predator.getBoundingBox().inflate(radius, Math.max(4, radius / 2.0D), radius),
                target -> target != predator && target.isAlive());
        Choice best = null;
        long worldDay = Math.floorDiv(now, 24000L);
        VampireBehaviorPolicy.Rates rates = behaviorRates();
        boolean behaviorEnabled = FolkloreConfig.VAMPIRE_BEHAVIOR_PROFILES.get();
        for (LivingEntity target : candidates) {
            if (now < victimCooldowns.getOrDefault(target.getUUID(), 0L)) continue;
            boolean animal = target instanceof Animal;
            boolean mca = isMca(target);
            if (!animal && !mca) continue;
            if (animal && target instanceof TamableAnimal tame && tame.isTame()) continue;
            boolean child = target instanceof AgeableMob ageable && ageable.isBaby();
            boolean closeFamily = false;
            if (kind == PredatorKind.MCA_VAMPIRE && mca) {
                McaRelationshipCategory relationship = CompatibilityManager.INSTANCE.mcaSocial()
                        .relationship(target, predator).relationship();
                if (relationship == McaRelationshipCategory.UNKNOWN
                        || relationship == McaRelationshipCategory.NOT_APPLICABLE) continue;
                closeFamily = CLOSE_FAMILY.contains(relationship);
            }
            FactResult vampireFact = CompatibilityManager.INSTANCE.isVampire(target);
            FactResult werewolfFact = CompatibilityManager.INSTANCE.isWerewolf(target);
            FactResult hunterFact = CompatibilityManager.INSTANCE.isHunter(target);
            if (!PredationPolicy.factsKnown(vampireFact, werewolfFact, hunterFact)) continue;
            boolean vampire = vampireFact == FactResult.TRUE;
            boolean werewolf = werewolfFact == FactResult.TRUE;
            boolean hunter = hunterFact == FactResult.TRUE;
            boolean providerEligible = kind == PredatorKind.WILD_VAMPIRISM
                    ? bridge.canWildFeed(predator, target)
                    : animal ? bridge.canMcaAnimalFeed(predator, target) : bridge.canMcaVampireTarget(predator, target);
            int witnesses = visibleWitnesses(level, predator, target);
            double distance = Math.sqrt(predator.distanceToSqr(target));
            boolean isolated = witnesses == 0;
            PredationPolicy.Candidate candidate = new PredationPolicy.Candidate(animal, mca, true, child,
                    closeFamily, vampire || werewolf, hunter, target.hasCustomName() && !mca,
                    providerEligible, witnesses, distance, isolated);
            PredationPolicy.Decision base = PredationPolicy.score(
                    new PredationPolicy.Context(kind, environmentAllowed, localRisk, personalRisk), candidate);
            if (!base.eligible()) continue;

            boolean knowsIdentity = victimKnowsIdentity(level, target, predator);
            VampirePredationIntent intent;
            double behaviorAdjustment = 0.0D;
            String behaviorDetail = "behavior profile does not steer provider-owned MCA AI";
            if (kind == PredatorKind.MCA_VAMPIRE) {
                intent = VampirePredationIntent.PROVIDER_OWNED;
            } else if (!behaviorEnabled) {
                intent = hungry ? VampirePredationIntent.FEED : VampirePredationIntent.NONE;
                behaviorDetail = "behavior profiles disabled";
            } else {
                VampireBehaviorPolicy.CandidateContext context = new VampireBehaviorPolicy.CandidateContext(
                        animal, mca, isolated, witnesses, knowsIdentity, localRisk, personalRisk);
                VampireBehaviorPolicy.Preference preference = VampireBehaviorPolicy.preference(behavior.profile(), context);
                behaviorAdjustment = preference.scoreAdjustment();
                behaviorDetail = preference.detail();
                intent = VampireBehaviorPolicy.intent(behavior.profile(), animal, knowsIdentity, hungry,
                        predator.getUUID(), target.getUUID(), worldDay, rates);
            }
            if (intent == VampirePredationIntent.NONE) continue;
            double score = base.score() + behaviorAdjustment;
            if (score < 10.0D) continue;
            String reason = base.reason() + "; " + behaviorDetail + "; intent=" + intent;
            if (best == null || score > best.score()
                    || score == best.score() && target.getUUID().toString()
                    .compareTo(best.target().getUUID().toString()) < 0) {
                best = new Choice(target, animal, score, reason, behaviorAdjustment, knowsIdentity, intent);
            }
        }
        return best;
    }

    public void onNativeFeed(LivingEntity predator, LivingEntity target, int amount) {
        if (!FolkloreConfig.VAMPIRE_PREDATION.get() || !(predator.level() instanceof ServerLevel level)
                || !FinalizedFeedPolicy.isRealFeed(amount, predator == target)) return;
        long now = level.getGameTime();
        FeedKey key = new FeedKey(predator.getUUID(), target.getUUID());
        if (now - observedFeeds.getOrDefault(key, Long.MIN_VALUE / 2) < 20L) return;
        observedFeeds.put(key, now);
        PredationSession session = sessions.get(predator.getUUID());
        boolean keepAggressing = false;
        if (session != null && session.phase() != PredationPhase.KILLING) {
            session.transition(PredationPhase.FEEDING, "provider confirmed real blood feed");
            int feeds = session.recordConfirmedFeed(now);
            if (session.kind() == PredatorKind.WILD_VAMPIRISM && !session.animal()
                    && target.isAlive() && FolkloreConfig.VAMPIRE_BEHAVIOR_PROFILES.get()) {
                if (session.intent() == VampirePredationIntent.KILL_AFTER_FEED) {
                    session.transition(PredationPhase.KILLING,
                            "behavior profile intentionally continues combat after feeding");
                    session.extendUntil(now + 300L);
                    keepAggressing = true;
                } else if (session.intent() == VampirePredationIntent.OVERFEED) {
                    int extraFeedsCompleted = Math.max(0, feeds - 1);
                    if (extraFeedsCompleted < FolkloreConfig.VAMPIRE_RIPPER_MAX_EXTRA_FEEDS.get()) {
                        session.transition(PredationPhase.OVERFEEDING,
                                "ripper deliberately continues drinking after satiation");
                    } else {
                        session.transition(PredationPhase.KILLING,
                                "ripper exhausted its bounded extra feeds and continues as lethal combat");
                    }
                    session.extendUntil(now + 300L);
                    keepAggressing = true;
                }
            }
        } else if (session != null && session.phase() == PredationPhase.KILLING) {
            keepAggressing = true;
        }

        if (!keepAggressing) markFeedCooldowns(level, predator, target, now);

        if (!FinalizedFeedPolicy.createsNonlethalEvidence(amount, false, target.isAlive())) return;

        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        WorldPosition victimPos = WorldPosition.of(level, target.blockPosition());
        long expires = now + FolkloreConfig.EVIDENCE_LIFETIME.get();
        data.addEvidence(new EvidenceRecord(UUID.randomUUID(), EvidenceType.BITE_MARK, "darkfolklore:vampire",
                Optional.of(predator.getUUID()), victimPos, now, expires, Optional.empty()));
        data.addEvidence(new EvidenceRecord(UUID.randomUUID(), EvidenceType.BLOOD, "darkfolklore:vampire",
                Optional.of(predator.getUUID()), victimPos, now, expires, Optional.empty()));

        List<LivingEntity> witnesses = WitnessEngine.INSTANCE.recordIncident(level, predator, target,
                SecretType.VAMPIRE, EvidenceType.BITE_MARK, target instanceof Animal ? 2 : 5);
        if (isMca(target)) recordVictimKnowledge(level, predator, target, now);
        if (!witnesses.isEmpty() && FolkloreConfig.VILLAGE_SOCIETY.get()) {
            VillageSocietyState village = data.village(VillageKey.at(level, target.blockPosition()).serialized());
            village.adjustInfluence(OrganizationType.HUNTER_SOCIETY, Math.min(3, witnesses.size()));
            data.setDirty();
        }
        if (isMca(target)) createFeedingAssault(level, predator, target, now);
    }

    @SubscribeEvent
    public void onConfirmedLivingDeath(ConfirmedLivingDeathEvent event) {
        LivingEntity victim = event.entity();
        Entity source = event.source().getEntity();
        if (source == null) return;
        FeedKey key = new FeedKey(source.getUUID(), victim.getUUID());
        PendingLethalIntent pending = lethalIntents.remove(key);
        if (pending == null || !(victim.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now > pending.expiresAt() || !pending.intent().lethal()) return;
        createFeedingMurder(level, source, victim, pending, now);
        markLethalCooldown(level, source, victim, now);
    }

    private static void recordVictimKnowledge(ServerLevel level, LivingEntity predator, LivingEntity victim, long now) {
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        SocialKnowledgeKey key = new SocialKnowledgeKey(victim.getUUID(), predator.getUUID(), SecretType.VAMPIRE);
        SocialKnowledgeRecord record = data.mergeSocial(key, new SocialKnowledgeRecord(
                SocialKnowledgeState.CONFIRMED, 0.95F, KnowledgeSource.DIRECT_WITNESS, now, EvidenceType.BITE_MARK));
        if (FolkloreConfig.RUMORS.get()) RumorEngine.INSTANCE.enqueue(victim, predator.getUUID(), SecretType.VAMPIRE, record, 0);
    }

    private static void createFeedingAssault(ServerLevel level, LivingEntity predator, LivingEntity victim, long now) {
        if (!FolkloreConfig.DYNAMIC_STORIES.get()) return;
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        String village = VillageKey.at(level, victim.blockPosition()).serialized();
        boolean recent = data.stories().stream().anyMatch(existing -> !existing.story().status().terminal()
                && existing.story().template().equals("feeding_assault")
                && existing.story().actors().contains(predator.getUUID())
                && existing.story().actors().contains(victim.getUUID())
                && now - existing.story().createdAt() < FolkloreConfig.STORY_COOLDOWN.get());
        if (recent) return;
        StoryInstance story = new StoryInstance(UUID.randomUUID(), "feeding_assault", "darkfolklore:vampire", now,
                now + FolkloreConfig.CONTRACT_LIFETIME.get() * 2L);
        story.addActor(predator.getUUID());
        story.addActor(victim.getUUID());
        PersistentStory persistent = new PersistentStory(story, WorldPosition.of(level, victim.blockPosition()), village);
        data.putStory(persistent);
        String implementation = BuiltInRegistries.ENTITY_TYPE.getKey(predator.getType()).toString();
        InvestigationSavedData.get(level.getServer()).putIncidentFact(story.id(),
                new IncidentFact(Optional.of(predator.getUUID()), implementation, now));
    }

    private void createFeedingMurder(ServerLevel level, Entity predator, LivingEntity victim,
                                     PendingLethalIntent pending, long now) {
        if (!FolkloreConfig.DYNAMIC_STORIES.get()) return;
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        String villageKey = VillageKey.at(level, victim.blockPosition()).serialized();
        boolean recent = data.stories().stream().anyMatch(existing -> !existing.story().status().terminal()
                && existing.story().template().equals("feeding_murder")
                && existing.story().actors().contains(predator.getUUID())
                && existing.story().actors().contains(victim.getUUID())
                && now - existing.story().createdAt() < FolkloreConfig.STORY_COOLDOWN.get());
        if (!recent) {
            StoryInstance story = new StoryInstance(UUID.randomUUID(), "feeding_murder", "darkfolklore:vampire", now,
                    now + FolkloreConfig.CONTRACT_LIFETIME.get() * 2L);
            story.addActor(predator.getUUID());
            story.addActor(victim.getUUID());
            data.putStory(new PersistentStory(story, WorldPosition.of(level, victim.blockPosition()), villageKey));
            String implementation = BuiltInRegistries.ENTITY_TYPE.getKey(predator.getType()).toString();
            InvestigationSavedData.get(level.getServer()).putIncidentFact(story.id(),
                    new IncidentFact(Optional.of(predator.getUUID()), implementation, now));
        }

        if (FolkloreConfig.VILLAGE_SOCIETY.get()) {
            VillageSocietyState village = data.village(villageKey);
            int witnesses = predator instanceof LivingEntity living
                    ? visibleWitnesses(level, living, victim) : 0;
            village.recordIncident(witnesses, true, 8);
            village.adjustInfluence(OrganizationType.HUNTER_SOCIETY, Math.max(2, Math.min(6, witnesses + 2)));
            data.setDirty();
        }
        Diagnostic previous = diagnostics.get(predator.getUUID());
        if (previous != null) {
            diagnostics.put(predator.getUUID(), new Diagnostic(previous.predator(), previous.kind(),
                    pending.profile(), pending.intent(), previous.profileDetail(), Optional.of(victim.getUUID()),
                    previous.localRisk(), previous.personalRisk(),
                    "confirmed lethal predation: " + pending.profile() + "/" + pending.intent(), now));
        }
    }

    private void markFeedCooldowns(ServerLevel level, LivingEntity predator, LivingEntity target, long now) {
        long cooldown = FolkloreConfig.VAMPIRE_PREDATION_COOLDOWN.get();
        predatorCooldowns.put(predator.getUUID(), now + cooldown);
        victimCooldowns.put(target.getUUID(), now + Math.max(100L, cooldown / 2L));
        recordRegionalIncident(level, target, now);
        PredationSession completed = sessions.remove(predator.getUUID());
        if (completed != null && completed.directedTarget() && predator instanceof Mob mob) {
            CompatibilityManager.INSTANCE.vampirePredation().clearWildHuntTarget(mob, completed.target());
        }
    }

    private void markLethalCooldown(ServerLevel level, Entity predator, LivingEntity victim, long now) {
        long cooldown = FolkloreConfig.VAMPIRE_PREDATION_COOLDOWN.get();
        predatorCooldowns.merge(predator.getUUID(), now + cooldown, Math::max);
        recordRegionalIncident(level, victim, now);
        PredationSession completed = sessions.remove(predator.getUUID());
        if (completed != null && completed.directedTarget() && predator instanceof Mob mob) {
            CompatibilityManager.INSTANCE.vampirePredation().clearWildHuntTarget(mob, completed.target());
        }
    }

    private void recordRegionalIncident(ServerLevel level, LivingEntity target, long now) {
        String region = VillageKey.at(level, target.blockPosition()).serialized();
        ArrayDeque<Long> history = regionalFeeds.computeIfAbsent(region, ignored -> new ArrayDeque<>());
        pruneRegion(history, now);
        history.addLast(now);
    }

    private void rememberLethalIntent(LivingEntity predator, LivingEntity target, PredationSession session, long now) {
        lethalIntents.put(new FeedKey(predator.getUUID(), target.getUUID()),
                new PendingLethalIntent(predator.getUUID(), target.getUUID(), session.behaviorProfile(),
                        session.intent(), now, now + 800L));
    }

    private void endSession(Mob predator, PredationSession session, VampirePredationBridge bridge) {
        sessions.remove(predator.getUUID(), session);
        if (session.directedTarget()) bridge.clearWildHuntTarget(predator, session.target());
    }

    private boolean regionalBudgetAvailable(String region, long now) {
        ArrayDeque<Long> history = regionalFeeds.computeIfAbsent(region, ignored -> new ArrayDeque<>());
        pruneRegion(history, now);
        return history.size() < FolkloreConfig.VAMPIRE_PREDATION_MAX_LOCAL_FEEDS.get();
    }

    private static void pruneRegion(ArrayDeque<Long> history, long now) {
        long window = FolkloreConfig.VAMPIRE_PREDATION_LOCAL_WINDOW.get();
        while (!history.isEmpty() && now - history.peekFirst() > window) history.removeFirst();
    }

    private static double localRisk(ServerLevel level, LivingEntity predator) {
        VillageSocietyState village = FolkloreSavedData.get(level.getServer())
                .village(VillageKey.at(level, predator.blockPosition()).serialized());
        return village.publicAwareness() * 0.35D + village.suspicion() * 0.35D + village.hunterInfluence() * 0.30D;
    }

    private static double personalRisk(ServerLevel level, LivingEntity predator) {
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        if (data.isPublic(new SecretClaimKey(predator.getUUID(), SecretType.VAMPIRE))) return 100.0D;
        double risk = 0.0D;
        for (Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord> entry : data.knowledgeAbout(predator.getUUID())) {
            if (entry.getKey().secret() != SecretType.VAMPIRE) continue;
            SocialKnowledgeRecord record = entry.getValue();
            double candidate = record.state().strength() / 4.0D * 70.0D + record.confidence() * 30.0D;
            risk = Math.max(risk, candidate);
        }
        return Math.min(100.0D, risk);
    }

    private static boolean victimKnowsIdentity(ServerLevel level, LivingEntity victim, LivingEntity predator) {
        SocialKnowledgeKey key = new SocialKnowledgeKey(victim.getUUID(), predator.getUUID(), SecretType.VAMPIRE);
        return FolkloreSavedData.get(level.getServer()).social(key)
                .map(record -> record.state().strength() >= SocialKnowledgeState.CONFIRMED.strength())
                .orElse(false);
    }

    private static int visibleWitnesses(ServerLevel level, LivingEntity predator, LivingEntity target) {
        int radius = Math.min(FolkloreConfig.WITNESS_RADIUS.get(), 16);
        int count = 0;
        for (LivingEntity observer : level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(radius), observer -> observer.isAlive()
                        && observer != predator && observer != target && !observer.isSleeping()
                        && SocialEntityClassifier.isSocial(observer))) {
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

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 1200 != 0) return;
        long now = event.getServer().overworld().getGameTime();
        predatorCooldowns.entrySet().removeIf(entry -> now > entry.getValue() + 2400L);
        victimCooldowns.entrySet().removeIf(entry -> now > entry.getValue() + 2400L);
        observedFeeds.entrySet().removeIf(entry -> now - entry.getValue() > 2400L);
        lethalIntents.entrySet().removeIf(entry -> now > entry.getValue().expiresAt());
        pruneExpiredSessions(event.getServer(), now);
        regionalFeeds.values().forEach(history -> pruneRegion(history, now));
        regionalFeeds.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        while (diagnostics.size() > 128) diagnostics.remove(diagnostics.keySet().iterator().next());
    }

    private void pruneExpiredSessions(MinecraftServer server, long now) {
        VampirePredationBridge bridge = CompatibilityManager.INSTANCE.vampirePredation();
        Iterator<Map.Entry<UUID, PredationSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PredationSession> entry = iterator.next();
            PredationSession session = entry.getValue();
            long retention = session.directedTarget() ? 2400L : 0L;
            if (now <= session.expiresAt() + retention) continue;
            session.transition(PredationPhase.ABORTED, "maintenance pruned expired session");
            if (session.directedTarget()) {
                for (ServerLevel level : server.getAllLevels()) {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity instanceof Mob mob) {
                        bridge.clearWildHuntTarget(mob, session.target());
                        break;
                    }
                }
            }
            iterator.remove();
        }
    }

    public Optional<Diagnostic> diagnostic(UUID predator) { return Optional.ofNullable(diagnostics.get(predator)); }
    public Optional<PredationPhase> sessionPhase(UUID predator) {
        return Optional.ofNullable(sessions.get(predator)).map(PredationSession::phase);
    }
    public Optional<String> sessionDetail(UUID predator) {
        return Optional.ofNullable(sessions.get(predator)).map(PredationSession::detail);
    }
    public Optional<VampireBehaviorProfile> sessionBehavior(UUID predator) {
        return Optional.ofNullable(sessions.get(predator)).map(PredationSession::behaviorProfile);
    }
    public Optional<VampirePredationIntent> sessionIntent(UUID predator) {
        return Optional.ofNullable(sessions.get(predator)).map(PredationSession::intent);
    }
    public int activeSessions() { return sessions.size(); }
    public int trackedRegions() { return regionalFeeds.size(); }
    public int pendingLethalIntents() { return lethalIntents.size(); }

    /**
     * Cancels only Dark Folklore orchestration. MCA provider targets/navigation are never touched; a target hint
     * installed by Core on a wild Vampirism mob is cleared only when it still points at this session's victim.
     */
    public void cancelSession(Entity predator) {
        PredationSession session = sessions.remove(predator.getUUID());
        if (session != null) session.transition(PredationPhase.ABORTED, "session cancelled by lifecycle/cleanup");
        if (session != null && session.directedTarget() && predator instanceof Mob mob) {
            CompatibilityManager.INSTANCE.vampirePredation().clearWildHuntTarget(mob, session.target());
        }
    }

    public void clearRuntimeState() {
        sessions.clear();
        predatorCooldowns.clear();
        victimCooldowns.clear();
        regionalFeeds.clear();
        observedFeeds.clear();
        lethalIntents.clear();
        diagnostics.clear();
    }

    private void remember(LivingEntity predator, PredatorKind kind,
                          VampireBehaviorResolver.Resolution behavior, VampirePredationIntent intent,
                          LivingEntity target, double localRisk, double personalRisk, String reason, long now) {
        diagnostics.remove(predator.getUUID());
        diagnostics.put(predator.getUUID(), new Diagnostic(predator.getUUID(), kind, behavior.profile(), intent,
                behavior.detail(), Optional.ofNullable(target).map(Entity::getUUID),
                localRisk, personalRisk, reason, now));
        while (diagnostics.size() > 128) diagnostics.remove(diagnostics.keySet().iterator().next());
    }

    public record Diagnostic(UUID predator, PredatorKind kind, VampireBehaviorProfile behaviorProfile,
                             VampirePredationIntent intent, String profileDetail, Optional<UUID> target,
                             double localRisk, double personalRisk, String reason, long gameTime) {
        public Diagnostic {
            behaviorProfile = behaviorProfile == null ? VampireBehaviorProfile.CONTROLLED : behaviorProfile;
            intent = intent == null ? VampirePredationIntent.NONE : intent;
            profileDetail = profileDetail == null ? "" : profileDetail;
            target = target == null ? Optional.empty() : target;
            reason = reason == null ? "" : reason;
        }
    }

    private record Choice(LivingEntity target, boolean animal, double score, String reason,
                          double behaviorAdjustment, boolean victimKnowsIdentity,
                          VampirePredationIntent intent) {}
    private record FeedKey(UUID predator, UUID victim) {}
    private record PendingLethalIntent(UUID predator, UUID victim, VampireBehaviorProfile profile,
                                       VampirePredationIntent intent, long createdAt, long expiresAt) {}
}
