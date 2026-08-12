# Dark Folklore Core 0.4.0 testing

This document records the current 0.4.0 automated evidence and the still-unrun manual acceptance matrix. Historical 0.3.1 evidence remains in [TESTING_0.3.1.md](TESTING_0.3.1.md); it is not relabeled as a 0.4 pass.

## Current local automated gate

Executed locally on Java 21 against the current working 0.4 integration:

```powershell
\.\gradlew.bat clean build runGameTestServer --no-daemon
```

| Check | Result |
| --- | --- |
| Java compilation and test compilation | **PASS** |
| JUnit | **119/119 PASS**, 0 failures, 0 errors, 0 skipped |
| `auditReleaseJar` | **PASS** |
| NeoForge GameTests | **3/3 PASS** |
| Exact local provider-stack dedicated startup | **PASS** |
| Compatibility reports in exact local stack | Vampirism 1.10.12, Werewolves 2.0.3.3, MCA 7.7.32+1.21.1, MCA Vamp Compat 2.0.12, and Field Guide 1.14.0 reported `ACTIVE` |
| Atomic data reload | `17 canonical / 5 weaknesses / 8 spawn / 2 magic / 9 investigation / 13 stories / 4 organizations / 6 political`, `0 invalid` |

The exact-stack run reused a local automated GameTest world and reported an expected `darkfolklore 0.3.1 -> 0.4.0` version-change warning. The wider external staging also emitted an unowned `RuntimeDistCleaner` client-class warning while the dedicated server continued and all GameTests passed. Neither warning is represented as a clean full-pack manual smoke.

## Final-head CI and artifact identity

These fields are intentionally pending until the final feature/documentation head receives a green GitHub Actions run. Do not reuse the earlier pre-rebase 0.4 run, head, size, or hash.

| Property | Final value |
| --- | --- |
| Commit | **PENDING FINAL HEAD** |
| GitHub Actions run | **PENDING** |
| Production artifact | `darkfolklore-core-0.4.0.jar` — **PENDING FINAL CI IDENTITY** |
| JAR size | **PENDING** |
| SHA-256 | **PENDING** |
| Class files | **PENDING** |

Optional provider JARs must not be shaded into the production artifact. The `-sources.jar` is a development artifact and must not be installed in the pack.

## 0.4-specific automated coverage

The 119-test suite retains all 0.3.1 regressions and adds focused coverage for:

- exact lifecycle state/transition classification;
- infection start, native-bite conversion, other conversion, inherited vampirism, cure start/cancel/completion, infection clear, and missed-intermediate-sample vampirism clear;
- cure-cancellation precedence when provider 2.0.12 retains inheritance, bite-cause, or source metadata;
- at-least-one-tick join delay and bounded 200-tick retry for a late provider capability;
- durable valid provider provenance, self-source rejection, and no fabricated inherited conversion source;
- strict MCA factual routing: active provider facts are authoritative, absent is `NOT_APPLICABLE`, and untested/partial/unsupported/error is `UNKNOWN` without generic fallback;
- independent fact/predation/lifecycle component status aggregation;
- exact native-bite attribution to the same incoming-damage event and direct attacker/target pair only when the provider attacker capability changes ready→cooldown;
- provider-valid native targets beyond MCA victims (player and vanilla-human targets are not excluded by an MCA-only victim marker);
- suspicion-aware candidate scoring, child/family/hunter/supernatural/tamed/named-non-MCA protections, cooldowns, and regional budgets;
- death finality, investigation continuity, knowledge-gated weaknesses, Fae/Field Guide resources, persistence, society, rumors, organizations, wolfsbane, and weakness regressions.

The three live GameTests prove validated data in a running server level, the fact/belief boundary for public social belief, and deterministic organization succession after confirmed death. They do not drive a real provider conversion/cure/inheritance lifecycle or render the Field Guide UI.

## Implemented safety properties

- MCA Vamp Compat facts, predation, and lifecycle are triple-gated on exact Vampirism + MCA + add-on versions and initialize independently.
- For MCA entities, the provider fact component is the only factual vampire/werewolf/hunter route.
- Provider absence/mismatch/failure cannot become a false factual negative or a generic-adapter answer.
- Core does not force infection, conversion, cure, or inheritance.
- The provider owns target selection, navigation, and native MCA vampire AI. Core does not set/clear targets or navigation paths.
- A bounded Core narrative session survives for a human candidate only when provider-native AI independently chose that same target.
- Native feed evidence requires the same direct attacker/target damage event and a provider attacker-capability ready→cooldown transition across the event.
- Cure and cleared transitions cancel only Core's session; historical belief remains.
- Initial capability observation cannot run before join+1 tick and retries only through a 200-tick window.
- Sampling is loaded-entity-only and staggered; no chunk is force-loaded.
- Runtime event correlations and observation caches are cleared on server stop.

## What the automated gate does not prove

Even the exact local dedicated GameTest startup does **not** prove:

```text
real in-world named-MCA feeding
+ provider-configured infection
+ same-character conversion
+ native converted-MCA target/navigation/bite behavior
+ real cure cancellation/completion
+ inheritance through MCA reproduction
+ authentic world save/restart migration
+ Field Guide pages, translations, models, toasts, or Recent Discoveries in a joined client world
```

No manual client or in-world row below has been run or claimed.

## Required exact-stack manual matrix — NOT RUN

| Area | Required action | Expected result |
| --- | --- | --- |
| Adapter status | Start the exact intended pack; run `/folklore diagnostics`, `/folklore predation status`, and `/folklore lifecycle status`. | Exact stack is truthfully active; component status is visible; `invalid=0`. |
| Wild feed | At night place a hungry wild Vampirism vampire near a named adult MCA civilian. | A real Vampirism blood drain occurs without globally removing custom-name protection. |
| Native infection | Enable provider infection and allow the wild feed. | Real `BloodDrinkEvent` reaches MCA Vamp Compat; provider alone decides infection. |
| Initial observation | Load/join an MCA entity whose provider capability attaches late. | No sample before join+1; capability is observed if it becomes available within the bounded 200-tick retry. |
| Same-person conversion | Let provider conversion finish. | The same MCA social character remains; Core observes `VAMPIRE` and creates no replacement entity. |
| Provenance | Inspect converted victim/source and restart. | Valid provider source UUID persists as provenance; self-source is rejected; it is not labeled biological parent/sire. |
| Native AI ownership | Observe a converted MCA vampire after conversion and restart. | Provider goals/target/navigation/bite own behavior; Core neither installs replacement AI nor commands target/path. |
| Native bite evidence | Allow provider-native bites against eligible MCA, player, and vanilla-human targets; include pre-canceled/redirected failures and provider post-success damage cancellation. | Evidence appears only for the exact direct event whose provider attacker capability changes ready→cooldown. Provider post-success damage cancellation does not hide a real feed; attempts with no cooldown transition produce none. |
| Risk policy | Compare isolated-civilian/animal options under low and high awareness/suspicion/Hunter pressure. | Narrative candidate preference shifts toward lower-risk feeding without Core commanding provider target/navigation. |
| Protections | Offer child MCA, close family, known hunter, supernatural NPC, tamed animal, and named non-MCA candidates. | Core's autonomous candidate policy rejects them. |
| Nonlethal confirmed feed | Let a target survive a witnessed confirmed feed. | `BITE_MARK`/`BLOOD`, applicable victim belief, witnesses/rumor/Hunter pressure, and `feeding_assault` occur once; no fake death story. |
| Cure start | Start provider cure on a converted MCA person. | Lifecycle reports `CURING`; Core animal-feed session is blocked/canceled while provider behavior remains provider-owned. |
| Cure cancellation | Cancel provider cure while retained metadata is present. | `CURING → VAMPIRE` reports `CURE_CANCELLED`, not conversion/inheritance. |
| Cure completion | Let provider cure finish. | Factual state becomes human; only Core's session is canceled; provider target/navigation is untouched and historical belief remains. |
| Inheritance | Use real provider-supported MCA reproduction involving vampire parent(s). | Provider decides inherited state; Core records `INHERITED_VAMPIRE` only with provider metadata/recent birth context and fabricates no source. |
| Restart | Save/restart with infected, converted, curing, and inherited MCA NPCs. | Provider remains authority; Core resamples, recovers valid provenance, and creates no duplicate social fact. |
| Field Guide | Open all Dark Folklore categories/pages in EN and IT and exercise scan/kill/lore unlocks and Recent Discoveries. | Seven categories and ten entries render with no raw keys/empty pages/duplicates; persistence works. |
| 0.3.1 loop | Complete contract, clue, testimony, magical analysis, tracking, hunt, issuer/culprit fallback, and Fae/KEEP_DISTINCT cases. | 0.4 does not regress the merged 0.3.1 loop. |
| Authentic world | Back up and upgrade an actual intended-pack world; restart at persistence milestones. | No destructive rewrite, lost provider state, duplicate migration, or class-loading failure. |

## Classification

0.4.0 remains **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`, until final-head CI/artifact identity and the exact-provider/full-pack manual matrix are recorded.
