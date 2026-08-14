package com.darkfolklore.core.magic;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Curated cross-mod magic vocabulary. It describes capabilities, never provider-owned factual state. */
public final class MagicDisciplineRegistry {
    private static final Map<MagicDiscipline, MagicDisciplineProfile> PROFILES;

    static {
        EnumMap<MagicDiscipline, MagicDisciplineProfile> values = new EnumMap<>(MagicDiscipline.class);
        values.put(MagicDiscipline.WITCHCRAFT, profile(MagicDiscipline.WITCHCRAFT, "darkfolklore:witchcraft",
                EnumSet.of(MagicUse.OCCULT_ANALYSIS, MagicUse.COUNTERMEASURE, MagicUse.CURSE_WORK,
                        MagicUse.RITUAL_CATALYSIS), "enchanted"));
        values.put(MagicDiscipline.SPIRITUALISM, profile(MagicDiscipline.SPIRITUALISM, "darkfolklore:spirit_magic",
                EnumSet.of(MagicUse.OCCULT_ANALYSIS, MagicUse.TRACKING, MagicUse.SUMMONING,
                        MagicUse.SOUL_READING, MagicUse.RITUAL_CATALYSIS), "occultism"));
        values.put(MagicDiscipline.SOUL_MAGIC, profile(MagicDiscipline.SOUL_MAGIC, "darkfolklore:soul_magic",
                EnumSet.of(MagicUse.OCCULT_ANALYSIS, MagicUse.TRACKING, MagicUse.SOUL_READING,
                        MagicUse.RITUAL_CATALYSIS), "malum"));
        values.put(MagicDiscipline.NECROMANCY, profile(MagicDiscipline.NECROMANCY, "darkfolklore:forbidden_lore",
                EnumSet.of(MagicUse.OCCULT_ANALYSIS, MagicUse.SOUL_READING, MagicUse.CURSE_WORK,
                        MagicUse.RITUAL_CATALYSIS), "eidolon_repraised"));
        values.put(MagicDiscipline.FAE_MAGIC, profile(MagicDiscipline.FAE_MAGIC, "darkfolklore:fae_lore",
                EnumSet.of(MagicUse.OCCULT_ANALYSIS, MagicUse.TRACKING, MagicUse.FAE_LORE,
                        MagicUse.RITUAL_CATALYSIS), "feywild"));
        values.put(MagicDiscipline.BLOOD_MAGIC, profile(MagicDiscipline.BLOOD_MAGIC, "darkfolklore:blood_magic",
                EnumSet.of(MagicUse.OCCULT_ANALYSIS, MagicUse.BLOOD_READING, MagicUse.COUNTERMEASURE,
                        MagicUse.RITUAL_CATALYSIS), "bloodlines"));
        values.put(MagicDiscipline.RITUAL_MAGIC, profile(MagicDiscipline.RITUAL_MAGIC, "darkfolklore:ritual_magic",
                EnumSet.of(MagicUse.OCCULT_ANALYSIS, MagicUse.RITUAL_CATALYSIS),
                "enchanted", "occultism", "malum", "eidolon_repraised", "feywild", "bloodlines"));
        PROFILES = Map.copyOf(values);
    }

    private MagicDisciplineRegistry() {}

    public static MagicDisciplineProfile profile(MagicDiscipline discipline) { return PROFILES.get(discipline); }
    public static Set<MagicDiscipline> disciplines() { return Set.copyOf(PROFILES.keySet()); }

    private static MagicDisciplineProfile profile(MagicDiscipline discipline, String concept,
                                                   EnumSet<MagicUse> uses, String... namespaces) {
        return new MagicDisciplineProfile(discipline, concept, uses, Set.of(namespaces));
    }
}
