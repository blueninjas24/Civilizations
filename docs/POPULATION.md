# Population implementation

Phase 1 now persists individual people and household identities. This is storage
and domain groundwork; discovering a Minecraft village does not yet create these
people, run demographic changes or spawn physical villagers.

## Current records

- `Resident`: permanent UUID, name, settlement UUID, optional household UUID,
  nonnegative age in logical simulation steps, and ALIVE/DECEASED status.
- `Household`: permanent UUID, settlement UUID and display name. A household is
  shared housing identity, not an automatically generated family or one bed.
- Names can change without changing identity. Deceased records stay available for
  future history but do not contribute to `countLivingResidents`.
- A person without assigned housing has a null household. No placeholder household
  or guessed capacity is created. Calendar conversion is intentionally undefined;
  an age step is not declared to be a Minecraft day, month or year.
- Parent/child and partner links are saved separately from housing. Siblings derive
  from shared recorded parents. Homes/buildings, occupations, skills and prosperity
  are not stored yet. No demographic or seeding probabilities are chosen.

## Storage and safety

Schema version 4 adds households/residents; version 5 adds family links and
ancestry safeguards. Each migration is
transactional and additive: existing settlement fields, UUIDs and census values
are preserved; no residents are inferred from legacy population counts.

The manager enables SQLite foreign keys on its connection. Residents reference
existing settlements; assigned households must exist in the same settlement.
Deleting referenced settlements/households is rejected rather than cascading away
people. Population persistence refuses connections with foreign keys disabled.
Valid status and integral nonnegative age are constrained in SQLite and domain code.

`PopulationRepository` supports insertion, resident lookup/update, immutable lists
ordered by UUID text and living headcount. It uses the manager's connection on its
owning thread; it does not make JDBC calls safe for concurrent use or introduce
async scheduling. Raw connection access remains an existing prototype limitation.

Household-plus-members insertion is atomic and validates all membership inputs
before writing. Duplicate IDs fail rather than replacing people. A failed batch
rolls back and can be retried. Nested transactions are rejected rather than
committing caller work. Resident updates cannot silently transfer settlement
membership; a future explicit migration operation must preserve family/history.

Legacy `Settlement.population` remains the listener's approximate census value;
it is not overwritten by these new records. Living resident count is available
separately until approved discovery/seeding and projection integration can safely
establish the authoritative simulated population.

## Verified tests and remaining gate

Temporary-database tests cover complete field round trips/reopen, nullable housing,
large ages, renaming/status retention, isolated stable reads, rejected bad references,
duplicate IDs, atomic failure/retry, nested transactions, protected referenced rows,
schema upgrade rollback/recovery and snapshots preserving population records.
No real server database has been opened or upgraded by these tests.

No real Paper smoke test of version 5 has been run. Before connecting this storage
to live gameplay, resolve village recognition/seeding and time pacing, add job
and demographic rules, persist simulation progress, and test projections on a
disposable server. None of those gates is passed merely by saving a test person.

## Family memory

`FamilyTie` records directed parent-to-child links or symmetric partner links.
Partner UUIDs normalize into text order so reversed inputs cannot duplicate a link.
Both residents must exist; self-links and indirect ancestry cycles are rejected.
Links cross settlements/households and remain after death. Siblings derive from
shared parents, including half-siblings; shared housing does not imply ancestry.

SQLite checks ancestry cycles on insertion and rejects in-place link overwrites.
No removal/correction API or active marriage lifecycle exists yet. No parent count,
gender, partner exclusivity, fertility/working age or family-formation rules chosen.
Six tests verify restart/death/snapshot preservation, duplicates, cycles/shared
ancestors, unique siblings, invalid data, and v4 migration failure/retry.
