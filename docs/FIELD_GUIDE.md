# Field Guide 1.14.0 Integration

Dark Folklore 0.2.0 supplies a curated bestiary to Field Guide. Field Guide remains the owner of its UI, scanning, binary unlocks, per-player progress, discovery timestamps, recent-discovery ordering, photographs and editable notes. Dark Folklore owns its richer `UNKNOWN` / `DISCOVERED` / `OBSERVED` / `STUDIED` / `MASTERED` lore model.

## Repair made in 0.2.0

The 0.1.0 category translation keys were reversed. It supplied keys such as `category.fieldguide.darkfolklore.vampires`, but Field Guide 1.14.0 constructs `category.darkfolklore.fieldguide.vampires`. That is why the client displayed the raw key.

The 0.1.0 categories also relied on broad entity-tag auto-population. This produced no useful curated content in the tested client and could generate unrelated pages as tags grew. The 0.2.0 resources use explicit `entry` objects for audited registry IDs. Only categories with at least one supported entry are shipped.

## Exact JAR audit

| Property | Audited value |
| --- | --- |
| File | `mods/fieldguide-neoforge-1.21.1-1.14.0.jar` |
| Size | 1,200,470 bytes |
| SHA-256 | `00B26B1351CB85B90ED86675C49BFC054A3141BEDAE22358D5A6AD4FE7CB0740` |
| Mod ID / version | `fieldguide` / `1.14.0` |
| Minecraft range | `[1.21.1, 1.21.2)` |
| NeoForge minimum | `21.1.226` |

The exact JAR's working content and implementation were inspected, including `ServerFieldGuideManager`, `EntryResolutionHelper`, `PlayerFieldGuideProgress`, `FieldGuideProgressManager`, client `ProgressManager`, `ClientCategoryManager` and `ClientTextManager`.

### Working category and entry format

Field Guide loads category stacks from:

```text
data/<namespace>/fieldguide/categories/<path>.json
```

The resulting category ID is `<namespace>:<path>`. Supported top-level fields include `sort_index`, `icon`, `group_by`, `replace`, `hidden`, `target_category` and `contents`. Entries are embedded in `contents`; there is no separate basic-entry resource directory in 1.14.0.

The built-in working categories use `auto_populate` strategies (`animals`, `monsters`, `plants`, `trees`, `tag:fieldguide:bosses` and `cobblemon`). Dark Folklore instead uses the loader's explicit form:

```json
{
  "type": "entry",
  "id": "entity:cnc/wendigo",
  "canonical_concept": "darkfolklore:wendigo",
  "unlock": {
    "triggers": ["scan", "kill"]
  }
}
```

`canonical_concept` is Dark Folklore validation metadata. Field Guide's tolerant JSON loader ignores that extra property and reads the standard `type`, `id` and `unlock` properties. An entity entry ID is `entity:<entity namespace>/<entity path>`. During reload, Field Guide resolves it through the live entity registry and omits it if the provider entity is absent or blacklisted.

### Translation keys

The exact category implementation returns:

```text
category.<category namespace>.fieldguide.<category path>
```

For `entity:vampirism/vampire`, the preferred static overrides read by `ClientTextManager` are:

```text
fieldguide.name.entity.vampirism.vampire
fieldguide.entity.vampirism.vampire.description
```

Dark Folklore supplies every required category title, entry name and entry description in both `assets/darkfolklore/lang/en_us.json` and `assets/darkfolklore/lang/it_it.json`.

## Curated 0.2.0 content

| Category | Entry ID | Canonical concept |
| --- | --- | --- |
| Vampires | `entity:vampirism/vampire` | `darkfolklore:vampire` |
| Werewolves | `entity:werewolves/human_werewolf` | `darkfolklore:werewolf` |
| Spirits | `entity:vampirism/ghost` | `darkfolklore:ghost` |
| Spirits | `entity:eidolon_repraised/wraith` | `darkfolklore:wraith` |
| Spirits | `entity:graveyard/wraith` | `darkfolklore:wraith` |
| Cryptids | `entity:cnc/wendigo` | `darkfolklore:wendigo` |
| Cryptids | `entity:cnc/chupacabra` | `darkfolklore:chupacabra` |
| Mythical Beasts | `entity:mythsandlegends/imp` | `darkfolklore:imp` |
| Constructs | `entity:occultism/iesnium_golem` | `darkfolklore:golem` |

This is six visible categories and nine explicit entries. `Undead`, `Fae`, `Witches & Occultists`, `Sea Horrors` and `Unknown` are not shipped in 0.2.0 because the current curated set does not justify separate non-empty pages. The broad `Unknown` overlap from 0.1.0 is gone.

All nine raw entity IDs were verified in the installed registry audit and resolve through the named canonical definition. Category icons reference five assets verified inside the exact Field Guide JAR: `wither.png`, `wolf.png`, `ghast.png`, `spider.png` and `ore.png`.

## Binary unlock and lore synchronization

Field Guide 1.14.0 has a binary unlocked set. It does not expose five presentational lore tiers, so Dark Folklore does not simulate them in the Field Guide UI.

The exact server flow is:

1. A curated entry declares both `SCAN` and `KILL` triggers.
2. Native Field Guide scanning calls its normal unlock path.
3. When a player kills a canonical implementation, `FieldGuideAdapter` resolves the implementation to the canonical entity page when one exists and calls `tryUnlock(..., KILL)`.
4. `tryUnlock` first verifies that the entry exists, prerequisites are satisfied and `KILL` is allowed.
5. Field Guide records the binary unlock, real-world discovery time and in-game discovery time and sends its normal progress update.
6. An unlocked curated implementation grants 10 Dark Folklore points only when that concept was previously unknown, reaching `DISCOVERED` without fabricating later tiers.
7. Conversely, reaching `OBSERVED` (25 points) unlocks an existing canonical Field Guide page. The adapter checks `isValidEntry` and `canUnlock` first and uses `grantXp=false`, because this was a lore threshold rather than a scan.

For `KEEP_DISTINCT` concepts without a single canonical entity ID, Dark Folklore does not guess which implementation the player studied. Those pages unlock by scanning or killing their exact entity.

Polling runs server-side once every 100 player ticks over the small canonical-definition set. A Field Guide unlock is monotonic with Dark Folklore lore: revoking a Field Guide page does not erase knowledge already learned elsewhere.

## Recent Discoveries

There is no separate recent-discovery datapack format. `PlayerFieldGuideProgress.unlock` stores `System.currentTimeMillis()` and the server level's `dayTime` for the entry. The progress packet synchronizes these maps to the client. `ClientCategoryManager.getRecentEntries` filters unlocked entries and sorts them by discovery time descending.

Therefore, both native scans/kills and the 25-point lore threshold use Field Guide's real unlock method and automatically participate in Recent Discoveries. Calling the progress API for a nonexistent entry would do nothing; this is why static, resolvable category content is mandatory.

The similarly named `recentlyScannedEntities` map is a separate ten-second server window used for scan-and-kill advancement behavior. It is not the Recent Discoveries list.

Field Guide persists progress per player at `<world>/fieldguide_progress/<uuid>.json`. Its files contain unlocked/seen sets, discovery time maps, custom text, photographs, selected variants and journal data. Dark Folklore does not write those files directly.

## Automated resource validation

`FieldGuideResourceValidatorTest` is a pure JUnit validator. It reads shipped source resources only; it has no game bootstrap, local Atlas path or runtime Atlas dependency.

The validator checks:

- exact category and entity-entry syntax;
- only audited Field Guide icon paths;
- `SCAN` plus `KILL` binary triggers;
- missing category translations in `en_us` and `it_it`;
- missing entry name or description translations in either locale;
- orphan Field Guide localization entries;
- invalid/unknown target categories;
- duplicate entry IDs;
- missing canonical concepts;
- entity IDs not belonging to their declared canonical concept;
- empty visible categories;
- invalid or broad auto-populated content.

The shipped-content assertion is:

```text
categories=6
entries=9
missingCategoryTranslations=0
missingEntryTranslations=0
orphanEntries=0
invalidCategories=0
duplicateEntryIds=0
missingCanonicalConcepts=0
unresolvedMappings=0
emptyCategories=0
invalidEntries=0
```

A second synthetic test introduces every required failure class and verifies the validator detects each one.

## Version boundary and failure behavior

`FieldGuideAdapter` is constructed only by the existing exact `1.14.0` compatibility gate. It references server/common Field Guide classes only:

- `EntryUnlockData.UnlockTrigger`;
- `FieldGuideProgressManager`;
- `PlayerFieldGuideProgress`.

No client GUI, renderer, toast or input class is referenced from common initialization. If the exact bridge nevertheless encounters a runtime or linkage failure, that adapter instance disables itself and emits one concise warning instead of throwing on every kill or polling pass. With Field Guide absent or at an untested version, the compatibility manager does not load this implementation class.

The integration does not copy or shade Field Guide. Its icons are external resource references and the dependency remains optional.

## Pack configuration observed

The audited pack keeps Field Guide scanning enabled, spyglass scanning enabled at 64 blocks, and `grantXpOnScan=true`. Naked-eye scanning, Field Guide item scanning, the physical Field Guide item and the lens item are disabled. Pause and inventory UI buttons are enabled. Dark Folklore does not overwrite these choices.

## Validation boundary

The pure resource tests prove schema, mappings and localization completeness. Java compilation proves the exact 1.14.0 server signatures. Dedicated-server and title-screen client startup prove common-side safety, resource reload, and exact bridge activation, but they cannot prove category rendering, translated labels, entry models, toast behavior or Recent Discoveries layout. Those require opening the UI in a joined world.

The final Field Guide workstream smoke used Java 21, NeoForge 21.1.248 and the existing 23-external-JAR curated server staging. Field Guide reported exact version `1.14.0` and status `ACTIVE`; Dark Folklore atomically loaded 16 canonical concepts with zero invalid definitions; no `Failed to load category`, `Unknown unlock trigger`, Field Guide bridge-disable, or Dark Folklore/Field Guide exception appeared. The server reached `Done (1.567s)!`, then the harness requested a normal halt and the log recorded `Stopping server`, `Saving players`, `Saving worlds`, all dimensions saved, and `BUILD SUCCESSFUL`. The harness's dynamic-agent warning is a serviceability warning from requesting that graceful halt, not shipped mod behavior.

The wider staging did emit one NeoForge `RuntimeDistCleaner` error saying that `net.minecraft.client.gui.screens.Screen` was requested on `DEDICATED_SERVER`. That line had no stack trace or owning mod, and loading continued. Dark Folklore's Field Guide adapter source and packaged class have no client references, so this did not identify the adapter as the requester; equally, without an owner it must not be presented as a completely error-free pack smoke. Other non-blocking staging warnings were missing development refmaps, optional MCA/Easy Villagers classes, external mob-category mismatches, legacy Forge tags and a CNC loot-item damage warning.

The reduced server staging did not contain Graveyard, so Field Guide correctly omitted `entity:graveyard/wraith` during that runtime. The Spirits category still contained its Vampirism and Eidolon entries. The complete-pack registry audit and pure resource validator cover the Graveyard mapping, but only the real complete client can prove its rendering.

A final graphical client startup later reached the NeoForge title state with Field Guide 1.14.0 and the wolfsbane bridge active, then exited cleanly. No world was loaded and no Field Guide page was opened, so the release-level report must distinguish successful client startup from the manual UI checks below.

## Manual client test matrix

1. Start the exact pack with Field Guide 1.14.0 and Dark Folklore 0.2.0.
2. Confirm the native Field Guide categories still populate normally.
3. Open every Dark Folklore tab and confirm the six translated titles render in English.
4. Switch to Italian and confirm the same category titles, nine entry names and nine descriptions render without raw keys.
5. Confirm each category contains exactly the entries in the curated table and no tab is empty.
6. Scan Vampire, Werewolf, Wendigo, Chupacabra and Imp; confirm their pages unlock once and appear in Recent Discoveries.
7. Kill a locked curated creature; confirm the declared `KILL` path unlocks it and records a discovery date.
8. Kill a noncanonical implementation of a concept with a canonical page; confirm the canonical page is targeted rather than a nonexistent duplicate page.
9. Reach 25 Dark Folklore points for a still-locked canonical creature; wait up to 100 ticks and confirm its page unlocks without scan XP.
10. Unlock a Field Guide page for an unknown concept; wait up to 100 ticks and confirm Dark Folklore reaches `DISCOVERED`, not a later tier.
11. Restart the world and confirm unlocks, discovery order and custom notes persist.
12. Remove Field Guide and start a dedicated server; confirm compatibility reports Field Guide absent/inactive and no Field Guide class-loading error occurs.

## Remaining limitations

- Field Guide remains binary; later Dark Folklore lore tiers are not rendered in its pages.
- The nine entries require their provider mods. Field Guide correctly omits an entry whose registry target is absent, so a deliberately reduced pack can have fewer entries than the complete-pack counts.
- `KEEP_DISTINCT` concepts do not receive an automatic lore-threshold page because there is no honest single page to choose.
- Custom Field Guide names, descriptions and photographs are not copied into Dark Folklore knowledge.
- Final UI confirmation remains a manual in-world client test; dedicated-server success and title-screen client startup are not evidence of correct Field Guide rendering.
