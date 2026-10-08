# Phase 2 — live food accounting

Villages with reliably observed planted fields now have a saved food economy.
This is partial labor/production work, not the completed Phase 2 milestone.

## Evidence and prototype rules

The main-thread observer reads already loaded chunk snapshots around saved household
homes. Wheat, carrots, potatoes and beetroot count at any growth stage when supported
by farmland. Default coverage is a horizontal 32-block circle and 16 blocks above or
below each home. Sixteen planted crops provide one worker slot. Output is bounded by
that capacity and saved living adults; children consume food but do not supply labor.
These are editable prototype capacity rules, not business hiring or farm ownership.

The observer snapshots at most one chunk per tick, reads at most 4,096 positions per
tick with a one-millisecond target, and rejects excessive footprints. Snapshot creation
and completion processing are indivisible: this is not a guaranteed tick deadline.
Large-server profiling remains necessary. Missing coverage, shared fields near two
saved towns and scan failures pause accounting instead of inventing a zero-food farm.
Complete zero-crop evidence means known zero production. Partial evidence discards
unprocessed economic time while preserving reserves and completed history.

Entirely unloaded villages retain their last reliable field capacity and simulate
without loading chunks. They cannot notice later edits until reliably observed again.
No production code harvests crops, edits player blocks, takes chest items or claims land.
An observed field is capacity evidence only; actual maturity/harvest behavior belongs
to future business/world integration. Existing unrelated residents are not newly paired
or relocated by the food economy.

## Saved time and settings

Food uses an independent 60-second economic period. One existing SQL owner schedules
bounded work; no JDBC runs in the Paper observer or command. Stocks, inputs and period
checkpoint commit together. Retried saves cannot duplicate production or consumption.
Fractional elapsed time and queued catch-up survive restart. Offline catch-up defaults
to 120 periods. New reliable enrollment seeds supplies once per actual living resident;
old settlement census counts and deceased people do not supply labor or demand.

Changing economy settings while stopped records a forward transition on restart.
Reserves, people, shortage streaks and original period explanations survive. Settings
transitions skip the offline interval and fractional remainder at that boundary and
record discarded milliseconds. Drain queued catch-up with the old file before retuning;
otherwise startup rejects the change with an explicit diagnostic. Existing commodity
catalogs cannot be silently replaced. Starting-supply edits never refill old accounts.

Schema 15 adds farm evidence, economic clocks and tuning history. Schema 14 history
receives its original saved policy before new periods store their own thresholds.
There is no destructive reset. Back up saves before upgrades using the documented
consistent SQLite backup command.

Catch-up uses current saved residents and last reliable capacity, not a historical
reconstruction of every demographic/economic event interleaving. Startup may process
an economic period before the full population backlog drains; normal pulses wait for
population catch-up. Do not describe this approximation as exact historic food usage.

## Inspection and remaining work

`/civ debug economy <settlement UUID>` reports saved periods, queued work, field
evidence, reserves, shortage explanations and indicative prices from immutable caches.
An uninitialized village is reported as awaiting reliable evidence, not starving.

Subsequent schema 16 work adds household needs snapshots and explicit treasury
accounting; see HOUSEHOLD_NEEDS_AND_TREASURY.md.

Only food has automatic flows. Other commodity stocks remain idle. Scarcity prices
are diagnostic Trade Values, not merchant offers. Automatic treasury funding,
household allocation/full wellbeing, famine mortality, business employment,
autonomous responses and player transactions remain pending. Observable household
needs are now saved separately. A synthetic Paper probe validates food behavior;
human survival playtesting and tuning remain outstanding.
