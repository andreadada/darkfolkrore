# Dark Folklore Core 0.4.0 testing

This document records automated evidence for the stacked 0.4.0 native MCA vampire lifecycle integration. It supplements the 0.3.1 hardening evidence and does not replace the exact full-modpack runtime gate.

## Latest verified stacked gate

Verified stacked branch head before this evidence refresh: `1c1bec00cad64bc7e8ef29462fce2b97ac00d445`.

GitHub Actions run `31593042850`: **PASS** with Ubuntu 24.04 and Java 21.

Recorded evidence:

- Gradle wrapper validation: **PASS**;
- `./gradlew clean build --no-daemon --no-configuration-cache --stacktrace`: **PASS**;
- Java compilation and test compilation: **PASS**;
- `auditReleaseJar`: **PASS**;
- JUnit: **84/84 PASS**, 0 failures, 0 errors, 0 skipped;
- NeoForge `runGameTestServer`: **3/3 PASS**;
- provider-absent GameTest startup: **PASS**; exact optional adapters correctly reported `DISABLED` rather than manufacturing facts;
- validated reload: **17 canonical concepts, 5 weaknesses, 8 spawn profiles, 2 magic integrations, 9 investigation profiles, 13 story templates, 4 organization archetypes, 6 political overrides, 0 invalid**;
- production artifact upload: **PASS**.

Production JAR from that verified gate:

| Property | Value |
| --- | --- |
| File | `darkfolklore-core-0.4.0.jar` |
| Size | `480,321` bytes |
| SHA-256 | `1FAA06E963FB3F28A3FD220EA8DB7B8A54B8B41B2D09820420D94078553DDC17` |
| Class files | `194` |
| Java class version | 65 / Java 21, enforced by `auditReleaseJar` |

## 0.4-specific automated coverage

Six new pure lifecycle tests increase the suite from 78 to 84 tests. They verify:

- `HUMAN -> INFECTED` is classified as infection start;
- `INFECTED -> VAMPIRE` with provider bite-cause metadata is distinguished as native-bite conversion;
- provider inheritance is recognized only with recent birth context, provider inheritance-processed state and no fabricated conversion source;
- cure start, cure cancellation and cure completion are distinct transitions;
- a missed intermediate cure sample still reports factual vampirism cleared rather than silently leaving Core in vampire state;
- cleared pre-conversion infection is not mislabeled as a vampire cure;
- unsupported/provider-absent snapshots and stable states do not manufacture transitions.

The existing 0.3.1 tests remain active, including suspicion-aware predation, child/family/hunter protections, death finality, investigation continuity, knowledge-gated weaknesses, Fae resources, persistence, society, rumors, organizations, wolfsbane and weakness regressions.

## Exact-provider audit assertions

Development inspection of the user-supplied MCA Reborn x Vampirism Compat 2.0.12 JAR (SHA-256 `BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`) established the exact signatures used by the 0.4 bridge. See `MCA_VAMP_COMPAT_2.0.12_AUDIT.md`.

Runtime reflection is constructor-validated and fail-closed. The lifecycle bridge reads provider state such as infection, conversion, cure, inheritance-processed, bite-conversion cause, AI-goal installation and conversion source. It does not expose Core APIs to force infection, conversion, cure or inheritance.

Additional safety properties implemented and compile-verified:

- first MCA lifecycle observation is delayed one server tick after entity join so provider join normalization/attachments run first;
- loaded MCA entities are sampled on a staggered interval only; no chunk is force-loaded and no whole-world scan is introduced;
- provider-native AI is installed only for an already factually converted MCA vampire and only through the audited idempotent provider method;
- valid provider conversion-source lineage is recoverable after load, while self-source UUIDs are ignored;
- inherited vampirism retains both-parent birth context for diagnostics instead of inventing one conversion source;
- cure/cleared transitions stop transient Core predation targeting/navigation but deliberately preserve historical social belief;
- native Vampirism `BloodDrinkEvent` observation remains at `LOWEST`, after MCA Vamp Compat's normal event handler.

## Why CI does not prove the exact provider lifecycle

The normal GitHub Actions environment intentionally does not redistribute/install the user's third-party MCA Vamp Compat binary or the complete modpack. Therefore a green CI establishes:

```text
Core compilation
+ pure lifecycle semantics
+ resource validation
+ provider-absent fail-closed runtime
+ regression GameTests
```

It does **not** establish:

```text
real named-MCA feeding
+ real MCA Vamp Compat infection
+ actual same-character conversion
+ provider-native converted-MCA AI
+ cure progression/completion
+ inheritance during real MCA reproduction
+ full-pack save/restart compatibility
```

Those remain manual promotion gates.

## Required exact-stack manual matrix

| Area | Required action | Expected result |
| --- | --- | --- |
| Adapter status | Start exact intended pack; run `/folklore diagnostics`, `/folklore predation status`, `/folklore lifecycle status`. | Vampirism 1.10.12, MCA 7.7.32+1.21.1 and MCA Vamp Compat 2.0.12 are truthfully active; `invalid=0`. |
| Wild feed | At night put a hungry wild Vampirism vampire near a named adult MCA civilian. | Named MCA can be selected despite custom name; real provider blood drain occurs. |
| Native infection | Enable provider infection and allow the wild feed. | Real `BloodDrinkEvent` reaches MCA Vamp Compat; provider alone decides infection. |
| Infection observation | Inspect victim with `/folklore lifecycle inspect <entity>`. | Provider snapshot becomes `INFECTED` if and only if provider actually infected the NPC. |
| Same-person conversion | Let provider conversion complete. | The same MCA social character remains; Core observes `VAMPIRE` rather than replacing it with `vampirism:vampire`. |
| Conversion provenance | Inspect converted victim/source. | Provider source UUID is retained as vampire lineage when valid; no self-source record is created. |
| Native MCA-vampire AI | Observe converted MCA after load and while hungry. | Provider native goals are present/idempotently repaired; Core does not install a duplicate infection system. |
| Low-risk stealth | Give an MCA vampire both isolated adult civilian and animal options under low suspicion. | Civilian can be chosen when provider accepts it. |
| High-risk stealth | Raise village/personal suspicion and Hunter influence with both prey types available. | Animal becomes strongly preferred. |
| Protections | Provide child MCA, close family, known hunter, supernatural NPC, tamed animal and named non-MCA candidates. | Autonomous Core predation rejects them. |
| Nonlethal witnessed feed | Let a civilian survive a witnessed feed. | `BITE_MARK`/`BLOOD`, victim belief, witnesses/rumor/Hunter pressure and `feeding_assault` occur; no fake death story. |
| Cure start | Start provider cure on converted MCA. | Lifecycle reports `CURING`; autonomous Core animal feeding is blocked. |
| Cure completion | Let provider cure complete. | Lifecycle observes `CURED`/human factual state, current predatory target stops, historic beliefs remain. |
| Cure cancellation | Interrupt/cancel provider cure if provider supports it. | Lifecycle can return to `VAMPIRE` as `CURE_CANCELLED`; Core does not force either outcome. |
| Inheritance | Produce a child through real provider-supported MCA reproduction involving vampire parent(s). | Provider decides inherited state; Core records `INHERITED_VAMPIRE` only when provider marks inheritance and does not fabricate a source UUID. |
| Restart | Save/restart with infected/converted/curing/inherited MCA NPCs. | Provider state remains authoritative; Core resamples it, restores valid provenance, and does not duplicate social facts. |
| Existing 0.3.1 loop | Complete investigation/contract/Field Guide/FAE tests. | 0.4 does not regress the 0.3.1 loop. |

## Classification

0.4.0 remains **`DEVELOPMENT / RELEASE_CANDIDATE`**, not `PRODUCTION_READY`, until the exact-provider manual matrix and intended full-modpack client/server acceptance are recorded.
