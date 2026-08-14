# Dark Folklore Core 0.6.0 — Test Matrix

## Automated gate

Required on every head:

1. Java 21 clean build.
2. all JUnit tests PASS with zero failures/errors/skips;
3. all required NeoForge GameTests PASS;
4. resource/data reload validates atomically with zero invalid definitions;
5. release JAR audit PASS;
6. Python Atlas tools compile;
7. committed baseline Atlas scan can be processed by `atlas_recipe_audit.py`;
8. production JAR identity and Atlas audit are uploaded as CI artifacts.

## Reference-pack preflight

Run after every fresh `/dfatlas scan`:

```bash
python tools/verify_reference_pack.py <scan-directory>
python tools/atlas_recipe_audit.py <scan-directory> --out build/current-pack-audit
```

Any provider version mismatch blocks audited-provider promotion.

## A. Wild Vampirism vampire -> MCA civilian

Setup: night, adult human MCA civilian, hungry wild Vampirism vampire, no other combat target.

Expected:

- `/folklore predation trace <vampire>` finds the MCA civilian as provider-valid;
- state advances to TARGET_SELECTED/PURSUING;
- vampire actively approaches instead of waiting for accidental proximity;
- real Vampirism `ExtendedCreature.onBite` / `drinkBlood` path occurs;
- real `BloodDrinkEvent` reaches MCA Vamp Compat before Dark Folklore LOWEST observation;
- provider alone decides infection;
- positive nonlethal feed creates BITE_MARK + BLOOD evidence once;
- victim receives confirmed vampire knowledge;
- witnesses/rumors/hunter pressure may react;
- `feeding_assault` continuity uses exact predator/victim UUIDs;
- no duplicate evidence from event correlation.

Repeat with another live combat target: Dark Folklore must not steal it.

## B. Environment

- night + open sky: hunt allowed;
- daytime + open sky: autonomous hunt rejected/aborted;
- daytime + solid shelter: hunt may proceed;
- moving from shelter into open daylight during a Core session aborts that session;
- no claim is made about provider sunscreen/equipment immunity.

## C. Converted MCA vampire -> MCA human

Expected:

- provider factual state reports converted;
- if provider native goals are missing, audited idempotent `registerGoalsIfNeeded` repair may run;
- MCA Vamp Compat independently chooses the human target;
- Dark Folklore never calls `setTarget`/navigation for MCA vampires;
- native bite correlation requires exact attacker+target and ready->cooldown transition;
- canceled/redirected attacks without cooldown transition create no evidence.

## D. MCA vampire -> animal

Expected:

- only exact audited animal-feed extension is used;
- Vampirism creature blood store decreases;
- MCA provider bite cooldown marker is respected;
- no animal infection/conversion/replacement occurs;
- failure of animal feed circuit does not disable wild feeding, factual snapshot reads or native-bite correlation.

## E. Infection / conversion / provenance

Verify with provider configuration unchanged:

- wild bite can trigger provider infection;
- infection alone does not become public belief;
- infection -> same-character conversion is observed;
- valid non-self provider source UUID is retained as provenance;
- self-source is rejected;
- reload does not replay a conversion transition;
- source recovery after save/reload remains correct.

## F. Cure

Test start, cancellation and completion:

- state HUMAN/INFECTED/VAMPIRE/CURING classifications are correct;
- CURE_STARTED suppresses only Dark Folklore transient predatory intent;
- provider target/navigation is not cleared by Core;
- CURE_CANCELLED wins over sticky conversion metadata;
- CURED/VAMPIRISM_CLEARED does not erase historical witnesses or rumors.

## G. Inheritance

- provider owns child supernatural inheritance;
- Dark Folklore observes after provider handling;
- inherited vampire has no fabricated conversion sire;
- both parents may appear only in bounded diagnostic birth context;
- save/reload does not invent a conversion event.

## H. Progressive knowledge / Field Guide

For one creature concept:

- UNKNOWN: no dossier facets;
- DISCOVERED: existence only;
- OBSERVED: signs + behavior, no weaknesses;
- provider Field Guide implementation unlock synchronizes at OBSERVED;
- STUDIED: weaknesses/countermeasures/cure become available to Dark Folklore preparation logic;
- MASTERED: origin/bloodline facets unlock;
- foreign implementation pages map to the right canonical concept without leaking unrelated entries.

Use:

```text
/folklore knowledge dossier <player> <concept>
/folklore fieldguide diagnostics
```

## I. Investigation end-to-end

Run a natural incident rather than an admin-only shortcut:

1. incident/story exists with exact culprit fact where available;
2. accept a contract from an eligible issuer;
3. collect physical evidence at the correct location;
4. collect credible testimony from the correct witness/culprit continuity;
5. perform occult analysis using at least two different provider traditions;
6. verify hypotheses narrow only from declared signatures;
7. IDENTIFIED does not reveal hidden weakness before STUDIED;
8. tracking pulse remains loaded-area-only;
9. prepared hunt bonus requires studied lore + real countermeasure;
10. confirmed-death finality completes the hunt only after next-tick confirmation;
11. turn-in/reputation/village consequences persist across restart.

## J. Deep magic

Test held/picked-up audited tools from Enchanted, Occultism, Malum, Eidolon, Feywild and optional Bloodlines content.

Expected:

- `/folklore magic inspect-held` reports the intended discipline(s);
- obtaining a discipline tool discovers only the matching lore concept;
- `RITUAL_MAGIC` may coexist with a specific discipline;
- no item equivalence or provider ritual bypass is introduced;
- existing OccultInvestigation profiles remain the authority for derived evidence.

## K. Village response

Create controlled society states through real incidents/admin diagnostics:

- low pressure -> CALM;
- modest fear/suspicion -> UNEASY/ALERT;
- hunter/public pressure -> MOBILIZED/LOCKDOWN;
- vampire influence >=70 and clearly above hunters -> COMPROMISED;
- entering/changing a non-calm tier produces one edge-triggered player message rather than chat spam;
- response never spawns fake provider faction members.

## L. Recipe weaving / canonicalization

Re-run every 0.5 recipe family plus fresh Atlas scan:

- garlic exact-only gaps are gone for audited Vampirism/MCA recipes;
- Weapon Table and Alchemical Cauldron serializers/skill gates remain provider-owned;
- tallow/fur/quicklime/fertilizer/ritual-ash alternatives work;
- Stone Altar and additive Totem Top paths are visible in JEI and craft correctly;
- KEEP_DISTINCT soul shards/mandrakes/poppets/altars remain non-interchangeable;
- AlmostUnified remains owner of base material convergence;
- no recipe loops or unintended progression shortcuts are introduced.

## M. Save / restart / dedicated server

- create active lore, rumors, organization state, village pressure, stories/contracts and lineage;
- save and stop cleanly;
- restart dedicated server;
- durable state round-trips;
- transient predation sessions/traces/village-player observations do not persist;
- provider lifecycle is resampled without replaying false transitions;
- no client-only class is loaded on dedicated server.

## N. Failure containment

Deliberately exercise optional failure paths in a development build where feasible:

- missing/unsupported provider -> UNKNOWN/NOT_APPLICABLE as documented;
- individual predation circuit failure disables only that capability;
- resource reload failure retains previous whole validated snapshot;
- canceled/rescued death never becomes confirmed death;
- Atlas same-name candidates never auto-rewrite content.
