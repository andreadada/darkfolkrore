# Source and API Audit

## Scope and method

Target: Minecraft 1.21.1, NeoForge 21.1.248, Java 21, Dark Folklore Core 0.1.0.

The installed JAR is authoritative. Auditing used JAR metadata, class/resource listings, public signatures, Atlas 0.2.0 registry/recipe/reference output, supplied configuration, and the implementation now in src/main. No method or event is assumed from a different mod version.

## Integration strategy

Dark Folklore follows this boundary:

1. Use standard NeoForge events and data first.
2. Use a documented/public external API when factual state cannot be represented by tags.
3. Isolate exact-version public implementation access when no stable API exists.
4. Return UNKNOWN and log on a query failure.
5. Never let optional external types enter unconditional common initialization.

There are no Mixins and no Access Transformers in this release.

Canonicalization enforcement uses three Core-owned, registered NeoForge extension types rather than external implementation hooks: CanonicalItemLootModifier, ConfigurableRemoveFeaturesBiomeModifier, and ConfigurableRemoveSpawnsBiomeModifier. Each reads FolkloreConfig.CANONICALIZATION before changing generated loot or biome settings.

## Vampirism 1.10.12

| Audit item | Finding |
| --- | --- |
| JAR | Vampirism-1.21-1.10.12.jar; SHA-256 C6DCCA1AF24DECA473A24470CCAB66053D3AA3324E453B4E1697090ED6D16BE2. |
| Public API inspected | de.teamlapen.vampirism.api.VampirismAPI, de.teamlapen.vampirism.api.entity.factions.IFactionRegistry, and IFaction. |
| API used | VampirismAPI.factionRegistry().getFaction(Entity), then IFaction.getID(). |
| IDs compared | vampirism:vampire and vampirism:hunter. |
| Events used | No Vampirism event is needed for state detection. Standard NeoForge entity/damage/death events drive Core systems. |
| Internal classes touched | None. Vampirism garlic/blood classes and resources were inspected only to choose canonical policy. |
| Failure containment | VampirismAdapter catches RuntimeException/LinkageError and returns UNKNOWN. The class is loaded by name only after exact-version confirmation. |
| Stability risk | Low for this exact public API; version changes still require audit. |

The faction registry is preferred over guessing from entity IDs or treating an absent optional player attachment as “not a vampire.” It handles faction-aware entities through Vampirism's owner-defined logic.

## Werewolves 2.0.3.3

| Audit item | Finding |
| --- | --- |
| JAR | Werewolves-1.21-2.0.3.3.jar; SHA-256 ECBCA2CD344E24AD48157834A8F321D1A7D2221C727FE8E61E4436D1219C6CFB. |
| Public surface inspected | Werewolves' faction registration through Vampirism, the werewolves:werewolf faction ID, and data/werewolves/tags/item/tools/silver.json. Wolfsbane/silver behavior classes and config were inspected to establish ownership, not called. |
| API used | The same public Vampirism faction-registry query, comparing IFaction.getID() with werewolves:werewolf. |
| Data used | Native silver tag, common material tags, Dark Folklore entity/item tags, spawn profiles, and optional biome/loot data. |
| Internal classes touched | None. No call is made to SilverEffect, WolfsbaneEffect, LevelWolfsbane, diffuser classes, or player implementation state. |
| Failure containment | Werewolf faction queries are enabled only when both Vampirism 1.10.12 and Werewolves 2.0.3.3 are exact. |
| Stability risk | Low-to-medium: faction API is public, but the external faction ID and tag IDs are versioned data. |

Werewolves owns native silver and wolfsbane mechanics. The Core's silver-versus-werewolf rule lists werewolves as a native provider namespace, so it does not stack the Core's damage multiplier onto native Werewolves entities.

## MCA Reborn 7.7.32+1.21.1

| Audit item | Finding |
| --- | --- |
| JAR | mca-neoforge-7.7.32+1.21.1.jar; SHA-256 874B5BD82D754033117EE6C1E7B5EBD142EC5DC0DF2881C9BB2F38A05AE7F4AB. |
| API used | No MCA implementation method is called by the Core. |
| Standard surface | MCA entities are scoped by their mca registry namespace and handled through ordinary NeoForge Entity, LivingEntity, interaction, join, death, and damage events. |
| Internal classes touched | None. Core source does not import net.conczin.mca classes. |
| Stability risk | Low for generic event handling. The exact MCA version remains important because the compatibility add-on below is built for it. |

Dark Folklore does not create a parallel MCA supernatural attachment or write MCA traits.

## MCA Reborn x Vampirism Compat 2.0.12

| Audit item | Finding |
| --- | --- |
| JAR | mca-vamp-compat-1.21.1-2.0.12.jar; SHA-256 BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF. |
| Stable API available | No separate stable API package for all required facts was found. Useful methods are public implementation methods. |
| Integration method | Exact-version reflection isolated in McaVampCompatAdapter. |
| Query methods | McaVampireStateService.isVampire(Entity), McaWerewolfStateService.isWerewolf(Entity), and McaHunterAlignmentService.isMcaHunterAligned(Entity). |
| Provenance methods | ModCapabilities.get(Entity), VampiricVillagerState.getSource(), and getWerewolfSourceUuid(). |
| Capability inspected | The add-on creates mca_vampirism_compat:vampiric_villager. Core does not hardcode the capability ID; it invokes ModCapabilities.get. |
| Writes performed | None. No ensure, markConverted, infection, cure, AI-goal, appearance, trait, or alignment setter is invoked. |
| Failure containment | initialize() resolves every expected class/method before the adapter is published. Missing signatures produce compatibility ERROR. Per-query reflection failures return UNKNOWN/empty and log a warning. |
| Stability risk | Medium-to-high outside 2.0.12, which is why the bridge is exact-gated. |

The inspected add-on already implements vampire/werewolf infection and conversion, cures, MCA trait synchronization, AI/combat behavior, faction inheritance, appearance, blood interactions, trades/social consequences, marriage/family behavior, and Vampirism village interactions. Core reuses that authority. It adds only cross-pack knowledge, witness/rumor state, investigation, contracts, stories, reputation, and a conversion-source lineage record.

The returned source UUID is provenance. The API does not justify treating it as an always-present or biologically meaningful sire.

## Enchanted 4.2.7

| Audit item | Finding |
| --- | --- |
| Surface inspected | Atlas/JAR registry entries and recipe/resource references for garlic, mandrake, wolfsbane, altars, poppets, seeds, and crop items. |
| Public API used | None required. |
| Integration used | Optional semantic tags, canonical definitions, and a config-aware Core global loot replacement for newly generated enchanted:garlic. |
| Internal classes touched | None. |
| Stability risk | Low for exact registry IDs; medium if future versions change crop loot or item semantics. |

Mandrake, altar, and poppet are deliberately not substituted. Enchanted wolfsbane gains semantic classification but is not injected into Werewolves' private aura state.

## Occultism 1.224.2

| Audit item | Finding |
| --- | --- |
| Surface inspected | Silver ore/material registry and worldgen resources, spirit/soul/ritual item IDs, and iesnium_golem entity semantics. |
| Public API used | None required. |
| Integration used | Optional tags; config-aware placed-feature removal and newly generated silver-loot routing; KEEP_DISTINCT golem policy. |
| Internal classes touched | None. |
| Stability risk | Low-to-medium, concentrated in exact placed-feature and registry IDs. |

## Malum 1.8.2

| Audit item | Finding |
| --- | --- |
| Surface inspected | Spirit items and malum:poppet registry/resource usage. |
| Public API used | None required. |
| Integration used | Optional SOUL traits and KEEP_DISTINCT poppet metadata. |
| Internal classes touched | None. KubeJS Eidolon does not mediate this integration. |
| Stability risk | Low for exact data IDs. |

## Eidolon: Repraised 0.5.0.2

| Audit item | Finding |
| --- | --- |
| Surface inspected | Silver material/worldgen resources, wraith entity/resources, holy/soul items, altar resources, recipes, and supplied configuration. |
| Public API used | None required. |
| Integration used | Tags, canonical metadata, config-aware placed-feature removal and newly generated silver-loot routing, Field Guide classification, and a standard spawn profile. |
| Internal classes touched | None. KJSEidolon is installed but unused by Core. |
| Stability risk | Low-to-medium for exact data IDs. |

## Feywild 5.5.5

| Audit item | Finding |
| --- | --- |
| Surface inspected | Fae entities/items and mandrake/mandrake-root registry, recipe, and worldgen references. |
| Public API used | None required. |
| Integration used | Optional FAE tags and KEEP_DISTINCT mandrake metadata. |
| Internal classes touched | None. |
| Stability risk | Low for exact IDs. |

## Field Guide 1.14.0

| Audit item | Finding |
| --- | --- |
| JAR | fieldguide-neoforge-1.21.1-1.14.0.jar; SHA-256 00B26B1351CB85B90ED86675C49BFC054A3141BEDAE22358D5A6AD4FE7CB0740. |
| Public API inspected | EntryUnlockData and EntryUnlockData.UnlockTrigger. |
| Public implementation inspected | FieldGuideProgressManager and PlayerFieldGuideProgress in com.evandev.fieldguide.server.progress. |
| Methods used | FieldGuideProgressManager.getInstance().getProgress(ServerPlayer), PlayerFieldGuideProgress.tryUnlock(..., KILL), and isUnlocked(ResourceLocation). |
| Events used | Standard LivingDeathEvent and PlayerTickEvent.Post. No suitable Field Guide unlock NeoForge event was found/used. |
| Data used | Field Guide category JSON, auto_populate tag strategies, language keys, and entity entry IDs of the form entity:<namespace>/<path>. |
| Internal classes touched | Public implementation classes outside the API package are used. No private fields, reflection, client classes, or Mixins. |
| Failure containment | FieldGuideAdapter is loaded by name only at exact 1.14.0; runtime calls catch RuntimeException. Polling is bounded to once per 100 player ticks and only unresolved canonical entity concepts. |
| Stability risk | Medium. The progress classes are public but not a declared stable API; exact pinning is mandatory. |

CompatibilityManager now labels this mechanism “exact 1.14.0 server progress bridge,” matching the implementation. There is no Core command injection into Field Guide and no Field Guide scan event dependency.

## Ancillary data integrations

| Mod | Version | Audit and use |
| --- | --- | --- |
| AlmostUnified | 1.21.1-1.4.2 | Supplied config favors Immersive Engineering, has recipe-viewer hiding on, loot_unification=false, and world_gen_unification=false. Core does not call its Java API; the Core's separate config-aware codecs supply the latter two enforcement layers. |
| Better Archeology | 1.21.1-1.3.8 | Four exact artifact IDs are optional members of darkfolklore:archaeological_lore. Standard ItemEntityPickupEvent.Post grants one-time lore. |
| Quest Giver | 1.5.1 | Detected and version-reported only; no classes, events, or data APIs are used. |
| KubeJS / Rhino / KJSEidolon | 2101.7.2-build.368 / 2101.2.7-build.85 / 1.3.1 | Installed scripts are untouched examples. Core does not call these APIs. See KUBEJS_AUDIT.md. |

## Dedicated-server review

- All gameplay bridges register on the common NeoForge event bus.
- No net.minecraft.client or external client GUI class is imported by common/server initialization.
- Field Guide access is limited to server progress classes.
- Optional adapters are not loaded when their exact provider is absent.
- Core biome/loot modifier codecs and their canonicalization checks live in common/server code and do not depend on a client.
- No compatibility layer sends client-authored facts to authoritative state.

## Re-audit triggers

Re-audit is mandatory if any pinned JAR changes, an external registry ID moves, a feature/tag file changes, a public signature changes, a provider starts applying a new weakness multiplier, or Field Guide changes its entry/progress format. An apparently compatible semantic version is not sufficient evidence.
