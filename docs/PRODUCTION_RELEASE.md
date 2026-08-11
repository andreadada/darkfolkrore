# Dark Folklore Core 0.2.0 production release

## Release target

| Property | Release value |
| --- | --- |
| Mod ID | `darkfolklore` |
| Display name | Dark Folklore Core |
| Version | 0.2.0 |
| Minecraft | exactly 1.21.1 |
| NeoForge | 21.1.248 through the 21.1 line |
| Java | 21 or newer; emitted class version 65 |
| License | All Rights Reserved; the complete notice is packaged as `META-INF/LICENSE_darkfolklore` |
| Authors | Dark Folklore |

The 0.2.0 minor version adds backwards-compatible society systems and persistent schema fields. It does not intentionally remove 0.1 registry IDs or foreign-mod content.

## Reproducible build

Install a 64-bit Java 21 JDK, then run from a fresh checkout:

```powershell
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

On Linux or macOS, use `./gradlew` with the same arguments. The production artifact is `build/libs/darkfolklore-core-0.2.0.jar`; the `-sources.jar` is a development artifact and must not be installed in a modpack.

Archive timestamps are stripped and entries use a stable order. The manifest deliberately omits a build timestamp. Running the same clean build twice from identical source and resolved inputs should therefore produce the same SHA-256.
The Gradle 9.2.1 wrapper distribution is also pinned by SHA-256 in `gradle/wrapper/gradle-wrapper.properties`.

## Audited compile-only dependencies

The project does not compile against files under `mods/`, a local Maven repository, an IDE cache, or an absolute path. The three Java integrations that require external types resolve from Modrinth's public Maven endpoint using immutable version IDs. Their content is verified before compilation.

| Purpose | Immutable Maven coordinate | SHA-256 |
| --- | --- | --- |
| Vampirism 1.21-1.10.12 API | `maven.modrinth:jVZ0F1wn:rAtxPNwi` | `C6DCCA1AF24DECA473A24470CCAB66053D3AA3324E453B4E1697090ED6D16BE2` |
| Werewolves 1.21-2.0.3.3 bridge | `maven.modrinth:3ElBohKg:zkd687ts` | `ECBCA2CD344E24AD48157834A8F321D1A7D2221C727FE8E61E4436D1219C6CFB` |
| Field Guide 1.14.0 NeoForge bridge | `maven.modrinth:field-guide:8jdVbcd0` | `00B26B1351CB85B90ED86675C49BFC054A3141BEDAE22358D5A6AD4FE7CB0740` |

These dependencies are `compileOnly` and non-transitive. They are not shaded, copied, or published by Dark Folklore Core. Dark Folklore Atlas, JUnit, local modpack JARs, and test classes never enter the production runtime configuration.

## Runtime dependency policy

Minecraft and NeoForge are required. Every third-party gameplay integration is declared optional. Metadata ranges are bounded to the audited compatibility line so NeoForge can order compatible mods, while adapters that use external Java contracts activate only on the exact version recorded by the compatibility manager. A missing or untested optional mod disables that adapter without inventing facts.

Dark Folklore Core 0.2.0 does not expose a stable public Java API. Classes under `compat` are internal, exact-version implementation details. Datapack formats and documented identifiers are the supported customization surface; patch releases should preserve them or supply migration behavior.

## Automated release gate

`build` runs unit tests, resource validators, the audited-dependency checksum task, and `auditReleaseJar`. The JAR audit fails on:

- unresolved metadata placeholders or an incorrect artifact version;
- missing manifest, license, English/Italian language files, Field Guide data, or canonical vampire data;
- Atlas, JUnit, test, temporary, cache, or shaded external-mod classes;
- embedded Windows user-directory paths;
- any class file not emitted as Java class version 65.

GitHub Actions repeats the Java 21 clean build without configuration-cache reuse and uploads only `darkfolklore-core-0.2.0.jar`. Local third-party JARs are ignored by Git and are not needed by CI.

## Final artifact audit

Two clean Java 21 builds from the frozen source produced byte-identical production JARs:

| Property | Final value |
| --- | --- |
| Filename | `darkfolklore-core-0.2.0.jar` |
| Size | 355,749 bytes |
| SHA-256 | `CCA1A4FE4F3D53A6F891FE05F51095EEE26A048CD6E850738100A4423176EBC7` |
| Java classes | 147, all class-file version 65 |

The independent audit confirmed 0.2.0 manifest/metadata, required resources/license, and no Atlas, JUnit, test, temporary, cache, nested-JAR, local-path, or shaded optional-mod content. Evidence is archived under `run/release-audit-020/`.

## Manual promotion gate

Automated success is necessary but not sufficient. Before publication, record the final filename, byte size, SHA-256, migration result, and smoke-matrix A–G results. Promotion to `PRODUCTION_READY` additionally requires successful dedicated-server and client validation, a fresh 0.2 world, and an upgraded 0.1 world with no known corruption or critical crash. If client validation remains outstanding, the maximum classification is `RELEASE_CANDIDATE`.

## Current classification

**`RELEASE_CANDIDATE`**. The 53-test JUnit suite, three live GameTests, deterministic clean builds, final JAR audit, mandatory-only/exact-adapter/curated headless smokes, final 0.2.0 dedicated-server startup/save/shutdown, graphical client startup to the title state, migration fixtures, and fresh-world smoke are recorded in [Testing](TESTING.md). Full in-world client/UI acceptance and an authentic 0.1-world upgrade remain unverified, so this build does not meet the `PRODUCTION_READY` gate.

The curated dedicated-server staging also logged one unowned NeoForge `RuntimeDistCleaner` request for `net.minecraft.client.gui.screens.Screen` on `DEDICATED_SERVER`. It did not prevent startup, save, or clean shutdown, and no Dark Folklore common/server class was identified as its owner. Treat it as an unresolved pack warning rather than claiming an error-free full-pack run.

The final graceful stop was requested by a dynamically attached test-harness agent. Its dynamic-attach/serviceability warning is not shipped mod behavior; the retained server log independently records the normal stop and complete world saves.

The final graphical client run loaded Core 0.2.0, completed resource reload with the exact Field Guide and wolfsbane bridges active, reached the NeoForge title state, and exited normally through Alt+F4 (`Stopping!`, `BUILD SUCCESSFUL`, empty stderr, no residual Java process). It did not join a world or exercise Field Guide pages, Recent Discoveries, Italian UI, contracts, or provider gameplay; matrix E therefore remains `BLOCKED`. The log also contains upstream provider model/sound/subtitle warnings but no Dark Folklore `ERROR`.
