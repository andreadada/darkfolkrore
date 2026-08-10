DARK FOLKLORE ATLAS - SCAN OUTPUT
=================================

This folder is a read-only snapshot of the content actually loaded by Minecraft/NeoForge.
Send the WHOLE folder (preferably zipped) for analysis. report.html is only a human-readable overview.

Atlas 0.2 adds:
- semantic duplicate/concept detection (e.g. wolfsbane_flower vs wolfsbane)
- reverse references from server resources to items/entities
- per-item/per-entity usage evidence
- recipe probe quality fields for modded/custom serializers
- concept groups and a canonicalization review matrix
- replacement-risk hints (LOW/MEDIUM/HIGH)

Important limitations:
- A registry/recipe/tag/resource scan cannot prove that another mod does not hardcode an exact Item/Entity reference in Java.
- Canonicalization rows are suggestions only; Atlas never replaces content automatically.
- Runtime/scripted/custom mechanics may not be fully represented by Recipe#getIngredients/getResultItem.
- HIGH replacement risk means "audit source/API before replacement", not "do not replace".
- resources.json indexes effective server data resources; it is not source-code analysis.

Generated: 2026-08-10T14:35:08.128059700Z
Atlas version: 0.2.0
Mods: 147
Items: 5346
Blocks: 3135
Entities: 453
Recipes: 6048
Candidate duplicate items: 99
Candidate duplicate entities: 18
Item concept groups: 99
Entity concept groups: 19
Canonicalization rows: 117
