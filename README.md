# Dark Folklore Core

Dark Folklore Core is the server-authoritative integration layer for the Dark Folklore Minecraft modpack. It gives overlapping supernatural content a shared semantic vocabulary and connects provider-owned facts to lore, witnesses, rumors, villages, organizations, investigations, weaknesses, contracts, and encounter pacing. Provider mods remain authoritative for transformations, families, factions, spellcasting, rendering, and their own progression.

## Target

- Minecraft **1.21.1**
- NeoForge **21.1.248** (21.1 line)
- Java **21**
- Mod ID `darkfolklore`
- Version **0.3.1**
- Society persistence schema **2**
- Investigation sidecar schema **1**

Minecraft and NeoForge are required. Third-party gameplay integrations are optional. Exact Java adapters activate only for audited versions; missing or different versions fail closed rather than guessing supernatural or political facts.

## What 0.3.1 implements

0.3.1 hardens the unified occult-investigation loop introduced in 0.3.0:

```text
incident
 -> physical evidence / credible testimony
 -> evidence-only hypotheses
 -> optional occult analysis
 -> identification
 -> Field Guide / lore
 -> research
 -> learned countermeasure
 -> bounded tracking
 -> hunt
 -> social / village / organization consequences
```

Key 0.3.1 changes:

- **Case continuity.** New investigation-sidecar data explicitly links a contract to the story that created it, the known incident culprit UUID, and the observed concrete provider implementation.
- **Actual culprit hunting.** When a factual culprit is known, tracking and contract completion prefer that entity. A same-concept fallback is enabled only after confirmed culprit death; merely unloading the entity never authorizes a fallback.
- **Issuer recovery.** A confirmed issuer death can enable a bounded local hand-in fallback. If a local Hunter Society exists, only an authorized member can receive it; otherwise a valid local villager/MCA representative can do so.
- **Creature sightings.** Cryptids, spirits, demons, constructs, and Fae use persistent concept-level observations instead of being forced into MCA-style `SecretType` identities. This lets a witness genuinely testify about `darkfolklore:wendigo`, `darkfolklore:wraith`, and other curated concepts.
- **Expired evidence safety.** Physical clue collection rejects logically expired evidence directly rather than relying only on later pruning.
- **Knowledge-gated preparation.** Weakness ground truth no longer leaks to an ordinary player just because a matching item is in the inventory. Countermeasure details become player-facing at `STUDIED`; the prepared-hunt bonus requires that learned knowledge.
- **KEEP_DISTINCT Field Guide support.** Investigation retains the concrete implementation, allowing the exact observed page to unlock for concepts such as provider-distinct Wraiths instead of arbitrarily collapsing them.
- **Fae investigation.** Feywild's existing `feywild:sprite` is curated as `darkfolklore:sprite`; its case requires Fae analysis to expose `GLAMOUR_TRACE`. No new mob or duplicate magic system is added.
- **Hypothesis terminology.** Player/operator output reports evidence `support`, not a misleading probability-like confidence percentage.
- **CI hardening.** Linux wrapper execution is fixed, Java 21 clean builds run in GitHub Actions, the 0.3.1 JAR is audited/uploaded, and NeoForge GameTests are part of the branch release gate.

The implementation deliberately avoids new Mixins for these systems and does not mutate optional mods' private state.

## Existing systems retained

- Reloadable canonical concepts, weakness rules, spawn profiles, magic integrations, investigation profiles, story templates, organization archetypes, social parameters, and political weights.
- Enchanted-owned canonical wolfsbane with the strict Werewolves bridge for diffuser, finder, crop-contact, recipes, loot, and legacy compatibility.
- Field Guide 1.14.0 integration with **seven** curated categories and **ten** explicit entity entries after the Fae Sprite addition, with English and Italian resources.
- Relationship-aware witnesses/rumors, family-secret reactions, controlled false accusations, MCA Capitals political weighting, organizations, society stories, village state, and contract consequences.
- Evidence-only hypothesis ranking: `HypothesisEngine` never reads the hidden target to manufacture certainty.
- Loaded-area-only tracking: no chunk force-loading and no whole-world scan.
- Weakness Engine remains the only Dark Folklore authority for cross-mod damage semantics; investigation does not apply a second damage multiplier.

## Install

1. Use Minecraft 1.21.1, NeoForge 21.1.248, and Java 21.
2. Build or obtain `darkfolklore-core-0.3.1.jar`.
3. Place only the production JAR, not the sources JAR, in the instance `mods` directory.
4. Install whichever optional provider mods the pack uses. Exact adapter versions are listed in [Compatibility](docs/COMPATIBILITY.md).
5. Back up an existing world, start the server, and review `config/darkfolklore-common.toml`.
6. Run `/folklore diagnostics`; require `invalid=0` and inspect adapter status before using the world for release validation.

The existing `darkfolklore_society` data remains schema 2. New 0.3.1 observation/case-continuity data is stored in a separate `darkfolklore_investigation` SavedData file so hardening does not destructively rewrite the established society schema.

## Build

The build resolves typed compile-only integrations from immutable public artifacts and verifies the audited checksums. It does not require a local `mods/` directory and does not shade provider mods into Core.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

Linux/macOS:

```bash
./gradlew clean build --no-daemon --no-configuration-cache --stacktrace
```

Artifacts:

```text
build/libs/darkfolklore-core-0.3.1.jar
build/libs/darkfolklore-core-0.3.1-sources.jar
```

Useful tasks:

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat runServer
.\gradlew.bat runClient
```

See [0.3.1 testing](docs/TESTING_0.3.1.md) for evidence generated specifically from this branch. Historical 0.2 smoke evidence remains documented separately and must not be presented as if it validated 0.3.1 gameplay.

## Data and reloads

Core atomically reloads:

```text
data/<namespace>/darkfolklore/canonical/
data/<namespace>/darkfolklore/weaknesses/
data/<namespace>/darkfolklore/spawn_profiles/
data/<namespace>/darkfolklore/magic_integrations/
data/<namespace>/darkfolklore/investigation_profiles/
data/<namespace>/darkfolklore/story_templates/
data/<namespace>/darkfolklore/organization_archetypes/
data/<namespace>/darkfolklore/social_parameters/
data/<namespace>/darkfolklore/political_weights/
```

The entire candidate state must validate. If one definition or cross-definition invariant fails, Core retains the previous validated snapshot. Field Guide categories, standard tags, recipes, loot modifiers, and NeoForge biome modifiers remain in their owning formats outside that transaction.

## Operator commands

All `/folklore` ground-truth commands require permission level 2.

```text
/folklore diagnostics
/folklore inspect <entity>
/folklore canonical <concept>
/folklore knowledge get <player> <concept>
/folklore knowledge grant <player> <concept> <points>
/folklore social get <observer> <subject> <secret>
/folklore social set <observer> <subject> <secret> <state> <confidence>
/folklore social inspect <entity>
/folklore rumor inspect
/folklore fieldguide diagnostics
/folklore capitals inspect <entity>
/folklore organization list
/folklore organization create <type> <leader> <name...>
/folklore organization inspect <uuid>
/folklore village [inspect]
/folklore story list
/folklore stories
/folklore contracts
/folklore investigation status <player>
/folklore investigation hypotheses <player>
/folklore investigation profile <concept>
```

## Contract quick start

A recognized supernatural actor killing an animal, villager, or MCA person can create a local incident. Empty-handed sneak-right-click a valid local issuer, then collect nearby physical clues, record credible testimony, or analyze the scene with a compatible existing magical implement. Identification advances lore to `OBSERVED`; weakness/preparation details remain hidden until `STUDIED`. If the incident recorded a factual culprit, tracking and hunt completion follow that culprit while it remains valid. Confirmed deaths can enable the documented fallback paths; unloads cannot.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Society](docs/SOCIETY.md)
- [Contracts](docs/CONTRACTS.md)
- [Occult Investigation](docs/OCCULT_INVESTIGATION.md)
- [0.3.1 testing](docs/TESTING_0.3.1.md)
- [0.3.1 release gate](docs/RELEASE_0.3.1.md)
- [Data formats](docs/DATA_FORMATS.md)
- [Compatibility](docs/COMPATIBILITY.md)
- [Field Guide](docs/FIELD_GUIDE.md)
- [Wolfsbane audit](docs/WOLFSBANE_AUDIT.md)
- [Historical testing](docs/TESTING.md)
- [Historical 0.2 production record](docs/PRODUCTION_RELEASE.md)
- [Changelog](CHANGELOG.md)

## Release classification

0.3.1 remains **`RELEASE_CANDIDATE`** until the final branch CI, in-world client investigation/Field Guide acceptance, and the required real-world migration/compatibility checks are recorded. Compilation or unit tests alone never promote it to `PRODUCTION_READY`.

## License

All Rights Reserved. The release JAR includes `META-INF/LICENSE_darkfolklore`.
