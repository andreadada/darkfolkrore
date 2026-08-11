# Changelog

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
