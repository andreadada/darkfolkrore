package com.darkfolklore.core.encounter;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import java.util.*;

public final class EncounterCatalog {
    /** Story manifestations deliberately sit above their ordinary natural-spawn implementation. */
    public static final EncounterDefinition WENDIGO = new EncounterDefinition(
            "darkfolklore:wendigo_hunger", "darkfolklore:wendigo", "cnc:wendigo",
            EncounterRank.LEGENDARY, EncounterSpawnMode.STORY_MANIFESTATION,
            Set.of(EncounterOrigin.STARVATION, EncounterOrigin.LOST_PERSON),
            3, true, 24, 96, 12000, 168000, 120000,
            List.of(EvidenceType.FOOTPRINT, EvidenceType.BONE, EvidenceType.BLOOD), 65);

    public static final EncounterDefinition CHUPACABRA = new EncounterDefinition(
            "darkfolklore:livestock_panic", "darkfolklore:chupacabra", "cnc:chupacabra",
            EncounterRank.DREAD, EncounterSpawnMode.STORY_MANIFESTATION,
            Set.of(EncounterOrigin.BLOOD_EVENT),
            2, true, 20, 80, 6000, 96000, 96000,
            List.of(EvidenceType.BLOOD, EvidenceType.SCENT, EvidenceType.FOOTPRINT), 50);

    public static final EncounterDefinition REVENANT = new EncounterDefinition(
            "darkfolklore:returned_dead", "darkfolklore:revenant", "graveyard:revenant",
            EncounterRank.DREAD, EncounterSpawnMode.STORY_MANIFESTATION,
            Set.of(EncounterOrigin.VIOLENT_DEATH, EncounterOrigin.SPIRIT_UNREST),
            2, true, 18, 72, 12000, 144000, 144000,
            List.of(EvidenceType.SPIRIT_ECHO, EvidenceType.SOUL_ECHO, EvidenceType.BONE), 45);

    public static final EncounterDefinition WILD_HUNT = new EncounterDefinition(
            "darkfolklore:wild_hunt", "darkfolklore:wild_hunt", "occultism:wild_hunt_wither_skeleton",
            EncounterRank.LEGENDARY, EncounterSpawnMode.EVENT_ONLY,
            Set.of(EncounterOrigin.WORLD_OMEN),
            2, true, 28, 112, 8000, 240000, 72000,
            List.of(EvidenceType.OCCULT_SIGNATURE, EvidenceType.SPIRIT_ECHO, EvidenceType.FOOTPRINT), 60);

    private static final Map<String, EncounterDefinition> BY_ID = new LinkedHashMap<>();
    static {
        for (EncounterDefinition definition : List.of(WENDIGO, CHUPACABRA, REVENANT, WILD_HUNT)) {
            BY_ID.put(definition.id(), definition);
        }
    }

    private EncounterCatalog() {}

    public static Optional<EncounterDefinition> byId(String id) { return Optional.ofNullable(BY_ID.get(id)); }
    public static List<EncounterDefinition> all() { return List.copyOf(BY_ID.values()); }
}
