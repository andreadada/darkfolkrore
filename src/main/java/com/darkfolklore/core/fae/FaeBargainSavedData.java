package com.darkfolklore.core.fae;

import com.darkfolklore.core.DarkFolkloreCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded regional cooldown state for offerings; individual fae provider entities remain provider-owned. */
public final class FaeBargainSavedData extends SavedData {
    private static final int SCHEMA = 1;
    private static final int MAX_REGIONS = 2048;
    private static final String FILE_ID = "darkfolklore_fae_bargains";
    private static final Factory<FaeBargainSavedData> FACTORY = new Factory<>(FaeBargainSavedData::new, FaeBargainSavedData::load);
    private final LinkedHashMap<String, Long> cooldowns = new LinkedHashMap<>();

    public static FaeBargainSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public boolean ready(String region, long now) { return now >= cooldowns.getOrDefault(region, 0L); }

    public void mark(String region, long until) {
        if (!cooldowns.containsKey(region) && cooldowns.size() >= MAX_REGIONS) {
            String oldest = cooldowns.entrySet().stream().min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            if (oldest != null) cooldowns.remove(oldest);
        }
        cooldowns.put(region, Math.max(0L, until));
        setDirty();
    }

    public int prune(long now) {
        int before = cooldowns.size();
        cooldowns.entrySet().removeIf(e -> e.getValue() + 168000L < now);
        int removed = before - cooldowns.size();
        if (removed > 0) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", SCHEMA);
        ListTag list = new ListTag();
        cooldowns.forEach((region, until) -> {
            CompoundTag row = new CompoundTag();
            row.putString("region", region);
            row.putLong("until", until);
            list.add(row);
        });
        tag.put("cooldowns", list);
        return tag;
    }

    private static FaeBargainSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FaeBargainSavedData data = new FaeBargainSavedData();
        int schema = tag.getInt("schema");
        if (schema > SCHEMA) {
            DarkFolkloreCore.LOGGER.warn("[fae/persistence] Save schema {} is newer than supported {}; attempting safe read",
                    schema, SCHEMA);
        }
        ListTag list = tag.getList("cooldowns", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_REGIONS); i++) {
            CompoundTag row = list.getCompound(i);
            data.cooldowns.put(row.getString("region"), Math.max(0L, row.getLong("until")));
        }
        if (schema == 0) data.setDirty();
        return data;
    }
}
