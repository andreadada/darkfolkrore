# Dark Folklore Core 0.3.1 testing

This file records evidence generated for the 0.3.1 hardening branch. It is intentionally separate from `TESTING.md`, whose A-G smoke evidence belongs to the historical 0.2.0 release candidate.

## Automated gate

GitHub Actions runs on Ubuntu with Java 21 and performs, in order:

```text
Gradle wrapper validation
clean build
JUnit suite
resource validators
auditReleaseJar
JUnit result-count audit
NeoForge GameTest server
production JAR identity audit
artifact upload
```

The first fully instrumented 0.3.1 code run at commit `bdea1a9616b4c87081869e2f74585686742d783a` completed successfully in GitHub Actions run `31525348730`.

Recorded evidence from that run:

- clean `./gradlew clean build --no-daemon --no-configuration-cache --stacktrace`: **PASS**;
- Java compilation: **PASS**;
- test compilation: **PASS**;
- all listed JUnit tests: **PASS**;
- `auditReleaseJar`: **PASS**;
- NeoForge `runGameTestServer`: **PASS**;
- GameTest runtime loaded **17 canonical concepts, 5 weaknesses, 8 spawn profiles, 2 magic integrations, 9 investigation profiles, 12 story templates, 4 organization archetypes, and 6 political overrides with 0 invalid definitions**;
- all **3 required GameTests passed**;
- GameTest server shut down normally and saved all dimensions;
- production JAR was uploaded successfully.

The CI workflow now prints the aggregate JUnit XML count explicitly on subsequent runs so future documentation does not need to infer the total from Gradle's per-test log.

## 0.3.1-specific unit/resource coverage

The hardening branch adds or extends automated checks for:

- investigation profile JSON and canonical cross-definition consistency;
- nine curated profiles and real Fae/`GLAMOUR_TRACE` coverage;
- seven Field Guide categories and ten explicit provider-backed entries;
- `InvestigationCaseLink` story/culprit/implementation continuity;
- independent culprit- and issuer-fallback flags;
- concept-level `CreatureSightingRecord` monotonic merge, confidence bounds, and forgetting behavior;
- investigation-sidecar NBT round trips for sightings, incident facts, and contract links;
- sidecar pruning of weak old sightings;
- knowledge-gated preparation: `OBSERVED` cannot leak weakness metadata, while `STUDIED` can expose documented present/missing countermeasures;
- existing hypothesis ambiguity/tie-breaking semantics;
- existing contract state-machine behavior and post-identification research;
- existing society, rumor, organization, persistence, canonicalization, Field Guide, wolfsbane, and weakness regression coverage.

## GameTest coverage

The current three live NeoForge GameTests remain focused regression tests rather than a complete simulation of the new interactive loop. They verify:

1. validated datapack state is available in a real running level;
2. persisted social belief remains distinct from factual supernatural state;
3. confirmed organization-leader death performs deterministic succession and index cleanup.

They do **not** yet synthesize a complete 0.3.1 player interaction chain for contracts, Field Guide UI, culprit tracking, or provider-native items.

## Automated limitations

A green CI result does not establish all of the following:

- real client UI rendering or Recent Discoveries behavior;
- English/Italian visual presentation in Field Guide;
- a complete incident → testimony/analysis → identification → tracking → hunt → hand-in interaction performed by a player;
- exact-provider runtime behavior with the full curated modpack installed;
- exact `KEEP_DISTINCT` Wraith page unlock in a real Field Guide client session;
- issuer-death recovery with real MCA/villager entities;
- authentic old-world migration from a user-created 0.1/0.2 save;
- long-session performance under a populated multiplayer region.

Those remain manual promotion gates.

## 0.3.1 manual acceptance matrix

| Area | Required action | Expected result |
| --- | --- | --- |
| Diagnostics | Start intended pack and run `/folklore diagnostics`. | Version 0.3.1, `invalid=0`, optional adapters have truthful status. |
| Knowledge command | `/folklore knowledge grant @s darkfolklore:vampire 25`. | Namespaced argument parses and lore reaches expected stage. |
| Vampire investigation | Create a real vampire-caused incident, accept contract, collect clue, use Witchcraft analysis. | Hypotheses use evidence only; analysis adds supported occult signature; identification reaches `OBSERVED`. |
| Monster testimony | Let an NPC directly witness a Wendigo incident and request testimony. | Witness stores concept-level `darkfolklore:wendigo`; testimony can count for that case rather than generic `SUPERNATURAL_IDENTITY`. |
| Same-concept incident isolation | Create two same-concept incidents in one society region. | Each new contract remains linked to its exact story; resolving one does not advance the other. |
| Exact culprit | Track/hunt a known incident culprit while another same-concept creature is nearby. | Tracking prefers culprit UUID; killing the unrelated creature does not satisfy the contract. |
| Culprit unload | Unload/reload the culprit without killing it. | No fallback is authorized solely from unload. |
| Culprit confirmed death fallback | Cause confirmed culprit death outside valid owner hunt flow, then continue case. | Sidecar authorizes documented same-concept fallback rather than permanently stranding the contract. |
| Issuer death | Kill/remove issuer through a confirmed death event after accepting/hunting. | Local fallback activates; Hunter Society member is required when a local Hunter Society exists, otherwise valid local representative works. |
| Expired evidence | Let logical evidence expire, then attempt physical collection before/around maintenance pruning. | Evidence cannot be collected after expiry. |
| Weakness knowledge | Compare `OBSERVED` and `STUDIED` target lore. | `OBSERVED` hides countermeasure ground truth; `STUDIED` reveals documented options and enables prepared state if carried. |
| Prepared hunt | Hunt once without learned countermeasure and once after studying/carrying it. | Bonus only applies in the learned + prepared case. |
| KEEP_DISTINCT Field Guide | Investigate a specific provider Wraith. | Exact observed provider entry unlocks; other distinct Wraith page is not substituted. |
| Fae | Trigger a `feywild:sprite` case; collect physical evidence; perform Fae analysis. | Physical clues alone remain below threshold; Fae analysis yields `GLAMOUR_TRACE` and can identify Sprite. |
| Persistence | Save/restart with active case, sighting, lore and Field Guide progress. | Society schema 2 and investigation-sidecar schema 1 reopen without loss; case continuity remains intact. |
| Localization | Inspect Field Guide in EN and IT. | Fae category/Sprite text and existing categories resolve without raw keys. |

## Classification

0.3.1 is **`RELEASE_CANDIDATE`** after automated verification. It must remain below `PRODUCTION_READY` until the relevant in-world client/full-pack/manual gates above are actually performed and recorded.
