# Dark Folklore Core 0.5.0 release gate

## Target

| Property | Value |
| --- | --- |
| Mod ID | `darkfolklore` |
| Version | `0.5.0` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.248` / 21.1 line |
| Java | 21 |
| Society persistence | schema 2 |
| Investigation sidecar | schema 1 |
| Classification | **`RELEASE_CANDIDATE`** |

0.5.0 is the Recipe Weaving & Universal Interoperability phase. It preserves the 0.4 factual/provider boundaries while making audited ingredients and progression paths cooperate across the intended modpack.

## Evidence baseline

Dark Folklore Atlas `scan-20260812-154548`:

- 192 mods;
- 8,371 items;
- 623 entities;
- 8,908 recipes;
- 1,286 item tags;
- 208 item concept groups;
- 247 canonicalization rows.

Atlas similarity is review evidence, not equivalence authority.

## Automated gate

First integrated 0.5 code head: `3283b39ac8d2f5a2de78c0213a9c38c21633967a`.

GitHub Actions run `31621963509`: **PASS**.

| Check | Result |
| --- | --- |
| Java 21 clean build | **PASS** |
| Resource JSON syntax | **PASS** |
| JUnit | **124/124 PASS**, 0 failures/errors/skipped |
| Release JAR audit | **PASS** |
| NeoForge GameTests | **3/3 PASS** |
| Core data reload | `17/5/8/2/9/13/4/6`, `0 invalid` |
| Artifact upload | **PASS** |

Production artifact from that gate:

| Property | Value |
| --- | --- |
| Artifact | `darkfolklore-core-0.5.0.jar` |
| Size | `523,311` bytes |
| SHA-256 | `FFE398538955BF96743C91F324BB454336773E117534A722CCA0031C49F8C777` |
| Class files | `207` |
| GitHub artifact ID | `9151612457` |

The source JAR is development-only and must not be installed in the pack.

## Recipe ownership guarantees

0.5 does not flatten provider progression:

- Vampirism Vampire Killer arrows, Hunter Axe and normal Hunter Coat pieces remain `vampirism:shaped_crafting_weapontable` recipes;
- Pure Salt and Purified Garlic remain `vampirism:alchemical_cauldron` recipes and retain their provider skill gates;
- Magician's Wax remains `eidolon_repraised:crucible` with the same steps/stirs;
- provider outputs remain provider registry objects;
- broad semantic tags are not treated as universal recipe currencies;
- same-ID recipe overrides retain the provider's original ingredient inside the replacement recipe-safe tag;
- additive weaving is preferred where the original route should remain available.

## Current 0.5 weave

- Enchanted garlic interoperates with the audited Vampirism/MCA Vamp Compat garlic recipe families.
- `c:crops/garlic` receives Enchanted garlic for future provider recipes already written against the common tag.
- MCA Vamp Compat Occult Arts Book links vampire knowledge to a recipe-safe ritual focus.
- Occultism tallow can satisfy Eidolon Magician's Wax.
- Naturalist fur can satisfy audited Fangs 'n Claws fur crafts.
- Enchanted quicklime can satisfy all audited Hearth & Timber plaster/rubblestone recipes.
- Immersive Engineering fertilizer can satisfy selected Farm & Charm soil/compost crafts.
- Enchanted, Eidolon and Occultism ashes can satisfy only the ash positions of Enchanted Ritual Chalk.
- Werewolves Stone Altar now consumes shared wolfsbane, common silver and a ritual focus.
- Vampirism keeps its original Totem Top route and gains an additive occult-focus-gem alternative using audited Occultism/Feywild/Eidolon/Malum items.

See `docs/RECIPE_WEAVING_0.5.md` for the exact tag and recipe matrix.

## Canonicalization boundary

0.5 retains existing audited choices:

- base silver/material priority remains Immersive Engineering/AlmostUnified-owned;
- garlic canonical acquisition remains `vampirism:garlic` while Enchanted garlic becomes recipe-interoperable;
- wolfsbane canonical acquisition remains `enchanted:wolfsbane_flower` while Werewolves native semantics remain bridged;
- foreign registry entries are never unregistered and existing stacks are not bulk-migrated.

KEEP_DISTINCT items such as the two soul shards, Enchanted/Feywild mandrakes, Enchanted/Malum poppets, and provider altars are explicitly guarded from accidental recipe equivalence.

## Automated evidence limitation

The standard GitHub GameTest environment intentionally contains Dark Folklore + Minecraft + NeoForge, not the complete optional 192-mod provider pack. Therefore a green CI run proves Core compilation/resources/tests and vanilla-side reload safety, **not** that every third-party custom serializer accepted the override in the authentic pack.

## Full-pack manual promotion blockers — not run

Before promotion, use the intended 192-mod instance and record:

1. server/client startup and `/reload` with no invalid recipe/tag/serializer errors;
2. both audited garlic items through all fourteen garlic recipe families;
3. Weapon Table and Alchemical Cauldron restrictions still enforced;
4. both tallow items in Magician's Wax through the real Eidolon Crucible;
5. both fur items in all six audited Fangs crafts;
6. both quicklime items in all eighteen audited Hearth & Timber recipes;
7. both fertilizer items in the selected Farm & Charm recipes;
8. all three ritual ashes in Ritual Chalk without bypassing the Enchanted-specific Tear/Gypsum requirements;
9. Stone Altar with vanilla amethyst and each installed audited ritual focus;
10. original Vampirism Totem Top recipe plus each available occult-gem alternative;
11. JEI/recipe-book visibility and station matching where applicable;
12. `/dfatlas scan` after installing 0.5 and a diff against `scan-20260812-154548`;
13. existing 0.4 MCA lifecycle/predation/Field Guide/save-restart manual gates.

For the Atlas diff, audited exact-only garlic/fur/quicklime/tallow gaps should collapse to zero or be explained by a verified provider runtime limitation.

## Classification

Current classification: **`RELEASE_CANDIDATE`**.

Do not promote to `PRODUCTION_READY` until the authentic full-pack recipe matrix and the inherited 0.4 manual gates are recorded.
