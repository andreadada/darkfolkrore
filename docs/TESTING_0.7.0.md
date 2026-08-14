# Dark Folklore Core 0.7.0 — Intended-Pack Test Matrix

0.7 is not production-ready until these tests pass with the intended provider stack. Standard CI proves deterministic policy/resource integrity but cannot prove real third-party AI/pathfinding, blood state or MCA provider mutations.

## Automated gate

Required before any manual test:

- Java 21 `clean build` PASS;
- all JUnit PASS;
- 3/3 NeoForge GameTests PASS;
- release JAR audit PASS;
- atomic reload reports 0 invalid resources;
- `feeding_murder` loads as the 16th curated story template;
- behavior resolver/policy/session tests PASS;
- inherited 0.6 authority-boundary tests remain PASS.

## A. Profile stability

For several wild `vampirism:vampire` entities:

1. run `/folklore predation inspect <entity>`;
2. record `behavior profile` and UUID;
3. unload/reload the entity;
4. save/restart the server;
5. inspect again.

Expected: the same UUID always receives the same profile. No per-tick/profile reroll.

## B. Controlled feeder

Find/spawn a wild vampire whose trace reports `CONTROLLED`.

Place one adult MCA human and one valid untamed animal nearby at night.

Expected:

- animal receives a strong profile preference;
- when the vampire feeds on an MCA human, intent is `FEED`, never an intentional lethal intent;
- successful feed still uses real Vampirism blood and can still be followed by provider-owned infection if MCA Vamp Compat decides so;
- Dark Folklore stops its session after the feed.

## C. Cautious feeder

With a `CAUTIOUS` wild vampire:

- compare an isolated MCA adult with an MCA adult surrounded by witnesses;
- compare human vs untamed animal under high village suspicion.

Expected: witnesses/risk strongly reduce human selection; animal/isolated prey receive a much better score.

## D. Predator

With a `PREDATOR` wild vampire:

- human prey should normally outscore an animal;
- `/folklore predation trace` must expose the deterministic per-day `FEED` or `KILL_AFTER_FEED` prediction;
- re-running trace on the same predator/victim during the same world day must not change the predicted intent;
- if intent is `KILL_AFTER_FEED`, the first blood drain must still be a real provider feed before lethal combat continues.

## E. Recruiter

With a `RECRUITER`:

1. place an isolated adult MCA human nearby;
2. allow one real bite/feed;
3. inspect lifecycle/target state.

Expected:

- intent is `RECRUIT`;
- Dark Folklore stops after one successful feed;
- victim remains alive unless provider/native combat independently caused death;
- Dark Folklore never calls infection/conversion mutation;
- MCA Vamp Compat independently decides whether infection starts;
- if infection starts, later conversion/lifecycle remains provider-owned.

## F. Ripper — hungry overfeed

With a `RIPPER` and a valid adult MCA human:

- confirm the trace predicts `OVERFEED` or `KILL_AFTER_FEED`;
- if `OVERFEED`, observe real blood feeds and the phase sequence;
- no more than `vampireRipperMaxExtraFeeds` additional provider-confirmed feeds may occur in one session;
- if further feed becomes impossible, phase may become `KILLING` but blood must not be fabricated;
- actual lethal damage remains native Vampirism combat.

Expected diagnostic phases include:

```text
TARGET_SELECTED -> PURSUING/STALKING -> ATTACKING -> FEEDING -> OVERFEEDING -> KILLING
```

Not every transition must appear if provider events happen between scan samples.

## G. Ripper — kill without hunger

A satiated `RIPPER` may only start `KILL_FOR_SPORT` when the deterministic configured sport-kill roll permits it.

Expected:

- no `drinkBlood`/infection call is required to start the lethal combat motive;
- trace says `KILL_FOR_SPORT`;
- a different existing live combat target is never stolen;
- regional anti-chaos budget and long behavior cooldown prevent murder spam.

For deterministic testing, temporarily set `vampireRipperSportKillChance=1.0` in a disposable test instance, then restore the production value.

## H. Vengeful witness targeting

Prepare two MCA civilians:

- A has `CONFIRMED`/`PUBLIC` knowledge that the exact predator is a vampire;
- B does not.

With a `VENGEFUL` wild vampire:

Expected:

- A receives the large grievance preference; B does not;
- non-hungry `KILL_FOR_SPORT` can only target A through the vengeance rule;
- merely living nearby or having a rumor is insufficient;
- a confirmed identity relationship is exact predator UUID → exact observer UUID.

For a deterministic manual gate, `vampireVengefulKillChance=1.0` may be used temporarily in a disposable instance.

## I. Protected targets

Repeat with every profile and verify behavior never bypasses:

- children;
- supernatural civilians;
- known Hunters as ordinary feeding prey;
- tamed animals;
- named protected non-MCA mobs;
- provider-UNKNOWN supernatural facts;
- open-sky daytime;
- a different already-active combat target.

For MCA vampires, close family protection/provider target rules remain provider-owned and Dark Folklore must never redirect their human target.

## J. MCA vampires remain provider-owned

Convert an MCA villager using the real provider path.

Expected:

- `predation inspect` may report a behavioral profile for narrative/debug context;
- intent for human predation remains `PROVIDER_OWNED`;
- target selection and navigation are MCA Vamp Compat native AI;
- Dark Folklore does not set/replace the target;
- infection-bite evidence is still correlated by the exact provider cooldown transition.

## K. Confirmed murder finality

For `KILL_AFTER_FEED`, `OVERFEED`, and `KILL_FOR_SPORT`:

1. allow the exact vampire to kill the exact victim;
2. verify the death finality dispatcher;
3. inspect stories/investigation/village state.

Expected:

- only a finalized death produces `feeding_murder`;
- cancelled/rescued/resurrected death produces no murder story;
- different attacker or different victim cannot consume another lethal-intent record;
- story stores the exact predator UUID and observed implementation;
- village fear/suspicion/Hunter pressure increase from the confirmed incident;
- `feeding_assault` remains the nonlethal bite story.

## L. Configuration boundaries

Verify:

- `vampireBehaviorProfiles=false` restores ordinary hunger-driven 0.6 behavior;
- all chance values clamp to [0,1];
- extra-feed count clamps to [0,8];
- rates are deterministic per predator/victim/day rather than scan-loop random;
- no config can make MCA vampire infection/conversion DarkFolklore-owned.

## M. Save/restart and performance

During an active mixed village test:

- run with multiple wild vampires/profile types;
- save/restart;
- verify no stale runtime session/target hint survives incorrectly;
- verify UUID profile remains stable;
- verify durable stories/evidence remain through existing SavedData;
- verify runtime caches are bounded and no chunk is force-loaded.

## Release rule

0.7 can be promoted only after the 0.6 full-provider gates also pass. In particular, a green behavior test cannot substitute for real MCA infection/conversion/cure/inheritance, Field Guide client, recipe/JEI, Atlas, dedicated-server, and save/restart validation inherited from 0.6.
