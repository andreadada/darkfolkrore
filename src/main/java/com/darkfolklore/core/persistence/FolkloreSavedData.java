package com.darkfolklore.core.persistence;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.contracts.*;
import com.darkfolklore.core.investigation.EvidenceRecord;
import com.darkfolklore.core.knowledge.lore.LoreProgress;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.reputation.*;
import com.darkfolklore.core.society.bloodline.LineageRecord;
import com.darkfolklore.core.society.FamilySecretReaction;
import com.darkfolklore.core.society.organization.*;
import com.darkfolklore.core.society.rumor.RumorRules;
import com.darkfolklore.core.society.story.*;
import com.darkfolklore.core.society.village.VillageSocietyState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.function.Consumer;

/** Versioned, server-authoritative world state for facts owned by Dark Folklore. */
public final class FolkloreSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 2;
    private static final int HARD_MAX_PUBLIC_SECRETS = 50_000;
    private static final String FILE_ID = "darkfolklore_society";
    private static final Factory<FolkloreSavedData> FACTORY =
            new Factory<>(FolkloreSavedData::new, FolkloreSavedData::load);

    private final Map<UUID, Map<String, Integer>> lore = new HashMap<>();
    private final Map<SocialKnowledgeKey, SocialKnowledgeRecord> social = new HashMap<>();
    private final Map<UUID, LinkedHashSet<SocialKnowledgeKey>> socialBySubject = new HashMap<>();
    private final Map<UUID, LinkedHashSet<SocialKnowledgeKey>> socialByObserver = new HashMap<>();
    private final Map<SecretClaimKey, Long> publicSecrets = new LinkedHashMap<>();
    private final Map<SocialKnowledgeKey, FamilySecretReaction> familyReactions = new LinkedHashMap<>();
    private final Map<UUID, ReputationLedger> reputations = new HashMap<>();
    private final Map<UUID, Organization> organizations = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> organizationsByMember = new HashMap<>();
    private final Map<String, VillageSocietyState> villages = new HashMap<>();
    private final Map<UUID, LineageRecord> lineages = new HashMap<>();
    private final Map<UUID, EvidenceRecord> evidence = new LinkedHashMap<>();
    private final Map<UUID, ContractAssignment> contracts = new LinkedHashMap<>();
    private final Map<UUID, PersistentStory> stories = new LinkedHashMap<>();
    private final Map<UUID, Integer> encounterPressure = new HashMap<>();
    private final Map<UUID, Long> rumorSilencedUntil = new HashMap<>();
    private transient boolean publicCapWarningLogged;

    public static FolkloreSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public LoreProgress lore(UUID player, String concept) {
        return new LoreProgress(lore.getOrDefault(player, Map.of()).getOrDefault(concept, 0));
    }

    public LoreProgress addLore(UUID player, String concept, int amount) {
        LoreProgress progress = lore(player, concept).add(amount);
        lore.computeIfAbsent(player, ignored -> new HashMap<>()).put(concept, progress.points());
        setDirty();
        return progress;
    }

    public Optional<SocialKnowledgeRecord> social(SocialKnowledgeKey key) {
        SocialKnowledgeRecord direct = social.get(key);
        Long publicAt = publicSecrets.get(new SecretClaimKey(key.subject(), key.secret()));
        if (publicAt == null) return Optional.ofNullable(direct);
        SocialKnowledgeRecord publicRecord = new SocialKnowledgeRecord(SocialKnowledgeState.PUBLIC, 1.0F,
                KnowledgeSource.PUBLIC_REVEAL, publicAt, direct == null ? null : direct.evidence());
        return Optional.of(direct == null ? publicRecord : direct.merge(publicRecord));
    }

    public SocialKnowledgeRecord mergeSocial(SocialKnowledgeKey key, SocialKnowledgeRecord record) {
        Long publicAt = publicSecrets.get(new SecretClaimKey(key.subject(), key.secret()));
        if (publicAt != null) {
            record = record.merge(new SocialKnowledgeRecord(SocialKnowledgeState.PUBLIC, 1.0F,
                    KnowledgeSource.PUBLIC_REVEAL, publicAt, record.evidence()));
        }
        boolean existed = social.containsKey(key);
        SocialKnowledgeRecord merged = social.merge(key, record, SocialKnowledgeRecord::merge);
        if (!existed) indexSocial(key);
        setDirty();
        return merged;
    }

    public boolean isPublic(SecretClaimKey claim) {
        return publicSecrets.containsKey(claim);
    }

    public Map<SecretClaimKey, Long> publicSecrets() {
        return Map.copyOf(publicSecrets);
    }

    public Optional<FamilySecretReaction> familyReaction(SocialKnowledgeKey key) {
        return Optional.ofNullable(familyReactions.get(key));
    }

    public void setFamilyReaction(SocialKnowledgeKey key, FamilySecretReaction reaction) {
        if (familyReactions.put(Objects.requireNonNull(key), Objects.requireNonNull(reaction)) != reaction) setDirty();
    }

    /** Publishes a fact globally without manufacturing one observer row per resident. */
    public boolean markPublic(SecretClaimKey claim, long gameTime) {
        if (!publicSecrets.containsKey(claim) && publicSecrets.size() >= HARD_MAX_PUBLIC_SECRETS) {
            if (!publicCapWarningLogged) {
                publicCapWarningLogged = true;
                DarkFolkloreCore.LOGGER.warn("[persistence] Public-secret hard cap {} reached; rejecting new claims",
                        HARD_MAX_PUBLIC_SECRETS);
            }
            return false;
        }
        if (publicSecrets.putIfAbsent(Objects.requireNonNull(claim), Math.max(0L, gameTime)) != null) return false;
        social.replaceAll((key, value) -> key.subject().equals(claim.subject()) && key.secret() == claim.secret()
                ? value.merge(new SocialKnowledgeRecord(SocialKnowledgeState.PUBLIC, 1.0F,
                KnowledgeSource.PUBLIC_REVEAL, gameTime, value.evidence())) : value);
        setDirty();
        return true;
    }

    public List<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> knowledgeAbout(UUID subject) {
        return socialBySubject.getOrDefault(subject, new LinkedHashSet<>()).stream()
                .filter(social::containsKey).map(key -> Map.entry(key, social.get(key))).toList();
    }

    public List<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> knowledgeHeldBy(UUID observer) {
        return socialByObserver.getOrDefault(observer, new LinkedHashSet<>()).stream()
                .filter(social::containsKey).map(key -> Map.entry(key, social.get(key))).toList();
    }

    public int pruneSocial(long now, float minimumConfidence, long maximumAge) {
        int before = social.size();
        Iterator<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> iterator = social.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord> entry = iterator.next();
            if (!entry.getValue().shouldForget(minimumConfidence, now, maximumAge)) continue;
            iterator.remove();
            unindexSocial(entry.getKey());
        }
        int removed = before - social.size();
        if (removed > 0) {
            pruneOrphanFamilyReactions();
            setDirty();
        }
        return removed;
    }

    public int decayRumors(long now, long halfLifeTicks, float forgetBelow) {
        int changed = 0;
        Iterator<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> iterator = social.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord> entry = iterator.next();
            SocialKnowledgeRecord record = entry.getValue();
            if (record.state() != SocialKnowledgeState.RUMOR || now <= record.gameTime()) continue;
            float confidence = RumorRules.decay(record.confidence(), now - record.gameTime(), halfLifeTicks);
            if (confidence < forgetBelow) {
                iterator.remove();
                unindexSocial(entry.getKey());
            } else {
                entry.setValue(new SocialKnowledgeRecord(record.state(), confidence, record.source(),
                        now, record.evidence()));
            }
            changed++;
        }
        if (changed > 0) {
            pruneOrphanFamilyReactions();
            setDirty();
        }
        return changed;
    }

    public ReputationLedger reputation(UUID holder) {
        return reputations.computeIfAbsent(holder, ignored -> new ReputationLedger());
    }

    public int addReputation(UUID holder, ReputationFaction faction, int delta) {
        int result = reputation(holder).add(faction, delta);
        setDirty();
        return result;
    }

    public Collection<Organization> organizations() { return List.copyOf(organizations.values()); }
    public Optional<Organization> organization(UUID id) { return Optional.ofNullable(organizations.get(id)); }
    public Set<UUID> organizationsForMember(UUID member) {
        return Set.copyOf(organizationsByMember.getOrDefault(member, new LinkedHashSet<>()));
    }

    public void putOrganization(Organization organization) {
        Organization previous = organizations.put(organization.id(), organization);
        if (previous != null) unindex(previous);
        index(organization);
        setDirty();
    }

    public boolean tryPutOrganization(Organization organization, int maximumOrganizations) {
        if (!organizations.containsKey(organization.id())
                && organizations.size() >= Math.max(1, maximumOrganizations)) return false;
        putOrganization(organization);
        return true;
    }

    public boolean removeOrganization(UUID id) {
        Organization removed = organizations.remove(id);
        if (removed == null) return false;
        unindex(removed);
        organizations.values().forEach(value -> value.removeRelation(id));
        setDirty();
        return true;
    }

    /** Applies cleanup only after a real death event, never merely because an entity is unloaded. */
    public DeathCleanupResult handleConfirmedDeath(UUID entity, long gameTime) {
        int membershipsRemoved = 0;
        int organizationsDissolved = 0;
        int successions = 0;
        for (UUID organizationId : List.copyOf(organizationsForMember(entity))) {
            Organization organization = organizations.get(organizationId);
            if (organization == null) continue;
            if (organization.members().size() <= 1) {
                removeOrganization(organizationId);
                organizationsDissolved++;
                continue;
            }
            if (organization.leader().equals(entity)) {
                UUID successor = organization.members().stream().filter(member -> !member.equals(entity))
                        .min(Comparator.comparing(UUID::toString)).orElseThrow();
                organization.setLeader(successor);
                organization.removeMember(entity);
                organization.addEvent(OrganizationEvent.of(OrganizationEventType.MEMBER_DIED, gameTime,
                        null, entity, "confirmed death"));
                organization.addEvent(OrganizationEvent.of(OrganizationEventType.LEADER_SUCCEEDED, gameTime,
                        successor, entity, "leadership succession"));
                successions++;
            } else {
                organization.removeMember(entity);
                organization.addEvent(OrganizationEvent.of(OrganizationEventType.MEMBER_DIED, gameTime,
                        null, entity, "confirmed death"));
            }
            membershipsRemoved++;
            unindexMember(entity, organizationId);
            index(organization);
        }
        if (membershipsRemoved > 0 || organizationsDissolved > 0) setDirty();
        return new DeathCleanupResult(membershipsRemoved, organizationsDissolved, successions);
    }

    public int enforceSocialLimit(int maximumRecords) {
        int limit = Math.max(1, maximumRecords);
        if (social.size() <= limit) return 0;
        List<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> candidates = new ArrayList<>(social.entrySet());
        candidates.sort(Comparator
                .comparingInt((Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord> entry) -> entry.getValue().state().strength())
                .thenComparingDouble(entry -> entry.getValue().confidence())
                .thenComparingLong(entry -> entry.getValue().gameTime())
                .thenComparing(entry -> entry.getKey().observer().toString())
                .thenComparing(entry -> entry.getKey().subject().toString())
                .thenComparing(entry -> entry.getKey().secret().name()));
        int remove = social.size() - limit;
        for (int i = 0; i < remove; i++) removeSocial(candidates.get(i).getKey());
        pruneOrphanFamilyReactions();
        setDirty();
        return remove;
    }

    public int enforcePublicSecretLimit(int maximumClaims) {
        int limit = Math.max(1, Math.min(HARD_MAX_PUBLIC_SECRETS, maximumClaims));
        if (publicSecrets.size() <= limit) return 0;
        List<Map.Entry<SecretClaimKey, Long>> oldest = new ArrayList<>(publicSecrets.entrySet());
        oldest.sort(Map.Entry.<SecretClaimKey, Long>comparingByValue()
                .thenComparing(entry -> entry.getKey().subject().toString())
                .thenComparing(entry -> entry.getKey().secret().name()));
        int remove = publicSecrets.size() - limit;
        for (int i = 0; i < remove; i++) publicSecrets.remove(oldest.get(i).getKey());
        setDirty();
        return remove;
    }

    public record DeathCleanupResult(int membershipsRemoved, int organizationsDissolved, int successions) {}

    public VillageSocietyState village(String key) {
        VillageSocietyState state = villages.get(key);
        if (state == null) {
            state = new VillageSocietyState();
            villages.put(key, state);
            setDirty();
        }
        return state;
    }

    public Map<String, VillageSocietyState> villages() { return Map.copyOf(villages); }

    public boolean addLineage(LineageRecord lineage) {
        if (lineages.containsKey(lineage.descendant())) return false;
        lineages.put(lineage.descendant(), lineage);
        setDirty();
        return true;
    }

    public Optional<LineageRecord> lineage(UUID descendant) { return Optional.ofNullable(lineages.get(descendant)); }

    public void addEvidence(EvidenceRecord record) { evidence.put(record.id(), record); setDirty(); }
    public Collection<EvidenceRecord> evidence() { return List.copyOf(evidence.values()); }

    public boolean collectEvidence(UUID id, UUID player) {
        EvidenceRecord record = evidence.get(id);
        if (record == null || record.collectedBy().isPresent()) return false;
        evidence.put(id, record.collect(player));
        setDirty();
        return true;
    }

    public int pruneEvidence(long now) {
        int before = evidence.size();
        evidence.values().removeIf(record -> record.expired(now));
        int removed = before - evidence.size();
        if (removed > 0) setDirty();
        return removed;
    }

    public void putContract(ContractAssignment assignment) { contracts.put(assignment.contract().id(), assignment); setDirty(); }
    public Collection<ContractAssignment> contracts() { return List.copyOf(contracts.values()); }
    public Optional<ContractAssignment> contract(UUID id) { return Optional.ofNullable(contracts.get(id)); }
    public Optional<ContractAssignment> activeContract(UUID player) {
        return contracts.values().stream().filter(value -> value.player().equals(player)
                && !value.contract().status().terminal()).findFirst();
    }

    public void putStory(PersistentStory story) { stories.put(story.story().id(), story); setDirty(); }
    public Collection<PersistentStory> stories() { return List.copyOf(stories.values()); }
    public Optional<PersistentStory> story(UUID id) { return Optional.ofNullable(stories.get(id)); }

    public int pruneNarrativeHistory(long now, long retentionTicks) {
        int contractsBefore = contracts.size();
        contracts.entrySet().removeIf(entry -> entry.getValue().contract().status().terminal()
                && now - entry.getValue().contract().expiresAt() > retentionTicks);
        int storiesBefore = stories.size();
        stories.entrySet().removeIf(entry -> entry.getValue().story().status().terminal()
                && now - entry.getValue().story().expiresAt() > retentionTicks);
        int removed = contractsBefore - contracts.size() + storiesBefore - stories.size();
        if (removed > 0) setDirty();
        return removed;
    }

    public int encounterPressure(UUID player) { return encounterPressure.getOrDefault(player, 0); }
    public void setEncounterPressure(UUID player, int pressure) {
        int normalized = Math.max(0, Math.min(100, pressure));
        if (normalized == 0) encounterPressure.remove(player);
        else encounterPressure.put(player, normalized);
        setDirty();
    }

    public boolean silenceRumors(UUID witness, long untilGameTime) {
        if (!rumorSilencedUntil.containsKey(witness) && rumorSilencedUntil.size() >= HARD_MAX_PUBLIC_SECRETS) {
            return false;
        }
        long until = Math.max(0L, untilGameTime);
        Long previous = rumorSilencedUntil.get(witness);
        if (previous != null && previous >= until) return false;
        rumorSilencedUntil.put(witness, until);
        setDirty();
        return true;
    }

    public boolean rumorsSilenced(UUID witness, long gameTime) {
        return rumorSilencedUntil.getOrDefault(witness, 0L) > gameTime;
    }

    public int pruneRumorSilence(long gameTime) {
        int before = rumorSilencedUntil.size();
        rumorSilencedUntil.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
        int removed = before - rumorSilencedUntil.size();
        if (removed > 0) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", SCHEMA_VERSION);
        tag.put("lore", saveLore());
        tag.put("social", saveSocial());
        tag.put("public_secrets", savePublicSecrets());
        tag.put("family_reactions", saveFamilyReactions());
        tag.put("reputation", saveReputation());
        tag.put("organizations", saveOrganizations());
        tag.put("villages", saveVillages());
        tag.put("lineages", saveLineages());
        tag.put("evidence", saveEvidence());
        tag.put("contracts", saveContracts());
        tag.put("stories", saveStories());
        tag.put("encounters", saveEncounters());
        tag.put("rumor_silence", saveRumorSilence());
        return tag;
    }

    static FolkloreSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FolkloreSavedData data = new FolkloreSavedData();
        int schema = tag.getInt("schema");
        if (schema > SCHEMA_VERSION) {
            DarkFolkloreCore.LOGGER.warn("[persistence] Save schema {} is newer than supported {}; attempting safe read",
                    schema, SCHEMA_VERSION);
        }
        data.readList(tag, "lore", data::readLore);
        data.readList(tag, "social", data::readSocial);
        data.readList(tag, "public_secrets", data::readPublicSecret);
        data.readList(tag, "family_reactions", data::readFamilyReaction);
        data.readList(tag, "reputation", data::readReputation);
        data.readList(tag, "organizations", data::readOrganization);
        data.readList(tag, "villages", data::readVillage);
        data.readList(tag, "lineages", data::readLineage);
        data.readList(tag, "evidence", data::readEvidence);
        data.readList(tag, "contracts", data::readContract);
        data.readList(tag, "stories", data::readStory);
        data.readList(tag, "encounters", data::readEncounter);
        data.readList(tag, "rumor_silence", data::readRumorSilence);
        data.rebuildOrganizationIndex();
        data.enforcePublicSecretLimit(HARD_MAX_PUBLIC_SECRETS);
        if (schema > 0 && schema < SCHEMA_VERSION) {
            DarkFolkloreCore.LOGGER.info("[persistence] Migrated Dark Folklore schema {} -> {}; "
                    + "legacy records were retained and new fields received safe defaults", schema, SCHEMA_VERSION);
            data.setDirty();
        }
        return data;
    }

    private void readList(CompoundTag root, String key, Consumer<CompoundTag> reader) {
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try { reader.accept(list.getCompound(i)); }
            catch (RuntimeException exception) {
                DarkFolkloreCore.LOGGER.error("[persistence] Ignoring malformed {} entry {}", key, i, exception);
            }
        }
    }

    private ListTag saveLore() {
        ListTag list = new ListTag();
        lore.forEach((player, concepts) -> concepts.forEach((concept, points) -> {
            CompoundTag row = new CompoundTag(); row.putString("player", player.toString());
            row.putString("concept", concept); row.putInt("points", points); list.add(row);
        }));
        return list;
    }

    private void readLore(CompoundTag row) {
        UUID player = uuid(row, "player"); String concept = row.getString("concept");
        lore.computeIfAbsent(player, ignored -> new HashMap<>()).put(concept, row.getInt("points"));
    }

    private ListTag saveSocial() {
        ListTag list = new ListTag();
        social.forEach((key, value) -> {
            CompoundTag row = new CompoundTag(); row.putString("observer", key.observer().toString());
            row.putString("subject", key.subject().toString()); row.putString("secret", key.secret().name());
            row.putString("state", value.state().name()); row.putFloat("confidence", value.confidence());
            row.putString("source", value.source().name()); row.putLong("time", value.gameTime());
            if (value.evidence() != null) row.putString("evidence", value.evidence().name()); list.add(row);
        });
        return list;
    }

    private void readSocial(CompoundTag row) {
        SocialKnowledgeKey key = new SocialKnowledgeKey(uuid(row, "observer"), uuid(row, "subject"),
                SecretType.valueOf(row.getString("secret")));
        EvidenceType evidence = row.contains("evidence") ? EvidenceType.valueOf(row.getString("evidence")) : null;
        social.put(key, new SocialKnowledgeRecord(SocialKnowledgeState.valueOf(row.getString("state")),
                row.getFloat("confidence"), KnowledgeSource.valueOf(row.getString("source")),
                row.getLong("time"), evidence));
        indexSocial(key);
    }

    private ListTag savePublicSecrets() {
        ListTag list = new ListTag();
        publicSecrets.forEach((claim, time) -> {
            CompoundTag row = new CompoundTag();
            row.putString("subject", claim.subject().toString());
            row.putString("secret", claim.secret().name());
            row.putLong("time", time);
            list.add(row);
        });
        return list;
    }

    private void readPublicSecret(CompoundTag row) {
        publicSecrets.put(new SecretClaimKey(uuid(row, "subject"), SecretType.valueOf(row.getString("secret"))),
                Math.max(0L, row.getLong("time")));
    }

    private ListTag saveFamilyReactions() {
        ListTag list = new ListTag();
        familyReactions.forEach((key, reaction) -> {
            CompoundTag row = new CompoundTag();
            row.putString("observer", key.observer().toString());
            row.putString("subject", key.subject().toString());
            row.putString("secret", key.secret().name());
            row.putString("reaction", reaction.name());
            list.add(row);
        });
        return list;
    }

    private void readFamilyReaction(CompoundTag row) {
        SocialKnowledgeKey key = new SocialKnowledgeKey(uuid(row, "observer"), uuid(row, "subject"),
                SecretType.valueOf(row.getString("secret")));
        familyReactions.put(key, FamilySecretReaction.valueOf(row.getString("reaction")));
    }

    private ListTag saveReputation() {
        ListTag list = new ListTag();
        reputations.forEach((holder, ledger) -> ledger.snapshot().forEach((faction, value) -> {
            CompoundTag row = new CompoundTag(); row.putString("holder", holder.toString());
            row.putString("faction", faction.name()); row.putInt("value", value); list.add(row);
        }));
        return list;
    }

    private void readReputation(CompoundTag row) {
        reputations.computeIfAbsent(uuid(row, "holder"), ignored -> new ReputationLedger())
                .add(ReputationFaction.valueOf(row.getString("faction")), row.getInt("value"));
    }

    private ListTag saveOrganizations() {
        ListTag list = new ListTag();
        organizations.values().forEach(value -> {
            CompoundTag row = new CompoundTag(); row.putString("id", value.id().toString());
            row.putString("type", value.type().name()); row.putString("name", value.name());
            row.putString("leader", value.leader().toString()); row.putString("home", value.home());
            row.putInt("influence", value.influence()); ListTag members = new ListTag();
            value.members().forEach(member -> members.add(StringTag.valueOf(member.toString())));
            row.put("members", members);
            ListTag seen = new ListTag();
            value.memberLastSeen().forEach((member, time) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("member", member.toString());
                entry.putLong("time", time);
                seen.add(entry);
            });
            row.put("member_seen", seen);

            ListTag objectives = new ListTag();
            value.objectives().forEach(objective -> objectives.add(StringTag.valueOf(objective.name())));
            row.put("objectives", objectives);

            ListTag intelligence = new ListTag();
            value.intelligence().forEach((key, state) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("subject", key.subject().toString());
                entry.putString("secret", key.secret().name());
                entry.putString("state", state.name());
                intelligence.add(entry);
            });
            row.put("intelligence", intelligence);

            ListTag relations = new ListTag();
            value.relations().forEach((other, relation) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("organization", other.toString());
                entry.putString("relation", relation.name());
                relations.add(entry);
            });
            row.put("relations", relations);

            ListTag events = new ListTag();
            value.events().forEach(event -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("type", event.type().name());
                entry.putLong("time", event.gameTime());
                event.actor().ifPresent(actor -> entry.putString("actor", actor.toString()));
                event.subject().ifPresent(subject -> entry.putString("subject", subject.toString()));
                entry.putString("detail", event.detail());
                events.add(entry);
            });
            row.put("events", events);
            list.add(row);
        });
        return list;
    }

    private void readOrganization(CompoundTag row) {
        List<UUID> validMembers = new ArrayList<>();
        int invalidMembers = 0;
        ListTag members = row.getList("members", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(members.size(), Organization.MAX_MEMBERS_HARD); i++) {
            try { validMembers.add(UUID.fromString(members.getString(i))); }
            catch (IllegalArgumentException exception) { invalidMembers++; }
        }
        UUID leader;
        try { leader = uuid(row, "leader"); }
        catch (IllegalArgumentException exception) {
            leader = validMembers.stream().min(Comparator.comparing(UUID::toString))
                    .orElseThrow(() -> new IllegalArgumentException("organization has no valid leader or members"));
            invalidMembers++;
        }
        Organization value = new Organization(uuid(row, "id"), OrganizationType.valueOf(row.getString("type")),
                row.getString("name"), leader);
        validMembers.forEach(value::addMember);
        if (invalidMembers > 0) {
            DarkFolkloreCore.LOGGER.warn("[persistence] Repaired organization {} by skipping {} invalid member UUID(s)",
                    value.id(), invalidMembers);
        }
        value.setHome(row.getString("home")); value.setInfluence(row.getInt("influence"));

        Map<UUID, Long> lastSeen = new LinkedHashMap<>();
        ListTag seenRows = row.getList("member_seen", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(seenRows.size(), Organization.MAX_MEMBERS_HARD); i++) {
            try {
                CompoundTag entry = seenRows.getCompound(i);
                lastSeen.put(uuid(entry, "member"), Math.max(0L, entry.getLong("time")));
            } catch (RuntimeException ignored) { }
        }
        value.restoreMemberLastSeen(lastSeen);

        if (row.contains("objectives", Tag.TAG_LIST)) {
            EnumSet<OrganizationObjective> objectives = EnumSet.noneOf(OrganizationObjective.class);
            ListTag list = row.getList("objectives", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) objectives.add(OrganizationObjective.valueOf(list.getString(i)));
            value.restoreObjectives(objectives);
        }

        Map<OrganizationIntelKey, SocialKnowledgeState> intelligence = new LinkedHashMap<>();
        ListTag intelligenceRows = row.getList("intelligence", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(intelligenceRows.size(), Organization.MAX_INTELLIGENCE); i++) {
            try {
                CompoundTag entry = intelligenceRows.getCompound(i);
                intelligence.put(new OrganizationIntelKey(uuid(entry, "subject"),
                                SecretType.valueOf(entry.getString("secret"))),
                        SocialKnowledgeState.valueOf(entry.getString("state")));
            } catch (RuntimeException ignored) { }
        }
        value.restoreIntelligence(intelligence);

        Map<UUID, OrganizationRelation> relations = new LinkedHashMap<>();
        ListTag relationRows = row.getList("relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(relationRows.size(), Organization.MAX_RELATIONS); i++) {
            try {
                CompoundTag entry = relationRows.getCompound(i);
                relations.put(uuid(entry, "organization"), OrganizationRelation.valueOf(entry.getString("relation")));
            } catch (RuntimeException ignored) { }
        }
        value.restoreRelations(relations);

        List<OrganizationEvent> events = new ArrayList<>();
        ListTag eventRows = row.getList("events", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, eventRows.size() - Organization.MAX_EVENT_HISTORY); i < eventRows.size(); i++) {
            try {
                CompoundTag entry = eventRows.getCompound(i);
                events.add(new OrganizationEvent(OrganizationEventType.valueOf(entry.getString("type")),
                        entry.getLong("time"), optionalUuid(entry, "actor"), optionalUuid(entry, "subject"),
                        entry.getString("detail")));
            } catch (RuntimeException ignored) { }
        }
        value.restoreEvents(events);
        organizations.put(value.id(), value);
    }

    private ListTag saveVillages() {
        ListTag list = new ListTag();
        villages.forEach((key, value) -> {
            CompoundTag row = new CompoundTag(); row.putString("key", key);
            row.putInt("awareness", value.publicAwareness()); row.putInt("vampire", value.vampireInfluence());
            row.putInt("hunter", value.hunterInfluence()); row.putInt("werewolf", value.werewolfInfluence());
            row.putInt("witch", value.witchInfluence()); row.putInt("fear", value.fear());
            row.putInt("suspicion", value.suspicion()); row.putInt("political", value.politicalImportance());
            list.add(row);
        });
        return list;
    }

    private void readVillage(CompoundTag row) {
        VillageSocietyState value = new VillageSocietyState();
        value.setValues(row.getInt("awareness"), row.getInt("vampire"), row.getInt("hunter"),
                row.getInt("werewolf"), row.getInt("witch"), row.getInt("fear"), row.getInt("suspicion"));
        value.setPoliticalImportance(row.getInt("political"));
        villages.put(row.getString("key"), value);
    }

    private ListTag saveLineages() {
        ListTag list = new ListTag(); lineages.values().forEach(value -> {
            CompoundTag row = new CompoundTag(); row.putString("descendant", value.descendant().toString());
            row.putString("source", value.source().toString()); row.putString("type", value.type().name());
            row.putLong("time", value.recordedAt()); list.add(row);
        }); return list;
    }

    private void readLineage(CompoundTag row) {
        LineageRecord value = new LineageRecord(uuid(row, "descendant"), uuid(row, "source"),
                SecretType.valueOf(row.getString("type")), row.getLong("time")); lineages.put(value.descendant(), value);
    }

    private ListTag saveEvidence() {
        ListTag list = new ListTag(); evidence.values().forEach(value -> {
            CompoundTag row = new CompoundTag(); row.putString("id", value.id().toString());
            row.putString("type", value.type().name()); row.putString("concept", value.concept());
            value.subject().ifPresent(subject -> row.putString("subject", subject.toString()));
            putPosition(row, value.position()); row.putLong("created", value.createdAt());
            row.putLong("expires", value.expiresAt()); value.collectedBy().ifPresent(player -> row.putString("collected", player.toString()));
            list.add(row);
        }); return list;
    }

    private void readEvidence(CompoundTag row) {
        EvidenceRecord value = new EvidenceRecord(uuid(row, "id"), EvidenceType.valueOf(row.getString("type")),
                row.getString("concept"), optionalUuid(row, "subject"), getPosition(row), row.getLong("created"),
                row.getLong("expires"), optionalUuid(row, "collected")); evidence.put(value.id(), value);
    }

    private ListTag saveContracts() {
        ListTag list = new ListTag(); contracts.values().forEach(value -> {
            MonsterContract contract = value.contract(); CompoundTag row = new CompoundTag();
            row.putString("id", contract.id().toString()); row.putString("player", value.player().toString());
            row.putString("issuer", contract.issuer().toString()); row.putString("concept", contract.targetConcept());
            row.putLong("expires", contract.expiresAt()); row.putString("status", contract.status().name());
            row.putString("village", value.villageKey()); row.putInt("required", value.requiredDistinctClues());
            putPosition(row, value.investigationCenter()); ListTag clues = new ListTag();
            contract.evidence().forEach(clue -> clues.add(StringTag.valueOf(clue.name()))); row.put("clues", clues); list.add(row);
        }); return list;
    }

    private void readContract(CompoundTag row) {
        MonsterContract contract = new MonsterContract(uuid(row, "id"), uuid(row, "issuer"),
                row.getString("concept"), row.getLong("expires")); EnumSet<EvidenceType> clues = EnumSet.noneOf(EvidenceType.class);
        ListTag list = row.getList("clues", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) clues.add(EvidenceType.valueOf(list.getString(i)));
        contract.restore(ContractStatus.valueOf(row.getString("status")), clues);
        ContractAssignment value = new ContractAssignment(uuid(row, "player"), contract, getPosition(row),
                row.getString("village"), row.getInt("required")); contracts.put(contract.id(), value);
    }

    private ListTag saveStories() {
        ListTag list = new ListTag(); stories.values().forEach(value -> {
            StoryInstance story = value.story(); CompoundTag row = new CompoundTag();
            row.putString("id", story.id().toString()); row.putString("template", story.template());
            row.putString("concept", story.concept()); row.putLong("created", story.createdAt());
            row.putLong("expires", story.expiresAt()); row.putString("status", story.status().name());
            row.putString("village", value.villageKey()); putPosition(row, value.location()); ListTag actors = new ListTag();
            story.actors().forEach(actor -> actors.add(StringTag.valueOf(actor.toString()))); row.put("actors", actors); list.add(row);
        }); return list;
    }

    private void readStory(CompoundTag row) {
        StoryInstance story = new StoryInstance(uuid(row, "id"), row.getString("template"), row.getString("concept"),
                row.getLong("created"), row.getLong("expires")); Set<UUID> actors = new LinkedHashSet<>();
        ListTag list = row.getList("actors", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) actors.add(UUID.fromString(list.getString(i)));
        story.restore(StoryStatus.valueOf(row.getString("status")), actors);
        stories.put(story.id(), new PersistentStory(story, getPosition(row), row.getString("village")));
    }

    private ListTag saveEncounters() {
        ListTag list = new ListTag(); encounterPressure.forEach((player, pressure) -> {
            CompoundTag row = new CompoundTag(); row.putString("player", player.toString()); row.putInt("pressure", pressure); list.add(row);
        }); return list;
    }

    private void readEncounter(CompoundTag row) { encounterPressure.put(uuid(row, "player"), row.getInt("pressure")); }

    private ListTag saveRumorSilence() {
        ListTag list = new ListTag();
        rumorSilencedUntil.forEach((witness, until) -> {
            CompoundTag row = new CompoundTag();
            row.putString("witness", witness.toString());
            row.putLong("until", until);
            list.add(row);
        });
        return list;
    }

    private void readRumorSilence(CompoundTag row) {
        if (rumorSilencedUntil.size() < HARD_MAX_PUBLIC_SECRETS) {
            rumorSilencedUntil.put(uuid(row, "witness"), Math.max(0L, row.getLong("until")));
        }
    }

    private void rebuildOrganizationIndex() {
        organizationsByMember.clear();
        organizations.values().forEach(this::index);
    }

    private void index(Organization organization) {
        organization.members().forEach(member -> organizationsByMember
                .computeIfAbsent(member, ignored -> new LinkedHashSet<>()).add(organization.id()));
    }

    private void unindex(Organization organization) {
        organization.members().forEach(member -> unindexMember(member, organization.id()));
    }

    private void unindexMember(UUID member, UUID organization) {
        LinkedHashSet<UUID> memberships = organizationsByMember.get(member);
        if (memberships == null) return;
        memberships.remove(organization);
        if (memberships.isEmpty()) organizationsByMember.remove(member);
    }

    private void pruneOrphanFamilyReactions() {
        familyReactions.keySet().retainAll(social.keySet());
    }

    private void indexSocial(SocialKnowledgeKey key) {
        socialBySubject.computeIfAbsent(key.subject(), ignored -> new LinkedHashSet<>()).add(key);
        socialByObserver.computeIfAbsent(key.observer(), ignored -> new LinkedHashSet<>()).add(key);
    }

    private void unindexSocial(SocialKnowledgeKey key) {
        removeIndexKey(socialBySubject, key.subject(), key);
        removeIndexKey(socialByObserver, key.observer(), key);
    }

    private void removeSocial(SocialKnowledgeKey key) {
        if (social.remove(key) != null) unindexSocial(key);
    }

    private static void removeIndexKey(Map<UUID, LinkedHashSet<SocialKnowledgeKey>> index,
                                       UUID owner, SocialKnowledgeKey key) {
        LinkedHashSet<SocialKnowledgeKey> values = index.get(owner);
        if (values == null) return;
        values.remove(key);
        if (values.isEmpty()) index.remove(owner);
    }

    private static void putPosition(CompoundTag row, WorldPosition value) {
        row.putString("dimension", value.dimension()); row.putInt("x", value.x()); row.putInt("y", value.y()); row.putInt("z", value.z());
    }

    private static WorldPosition getPosition(CompoundTag row) {
        return new WorldPosition(row.getString("dimension"), row.getInt("x"), row.getInt("y"), row.getInt("z"));
    }

    private static UUID uuid(CompoundTag row, String key) { return UUID.fromString(row.getString(key)); }
    private static Optional<UUID> optionalUuid(CompoundTag row, String key) {
        return row.contains(key) ? Optional.of(uuid(row, key)) : Optional.empty();
    }
}
