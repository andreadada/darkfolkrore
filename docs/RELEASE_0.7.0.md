# Dark Folklore Core 0.7.0 — Release Gate

Status: **DEVELOPMENT / RELEASE_CANDIDATE ONLY** until the full intended-pack behavior matrix is evidenced.

## Scope

0.7 is stacked on 0.6 and adds deterministic vampire behavioral predation:

- stable `CONTROLLED`, `CAUTIOUS`, `PREDATOR`, `RIPPER`, `RECRUITER`, `VENGEFUL` profiles;
- explicit session intents `FEED`, `RECRUIT`, `OVERFEED`, `KILL_AFTER_FEED`, `KILL_FOR_SPORT`, `PROVIDER_OWNED`, `NONE`;
- deterministic predator/victim/day lethal decisions rather than scan-loop rerolls;
- human/animal/witness/risk/grievance-aware preference scoring;
- bounded Ripper extra feeding using real Vampirism blood operations;
- deliberate wild-vampire combat without fake infection calls;
- exact final-death-gated `feeding_murder` narrative consequence;
- behavior-aware predation diagnostics;
- configurable bounded behavior rates;
- regression tests and intended-pack manual matrix.

## Authority boundary

0.7 does not change the existing provider ownership contract:

- Vampirism owns real blood drain and the real blood event;
- MCA Vamp Compat owns MCA infection eligibility/chance/duration, conversion, cure, inheritance, capability persistence, MCA vampire target/navigation and native AI;
- Dark Folklore does not expose or call a generic force-infection/conversion/cure path;
- a `RECRUIT` intent deliberately stops after a real nonlethal bite but cannot guarantee infection;
- `KILL_FOR_SPORT` never invokes blood/infection mutation;
- an MCA vampire may receive a narrative/debug profile, but human predation intent remains `PROVIDER_OWNED` and Core never redirects that target.

## Automated evidence

The final branch evidence is recorded after the current 0.7 GitHub Actions gate completes. Required automated evidence is:

- Java 21 clean build PASS;
- all JUnit PASS with 0 failures/errors;
- 3/3 NeoForge GameTests PASS;
- release JAR audit PASS;
- atomic resource reload with 0 invalid and `feeding_murder` included;
- production `darkfolklore-core-0.7.0.jar` identity recorded;
- Atlas tooling/audit artifact PASS.

A green standard CI still does not prove optional-provider runtime gameplay because the CI GameTest instance intentionally does not ship the complete modpack provider set.

## Full intended-pack blockers

Do **not** mark 0.7 `PRODUCTION_READY` until the exact intended instance proves:

1. profile stability across unload/reload/save/restart;
2. Controlled animal preference and non-intentional-lethal human feeding;
3. Cautious witness/risk avoidance;
4. Predator human preference and deterministic FEED/KILL_AFTER_FEED behavior;
5. Recruiter isolated-human preference, one real nonlethal feed, deliberate stop, and provider-only infection decision;
6. Ripper bounded real overfeeding and transition to native lethal combat without fabricated blood drain;
7. satiated Ripper sport-kill gate under deterministic configured rate;
8. Vengeful exact confirmed-identity witness preference, with no grievance against rumor-only/unrelated civilians;
9. every inherited protected-target/fail-closed/daylight/combat-target rule remains intact;
10. converted MCA vampire target/navigation remains provider-native and `PROVIDER_OWNED`;
11. finalized exact-pair lethal deaths create one `feeding_murder`, while cancelled/rescued/resurrected/different-pair deaths do not;
12. village/Hunter consequence is proportionate and does not create incident spam;
13. `vampireBehaviorProfiles=false` restores hunger-driven 0.6 orchestration;
14. chance/extra-feed config bounds are respected;
15. no stale target/session/lethal-attribution state survives incorrectly across unload/server restart;
16. every full-provider, Field Guide, investigation, recipe/JEI, Atlas and save/restart gate inherited from 0.6 still passes.

See `docs/VAMPIRE_BEHAVIOR_0.7.md` and `docs/TESTING_0.7.0.md`.

## Promotion order

```text
PR #3 / 0.5 validate + merge
 -> PR #4 / 0.6 rebase, validate + merge
 -> PR #5 / 0.7 rebase onto resulting main
 -> rerun complete 0.7 intended-pack matrix
 -> only then promote/merge 0.7
```
