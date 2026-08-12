# Dark Folklore Core 0.5.0 testing

## Automated evidence

Code head: `3283b39ac8d2f5a2de78c0213a9c38c21633967a`

GitHub Actions run: `31621963509` — **PASS**.

- clean Java 21 build: PASS;
- JUnit: **124/124 PASS**, 0 failures/errors/skipped;
- five new `RecipeWeavingResourceTest` tests: PASS;
- release JAR audit: PASS;
- NeoForge GameTests: **3/3 PASS**;
- Core reload: 17 canonical concepts, 5 weaknesses, 8 spawn profiles, 2 magic integrations, 9 investigation profiles, 13 story templates, 4 organization archetypes, 6 political overrides, 0 invalid;
- artifact: `darkfolklore-core-0.5.0.jar`, 523,311 bytes, SHA-256 `FFE398538955BF96743C91F324BB454336773E117534A722CCA0031C49F8C777`, 207 classes.

### New regression assertions

`RecipeWeavingResourceTest` verifies that:

- recipe garlic equivalence contains Vampirism + Enchanted garlic but excludes Vampire's Delight wild garlic;
- Enchanted garlic extends `c:crops/garlic`;
- Vampirism Weapon Table and Alchemical Cauldron serializers remain provider-owned;
- Eidolon Magician's Wax remains a Crucible recipe;
- key interoperability overrides reference the new narrow recipe-safe tags;
- KEEP_DISTINCT soul shards/mandrakes/poppets are not accidentally collapsed into recipe currencies;
- Stone Altar retains its provider output and has a vanilla ritual-focus fallback;
- the occult Totem Top route is additive and references the curated focus-gem tag.

## Authentic-pack matrix

Run with the intended 192-mod pack and record PASS/FAIL plus screenshots/log excerpts where useful.

| Area | Test | Status |
| --- | --- | --- |
| Startup | client reaches title/world with 0.5 | NOT RUN |
| Reload | `/reload` without recipe/tag errors | NOT RUN |
| Diagnostics | `/folklore diagnostics` and `invalid=0` | NOT RUN |
| Garlic | Enchanted + Vampirism garlic in all audited families | NOT RUN |
| Vampirism stations | Weapon Table restrictions preserved | NOT RUN |
| Vampirism alchemy | skills/Alchemical Cauldron preserved | NOT RUN |
| Tallow | Occultism/Eidolon tallow in Magician's Wax | NOT RUN |
| Fur | Fangs/Naturalist fur in six Fangs crafts | NOT RUN |
| Quicklime | H&T/Enchanted quicklime in 18 crafts | NOT RUN |
| Fertilizer | Farm & Charm/IE in selected soil crafts | NOT RUN |
| Ritual ash | all 3 audited ashes in Enchanted Ritual Chalk | NOT RUN |
| Stone Altar | amethyst + installed ritual-focus alternatives | NOT RUN |
| Totem Top | original route + occult gem alternatives | NOT RUN |
| JEI | recipe visibility and correct stations | NOT RUN |
| Atlas | new `/dfatlas scan` and baseline diff | NOT RUN |
| Save | existing 0.4 world loads without migration damage | NOT RUN |

## Atlas acceptance

Baseline: `scan-20260812-154548`.

After installing the CI-produced 0.5 JAR:

```mcfunction
/dfatlas scan
```

Compare at minimum:

- recipe consumers by item;
- tag membership;
- exact-only vs tag-interoperable ingredient slots;
- canonicalization matrix;
- recipe producer changes;
- conflicts and missing references.

The intended outcome is not “all similar names become interchangeable”. The intended outcome is that audited equivalence gaps disappear while KEEP_DISTINCT groups remain separate.

## Promotion rule

Automated PASS is necessary but insufficient. 0.5 remains `RELEASE_CANDIDATE` until the full-pack matrix above and inherited 0.4 provider/client gates are completed.
