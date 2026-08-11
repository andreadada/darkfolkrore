# Canonicalization

Dark Folklore separates a folklore concept from the registry object that currently implements it. For example, darkfolklore:werewolf is the stable concept; werewolves:human_werewolf is one implementation. This lets lore, contracts, stories, and diagnostics refer to a stable concept without registering a duplicate creature.

Canonicalization is not registry replacement. Foreign entries remain registered so old worlds, commands, spawn eggs, recipes, and mod-owned logic can still resolve them.

## Definitions

Definitions live under data/<namespace>/darkfolklore/canonical/*.json and are loaded on server data reload. Each record contains:

| Field | Meaning |
| --- | --- |
| concept | Stable namespaced Dark Folklore concept ID. |
| kind | ENTITY, ITEM, or CONCEPT. |
| canonical | Preferred registry ID, or an empty string only for KEEP_DISTINCT/DEFERRED_UNSAFE. |
| implementations | Other audited registry IDs belonging to or related through that concept. |
| policy | The semantic policy selected after audit. |
| reason | Human-readable decision evidence. |

CanonicalRegistry builds one concept index and one implementation index. Duplicate concepts or an implementation assigned to two concepts reject the reload rather than produce an ambiguous resolver.

The public facade resolves an EntityType or ItemStack to its CanonicalDefinition. Resolution reports metadata; it does not mutate the entity or stack.

## Policy meanings

| Policy | Meaning in this project |
| --- | --- |
| CANONICAL | One verified implementation exists and is the preferred representation. |
| FULL_CANONICALIZATION | Normal future acquisition or natural ecology is directed to the canonical implementation while legacy entries remain loadable. |
| INTEROPERABILITY_ONLY | Implementations stay distinct, but semantic tags and shared systems let them participate in common mechanics. |
| KEEP_DISTINCT | A shared English name does not mean mechanical equivalence. No canonical registry object is selected. |
| SUPPRESS_DUPLICATE_SPAWN | A duplicate stays registered but its natural spawn path is removed. |
| KEEP_RARE | The implementation is retained with curated rarity. |
| EVENT_ONLY | Normal spawning/acquisition is withheld for encounter use. |
| COMPATIBILITY_ONLY | The entry exists to bridge another system rather than become a visible canonical object. |
| HIDDEN_FROM_NORMAL_ACQUISITION | The entry stays registered for save safety but new ordinary acquisition is routed elsewhere. |
| DEFERRED_UNSAFE | No change is made because the audit did not establish a safe replacement. |

Not every enum value is used by the 0.2.0 defaults. The detailed audit records the selected outcomes and actual enforcement status.

## Enforcement layers

Canonical metadata alone has no destructive effect. The current pack uses several independent, auditable layers:

1. AlmostUnified independently rewrites silver recipes to the Immersive Engineering material family.
2. The Core-registered darkfolklore:remove_features_when_canonicalization_enabled biome-modifier codec removes four optional noncanonical silver placed features from the Overworld underground_ores step when canonicalization is enabled.
3. The config-aware darkfolklore:canonicalize_items global loot modifier converts 22 audited Eidolon, Occultism, and Werewolves base silver loot forms to their Immersive Engineering equivalents.
4. The same loot modifier converts newly generated enchanted:garlic loot to vampirism:garlic.
5. The Core-registered darkfolklore:remove_spawns_when_canonicalization_enabled biome-modifier codec removes mobs_of_mythology:chupacabra from Overworld natural spawn lists when canonicalization is enabled.
6. Spawn profiles marked canonicalization_suppression reject only NATURAL spawns for audited duplicates such as the Fangs werewolf/imp/ghost and Mobs of Mythology chupacabra while both the spawn director and canonicalization are enabled.
7. Semantic item and entity tags feed traits, lore, Field Guide categories, weaknesses, contracts, and stories.

All optional entries in tags and placed-feature/entity-type tags use required: false. The loot modifier skips a replacement if its canonical target is absent. Converted stacks preserve their count and component patch.

## Master toggle

The common canonicalization toggle defaults to true. It gates every Core-owned mutating/suppressing canonicalization path:

- CanonicalItemLootModifier returns generated loot unchanged when the toggle is false.
- ConfigurableRemoveFeaturesBiomeModifier and ConfigurableRemoveSpawnsBiomeModifier perform work only in NeoForge's REMOVE phase while the toggle is true.
- SpawnDirector ignores natural_spawn_enabled=false for profiles marked canonicalization_suppression when the toggle is false.

The toggle does not unload canonical definitions or semantic tags, disable lore resolution, rewrite foreign mod configuration, or turn off AlmostUnified's independent recipe unification. The spawn-profile layer also requires the separate spawnDirector toggle. Biome-modifier decisions affect subsequently constructed generation/spawn settings; no toggle setting removes ore already placed in chunks or entities already spawned.

## Safe migration rules

- No foreign registry entry is removed or remapped.
- Existing stacks are not scanned or converted.
- Existing placed ores and already spawned entities are not deleted.
- Creative inventory, commands, and deliberate non-natural spawn mechanisms can still access legacy objects.
- Core loot routing affects newly generated loot only and is bypassed when canonicalization is disabled.
- Core spawn suppression affects biome natural-spawn lists and/or MobSpawnType.NATURAL only while canonicalization is enabled; spawners, structures, rituals, commands, and boss/event spawning are left to their owners.
- KEEP_DISTINCT concepts are never treated as item substitutions merely because names match.

These constraints trade absolute cleanup for save compatibility and predictable ownership.

## Semantic traits

The canonical layer supplies item concepts such as SILVER, SILVER_WEAPON, GARLIC, WOLFSBANE, COLD_IRON, HOLY, SPIRITUAL, SOUL, FAE, RITUAL_COMPONENT, and ARCHAEOLOGICAL_LORE. Creature concepts include VAMPIRE, WEREWOLF, HUNTER, UNDEAD, SPIRIT, FAE, CRYPTID, DEMON, CONSTRUCT, and related categories.

Traits come from darkfolklore tags plus exact compatibility adapters. Weakness rules consume traits rather than hardcoding every item/entity pair. The silver-versus-werewolf rule excludes the werewolves entity namespace because that mod already owns its native silver behavior; this avoids adding the Core multiplier to the native Werewolves implementation.

## Reload and validation

Canonical, weakness, spawn-profile, and magic-integration JSON are parsed on server resource reload. Bad enum values, malformed IDs, duplicate concepts, ambiguous implementations, invalid multipliers, and invalid profile ranges are reported. Valid definitions replace the previous immutable registries together after preparation.

Datapack authors may override the defaults, but should preserve these rules:

- Use real registry IDs confirmed against the installed JAR.
- Keep implementation lists semantically narrow.
- Use KEEP_DISTINCT when two systems merely share a folklore word.
- Put gameplay enforcement in explicit tags, recipes, loot modifiers, biome modifiers, or a documented adapter; do not imply that a metadata policy alone performs a migration.
- Never list the same registry ID under two concepts.

See CANONICALIZATION_AUDIT.md for each Atlas conflict, its risk, and the precise runtime behavior.
