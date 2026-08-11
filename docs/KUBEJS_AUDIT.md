# KubeJS Audit

## Installed runtime

| Component | Installed version |
| --- | --- |
| KubeJS | 2101.7.2-build.368 |
| Rhino | 2101.2.7-build.85 |
| KubeJS Eidolon | 1.3.1 |

The audit covered every file currently under kubejs. No file was deleted or edited by Dark Folklore Core.

## Script inventory

| Script | Current purpose | Game-state effect | Superseded by Core? | Recommendation |
| --- | --- | --- | --- | --- |
| kubejs/client_scripts/main.js | Stock example; logs “Hello, World! (Loaded client example script)”. | None. It registers no tooltip, UI, JEI/EMI, resource, or event behavior. | No functionality existed to supersede. | Safe to remove after pack-owner approval; retained unchanged in this release. |
| kubejs/server_scripts/main.js | Stock example; logs “Hello, World! (Loaded server example script)”. | None. It changes no recipe, tag, loot table, spawn, or server event. | No functionality existed to supersede. | Safe to remove after pack-owner approval; retained unchanged in this release. |
| kubejs/startup_scripts/main.js | Stock example; logs “Hello, World! (Loaded startup example script)”. | None. It registers no item, block, fluid, entity, or custom component. | No functionality existed to supersede. | Safe to remove after pack-owner approval; retained unchanged in this release. |

## Non-script inventory

The KubeJS folder also contains the generated README, default KubeJS configuration, and two example images:

- kubejs/assets/kubejs/textures/block/example_block.png
- kubejs/assets/kubejs/textures/item/example_item.png

No script registers or references an example block/item, so the images have no known gameplay consumer. They were retained because this audit is non-destructive.

The common configuration has server_only=false, hide_server_script_errors=false, announce_reload=true, and startup_error_gui=true. These are normal development-friendly settings; Dark Folklore does not depend on them.

## Ownership decision

Stable foundational behavior now belongs to Java or reloadable data in Dark Folklore Core:

- canonical concept resolution;
- optional-mod detection and exact-version adapters;
- semantic item/entity traits;
- weakness calculation and native-provider protection;
- config-aware canonical loot and worldgen routing;
- config-aware duplicate natural-spawn suppression plus independent encounter directing;
- persisted lore, social knowledge, rumors, organizations, village state, lineage, evidence, contracts, and stories;
- Field Guide synchronization.

None of that logic was migrated from KubeJS; the supplied scripts never implemented it. The distinction matters because there is no hidden legacy behavior to preserve.

## Recommended future use

KubeJS can remain installed for pack-local prototyping and low-risk presentation tweaks, for example:

- temporary recipe balance experiments;
- tooltip copy;
- short-lived event tuning during playtests;
- integration experiments before a stable data/Java implementation is accepted.

It should not become a second authority for canonical concepts, supernatural identity, saved social state, contract state, weakness multipliers, or spawn suppression. Duplicating those systems in scripts would create ordering, persistence, and dedicated-server ambiguity.

KubeJS Eidolon is currently unused. Its presence is not evidence that Eidolon integration is script-owned; the Core's present Eidolon integration uses ordinary registry IDs, tags, loot/worldgen data, and standard events.

## Safe cleanup plan

No automatic cleanup is required for 0.2.0. If the pack owner chooses to remove the examples later:

1. Confirm logs/kubejs contain only the three Hello World messages and no undeclared generated script.
2. Remove the three example main.js files and two example PNGs in a dedicated, reviewable change.
3. Launch client and dedicated server once with KubeJS still installed.
4. If KubeJS/Rhino/KJSEidolon are candidates for removal entirely, first audit every mod dependency and pack workflow; this document does not authorize removing their JARs.

## Audit conclusion

Current KubeJS functionality is example-only and nonessential. It remains untouched, available for future prototyping, and outside the authoritative Dark Folklore architecture.
