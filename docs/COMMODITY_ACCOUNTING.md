# Phase 2 — commodity accounting foundation

Verified October 8, 2026. This is the first Phase 2 reporting milestone, not a
running village economy or a completed phase. Phase 1 gameplay remains enabled.

## Implemented

Plain-Java `Commodity` keys identify food, timber, stone, iron, coal and livestock
products; additional stable keys are allowed. These are abstract commodities,
not a Minecraft item catalog, crafting conversion or price list.

`CommodityAccounting` processes one explicitly supplied economic period:

- Available goods = opening reserves + local production + imports.
- Requested demand = household uses + industry inputs + construction + exports + events.
- Fulfilled demand = the smaller of available goods and requested demand.
- Closing reserves = available goods minus fulfilled demand; unmet demand is retained
  in the period report. Unmet demand is not automatically debt carried into tomorrow.
- A deficit increments that commodity's consecutive shortage periods. A fully supplied
  period resets the streak, including a period with no demand.
- Caller-supplied quantitative thresholds label Severe Shortage, Shortage, Balanced,
  Surplus or Abundant. Severe requires both sufficiently poor coverage and sufficient
  duration. With no demand, the label is Balanced: idle reserves alone do not prove
  a market opportunity.

The calculation includes reserves before declaring shortages. Stocks never become
negative; checked integer arithmetic fails on overflow. Exact ratio comparisons
avoid floating-point drift and multiplication overflow. Immutable snapshots and
stable commodity ordering make replay independent of caller map order.

There are **no runtime default quantities or thresholds**. Test fixtures use
illustrative inputs only. All initialized commodities require explicit period inputs;
missing evidence is not silently converted to zero production/consumption.
Requested uses are aggregated; this does not choose how scarce goods are rationed
between families, exports and industry. A requested export is not a completed shipment.

## Persistence and upgrade

Transactional schema 14 adds `economy_accounts`, `economy_stocks` and
`economy_periods`. Existing settlements, residents, families, entity aliases and
population history are preserved. Existing villages receive no fabricated inventory.
An absent account means uninitialized economy, not a village with zero food.

`CommodityRepository` belongs on the existing SQL owner thread. Its explicit APIs:

- `initialize`: seed an already existing settlement with caller-approved reserves and
  policy. Duplicate initialization fails instead of replacing goods/settings.
- `load`: consistent saved policy, reserves, shortage streaks and completed periods.
- `advance`: consume one consecutive period's explicit inputs. Repeated/skipped work
  and policy changes fail. Stocks, retained inputs and checkpoint commit together.
- `history`: reconstruct exact period calculations/explanations from retained inputs
  using protected policy/replay version 1. No silent tuning transition is supported.

Economic periods are separate from demographic months; no cadence, scheduler,
automatic enrollment, live effects, command, merchant or inventory mutation is wired.
Production inputs must eventually come from plausible observed/owned facilities and
labor. This API does not prove production capacity or invent offscreen farms/mines.

## Validation

Windows `.\mvnw.cmd clean verify -B -ntp` passed **242 cases**: 240 unit and two
packaged integration, zero failures/errors/skips. Eighteen added cases cover exact
accounting, commodity-specific escalation/recovery, invalid/overflow inputs, immutable
ordering, safe initialization, restart/history, stale periods, protected policy,
write rollback/retry, schema-13 upgrades and malformed schema detection.

Two 1,200-period scenarios exercise reserve drawdown, a crisis and import recovery.
The saved scenario restarts during the shortage and reproduces its original report;
the pure scenario checks conservation on every step. Calling these a century is a
fixture convention of twelve periods/year, not a chosen live economic time scale.
The saved scenario took approximately 14.7 seconds including test setup on this
Windows/OneDrive machine; no server tick or large-city performance claim follows.

Backed up the explicitly marked disposable Paper database before deploying. Paper
26.2 build 129/Java 25 upgraded schema 13 to 14, enabled population and diagnostics,
left fixture chunks unloaded with no forced chunks, and stopped cleanly. Read-only
comparison with the backup preserved original settlement/person/household/family/entity
identities. Integrity was OK, foreign-key violations zero, economy account count zero.
No production database/world reset, remote push or merge.

## Next gate

Approve editable experimental economy quantities/cadence/thresholds before live
consumption and pricing. Then implement evidence-backed production and labor,
treasury versus household wellbeing, bounded economic scheduling and explanations.
Food crisis population effects, material allocation priority, prices, barter and
autonomous response execution still need implementation and appropriate decisions.
