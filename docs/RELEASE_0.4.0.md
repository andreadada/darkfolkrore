# Dark Folklore Core 0.4.0 release gate

## Target

| Property | Value |
| --- | --- |
| Mod ID | `darkfolklore` |
| Version | `0.4.0` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.248` / 21.1 line |
| Java | 21, class-file major 65 |
| Society persistence | schema 2 |
| Investigation sidecar | schema 1 |
| Classification | **`RELEASE_CANDIDATE`** |

0.4.0 integrates the exact MCA vampire lifecycle on top of the merged 0.3.1 investigation hardening. It is not a second infection, conversion, cure, inheritance, targeting, navigation, or AI implementation.

## Authority boundary

> MCA Reborn × Vampirism Compat remains authoritative for factual supernatural MCA mechanics. Dark Folklore observes those mechanics and implements knowledge, investigation, rumor, reputation and narrative consequences around them.

Provider state is **FACT**. Dark Folklore social/investigation state is **BELIEF**. Belief cannot create or override a fact.

For MCA entities, exact MCA Vamp Compat is the only factual vampire/werewolf/hunter route. The provider being absent yields `NOT_APPLICABLE`; an untested, partial, unsupported, or failed authority yields `UNKNOWN`. Core never falls through to a generic Vampirism fact for an MCA person.

The exact factual, predation, and lifecycle components require Vampirism 1.10.12 + MCA 7.7.32+1.21.1 + MCA Vamp Compat 2.0.12. They initialize independently so one failure cannot suppress healthy siblings; combined status reports `ACTIVE`, `PARTIAL`, or `ERROR`.

## Current automated evidence

Local Java 21 gate:

| Check | Result |
| --- | --- |
| Clean build | **PASS** |
| JUnit | **119/119 PASS**, 0 failures/errors/skipped |
| Release JAR audit | **PASS** |
| NeoForge GameTests | **3/3 PASS** |
| Exact provider-stack dedicated GameTest startup | **PASS**; audited optional versions reported `ACTIVE` |
| Data reload | `17/5/8/2/9/13/4/6`, `0 invalid` |

The exact local startup is automated dedicated-server evidence, not a manual client/in-world pack pass.

## Final-head CI/artifact — pending

The earlier pre-rebase 0.4 CI run and artifact are superseded. Populate this table only from the final green documentation head:

| Property | Final value |
| --- | --- |
| Commit | **PENDING FINAL HEAD** |
| GitHub Actions run | **PENDING** |
| Artifact | `darkfolklore-core-0.4.0.jar` — **PENDING** |
| JAR size | **PENDING** |
| SHA-256 | **PENDING** |
| Class files | **PENDING** |

The production artifact must contain Java 21 classes, required resources/license/metadata, no test/cache/local paths, no nested provider JARs, and no shaded optional-provider packages. The sources JAR is development-only and must not be installed.

## Exact provider binary audited

User-supplied MCA Reborn × Vampirism Compat 2.0.12 JAR SHA-256:

`BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`

It is not committed, redistributed, or shaded. See [the exact-provider audit](MCA_VAMP_COMPAT_2.0.12_AUDIT.md).

## Release-critical 0.4 behavior

- Exact provider snapshots classify `HUMAN`, `INFECTED`, `VAMPIRE`, and `CURING` plus their meaningful transitions.
- Initial lifecycle observation cannot run before entity join+1 tick; an unavailable capability is retried only through a bounded 200-tick window.
- `CURING → VAMPIRE` is classified as `CURE_CANCELLED` before retained inheritance/bite/source metadata is considered.
- Valid provider source UUID is durable provenance recoverable after load; self-source is rejected. Provenance is not necessarily a biological parent or universally reliable sire.
- Inherited vampirism keeps both-parent birth context and fabricates no conversion source.
- Provider owns target selection, navigation, and native MCA vampire AI. Core can ask only the audited idempotent provider goal-repair method after factual conversion; it sets/clears no target or path.
- A bounded Core human-candidate session continues only when provider-native AI independently selected the same target.
- Native bite evidence is correlated to the exact same incoming-damage event and direct attacker/target pair, plus a provider attacker-capability ready→cooldown transition from `HIGHEST` to `LOWEST`. This covers provider-valid MCA, player, and vanilla-human targets without relying on an MCA-only victim marker. Provider post-success damage cancellation/zeroing does not suppress evidence; attempts without the cooldown transition do not create it.
- Wild feeding evidence separately observes the finalized real `BloodDrinkEvent` after the provider's normal handler and still requires a positive finalized amount.
- Cure/cleared transitions cancel only Core's scoped session; they do not mutate provider target/navigation or erase historical witnesses/rumors.
- Sampling is staggered and loaded-entity-only; no whole-world scan or chunk force-load is introduced.
- Runtime correlation/session/lifecycle caches clear on server stop.

## Persistence

No destructive schema bump is introduced:

- `darkfolklore_society`: schema 2;
- `darkfolklore_investigation`: schema 1;
- lifecycle snapshots, birth context, recent transition diagnostics, and bite-event correlation: transient;
- valid provider provenance uses existing Core lineage persistence;
- provider capability remains the sole saved authority for infection/conversion/cure/inheritance.

## Manual promotion blockers — not run

Before promotion, record intended-pack client/in-world evidence for:

- exact component/status diagnostics and `invalid=0`;
- real wild-vampire feeding from a named adult MCA civilian and provider-configured infection;
- join+1/late-capability observation behavior;
- same MCA character through provider conversion;
- provenance interpretation/recovery after restart;
- provider-native AI, target, navigation, and bite ownership after conversion/restart;
- exact native-bite evidence for eligible MCA/player/vanilla-human targets, including provider post-success damage cancellation and negative pre-canceled/redirected cases with no cooldown transition;
- low/high social-risk candidate policy and all prey protections;
- nonlethal evidence, witnesses, rumors, Hunter pressure, and one `feeding_assault` story;
- cure start, sticky-metadata cancellation, completion, and scoped Core-session cancellation;
- provider inheritance through real MCA reproduction;
- restart with infected/converted/curing/inherited NPCs;
- the complete merged 0.3.1 investigation/contract/culprit/issuer/Fae/KEEP_DISTINCT loop;
- all seven Field Guide categories and ten entries in EN/IT, scan/kill/lore unlocks, Recent Discoveries, and persistence;
- authentic backed-up world upgrade and complete-pack server/client acceptance.

No manual row above is claimed complete. The detailed matrix is in [TESTING_0.4.0.md](TESTING_0.4.0.md).

## Deferred work

Native ritual hooks for Enchanted, Occultism, Malum, Eidolon: Repraised, and Feywild remain a separate exact-provider Deep Magic phase rather than being simulated in 0.4.0.

## Classification

Current classification: **`RELEASE_CANDIDATE`**.

Do not promote to `PRODUCTION_READY` until final-head CI/artifact identity and the exact-provider/full-pack manual gates have passed and been recorded.
