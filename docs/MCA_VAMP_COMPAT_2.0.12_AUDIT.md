# MCA Reborn × Vampirism Compat 2.0.12 exact-provider audit

This audit defines the version-specific code and ownership boundary used by Dark Folklore Core 0.4.0.

## Audited binary

The development audit inspected a user-supplied `mca_vamp_compat` 2.0.12 JAR for Minecraft 1.21.1 / NeoForge.

SHA-256: `BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`

The binary is **not** committed, bundled, shaded, or redistributed by Dark Folklore Core.

## Ownership rule

> MCA Reborn × Vampirism Compat remains authoritative for factual supernatural MCA mechanics. Dark Folklore observes those mechanics and implements knowledge, investigation, rumor, reputation and narrative consequences around them.

Provider-owned **FACT** includes infection eligibility/chance/duration, conversion, cure, inherited vampirism, capability persistence, appearance normalization, target selection, navigation, and native MCA vampire combat/infection-bite AI. Dark Folklore-owned **BELIEF** includes observer knowledge/suspicion, rumors, evidence, investigations, reputation, stories, and contracts.

Core does not expose a generic `forceInfect`, `forceConvert`, `forceCure`, `applyInheritance`, replacement-goal, target, or navigation API. Belief never changes or substitutes for provider fact.

## Activation and failure isolation

The exact adapter stack requires all of:

- Vampirism `1.10.12`;
- MCA Reborn `7.7.32+1.21.1`;
- MCA Reborn × Vampirism Compat `2.0.12`.

After that triple version gate, factual queries, predation, and lifecycle observation initialize independently. The combined provider status can be `ACTIVE`, `PARTIAL`, or `ERROR`; factual MCA routing uses the fact component's status and never falls through to generic Vampirism detection. Absent authority returns `NOT_APPLICABLE`; untested, partial, unsupported, or failed authority returns `UNKNOWN`.

Expected reflective classes/methods are resolved during initialization. Runtime/linkage errors open the affected optional bridge circuit, return unavailable/unknown state, and log once rather than failing a hot loop.

## Exact 2.0.12 signatures audited

### `McaVampireBiteService`

Public methods include:

```text
canBite(PathfinderMob, LivingEntity)
isNonLethalInfectionBite(PathfinderMob, LivingEntity)
applyInfectionBite(PathfinderMob, LivingEntity)
applyHunterBloodBite(PathfinderMob, LivingEntity)
applySanguinareInfection(LivingEntity, LivingEntity)
shouldCancelBiteDamage(Entity, LivingEntity)
wasRecentlyBitten(LivingEntity)
recordPlayerConversionBite(Player, VillagerEntityMCA)
canMcaVillagerReceiveInfection(LivingEntity)
```

Dark Folklore reads provider eligibility/marker state; it does not call infection mutation methods. The provider chooses the MCA human target and performs the bite.

### `McaVampireInfectionService`

Public methods include infection eligibility, configured duration/chance, and source-type helpers. These remain provider-owned and are not used as a second infection authority.

### `McaVampireStateService` and `VampiricVillagerState`

Audited reads used by 0.4 include:

```text
isInfected()
isConverted()
isCuringVampire()
isFactionInheritanceProcessed()
isBiteWasConversionCause()
areAiGoalsAdded()
getSource()
```

The state also owns mutation methods such as `startInfection`, `finishConversion`, `markInheritedVampire`, `clearVampirismAfterCure`, `canBite`, and `markBite`. Core does not invoke infection/conversion/cure/inheritance mutations. Its optional animal-feed path uses only the provider cooldown marker plus Vampirism's real creature blood store; it cannot infect or replace an animal.

### `McaVampireAi`

`registerGoalsIfNeeded(LivingEntity)` is audited as the provider's idempotent goal-repair extension point. Core may call it only after the same exact provider reports a factually converted MCA vampire whose provider goals are missing. The goals, subsequent target choice, navigation, and native bite remain provider-owned. Core never installs replacement AI and never sets/clears a target or navigation path.

### `McaVampireInheritanceHandler`

Audited provider methods include `applyInheritance`, `applyDecision`, `decide`, and parent/chance helpers. Provider `VampiricVillagerEvents.onBabySpawn` invokes inheritance. Dark Folklore observes the child only after provider handling and never calls inheritance mutations.

Inherited state has no conversion-source UUID. Core retains both parents only as bounded runtime birth context; it does not invent a one-parent lineage.

### `McaVampireCureService`

Audited methods cover interaction handling, cure-state queries, tick processing, cancellation, village-capture cure, and completion. Core reads `isCuringVampire` and final factual state and never advances cure stages.

Provider 2.0.12 may retain inheritance, bite-cause, and conversion-source metadata when a cure is cancelled. The 0.4 classifier therefore gives the prior factual `CURING` state precedence: `CURING → VAMPIRE` is `CURE_CANCELLED`, not a new conversion or inherited-vampire event.

## Real feeding paths and evidence correlation

### Wild Vampirism vampire → MCA civilian

```text
wild Vampirism vampire
 -> ExtendedCreature.onBite
 -> IVampireMob.drinkBlood(... IDrinkBloodContext[MCA victim] ...)
 -> real BloodDrinkEvent
 -> MCA Vamp Compat NORMAL handler/provider infection decision
 -> Dark Folklore LOWEST observation of finalized positive amount
```

This preserves provider configuration, infection rules, cooldowns, and conversion semantics. Core does not call MCA infection services directly.

### Converted MCA vampire → provider-valid human target

Core may score an eligible candidate for bounded narrative diagnostics, but a human session continues only if provider-native AI independently selects the same target. Successful-feed evidence is not inferred from that session.

Instead, the exact adapter brackets a single `LivingIncomingDamageEvent`:

1. At `HIGHEST` with canceled events visible, capture only a direct MCA-vampire attacker, a provider-eligible target, and a ready provider attacker capability.
2. At `LOWEST` for that same event identity, require the same direct attacker UUID and target UUID.
3. Confirm that the provider attacker's bite capability changed from ready to cooldown.
4. Only then notify the narrative engine of a real native feed.

The attribution covers provider-valid MCA, player, and vanilla-human targets and does not rely on an MCA-only victim marker. Provider 2.0.12 may intentionally cancel or zero ordinary damage after a successful nonlethal bite; when the same direct pair consumes the provider cooldown, that post-success cancellation does not suppress evidence. Pre-canceled/redirected/failed attempts with no provider cooldown transition, different-source/target events, already-on-cooldown attackers, proximity-only cases, and expired narrative sessions do not manufacture evidence.

## 0.4 lifecycle observation

Core samples only loaded MCA entities and classifies exact provider snapshots into:

```text
HUMAN
INFECTED
VAMPIRE
CURING
```

Observed transitions include infection start, native-bite conversion, other conversion, inherited vampire, cure start/cancel/completion, infection cleared, and factual vampirism cleared.

Initial observation waits at least one server tick after entity join. If the provider capability is not ready, Core retries for a bounded 200 ticks; it does not permanently discard a late attachment after the first sample. Normal sampling is staggered, loaded-entity-only, and never force-loads chunks.

Important semantics:

- infection alone does not create public belief;
- a valid provider conversion-source UUID is durable provenance and can be recovered after load;
- a source equal to the converted entity UUID is rejected;
- a source UUID is not necessarily a biological parent or universally reliable sire;
- inherited vampirism does not fabricate a conversion source;
- cure/cleared transitions cancel only Core's scoped narrative predation session;
- Core never clears provider/MCA target or navigation state;
- cure does not erase historical witness belief or rumors;
- loaded observation caches and event-correlation state are cleared on server stop.

## Automated and manual validation boundary

The current local automated gate passes 119 JUnit tests and three NeoForge GameTests. Pure tests cover lifecycle transitions, sticky cure-cancellation metadata, bounded initial retry, status-aware MCA fact routing, component isolation, and exact attacker/target ready-to-cooldown attribution. The exact optional stack also reached dedicated GameTest startup with all named provider versions `ACTIVE` and `3/3` GameTests passing.

No manual client/in-world provider lifecycle pass is claimed. Before promotion, the intended pack still needs real wild-vampire → named-MCA feeding, provider infection, same-character conversion, native converted-MCA AI/targeting, exact native-bite evidence, cure cancellation/completion, inheritance, provenance inspection, Field Guide UI, and save/restart checks. See [0.4 testing](TESTING_0.4.0.md) and [0.4 release gate](RELEASE_0.4.0.md).
