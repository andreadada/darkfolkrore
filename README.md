# Dark Folklore Core

Dark Folklore Core is the server-authoritative integration layer for the Dark Folklore Minecraft modpack. It gives overlapping content a shared semantic vocabulary and connects supernatural facts to lore, witnesses, relationship-aware rumors, village pressure, living organizations, society stories, investigations, weaknesses, and encounter pacing. Provider mods remain authoritative for transformations, families, factions, spellcasting, rendering, and their own progression.

## Target

- Minecraft **1.21.1**
- NeoForge **21.1.248** (the 21.1 line)
- Java **21**
- Mod ID `darkfolklore`
- Version **0.3.0**
- Persistent data schema **2**, with an idempotent schema-1 upgrade

Minecraft and NeoForge are required. Every third-party gameplay integration is optional. Code adapters activate only for exact audited versions; missing or different versions report `DISABLED` or `UNTESTED_VERSION` and fail closed without guessing supernatural or political facts.

## What 0.3.0 implements

- A unified occult-investigation loop: curated physical evidence and testimony produce evidence-only hypotheses; five cross-mod magical traditions can analyze nearby clues; identification unlocks research, preparation assessment, bounded tracking, and prepared-hunt rewards. See [Occult Investigation](docs/OCCULT_INVESTIGATION.md).

- Reloadable canonical concepts, weakness rules, spawn profiles, magic integrations, story templates, organization archetypes, social parameters, and political weights.
- A completed `darkfolklore:vampire` canonical concept and audited wolfsbane canonicalization. Enchanted owns the farmable canonical crop while Werewolves keeps its native diffuser, finder, contact-effect, and recipe mechanics through a strict bridge. See [Wolfsbane audit](docs/WOLFSBANE_AUDIT.md).
- A curated Field Guide 1.14.0 dataset: six categories, nine explicit entity entries, English/Italian text, native binary unlocks, recent-discovery participation, and two-way lore threshold synchronization. See [Field Guide](docs/FIELD_GUIDE.md).
- Event-driven witnesses and local rumor propagation with explainable contributions from prior knowledge, shared organizations, exact MCA relationships, verified MCA personalities, and exact MCA Capitals roles.
- Family-secret reactions, controlled false accusations, explicit public-reveal thresholds, and a strict separation between factual supernatural state and social belief.
- Four persistent organization types with objectives, bounded intelligence/event history, local recruitment, influence changes, contract consequences, confirmed-death cleanup, and deterministic leader succession.
- Data-driven society stories for family discovery, public exposure, hunter investigations, recruitment, full moons, witching hour, controlled false accusations, and political scandal.
- Persistent incident contracts with logical clues, witness testimony, search/collection particle feedback, lore rewards, canonical hunt validation, village consequences, and operator diagnostics.
- Schema-2 server save data, atomic all-or-nothing definition reloads, bounded work queues, configurable growth limits, reproducible archives, CI, and a production-JAR audit.

The implementation deliberately avoids Mixins and does not mutate optional mods' private state. Current presentation and verification boundaries are recorded in [Known Limitations](docs/KNOWN_LIMITATIONS.md).

## Install

1. Use Minecraft 1.21.1, NeoForge 21.1.248, and Java 21.
2. Build or obtain `darkfolklore-core-0.3.0.jar`.
3. Place only the production JAR, not the sources JAR, in the instance `mods` directory.
4. Install whichever optional provider mods the pack uses. Exact adapter versions are listed in [Compatibility](docs/COMPATIBILITY.md).
5. Back up an existing world, start the server, and review `config/darkfolklore-common.toml`.
6. Run `/folklore diagnostics`; require `invalid=0` and inspect every adapter status before enabling the world for players.

A 0.1 world is read as schema 1, receives safe defaults for new fields, is marked dirty once, and is written as schema 2 on the next save. Reopening schema 2 does not rerun the migration.

## Build

The build resolves the three typed compile-only integrations from immutable Modrinth Maven version IDs and verifies their SHA-256 hashes. It does **not** require files under a local `mods/` directory and does not shade those mods into Core.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build --no-daemon --no-configuration-cache
```

Artifacts:

```text
build/libs/darkfolklore-core-0.3.0.jar
build/libs/darkfolklore-core-0.3.0-sources.jar
```

Useful tasks:

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat runServer
.\gradlew.bat runClient
```

The 0.3.0 automated snapshot is **59 passing JUnit tests**, **3 passing GameTests**, and a successfully audited production JAR. The broader mandatory-only, exact-adapter, dedicated-server, graphical-startup, and migration evidence in [Testing](docs/TESTING.md) was recorded for 0.2.0; the new interactive occult-investigation loop still needs the manual acceptance matrix in [Occult Investigation](docs/OCCULT_INVESTIGATION.md). The current release classification therefore remains **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`.

The curated dedicated-server staging also emitted one unowned NeoForge client-`Screen` dist warning while continuing through startup, save, and shutdown. No Dark Folklore class was identified as its owner; the exact evidence and boundary are recorded in [Testing](docs/TESTING.md).

## Data and reloads

Core atomically reloads these directories:

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

The entire candidate state must validate. If any definition or cross-definition invariant fails, Core logs the precise resource error and retains the previous validated snapshot. Field Guide categories, standard tags, loot modifiers, recipes, and NeoForge biome modifiers use their owning formats and are outside this nine-directory transaction. See [Data formats](docs/DATA_FORMATS.md).

## Operator commands

All `/folklore` commands require permission level 2.

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

`inspect` and social/political diagnostics expose ground truth and are administrative tools, not ordinary-player knowledge.

## Contract quick start

A recognized supernatural actor killing an animal, villager, or MCA person can create a local incident with logical evidence. Empty-handed sneak-right-click a villager/MCA issuer in the same 8-by-8-chunk society region, then collect nearby clues, record credible testimony, or analyze the scene with a compatible magical implement. The profile's required number of distinct evidence types identifies the canonical target. Defeat a matching entity and return to the exact issuer for the reward. See [Contracts](docs/CONTRACTS.md) and [Occult Investigation](docs/OCCULT_INVESTIGATION.md).

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Society](docs/SOCIETY.md)
- [Contracts](docs/CONTRACTS.md)
- [Occult Investigation](docs/OCCULT_INVESTIGATION.md)
- [Data formats and schema 2](docs/DATA_FORMATS.md)
- [Compatibility](docs/COMPATIBILITY.md), [MCA social audit](docs/MCA_SOCIAL_AUDIT.md), and [MCA Capitals](docs/MCA_CAPITALS.md)
- [Field Guide](docs/FIELD_GUIDE.md) and [Wolfsbane audit](docs/WOLFSBANE_AUDIT.md)
- [Development](docs/DEVELOPMENT.md) and [Testing](docs/TESTING.md)
- [Known Limitations](docs/KNOWN_LIMITATIONS.md)
- [Changelog](CHANGELOG.md) and [0.2.0 historical changelog](docs/CHANGELOG_0.2.0.md)

## License

All Rights Reserved. The release JAR includes `META-INF/LICENSE_darkfolklore`.
