# Known Limitations

This document records the boundary of Dark Folklore Core 0.3.1 so server owners can distinguish verified framework behavior from planned depth.

## Canonicalization and pack content

- Canonical definitions/tags provide semantic resolution; they do not universally migrate every existing foreign stack/entity.
- Audited worldgen/loot routing is finite and version-specific. Existing chunks, inventories, machines, trades, commands, structures, rituals, and future provider IDs can still expose legacy forms.
- AlmostUnified remains owner of configured recipe/material unification.
- Duplicate mob suppression filters natural spawn attempts only. Commands, eggs, spawners, rituals, structures, and existing entities are intentionally preserved.

## Spawning and encounters

- Spawn profiles filter provider spawn attempts; Core does not replace provider spawn registration.
- Encounter pressure currently models bounded time/event/player pressure rather than a complete ecology simulation.
- Tracking never force-loads chunks. If a factual culprit is outside the loaded area, the tracker reports no loaded matching trace rather than loading that entity's chunk.
- Contract targets are not spawned by the encounter director.

## Investigation, stories, and contracts

- Evidence remains server-side logical data, not a custom item/block/model. UX is chat, particles, Field Guide integration, and commands; no standalone contract journal screen ships in Core.
- New 0.3.1 contracts explicitly link to their source story and, when known, the factual culprit UUID/provider implementation through the investigation sidecar. Older pre-sidecar contracts necessarily use compatibility fallbacks.
- A known culprit remains authoritative until a confirmed death enables same-concept fallback. Unloading alone is not proof of death and never enables fallback.
- A confirmed issuer death can enable a local hand-in fallback. This is intentionally constrained; there is still no general contract transfer/cancellation/abandonment UI.
- Concept-level creature sightings are bounded and observer-specific, but are not a full spatial memory graph. A witness records the strongest/recent merged observation for a concept rather than an unlimited history of every individual creature sighting.
- For an exact known culprit, testimony now requires the sighting's observed entity UUID to match while exact continuity remains authoritative. When a documented culprit fallback is active, concept-level testimony can again be relevant.
- Ambient folklore reports do not yet create contracts independently; current incident generation still begins from recognized supernatural actors killing supported social/animal victims or from existing society-story paths.
- Completed/expired stories/contracts remain queryable only according to the existing bounded history/cleanup policy; no external archive/export format is provided.

## Magic and archaeology

- 0.3.1 is **Deep Magic Phase 1**, not native ritual integration. It recognizes curated existing provider items/tags/traits and turns explicit player analysis into investigation evidence while preserving provider-owned right-click behavior.
- Core does not currently hook the exact completion lifecycle of Enchanted circles/rituals, Occultism rites, Malum soul systems, Eidolon theurgy, or Feywild progression. Those provider-native hooks require a separate exact-version API/event/codec audit.
- Not every monster requires magic. Chupacabra and some cryptid cases intentionally remain solvable primarily through physical evidence.
- Fae integration is intentionally narrow: Feywild Sprite is the first curated `GLAMOUR_TRACE` case. Core does not classify every Feywild entity as the same investigative concept.
- Archaeology remains tag/pickup oriented; Core does not add replacement ruins, brush loot, relic models, or a second archaeology progression tree.

## Knowledge and weaknesses

- Weakness existence and player knowledge are separate in 0.3.1. Ordinary preparation output hides WeaknessRule details below `STUDIED` and the prepared-hunt bonus requires learned + satisfied knowledge.
- This does not imply every investigation profile has a special Dark Folklore weakness. If provider/lore/tag evidence does not justify a cross-mod countermeasure, Core intentionally reports no documented Core countermeasure instead of inventing one.
- Weakness Engine remains the only Dark Folklore cross-mod damage authority. Investigation does not apply a duplicate combat multiplier.
- Some provider projectile systems may not expose the semantic ammunition stack through NeoForge's normal `DamageSource` weapon stack; provider-specific projectile bonuses remain unavailable until a safe hook is audited.

## Field Guide

- Field Guide 1.14.0 remains binary and owns the bestiary UI/discovery storage. Dark Folklore does not create a parallel guide screen.
- 0.3.1 can retain the observed implementation for `KEEP_DISTINCT` cases and ask Field Guide to unlock that exact provider page. For canonical concepts with internal provider variants, the bridge can fall back to the canonical page.
- The curated Dark Folklore dataset now contains seven categories and ten explicit entries, including Feywild Sprite.
- Client rendering, Recent Discoveries ordering, and EN/IT presentation still require manual in-world acceptance; resource validation cannot prove visual layout.

## Knowledge, society, and organizations

- Social identity secrets remain deliberately separate from creature observations. `VAMPIRE`, `WEREWOLF`, `WITCH`, etc. are not expanded into one enum value per monster species.
- Witness detection is event-driven, LOS/proximity based, and bounded. It is not acoustic or room-topology simulation.
- Rumors use bounded nearby social candidates and audited relationship/personality/political weights; unsupported MCA/NPC relationship concepts are not invented.
- Organizations have bounded membership/intelligence/event history and confirmed-death cleanup, but Core does not add physical headquarters, schedules, or replace foreign AI.
- Unloaded organization members remain dormant rather than being removed, because unload is not proof of death.
- Village society still uses Core's bounded regional key rather than reconstructing every Minecraft POI/village boundary.
- Dark Folklore can answer social-knowledge/disguise questions but does not override another mod's renderer.

## Persistence and migration

- Established society data remains schema 2. The historical schema-1-to-2 migration remains defensive and idempotent.
- 0.3.1 investigation continuity lives in separate `darkfolklore_investigation` SavedData schema 1. This avoids a destructive society-schema rewrite but introduces a second intentionally scoped save file.
- Sidecar maps have hard caps and pruning. Very old/weak sightings and orphan continuity metadata are removed by maintenance policy.
- A unit NBT round trip is not equivalent to upgrading an authentic user world. Real backed-up world validation remains mandatory before a production-ready claim.
- Runtime queues/caches such as tracking cooldowns are intentionally not durable state.

## Compatibility

- Java/reflection adapters are exact-version gated. A compatible-looking but unaudited update remains `UNTESTED_VERSION` for code-level bridging.
- Missing optional mods are supported; their concrete members/features simply disappear or their exact bridge is disabled.
- 0.3.1 adds no new Mixins to force private behavior. Unsupported provider hooks remain partial rather than invasive.

## Testing and presentation

- CI now validates the wrapper, Java 21 clean build, unit/resource tests, JAR audit, and NeoForge GameTests, but those tests do not render the client or exercise every optional provider.
- The complete 0.3.1 manual matrix is in [Testing](TESTING.md). High-risk cases include exact culprit continuity, concept testimony, issuer fallback, KEEP_DISTINCT Field Guide unlocks, Fae Sprite analysis, STUDIED weakness gating, and save/restart.
- Historical 0.2 mandatory/exact/dedicated/client-startup evidence remains useful context but is not a substitute for a 0.3.1 in-world pass.
- The old curated provider staging emitted one unowned NeoForge client-`Screen` dedicated-side warning and upstream resource warnings. No Dark Folklore class was identified as the requester, but the full pack is not claimed warning-free.
- No custom placeholder models/screens/entities are added by Core; presentation continues to leverage provider content and Field Guide.

Until the intended-pack client/in-world matrix and remaining real-save checks are recorded, 0.3.1 remains **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`.
