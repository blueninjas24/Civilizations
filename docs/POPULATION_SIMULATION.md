# Saved logical population stepping

Schema v6 adds an opt-in clock per settlement: signed 64-bit seed, completed
logical steps and replay version. It never creates a clock or residents merely
because a village was discovered. Replay version 1 is **aging only**; it must not
be silently reused for a different demographic algorithm.

`PopulationSimulationRepository.initializeClock` initializes once and rejects
duplicate/unknown settlements. `load` reads clock and immutable UUID-ordered people
under one transaction. `advanceAging` runs the existing deterministic runner with
an explicit target and maximum step count, then commits resident ages and progress
in one transaction. Repeating a completed target does nothing. Failed batches
roll back; unsupported replay versions fail instead of changing outcomes.

Living residents gain one logical age unit per step. Deceased records keep their
age and family links. `PopulationAging` is pure Java and preserves identity, housing
and names. Overflow fails without mutating input or stored progress. Caller-owned
transactions are refused; the manager's connection still has one owning thread.

Tests verify restart/resume, batching equality, duplicate requests, empty/isolated
settlements, overflow, failure after resident writes, replay-version rejection,
atomic snapshots and v5 migration retry. A pure-domain replay covers 500 residents
for 1,000 logical steps; this is not a Paper tick-time benchmark or calendar-century
test. The step budget does not bound CPU time as populations grow.

No runtime scheduler, offline elapsed-time conversion, birth/death/migration
algorithm or Minecraft adapter is wired here. User approved configurable prototype
population settings and automatic village participation; those are next work.
The existing age-only replay convention remains unitless until explicit versioned
integration establishes calendar units. No live database has been touched.
