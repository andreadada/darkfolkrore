# Dark Folklore Core

Dark Folklore Core is the server-authoritative integration and orchestration layer for the Dark Folklore Minecraft modpack. It gives overlapping supernatural content a shared semantic vocabulary and connects provider-owned facts to lore, witnesses, rumors, villages, organizations, investigations, weaknesses, contracts, vampire predation, and encounter pacing.

> MCA Reborn × Vampirism Compat remains authoritative for factual supernatural MCA mechanics. Dark Folklore observes those mechanics and implements knowledge, investigation, rumor, reputation and narrative consequences around them.

The same rule applies across the pack: provider mods own transformations, infection and cure, families, factions, rendering, spellcasting, native AI, and their own progression. Core observes and orchestrates without silently replacing those systems.

## Target

- Minecraft **1.21.1**
- NeoForge **21.1.248** (21.1 line)
- Java **21**
- Mod ID `darkfolklore`
- Version **0.4.0**
- Society persistence schema **2**
- Investigation sidecar schema **1**

Minecraft and NeoForge are required. Third-party gameplay integrations are optional. Exact Java/reflection adapters activate only for audited versions; missing, partial, failed, or different versions fail closed rather than guessing supernatural state.

## Unified gameplay loop

```text
incident / suspicious feeding
 -> physical evidence + witnesses
 -> evidence-only hypotheses
 -> optional occult analysis
 -> identification
 -> Field Guide / lore
 -> research
 -> learned countermeasure
 -> bounded tracking
 -> hunt / social resolution
 -> village, family, rumor and organization consequences
```

## 0.4.0 — native MCA vampire lifecycle integration

0.4.0 builds on the merged 0.3.1 investigation and Vampire Society hardening. Its purpose is to understand the real lifecycle owned by MCA Reborn × Vampirism Compat 2.0.12 without creating a second conversion or AI system.

### FACT and BELIEF are separate

| Layer | Authority | Examples |
| --- | --- | --- |
| **FACT** | Exact provider adapter | Whether an MCA person is infected, converted, curing, inherited-vampire, hunter, or werewolf; conversion-source UUID when supplied. |
| **BELIEF** | Dark Folklore | What a witness suspects or knows, rumor confidence, reputation, investigation state, stories, and contracts. |

A rumor can never become a provider fact. For an MCA entity, factual vampire/werewolf/hunter queries route only through the exact MCA Vamp Compat authority. Core does not fall through to a generic Vampirism adapter if that authority is absent, untested, partial, unsupported, or failed: the result is `NOT_APPLICABLE` when the provider is absent and `UNKNOWN` when authority exists but cannot be trusted.

### Exact provider lifecycle

For the exact audited stack, Core can observe:

```text
HUMAN
 -> INFECTED
 -> VAMPIRE
 -> CURING
 -> HUMAN
```

and distinguish:

- infection started;
- conversion caused by the provider's native bite path;
- another provider conversion;
- inherited vampirism;
- cure start, cancellation, and completion;
- cleared pre-conversion infection;
- factual vampirism cleared even if an intermediate cure sample was missed.

Initial observation waits at least one server tick after entity join, then retries unavailable provider capability snapshots for a bounded 200 ticks. Loaded entities are subsequently sampled on a staggered interval; Core performs no whole-world entity scan and force-loads no chunks.

Cure cancellation is classified from the prior factual `CURING` state before interpreting retained inheritance, bite-cause, or source metadata. That matters because provider 2.0.12 can retain those fields when a cure is cancelled.

### Same MCA person, provider-owned behavior

Dark Folklore never replaces an MCA vampire with a generic `vampirism:vampire`. The provider remains the sole owner of infection, conversion, cure, inheritance, capability persistence, target selection, navigation, and native MCA vampire AI. Core may ask the provider's audited idempotent `McaVampireAi.registerGoalsIfNeeded(LivingEntity)` method to repair missing provider goals after factual conversion; it never installs replacement goals or sets/clears an entity target or navigation path.

Core may score a socially appropriate feeding candidate and retain a bounded narrative session. For an MCA human target, that session continues only when provider-native AI independently selects the same target. A cure or cleared factual state cancels only Core's session. It does not mutate the provider/MCA target or navigation, and it does not erase historical witnesses or rumors.

When the provider supplies a valid conversion-source UUID, Core records it as provider provenance and can recover it after load. A self-source is rejected. The UUID is not necessarily a biological parent or universally reliable sire. Inherited vampirism instead keeps both parents as short-lived birth context and never fabricates a one-parent conversion source.

### Evidence comes from completed provider actions

Wild Vampirism feeding uses the real creature blood attachment and `BloodDrinkEvent`; Core observes at `LOWEST`, after MCA Vamp Compat's normal handler and final amount mutations.

Native converted-MCA bites use exact `LivingIncomingDamageEvent` correlation. At `HIGHEST`, Core captures the same event identity, direct attacker/target pair, provider target eligibility, and the attacker's ready bite capability. At `LOWEST`, it requires that same pair and a provider attacker-capability transition from ready to cooldown. This covers provider-valid MCA, player, and vanilla-human targets without relying on an MCA-only victim marker. Provider 2.0.12 may intentionally cancel or zero ordinary damage after a successful nonlethal bite; that post-success cancellation does not suppress evidence when the provider cooldown transition proves the feed. Only that completed provider action creates consequences. Core does not infer a bite from proximity or session state alone.

## 0.3.1 foundation retained

### Investigation hardening

- explicit story ↔ contract continuity;
- factual culprit UUID and concrete provider implementation when known;
- exact culprit tracking while valid;
- same-concept fallback only after confirmed death, never ordinary unload;
- cancellable/rescued `LivingDeathEvent` handling deferred until death finality;
- bounded issuer-death hand-in recovery;
- concept-level creature sightings separate from social identity secrets;
- concept-specific testimony for curated monsters;
- expired evidence rejected at collection time;
- weaknesses/preparation hidden until `STUDIED` lore;
- `KEEP_DISTINCT` Field Guide implementation-specific discovery;
- curated Feywild Sprite investigation using `GLAMOUR_TRACE`;
- evidence `support` wording rather than a manufactured probability.

### Vampire Society and predation

For exact Vampirism 1.10.12 + MCA 7.7.32+1.21.1 + MCA Vamp Compat 2.0.12:

- hungry wild Vampirism vampires can use named adult MCA civilians without globally removing Vampirism's custom-name protection;
- MCA Vamp Compat alone decides native MCA infection and conversion;
- public awareness, village suspicion, Hunter Society influence, personal suspicion, and visible witnesses affect Core's bounded narrative candidate score;
- children, close family, hunters, supernatural targets, tamed animals, and named non-MCA entities are excluded from autonomous candidate scoring;
- Core never commands target acquisition or navigation; animal feeding is opportunistic and human feeding remains provider-native;
- animal feeding drains Vampirism's real creature blood store and respects MCA Vamp Compat's bite cooldown without infecting or replacing the animal;
- nonlethal confirmed feeds can produce `BITE_MARK`, `BLOOD`, victim knowledge, witnesses, rumors, Hunter pressure, and a contract-eligible `feeding_assault` story;
- lethal attacks remain on the confirmed-death incident path so one event is not duplicated;
- cooldowns and a rolling regional feed budget limit chaos;
- staggered loaded-area scans never force-load chunks.

## Exact provider audit

The MCA Reborn × Vampirism Compat 2.0.12 development audit used a user-supplied JAR with SHA-256:

```text
BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF
```

The binary is not committed, redistributed, or shaded into Core. See [the exact provider audit](docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md) for the inspected methods and ownership boundary.

## Existing systems retained

- Reloadable, atomically validated canonical concepts, weakness rules, spawn profiles, magic integrations, investigation profiles, story templates, organization archetypes, social parameters, and political weights.
- Enchanted-owned canonical wolfsbane with strict Werewolves compatibility.
- Field Guide 1.14.0 integration with **seven** curated categories and **ten** explicit entries, localized in English and Italian.
- Relationship-aware witnesses/rumors, family-secret reactions, MCA Capitals political weighting, supernatural organizations, dynamic stories, and village state.
- Evidence-only hypothesis ranking: the hidden target is never read to manufacture certainty.
- Weakness Engine remains the sole Dark Folklore cross-mod damage authority.

## Install

1. Use Minecraft 1.21.1, NeoForge 21.1.248, and Java 21.
2. Build or obtain `darkfolklore-core-0.4.0.jar`.
3. Put only the production JAR, **not** `darkfolklore-core-0.4.0-sources.jar`, in the instance `mods` directory.
4. Install whichever optional provider mods the pack uses. Exact adapter versions are listed in [Compatibility](docs/COMPATIBILITY.md).
5. Back up an existing world before changing Core or provider versions, then review `config/darkfolklore-common.toml`.
6. Run `/folklore diagnostics` after startup and require `invalid=0`.
7. For the vampire stack, also inspect `/folklore predation status` and `/folklore lifecycle status`.

## Build

Windows:

```powershell
\.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

Linux/macOS:

```bash
./gradlew clean build --no-daemon --no-configuration-cache --stacktrace
```

Artifacts:

```text
build/libs/darkfolklore-core-0.4.0.jar
build/libs/darkfolklore-core-0.4.0-sources.jar
```

The sources JAR is a development artifact and must not be installed in the pack. Optional provider code is compile-only and is never shaded into the production JAR.

Useful tasks:

```text
./gradlew test
./gradlew runGameTestServer
./gradlew runServer
./gradlew runClient
```

## Data and reloads

Core atomically reloads:

```text
data/<namespace>/darkfolklore/canonical/
data/<namespace>/darkfolklore/weaknesses/
data/<namespace>/darkfolklore/spawn_profiles/
data/<namespace>/darkfolklore/magic_integrations/
data/<namespace>/darkfolklore/investigation_profiles/
data/<namespace>/darkfolklore/story_templates/
data/<namespace>/darkfolklore/organization_archetypes/
data/<namespace>/darkfolklore/social_parameters/
data/<namespace>/darkfolklore/political_weights/
```

The complete candidate must validate. If one definition or cross-definition invariant fails, Core keeps the previous validated snapshot. Field Guide categories, standard tags, recipes, loot modifiers, and NeoForge biome modifiers remain in their owning formats outside that transaction.

## Contract quick start

A recognized supernatural actor killing an animal, villager, or MCA person can create a local incident. Empty-handed sneak-right-click a valid local issuer, then collect nearby physical clues, record credible testimony, or analyze the scene with a compatible existing magical implement. Identification advances lore to `OBSERVED`; weakness and preparation details remain hidden until `STUDIED`. If an incident recorded a factual culprit, tracking and hunt completion follow that culprit while valid. Confirmed deaths can enable documented fallback paths; unloads cannot.

## Operator diagnostics

All ground-truth commands require permission level 2.

```text
/folklore diagnostics
/folklore inspect <entity>
/folklore canonical <concept>
/folklore knowledge get <player> <concept>
/folklore knowledge grant <player> <concept> <points>
/folklore social get <observer> <subject> <secret>
/folklore social set <observer> <subject> <secret> <state> <confidence>
/folklore social inspect <entity>
/folklore rumor inspect
/folklore fieldguide diagnostics
/folklore capitals inspect <entity>
/folklore organization list
/folklore organization inspect <uuid>
/folklore village [inspect]
/folklore story list
/folklore contracts
/folklore investigation status <player>
/folklore investigation hypotheses <player>
/folklore investigation profile <concept>
/folklore predation status
/folklore predation inspect <entity>
/folklore lifecycle status
/folklore lifecycle inspect <entity>
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Data formats](docs/DATA_FORMATS.md)
- [Development](docs/DEVELOPMENT.md)
- [Society](docs/SOCIETY.md)
- [Contracts](docs/CONTRACTS.md)
- [Occult Investigation](docs/OCCULT_INVESTIGATION.md)
- [Compatibility and factual routing](docs/COMPATIBILITY.md)
- [MCA social audit](docs/MCA_SOCIAL_AUDIT.md)
- [MCA Vamp Compat 2.0.12 exact audit](docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md)
- [Field Guide](docs/FIELD_GUIDE.md)
- [0.4.0 testing](docs/TESTING_0.4.0.md)
- [0.4.0 release gate](docs/RELEASE_0.4.0.md)
- [0.4.0 handoff](docs/HANDOFF_0.4.0.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md)
- [Historical 0.3.1 testing](docs/TESTING_0.3.1.md)
- [Historical 0.3.1 release record](docs/RELEASE_0.3.1.md)
- [Changelog](CHANGELOG.md)

## Release classification

0.4.0 remains **`RELEASE_CANDIDATE`**, not `PRODUCTION_READY`. Local and GitHub Actions validation cover 119 JUnit tests and three NeoForge GameTests; the post-rebase production JAR identity is recorded in the [release gate](docs/RELEASE_0.4.0.md). The exact-provider/full-pack client and in-world manual matrix remains unrun, and no manual gameplay pass is claimed here.

## License

All Rights Reserved. The release JAR includes `META-INF/LICENSE_darkfolklore`.
