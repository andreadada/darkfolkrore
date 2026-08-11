# Dark Folklore 0.3 — Occult Investigation & Monster Hunting

0.3 deliberately does **not** add a sixth spell system, a new mana bar, a replacement ritual altar, a clue item, or a custom monster registry.
It turns the five existing magical traditions into different investigative disciplines that operate on the existing 0.2 contract/evidence/lore/society foundation.

## Unified loop

```text
supernatural incident
 -> curated physical clues / testimony
 -> hypotheses
 -> optional magical analysis
 -> identification
 -> Field Guide OBSERVED unlock
 -> deeper occult research
 -> preparation against existing weakness rules
 -> explicit bounded tracking pulse
 -> hunt
 -> society / reputation / contract consequences
```

## Traditions

Dark Folklore resolves an investigation implement first by exact provider namespace, then by existing item traits:

| Tradition | Primary provider |
| --- | --- |
| WITCHCRAFT | Enchanted |
| SPIRIT | Occultism |
| SOUL | Malum |
| FORBIDDEN_THEURGY | Eidolon: Repraised |
| FAE | Feywild |

No provider Java class is imported. Optional-mod absence is therefore safe.

During an active `INVESTIGATING` or `IDENTIFIED` contract, sneak-right-click a block close to an existing logical clue while holding a compatible magical item.
If that tradition is meaningful for the target profile, Core records one tradition-specific evidence type and awards target/tradition lore.
The item is not consumed and the external mod retains ownership of its native progression.

Examples:

- Witchcraft can expose a `HERBAL_REACTION` for vampire/werewolf/chupacabra cases.
- Occultism-style spirit practice can expose `SPIRIT_ECHO`.
- Malum-style soul practice can expose `SOUL_ECHO`.
- Eidolon-style forbidden practice can expose `OCCULT_SIGNATURE` or `CURSE_TRACE`.
- Fey practice is wired through `GLAMOUR_TRACE` for future curated fae case profiles rather than globally guessing every Feywild entity.

## Hypotheses

`HypothesisEngine` ranks only the evidence currently known to the contract.
It never reads the hidden target to manufacture certainty.
Magical evidence is weighted more strongly than generic blood/body/testimony because it represents a deliberate analysis step.

Admin diagnostics:

```text
/folklore investigation status <player>
/folklore investigation hypotheses <player>
/folklore investigation profile <concept>
```

## Identification and Field Guide

0.3 replaces the old hard-coded two-clue runtime threshold with a per-creature investigation profile.
The shipped profiles use three distinct evidence types. Vampire and spectral cases intentionally start with fewer physical clues, so testimony or occult analysis can complete identification; cryptid cases can remain solvable through physical tracking evidence.
Once identified, Occult Investigation ensures the target's Dark Folklore lore reaches `OBSERVED` (25 points).
The existing exact Field Guide 1.14.0 bridge can then unlock the canonical page on its normal 100-tick poll.

Additional magical analyses can still be recorded after identification through `MonsterContract.recordEvidence`, allowing research to continue before the hunt.

## Preparation

Preparation is **not hard-coded per monster**.
The engine combines the profile's verified creature traits with the already-loaded `WeaknessRule` registry.
Inventory/offhand item traits are compared against the rule requirements.

This means:

- the Weakness Engine remains the single authority for Dark Folklore cross-mod weakness semantics;
- provider-native exclusions remain intact;
- adding a datapack weakness can automatically become visible to preparation assessment without a second monster-specific switch statement.

If a player kills the identified contract target while carrying a currently satisfied documented countermeasure, the optional prepared-hunt bonus grants 5 lore and +2 Hunter reputation.

## Tracking

After identification, sneak-use a suitable magical implement/monster-part in the air.
The server performs one explicit bounded search of **already loaded entities only**.
It never loads chunks.
The result is a coarse direction, range and elevation plus a short particle trace.

Default global cap: 96 blocks.
Profiles may request less/more but are clamped by config and hard limits.
Tracking has a server-side cooldown.

The tracking pulse locates a nearby matching canonical concept, not the original incident UUID; this intentionally matches the existing 0.2 contract hunt semantics.

## New evidence types

```text
HERBAL_REACTION
SPIRIT_ECHO
SOUL_ECHO
OCCULT_SIGNATURE
GLAMOUR_TRACE
CURSE_TRACE
BINDING_TRACE
```

These are logical evidence values. No low-quality custom models or placeholder items are introduced.

## 0.2 bug fix included

The admin knowledge commands now use Minecraft's `ResourceLocationArgument`:

```text
/folklore knowledge get <player> darkfolklore:vampire
/folklore knowledge grant <player> darkfolklore:vampire 25
```

Namespaced concepts no longer fail at the colon.

## Production boundaries

- This release adds deep **gameplay integration of the existing traditions**, not undocumented invasive hooks into provider ritual internals.
- Native Enchanted/Occultism/Malum/Eidolon/Feywild rituals remain owned by those mods.
- A later provider-specific ritual adapter should only be added after auditing the exact recipe/event/codecs for the installed JAR and proving it does not bypass native progression.
- Investigation profiles are curated Java data in this patch to keep the first unified gameplay pass small and safe. Moving the profile catalogue to validated reloadable JSON is a follow-up hardening task if the manual loop proves fun and stable.

## Manual acceptance

1. Run `/folklore diagnostics` and confirm `invalid=0`.
2. Verify `/folklore knowledge grant @s darkfolklore:vampire 25`.
3. Create a vampire incident and accept the contract.
4. Collect BLOOD.
5. Hold an Enchanted investigation-capable item and sneak-right-click close to the scene; verify `HERBAL_REACTION` when the profile supports it.
6. Run `/folklore investigation hypotheses @s`.
7. Identify the target; within 100 ticks verify the Field Guide page unlocks.
8. Continue analysis with a second supported magical tradition and verify evidence/lore grows without changing the identified status.
9. Run `/folklore investigation status @s` and inspect preparation assessment.
10. With an identified target nearby, sneak-use a compatible magical item in air; verify bounded direction/range tracking.
11. Hunt with and without a documented countermeasure; verify the prepared-hunt bonus only in the prepared case.
12. Restart and confirm contract evidence/lore/Field Guide state persists through the existing schema-2 persistence.

## Automated validation

`InvestigationResourceValidatorTest` verifies the shipped profile catalogue against the shipped canonical concepts, checks non-empty creature/signature sets, validates tracking bounds, guarantees every magical analysis result is also a declared signature, and requires all five investigation-tool tags to exist and contain curated values.

`auditReleaseJar` additionally requires the vampire investigation profile and Witchcraft tool tag in the production artifact so a resource-copy regression cannot silently ship a code-only investigation system.

## Incident expansion

0.2 only created contract incidents when the attacker exposed a social secret such as VAMPIRE or WEREWOLF.
0.3 also accepts canonical creatures that have an explicit investigation profile, allowing Wendigo, Chupacabra, Ghost, Wraith, Imp and Golem cases to enter the same contract loop without pretending they are MCA-style secret identities. Their incident evidence is profile-driven instead of always being BLOOD plus a generic magical residue.

Witchcraft analysis also distinguishes canonical countermeasure reagents where the existing trait system supports them: canonical garlic can produce `GARLIC_REACTION` in vampire cases and canonical wolfsbane can produce `WOLFSBANE_REACTION` in werewolf cases. This makes an Enchanted/Vampirism/Werewolves preparation item part of evidence reasoning instead of a decorative inventory check.
