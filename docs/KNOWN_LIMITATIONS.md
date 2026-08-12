# Known Limitations

This document records the current Dark Folklore Core 0.4.0 release-candidate boundary. Automated compilation/tests/dedicated startup and manual intended-pack gameplay are different evidence layers.

## Investigation, stories, and contracts

- Evidence is server-side logical data, not a custom item/block/model. UX uses chat, particles, Field Guide integration, and commands.
- New contracts link their source story and, when known, factual culprit UUID/provider implementation. Pre-sidecar contracts use compatibility fallbacks.
- A known culprit remains authoritative until confirmed death enables same-concept fallback. Unload is not death.
- Confirmed issuer death can enable bounded local hand-in fallback; no general transfer/abandonment UI exists.
- Concept sightings are bounded merged observations, not unlimited spatial history.
- Nonlethal confirmed feeding can create one `feeding_assault`; lethal feeding remains on the confirmed-death incident path.

## FACT, BELIEF, and MCA authority

> MCA Reborn × Vampirism Compat remains authoritative for factual supernatural MCA mechanics. Dark Folklore observes those mechanics and implements knowledge, investigation, rumor, reputation and narrative consequences around them.

- Provider state is **FACT**; witness knowledge, rumors, reputation, investigations, stories, and contracts are **BELIEF**.
- MCA supernatural facts route only through the exact MCA Vamp Compat factual component. An absent provider is `NOT_APPLICABLE`; untested/partial/unsupported/failed authority is `UNKNOWN`; no generic Vampirism fallback is used.
- Core does not force infection, conversion, cure, or inheritance and does not replace an MCA person with a generic vampire.
- A provider conversion-source UUID is provenance. It is not necessarily a biological parent or universally reliable sire.
- Cure changes current fact but does not erase a truthful historical observation or rumor.

## Vampire lifecycle and predation

- Exact 0.4 components require Vampirism 1.10.12 + MCA 7.7.32+1.21.1 + MCA Vamp Compat 2.0.12. Different versions fail closed.
- Initial provider observation cannot occur before join+1 tick and retries an unavailable capability only for a bounded 200 ticks.
- Lifecycle sampling is loaded-entity-only, staggered, and bounded; no chunks are force-loaded.
- Provider 2.0.12 can retain metadata during cure cancellation; Core uses prior `CURING` state to avoid misclassifying the return to `VAMPIRE`.
- Provider owns target selection, navigation, and native MCA vampire AI. Core sets/clears no target or path and installs no replacement goal system.
- Core may retain a bounded narrative candidate session. A human session survives only while provider-native AI independently selected the same target.
- Native bite evidence requires one exact direct attacker/target incoming-damage event and a provider attacker-capability ready→cooldown transition. Provider post-success damage cancellation/zeroing does not suppress that evidence; pre-canceled/redirected/failed attempts without a cooldown transition and proximity/session state alone are not evidence.
- Exact observation supports provider-valid MCA, player, and vanilla-human targets; narrative consequences still depend on which target types support the corresponding knowledge/story systems.
- Animal feeding is an explicit fallback through Vampirism's creature blood attachment and provider cooldown; it does not infect/replace animals and Core does not command navigation to them.
- Candidate policy excludes children, close family, hunters, supernatural targets, tamed animals, and named non-MCA entities. This does not prevent unrelated provider-native combat.
- Predation cooldowns, budgets, sessions, correlations, and diagnostics are transient anti-chaos/orchestration state and clear on server stop.

## Magic and archaeology

- 0.4 retains Deep Magic Phase 1: curated provider items/tags/traits plus explicit analysis, without owning provider rituals.
- Core does not hook exact completion lifecycles for Enchanted rituals, Occultism rites, Malum soul systems, Eidolon theurgy, or Feywild progression.
- Not every monster needs magic; some cryptids remain primarily physical investigations.

## Knowledge, weaknesses, and Field Guide

- Weakness existence and player knowledge are separate. Preparation hides rule details below `STUDIED`; the bonus requires learned and satisfied knowledge.
- Weakness Engine remains the only Dark Folklore cross-mod damage authority.
- Field Guide 1.14.0 owns UI, notes/photos, binary discovery, and progress storage. Core does not create a parallel guide.
- The current dataset contains seven categories and ten entries, including Feywild Sprite.
- Joined-world EN/IT presentation, models, toasts, Recent Discoveries, and persistence remain unrun manual acceptance items.

## Society, persistence, and migration

- Witness detection is bounded line-of-sight/proximity logic, not acoustic/room-topology simulation.
- Rumors and organizations use bounded queues/history; unloaded actors are not treated as dead.
- Village society uses a bounded regional key rather than reconstructing all Minecraft POI boundaries.
- Society remains schema 2; investigation sidecar remains schema 1. Provider capability remains the lifecycle save authority.
- Unit NBT round trips are not an authentic user-world migration. A backed-up intended-pack world upgrade/restart remains mandatory.

## Testing and release

- Current local evidence: clean Java 21 build, **119/119 JUnit**, release JAR audit, exact-provider dedicated startup, and **3/3 GameTests** pass with `0 invalid` reload data.
- Final-head CI run and artifact identities remain pending until the final documentation head is built.
- No manual client/in-world lifecycle, Field Guide, or authentic-world row is claimed complete.
- Historical 0.2/0.3.1 evidence remains context only and is not a 0.4 gameplay pass.

Until final-head CI/artifact identity and the intended-pack client/in-world matrix are recorded, 0.4.0 remains **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`.
