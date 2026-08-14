# Dark Folklore 0.10 — Living Folklore

## Purpose

0.10 restores the original gameplay loop as the center of Dark Folklore:

`INCIDENT -> TESTIMONY / PHYSICAL EVIDENCE -> HYPOTHESES -> MAGICAL FORENSICS -> IDENTIFICATION -> PREPARATION -> HUNT -> SOCIAL CONSEQUENCES`

The release does not turn Dark Folklore into a second quest mod, magic mod or NPC AI provider. It connects systems that already existed but were previously experienced as separate features.

The central rule remains **FACT != BELIEF**. Provider facts are never inferred from rumors, political authority, family relationships or an evidence score. Hypotheses are computed only from evidence known to the investigation. The hidden factual contract concept is consulted only at the final authorization boundary so a convincing but incorrect belief cannot become a factual identification.

## Persistent Casebook

Every active supernatural contract can now create one `InvestigationCaseRecord` in `darkfolklore_living` SavedData. The case stores only information the player has legitimately encountered:

- exact contract and optional story continuity;
- investigation anchor;
- evidence types already acquired;
- testimony/social notes that actually reached the player;
- occult/blood-analysis notes;
- evidence-derived hypothesis notes;
- preparation and resolution notes;
- the identified concept only after identification has actually occurred.

The case progresses through `INVESTIGATING -> IDENTIFIED -> PREPARED -> HUNTED -> RESOLVED`, with explicit terminal states for expired/dismissed cases. Notes and cases are bounded; terminal history is pruned after a configurable retention window.

Player diagnostics:

- `/folklore case`
- `/folklore case notes`

The Casebook is not a new GUI and does not replace Field Guide. It is the server-authoritative continuity layer for an investigation.

## False leads and conclusive identification

Historically a contract became `IDENTIFIED` when its distinct-clue count reached the configured minimum. 0.10 adds a second, independent gate.

A candidate identification is accepted only when:

1. the minimum number of distinct clues exists;
2. the leading `HypothesisEngine` result exceeds the configured confidence floor;
3. it leads the runner-up by the configured margin;
4. only at the authorization boundary, the deduced concept matches the provider/story-backed factual incident concept.

If the old clue-count transition fires while explanations remain close, `ConclusiveIdentificationGuard` reopens the case as `INVESTIGATING` while preserving every collected clue. The player is told that the evidence is ambiguous and must find a discriminating clue or use an appropriate forensic practice.

This deliberately supports situations such as `BLOOD + BITE_MARK` keeping Vampire and Chupacabra plausible until a more specific trace appears.

## Existing occult forensic traditions

0.10 keeps the five existing investigative magical practices distinct:

- Enchanted / Witchcraft — herbal reactions, garlic/wolfsbane and curse-oriented evidence;
- Occultism / Spirit — spirit echoes and bindings;
- Malum / Soul — soul echoes and soul damage;
- Eidolon Repraised / Forbidden Theurgy — necromantic, curse and occult signatures;
- Feywild / Fae — glamour/fae signatures.

These continue to run through `OccultInvestigationEngine`; they do not become interchangeable generic magic ingredients.

## Blood Forensics

The broader magic registry already defined `BLOOD_MAGIC` with Bloodlines as its primary provider, but the investigation loop had no dedicated blood-analysis path. 0.10 fills that gap without forcing Blood Magic into the five ritual-analysis traditions.

A player can sneak-use an existing provider blood-analysis item in `darkfolklore:investigation_tools/blood` near a real, unexpired, exact-case `BLOOD` clue. The engine derives `BLOOD_RESONANCE`.

Current optional tools include existing provider content from Bloodlines, The Graveyard and Vampirism. No Dark Folklore analyzer item is registered.

`BLOOD_RESONANCE` is intentionally a medium-strength clue. Vampire, Werewolf, Wendigo and Chupacabra profiles may all support it. Therefore blood analysis can establish that a supernatural blood-linked phenomenon is involved without automatically naming the species.

## Preparation is part of the case

The existing `PreparationAssessment` remains authoritative. Once a case is identified, the Casebook can advance to `PREPARED` only when the player has sufficiently studied the creature and actually carries a known compatible countermeasure.

`OBSERVED` knowledge still does not leak hidden weaknesses. `STUDIED` remains the point where actionable countermeasures may be exposed. The prepared-hunt reward remains owned by the existing investigation engine.

## Social evidence is not factual evidence

Rumors relevant to the exact case subject can enter the player's Casebook only after the rumor really reaches that player. The note explicitly remains testimony/social knowledge, not provider fact.

The existing society stack remains authoritative for the deeper social simulation:

- witness confidence and direct/indirect observation;
- rumor confidence loss, decay, bounded hops and temporary silence;
- MCA spouse/parent/child/sibling reactions;
- `PROTECT_SECRET`, `CONFRONT_RELATIVE`, `FEARFUL_WITHDRAWAL`, `REPORT_TO_HUNTERS`;
- factual Hunter alignment overriding protective defaults;
- controlled false accusations that create belief without rewriting factual identity;
- witness-threatened stories and vengeful-vampire consequences;
- public reveal requiring credible witnesses;
- political authority changing social consequences, never magically creating knowledge.

0.10 deliberately reuses those engines instead of creating `LivingFamilyEngine`, `LivingRumorEngine` or another NPC AI layer.

## Organizations and villages

The existing organization system already maintains bounded intelligence, objectives, history and inter-organization relations for Hunter Societies, Vampire Covens, Werewolf Packs and Witch Covens. Hunter organizations can emerge as a consequence of supernatural pressure instead of existing merely because the feature is enabled.

Village society states and influence remain shared world state. This includes mobilization and `COMPROMISED` outcomes; 0.10 does not create a parallel settlement-pressure model.

## Archaeology and folklore

Better Archeology integration already existed through the semantic `ARCHAEOLOGICAL_LORE` trait. Existing artifacts can grant lore before the player directly encounters a creature or ritual. This remains the archaeology path in 0.10; there is no duplicate archaeology scanner or research currency.

## Field Guide ownership

Dark Folklore owns the knowledge state and evidence progression. Field Guide remains the provider UI/scanning surface. The existing progression remains:

`DISCOVERED -> OBSERVED -> STUDIED -> MASTERED`

Casebook does not replace this. It answers "what is happening in this incident?" while Field Guide answers "what has this player learned about this concept over time?"

## Performance and safety boundaries

- no world-wide case scans;
- no chunk force-loading;
- bounded notes, player cases, social rumor propagation and organization intelligence;
- case synchronization runs on a coarse server tick interval;
- blood analysis searches only the local exact-case evidence envelope;
- no provider AI replacement;
- no MCA relationship fabrication;
- no provider supernatural fact fabrication;
- no new Dark Folklore item, block, entity, sound, texture or model in 0.10.

## What 0.10 changes conceptually

Dark Folklore's center is not a boss list. A normal session should be able to begin with an ambiguous incident, continue through witnesses and conflicting explanations, use the pack's different magical traditions as forensic tools, require actual preparation and only then become a hunt. The consequences remain in the families, rumors, organizations and village that experienced the incident after the physical monster is gone.
