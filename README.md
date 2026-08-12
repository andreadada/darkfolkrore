# Dark Folklore Core

Dark Folklore Core is the server-authoritative integration and orchestration layer for the Dark Folklore Minecraft modpack. It gives overlapping supernatural content a shared semantic vocabulary and connects provider-owned facts to lore, witnesses, rumors, villages, organizations, investigations, weaknesses, contracts, vampire predation, recipe interoperability, and cross-mod progression.

> **Provider mods own their facts and native mechanics. Dark Folklore connects them without silently replacing them.**

For MCA entities specifically, MCA Reborn × Vampirism Compat remains authoritative for factual supernatural state, infection, conversion, cure, inheritance, persistence, targeting, navigation, and native AI. Dark Folklore observes those facts and owns knowledge, investigation, rumor, reputation, evidence, stories, and contracts around them.

## Target

- Minecraft **1.21.1**
- NeoForge **21.1.248** / 21.1 line
- Java **21**
- Mod ID `darkfolklore`
- Version **0.5.0**
- Society persistence schema **2**
- Investigation sidecar schema **1**

Third-party gameplay integrations are optional. Exact Java/reflection adapters activate only for audited versions; missing, partial, failed, or different versions fail closed rather than guessing supernatural state.

## 0.5.0 — Recipe Weaving & Universal Interoperability

0.5.0 builds on the merged 0.4.0 lifecycle/investigation foundation and uses Dark Folklore Atlas `scan-20260812-154548` as the recipe-graph baseline: **192 mods, 8,371 items, 623 entities, 8,908 recipes, 1,286 item tags, 208 item concept groups, and 247 canonicalization rows**.

The recipe layer deliberately separates three concepts:

1. **semantic traits** — broad lore/gameplay meaning such as `darkfolklore:garlic`, `holy`, `soul`, and `spiritual`;
2. **recipe-safe equivalence** — narrow audited ingredient tags under `darkfolklore:recipe/*`;
3. **canonicalization** — one preferred future acquisition/output only where registry objects are truly interchangeable.

A same-name item is never made interchangeable solely because Atlas grouped it.

### Current 0.5 weave

- **Enchanted ↔ Vampirism ↔ MCA Vamp Compat:** Enchanted and Vampirism garlic can satisfy the audited garlic recipe families while Weapon Table and Alchemical Cauldron recipes remain provider-owned.
- **Occult Arts Book:** now requires a vampire fang, recipe-safe garlic, and a ritual focus. The focus can come from Enchanted, Bloodlines, Occultism, Eidolon, or the vanilla amethyst fallback.
- **Occultism ↔ Eidolon:** either common tallow can be used for Magician's Wax while the recipe remains an Eidolon Crucible recipe.
- **Naturalist ↔ Fangs 'n Claws:** either audited fur can make Fangs fur equipment, horse blanket, and wool conversion.
- **Enchanted ↔ Hearth & Timber:** either audited quicklime works in all sixteen colored plaster recipes plus covered/plastered rubblestone.
- **Immersive Engineering ↔ Farm & Charm:** selected compost/fertilized-soil recipes accept either audited fertilizer; growth/machine behavior remains provider-owned.
- **Enchanted ↔ Eidolon ↔ Occultism:** audited ritual ashes can substitute only in Enchanted Ritual Chalk's ash positions.
- **Werewolves Stone Altar:** now weaves stone, shared wolfsbane, common silver, and a ritual focus instead of being an eight-stone-brick recipe.
- **Vampirism Totem Top:** the original provider recipe stays valid; 0.5 adds an occult alternative using an Occultism/Feywild/Eidolon/Malum focus gem.

Full rationale and the mod-to-mod weave map are in [Recipe Weaving 0.5](docs/RECIPE_WEAVING_0.5.md).

### KEEP_DISTINCT guardrails

Broad semantic similarity is not universal crafting equivalence. In particular:

- Eidolon and Occultism `soul_shard` remain mechanically distinct;
- Enchanted and Feywild mandrakes remain distinct;
- Enchanted and Malum poppets remain distinct;
- provider altars remain owned by their ritual/faction systems;
- silver weapons remain separate equipment despite sharing anti-werewolf semantics;
- broad `darkfolklore:holy`, `soul`, and `spiritual` tags are not universal recipe currencies.

## Canonicalization

The pack-facing canonical choices retained by 0.5 include:

- base silver/material family → **Immersive Engineering**, with AlmostUnified owning recipe material unification and Core complementing audited loot/worldgen paths;
- garlic → **Vampirism garlic**, while Enchanted garlic remains recipe-interoperable;
- wolfsbane → **Enchanted wolfsbane flower**, while Werewolves retains its native diffuser/contact/finder/faction semantics;
- werewolf gameplay/ecology → **Werewolves**;
- natural Chupacabra → **Critters n' Crawlers**;
- natural Imp → **Myths & Legends**.

No foreign registry entry is removed, existing stacks are not bulk-rewritten, and save compatibility remains the default constraint.

## Unified gameplay loop

```text
incident / suspicious feeding
 -> physical evidence + witnesses
 -> evidence-only hypotheses
 -> optional occult analysis
 -> identification
 -> Field Guide / lore
 -> research
 -> learned countermeasure
 -> bounded tracking
 -> hunt / social resolution
 -> village, family, rumor and organization consequences
```

0.5 adds a parallel integration goal:

```text
provider progression
 -> shared audited ingredient vocabulary
 -> cross-mod recipe bridges
 -> curated convergence recipes
 -> one coherent pack progression
```

## 0.4 foundation retained

0.5 preserves the 0.4 provider boundary and lifecycle observation:

- exact MCA states `HUMAN`, `INFECTED`, `VAMPIRE`, and `CURING`;
- infection/conversion/inheritance/cure transitions observed without becoming their authority;
- provider provenance recovery without fabricated inherited conversion sources;
- provider-native AI ownership and fail-closed factual routing;
- exact native-bite/feed evidence correlation;
- 0.3.1 confirmed-death finality, investigation continuity, witness/rumor, Field Guide, contract, and vampire-society hardening.

See [0.4.0 handoff](docs/HANDOFF_0.4.0.md) and [exact MCA Vamp Compat audit](docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md) for the detailed factual ownership model.

## Install

1. Use Minecraft 1.21.1, NeoForge 21.1.248, and Java 21.
2. Build or obtain `darkfolklore-core-0.5.0.jar`.
3. Put only the production JAR — **not** the sources JAR — in the instance `mods` directory.
4. Install the optional provider mods used by the pack; exact adapter versions are listed in [Compatibility](docs/COMPATIBILITY.md).
5. Back up an existing world before changing Core/provider versions.
6. Run `/folklore diagnostics` and require `invalid=0`.
7. For the vampire stack also inspect `/folklore predation status` and `/folklore lifecycle status`.
8. For 0.5 full-pack acceptance, rerun `/dfatlas scan` and compare the recipe/tag graph with the 0.4 baseline.

## Build

Windows:

```powershell
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

Linux/macOS:

```bash
./gradlew clean build --no-daemon --no-configuration-cache --stacktrace
```

Artifacts:

```text
build/libs/darkfolklore-core-0.5.0.jar
build/libs/darkfolklore-core-0.5.0-sources.jar
```

Useful tasks:

```text
./gradlew test
./gradlew runGameTestServer
./gradlew runServer
./gradlew runClient
```

Optional provider code remains compile-only and is never shaded into the production JAR.

## Operator diagnostics

Ground-truth commands require permission level 2.

```text
/folklore diagnostics
/folklore inspect <entity>
/folklore canonical <concept>
/folklore knowledge get <player> <concept>
/folklore knowledge grant <player> <concept> <points>
/folklore social inspect <entity>
/folklore rumor inspect
/folklore fieldguide diagnostics
/folklore capitals inspect <entity>
/folklore organization list
/folklore village [inspect]
/folklore story list
/folklore contracts
/folklore investigation status <player>
/folklore investigation hypotheses <player>
/folklore predation status
/folklore predation inspect <entity>
/folklore lifecycle status
/folklore lifecycle inspect <entity>
```

## Documentation

- [Recipe Weaving 0.5](docs/RECIPE_WEAVING_0.5.md)
- [0.5.0 release gate](docs/RELEASE_0.5.0.md)
- [0.5.0 testing](docs/TESTING_0.5.0.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Compatibility and factual routing](docs/COMPATIBILITY.md)
- [Canonicalization](docs/CANONICALIZATION.md)
- [MCA Vamp Compat 2.0.12 exact audit](docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md)
- [Field Guide](docs/FIELD_GUIDE.md)
- [0.4.0 handoff](docs/HANDOFF_0.4.0.md)
- [0.4.0 release gate](docs/RELEASE_0.4.0.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md)
- [Changelog](CHANGELOG.md)

## Release classification

0.5.0 is **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`. GitHub Actions validates the Core build, resource syntax, 124 JUnit tests, three NeoForge GameTests, and release JAR audit, but the intended **192-mod** pack must still prove real provider recipe deserialization, JEI/station behavior, ingredient substitution, and the Atlas before/after graph. No full-pack manual pass is claimed until that matrix is recorded.

## License

All Rights Reserved. The release JAR includes `META-INF/LICENSE_darkfolklore`.
