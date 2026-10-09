# Timed operation of existing industries

IndustryRuntime is a bounded SQL-owner loop for existing funded mine/workshop
businesses with saved storage. It does not found businesses, discover hidden ore,
create buyers, grant money or place Minecraft blocks. Farms retain their separate
observed-field food runtime. This advances the second Phase 3 reporting group;
automatic nonfood founding and local customer connections remain unfinished.

New economy.yml defaults enable one work period every 30 real seconds, at most eight
firms completing one earned period each per owner pulse. Old files without the new
section remain disabled, with no automatic rewrite:

```yaml
industry-runtime:
  enabled: true
  real-millis-per-period: 30000
  businesses-per-pulse: 8
```

Industry/recipe text settings control work rates. Saved deposits retain their original
finite yields; old storage retains its catalog/capacity. At most one runtime recipe
per industry is supported. Ambiguous recipes/invalid settings fail before startup.
Recipe removal pauses that existing industry rather than deleting it.

Each firm has a saved online clock, remainder, earned target and completed period.
Only present worlds earn new time. Unloaded chunks in present worlds still work
entirely in data. World transitions accrue the old mask before changing it. Already
earned work remains queued while its world is absent; returning resumes that work,
without adding absent time. Restart retains queued periods/fractions and skips server
downtime. New firms start now above legacy manual production periods, without
backfilling time before enrollment. Backward wall clocks stay pinned.

Round-robin pending work prevents one firm monopolizing the bounded work budget.
Clocks/enrollment are capped at 4096 inspected rows per observation; exceeding the
budget fails explicitly. This is not a large-server profiling or survival-balance
claim. Long queues process with current saved residents and current settings;
historical staffing is not reconstructed. Work settings affect future executions,
not old receipts. Changing cadence requires an empty queue and records the discarded
fraction, old/new cadence and transition time. Finish pending work under old cadence
before changing it. Every accepted checkpoint and result is durable.

Mine output consumes one saved unambiguous deposit, actual qualified employees and
available storage; byproducts share work and space. Workshops consume owned delivered
inputs, including input-space release, and create bounded products. Full storage,
missing inputs, zero workers and exhaustion yield zero output with explanations;
no fake shopper clears stock. An unavailable owner pauses work without assuming an
inheritance recipient. Stock, money and site/history remain. Owner succession still
needs the user's decision and separate historical ownership persistence.

Work, reserve/stock/input changes, receipt, diagnostic history and runtime checkpoint
commit in one transaction. Injected final-checkpoint failure rolls everything back
for an identical retry. Existing public mine/process APIs retain their idle-owner
transaction contracts; internal row helpers require an active owner transaction.
There is no second DB thread, Paper access, forced chunk loading or simulation worker
spawn. Business diagnostics report the latest runtime outcome, including pauses.
Production failure is logged/diagnosed and retries without blocking food or population.
World boundaries accrue industry time without executing production; an industry
clock boundary failure suspends industry for that session while population still
receives the new world mask. Fix the cause and restart to resume; the uncertain
unpersisted interval is skipped rather than counted as absent-world production.

Schema 35 adds industry_runtime, industry_runtime_history and industry_runtime_tuning
with foreign keys/type/range/checkpoint constraints. Existing businesses, goods,
reserves, identities, provenance and funds are retained. Migration invents no clock
rows or opening goods/capital; only explicit runtime opt-in enrolls existing stores.
Fresh install, prior-schema failure/rollback/recovery and retained industry are tested
using temporary SQLite files. Back up established saves before upgrading; use the
consistent SQLite backup procedure in DEVELOPMENT.md.

Final Windows Wrapper clean verify passed 666 tests (664 unit and two packaged
integration), zero failures/errors/skips. Eleven new runtime/application cases plus
two configuration/policy cases verify failure rollback/isolation, retained masks,
paid delivered inputs, fairness, owner/worker absence, full storage, exhaustion,
retuning and schema-34 upgrade failure/recovery. The guarded disposable Paper probe
passed accelerated real PopulationSession pulses, an explicitly unloaded site,
world absence and restart: three coal, seven remaining batches, three histories,
actual treasury 8500 and store capital 1000. Main schema upgrade retained all old
industry/funds/provenance/settings; integrity/FKs passed, normal stop, no tickets.
A separate fresh player server passed default-settings startup and normal stop.
No natural automatic founding, scheduled NPC customers or human-client claim.
