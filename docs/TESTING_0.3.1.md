# Dark Folklore Core 0.3.1 testing

This file records evidence generated specifically for the 0.3.1 hardening branch. Historical 0.2 evidence remains in `TESTING.md` and must not be presented as 0.3.1 validation.

## Latest automated gate

Exact predation-hardening head: `f3a8635a15d46855e1eb4e0d78d2f5ab69a5b917`.

GitHub Actions run `31590162424` completed successfully on the PR merge ref for that head with Ubuntu 24.04 and Java 21.

Recorded evidence:

- Gradle wrapper validation: **PASS**;
- `./gradlew clean build --no-daemon --no-configuration-cache --stacktrace`: **PASS**;
- Java compilation and test compilation: **PASS**;
- `auditReleaseJar`: **PASS**;
- JUnit: **78/78 PASS**, 0 failures, 0 errors, 0 skipped;
- NeoForge `runGameTestServer`: **3/3 PASS**;
- validated reload: **17 canonical concepts, 5 weaknesses, 8 spawn profiles, 2 magic integrations, 9 investigation profiles, 13 story templates, 4 organization archetypes, 6 political overrides, 0 invalid**;
- production artifact upload: **PASS**.

Production JAR from that run:

| Property | Value |
| --- | --- |
| File | `darkfolklore-core-0.3.1.jar` |
| Size | `457,738` bytes |
| SHA-256 | `A62623ED93B3912FDFB009DE62382B5C1CA7F014F7EAE9ACD5F2B0AF390BD10F` |
| Class files | `182` |
| Java class version | 65 / Java 21, enforced by JAR audit |

## 0.3.1-specific automated coverage

The branch now covers the investigation hardening plus the Vampire Society & Predation policy. Automated checks include:

- story/contract/culprit continuity and confirmed-death fallback semantics;
- cancelled/rescued death finality;
- concept-level creature sightings and investigation-sidecar persistence;
- knowledge-gated preparation and evidence-only hypotheses;
- Fae/`GLAMOUR_TRACE` resource coverage;
- Field Guide resource consistency;
- deterministic predation policy: low-risk MCA vampires may prefer isolated adult civilians, rising local/personal suspicion pushes them toward animals, visible witnesses penalize civilian attacks, children/close family/hunters/supernatural targets/named non-MCA targets are rejected, wild Vampirism mobs react less strongly to social risk, and autonomous feeding is night-only;
- existing society, rumor, organization, canonicalization, wolfsbane and weakness regressions.

## Exact-provider audit boundary

The predation bridge activates only when exact audited versions are present: Vampirism `1.10.12`, MCA Reborn `7.7.32+1.21.1`, and MCA Reborn x Vampirism Compat `2.0.12`. Unsupported/missing versions fail closed.

The exact user-supplied MCA Vamp Compat 2.0.12 JAR audited during development had SHA-256:

`BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`

Dark Folklore does **not** redistribute or shade that JAR. The normal CI intentionally runs without the full optional provider pack, so a green CI proves compilation, resources, pure policy and Core runtime regressions, not the real three-mod predation/infection interaction.

The audited provider path is intentionally ownership-preserving:

```text
wild Vampirism vampire
 -> real Vampirism blood drain / BloodDrinkEvent
 -> MCA Vamp Compat observes the native event
 -> MCA Vamp Compat alone decides configured infection/conversion

MCA vampire
 -> Dark Folklore chooses a socially appropriate target
 -> MCA Vamp Compat native AI performs human infection bites
 -> provider owns infection/conversion/cure
```

For MCA vampires choosing animals, Dark Folklore uses Vampirism's exact creature blood attachment to drain/sync animal blood while honoring MCA Vamp Compat's native bite cooldown. It does not infect or replace the animal.

## GameTest coverage

The current three live NeoForge GameTests verify validated datapack state, separation of factual supernatural state from social belief, and deterministic organization-leader death/succession cleanup. They do not install the optional provider pack and therefore do not prove exact MCA/Vampirism runtime behavior.

## Required full-pack manual acceptance

| Area | Required action | Expected result |
| --- | --- | --- |
| Diagnostics | Run `/folklore diagnostics` and `/folklore predation status`. | `invalid=0`; exact provider adapters truthful and predation bridge active with intended versions. |
| Wild vampire → named MCA | At night place an eligible hungry Vampirism vampire near an adult MCA civilian. | Named MCA is no longer excluded merely because it has a custom name; a real feed can occur. |
| Native infection | Let the wild vampire feed on MCA with provider infection enabled. | Real provider `BloodDrinkEvent` reaches MCA Vamp Compat; only provider rules decide infection. |
| Same-person conversion | Allow infected MCA to convert. | The same MCA character/entity remains the social person; Core does not replace it with `vampirism:vampire`. |
| MCA vampire social stealth | Compare low-suspicion and high-suspicion MCA vampires with both civilian and animal prey available. | Low risk may choose isolated adult civilian; high risk strongly prefers animal feeding. |
| Protections | Place child MCA, close family, known hunter, supernatural target and named non-MCA targets nearby. | Autonomous predation rejects them according to policy. |
| Witnessed nonlethal feed | Let an MCA victim survive a witnessed bite. | `BITE_MARK` + `BLOOD`, direct victim knowledge, witness/rumor propagation and `feeding_assault` story occur without fake death. |
| Hunter pressure | Repeat witnessed feeds within bounded policy. | Village suspicion/awareness and Hunter Society pressure rise without unbounded spawning/scanning. |
| Anti-chaos | Keep several vampires in one region. | Per-predator/per-victim cooldowns and rolling regional feed budget prevent constant feeding spam. |
| Vampire investigation | Accept a resulting Vampire contract and investigate. | Existing evidence/hypothesis/identification/culprit continuity remains correct. |
| Cure | Cure an MCA vampire through MCA Vamp Compat. | Provider cure owns factual transition; Dark Folklore must stop treating the cured NPC as a predator while historical beliefs may persist. |
| Inheritance | Produce a child through provider-supported MCA reproduction with vampire parent(s). | MCA Vamp Compat owns inheritance; Core only observes the resulting factual state. |
| Save/restart | Restart with active stories, lore, sightings and converted/infected NPCs. | Core persistence reopens safely and provider factual state remains authoritative. |
| Field Guide / EN+IT | Exercise Wraith KEEP_DISTINCT, Sprite/FAE and Recent Discoveries in both languages. | Correct provider page/localization, no raw keys. |

## Classification

0.3.1 remains **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`. Automated verification is green, but exact optional-provider runtime, client UI and authentic-world migration remain manual promotion gates.
