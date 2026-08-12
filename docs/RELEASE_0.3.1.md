# Dark Folklore Core 0.3.1 release gate

## Target

| Property | Value |
| --- | --- |
| Mod ID | `darkfolklore` |
| Version | `0.3.1` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.248` / 21.1 line |
| Java | 21, class-file major 65 |
| Society persistence | schema 2 |
| Investigation sidecar | schema 1 |
| Classification | `RELEASE_CANDIDATE` |

0.3.1 hardens Occult Investigation and adds the bounded Vampire Society & Predation bridge needed for Vampirism/MCA to produce natural social incidents without taking ownership of provider transformations.

## Latest automated code gate

Latest code-changing branch head: `0df64c4455e2a08bcf0a364d387098d56676894a`.

GitHub Actions run `31592180073`: **PASS**.

| Property | Recorded value |
| --- | --- |
| Artifact | `darkfolklore-core-0.3.1.jar` |
| JAR size | `457,973` bytes |
| SHA-256 | `0AADE878DD00EE168E36F546D0E4A2321F05E0C85185C6F28CB79D7012C3E4C3` |
| Class files | `182` |
| JUnit | `78/78 PASS`, 0 failures/errors/skipped |
| GameTests | `3/3 PASS` |
| Datapack reload | `17 canonical / 5 weaknesses / 8 spawn / 2 magic / 9 investigation / 13 stories / 4 organizations / 6 political, 0 invalid` |
| Class-file version | `65`, enforced by `auditReleaseJar` |

The release JAR audit rejects test/development classes, nested/shaded provider JARs, provider-owned packages, local user paths, wrong metadata/version, missing investigation/Field Guide resources, and non-Java-21 classes.

The final code gate also includes two provider-safety corrections made after the first predation implementation:

- entity `BloodDrinkEvent` observation runs at `LOWEST`, after MCA Vamp Compat's normal handler, so a provider-blocked/zeroed blood drain cannot create false Dark Folklore evidence, stories or cooldown state;
- malformed conversion provenance where the provider source UUID equals the converted entity UUID is ignored before constructing lineage data.

## Vampire Society & Predation ownership model

0.3.1 introduces a bounded director rather than replacing provider AI globally.

- Wild Vampirism entities remain Vampirism entities. When eligible, Core may guide them toward an adult MCA target or animal, but the blood drain uses Vampirism's real blood attachment/API and emits the real `BloodDrinkEvent`.
- MCA Vamp Compat remains the sole owner of MCA infection, conversion, cure, inherited vampirism and the MCA vampire's native infection-bite AI.
- A named MCA civilian is no longer rejected merely because Vampirism's generic feed goal protects custom-named entities.
- MCA vampires use social-risk scoring: public awareness, village suspicion, Hunter Society influence, personal `VAMPIRE` suspicion and visible witnesses make civilian feeding progressively less attractive. Animals become the safer option.
- Children, close family, known hunters, supernatural civilians, tamed animals and named non-MCA entities are protected from autonomous feeding selection.
- Feeding is night-only, local, staggered and bounded by per-predator/per-victim cooldowns plus a rolling regional feed budget. No chunk is force-loaded.
- Nonlethal feeding can create `BITE_MARK`/`BLOOD`, victim knowledge, witnesses, rumors, Hunter pressure and the contract-eligible `feeding_assault` story. Lethal feeds remain owned by the existing death/incident path so the same event is not duplicated.
- MCA vampire animal feeding uses Vampirism's real creature blood attachment and respects MCA Vamp Compat's native bite cooldown. It does not infect or replace animals and is disabled while the provider reports an active cure state.

## Exact provider audit boundary

Development audit used the user-supplied MCA Reborn x Vampirism Compat 2.0.12 JAR with SHA-256:

`BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`

The binary is not committed, redistributed or shaded. The bridge validates exact 2.0.12 method/class signatures at runtime and fails closed if they are missing. Normal CI intentionally does not install the full optional provider pack, so the green gate does not replace an in-world exact-provider test.

The intended native chain is:

```text
wild Vampirism vampire
 -> real Vampirism blood drain
 -> real BloodDrinkEvent
 -> MCA Vamp Compat processes event/infection at NORMAL
 -> Dark Folklore observes finalized event at LOWEST
```

Dark Folklore never calls provider infection/conversion/cure/inheritance mutations to manufacture lifecycle state.

## Persistence decision

The established `darkfolklore_society` save remains schema 2. Investigation continuity remains in `darkfolklore_investigation` schema 1. Predation sessions, cooldowns and scoring diagnostics are transient runtime state and are deliberately not persisted as authoritative facts.

## Correctness decisions retained

- Fact and belief are separate: a provider-confirmed vampire state does not automatically become public social knowledge.
- Known contract culprit UUID remains authoritative until confirmed death activates fallback; unload never equals death.
- New contracts retain explicit story identity.
- Weakness existence is not player knowledge; player-facing countermeasures require `STUDIED` lore.
- `KEEP_DISTINCT` investigation cases retain their concrete provider implementation.
- Fae remains curated rather than globally canonicalized.
- Dark Folklore does not force infection/cure/inheritance; provider lifecycle state is the fact.

## Deferred / manual gates

Before any `PRODUCTION_READY` claim, retain in-world evidence for:

- wild Vampirism vampire feeding on a named adult MCA civilian;
- real `BloodDrinkEvent` → provider-configured MCA infection;
- provider conversion retaining the same MCA person/entity rather than replacing it with a generic vampire mob;
- MCA vampire low-risk civilian feeding versus high-risk animal preference;
- child/family/hunter/supernatural/tamed protections;
- witnessed nonlethal feeding → evidence/rumor/Hunter pressure/`feeding_assault`;
- existing investigation culprit/issuer/Field Guide/FAE/persistence acceptance;
- intended full-modpack client/server and authentic backed-up world migration.

Cure/inheritance lifecycle observation is developed in the stacked 0.4 follow-up. Provider-native ritual integration for the five magic mods remains a later phase. 0.3.1 itself remains **`RELEASE_CANDIDATE`**.
