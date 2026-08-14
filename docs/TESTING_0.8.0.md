# Dark Folklore Core 0.8.0 — Runtime Test Matrix

0.8 remains a release candidate until this matrix is executed in the intended 1.21.1 NeoForge pack.

## Startup / persistence

1. Start client and integrated/dedicated server with 0.8.0.
2. `/reload` must complete without invalid Dark Folklore resources or third-party tag/serializer errors.
3. `/folklore diagnostics` must show expected provider statuses.
4. Seed one test encounter, save/quit, reopen, and confirm `/folklore encounter list` retains stage, origin, encounter UUID and linked story UUID.
5. Repeat after the manifestation entity unloads and reloads.
6. Confirm schema-1 legendary encounter saves upgrade safely to schema 2 and are re-saved without losing prior state.

## Encounter → investigation → contract continuity

For Wendigo, Chupacabra and Revenant:

1. Seed/create the story and accept its contract **before manifestation**.
2. Confirm subjectless omen evidence is usable only for the exact linked story, within its bounded pre-manifestation envelope.
3. Allow the provider entity to manifest.
4. Confirm the case link is promoted to that exact manifestation UUID.
5. Spawn/find another natural mob of the same canonical concept and verify it cannot satisfy the encounter contract.
6. Confirm post-manifestation evidence carrying the exact manifestation UUID still works even though it occurs long after the story origin tick.
7. Kill the exact manifestation and verify normal contract/story completion ordering.

## Wendigo

Use `/folklore encounter seed wendigo` for deterministic QA.

Expected:
- ORIGIN/OMENS state appears before the mob;
- three omen evidence points are produced over time;
- existing investigation/occult-analysis tools can consume the evidence;
- after eligibility, a real `cnc:wendigo` appears in a loaded, world-border-valid, collision-free, sturdy-ground position at night;
- no MCA entity is removed/transformed;
- killing the exact manifestation through a finalized death resolves the encounter;
- `cnc:wendigo_head` keeps its provider behavior and can grant the research floor on pickup.

## Chupacabra

Natural path:
- only suspicious nighttime livestock deaths should build panic: no identifiable killer, or a real `cnc:chupacabra`;
- player kills and tame-animal deaths must not increment the counter;
- kills factually attributed to another known creature must not be narrated as Chupacabra evidence;
- after the configured threshold, a story/omens precede the real `cnc:chupacabra` manifestation;
- blood/scent/tracks are usable by the investigation backend;
- `cnc:chupacabra_head` grants research/proof semantics once without replacing the item.

Watch unexplained environmental deaths in the full pack and tune the threshold/window only from real observed data.

## Revenant

1. Create a violent confirmed death of a vanilla villager or MCA person by a living attacker; test both mob and player killers.
2. Repeat controlled trials or temporarily raise `revenantDeathChance` for QA.
3. Confirm origin snapshot keeps victim name/UUID and killer when available.
4. Confirm no Revenant is created from cancelled/resurrected death paths.
5. When manifested, verify a real `graveyard:revenant` is used.
6. If the original killer is loaded and nearby, verify target hint behavior does not fight Graveyard AI.
7. Save/restart before and after manifestation.

## Wild Hunt

Use `/folklore encounter seed wild_hunt` or test the natural full-moon path.

Expected:
- both captain and follower provider entity IDs must exist before the event is admitted;
- occult/spirit omens occur first;
- captain is `occultism:wild_hunt_wither_skeleton`;
- followers are real `occultism:wild_hunt_skeleton` entities;
- group UUIDs survive save/restart and remain bounded;
- members without combat targets move in the same broad regional direction;
- combat targets remain provider AI-owned;
- no forced chunk loading occurs;
- provider AI overriding the movement hint must not cause crashes or tick storms.

## Wards

Create wards by sneak-right-clicking a door with an existing `RITUAL_COMPONENT` in the main hand and:

- garlic -> VAMPIRE;
- holy -> UNDEAD;
- spiritual/soul -> SPIRIT;
- fae -> FAE;
- another plain ritual component or a multi-tradition focus -> GENERAL.

Verify:
- clicking either half of one door addresses the same normalized threshold;
- every ward type has sufficient strength to reach its intended activation threshold;
- items are consumed once outside creative and only after persistence accepts the ward;
- the same creator can refresh/change a ward on the same threshold without stacking rows;
- another player cannot overwrite an active foreign ward;
- an expired foreign ward can be replaced;
- a full ward store never evicts another ward silently;
- `/folklore ward list` shows type/radius/strength/expiry;
- save/restart preserves the ward;
- GENERAL can delay Wendigo/Chupacabra;
- SPIRIT or UNDEAD can delay Revenant/Wild Hunt;
- VAMPIRE cancels only provider-valid wild-Vampirism prey targeting inside protection;
- tame animals remain excluded;
- an MCA vampire remains provider-owned and is not target/navigation-controlled by the ward guard.

## Fae bargains

1. Sneak-right-click an existing flower with each allowed existing offering.
2. With no valid nearby growable block, verify **nothing** is consumed and no cooldown/lore/reputation is granted.
3. With valid growth nearby, verify at most eight loaded `BonemealableBlock`s advance using their real growth method.
4. Verify one successful offering is consumed.
5. Milk bucket must return an empty bucket; honey bottle must return a glass bottle.
6. Verify regional cooldown.
7. Test Farm & Charm / Meadow / Bakery crops and offerings from the actual pack where applicable.
8. Confirm no chunk loading, provider crop crashes or unsupported growth loops.

## L2 Hostility — intentionally disabled

Run `/folklore encounter l2`.

Expected in **all** 0.8 configurations:

- reports `DISABLED_BY_DARKFOLKLORE_POLICY`;
- may report whether L2 is installed, but performs no mutation;
- no encounter level is changed by Dark Folklore;
- no L2 trait is added/removed by Dark Folklore;
- changing an old config entry to `l2HostilityScaling=true` must still have no effect.

Do not treat L2 as part of the 0.8 gameplay acceptance matrix. A future release may re-enable it only after an explicit exact-JAR/API audit.

## Investigation / contracts

For Wendigo, Chupacabra and Revenant:
- story is visible as a real persisted Core story;
- local villager/MCA issuer can offer the contract when eligible;
- target is hidden until the normal evidence threshold;
- encounter omen evidence and existing occult analysis can contribute;
- pre-manifestation evidence cannot cross-link another same-concept story;
- after manifestation, hunt validation uses the exact persisted culprit UUID rather than any same-concept natural mob;
- completion rewards and village/organization consequences remain unchanged.

Wild Hunt is deliberately not a normal contract source.

## Final audit

Run:

```text
/dfatlas scan
```

Compare with the last intended-pack baseline for:
- mod/version matrix;
- invalid resources;
- item/entity concepts;
- tags;
- recipe graph;
- duplicate acquisition paths;
- provider entity IDs used by 0.8.
