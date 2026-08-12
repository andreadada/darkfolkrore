# Dark Folklore Core 0.6.0 — Release Gate

Status: **DEVELOPMENT / RELEASE_CANDIDATE ONLY** until every required full-pack gate below is evidenced.

## Scope delivered

0.6 is a stacked successor to 0.5 recipe weaving. It adds:

- explicit bounded vampire predation phases and active wild-Vampirism -> MCA hunting;
- sheltered-daytime/open-sky environment gating;
- detailed predation trace diagnostics;
- capability-scoped predation bridge failure isolation;
- strict MCA authority routing: an MCA entity can never fall through to generic wild-Vampirism predation if the MCA factual circuit fails;
- progressive knowledge dossier facets;
- seven cross-mod magic disciplines and lore discovery;
- visible village response tiers with major changes persisted through the existing story engine;
- a unified world-loop status command for end-to-end validation;
- bounded runtime caches for traces, sessions and village observations plus expired directed-target cleanup;
- Atlas recipe graph audit and exact reference-pack version verifier;
- release CI that validates Atlas tooling and publishes both JAR and audit artifacts.

All inherited 0.3.1/0.4/0.5 ownership and hardening rules remain mandatory.

## Final automated evidence

Final clean code commit: `44f86f35c7d82abab6f213e23105ba2ea998355c`.

GitHub Actions push run `31630231534`: **PASS**.

- Java 21 clean build: PASS
- JUnit: **141/141 PASS**, 0 failures, 0 errors, 0 skipped
- NeoForge GameTests: **3/3 PASS**
- release JAR audit: PASS
- Core data reload: **17 canonical concepts, 5 weaknesses, 8 spawn profiles, 2 magic integrations, 9 investigation profiles, 15 story templates, 4 organization archetypes, 6 political overrides, 0 invalid**
- Atlas tool syntax validation: PASS
- committed baseline Atlas recipe graph audit: PASS
- baseline audit: 147 mods, 11,296 cross-mod ingredient uses, 176 namespace bridges
- artifact: `darkfolklore-core-0.6.0.jar`
- JAR size: **576,563 bytes**
- JAR SHA-256: `E70C79CE9D4FA0465145434474C63081696613315BAA50AD6F3000F5386A67A6`
- class count: **233**
- production JAR artifact ID: **9154801482**
- Atlas audit artifact ID: **9154802118**

The branch may contain a later documentation-only commit; the binary identity above is anchored to the clean functional commit and must remain unchanged by documentation edits.

Standard CI deliberately does not install the complete optional provider pack, so the evidence above proves code/resource integrity rather than full provider gameplay.

## Full intended-pack blockers

Do **not** mark `PRODUCTION_READY` until the intended client/server instance proves all of:

1. fresh Atlas scan passes `verify_reference_pack.py`;
2. wild Vampirism vampire actively hunts and feeds on an adult MCA human;
3. MCA Vamp Compat alone decides subsequent infection/conversion;
4. converted MCA vampire uses provider-native AI to select and bite a human;
5. native bite evidence correlation is exact and non-duplicated;
6. cure start/cancel/complete transitions work in-world;
7. inherited vampire child remains same MCA character with no fake sire;
8. provenance survives save/restart;
9. Field Guide real client UI unlocks all intended foreign implementations and never leaks hidden weakness early;
10. end-to-end investigation works from incident through evidence/testimony/occult analysis/preparation/hunt/turn-in;
11. village response tiers change from real persisted pressure without spam or fake provider NPCs;
12. deep-magic discipline discovery works with installed provider items;
13. all 0.5 woven recipes load in real custom serializers and appear correctly in JEI;
14. new Atlas recipe audit shows no unintended exact-only regression, duplicate-output loop or KEEP_DISTINCT collapse;
15. dedicated server start/save/stop/restart is clean;
16. client start/reload/resource handling is clean.

## Promotion rule

Automated CI proves code/resource integrity, not provider gameplay. A green CI alone is insufficient to merge stacked 0.6 into `main` or label it production-ready.

Recommended order:

```text
PR #3 (0.5 recipe weaving) full-pack validation
 -> merge 0.5
 -> retarget/rebase 0.6 onto main
 -> run complete 0.6 intended-pack matrix
 -> only then merge/promote
```
