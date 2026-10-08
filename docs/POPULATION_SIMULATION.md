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

## Headless demographic prototype

`PopulationStep` now performs immutable aging, deaths, then eligible births.
`PopulationState` validates identities/housing/family references, rejects ancestry
cycles, and fixes collection order for replay. Outcome includes structured birth
and death reasons with stable person/settlement IDs. This engine is not yet saved
or scheduled by the plugin; do not discard its event output in runtime integration.

Editable `PopulationPolicy.prototype()` uses 12 logical months/year; adulthood 18,
parent ages 20–40 inclusive after aging, senior threshold 60. Per-step birth chance
is 0.015 per eligible partner pair. Background death chance is 0.0001 per living
resident plus 0.0005 per year beyond 60, capped at 1. These are approved experimental
inputs, not balanced real-world statistics. Real seconds/month is still unwired.

Birth requires two living eligible partners in the same household, observed spare
capacity, and no ancestor/descendant or shared-parent relationship. A parent can
participate in at most one birth per step. No sex/gender model is imposed. Newborns
get seeded UUIDs, age zero, two parent links, and temporary identity-derived display
names; richer names/automatic partner formation remain future work. Missing housing
capacity means unknown and blocks births; it is never treated as a measured zero.
Deaths resolve first and never delete records. No crisis/economy modifier yet.

Nine new tests cover birth/death accounting, eligibility/housing limits, relationships,
stable replay/batching, snapshot validation and accelerated 100-year scenarios
(1,200 monthly steps). These scenarios test conservation/replay, not long-term
population balance; a closed two-founder population can naturally die out.

## Atomic demographic storage

Schema v7 adds a global monthly demographic clock (replay version 2), observed
household capacity, and sequenced birth/death event records. The earlier age-only
clocks remain intact; triggers prohibit activating both modes in one database.
No existing clock or age is silently reinterpreted. Explicit demographic startup
assumes the caller has deliberately supplied monthly ages (future import adapter).

`DemographicRepository` reads an immutable global population snapshot, advances a
bounded number of months, and saves all changed/new people, parent links, event
reasons and clock progress in one transaction. A failed event/clock/person write
rolls everything back; retry produces the same identities/outcomes. Event reads
use a sequence cursor with at most 1,000 rows/page, preserving multiple same-month
events across page boundaries. Deceased people and their family records remain.

The persisted settings key uses a fixed encoding of policy fields. Changed settings
or unsupported replay versions fail explicitly; live tuning needs an explicit
recorded transition API before any configuration reload may change a running clock.
Prototype policies are configurable when constructing/initializing a run; there is
not yet a Minecraft config file. Capacity updates are explicit observations; no
beds-to-households formula is wired or inferred from incomplete loaded chunks.

Eight storage tests cover births/restart, retained deaths/event paging, rollback and
deterministic retry, snapshot-restored 100-year replay across batch sizes, clock-mode
and setting protection, capacity changes, transaction ownership and v6 upgrade
failure/recovery. All use temporary SQLite files. The current prototype listener
does not call this engine. Scheduler, automatic seeding, NPC projections, migration,
partner formation and actual Paper validation remain pending.

This implementation loads global people/relationships into memory per batch and
writes existing residents per processed batch. Step count bounds work, not wall-clock
cost; profiling/optimization is required before connecting it to a server tick loop.
