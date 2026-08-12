# Compatibility

This document describes Dark Folklore Core 0.4.0 for Minecraft 1.21.1, NeoForge 21.1.248, and Java 21. It records exact installed versions that were audited; it is not a promise that later provider releases are compatible.

## Authority rule: FACT is not BELIEF

> MCA Reborn × Vampirism Compat remains authoritative for factual supernatural MCA mechanics. Dark Folklore observes those mechanics and implements knowledge, investigation, rumor, reputation and narrative consequences around them.

- **FACT** means provider-owned state: vampire/werewolf/hunter identity, infection, conversion, cure, inheritance, target/navigation/native AI, and provider conversion-source UUID.
- **BELIEF** means Dark Folklore state: what an observer suspects or knows, rumor confidence, reputation, evidence, investigation progress, stories, and contracts.

Belief never writes, substitutes for, or resolves a missing fact. For MCA entities, `CompatibilityManager` routes vampire/werewolf/hunter queries only to the exact MCA Vamp Compat factual adapter. It does not fall through to the generic Vampirism adapter. The provider being absent yields `NOT_APPLICABLE`; an untested, partial, unsupported, or failed authority yields `UNKNOWN`; an active provider's unexpected not-applicable result is also treated as `UNKNOWN`.

For non-MCA entities, independent applicable adapters aggregate conservatively: `TRUE` wins, then `UNKNOWN`, then `FALSE`, otherwise `NOT_APPLICABLE`.

## Status model

`CompatibilityManager` first records all discovered versions, then initializes optional components. This prevents an early adapter failure from hiding the real status of another component.

| Runtime status | Meaning |
| --- | --- |
| `ACTIVE` | Exact audited version/signatures are active. For MCA Vamp Compat, all facts, predation, and lifecycle components initialized. |
| `DISABLED` | Optional provider is absent. Core continues without it. |
| `UNTESTED_VERSION` | Installed version differs; version-sensitive code is not loaded. |
| `PARTIAL` | At least one MCA Vamp Compat component initialized but not all three. Factual routing still reflects the fact component's own status. |
| `UNSUPPORTED` | An exact add-on cannot be activated because another required member of its audited stack is not exact/available. |
| `ERROR` | Exact-version code was selected but required classes/methods failed validation or a component circuit opened. Queries fail closed. |

Data-driven optional entries use `required: false` and remain safe when a provider is absent. They are not automatically proven compatible by an `ACTIVE` detection row; registry IDs and semantics must be re-audited after an update.

## Exact MCA vampire component gate

The factual, predation, and lifecycle components are eligible only when all three versions match:

| Required component | Exact version |
| --- | --- |
| Vampirism | `1.10.12` |
| MCA Reborn | `7.7.32+1.21.1` |
| MCA Reborn × Vampirism Compat | `2.0.12` |

Once the triple gate succeeds, each component initializes independently:

- **facts**: authoritative vampire/werewolf/hunter queries and conversion-source provenance;
- **predation**: provider snapshots, real feeding actions, cooldown semantics, and exact event observation;
- **lifecycle**: infection/conversion/cure/inheritance snapshots and provider-native AI repair.

One component failure does not suppress a healthy sibling component. The combined add-on report is `ACTIVE`, `PARTIAL`, or `ERROR`; factual queries use the factual component status rather than the combined label.

Runtime/linkage failures open the affected optional bridge's circuit, return unknown/unavailable state, and log once instead of throwing repeatedly. Server stop clears bounded correlation and observation caches.

## Audited pack matrix

| Mod ID | Exact audited version | Mechanism | Coverage in 0.4.0 |
| --- | --- | --- | --- |
| `vampirism` | 1.10.12 | Typed public faction API in an isolated adapter | Generic non-MCA vampire/hunter facts; canonical garlic/blood semantics; weakness integration; real wild-vampire blood drain. It is not an MCA fact fallback. |
| `werewolves` | 2.0.3.3 | Vampirism public faction registry, native tags, exact wolfsbane bridge | Generic non-MCA werewolf facts; native silver semantics; canonicalization behavior. Exact Enchanted 4.2.7 preserves diffuser, contact, finder, and crop semantics for canonical wolfsbane. |
| `mca` | 7.7.32+1.21.1 | Cached exact-version implementation reads | Read-only relationships, hearts/personality context, and ordinary MCA identity for social trust. It never alters MCA state and never guesses supernatural identity. |
| `mcacapitals` | 1.1.0 | Cached exact-version read-only implementation bridge | Verified offices map to political roles and configurable weights. The 1,024-entry Core DTO cache expires after 20 ticks; Core never writes monarchy, capital, village, family, or succession state. |
| `mca_vamp_compat` | 2.0.12 | Triple-gated exact reflection with independent fact/predation/lifecycle components | Authoritative MCA supernatural facts, loaded-entity lifecycle observation, provider-native action evidence, provider provenance, and provider-owned native AI extension. No duplicate infection, cure, inheritance, target/navigation, trade, marriage, or village-capture system. |
| `fieldguide` | 1.14.0 | Public unlock type and server progress implementation | Seven curated categories, ten explicit provider-backed entries, EN/IT text, native scan/kill participation, bounded unlock-to-Core polling, and `OBSERVED` forwarding to an existing page. Field Guide owns UI, notes, photos, and binary progress. |
| `enchanted` | 4.2.7 | Registry IDs, tags, canonical loot/worldgen routing, exact wolfsbane pairing | Enchanted owns canonical farmable wolfsbane. New audited acquisition routes avoid the legacy flower while existing stacks remain. Garlic loot can route to Vampirism garlic. Mandrake, altars, and poppets remain distinct. |
| `occultism` | 1.224.2 | Registry IDs, tags, loot/worldgen data | Config-gated silver routing, spirit/soul/ritual traits, distinct golem semantics. No Java adapter. |
| `malum` | 1.8.2 | Registry IDs and tags | Soul traits and distinct poppet semantics. No Java adapter. |
| `eidolon_repraised` | 0.5.0.2 | Registry IDs, tags, loot/worldgen data | Config-gated silver routing, holy/soul traits, curated wraith profile, distinct altar/wraith semantics. No Java adapter. |
| `feywild` | 5.5.5 | Registry IDs and tags | Fae classification, curated Sprite investigation/Field Guide page, and distinct mandrake semantics. No Java adapter. |
| `betterarcheology` | 1.21.1-1.3.8 | Optional item tags and standard pickup event | Four audited artifacts can grant one archaeology discovery. |
| `quest_giver` | 1.5.1 | Detection only | Optional future presentation integration; Dark Folklore owns contract state and has no Quest Giver bridge in 0.4.0. |
| `almostunified` | 1.21.1-1.4.2 | Existing pack configuration | Recipe unification delegates silver output to Immersive Engineering; Core owns only its separately configurable loot/worldgen/spawn routing. |

The manager also reports data-only/detection-only components. For those rows, `ACTIVE` means “exact audited pack component found,” not “a private Java adapter is running.”

## Code-bound JAR identity

| JAR | SHA-256 |
| --- | --- |
| `Vampirism-1.21-1.10.12.jar` | `C6DCCA1AF24DECA473A24470CCAB66053D3AA3324E453B4E1697090ED6D16BE2` |
| `Werewolves-1.21-2.0.3.3.jar` | `ECBCA2CD344E24AD48157834A8F321D1A7D2221C727FE8E61E4436D1219C6CFB` |
| `mca-neoforge-7.7.32+1.21.1.jar` | `874B5BD82D754033117EE6C1E7B5EBD142EC5DC0DF2881C9BB2F38A05AE7F4AB` |
| `mcacapitals-1.1.0.jar` | `73AF01FAE88C9698D93EF0372854EA57373EFA0276C0CB33CC38FEFEEDED7B56` |
| `mca-vamp-compat-1.21.1-2.0.12.jar` | `BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF` |
| `enchanted-neoforge-1.21.1-4.2.7.jar` | `205A3E1EDB7E53E9BBF3B8AB965AC5EB3840BC43760E1B003200E4735EBF1BB0` |
| `fieldguide-neoforge-1.21.1-1.14.0.jar` | `00B26B1351CB85B90ED86675C49BFC054A3141BEDAE22358D5A6AD4FE7CB0740` |

## Provider ownership and predation boundary

MCA Vamp Compat owns infection/configuration, conversion, cure, inheritance, capability persistence, appearance normalization, target selection, navigation, native combat/infection-bite AI, social/marriage consequences, trades, and Vampirism village interactions.

Dark Folklore may score an eligible feeding candidate and retain a bounded narrative session. It never sets or clears provider/MCA target/navigation state. An MCA-human session continues only if provider-native AI independently selects the same target. Core observes a successful native bite only by correlating the exact same incoming-damage event, direct attacker/target pair, and provider attacker-capability ready-to-cooldown transition between `HIGHEST` and `LOWEST`. This supports provider-valid MCA, player, and vanilla-human targets without relying on an MCA-only victim marker. Provider post-success damage cancellation/zeroing does not suppress a feed proven by the cooldown transition; attempts without that transition, proximity, or session state alone are not evidence.

A cure/cleared transition cancels only Core's session. Historical witness belief is intentionally retained because a past observation does not become false when the subject is cured.

`VampiricVillagerState.getSource()` (or the equivalent werewolf source accessor) is stored as provider conversion provenance when valid. It is not necessarily a biological parent or universally reliable sire. Inherited vampirism has no fabricated conversion source; both parents remain only as bounded birth context.

See [MCA Vamp Compat 2.0.12 exact-provider audit](MCA_VAMP_COMPAT_2.0.12_AUDIT.md) and [MCA social audit](MCA_SOCIAL_AUDIT.md).

## General failure containment

- Typed optional classes are loaded only after their exact version gate succeeds.
- Reflective classes/methods are resolved at initialization, not discovered in hot event paths.
- External query failures return `UNKNOWN`/unavailable, never silently `FALSE`.
- Runtime caches hold bounded Core DTOs or same-event correlation records; they do not retain a second provider save authority.
- No compatibility Mixin or Access Transformer is used.
- Common/server initialization references no client-only class.

## Known boundaries

- Field Guide 1.14.0 has no stable unlock event used by Core; its public progress implementation is therefore exact-version-only.
- MCA relationship/personality and MCA Capitals office reads use public implementation signatures rather than a stable API namespace and remain exact-version-only.
- Optional tag entries protect absent registry IDs but do not prove that a future changed entity/item has the same semantics.
- Core loot conversion applies only to newly generated loot while canonicalization is enabled. Existing stacks and other creation paths are not migrated.
- Silver projectile enhancement applies only when Minecraft exposes a tagged weapon stack; Core does not reflect into Immersive Engineering projectile internals.
- Biome modifier toggles do not retroactively alter existing chunks or entities.
- Existing foreign items, blocks, entities, and save data are never unregistered or rewritten in place.

## Upgrade procedure

For every optional-provider update:

1. Re-run the installed-pack inventory/audit.
2. Compare JAR metadata and SHA-256.
3. Re-audit every Java-bound class and signature.
4. Validate registry IDs, tags, loot mappings, placed features, and Field Guide entry IDs.
5. Run a provider-absent dedicated-server smoke and an exact complete-pack server/client/in-world matrix.
6. Update the exact version gate and this document only after those checks.

0.4.0 remains `RELEASE_CANDIDATE`; the full manual exact-provider/client matrix is not claimed complete.
