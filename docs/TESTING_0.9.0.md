# Dark Folklore Core 0.9.0 — Full-Pack Test Matrix

Use the exact intended 1.21.1 instance. Standard CI is necessary but not sufficient because custom provider serializers and boss lifecycle behavior are not present there.

## Startup and resources

1. Start client and dedicated server with the complete pack.
2. Run `/reload`; require no invalid recipe/structure errors.
3. Confirm `the_day_of_the_beast:demon_invocation_structure` no longer appears in newly generated terrain. Do not delete structures already generated in old chunks.
4. Verify JEI/provider books show the intended ritual routes without replacing unrelated provider recipes.

## Demon Heart

1. Confirm the old Goat Heart + Nether Wart + Blaze Powder workbench recipe no longer crafts a Demon Heart.
2. Produce Enchanted Demonic Blood through the real Distillery path and Nether Chalk through the real Kettle path.
3. Use the Eidolon brazier ritual with the existing Goat Heart, Enchanted components, Enchanted Ash and Crimson Essence.
4. Confirm exactly one normal provider Demon Heart is produced and has no Beast Heart marker/glint.

## Infernal masonry and frame

1. Run the Malum Spirit Infusion and verify eight provider Demon Broken Bricks.
2. Run the Occultism `craft_afrit` Demon Brick ritual and verify sixteen provider Demon Bricks.
3. Run the high-tier frame ritual and verify the provider Demon Invocation Frame.
4. Confirm no vanilla workbench route was introduced for these blocks.

## Invocation monument

1. Build the documented four-pillar/recessed-cross pattern around the provider frame.
2. Try a wrong item: no activation.
3. Try a normal Demon Heart outside Witching Hour: no activation and no heart consumption.
4. Remove one required brick, retry during Witching Hour: no activation and no consumption.
5. Restore the block and activate during world time 17500–18500.
6. Confirm exactly one provider phase-one boss appears, a persisted invocation site is created, and one Demon Heart is consumed only after successful spawn.
7. Save/restart during the fight and verify the site remains associated with the provider encounter.
8. Verify provider phase changes/AI/combat work normally and Dark Folklore does not replace them.
9. Kill/cancel/resurrect intermediate phases where possible; no Beast Heart may be generated from a non-final or non-confirmed death.
10. Defeat the final `the_day_of_the_beast:beast` associated with the invocation and verify one Beast Heart is awarded.

## Beast Heart

1. Confirm it uses the existing Demon Heart texture/base item.
2. Confirm display name is `Beast Heart` and glint is visible.
3. Confirm a renamed normal Demon Heart is rejected as a Beast Heart.
4. Confirm the Beast Heart cannot activate a Demon Invocation Frame.
5. Save/restart and confirm its stack marker persists.

## Cult of Azazel

1. Confirm the native altar still spawns/controls native Azazel exactly as the provider expects.
2. Confirm maze, keys and quota progression remain native.
3. Confirm nine Faith Parts no longer create Faith Essence on a crafting table.
4. Run the new high-tier Occultism ritual and verify the existing `netherman:faith_essence` result retains provider behavior.
5. Kill Azazel as a player and verify the Dark Folklore milestone is persisted.
6. Progress through the native quota/Maze path to `netherman:azazel_human`; defeat it and verify the True Azazel milestone persists after restart.

## Regression

Retest 0.8 legendary encounters, wards, Fae offerings, vampire predation, MCA lifecycle, Field Guide, Recipe Weaving and `/reload`. Then run a fresh `/dfatlas scan` and compare against `scan-20260813-123457`.