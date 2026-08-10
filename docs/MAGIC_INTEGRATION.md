# Magic Integration

Dark Folklore does not add another mana, spell, altar, or ritual implementation. It supplies a semantic layer across existing traditions and awards one-time lore discoveries when a player's inventory demonstrates a meaningful cross-mod combination.

## Traditions

The data model recognizes:

| Tradition | Intended provider |
| --- | --- |
| `WITCHCRAFT` | Enchanted |
| `SPIRIT` | Occultism |
| `SOUL` | Malum |
| `FORBIDDEN_THEURGY` | Eidolon: Repraised and related forbidden rites |
| `FAE` | Feywild |

An integration definition must name at least two traditions and at least one required item trait. Traditions are descriptive provenance in the current implementation; gameplay matching uses the required trait set.

## Discovery behavior

When a server player picks up an item, `LoreEngine` resolves traits from the picked-up stack and all main-inventory stacks. For every loaded magic integration whose required traits are all present, it grants the configured lore reward only if that reward is still at zero points.

This is deliberately bounded:

- Evaluation occurs on item pickup, not every tick.
- The player inventory is scanned once for that event.
- The definition list is datapack-sized and immutable between reloads.
- `discoverOnce` prevents farming the same discovery for repeated points.

## Default integrations

### Blood Soul Rite

```text
id: darkfolklore:blood_soul_rite
traditions: SPIRIT, SOUL, FORBIDDEN_THEURGY
required traits: VAMPIRE_BLOOD, SOUL, RITUAL_COMPONENT
reward: 10 points of darkfolklore:forbidden_lore
```

The default tags allow, when installed, blood from Vampirism or Kaleidoscope Bloodwine; soul material from Eidolon, Occultism, or Malum; and a ritual component from Enchanted, Bloodlines, Occultism, or Eidolon.

### Fae Binding

```text
id: darkfolklore:fae_binding
traditions: WITCHCRAFT, FAE
required traits: WOLFSBANE, FAE, RITUAL_COMPONENT
reward: 8 points of darkfolklore:fae_lore
```

The default tags bridge Werewolves/Enchanted wolfsbane, Feywild materials, and selected ritual components.

Optional entries in tags use `required: false`, so a missing content mod does not invalidate tag loading.

## Adding an integration

Create `data/<namespace>/darkfolklore/magic_integrations/<name>.json`:

```json
{
  "id": "example:grave_binding",
  "traditions": ["SOUL", "FORBIDDEN_THEURGY"],
  "required_traits": ["SOUL", "SPIRITUAL", "RITUAL_COMPONENT"],
  "knowledge_reward": "example:grave_lore",
  "knowledge_points": 12
}
```

`knowledge_reward` should be namespaced. Points are clamped to 0 through 100. Extend the corresponding `darkfolklore` item tags for concrete ingredients, reload data, and verify `/folklore diagnostics` reports no invalid definitions.

## Current boundaries

- No external mod recipe, altar, rite, spell, or progression API is modified.
- The definitions do not consume items and do not prove that a ritual was performed; they recognize possession-based thematic synergy.
- Discovery is evaluated only after a pickup with at least one recognized trait. Items already present across login, direct inventory mutation, or some crafting/container paths may not trigger evaluation until another recognized item is picked up.
- Only the player's main inventory list participates; armor, offhand, curios, nested containers, and remote storage are not scanned.
- Traditions are not independently registered against provider APIs and currently do not alter matching.
- The default set contains two integrations, not an exhaustive cross-product of the installed magic mods.

Actual cross-mod rituals should be added only after auditing the exact provider recipe/codecs and confirming they do not bypass or duplicate native progression.
