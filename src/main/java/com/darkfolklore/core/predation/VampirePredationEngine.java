package com.darkfolklore.core.predation;

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
 * Bounded social director for vampire feeding. It chooses risk-aware prey but delegates factual vampire state,
 * infection and conversion to the exact provider bridges.
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

        PredationSession current = sessions.get(predator.getUUID());
        if (current != null) {
            if (continueSession(level, predator, current, bridge, now, environmentAllowed)) return;
            endSession(predator, current, bridge);
        }

        if (!environmentAllowed) {
            remember(predator, kind, null, 0, 0, "daylight exposure blocks autonomous predation", now);
            return;
        }
        if (!bridge.wantsBlood(predator)) {
            remember(predator, kind, null, 0, 0, "provider reports no feeding pressure", now);
            return;
        }
        if (now < predatorCooldowns.getOrDefault(predator.getUUID(), 0L)) {
            remember(predator, kind, null, 0, 0, "predator cooldown", now);
            return;
        }
        String region = VillageKey.at(level, predator.blockPosition()).serialized();
        if (!regionalBudgetAvailable(region, now)) {
            remember(predator, kind, null, 0, 0, "local anti-chaos feeding budget exhausted", now);
            return;
        }

        double localRisk = localRisk(level, predator);
        double personalRisk = personalRisk(level, predator);
        Choice choice = choose(level, predator, kind, bridge, localRisk, personalRisk, now, environmentAllowed);
        if (choice == null) {
            remember(predator, kind, null, localRisk, personalRisk, "no socially/provider-valid prey", now);
            return;
        }

        boolean directedTarget = false;
        if (kind == PredatorKind.WILD_VAMPIRISM && !choice.animal()) {
            LivingEntity existing = predator.getTarget();
            boolean alreadyOwnsChosenTarget = existing != null && existing.isAlive()
                    && existing.getUUID().equals(choice.target().getUUID());
            if (!bridge.requestWildHuntTarget(predator, choice.target())) {
                remember(predator, kind, choice.target(), localRisk, personalRisk,
                        "selected mca_civilian but another live combat target/provider guard refused hunt steering", now);
                return;
            }
            directedTarget = !alreadyOwnsChosenTarget;
        }

        PredationSession session = new PredationSession(choice.target().getUUID(), choice.animal(), kind,
                directedTarget, now, now + 240L);
        sessions.put(predator.getUUID(), session);
        remember(predator, kind, choice.target(), localRisk, personalRisk,
                (directedTarget ? "directed " : "selected ") + choice.reason()
                        + " score=" + Math.round(choice.score()), now);
    }

    private boolean continueSession(ServerLevel level, Mob predator, PredationSession session,
                                    VampirePredationBridge bridge, long now, boolean environmentAllowed) {
        if (!environmentAllowed) return abort(session, "predator became exposed to daylight");
        if (now > session.expiresAt() || !predator.isAlive()) return abort(session, "session expired or predator died");
        Entity loaded = level.getEntity(session.target());
        if (!(loaded instanceof LivingEntity target) || !target.isAlive()) return abort(session, "target is no longer alive/loaded");
        if (now < victimCooldowns.getOrDefault(target.getUUID(), 0L)) return abort(session, "victim cooldown");

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
        if (!canFeed) return abort(session, "provider rejected feeding target");

        if (session.kind() == PredatorKind.WILD_VAMPIRISM && session.directedTarget()
                && !bridge.requestWildHuntTarget(predator, target)) {
            return abort(session, "wild target steering was revoked");
        }

        boolean lineOfSight = predator.getSensing().hasLineOfSight(target);
        if (!lineOfSight) {
            if (session.kind() == PredatorKind.WILD_VAMPIRISM && session.directedTarget()) {
                session.transition(PredationPhase.STALKING, "pathing toward hidden target");
                return true;
            }
            return abort(session, "opportunistic prey left line of sight");
        }
        if (predator.distanceToSqr(target) > 3.5D) {
            session.transition(PredationPhase.PURSUING, "closing distance");
            return true;
        }

        session.transition(PredationPhase.ATTACKING, "in feeding range");
        boolean fed = session.kind() == PredatorKind.WILD_VAMPIRISM
                ? bridge.performWildFeed(predator, target)
                : bridge.performMcaAnimalFeed(predator, target);
        if (!fed && sessions.containsKey(predator.getUUID())) {
            session.transition(PredationPhase.ABORTED, "provider feed action returned false");
        }
        return false;
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
                          double localRisk, double personalRisk, long now, boolean environmentAllowed) {
        int radius = FolkloreConfig.VAMPIRE_PREDATION_RADIUS.get();
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                predator.getBoundingBox().inflate(radius, Math.max(4, radius / 2.0D), radius),
                target -> target != predator && target.isAlive());
        Choice best = null;
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
            PredationPolicy.Candidate candidate = new PredationPolicy.Candidate(animal, mca, true, child,
                    closeFamily, vampire || werewolf, hunter, target.hasCustomName() && !mca,
                    providerEligible, witnesses, distance, witnesses == 0);
            PredationPolicy.Decision decision = PredationPolicy.score(
                    new PredationPolicy.Context(kind, environmentAllowed, localRisk, personalRisk), candidate);
            if (!decision.eligible()) continue;
            if (best == null || decision.score() > best.score()
                    || decision.score() == best.score() && target.getUUID().toString()
                    .compareTo(best.target().getUUID().toString()) < 0) {
                best = new Choice(target, animal, decision.score(), decision.reason());
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
        if (session != null) session.transition(PredationPhase.FEEDING, "provider confirmed real blood feed");
        markFeedCooldowns(level, predator, target, now);

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

    private void markFeedCooldowns(ServerLevel level, LivingEntity predator, LivingEntity target, long now) {
        long cooldown = FolkloreConfig.VAMPIRE_PREDATION_COOLDOWN.get();
        predatorCooldowns.put(predator.getUUID(), now + cooldown);
        victimCooldowns.put(target.getUUID(), now + Math.max(100L, cooldown / 2L));
        String region = VillageKey.at(level, target.blockPosition()).serialized();
        ArrayDeque<Long> history = regionalFeeds.computeIfAbsent(region, ignored -> new ArrayDeque<>());
        pruneRegion(history, now);
        history.addLast(now);
        PredationSession completed = sessions.remove(predator.getUUID());
        if (completed != null && completed.directedTarget() && predator instanceof Mob mob) {
            CompatibilityManager.INSTANCE.vampirePredation().clearWildHuntTarget(mob, completed.target());
        }
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
    public int activeSessions() { return sessions.size(); }
    public int trackedRegions() { return regionalFeeds.size(); }

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
        diagnostics.clear();
    }

    private void remember(LivingEntity predator, PredatorKind kind, LivingEntity target,
                          double localRisk, double personalRisk, String reason, long now) {
        diagnostics.remove(predator.getUUID());
        diagnostics.put(predator.getUUID(), new Diagnostic(predator.getUUID(), kind,
                Optional.ofNullable(target).map(Entity::getUUID), localRisk, personalRisk, reason, now));
        while (diagnostics.size() > 128) diagnostics.remove(diagnostics.keySet().iterator().next());
    }

    public record Diagnostic(UUID predator, PredatorKind kind, Optional<UUID> target,
                             double localRisk, double personalRisk, String reason, long gameTime) {}
    private record Choice(LivingEntity target, boolean animal, double score, String reason) {}
    private record FeedKey(UUID predator, UUID victim) {}
}
