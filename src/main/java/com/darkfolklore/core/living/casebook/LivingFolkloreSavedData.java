package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.persistence.WorldPosition;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class LivingFolkloreSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;
    private static final String FILE_ID = "darkfolklore_living";
    private static final int HARD_MAX_CASES = 8192;
    private static final Factory<LivingFolkloreSavedData> FACTORY = new Factory<>(LivingFolkloreSavedData::new, LivingFolkloreSavedData::load);
    private final LinkedHashMap<UUID, InvestigationCaseRecord> cases = new LinkedHashMap<>();
    private final Map<UUID, UUID> byContract = new HashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> byPlayer = new HashMap<>();

    public static LivingFolkloreSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public Optional<InvestigationCaseRecord> caseForContract(UUID contract) {
        UUID id = byContract.get(contract);
        return id == null ? Optional.empty() : Optional.ofNullable(cases.get(id));
    }

    public Optional<InvestigationCaseRecord> activeCase(UUID player) {
        return casesForPlayer(player).stream().filter(value -> !value.stage().terminal())
                .max(Comparator.comparingLong(InvestigationCaseRecord::updatedAt));
    }

    public List<InvestigationCaseRecord> casesForPlayer(UUID player) {
        return byPlayer.getOrDefault(player, new LinkedHashSet<>()).stream().map(cases::get).filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(InvestigationCaseRecord::updatedAt).reversed()).toList();
    }

    public InvestigationCaseRecord create(UUID player, UUID contract, Optional<UUID> story, WorldPosition anchor,
                                          long now, long expiresAt, int playerLimit) {
        InvestigationCaseRecord existing = caseForContract(contract).orElse(null);
        if (existing != null) return existing;
        enforcePlayerLimit(player, Math.max(1, playerLimit));
        if (cases.size() >= HARD_MAX_CASES) pruneOldestTerminal();
        if (cases.size() >= HARD_MAX_CASES) throw new IllegalStateException("casebook capacity reached");
        InvestigationCaseRecord created = new InvestigationCaseRecord(UUID.randomUUID(), player, contract, story,
                CaseOrigin.CONTRACT, anchor, now, expiresAt);
        cases.put(created.id(), created); index(created); setDirty();
        return created;
    }

    public void changed(InvestigationCaseRecord record) {
        if (record != null && cases.containsKey(record.id())) setDirty();
    }

    public int prune(long now, long retention) {
        List<UUID> remove = cases.values().stream().filter(value -> value.stage().terminal()
                && now - value.updatedAt() > Math.max(1L, retention)).map(InvestigationCaseRecord::id).toList();
        remove.forEach(this::remove);
        if (!remove.isEmpty()) setDirty();
        return remove.size();
    }

    private void enforcePlayerLimit(UUID player, int limit) {
        List<InvestigationCaseRecord> values = casesForPlayer(player);
        if (values.size() < limit) return;
        values.stream().filter(value -> value.stage().terminal()).sorted(Comparator.comparingLong(InvestigationCaseRecord::updatedAt))
                .limit(values.size() - limit + 1L).map(InvestigationCaseRecord::id).toList().forEach(this::remove);
    }

    private void pruneOldestTerminal() {
        cases.values().stream().filter(value -> value.stage().terminal()).min(Comparator.comparingLong(InvestigationCaseRecord::updatedAt))
                .map(InvestigationCaseRecord::id).ifPresent(this::remove);
    }

    private void remove(UUID id) {
        InvestigationCaseRecord value = cases.remove(id);
        if (value == null) return;
        byContract.remove(value.contractId(), id);
        LinkedHashSet<UUID> ids = byPlayer.get(value.player());
        if (ids != null) { ids.remove(id); if (ids.isEmpty()) byPlayer.remove(value.player()); }
    }

    private void index(InvestigationCaseRecord value) {
        byContract.put(value.contractId(), value.id());
        byPlayer.computeIfAbsent(value.player(), ignored -> new LinkedHashSet<>()).add(value.id());
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", SCHEMA_VERSION);
        ListTag rows = new ListTag();
        cases.values().forEach(value -> rows.add(CasebookNbtCodec.save(value)));
        tag.put("cases", rows);
        return tag;
    }

    static LivingFolkloreSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LivingFolkloreSavedData data = new LivingFolkloreSavedData();
        int schema = tag.getInt("schema");
        if (schema > SCHEMA_VERSION) DarkFolkloreCore.LOGGER.warn("[living/persistence] newer schema {}", schema);
        ListTag rows = tag.getList("cases", Tag.TAG_COMPOUND);
        for (int i=0;i<Math.min(rows.size(),HARD_MAX_CASES);i++) {
            try {
                InvestigationCaseRecord value = CasebookNbtCodec.read(rows.getCompound(i));
                if (data.byContract.containsKey(value.contractId())) continue;
                data.cases.put(value.id(), value); data.index(value);
            } catch (RuntimeException ex) {
                DarkFolkloreCore.LOGGER.error("[living/persistence] malformed case row {}", i, ex);
            }
        }
        return data;
    }
}
