# Dark Folklore 0.3.1 — Occult Investigation & Monster Hunting

Dark Folklore deliberately does **not** add a sixth spell system, mana bar, replacement ritual altar, clue item, or custom monster registry. The five existing magical traditions are investigative disciplines layered over the existing contract, lore, society, Field Guide, and Weakness systems.

## Unified loop

```text
supernatural incident
 -> physical clues / concept-level testimony
 -> evidence-supported hypotheses
 -> optional magical analysis
 -> identification
 -> exact observed Field Guide entry + OBSERVED lore
 -> deeper research
 -> STUDIED countermeasure knowledge
 -> bounded culprit tracking
 -> prepared hunt
 -> story / village / organization consequences
```

Provider mods remain authoritative for their creatures, transformations, rituals, items, and progression.

## Five traditions

| Tradition | Primary provider | Investigation role |
| --- | --- | --- |
| `WITCHCRAFT` | Enchanted | herbal reactions, garlic/wolfsbane semantics, ritual traces |
| `SPIRIT` | Occultism | spirit echoes and bindings |
| `SOUL` | Malum | soul echoes / death resonance |
| `FORBIDDEN_THEURGY` | Eidolon: Repraised | occult and curse signatures |
| `FAE` | Feywild | glamour traces |

The first 0.3.x integration layer is semantic and additive: Dark Folklore recognizes curated existing items/tags and adds investigation evidence without stealing the provider item's own right-click behavior. Native provider ritual hooks are intentionally deferred until exact-version APIs/events are audited.

## Reloadable investigation profiles

Investigation profiles are already validated, reloadable JSON under:

```text
data/<namespace>/darkfolklore/investigation_profiles/
```

0.3.1 ships nine curated profiles:

- Vampire
- Werewolf
- Wendigo
- Chupacabra
- Ghost
- Wraith
- Imp
- Iesnium Golem
- Feywild Sprite

A profile defines creature traits, possible signatures, tradition-specific analysis results, incident evidence, identification threshold, and tracking radius. Cross-definition validation rejects profiles whose canonical concept does not exist.

The Sprite profile is intentionally narrow rather than classifying every Feywild entity. It produces two physical traces but requires a third clue; Fae analysis can expose `GLAMOUR_TRACE` to complete the case.

## Physical evidence and expiry

Incident evidence is logical server data, not placeholder inventory items. Evidence is associated with a canonical concept and can retain the factual incident actor UUID.

Physical collection now rejects expired evidence at interaction time. Periodic pruning is maintenance only; it is not the security boundary for clue validity.

## Hypotheses

`HypothesisEngine` ranks only evidence currently recorded on the contract. It never reads the hidden target to manufacture certainty.

Generic traces can support multiple possibilities. More diagnostic occult evidence carries more weight. The displayed percentage is called **support**, not probability: it is an evidence-consistency score, not a calibrated statistical likelihood.

Operator diagnostics:

```text
/folklore investigation status <player>
/folklore investigation hypotheses <player>
/folklore investigation profile <concept>
```

## Social identity versus creature sightings

0.3.1 keeps two different facts separate:

```text
"This villager is secretly a vampire"
= social identity secret

"This witness saw a Wendigo near the incident"
= concept-level creature sighting
```

Cryptids, spirits, demons, constructs, and Fae are therefore not forced into an ever-growing `SecretType` enum. A bounded investigation SavedData sidecar persists concept sightings with state, confidence, source, time, optional observed entity UUID, location, and evidence.

Credible matching sightings can supply `TESTIMONY` to a contract. Existing Vampire/Werewolf identity testimony continues to use the social-knowledge path.

## Case continuity

0.3.1 adds explicit factual continuity metadata without rewriting the established society schema.

For new incidents the sidecar can retain:

```text
story UUID
culprit UUID
observed provider implementation
```

A contract created from that incident gets a case link to the exact story. This avoids resolving an arbitrary same-concept story when several incidents exist in one village region.

### Culprit policy

If a factual culprit UUID is known:

1. tracking prefers that exact entity;
2. a matching different creature is **not** enough while the original culprit remains valid;
3. merely unloading the culprit never authorizes fallback;
4. a confirmed culprit death outside the owner's valid hunt path can enable a same-concept fallback so the contract does not become permanently impossible.

Older contracts without sidecar metadata retain the legacy concept-level fallback behavior for compatibility.

### Issuer policy

The exact living issuer remains the normal hand-in target. If that issuer is confirmed dead, a fallback can be enabled. The fallback remains local to the contract's village region. If a local Hunter Society exists, an authorized local member is required; otherwise a valid local villager/MCA representative can accept completion.

## Identification and Field Guide

Identification advances target lore to `OBSERVED` (25 points).

For ordinary canonical concepts, the existing Field Guide bridge can use the canonical entity ID. For `KEEP_DISTINCT` concepts, 0.3.1 also retains the **observed implementation** from the incident, such as:

```text
eidolon_repraised:wraith
graveyard:wraith
```

The exact Field Guide 1.14.0 bridge can therefore unlock the page that was actually observed instead of arbitrarily collapsing distinct provider creatures.

The Fae Sprite addition expands Dark Folklore's curated Field Guide content to seven categories and ten explicit entries.

## Research and preparation

Weakness ground truth and player knowledge are separate.

```text
UNKNOWN / DISCOVERED / OBSERVED
 -> weakness details are not exposed

STUDIED / MASTERED
 -> documented WeaknessRule countermeasures may be shown and evaluated
```

`PreparationAssessment` combines the player's learned lore stage, inventory item traits, the investigation profile's creature traits, and the existing `WeaknessRule` registry. The Weakness Engine remains the sole authority that applies Dark Folklore's cross-mod damage multipliers and provider-native exclusions.

The prepared-hunt bonus therefore requires a **studied** relevant countermeasure, not accidental possession of the correct item.

## Tracking

After identification, sneak-use a compatible investigation implement in the air. Tracking performs a bounded search of **already loaded entities only** and never force-loads chunks.

When case continuity knows the culprit, the pulse tracks that culprit. Only an explicitly authorized fallback changes tracking to another matching canonical entity.

The result remains deliberately coarse: direction, distance, elevation, and a short particle trace.

## Evidence types added by the investigation layer

```text
HERBAL_REACTION
GARLIC_REACTION
WOLFSBANE_REACTION
SPIRIT_ECHO
SOUL_ECHO
OCCULT_SIGNATURE
GLAMOUR_TRACE
CURSE_TRACE
BINDING_TRACE
```

No duplicate clue-item registry is created.

## Knowledge command parser fix

The 0.2 namespaced-argument bug remains fixed through Minecraft's `ResourceLocationArgument`:

```text
/folklore knowledge get <player> darkfolklore:vampire
/folklore knowledge grant <player> darkfolklore:vampire 25
```

## Persistence boundary

Existing society state remains in schema 2. New 0.3.1 case/sighting continuity is stored separately as:

```text
darkfolklore_investigation
```

with sidecar schema 1. This minimizes upgrade risk and allows old contracts that lack new metadata to retain safe compatibility defaults.

The sidecar contains hard caps and periodic pruning for sightings, incident facts, and contract links.

## Automated validation

0.3.1 adds/extends tests for:

- profile/resource consistency and Fae coverage;
- Field Guide category/entry consistency after the Sprite addition;
- knowledge-gated preparation;
- case-link continuity and fallback flags;
- concept-sighting merge/decay behavior;
- investigation-sidecar NBT round trips;
- the existing hypothesis and contract state machines.

GitHub Actions performs a Java 21 clean build, resource validation, release-JAR audit, unit tests, NeoForge GameTests, and production artifact upload.

## Manual acceptance matrix

Before promotion beyond `RELEASE_CANDIDATE`, test at least:

1. `/folklore diagnostics` with `invalid=0` on the intended modpack.
2. A Vampire case using physical evidence plus Witchcraft analysis.
3. A Wendigo case where an NPC sighting becomes valid concept-level testimony.
4. A `KEEP_DISTINCT` Wraith case and verify the observed provider page, not an arbitrary Wraith page, unlocks in Field Guide.
5. A Feywild Sprite case where physical evidence alone remains insufficient and Fae analysis yields `GLAMOUR_TRACE`.
6. `OBSERVED` lore must not reveal weakness details; `STUDIED` must reveal documented options.
7. Prepared-hunt bonus with and without learned/carrying countermeasure.
8. Exact culprit tracking while loaded, no fallback merely after unload, then confirmed-death fallback behavior.
9. Issuer death followed by the documented local fallback hand-in.
10. Two same-concept incidents in one region and verify each contract advances its linked story.
11. Save/restart and verify contract evidence, sidecar case link, creature sighting, lore, and Field Guide progress persist.
12. English and Italian Field Guide presentation, Recent Discoveries, and the new Fae category.

## Production boundary

0.3.1 is a hardening release, not Deep Magic Phase 2. It does not claim native Enchanted/Occultism/Malum/Eidolon/Feywild ritual-event integration that has not been audited against the exact installed JARs.

That deeper provider-specific work belongs in a later release after exact API/event/recipe-codec inspection. Until the full client/in-world matrix is actually recorded, 0.3.1 remains `RELEASE_CANDIDATE`, not `PRODUCTION_READY`.
