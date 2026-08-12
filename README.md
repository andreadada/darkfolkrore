# Dark Folklore Core

Dark Folklore Core is the server-authoritative integration/orchestration layer for the Dark Folklore Minecraft modpack. It gives overlapping supernatural content a shared semantic vocabulary and connects provider-owned facts to lore, witnesses, rumors, villages, organizations, investigations, weaknesses, contracts, vampire predation, and encounter pacing.

Provider mods remain authoritative for transformations, infection/cure, families, factions, rendering, spellcasting and their own progression. Core observes and orchestrates; it does not silently replace those systems.

## Target

- Minecraft **1.21.1**
- NeoForge **21.1.248** (21.1 line)
- Java **21**
- Mod ID `darkfolklore`
- Version **0.4.0**
- Society persistence schema **2**
- Investigation sidecar schema **1**

Minecraft and NeoForge are required. Third-party gameplay integrations are optional. Exact Java/reflection adapters activate only for audited versions; missing or different versions fail closed rather than guessing supernatural state.

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

0.4.0 stacks on the 0.3.1 investigation and Vampire Society hardening. Its main purpose is to understand the **real** lifecycle owned by MCA Reborn x Vampirism Compat 2.0.12 without creating a second conversion system.

### Exact provider lifecycle

For the exact audited stack, Core can observe:

```text
HUMAN
 -> INFECTED
 -> VAMPIRE
 -> CURING
 -> HUMAN
```

and distinguish important transitions such as:

- infection started;
- conversion caused by the provider's native bite path;
- other provider conversion;
- inherited vampirism;
- cure start, cancellation and completion;
- cleared pre-conversion infection;
- factual vampirism cleared even if the intermediate cure sample was missed.

The provider remains the sole authority for whether infection happens, how long it lasts, whether conversion succeeds, cure progress/completion, inheritance and MCA vampire AI.

### Same MCA person, supernatural state layered on top

Dark Folklore never replaces an MCA vampire with a generic `vampirism:vampire`. The factual state is read from MCA Vamp Compat while MCA identity/family/social data remain owned by MCA.

When the provider supplies a valid conversion-source UUID, Core records vampire provenance/lineage. Provenance is recovered both during a live conversion and after a load, and malformed self-source records are ignored. Inherited vampirism deliberately keeps both parents only as birth context rather than inventing a fake one-parent conversion source.

### Native AI extension point

If an MCA NPC is already factually converted but the provider reports that its native vampire goals were not installed, Core may call the audited idempotent:

```text
McaVampireAi.registerGoalsIfNeeded(LivingEntity)
```

Core does **not** install a replacement infection goal.

### Cure behavior

A factual cure is observed from provider state. Cure transitions stop transient Dark Folklore predatory intent, but historical witnesses/rumors are intentionally not erased: factual state and social belief remain separate.

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
- evidence `support` wording rather than pretending to expose a calibrated probability.

### Vampire Society & Predation

For exact Vampirism 1.10.12 + MCA 7.7.32+1.21.1 + MCA Vamp Compat 2.0.12:

- wild Vampirism vampires can be guided toward **named adult MCA civilians** without globally removing Vampirism's custom-name protection;
- the actual blood drain uses Vampirism's real creature blood attachment and emits the real provider `BloodDrinkEvent`;
- the BloodDrink observation runs at `LOWEST`, after MCA Vamp Compat's normal handler, so a blocked/zeroed provider event cannot manufacture Core evidence;
- MCA Vamp Compat alone decides native MCA infection/conversion;
- factual MCA vampires use provider-native human infection-bite AI;
- social risk changes feeding choice: public awareness, village suspicion, Hunter Society influence, personal Vampire suspicion and visible witnesses push MCA vampires toward safer animal feeding;
- children, close family, hunters, supernatural targets, tamed animals and named non-MCA entities are excluded from autonomous prey selection;
- animal feeding drains Vampirism's real creature blood store and respects MCA Vamp Compat's bite cooldown, without infecting or replacing the animal;
- nonlethal attacks can produce `BITE_MARK`, `BLOOD`, victim knowledge, witnesses, rumors, Hunter pressure and a contract-eligible `feeding_assault` story;
- lethal attacks remain on the existing death-driven incident path so one event is not duplicated;
- per-predator/per-victim cooldowns and a rolling regional feed budget limit chaos;
- loaded-area-only staggered scans never force-load chunks.

## Exact provider audit

The MCA Reborn x Vampirism Compat 2.0.12 development audit used a user-supplied JAR with SHA-256:

```text
BD042DF1C5275C2DF3C8596D78761EC7FE2D8CD6338738F078C531AA0EF8B7CF
```

The binary is not committed, redistributed or shaded into Core. See [`docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md`](docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md) for the exact methods/ownership boundary.

## Existing systems retained

- Reloadable/atomically validated canonical concepts, weakness rules, spawn profiles, magic integrations, investigation profiles, story templates, organization archetypes, social parameters and political weights.
- Enchanted-owned canonical wolfsbane with strict Werewolves compatibility.
- Field Guide 1.14.0 integration with seven curated categories and ten explicit entries, localized EN/IT.
- Relationship-aware witnesses/rumors, family-secret reactions, MCA Capitals political weighting, supernatural organizations, dynamic stories and village state.
- Evidence-only hypothesis ranking: the hidden target is never read to manufacture certainty.
- Weakness Engine remains the sole Dark Folklore cross-mod damage authority.

## Install

1. Use Minecraft 1.21.1, NeoForge 21.1.248 and Java 21.
2. Build or obtain `darkfolklore-core-0.4.0.jar`.
3. Put only the production JAR in the instance `mods` directory.
4. Install whichever optional provider mods the pack uses. Exact adapter versions are listed in [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md).
5. Back up an existing world before changing Core/provider versions.
6. Run `/folklore diagnostics` after startup and require `invalid=0`.
7. For the vampire stack also inspect `/folklore predation status` and `/folklore lifecycle status`.

## Build

Windows:

```powershell
.\gradlew.bat clean build --no-daemon --no-configuration-cache --stacktrace
```

Linux/macOS:

```bash
./gradlew clean build --no-daemon --no-configuration-cache --stacktrace
```

Production artifact:

```text
build/libs/darkfolklore-core-0.4.0.jar
```

Useful tasks:

```text
./gradlew test
./gradlew runGameTestServer
./gradlew runServer
./gradlew runClient
```

The build uses immutable checksum-verified compile-only provider artifacts where public artifacts are available. Optional provider code is never shaded into the release JAR.

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
- [Society](docs/SOCIETY.md)
- [Contracts](docs/CONTRACTS.md)
- [Occult Investigation](docs/OCCULT_INVESTIGATION.md)
- [MCA Vamp Compat 2.0.12 exact audit](docs/MCA_VAMP_COMPAT_2.0.12_AUDIT.md)
- [0.3.1 testing](docs/TESTING_0.3.1.md)
- [0.3.1 release gate](docs/RELEASE_0.3.1.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md)
- [Compatibility](docs/COMPATIBILITY.md)
- [Field Guide](docs/FIELD_GUIDE.md)
- [Wolfsbane audit](docs/WOLFSBANE_AUDIT.md)
- [Changelog](CHANGELOG.md)

## Release classification

0.4.0 remains **`DEVELOPMENT / RELEASE_CANDIDATE`** until the exact optional-provider stack is exercised in-world. A green Core CI proves compilation, policy tests, resources and provider-absent runtime safety; it does not by itself prove real MCA infection, conversion, cure or inheritance.

## License

All Rights Reserved. The release JAR includes `META-INF/LICENSE_darkfolklore`.
