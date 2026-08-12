# Missing Content Research

## Current conclusion

No new entity, item, block, model, texture, or animation is required for the current Dark Folklore Core 0.4.0 release-candidate scope.

The implemented architecture is deliberately systemic. Canonical concepts point at installed providers, and concepts without safe equivalence are kept distinct rather than replaced by placeholders. Because no necessary concept failed the installed-content audit, no external asset/mod candidate was selected and no licensing claim is made here.

## Coverage evidence

| Folklore need | Existing audited coverage | Decision |
| --- | --- | --- |
| Werewolf faction and creatures | Werewolves 2.0.3.3 | Canonical gameplay owner. |
| Wendigo | Critters n' Crawlers 2.2.5 (cnc:wendigo) | Only verified implementation; canonical. |
| Chupacabra | Critters n' Crawlers 2.2.5 and Mobs of Mythology 3.0.0 | Critters n' Crawlers selected; the duplicate natural spawn is suppressed while Core canonicalization is enabled. |
| Imp | Myths & Legends 0.0.8.6 and Fangs 'n Claws 1.2.2 | Myths & Legends selected for natural ecology. |
| Ghosts | Fangs 'n Claws and Vampirism | Distinct roles retained. |
| Wraiths | Eidolon: Repraised and The Graveyard | Distinct roles retained. |
| Fae | Feywild 5.5.5 plus related occult entities | Classified through data; no duplicate Core entity. |
| Undead and spirits | The Graveyard, Eidolon, Occultism, Malum, Vampirism, and vanilla | Classified through semantic tags. |
| Constructs/golems | Fangs 'n Claws, Occultism, Myths & Legends | Distinct implementations retained. |
| Garlic/wolfsbane/silver/blood herbs and materials | Vampirism, Werewolves, Enchanted, Immersive Engineering, Occultism, Eidolon, and add-ons | Canonical/interoperability policy uses semantic data plus finite config-aware generated-loot/worldgen routing and the exact wolfsbane mechanics bridge; no Core item needed. |
| Ritual altars and poppets | Enchanted, Eidolon, Werewolves, The Graveyard, and Malum | Name collisions kept distinct. |

This table establishes coverage, not ownership of the providers' art assets. Dark Folklore does not copy those assets into its own JAR.

## Concepts that are not missing content

The following Dark Folklore records are abstract system data and must not be turned into placeholder registry objects:

- canonical concept IDs;
- lore stages;
- social secrets and beliefs;
- rumor/evidence records;
- organizations and village state;
- contracts and persistent stories;
- encounter pressure and world events;
- magic-tradition integration definitions.

They can use text, tags, and existing entities/items. A new model would not improve their correctness.

## Deferred presentation work

Field Guide currently provides seven curated categories, ten explicit provider-backed entries, and English/Italian names and descriptions after the Fae Sprite addition. Final joined-world client rendering and Recent Discoveries remain a manual release gate; that presentation work does not justify placeholder creatures/items.

Dynamic Field Guide tier descriptions are also a capability limitation, not missing visual content. Dark Folklore keeps the full lore-stage state in its own server data rather than manufacturing an asset workaround.

## Required process for any future content gap

Before adding a new registry object or recommending another mod, create a concept-specific section in this document with all of the following evidence:

1. Missing concept and the gameplay requirement it uniquely serves.
2. Search of the current Atlas registry, recipes, tags, resources, and installed JAR behavior.
3. Why every apparent existing candidate is semantically or visually unsuitable.
4. Candidate external mod or asset, exact Minecraft/loader/version compatibility, and maintained source location.
5. Visual-quality review covering model, texture, animation, scale, readability, and pack art direction.
6. License text and whether redistribution, modification, and inclusion in a modpack are permitted.
7. Integration feasibility: registry stability, spawn ownership, AI, loot, networking, server safety, and canonicalization impact.
8. Save-migration and removal plan if the candidate later changes.
9. Explicit pack-owner approval.

If no compliant candidate exists, use properly commissioned/original assets or defer the creature. Do not ship an unlabeled placeholder, scraped model, AI-generated derivative of a protected asset, or registry object with temporary art.

## Research log

| Date | Missing concept | Candidate research | Licensing | Result |
| --- | --- | --- | --- | --- |
| 2026-08-10 | None required for 0.2.0 | Not initiated; installed pack covers the release scope. | Not applicable. | Release is not blocked by custom content. |
