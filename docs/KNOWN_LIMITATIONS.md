# Known Limitations

This document records the boundary of Dark Folklore Core 0.3.1 so server owners can distinguish verified framework behavior from exact-provider behavior that still requires an intended-pack runtime pass.

## Investigation, stories, and contracts

- Evidence remains server-side logical data, not a custom item/block/model. UX uses chat, particles, Field Guide integration and commands.
- New 0.3.1 contracts explicitly link their source story and, when known, factual culprit UUID/provider implementation through the investigation sidecar. Pre-sidecar contracts use compatibility fallbacks.
- A known culprit remains authoritative until confirmed death enables same-concept fallback. Unload is not death.
- Confirmed issuer death can enable the bounded local hand-in fallback; no general transfer/abandonment UI exists.
- Concept-level creature sightings are bounded merged observations, not unlimited spatial history.
- Nonlethal vampire feeding can create a `feeding_assault` story; lethal feeding remains on the existing death-driven incident path to avoid duplicate narrative cases.

## Vampire Society & Predation

- The predation director is exact-version gated to the audited Vampirism/MCA/MCA Vamp Compat combination. Different provider versions fail closed rather than guessing private state.
- Autonomous feeding is night-only, loaded-area-only, staggered and bounded. It does not force-load chunks.
- MCA vampire social stealth uses existing Core social pressure: village awareness/suspicion/Hunter influence, personal Vampire suspicion and visible witnesses. It is a behavior heuristic, not a perfect NPC planning simulation.
- Children, close family, hunters, supernatural targets, tamed animals and named non-MCA entities are excluded from autonomous feeding selection. This does not prevent provider combat triggered by unrelated native systems.
- Wild Vampirism mobs can be guided to named MCA civilians through a real provider blood drain. MCA Vamp Compat remains owner of whether that real blood-drink event applies infection.
- MCA Vamp Compat 2.0.12 has native human infection-bite AI but no equivalent animal-feeding goal. Dark Folklore's exact bridge therefore drains animal blood through Vampirism's audited creature blood attachment while honoring the MCA provider's bite cooldown. It does not infect/replace the animal.
- Predation cooldowns, regional feed history, sessions and diagnostics are transient. They are anti-chaos/director state, not supernatural facts that require persistence across restart.
- CI does not install the full optional provider pack. Real named-MCA feeding, infection, conversion, cure and inheritance must still be exercised in the intended pack.

## Magic and archaeology

- 0.3.1 remains Deep Magic Phase 1. It recognizes curated provider items/tags/traits and turns explicit analysis into evidence while preserving provider-owned actions.
- Core does not yet hook the exact completion lifecycle of Enchanted rituals, Occultism rites, Malum soul systems, Eidolon theurgy or Feywild progression.
- Not every monster requires magic; some cryptids intentionally remain primarily physical investigations.
- Fae integration remains narrow and curated around the existing Feywild Sprite case.

## Knowledge and weaknesses

- Weakness existence and player knowledge are separate. Player-facing preparation hides WeaknessRule details below `STUDIED`; prepared-hunt bonus requires learned + satisfied knowledge.
- Not every investigation profile has a special Core weakness. Missing provider/lore evidence is not filled with invented folklore rules.
- Weakness Engine remains the only Dark Folklore cross-mod damage authority.

## Field Guide

- Field Guide 1.14.0 remains owner of the bestiary UI/discovery storage. Core does not create a parallel guide.
- `KEEP_DISTINCT` investigations can retain the exact observed implementation and request the matching provider page.
- The curated dataset contains seven categories and ten explicit entries, including Feywild Sprite.
- Recent Discoveries ordering, visual layout and EN/IT client presentation remain manual acceptance items.

## Society and organizations

- Social identity secrets remain separate from creature observations.
- Witness detection is bounded LOS/proximity logic, not acoustic/room-topology simulation.
- Rumors and organizations have bounded queues/histories; unloaded members are not treated as dead.
- Village society uses Core's bounded regional key rather than reconstructing all Minecraft POI boundaries.
- Factual provider conversion/cure does not automatically erase historical social beliefs. Fact and belief intentionally remain separate.

## Persistence and migration

- Established society data remains schema 2; investigation continuity remains in `darkfolklore_investigation` schema 1.
- Runtime predation caches are intentionally non-durable.
- Unit NBT round trips do not equal an authentic user-world migration. Backed-up real-world validation remains mandatory.

## Compatibility

- Java/reflection adapters are exact-version gated. An unaudited update remains `UNTESTED_VERSION` for code-level bridging.
- Missing optional mods are supported; their exact feature simply disappears or disables.
- No third-party provider JAR is bundled/shaded into Core.
- The MCA Vamp Compat 2.0.12 binary used for development audit had SHA-256 `BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`; it is not redistributed by this repository.

## Testing and presentation

- Latest predation-hardening automated evidence: Actions run `31590162424`, 78/78 JUnit, 3/3 GameTests, release JAR audit PASS, validated reload 0 invalid.
- Those GameTests run without the complete optional provider pack. High-risk manual checks remain exact provider predation/infection/conversion/cure/inheritance, culprit/issuer continuity, KEEP_DISTINCT Field Guide, Fae analysis, STUDIED weakness UX, save/restart and client localization.
- Historical 0.2 evidence is context only and is not a substitute for a 0.3.1 in-world pass.

Until the intended-pack client/in-world matrix and remaining real-save checks are recorded, 0.3.1 remains **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`.
