# Development

## Prerequisites

- A Java 21 JDK.
- Network access for the Gradle wrapper, NeoForge/Parchment artifacts, Maven Central, and the immutable compile-only Modrinth artifacts on first resolution.
- No local `mods/*.jar`, Atlas installation, local Maven publication, or workstation-specific absolute path.

Pinned inputs include ModDevGradle `2.0.143`, NeoForge `21.1.248`, Parchment `2024.11.17` for Minecraft 1.21.1, and the Gradle wrapper declared under `gradle/wrapper`. Java compilation uses `--release 21`.

Three typed integrations are resolved through the `auditedCompat` configuration and extended into `compileOnly`:

| Integration | Immutable coordinate | Audited version |
| --- | --- | --- |
| Vampirism API | `maven.modrinth:jVZ0F1wn:rAtxPNwi` | 1.10.12 |
| Werewolves bridge | `maven.modrinth:3ElBohKg:zkd687ts` | 2.0.3.3 |
| Field Guide bridge | `maven.modrinth:field-guide:8jdVbcd0` | 1.14.0 |

`verifyAuditedCompat` compares all three downloaded files to hardcoded SHA-256 values before `compileJava`. They are non-transitive, not shaded, and not copied into the release JAR.

## Build and run

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

Common tasks:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat runServer
.\gradlew.bat runClient
.\gradlew.bat runData
```

The first development-server launch may stop after creating `run/eula.txt`. Set `eula=true` only after accepting the Minecraft EULA. The server run already passes `--nogui`.

Expected artifacts:

```text
build/libs/darkfolklore-core-0.2.0.jar
build/libs/darkfolklore-core-0.2.0-sources.jar
```

`clean build` runs JUnit, resource validators, dependency checksum verification, and `auditReleaseJar`. Archives use stable entry ordering and stripped timestamps. The JAR audit rejects metadata placeholders, absent required resources, Java classes other than version 65, local absolute user paths, test/Atlas/cache content, nested JARs, and shaded optional-mod packages.

GitHub Actions executes the same Java 21 clean build from a fresh checkout and uploads only the production JAR.

## Project layout

```text
src/main/java/com/darkfolklore/core/
  api/             query facade and observational events
  canonical/       shared concepts and canonicalization codecs
  compat/          exact-version, fail-closed optional adapters
  config/          common NeoForge configuration
  contracts/       persistent investigation state and interactions
  data/            atomic JSON reload preparation/validation
  diagnostics/     permission-level-2 command tree
  gametest/        three live NeoForge regression tests
  investigation/   logical evidence records
  knowledge/       player lore and observer-specific beliefs
  persistence/     schema-2 SavedData and schema-1 migration
  society/         witnesses, rumors, families, organizations, stories, villages, lineage
  spawn/           natural-spawn filtering and encounter pressure
  traits/          item/entity semantic tag resolution
  weakness/        guarded damage rules
  world/           full-moon and witching-hour state

src/main/resources/
  data/darkfolklore/darkfolklore/  eight Core reload directories
  data/darkfolklore/fieldguide/    curated Field Guide categories
  data/darkfolklore/tags/          semantic tags
  data/darkfolklore/neoforge/      canonicalization biome modifiers
  data/werewolves/recipe/          exact-provider wolfsbane recipe overrides
  data/neoforge/loot_modifiers/    global loot-modifier index
  assets/darkfolklore/lang/        English and Italian localization

src/test/java/                     49 JUnit tests and resource validators
.github/workflows/build.yml        clean-build CI
docs/                              behavior, audits, release gates, and limitations
```

`src/main/templates/META-INF/neoforge.mods.toml` is expanded by `generateModMetadata`; do not edit generated build output.

## Design rules

1. Keep gameplay authoritative on the logical server. Player messages and vanilla particles are feedback, not trusted state.
2. Treat provider supernatural state as fact and Dark Folklore social records as belief. Never turn a rumor into a provider capability or attachment.
3. Use public APIs first. Isolate necessary reflection or direct optional types behind exact mod/version and signature checks.
4. Fail closed: use `UNKNOWN`, `NOT_APPLICABLE`, or a disabled compatibility report instead of inferring an unavailable fact.
5. Never resolve optional implementation classes before the corresponding exact-version gate succeeds.
6. Prefer tags and reloadable policy. Use recipes, loot/worldgen changes, or runtime bridges only after the acquisition/mechanics audit proves the ownership boundary.
7. Bound event work by radius, candidates, queues, per-pass budgets, TTLs, caps, or retention windows. Do not add every-tick world scans.
8. Persist durable consequences, not optional-mod objects or derived caches. A schema bump requires a backward-compatible, idempotent migration test and documentation.
9. Publish public NeoForge events only after the in-memory state mutation has succeeded.
10. Keep logs quiet by default; detailed simulation reasons belong behind `debugLogging` or bounded diagnostics.

## Adding reloadable data

Core's atomic reload roots are:

```text
darkfolklore/canonical
darkfolklore/weaknesses
darkfolklore/spawn_profiles
darkfolklore/magic_integrations
darkfolklore/story_templates
darkfolklore/organization_archetypes
darkfolklore/social_parameters
darkfolklore/political_weights
```

Definitions from any resource namespace participate in one candidate state. Parsing and cross-record validation must both succeed before that state replaces the previous snapshot. A malformed file intentionally rejects the complete candidate rather than partially applying it.

Use namespaced IDs, make optional tag entries `required: false`, add constructor-policy tests, and add a source-resource test for every shipped format. After a change, run `/reload` and require `invalid=0` in `/folklore diagnostics`.

Field Guide categories, standard tags/recipes, global loot modifiers, and NeoForge biome modifiers use external/standard loaders and are tested separately. Do not add them to `FolkloreDataManager` merely to share the transaction.

## Adding an optional adapter

1. Record the mod ID, exact display version, JAR hash, inspected classes/signatures, and ownership boundary in the appropriate audit.
2. Add an explicit compatibility report and a pure fail-closed behavior for absent, untested, or mismatched providers.
3. Cache reflective handles at initialization; do not perform signature discovery in a hot event path.
4. Cache only Core DTOs with a bounded size/TTL. Never retain foreign world/entity objects.
5. Load typed bridge classes only after exact validation.
6. Test the pure gate and model, then smoke startup with the provider absent and present.
7. Expose the active/disabled/error reason through `/folklore diagnostics` or a focused diagnostic command.

MCA Reborn and MCA Capitals details are documented in [MCA social audit](MCA_SOCIAL_AUDIT.md) and [MCA Capitals](MCA_CAPITALS.md). Field Guide and wolfsbane have their own implementation audits.

## Persistence changes

`FolkloreSavedData.SCHEMA_VERSION` is 2. Schema-1 data must remain readable. New fields need safe defaults; load must mark a legacy save dirty exactly once; saving writes schema 2; reloading schema 2 must not rerun migration. Preserve independent row decoding so one malformed row does not discard unrelated rows.

Back up a real 0.1 world and exercise the upgrade path as part of the production smoke matrix. Unit round trips do not substitute for a world-level migration test.

## Release checklist

1. Run the 49-test JUnit suite and the three GameTests with Java 21.
2. Run a clean build twice if checking reproducibility.
3. Inspect the production JAR and record its filename, size, and SHA-256.
4. Run the A-G matrix in [Testing](TESTING.md), including mandatory-only, exact adapters, curated pack, dedicated server, client, upgraded 0.1 world, and fresh 0.2 world.
5. Test `/reload`, focused diagnostics, contract testimony, public reveal, organization succession, Field Guide UI/recent discoveries, and wolfsbane mechanics.
6. Reconcile [Known Limitations](KNOWN_LIMITATIONS.md) and audit documents with observed results.

Do not publish or install the sources JAR. Do not classify the build `PRODUCTION_READY` without client validation (or an explicitly accepted equivalent), migration validation, dedicated-server startup, resource validation, and final JAR inspection.
