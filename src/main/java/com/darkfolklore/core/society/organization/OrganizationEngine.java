package com.darkfolklore.core.society.organization;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.api.event.ContractCompletedEvent;
import com.darkfolklore.core.api.event.ConfirmedLivingDeathEvent;
import com.darkfolklore.core.api.event.WitnessEvent;
import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.mca.McaPersonalityInfluence;
import com.darkfolklore.core.compat.mcacapitals.PoliticalContext;
import com.darkfolklore.core.compat.mcacapitals.PoliticalWeights;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.social.SecretClaimKey;
import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeKey;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.PublicRevealRules;
import com.darkfolklore.core.society.FamilySecretReaction;
import com.darkfolklore.core.society.FamilySecretRules;
import com.darkfolklore.core.society.SecretFacts;
import com.darkfolklore.core.society.SocialEntityClassifier;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.society.village.VillageSocietyState;
import com.darkfolklore.core.society.story.SocietyStoryEngine;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

/** Event-driven organization lifecycle. No global entity scans are performed. */
public final class OrganizationEngine {
    public static final OrganizationEngine INSTANCE = new OrganizationEngine();
    private int maintenanceCursor;
    private boolean organizationCapLogged;

    private OrganizationEngine() {}

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!FolkloreConfig.ORGANIZATIONS.get() || !FolkloreConfig.ORGANIZATION_BEHAVIOR.get()
                || !(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        if (entity instanceof Player || !SocialEntityClassifier.isSocial(entity)) return;

        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        Set<UUID> existingMemberships = data.organizationsForMember(entity.getUUID());
        if (!existingMemberships.isEmpty()) {
            for (UUID organizationId : existingMemberships) {
                data.organization(organizationId).ifPresent(organization -> {
                    if (organization.markMemberSeen(entity.getUUID(), level.getGameTime())) {
                        data.putOrganization(organization);
                    }
                });
            }
            return;
        }

        // EntityJoinLevelEvent is also fired when persistent entities are loaded from disk. Recruitment/founding is
        // a lifecycle decision, not a chunk-loading side effect: otherwise repeatedly unloading a villager rerolls
        // recruitment probability and old worlds manufacture organizations merely by being explored again.
        if (event.loadedFromDisk()) return;

        OrganizationType affiliation = OrganizationRules.naturalAffiliation(SecretFacts.actualSecrets(entity)).orElse(null);
        if (affiliation == null) return;
        VillageKey village = VillageKey.at(level, entity.blockPosition());
        Organization compatible = data.organizations().stream()
                .filter(value -> value.type() == affiliation && value.home().equals(village.serialized()))
                .min(Comparator.comparing(value -> value.id().toString())).orElse(null);

        if (compatible == null) {
            OrganizationArchetypeDefinition archetype = FolkloreDataManager.INSTANCE
                    .organizationArchetype(affiliation).orElse(null);
            boolean autoFound = archetype == null ? OrganizationRules.mayAutoFound(affiliation) : archetype.autoFound();
            if (!autoFound) return;
            Organization founded = new Organization(UUID.randomUUID(), affiliation,
                    OrganizationRules.generatedName(affiliation, village.regionX(), village.regionZ()), entity.getUUID());
            founded.setHome(village.serialized());
            founded.setInfluence(archetype == null ? 5 : archetype.baseInfluence());
            if (archetype != null) founded.restoreObjectives(archetype.objectives());
            founded.addEvent(OrganizationEvent.of(OrganizationEventType.FOUNDED, level.getGameTime(),
                    entity.getUUID(), null, "founded by an eligible local resident"));
            if (!data.tryPutOrganization(founded, FolkloreConfig.MAX_ORGANIZATIONS.get())) {
                if (!organizationCapLogged) {
                    organizationCapLogged = true;
                    DarkFolkloreCore.LOGGER.warn("[society] Organization cap {} reached; further automatic "
                                    + "founding is suppressed until capacity is available",
                            FolkloreConfig.MAX_ORGANIZATIONS.get());
                }
            } else if (entity instanceof LivingEntity recruit) {
                SocietyStoryEngine.INSTANCE.onOrganizationRecruitment(level, recruit, affiliation);
            }
            return;
        }

        OrganizationArchetypeDefinition archetype = FolkloreDataManager.INSTANCE
                .organizationArchetype(affiliation).orElse(null);
        if (archetype != null && compatible.members().size() >= archetype.maxMembers()) return;
        if (level.getRandom().nextDouble() > FolkloreConfig.ORGANIZATION_RECRUITMENT_RATE.get()) return;
        if (compatible.addMember(entity.getUUID())) {
            compatible.addEvent(OrganizationEvent.of(OrganizationEventType.MEMBER_JOINED, level.getGameTime(),
                    entity.getUUID(), null, "eligible local recruit"));
            compatible.setInfluence(compatible.influence() + 1);
            data.putOrganization(compatible);
            if (entity instanceof LivingEntity recruit) {
                SocietyStoryEngine.INSTANCE.onOrganizationRecruitment(level, recruit, affiliation);
            }
        }
    }

    @SubscribeEvent
    public void onConfirmedDeath(ConfirmedLivingDeathEvent event) {
        if (!FolkloreConfig.ORGANIZATIONS.get() || event.entity() instanceof Player
                || !(event.entity().level() instanceof ServerLevel level)) return;
        FolkloreSavedData.DeathCleanupResult result = FolkloreSavedData.get(level.getServer())
                .handleConfirmedDeath(event.entity().getUUID(), level.getGameTime());
        if (FolkloreConfig.DEBUG_LOGGING.get() && (result.membershipsRemoved() > 0
                || result.organizationsDissolved() > 0)) {
            DarkFolkloreCore.LOGGER.debug("[society] Confirmed death cleanup: memberships={}, dissolved={}, successions={}",
                    result.membershipsRemoved(), result.organizationsDissolved(), result.successions());
        }
    }

    @SubscribeEvent
    public void onWitness(WitnessEvent event) {
        if (!FolkloreConfig.ORGANIZATIONS.get() || !FolkloreConfig.ORGANIZATION_BEHAVIOR.get()) return;
        FolkloreSavedData data = FolkloreSavedData.get(event.level().getServer());
        SocialKnowledgeKey individualClaim = new SocialKnowledgeKey(event.observer().getUUID(),
                event.actor().getUUID(), event.secret());
        FamilySecretReaction familyReaction = data.familyReaction(individualClaim).orElse(null);
        VillageKey witnessVillage = VillageKey.at(event.level(), event.observer().blockPosition());
        VillageSocietyState witnessVillageState = data.village(witnessVillage.serialized());
        if (familyReaction == FamilySecretReaction.FEARFUL_WITHDRAWAL) {
            witnessVillageState.adjustFear(2);
            data.setDirty();
        }
        if (familyReaction != null && FamilySecretRules.suppressesOrganizationReport(familyReaction)) return;

        CompatibilityManager compatibility = CompatibilityManager.INSTANCE;
        McaPersonalityInfluence observerPersonality = FolkloreConfig.PERSONALITY_MODIFIERS.get()
                ? compatibility.mcaSocial().personality(event.observer())
                .map(McaPersonalityInfluence::fromVerifiedName)
                .orElseGet(() -> new McaPersonalityInfluence(false, 0, 0, 0))
                : new McaPersonalityInfluence(false, 0, 0, 0);
        PoliticalWeights political = PoliticalWeights.NONE;
        if (FolkloreConfig.MCA_CAPITALS.get()) {
            PoliticalContext politicalContext = compatibility.mcaCapitals().politicalContext(
                    event.level(), event.observer().getUUID());
            political = FolkloreDataManager.INSTANCE.politicalWeights(politicalContext.role());
        }
        int politicalAwareness = (int) Math.round(political.publicAwareness() * 5.0D);
        if (politicalAwareness > 0) {
            witnessVillageState.adjustPublicAwareness(politicalAwareness);
            witnessVillageState.adjustPoliticalImportance((int) Math.round(
                    (political.credibility() + political.publicAwareness()) * 4.0D));
            data.setDirty();
        }

        OrganizationIntelKey claim = new OrganizationIntelKey(event.actor().getUUID(), event.secret());
        boolean authorizedHunter = false;
        for (UUID organizationId : data.organizationsForMember(event.observer().getUUID())) {
            Organization organization = data.organization(organizationId).orElse(null);
            if (organization == null || !organization.recordIntelligence(claim, event.state())) continue;
            organization.addEvent(OrganizationEvent.of(OrganizationEventType.INTELLIGENCE_RECEIVED,
                    event.level().getGameTime(), event.observer().getUUID(), event.actor().getUUID(),
                    event.secret().name().toLowerCase(Locale.ROOT) + " " + event.state().name().toLowerCase(Locale.ROOT)));
            boolean revealAuthority = FolkloreDataManager.INSTANCE.organizationArchetype(organization.type())
                    .map(OrganizationArchetypeDefinition::publicRevealAuthority)
                    .orElse(organization.type() == OrganizationType.HUNTER_SOCIETY);
            if (revealAuthority
                    && event.state().strength() >= SocialKnowledgeState.CONFIRMED.strength()) {
                authorizedHunter = true;
                int investigation = 1 + (int) Math.round(political.investigationPriority() * 4.0D
                        + Math.max(-0.5D, observerPersonality.investigationModifier()));
                organization.setInfluence(organization.influence() + Math.max(0, investigation));
                organization.addEvent(OrganizationEvent.of(OrganizationEventType.INVESTIGATION_OPENED,
                        event.level().getGameTime(), event.observer().getUUID(), event.actor().getUUID(),
                        "credible supernatural report"));
                for (UUID subjectOrganizationId : data.organizationsForMember(event.actor().getUUID())) {
                    if (subjectOrganizationId.equals(organization.id())) continue;
                    Organization subjectOrganization = data.organization(subjectOrganizationId).orElse(null);
                    if (subjectOrganization == null || subjectOrganization.type() == OrganizationType.HUNTER_SOCIETY) {
                        continue;
                    }
                    organization.setRelation(subjectOrganizationId, OrganizationRelation.HOSTILE);
                    subjectOrganization.setRelation(organization.id(), OrganizationRelation.HOSTILE);
                    subjectOrganization.addEvent(OrganizationEvent.of(OrganizationEventType.INVESTIGATION_OPENED,
                            event.level().getGameTime(), event.observer().getUUID(), event.actor().getUUID(),
                            "hunter organization identified a member"));
                    data.putOrganization(subjectOrganization);
                }
            }
            if ((organization.type() == OrganizationType.VAMPIRE_COVEN && event.secret() == SecretType.VAMPIRE
                    || organization.type() == OrganizationType.WEREWOLF_PACK && event.secret() == SecretType.WEREWOLF)
                    && organization.members().contains(event.actor().getUUID())) {
                organization.addEvent(OrganizationEvent.of(OrganizationEventType.SECRET_SHARED,
                        event.level().getGameTime(), event.observer().getUUID(), event.actor().getUUID(),
                        "member identity retained inside the organization"));
            }
            int response = (int) Math.round(political.organizationResponse() * 3.0D);
            if (response > 0) organization.setInfluence(organization.influence() + response);
            data.putOrganization(organization);
        }
        if (!authorizedHunter) return;

        PublicRevealRules.Assessment assessment = PublicRevealRules.assess(data.knowledgeAbout(event.actor().getUUID()),
                event.actor().getUUID(), event.secret(), FolkloreConfig.PUBLIC_REVEAL_WITNESSES.get(),
                FolkloreConfig.PUBLIC_REVEAL_CONFIDENCE.get().floatValue());
        SecretClaimKey publicClaim = new SecretClaimKey(event.actor().getUUID(), event.secret());
        if (!assessment.eligible() || !data.markPublic(publicClaim, event.level().getGameTime())) return;
        SocietyStoryEngine.INSTANCE.onPublicReveal(event.level(), event.actor(), event.secret());
        VillageKey villageKey = VillageKey.at(event.level(), event.actor().blockPosition());
        VillageSocietyState village = data.village(villageKey.serialized());
        village.recordIncident(assessment.credibleObservers(), true, 5);
        data.setDirty();
        for (Organization organization : data.organizations()) {
            if (organization.type() == OrganizationType.HUNTER_SOCIETY
                    && organization.home().equals(villageKey.serialized())) {
                organization.addEvent(OrganizationEvent.of(OrganizationEventType.PUBLIC_REVEAL,
                        event.level().getGameTime(), event.observer().getUUID(), event.actor().getUUID(),
                        assessment.reason()));
                data.putOrganization(organization);
            }
        }
    }

    @SubscribeEvent
    public void onContractCompleted(ContractCompletedEvent event) {
        if (!FolkloreConfig.ORGANIZATIONS.get()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        FolkloreSavedData data = FolkloreSavedData.get(server);
        for (Organization organization : data.organizations()) {
            if (!organization.home().equals(event.assignment().villageKey())) continue;
            organization.setInfluence(organization.influence()
                    + OrganizationRules.completedContractInfluence(organization.type()));
            organization.addEvent(OrganizationEvent.of(OrganizationEventType.CONTRACT_COMPLETED,
                    server.overworld().getGameTime(), event.assignment().player(), null,
                    event.assignment().contract().targetConcept()));
            data.putOrganization(organization);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!FolkloreConfig.ORGANIZATIONS.get() || event.getServer().getTickCount() % 1200 != 0) return;
        FolkloreSavedData data = FolkloreSavedData.get(event.getServer());
        data.enforceSocialLimit(FolkloreConfig.MAX_SOCIAL_RECORDS.get());
        data.enforcePublicSecretLimit(Math.max(1000, FolkloreConfig.MAX_SOCIAL_RECORDS.get() / 4));
        List<Organization> organizations = List.copyOf(data.organizations());
        if (organizations.size() < FolkloreConfig.MAX_ORGANIZATIONS.get()) organizationCapLogged = false;
        if (organizations.isEmpty()) {
            maintenanceCursor = 0;
            return;
        }
        int budget = Math.min(FolkloreConfig.ORGANIZATION_MAINTENANCE_BUDGET.get(), organizations.size());
        for (int i = 0; i < budget; i++) {
            Organization organization = organizations.get((maintenanceCursor + i) % organizations.size());
            if (!organization.members().contains(organization.leader())) {
                organization.setLeader(organization.leader());
                data.putOrganization(organization);
            }
        }
        maintenanceCursor = (maintenanceCursor + budget) % organizations.size();
    }
}
