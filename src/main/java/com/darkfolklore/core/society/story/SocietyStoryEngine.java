package com.darkfolklore.core.society.story;

import com.darkfolklore.core.api.event.WitnessEvent;
import com.darkfolklore.core.api.event.WorldEventChangedEvent;
import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.mcacapitals.PoliticalRole;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.investigation.EvidenceRecord;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.persistence.WorldPosition;
import com.darkfolklore.core.society.*;
import com.darkfolklore.core.society.organization.OrganizationType;
import com.darkfolklore.core.society.rumor.RumorEngine;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.world.WorldEventType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.*;

/** Data-driven, event-triggered society stories with per-template regional cooldowns. */
public final class SocietyStoryEngine {
    public static final SocietyStoryEngine INSTANCE = new SocietyStoryEngine();
    private static final int MAX_PLAYERS_PER_WORLD_EVENT = 4;
    private static final int MAX_NEARBY_ACTORS = 16;

    private SocietyStoryEngine() {}

    @SubscribeEvent
    public void onWitness(WitnessEvent event) {
        if (!FolkloreConfig.DYNAMIC_STORIES.get()) return;
        FolkloreSavedData data = FolkloreSavedData.get(event.level().getServer());
        SocialKnowledgeKey key = new SocialKnowledgeKey(event.observer().getUUID(),
                event.actor().getUUID(), event.secret());
        if (data.familyReaction(key).isPresent()) {
            maybeCreate(event.level(), event.actor().blockPosition(), StoryTrigger.FAMILY_DISCOVERY,
                    concept(event.secret()), Optional.of(event.secret()),
                    List.of(event.observer().getUUID(), event.actor().getUUID()), false, 1.0D);
        }
        boolean hunterReport = data.organizationsForMember(event.observer().getUUID()).stream()
                .map(data::organization).flatMap(Optional::stream)
                .anyMatch(organization -> organization.type() == OrganizationType.HUNTER_SOCIETY);
        if (hunterReport && event.state().strength() >= SocialKnowledgeState.CONFIRMED.strength()) {
            Optional<PersistentStory> created = maybeCreate(event.level(), event.actor().blockPosition(),
                    StoryTrigger.HUNTER_INVESTIGATION,
                    concept(event.secret()), Optional.of(event.secret()),
                    List.of(event.observer().getUUID(), event.actor().getUUID()), false, 1.25D);
            if (created.map(value -> value.story().template().equals("darkfolklore:witness_threatened"))
                    .orElse(false)) {
                data.silenceRumors(event.observer().getUUID(), event.level().getGameTime() + 12000L);
                data.village(VillageKey.at(event.level(), event.observer().blockPosition()).serialized()).adjustFear(2);
                data.setDirty();
            }
        }
    }

    @SubscribeEvent
    public void onWorldEvent(WorldEventChangedEvent event) {
        if (!event.active() || !FolkloreConfig.DYNAMIC_STORIES.get()) return;
        int players = 0;
        for (ServerPlayer player : event.level().players()) {
            if (player.isSpectator() || players++ >= MAX_PLAYERS_PER_WORLD_EVENT) continue;
            List<LivingEntity> nearby = event.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(48.0D), entity -> entity.isAlive()
                            && !(entity instanceof Player) && SocialEntityClassifier.isSocial(entity));
            nearby.sort(Comparator.comparingDouble(player::distanceToSqr));
            if (nearby.size() > MAX_NEARBY_ACTORS) nearby = nearby.subList(0, MAX_NEARBY_ACTORS);
            if (event.type() == WorldEventType.FULL_MOON) {
                LivingEntity werewolf = nearby.stream()
                        .filter(entity -> SecretFacts.actualSecrets(entity).contains(SecretType.WEREWOLF))
                        .findFirst().orElse(null);
                if (werewolf != null && maybeCreate(event.level(), werewolf.blockPosition(),
                        StoryTrigger.FULL_MOON_INCIDENT, "darkfolklore:werewolf",
                        Optional.of(SecretType.WEREWOLF), List.of(werewolf.getUUID()), false, 1.5D).isPresent()) {
                    addEventEvidence(event.level(), werewolf, EvidenceType.FOOTPRINT, "darkfolklore:werewolf");
                }
            } else if (event.type() == WorldEventType.WITCHING_HOUR) {
                LivingEntity witch = nearby.stream()
                        .filter(entity -> SecretFacts.actualSecrets(entity).contains(SecretType.WITCH))
                        .findFirst().orElse(null);
                if (witch != null && maybeCreate(event.level(), witch.blockPosition(), StoryTrigger.WITCHING_HOUR,
                        "darkfolklore:witch", Optional.of(SecretType.WITCH), List.of(witch.getUUID()),
                        false, 1.25D).isPresent()) {
                    addEventEvidence(event.level(), witch, EvidenceType.MAGICAL_RESIDUE, "darkfolklore:witch");
                } else if (FolkloreConfig.FALSE_ACCUSATIONS.get()) {
                    createControlledFalseAccusation(event.level(), nearby);
                }
            }
        }
    }

    public Optional<PersistentStory> onOrganizationRecruitment(ServerLevel level, LivingEntity recruit,
                                                                OrganizationType type) {
        SecretType secret = switch (type) {
            case VAMPIRE_COVEN -> SecretType.VAMPIRE;
            case HUNTER_SOCIETY -> SecretType.HUNTER;
            case WEREWOLF_PACK -> SecretType.WEREWOLF;
            case WITCH_COVEN -> SecretType.WITCH;
        };
        return maybeCreate(level, recruit.blockPosition(), StoryTrigger.ORGANIZATION_RECRUITMENT,
                concept(secret), Optional.of(secret), List.of(recruit.getUUID()), false, 1.0D);
    }

    public void onPublicReveal(ServerLevel level, Entity subject, SecretType secret) {
        maybeCreate(level, subject.blockPosition(), StoryTrigger.PUBLIC_REVEAL, concept(secret),
                Optional.of(secret), List.of(subject.getUUID()), false, 2.0D);
        PoliticalRole role = FolkloreConfig.MCA_CAPITALS.get()
                ? CompatibilityManager.INSTANCE.mcaCapitals().politicalContext(level, subject.getUUID()).role()
                : PoliticalRole.NONE;
        if (role != PoliticalRole.NONE && role != PoliticalRole.COMMONER && role != PoliticalRole.UNKNOWN) {
            maybeCreate(level, subject.blockPosition(), StoryTrigger.POLITICAL_EXPOSURE, concept(secret),
                    Optional.of(secret), List.of(subject.getUUID()), true, 2.0D);
        }
    }

    public Optional<PersistentStory> maybeCreate(ServerLevel level, net.minecraft.core.BlockPos position,
                                                  StoryTrigger trigger, String eventConcept,
                                                  Optional<SecretType> secret, Collection<UUID> actors,
                                                  boolean capital, double rateScale) {
        if (!FolkloreConfig.DYNAMIC_STORIES.get()) return Optional.empty();
        List<StoryTemplateDefinition> eligible = FolkloreDataManager.INSTANCE.storyTemplates().stream()
                .filter(StoryTemplateDefinition::enabled)
                .filter(template -> template.trigger() == trigger)
                .filter(template -> !template.capitalOnly() || capital)
                .filter(template -> template.requiredSecret().isEmpty()
                        || template.requiredSecret().equals(secret))
                .toList();
        if (eligible.isEmpty()) return Optional.empty();
        double rate = Math.min(1.0D, FolkloreConfig.DYNAMIC_STORY_RATE.get() * Math.max(0.0D, rateScale));
        if (level.getRandom().nextDouble() > rate) return Optional.empty();

        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        VillageKey village = VillageKey.at(level, position);
        long now = level.getGameTime();
        List<StoryTemplateDefinition> cooled = eligible.stream().filter(template -> data.stories().stream()
                .noneMatch(existing -> existing.villageKey().equals(village.serialized())
                        && existing.story().template().equals(template.id())
                        && now - existing.story().createdAt() < template.cooldownTicks())).toList();
        if (cooled.isEmpty()) return Optional.empty();
        int totalWeight = cooled.stream().mapToInt(StoryTemplateDefinition::weight).sum();
        int roll = level.getRandom().nextInt(totalWeight);
        StoryTemplateDefinition selected = cooled.getLast();
        for (StoryTemplateDefinition candidate : cooled) {
            roll -= candidate.weight();
            if (roll < 0) {
                selected = candidate;
                break;
            }
        }
        StoryInstance story = new StoryInstance(UUID.randomUUID(), selected.id(),
                selected.resolvedConcept(eventConcept), now, now + selected.lifetimeTicks());
        actors.stream().filter(Objects::nonNull).limit(8).forEach(story::addActor);
        PersistentStory persistent = new PersistentStory(story, WorldPosition.of(level, position), village.serialized());
        data.putStory(persistent);
        return Optional.of(persistent);
    }

    public boolean isContractEligible(String templateId) {
        if (templateId.equals("drained_animal") || templateId.equals("body_discovered")) return true;
        return FolkloreDataManager.INSTANCE.storyTemplates().stream()
                .anyMatch(template -> template.id().equals(templateId) && template.contractEligible());
    }

    private void createControlledFalseAccusation(ServerLevel level, List<LivingEntity> nearby) {
        if (nearby.size() < 2) return;
        LivingEntity subject = nearby.stream()
                .filter(entity -> !SecretFacts.actualSecrets(entity).contains(SecretType.VAMPIRE))
                .findFirst().orElse(null);
        LivingEntity observer = nearby.stream().filter(entity -> entity != subject).findFirst().orElse(null);
        if (subject == null || observer == null) return;
        SocialKnowledgeRecord belief = new SocialKnowledgeRecord(SocialKnowledgeState.RUMOR, 0.25F,
                KnowledgeSource.RUMOR, level.getGameTime(), EvidenceType.MAGICAL_RESIDUE);
        if (!FalseAccusationRules.eligible(true, false, false, true, belief)) return;
        Optional<PersistentStory> story = maybeCreate(level, subject.blockPosition(),
                StoryTrigger.CONTROLLED_FALSE_ACCUSATION, "darkfolklore:vampire", Optional.empty(),
                List.of(observer.getUUID(), subject.getUUID()), false, 0.5D);
        if (story.isEmpty()) return;
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        data.mergeSocial(new SocialKnowledgeKey(observer.getUUID(), subject.getUUID(), SecretType.VAMPIRE), belief);
        RumorEngine.INSTANCE.enqueue(observer, subject.getUUID(), SecretType.VAMPIRE, belief, 0);
    }

    private static void addEventEvidence(ServerLevel level, LivingEntity actor, EvidenceType type, String concept) {
        long now = level.getGameTime();
        WorldPosition position = WorldPosition.of(level, actor.blockPosition());
        FolkloreSavedData.get(level.getServer()).addEvidence(new EvidenceRecord(UUID.randomUUID(), type, concept,
                Optional.of(actor.getUUID()), position, now, now + FolkloreConfig.EVIDENCE_LIFETIME.get(),
                Optional.empty()));
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
}
