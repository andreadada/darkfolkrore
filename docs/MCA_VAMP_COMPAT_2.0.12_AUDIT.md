# MCA Reborn x Vampirism Compat 2.0.12 exact-provider audit

This audit defines the code-level boundary used by Dark Folklore Core 0.4.0. It is intentionally version-specific.

## Audited binary

The development audit inspected a user-supplied `mca_vamp_compat` 2.0.12 JAR for Minecraft 1.21.1 / NeoForge.

SHA-256:

`BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF`

The binary is **not** committed, bundled, shaded or redistributed by Dark Folklore Core.

## Ownership rule

MCA Vamp Compat remains the factual owner of:

- MCA vampire infection;
- conversion;
- cure progress/completion;
- inherited vampirism;
- MCA vampire native combat/infection-bite AI;
- provider capability persistence;
- appearance/state normalization.

Dark Folklore observes those facts and may orchestrate social/investigation consequences. It does not expose a generic `forceInfect`, `forceConvert`, `forceCure` or `applyInheritance` API.

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

Dark Folklore does not call the mutation methods to manufacture infection. The 0.3.1 predation bridge uses the provider's target eligibility/native AI and lets the provider perform the actual human bite.

### `McaVampireInfectionService`

Public methods include configured infection eligibility, duration, conversion-chance and source-type helpers. These remain provider-owned and are not used as a second infection authority.

### `McaVampireStateService`

Audited methods include MCA/vampire factual queries plus provider state normalization/conversion helpers. Dark Folklore uses factual reads only through the exact adapter.

### `VampiricVillagerState`

Audited read state used by 0.4:

```text
isInfected()
isConverted()
isCuringVampire()
isFactionInheritanceProcessed()
isBiteWasConversionCause()
areAiGoalsAdded()
getSource()
```

The provider state also owns:

```text
startInfection(...)
finishConversion()
markInheritedVampire()
clearVampirismAfterCure()
canBite(...)
markBite(...)
```

Core does not call the infection/conversion/cure mutations. The 0.3.1 animal-feeding adapter uses only the audited bite-cooldown semantics required to prevent a social feeding director from bypassing the provider's own cadence.

### `McaVampireAi`

`registerGoalsIfNeeded(LivingEntity)` is audited as an idempotent native-AI extension point. 0.4 may call it only after the provider already reports a factual converted MCA vampire. Core never installs a replacement infection goal.

### `McaVampireInheritanceHandler`

Audited provider methods include `applyInheritance`, `applyDecision`, `decide`, and parent/chance helpers. Provider `VampiricVillagerEvents.onBabySpawn` invokes inheritance. Dark Folklore therefore only observes the child after the provider event completes; it never calls `applyInheritance` itself.

Inherited provider state has no conversion-source UUID. Core retains the two-parent birth context in runtime diagnostics rather than fabricating a single conversion source in the existing lineage table.

### `McaVampireCureService`

Audited provider methods include interaction handling, cure-state queries, cure tick processing, cancellation, village-capture cure and completion. Core observes `isCuringVampire`/final factual state and never advances cure stages itself.

## Native BloodDrinkEvent infection chain

The exact provider `VampiricVillagerEvents` subscribes to both player and entity `BloodDrinkEvent`. Its shared blood-drink path obtains `IDrinkBloodContext.getEntity()` and, for an MCA victim, routes the real source/target to MCA vampire bite/infection handling.

This is why the 0.3.1 wild-vampire bridge intentionally performs a real Vampirism blood drain instead of calling MCA infection services directly:

```text
wild Vampirism vampire
 -> Vampirism ExtendedCreature.onBite
 -> IVampireMob.drinkBlood(..., IDrinkBloodContext[MCA victim])
 -> real BloodDrinkEvent
 -> MCA Vamp Compat event subscriber
 -> provider-configured infection decision
```

That preserves provider configuration, cooldowns and conversion semantics.

## 0.4 lifecycle observation

Dark Folklore 0.4 samples only loaded MCA entities and classifies provider snapshots into:

```text
HUMAN
INFECTED
VAMPIRE
CURING
```

Observed transitions include infection start, native-bite conversion, other conversion, inherited vampire, cure start/cancel/completion, infection cleared and factual vampirism cleared.

Important semantics:

- infection alone does not create public belief;
- conversion source is recorded in Core lineage only when the provider actually supplies a source UUID;
- inherited vampirism does not fabricate a conversion source;
- cure does not erase historical witness beliefs/rumors;
- a factual converted MCA vampire can be asked to install provider-native AI idempotently;
- loaded-entity sampling is staggered and no chunks are force-loaded.

## Runtime validation boundary

Normal GitHub CI does not install this third-party JAR, so unit tests validate the state/transition policy without importing provider classes. Exact runtime reflection is fail-closed and still requires an intended-pack manual pass.

Required manual checks include real wild-vampire → named MCA feeding, provider infection, same-character conversion, native MCA-vampire AI, cure, inheritance and save/restart.
