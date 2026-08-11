# Compatibility

This document describes Dark Folklore Core 0.2.0 for Minecraft 1.21.1, NeoForge 21.1.248, and Java 21. It records the installed versions that were audited, not a promise that later releases of those mods are compatible.

## Status model

CompatibilityManager compares the version reported by NeoForge with the exact audited version.

| Runtime status | Meaning |
| --- | --- |
| ACTIVE | The optional mod is installed at the exact audited version. |
| DISABLED | The optional mod is not installed. The Core continues without it. |
| UNTESTED_VERSION | The mod is installed at another version. Version-sensitive Java adapters are not loaded. |
| ERROR | An exact-version adapter was selected, but its expected classes or methods could not be validated or loaded. |

Data-driven optional entries use required: false and therefore remain safe when their provider is absent. They are not dynamically removed on a version mismatch; an UNTESTED_VERSION report should still prompt a pack audit of registry IDs and data semantics.

The Core-owned canonicalization enforcement paths are also gated by the common canonicalization toggle, which defaults to true. That gate covers the custom biome modifiers, the global loot modifier, and spawn profiles marked canonicalization_suppression. It does not disable AlmostUnified's separate recipe configuration or another mod's own spawn toggles.

## Audited pack matrix

The versions below come from the installed JAR metadata recorded by Dark Folklore Atlas 0.2.0. “Coverage” describes what the current Core actually does; it is deliberately more specific than the runtime ACTIVE label.

| Mod ID | Exact tested version | Mechanism | Coverage in this release |
| --- | --- | --- | --- |
| vampirism | 1.10.12 | Typed public faction API in an isolated adapter | Full factual vampire/hunter detection; canonical garlic and blood semantics; tag and weakness integration. |
| werewolves | 2.0.3.3 | Vampirism public faction registry, native tags, and an isolated exact wolfsbane bridge | Full factual werewolf detection when the Vampirism adapter is active; native silver semantics are preserved; duplicate Fangs natural-spawn suppression follows the canonicalization toggle. With exact Enchanted 4.2.7, the bridge preserves diffuser fuel, contact effect, and finder behavior for canonical Enchanted wolfsbane. |
| mca | 7.7.32+1.21.1 | Standard events plus cached exact-version public-implementation reads | Core never alters MCA state. It reads only verified relationship categories, hearts/personality context, and ordinary entity identity for social trust; supernatural facts come from the exact MCA Vamp Compat bridge below. |
| mcacapitals | 1.1.0 | Cached exact-version read-only public-implementation bridge | Verified offices map to Core political roles and configurable credibility/response weights. The 1,024-entry Core-DTO cache expires after 20 ticks; Core never writes monarchy, capital, village, family, or succession state. |
| mca_vamp_compat | 2.0.12 | Exact-version reflection over public implementation methods | Read-only vampire, werewolf, hunter, and conversion-provenance queries for MCA entities. No duplicate infection, cure, AI, inheritance, trade, marriage, or village-capture implementation. |
| fieldguide | 1.14.0 | Public unlock type plus public server progress implementation | Six curated categories, nine explicit provider-backed entries, English/Italian text, normal scan/kill unlock participation, bounded unlock-to-Core discovery polling, and Core `OBSERVED` threshold forwarding to an existing Field Guide page. Field Guide remains binary and owns its UI/notes. |
| enchanted | 4.2.7 | Registry IDs, optional tags, canonical loot/worldgen routing, and exact wolfsbane pairing | Enchanted owns the canonical farmable wolfsbane flower/seed/crop; new audited acquisition routes avoid the legacy flower while existing stacks remain. With exact Werewolves 2.0.3.3, the isolated bridge preserves native mechanics. Garlic loot routes to Vampirism garlic when canonicalization is enabled; mandrake, altars, and poppets remain distinct. |
| occultism | 1.224.2 | Registry IDs, tags, loot and worldgen data | Config-gated silver loot/worldgen routing, spirit/soul/ritual traits, and distinct golem semantics. No Java adapter. |
| malum | 1.8.2 | Registry IDs and tags | Soul traits and distinct poppet semantics. No Java adapter. |
| eidolon_repraised | 0.5.0.2 | Registry IDs, tags, loot and worldgen data | Config-gated silver routing, holy/soul traits, a curated wraith spawn profile, and distinct altar/wraith semantics. No Java adapter. |
| feywild | 5.5.5 | Registry IDs and tags | Fae classification and distinct mandrake semantics. No Java adapter. |
| betterarcheology | 1.21.1-1.3.8 | Optional item tags and standard pickup event | Four audited artifact items can grant a one-time archaeology lore discovery. |
| quest_giver | 1.5.1 | Detection only | Optional future/front-end integration. Dark Folklore owns contract state; no Quest Giver bridge is implemented in 0.2.0. |
| almostunified | 1.21.1-1.4.2 | Existing pack configuration | Recipe unification delegates silver material output to Immersive Engineering. Core adds separately configurable new-loot routing and duplicate-feature removal. |

The manager also reports MCA Capitals, Enchanted, Occultism, Malum, Eidolon, Feywild, Better Archeology, Quest Giver, and AlmostUnified as ACTIVE when their exact versions are present. For data-only or detection-only rows, ACTIVE means “exact audited pack component found,” not “a private Java adapter is running.” The separate wolfsbane runtime report requires both exact Werewolves and exact Enchanted before enabling its mechanics bridge.

## Code-bound JAR identity

The deepest integrations were inspected against these exact files and SHA-256 values:

| JAR | SHA-256 |
| --- | --- |
| Vampirism-1.21-1.10.12.jar | C6DCCA1AF24DECA473A24470CCAB66053D3AA3324E453B4E1697090ED6D16BE2 |
| Werewolves-1.21-2.0.3.3.jar | ECBCA2CD344E24AD48157834A8F321D1A7D2221C727FE8E61E4436D1219C6CFB |
| mca-neoforge-7.7.32+1.21.1.jar | 874B5BD82D754033117EE6C1E7B5EBD142EC5DC0DF2881C9BB2F38A05AE7F4AB |
| mcacapitals-1.1.0.jar | 73AF01FAE88C9698D93EF0372854EA57373EFA0276C0CB33CC38FEFEEDED7B56 |
| mca-vamp-compat-1.21.1-2.0.12.jar | BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF |
| enchanted-neoforge-1.21.1-4.2.7.jar | 205A3E1EDB7E53E9BBF3B8AB965AC5EB3840BC43760E1B003200E4735EBF1BB0 |
| fieldguide-neoforge-1.21.1-1.14.0.jar | 00B26B1351CB85B90ED86675C49BFC054A3141BEDAE22358D5A6AD4FE7CB0740 |

## Failure containment

Vampirism, Field Guide, and the Werewolves/Enchanted wolfsbane bridge are loaded by class name only after their exact versions are confirmed. Their external types therefore do not enter common initialization when the mods are absent. MCA, MCA Capitals, and MCA Vamp Compat reflection is isolated; expected methods are resolved once during exact-version adapter initialization, and runtime caches retain only bounded Core DTOs.

Queries return four states: TRUE, FALSE, UNKNOWN, and NOT_APPLICABLE. A failed external query becomes UNKNOWN rather than silently becoming FALSE. Compatibility failures are logged and exposed by diagnostics.

No compatibility Mixin or Access Transformer is used. Common/server initialization references no client-only class.

## MCA Vamp Compat ownership boundary

MCA Reborn x Vampirism Compat remains the authority for supernatural MCA villagers. It already owns supernatural state, infection and conversion, cure flow, faction traits, combat/AI behavior, inheritance, social and marriage consequences, trades, and Vampirism village interactions. Dark Folklore only reads its state and adds pack-wide lore, witness knowledge, rumors, investigation stories, contracts, reputation, and a provenance record.

The UUID returned by VampiricVillagerState.getSource or getWerewolfSourceUuid is stored as a conversion source. It must not be presented as a guaranteed biological parent or universally reliable sire.

## Known compatibility boundaries

- The MCA bridge is gated directly on mca_vamp_compat 2.0.12. CompatibilityManager reports the MCA version separately, but the adapter does not independently require an exact MCA report before loading. The add-on's own dependency metadata is expected to reject an incompatible MCA installation; a future Core release should make this double gate explicit.
- Field Guide has no stable unlock event used by Core. The 1.14.0 bridge reads public progress implementation classes and is therefore exact-version only.
- MCA relationship/personality and MCA Capitals office reads use public implementation signatures rather than stable API namespaces. Both bridges are exact-version-only and fail closed; the remaining end-to-end checks with real families and a founded capital are documented in their focused audits.
- Optional tag entries protect absent registry IDs. They do not prove that a changed future entity or item still has the same semantics.
- Core loot conversion applies only to newly generated loot and only while canonicalization is enabled. Existing stacks and non-loot creation paths are not migrated.
- The Immersive Engineering silver-bullet stack is tagged semantically, but third-party projectile damage is enhanced only when Minecraft exposes that tagged stack as the damage source's weapon item; the Core does not reflect into IE projectile internals.
- Core feature/spawn removal uses custom, config-aware NeoForge biome-modifier codecs. The toggle is evaluated when biome modifiers apply; changing it never retroactively changes existing chunks or entities.
- Quest Giver is not a contract backend or frontend in this release despite being detected.
- Existing foreign items, blocks, entities, and save data are never unregistered or rewritten in place.

## Upgrade procedure

For every optional-mod update:

1. Re-run Atlas against the actual pack.
2. Compare the installed JAR hash and metadata.
3. Re-run the API audit for every Java-bound integration.
4. Validate optional registry IDs, tags, loot mappings, placed features, and Field Guide entry IDs.
5. Run a dedicated-server smoke test with the complete pack and another with optional mods absent.
6. Only then update the exact tested version in CompatibilityManager and this document.
