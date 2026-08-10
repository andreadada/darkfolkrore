# Spawn and Encounter Director

The director makes selected supernatural entities feel like encounters without taking ownership of another mod's summoning or progression. It is a server-side filter over existing spawn attempts; it does not register new biome spawns or manufacture targets for contracts. A supplemental NeoForge biome modifier also removes the tagged noncanonical Mobs of Mythology chupacabra from Overworld spawn lists.

## Spawn decision

`SpawnDirector` listens to `MobSpawnEvent.PositionCheck` and acts only when `MobSpawnType` is `NATURAL`. Ritual, spawner, structure, command, spawn-egg, conversion, and other non-natural reasons are not denied by this filter.

For an entity with a loaded spawn profile, the decision is:

```text
profile missing                         -> leave the provider's decision unchanged
natural_spawn_enabled = false           -> deny
nocturnal = true and level is not night -> deny
base rarity chance
  x naturalSpawnMultiplier
  x eventMultiplier if any core world event is active
  x encounter-pressure factor
random roll                             -> allow or deny
```

The result is capped at probability `1.0`. The encounter-pressure factor is `max(0.2, 1 - pressure/125)` and is applied only when an online player is found within 128 blocks. This preserves a floor rather than making rare encounters impossible.

## Rarity values

| Rarity | Base natural chance |
| --- | ---: |
| `COMMON` | 1.00 |
| `UNCOMMON` | 0.65 |
| `RARE` | 0.30 |
| `VERY_RARE` | 0.12 |
| `LEGENDARY` | 0.04 |

These values filter attempts already produced by the owning mod. They are not absolute mobs-per-chunk rates.

## Default curated profiles

| Entity | Rarity | Natural | Night only | Event multiplier |
| --- | --- | --- | --- | ---: |
| `cnc:chupacabra` | `RARE` | yes | yes | 1.25 |
| `cnc:wendigo` | `VERY_RARE` | yes | yes | 1.50 |
| `eidolon_repraised:wraith` | `RARE` | yes | yes | 1.40 |
| `fangs_n_claws:ghost` | `VERY_RARE` | no | yes | 1.00 |
| `fangs_n_claws:imp` | `VERY_RARE` | no | no | 1.00 |
| `fangs_n_claws:werewolf` | `LEGENDARY` | no | yes | 1.00 |
| `mobs_of_mythology:chupacabra` | `VERY_RARE` | no | yes | 1.00 |
| `mythsandlegends:imp` | `RARE` | yes | no | 1.15 |

The disabled profiles suppress duplicate natural implementations while leaving deliberate non-natural access paths alone. The `#darkfolklore:noncanonical_natural_spawn` entity-type tag currently contains `mobs_of_mythology:chupacabra` and feeds the static `neoforge:remove_spawns` biome modifier. A profile can be replaced by a higher-priority datapack without changing code.

## Encounter pressure

When any profiled `RARE`, `VERY_RARE`, or `LEGENDARY` entity successfully joins a server level, the nearest online player within 128 blocks receives 15 pressure. This join accounting currently includes non-natural spawn reasons even though filtering applies only to natural spawns.

Pressure for each online player decreases by one at an interval of `max(1, encounterCooldownTicks / 100)`. With the default 12,000-tick cooldown, that is one point every 120 ticks; a maximum pressure value of 100 therefore relaxes over approximately the configured period. Pressure is stored by player UUID in versioned server save data and survives restart. The work is bounded by the number of online players; there is no entity or chunk scan.

The `encounterDirector` switch controls pressure accounting and use. `spawnDirector` controls natural filtering. `naturalSpawnMultiplier` defaults to `1.0` and accepts `0.0` through `4.0`.

## World events

The core evaluates each loaded level every 200 ticks:

- `FULL_MOON` is active at night when the vanilla moon phase is zero.
- `WITCHING_HOUR` is active from day time 17,500 through 18,500 inclusive.

Starting or ending an event is logged and emits `WorldEventChangedEvent`. If one or both events are active, a profile's `eventMultiplier` is applied once. Events are recomputed state, not persisted state.

## Adding a profile

Create `data/<namespace>/darkfolklore/spawn_profiles/<name>.json`:

```json
{
  "entity": "examplemod:night_thing",
  "rarity": "VERY_RARE",
  "natural_spawn_enabled": true,
  "nocturnal": true,
  "event_multiplier": 1.5
}
```

Allowed rarities are `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`, and `LEGENDARY`. `event_multiplier` must be finite and in the range 0 through 10. Reload with `/reload` and verify the loaded profile and invalid-definition counts with `/folklore diagnostics`.

## Current boundaries

- There are no biome, weather, village-distance, local-density, or dimension predicates yet.
- The director does not add missing spawn placements and cannot make an entity appear if its provider never attempts a spawn.
- The static remove-spawns modifier is Overworld-scoped and currently contains only the audited duplicate chupacabra; Java profiles remain the broader runtime filter.
- Pressure decays only while the affected player is online; an offline player's persisted pressure resumes relaxing after login.
- There is no per-profile logging or visual overlay for denied attempts; normal operation stays quiet.
- A rare command/spawner/ritual spawn can add encounter pressure even though that spawn is never suppressed.

These boundaries are intentional release notes, not claims that all pack spawning is canonicalized.
