# Dark Folklore Core architecture

## Scope and authority

Dark Folklore Core 0.2.0 is a logical-server coordination layer for Minecraft 1.21.1 and NeoForge 21.1.248. It reads provider-owned facts, resolves them into shared concepts/traits, and owns the cross-mod consequences built on those semantics: lore, beliefs, witnesses, rumors, village pressure, organizations, stories, evidence, contracts, reputation, spawn pressure, and world-event state.

Core does not grant or cure vampirism/lycanthropy, rewrite MCA relationships or offices, replace a provider's AI/progression/UI, or store foreign implementation objects. A social claim can be wrong; it never becomes the source of factual supernatural state.

| Question | Authority |
| --- | --- |
| Is this entity mechanically a vampire, werewolf, or hunter? | Exact provider API/adapter, otherwise Core tags or an explicit unknown/not-applicable result. |
| What does one observer believe about a subject? | Core schema-2 social knowledge. |
| Who is spouse/parent/child/sibling or a qualifying player friend/bounty target? | Exact MCA 7.7.32 read-only evidence. Unsupported NPC friendship/enmity is not inferred. |
| What political title/capital is attached to an MCA entity? | Exact MCA Capitals 1.1.0 read-only evidence. |
| Which binary guide pages are unlocked? | Field Guide 1.14.0. Core requests native unlocks; it does not edit Field Guide files directly. |
| What is the preferred shared concept or item family? | Reloaded Core canonical policy plus explicitly audited tags/recipes/loot/worldgen/runtime bridges. |
| What are the organizations, public claims, stories, contracts, and regional values? | Core. |

## Runtime layers

| Layer | Main packages | Responsibility |
| --- | --- | --- |
| Bootstrap | `com.darkfolklore.core` | Registers common config, codecs, reload listener, commands, and server gameplay handlers. |
| Compatibility | `compat` | Exact-version, fail-closed provider reads and compact Core DTOs. |
| Semantics/data | `canonical`, `traits`, `data` | Shared concepts/tags, canonicalization codecs, and one atomically published definition state. |
| Rule engines | `weakness`, `spawn`, `magic`, `world` | Guarded damage, natural-spawn/pressure, semantic discoveries, and world events. |
| Knowledge/society | `knowledge`, `society`, `reputation` | Lore, beliefs, witnesses, trust, families, organizations, villages, lineage, and stories. |
| Investigation | `investigation`, `contracts` | Logical evidence, testimony, contract state, rewards, and consequences. |
| Persistence | `persistence` | Schema-2 overworld `SavedData`, row-level defensive decoding, and schema-1 upgrade. |
| Extension/operations | `api`, `api.event`, `diagnostics` | Convenience query facade, observational events, and operator-only commands. |

The main flow is:

```text
provider fact or registry/tag identity
  -> exact compatibility/semantic resolution
  -> bounded event-driven engine
  -> Core state mutation
  -> observational event and player/operator feedback
```

Client text and vanilla particles are feedback. Core registers no custom authoritative client cache or custom packet channel. Field Guide uses its own normal progress synchronization when its exact adapter is active.

## Bootstrap and optional integration lifecycle

`DarkFolkloreCore` registers its common handlers on `NeoForge.EVENT_BUS`; codecs and common setup listeners use the mod bus. `CompatibilityManager.initialize` runs in enqueued common setup work and records one report for every audited integration.

Each report is one of:

- `ACTIVE`: exact audited display version and required signatures are available.
- `DISABLED`: optional mod is absent.
- `UNTESTED_VERSION`: mod is present at a different version; implementation-specific reads remain off.
- `ERROR`: the exact version was present but a required bridge/signature failed.

Typed Vampirism and Field Guide bridge classes are dynamically loaded only after their exact gates. MCA/MCA Capitals reflective handles are resolved and cached during activation. Query failure returns unknown/disabled context and logs a bounded warning instead of inventing state. MCA Capitals role results are Core DTOs held in a 1,024-entry LRU for 20 game ticks; no foreign objects enter the cache.

The wolfsbane bridge has an independent two-provider gate: exact Werewolves 2.0.3.3 plus Enchanted 4.2.7. It preserves Werewolves mechanics only when the audited signatures are present. See [Wolfsbane audit](WOLFSBANE_AUDIT.md).

## Atomic server-data reload

`FolkloreDataManager` scans eight directories during the prepare phase:

```text
canonical                 weaknesses
spawn_profiles            magic_integrations
story_templates           organization_archetypes
social_parameters         political_weights
```

Every file is parsed into a candidate collection. The candidate then receives cross-definition validation: unique canonical ownership, IDs/entity profiles, story IDs, organization types, political roles, and the single social-parameter constraint. Only an error-free candidate becomes a complete immutable `State` and is assigned to the volatile state reference.

If any parsing or cross-record error exists, apply logs an atomic rejection and retains the previous validated state. Readers therefore see either the old complete state or the new complete state, never an incrementally rebuilt mixture. On initial startup with no prior valid state, rejection leaves the empty state active. `/folklore diagnostics` exposes the candidate error count.

Field Guide categories, standard tags/recipes, loot modifiers, and NeoForge biome modifiers remain under their owning loaders and are not part of this transaction.

## Event-driven society flow

There are no global every-tick entity scans.

1. A relevant NeoForge event supplies an actor, observer, item, position, spawn attempt, death, interaction, or world-event transition.
2. Core checks the appropriate common-config gate.
3. Registry tags and exact adapters resolve facts without mutation.
4. Spatial work is local and capped; rumor work is queued/batched; organization/story work is event-triggered.
5. A successful durable consequence is written to `FolkloreSavedData` and marked dirty.
6. Public observational events are posted after the state change.

Examples:

- Positive supernatural damage selects at most the nearest configured witnesses, persists beliefs, computes family reactions, posts witness/discovery events, and optionally queues rumors.
- Rumor delivery inspects at most 24 local social candidates, uses a queue of at most 1,024 tasks and three hops, records at most 128 diagnostics, and processes a configured batch.
- Social entity join can found/recruit into a compatible archetyped organization without scanning unloaded entities.
- Confirmed non-player death removes organization membership, deterministically succeeds a dead leader, or dissolves the final-member organization. Mere unload is not treated as death.
- Witness/public/world/organization events may select a validated story template using rate, weight, secret, capital, and per-template regional cooldown rules.
- Contract block/entity interactions consume evidence/testimony server-side and provide text/particle feedback.

## Durable schema-2 state

All Core-owned durable state is anchored in the overworld data storage under file ID `darkfolklore_society`. Positions may reference any dimension.

Schema 2 persists:

- player lore points;
- observer-specific social beliefs;
- global public secret claims without per-observer fan-out;
- family-secret reactions;
- reputation ledgers;
- organizations, objectives, intelligence, relations, bounded event histories, membership, and influence;
- 8-by-8-chunk regional village state;
- provider-supplied conversion lineage provenance;
- logical evidence and collection ownership;
- contract assignments and state;
- persistent story actors/status/region;
- per-player encounter pressure.

Schema-1 rows are decoded using their existing fields. New lists are absent and therefore empty; old organizations retain constructor defaults for objectives and receive empty intelligence/relations/events. The load is marked dirty, the next save writes schema 2, and a subsequent schema-2 load does not reapply migration. A newer schema produces a warning and best-effort row decoding; no downgrade writer exists.

Every compound-list row is read independently. A malformed row is logged and skipped rather than aborting other rows. This defensive behavior is corruption containment, not a guarantee that an arbitrarily damaged world can be repaired.

Transient state includes rumor tasks/cooldowns/diagnostics, witness cooldowns, active computed world events, compatibility reports, reload errors, Field Guide polling state, organization maintenance cursor, and MCA Capitals role cache. Restarting clears these without erasing durable outcomes.

## Scheduling and growth control

- Rumors process every configured `rumorIntervalTicks`, at most `rumorsPerBatch` tasks.
- Contract/story expiry and world-event checks run every 200 server ticks.
- Rumor decay, general social/evidence/narrative cleanup, and stale cooldown cleanup run every 1,200 ticks.
- Organization maintenance runs every 1,200 ticks with a configurable per-pass budget and enforces the configured social-record safety limit while organizations are enabled.
- Organization event history is capped at 64 and intelligence at 256 entries per organization. Organization creation is capped by config (512 by default).
- Field Guide synchronization polls each server player every 100 player ticks over the small canonical definition set.
- MCA Capitals role cache is capped and expires after 20 ticks.

The general performance model is detailed in [Performance](PERFORMANCE.md). Some long-lived maps remain retention-based or naturally bounded by actual players/entities/regions rather than a universal hard cap; see [Known Limitations](KNOWN_LIMITATIONS.md).

## Extension and operator surfaces

`com.darkfolklore.core.api.DarkFolklore` provides convenience queries for facts, traits, canonical concepts, lore, social knowledge, and reputation. Observational events include knowledge/secret changes, witnesses, rumors, contract start/completion, and world-event changes. The 0.2 production policy does not promise a semver-stable public Java API; addons should pin the Core version and prefer documented data/tags where possible.

All `/folklore` commands require permission level 2. The expanded diagnostics distinguish registry/static canonical identity, dynamic provider facts, beliefs/public claims, organizations, lineage, political context, rumor trust reasons, Field Guide status, wolfsbane bridge state, stories, contracts, and local village values. These commands can reveal ground truth and must not be exposed as ordinary-player gameplay.

## Configuration boundaries

Common toggles cover canonicalization, weaknesses, lore, social knowledge, witnesses, rumors, organizations, village society, spawning, encounter pressure, contracts, dynamic stories, world events, exact relationship trust, personality effects, family secrets, organization behavior, MCA Capitals, false accusations, and debug logging. Numeric bounds cover local/batch work, half-life/retention, story/recruitment rates, public thresholds, political scaling, maintenance budgets, and persisted social/organization caps.

`canonicalization=false` disables Core enforcement layers but keeps semantic definitions/tags available for queries and interoperability. Exact optional adapters remain governed by compatibility/version gates; turning a downstream feature off does not make an unavailable provider fact available. Config changes should be exercised on a disposable world and verified through diagnostics after restart/reload.
