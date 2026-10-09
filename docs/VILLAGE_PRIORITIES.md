# Phase 2 — explainable village priorities

Villages now select and retain a nonbinding priority from actual saved needs:

- **MONITOR:** conditions do not justify another available priority.
- **SEEK_FOOD:** the completed food period has unmet demand.
- **INVESTIGATE_HOUSING:** at least one occupied household exceeds observed capacity.

These are intentions, not executed purchases, project permissions, jobs or buildings.
Seeking food does not invent a supplier/route or spend an unknown treasury. Investigating
housing does not edit player blocks or assume an approved construction site. Unknown
housing and uninitialized economy do not manufacture a shortage. Unknown employment,
leader traits and resource prospects are not fabricated into scores.

## Feasibility and choice

Pure Java takes recorded food unmet demand/condition and observed crowded-home count.
It records each candidate's feasibility, weight, reasonable-choice eligibility and
reason. Infeasible candidates cannot be selected, regardless of seed or weight.

Editable prototype weights favor food pressure over housing investigation; severe
food multiplies food urgency. A minimum percentage of the best feasible weight excludes
irrelevant alternatives, then a seeded weighted draw chooses among remaining candidates.
This allows reasonable variation without always choosing the maximum score. It is
not a full project cost/benefit model or government/leader decision engine.

Defaults in the optional `priorities` section of server `economy.yml`:

```yaml
priorities:
  review-after-periods: 5
  food-weight: 10
  crisis-multiplier: 3
  housing-weight: 6
  monitor-weight: 1
  minimum-relative-percent: 25
```

Under normal shortage plus crowded housing, food and housing priorities are both
eligible. Severe shortage increases food weight enough to exclude weaker default
alternatives. A healthy village with no observed crowding monitors. These weights
are configurable prototype settings under the existing economy authorization.

## Review and persistence

Each village normally reviews after five completed economic periods. A food deficit
appearing/clearing, severity changing, crowding appearing/clearing or changed priority
settings triggers an earlier review on the next completed period. Changes in deficit
quantity alone do not reselect every tick. Partial farm evidence pauses economic
periods, including priority reviews. Unloaded villages use saved evidence with no
world/entity operations.

Population seed, town UUID and economic period derive deterministic randomness under
priority replay version 1. Schema 17 saves each review's facts, policy, chosen action
and seed. Those inputs reproduce all candidate explanations on read. Settings edits
apply forward at a future review; old choices are not rewritten and food-clock elapsed
time is not discarded merely because priority settings changed. Seed changes require
an explicit transition; the existing population seed is already protected on startup.

Food, needs and review save in one transaction. Failed review writes roll back period
effects, while the elapsed target remains retryable. Old schema-16 periods receive no
invented past decisions. Stable timestamps are logical economic periods, not world ticks.

`/civ debug economy <settlement UUID>` displays the last reviewed period, selected
priority and every considered candidate's reason/eligibility/weight from immutable
cached views. A priority may predate today's needs; the review period is shown.

## Remaining decision gate

This completes pressure-to-priority selection and saved explanations, not the full
autonomous response milestone. Future work must evaluate actual counterparties, funds,
materials, labor, sites and ownership before executing a purchase or project. Outcome
feedback, actual response execution, merchant transactions and broader wellbeing are
still pending. No fake successful trade, stock transfer or construction is recorded.

Tests cover blocked choices over 1,000 seeds, reproducibility and alternative reasonable
choices, crisis filtering, early review/cadence, 1,200-period crisis/recovery histories,
transaction rollback/retry, migration and history-preserving tuning. Real Paper checks
cover food-crisis selection, restart preservation, recovery and unloaded operation.


## Food appeal response — October 8 follow-up

SEEK_FOOD now publishes a voluntary appeal in the existing supplies menu when the
latest saved need is unmet. Schema 20 records the governing review, changing deficit
and observed recovery with actual production/import inputs. This response neither
buys goods nor grants construction permissions, and observed recovery is not proof
of causal success. See FOOD_APPEALS.md. Paid responses and adaptive weights remain
future work requiring real feasibility and attributable outcomes.

## Saved learning — October 9 follow-up

Paid food purchases and finite-stock player barter have since been implemented.
Schema 25 now also retains a configurable bounded food-weight bonus from earlier
verified deliveries' exact unmet-demand prevention. No observed appeal recovery is
treated as proof an appeal attracted a supplier. Old reviews retain their original
replay; new reviews save their learning policy/evidence and fail on missing metadata.
See PRIORITY_LEARNING.md for tuning, seeded behavior, validation and remaining scope.

## Published responses and Phase 2 gate — October 9 follow-up

Schema 26 now retains a separate feasible donation/cash/timber/stone/wait comparison
for each enabled food period. Actual funds, pending supplies, idle stock, keep floors
and supported stack value constrain the optional published preference. Player choice
and fresh exact confirmation remain. Real executed receipts are distinct from the
published intention; no supplier success is inferred. Combined native/automated
acceptance closes Phase 2's prototype gate, 4/4 (100%). See FOOD_RESPONSES.md and
PHASE2_ACCEPTANCE.md; construction/industry/regional suppliers remain later work.
