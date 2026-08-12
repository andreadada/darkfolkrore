# Dark Folklore Core 0.3.1 release candidate

> **Historical release record:** this file intentionally preserves the 0.3.1 gate and its evidence. It is not the current 0.4.0 release report. Use [RELEASE_0.4.0.md](RELEASE_0.4.0.md) and [HANDOFF_0.4.0.md](HANDOFF_0.4.0.md) for current status, final-head placeholders, and manual blockers.

## Release target

| Property | Release value |
| --- | --- |
| Mod ID | `darkfolklore` |
| Display name | Dark Folklore Core |
| Version | `0.3.1` |
| Minecraft | exactly 1.21.1 |
| NeoForge | 21.1.248 through the 21.1 line |
| Java | 21; production classes must be class-file version 65 |
| Society persistence | schema 2 |
| Investigation persistence | sidecar schema 1 (`darkfolklore_investigation`) |
| License | All Rights Reserved; `META-INF/LICENSE_darkfolklore` |

0.3.1 is an investigation-hardening release. It does not intentionally replace provider-owned creature, transformation, ritual, family, faction, rendering, or progression state.

## Build

From a fresh checkout with Java 21:

```powershell
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
.\gradlew.bat runGameTestServer --no-daemon --no-configuration-cache --stacktrace
```

Linux CI uses:

```bash
chmod +x gradlew
./gradlew clean build --no-daemon --no-configuration-cache --stacktrace
./gradlew runGameTestServer --no-daemon --no-configuration-cache --stacktrace
```

The intended production artifact is:

```text
build/libs/darkfolklore-core-0.3.1.jar
```

The `-sources.jar` is a development artifact and must not be installed in the modpack.

## Reproducibility and JAR audit

Archive timestamps are stripped and entries use stable ordering. `auditReleaseJar` is part of `check`/`build` and must reject at least:

- unresolved metadata placeholders or a version other than 0.3.1;
- missing manifest, license, EN/IT localization, canonical/investigation resources, or Field Guide data;
- test/JUnit/Atlas classes;
- nested JARs or shaded optional-provider classes;
- temporary/cache files and local absolute user paths;
- class files that are not Java 21 / major version 65.

GitHub Actions additionally runs NeoForge GameTests and uploads only the production 0.3.1 JAR.

A final release report must record the JAR's exact size and SHA-256 from the **final green head commit**. Do not reuse an artifact hash from an earlier branch commit after further source changes.

## Runtime dependency policy

Minecraft and NeoForge are mandatory. Provider integrations remain optional. Exact Java/reflection bridges activate only for audited versions and fail closed when the expected contract is absent.

The current exact-version integration set includes the audited Vampirism, Werewolves, MCA, MCA Capitals, MCA Reborn x Vampirism Compat, Enchanted, and Field Guide bridges documented under `docs/COMPATIBILITY.md`. Other cross-mod semantics use tags/data where possible.

Deep provider-native ritual hooks for Enchanted, Occultism, Malum, Eidolon: Repraised, and Feywild are **not** claimed by 0.3.1. The current magic investigation layer is additive/semantic; native ritual-event integration belongs to a later exact-version audit.

## 0.3.1 persistence policy

The established society save remains schema 2. 0.3.1 does not force a destructive society-schema bump for investigation hardening.

New factual investigation metadata is stored in a separate sidecar:

```text
darkfolklore_investigation
```

Sidecar schema 1 persists bounded:

- concept-level creature sightings;
- story incident facts;
- contract-to-story case links;
- factual culprit UUID when known;
- observed provider implementation;
- confirmed-death fallback state.

Older contracts that have no sidecar metadata retain documented compatibility fallbacks. A real backed-up user world must still be tested before any claim that every historical save path is production-proven.

## Release-critical 0.3.1 behavior

The release is expected to preserve these invariants:

- physical evidence cannot be collected after logical expiry;
- a new contract is explicitly associated with its source story;
- a known incident culprit is preferred for tracking and hunt completion;
- simply unloading a culprit never authorizes same-concept fallback;
- culprit/issuer fallback activates only from confirmed lifecycle conditions;
- cryptid/spirit/demon/construct/Fae sightings are concept observations, not fabricated social identity secrets;
- matching testimony about a known culprit must refer to that culprit while exact continuity remains authoritative;
- `KEEP_DISTINCT` investigations can unlock the concrete observed Field Guide entry;
- canonical concepts with internal runtime variants can fall back to their canonical Field Guide page;
- weakness details are hidden from ordinary preparation output below `STUDIED` lore;
- prepared-hunt rewards require learned and satisfied countermeasure knowledge;
- Fae investigation has a real curated path through Feywild Sprite and `GLAMOUR_TRACE`;
- tracking remains bounded to loaded entities and never force-loads chunks.

## Automated promotion gate

Before publishing a 0.3.1 artifact, require one final GitHub Actions run for the exact release head with:

- wrapper validation: PASS;
- Java 21 clean build: PASS;
- JUnit/resource tests: PASS;
- `auditReleaseJar`: PASS;
- NeoForge GameTests: PASS;
- production artifact upload: PASS.

The exact run/commit, test evidence, JAR size, and JAR SHA-256 belong in the final release report.

## Manual promotion gate

A green automated gate is necessary but insufficient. The intended modpack still needs the in-world matrix in `docs/TESTING.md`, especially:

- Vampire/Wendigo/Fae investigations;
- concept-level witness testimony;
- Wraith `KEEP_DISTINCT` Field Guide behavior;
- canonical-page fallback for provider implementation variants;
- OBSERVED vs STUDIED weakness visibility;
- culprit unload/death behavior;
- issuer-death hand-in fallback;
- two same-concept incidents in one region;
- save/restart of the sidecar plus existing society state;
- Field Guide EN/IT presentation and Recent Discoveries.

An authentic historical user-world upgrade remains a manual gate; fixtures are not equivalent to a real save.

## Historical 0.2 evidence

0.2.0 had a separately frozen and audited artifact. Its recorded production JAR was 355,749 bytes with SHA-256:

```text
CCA1A4FE4F3D53A6F891FE05F51095EEE26A048CD6E850738100A4423176EBC7
```

That hash and the 0.2 smoke matrix are historical evidence only. They must not be attributed to 0.3.1.

## Current classification

**`RELEASE_CANDIDATE`**.

0.3.1 must not be promoted to `PRODUCTION_READY` solely because compilation, unit tests, GameTests, or JAR audit pass. Full intended-pack client/in-world acceptance and the remaining real-world persistence gates must be recorded first.
