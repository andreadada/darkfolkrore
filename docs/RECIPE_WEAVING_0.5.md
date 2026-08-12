# Dark Folklore 0.5 — Recipe Weaving & Universal Interoperability

## Evidence baseline

This phase uses Dark Folklore Atlas `scan-20260812-154548`, captured from the intended Minecraft 1.21.1 pack after the 0.4.0 merge: **192 mods, 8,371 items, 623 entities, 8,908 recipes, 1,286 item tags, 208 item concept groups, and 247 canonicalization rows**.

Atlas similarity is candidate evidence, never proof of equivalence. A same-name object is not made interchangeable without a semantic and recipe audit.

## Three distinct layers

1. **Semantic trait** — broad lore/gameplay meaning (`darkfolklore:garlic`, `holy`, `soul`, etc.). Related objects may be deliberately non-substitutable.
2. **Recipe-safe equivalence** — narrow audited tags under `darkfolklore:recipe/*`. Same-ID overrides always keep their provider's original ingredient inside the replacement tag.
3. **Curated weaving** — deliberate cross-mod progression recipes that keep the owning provider's output/station/skills and only broaden or enrich ingredients.

## Recipe-safe tags

| Tag | Intended members / purpose |
| --- | --- |
| `recipe/garlic` | Vampirism garlic + Enchanted garlic only. Vampire's Delight wild garlic remains semantic-only. |
| `recipe/bread` | Vanilla bread plus `c:bread`. |
| `recipe/fur` | Fangs 'n Claws fur + Naturalist fur. |
| `recipe/quicklime` | Hearth & Timber quicklime + Enchanted quicklime. |
| `recipe/fertilizer` | Farm & Charm + Immersive Engineering fertilizer for selected crafting only. |
| `recipe/ritual_ash` | Enchanted Wood Ash, Eidolon Enchanted Ash, Occultism Otherworld Ashes. |
| `recipe/ritual_focus` | Vanilla amethyst fallback plus Enchanted/Bloodlines/Occultism/Eidolon ritual components. |
| `recipe/occult_focus_gem` | Occultism Spirit Attuned Gem, Feywild Fey Gem, Eidolon Shadow Gem, Malum Refined Soulstone. |
| `recipe/obsidian` | Obsidian family for woven construction. |
| `recipe/holy_consumable` | Narrow Vampirism holy-water consumables; distinct from broad `darkfolklore:holy`. |

Dark Folklore also contributes Enchanted garlic to `c:crops/garlic`, so future/provider recipes already written against the common garlic crop tag interoperate automatically.

## Implemented interoperability

### Enchanted ↔ Vampirism ↔ MCA Vamp Compat — garlic

The Atlas baseline exposed 21 exact-only garlic ingredient slots across 14 recipes. 0.5 routes all audited garlic inputs through `darkfolklore:recipe/garlic`:

- MCA Vamp Compat Occult Arts Book;
- Vampirism garlic bread, injection, Hunter Table, Alchemical Cauldron block, Garlic Finder;
- Vampire Killer Crossbow Arrows;
- Hunter Axe;
- normal Hunter Coat head/chest/legs/feet;
- Pure Salt;
- Purified Garlic.

Weapon Table recipes remain `vampirism:shaped_crafting_weapontable`; Pure Salt/Purified Garlic remain `vampirism:alchemical_cauldron`. No vanilla-crafting shortcut bypasses provider skills or stations.

The Occult Arts Book is additionally woven into the pack: book + vampire fang + recipe-safe garlic + `recipe/ritual_focus`. The focus can be vanilla amethyst or an audited component from Enchanted, Bloodlines, Occultism, or Eidolon.

### Occultism ↔ Eidolon — tallow

`eidolon_repraised:magicians_wax` remains an Eidolon Crucible recipe with the same steps and stir count, but both tallow slots consume `c:tallow`.

### Naturalist ↔ Fangs 'n Claws — fur

Fur armor, horse blanket and fur-to-wool accept either Fangs fur or Naturalist fur. No item/loot migration occurs.

### Enchanted ↔ Hearth & Timber — quicklime

All sixteen colored plaster recipes plus covered/plastered rubblestone accept either audited quicklime. This is recipe-context interoperability, not a claim that provider Java mechanics are identical.

### Immersive Engineering ↔ Farm & Charm — fertilizer

Only `compost` and `fertilized_soil` are broadened. Machine/growth behavior remains provider-owned.

### Enchanted ↔ Eidolon ↔ Occultism — ritual ashes

Enchanted Ritual Chalk accepts the three audited ash families in its ash positions while Tear of the Goddess and Gypsum remain Enchanted requirements.

## Curated progression weaving

### Werewolves Stone Altar

The previous eight-stone-brick recipe is replaced by a supernatural recipe:

```text
Stone Brick | Wolfsbane    | Stone Brick
Silver      | Ritual Focus | Silver
Stone Brick | Stone Brick  | Stone Brick
```

This ties Werewolves to the pack's shared wolfsbane/silver economies and at least one ritual focus. `recipe/ritual_focus` always includes vanilla amethyst, so Werewolves does not gain a hard dependency on another optional magic mod.

### Vampirism Totem Top — occult alternative

The provider's original Totem Top recipe remains valid. 0.5 adds an alternative that replaces the diamond focus with an Occultism/Feywild/Eidolon/Malum occult gem. This makes progress in another discipline useful without removing Vampirism's independent route.

## Canonicalization retained

- base silver/lead/steel material unification remains owned by AlmostUnified's Immersive Engineering priority; Core complements loot/worldgen instead of becoming a second material-unifier;
- garlic pack canonical remains `vampirism:garlic`, while Enchanted garlic is recipe-interoperable;
- wolfsbane pack canonical remains `enchanted:wolfsbane_flower`, with both Enchanted/Werewolves implementations usable by audited recipes and native mechanics;
- Werewolves remains the canonical werewolf gameplay owner; Critters n' Crawlers the canonical natural Chupacabra; Myths & Legends the canonical natural Imp.

## KEEP_DISTINCT guardrails

Do **not** use broad semantic tags as universal ingredients. In particular:

- Eidolon and Occultism `soul_shard` remain different mechanics;
- Enchanted and Feywild mandrakes remain distinct;
- Enchanted and Malum poppets remain distinct;
- altars remain owned by their ritual/faction systems;
- broad `darkfolklore:holy`, `soul`, and `spiritual` remain semantic traits, not universal crafting currencies;
- silver weapons remain separate equipment even when all share anti-werewolf semantics.

## Mod-to-mod weave map

```text
Enchanted garlic ─────┐
                      ├─> Vampirism Hunter/alchemy/crafting
Vampirism garlic ─────┘

Enchanted/Werewolves wolfsbane ───────────┐
IE/Occultism/Eidolon/Werewolves silver ───┼─> Werewolves Stone Altar
Enchanted/Bloodlines/Occultism/Eidolon focus ─┘

Occultism tallow ─────> Eidolon Crucible wax
Enchanted/Eidolon/Occultism ashes ─> Enchanted Ritual Chalk
Naturalist fur ───────> Fangs equipment
Enchanted quicklime ──> Hearth & Timber masonry
IE fertilizer ────────> Farm & Charm soil/compost
Occultism/Feywild/Eidolon/Malum gem ─> alternate Vampirism Totem Top
```

## Acceptance gates

Automated gates require all JSON to parse, narrow garlic equivalence, preservation of Weapon Table/Alchemical Cauldron/Eidolon Crucible serializers, key tag references, KEEP_DISTINCT exclusions, existing unit/GameTests, and release JAR audit.

Full-pack manual gates:

1. reload the intended 192-mod pack with zero invalid recipe/tag errors;
2. verify both audited garlic items in all 14 garlic recipe families, including native Weapon Table/alchemy restrictions;
3. verify both tallow items in Magician's Wax;
4. verify both fur items in all Fangs recipes;
5. verify both quicklime items in all 18 audited Hearth & Timber recipes;
6. verify selected fertilizer interoperability and all three ritual ashes;
7. verify Stone Altar with vanilla amethyst and each installed occult focus;
8. verify all four occult-gem Totem Top alternatives while the original Vampirism recipe remains;
9. rerun `/dfatlas scan` and compare against `scan-20260812-154548`; audited exact-only garlic/fur/quicklime/tallow gaps should collapse to zero or an explicitly documented runtime exception.

0.5 remains a development/release-candidate branch until this full-pack matrix is recorded.
