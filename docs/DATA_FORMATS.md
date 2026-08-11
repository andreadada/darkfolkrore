# Dark Folklore data formats

## Atomic reload roots

`FolkloreDataManager` scans these directories across the active server datapack stack:

```text
data/<namespace>/darkfolklore/canonical/*.json
data/<namespace>/darkfolklore/weaknesses/*.json
data/<namespace>/darkfolklore/spawn_profiles/*.json
data/<namespace>/darkfolklore/magic_integrations/*.json
data/<namespace>/darkfolklore/story_templates/*.json
data/<namespace>/darkfolklore/organization_archetypes/*.json
data/<namespace>/darkfolklore/social_parameters/*.json
data/<namespace>/darkfolklore/political_weights/*.json
```

The bundled namespace is `darkfolklore`, producing paths such as `data/darkfolklore/darkfolklore/story_templates/...`. Definitions may come from another namespace. Normal resource-location override priority applies before Core receives the scan result.

Enum strings are case-insensitive (`Locale.ROOT` uppercase). Unknown JSON fields are ignored and the Gson parser is lenient, but record validation is strict. There is no JSON `format_version` field in 0.2.

Reload is one transaction across all eight directories:

1. Every file is parsed into candidate collections.
2. Cross-record invariants are checked.
3. Only a candidate with zero errors becomes one immutable state snapshot.
4. Any error rejects the complete candidate and retains the previous validated snapshot.

`/folklore diagnostics` reports the latest candidate's `invalid` count. Correct the offending resources and reload again. On initial startup with invalid data and no previous snapshot, the Core definition state remains empty.

## Canonical definitions

```json
{
  "concept": "darkfolklore:wolfsbane",
  "kind": "ITEM",
  "canonical": "enchanted:wolfsbane_flower",
  "implementations": ["werewolves:wolfsbane"],
  "policy": "FULL_CANONICALIZATION",
  "reason": "Audited ownership and compatibility rationale."
}
```

| Field | Type | Default | Validation/meaning |
| --- | --- | --- | --- |
| `concept` | string | file resource ID | Nonblank namespaced shared concept. |
| `kind` | enum | `CONCEPT` | `ITEM`, `ENTITY`, `FLUID`, or `CONCEPT`. |
| `canonical` | string | empty | Preferred registry ID; required except for `KEEP_DISTINCT` and `DEFERRED_UNSAFE`. |
| `implementations` | string array | empty | Registry IDs resolving to the concept. |
| `policy` | enum | `CANONICAL` | Audit/enforcement intent. |
| `reason` | string | empty | Human-readable rationale. |

Policies are `CANONICAL`, `FULL_CANONICALIZATION`, `INTEROPERABILITY_ONLY`, `KEEP_DISTINCT`, `SUPPRESS_DUPLICATE_SPAWN`, `KEEP_RARE`, `EVENT_ONLY`, `COMPATIBILITY_ONLY`, `HIDDEN_FROM_NORMAL_ACQUISITION`, and `DEFERRED_UNSAFE`.

The registry rejects duplicate concept IDs and one implementation ID claimed by multiple concepts. A policy declaration is not a universal stack transformer: concrete enforcement comes from audited tags, recipes, loot modifiers, biome modifiers, spawn filters, external unifiers, or exact runtime bridges.

## Weakness rules

```json
{
  "id": "darkfolklore:silver_vs_werewolf",
  "target_traits": ["WEREWOLF"],
  "item_traits": ["SILVER_WEAPON"],
  "multiplier": 1.5,
  "native_provider_namespaces": ["werewolves"],
  "priority": 100
}
```

| Field | Default | Validation/meaning |
| --- | --- | --- |
| `id` | file resource ID | Nonblank and unique. |
| `target_traits` | empty | Nonempty creature-trait set; all must match. |
| `item_traits` | empty | Nonempty item-trait set; all must match. |
| `multiplier` | `1.0` | Finite, greater than 0, at most 16. |
| `native_provider_namespaces` | empty | Target namespaces for which Core skips the rule to avoid native double application. |
| `priority` | `0` | Higher values are evaluated first; only the first matching rule applies. |

Trait names map to `darkfolklore` item/entity-type tags. The Java enums are the authoritative accepted list; shipped examples cover vampire, werewolf, fae, spirit, and undead weaknesses.

## Spawn profiles

```json
{
  "entity": "cnc:wendigo",
  "rarity": "VERY_RARE",
  "natural_spawn_enabled": true,
  "canonicalization_suppression": false,
  "nocturnal": true,
  "event_multiplier": 1.5
}
```

| Field | Default | Validation/meaning |
| --- | --- | --- |
| `entity` | file resource ID | Namespaced registry ID; only one profile per entity. |
| `rarity` | `RARE` | `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`, or `LEGENDARY`. |
| `natural_spawn_enabled` | `true` | False rejects matching `NATURAL` position checks. |
| `canonicalization_suppression` | `false` | Makes a false natural flag conditional on `canonicalization=true`. |
| `nocturnal` | `false` | Rejects the natural attempt outside night. |
| `event_multiplier` | `1.0` | Finite 0..10 multiplier while a Core world event is active. |

Base rarity chances are 1.0, 0.65, 0.30, 0.12, and 0.04 respectively. Global spawn multiplier, active event, and encounter pressure modify the chance before capping. Profiles affect only `MobSpawnType.NATURAL`, not command, egg, spawner, ritual, structure, conversion, or boss creation.

## Magic integrations

```json
{
  "id": "darkfolklore:blood_soul_rite",
  "traditions": ["SPIRIT", "SOUL", "FORBIDDEN_THEURGY"],
  "required_traits": ["VAMPIRE_BLOOD", "SOUL", "RITUAL_COMPONENT"],
  "knowledge_reward": "darkfolklore:forbidden_lore",
  "knowledge_points": 10
}
```

`id` must be namespaced and unique. At least two distinct traditions and one required trait are required. `knowledge_points` is clamped to 0..100. The shipped traditions are `WITCHCRAFT`, `SPIRIT`, `SOUL`, `FORBIDDEN_THEURGY`, and `FAE`. This format grants lore when the inventory trait combination is recognized; it does not create a native ritual or recipe.

## Story templates

```json
{
  "id": "darkfolklore:supernatural_royal_scandal",
  "trigger": "POLITICAL_EXPOSURE",
  "concept": "*",
  "weight": 2,
  "cooldown_ticks": 72000,
  "lifetime_ticks": 168000,
  "required_secret": "VAMPIRE",
  "capital_only": true,
  "contract_eligible": false,
  "enabled": true
}
```

| Field | Default | Validation/meaning |
| --- | --- | --- |
| `id` | file resource ID | Nonblank and unique. |
| `trigger` | `WITCHING_HOUR` | One of the triggers below. |
| `concept` | `*` | Fixed story concept; blank/`*` uses the event concept. |
| `weight` | `1` | Integer 1..1000. |
| `cooldown_ticks` | `24000` | Nonnegative per-template/per-region creation cooldown. |
| `lifetime_ticks` | `144000` | At least 200 ticks. |
| `required_secret` | absent | Optional exact `SecretType` filter. |
| `capital_only` | `false` | Requires the caller to provide verified capital context. |
| `contract_eligible` | `false` | Explicitly allows an `INCIDENT` story to be offered as a contract. |
| `enabled` | `true` | Excludes the template when false. |

Triggers are `FAMILY_DISCOVERY`, `PUBLIC_REVEAL`, `HUNTER_INVESTIGATION`, `ORGANIZATION_RECRUITMENT`, `FULL_MOON_INCIDENT`, `WITCHING_HOUR`, `CONTROLLED_FALSE_ACCUSATION`, and `POLITICAL_EXPOSURE`.

The runtime filters enabled/trigger/capital/secret/cooldown, rolls `dynamicStoryRate`, then selects by weight. A template defines narrative eligibility, not provider truth.

## Organization archetypes

```json
{
  "type": "HUNTER_SOCIETY",
  "base_influence": 10,
  "max_members": 48,
  "auto_found": false,
  "public_reveal_authority": true,
  "objectives": ["PROTECT_COMMUNITY", "INVESTIGATE_SUPERNATURAL"]
}
```

`type` is one of `VAMPIRE_COVEN`, `HUNTER_SOCIETY`, `WEREWOLF_PACK`, or `WITCH_COVEN`, and may appear only once. `base_influence` is 0..100; `max_members` is 1..256; objectives must be nonempty. Valid objectives are `PROTECT_COMMUNITY`, `INVESTIGATE_SUPERNATURAL`, `PROTECT_MEMBERS`, `DEFEND_TERRITORY`, `GROW_INFLUENCE`, `PRESERVE_SECRETS`, and `STUDY_OCCULT`.

`auto_found` governs factual social NPC join behavior. Hunter societies ship with it false and instead form from regional suspicion/witnesses. `public_reveal_authority` allows an organization report to attempt the independent credible-witness threshold; it does not make one member's claim public by itself.

## Social parameters

Only one definition may exist across the active resource stack:

```json
{
  "self": 0.30,
  "spouse": 0.25,
  "parent_or_child": 0.22,
  "sibling": 0.18,
  "player_friend": 0.12,
  "player_bounty_target": -0.20
}
```

All six numbers must be finite. They are additive trust contributions for relationships the exact MCA 7.7.32 adapter can verify. Absence uses the same built-in defaults. The format intentionally has no generic NPC friend/enemy fields because that API is not available in the audited MCA version.

## Political weights

```json
{
  "role": "SOVEREIGN",
  "credibility": 0.28,
  "organization_response": 0.32,
  "investigation_priority": 0.32,
  "public_awareness": 0.28
}
```

Each semantic `PoliticalRole` may appear once. Every weight must be finite and in 0..1. Missing role definitions use `PoliticalWeightModel` defaults. Weights only operate on verified MCA Capitals 1.1.0 context and existing knowledge; they never create facts or offices. Exact accepted titles/roles are documented in [MCA Capitals](MCA_CAPITALS.md).

## Standard tags and provider recipe overrides

Core resolves semantic item/entity traits from normal tags:

```text
data/<namespace>/tags/item/<trait>.json
data/<namespace>/tags/entity_type/<trait>.json
```

Optional concrete members should use `"required": false`. Bundled data can also extend provider-owned tags, such as Werewolves silver tools or Fangs 'n Claws werewolf bane.

Exact provider recipes under `data/werewolves/recipe/` use NeoForge mod-loaded conditions and the semantic `#darkfolklore:wolfsbane` ingredient where the Werewolves 2.0.3.3 audit supports it. These are ordinary recipe resources, not Core atomic definitions. See [Wolfsbane audit](WOLFSBANE_AUDIT.md).

## Canonicalization codecs

These formats are registered with NeoForge and are outside the eight-directory definition transaction. All consult `canonicalization`; semantic lookup/tags remain available when enforcement is off.

### Generated-loot replacement

```json
{
  "type": "darkfolklore:canonicalize_items",
  "conditions": [],
  "replacements": {
    "werewolves:wolfsbane": "enchanted:wolfsbane_flower",
    "occultism:silver_ingot": "immersiveengineering:ingot_silver"
  }
}
```

The global loot modifier replaces newly generated mapped stacks while preserving count and component patch. A missing target leaves the original. It does not rewrite existing inventories, direct Java creation, trades, commands, or registries.

### Config-aware placed-feature removal

```json
{
  "type": "darkfolklore:remove_features_when_canonicalization_enabled",
  "biomes": "#minecraft:is_overworld",
  "features": "#darkfolklore:noncanonical_wolfsbane",
  "step": "vegetal_decoration"
}
```

`biomes`/`features` are holder sets and `step` is a generation decoration value. Removal occurs only in NeoForge's `REMOVE` phase when canonicalization is enabled and any resource conditions pass. It affects new biome generation, not existing chunks.

### Config-aware biome-spawn removal

```json
{
  "type": "darkfolklore:remove_spawns_when_canonicalization_enabled",
  "biomes": "#minecraft:is_overworld",
  "entity_types": "#darkfolklore:noncanonical_natural_spawn"
}
```

This removes matching biome spawn entries in the `REMOVE` phase. It does not cancel non-natural creation paths.

## Field Guide 1.14.0 categories

Field Guide owns `data/<namespace>/fieldguide/categories/*.json`; Core does not parse these files in `FolkloreDataManager`. The curated format embeds explicit entries:

```json
{
  "sort_index": 20,
  "icon": "fieldguide:textures/gui/icons/wither.png",
  "contents": [
    {
      "type": "entry",
      "id": "entity:vampirism/vampire",
      "canonical_concept": "darkfolklore:vampire",
      "unlock": {"triggers": ["scan", "kill"]}
    }
  ]
}
```

`canonical_concept` is Core validator metadata ignored by Field Guide's tolerant loader. Field Guide resolves entity IDs through the live registry and omits provider-absent entries. The source validator checks category IDs, entity mappings, concepts, unlocks, English/Italian text, icons, target categories, orphan localization, duplicates, and empty visible pages. Exact 1.14.0 behavior is documented in [Field Guide](FIELD_GUIDE.md).

## Saved-data schema 2

Core anchors overworld `SavedData` file ID `darkfolklore_society`. Root integer `schema` is `2`; the following fields are compound lists:

| Root list | Row fields |
| --- | --- |
| `lore` | `player`, `concept`, `points` |
| `social` | `observer`, `subject`, `secret`, `state`, `confidence`, `source`, `time`, optional `evidence` |
| `public_secrets` | `subject`, `secret`, `time` |
| `family_reactions` | `observer`, `subject`, `secret`, `reaction` |
| `reputation` | `holder`, `faction`, `value` |
| `organizations` | `id`, `type`, `name`, `leader`, `home`, `influence`; string `members`; compound `member_seen`, `intelligence`, `relations`, `events`; string `objectives` |
| `villages` | `key`, `awareness`, `vampire`, `hunter`, `werewolf`, `witch`, `fear`, `suspicion`, `political` |
| `lineages` | `descendant`, `source`, `type`, `time` |
| `evidence` | `id`, `type`, `concept`, optional `subject`, position, `created`, `expires`, optional `collected` |
| `contracts` | `id`, `player`, `issuer`, `concept`, `expires`, `status`, `village`, `required`, position, string `clues` |
| `stories` | `id`, `template`, `concept`, `created`, `expires`, `status`, `village`, position, string `actors` |
| `encounters` | `player`, `pressure` |
| `rumor_silence` | `witness`, `until` |

Position fields are `dimension`, `x`, `y`, and `z`. UUIDs/enums are strings. Organization nested rows are:

- `member_seen`: `member`, `time`;
- `intelligence`: `subject`, `secret`, `state`;
- `relations`: `organization`, `relation`;
- `events`: `type`, `time`, optional `actor`/`subject`, `detail`.

Every root row is decoded independently. A runtime error logs and skips that row while other rows/lists survive. Unknown root fields are ignored. Organization nested collection loads enforce the hard member/intelligence/relation/event limits. Public claims and rumor-silence rows have a 50,000 hard load/runtime ceiling.

### Schema-1 migration

Schema 1 already used `lore`, `social`, `reputation`, basic `organizations`, `villages`, `lineages`, `evidence`, `contracts`, `stories`, and `encounters`. On load into 0.2:

- those rows retain their existing shape and values;
- absent public/family/silence lists become empty;
- old organizations retain safe type defaults for objectives, receive zero last-seen timestamps for retained members, and receive empty intelligence/relations/events;
- membership indexes are rebuilt;
- the save is marked dirty and the next write emits schema 2.

Reopening the written schema-2 save does not rerun migration, making the upgrade idempotent. A schema newer than 2 logs a warning and receives best-effort read only; Core has no downgrade writer. Back up worlds before changing versions.

### Maintenance and caps

Every 1,200 ticks Core decays/prunes rumors, expires evidence, removes expired rumor-silence rows, and removes terminal contracts/stories after the configured retention window. Organization maintenance enforces the configurable social limit and a derived public-claim limit while organizations are enabled. Organizations, member/intelligence/relation/event collections, public claims, and rumor-silence rows also have insertion/load caps. Encounter pressure is clamped to 0..100; zero removes the row.

The 53-test JUnit suite includes schema-2 round trips and an idempotent schema-1 organization migration fixture. Keep a backed-up real-world upgrade in the release smoke matrix because the fixture does not exercise every foreign attachment in a modpack save.
