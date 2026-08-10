# Canonicalization Audit

## Evidence set

This audit uses the installed JARs/configuration and Dark Folklore Atlas 0.2.0 scan scan-20260810-143508. The scan targeted Minecraft 1.21.1, was generated at 2026-08-10T14:35:08.128059700Z, and was read-only. It indexed 147 mods, 5,346 items, 453 entities, 6,048 recipes, 99 item concept groups, 19 entity concept groups, and 117 matrix rows.

Atlas similarity is evidence for review, not proof of equivalence. Every outcome below was selected semantically.

## Outcome vocabulary

| Outcome | Audit meaning |
| --- | --- |
| FULL_CANONICALIZATION | A preferred implementation is chosen and normal future acquisition/ecology is actively routed toward it. Foreign entries remain registered. |
| INTEROPERABILITY_ONLY | Distinct registry objects share semantic traits or pack systems; no replacement occurs. |
| KEEP_DISTINCT | The names overlap, but roles or owning systems do not. |
| SUPPRESS_DUPLICATE_SPAWN | A duplicate remains addressable but cannot enter through natural spawning. |
| DEFERRED_UNSAFE | Evidence is insufficient for a safe change. |

“Implementation status” distinguishes the declared metadata policy from the actual mechanisms present in 0.1.0.

Unless a row says otherwise, Core-owned loot conversion, duplicate feature/spawn-list removal, and duplicate spawn-profile suppression apply while the common canonicalization toggle is true (its default). AlmostUnified recipe rewriting and provider-owned configuration are independent of that toggle.

## Silver material family

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:silver |
| Candidates | Immersive Engineering, Eidolon: Repraised, Occultism, and Werewolves raw material, ore, ingot, nugget, and storage forms. |
| Decision | FULL_CANONICALIZATION to the Immersive Engineering family; the definition's representative ID is immersiveengineering:ingot_silver. |
| Reason | The supplied AlmostUnified material priority already favors Immersive Engineering and rewrites recipes. A second recipe-unification engine would conflict with that owner. |
| Risk | High if registry objects were removed or saved stacks were rewritten. Low-to-medium with future-only acquisition routing. |
| Runtime policy | AlmostUnified handles recipes independently. While Core canonicalization is enabled, ConfigurableRemoveFeaturesBiomeModifier removes eidolon_repraised:silver_ore_placed, occultism:ore_silver, occultism:ore_silver_deepslate, and werewolves:silver_ore from the Overworld underground_ores step; CanonicalItemLootModifier maps 22 audited foreign base-silver loot outputs to matching Immersive Engineering forms. |
| Implementation status | FULL_CANONICALIZATION policy with PARTIAL-by-design universal migration. Recipe routing, the four audited duplicate features, and newly generated matching loot are covered while enabled; existing stacks/blocks and non-loot creation paths are intentionally outside the Core converter. AlmostUnified has loot_unification=false and world_gen_unification=false, so the Core supplies those missing layers behind its own toggle. |
| Migration impact | Existing chunks, ore blocks, inventory stacks, and foreign registry IDs remain valid. With canonicalization enabled, subsequently generated matching loot is canonical and newly generated chunks use biome settings without the four audited duplicate features. Commands/creative and unusual non-loot acquisition can still produce legacy objects. |
| API/code finding | Core-registered NeoForge biome-modifier and global-loot-modifier codecs; no external Java internals. Both Core paths read FolkloreConfig.CANONICALIZATION. Loot conversion preserves count/components and skips an absent target. |

The placed-feature list is optional. If a provider is absent, the tag entry is ignored. Immersive Engineering's own silver feature is not removed. Turning canonicalization off bypasses Core feature removal and loot conversion, but does not turn off AlmostUnified's existing recipe policy or restore features to chunks already generated.

## Silver weapons

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:silver_weapon |
| Candidates | Werewolves sword/axe; Eidolon sword/axe; Fangs 'n Claws sword; Vampire's Delight knife; Immersive Engineering silver bullet. |
| Decision | INTEROPERABILITY_ONLY. werewolves:silver_sword is the representative ID, not a replacement target. |
| Reason | Weapons have different stats, projectile behavior, recipes, and mod ownership. Their shared fact is “silver weapon,” not functional equivalence. |
| Runtime policy | darkfolklore:silver_weapon includes the native werewolves:tools/silver tag plus audited optional items. Dark Folklore also extends the native Werewolves and Fangs werewolf-bane tags with compatible weapons. |
| Implementation status | Complete item-level semantic bridge; no item migration. Melee/native-tag handling is verified, but a fired third-party projectile may expose its firearm rather than the tagged ammunition through DamageSource.getWeaponItem, so provider-specific silver-bullet damage remains a documented edge. |
| Migration impact | None. All source weapons remain distinct. |
| API/code finding | Weakness rules consume the SILVER_WEAPON trait. The Core skips its werewolf multiplier for targets in the werewolves namespace to preserve native Werewolves damage/effects. |

## Werewolf

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:werewolf |
| Candidates | Werewolves faction/progression entities and fangs_n_claws:werewolf. |
| Decision | FULL_CANONICALIZATION with Werewolves as the gameplay owner; SUPPRESS_DUPLICATE_SPAWN for the Fangs implementation. |
| Reason | Werewolves owns player faction progression, transformations, skills, silver/wolfsbane behavior, and its own ecology. A second generic natural werewolf would fragment those systems. |
| Runtime policy | The representative entity is werewolves:human_werewolf. Fangs natural spawning is disabled independently in its supplied config. The Core profile is marked canonicalization_suppression and rejects MobSpawnType.NATURAL only while both canonicalization and the spawn director are enabled. |
| Implementation status | Complete for the audited pack's natural ecology and shared classification; the Core suppression layer is deliberately configurable. |
| Migration impact | Existing Fangs werewolves, spawn eggs, commands, and non-natural spawns remain valid. |
| API/code finding | Factual state uses VampirismAPI.factionRegistry and the werewolves:werewolf faction ID at exact tested versions. No Werewolves internals or Mixins. |

## Wolfsbane

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:wolfsbane |
| Candidates | werewolves:wolfsbane and enchanted:wolfsbane_flower. |
| Decision | INTEROPERABILITY_ONLY with Werewolves as the representative implementation. |
| Reason | Werewolves' item is integrated with WolfsbaneBlock, diffuser block entities, LevelWolfsbane aura state, effects, recipes, and faction behavior. Replacing it with the Enchanted crop would bypass those mechanics. |
| Runtime policy | Both items receive the Dark Folklore WOLFSBANE trait. No stack rewrite occurs. |
| Implementation status | Partial by design: shared semantic queries work, but Enchanted's flower does not become a native Werewolves aura/diffuser item. |
| Migration impact | None. Both crop chains remain intact. |
| API/code finding | Registry/tag bridge only. No internal aura calls. |

## Garlic

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:garlic |
| Candidates | vampirism:garlic and enchanted:garlic; vampiresdelight:wild_garlic is classified as related garlic. |
| Decision | FULL_CANONICALIZATION toward Vampirism garlic. |
| Reason | Vampirism garlic uses a specialized GarlicItem/GarlicBlock and participates in diffuser, AI avoidance, effect, crop, purified-garlic, bread, and hunter recipes. Enchanted garlic is a separate crop item with no equivalent supernatural integration. |
| Runtime policy | While canonicalization is enabled, newly generated enchanted:garlic loot is replaced with vampirism:garlic when the target is registered. All three audited garlic items receive the GARLIC semantic trait regardless of that toggle. |
| Implementation status | Substantially enforced, but still PARTIAL outside loot: existing Enchanted stacks, creative/command acquisition, exact-item third-party recipes, and wild garlic are not rewritten. |
| Migration impact | Existing Enchanted crops/stacks remain loadable. Future Enchanted crop/block loot routes to the canonical item through the global loot modifier only while canonicalization is enabled. |
| API/code finding | JAR resources and registered classes were inspected; no Vampirism/Enchanted internal method is invoked for conversion. |

## Mandrake

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:mandrake |
| Candidates | enchanted:mandrake_root, feywild:mandrake, and feywild:mandrake_root. |
| Decision | KEEP_DISTINCT. |
| Reason | Enchanted's witchcraft root/seed chain and Feywild's fae crop/summoning chain are not interchangeable despite the shared folklore name. |
| Runtime policy | No canonical item is selected and no acquisition is redirected. Feywild mandrake items participate in the FAE semantic tag; that does not make them Enchanted ingredients. |
| Implementation status | Complete KEEP_DISTINCT decision. |
| Migration impact | None. |
| API/code finding | Atlas recipes/resources and registry classes were sufficient; no Java integration. |

## Supernatural blood

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:vampire_blood |
| Candidates | Vampirism vampire/blood bottles, Vampirism pure-blood tag members, and kaleidoscope_bloodwine:blood_bucket. |
| Decision | INTEROPERABILITY_ONLY with vampirism:vampire_blood_bottle as the representative supernatural blood. |
| Reason | Kaleidoscope Bloodwine already deliberately selects Vampirism blood when available while retaining its own fluid/container semantics. A bucket and a faction blood bottle are not safe stack substitutes. |
| Runtime policy | Optional items/tags share the VAMPIRE_BLOOD trait. No loot or inventory replacement. |
| Implementation status | Complete semantic bridge; container behavior remains mod-owned. |
| Migration impact | None. |
| API/code finding | Tags/resources only. |

## Chupacabra

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:chupacabra |
| Candidates | cnc:chupacabra and mobs_of_mythology:chupacabra. |
| Decision | FULL_CANONICALIZATION to Critters n' Crawlers; SUPPRESS_DUPLICATE_SPAWN for Mobs of Mythology. |
| Reason | The pack selected the Critters n' Crawlers implementation for curated cryptid ecology. Keeping both as ordinary natural predators would duplicate the encounter. |
| Runtime policy | While canonicalization is enabled, ConfigurableRemoveSpawnsBiomeModifier removes the Mobs of Mythology entity from every category of Overworld biome natural-spawn lists. Its canonicalization_suppression profile also rejects NATURAL position checks when the spawn director is enabled. cnc:chupacabra remains a rare nocturnal profile. |
| Implementation status | Complete for natural spawning while canonicalization is enabled; both Core suppression paths are bypassable by configuration. |
| Migration impact | Existing Mobs of Mythology chupacabras and deliberate spawner/command/structure use remain valid. |
| API/code finding | Registry/data and standard spawn events only; no external entity class calls. |

## Wendigo

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:wendigo |
| Candidates | cnc:wendigo was the only verified installed implementation. |
| Decision | CANONICAL. |
| Reason | No duplicate exists in the Atlas registry scan. |
| Runtime policy | Very rare, nocturnal, natural profile with a 1.5 world-event multiplier. |
| Implementation status | Complete canonical classification; no replacement needed. |
| Migration impact | None. |
| API/code finding | Registry ID and standard events only. |

## Imp

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:imp |
| Candidates | mythsandlegends:imp and fangs_n_claws:imp. |
| Decision | FULL_CANONICALIZATION to Myths & Legends; SUPPRESS_DUPLICATE_SPAWN for Fangs. |
| Reason | Myths & Legends owns the enabled Nether ecology in the supplied pack. Fangs already disables its duplicate in configuration. |
| Runtime policy | The Myths & Legends imp remains a rare natural profile. Fangs independently has allow_imp=false in the supplied config. Its Core profile adds a canonicalization_suppression NATURAL check while canonicalization and the spawn director are enabled. |
| Implementation status | Complete for the audited pack's natural ecology; the Core layer is configurable and the provider config remains separately authoritative. |
| Migration impact | Existing and deliberate Fangs imps remain valid. |
| API/code finding | Registry/config/data only. |

## Ghost

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:ghost |
| Candidates | Fangs ghost/fire ghost and vampirism:ghost. |
| Decision | KEEP_DISTINCT. |
| Reason | The Fangs entities are wandering monster variants; Vampirism's ghost is tied to its faction/boss content. Shared naming does not establish replacement equivalence. |
| Runtime policy | All are classified as ghosts/spirits. The supplied Fangs config independently disables natural ghost and fire-ghost spawning. The Core additionally gives the base Fangs ghost a canonicalization_suppression NATURAL profile, active only while canonicalization and the spawn director are enabled. That curation does not merge it into Vampirism's ghost. |
| Implementation status | Complete KEEP_DISTINCT decision. |
| Migration impact | None; disabled natural entries remain summonable. |
| API/code finding | Tags and config only. |

## Wraith

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:wraith |
| Candidates | eidolon_repraised:wraith and graveyard:wraith. |
| Decision | KEEP_DISTINCT. |
| Reason | Eidolon's chilled ritual-linked undead and Graveyard's light-extinguishing grave spirit have different behavior and ownership. |
| Runtime policy | Both receive spirit/undead classification and separate Field Guide descriptions. Eidolon has a rare nocturnal profile; Graveyard's supplied config has its wraith disabled. No ID substitution occurs. |
| Implementation status | Complete KEEP_DISTINCT decision. |
| Migration impact | None. |
| API/code finding | JAR/config/resources only. |

## Altars

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:altar |
| Candidates | enchanted:altar, graveyard:altar, werewolves:stone_altar, and eidolon_repraised:stone_altar. |
| Decision | KEEP_DISTINCT. |
| Reason | Each is owned by a different ritual, structure, or faction system. |
| Runtime policy | Concept grouping only; no canonical registry ID. |
| Implementation status | Complete KEEP_DISTINCT decision. |
| Migration impact | None. |
| API/code finding | Name/resource comparison only. |

## Poppets

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:poppet |
| Candidates | enchanted:poppet and malum:poppet. |
| Decision | KEEP_DISTINCT. |
| Reason | Enchanted's poppet is functional in its system; the audited Malum item does not establish equivalent behavior and is reserved for its own/future system. |
| Runtime policy | Concept grouping only; no recipe or item substitution. |
| Implementation status | Complete KEEP_DISTINCT decision. |
| Migration impact | None. |
| API/code finding | Atlas/JAR resource audit only. |

## Golems

| Item | Finding |
| --- | --- |
| Concept | darkfolklore:golem |
| Candidates | Fangs golem/ice golem and occultism:iesnium_golem. |
| Decision | KEEP_DISTINCT. |
| Reason | Fangs supplies hostile monster encounters; Occultism supplies a ritual-created familiar/entity in the misc category. They are not alternate skins for one mechanic. |
| Runtime policy | Shared construct classification only. |
| Implementation status | Complete KEEP_DISTINCT decision. |
| Migration impact | None. |
| API/code finding | Entity registry/resource audit only. |

## Residual risks and deferred work

- Garlic is not a universal ingredient substitution layer. Recipe JSON that hardcodes enchanted:garlic or vampirism:garlic retains its owner's behavior.
- Silver worldgen removal covers the four exact placed-feature IDs found in this pack. A provider update can add or rename features and must be re-audited.
- Global loot routing is config-gated and intentionally not an inventory migration. Machines or code paths that create stacks without a loot table may still emit legacy silver.
- Core spawn suppression is config-gated and intentionally limited to biome natural-spawn lists/MobSpawnType.NATURAL; it does not cancel scripted encounters or delete existing entities. Provider-owned spawn config can still suppress an entity independently.
- Disabling Core canonicalization does not undo AlmostUnified recipes, provider configuration, existing chunks, or prior loot conversions.
- No Atlas candidate not listed above was automatically canonicalized. Similar names remain unchanged pending semantic evidence.
