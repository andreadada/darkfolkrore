# Dark Folklore 0.7 — Vampire Behavior & Moral Predation

0.7 gives wild Vampirism vampires stable behavioral archetypes so feeding, secrecy, recruitment and deliberate murder do not collapse into one generic hostile behavior.

## Non-negotiable ownership boundary

Dark Folklore does **not** own vampire infection or conversion.

- Vampirism owns real blood draining and `BloodDrinkEvent`.
- MCA Reborn × Vampirism Compat owns MCA infection eligibility/chance/duration, conversion, cure, inheritance, capability persistence and converted-MCA vampire target/navigation/AI.
- Dark Folklore owns bounded social intent, evidence, witnesses, stories, village consequences and — only for ordinary wild Vampirism mobs — a scoped target hint.
- A `RECRUIT` intent means “deliberately leave the victim alive after a real bite”; it never means “force infection”.
- `KILL_FOR_SPORT` never calls a blood/infection API. Native Vampirism combat owns the actual damage.
- A murder story is emitted only after `ConfirmedLivingDeathEvent`, so cancelled/rescued/resurrected deaths cannot become narrative murders.

## Stable profiles

A wild Vampirism mob receives a deterministic profile from its UUID. The profile is not randomly rerolled every scan tick or server restart.

Default distribution:

| Profile | Default share | Main behavior |
|---|---:|---|
| `CONTROLLED` | 25% | prefers animals, feeds once, avoids unnecessary human violence |
| `CAUTIOUS` | 20% | strongly avoids witnesses/risk and prefers animal or isolated prey |
| `PREDATOR` | 25% | prefers human prey; sometimes continues attacking after feeding |
| `RIPPER` | 12% | strongly prefers humans, may overfeed, may rarely kill without hunger |
| `RECRUITER` | 12% | prefers isolated humans and intentionally stops after a nonlethal bite |
| `VENGEFUL` | 6% | prioritizes witnesses who already know its vampire identity and may kill them |

For MCA vampires, the profile is currently **observational/narrative only** because MCA Vamp Compat owns their target and navigation. When exact audited MCA personality data is available, the known mappings are:

- `EXTROVERTED` → `PREDATOR`
- `INTROVERTED` → `CAUTIOUS`
- `ANXIOUS` → `CAUTIOUS`
- `RELAXED` → `CONTROLLED`

Any missing or unrecognized personality falls back to the stable UUID profile. This does not create a new personality fact.

## Session intents

One bounded predation session receives one explicit intent:

| Intent | Meaning |
|---|---|
| `FEED` | one ordinary provider-confirmed blood feed, then stop |
| `RECRUIT` | one nonlethal feed and deliberate stop; provider independently decides infection |
| `OVERFEED` | Ripper keeps attempting real Vampirism feeds after satiation, within a hard cap |
| `KILL_AFTER_FEED` | feed once, then keep the same victim as a combat target |
| `KILL_FOR_SPORT` | pursue/kill without needing current feeding pressure; no infection call |
| `PROVIDER_OWNED` | MCA vampire human predation remains entirely MCA Vamp Compat-owned |
| `NONE` | no valid motive in the current context |

The lethal/nonlethal roll is deterministic per predator/victim/world-day. A scan cannot repeatedly reroll until it eventually gets a lethal result.

## Default lethal rates

All rates are server config and clamped to safe ranges:

```text
vampirePredatorKillChance = 0.18
vampireRipperOverfeedChance = 0.78
vampireRipperSportKillChance = 0.10
vampireVengefulKillChance = 0.90
vampireRipperMaxExtraFeeds = 2
```

Interpretation:

- a hungry `PREDATOR` that selected a human has an 18% stable chance for that predator/victim/day to continue lethal combat after feeding;
- a hungry `RIPPER` normally becomes lethal and has a 78% chance to enter the bounded extra-feed path first;
- a satiated `RIPPER` has only a 10% stable chance to initiate a sport-kill against an otherwise valid MCA human;
- a `VENGEFUL` vampire only gets its lethal grievance behavior when that exact victim already holds `CONFIRMED`/`PUBLIC` knowledge that the predator is a vampire.

## Candidate scoring

The profile modifies the existing social score rather than bypassing its safety checks.

Hard protections remain before profile scoring:

- children are never autonomous feeding targets;
- close MCA family is protected for provider-owned MCA-vampire feeding;
- known vampires/werewolves are not ordinary feeding civilians;
- known Hunters are threats, not feeding prey;
- tamed animals are protected;
- named non-MCA entities keep the existing protection;
- provider `UNKNOWN` facts fail closed;
- open-sky daytime blocks/aborts the Dark Folklore session;
- regional anti-chaos budgets and predator/victim cooldowns remain active;
- Dark Folklore never steals a different live combat target.

Profiles then modify preferences:

- Controlled/Cautious increase animal preference and witness/risk aversion.
- Predator/Ripper increase human preference.
- Recruiter strongly rewards isolated human prey.
- Vengeful strongly rewards the exact witness who knows the predator's identity.

## Ripper overfeeding

`OVERFEED` always uses the real audited Vampirism path:

```text
ExtendedCreature.onBite
 -> IVampireMob.drinkBlood
 -> BloodDrinkEvent
 -> provider infection decision
 -> Dark Folklore LOWEST observation
```

The number of additional feeds is bounded by `vampireRipperMaxExtraFeeds`.

If the victim becomes temporarily non-biteable after a feed, the Ripper can hold the same combat target for a short bounded window. If another real feed is not possible, the intent degrades to ordinary lethal combat rather than fabricating blood removal.

## Deliberate murder consequences

Lethal intent is correlated to the exact predator UUID + victim UUID and retained only for a bounded period. A narrative murder is created only when the finality dispatcher confirms the real death.

A confirmed deliberate vampire kill can create:

- `feeding_murder` story;
- exact culprit/implementation incident fact;
- increased village fear/suspicion;
- increased Hunter Society influence/pressure;
- normal downstream investigation/contract behavior.

The normal nonlethal path still creates `BITE_MARK`, `BLOOD`, direct witness knowledge, rumors and `feeding_assault`.

## Diagnostics

Use:

```mcfunction
/folklore predation status
/folklore predation inspect <entity>
/folklore predation trace <entity>
```

`inspect`/`trace` now report:

- profile;
- profile source;
- current intent;
- current state-machine phase;
- whether the candidate already knows the vampire identity;
- behavior score adjustment;
- predicted intent per candidate;
- normal provider/social rejection reasons.

This is designed to make real modpack debugging reproducible rather than guessing why one vampire hunted, recruited or killed and another did not.
