package com.darkfolklore.core.ward;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.persistence.WorldPosition;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public final class WardSavedData extends SavedData {
    private static final int SCHEMA = 1;
    private static final int MAX_WARDS = 512;
    private static final String FILE_ID = "darkfolklore_wards";
    private static final Factory<WardSavedData> FACTORY = new Factory<>(WardSavedData::new, WardSavedData::load);
    private final LinkedHashMap<UUID, WardRecord> wards = new LinkedHashMap<>();

    public static WardSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public Collection<WardRecord> wards() { return List.copyOf(wards.values()); }

    /** One normalized threshold owns at most one ward. A full store fails closed instead of evicting another ward. */
    public boolean add(WardRecord ward) {
        List<WardRecord> sameThreshold = wards.values().stream()
                .filter(value -> sameAnchor(value.anchor(), ward.anchor())).toList();
        boolean protectedByOtherCreator = sameThreshold.stream()
                .anyMatch(value -> value.active(ward.createdAt()) && !value.creator().equals(ward.creator()));
        if (protectedByOtherCreator) return false;
        if (sameThreshold.isEmpty() && wards.size() >= MAX_WARDS) return false;
        sameThreshold.forEach(value -> wards.remove(value.id()));
        wards.put(ward.id(), ward);
        setDirty();
        return true;
    }

    private static boolean sameAnchor(WorldPosition a, WorldPosition b) {
        return a.dimension().equals(b.dimension()) && a.x() == b.x() && a.y() == b.y() && a.z() == b.z();
    }

    public int strengthAt(String dimension, double x, double y, double z, WardType type, long now) {
        int best = 0;
        for (WardRecord ward : wards.values()) {
            if (!ward.active(now) || !ward.appliesTo(type) || !ward.anchor().dimension().equals(dimension)) continue;
            double dx = x - (ward.anchor().x() + .5);
            double dy = y - (ward.anchor().y() + .5);
            double dz = z - (ward.anchor().z() + .5);
            if (dx * dx + dy * dy + dz * dz <= ward.radius() * ward.radius()) best = Math.max(best, ward.strength());
        }
        return best;
    }

    public int prune(long now) {
        int before = wards.size();
        wards.values().removeIf(ward -> !ward.active(now));
        int removed = before - wards.size();
        if (removed > 0) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", SCHEMA);
        ListTag list = new ListTag();
        wards.values().forEach(ward -> {
            CompoundTag row = new CompoundTag();
            row.putString("id", ward.id().toString()); row.putString("type", ward.type().name());
            row.putString("dimension", ward.anchor().dimension()); row.putInt("x", ward.anchor().x()); row.putInt("y", ward.anchor().y()); row.putInt("z", ward.anchor().z());
            row.putInt("radius", ward.radius()); row.putInt("strength", ward.strength()); row.putString("creator", ward.creator().toString());
            row.putLong("created", ward.createdAt()); row.putLong("expires", ward.expiresAt()); list.add(row);
        });
        tag.put("wards", list);
        return tag;
    }

    private static WardSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WardSavedData data = new WardSavedData();
        if (tag.getInt("schema") > SCHEMA) DarkFolkloreCore.LOGGER.warn("[ward] newer schema found; attempting safe read");
        ListTag list = tag.getList("wards", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_WARDS); i++) {
            try {
                CompoundTag row = list.getCompound(i);
                WardRecord ward = new WardRecord(UUID.fromString(row.getString("id")), WardType.valueOf(row.getString("type")),
                        new WorldPosition(row.getString("dimension"), row.getInt("x"), row.getInt("y"), row.getInt("z")),
                        row.getInt("radius"), row.getInt("strength"), UUID.fromString(row.getString("creator")),
                        row.getLong("created"), row.getLong("expires"));
                data.wards.put(ward.id(), ward);
            } catch (RuntimeException ex) {
                DarkFolkloreCore.LOGGER.error("[ward] ignoring malformed row {}", i, ex);
            }
        }
        return data;
    }
}
