# DARK FOLKLORE — FINAL HANDOFF

_Last updated: 2026-08-11_

> **Historical design archive:** this file preserves the 0.2.0 handoff and long-form project direction. For the current integrated 0.4.0 state, use the [0.4.0 handoff](docs/HANDOFF_0.4.0.md), [release gate](docs/RELEASE_0.4.0.md), and [testing record](docs/TESTING_0.4.0.md).

This file summarizes the complete design direction for the Dark Folklore Minecraft 1.21.1 NeoForge project so the work can be resumed if the original conversation is lost.

## 0.2.0 implementation status

Sections below preserve the original design/research direction, including some “possible,” “suggested,” and “requires audit” language. For implemented behavior, exact compatibility boundaries, and remaining gaps, the repository documents are authoritative: [README](README.md), [Architecture](docs/ARCHITECTURE.md), [Compatibility](docs/COMPATIBILITY.md), [Testing](docs/TESTING.md), and [Known Limitations](docs/KNOWN_LIMITATIONS.md).

The 0.2.0 implementation has resolved the major open work described by this handoff:

- schema-2 living society includes relationship/personality/political trust, family reactions, bounded organizations, public reveal, controlled false accusation, twelve story templates, contracts/testimony, and operator diagnostics;
- Enchanted 4.2.7 is the canonical farmable wolfsbane, with an exact Werewolves 2.0.3.3 bridge for diffuser, contact effect, finder, recipes, and legacy-stack safety;
- Field Guide 1.14.0 receives six curated categories, nine explicit entries, English/Italian text, and binary-progress/Core-lore synchronization;
- 53 JUnit tests and three live GameTests pass; mandatory-only, exact-adapter, curated headless, final dedicated-server lifecycle, and fresh-world smokes have recorded evidence;
- two clean builds produced the identical audited `darkfolklore-core-0.2.0.jar` (355,749 bytes; SHA-256 `CCA1A4FE4F3D53A6F891FE05F51095EEE26A048CD6E850738100A4423176EBC7`).

Release classification is **`RELEASE_CANDIDATE`**. Graphical client startup with Core 0.2.0 and the curated 23-JAR set reached the title state and exited cleanly, but no world/UI gameplay validation or authentic 0.1-world upgrade has been completed. Curated dedicated-server runs also contain one unowned, nonfatal NeoForge `RuntimeDistCleaner` request for the client `Screen` class; no Dark Folklore common/server class was identified as its owner.

---

# 1. Project goal

Build a Minecraft 1.21.1 NeoForge modpack that feels like **one coherent dark-folklore supernatural RPG/life-sim**, rather than many unrelated mods installed together.

Themes:

- vampires;
- werewolves;
- witches;
- spirits;
- fae;
- cryptids;
- mythology;
- undead;
- monster hunting;
- archaeology/lore;
- MCA families and society;
- secrets, rumors and witnesses;
- Witcher-like investigation;
- restrained early-industrial technology.

The objective is **coherence**, not maximum mod count.

---

# 2. Core philosophy

Dark Folklore Core is primarily:

> **compatibility + orchestration + cross-mod gameplay + social simulation**

It should not become a random content dump.

Existing mods remain authorities over what they already do well.

Examples:

- Vampirism = vampires/hunters.
- Werewolves = werewolf mechanics.
- Enchanted = folk witchcraft.
- Occultism = spirit/summoning.
- Malum = soul magic.
- Eidolon = forbidden/theurgic ritualism.
- Feywild = fae.
- MCA = family/social identity.
- MCA Vamp Compat = factual supernatural MCA integration.
- Field Guide = bestiary/discovery UI.
- Almost Unified = material unification.
- Immersive Engineering = early-industrial human technology.

Dark Folklore links them.

---

# 3. Important current mod ecosystem

Major relevant mods include:

## Vampirism
- Vampirism 1.10.12
- Bloodlines 3.0.8
- Werewolves 2.0.3.3
- Vampiric Ageing
- Vampirism Rings
- Vampires Need Umbrellas
- Harsher Sun
- Vampires Delight
- Immersive Vampirism
- Vampirism Integrations
- MCA Vampirism Compat 2.0.12
- Kaleidoscope Bloodwine

## Magic
- Enchanted 4.2.7
- Occultism 1.224.2
- Malum 1.8.2
- Gaze
- Malstone
- Malum Arma
- Vestis
- Eidolon Repraised 0.5.0.2
- Feywild 5.5.5

## Creatures
- Critters n' Crawlers
- Fangs 'n Claws
- The Graveyard
- Mobs of Mythology
- Myths & Legends
- MOTS / Myths of the Sea

## Society
- MCA Reborn 7.7.32
- MCA Social Expansion
- MCA Capitals
- Townstead
- Quest Giver

## Discovery
- Field Guide 1.14.0
- Modonomicon
- Modopedia
- Patchouli
- Better Archaeology

## Other
- Almost Unified
- Immersive Engineering
- Farmer's Delight
- KubeJS
- Rhino
- KJSEidolon
- Vanity ecosystem

Exact supplied JARs and Atlas scan always override this snapshot.

---

# 4. Dark Folklore Atlas

A development-only NeoForge mod called **Dark Folklore Atlas** was created.

Purpose:

> Inspect the real loaded modpack before changing anything.

Atlas scans:

- mods;
- items;
- blocks;
- entities;
- effects;
- fluids;
- attributes;
- biomes;
- structures;
- tags;
- recipes;
- recipe producers;
- recipe consumers;
- JSON/server resource references;
- possible duplicate items;
- possible duplicate entities;
- canonicalization candidates.

Atlas 0.2 adds semantic duplicate grouping and canonicalization reports.

It remains read-only.

It should generally not ship in the player release.

---

# 5. Atlas results / canonicalization direction

Known important findings:

## Silver
Several mods add silver, but Almost Unified already hides/suppresses many duplicate forms and Immersive Engineering appears to be the preferred visible ecosystem.

Conclusion:

- do not duplicate ore unification;
- Dark Folklore should care about semantic traits such as `SILVER` and `SILVER_WEAPON`.

## Werewolf
Canonical gameplay implementation:

> Werewolves mod

Fangs 'n Claws werewolf should likely have natural spawn disabled.

## Wolfsbane
Candidates:

- Enchanted wolfsbane;
- Werewolves wolfsbane.

Desired direction:
Enchanted is a candidate visible/canonical herb.

But Werewolves' wolfsbane is deeply integrated.

Resolved in 0.2.0: Enchanted owns the canonical farmable flower/crop/seed path, while an exact-version bridge preserves the audited Werewolves mechanics and existing legacy stacks.

Possible final policies:

- FULL_CANONICALIZATION;
- INTEROPERABILITY_ONLY;
- KEEP_DISTINCT;
- DEFERRED_UNSAFE.

## Garlic
Candidates:

- Enchanted garlic;
- Vampirism garlic.

Vampirism garlic is deeply integrated and appears to use specialized Java behavior.

Likely canonical:

> Vampirism garlic

Enchanted should accept canonical garlic where safe.

## Mandrake
Candidates:

- Enchanted;
- Feywild.

Current candidate:

> Enchanted.

## Blood
Candidates include Vampirism and Kaleidoscope Bloodwine.

Investigate whether Vampirism blood can become the pack-wide semantic blood concept.

## Chupacabra
Candidates:

- Critters n' Crawlers;
- Mobs of Mythology.

Current preference:

> Critters n' Crawlers

pending visual/gameplay comparison.

## Imp
Candidates:

- Myths & Legends;
- Fangs 'n Claws.

Current preference:

> Myths & Legends

pending quality check.

## Ghost
Fangs 'n Claws ghost is likely redundant.

## Wraith
Eidolon and Graveyard wraiths may be distinct enough to keep both.

Do not merge generic same-name concepts automatically.

---

# 6. Canonicalization strategy

Never blindly unregister foreign items/entities.

Safe pattern:

```text
legacy registry entry remains
normal acquisition disabled if appropriate
worldgen suppressed if appropriate
recipes redirected
loot redirected
trades redirected where safe
canonical tags used
hardcoded mechanics bridged by compatibility adapter
```

Existing player items must not silently disappear.

Canonical concepts are semantic identities, not necessarily new Minecraft registry objects.

---

# 7. Core architecture

Suggested top-level architecture:

```text
darkfolklore/
    api/
    compat/
    canonical/
    traits/
    creature/
    weakness/
    knowledge/
    society/
    spawn/
    encounters/
    reputation/
    contracts/
    magic/
    diagnostics/
    network/
    persistence/
    data/
```

Compatibility adapters should be isolated per mod.

Important adapters:

- Vampirism;
- Werewolves;
- MCA;
- MCA Vamp Compat;
- Enchanted;
- Occultism;
- Malum;
- Eidolon;
- Feywild;
- Field Guide.

---

# 8. Item Traits

Dark Folklore should classify items semantically.

Possible traits:

```text
SILVER
SILVER_WEAPON
COLD_IRON
WOOD
WOODEN_STAKE
HOLY
FIRE
SUNLIGHT
WOLFSBANE
VERVAIN
GARLIC
SPIRITUAL
SOUL
CURSED
FAE
VAMPIRE_BLOOD
MONSTER_PART
RITUAL_COMPONENT
RITUAL_WEAPON
```

Prefer tags/data-driven definitions.

---

# 9. Creature Classification

Possible creature traits/categories:

```text
VAMPIRE
WEREWOLF
HUNTER
WITCH
FAE
UNDEAD
GHOST
REVENANT
SPIRIT
DEMON
SHAPESHIFTER
CRYPTID
MYTHICAL_BEAST
CONSTRUCT
MONSTER
SUPERNATURAL
```

Entities may have multiple classifications.

---

# 10. Weakness / Resistance Engine

Goal:

> Cross-mod semantic interoperability.

Examples:

- werewolves react to silver weapons regardless of which compatible mod supplied them;
- fae can react to cold iron;
- spirits can react to spiritual/holy weapons;
- undead can react to configured holy/fire traits.

Do not double-apply weaknesses already correctly handled by source mods.

---

# 11. Two Knowledge Systems

This is a crucial distinction.

## Lore Knowledge

> What does the player know about a creature or magic tradition?

Examples:

```text
MONSTER_LORE
WITCHCRAFT
SPIRIT_MAGIC
SOUL_MAGIC
FORBIDDEN_LORE
FAE_LORE
```

Per creature:

```text
Wendigo 37
Vampire 82
```

Possible stages:

```text
UNKNOWN
DISCOVERED
OBSERVED
STUDIED
MASTERED
```

## Social Knowledge

> What does one specific NPC/person know about another person's supernatural secret?

Example:

```text
Caroline actually IS a vampire.

Damon   -> CONFIRMED
Elena   -> SUSPECTED
Matt    -> UNKNOWN
Sheriff -> RUMOR
```

Possible states:

```text
UNKNOWN
RUMOR
SUSPECTED
CONFIRMED
PUBLIC
```

Facts and beliefs must be stored separately.

---

# 12. Field Guide

Field Guide is the bestiary/discovery frontend.

Suggested categories:

```text
Vampires
Werewolves
Undead
Spirits
Fae
Witches & Occultists
Cryptids
Mythical Beasts
Sea Horrors
Constructs
Unknown
```

Field Guide = **WHAT EXISTS**

Modonomicon = **HOW SYSTEMS WORK**

Do not build a duplicate bestiary GUI unless Field Guide proves insufficient.

---

# 13. MCA Vamp Compat audit discovery

A direct JAR audit showed that `MCA Reborn × Vampirism Compat 2.0.12` already does much more than originally expected.

It appears to implement:

- MCA vampire state;
- infection;
- conversion;
- cure;
- vampire bite/feeding;
- sunlight behavior;
- hunters;
- hunter alignment;
- werewolf state;
- werewolf infection/conversion;
- AI;
- trade integration;
- social integration;
- marriage handling;
- inheritance;
- mixed vampire/werewolf families;
- village capture integration;
- village faction control;
- vampire threat response;
- defensive villagers.

It keeps MCA villagers as MCA entities and attaches supernatural state rather than replacing them.

Therefore:

> Dark Folklore must NOT rebuild MCA × Vampirism compatibility.

Instead:

```text
MCA Vamp Compat:
"Caroline IS a vampire."

Dark Folklore:
"Who knows Caroline is a vampire?"
```

Use an isolated `McaVampCompatAdapter`.

Do not treat its internals as globally stable API.

---

# 14. Dark Folklore Society

This became a major first-class subsystem.

The Society layer adds:

- social knowledge;
- secrets;
- suspicions;
- witnesses;
- rumors;
- fear;
- loyalty;
- reputation;
- organizations;
- village social state;
- bloodline social graphs;
- emergent stories.

It does **not** duplicate actual supernatural state.

---

# 15. Witness System

Meaningful supernatural events can create witnesses.

Examples:

- vampire feeding;
- vampire reveal;
- werewolf transformation;
- supernatural attack;
- suspicious corpse;
- visible ritual.

Witness checks should be event-driven.

Possible factors:

- distance;
- line of sight;
- sleeping;
- blindness/perception;
- disguise;
- relationship;
- event obviousness.

Examples:

```text
sees feeding directly
-> CONFIRMED
```

```text
hears scream
-> RUMOR / suspicion
```

```text
finds drained corpse
-> suspicion
```

---

# 16. Rumor System

Rumors propagate through social relationships.

Example:

```text
Anna knows secret
-> tells spouse
-> spouse gets rumor
-> tells friend
```

Rumors should:

- lose certainty when retold;
- decay;
- be reinforced;
- conflict;
- support false accusations;
- use bounded processing to avoid exponential spread.

MCA family/social relations should be reused where safe.

---

# 17. Secret Identity

Disguise should interact with individual knowledge.

Example:

```text
actual vampire + disguise

NPC A UNKNOWN
-> fooled

NPC B CONFIRMED
-> not fooled socially

NPC C SUSPECTED
-> cautious
```

Dark Folklore should extend Vampirism socially rather than destructively replacing its disguise mechanics.

---

# 18. Reputation

Potential reputation groups:

```text
VILLAGERS
HUNTERS
VAMPIRES
WEREWOLVES
WITCHES
FAE
OCCULTISTS
```

May influence:

- dialogue;
- prices;
- information;
- hostility;
- organizations;
- contracts;
- rumor credibility.

---

# 19. Organizations

Persistent social organizations may include:

```text
VAMPIRE_COVEN
HUNTER_SOCIETY
WEREWOLF_PACK
WITCH_COVEN
```

Example:

```text
Founders Council

Leader:
Mayor

Members:
Sheriff
Blacksmith
Guard

Known vampires:
A CONFIRMED

Suspected:
B SUSPECTED

Objectives:
protect village
investigate deaths
recruit hunters
```

Organizations are not replacements for Vampirism factions.

---

# 20. Bloodline / Sire Graph

The MCA Vamp Compat audit suggests conversion state may store a source UUID.

Verify before implementation.

Potential social lineage:

```text
Klaus
├── Stefan
│   └── descendant
└── Damon
```

Possible metadata:

- sire;
- descendants;
- generation;
- creation time;
- loyalty;
- rivalry;
- grudge.

Do not replace Bloodlines or Vampiric Ageing.

---

# 21. Village Society

Track social state separately from official Vampirism village faction.

Possible values:

```text
PUBLIC_AWARENESS
VAMPIRE_INFLUENCE
HUNTER_INFLUENCE
WEREWOLF_INFLUENCE
WITCH_INFLUENCE
FEAR
SUSPICION
```

Possible state:

```text
Official control:
VAMPIRE

Public awareness:
LOW
```

or:

```text
Official control:
HUMAN

Hidden vampire influence:
HIGH
```

This enables secret infiltration.

---

# 22. Dynamic Stories

Create event-driven emergent social stories.

Examples:

```text
drained animal
missing villager
body discovered
witness disappears
hunter investigation
false accusation
secret revealed
family conflict
revenge
coven recruitment
hunter recruitment
werewolf incident
ritual witnessed
vampire infiltrates family
hunter discovers supernatural relative
```

Stories should use actual NPC UUIDs/villages and persist meaningful consequences.

---

# 23. Spawn / Encounter Director

The modpack contains many supernatural entities.

The world should not become a supernatural zoo.

Rarity:

```text
COMMON
UNCOMMON
RARE
VERY_RARE
LEGENDARY
```

Consider:

- biome;
- time;
- weather;
- moon;
- dimension;
- local supernatural density;
- village distance;
- recent encounter pressure;
- spawn reason.

Do not suppress ritual/quest/boss/structure spawns by default.

---

# 24. Magic Traditions

Semantic schools:

```text
WITCHCRAFT -> Enchanted
SPIRIT -> Occultism
SOUL -> Malum
FORBIDDEN/THEURGY -> Eidolon
FAE -> Feywild
```

Do not build another generic spell system.

Dark Folklore should create interoperability between existing systems.

---

# 25. Cross-mod Magic

Potential integrations:

```text
witch herb
+ vampire blood
+ soul component
+ ritual component
```

or:

```text
Graveyard relic
+ Malum soul
+ Eidolon ritual
```

Only create integrations with thematic purpose and verified APIs/data formats.

---

# 26. Better Archaeology

Archaeology may unlock supernatural knowledge.

Examples:

```text
ancient vampire document
-> vampire lore
```

```text
fae relic
-> fae lore
```

```text
occult tablet
-> forbidden lore
```

---

# 27. Monster Contracts

Target gameplay:

```text
NPC reports problem
-> investigation
-> clues
-> identify monster
-> Field Guide
-> prepare weakness
-> hunt
-> return
-> reward
-> reputation
-> knowledge
```

Avoid generic “kill X mobs” quests.

Quest Giver may be a frontend, but Dark Folklore should own the backend.

---

# 28. Investigation

Possible clue types:

```text
TRACK
CORPSE
BLOOD
HAIR
BONE
MAGICAL_RESIDUE
SCORCH
FOOTPRINT
SCENT
WITNESS
```

Creature profiles should define clue signatures.

---

# 29. World Events

Potential global supernatural events:

```text
BLOOD_MOON
FULL_MOON
WITCHING_HOUR
SPIRIT_SURGE
FAE_NIGHT
WILD_HUNT
HAUNTED_NIGHT
```

Use a generic event framework.

Integrate existing mods when possible.

---

# 30. Performance rules

Never:

- scan every NPC against every NPC every tick;
- run LOS checks globally every tick;
- propagate rumors recursively without limits;
- scan the entire world constantly;
- parse data repeatedly during gameplay.

Use:

- events;
- spatial locality;
- bounded queues;
- cooldowns;
- caching;
- dirty flags;
- indexed persistent data.

---

# 31. New content rule

If a creature/item is genuinely missing:

1. verify it is missing;
2. search for existing high-quality implementations;
3. inspect model/texture/animation quality;
4. verify licenses;
5. prefer integration;
6. only add custom content if legally and visually appropriate.

No placeholder mobs.

---

# 32. KubeJS

Current pack includes:

```text
KubeJS
Rhino
KJSEidolon
```

Do not remove automatically.

Audit current scripts.

Stable foundational logic may move into Java/data-driven Core.

KubeJS may remain for prototyping and quick tweaks.

---

# 33. Diagnostics

Expected admin tools include equivalents of:

```text
/folklore diagnostics
/folklore inspect
/folklore canonical <concept>
/folklore knowledge
/folklore social
/folklore organization
/folklore village
/folklore stories
```

Diagnostics are a first-class feature.

---

# 34. Documentation expected

The Core repository should eventually include at least:

```text
README.md

docs/
    ARCHITECTURE.md
    COMPATIBILITY.md
    CANONICALIZATION.md
    CANONICALIZATION_AUDIT.md
    API_AUDIT.md
    DATA_FORMATS.md
    SOCIETY.md
    KNOWLEDGE.md
    FIELD_GUIDE.md
    SPAWN_DIRECTOR.md
    CONTRACTS.md
    MAGIC_INTEGRATION.md
    KUBEJS_AUDIT.md
    PERFORMANCE.md
    TESTING.md
    KNOWN_LIMITATIONS.md
    DEVELOPMENT.md
    ROADMAP.md
```

---

# 35. Codex execution goal

The final implementation prompt is stored separately as:

```text
DARK_FOLKLORE_MASTER_PROMPT_CODEX_5_6_ULTRA.md
```

Codex should receive:

- this handoff;
- the master prompt;
- the latest Atlas 0.2 scan;
- actual mod JARs or `/mods`;
- config/defaultconfigs;
- KubeJS scripts;
- relevant audit notes.

Codex must:

- inspect exact APIs;
- implement;
- test;
- build;
- produce the JAR;
- document real blockers rather than inventing support.

---

# 36. Final project principle

The final player experience should make it feel like:

> Vampires exist in a society.  
> Werewolves have consequences beyond combat.  
> Witches and occultists inhabit the same world.  
> Monsters are rare and meaningful.  
> Knowledge matters.  
> Witnesses matter.  
> Rumors matter.  
> Families matter.  
> Organizations matter.  
> Villages can be secretly infiltrated.  
> Hunters can investigate.  
> A supernatural identity can remain secret — or be exposed.  
> Monster hunting requires research and preparation.

Dark Folklore should not erase the identities of the installed mods.

It should make them **feel like one game**.
