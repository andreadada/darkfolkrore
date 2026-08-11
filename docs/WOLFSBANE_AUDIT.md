# Wolfsbane audit — Dark Folklore Core 0.2.0

This audit is authoritative for the locally installed artifacts:

| Mod | Installed artifact | Version | SHA-256 |
| --- | --- | --- | --- |
| Werewolves | `Werewolves-1.21-2.0.3.3.jar` | 2.0.3.3 | `ECBCA2CD344E24AD48157834A8F321D1A7D2221C727FE8E61E4436D1219C6CFB` |
| Enchanted | `enchanted-neoforge-1.21.1-4.2.7.jar` | 4.2.7 | `205A3E1EDB7E53E9BBF3B8AB965AC5EB3840BC43760E1B003200E4735EBF1BB0` |

The audit used the exact JAR resources, constant pools, Java 21 `javap` output, and a local
decompilation of every class whose bytecode contains `wolfsbane`, `WOLFSBANE`,
`LevelWolfsbane`, or a relevant registry-field reference. All installed JAR entries were also
scanned for the case-insensitive string `wolfsbane`; this found 97 Werewolves entries, 45 Enchanted
entries (including five generator-cache hashes), and two Atlas scanner-vocabulary entries. The
addon result is recorded below.
Decompiled source is evidence for bytecode interpretation only and is not shipped.

### Scan coverage ledger

The Werewolves constant-pool/source hits were fully classified as follows:

- gameplay and persistence: `WolfsbaneBlock`, `WolfsbaneDiffuserBlock` and its `Type`,
  `WolfsbaneDiffuserBlockEntity`, `WolfsbaneEffect`, `WerewolfPlayer`, and `LevelWolfsbane`;
- registration/configuration: `WerewolvesAttachments.Keys`, `ModAttachments`, `ModBlocks`,
  `ModItems`, `ModEffects`, `ModTiles`, `BalanceConfig.Blocks`, `WerewolvesBiomeFeatures`, and
  `ModTags.Biomes.HasGen`;
- client/UI: `ModBlocksRenderer`, `WolfsbaneDiffuserBESR` and its accessor, the diffuser screen,
  `Proxy`, and `ClientProxy`;
- generated-data and display: the creative-tab generator, advancement, blockstate, item-model,
  loot-table, tag, and recipe providers;
- shipped resources: the flower/potted-flower and three diffuser blockstates/models, their item and
  block textures, the Finder texture, the effect icon, item models, translations, flower/pot/tool
  tags, six recipe and six recipe-advancement files, the `natural_succession` advancement, five
  related block loot tables, and the configured feature, placed feature, biome modifier, and biome
  tag.

The Enchanted hits were likewise classified: `EBlocks`, `EItems`, and `ECreativeTab`; its
blockstate, item-model, compost-map, crop-loot, block-tag, altar-power, mutagen, and Modopedia data
providers; and the shipped crop models/blockstate/textures, flower/seed models/textures and
translations, crop loot, grass-seed extension, crop tag, compost map, nine sapling-mutation entries,
and Modopedia page.
Generic crop/mutation engine classes do not contain a Wolfsbane-specific branch. The sections
below record the behavior at every one of these surfaces; model and translation hits are
identity/display-only. Enchanted's five `.cache/*` hits are shipped data-generator hash metadata,
not loadable game data or code.

## Decision

| Property | Result |
| --- | --- |
| Concept | `darkfolklore:wolfsbane` |
| Policy | `FULL_CANONICALIZATION` |
| Canonical item | `enchanted:wolfsbane_flower` |
| Canonical crop block | `enchanted:wolfsbane` |
| Canonical seed | `enchanted:wolfsbane_seeds` |
| Legacy item/block | `werewolves:wolfsbane` |
| Semantic item tag | `#darkfolklore:wolfsbane` |
| Semantic plant tag | `#darkfolklore:wolfsbane_plants` |
| Seed tag | `#darkfolklore:wolfsbane_seeds` |
| Mixin required | No |

The legacy registry entries remain registered. No inventory, container, block, item entity, or
serialized stack is migrated automatically.

## Exact Werewolves 2.0.3.3 integration points

### Registry and block behavior

`de.teamlapen.werewolves.core.ModBlocks` registers:

- `werewolves:wolfsbane` with `registerWithItem("wolfsbane", WolfsbaneBlock::new)`;
- `werewolves:potted_wolfsbane`, whose flower-pot plant is the same native block;
- `werewolves:wolfsbane_diffuser_normal`;
- `werewolves:wolfsbane_diffuser_long`;
- `werewolves:wolfsbane_diffuser_improved`.

`de.teamlapen.werewolves.blocks.WolfsbaneBlock#entityInside` is the only native flower-contact
behavior. On the logical server, outside Peaceful difficulty, it calls
`Helper.isWerewolf(entity)` and applies `WolfsbaneEffect.createWolfsbaneEffect(entity, 45, 1)`.
There is no held-item toxicity, food behavior, attack handler, mob AI goal, or inventory scan for
the herb.

`de.teamlapen.werewolves.effects.WolfsbaneEffect` is a native harmful
`WerewolfWeakeningEffect`. It applies the Werewolves-owned movement-speed modifier identified by
the `werewolves:wolfsbane` effect ID. The weakening superclass filters its attribute modifiers to
entities for which Werewolves' `Helper.isWerewolf` is true. Dark Folklore does not reproduce that
formula; it invokes the exact native effect factory.

Compatibility method: an exact-version, server-authoritative entity-tick bridge checks the few
blocks intersecting a living entity's bounding box. Contact with exactly
`enchanted:wolfsbane` invokes the same native effect factory with the same duration and amplifier.
The legacy block continues to run its own native callback and is deliberately excluded from the
bridge, preventing duplicate application.

### Diffuser insertion, aura, and persistence

`de.teamlapen.werewolves.blocks.WolfsbaneDiffuserBlock#useItemOn` performs an unavoidable exact
item comparison:

```text
ModBlocks.WOLFSBANE.asItem() == stack.getItem()
```

When empty, the diffuser calls its public block entity's `onFueled()`, consumes one item for a
non-creative player, and sends the same success message used by Vampirism's garlic diffuser. When
already fueled it sends the native already-fueled message. There is no item capability, menu slot,
recipe lookup, or tag hook at this call site.

`WolfsbaneDiffuserBlockEntity` stores `type`, `fueled`, `boot_timer`, and `max_boot_timer` in NBT.
It does not serialize a wolfsbane ItemStack, so accepting the Enchanted flower does not introduce a
foreign stack into block-entity persistence. The locally configured native durations and chunk
radii are:

| Diffuser | Duration | Chunk radius | Native amplifier |
| --- | ---: | ---: | ---: |
| Normal | 600 seconds | 1 | 1 |
| Long | 1200 seconds | 0 | 1 |
| Improved | 600 seconds | 2 | 2 |

After its native startup delay, the block entity registers affected chunks with
`LevelWolfsbane`, held in the non-serialized level attachment whose exact native registry path is
`werewolves:wolfbane_handler` (the missing `s` is native). Loaded block entities rebuild this
ephemeral chunk map. `WerewolfPlayer` refreshes the native aura check every 40 ticks and applies a
50-tick `WolfsbaneEffect` using the registered amplifier. This downstream aura is independent of
the fuel item's identity; once `onFueled()` runs, all native Werewolves behavior is retained.

Compatibility method: `PlayerInteractEvent.RightClickBlock` intercepts only the canonical
`enchanted:wolfsbane_flower` on the three audited diffuser IDs. It performs the same public
`getFuelTime()` / `onFueled()` sequence, consumption rule, messages, sided result, and event
cancellation. Legacy fuel is not intercepted and follows Werewolves' original code. No Mixin is
used.

### Wolfsbane Finder

The name is misleading in 2.0.3.3. `ModItems.WOLFSBANE_FINDER` is a plain rare `Item`; it has no
`use`, compass, locate, target, NBT, or plant-search implementation. The only runtime behavior is
client-side in `WolfsbaneDiffuserBESR`: while the finder is held, in-range diffuser block entities
render a rotating marker made from the native wolfsbane item model. It locates diffusers, not
`werewolves:wolfsbane` plants.

Compatibility method: the original held-item diffuser marker is untouched. Dark Folklore adds a
server-authoritative right-click-in-air extension that searches loaded chunks only, within 32
horizontal and 8 vertical blocks, for exactly `enchanted:wolfsbane`. It reports the nearest crop's
coordinates and distance and applies a 60-tick item cooldown. The bounded scan does not generate or
load chunks. This makes the Finder useful for planted Enchanted crops without claiming that the
native implementation ever found plants.

### Recipes

The exact JAR has four recipe inputs that directly require `werewolves:wolfsbane`:

| Recipe ID | Serializer | Exact native use | 0.2 compatibility |
| --- | --- | --- | --- |
| `werewolves:purple_dye` | `minecraft:crafting_shapeless` | one native herb | ingredient changed to `#darkfolklore:wolfsbane` |
| `werewolves:wolfsbane_finder` | `minecraft:crafting_shaped` | center `Y` is native herb | `Y` changed to `#darkfolklore:wolfsbane` |
| `werewolves:wolfsbane_diffuser_core` | `vampirism:alchemical_cauldron` | `fluid` is native herb | `fluid` changed to `#darkfolklore:wolfsbane` |
| `werewolves:wolfsbane_diffuser_core_improved` | `vampirism:alchemical_cauldron` | `fluid` is native herb | `fluid` changed to `#darkfolklore:wolfsbane` |

Vampirism 1.10.12's exact `AlchemicalCauldronRecipe.Serializer` decodes `fluid` as
`Either<Ingredient, FluidStack>` using `Ingredient.CODEC_NONEMPTY`; a normal item tag is therefore
a verified input format rather than a guessed extension.

The shaped recipes named `wolfsbane_diffuser_normal` and `wolfsbane_diffuser_improved` consume
the already-crafted diffuser core items, not raw wolfsbane, and need no herb substitution. Their
somewhat counterintuitive result IDs are native Werewolves behavior and remain unchanged.

Recipe overrides are conditioned on Werewolves being loaded and the exact legacy/output item IDs
used by each override still existing. The semantic tag retains the legacy flower as an accepted
input, so a Werewolves-only installation does not lose its recipes and old stacks remain useful.
Enchanted seeds are intentionally not accepted as harvested herb.

### Loot and acquisition

Werewolves ships only two loot outputs for raw native wolfsbane:

- `werewolves:blocks/wolfsbane` returns one `werewolves:wolfsbane`;
- `werewolves:blocks/potted_wolfsbane` returns a flower pot and one
  `werewolves:wolfsbane`.

No chest, structure, entity, injection table, quest reward, or villager trade in the exact JAR
produces or consumes wolfsbane. `ModVillage#getWerewolfTrades` was inspected and contains liver,
cracked bone, and werewolf tooth trades only.

Compatibility method: the existing global loot modifier maps newly generated
`werewolves:wolfsbane` stacks to `enchanted:wolfsbane_flower`, preserving count and data-component
patches. If the target item is absent, the modifier skips the replacement. Existing inventory
stacks and blocks are never touched; breaking a legacy block is a new loot event and therefore
returns the canonical flower in the target pack.

### World generation

The exact Werewolves path is:

1. configured feature `werewolves:wolfsbane`, a flower patch whose simple state provider places
   `werewolves:wolfsbane` with 96 tries, X/Z spread 7, and Y spread 3;
2. placed feature `werewolves:wolfsbane`, rarity filter one attempt per 20, motion-blocking
   heightmap, and in-square placement;
3. biome modifier `werewolves:gen/wolfsbane`, adding the placed feature at
   `vegetal_decoration` to `#werewolves:has_gen/wolfsbane`;
4. that biome tag contains `#minecraft:is_forest`.

Compatibility method: the placed feature is in
`#darkfolklore:noncanonical_wolfsbane` and the config-aware Dark Folklore removal modifier removes
it from new Overworld biome generation during the REMOVE phase. The modifier resource requires
both mods, the legacy item, and the canonical flower and seed IDs. It does not delete blocks, edit
chunks, unregister features, or modify the Werewolves registry. With Enchanted or its verified seed
ecology missing, the removal resource is not enabled and native worldgen remains.

### Advancements, tags, UI, and serialization

- `werewolves:natural_succession` uses `werewolves:wolfsbane` only as its display icon. Its actual
  criterion is killing `werewolves:alpha_werewolf`; wolfsbane is not progression data. The icon is
  retained so the advancement remains valid when Enchanted is absent.
- The generated recipe advancement for `werewolves:purple_dye` has an exact native-item inventory
  criterion. Dark Folklore overrides only that criterion with `#darkfolklore:wolfsbane`.
- The finder recipe advancement unlocks from redstone and silver and has no wolfsbane criterion.
- Both alchemical core advancements unlock by recipe ID and have no item criterion.
- Werewolves directly adds the legacy item to `#minecraft:small_flowers`; loaded tag closure also
  makes it a `#minecraft:flowers`, `#minecraft:bee_food`, and `#c:animal_foods` item. Dark Folklore
  appends the canonical flower to `#minecraft:small_flowers`, preserving those transitive flower
  and animal/bee consumers without removing the legacy member. The legacy plant's flower-pot
  registration is retained.
- Werewolves shows the legacy block in its creative tab, and the diffuser renderer uses the legacy
  item model. Dark Folklore does not remove a creative-tab entry or patch the renderer; doing so is
  cosmetic and is not worth an optional client-only dependency or Mixin.
- The item and block registry IDs, models, translations, loot tables, flower pot, and existing
  serialized stacks remain valid. There is no automatic inventory/container migration command in
  0.2.0 because broad container discovery would be materially riskier than retaining a harmless
  legacy item.

## Exact Enchanted 4.2.7 model

The installed JAR verifies all three IDs:

- `EBlocks.WOLFSBANE` registers block `enchanted:wolfsbane` as `CropBlockAgeFive`;
- `EItems.WOLFSBANE_FLOWER` registers ordinary item `enchanted:wolfsbane_flower`;
- `EItems.WOLFSBANE_SEEDS` registers `enchanted:wolfsbane_seeds` as the named block item for the
  crop.

At crop age 4, `data/enchanted/loot_table/blocks/wolfsbane.json` yields the flower plus a
fortune-aware seed pool. Before maturity it yields seeds. The flower and seed have compost chances
0.65 and 0.3 respectively. The crop is in `#enchanted:crops`. Enchanted's altar block-power data
assigns the crop block power 8 with limit 20. Its Modopedia page assigns both the flower and seed.

The crop block also remains an Enchanted mutagen. Nine entries (rowan, hawthorn, and alder outcomes
for oak, birch, and acacia saplings) list `enchanted:wolfsbane` among four accepted mutagen blocks.
Those entries consume the canonical crop-block identity rather than either flower item, so no
compatibility rewrite is needed and suppressing Werewolves' unrelated wild flower does not affect
them.

Enchanted does **not** ship a wolfsbane configured feature, placed feature, or biome modifier.
Its acquisition path extends `minecraft:blocks/short_grass`: with a 10% pool chance it selects
among six Enchanted seed/garlic entries, including `enchanted:wolfsbane_seeds`. The installed
`enchanted-server.toml` sets `hoe_seeds = true`, so this extension runs only when short grass is
broken with a hoe. Players then farm the seed on farmland. Consequently, “Enchanted wolfsbane
generates naturally as a crop block” is not a truthful 4.2.7 test expectation. The canonical
ecology is natural seed acquisition followed by farming.

Enchanted 4.2.7 contains no Werewolves check and no anti-werewolf behavior of its own. The native
effect equivalence is supplied by the exact-version bridge described above.

## Addon audit

Every installed JAR entry was scanned as decompressed bytes, so both literal resource IDs and Java
constant-pool field/class names were covered. Only Werewolves, Enchanted, and Dark Folklore Atlas
contained `wolfsbane` in code/resources. In particular:

- `mca-vamp-compat-1.21.1-2.0.12.jar` declares Werewolves compatibility but has no wolfsbane
  resource, class name, field reference, effect reference, or registry-ID string;
- `vampiricageing-1.21-1.4.21.jar` declares Werewolves compatibility but has no wolfsbane reference;
- `vampirism_integrations-1.21.1-1.10.2.jar` has no wolfsbane reference;
- MCA, MCA Capitals, Vampires' Delight, Vampirism Rings, MCA Sirben Enhanced, and the remaining
  installed addons have no wolfsbane reference.

The two Atlas JAR hits are scanner vocabulary (`AtlasWriter` and `DuplicateDetector`), not runtime
gameplay dependencies. Dark Folklore Core has no runtime dependency on Atlas and contains no local
Atlas path.

## Production hardening

The implementation-bound event bridge is active only when all of the following are true:

1. Werewolves is exactly `2.0.3.3`;
2. Enchanted is exactly `4.2.7`;
3. the canonical flower, canonical seed, canonical crop, legacy flower, Finder, and all three
   diffuser block IDs exist, as do the legacy block, native `werewolves:wolfsbane` effect, and
   `werewolves:wolfsbane_diffuser` block-entity type;
4. the isolated bridge class constructs successfully.

The optional bridge class is selected with one cached `Class.forName`/constructor resolution during
common setup. `DarkFolkloreCore` explicitly wires `WolfsbaneIntegration::onCommonSetup` to the mod
bus; the optional implementation class is not an automatically discovered subscriber. The exact
item and block registry objects are also resolved once when that isolated class loads; gameplay
uses direct identity checks. No reflective or registry lookup occurs during gameplay. A missing
mod, version mismatch, missing ID, linkage failure, constructor failure, or event-registration
runtime failure leaves all event flags false and loads no active Werewolves bridge. A failed
registration attempts to unregister the cached listener before reporting the error. Logging occurs
once at setup; no per-tick or per-interaction errors are logged.

`WolfsbaneIntegration.snapshot()` exposes the actual versions, state, detail, and separately
validated diffuser/contact/Finder flags for command diagnostics. Data-only claims such as recipe
or worldgen activation are deliberately not represented by those runtime flags; diagnostics must
validate them against the loaded recipe/tag/biome data before printing `ACTIVE`.

## Automated validation

`WolfsbaneSemanticsTest` verifies canonical, legacy, seed, future-loot, recipe-predicate, and strict
version-gate rules without loading either optional mod. `WolfsbaneResourceTest` verifies the
canonical definition, both semantic identities, exact Enchanted block/seed IDs, all four direct
recipe inputs, purple-dye advancement predicate, future-loot replacement, and conditional
worldgen-removal resource.

## Manual test matrix

| # | Test | Expected result |
| ---: | --- | --- |
| 1 | Break short grass with a hoe until `enchanted:wolfsbane_seeds` drops; plant on farmland and harvest at age 4. | Mature crop yields `enchanted:wolfsbane_flower` and seeds. No claim is made that wild crop blocks generate. |
| 2 | Explore newly generated forest chunks with canonicalization enabled. | No new `werewolves:wolfsbane` flower patches. Existing chunks are unchanged. |
| 3 | Load an old native block and stack. | Both remain registered and usable; no automatic mutation occurs. Breaking the block yields the canonical flower through new loot. |
| 4 | Craft purple dye and the Finder with the Enchanted flower. | Both recipes accept it. A legacy stack also remains accepted. |
| 5 | Make both alchemical diffuser cores using the Enchanted flower. | The cauldron `fluid` ingredient accepts `#darkfolklore:wolfsbane`; native skill gates remain. |
| 6 | Fuel each empty diffuser with the Enchanted flower. | Exactly one flower is consumed outside creative mode; native fuel duration, startup, UI, aura, and already-fueled response work. |
| 7 | Hold the Finder near a diffuser. | The unchanged native rotating diffuser marker renders. |
| 8 | Right-click air with the Finder near a planted Enchanted crop. | Coordinates and distance are reported without loading chunks; the item receives a 60-tick cooldown. |
| 9 | Walk a Werewolves player/entity through `enchanted:wolfsbane` outside Peaceful. | Native `werewolves:wolfsbane` effect is applied for 45 ticks at amplifier 1, matching legacy contact. |
| 10 | Stand a Werewolves player in a fueled diffuser's chunk range. | The native `LevelWolfsbane` aura continues to apply its 50-tick effect at the diffuser amplifier. |
| 11 | Kill an alpha werewolf and unlock relevant recipes. | `natural_succession` still progresses; purple-dye inventory discovery accepts either semantic flower; other recipe advancements remain recipe-ID based. |
| 12 | Run without Enchanted, then with an unaudited Werewolves/Enchanted version. | No implementation bridge loads. Without Enchanted, Werewolves worldgen/legacy recipes remain usable. An unaudited code bridge reports `UNTESTED_VERSION` and fails closed. |

## Candid limitations

- The Werewolves creative tab, native diffuser marker model, and `natural_succession` display icon
  can still show the registered legacy item. They do not make that item necessary for progression.
- The Finder's added crop search is bounded to loaded chunks within 32 by 8 by 32 blocks; it is not
  a global locate command and intentionally does not generate chunks.
- No safe automatic migration scans arbitrary inventories or containers. Admin-controlled migration
  was not added because the legacy item remains valid and future acquisition is already routed.
- Data-pack recipe overrides are robust tag integration rather than implementation-bound code. The
  native diffuser/contact/Finder bridge is the only version-pinned code path.
- Graphical client startup to the title state passed with the exact bridge active, but in-world
  visual behavior still requires a client smoke; startup alone cannot validate the native diffuser
  marker or localized Finder messages.
