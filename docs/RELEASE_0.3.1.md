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

0.3.1 hardens Occult Investigation and now adds the bounded Vampire Society & Predation bridge needed for Vampirism/MCA to produce natural social incidents without taking ownership of provider transformations.

## Latest automated build gate

Exact branch head: `f3a8635a15d46855e1eb4e0d78d2f5ab69a5b917`.

GitHub Actions run `31590162424`: **PASS**.

| Property | Recorded value |
| --- | --- |
| Artifact | `darkfolklore-core-0.3.1.jar` |
| JAR size | `457,738` bytes |
| SHA-256 | `A62623ED93B3912FDFB009DE62382B5C1CA7F014F7EAE9ACD5F2B0AF390BD10F` |
| Class files | `182` |
| JUnit | `78/78 PASS` |
| GameTests | `3/3 PASS` |
| Datapack reload | `17 canonical / 5 weaknesses / 8 spawn / 2 magic / 9 investigation / 13 stories / 4 organizations / 6 political, 0 invalid` |
| Class-file version | `65`, enforced by `auditReleaseJar` |

The release JAR audit rejects test/development classes, nested/shaded provider JARs, provider-owned packages, local user paths, wrong metadata/version, missing investigation/Field Guide resources, and non-Java-21 classes.

## Vampire Society & Predation ownership model

0.3.1 introduces a bounded director rather than replacing provider AI globally.

- Wild Vampirism entities remain Vampirism entities. When eligible, Core may guide them toward an adult MCA target or animal, but the blood drain uses Vampirism's real blood attachment/API and emits the real `BloodDrinkEvent`.
- MCA Vamp Compat remains the sole owner of MCA infection, conversion, cure, inherited vampirism and the MCA vampire's native infection-bite AI.
- A named MCA civilian is no longer rejected merely because Vampirism's generic feed goal protects custom-named entities.
- MCA vampires use social-risk scoring: public awareness, village suspicion, Hunter Society influence, personal VAMPIRE suspicion and visible witnesses make civilian feeding progressively less attractive. Animals become the safer option.
- Children, close family, known hunters, supernatural civilians, tamed animals and named non-MCA entities are protected from autonomous feeding selection.
- Feeding is night-only, local, staggered and bounded by per-predator/per-victim cooldowns plus a rolling regional feed budget. No chunk is force-loaded.
- Nonlethal feeding can create `BITE_MARK`/`BLOOD`, victim knowledge, witnesses, rumors, Hunter pressure and the contract-eligible `feeding_assault` story. Lethal feeds remain owned by the existing death/incident path so the same event is not duplicated.

## Exact provider audit boundary

Development audit used the user-supplied MCA Reborn x Vampirism Compat 2.0.12 JAR with SHA-256:

`BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`

The binary is not committed, redistributed or shaded. The bridge validates exact 2.0.12 method/class signatures at runtime and fails closed if they are missing. Normal CI intentionally does not install the full optional provider pack, so the green gate does not replace an in-world exact-provider test.

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
- real BloodDrinkEvent → provider-configured MCA infection;
- provider conversion retaining the same MCA person/entity rather than replacing it with a generic vampire mob;
- MCA vampire low-risk civilian feeding versus high-risk animal preference;
- child/family/hunter/supernatural/tamed protections;
- witnessed nonlethal feeding → evidence/rumor/Hunter pressure/`feeding_assault`;
- cure and inheritance observed from the exact provider;
- existing investigation culprit/issuer/Field Guide/FAE/persistence acceptance;
- intended full-modpack client/server and authentic backed-up world migration.

Provider-native ritual integration for the five magic mods remains a later phase. Exact MCA vampire lifecycle observation is developed separately as the 0.4 stacked follow-up; 0.3.1 itself remains **`RELEASE_CANDIDATE`**.
