package com.darkfolklore.core.endgame;

import com.darkfolklore.core.persistence.WorldPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class ForbiddenEndgameSavedData extends SavedData {
    private static final int MAX_SITES = 128;
    private static final int MAX_PLAYERS = 4096;
    private static final String FILE_ID = "darkfolklore_forbidden_endgame";
    private static final Factory<ForbiddenEndgameSavedData> FACTORY = new Factory<>(ForbiddenEndgameSavedData::new, ForbiddenEndgameSavedData::load);
    private final LinkedHashMap<UUID, DemonInvocationSite> sites = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, EnumSet<EndgameMilestone>> milestones = new LinkedHashMap<>();

    public static ForbiddenEndgameSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public Collection<DemonInvocationSite> sites() { return List.copyOf(sites.values()); }
    public boolean hasCapacityForSite() { return sites.size() < MAX_SITES; }

    public Optional<DemonInvocationSite> activeNear(String dimension, BlockPos pos, double radius) {
        double max = radius * radius;
        return sites.values().stream().filter(s -> s.state() == DemonInvocationState.ACTIVE)
                .filter(s -> s.anchor().dimension().equals(dimension)).filter(s -> s.anchor().distanceSquared(pos) <= max).findFirst();
    }

    public Optional<DemonInvocationSite> byParticipant(UUID entity) {
        return sites.values().stream().filter(s -> s.participants().contains(entity)).findFirst();
    }

    public Optional<DemonInvocationSite> begin(UUID owner, WorldPosition anchor, long now, UUID boss) {
        if (!hasCapacityForSite()) return Optional.empty();
        DemonInvocationSite site = new DemonInvocationSite(UUID.randomUUID(), owner, anchor, now, boss);
        sites.put(site.id(), site);
        markMilestone(owner, EndgameMilestone.DEMON_INVOCATION);
        setDirty();
        return Optional.of(site);
    }

    public boolean bindParticipant(DemonInvocationSite site, UUID participant) {
        boolean changed = site != null && site.state() == DemonInvocationState.ACTIVE && site.addParticipant(participant);
        if (changed) setDirty();
        return changed;
    }

    public void complete(DemonInvocationSite site, long now) {
        if (site != null && !site.state().terminal()) { site.complete(now); setDirty(); }
    }

    public void markMilestone(UUID player, EndgameMilestone milestone) {
        if (player == null || milestone == null || (!milestones.containsKey(player) && milestones.size() >= MAX_PLAYERS)) return;
        if (milestones.computeIfAbsent(player, x -> EnumSet.noneOf(EndgameMilestone.class)).add(milestone)) setDirty();
    }

    public Set<EndgameMilestone> milestones(UUID player) {
        return Set.copyOf(milestones.getOrDefault(player, EnumSet.noneOf(EndgameMilestone.class)));
    }

    public void prune(long now, long retention) {
        if (sites.entrySet().removeIf(e -> e.getValue().state().terminal() && e.getValue().completedAt() > 0 && now - e.getValue().completedAt() > retention)) setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", 1);
        ListTag siteRows = new ListTag();
        sites.values().forEach(s -> siteRows.add(saveSite(s)));
        tag.put("sites", siteRows);
        ListTag playerRows = new ListTag();
        milestones.forEach((player, values) -> {
            CompoundTag row = new CompoundTag(); row.putUUID("player", player);
            ListTag list = new ListTag();
            values.forEach(value -> { CompoundTag v = new CompoundTag(); v.putString("id", value.name()); list.add(v); });
            row.put("values", list); playerRows.add(row);
        });
        tag.put("milestones", playerRows);
        return tag;
    }

    private static ForbiddenEndgameSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ForbiddenEndgameSavedData data = new ForbiddenEndgameSavedData();
        ListTag siteRows = tag.getList("sites", Tag.TAG_COMPOUND);
        for (int i=0; i<Math.min(siteRows.size(), MAX_SITES); i++) {
            try { DemonInvocationSite site = readSite(siteRows.getCompound(i)); data.sites.put(site.id(), site); } catch (RuntimeException ignored) { }
        }
        ListTag playerRows = tag.getList("milestones", Tag.TAG_COMPOUND);
        for (int i=0; i<Math.min(playerRows.size(), MAX_PLAYERS); i++) {
            CompoundTag row = playerRows.getCompound(i); if (!row.hasUUID("player")) continue;
            EnumSet<EndgameMilestone> values = EnumSet.noneOf(EndgameMilestone.class);
            ListTag list = row.getList("values", Tag.TAG_COMPOUND);
            for (int j=0; j<list.size(); j++) try { values.add(EndgameMilestone.valueOf(list.getCompound(j).getString("id"))); } catch (IllegalArgumentException ignored) { }
            if (!values.isEmpty()) data.milestones.put(row.getUUID("player"), values);
        }
        return data;
    }

    private static CompoundTag saveSite(DemonInvocationSite s) {
        CompoundTag row = new CompoundTag();
        row.putUUID("id", s.id()); row.putUUID("owner", s.owner()); row.putString("dimension", s.anchor().dimension());
        row.putInt("x", s.anchor().x()); row.putInt("y", s.anchor().y()); row.putInt("z", s.anchor().z());
        row.putLong("created", s.createdAt()); row.putString("state", s.state().name()); row.putLong("completed", s.completedAt());
        s.currentBoss().ifPresent(id -> row.putUUID("boss", id));
        ListTag list = new ListTag(); s.participants().forEach(id -> { CompoundTag p = new CompoundTag(); p.putUUID("id", id); list.add(p); }); row.put("participants", list);
        return row;
    }

    private static DemonInvocationSite readSite(CompoundTag row) {
        UUID boss = row.hasUUID("boss") ? row.getUUID("boss") : null;
        DemonInvocationSite site = new DemonInvocationSite(row.getUUID("id"), row.getUUID("owner"),
                new WorldPosition(row.getString("dimension"), row.getInt("x"), row.getInt("y"), row.getInt("z")), row.getLong("created"), boss);
        List<UUID> participants = new ArrayList<>(); ListTag list = row.getList("participants", Tag.TAG_COMPOUND);
        for (int i=0; i<Math.min(list.size(), DemonInvocationSite.MAX_PARTICIPANTS); i++) if (list.getCompound(i).hasUUID("id")) participants.add(list.getCompound(i).getUUID("id"));
        DemonInvocationState state; try { state = DemonInvocationState.valueOf(row.getString("state")); } catch (IllegalArgumentException ex) { state = DemonInvocationState.ACTIVE; }
        site.restore(state, boss, row.getLong("completed"), participants); return site;
    }
}
