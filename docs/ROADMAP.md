# Roadmap

The current code already supplies complete initial paths for witnesses, rumors, persistent incidents, contracts, world-event spawn influence, and semantic magic discovery. This roadmap is for closing measured gaps, not postponing the implemented foundation.

## Priority 0: release hardening

- Run and archive the full manual pack matrix on a disposable world, including a restart between each persistence milestone.
- Extend the three live NeoForge GameTests with witness radius/line-of-sight, `NATURAL` spawn cancellation, non-natural spawn preservation, and cross-restart `SavedData` behavior.
- Expand automated default-resource validation from JSON syntax to registry-aware reload tests with the complete pack.
- Add optional-mod CI/smoke lanes where upstream redistribution and automation licenses allow it.
- Retain the implemented schema-1-to-2 migration fixture and add a repeatable backed-up-world migration lane before any future schema change.

## Priority 1: canonical acquisition and world generation

- Validate the implemented silver placed-feature removal and finite loot map in a full-pack world, then cover remaining machines, trades, direct stack generators, and existing-world policy. Continue coordinating recipes with AlmostUnified instead of competing with it.
- Extend garlic acquisition routing beyond the implemented Enchanted-loot replacement only after verifying Enchanted and Vampirism crop/recipe/progression contracts.
- Add diagnostics that explain which canonicalization effects are semantic, acquisition-level, spawn-level, or intentionally interoperability-only.
- Add a reload warning when canonicalization is toggled after world creation, clarifying that generated chunks and existing stacks are not retroactively changed.

## Priority 1: contract robustness and presentation

- Make incident templates, clue signatures, required clue count, rewards, reputation, and lore rewards data-driven.
- Track and validate the original culprit separately from the canonical fallback; support contracts that intentionally accept a species match versus a named culprit.
- Add safe issuer reassignment/abandonment when the issuer dies or is removed.
- Evaluate richer opt-in clue presentation beyond the implemented rate-limited vanilla particles and status text, without requiring a placeholder entity.
- Add a journal/front-end abstraction, with Quest Giver as an optional presentation adapter rather than backend owner.
- Add investigation hypotheses so one clue narrows several possible concepts before final identification.

## Priority 1: encounter depth

- Refine the implemented `encounterCooldownTicks` pressure curve and distinguish natural from ritual/spawner pressure accounting.
- Add data-driven biome, dimension, weather, moon, village-distance, local-density, and progression predicates.
- Introduce an opt-in encounter scheduler for contract/story targets while preserving provider ritual and boss progression.
- Add bounded operator diagnostics for a single spawn decision and investigation area; keep visualization disabled by default.

## Priority 2: society and stories

- Deepen the implemented exact-MCA relationship/personality trust model only when additional relationship semantics are verifiable; do not invent unsupported NPC friendship/enmity.
- Add richer organization roles, home validation, and lifecycle events beyond the implemented bounded recruitment, influence, confirmed-death cleanup, succession, and dissolution paths.
- Reconcile 8-by-8-chunk society regions with actual village POIs without expensive scans.
- Add further competing-organization branches beyond the implemented controlled false accusation, family protection, public exposure, witness intimidation, recruitment, and political-scandal stories.
- Resolve or archive stories whose referenced actors are permanently gone.
- Add optional export/archival for terminal stories and contracts beyond the implemented bounded retention cleanup.

## Priority 2: magic and archaeology

- Audit exact recipe/ritual codecs for Enchanted, Occultism, Malum, Eidolon, and Feywild.
- Add a small number of thematic, progression-safe cross-mod recipes or rites; never generate a generic all-mod recipe mesh.
- Listen to authoritative ritual completion where a public event exists, rather than using inventory possession as the final proof.
- Extend archaeology with provider-safe loot discoveries and guide entries, without replacing Better Archaeology or adding placeholder art.

## Priority 2: UI, accessibility, and observability

- Localize all player-facing contract and diagnostic messages; remove raw concept IDs from normal play where a translation exists.
- Add accessible journal/status presentation that remains optional on dedicated servers.
- Add structured debug counters for witness candidates, rumor transmissions, spawn denials, story creation, and contract transitions behind `debugLogging`.
- Complete graphical-client accessibility/localization review for all six curated Field Guide categories and nine entries while preserving Field Guide-owned notes and photographs.

## Exit criteria for the next release

- Clean Java 21 build and all tests pass.
- Core-only and exact full-pack servers start and reload with zero invalid core data.
- The manual persistence, contract, spawn-reason, weakness, Field Guide, MCA state, and rumor cases pass.
- Every new adapter has an exact API audit and fails closed on untested versions.
- Config values either affect documented behavior or are removed.
- No release claim exceeds the evidence recorded in [Testing](TESTING.md) and [Known Limitations](KNOWN_LIMITATIONS.md).
