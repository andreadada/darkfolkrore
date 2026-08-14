package com.darkfolklore.core.encounter;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.persistence.WorldPosition;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Dedicated bounded persistence for story-backed supernatural encounters. */
public final class LegendaryEncounterSavedData extends SavedData {
    private static final int SCHEMA = 2;
    private static final int MAX_ENCOUNTERS = 512;
    private static final int MAX_REGIONS = 2048;
    private static final String FILE_ID = "darkfolklore_legendary_encounters";
    private static final Factory<LegendaryEncounterSavedData> FACTORY =
            new Factory<>(LegendaryEncounterSavedData::new, LegendaryEncounterSavedData::load);

    private final LinkedHashMap<UUID, EncounterInstance> encounters = new LinkedHashMap<>();
    private final LinkedHashMap<String, Long> regionCooldowns = new LinkedHashMap<>();
    private final LinkedHashMap<String, LivestockPanic> livestockPanic = new LinkedHashMap<>();

    public static LegendaryEncounterSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public Collection<EncounterInstance> encounters() { return List.copyOf(encounters.values()); }
    public Optional<EncounterInstance> encounter(UUID id) { return Optional.ofNullable(encounters.get(id)); }

    public Optional<EncounterInstance> byManifestation(UUID entity) {
        return encounters.values().stream()
                .filter(value -> value.manifestationEntity().filter(entity::equals).isPresent())
                .findFirst();
    }

    public Optional<EncounterInstance> byParticipant(UUID entity) {
        return encounters.values().stream().filter(value -> value.participants().contains(entity)).findFirst();
    }

    public boolean hasActiveInRegion(String region) {
        return encounters.values().stream().anyMatch(value -> value.region().equals(region) && !value.stage().terminal());
    }

    public long nextAllowed(String region) { return regionCooldowns.getOrDefault(region, 0L); }
    public boolean regionReady(String region, long now) { return now >= nextAllowed(region) && !hasActiveInRegion(region); }

    public boolean put(EncounterInstance encounter) {
        if (!encounters.containsKey(encounter.id()) && encounters.size() >= MAX_ENCOUNTERS) return false;
        encounters.put(encounter.id(), encounter);
        setDirty();
        return true;
    }

    public void markRegionCooldown(String region, long until) {
        if (!regionCooldowns.containsKey(region) && regionCooldowns.size() >= MAX_REGIONS) {
            String oldest = regionCooldowns.entrySet().stream().min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null);
            if (oldest != null) regionCooldowns.remove(oldest);
        }
        regionCooldowns.put(region, Math.max(0L, until));
        setDirty();
    }

    public LivestockPanic recordLivestockDeath(String region, long now) {
        LivestockPanic current = livestockPanic.get(region);
        if (current == null || now - current.lastIncident() > 72000L) current = new LivestockPanic(0, now, now);
        LivestockPanic next = current.increment(now);
        if (!livestockPanic.containsKey(region) && livestockPanic.size() >= MAX_REGIONS) {
            String oldest = livestockPanic.entrySet().stream()
                    .min(Comparator.comparingLong(value -> value.getValue().lastIncident()))
                    .map(Map.Entry::getKey).orElse(null);
            if (oldest != null) livestockPanic.remove(oldest);
        }
        livestockPanic.put(region, next);
        setDirty();
        return next;
    }

    public Optional<LivestockPanic> livestockPanic(String region) { return Optional.ofNullable(livestockPanic.get(region)); }
    public void clearLivestockPanic(String region) { if (livestockPanic.remove(region) != null) setDirty(); }

    public int prune(long now, long retention) {
        int before = encounters.size() + regionCooldowns.size() + livestockPanic.size();
        encounters.entrySet().removeIf(entry -> entry.getValue().stage().terminal()
                && now - entry.getValue().expiresAt() > retention);
        regionCooldowns.entrySet().removeIf(entry -> entry.getValue() + retention < now);
        livestockPanic.entrySet().removeIf(entry -> now - entry.getValue().lastIncident() > 144000L);
        int removed = before - encounters.size() - regionCooldowns.size() - livestockPanic.size();
        if (removed > 0) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", SCHEMA);
        ListTag list = new ListTag();
        encounters.values().forEach(value -> list.add(saveEncounter(value)));
        tag.put("encounters", list);

        ListTag cooldowns = new ListTag();
        regionCooldowns.forEach((region, until) -> {
            CompoundTag row = new CompoundTag();
            row.putString("region", region);
            row.putLong("until", until);
            cooldowns.add(row);
        });
        tag.put("region_cooldowns", cooldowns);

        ListTag panic = new ListTag();
        livestockPanic.forEach((region, value) -> {
            CompoundTag row = new CompoundTag();
            row.putString("region", region);
            row.putInt("count", value.count());
            row.putLong("first", value.firstIncident());
            row.putLong("last", value.lastIncident());
            panic.add(row);
        });
        tag.put("livestock_panic", panic);
        return tag;
    }

    private static LegendaryEncounterSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LegendaryEncounterSavedData data = new LegendaryEncounterSavedData();
        int schema = tag.getInt("schema");
        if (schema > SCHEMA) {
            DarkFolkloreCore.LOGGER.warn("[encounter] newer save schema {}; attempting safe read", schema);
        }
        ListTag list = tag.getList("encounters", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_ENCOUNTERS); i++) {
            try {
                EncounterInstance value = readEncounter(list.getCompound(i));
                data.encounters.put(value.id(), value);
            } catch (RuntimeException ex) {
                DarkFolkloreCore.LOGGER.error("[encounter] malformed row {}", i, ex);
            }
        }
        ListTag cooldowns = tag.getList("region_cooldowns", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(cooldowns.size(), MAX_REGIONS); i++) {
            CompoundTag row = cooldowns.getCompound(i);
            data.regionCooldowns.put(row.getString("region"), Math.max(0L, row.getLong("until")));
        }
        ListTag panic = tag.getList("livestock_panic", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(panic.size(), MAX_REGIONS); i++) {
            CompoundTag row = panic.getCompound(i);
            data.livestockPanic.put(row.getString("region"), new LivestockPanic(
                    Math.max(0, row.getInt("count")), Math.max(0L, row.getLong("first")), Math.max(0L, row.getLong("last"))));
        }
        if (schema > 0 && schema < SCHEMA) data.setDirty();
        return data;
    }

    private static CompoundTag saveEncounter(EncounterInstance value) {
        CompoundTag row = new CompoundTag();
        row.putString("id", value.id().toString());
        row.putString("definition", value.definitionId());
        row.putString("concept", value.concept());
        row.putString("implementation", value.implementation());
        row.putString("rank", value.rank().name());
        row.putString("spawn_mode", value.spawnMode().name());
        row.putString("origin", value.origin().name());
        row.putString("stage", value.stage().name());
        row.putString("region", value.region());
        row.putLong("created", value.createdAt());
        row.putLong("next", value.nextStageAt());
        row.putLong("expires", value.expiresAt());
        row.putInt("omens", value.omensCompleted());
        row.putString("resolution", value.resolution());

        CompoundTag pos = new CompoundTag();
        pos.putString("dimension", value.anchor().dimension());
        pos.putInt("x", value.anchor().x());
        pos.putInt("y", value.anchor().y());
        pos.putInt("z", value.anchor().z());
        row.put("anchor", pos);

        value.manifestationEntity().ifPresent(uuid -> row.putString("manifestation", uuid.toString()));
        value.storyId().ifPresent(uuid -> row.putString("story", uuid.toString()));
        ListTag participants = new ListTag();
        for (UUID participant : value.participants()) {
            CompoundTag p = new CompoundTag();
            p.putString("id", participant.toString());
            participants.add(p);
        }
        row.put("participants", participants);

        value.originPerson().ifPresent(person -> {
            CompoundTag p = new CompoundTag();
            p.putString("id", person.personId().toString());
            p.putString("name", person.displayName());
            person.killer().ifPresent(killer -> p.putString("killer", killer.toString()));
            p.putString("home", person.homeRegion());
            p.putBoolean("political", person.politicalFigure());
            row.put("person", p);
        });
        return row;
    }

    private static EncounterInstance readEncounter(CompoundTag row) {
        CompoundTag pos = row.getCompound("anchor");
        EncounterInstance value = new EncounterInstance(
                UUID.fromString(row.getString("id")), row.getString("definition"), row.getString("concept"),
                row.getString("implementation"), EncounterRank.valueOf(row.getString("rank")),
                EncounterSpawnMode.valueOf(row.getString("spawn_mode")), EncounterOrigin.valueOf(row.getString("origin")),
                new WorldPosition(pos.getString("dimension"), pos.getInt("x"), pos.getInt("y"), pos.getInt("z")),
                row.getString("region"), row.getLong("created"), row.getLong("next"), row.getLong("expires"));
        UUID manifestation = row.contains("manifestation") ? UUID.fromString(row.getString("manifestation")) : null;
        UUID story = row.contains("story") ? UUID.fromString(row.getString("story")) : null;
        PersonSnapshot person = null;
        if (row.contains("person", Tag.TAG_COMPOUND)) {
            CompoundTag p = row.getCompound("person");
            person = new PersonSnapshot(UUID.fromString(p.getString("id")), p.getString("name"),
                    p.contains("killer") ? Optional.of(UUID.fromString(p.getString("killer"))) : Optional.empty(),
                    p.getString("home"), p.getBoolean("political"));
        }
        value.restoreStage(EncounterStage.valueOf(row.getString("stage")), row.getInt("omens"), manifestation,
                story, person, row.getString("resolution"), row.getLong("next"));
        List<UUID> participants = new ArrayList<>();
        ListTag participantTags = row.getList("participants", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(participantTags.size(), EncounterInstance.MAX_PARTICIPANTS); i++) {
            try { participants.add(UUID.fromString(participantTags.getCompound(i).getString("id"))); }
            catch (IllegalArgumentException ignored) { }
        }
        value.restoreParticipants(participants);
        return value;
    }

    public record LivestockPanic(int count, long firstIncident, long lastIncident) {
        public LivestockPanic increment(long now) { return new LivestockPanic(count + 1, firstIncident, now); }
    }
}
