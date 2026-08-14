# Dark Folklore 0.6 — Supernatural World Loop

## Goal

0.6 connects the existing compatibility, knowledge, investigation, society, lifecycle and recipe systems into one coherent loop:

```text
provider FACT
 -> incident / sighting
 -> witnesses + rumors
 -> player investigation
 -> occult analysis / testimony
 -> progressive lore dossier
 -> preparation / cross-mod crafting
 -> hunt, cure or resolution
 -> village / organization reaction
 -> durable historical belief
```

Dark Folklore remains an integration/orchestration layer. It does **not** become a second Vampirism, MCA, Werewolves, Enchanted, Occultism, Malum, Eidolon or Feywild implementation.

## Ownership boundary

Provider-owned FACT remains authoritative for:

- MCA family/social identity;
- vampire/werewolf/hunter factual state;
- infection, conversion, cure and inheritance;
- provider capability persistence;
- native MCA vampire target selection/navigation/AI;
- provider station/ritual mechanics and skill gates.

Dark Folklore owns:

- evidence and incident continuity;
- observer knowledge, rumors and public reveal;
- player lore progression;
- contracts and investigation orchestration;
- cross-mod recipe-safe interoperability;
- village pressure/response presentation;
- bounded wild-vampire hunt steering where Vampirism itself exposes a normal Mob target;
- diagnostics and Atlas-driven audit tooling.

## Vampire predation state machine

A bounded runtime session now exposes explicit phases:

```text
TARGET_SELECTED
  -> PURSUING / STALKING
  -> ATTACKING
  -> FEEDING

any active phase -> ABORTED
```

The session is never persisted. It cannot force-load chunks or teleport a target.

Wild Vampirism mobs may receive a temporary MCA civilian target only when:

- Vampirism accepts the victim as a blood source;
- the vampire is hungry;
- Dark Folklore social policy accepts the victim;
- the vampire does not already own another live combat target.

Converted MCA vampires remain provider-owned: Core observes provider-native target selection and cannot redirect it.

### Environment

Autonomous predation is allowed:

- at night; or
- during daytime while the predator is sheltered from open sky.

Open-sky daytime exposure aborts the Dark Folklore session. This conservative rule does not infer provider sunscreen/equipment immunity.

### Trace command

`/folklore predation trace <entity>` reports:

- predator kind and current phase;
- day / sky exposure / environment gate;
- provider feeding pressure;
- local and personal social risk;
- selected target and director reason;
- up to 16 nearby candidate prey entries with provider eligibility, witnesses, distance, score and rejection reason.

Trace is read-only.

## Failure isolation

The exact Vampirism/MCA predation adapter now has independent runtime circuits:

- wild feeding;
- MCA factual snapshot reads;
- MCA target eligibility;
- MCA animal feeding extension;
- native MCA bite correlation.

A runtime/linkage failure opens only the affected circuit. Other capabilities remain usable. Every failed circuit still fails closed and logs once. Runtime circuit state resets on server shutdown; constructor/version-gate failure still rejects the adapter as a whole.

## Progressive knowledge dossier

The existing `UNKNOWN -> DISCOVERED -> OBSERVED -> STUDIED -> MASTERED` lore stages now map to explicit information facets.

- `UNKNOWN`: nothing.
- `DISCOVERED`: existence.
- `OBSERVED`: existence, signs, behavior.
- `STUDIED`: identity, feeding habits, weaknesses, countermeasures and cure knowledge become actionable.
- `MASTERED`: origin and bloodline-level lore are also available.

This deliberately preserves the existing rule that OBSERVED knowledge must not leak weaknesses.

Admin diagnostic:

`/folklore knowledge dossier <player> <concept>`

Field Guide remains the provider UI and binary implementation unlock authority. Dark Folklore continues to synchronize observed implementation unlocks while its own dossier controls what its gameplay systems may reveal.

## Deep-magic disciplines

0.6 adds a semantic gameplay vocabulary above provider mods without treating their items as equivalent:

| Discipline | Primary provider | Example uses |
|---|---|---|
| WITCHCRAFT | Enchanted | analysis, countermeasures, curses, ritual catalysis |
| SPIRITUALISM | Occultism | spirits, summoning, tracking, soul reading |
| SOUL_MAGIC | Malum | soul reading, tracking, occult analysis |
| NECROMANCY | Eidolon: Repraised | forbidden lore, soul/curse analysis |
| FAE_MAGIC | Feywild | glamour/fae lore and tracking |
| BLOOD_MAGIC | Bloodlines-compatible content | blood reading and ritual work |
| RITUAL_MAGIC | cross-provider | common ritual-catalysis vocabulary |

Obtaining an audited investigation/ritual tool can discover the corresponding discipline lore. This is additive knowledge progression only; provider rituals remain provider-owned.

Commands:

- `/folklore magic disciplines`
- `/folklore magic inspect-held`

## Visible village response

Persisted society pressure is projected into one read-only response tier:

```text
CALM -> UNEASY -> ALERT -> MOBILIZED -> LOCKDOWN
                           \
                            -> COMPROMISED (vampire dominance)
```

The calculation combines hunter readiness, public awareness, suspicion, fear, political importance and supernatural influence. Players receive an edge-triggered message when entering a non-calm response or when the local tier changes.

This does not manufacture hunter/vampire faction members or mutate provider AI.

Admin diagnostic:

`/folklore village response <player>`

## Atlas recipe graph

`tools/atlas_recipe_audit.py <scan-dir>` converts Atlas outputs into a deterministic cross-mod report:

- cross-mod ingredient -> recipe namespace edges;
- strongest existing mod-to-mod recipe bridges;
- duplicate candidates with asymmetric recipe usage;
- producerless duplicate candidates;
- high-risk / undecided canonicalization candidates;
- recipe-isolated namespaces.

The report is advisory. It never rewrites recipes and never treats same-name similarity as equivalence.

`tools/verify_reference_pack.py <scan-dir>` checks Atlas-reported provider versions against the exact audited stack. Version match is necessary but is not a substitute for binary hash or in-world runtime tests.

## Canonicalization policy

Three decisions remain mandatory:

- `EQUIVALENT`: recipe-safe substitution and reviewed canonical acquisition/output may be allowed.
- `INTEROPERABILITY_ONLY`: selected recipes/mechanics may share a narrow tag; items remain distinct.
- `KEEP_DISTINCT`: no substitution.

Semantic tags are always broader than recipe-safe equivalence tags. AlmostUnified remains owner of base-material recipe unification where already configured.

## Performance and persistence

0.6 keeps the existing constraints:

- no tick-wide world scans;
- no chunk forcing;
- staggered/bounded entity work;
- bounded trace/session caches;
- durable consequences persisted, optional runtime orchestration not persisted;
- server stop clears all runtime session/trace/response state.
