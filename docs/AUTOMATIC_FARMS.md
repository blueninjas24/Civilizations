# Automatic village farms — Phase 3 prototype

The running plugin can register one persistent field enterprise per settlement,
choose an available qualified adult founder, pay startup capital from the existing
village treasury, and hire available residents. A complete crop survey supplies an
actual representative crop position. Partial observations, unknown old positions,
insufficient fields, occupied sites, unavailable founders and insufficient capital
give explicit blocked reasons instead of creating a business or inventing money.

The enterprise represents the village's observed fields together. It is not a
building, land claim, chest inventory or permission to alter a player's crops.
Production remains abstract food units supported by actual planted farmland.
Automatic founding of mines, warehouses and workshops is still future work.

## Owner settings

Newly generated economy.yml includes:

```yaml
farm-businesses:
  enabled: true
  worker-capacity: 4
  startup-trade-value-hundredths: 2500
  minimum-age-months: 216
  minimum-farming-experience: 0
```

These are configurable prototype choices under the user's economy-tuning approval,
not locked balance rules. 2500 hundredths means 25 Trade Value. Age eligibility is
at least the configured population adulthood threshold. Zero required experience
allows beginners; hiring does not create experience or training rewards.

Existing owner files are never rewritten. Without this whole section they retain
legacy field accounting with automatic farm businesses disabled. To opt in, add
the complete section and restart. First finish any already earned economic backlog
under the old settings; unfinished backlog blocks retuning rather than reinterpreting
its history. Policy changes record a forward tuning transition. Existing businesses'
saved capacity, founding requirements and paid startup terms remain unchanged;
automatic expansion, requalification or refunds are not implemented.

Enabling this section changes food labor to actual assigned farm workers. Food is
not produced by every adult as well. With defaults, 32 planted crop blocks provide
two field slots; two workers produce eight abstract food units per period. Output
is bounded by field slots, available assigned workers, units-per-worker and
output-percent. A town without a viable funded farm produces zero local farm food
under this mode. Existing reserves and verified food deliveries remain available.

## Persistence and safety

- Schema 30 adds nullable crop-site coordinates to farm_evidence transactionally.
  Existing observations retain unknown coordinates until a fresh real survey;
  business, population, money and historical records are preserved.
- Stable town-derived farm/site identities survive restart and later surveys.
  Stable employment episode identities support retry without duplicate hiring.
- Founding, its site/requirements and actual startup debit share one transaction.
  Each subsequent hire is transactional. Food accounting is a separate period
  transaction. If that period fails, the successfully established business/jobs
  remain valid; retry does not charge startup twice or duplicate the food period.
- Residents can have only one open primary job. Dead/moved workers are excluded
  and their ended episodes retained; eligible replacements can be hired.
- Smaller fields lower output without silently firing existing workers. Ownership
  succession, closure, wages, personal wallets, business revenue and farm expansion
  are not included. Owner death is diagnostic, not an invented inheritance rule.
- Server downtime and absent worlds earn no progress. Already earned work waits
  for world return. Loaded-world unloaded chunks continue data-only simulation
  using saved reliable evidence; production observation never force-loads chunks.

## Diagnostics and verification

Admins use `/civ debug economy <town UUID>` for output and farm reasons and
`/civ debug businesses <town UUID>` for owner, staffing, capacity and saved site.
Business cache details refresh on a bounded rotation and may lag one or more pulses.
These commands do not create a farm themselves.

AutomaticFarmTest uses temporary SQLite databases for founding, all saved fields,
funding/qualification failures, partial evidence, staffing/replacement, once-only
charges, separate transaction failure/retry, restart/pause, schema-29 upgrade and
240-period food conservation. Manual FarmBusinessProbe lives under src/test/paper,
outside the shipped JAR. Its marker/localhost/no-player guarded disposable fixture
uses real crop snapshots and a separate session/database. Its chunk tickets and
temporary villagers are test setup only, not production behavior. Human client
survival balance and multi-town field usability remain release validation.
