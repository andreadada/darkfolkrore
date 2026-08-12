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
| Classification | `DEVELOPMENT / RELEASE_CANDIDATE` |

0.4.0 is a stacked follow-up to 0.3.1. It deepens the exact MCA Reborn x Vampirism integration by observing native infection/conversion/cure/inheritance state and using the provider's audited native AI extension point, while leaving all lifecycle decisions with the provider.

## Latest verified stacked gate

Verified stacked branch head before this evidence refresh: `1c1bec00cad64bc7e8ef29462fce2b97ac00d445`.

GitHub Actions run `31593042850`: **PASS**.

| Property | Recorded value |
| --- | --- |
| Artifact | `darkfolklore-core-0.4.0.jar` |
| JAR size | `480,321` bytes |
| SHA-256 | `1FAA06E963FB3F28A3FD220EA8DB7B8A54B8B41B2D09820420D94078553DDC17` |
| Class files | `194` |
| JUnit | `84/84 PASS`, 0 failures/errors/skipped |
| GameTests | `3/3 PASS` |
| Datapack reload | `17 canonical / 5 weaknesses / 8 spawn / 2 magic / 9 investigation / 13 stories / 4 organizations / 6 political, 0 invalid` |
| Class-file version | `65`, enforced by `auditReleaseJar` |

The build uploads `darkfolklore-core-0.4.0.jar`. Optional provider JARs are not shaded into Core.

## Exact provider binary audited

MCA Reborn x Vampirism Compat 2.0.12 user-supplied JAR:

`BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`

The binary is not committed, redistributed or shaded. Exact class/method contracts are documented in `MCA_VAMP_COMPAT_2.0.12_AUDIT.md` and validated fail-closed at runtime.

## 0.4 ownership model

Dark Folklore **observes**, but does not own:

- MCA infection eligibility/chance/duration;
- conversion completion;
- cure progression/completion/cancellation;
- inherited vampirism;
- provider capability persistence;
- provider appearance/state normalization;
- provider-native MCA vampire infection-bite AI.

Core therefore does not expose a generic `forceInfect`, `forceConvert`, `forceCure` or `applyInheritance` pathway.

The only lifecycle-side provider mutation intentionally exposed by Core is the audited idempotent `McaVampireAi.registerGoalsIfNeeded(LivingEntity)`, used only after the provider already reports that the MCA NPC is factually converted.

## Lifecycle semantics

Core classifies exact provider snapshots into:

```text
UNAVAILABLE
HUMAN
INFECTED
VAMPIRE
CURING
```

and observes:

```text
INFECTION_STARTED
NATIVE_BITE_CONVERTED
CONVERTED
INHERITED_VAMPIRE
CURE_STARTED
CURE_CANCELLED
CURED
INFECTION_CLEARED
VAMPIRISM_CLEARED
```

Important safety decisions:

- initial observation is delayed after entity join so provider normalization runs first;
- only loaded MCA entities are sampled, on a staggered interval;
- no whole-world entity scan or chunk force-load is introduced;
- provider conversion source is treated as durable factual provenance and can be recovered after load;
- source UUID equal to descendant UUID is rejected;
- inherited vampirism keeps both-parent birth context without fabricating a conversion source;
- cure stops transient Core predation targeting/navigation but does not erase historical witness beliefs or rumors;
- factual state and social belief remain separate.

## Predation/native infection chain retained from 0.3.1

For wild Vampirism mobs feeding on named MCA civilians:

```text
Vampirism ExtendedCreature.onBite
 -> IVampireMob.drinkBlood(... MCA victim context ...)
 -> real BloodDrinkEvent
 -> MCA Vamp Compat NORMAL handler/infection decision
 -> Dark Folklore LOWEST observer
 -> evidence / witnesses / stories only if finalized amount > 0
```

This route exists specifically so Core does not bypass provider configuration or install an independent infection system.

For factually converted MCA vampires, the 0.3.1 social director chooses prey while the provider remains owner of the human infection-bite path. Animal fallback uses Vampirism's exact blood attachment plus MCA Vamp Compat's bite cooldown and is blocked while provider cure is active.

## Persistence

No destructive society schema bump is introduced.

- `darkfolklore_society`: schema 2;
- `darkfolklore_investigation`: schema 1;
- lifecycle snapshots, birth context and recent transition diagnostics: transient observations, reconstructed from provider facts after load;
- valid conversion lineage remains in existing Core lineage persistence.

This design avoids duplicating provider capability state in a second save authority.

## Promotion blockers

A green Core CI is insufficient for `PRODUCTION_READY` because normal CI intentionally does not install the complete optional provider stack. Before promotion, record real in-world evidence for:

- hungry wild Vampirism vampire feeding from a named adult MCA civilian;
- native `BloodDrinkEvent` causing provider-configured infection when appropriate;
- infection state observed by `/folklore lifecycle inspect`;
- same MCA person remaining the same social NPC after provider conversion;
- valid conversion source/provenance;
- provider-native converted-MCA vampire AI after live conversion and after restart;
- low-suspicion civilian feeding vs high-suspicion animal preference;
- all autonomous prey protections;
- nonlethal feed evidence/witness/rumor/Hunter pressure/`feeding_assault`;
- provider cure start, cancellation/completion and post-cure predation stop;
- provider inheritance during real MCA reproduction;
- save/restart with infected, converted, curing and inherited NPCs;
- existing 0.3.1 investigation, culprit/issuer continuity, Field Guide KEEP_DISTINCT, Fae and EN/IT client behavior;
- authentic backed-up world upgrade/full-pack server-client smoke.

## Deferred work

0.4.0 focuses on the MCA/Vampirism provider lifecycle. Native ritual hooks for Enchanted, Occultism, Malum, Eidolon: Repraised and Feywild remain a separate exact-provider Deep Magic phase rather than being simulated here.

## Classification

Current classification: **`DEVELOPMENT / RELEASE_CANDIDATE`**.

Do not promote to `PRODUCTION_READY` until the exact-provider and full-modpack manual gates above have passed and been recorded.
