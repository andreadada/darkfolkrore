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

0.3.1 is a hardening release for the 0.3 Occult Investigation architecture. It does not claim completion of provider-native Deep Magic Phase 2.

## Automated build gate

The 0.3.1 branch GitHub Actions gate requires:

```text
wrapper validation
Java 21 clean build
JUnit + resource validators
release JAR audit
explicit JUnit XML count
NeoForge GameTests
artifact identity print
artifact upload
```

A verified successful code run at `bdea1a9616b4c87081869e2f74585686742d783a` / Actions run `31525348730` produced:

| Property | Recorded value |
| --- | --- |
| Artifact | `darkfolklore-core-0.3.1.jar` |
| JAR size | `415,902` bytes |
| SHA-256 | `05FA8A21F91BFA61B0801CA88032B50A504F26894E2A928F598B2B0ACA0EAF5A` |
| Class files | `164` |
| Class-file version | `65` enforced by `auditReleaseJar` |
| GameTests | `3/3 PASS` |
| Datapack reload | `0 invalid` |

The artifact was uploaded by GitHub Actions. A later branch run after documentation-only updates must remain green before merge; if its deterministic artifact identity differs, this record must be updated rather than assuming equivalence.

## Release JAR audit

`auditReleaseJar` rejects at least:

- missing manifest/license/NeoForge metadata;
- wrong or unresolved mod version;
- missing EN/IT localization;
- missing canonical Vampire resources;
- missing 0.3 investigation resources;
- missing 0.3.1 Sprite/Fae canonical, profile, Field Guide category, or Fae tool tag;
- missing Field Guide data entirely;
- Atlas classes;
- JUnit/test dependencies/classes;
- shaded Vampirism/Field Guide/MCA compatibility classes;
- nested JARs;
- temporary/cache/IDE artifacts;
- leaked Windows user paths;
- any class not emitted as Java 21 / major 65.

The already-applied `apply_darkfolklore_0_3.py` development helper is removed from the 0.3.1 branch.

## Persistence decision

The established `darkfolklore_society` save remains schema 2. New 0.3.1 hardening metadata uses a separate `darkfolklore_investigation` SavedData sidecar with schema 1.

This sidecar stores:

- concept-level creature sightings;
- factual incident metadata;
- story → contract continuity;
- known culprit UUID;
- concrete observed implementation;
- confirmed-death fallback authorization.

This avoids forcing a broad schema-3 rewrite only to attach investigation metadata. New records are bounded and pruned. Pre-0.3.1 contracts without sidecar data retain documented legacy fallback behavior.

## Correctness decisions

### Factual state and belief remain separate

Social identity secrets are not expanded into every monster species. Cryptid/spirit/demon/construct/Fae observations use concept-level sighting records instead.

### Known culprit is authoritative

A new case with a known culprit does not accept a different same-concept target while that culprit remains valid. Unload is not death. A same-concept fallback requires explicit policy activation after a confirmed death.

### Story identity is explicit

New contracts store the story UUID they came from, avoiding accidental advancement of a different same-concept story in the same village region.

### Weakness truth is not player knowledge

Dark Folklore may know a WeaknessRule internally without exposing it to the player. Preparation feedback and the prepared-hunt bonus require `STUDIED` target lore.

### KEEP_DISTINCT remains distinct

The investigation sidecar retains the concrete provider implementation. Field Guide 1.14.0 can therefore unlock the observed implementation page when no single canonical entity ID exists.

### Fae is curated, not global

0.3.1 adds only the existing `feywild:sprite` as the first Fae investigation case. It does not globally treat all Feywild entities as one canonical monster.

## Deferred work

The following is intentionally **not** claimed as complete in 0.3.1:

- native provider ritual/event integration for all five magic mods;
- full client/in-world acceptance;
- every exact optional-provider permutation;
- authentic user-created old-world upgrade acceptance;
- multiplayer-scale performance benchmarking;
- polished player-facing quest UI beyond existing interaction/messages/Field Guide integration.

Deep provider-native integration belongs to a later exact-JAR/API audit rather than being simulated in this hardening patch.

## Manual promotion gate

Before any `PRODUCTION_READY` claim, complete and retain evidence for the manual matrix in [TESTING_0.3.1.md](TESTING_0.3.1.md), particularly:

- real in-world incident/contract loop;
- concept-level monster testimony;
- exact culprit behavior and unload/death distinction;
- issuer fallback;
- expired evidence rejection;
- STUDIED weakness gating;
- KEEP_DISTINCT Field Guide unlock;
- Sprite/FAE `GLAMOUR_TRACE` flow;
- save/restart persistence;
- EN/IT Field Guide and Recent Discoveries;
- intended full modpack client/server smoke.

Until those are actually completed, the maximum justified classification is **`RELEASE_CANDIDATE`**.
