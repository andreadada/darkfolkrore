# Testing

Dark Folklore uses four evidence layers: unit/resource tests, live NeoForge GameTests, clean-build/JAR validation, and manual server/client/world smokes. A lower layer never implies that the complete modpack or an optional-provider UI is release-ready.

## Commands

Use Java 21.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

Linux/macOS release gate:

```bash
chmod +x gradlew
./gradlew clean build --no-daemon --no-configuration-cache --stacktrace
./gradlew runGameTestServer --no-daemon --no-configuration-cache --stacktrace
```

Runtime tasks:

```powershell
.\gradlew.bat runServer
.\gradlew.bat runClient
```

## 0.3.1 automated scope

The 0.3.1 branch CI performs all of the following on Java 21:

1. Gradle wrapper validation.
2. Clean Gradle build with configuration-cache reuse disabled.
3. JUnit/resource validation through `build`/`check`.
4. `auditReleaseJar` against the 0.3.1 production JAR.
5. NeoForge `runGameTestServer`.
6. Upload of only `darkfolklore-core-0.3.1.jar` as the production artifact.

The 0.3.1 additions are covered by focused automated tests for:

- investigation-profile consistency and the curated Fae Sprite/`GLAMOUR_TRACE` path;
- Field Guide category/entry/localization consistency after the seventh category/tenth entry;
- hypothesis ambiguity and occult evidence weighting;
- knowledge-gated preparation, including no weakness leak below `STUDIED`;
- exact story/culprit/observed-implementation case links and monotonic fallback flags;
- concept-level creature-sighting merge and decay semantics;
- investigation-sidecar NBT save/load round trips;
- the existing contract state machine and broader 0.2 canonical/social/persistence policies.

The live GameTests remain intentionally small. They prove that the validated datapack state is present in a running server level, that public social belief cannot mutate factual supernatural state, and that confirmed organization-leader death performs deterministic succession. They do **not** render Field Guide UI or drive the entire player interaction loop.

## 0.3.1 regression targets

The hardening release specifically protects these previously weak boundaries:

| Area | Automated/structural guarantee | Manual check still required |
| --- | --- | --- |
| Linux CI | wrapper executable before Gradle; correct 0.3.1 artifact | none beyond observing final green run |
| Physical clues | expired evidence rejected at interaction time | verify feedback in a live case |
| Story continuity | new contract stores direct story link | two same-concept incidents in one village |
| Culprit continuity | exact UUID preferred; unload never enables fallback | unload/reload and confirmed-death fallback |
| Testimony | concept sightings are separate from identity secrets; known culprit requires matching observed entity | real Wendigo/cryptid witness interaction |
| Issuer recovery | fallback flag only after confirmed issuer death; hand-in remains local/authorized | kill issuer and finish the hunt |
| Field Guide | exact implementation bridge plus canonical fallback for internal provider variants | Wraith KEEP_DISTINCT and Vampire internal variant in client |
| Preparation | weaknesses hidden below `STUDIED`; prepared bonus requires learned countermeasure | compare OBSERVED vs STUDIED in-world output |
| Fae | Sprite concept/profile/category/resources validated | Sprite scene + Fae analysis + `GLAMOUR_TRACE` |
| Tracking | bounded loaded-entity search, culprit-aware target policy | direction/range UX and no chunk loading |

## Manual 0.3.1 acceptance matrix

Run these on disposable worlds or backed-up copies with the intended 1.21.1 pack. Retain `latest.log`, the final production JAR hash, config/datapacks, and screenshots where UI behavior matters.

| ID | Area | Procedure | Acceptance |
| --- | --- | --- | --- |
| H1 | Diagnostics/data reload | Start intended pack, run `/folklore diagnostics`, then `/reload`. | `invalid=0`; exact adapters show expected status; reload retains a complete valid state. |
| H2 | Vampire case | Produce a factual vampire incident, accept contract, gather physical clue and Witchcraft analysis. | Hypotheses use evidence support; identification reaches OBSERVED; canonical Field Guide page unlocks. |
| H3 | Wendigo testimony | Let a social NPC witness the actual Wendigo that creates the incident, then request testimony. | Concept-level `darkfolklore:wendigo` testimony counts once; an unrelated Wendigo sighting is not accepted while the original culprit remains authoritative. |
| H4 | KEEP_DISTINCT Wraith | Create an Eidolon Wraith and a Graveyard Wraith case separately. | The concrete observed provider page unlocks; the other Wraith page is not chosen arbitrarily. |
| H5 | Fae Sprite | Create a Sprite incident and collect its two physical clues. Perform Fae analysis with a curated Feywild implement. | Physical clues alone remain insufficient; Fae analysis produces `GLAMOUR_TRACE` and can complete identification. |
| H6 | Weakness knowledge | Inspect preparation at OBSERVED, then reach STUDIED. | OBSERVED leaks no weakness options; STUDIED reveals only documented WeaknessRules. |
| H7 | Prepared hunt | Hunt once without a learned/carrying countermeasure and once with one. | Bonus lore/Hunter reputation occurs only in the learned + prepared path. |
| H8 | Culprit continuity | Track a known culprit; unload its chunk; return; later cause a confirmed culprit death outside the valid owner-hunt path. | Unload alone does not authorize another same-concept kill; confirmed death enables documented fallback. |
| H9 | Issuer death | Accept contract, kill/remove issuer through a confirmed death, finish hunt. | Exact issuer no longer required; only documented local authorized fallback can turn in. |
| H10 | Concurrent incidents | Create two same-concept incidents in one village and accept/resolve one. | Only the story explicitly linked to that contract advances/resolves. |
| H11 | Persistence | Save/restart after sightings, active contract, exact case link, lore, and Field Guide unlock. | Society schema-2 state and investigation sidecar schema-1 state reopen without loss or duplicate migration. |
| H12 | Client/UI | Inspect all seven Field Guide categories/ten entries in EN and IT; Recent Discoveries; contract feedback/particles. | No empty category, unresolved key, wrong provider page, duplicate discovery, or client crash. |

## Historical 0.2 evidence

The following evidence belongs to **0.2.0** and must not be presented as a new 0.3.1 runtime pass:

- 53 JUnit tests passed in the recorded frozen 0.2.0 source.
- 3 NeoForge GameTests passed.
- Mandatory-only, exact-adapter, curated headless, final dedicated-server, and fresh-world smokes passed for 0.2.0.
- Graphical 0.2.0 startup reached the title/resource-reload state and exited normally, but no world was joined; its in-world/UI row remained blocked.
- The schema-1-to-2 unit fixture passed, but no authentic user-created 0.1 world was upgraded end-to-end; that migration row remained blocked.
- The frozen 0.2.0 production JAR was byte-reproducible and independently audited. Its historical SHA-256 is `CCA1A4FE4F3D53A6F891FE05F51095EEE26A048CD6E850738100A4423176EBC7`.

The curated 0.2 provider staging also logged an unowned NeoForge `RuntimeDistCleaner` request for `net.minecraft.client.gui.screens.Screen` on `DEDICATED_SERVER`. Dark Folklore was not identified as its owner and startup/save/shutdown continued, but a full-pack run must not be called warning-free until the originating provider is known.

## Production classification

Compilation, unit tests, GameTests, and a clean JAR audit are necessary but insufficient. 0.3.1 must remain **`RELEASE_CANDIDATE`** until the intended-pack in-world/client acceptance above is actually recorded. In particular, do not infer `PRODUCTION_READY` from a green CI run, and do not treat unit migration fixtures as an authentic world-upgrade test.
