# Testing

Dark Folklore uses four evidence layers: fast JUnit policy/resource tests, live NeoForge GameTests, clean-build/JAR validation, and manual server/client/world-upgrade smokes. Passing a lower layer does not imply that an optional-mod UI or complete pack is release-ready.

## Commands

Use Java 21:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

Runtime tasks:

```powershell
.\gradlew.bat runServer
.\gradlew.bat runClient
```

The first development-server launch may create `run/eula.txt` and stop. Continue only after accepting the Minecraft EULA.

## Current automated snapshot

- **49 JUnit tests passed** with no skipped, failed, or errored tests in the recorded 0.2.0 pass.
- **3 GameTests passed** on the NeoForge GameTest server.
- The project contains 49 concrete `@Test` methods and three concrete `@GameTest` methods; these are no longer inherited 0.1 counts or an empty GameTest task.

The JUnit suite covers:

- Canonical ID resolution and invalid policy declarations.
- Syntax of every shipped JSON resource.
- Field Guide category/entry/localization/mapping completeness and validator failure cases.
- Canonical/legacy wolfsbane semantics, recipes, tags, loot/worldgen routing, and exact bridge pinning.
- Weakness multiplication and native-provider double-application avoidance.
- Lore thresholds and monotonic social-knowledge merges.
- Rumor retelling/half-life rules and explainable exact-MCA trust contributions.
- Exact MCA Capitals title mapping, political weights, and fail-closed version gates.
- Family-secret reactions, public-reveal thresholds, and false-accusation safety.
- Organization invariants, bounded intelligence/event history, natural affiliation, and hunter founding rules.
- Story-template validation and story/contract state machines.
- Schema-2 NBT round trips, global public claims, family-reaction persistence, organization succession, terminal cleanup, and idempotent schema-1 migration defaults.
- Reputation and village-state bounds.

The live GameTests verify:

1. The validated datapack state is available in a running level, including `darkfolklore:vampire` and at least ten society templates.
2. A persisted public vampire belief about an ordinary villager is visible to another observer but never changes the villager's factual supernatural state.
3. Confirmed organization-leader death performs deterministic succession and removes the dead leader from the membership index.

These tests use Minecraft/NeoForge classes and a real server level. They do not render a client, exercise every optional provider, or replace world-upgrade testing.

## Production smoke matrix A-G

Record each row as `PASS`, `FAIL`, or `BLOCKED`, with the exact command/mod set, log path, and reason. The automated snapshot above does not close this matrix by itself.

| ID | Configuration | Acceptance evidence |
| --- | --- | --- |
| A | Dark Folklore plus mandatory Minecraft/NeoForge environment only | Startup completes; all optional adapters are `DISABLED`; no optional-class linkage failure; `/folklore diagnostics` shows `invalid=0`. |
| B | Dark Folklore plus primary exact adapters | Exact adapters report `ACTIVE`; missing non-primary integrations remain safely disabled; focused factual, social, Field Guide, and wolfsbane checks pass. |
| C | Curated real modpack integration set | Startup/reload complete with real providers; canonical resources resolve; no known incompatibility or data error. |
| D | Dedicated server | Headless startup reaches `Done`; reload, commands, save, shutdown, and restart complete without critical errors. |
| E | Client | Join a world/server; exercise Field Guide UI, recent discoveries, localized entries, contract feedback, and normal provider gameplay without client crash. |
| F | Existing 0.1 world upgraded to 0.2 | Backed-up schema-1 world loads, retains old records, receives safe new defaults, saves schema 2, and reopens without rerunning migration or corrupting state. |
| G | Fresh 0.2 world | New world creates schema 2 and exercises witnesses, organizations, stories, contracts, save/restart, and reload cleanly. |

At this documentation snapshot the 49 JUnit and three GameTest passes are recorded. Client behavior and the full manual A-G evidence remain separate promotion gates. If client validation (or an explicitly accepted equivalent) is outstanding, the maximum release classification is `RELEASE_CANDIDATE`.

## Manual high-risk matrix

Run on disposable worlds or backed-up copies. Retain `latest.log`, relevant config/datapacks, and the tested production JAR hash. Unless noted, use a permission-level-2 operator.

| Area | Setup and action | Expected result |
| --- | --- | --- |
| Exact optional gating | Test each primary adapter absent, exact, and deliberately different in isolation where practical. | Absent is `DISABLED`; exact is `ACTIVE`; different is `UNTESTED_VERSION`; an audited-signature failure is `ERROR`. No unavailable fact is returned as true. |
| Atomic reload | Start from valid data, add one malformed definition in a disposable datapack, run `/reload`, inspect diagnostics, remove it, and reload again. | The malformed candidate is rejected as a whole, the previous validated state remains active, the precise resource appears in validation errors, and the corrected complete candidate swaps in. |
| Schema migration | Upgrade a copy of a real 0.1 world, inspect old lore/social/org/story/contract data, save, restart twice. | Old rows survive; new schema-2 fields get safe defaults; schema becomes 2; migration is not repeated after the first save. |
| MCA relationships | Arrange spouse, parent/child, sibling, qualifying player-friend, qualifying bounty-target, stranger, and unsupported NPC relationships; propagate identical rumors. | Only relationships actually exposed by MCA 7.7.32 contribute; `/folklore rumor inspect` names the contributions; unsupported NPC friendship/enmity is not invented. |
| MCA personalities | Compare verified extroverted, introverted, anxious, relaxed, neutral, and unknown labels. | Only audited labels receive the small mapped transmission/fear/investigation effects; unknown labels contribute zero. |
| MCA Capitals | Inspect exact 1.1.0 officeholders and non-political entities with `/folklore capitals inspect`. | Exact titles map to documented semantic roles; role/capital/village/state diagnostics are read-only; weights change credibility/response only after knowledge exists. |
| Witness/public reveal | Produce direct witnessed incidents with fewer and then at least the configured credible observers, including a hunter-society reporter. | Individual confirmed beliefs persist; no public claim occurs below threshold or without authorized organization processing; eligible reveal becomes global without observer fan-out. |
| Family secrets | Let exact MCA relatives witness a factual secret, including a factual hunter and anxious relative. | The bounded reaction table selects protect, confront, fear/withdraw, or report; persisted reaction affects retelling/reporting without mutating the family graph. |
| False accusation | Trigger eligible witching-hour story conditions around non-vampire social entities. | Any accusation remains low-confidence belief, requires the controlled template path, and never changes factual state or becomes automatic proof. |
| Organization lifecycle | Join factual vampire/werewolf/witch NPCs in a region, create suspicion for hunters, add recruits, complete a local contract, and kill a member/leader. | Archetype caps/founding rules apply; intelligence/events remain bounded; influence changes; confirmed death removes membership; leader succession is deterministic; an empty organization dissolves. |
| Society stories | Exercise family discovery, public reveal, hunter report, recruitment, full moon, witching hour, and political exposure. | Eligible enabled templates are chosen by weight/rate, respect per-template regional cooldown/lifetime/capital/secret filters, and persist actors/region/status. |
| Contract testimony | Accept an incident, record testimony from an NPC holding at least suspected 0.35-confidence matching knowledge, then collect a second distinct clue. | `TESTIMONY` counts once, grants clue lore, and can identify the canonical target without exposing it before the threshold. |
| Contract feedback | Search near and away from logical clue points; collect a clue; hunt a matching concept; return to issuer. | Miss feedback is rate-limited; vanilla smoke/happy-villager particles appear appropriately; completion awards 8 emeralds, 150 XP, reputation/lore, and village/organization consequences. |
| Field Guide | With exact 1.14.0, inspect all six categories and nine provider-backed entries; scan/kill; reach 25 lore points; restart. | No empty/unknown category; English/Italian text resolves; native unlock records discovery time/recent ordering; lore threshold unlocks an existing page; progress/custom notes persist. |
| Wolfsbane | With exact Enchanted 4.2.7 and Werewolves 2.0.3.3, test crop/seed acquisition, legacy loot/worldgen, recipes, diffuser, finder, and contact behavior. | Enchanted is the farmable canonical plant; new audited acquisition routes away from the legacy flower; Werewolves native mechanics continue through the strict bridge. |
| Performance/growth | Profile a populated region with repeated incidents, rumor traffic, organizations, and stories over several maintenance cycles. | No whole-world every-tick scan; rumor/local candidate/batch caps hold; role cache, org histories/intelligence, organizations, social records, and retained narratives stay within documented bounds. |
| Permissions/logging | Try all command roots as an ordinary player and operator; run normal simulation with `debugLogging` false/true. | Ordinary players cannot access ground-truth/admin commands; normal logs are concise; detailed simulation logs require the debug toggle; repeated optional failures do not spam. |

## Known automated gaps

- No automated test renders Field Guide categories, entry models, toasts, recent-discovery layout, or Italian client presentation.
- GameTests do not yet drive the full event-bus witness LOS path, natural-spawn cancellation, contract interactions, or cross-restart disk persistence.
- Optional adapter models and gates are unit tested, but every real foreign JAR permutation and runtime signature still needs smoke coverage.
- Syntactic/resource validation cannot prove every optional registry ID exists in every pack variant.
- The schema-1 unit migration fixture is not a substitute for upgrading a real 0.1 world directory.
- No automated performance benchmark proves tick-time behavior at a particular player/entity scale.

Release acceptance requires the final completion report to record the clean build, production JAR audit/hash, migration result, and A-G statuses. Never infer `PRODUCTION_READY` from compilation or unit tests alone.
