# Dark Folklore Core 0.4.0 handoff

## Decision

Current classification: **`RELEASE_CANDIDATE`**.

The integrated code, documentation, and release artifact passed the PR gate. They are not sufficient for `PRODUCTION_READY`; the exact-provider gameplay, Field Guide client, authentic-world, and full intended-pack manual gates remain unrun.

## Non-negotiable ownership boundary

> MCA Reborn × Vampirism Compat remains authoritative for factual supernatural MCA mechanics. Dark Folklore observes those mechanics and implements knowledge, investigation, rumor, reputation and narrative consequences around them.

Provider state is **FACT**; observer knowledge, rumors, reputation, investigations, stories, and contracts are **BELIEF**. MCA facts route only through the exact MCA Vamp Compat fact component. Core never forces infection/conversion/cure/inheritance, never substitutes generic Vampirism facts for an MCA entity, and never sets/clears provider/MCA target or navigation state.

## Implemented 0.4 safeguards

- Exact Vampirism 1.10.12 + MCA 7.7.32+1.21.1 + MCA Vamp Compat 2.0.12 triple gate.
- Independently initialized fact, predation, and lifecycle components with truthful `ACTIVE`/`PARTIAL`/`ERROR` reporting.
- Status-aware MCA fact routing: absent → `NOT_APPLICABLE`; untested/partial/unsupported/error → `UNKNOWN`; no generic fallback.
- At-least-one-tick initial observation delay and bounded 200-tick retry for late provider capability attachment.
- Sticky provider metadata-safe cure cancellation: prior `CURING` state takes precedence.
- Durable valid provider provenance, self-source rejection, no fabricated inherited source, and explicit warning that provenance is not necessarily biological parent/sire.
- Provider-owned target/navigation/native AI; Core retains only bounded narrative sessions and may call the provider's audited idempotent goal-repair method after factual conversion.
- Exact native-bite evidence tied to one direct attacker/target damage event and the provider attacker's ready→cooldown capability transition. Provider-valid MCA, player, and vanilla-human targets are supported without an MCA-only victim marker; provider post-success damage cancellation does not hide a proven feed, while attempts with no cooldown transition produce no evidence.
- Cure/cleared transitions cancel only Core's session and preserve historical belief.
- Loaded-entity-only staggered work, bounded caches, no forced chunks/global scans, and server-stop cleanup.

## Recorded automated evidence

Local Java 21 evidence on the current integration:

- clean build: **PASS**;
- JUnit: **119/119 PASS**;
- release JAR audit: **PASS**;
- NeoForge GameTests: **3/3 PASS**;
- exact optional-provider dedicated GameTest startup: **PASS** with audited versions `ACTIVE`;
- atomic reload: `17/5/8/2/9/13/4/6`, `0 invalid`.

This is automated dedicated-server evidence. It is not a manual client/in-world full-pack pass.

## Verified integration gate

The superseded pre-rebase 0.4 run/artifact is not used. The production identity below comes from the post-rebase integration head and was independently checked by downloading the uploaded artifact.

| Evidence | Value |
| --- | --- |
| Verified integration commit | `f57b4f8424d445433e0e57f2d07aeecb96ac8307` |
| Green GitHub Actions runs | [push `31604882766`](https://github.com/andreadada/darkfolkrore/actions/runs/31604882766) and [PR `31604885701`](https://github.com/andreadada/darkfolkrore/actions/runs/31604885701) |
| `darkfolklore-core-0.4.0.jar` size | `498,581` bytes |
| SHA-256 | `C161D2EFF75CABE9F3F25BAEC0DEB39BE4186F54848C88B288778620A06A74B3` |
| Class-file count | `207` |

The uploaded artifact is retained by Actions for 14 days. Keep the sources JAR out of the pack. This evidence refresh is documentation-only; its own CI must remain green before merge.

## Manual acceptance still required

No item in this section is claimed complete:

- real named-MCA wild feeding and provider infection;
- join+1/late provider capability observation;
- same-character conversion and provenance after restart;
- provider native AI/target/navigation/bite ownership;
- positive and negative exact native-bite evidence cases for provider-valid target kinds;
- low/high risk candidate behavior and exclusions;
- cure start, sticky-metadata cancellation, completion, and scoped session cancellation;
- real provider inheritance and both-parent birth context;
- infected/converted/curing/inherited save/restart;
- merged 0.3.1 investigation/contract/death-finality/Fae/KEEP_DISTINCT flows;
- Field Guide's seven categories/ten entries, EN/IT, unlock paths, Recent Discoveries, and persistence in a joined client world;
- authentic backed-up world upgrade and full intended-pack server/client acceptance.

Use the detailed [manual matrix](TESTING_0.4.0.md) and do not infer any of these outcomes from unit tests, GameTests, dedicated startup, or title-screen startup.

## Release references

- [Compatibility and authority routing](COMPATIBILITY.md)
- [Exact MCA Vamp Compat audit](MCA_VAMP_COMPAT_2.0.12_AUDIT.md)
- [MCA social FACT/BELIEF boundary](MCA_SOCIAL_AUDIT.md)
- [0.4 automated/manual testing](TESTING_0.4.0.md)
- [0.4 release gate](RELEASE_0.4.0.md)
- [Field Guide client gate](FIELD_GUIDE.md)
