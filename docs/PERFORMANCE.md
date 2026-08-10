# Performance characteristics

## Summary

Dark Folklore's gameplay work is predominantly event-driven and local. It does not run a whole-world entity scan every tick. The main explicit bounds are a 1,024-task rumor queue, a configurable rumor batch budget, a maximum rumor-task hop index of 3 (a hop-3 task cannot enqueue a successor), and a configurable maximum number of witnesses processed per incident.

This document describes code-level complexity and configured schedules. It is not a benchmark report; no production tick-time or heap measurements are currently checked into the project.

## Scheduled work

| Schedule | Work |
| --- | --- |
| Every server tick | Constant modulo checks in rumor, contract, encounter, and world-event handlers. No scheduled scan runs unless its interval matches. |
| Every `rumorIntervalTicks` (default 100) | Process at most `rumorsPerBatch` tasks (default 8) from the rumor queue. Each live task performs one 12-block query, filters to social participants, and transfers to at most one recipient. |
| Every 100 player ticks, exact Field Guide adapter only | For that player, scan canonical entity definitions not yet discovered and query their implementation entries. |
| Every 200 server ticks | Scan active contracts/stories for expiry and recompute two world-event predicates for each loaded server level. |
| Every `max(1, encounterCooldownTicks / 100)` ticks (default 120) | Remove one encounter-pressure point from each online player with positive pressure; reaching zero removes the map entry. |
| Every 1,200 ticks | Decay persisted rumors; scan social records, evidence, terminal narrative history, and rumor cooldowns for cleanup. |

World-event checks cover `FULL_MOON` and `WITCHING_HOUR`; they do not inspect entities. Encounter relaxation is linear in online player count. Field Guide work is linear in loaded canonical entity definitions and their implementation lists, not in all registered entity types.

## Event-local work

### Damage and weaknesses

For an incoming weapon-damage event, Core resolves all item and creature trait enum tags, queries active supernatural adapters, sorts the loaded weakness rules by priority, and applies only the first match. With the bundled five rules this is small, but sorting occurs on every qualifying damage event rather than once at reload. A large third-party weakness set would make this path `O(R log R)` per hit and should be profiled.

The `ThreadLocal` processing guard prevents Core re-entry on the same thread. Rules can skip native provider namespaces to avoid multiplying a weakness already owned by the target mod.

### Witness incidents

A qualifying supernatural damage incident performs one living-entity query in the configured axis-aligned radius, filters it to players, villager-like NPCs, and MCA people, sorts the candidates by distance, then truncates to `maxWitnessesPerIncident` before line-of-sight processing. The default processed cap is 32 and allowed range is 1 through 128. The query result and sort are not hard-capped before allocation, so very dense social crowds inside a large radius can still produce a spike.

Repeated actor/victim/secret damage is suppressed for 100 ticks. When the incident-cooldown map grows beyond 2,048 entries, values older than 1,200 ticks are removed. This is age-based cleanup rather than a strict 2,048-entry bound.

Automatic organization creation scans the stored organization collection to ensure there is only one hunter society for the region. That cost grows with historical organization count.

### Rumors

The rumor queue has a strict admission limit of 1,024 tasks. Hop count is limited to 3, work is batch-budgeted, and each task stops after one successful recipient or one failed chance. A local query still evaluates the living entities inside 12 blocks, but only players, villager-like NPCs, and MCA people can receive a rumor.

Organization trust checks scan organizations linearly. Rumor cooldowns and the queue are transient and reset on restart. If the queue is full, new tasks are dropped silently by design; diagnostics exposes only the current queue length.

### Canonicalization

The global loot modifier performs one toggle check and, when enabled, a map lookup for each newly generated loot stack. Replacement copies count and the component patch. It does not visit existing inventories.

The two custom biome modifier codecs run during biome modification, not every tick. In NeoForge's `REMOVE` phase they test the configured biome holder set, then remove matching placed features from one generation step or matching entity types across mob-category spawn lists. When canonicalization is disabled they return without mutation. Spawn profiles marked `canonicalization_suppression` also consult the toggle during natural position checks.

### Natural spawns

Spawn-profile lookup is a map access and runs only for `NATURAL` position checks. Profiled spawns may also perform a nearest-player query within 128 blocks and one encounter-pressure lookup. Non-profiled entities return immediately. Entity joins for rare-or-higher profiles scan online players in that level to find the nearest within 128 blocks.

Ritual, spawner, structure, conversion, command, and boss creation paths are not filtered, which both protects authored encounters and avoids work on those paths.

### Lore and inventory traits

Tracking and kill lore are local to the affected entities. A qualifying item pickup scans the player's main inventory and all loaded magic-integration definitions. Cost is proportional to inventory slots, trait enum size, and integration count. Discovery-once checks prevent repeat writes but do not skip the inventory scan before a matching integration is found.

### Stories, evidence, and contracts

An eligible supernatural social kill scans all stored stories to test the regional cooldown. Accepting a contract scans stories in the local region. Investigating a block scans all stored evidence for uncollected nearby matches. Target kills and completion scan stories again. The 200-tick expiry pass scans every stored contract and story.

Terminal contracts and stories are retained only until their original deadline is older than `terminalHistoryRetentionTicks`; the default retention is 168,000 ticks. Evidence, including collected evidence, remains until its normal expiry. Event-heavy servers can still accumulate many records inside the active plus retention windows, so the linear scans remain relevant even though maintenance bounds historical growth over time.

## Persistence and growth

`FolkloreSavedData` serializes each logical map/collection as a list. Save and load are linear in total stored rows. The following collections have no explicit cardinality limit or historical compaction:

- lore concepts per player;
- social observer/subject/secret records;
- reputation holders;
- organizations and village regions;
- lineage records;
- active and recently terminal contracts and stories;
- positive encounter-pressure UUIDs, including offline players until their pressure can resume decaying while online.

Rumor records receive exponential half-life decay every 1,200 ticks and are removed below 0.08. The general social prune also removes sufficiently old non-public records below 0.12. Stronger non-rumor records and permanent lineage can remain indefinitely by design. Terminal story/contract history is pruned after the configurable post-deadline retention period. Encounter pressure is clamped to 0 through 100, and the setter removes rather than stores zero values.

Every state mutation marks the single SavedData object dirty. Normal Minecraft saved-data scheduling coalesces disk writes; Core does not synchronously write the file on each event.

## Reload and lookup behavior

JSON parsing happens in the resource reload prepare phase. Apply publishes immutable map/list snapshots. Canonical and spawn lookup are map operations. Weakness rules and magic integrations are lists.

Datapack authors should avoid thousands of narrowly duplicated definitions. Besides reload time, large weakness, magic, and Field Guide-linked canonical sets affect hot gameplay paths described above. Duplicate canonical ownership and duplicate spawn profiles fail replacement rather than creating ambiguous lookup behavior.

## Configuration knobs

The main performance-sensitive common-config values are:

| Setting | Default | Allowed range | Effect |
| --- | ---: | ---: | --- |
| `witnessRadius` | 24 | 4–64 | Spatial query volume per qualifying incident. Volume grows roughly with the cube of radius in dense spaces. |
| `maxWitnessesPerIncident` | 32 | 1–128 | Caps candidates processed after query and sort. |
| `rumorIntervalTicks` | 100 | 20–24,000 | Frequency of rumor batches. |
| `rumorsPerBatch` | 8 | 1–64 | Maximum tasks handled per batch. |
| `rumorPropagationChance` | 0.35 | 0–1 | Transfer probability; indirectly controls follow-up queue growth. |
| `rumorHalfLifeTicks` | 72,000 | 1,200–2,147,483,647 | Exponential confidence half-life for rumor records and cooldown cleanup threshold. |
| `storyCooldownTicks` | 24,000 | 1,200–2,147,483,647 | Limits new incidents per region. |
| `encounterCooldownTicks` | 12,000 | 200–2,147,483,647 | Approximate time for encounter pressure to decay from 100 to 0; one point is removed every floored value divided by 100, with a minimum interval of one tick. |
| `naturalSpawnMultiplier` | 1.0 | 0–4 | Changes profiled natural-spawn acceptance, not handler frequency. |
| `evidenceLifetimeTicks` | 24,000 | 1,200–2,147,483,647 | Controls evidence retention. |
| `contractLifetimeTicks` | 72,000 | 2,400–2,147,483,647 | Controls active contract lifetime and contributes to story expiry. |
| `terminalHistoryRetentionTicks` | 168,000 | 24,000–2,147,483,647 | Keeps completed/expired stories and contracts this long after their original deadline. |

Feature toggles can eliminate their event work. `canonicalization` disables generated-loot rewriting, the two custom biome-removal actions, and profile suppressions marked as canonicalization-driven; it intentionally does not disable datapack reload, tag semantics, or canonical API lookups.

## Operational guidance

- Keep `witnessRadius` near the default in entity-dense settlements; lower it before raising the witness cap.
- Prefer increasing `rumorIntervalTicks` or lowering `rumorsPerBatch` if rumor batches appear in tick profiles. Queue saturation means fidelity is being traded for bounded work, as intended.
- Watch the counts reported by `/folklore diagnostics` and `/folklore stories`/`contracts` on long-running worlds. Diagnostics currently reports data-definition counts and rumor queue length, but not SavedData collection cardinalities.
- Profile qualifying damage events after installing a datapack with many weakness rules, because priority sorting is per hit.
- Back up and inspect the `darkfolklore_society` saved data before major upgrades; a very large social/story history affects both periodic scans and world-save serialization.
- Avoid treating debug logging as a profiler. `debugLogging` adds weakness messages on qualifying hits and should remain off during normal play.

## Known optimization opportunities

The clearest safe optimizations for a future release are to pre-sort weakness rules at reload, pre-index stories/evidence by region and concept, use expiry indexes instead of full maintenance scans, cap or index organization lookups, and expose saved-state cardinalities/timing counters in diagnostics. None of those indexing optimizations is implemented in schema 1, so capacity planning should use the current behavior rather than assume them.
