package com.darkfolklore.core.society.story;

import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.investigation.EvidenceRecord;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.*;
import com.darkfolklore.core.society.SecretFacts;
import com.darkfolklore.core.society.village.VillageKey;
import com.darkfolklore.core.society.witness.WitnessEngine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.*;

public final class IncidentStoryEngine {
    public static final IncidentStoryEngine INSTANCE = new IncidentStoryEngine();
    private IncidentStoryEngine() {}

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!FolkloreConfig.DYNAMIC_STORIES.get() || !(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof LivingEntity actor)
                || SecretFacts.actualSecrets(actor).isEmpty() || !isSocialVictim(event.getEntity())) return;
        FolkloreSavedData data = FolkloreSavedData.get(level.getServer());
        long now = level.getGameTime();
        VillageKey village = VillageKey.at(level, event.getEntity().blockPosition());
        boolean coolingDown = data.stories().stream().anyMatch(value -> value.villageKey().equals(village.serialized())
                && (value.story().template().equals("drained_animal")
                || value.story().template().equals("body_discovered"))
                && now - value.story().createdAt() < FolkloreConfig.STORY_COOLDOWN.get());
        if (coolingDown) return;

        String concept = SecretFacts.canonicalConcept(actor);
        String template = event.getEntity() instanceof Animal ? "drained_animal" : "body_discovered";
        StoryInstance story = new StoryInstance(UUID.randomUUID(), template, concept, now,
                now + FolkloreConfig.CONTRACT_LIFETIME.get() * 2L);
        story.addActor(actor.getUUID()); story.addActor(event.getEntity().getUUID());
        WorldPosition location = WorldPosition.of(level, event.getEntity().blockPosition());
        data.putStory(new PersistentStory(story, location, village.serialized()));

        SecretType secret = SecretFacts.actualSecrets(actor).stream()
                .filter(value -> value != SecretType.SUPERNATURAL_IDENTITY).findFirst()
                .orElse(SecretType.SUPERNATURAL_IDENTITY);
        EvidenceType signature = secret == SecretType.VAMPIRE ? EvidenceType.BITE_MARK
                : secret == SecretType.WEREWOLF ? EvidenceType.FOOTPRINT : EvidenceType.MAGICAL_RESIDUE;
        long expires = now + FolkloreConfig.EVIDENCE_LIFETIME.get();
        data.addEvidence(new EvidenceRecord(UUID.randomUUID(), EvidenceType.BLOOD, concept,
                Optional.of(actor.getUUID()), location, now, expires, Optional.empty()));
        data.addEvidence(new EvidenceRecord(UUID.randomUUID(), signature, concept,
                Optional.of(actor.getUUID()), WorldPosition.of(level, actor.blockPosition()),
                now, expires, Optional.empty()));
        WitnessEngine.INSTANCE.recordIncident(level, actor, event.getEntity(), secret, EvidenceType.BODY, 8);
    }

    private static boolean isSocialVictim(LivingEntity entity) {
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace();
        return entity instanceof Animal || entity instanceof Villager || namespace.equals("mca");
    }
}
