# Investigation and monster contracts

Dark Folklore owns a server-authoritative contract backend. It is playable through ordinary interactions and messages without Quest Giver or another quest mod. Evidence remains a logical saved-data point; Core uses text and vanilla particles as search/collection feedback rather than adding a clue block, item, waypoint, or journal screen.

## End-to-end flow

```text
eligible supernatural incident/story
  -> logical evidence and/or credible testimony
  -> player accepts from a local villager/MCA issuer
  -> INVESTIGATING
  -> two distinct evidence types
  -> IDENTIFIED (canonical target is revealed)
  -> matching entity killed by contract owner
  -> HUNTED
  -> return to exact issuer
  -> COMPLETE + reward + lore/reputation/village/organization consequences
```

## Incident and story sources

The primary incident path requires:

- `dynamicStories=true`;
- a recognized supernatural living actor causing death;
- an animal, vanilla villager, or MCA entity as victim;
- no `drained_animal`/`body_discovered` incident inside the same 8-by-8-chunk society region during `storyCooldownTicks`.

Animal victims create `drained_animal`; villager/MCA victims create `body_discovered`. The persistent story stores canonical concept, actor/victim UUIDs, position/dimension, region, creation/expiry, and status. It expires after twice the configured contract lifetime.

The engine creates:

- `BLOOD` at the victim position;
- `BITE_MARK` for vampire incidents, `FOOTPRINT` for werewolves, otherwise `MAGICAL_RESIDUE`, at the actor position.

Both evidence points carry concept, optional actor subject, position, creation/expiry, and optional collector. The same death runs the witness pipeline with `BODY` evidence and severity 8.

Data-driven society templates may also be contract sources only when `contract_eligible` is explicitly true. The shipped full-moon werewolf and witching-hour ritual paths create one matching logical clue; testimony can provide the second distinct type. The `SocietyStoryEngine` keeps the old incident templates contract-eligible for backward compatibility.

## Accepting

Empty-handed sneak-right-click a vanilla `AbstractVillager` or an entity in the `mca` namespace. The issuer's Core region must contain an `INCIDENT` story that is contract-eligible. The oldest eligible local story is selected.

Only one non-terminal contract may be active per player. Acceptance stores:

- contract/player/issuer UUIDs;
- canonical target concept;
- investigation center and region key;
- expiry time;
- required distinct evidence count (two in the runtime path).

The story advances to `INVESTIGATING`, `ContractStartedEvent` is posted, and the player receives investigation coordinates. The acceptance message does not reveal the target concept.

## Collecting positional clues

While `INVESTIGATING`, empty-handed sneak-right-click a block. The server searches for the nearest uncollected evidence that:

- matches the contract's hidden canonical concept;
- is in the current dimension;
- is within four blocks of the clicked position.

Each `EvidenceType` counts once. A successful clue:

- records the collector UUID;
- adds the type to the contract;
- grants 2 lore points for the hidden concept;
- emits happy-villager particles at the logical clue position;
- reports the type and progress.

When the second distinct type changes the state to `IDENTIFIED`, Core grants another 8 lore points and reveals the canonical target. Repeating the same clue type cannot advance the count.

When the player searches within 32 blocks of the investigation center but no usable clue is within four blocks, Core provides an action-bar hint and smoke particles at the clicked block. Miss feedback is rate-limited to once every 40 ticks per player. These particles do not persist or reveal the exact logical point before a successful interaction.

## Witness testimony

During `INVESTIGATING`, the same empty-handed sneak-right-click interaction can record testimony from a villager/MCA person other than the player. The witness must already hold at least `SUSPECTED` knowledge at confidence 0.35 or greater whose secret maps to the contract concept.

The best matching belief supplies `TESTIMONY` as a logical evidence type. It counts only once, grants 3 lore points, reports the witness confidence, and can supply the identification threshold. If it identifies the target, the same 8-point identification bonus applies.

Testimony uses existing Core belief; it does not fabricate a factual secret, expose the target before identification, create a positional `EvidenceRecord`, or consume/remove the witness's knowledge.

## Hunt and completion

Only a direct kill by the contract owner is considered. While `IDENTIFIED`, Core compares the victim's live factual canonical concept and static canonical registry mapping with the target concept. A match advances to `HUNTED`, advances a matching investigating story to `CONFRONTATION`, and instructs the player to return.

Completion requires empty-handed sneak-right-clicking the exact issuer UUID. It grants:

- 8 emeralds (dropped if inventory insertion fails);
- 150 experience points;
- +10 villager reputation;
- +8 hunter reputation;
- 20 target-concept lore points;
- +4 regional public awareness;
- -3 regional suspicion;
- +3 regional hunter influence;
- local organization influence/event consequences;
- a resolved matching story;
- `ContractCompletedEvent` after state/rewards are committed.

The contract validates a canonical concept, not the original culprit UUID. It does not spawn, reserve, or track a target entity.

## State and expiry

Contract states:

```text
OFFERED -> INVESTIGATING -> IDENTIFIED -> HUNTED -> COMPLETE
   non-terminal state ---------------------------> EXPIRED
```

`CANCELLED` is a terminal model value but has no player/operator transition in 0.2.

Story states:

```text
INCIDENT -> INVESTIGATING -> CONFRONTATION -> RESOLVED
                    \----------------------> RESOLVED
non-terminal state ------------------------> EXPIRED
```

Invalid transitions return false without mutation. Every 200 ticks, Core expires overdue contracts and stories. Every 1,200 ticks it removes expired evidence and prunes terminal narratives only after their original deadline plus `terminalHistoryRetentionTicks` (168,000 by default). This keeps recent history available to diagnostics without indefinite normal growth.

Contracts, evidence, stories, and testimony evidence sets persist in `darkfolklore_society` schema 2. Schema-1 contracts/stories/evidence load without a shape change.

## Diagnostics

All commands require permission level 2:

```text
/folklore contracts
/folklore story list
/folklore stories
/folklore inspect <entity>
/folklore social inspect <entity>
/folklore knowledge get <player> <concept>
```

Contract/story diagnostics expose administrative ground truth, IDs, target concepts, statuses, and evidence sets. They are not player-safe journals.

## Current boundaries

- There is no journal, waypoint, rendered evidence object, scent trail, Quest Giver frontend, abandonment, or issuer reassignment.
- The exact issuer must remain available; issuer death/removal can prevent completion.
- Rewards and runtime required-clue count are code-defined. Story templates, weights, cooldowns, lifetimes, concepts, and contract eligibility are data-driven.
- Hunt validation accepts any entity matching the target concept, not necessarily the original actor.
- Story association is by region/concept/status rather than a persisted story UUID on the contract, so multiple matching stories can make the first eligible one receive a transition.
- Positional collection relies on periodic expiry pruning; an evidence row just past its timestamp can remain queryable until the next maintenance pass.
- Logical evidence is discoverable only through informed searching and interaction feedback; there is no persistent visual marker.
- Ambient fabricated contracts are not generated. Controlled false accusations are society beliefs/stories and do not by themselves create a factual target.
