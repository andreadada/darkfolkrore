# MCA Social Audit — 7.7.32+1.21.1

## Audited artifacts

| Artifact | Declared mod/version | SHA-256 |
| --- | --- | --- |
| `mca-neoforge-7.7.32+1.21.1.jar` | `mca` / `7.7.32+1.21.1` | `874B5BD82D754033117EE6C1E7B5EBD142EC5DC0DF2881C9BB2F38A05AE7F4AB` |
| `mca-vamp-compat-1.21.1-2.0.12.jar` | `mca_vamp_compat` / `2.0.12` | `BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF` |

The MCA JAR contains 618 classes. The audit used its NeoForge metadata and Java 21 class signatures/bytecode as the authority; it did not infer methods from an online version.

## Verified relationship access

These are public methods in the installed MCA JAR, although they are implementation classes rather than a separately versioned API package:

| Class | Verified access used by Core |
| --- | --- |
| `net.conczin.mca.server.world.data.FamilyTree` | `get(ServerLevel)`, `getOrEmpty(UUID)` |
| `net.conczin.mca.server.world.data.FamilyTreeNode` | `partner()`, `getRelationshipState()`, `isParent(UUID)`, `siblings()` |
| `net.conczin.mca.entity.ai.relationship.RelationshipState` | `isMarried()` |
| `net.conczin.mca.entity.VillagerEntityMCA` | `getVillagerBrain()`, `getTraits()` |
| `net.conczin.mca.entity.ai.brain.VillagerBrain` | `getPersonality()`, `getMemories()` |
| `net.conczin.mca.entity.ai.Memories` | `getHearts()` |
| `net.conczin.mca.Config` | `getInstance()`, public `heartsToBeConsideredAsFriend`, public `bountyHunterHearts` |

`FamilyTree.get` stores the family tree in the server overworld, so relationships remain available when the two entities are in different dimensions on the same logical server. Core uses `getOrEmpty`; it never calls `getOrCreate` from a read path.

### Supported facts

`McaSocialAdapter.relationship(observer, source)` exposes these directional categories:

- `SPOUSE`, only when the source is the observer's partner and MCA's relationship state is married;
- `SOURCE_IS_PARENT`;
- `SOURCE_IS_CHILD`;
- `SIBLING`;
- `PLAYER_FRIEND`, using MCA's live `heartsToBeConsideredAsFriend` value (default in the audited bytecode: 40);
- `PLAYER_BOUNTY_TARGET`, using MCA's live `bountyHunterHearts` value (default: -150);
- `STRANGER` only when enough MCA data exists to rule out the supported relationships;
- `UNKNOWN` when the family/affinity evidence is absent or a query fails.

The adapter reads a villager's existing memories map. It deliberately avoids `getMemoriesForPlayer`, because that method creates a new memory entry when none exists and is therefore not a read-only query.

### Unsupported relationship facts

- MCA 7.7.32 has no general NPC-to-NPC friend, close-friend, rival, or enemy graph that this audit could verify.
- The configured player-friend threshold is real, but there is no separate verified “close friend” threshold.
- Negative hearts are retained as diagnostic affinity evidence but are not renamed `ENEMY`. Only MCA's actual bounty-target threshold gets a semantic category.

Those individual facts are blocked; spouse and family trust are not blocked.

## Personality and traits

`VillagerBrain.getPersonality()` is a stable public accessor in this exact JAR. The actual enum constants are:

`UNASSIGNED`, `FRIENDLY`, `FLIRTY`, `PLAYFUL`, `GLOOMY`, `SENSITIVE`, `GREEDY`, `ODD`, `CRABBY`, `EXTROVERTED`, `INTROVERTED`, `RELAXED`, `ANXIOUS`, `PEACEFUL`, and `UPBEAT`.

Core's small pure mapping uses only labels present in that list:

| MCA personality | Dark Folklore interpretation |
| --- | --- |
| `EXTROVERTED` | +0.15 rumor transmission tendency |
| `INTROVERTED` | -0.15 rumor transmission tendency |
| `ANXIOUS` | +0.15 fear, -0.10 investigation tendency |
| `RELAXED` | -0.10 fear, +0.05 investigation tendency |
| Other verified values | Recognized, but no behavior inferred |

MCA does not have personalities named brave, cautious, aggressive, gossipy, or reserved in 7.7.32. Core does not invent aliases for them.

The installed MCA genetic/physical trait registry exposes IDs for lactose intolerance, sexuality, albinism, rainbow variants, Sirben, dwarfism, heterochromia, color blindness, athletic, left-handed, weak, tough, coeliac disease, diabetes, vegetarian, infertile, electrified, and no-aging traits. None is treated as a gossip personality.

MCA Vamp Compat 2.0.12 additionally registers the exact MCA trait IDs `vampirism_mca_compat:vampire`, `vampirism_mca_compat:werewolf`, and `vampirism_mca_compat:hunter`. These are available through `traitIds` for diagnostics, while factual supernatural identity continues to come from the existing `McaVampCompatAdapter` service queries rather than trait-name guessing.

## Trust model

`McaTrustModel` is dependency-free and returns both a modifier and a bounded list of reasons suitable for administrator diagnostics. Defaults are:

| Evidence | Default modifier |
| --- | ---: |
| Self | +0.30 |
| Spouse | +0.25 |
| Parent or child | +0.22 |
| Sibling | +0.18 |
| MCA player friend | +0.12 |
| MCA bounty target | -0.20 |

All values are replaceable through `McaTrustSettings`. Unknown, unsupported, and not-applicable evidence contributes zero. This model changes belief confidence only; it must never change a supernatural fact.

## Adapter hardening

- `McaSocialAdapter.initialize(actualVersion)` activates only for exact `7.7.32+1.21.1`.
- Every class, method, and field is resolved once during initialization.
- A version or signature mismatch produces an actionable disabled reason without loading optional classes on the mismatch path.
- Query failures return unknown/empty evidence and produce at most one concise warning per adapter instance.
- No Atlas output, local scan, configuration file, or mod JAR is needed at runtime.
- Trait enumeration is documented as a diagnostics/story operation, not a per-tick polling path.

## Wiring contract

Compatibility initialization should pass MCA's exact `ModInfo` version to `initialize`. At rumor delivery:

1. Query `relationship(recipient, source)`.
2. Evaluate it with `McaTrustModel` and add the returned modifier/reasons to the existing trust result.
3. Use `McaPersonalityInfluence` for transmission/fear/investigation tendency only.
4. Keep factual identity and observer belief separate.

Diagnostics should print the relationship category, optional player hearts, exact personality names, trust contribution reasons, adapter status detail, and never dump raw reflective objects.

## Validation and remaining smoke work

Nine focused unit tests cover version fail-closed behavior, trust settings/reasons, refusal to map an invented personality, exact political-title mapping, and political weights. The exact signatures were also verified with `javap` against the installed binaries.

An in-game smoke still needs real MCA spouses, parents/children/siblings, positive-heart and bounty-threshold players, and each personality. The unit tests cannot construct MCA tracked entity data safely outside a running server, so they do not claim live entity-state coverage.
