# Dark Folklore Core

Dark Folklore Core is the server-authoritative integration and orchestration layer for the Dark Folklore Minecraft modpack. It connects provider-owned supernatural facts to lore, witnesses, rumors, villages, organizations, investigations, weaknesses, contracts, vampire predation, recipe interoperability, deep magic, and cross-mod progression.

> **Provider mods own their facts and native mechanics. Dark Folklore connects them without silently replacing them.**

For MCA entities, MCA Reborn × Vampirism Compat remains authoritative for factual supernatural state, infection, conversion, cure, inheritance, persistence, native targeting/navigation, and MCA vampire AI. Dark Folklore observes those facts and owns knowledge, evidence, investigation, rumor, reputation, stories, contracts, and bounded orchestration around them.

## Target

- Minecraft **1.21.1**
- NeoForge **21.1.248** / 21.1 line
- Java **21**
- Mod ID `darkfolklore`
- Development version **0.6.0**
- Society persistence schema **2**
- Investigation sidecar schema **1**

Third-party gameplay integrations are optional. Exact Java/reflection adapters activate only for audited versions; missing, partial, failed, or different versions fail closed rather than guessing supernatural state.

> **Branch status:** 0.6 is stacked on the 0.5 recipe-weaving branch/PR. It must not be merged to `main` before 0.5 completes intended-pack validation and is merged/rebased first.

## 0.6 — Supernatural World Loop

0.6 connects the existing systems into one loop:

```text
provider FACT
 -> incident / sighting
 -> witnesses + rumors
 -> investigation
 -> occult analysis / testimony
 -> progressive lore dossier
 -> cross-mod preparation
 -> hunt, cure or resolution
 -> village / organization reaction
```

### Vampire predation

Wild Vampirism vampires can actively pursue a provider-valid adult MCA civilian rather than waiting for accidental proximity. Dark Folklore never steals a different live combat target and never redirects converted MCA-vampire target/navigation, which remain MCA Vamp Compat-owned.

A bounded runtime hunt exposes explicit phases:

```text
TARGET_SELECTED
 -> PURSUING / STALKING
 -> ATTACKING
 -> FEEDING

any active phase -> ABORTED
```

Autonomous predation is allowed at night or while sheltered during daytime. Open-sky daytime exposure blocks/aborts the Dark Folklore session. This conservative rule does not guess provider sunscreen/equipment immunity.

The exact predation adapter isolates runtime failure into separate circuits for wild feeding, MCA fact reads, MCA target eligibility, MCA animal feeding, and native bite attribution. One failed optional capability no longer disables the healthy ones; every failure still fails closed.

### Progressive knowledge

The existing lore stages now map to explicit information facets:

- `UNKNOWN`: nothing;
- `DISCOVERED`: existence;
- `OBSERVED`: signs and behavior;
- `STUDIED`: identity, feeding habits, weaknesses, countermeasures, cure;
- `MASTERED`: origin and bloodline-level lore as well.

OBSERVED knowledge still cannot leak hidden weaknesses. Field Guide remains the provider UI/implementation unlock integration.

### Deep magic

0.6 adds a semantic gameplay vocabulary above existing provider integrations without making provider items universally equivalent:

| Discipline | Primary provider |
|---|---|
| Witchcraft | Enchanted |
| Spiritualism | Occultism |
| Soul Magic | Malum |
| Necromancy | Eidolon: Repraised |
| Fae Magic | Feywild |
| Blood Magic | optional Bloodlines-compatible content |
| Ritual Magic | cross-provider ritual vocabulary |

Obtaining audited investigation/ritual tools can discover the matching discipline lore. Existing `OccultInvestigationEngine` profiles remain authoritative for derived evidence and provider rituals remain provider-owned.

### Living village response

Persisted village pressure is projected into visible tiers:

```text
CALM -> UNEASY -> ALERT -> MOBILIZED -> LOCKDOWN
                          
                           -> COMPROMISED (strong vampire dominance)
```

The response uses existing awareness, suspicion, fear, political importance, and supernatural/Hunter influence. Major observed transitions become persistent `village_mobilization` or `village_compromised` stories through the existing story engine rather than fake faction NPCs or replacement AI.

### Atlas recipe graph

0.6 ships development tools for repeatable full-pack analysis:

```bash
python tools/verify_reference_pack.py <atlas-scan>
python tools/atlas_recipe_audit.py <atlas-scan> --out build/current-pack-audit
```

The recipe audit reports cross-mod ingredient bridges, asymmetric duplicate usage, producerless/high-risk candidates, and isolated recipe namespaces. It is advisory only: same-name similarity never authorizes substitution.

## 0.5 recipe-weaving foundation retained

0.6 inherits the 0.5 recipe-safe interoperability layer:

- Enchanted ↔ Vampirism/MCA Vamp Compat garlic;
- Occultism ↔ Eidolon tallow;
- Naturalist ↔ Fangs 'n Claws fur;
- Enchanted ↔ Hearth & Timber quicklime;
- Immersive Engineering ↔ Farm & Charm fertilizer;
- Enchanted ↔ Eidolon ↔ Occultism ritual ashes;
- woven MCA Occult Arts Book;
- redesigned Werewolves Stone Altar;
- additive occult Vampirism Totem Top route.

Recipe-safe tags remain deliberately narrower than semantic tags. `KEEP_DISTINCT` items such as provider soul shards, mandrakes, poppets, altars, broad holy/soul objects, and distinct silver equipment are not collapsed.

Canonical base-material convergence remains AlmostUnified/Immersive Engineering-owned where already established. No foreign registry entry is removed and existing inventories are not bulk-migrated.

## Install for full-pack testing

1. Use Minecraft 1.21.1, NeoForge 21.1.248, and Java 21.
2. Download the production `darkfolklore-core-0.6.0.jar` artifact from the current 0.6 GitHub Actions run.
3. Remove older Dark Folklore Core JARs from the instance `mods` directory and install only the production JAR, not the sources JAR.
4. Keep the audited provider versions listed in [Compatibility](docs/COMPATIBILITY.md).
5. Back up existing worlds before changing Core/provider versions.
6. Start the intended pack and run `/reload` and `/folklore diagnostics`.
7. Run the predation/lifecycle/world-loop diagnostics below.
8. After the gameplay matrix, run `/dfatlas scan` and verify/analyze the fresh scan with the supplied tools.

## Build

Windows:

```powershell
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

Linux/macOS:

```bash
./gradlew clean build --no-daemon --no-configuration-cache --stacktrace
```

Production artifact:

```text
build/libs/darkfolklore-core-0.6.0.jar
```

Useful tasks:

```text
./gradlew test
./gradlew runGameTestServer
./gradlew runServer
./gradlew runClient
```

Optional provider code is never shaded into the production JAR.

## Operator diagnostics

Ground-truth commands require permission level 2.

```text
/folklore diagnostics
/folklore inspect <entity>
/folklore canonical <concept>

/folklore knowledge get <player> <concept>
/folklore knowledge grant <player> <concept> <points>
/folklore knowledge dossier <player> <concept>

/folklore social inspect <entity>
/folklore rumor inspect
/folklore fieldguide diagnostics
/folklore capitals inspect <entity>
/folklore organization list
/folklore village [inspect]
/folklore village response <player>
/folklore story list
/folklore contracts

/folklore investigation status <player>
/folklore investigation hypotheses <player>

/folklore predation status
/folklore predation inspect <entity>
/folklore predation trace <entity>

/folklore lifecycle status
/folklore lifecycle inspect <entity>

/folklore magic disciplines
/folklore magic inspect-held

/folklore world status <player>
```

## 0.6 automated evidence

Final evidenced code head `d1cbea4e90700542532d7e2c2b1217f6fab2a79f` passed GitHub Actions run `31629194226`:

- **139/139 JUnit PASS**;
- **3/3 NeoForge GameTests PASS**;
- release JAR audit PASS;
- atomic reload: 17 canonical concepts, 5 weaknesses, 8 spawn profiles, 2 magic integrations, 9 investigation profiles, **15 story templates**, 4 organization archetypes, 6 political overrides, **0 invalid**;
- baseline Atlas audit: 147 mods, 11,296 cross-mod ingredient uses, 176 namespace bridges;
- `darkfolklore-core-0.6.0.jar`: **575,754 bytes**, **232 classes**;
- SHA-256 `BBE92BA67EE44ABD0AF010F271712FF4EDD5029FE6AF27C0AAF7A1E7D2AF6404`.

Automated CI proves code/resource integrity, not complete provider gameplay.

## Documentation

- [Supernatural World Loop 0.6](docs/SUPERNATURAL_WORLD_LOOP_0.6.md)
- [0.6.0 test matrix](docs/TESTING_0.6.0.md)
- [0.6.0 release gate](docs/RELEASE_0.6.0.md)
- [Recipe Weaving 0.5](docs/RECIPE_WEAVING_0.5.md)
- [0.5.0 release gate](docs/RELEASE_0.5.0.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Compatibility and factual routing](docs/COMPATIBILITY.md)
- [Canonicalization](docs/CANONICALIZATION.md)
- [MCA Vamp Compat 2.0.12 exact audit](docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md)
- [Field Guide](docs/FIELD_GUIDE.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md)
- [Changelog](CHANGELOG.md)

## Release classification

0.6.0 is **`DEVELOPMENT / RELEASE_CANDIDATE`**, not `PRODUCTION_READY`. Before promotion, the intended modpack must pass the complete matrix in [TESTING_0.6.0](docs/TESTING_0.6.0.md), including real Vampirism→MCA hunting/feeding, provider infection/conversion, provider-native converted-MCA AI, cure, inheritance, Field Guide client UI, end-to-end investigation, deep-magic discovery, village response, inherited 0.5 custom recipe serializers/JEI, fresh Atlas diff, and client/dedicated save-restart.

## License

All Rights Reserved. The release JAR includes `META-INF/LICENSE_darkfolklore`.
