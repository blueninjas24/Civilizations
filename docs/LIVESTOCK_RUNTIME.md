# Timed herd work and paid feed

Existing saved, qualified livestock businesses now run on the shared bounded
industry clock. This connects their real feed, milk/eggs, logical births, saved
maturity and physical newborn reconciliation. The separate approved adoption/
funding adapter now supplies eligible new businesses (LIVESTOCK_ADOPTION.md).
Intentional slaughter remains unfinished.

## Operation

At an earned industry period, due owned cargo arrives first. The herd forecasts
care/birth feed using the same deterministic herd calculator and actual qualified
workers, work limits and confirmed-away animals. It orders only missing feed
coverage from operating local warehouses, using its own actual cash and the
configured commodity price. Existing stock and incoming feed reduce orders.

Purchases share the existing local haul quota, delivery delay, cargo limit and
finite storage reservations. Real seller stock and buyer money move once; cargo
cannot be used before arrival. No public village supplies or treasury are taken,
and no money, feed or artificial recipe output is created. An unpriced commodity,
unavailable warehouse, insufficient cash/labor/space/haul or disabled input
purchasing simply limits orders. Buying is bounded to the configured warehouse
review and128 pending shipments; the per-period feed forecast is bounded to one
million units. Grain remains an actual separately harvested commodity.

Actual production uses only delivered feed, qualified labor and current unreserved
storage. Its existing calculator controls milk/eggs and breeding. Saved birth IDs
and the existing nearby newborn adapter handle visible bodies independently;
missing bodies are not replaced. Deliberate slaughter is still disabled pending
physical retirement with the approved no-duplicate-drops accounting.

Delivery, purchases, feed consumption, products, births, applied losses and the
industry work checkpoint/history commit in one transaction. A late failure rolls
them all back. New herd clocks start above their saved manual herd periods, so
enabling scheduling never replays old work. Existing generic custom `livestock`
recipes retain their old path while herd work is disabled; enabling dedicated
herd work alongside one fails explicitly instead of mixing the two systems.

Only loaded worlds earn time. Startup skips server downtime; disabled/untrusted
herd work earns no time. A missing/unloaded animal alone is not death or departure.
The durable capture fence and immediate pending-death fence gate live herd work;
unclean/gapped history requires reviewed recovery, never silent certification.

## Configuration

New `economy.yml` defaults include:

```yaml
livestock-runtime:
  enabled: true
  work-per-worker: 10
  site-work-limit: 40
```

Installed files without this section keep scheduled herd work disabled and are
not rewritten. Both `industry-runtime.enabled` and `livestock.enabled` must also
permit work. Care/breeding terms stay in `livestock`; the positive-sighting radius
stays in `livestock-working-area`. Feed coverage/review limits use `industry-supply`,
prices use `prices.base-trade-value`, and delivery/hauling use existing industry
settings. Edit while stopped and restart; no new command-based tuning required.
The original scheduling batch retained schema88. The subsequent schema89 learning
integration adds only care-experience audit tables; see LIVESTOCK_LEARNING.md and
PROJECT_STATUS.md for verification. No herd, animal, stock or historical reset.

## Verification

Four new temporary-SQLite end-to-end scenarios cover paid feed/cargo before
production, every-step goods/cash conservation, replay/restart, absent worlds,
untrusted history, missing funds/qualification, manual-period enrollment and late
clock-write rollback/retry. One new configuration case covers editable limits,
strict types and installed-file opt-out. The initial checks caught a fixture cash
column typo and missing root-key registration; both were fixed.

Final Windows Wrapper scoped verify passed126 affected unit plus2 packaged
integration tests (128 total), zero failures/errors/skips;43.518 seconds,
finished2026-10-17T12:26:27-04:00. This is not a new full-suite run. The complete
Windows/Linux workflow remains enabled after feature pushes.

Guarded Paper26.2 build129 passed14:44:59 EDT using the actual packaged
PopulationSession and a fresh private synthetic herd/warehouse save. Seven
automatic industry periods bought/delivered/consumed real feed and produced milk/
saved births; stock/cash stayed conserved. Restart, absent-world and explicit
capture-gap checks produced no additional goods or work. This tests owner-clock
wiring, not automatic animal discovery or actual physical slaughter. No blocks or
animals were changed. All149 main test-save tables retained/145 exact; only normal
clock/runtime/capture/ore metadata changed, no aging. Integrity/FKs clean; server
stopped, original settings restored, helper archived and backups retained. The
human Minecraft server was untouched.
