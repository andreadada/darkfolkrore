# Knowledge systems

Dark Folklore has two intentionally separate knowledge models:

- **Lore knowledge** is a player's bounded mastery of a namespaced concept.
- **Social knowledge** is one observer's belief about one subject's secret identity.

Lore is not proof of a particular entity's identity. Social belief is not the entity's mechanical state. Addons should preserve that distinction.

## Lore points and stages

`LoreProgress` contains an integer clamped from 0 through 100. Only nonnegative grants are supported. Stages are derived rather than stored:

| Stage | Minimum points |
| --- | ---: |
| `UNKNOWN` | 0 |
| `DISCOVERED` | 1 |
| `OBSERVED` | 25 |
| `STUDIED` | 60 |
| `MASTERED` | 100 |

Progress is keyed by player UUID and arbitrary concept string. Built-in gameplay uses namespaced concepts, but `FolkloreSavedData.addLore` itself does not validate the concept's syntax or require a canonical definition.

Every effective grant posts `KnowledgeChangedEvent` with the player, concept, before value, and after value. Crossing a stage boundary also displays an action-bar message. A grant at 100 that remains 100 does not post an event.

## Automatic lore sources

| Trigger | Award |
| --- | --- |
| A server player starts tracking a living entity with at least one Core creature trait | 1 point to that entity's concept, only while the player's value is 0 |
| A server player kills an entity with at least one Core creature trait | 15 points to the entity concept and 3 points to `darkfolklore:monster_lore` |
| A server player picks up an item tagged `#darkfolklore:archaeological_lore` | 10 points once to the item's canonical concept, or `darkfolklore:forbidden_lore` if unmapped |
| A tagged item pickup leaves all required traits for a magic integration represented across the player's inventory | The integration's configured reward and points, only while that reward concept is at 0 |
| A monster contract is completed | 20 points to the target concept |
| An audited Field Guide entry is already unlocked | 10 points once to its canonical entity concept |

The kill awards are repeatable until the 100-point clamp. Tracking, archaeology, magic integration, and Field Guide translation use discovery-once semantics: they grant only when the current value is exactly 0.

The entity concept is resolved from the canonical registry first. If no mapping exists, trait priority falls back to vampire, werewolf, fae, spirit, undead, and finally `darkfolklore:supernatural`.

## Magic discovery semantics

After any pickup with at least one Core item trait, Core gathers traits from that item and every stack in the player's main inventory list. It then checks each loaded magic integration. If all required traits are present, Core discovers the configured reward once.

This is a semantic inventory conjunction, not a recipe or ritual execution. The participating items are not consumed, item counts are not checked, equipment/offhand inventories are not explicitly scanned beyond what appears in the main inventory list, and no spellcasting API is called.

## Field Guide boundary

The Field Guide adapter is loaded only for exactly audited Field Guide 1.14.0. Field Guide keeps its own binary unlocked/not-unlocked progress.

On a qualifying player kill, Core requests Field Guide's `KILL` trigger for the entity entry. Every 100 player ticks, the adapter checks canonical entity concepts the player has not discovered and asks Field Guide whether any corresponding implementation entry is unlocked. An unlock becomes one 10-point Core discovery.

The bridge is asymmetric. Core lore gained from tracking, archaeology, a contract, an admin grant, or a magic integration does not generally unlock a Field Guide entry. Core also does not turn Field Guide into a tiered system or overwrite its custom descriptions. Adapter runtime failures are caught and logged at debug level so a temporarily unavailable progress manager does not break server play.

## Social knowledge

Social knowledge is keyed by observer UUID, subject UUID, and `SecretType`. Self-observation keys are invalid. Supported secret types are vampire, werewolf, hunter, witch, occultist, fae-touched, cursed, and supernatural identity.

States have monotonic strength:

```text
UNKNOWN < RUMOR < SUSPECTED < CONFIRMED < PUBLIC
```

A persisted record includes:

- confidence clamped from 0 through 1;
- source: direct witness, physical evidence, testimony, rumor, organization, investigation, public reveal, or admin;
- game time;
- optional evidence type.

Merging never lowers state or confidence. This protects confirmed knowledge from weak retellings, but it also means normal gameplay cannot retract a false high-confidence record. There is no negative evidence or contradiction model yet.

Automatic direct witnesses receive confirmed beliefs; nearby observers without line of sight can receive rumor testimony. Successful rumor transfers always create rumor-state records with degraded confidence. Detailed propagation, retention, and village effects are in `SOCIETY.md`.

Every 1,200 ticks, persisted `RUMOR` records are recalculated with exponential half-life decay from their stored timestamp to the current game time. Survivors are timestamped at that maintenance pass, and rumors below 0.08 are removed. A separate general prune can remove non-public records below 0.12 after four configured half-lives. Stronger non-rumor states remain monotonic unless a future explicit deletion mechanism is added.

## Actual facts versus social records

`SecretFacts.actualSecrets(entity)` reads current traits, which in turn combine datapack tags and exact-version optional-mod queries. It does not consult stored social records. Conversely, reading social knowledge does not revalidate it against the current fact. This allows rumors, mistakes, secrets that survive disguise, and beliefs that outlive a transformation, but consumers must choose deliberately whether they need truth or belief.

For social disguise interpretation, `SecretIdentityService.canBeFooled(server, observer, subject, secret)` returns true only for unknown or rumor states. It does not alter another mod's mechanical disguise system by itself.

## Supported API queries

Use `com.darkfolklore.core.api.DarkFolklore` rather than persistence or adapter internals:

```java
boolean vampire = DarkFolklore.isVampire(entity);
Set<CreatureTrait> traits = DarkFolklore.creatureTraits(entity);
Set<ItemTrait> itemTraits = DarkFolklore.itemTraits(stack);
Optional<CanonicalDefinition> concept = DarkFolklore.resolveCanonicalEntity(entity.getType());
LoreProgress progress = DarkFolklore.lore(player, "darkfolklore:vampire");
Optional<SocialKnowledgeRecord> belief = DarkFolklore.socialKnowledge(
        server, observerId, subjectId, SecretType.VAMPIRE);
int reputation = DarkFolklore.reputation(server, holderId, ReputationFaction.HUNTERS);
```

Trait and canonical results are snapshots/read-only values. Lore and reputation queries return current server state. The social query returns empty for unknown, not a synthesized `UNKNOWN` record.

## Public events

Knowledge-related notifications are posted on `NeoForge.EVENT_BUS`:

- `KnowledgeChangedEvent` after lore points actually change;
- `SecretDiscoveredEvent` after an accepted witness record is merged;
- `WitnessEvent` for each accepted observer in an incident;
- `RumorSpreadEvent` after a successful retelling.

These events are observational and not cancellable. `SecretDiscoveredEvent` is currently posted by the witness engine; an admin `social set` merge does not post it.

## Operator commands

The entire command tree requires permission level 2:

```text
/folklore knowledge get <player> <concept>
/folklore knowledge grant <player> <concept> <points 1..100>
/folklore social get <observer> <subject> <secret>
/folklore social set <observer> <subject> <secret> <state> <confidence 0..1>
/folklore inspect <entity>
```

Lore grants add points; they do not set an absolute value. Social sets merge, so they cannot downgrade or clear a record. `inspect` reports actual traits and facts alongside aggregate observer-state counts, making it a diagnostic command rather than an in-world discovery mechanic.

There is currently no client knowledge GUI, packet protocol, export command, record deletion command, or player-accessible query command. Addons that expose knowledge to players must enforce their own information-access rules and should avoid revealing actual facts when only observer belief is appropriate.
