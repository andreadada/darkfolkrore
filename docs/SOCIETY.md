# Society model

## Facts and beliefs are separate

Provider APIs, exact adapters, registry identity, and entity tags establish factual supernatural state. `SecretFacts.actualSecrets` translates supported facts into `VAMPIRE`, `WEREWOLF`, `HUNTER`, `WITCH`, `FAE_TOUCHED`, and the umbrella `SUPERNATURAL_IDENTITY` where applicable.

Core beliefs use a different key:

```text
(observer UUID, subject UUID, SecretType) -> SocialKnowledgeRecord
```

A record contains `UNKNOWN`, `RUMOR`, `SUSPECTED`, `CONFIRMED`, or `PUBLIC`, confidence from 0 to 1, source, game time, and optional evidence. Merge is monotonic: weaker or less confident input cannot downgrade an existing belief. A false belief or public accusation never writes a provider capability, attachment, relationship, or transformation.

Schema 2 also stores a global `(subject, secret)` public-claim map. Once a claim becomes public, any observer query receives a synthetic `PUBLIC`/1.0 record without creating one persisted row per observer. This prevents reveal fan-out while preserving a universal public result.

## Witness pipeline

The normal path begins after positive living damage when the source is a different living actor with a supported secret.

1. The first applicable primary secret is selected: vampire, werewolf, witch, hunter, then fae-touched.
2. The same actor/victim/secret incident is suppressed for 100 ticks.
3. Core queries nearby social entities: players, villager-like NPCs, and MCA entities. Actor, victim, sleepers, blinded entities, dead entities, and spectators are excluded.
4. Candidates are sorted by distance and capped by `maxWitnessesPerIncident` (32 by default).
5. Line-of-sight observers receive `CONFIRMED`; obstructed observers qualify only within 60% of the radius and receive `RUMOR` testimony.
6. Direct confidence is `min(1, 0.75 + 0.025 * severity)`; indirect confidence is 0.35. Damage severity is half final damage, rounded and clamped to 1..10.
7. The belief is persisted before `WitnessEvent` and `SecretDiscoveredEvent` are posted.
8. At least-suspected knowledge is offered to the rumor queue unless a family/story reaction suppresses retelling.
9. The 8-by-8-chunk region records fear, suspicion, and confirmed public awareness.

The query is event-driven and local. The incident cooldown map cleans entries older than 1,200 ticks once it grows past 2,048; recent unique incidents may temporarily exceed that threshold.

## Exact MCA relationship and personality effects

When exact MCA Reborn `7.7.32+1.21.1` is active, Core can verify only:

- self;
- spouse;
- source is observer's parent or child;
- sibling;
- a player crossing MCA's configured friend threshold;
- a player crossing MCA's configured bounty-target threshold;
- stranger/unknown/not applicable.

MCA 7.7.32 does not expose a general NPC-to-NPC friend/enemy/close-friend graph, so Core does not fabricate those categories. Relationship values contribute only to trust; they do not promote rumor state to factual truth.

Verified source personalities have deliberately small effects: extroverted/introverted change rumor transmission by +0.15/-0.15; anxious changes fear/investigation; relaxed changes fear/investigation in the opposite direction; other audited labels contribute zero. Unknown labels contribute zero. The exact evidence and limits are in [MCA social audit](MCA_SOCIAL_AUDIT.md).

## Family secrets

When relationship trust and family secrets are enabled, a verified spouse, parent/child, or sibling witnessing a secret receives one persisted reaction:

- `PROTECT_SECRET` by default;
- `CONFRONT_RELATIVE` for a sibling;
- `FEARFUL_WITHDRAWAL` for an anxious observer;
- `REPORT_TO_HUNTERS` if the observer is factually a hunter.

Protection suppresses both retelling and organization reporting; confrontation suppresses retelling; fearful withdrawal adds local fear; hunter reporting is allowed. A family-discovery story may be created by the data-driven story engine. This is a small social reaction table, not a replacement for MCA dialogue, marriage, family trees, hearts, or AI.

## Rumor propagation and trust diagnostics

Rumors are local queued deliveries, not recursive society scans.

- Queue maximum: 1,024 tasks.
- Maximum hops: three.
- Processing: at most `rumorsPerBatch` every `rumorIntervalTicks`.
- Local search: 12 blocks and at most 24 shuffled social candidates.
- Delivery cooldown: one sender/subject/secret transfer per four rumor intervals.
- Retelling floor: confidence below 0.1 is not stored; at least 0.2 may be queued for the next hop.
- Diagnostic history: the most recent 128 attempts, including named trust contributions.

Trust begins at 0.5 and can add:

- +0.20 for a shared Core organization;
- +0.10 if the recipient already suspects the claim or +0.20 for credible prior knowledge;
- the exact-MCA relationship contribution loaded from `social_parameters`;
- verified source-personality transmission scaling;
- verified MCA Capitals source-role credibility, scaled by `politicalRumorWeight`.

`RumorRules.retell` always produces `RUMOR` and degrades source confidence before applying trust. `RumorSpreadEvent` is posted only after successful persistence. `/folklore rumor inspect` shows source/recipient/subject, trust, input/output confidence, decision, and named reasons.

A selected `darkfolklore:witness_threatened` story can persistently silence that witness's new rumor deliveries for 12,000 ticks and add local fear. Expired silence records are pruned during the 1,200-tick maintenance path.

Persisted `RUMOR` confidence decays exponentially using `rumorHalfLifeTicks`; records below 0.08 are forgotten. A general low-confidence/old-record pass also runs every 1,200 ticks. Confirmed and public knowledge do not decay through the rumor rule.

## MCA Capitals political context

Exact MCA Capitals 1.1.0 is read-only and fail-closed. Exact emitted titles map to semantic roles; the adapter also reports capital UUID, MCA village ID, capital state, and an independent royal-guard flag where available. Results are cached for 20 game ticks in a 1,024-entry LRU of Core DTOs.

Political weights are data-driven and may influence:

- rumor credibility after a claim already exists;
- organization response and investigation influence;
- regional public-awareness gain from an informed officeholder;
- eligibility for capital-only political-exposure stories after a public reveal.

Political office never creates supernatural knowledge on its own and is never written back to MCA Capitals. See [MCA Capitals](MCA_CAPITALS.md).

## Public reveal

An individual confirmed belief is not automatically public. Automatic reveal requires:

1. organization behavior enabled;
2. a witness belonging to an organization with `public_reveal_authority` (the shipped hunter archetype);
3. at least `publicRevealWitnesses` distinct confirmed observers (default three, minimum two);
4. average confidence at least `publicRevealAverageConfidence` (default 0.75).

An eligible claim is persisted once as a global public claim. Core records public-reveal organization events, raises the subject's regional incident awareness/fear/suspicion, and may create public/political stories. No automatic reveal path merely counts low-confidence rumors.

## Controlled false accusations

False accusations are deliberately rare and story-driven. The shipped path runs only during eligible witching-hour processing when no factual witch story was created, chooses a social subject who is not factually a vampire, creates a low-confidence (0.25) vampire rumor, and requires the enabled `CONTROLLED_FALSE_ACCUSATION` template/rate/cooldown path.

The pure eligibility rule rejects factual secrets, unapproved/non-hostile contexts, confidence outside 0.15..0.55, and states stronger than `SUSPECTED`. The result remains a belief and can decay; it cannot mutate provider state or satisfy the credible confirmed-witness reveal threshold by itself.

## Village regions

A Core village is a social region, not a vanilla POI boundary. `VillageKey` divides every dimension into 8-by-8-chunk regions:

```text
<dimension>|<floor(chunkX / 8)>|<floor(chunkZ / 8)>
```

Each region stores public awareness, vampire/hunter/werewolf/witch influence, fear, suspicion, and political importance, clamped to 0..100. Witness incidents increase suspicion/fear and confirmed awareness. Verified political consequences can add awareness and political importance only after knowledge exists. Family fear adds fear. Contract completion adds 4 awareness, removes 3 suspicion, and adds 3 hunter influence. Reading an unknown region creates an all-zero durable state.

These values are Core narrative pressure, not provider faction reputation or vanilla village statistics.

## Living organizations

The four types are `VAMPIRE_COVEN`, `HUNTER_SOCIETY`, `WEREWOLF_PACK`, and `WITCH_COVEN`. Each organization persists:

- UUID, type, name, leader, home region, and 0..100 influence;
- unique member UUIDs and last-seen game times;
- data-driven objectives;
- subject/secret intelligence with monotonic knowledge state;
- inter-organization relation values;
- a bounded event history.

Hard limits are 256 members, 256 intelligence claims, 1,024 relations, and 64 recent events per organization. The global organization limit defaults to 512. Archetypes add lower normal member caps and determine base influence, auto-founding, objectives, and public-reveal authority.

On a social NPC joining a server level, factual vampire/werewolf/witch identity can found the matching local organization if its archetype permits, or recruit into an existing one at the configured probability. Hunters do not silently found a society merely by loading; a hunter society is created after regional suspicion reaches 30 with accepted witnesses. Existing members update their last-seen time. Last-seen is diagnostic/dormancy information and is not used to delete an unloaded member.

Witnesses share stronger intelligence with their organizations. Hunter reports can open investigations and raise influence; matching covens/packs retain member-secret events; political context can scale response. Completing a local contract changes organization influence and records an event.

Cleanup is evidence-based: confirmed non-player death removes memberships. If the dead entity is leader, the lexicographically smallest remaining UUID becomes the deterministic successor; if no member remains, the organization dissolves. Mere absence/unload never proves death. Autonomous diplomacy/conflict, headquarters, patrol AI, schedules, and provider faction mutation are outside 0.2.

## Data-driven society stories

Core ships twelve templates across eight triggers:

- family discovery;
- public reveal;
- hunter investigation (including witness threatened);
- organization recruitment;
- full-moon incident;
- witching hour;
- controlled false accusation;
- political exposure.

An enabled template can filter by required secret and capital context, has a weight, lifetime, regional cooldown, wildcard/fixed concept, and explicit contract-eligibility flag. Creation also passes through `dynamicStoryRate` with an event-specific scale. Actors are capped at eight per story; world-event discovery inspects at most four players and sixteen nearby social actors per player.

Full-moon werewolf and witching-hour ritual stories create matching logical evidence. Public reveal can create political scandal/heir stories only for a verified non-common political role. Incident stories from supernatural kills remain the primary contract source, while additional templates opt into contracts explicitly.

## Lineage and reputation

On entity join, an exact compatibility adapter may expose a vampire/werewolf conversion-source UUID. Core stores that source once as provenance. It does not fabricate a source, infer biology, walk ancestry, or write back to the provider.

Core reputation factions are villagers, hunters, vampires, werewolves, witches, fae, and occultists. Contract completion currently grants +10 villagers and +8 hunters. These values remain separate from provider faction/relationship mechanics.

## Operator commands

All commands require permission level 2:

```text
/folklore inspect <entity>
/folklore social get <observer> <subject> <secret>
/folklore social set <observer> <subject> <secret> <state> <confidence>
/folklore social inspect <entity>
/folklore rumor inspect
/folklore capitals inspect <entity>
/folklore organization list
/folklore organization inspect <uuid>
/folklore organization create <type> <leader> <name...>
/folklore village [inspect]
/folklore story list
/folklore stories
/folklore contracts
```

`social set` uses source `ADMIN` but still merges monotonically. There is no ordinary-player ground-truth command and no command in 0.2 to delete a belief, public claim, lineage, story, historical contract, or organization.
