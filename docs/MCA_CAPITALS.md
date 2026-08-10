# MCA Capitals Integration — 1.1.0

## Exact version audit

| Property | Audited value |
| --- | --- |
| File | `mcacapitals-1.1.0.jar` |
| Mod ID | `mcacapitals` |
| Version | `1.1.0` |
| SHA-256 | `73AF01FAE88C9698D93EF0372854EA57373EFA0276C0CB33CC38FEFEEDED7B56` |
| Classes / JAR entries | 233 / 490 |
| Required MCA range | `[7.7.7,7.8.0)` |
| Minecraft | exact `1.21.1` |

The implementation was built on 2026-06-06 according to its manifest. There is no dedicated API namespace. Core therefore treats public implementation signatures as usable only behind an exact 1.1.0 gate.

## Verified state and access

MCA Capitals owns all monarchy and family state. Core only reads it.

| Concept | Exact installed class/access | Core support |
| --- | --- | --- |
| Capital identity | `CapitalRecord.getCapitalId()`, `getVillageId()`, `getState()` | Supported |
| Capital lookup by MCA village ID | `CapitalManager.getCapitalByVillageId(Integer)` | Supported |
| Political actor lookup | `CapitalTitleResolver.findCapitalForEntity(ServerLevel, UUID)` | Supported |
| Sovereign | `CapitalRecord.getSovereign()` and title resolver | Supported through role context |
| Consort / dowager | `getConsort()`, `getDowager()` and title resolver | Supported through role context |
| Heir / succession | `getHeir()`, `getHeirMode()`, `getRoyalSuccessionOrder()` | Current heir role supported; full order audited but not copied into Core state |
| Royal children / household | `getRoyalChildren()`, `isRoyalChild()`, `getRoyalHousehold()` | Titled royal children supported; household membership is not treated as a title |
| Nobility | `isDuke`, `isLord`, `isKnight` and associated title state | Supported through role context |
| Court offices | `getHand`, `getHerald`, `getGrandMaester`, `getCommander` | Supported |
| Royal guard | `isRoyalGuard(UUID)` | Supported, including Sir/Dame disambiguation |
| Persistent storage | `CapitalDataAccess`, `CapitalSavedData`, `CapitalRecord` | Owned exclusively by MCA Capitals |

`CapitalState` has the exact values `PENDING`, `FOUNDED`, and `ACTIVE`. `McaCapitalsCompat.capitalByMcaVillageId` returns this state with the capital UUID and MCA village ID.

### Exact role mapping

The adapter maps only hard-coded title strings emitted by `CapitalTitleResolver` in the installed 1.1.0 bytecode:

| Core role | Exact MCA Capitals titles |
| --- | --- |
| `HIGH_SOVEREIGN` | High Queen, High King |
| `SOVEREIGN` | Queen, King |
| `CONSORT` | Queen Consort, King Consort |
| `DOWAGER` | Dowager Queen, Dowager King |
| `HEIR` | Heir Apparent, Crown Princess, Crown Prince |
| `ROYAL_CHILD` | Princess, Prince |
| `PRINCE_CONSORT` | Princess Consort, Prince Consort |
| `DOWAGER_PRINCE` | Dowager Princess, Dowager Prince |
| `HAND` | Hand of the Queen, Hand of the King |
| `GRAND_MAESTER` | Grand Maester |
| `HERALD` | Court Herald |
| `DUKE` | Duchess, Duke |
| `DOWAGER_DUKE` | Dowager Duchess, Dowager Duke |
| `MAESTER` | Maester |
| `COMMANDER` | Lord Commander |
| `ROYAL_GUARD` | Dame, Sir when `CapitalRecord.isRoyalGuard` is true |
| `KNIGHT` | Dame, Sir otherwise |
| `LORD` | Lady, Lord |
| `COMMONER` / `NONE` | Commoner / None |

An unrecognized title remains `UNKNOWN` and is visible in diagnostics; it is never assigned the nearest-looking role.

## Political behavior

`PoliticalWeightModel` provides four small, explicit dimensions: credibility, organization response, investigation priority, and public-awareness pressure. Sovereigns, the Hand, commander, court, herald, guards, and nobles have different defaults. Commoner, none, and unknown have zero political weight.

These weights apply only after an actor has obtained real observer-specific knowledge. A king is not omniscient, a royal title does not confirm a rumor, and political pressure cannot rewrite the factual supernatural state supplied by Vampirism or MCA Vamp Compat.

Recommended runtime flow:

1. Society determines whether this specific observer knows or believes a claim.
2. `McaCapitalsCompat.politicalContext(level, observerUuid)` supplies the verified role.
3. The relevant political weight affects response priority, not knowledge acquisition.
4. Supernatural-social state stays in Dark Folklore SavedData; monarchy state stays in MCA Capitals.

The defaults are pure data returned by `PoliticalWeightModel`; Core configuration may scale or replace them without changing the adapter.

## Runtime and performance design

- `initialize(actualVersion)` activates only for exact 1.1.0 and returns an actionable disabled reason otherwise.
- All reflective classes and methods are resolved and cached at activation. No method lookup occurs in a role query.
- Role results use a 20-tick, access-ordered cache capped at 1,024 entries.
- The cache key contains server identity, dimension, and entity UUID; values contain only dependency-free Core records, never MCA Capitals objects.
- Failed and not-political results are also briefly cached to prevent repeated scans.
- `clearCache()` is available for server-stop/reload wiring.
- Query failures fail closed and log at most one concise warning per adapter instance.
- The adapter has no Atlas, scan-output, config-file, or local JAR runtime dependency.

MCA Capitals' own `findCapitalForEntity` walks its capital records. The short bounded cache prevents that walk from becoming a rumor hot path.

## Deliberate boundaries

- Capital lookup is supported by MCA village ID. MCA Capitals 1.1.0's spatial MCA village bridge is package-private, so Core does not guess that a coordinate belongs to a capital.
- `CapitalManager.getCapitalForResident` means membership in recorded royal/court/noble/guard roles; it is not a complete set of ordinary residents. Core does not mislabel it as population membership.
- Full succession order is readable but is not duplicated into Core persistence. Current heir/title context is sufficient for initial royal-secret stories.
- The adapter does not appoint, marry, legitimize, disinherit, promote, demote, found, rename, or otherwise mutate a capital.
- A constant-pool scan of all 233 MCA Capitals classes found no Vampirism, werewolf, or hunter references. Supernatural facts must come from their owning integrations.

## Wiring checklist

- Add optional compatibility reporting for `mcacapitals` and call `initialize` with the exact loaded version.
- Clear the role cache on server stopping and when a relevant integration is deliberately reinitialized.
- Add `PoliticalWeights` only after ordinary relationship/organization trust has been calculated.
- Surface `PoliticalContext.detail`, exact title, capital UUID, MCA village ID, and state in administrator diagnostics.
- Permit royal-scandal/heir-secret stories only when the subject has a verified political role and a separately verified supernatural fact.
- Do not merge Dark Folklore village state with `CapitalSavedData`; link them only when a real MCA village ID is available.

## Manual scenario

1. Use MCA Capitals itself to found a capital and appoint a sovereign, heir, Hand, Lord Commander, herald, royal guard, and ordinary knight.
2. Inspect each actor and verify the exact title maps to the expected Core role. In particular, verify Sir/Dame distinguishes guard from knight.
3. Give one political actor observer-specific supernatural knowledge and leave another unaware.
4. Confirm only the informed actor produces political response weight.
5. Make a royal MCA villager supernatural through the owning Vampirism/MCA Vamp Compat mechanics.
6. Confirm factual transformation is unchanged while royal-secret story eligibility and political consequences appear.
7. Restart the server and verify MCA Capitals still owns the court while Dark Folklore retains only its social consequences.

## Validation status

Pure unit tests cover every role family, Sir/Dame guard disambiguation, unknown-title fail-closed behavior, authority weight ordering, and version rejection before optional class loading. Installed bytecode signatures and all exact title constants were audited directly.

A full in-game smoke with a founded capital remains required. Unit tests do not instantiate `CapitalRecord` because MCA Capitals and MCA tracked world state are intentionally optional at test runtime. Until that scenario is performed, the adapter is signature-verified and compile-tested, not claimed as end-to-end gameplay verified.
