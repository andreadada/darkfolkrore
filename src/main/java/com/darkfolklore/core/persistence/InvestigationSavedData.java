package com.darkfolklore.core.persistence;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.contracts.ContractAssignment;
import com.darkfolklore.core.investigation.IncidentFact;
import com.darkfolklore.core.investigation.InvestigationCaseLink;
import com.darkfolklore.core.knowledge.observation.CreatureSightingKey;
import com.darkfolklore.core.knowledge.observation.CreatureSightingRecord;
import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.knowledge.social.KnowledgeSource;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * 0.3.1 investigation-specific persistence.
 *
 * Society data deliberately remains schema 2. New concept-level sightings and
 * case-continuity metadata live in this fail-closed sidecar so existing worlds
 * need no destructive rewrite of darkfolklore_society.
 */
public final class InvestigationSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;
    private static final String FILE_ID = "darkfolklore_investigation";
    private static final int HARD_MAX_SIGHTINGS = 50_000;
    private static final int HARD_MAX_INCIDENT_FACTS = 20_000;
    private static final int HARD_MAX_CASE_LINKS = 20_000;
    private static final Factory<InvestigationSavedData> FACTORY =
            new Factory<>(InvestigationSavedData::new, InvestigationSavedData::load);

    private final Map<CreatureSightingKey, CreatureSightingRecord> sightings = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<CreatureSightingKey>> sightingsByObserver = new HashMap<>();
    private final Map<UUID, IncidentFact> incidentFacts = new LinkedHashMap<>();
    private final Map<UUID, InvestigationCaseLink> caseLinks = new LinkedHashMap<>();

    public static InvestigationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public CreatureSightingRecord mergeSighting(CreatureSightingKey key, CreatureSightingRecord record) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(record, "record");
        boolean existed = sightings.containsKey(key);
        CreatureSightingRecord merged = sightings.merge(key, record, CreatureSightingRecord::merge);
        if (!existed) {
            sightingsByObserver.computeIfAbsent(key.observer(), ignored -> new LinkedHashSet<>()).add(key);
            enforceSightingHardCap();
        }
        setDirty();
        return merged;
    }

    public Optional<CreatureSightingRecord> sighting(UUID observer, String concept) {
        return Optional.ofNullable(sightings.get(new CreatureSightingKey(observer, concept)));
    }

    public List<Map.Entry<CreatureSightingKey, CreatureSightingRecord>> sightingsHeldBy(UUID observer) {
        return sightingsByObserver.getOrDefault(observer, new LinkedHashSet<>()).stream()
                .filter(sightings::containsKey)
                .map(key -> Map.entry(key, sightings.get(key)))
                .toList();
    }

    public int pruneSightings(long now, float minimumConfidence, long maximumAge) {
        int before = sightings.size();
        Iterator<Map.Entry<CreatureSightingKey, CreatureSightingRecord>> iterator = sightings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<CreatureSightingKey, CreatureSightingRecord> entry = iterator.next();
            if (!entry.getValue().shouldForget(minimumConfidence, now, maximumAge)) continue;
            iterator.remove();
            unindexSighting(entry.getKey());
        }
        int removed = before - sightings.size();
        if (removed > 0) setDirty();
        return removed;
    }

    public void putIncidentFact(UUID storyId, IncidentFact fact) {
        Objects.requireNonNull(storyId, "storyId");
        Objects.requireNonNull(fact, "fact");
        incidentFacts.put(storyId, fact);
        enforceOldestIncidentCap();
        setDirty();
    }

    public Optional<IncidentFact> incidentFact(UUID storyId) {
        return Optional.ofNullable(incidentFacts.get(storyId));
    }

    public boolean putCaseLink(UUID contractId, InvestigationCaseLink link) {
        Objects.requireNonNull(contractId, "contractId");
        Objects.requireNonNull(link, "link");
        if (!caseLinks.containsKey(contractId) && caseLinks.size() >= HARD_MAX_CASE_LINKS) return false;
        caseLinks.put(contractId, link);
        setDirty();
        return true;
    }

    public Optional<InvestigationCaseLink> caseLink(UUID contractId) {
        return Optional.ofNullable(caseLinks.get(contractId));
    }

    /**
     * Promotes one story from concept-only continuity to an exact manifested culprit. Contracts accepted before
     * manifestation are updated in-place so another natural mob of the same canonical concept cannot satisfy them.
     */
    public int bindCulpritForStory(UUID storyId, UUID culprit, String implementation, long observedAt) {
        Objects.requireNonNull(storyId, "storyId");
        Objects.requireNonNull(culprit, "culprit");
        String observedImplementation = Objects.requireNonNullElse(implementation, "");
        putIncidentFact(storyId, new IncidentFact(Optional.of(culprit), observedImplementation, Math.max(0L, observedAt)));
        int updated = 0;
        for (Map.Entry<UUID, InvestigationCaseLink> entry : caseLinks.entrySet()) {
            InvestigationCaseLink current = entry.getValue();
            if (current.storyId().filter(storyId::equals).isEmpty()) continue;
            entry.setValue(current.bindCulprit(culprit, observedImplementation));
            updated++;
        }
        if (updated > 0) setDirty();
        return updated;
    }

    public boolean allowCulpritFallback(UUID contractId) {
        InvestigationCaseLink current = caseLinks.get(contractId);
        if (current == null || current.culpritFallbackAllowed()) return false;
        caseLinks.put(contractId, current.allowCulpritFallback());
        setDirty();
        return true;
    }

    public boolean allowIssuerFallback(UUID contractId) {
        InvestigationCaseLink current = caseLinks.get(contractId);
        if (current == null || current.issuerFallbackAllowed()) return false;
        caseLinks.put(contractId, current.allowIssuerFallback());
        setDirty();
        return true;
    }

    /** Remove metadata only after the owning society rows have themselves been pruned. */
    public int pruneOrphans(Collection<ContractAssignment> contracts, Set<UUID> liveStoryIds) {
        Set<UUID> contractIds = new HashSet<>();
        for (ContractAssignment assignment : contracts) contractIds.add(assignment.contract().id());
        int before = caseLinks.size() + incidentFacts.size();
        caseLinks.keySet().retainAll(contractIds);
        incidentFacts.keySet().retainAll(liveStoryIds);
        int removed = before - caseLinks.size() - incidentFacts.size();
        if (removed > 0) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", SCHEMA_VERSION);
        tag.put("sightings", saveSightings());
        tag.put("incidents", saveIncidentFacts());
        tag.put("cases", saveCaseLinks());
        return tag;
    }

    static InvestigationSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        InvestigationSavedData data = new InvestigationSavedData();
        int schema = tag.getInt("schema");
        if (schema > SCHEMA_VERSION) {
            DarkFolkloreCore.LOGGER.warn("[investigation/persistence] Save schema {} is newer than supported {}; attempting safe read",
                    schema, SCHEMA_VERSION);
        }
        data.readSightings(tag.getList("sightings", Tag.TAG_COMPOUND));
        data.readIncidentFacts(tag.getList("incidents", Tag.TAG_COMPOUND));
        data.readCaseLinks(tag.getList("cases", Tag.TAG_COMPOUND));
        if (schema > 0 && schema < SCHEMA_VERSION) data.setDirty();
        return data;
    }

    private ListTag saveSightings() {
        ListTag list = new ListTag();
        sightings.forEach((key, value) -> {
            CompoundTag row = new CompoundTag();
            row.putString("observer", key.observer().toString());
            row.putString("concept", key.concept());
            row.putString("state", value.state().name());
            row.putFloat("confidence", value.confidence());
            row.putString("source", value.source().name());
            row.putLong("time", value.gameTime());
            value.entityId().ifPresent(id -> row.putString("entity", id.toString()));
            value.location().ifPresent(position -> putPosition(row, position));
            if (value.evidence() != null) row.putString("evidence", value.evidence().name());
            list.add(row);
        });
        return list;
    }

    private void readSightings(ListTag list) {
        for (int i = 0; i < list.size() && i < HARD_MAX_SIGHTINGS; i++) {
            try {
                CompoundTag row = list.getCompound(i);
                CreatureSightingKey key = new CreatureSightingKey(uuid(row, "observer"), row.getString("concept"));
                CreatureSightingRecord record = new CreatureSightingRecord(
                        SocialKnowledgeState.valueOf(row.getString("state")), row.getFloat("confidence"),
                        KnowledgeSource.valueOf(row.getString("source")), row.getLong("time"),
                        optionalUuid(row, "entity"), row.contains("dimension")
                        ? Optional.of(getPosition(row)) : Optional.empty(),
                        row.contains("evidence") ? EvidenceType.valueOf(row.getString("evidence")) : null);
                sightings.put(key, record);
                sightingsByObserver.computeIfAbsent(key.observer(), ignored -> new LinkedHashSet<>()).add(key);
            } catch (RuntimeException exception) {
                DarkFolkloreCore.LOGGER.error("[investigation/persistence] Ignoring malformed sighting row {}", i, exception);
            }
        }
    }

    private ListTag saveIncidentFacts() {
        ListTag list = new ListTag();
        incidentFacts.forEach((storyId, fact) -> {
            CompoundTag row = new CompoundTag();
            row.putString("story", storyId.toString());
            fact.culpritId().ifPresent(id -> row.putString("culprit", id.toString()));
            row.putString("implementation", fact.observedImplementation());
            row.putLong("created", fact.createdAt());
            list.add(row);
        });
        return list;
    }

    private void readIncidentFacts(ListTag list) {
        for (int i = 0; i < list.size() && i < HARD_MAX_INCIDENT_FACTS; i++) {
            try {
                CompoundTag row = list.getCompound(i);
                incidentFacts.put(uuid(row, "story"), new IncidentFact(optionalUuid(row, "culprit"),
                        row.getString("implementation"), row.getLong("created")));
            } catch (RuntimeException exception) {
                DarkFolkloreCore.LOGGER.error("[investigation/persistence] Ignoring malformed incident row {}", i, exception);
            }
        }
    }

    private ListTag saveCaseLinks() {
        ListTag list = new ListTag();
        caseLinks.forEach((contractId, link) -> {
            CompoundTag row = new CompoundTag();
            row.putString("contract", contractId.toString());
            link.storyId().ifPresent(id -> row.putString("story", id.toString()));
            link.culpritId().ifPresent(id -> row.putString("culprit", id.toString()));
            row.putString("implementation", link.observedImplementation());
            row.putBoolean("culprit_fallback", link.culpritFallbackAllowed());
            row.putBoolean("issuer_fallback", link.issuerFallbackAllowed());
            list.add(row);
        });
        return list;
    }

    private void readCaseLinks(ListTag list) {
        for (int i = 0; i < list.size() && i < HARD_MAX_CASE_LINKS; i++) {
            try {
                CompoundTag row = list.getCompound(i);
                caseLinks.put(uuid(row, "contract"), new InvestigationCaseLink(optionalUuid(row, "story"),
                        optionalUuid(row, "culprit"), row.getString("implementation"),
                        row.getBoolean("culprit_fallback"), row.getBoolean("issuer_fallback")));
            } catch (RuntimeException exception) {
                DarkFolkloreCore.LOGGER.error("[investigation/persistence] Ignoring malformed case row {}", i, exception);
            }
        }
    }

    private void enforceSightingHardCap() {
        while (sightings.size() > HARD_MAX_SIGHTINGS) {
            CreatureSightingKey oldest = sightings.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().gameTime()))
                    .map(Map.Entry::getKey).orElse(null);
            if (oldest == null) return;
            sightings.remove(oldest);
            unindexSighting(oldest);
        }
    }

    private void enforceOldestIncidentCap() {
        while (incidentFacts.size() > HARD_MAX_INCIDENT_FACTS) {
            UUID oldest = incidentFacts.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().createdAt()))
                    .map(Map.Entry::getKey).orElse(null);
            if (oldest == null) return;
            incidentFacts.remove(oldest);
        }
    }

    private void unindexSighting(CreatureSightingKey key) {
        LinkedHashSet<CreatureSightingKey> values = sightingsByObserver.get(key.observer());
        if (values == null) return;
        values.remove(key);
        if (values.isEmpty()) sightingsByObserver.remove(key.observer());
    }

    private static void putPosition(CompoundTag row, WorldPosition position) {
        row.putString("dimension", position.dimension());
        row.putInt("x", position.x());
        row.putInt("y", position.y());
        row.putInt("z", position.z());
    }

    private static WorldPosition getPosition(CompoundTag row) {
        return new WorldPosition(row.getString("dimension"), row.getInt("x"), row.getInt("y"), row.getInt("z"));
    }

    private static UUID uuid(CompoundTag row, String key) {
        return UUID.fromString(row.getString(key));
    }

    private static Optional<UUID> optionalUuid(CompoundTag row, String key) {
        return row.contains(key) ? Optional.of(uuid(row, key)) : Optional.empty();
    }
}
