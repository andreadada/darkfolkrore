# Dark Folklore Core 0.3.1 testing

> **Historical evidence:** this document intentionally preserves the final 0.3.1 branch gate. Its “stacked 0.4 follow-up” wording describes the state at that release and is not current branch topology. Use [TESTING_0.4.0.md](TESTING_0.4.0.md) for current evidence.

This file records evidence generated specifically for the 0.3.1 hardening branch. Historical 0.2 evidence remains in `TESTING.md` and must not be presented as 0.3.1 validation.

## Latest code gate

Latest code-changing 0.3.1 head: `dd15493d0d18168731ce5dcdc900c75e0cc90182`.

GitHub Actions run `31601452108` completed successfully for PR #1 with Ubuntu 24.04 and Java 21.

Recorded evidence:

- Gradle wrapper validation: **PASS**;
- `./gradlew clean build --no-daemon --no-configuration-cache --stacktrace`: **PASS**;
- Java compilation and test compilation: **PASS**;
- `auditReleaseJar`: **PASS**;
- JUnit: **89/89 PASS**, 0 failures, 0 errors, 0 skipped;
- NeoForge `runGameTestServer`: **3/3 PASS**;
- validated reload: **17 canonical concepts, 5 weaknesses, 8 spawn profiles, 2 magic integrations, 9 investigation profiles, 13 story templates, 4 organization archetypes, 6 political overrides, 0 invalid**;
- production artifact upload: **PASS**.

Production JAR from that run:

| Property | Value |
| --- | --- |
| File | `darkfolklore-core-0.3.1.jar` |
| Size | `464,700` bytes |
| SHA-256 | `C4BA5810394074E3C90EEECD231352ED629D587B4F23AAE04B0F61CE78CB4001` |
| Class files | `186` |
| Java class version | 65 / Java 21, enforced by JAR audit |

## 0.3.1-specific automated coverage

The branch covers investigation hardening plus Vampire Society & Predation. Automated checks include:

- story/contract/culprit continuity, exact testimony subjects and incident actor/location/dimension correlation;
- one central next-tick confirmed-death dispatcher for contracts, lore, investigations, stories, organizations and Field Guide unlocks;
- cancelled deaths, rescued/revived entities and live same-UUID replacements fail closed before any death-dependent mutation;
- culprit and issuer fallback policy is exact-identity and final-death-only;
- investigation sidecar admission and adversarial duplicate-row loads are bounded without silently weakening exact targeting;
- concept-level creature sightings and investigation-sidecar persistence;
- knowledge-gated preparation and evidence-only hypotheses;
- Fae/`GLAMOUR_TRACE` resource coverage;
- Field Guide resource consistency;
- deterministic predation policy: low-risk MCA vampires may prefer isolated adult civilians, rising local/personal suspicion pushes them toward animals, visible witnesses penalize civilian attacks, children/close family/hunters/supernatural targets/named non-MCA targets are rejected, wild Vampirism mobs react less strongly to social risk, and autonomous feeding is night-only;
- existing society, rumor, organization, canonicalization, wolfsbane and weakness regressions.

The final code gate additionally verifies compilation after provider- and continuity-safety fixes:

- entity `BloodDrinkEvent` observation now runs at `LOWEST`, after MCA Vamp Compat's normal handler, so a provider-blocked/zeroed drain cannot manufacture Core evidence or cooldown state;
- malformed conversion lineage whose provider source UUID equals the descendant UUID is ignored rather than passed to `LineageRecord`.
- applicable `UNKNOWN` supernatural/relationship facts are rejected during autonomous target selection rather than treated as mundane/acceptable;
- provider classification occurs only after the 40-tick stagger gate, keeping reflection out of the every-entity/every-tick path.

## Exact-provider audit boundary

The predation bridge activates only when exact audited versions are present: Vampirism `1.10.12`, MCA Reborn `7.7.32+1.21.1`, and MCA Reborn x Vampirism Compat `2.0.12`. Unsupported/missing versions fail closed.

The exact user-supplied MCA Vamp Compat 2.0.12 JAR audited during development had SHA-256:

`BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`

Dark Folklore does **not** redistribute or shade that JAR. Normal CI intentionally runs without the full optional provider pack, so green CI proves compilation, resources, pure policy and provider-absent Core runtime regressions, not the real three-mod predation/infection interaction.

The audited provider path is ownership-preserving:

```text
wild Vampirism vampire
 -> real Vampirism blood drain / BloodDrinkEvent
 -> MCA Vamp Compat handles the event first
 -> MCA Vamp Compat alone decides configured infection/conversion
 -> Dark Folklore observes the finalized event at LOWEST

MCA vampire
 -> Dark Folklore chooses a socially appropriate target
 -> MCA Vamp Compat native AI performs human infection bites
 -> provider owns infection/conversion/cure
```

For MCA vampires choosing animals, Dark Folklore uses Vampirism's exact creature blood attachment to drain/sync animal blood while honoring MCA Vamp Compat's native bite cooldown. It does not infect or replace the animal.

## GameTest coverage

The current three CI NeoForge GameTests verify validated datapack state, separation of factual supernatural state from social belief, and deterministic organization-leader death/succession cleanup. CI does not install the optional provider pack and therefore does not prove exact MCA/Vampirism runtime behavior.

An additional local dedicated GameTest-server run loaded the exact development modpack, including MCA Reborn `7.7.32+1.21.1`, Vampirism `1.10.12`, MCA Reborn x Vampirism Compat `2.0.12` and Field Guide `1.14.0`, and passed the same **3/3** Core GameTests. This proves exact-pack startup compatibility for that automated server gate only; it is not a substitute for the client-side and in-world scenarios below.

## Required full-pack manual acceptance

| Area | Required action | Expected result |
| --- | --- | --- |
| Diagnostics | Run `/folklore diagnostics` and `/folklore predation status`. | `invalid=0`; exact provider adapters truthful and predation bridge active with intended versions. |
| Wild vampire → named MCA | At night place an eligible hungry Vampirism vampire near an adult MCA civilian. | Named MCA is no longer excluded merely because it has a custom name; a real feed can occur. |
| Native infection | Let the wild vampire feed on MCA with provider infection enabled. | Real provider `BloodDrinkEvent` reaches MCA Vamp Compat; only provider rules decide infection. |
| Same-person conversion | Allow infected MCA to convert. | The same MCA character/entity remains the social person; Core does not replace it with `vampirism:vampire`. |
| MCA vampire social stealth | Compare low-suspicion and high-suspicion MCA vampires with both civilian and animal prey available. | Low risk may choose isolated adult civilian; high risk strongly prefers animal feeding. |
| Protections | Place child MCA, close family, known hunter, supernatural target, tamed animal and named non-MCA target nearby. | Autonomous predation rejects them according to policy. |
| Witnessed nonlethal feed | Let an MCA victim survive a witnessed bite. | `BITE_MARK` + `BLOOD`, direct victim knowledge, witness/rumor propagation and `feeding_assault` story occur without fake death. |
| Hunter pressure | Repeat witnessed feeds within bounded policy. | Village suspicion/awareness and Hunter Society pressure rise without unbounded spawning/scanning. |
| Anti-chaos | Keep several vampires in one region. | Per-predator/per-victim cooldowns and rolling regional feed budget prevent constant feeding spam. |
| Vampire investigation | Accept a resulting Vampire contract and investigate. | Existing evidence/hypothesis/identification/culprit continuity remains correct. |
| Save/restart | Restart with active stories, lore and sightings. | Core persistence reopens safely; transient predation caches reset without changing provider facts. |
| Field Guide / EN+IT | Exercise Wraith KEEP_DISTINCT, Sprite/FAE and Recent Discoveries in both languages. | Correct provider page/localization, no raw keys. |

Cure/inheritance lifecycle observation is intentionally the stacked 0.4 follow-up rather than being falsely claimed as 0.3.1 functionality.

## Classification

0.3.1 remains **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`. Automated verification is green, but exact optional-provider runtime, client UI and authentic-world migration remain manual promotion gates.
