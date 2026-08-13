# Dark Folklore 0.9 — Forbidden Endgame

0.9 connects The Day of the Beast and Cult of Azazel to the pack's existing magical progression without registering new Dark Folklore items, blocks, entities, sounds, textures or models.

## The Day of the Beast

The natural `the_day_of_the_beast:demon_invocation_structure` placement is disabled for newly generated terrain. The structure and every provider block remain registered, so old worlds and provider references are not destructively migrated.

The vanilla workbench path to `the_day_of_the_beast:demon_heart` is replaced under the provider recipe id by an Eidolon Repraised brazier ritual. The existing Goat Heart is the reagent; Enchanted Demonic Blood and Nether Chalk plus Eidolon Enchanted Ash and Crimson Essence provide the ritual context.

Infernal construction is deliberately multi-discipline:

1. Malum Spirit Infusion creates batches of provider Demon Broken Bricks using infernal/wicked spirits and existing Enchanted/Eidolon reagents.
2. A high-tier Occultism Afrit crafting ritual stabilizes those into batches of provider Demon Bricks.
3. A more expensive Occultism Afrit ritual creates the provider Demon Invocation Frame.
4. The player builds the Dark Folklore invocation pattern physically around that frame.
5. The frame validates the structure only when interacted with. No world-wide multiblock scan runs.
6. A normal provider Demon Heart is accepted only during the configured witching-hour window. A Beast Heart is explicitly rejected.
7. On successful validation the heart is consumed and the real provider phase-one entity is spawned. Dark Folklore does not replace its combat AI.
8. Later provider boss phases joining near the active site are associated with the same persisted invocation.
9. Only confirmed final `the_day_of_the_beast:beast` death for that invocation completes the site and creates the Beast Heart reward.

The Beast Heart is not a new registry item. It is `the_day_of_the_beast:demon_heart` with Dark Folklore stack data, the custom name `Beast Heart`, and enchantment glint. The marker rather than the display name is authoritative.

## Cult of Azazel

Cult of Azazel's altar, quota system, Azazel AI, True Azazel AI, keys, maze and native progression remain provider-owned.

Dark Folklore changes only the acquisition path of `netherman:faith_essence`: the nine existing Faith Parts now feed a high-tier Occultism ritual instead of a 3x3 workbench recipe. Existing Faith Essence runtime behavior is untouched.

Confirmed player kills of `netherman:azazel` and `netherman:azazel_human` become persisted endgame milestones. This lets later forbidden-tier integrations require real boss progression without fabricating a replacement boss or cult state.

## Persistence and safety

`ForbiddenEndgameSavedData` is bounded to 128 invocation sites and 4096 milestone-bearing players. Invocation participants are bounded to 16 UUIDs. Terminal sites are eventually pruned using the existing narrative history retention window.

The Day of the Beast provider item and entity registry entries are never removed. Natural structure placement is suppressed rather than deleting the structure. The Demon Heart is consumed only after entity lookup, collision, structure validation and persisted-site admission all succeed.

## Configuration

A separate `darkfolklore-endgame.toml` exposes:

- `forbiddenEndgame`
- `dayOfTheBeastEndgame`
- `cultOfAzazelEndgame`
- `demonInvocationRequiresWitchingHour`

## Release boundary

The provider-less CI validates Java, JSON syntax, Core reload and GameTests but cannot execute Eidolon, Malum, Occultism, The Day of the Beast or Cult of Azazel serializers/AI because those optional provider jars are intentionally absent from standard CI. Full-pack validation remains mandatory before promotion.