package com.darkfolklore.core.persistence;

import com.darkfolklore.core.contracts.ContractAssignment;
import com.darkfolklore.core.contracts.MonsterContract;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.reputation.ReputationFaction;
import com.darkfolklore.core.society.bloodline.LineageRecord;
import com.darkfolklore.core.society.FamilySecretReaction;
import com.darkfolklore.core.society.organization.*;
import com.darkfolklore.core.society.story.PersistentStory;
import com.darkfolklore.core.society.story.StoryInstance;
import com.darkfolklore.core.society.story.StoryStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FolkloreSavedDataTest {
    @Test
    void publicRevealIsGlobalAndRoundTripsWithoutObserverFanout() {
        FolkloreSavedData original = new FolkloreSavedData();
        UUID subject = UUID.randomUUID();
        UUID observer = UUID.randomUUID();
        SecretClaimKey claim = new SecretClaimKey(subject, SecretType.VAMPIRE);

        assertTrue(original.markPublic(claim, 250));
        assertFalse(original.markPublic(claim, 300));
        SocialKnowledgeRecord synthetic = original.social(
                new SocialKnowledgeKey(observer, subject, SecretType.VAMPIRE)).orElseThrow();
        assertEquals(SocialKnowledgeState.PUBLIC, synthetic.state());
        assertEquals(KnowledgeSource.PUBLIC_REVEAL, synthetic.source());
        assertTrue(original.knowledgeAbout(subject).isEmpty(), "global publication must not create observer rows");

        FolkloreSavedData restored = FolkloreSavedData.load(original.save(new CompoundTag(), null), null);
        assertTrue(restored.isPublic(claim));
        assertEquals(SocialKnowledgeState.PUBLIC, restored.social(
                new SocialKnowledgeKey(UUID.randomUUID(), subject, SecretType.VAMPIRE)).orElseThrow().state());
    }

    @Test
    void familyReactionPersistsOnlyAlongsideItsBelief() {
        FolkloreSavedData original = new FolkloreSavedData();
        SocialKnowledgeKey key = new SocialKnowledgeKey(UUID.randomUUID(), UUID.randomUUID(), SecretType.WEREWOLF);
        original.mergeSocial(key, new SocialKnowledgeRecord(SocialKnowledgeState.CONFIRMED, 0.9F,
                KnowledgeSource.DIRECT_WITNESS, 10, EvidenceType.DIRECT_WITNESS));
        original.setFamilyReaction(key, FamilySecretReaction.PROTECT_SECRET);

        FolkloreSavedData restored = FolkloreSavedData.load(original.save(new CompoundTag(), null), null);
        assertEquals(FamilySecretReaction.PROTECT_SECRET, restored.familyReaction(key).orElseThrow());
        assertEquals(1, restored.pruneSocial(1000, 1.0F, 1));
        assertTrue(restored.familyReaction(key).isEmpty());
    }

    @Test
    void witnessIntimidationIsTemporaryAndPersists() {
        FolkloreSavedData original = new FolkloreSavedData();
        UUID witness = UUID.randomUUID();
        assertTrue(original.silenceRumors(witness, 500));
        assertTrue(original.rumorsSilenced(witness, 499));
        assertFalse(original.rumorsSilenced(witness, 500));

        FolkloreSavedData restored = FolkloreSavedData.load(original.save(new CompoundTag(), null), null);
        assertTrue(restored.rumorsSilenced(witness, 100));
        assertEquals(1, restored.pruneRumorSilence(500));
        assertFalse(restored.rumorsSilenced(witness, 100));
    }

    @Test
    void confirmedLeaderDeathSucceedsDeterministicallyAndPersistsOrganizationState() {
        FolkloreSavedData original = new FolkloreSavedData();
        UUID leader = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Organization organization = new Organization(UUID.randomUUID(), OrganizationType.VAMPIRE_COVEN,
                "Night Court", leader);
        organization.addMember(second);
        organization.addMember(first);
        organization.recordIntelligence(new OrganizationIntelKey(second, SecretType.HUNTER),
                SocialKnowledgeState.SUSPECTED);
        organization.markMemberSeen(first, 850);
        organization.setRelation(UUID.randomUUID(), OrganizationRelation.RIVAL);
        original.putOrganization(organization);

        FolkloreSavedData.DeathCleanupResult result = original.handleConfirmedDeath(leader, 900);
        assertEquals(1, result.membershipsRemoved());
        assertEquals(1, result.successions());
        assertEquals(first, organization.leader());
        assertTrue(original.organizationsForMember(leader).isEmpty());
        assertEquals(2, organization.events().size());

        FolkloreSavedData restored = FolkloreSavedData.load(original.save(new CompoundTag(), null), null);
        Organization persisted = restored.organization(organization.id()).orElseThrow();
        assertEquals(first, persisted.leader());
        assertEquals(SocialKnowledgeState.SUSPECTED,
                persisted.intelligence().get(new OrganizationIntelKey(second, SecretType.HUNTER)));
        assertEquals(2, persisted.events().size());
        assertEquals(850L, persisted.memberLastSeen().get(first));
        assertTrue(restored.organizationsForMember(first).contains(organization.id()));
    }

    @Test
    void malformedLegacyMemberUuidDoesNotDiscardTheWholeOrganization() {
        FolkloreSavedData original = new FolkloreSavedData();
        UUID valid = UUID.randomUUID();
        Organization organization = new Organization(UUID.randomUUID(), OrganizationType.HUNTER_SOCIETY,
                "Repairable Watch", valid);
        original.putOrganization(organization);
        CompoundTag serialized = original.save(new CompoundTag(), null);
        CompoundTag row = serialized.getList("organizations", Tag.TAG_COMPOUND).getCompound(0);
        row.putString("leader", "not-a-uuid");
        row.getList("members", Tag.TAG_STRING).add(net.minecraft.nbt.StringTag.valueOf("also-not-a-uuid"));

        FolkloreSavedData repaired = FolkloreSavedData.load(serialized, null);
        Organization loaded = repaired.organization(organization.id()).orElseThrow();
        assertEquals(valid, loaded.leader());
        assertEquals(1, loaded.members().size());
    }

    @Test
    void schemaOneOrganizationReceivesSafeDefaultsAndUpgradesIdempotently() {
        FolkloreSavedData legacy = new FolkloreSavedData();
        Organization organization = new Organization(UUID.randomUUID(), OrganizationType.WEREWOLF_PACK,
                "Old Pack", UUID.randomUUID());
        legacy.putOrganization(organization);
        CompoundTag schemaOne = legacy.save(new CompoundTag(), null);
        schemaOne.putInt("schema", 1);
        ListTag rows = schemaOne.getList("organizations", Tag.TAG_COMPOUND);
        CompoundTag row = rows.getCompound(0);
        row.remove("objectives");
        row.remove("intelligence");
        row.remove("relations");
        row.remove("events");
        schemaOne.remove("public_secrets");

        FolkloreSavedData firstUpgrade = FolkloreSavedData.load(schemaOne, null);
        Organization upgraded = firstUpgrade.organization(organization.id()).orElseThrow();
        assertTrue(upgraded.objectives().contains(OrganizationObjective.PROTECT_MEMBERS));
        CompoundTag schemaTwo = firstUpgrade.save(new CompoundTag(), null);
        assertEquals(2, schemaTwo.getInt("schema"));

        FolkloreSavedData secondLoad = FolkloreSavedData.load(schemaTwo, null);
        assertEquals(upgraded.objectives(), secondLoad.organization(organization.id()).orElseThrow().objectives());
    }

    @Test
    void rumorConfidenceDecaysAndEventuallyForgets() {
        FolkloreSavedData data = new FolkloreSavedData();
        SocialKnowledgeKey key = new SocialKnowledgeKey(UUID.randomUUID(), UUID.randomUUID(), SecretType.WEREWOLF);
        data.mergeSocial(key, new SocialKnowledgeRecord(SocialKnowledgeState.RUMOR, 0.8F,
                KnowledgeSource.RUMOR, 0, EvidenceType.TESTIMONY));

        assertEquals(1, data.decayRumors(100, 100, 0.08F));
        assertEquals(0.4F, data.social(key).orElseThrow().confidence(), 0.0001F);
        assertEquals(1, data.decayRumors(400, 100, 0.08F));
        assertTrue(data.social(key).isEmpty());
    }

    @Test
    void terminalNarrativeHistoryIsPrunedAfterRetentionWindow() {
        FolkloreSavedData data = new FolkloreSavedData();
        UUID player = UUID.randomUUID();
        UUID issuer = UUID.randomUUID();
        MonsterContract contract = new MonsterContract(UUID.randomUUID(), issuer, "darkfolklore:werewolf", 100);
        assertTrue(contract.start());
        assertTrue(contract.addEvidence(EvidenceType.TRACK, 1));
        assertTrue(contract.markHunted());
        assertTrue(contract.complete());
        data.putContract(new ContractAssignment(player, contract,
                new WorldPosition("minecraft:overworld", 0, 64, 0), "village", 1));

        StoryInstance story = new StoryInstance(UUID.randomUUID(), "incident", "darkfolklore:werewolf", 0, 100);
        assertTrue(story.advance(StoryStatus.INVESTIGATING));
        assertTrue(story.advance(StoryStatus.RESOLVED));
        data.putStory(new PersistentStory(story,
                new WorldPosition("minecraft:overworld", 0, 64, 0), "village"));

        assertEquals(0, data.pruneNarrativeHistory(150, 100));
        assertEquals(2, data.pruneNarrativeHistory(201, 100));
        assertTrue(data.contracts().isEmpty());
        assertTrue(data.stories().isEmpty());
    }

    @Test
    void versionedStateRoundTripsThroughNbt() {
        UUID player = UUID.randomUUID();
        UUID observer = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        FolkloreSavedData original = new FolkloreSavedData();
        original.addLore(player, "darkfolklore:vampire", 60);
        original.addReputation(player, ReputationFaction.HUNTERS, 15);
        original.mergeSocial(new SocialKnowledgeKey(observer, subject, SecretType.VAMPIRE),
                new SocialKnowledgeRecord(SocialKnowledgeState.CONFIRMED, 0.9F,
                        KnowledgeSource.DIRECT_WITNESS, 400, EvidenceType.DIRECT_WITNESS));
        assertTrue(original.addLineage(new LineageRecord(subject, source, SecretType.VAMPIRE, 500)));

        CompoundTag serialized = original.save(new CompoundTag(), null);
        assertEquals(FolkloreSavedData.SCHEMA_VERSION, serialized.getInt("schema"));
        FolkloreSavedData restored = FolkloreSavedData.load(serialized, null);

        assertEquals(60, restored.lore(player, "darkfolklore:vampire").points());
        assertEquals(15, restored.reputation(player).get(ReputationFaction.HUNTERS));
        assertEquals(SocialKnowledgeState.CONFIRMED,
                restored.social(new SocialKnowledgeKey(observer, subject, SecretType.VAMPIRE)).orElseThrow().state());
        assertEquals(source, restored.lineage(subject).orElseThrow().source());
    }
}
