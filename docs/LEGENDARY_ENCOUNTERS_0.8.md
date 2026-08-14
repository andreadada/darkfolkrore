# Dark Folklore 0.8 — Legendary Encounters & Supernatural Ecology

## Goal

0.8 turns selected rare provider creatures into **persistent supernatural events** instead of treating them as ordinary random mobs. Dark Folklore owns origin, omen, investigation, manifestation provenance and social consequences; the provider mod continues to own the actual entity implementation and combat mechanics.

No new Dark Folklore item, block, entity, sound event, texture or model is introduced by this release.

## Core state machine

```text
ORIGIN -> OMENS -> ELIGIBLE -> MANIFESTED -> ACTIVE -> RESOLVED
                  \                         \-> ESCAPED / EXPIRED
```

`LegendaryEncounterSavedData` schema 2 persists the encounter independently from the physical entity. Each row records definition/concept/provider implementation, rank, origin, region/anchor, timestamps, omen count, exact linked story UUID, manifestation UUID, bounded group participants, optional narrative person snapshot and resolution. Data is bounded and pruned.

The manifestation is always a provider `EntityType`; Dark Folklore does not register replacement copies. Encounter creation fails closed when the required provider entity is not registered. Wild Hunt also requires its provider follower type before the event can be admitted.

## Investigation continuity

Legendary encounters build evidence before a physical culprit exists, so they cannot use the legacy `subject + actor + +/-20 tick` continuity rule unchanged.

0.8 therefore keeps two exact phases for its own encounter story templates only:

1. **pre-manifestation omens** are subjectless but must belong to the exact story UUID, exact concept, same dimension, story lifetime and the bounded 18-block omen envelope;
2. **post-manifestation evidence** must use the exact provider manifestation UUID once one exists.

When a provider entity materializes, every already-accepted contract for the linked story is promoted from concept-only continuity to the exact manifestation UUID. This prevents a natural mob of the same concept from satisfying the wrong encounter contract. Legacy/non-encounter investigation continuity remains unchanged.

## Built-in encounters

### Wendigo — The Hunger in the Wilds

- implementation: `cnc:wendigo`
- concept: `darkfolklore:wendigo`
- rank: `LEGENDARY`
- origin: narrative missing traveller / starvation theme
- three omen stages: tracks, remains/bone evidence, blood
- night-only manifestation
- contract-eligible story: `darkfolklore:missing_traveller`

0.8 deliberately uses a narrative traveller. It does **not** discard, hide or transform an MCA person. A real MCA disappearance/conversion path is deferred until an audited MCA API can preserve marriage, family, village and persistence invariants.

### Chupacabra — Livestock Panic

- implementation: `cnc:chupacabra`
- concept: `darkfolklore:chupacabra`
- rank: `DREAD`
- origin: repeated suspicious nighttime livestock deaths in one social region
- evidence: blood, scent, tracks
- contract-eligible story: `darkfolklore:livestock_panic`

The regional counter is bounded and time-windowed. To avoid assigning another creature's kill to the Chupacabra narrative, it currently counts only nighttime deaths with no identifiable killer or a real `cnc:chupacabra`; player kills and tame-animal deaths are excluded. Full-pack testing remains the authority for tuning unexplained environmental-death frequency.

### Revenant — The Returned Dead

- implementation: `graveyard:revenant`
- concept: `darkfolklore:revenant`
- rank: `DREAD`
- origin: a **confirmed** violent villager/MCA death
- stores a narrative snapshot of the dead person and exact living killer UUID when known, including a player killer
- evidence: spirit echo, soul echo, bone
- contract-eligible story: `darkfolklore:returned_dead`

A Core-manifested Revenant may target its original loaded killer when that killer is nearby. The Revenant is a separate provider entity linked narratively to the dead person; Core never claims provider resurrection or rewrites MCA identity.

### Wild Hunt

- captain: `occultism:wild_hunt_wither_skeleton`
- followers: `occultism:wild_hunt_skeleton`
- concept: `darkfolklore:wild_hunt`
- rank: `LEGENDARY`
- origin: full-moon world omen
- evidence: occult signature, spirit echo, tracks
- not contract eligible

Core materializes a bounded local group and persists participant UUIDs. Loaded members without an active combat target receive ordinary navigation toward a deterministic regional direction. No chunk loading is performed. Provider AI may override this movement; real-pack testing is the authority.

## Omens and investigation

Omens use existing systems only:

- logical `EvidenceRecord`s;
- existing `EvidenceType`s;
- vanilla particles;
- action-bar messages;
- village fear/suspicion;
- existing persistent stories.

The existing `OccultInvestigationEngine` remains the forensics owner. 0.8 adds Revenant and Wild Hunt profiles and feeds encounter evidence into the same analysis/hypothesis/contract pipeline used by the rest of Core. It does not add a competing forensic engine.

## Wards

A ward is an explicit persisted Dark Folklore state centered on an existing door.

Creation:

1. sneak-right-click an existing door;
2. main hand must carry an existing item with `RITUAL_COMPONENT`;
3. offhand focus determines the ward tradition:
   - `GARLIC` -> vampire;
   - `HOLY` -> undead;
   - `SPIRITUAL` / `SOUL` -> spirit;
   - `FAE` -> fae;
   - multiple traditions, or a second plain `RITUAL_COMPONENT`, -> general;
4. one ritual component and one focus are consumed unless creative, **only after persistence accepts the ward**.

The upper/lower half of one door normalizes to one threshold. One threshold stores at most one ward: the same creator may refresh it, while another player cannot overwrite an active ward. A full ward store fails closed rather than evicting another player's protection.

No ward block is created. Ward data stores type, radius, strength, creator and expiry. Every creatable ward reaches its intended activation threshold; `GENERAL` is therefore not a dead/unreachable type.

Effects are intentionally limited to Dark Folklore-owned decisions:

- GENERAL wards can delay Wendigo/Chupacabra manifestations;
- SPIRIT or UNDEAD wards can delay Revenant/Wild Hunt manifestations;
- VAMPIRE wards can cancel wild-Vampirism target changes only when the exact predation provider also says that target is valid prey;
- tame animals are excluded;
- MCA-vampire target/navigation remains provider-owned and is never intercepted by this guard.

## Fae offerings

Fae bargains reuse existing food and flowers.

- sneak-right-click an existing flower with a tagged offering;
- the scan touches loaded chunks only;
- at most eight nearby valid `BonemealableBlock`s advance using their real growth method;
- the offering, cooldown, lore and reputation are committed only when at least one block actually grows;
- milk buckets return a bucket and honey bottles return a glass bottle;
- regional cooldown prevents farming the mechanic;
- vanilla particles only.

No fae currency, offering item, block, mob or sound is added.

## Existing trophies

0.8 gives existing provider drops a research/proof meaning:

- `cnc:wendigo_head`
- `cnc:chupacabra_head`

First meaningful pickup can raise the corresponding lore floor and Hunter reputation. The item is not replaced or modified and is not currently a hard physical requirement for contract completion.

## L2 Hostility — disabled in 0.8

L2 Hostility performs **no Dark Folklore runtime mutation in 0.8**.

- `l2HostilityScaling` defaults to `false`;
- the L2 bridge itself is policy-disabled;
- even an old server config containing `l2HostilityScaling=true` cannot apply levels or traits;
- `/folklore encounter l2` reports `DISABLED_BY_DARKFOLKLORE_POLICY` and may show only whether L2 is installed.

The integration boundary is intentionally retained for a future exact-JAR audit, but there is no active reflection, level scaling or trait mutation in this release candidate.

## Spawn / performance safety

- maximum active encounter count is configurable, default 4;
- encounter evaluation runs every 200 ticks;
- only loaded players/chunks are considered for manifestation;
- no forced chunk loading;
- spawn search requires world-border inclusion, empty collision at feet/head and sturdy loaded ground;
- persistence caps encounter/region rows;
- participant lists are capped and persisted defensively;
- Wild Hunt group size is fixed and bounded;
- Fae crop scan is one bounded interaction scan, not a recurring world scan;
- ward count is bounded and expired rows are pruned;
- provider facts are never inferred from encounter narrative state.

## Operator diagnostics

```text
/folklore encounter list
/folklore encounter inspect <uuid>
/folklore encounter seed <wendigo|chupacabra|revenant|wild_hunt>
/folklore encounter l2
/folklore ward list
```

The seed command uses an explicit origin per encounter and still obeys provider availability, active-region and global-budget admission.

## Release boundaries

0.8 remains a release candidate until the intended modpack proves:

- every provider entity ID exists and can be safely instantiated with its real provider behavior;
- encounter/story/contract identity survives unload/reload/save/restart correctly;
- pre- and post-manifestation evidence works end to end in the actual pack;
- Occultism Wild Hunt AI tolerates the bounded navigation hint;
- Graveyard Revenant target hint does not break provider behavior;
- CNC Wendigo/Chupacabra drops and natural ecology remain balanced with story manifestations;
- Chupacabra unexplained-death provenance is not too permissive in the real ecosystem;
- wards do not interfere with MCA-vampire provider AI;
- Fae offerings behave correctly with the installed crop mods;
- a fresh Atlas scan reports no unexpected resource/recipe/tag regressions.
