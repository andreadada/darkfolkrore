# Changelog

## 0.4.0 — Native MCA Vampire Lifecycle Integration (2026-08-12)

### Added

- Exact read-mostly lifecycle bridge for MCA Reborn x Vampirism Compat 2.0.12, with constructor-time class/method validation and fail-closed behavior on unsupported signatures.
- Provider lifecycle states for MCA civilians: `HUMAN`, `INFECTED`, `VAMPIRE`, and `CURING`.
- Observed transition classification for infection start, native-bite conversion, other conversion, inherited vampirism, cure start/cancel/completion, infection clear, and factual vampirism clear.
- Initial provider observation delayed by at least one server tick, with bounded retries for up to 200 ticks when the provider capability is not yet available.
- Loaded-entity-only, staggered lifecycle observation without world scans or chunk force-loading.
- Recovery of valid provider conversion-source lineage both during live conversion and after world/entity reload.
- Birth context for inherited vampires that retains both provider parents in diagnostics without fabricating a single conversion source.
- Exact provider-native AI repair through the audited idempotent `McaVampireAi.registerGoalsIfNeeded` extension point, only after factual conversion.
- Strict, status-aware supernatural fact routing: MCA entities use only the exact MCA Vamp Compat authority; generic Vampirism state cannot override or replace an unavailable MCA result.
- Independent factual, predation, and lifecycle component activation, reported as `ACTIVE`, `PARTIAL`, or `ERROR` without one optional component failure suppressing the others.
- Exact native-MCA bite evidence correlation across the same incoming-damage event, direct attacker/target pair, and provider attacker-capability ready-to-cooldown transition, covering provider-valid MCA, player, and vanilla-human targets.
- `/folklore lifecycle status` and `/folklore lifecycle inspect <entity>` diagnostics.
- `docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md` documenting the exact user-supplied binary, provider methods, event chain, ownership boundary, cure/inheritance semantics, and runtime validation limits.
- Pure lifecycle transition regression tests.

### Changed

- Version advanced to `0.4.0`; CI builds/audits/uploads `darkfolklore-core-0.4.0.jar`.
- Vampire feeding observation runs at `LOWEST` event priority, after MCA Vamp Compat's normal `BloodDrinkEvent` handler, so provider-blocked/zeroed drains cannot create false Dark Folklore evidence.
- MCA vampire animal feeding is disabled while the exact provider reports an active cure state.
- Provider provenance is treated as durable factual data rather than only a live-transition edge, allowing conversion lineage to recover after load.
- MCA human predation is provider-owned end to end: Core may score and retain bounded narrative intent, but never sets a target or navigation path and recognizes a feed only from an exact provider action.

### Fixed

- Malformed provider conversion-source UUIDs equal to the descendant UUID are rejected before constructing lineage records.
- Cure cancellation now takes precedence over retained provider inheritance, bite-cause, and conversion-source metadata when the prior factual state was `CURING`.
- Cure/cleared transitions cancel only Dark Folklore's scoped predation session without mutating provider/MCA target or navigation state and without deleting historical witness beliefs or rumors.

### Provider ownership boundary

- Dark Folklore does **not** force infection, conversion, cure, inheritance, or MCA vampire replacement.
- MCA Reborn x Vampirism Compat 2.0.12 remains sole factual owner of infection rules, conversion, cure, inherited vampirism, capability persistence, appearance normalization, and native MCA vampire bite AI.
- MCA Reborn × Vampirism Compat remains authoritative for factual supernatural MCA mechanics. Dark Folklore observes those mechanics and implements knowledge, investigation, rumor, reputation and narrative consequences around them.
- The exact audited user-supplied provider JAR SHA-256 is `BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`; it is not redistributed or shaded.

### Release boundary

- Local and GitHub Actions validation pass 119 JUnit tests, three NeoForge GameTests, the release JAR audit, and artifact upload; exact run and artifact identities are recorded in `docs/RELEASE_0.4.0.md`.
- 0.4.0 remains `RELEASE_CANDIDATE` until the exact full provider stack is exercised in-world for named-MCA feeding, real provider infection, same-character conversion, native MCA vampire AI, cure, inheritance, Field Guide client rendering, and save/restart.
- Core CI intentionally runs without the complete optional provider pack, so green automated tests do not substitute for those manual gates. No manual gameplay pass is claimed for this release candidate.

## 0.3.1 — Investigation Hardening (2026-08-11)

### Added

- Persistent concept-level creature sightings for cryptids, spirits, demons, constructs, and Fae, kept separate from social identity secrets.
- Investigation sidecar persistence for story/contract links, known culprit UUIDs, observed provider implementations, sightings, and confirmed-death fallback flags without rewriting society schema 2.
- Exact story continuity for new contracts so same-concept incidents in one region do not rely on `findFirst()` narrative resolution.
- Factual culprit tracking for new incidents, with same-concept fallback allowed only after a confirmed death rather than an unload.
- Bounded issuer-death hand-in fallback: local Hunter Society members are preferred/required when such a society exists; otherwise a valid local villager/MCA representative can receive the completed hunt.
- A curated Feywild Sprite concept/profile using `GLAMOUR_TRACE`, plus a localized Fae Field Guide category and exact Sprite entry.
- Vampire Society & Predation: bounded prey selection for wild Vampirism mobs and factual MCA vampires, social-risk-aware animal/civilian choice, native feeding observation, nonlethal feeding evidence/stories, witness/rumor/Hunter pressure, anti-chaos budgets, and predation diagnostics.
- Tests for investigation sidecar persistence, case-link continuity, sighting merge/decay semantics, knowledge-gated preparation, Fae resources, death finality, and vampire predation policy.
- NeoForge GameTests in the GitHub Actions release gate.

### Changed

- Version advanced to `0.3.1`; CI now builds, audits, and uploads `darkfolklore-core-0.3.1.jar`.
- Weakness/preparation information is player-facing only from `STUDIED` lore onward. Accidentally carrying a correct item no longer qualifies an uninformed player for the prepared-hunt bonus.
- `KEEP_DISTINCT` investigation cases retain the observed implementation so Field Guide 1.14.0 can unlock the actual provider page, such as a specific Wraith implementation.
- Hypothesis percentages are presented as evidence `support`, not as calibrated probability.
- Field Guide curated content expands from six categories/nine entries to seven categories/ten entries with the Sprite case.
- Tracking and prepared-hunt validation share factual investigation-target matching rather than accepting any same-concept target when a known culprit remains valid.
- Wild Vampirism feeding on named MCA civilians uses a real provider blood drain instead of globally removing custom-name protection; MCA Vamp Compat remains sole owner of native infection/conversion.
- MCA vampire social stealth reacts to village/public/personal suspicion and visible witnesses; high risk strongly favors safer animal feeding.

### Fixed

- Linux GitHub Actions no longer stops at `./gradlew: Permission denied`; the workflow ensures wrapper execution before the clean Java 21 build.
- Physical evidence collection rejects expired clues directly at interaction time.
- Wendigo/Chupacabra/Ghost/Wraith/Imp/Golem/Fae observations can supply concept-specific testimony instead of being reduced to generic `SUPERNATURAL_IDENTITY` claims.
- New contracts retain an exact incident/story association and can recover from confirmed culprit or issuer deaths without treating ordinary unloads as deaths.
- Cancellable/rescued death events do not mark a hunt or authorize fallback before death is final.
- Vampire predation rejects children, close family, known hunters, supernatural prey, tamed animals, and named non-MCA entities from autonomous feeding selection.
- Removed the already-applied 0.3 patch generator from the repository root.

### Release boundary

- 0.3.1 is a hardening release. Deep provider-native ritual hooks for Enchanted, Occultism, Malum, Eidolon, and Feywild remain deferred until their exact APIs/events/codecs are audited.
- Automated build/tests are necessary but not sufficient. In-world client investigation, Field Guide/Recent Discoveries, exact optional-provider gameplay, and real-world migration remain manual promotion gates. Classification remains `RELEASE_CANDIDATE` until those checks are actually recorded.

## 0.3.0 — Occult Investigation (2026-08-11)

### Added

- Eight reloadable, atomically validated investigation profiles for the curated supernatural concepts.
- Evidence-only hypothesis ranking that does not expose the hidden contract target.
- Additive clue analysis through Enchanted, Occultism, Malum, Eidolon, and Feywild item semantics without importing optional-provider internals.
- Explicit, cooldown-bound tracking of identified targets in loaded areas only.
- Weakness-rule-based preparation assessment, prepared-hunt lore/reputation rewards, operator diagnostics, and five investigation-tool tags.

### Changed

- Curated cryptids, spirits, demons, and constructs can now enter the incident-to-contract loop without requiring an MCA-style secret identity.
- Incident evidence is profile-specific, and contracts retain evidence collected after identification for further research.
- The production artifact version is now `darkfolklore-core-0.3.0.jar`; persistent data remains schema 2.

### Fixed

- Preserved provider-owned right-click actions during both occult analysis and tracking.
- Added the missing build-time import in the generated contract integration and expanded the release-JAR audit for investigation resources.

### Known Issues

- Automated validation covers 59 JUnit tests and three GameTests. The interactive investigation, Field Guide, tracking, preparation, save/restart, and optional-provider client matrix remains a manual release gate.
- The curated dedicated-server staging retains the previously documented unowned NeoForge client-`Screen` dist warning from the external pack.

## 0.2.0 — Living Society (2026-08-10)

### Added

- Living supernatural organizations with objectives, bounded intelligence and event histories, recruitment, and leader succession after confirmed deaths.
- Relationship-, family-, and personality-aware social trust for the exact audited MCA Reborn release.
- Read-only MCA Capitals political context, allowing verified offices and influence to affect rumor credibility and investigations without changing MCA's own state.
- Public-secret reveal rules, controlled false accusations, and additional incident-driven society stories.
- Expanded investigation testimony, evidence feedback, organization consequences, and lore rewards.
- A complete Dark Folklore Field Guide dataset with six localized English/Italian categories, nine explicit entity mappings, native binary discoveries, and server-side synchronization with Core lore thresholds.
- Production diagnostics for social state, organizations, stories, compatibility adapters, and canonicalization.

### Changed

- Enchanted wolfsbane is now the canonical farmable plant while exact-version bridging preserves Werewolves' diffuser, finder, contact-effect, recipe, loot, and legacy-item behavior.
- Rumors use explainable trust contributions instead of a single same-organization constant.
- Society processing now uses configurable budgets and bounded persistent histories to protect server tick time and world size.
- Persistent world data advances to schema 2 with an idempotent migration path from 0.1 worlds.
- Build dependencies use immutable, checksum-verified remote artifacts; a local `mods/` directory is no longer required to compile.

### Fixed

- Added the missing `darkfolklore:vampire` canonical concept and kept special vampire variants distinct.
- Repaired Field Guide category, entry, localization, and entity-mapping resources for Field Guide 1.14.0.
- Prevented optional integration failures from being treated as factual supernatural state.
- Prevented unbounded organization intelligence/event growth and unsafe cleanup based only on unloaded entity UUIDs.

### Compatibility

- Target: Minecraft 1.21.1, NeoForge 21.1.248, and Java 21.
- Exact adapters are audited for Vampirism 1.10.12, Werewolves 2.0.3.3, MCA Reborn 7.7.32+1.21.1, MCA Capitals 1.1.0, MCA Reborn x Vampirism Compat 2.0.12, Enchanted 4.2.7, and Field Guide 1.14.0.
- Every external mod remains optional to Dark Folklore Core. Exact-version adapters disable themselves when their audited contract is unavailable; foreign gameplay state remains owned by its source mod.
- Dark Folklore Atlas is a development tool and is neither required nor bundled.

### Known Issues

- Release classification is `RELEASE_CANDIDATE`: graphical startup to the title state passed, but no client world/UI gameplay pass or authentic 0.1-world upgrade has been completed, so presentation and real-save migration remain manual gates.
- Curated dedicated-server staging logged one unowned NeoForge `RuntimeDistCleaner` request for the client `Screen` class. Startup, save, and shutdown completed, and no Dark Folklore class was identified as the requester, but the pack run is not claimed to be warning-free.
- A complete interactive client UX smoke pass and every optional-mod permutation must be completed before promoting this build beyond release-candidate status.
- Quest Giver remains a detected optional front end; Dark Folklore continues to own contract state and does not publish contracts into Quest Giver 1.5.1.
- Versions outside the audited integration set retain safe data/tag interoperability where possible, but their reflective or direct adapters remain disabled until audited.
