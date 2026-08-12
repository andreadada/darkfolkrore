package com.darkfolklore.core.contracts;

import com.darkfolklore.core.api.event.*;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.investigation.*;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.knowledge.observation.CreatureSightingRecord;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.*;
import com.darkfolklore.core.reputation.ReputationFaction;
import com.darkfolklore.core.society.story.*;
import com.darkfolklore.core.society.organization.*;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.society.village.VillageSocietyState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

public final class ContractEngine {
    public static final ContractEngine INSTANCE = new ContractEngine();
    private final Map<UUID, Long> feedbackCooldowns = new HashMap<>();
    private ContractEngine() {}

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!FolkloreConfig.CONTRACTS.get() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty() || !isIssuer(event.getTarget())) return;
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        InvestigationSavedData investigation = InvestigationSavedData.get(player.getServer());
        Optional<ContractAssignment> existing = data.activeContract(player.getUUID());
        if (existing.isPresent()) {
            ContractAssignment assignment = existing.get();
            if (assignment.contract().status() == ContractStatus.HUNTED
                    && canTurnIn(player.serverLevel(), event.getTarget(), data, investigation, assignment)) {
                complete(player, data, investigation, assignment);
                consume(event);
            } else if (assignment.contract().status() == ContractStatus.INVESTIGATING
                    && tryCollectTestimony(player, event.getTarget(), data, investigation, assignment)) {
                consume(event);
            } else if (assignment.contract().issuer().equals(event.getTarget().getUUID())) {
                player.displayClientMessage(Component.literal(statusMessage(investigation, assignment)), false);
                consume(event);
            }
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;
        String village = VillageKey.at(level, event.getTarget().blockPosition()).serialized();
        PersistentStory available = data.stories().stream()
                .filter(value -> value.villageKey().equals(village) && value.story().status() == StoryStatus.INCIDENT)
                .filter(value -> SocietyStoryEngine.INSTANCE.isContractEligible(value.story().template()))
                .min(Comparator.comparingLong(value -> value.story().createdAt())).orElse(null);
        if (available == null) {
            player.displayClientMessage(Component.literal("No local supernatural incident is ready for investigation."), true);
            return;
        }
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), event.getTarget().getUUID(),
                available.story().concept(), level.getGameTime() + FolkloreConfig.CONTRACT_LIFETIME.get());
        contract.start();
        int requiredEvidence = FolkloreDataManager.INSTANCE.investigationProfile(available.story().concept())
                .map(InvestigationProfile::requiredEvidence).orElse(2);
        ContractAssignment assignment = new ContractAssignment(player.getUUID(), contract,
                available.location(), village, requiredEvidence);
        IncidentFact fact = investigation.incidentFact(available.story().id())
                .orElseGet(() -> inferIncidentFact(level, data, available));
        Set<UUID> liveStories = new HashSet<>();
        for (PersistentStory story : data.stories()) liveStories.add(story.story().id());
        investigation.pruneOrphans(data.contracts(), liveStories);
        if (!investigation.putCaseLink(contract.id(), InvestigationCaseLink.fromStory(available.story().id(), fact))) {
            player.displayClientMessage(Component.literal(
                    "Investigation continuity storage is full; contract acceptance failed closed."), true);
            return;
        }
        if (fact != null) investigation.putIncidentFact(available.story().id(), fact);
        data.putContract(assignment);

        available.story().advance(StoryStatus.INVESTIGATING);
        data.putStory(available);
        player.displayClientMessage(Component.literal("Contract accepted: investigate the incident near "
                + available.location().x() + ", " + available.location().y() + ", " + available.location().z()
                + ". Sneak-right-click nearby clue locations. Physical clues, credible testimony, and occult analysis "
                + "can all advance the investigation."), false);
        NeoForge.EVENT_BUS.post(new ContractStartedEvent(assignment));
        consume(event);
    }

    @SubscribeEvent
    public void onInvestigate(PlayerInteractEvent.RightClickBlock event) {
        if (!FolkloreConfig.CONTRACTS.get() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty() || !(player.level() instanceof ServerLevel level)) return;
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        InvestigationSavedData investigation = InvestigationSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null || assignment.contract().status() != ContractStatus.INVESTIGATING) return;
        String dimension = level.dimension().location().toString();
        long now = level.getGameTime();
        InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
        PersistentStory caseStory = InvestigationTargeting.exactLinkedStory(data, link);
        EvidenceRecord clue = data.evidence().stream()
                .filter(value -> !value.expired(now)
                        && value.collectedBy().isEmpty()
                        && InvestigationTargeting.matchesEvidence(assignment.contract().targetConcept(), value,
                        link, caseStory)
                        && !assignment.contract().evidence().contains(value.type())
                        && value.position().dimension().equals(dimension)
                        && value.position().distanceSquared(event.getPos()) <= 16.0D)
                .min(Comparator.comparingDouble(value -> value.position().distanceSquared(event.getPos())))
                .orElse(null);
        if (clue == null) {
            if (assignment.investigationCenter().dimension().equals(dimension)
                    && assignment.investigationCenter().distanceSquared(event.getPos()) <= 1024.0D) {
                if (now - feedbackCooldowns.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) >= 40) {
                    feedbackCooldowns.put(player.getUUID(), now);
                    player.displayClientMessage(Component.literal("No usable clue here; search close to visible incident traces."), true);
                    level.sendParticles(ParticleTypes.SMOKE, event.getPos().getX() + 0.5D,
                            event.getPos().getY() + 1.0D, event.getPos().getZ() + 0.5D,
                            3, 0.15D, 0.1D, 0.15D, 0.01D);
                }
            }
            return;
        }
        if (!data.collectEvidence(clue.id(), player.getUUID())) return;
        ContractStatus before = assignment.contract().status();
        if (!assignment.contract().addEvidence(clue.type(), assignment.requiredDistinctClues())) return;
        data.putContract(assignment);
        LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 2);
        if (before != ContractStatus.IDENTIFIED
                && assignment.contract().status() == ContractStatus.IDENTIFIED) {
            LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 8);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, clue.position().x() + 0.5D,
                clue.position().y() + 0.8D, clue.position().z() + 0.5D,
                10, 0.25D, 0.2D, 0.25D, 0.02D);
        player.displayClientMessage(Component.literal("Evidence collected: " + clue.type()
                + " (" + assignment.contract().evidence().size() + "/" + assignment.requiredDistinctClues() + ")"
                + (assignment.contract().status() == ContractStatus.IDENTIFIED
                ? ". Target identified: " + assignment.contract().targetConcept()
                : ". " + OccultInvestigationEngine.INSTANCE.hypothesisSummary(assignment))), false);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public void onConfirmedDeath(ConfirmedLivingDeathEvent event) {
        if (!FolkloreConfig.CONTRACTS.get()) return;
        UUID killerId = event.source().getEntity() instanceof ServerPlayer killer ? killer.getUUID() : null;
        finalizeConfirmedDeath(event.server(), event.entity(), Optional.ofNullable(killerId));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!FolkloreConfig.CONTRACTS.get()) {
            return;
        }

        if (event.getServer().getTickCount() % 200 != 0) return;

        FolkloreSavedData data = FolkloreSavedData.get(event.getServer());
        InvestigationSavedData investigation = InvestigationSavedData.get(event.getServer());
        long now = event.getServer().overworld().getGameTime();
        for (ContractAssignment assignment : data.contracts()) {
            if (assignment.contract().expire(now)) data.putContract(assignment);
        }
        for (PersistentStory story : data.stories()) {
            if (story.story().expire(now)) data.putStory(story);
        }
        investigation.pruneSightings(now, 0.08F, Math.max(2400L, (long) FolkloreConfig.RUMOR_HALF_LIFE.get() * 4L));
        Set<UUID> storyIds = new HashSet<>();
        for (PersistentStory story : data.stories()) storyIds.add(story.story().id());
        investigation.pruneOrphans(data.contracts(), storyIds);
        feedbackCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > 1200L);
    }

    private static void finalizeConfirmedDeath(MinecraftServer server, LivingEntity deadEntity,
                                               Optional<UUID> killerId) {
        UUID dead = deadEntity.getUUID();
        FolkloreSavedData data = FolkloreSavedData.get(server);
        InvestigationSavedData investigation = InvestigationSavedData.get(server);
        UUID huntedContractId = null;

        if (killerId.isPresent()) {
            UUID killer = killerId.get();
            ContractAssignment assignment = data.activeContract(killer).orElse(null);
            if (assignment != null && assignment.contract().status() == ContractStatus.IDENTIFIED) {
                InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
                if (InvestigationTargeting.matches(assignment, deadEntity, link)
                        && assignment.contract().markHunted()) {
                    data.putContract(assignment);
                    huntedContractId = assignment.contract().id();
                    PersistentStory story = linkedStory(data, investigation, assignment);
                    if (story != null && story.story().status() == StoryStatus.INVESTIGATING) {
                        story.story().advance(StoryStatus.CONFRONTATION);
                        data.putStory(story);
                    }
                    ServerPlayer player = server.getPlayerList().getPlayer(killer);
                    if (player != null) {
                        player.displayClientMessage(Component.literal(link != null && link.issuerFallbackAllowed()
                                ? "Target defeated. The original issuer is unavailable; return to an authorized local representative."
                                : "Target defeated. Return to the contract issuer."), false);
                    }
                }
            }
        }

        // Fallback authorization is also death-dependent, so it happens only here after the
        // uncancelled/non-alive confirmation. A normal unload never enters this path.
        for (ContractAssignment value : data.contracts()) {
            InvestigationCaseLink link = investigation.caseLink(value.contract().id()).orElse(null);
            if (ContractDeathPolicy.allowIssuerFallback(value.contract(), dead)) {
                investigation.allowIssuerFallback(value.contract().id());
            }
            if (ContractDeathPolicy.allowCulpritFallback(value.contract(), link, dead, huntedContractId)) {
                investigation.allowCulpritFallback(value.contract().id());
            }
        }
    }

    private static void complete(ServerPlayer player, FolkloreSavedData data, InvestigationSavedData investigation,
                                 ContractAssignment assignment) {
        if (!assignment.contract().complete()) return;
        data.putContract(assignment);
        ItemStack reward = new ItemStack(Items.EMERALD, 8);
        if (!player.getInventory().add(reward)) player.spawnAtLocation(reward);
        player.giveExperiencePoints(150);
        data.addReputation(player.getUUID(), ReputationFaction.VILLAGERS, 10);
        data.addReputation(player.getUUID(), ReputationFaction.HUNTERS, 8);
        LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 20);
        VillageSocietyState village = data.village(assignment.villageKey());
        village.adjustPublicAwareness(4);
        village.adjustSuspicion(-3);
        village.adjustInfluence(OrganizationType.HUNTER_SOCIETY, 3);
        data.setDirty();
        PersistentStory story = linkedStory(data, investigation, assignment);
        if (story != null && !story.story().status().terminal()) {
            if (story.story().status() == StoryStatus.INVESTIGATING
                    || story.story().status() == StoryStatus.CONFRONTATION) {
                story.story().advance(StoryStatus.RESOLVED);
                data.putStory(story);
            }
        }
        player.displayClientMessage(Component.literal("Contract complete: 8 emeralds, 150 XP, reputation and lore awarded."), false);
        NeoForge.EVENT_BUS.post(new ContractCompletedEvent(assignment));
    }

    private static String statusMessage(InvestigationSavedData investigation, ContractAssignment assignment) {
        InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
        return switch (assignment.contract().status()) {
            case INVESTIGATING -> "Active contract: collect distinct evidence near the investigation area.";
            case IDENTIFIED -> link != null && link.culpritId().isPresent() && !link.culpritFallbackAllowed()
                    ? "Active contract: track and hunt the identified incident culprit."
                    : "Active contract: hunt " + assignment.contract().targetConcept() + ".";
            case HUNTED -> link != null && link.issuerFallbackAllowed()
                    ? "Active contract: the issuer is unavailable; return to an authorized local representative."
                    : "Active contract: return to issuer " + assignment.contract().issuer() + ".";
            default -> "Contract status: " + assignment.contract().status();
        };
    }

    private static boolean tryCollectTestimony(ServerPlayer player, Entity witness, FolkloreSavedData data,
                                               InvestigationSavedData investigation, ContractAssignment assignment) {
        if (!(witness instanceof LivingEntity) || witness.getUUID().equals(player.getUUID())) return false;

        CreatureSightingRecord sighting = investigation.sighting(witness.getUUID(), assignment.contract().targetConcept())
                .filter(value -> value.confidence() >= 0.35F
                        && value.state().strength() >= SocialKnowledgeState.SUSPECTED.strength()
                        && relevantToCase(value, data, investigation, assignment))
                .orElse(null);
        if (sighting != null) {
            return recordTestimony(player, data, assignment, sighting.confidence(), "creature sighting");
        }

        InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
        Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord> testimony = data.knowledgeHeldBy(witness.getUUID())
                .stream().filter(entry -> entry.getValue().confidence() >= 0.35F
                        && entry.getValue().state().strength() >= SocialKnowledgeState.SUSPECTED.strength()
                        && InvestigationTargeting.matchesTestimonySubject(entry.getKey().subject(), link)
                        && concept(entry.getKey().secret()).equals(assignment.contract().targetConcept()))
                .max(Comparator.comparingDouble(entry -> entry.getValue().confidence())).orElse(null);
        if (testimony == null) return false;
        return recordTestimony(player, data, assignment, testimony.getValue().confidence(), "identity testimony");
    }

    private static boolean recordTestimony(ServerPlayer player, FolkloreSavedData data, ContractAssignment assignment,
                                           float confidence, String source) {
        ContractStatus before = assignment.contract().status();
        if (!assignment.contract().addEvidence(EvidenceType.TESTIMONY, assignment.requiredDistinctClues())) return false;
        data.putContract(assignment);
        LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 3);
        if (before != ContractStatus.IDENTIFIED
                && assignment.contract().status() == ContractStatus.IDENTIFIED) {
            LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 8);
        }
        player.displayClientMessage(Component.literal("Credible " + source + " recorded (confidence "
                + Math.round(confidence * 100.0F) + "%)."
                + (assignment.contract().status() == ContractStatus.IDENTIFIED
                ? " The evidence now identifies " + assignment.contract().targetConcept() + "."
                : " " + OccultInvestigationEngine.INSTANCE.hypothesisSummary(assignment))), false);
        return true;
    }

    private static boolean relevantToCase(CreatureSightingRecord record, FolkloreSavedData data,
                                          InvestigationSavedData investigation, ContractAssignment assignment) {
        InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
        if (link != null && link.culpritId().isPresent() && !link.culpritFallbackAllowed()) {
            if (record.entityId().isEmpty() || !record.entityId().get().equals(link.culpritId().get())) return false;
        }
        if (link == null || link.storyId().isEmpty()) return true;
        PersistentStory story = data.story(link.storyId().get()).orElse(null);
        return story == null || record.gameTime() >= Math.max(0L, story.story().createdAt() - 200L);
    }

    private static PersistentStory linkedStory(FolkloreSavedData data, InvestigationSavedData investigation,
                                               ContractAssignment assignment) {
        InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
        if (!InvestigationTargeting.mayUseLegacyStoryFallback(link)) {
            return data.story(link.storyId().orElseThrow()).orElse(null);
        }
        // Backward-compatible fallback for pre-0.3.1 contracts that have no sidecar link.
        return data.stories().stream().filter(value -> value.villageKey().equals(assignment.villageKey())
                && value.story().concept().equals(assignment.contract().targetConcept())
                && !value.story().status().terminal()).min(Comparator.comparingLong(value -> value.story().createdAt()))
                .orElse(null);
    }

    private static IncidentFact inferIncidentFact(ServerLevel level, FolkloreSavedData data, PersistentStory story) {
        EvidenceRecord evidence = data.evidence().stream()
                .filter(value -> IncidentContinuity.matches(story, value))
                .min(Comparator.comparingLong(EvidenceRecord::createdAt)).orElse(null);
        if (evidence == null || evidence.subject().isEmpty()) return null;
        UUID culprit = evidence.subject().get();
        Entity loaded = level.getEntity(culprit);
        String implementation = loaded == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(loaded.getType()).toString();
        return new IncidentFact(Optional.of(culprit), implementation, story.story().createdAt());
    }

    private static boolean canTurnIn(ServerLevel level, Entity target, FolkloreSavedData data,
                                     InvestigationSavedData investigation, ContractAssignment assignment) {
        if (assignment.contract().issuer().equals(target.getUUID())) return true;
        InvestigationCaseLink link = investigation.caseLink(assignment.contract().id()).orElse(null);
        if (link == null || !link.issuerFallbackAllowed()) return false;
        if (!VillageKey.at(level, target.blockPosition()).serialized().equals(assignment.villageKey())) return false;

        boolean localHunterSociety = data.organizations().stream().anyMatch(org ->
                org.type() == OrganizationType.HUNTER_SOCIETY && org.home().equals(assignment.villageKey()));
        boolean authorizedHunter = data.organizationsForMember(target.getUUID()).stream()
                .map(data::organization).flatMap(Optional::stream)
                .anyMatch(org -> org.type() == OrganizationType.HUNTER_SOCIETY
                        && org.home().equals(assignment.villageKey()));
        return localHunterSociety ? authorizedHunter : isIssuer(target);
    }

    private static String concept(SecretType secret) {
        return switch (secret) {
            case VAMPIRE -> "darkfolklore:vampire";
            case WEREWOLF -> "darkfolklore:werewolf";
            case HUNTER -> "darkfolklore:hunter";
            case WITCH, OCCULTIST -> "darkfolklore:witch";
            case FAE_TOUCHED -> "darkfolklore:fae";
            case CURSED -> "darkfolklore:curse";
            case SUPERNATURAL_IDENTITY -> "darkfolklore:supernatural";
        };
    }

    private static boolean isIssuer(Entity entity) {
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace();
        return entity instanceof AbstractVillager || namespace.equals("mca");
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

}
