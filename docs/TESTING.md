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

- **53 JUnit tests passed** with no skipped, failed, or errored tests in the recorded 0.2.0 pass.
- **3 GameTests passed** on the NeoForge GameTest server.
- **Graphical client startup passed** with final Core 0.2.0 and the curated 23-JAR set: resource reload completed, the audited Field Guide/wolfsbane adapters were active, the title screen was reached, and Alt+F4 produced `Stopping!` plus `BUILD SUCCESSFUL` with empty stderr.
- The project contains 53 concrete `@Test` methods and three concrete `@GameTest` methods; these are no longer inherited 0.1 counts or an empty GameTest task.

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

1. The validated datapack state is available in a running level, including `darkfolklore:vampire`, all four organization archetypes, twelve society templates, and the sovereign political weight.
2. A persisted public vampire belief about an ordinary villager is visible to another observer but never changes the villager's factual supernatural state.
3. Confirmed organization-leader death performs deterministic succession and removes the dead leader from the membership index.

These tests use Minecraft/NeoForge classes and a real server level. They do not render a client, exercise every optional provider, or replace world-upgrade testing.

## Production smoke matrix A-G

The statuses below record the final evidence available on 2026-08-11. A `PASS` is limited to the stated smoke scope; it does not imply that every manual high-risk case later in this document was exercised.

| ID | Status | Configuration and recorded reason | Evidence |
| --- | --- | --- | --- |
| A | `PASS` | Mandatory Minecraft/NeoForge environment only: every optional adapter disabled cleanly, all Core data loaded with `invalid=0`, all three GameTests passed, and the world saved and stopped. | `run/matrix-a-mandatory.log` |
| B | `PASS` | Primary exact adapters: Vampirism, Werewolves, MCA, MCA Capitals, MCA Vamp Compat, Enchanted, Field Guide, and the wolfsbane bridge reported `ACTIVE`; all three GameTests passed. | `run/matrix-b-exact-adapters.log` |
| C | `PASS` | Curated real-provider headless staging loaded final Core 0.2.0, validated all included data with `invalid=0`, and passed all three GameTests. The unowned dedicated-side `Screen` warning described below remains open. | `run/release-audit-020/02-runGameTestServer.log` |
| D | `PASS` | A real headless `DedicatedServer` loaded final Core 0.2.0, validated Core data with `invalid=0`, reached `Done (6.923s)`, then stopped gracefully and saved every dimension. Focused commands, `/reload`, and restart remain manual. | `run/release-audit-020/10-final-dedicated-server-latest.log` |
| E | `BLOCKED` | Graphical startup itself passed: final Core 0.2.0 reached the NeoForge title state, completed resource reload, activated the exact Field Guide/wolfsbane adapters, and exited cleanly. No world was joined, so Field Guide rendering/Recent Discoveries, Italian presentation, contract feedback, and provider gameplay remain unverified. | `run/release-audit-020/11-final-client.log`; in-world UI/gameplay pass still required. |
| F | `BLOCKED` | The schema-1 NBT fixture proves retained legacy rows, safe schema-2 defaults, dirty-once migration, and idempotent reopen, but no authentic 0.1 world directory was upgraded and inspected end to end. | `FolkloreSavedDataTest` passes; real backed-up 0.1 save required. |
| G | `PASS` | A fresh 0.2 test world loaded all Core data with `invalid=0`, passed all three live GameTests, saved every dimension, and stopped. Deeper society interactions remain manual. | `run/matrix-g-fresh-world.log` |

The exact-adapter and curated dedicated-server staging emitted one NeoForge `RuntimeDistCleaner` error stating that `net.minecraft.client.gui.screens.Screen` was requested on `DEDICATED_SERVER`. The line had no stack trace or owning mod; startup, tests, save, and shutdown continued, and Dark Folklore's audited common/server adapter classes contain no client reference. It is therefore an unowned staging warning, not evidence of a Core crash, but the pack smoke must not be described as error-free until its owner is identified.

The final dedicated-server harness requested the graceful halt through a dynamically attached stop agent. Any dynamic-agent/serviceability warning produced by that request belongs to the test harness, not to the shipped mod; the server lifecycle itself logged `Stopping server`, player/world saves, and all dimensions saved.

The client log contains provider/resource warnings for missing Easy Villagers compatibility classes, invalid upstream sound paths, models, and subtitle translations. It contains no Dark Folklore `ERROR`, bridge-disable, or client crash. These upstream warnings mean the curated client startup is not claimed to be warning-free.

At this documentation snapshot the 53 JUnit, three GameTest, final dedicated-server lifecycle, and graphical client-startup passes are recorded. Because full row-E in-world/UI acceptance and row-F real-world migration remain blocked, the 0.2.0 release classification is **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`.

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
- The schema-1 unit migration fixture is not a substitute for upgrading and inspecting an authentic 0.1 world directory; row F remains blocked.
- No automated performance benchmark proves tick-time behavior at a particular player/entity scale.

Release acceptance requires the final completion report to record the clean build, production JAR audit/hash, migration result, and A-G statuses. Never infer `PRODUCTION_READY` from compilation or unit tests alone.
