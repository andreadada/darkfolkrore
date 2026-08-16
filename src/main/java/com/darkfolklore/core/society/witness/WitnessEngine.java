package com.darkfolklore.core.society.witness;

import com.darkfolklore.core.api.event.SecretDiscoveredEvent;
import com.darkfolklore.core.api.event.WitnessEvent;
import com.darkfolklore.core.compat.CompatibilityManager;
import com.darkfolklore.core.compat.mca.McaSocialContext;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.knowledge.observation.CreatureSightingKey;
import com.darkfolklore.core.knowledge.observation.CreatureSightingRecord;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.persistence.InvestigationSavedData;
import com.darkfolklore.core.persistence.WorldPosition;
import com.darkfolklore.core.society.SecretFacts;
import com.darkfolklore.core.society.FamilySecretReaction;
import com.darkfolklore.core.society.FamilySecretRules;
import com.darkfolklore.core.society.SocialEntityClassifier;
import com.darkfolklore.core.society.organization.*;
import com.darkfolklore.core.society.rumor.RumorEngine;
import com.darkfolklore.core.society.village.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.*;

public final class WitnessEngine {
    public static final WitnessEngine INSTANCE = new WitnessEngine();
    private static final int MAX_INCIDENT_COOLDOWNS = 2048;
    private final LinkedHashMap<IncidentKey, Long> incidentCooldown = new LinkedHashMap<>();

    private WitnessEngine() {}

    @SubscribeEvent
    public void onSupernaturalDamage(LivingDamageEvent.Post event) {
        if (!FolkloreConfig.WITNESSES.get() || event.getNewDamage() <= 0
                || !(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof LivingEntity actor)
                || actor == event.getEntity()) return;
        Optional<SecretType> secret = primarySecret(actor);
        if (secret.isEmpty()) return;
        long now = level.getGameTime();
        IncidentKey key = new IncidentKey(actor.getUUID(), event.getEntity().getUUID(), secret.get());
        if (now - incidentCooldown.getOrDefault(key, Long.MIN_VALUE / 2) < 100) return;
        incidentCooldown.remove(key);
        incidentCooldown.put(key, now);
        if (incidentCooldown.size() > MAX_INCIDENT_COOLDOWNS) {
            incidentCooldown.entrySet().removeIf(entry -> now - entry.getValue() > 1200);
            trimOldest(incidentCooldown, MAX_INCIDENT_COOLDOWNS);
        }
        recordIncident(level, actor, event.getEntity(), secret.get(), EvidenceType.DIRECT_WITNESS,
                Math.max(1, Math.min(10, Math.round(event.getNewDamage() / 2.0F))));
    }

    public List<LivingEntity> recordIncident(ServerLevel level, LivingEntity actor, Entity victim,
                                             SecretType secret, EvidenceType evidence, int severity) {
        if (!FolkloreConfig.SOCIAL_KNOWLEDGE.get() || !FolkloreConfig.WITNESSES.get()) return List.of();
        List<LivingEntity> nearby = candidateWitnesses(level, actor, victim);
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        List<LivingEntity> accepted = new ArrayList<>();
        long now = level.getGameTime();
        int radius = FolkloreConfig.WITNESS_RADIUS.get();
        for (LivingEntity observer : nearby) {
            boolean direct = observer.hasLineOfSight(actor);
            if (!direct && observer.distanceToSqr(actor) > radius * radius * 0.36D) continue;
            SocialKnowledgeState state = direct ? SocialKnowledgeState.CONFIRMED : SocialKnowledgeState.RUMOR;
            float confidence = direct ? Math.min(1.0F, 0.75F + severity * 0.025F) : 0.35F;
            KnowledgeSource source = direct ? KnowledgeSource.DIRECT_WITNESS : KnowledgeSource.RUMOR;
            EvidenceType recordEvidence = direct ? evidence : EvidenceType.TESTIMONY;
            SocialKnowledgeKey key = new SocialKnowledgeKey(observer.getUUID(), actor.getUUID(), secret);
            SocialKnowledgeRecord merged = data.mergeSocial(key,
                    new SocialKnowledgeRecord(state, confidence, source, now, recordEvidence));
            boolean suppressRetelling = false;
            if (FolkloreConfig.FAMILY_SECRETS.get() && FolkloreConfig.RELATIONSHIP_TRUST.get()) {
                McaSocialContext relationship = CompatibilityManager.INSTANCE.mcaSocial().relationship(observer, actor);
                Optional<FamilySecretReaction> reaction = FamilySecretRules.choose(relationship.relationship(),
                        relationship.observerPersonality().orElse(null),
                        SecretFacts.actualSecrets(observer).contains(SecretType.HUNTER));
                if (reaction.isPresent()) {
                    data.setFamilyReaction(key, reaction.get());
                    suppressRetelling = FamilySecretRules.suppressesRetelling(reaction.get());
                }
            }
            accepted.add(observer);
            NeoForge.EVENT_BUS.post(new WitnessEvent(level, actor, observer, secret, merged.state()));
            NeoForge.EVENT_BUS.post(new SecretDiscoveredEvent(observer.getUUID(), actor.getUUID(), secret, merged));
            if (!suppressRetelling && merged.state().strength() >= SocialKnowledgeState.SUSPECTED.strength()) {
                RumorEngine.INSTANCE.enqueue(observer, actor.getUUID(), secret, merged, 0);
            }
        }

        updateVillageAfterWitnesses(level, actor, accepted, severity, now);
        return List.copyOf(accepted);
    }

    /**
     * Records concept-level creature observations without manufacturing a social
     * identity secret. Used for cryptids, spirits, demons, constructs and Fae.
     */
    public List<LivingEntity> recordCreatureSighting(ServerLevel level, LivingEntity actor, Entity victim,
                                                      String concept, EvidenceType evidence, int severity) {
        if (!FolkloreConfig.SOCIAL_KNOWLEDGE.get() || !FolkloreConfig.WITNESSES.get()
                || !validConcept(concept)) return List.of();
        List<LivingEntity> nearby = candidateWitnesses(level, actor, victim);
        InvestigationSavedData observations = InvestigationSavedData.get(level.getServer());
        List<LivingEntity> accepted = new ArrayList<>();
        long now = level.getGameTime();
        int radius = FolkloreConfig.WITNESS_RADIUS.get();
        WorldPosition location = WorldPosition.of(level, actor.blockPosition());
        for (LivingEntity observer : nearby) {
            boolean direct = observer.hasLineOfSight(actor);
            if (!direct && observer.distanceToSqr(actor) > radius * radius * 0.36D) continue;
            SocialKnowledgeState state = direct ? SocialKnowledgeState.CONFIRMED : SocialKnowledgeState.RUMOR;
            float confidence = direct ? Math.min(1.0F, 0.75F + severity * 0.025F) : 0.35F;
            KnowledgeSource source = direct ? KnowledgeSource.DIRECT_WITNESS : KnowledgeSource.RUMOR;
            observations.mergeSighting(new CreatureSightingKey(observer.getUUID(), concept),
                    new CreatureSightingRecord(state, confidence, source, now, Optional.of(actor.getUUID()),
                            Optional.of(location), direct ? evidence : EvidenceType.TESTIMONY));
            accepted.add(observer);
        }
        updateVillageAfterWitnesses(level, actor, accepted, severity, now);
        return List.copyOf(accepted);
    }

    private static List<LivingEntity> candidateWitnesses(ServerLevel level, LivingEntity actor, Entity victim) {
        int radius = FolkloreConfig.WITNESS_RADIUS.get();
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                actor.getBoundingBox().inflate(radius), observer -> observer.isAlive()
                        && SocialEntityClassifier.isSocial(observer)
                        && observer != actor && observer != victim
                        && !observer.isSleeping()
                        && !observer.hasEffect(MobEffects.BLINDNESS)
                        && (!(observer instanceof Player player) || !player.isSpectator()));
        nearby.sort(Comparator.comparingDouble(observer -> observer.distanceToSqr(actor)));
        if (nearby.size() > FolkloreConfig.MAX_WITNESSES.get()) {
            nearby = new ArrayList<>(nearby.subList(0, FolkloreConfig.MAX_WITNESSES.get()));
        }
        return nearby;
    }

    private static void updateVillageAfterWitnesses(ServerLevel level, LivingEntity actor,
                                                    List<LivingEntity> accepted, int severity, long now) {
        if (!FolkloreConfig.VILLAGE_SOCIETY.get()) return;
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        VillageKey villageKey = VillageKey.at(level, actor.blockPosition());
        VillageSocietyState village = data.village(villageKey.serialized());
        village.recordIncident(accepted.size(), accepted.stream().anyMatch(observer -> observer.hasLineOfSight(actor)), severity);
        data.setDirty();
        maybeCreateHunterSociety(data, villageKey, village, accepted, now);
    }

    private static void maybeCreateHunterSociety(FolkloreSavedData data, VillageKey key,
                                                 VillageSocietyState village, List<LivingEntity> witnesses,
                                                 long gameTime) {
        if (!FolkloreConfig.ORGANIZATIONS.get() || village.suspicion() < 30 || witnesses.isEmpty()) return;
        boolean exists = data.organizations().stream().anyMatch(org -> org.type() == OrganizationType.HUNTER_SOCIETY
                && org.home().equals(key.serialized()));
        if (exists) return;
        LivingEntity leader = witnesses.getFirst();
        Organization organization = new Organization(UUID.randomUUID(), OrganizationType.HUNTER_SOCIETY,
                "Hunter Society " + key.regionX() + "," + key.regionZ(), leader.getUUID());
        organization.setHome(key.serialized());
        organization.setInfluence(Math.max(10, village.suspicion() / 2));
        witnesses.stream().skip(1).limit(4).forEach(observer -> organization.addMember(observer.getUUID()));
        organization.addEvent(OrganizationEvent.of(OrganizationEventType.FOUNDED, gameTime,
                leader.getUUID(), null, "formed after repeated local incidents"));
        data.tryPutOrganization(organization, FolkloreConfig.MAX_ORGANIZATIONS.get());
    }

    /** Clears non-persistent incident throttles when a server lifecycle ends. */
    public void clearRuntimeState() {
        incidentCooldown.clear();
    }

    private static Optional<SecretType> primarySecret(Entity actor) {
        Set<SecretType> secrets = SecretFacts.actualSecrets(actor);
        for (SecretType type : List.of(SecretType.VAMPIRE, SecretType.WEREWOLF,
                SecretType.WITCH, SecretType.HUNTER, SecretType.FAE_TOUCHED)) {
            if (secrets.contains(type)) return Optional.of(type);
        }
        return Optional.empty();
    }

    private static boolean validConcept(String concept) {
        if (concept == null || !concept.contains(":")) return false;
        ResourceLocation id = ResourceLocation.tryParse(concept);
        return id != null && !id.getNamespace().isBlank() && !id.getPath().isBlank();
    }

    private static <K, V> void trimOldest(LinkedHashMap<K, V> map, int maximum) {
        while (map.size() > maximum) {
            Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }

    private record IncidentKey(UUID actor, UUID victim, SecretType secret) {}
}
