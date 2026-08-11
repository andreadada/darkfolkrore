# Known Limitations

This document records the boundary of the current implementation so server owners can distinguish a functional path from planned depth.

## Canonicalization and pack content

- Canonical definitions and tags provide semantic resolution. In addition, four audited noncanonical silver placed features are removed from new Overworld generation and a finite replacement map routes audited silver/Enchanted-garlic loot. This is not a universal stack migration.
- The `canonicalization` toggle gates the global loot rewrite, custom feature/spawn biome modifiers, and spawn profiles marked as canonicalization suppression. It intentionally does not unload semantic definitions/tags or rewrite existing inventories and chunks when changed.
- AlmostUnified remains the owner of configured material recipe unification. Core supplies the audited worldgen and loot layers, but machines, direct Java stack creation, trades, commands/creative acquisition, existing chunks/stacks, and IDs added by future provider versions can still produce noncanonical forms.
- Disabled duplicate mob profiles suppress only natural spawn attempts, not recipes, eggs, commands, spawners, structures, rituals, or existing entities.

## Spawning and encounters

- Spawn profiles are exact entity-ID filters over provider attempts. They do not register spawns or schedule bespoke encounters.
- Profile decisions currently use time-of-day, rarity, world-event state, global multiplier, nearby player, and pressure. Biome, weather, dimension, village distance, local supernatural density, and provider progression are not modeled.
- Encounter pressure relaxes only for online players; persisted offline pressure resumes decay after login.
- Any successfully joined rare profiled entity, including a non-natural one, may add pressure to the nearest player.
- Contract targets are not spawned by the encounter director.

## Investigation, stories, and contracts

- Evidence is a persistent logical point, not a visible model, particle, block, item, scent trail, or Field Guide marker.
- The player interface is chat/status text plus admin commands; there is no contract journal or Quest Giver frontend.
- The target is identified after two distinct clues, and reward/clue count/templates are hardcoded.
- Hunt validation accepts any entity matching the canonical concept, not necessarily the incident's original actor.
- The exact issuer must remain available for completion. There is no issuer reassignment, cancellation, abandonment, or compensation path.
- Stories retain actor UUIDs but do not proactively resolve dead, removed, or permanently unloaded actors.
- Completed/expired contracts and resolved/expired stories remain queryable until the configured terminal-history retention window passes; there is no external archival/export facility.
- Only incidents caused by recognized supernatural actors killing animals, villagers, or MCA entities generate contracts. No ambient report generator exists.
- Evidence pruning is periodic; a record can be past its timestamp briefly before removal.

## Magic and archaeology

- Magic integrations recognize an inventory trait combination on item pickup and grant lore once. They do not execute or modify native rituals, recipes, altars, spells, costs, or progression.
- Existing qualifying items may not be detected until another recognized item is picked up. Armor, offhand, curios, nested containers, and external storage are not scanned.
- Only two cross-tradition definitions ship by default.
- Archaeology integration is tag-and-pickup based. It does not add generated ruins, brush loot, books, models, or bespoke relics.

## Knowledge, society, and organizations

- Witness detection is event-driven and bounded but uses ordinary entity line-of-sight and proximity, not acoustic simulation or room topology.
- Rumors consider nearby players, villager-like NPCs, and MCA people. Explainable trust can include same-organization membership, prior suspicion, the exact audited MCA spouse/parent/child/sibling/player-friend/player-bounty categories, verified MCA personalities, and exact MCA Capitals roles. Unsupported NPC friendship/enmity, ordinary profession, and pathfinding distance are not invented as inputs.
- Confidence degrades on each retelling and on bounded periodic half-life passes. This decay applies to `RUMOR` records only; direct confirmed and public facts are intentionally retained.
- Organizations have bounded factual recruitment, objectives, intelligence/events, influence, confirmed-death cleanup, deterministic leader succession, and empty-organization dissolution. They do not add headquarters structures, schedules, a rich role hierarchy, or foreign-mod AI. Unloaded members remain as diagnosable dormant members because unload alone is not proof of death/removal.
- Village society uses fixed 8-by-8-chunk regions rather than Minecraft POI-derived village boundaries.
- Secret awareness can answer whether an observer should be fooled, but Dark Folklore does not override another mod's renderer or disguise mechanics.
- Lineage records only reliable conversion-source UUIDs exposed by the exact MCA Vamp Compat implementation. It is provenance, not a guaranteed biological parent/sire relationship.

## Compatibility

- Code-level adapters are exact-version gated. A compatible-but-unaudited update reports `UNTESTED_VERSION` and internal code bridging stays disabled until reviewed.
- Weakness damage uses the `DamageSource` weapon stack. The Immersive Engineering silver-bullet item is semantically tagged, but a fired projectile may expose its firearm rather than its ammunition as that stack, so silver-bullet bonus damage is not guaranteed without a safe provider projectile hook.
- Field Guide remains binary and owns its UI/descriptions. Dark Folklore does not write user notes or create a parallel guide screen.
- Missing optional mods are supported; the semantic tag/data layer may simply have fewer concrete members.
- No Mixins are used to force private integration behavior. Features without safe public or audited hooks remain partial.

## Persistence and migration

- Save data carries schema version 2 and each entry is loaded defensively. Schema 1 receives safe defaults, is marked dirty once, and rewrites as schema 2; a newer schema is only best-effort read with a warning, and there is no downgrade writer.
- World events, rumor tasks, and general delivery cooldowns are runtime-derived and are not persisted. Durable social records and explicit witness-intimidation rumor-silence deadlines are persisted.
- Corrupt individual entries are skipped where handled; administrators should still back up worlds before changing builds.

## Testing and presentation

- Pure logic and NBT round-trip tests exist, but there are not yet real GameTests for witnesses, spawn cancellation, or live event persistence.
- Full-pack behavior still requires the manual matrix in [Testing](TESTING.md), especially client UI, real MCA relationship/capital state, optional-provider gameplay, and registry/resource presentation.
- Mandatory-only, exact-adapter, fresh-world, and curated 23-external-JAR headless smokes passed their recorded scopes. The exact/curated dedicated staging emitted one unowned NeoForge `RuntimeDistCleaner` request for `net.minecraft.client.gui.screens.Screen`; it continued through startup, tests/save, and shutdown, and no Dark Folklore class was identified as the requester. Graphical client startup with the curated set also reached the title state and exited cleanly, but no world/UI gameplay path was exercised. Neither pack run is claimed warning-free because upstream resource/model/sound warnings remain.
- Schema-1-to-2 migration is covered by an idempotent NBT fixture, but no authentic 0.1 world directory has yet been upgraded and inspected end to end.
- There are no custom models, textures, particles, sounds, screens, structures, or placeholder entities in this core. Visual content remains with provider mods.
- Admin debug visualization is not implemented; diagnostics are text-based.
