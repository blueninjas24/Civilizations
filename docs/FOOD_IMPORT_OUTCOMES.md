# Food import outcome evidence

Phase 2 decisions need real outcomes before they can learn from them. Each retained
food period now supports an exact accounting comparison: what would unmet demand
and closing reserves have been **without that period's imports**, with opening
stock, local production and demand held fixed?

For example, a village with no food or production needs four units. A verified
twelve-unit delivery prevents four units of unmet demand and adds eight units to
closing reserves. If local production already covered the four units, that same
delivery adds twelve reserve units and prevents no unmet demand this period.

`/civ debug economy <settlement UUID>` shows imported units, unmet demand prevented,
additional closing reserves, and actual versus hypothetical unmet demand. These are
abstract food-accounting units, not a count of physical meals or saved residents.
All imports are assessed together, avoiding arbitrary credit based on receipt order.
Donations, paid purchases and barter deliveries use the same retained period inputs.
Pending deliveries contribute nothing until their actual application period.

## What the evidence means

- Unmet demand prevented plus additional closing reserves equals imported units.
- A later period consuming those reserves does not receive fresh import credit.
- A recovery caused by local production is not credited to imports.
- The result is a period-local marginal comparison. It does not simulate an alternate
  history of population, production or later prices.
- It does not prove an appeal attracted the supplier, assign benefit to an individual
  player or distinguish payment types. This comparison assigns no player rewards.
  The follow-up in PRIORITY_LEARNING.md uses verified earlier delivery impacts for
  a bounded future food-priority bonus, with separately saved replay inputs.
- A reserve-only delivery can be useful later; zero immediate relief is not failure.

## Persistence and verification

The outcome comparison itself requires no schema or saved behavior changes.
The learning follow-up uses schema 25 as described in PRIORITY_LEARNING.md.
Schema 24 already retains the exact opening
stock, production, imports and all demand components in transactional economy
history. The immutable cached view computes this evidence without SQL on the Paper
thread. Restart and later consumption reconstruct the original result from those
inputs. Existing priority replay version 1 remains untouched. No config or database
reset is required; there are no new tuning numbers.

Domain tests cover partial relief, excess reserves, independent farm recovery, idle
demand, existing reserves, maximum quantities, contradictory reports and 2,400
seeded periods. A temporary SQLite restart fixture preserves old outcomes after
later consumption. The disposable Paper regression fixture asserts the exact impact
of the verified twelve-unit native donation and exercises the admin diagnostic.

Phase 2 remains 3/4 complete. This supplies trustworthy aggregate outcome evidence;
bounded configurable food-priority learning is now implemented separately. Broader
explicit feasible-response comparison/execution explanations still need validation before
the autonomous-decision reporting group can be checked off.
