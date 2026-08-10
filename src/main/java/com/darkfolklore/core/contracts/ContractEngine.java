package com.darkfolklore.core.contracts;

import com.darkfolklore.core.api.event.*;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.investigation.EvidenceRecord;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.persistence.*;
import com.darkfolklore.core.reputation.ReputationFaction;
import com.darkfolklore.core.society.SecretFacts;
import com.darkfolklore.core.society.story.*;
import com.darkfolklore.core.society.organization.OrganizationType;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.society.village.VillageSocietyState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        Optional<ContractAssignment> existing = data.activeContract(player.getUUID());
        if (existing.isPresent()) {
            ContractAssignment assignment = existing.get();
            if (assignment.contract().status() == ContractStatus.HUNTED
                    && assignment.contract().issuer().equals(event.getTarget().getUUID())) {
                complete(player, data, assignment);
                consume(event);
            } else if (assignment.contract().status() == ContractStatus.INVESTIGATING
                    && tryCollectTestimony(player, event.getTarget(), data, assignment)) {
                consume(event);
            } else if (assignment.contract().issuer().equals(event.getTarget().getUUID())) {
                player.displayClientMessage(Component.literal(statusMessage(assignment)), false);
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
        ContractAssignment assignment = new ContractAssignment(player.getUUID(), contract,
                available.location(), village, 2);
        data.putContract(assignment);
        available.story().advance(StoryStatus.INVESTIGATING);
        data.putStory(available);
        player.displayClientMessage(Component.literal("Contract accepted: investigate the incident near "
                + available.location().x() + ", " + available.location().y() + ", " + available.location().z()
                + ". Sneak-right-click nearby clue locations."), false);
        NeoForge.EVENT_BUS.post(new ContractStartedEvent(assignment));
        consume(event);
    }

    @SubscribeEvent
    public void onInvestigate(PlayerInteractEvent.RightClickBlock event) {
        if (!FolkloreConfig.CONTRACTS.get() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty() || !(player.level() instanceof ServerLevel level)) return;
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null || assignment.contract().status() != ContractStatus.INVESTIGATING) return;
        String dimension = level.dimension().location().toString();
        EvidenceRecord clue = data.evidence().stream()
                .filter(value -> value.collectedBy().isEmpty()
                        && value.concept().equals(assignment.contract().targetConcept())
                        && !assignment.contract().evidence().contains(value.type())
                        && value.position().dimension().equals(dimension)
                        && value.position().distanceSquared(event.getPos()) <= 16.0D)
                .min(Comparator.comparingDouble(value -> value.position().distanceSquared(event.getPos())))
                .orElse(null);
        if (clue == null) {
            if (assignment.investigationCenter().dimension().equals(dimension)
                    && assignment.investigationCenter().distanceSquared(event.getPos()) <= 1024.0D) {
                long now = level.getGameTime();
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
                ? ". Target identified: " + assignment.contract().targetConcept() : "")), false);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public void onTargetKilled(LivingDeathEvent event) {
        if (!FolkloreConfig.CONTRACTS.get() || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        FolkloreSavedData data = FolkloreSavedData.get(player.getServer());
        ContractAssignment assignment = data.activeContract(player.getUUID()).orElse(null);
        if (assignment == null || assignment.contract().status() != ContractStatus.IDENTIFIED) return;
        String actual = SecretFacts.canonicalConcept(event.getEntity());
        String registry = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        boolean matches = actual.equals(assignment.contract().targetConcept())
                || com.darkfolklore.core.data.FolkloreDataManager.INSTANCE.canonical().resolve(registry)
                .map(value -> value.concept().equals(assignment.contract().targetConcept())).orElse(false);
        if (!matches || !assignment.contract().markHunted()) return;
        data.putContract(assignment);
        data.stories().stream().filter(value -> value.villageKey().equals(assignment.villageKey())
                && value.story().concept().equals(assignment.contract().targetConcept())
                && value.story().status() == StoryStatus.INVESTIGATING).findFirst().ifPresent(story -> {
            story.story().advance(StoryStatus.CONFRONTATION); data.putStory(story);
        });
        player.displayClientMessage(Component.literal("Target defeated. Return to the contract issuer."), false);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!FolkloreConfig.CONTRACTS.get() || event.getServer().getTickCount() % 200 != 0) return;
        FolkloreSavedData data = FolkloreSavedData.get(event.getServer());
        long now = event.getServer().overworld().getGameTime();
        for (ContractAssignment assignment : data.contracts()) {
            if (assignment.contract().expire(now)) data.putContract(assignment);
        }
        for (PersistentStory story : data.stories()) {
            if (story.story().expire(now)) data.putStory(story);
        }
        feedbackCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > 1200L);
    }

    private static void complete(ServerPlayer player, FolkloreSavedData data, ContractAssignment assignment) {
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
        data.stories().stream().filter(value -> value.villageKey().equals(assignment.villageKey())
                && value.story().concept().equals(assignment.contract().targetConcept())
                && !value.story().status().terminal()).findFirst().ifPresent(story -> {
            if (story.story().status() == StoryStatus.INVESTIGATING) story.story().advance(StoryStatus.RESOLVED);
            else if (story.story().status() == StoryStatus.CONFRONTATION) story.story().advance(StoryStatus.RESOLVED);
            data.putStory(story);
        });
        player.displayClientMessage(Component.literal("Contract complete: 8 emeralds, 150 XP, reputation and lore awarded."), false);
        NeoForge.EVENT_BUS.post(new ContractCompletedEvent(assignment));
    }

    private static String statusMessage(ContractAssignment assignment) {
        return switch (assignment.contract().status()) {
            case INVESTIGATING -> "Active contract: collect distinct evidence near the investigation area.";
            case IDENTIFIED -> "Active contract: hunt " + assignment.contract().targetConcept() + ".";
            case HUNTED -> "Active contract: return to issuer " + assignment.contract().issuer() + ".";
            default -> "Contract status: " + assignment.contract().status();
        };
    }

    private static boolean tryCollectTestimony(ServerPlayer player, Entity witness, FolkloreSavedData data,
                                               ContractAssignment assignment) {
        if (!(witness instanceof LivingEntity) || witness.getUUID().equals(player.getUUID())) return false;
        Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord> testimony = data.knowledgeHeldBy(witness.getUUID())
                .stream().filter(entry -> entry.getValue().confidence() >= 0.35F
                        && entry.getValue().state().strength() >= SocialKnowledgeState.SUSPECTED.strength()
                        && concept(entry.getKey().secret()).equals(assignment.contract().targetConcept()))
                .max(Comparator.comparingDouble(entry -> entry.getValue().confidence())).orElse(null);
        if (testimony == null) return false;
        ContractStatus before = assignment.contract().status();
        if (!assignment.contract().addEvidence(EvidenceType.TESTIMONY, assignment.requiredDistinctClues())) return false;
        data.putContract(assignment);
        LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 3);
        if (before != ContractStatus.IDENTIFIED
                && assignment.contract().status() == ContractStatus.IDENTIFIED) {
            LoreEngine.INSTANCE.grant(player, assignment.contract().targetConcept(), 8);
        }
        player.displayClientMessage(Component.literal("Credible witness testimony recorded (confidence "
                + Math.round(testimony.getValue().confidence() * 100.0F) + "%)."
                + (assignment.contract().status() == ContractStatus.IDENTIFIED
                ? " The evidence now identifies " + assignment.contract().targetConcept() + "." : "")), false);
        return true;
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
