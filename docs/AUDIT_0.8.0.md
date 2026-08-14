# Dark Folklore Core 0.8.0 — Code Audit & Hardening

This audit covers the 0.8 Legendary Encounters delta and the inherited high-risk Core boundaries it touches: provider ownership, death finality, evidence/contract continuity, persistence, loaded-area scanning, target/navigation mutation, config fail-closed behavior and resource validation.

## Fixed during the audit

### L2 Hostility

- Default config changed to `l2HostilityScaling=false`.
- The bridge is policy-disabled and performs no reflection, level mutation or trait mutation even if an old server config still says `true`.

### Encounter / contract continuity

- Encounter records now persist the exact linked story UUID (legendary persistence schema 2).
- Subjectless pre-manifestation omens are accepted only for the exact encounter story, concept, dimension, lifetime and bounded omen envelope.
- On manifestation, existing contracts for that exact story are rebound to the provider entity UUID.
- Post-manifestation evidence requires that exact culprit UUID rather than the legacy short actor/time window.
- Natural same-concept mobs therefore cannot satisfy the wrong legendary encounter contract.

### Encounter lifecycle / persistence

- Participant lists are hard-capped on mutation and deserialization.
- Story identity survives encounter stage rescheduling.
- Missing provider registry entries fail closed before encounter admission; Wild Hunt requires both captain and follower types.
- Unclaimed incident stories are closed when their encounter is terminal instead of remaining indefinitely offerable.
- Revenant provenance accepts a real living player killer as well as mob killers.

### Chupacabra semantics

- Livestock panic no longer treats arbitrary supernatural kills as Chupacabra provenance.
- Only suspicious nighttime deaths with no identifiable killer, or an actual `cnc:chupacabra`, contribute.
- Player kills and tame-animal deaths are excluded.

### Spawn safety

- Manifestation position search stays in loaded chunks.
- It now also requires world-border inclusion, collision-free feet/head space and sturdy loaded ground.

### Wards

- Upper/lower door halves normalize to one threshold.
- One threshold stores at most one ward.
- The same creator may refresh a ward; another player cannot overwrite an active foreign ward.
- Capacity failure never evicts an unrelated ward and consumes no items.
- `GENERAL` is now actually constructible and reaches the manifestation-block threshold.
- Vampire predation cancellation requires the provider bridge to agree that the target is valid wild-vampire prey; tame animals are excluded.
- MCA-vampire target/navigation remains provider-owned.

### Fae bargains

- Scans read loaded chunks only.
- No offering/cooldown/lore/reputation is committed if no growth actually succeeds.
- Milk buckets return buckets; honey bottles return glass bottles.
- At most eight valid native `BonemealableBlock` growth operations occur per accepted bargain.

### Definition / diagnostics hardening

- Encounter IDs are validated as real `ResourceLocation`s rather than by a colon string check.
- Definitions cannot claim zero omens while the runtime always emits at least one.
- QA seeding uses deterministic, explicit origins instead of unordered `Set` iteration.
- Encounter diagnostics expose the linked story UUID.

## Regression coverage added

- exact subjectless legendary omen continuity;
- post-manifestation exact culprit evidence;
- late story-to-culprit contract binding;
- schema-2 legendary save/load round trip;
- story identity survival during state rescheduling;
- participant cap enforcement;
- ward same-owner refresh, foreign-owner protection and expired replacement.

## Inherited boundaries rechecked

The audit preserves the existing authority model:

- MCA Reborn × Vampirism Compat remains factual owner of MCA infection, conversion, cure, inheritance and native MCA-vampire target/navigation/AI;
- generic Vampirism logic never takes factual ownership of MCA entities when the exact MCA authority is unavailable;
- recipe weaving still distinguishes recipe-safe equivalence from semantic tags and does not collapse `KEEP_DISTINCT` concepts;
- death-derived provenance still begins only after `ConfirmedLivingDeathEvent` finality;
- loaded-area work remains bounded and no new force-loading path was introduced.

## Automated gate

The final authoritative CI identity is recorded only after the hardened branch is squashed and rebuilt. Provider-less CI proves Java/resource/state-machine regressions; it does not replace the intended-pack runtime matrix in `docs/TESTING_0.8.0.md`.

## Release boundary

0.8 remains `RELEASE_CANDIDATE`, not `PRODUCTION_READY`, until the complete intended pack validates provider entity initialization/AI, save/restart, exact encounter contracts, wards, Fae crops and a fresh Atlas diff.
