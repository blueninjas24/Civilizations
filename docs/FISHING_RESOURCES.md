# Renewable fishing resources — saved stock and timed catches

Approved October 12: “Yes the fish stock should ebb and flow just like real life.”
Suitable observed open water supports a saved renewable supply; catches reduce it,
rest allows gradual recovery, and heavy harvesting can temporarily exhaust it.
Abstract catches do not require one visible Minecraft fish entity per food unit.
Server downtime and absent worlds must earn no recovery or fishing work.

## Persistence and owner-clock integration

Schema66 adds initially empty `fishery_resources`, `fishery_columns` and
`fishery_work` tables plus a site-column index. Transactional upgrade retains all
old data. Resources belong to permanent saved sites/worlds; distinct world/x/z
column claims reject overlap, including a late conflict after partial inserts.
This is conservative identity protection, not a decision about public fishing
rights or a live water-suitability classifier. Opening requires trusted complete
evidence; repeat visits or opening-policy edits never refill/extend an existing
resource. Changed footprints require an explicit later transition.

`FisheryRepository` saves capacity, available stock, fractional recovery, original
terms and column identity. `IndustryRepository.fish` is a trusted earned-period
operation: actual living/local employees are checked against saved qualifications,
owned food storage includes pending reserved space, and catch/recovery, food stock,
receipt, production period and resource audit commit together. Retries with the
same terms return the old receipt without another catch; stale periods and reused
IDs with changed terms are rejected. No fish is taken from village reserves and
no cash is created. Existing real village food purchases can buy owned catches;
paid cargo arrives before needs consume it.

`IndustryRuntime`/`PopulationSession` now run saved habitat-backed fishing firms
through the normal bounded SQL-owner clock. Downtime/absent-world handling and
already-earned queues retain the existing clock contract. Resource-disabled periods
advance bookkeeping with unchanged stock, preventing later disabled-period catch-up.
Ecology can recover while a present-world firm rests or its owner is unavailable;
unqualified/unavailable workers, a missing workplace marker or a closed business
never catch. Existing optional tool upkeep and productive skill learning apply
when the stored catalogs/configuration support them. Old fishing recipes without
saved habitats retain their existing path; no fake habitat is backfilled.

Automatic water observation, qualified first founders and atomic fishery startup
remain unfinished. This does not make every fisherman profession a producer,
certify player ponds, scan/load chunks, claim buildings or change world blocks.
The strict optional resource settings are connected at plugin startup; old missing
sections remain disabled and installed files are never rewritten.

## Resource calculations

`FishingPolicy` and immutable `FishStock` are plain Java calculations independent
of Paper and SQLite. Opening requires an enabled policy and a caller's complete,
suitable, distinct open-water column count. This is a trusted adapter contract,
**not an implemented water survey or permission to claim player ponds**. Opening
rejects insufficient/incomplete evidence; capacity and starting availability are
bounded and separate from catch speed.

Each call advances exactly one earned active-world industry period, recovering
stock first and then calculating catches. Catch is limited by available stock,
requested work, actual qualified workers, site throughput and free food storage.
The persistence adapter establishes worker/storage facts; the arithmetic cannot
independently certify employment, stock ownership or reservations.
The result explains all limits and returns resource stock, recovered units, caught
food units and required storage space. No cash is created or spent. No Bukkit
entities, inventories, blocks, chunks or database rows are changed by this code.

Fractional recovery is retained in integer basis-point remainder, so small habitats
do not lose renewal to rounding. Capacity cannot be exceeded. A full habitat drops
unused recovery rather than banking it for later catches. An exhausted fishery can
recover without workers; catches still require workers. Disabled policy or absent
world returns the exact unchanged state. Retuning opening abundance never alters
an existing state's capacity or refills it. The owner clock avoids calling the
transition for downtime and world absence; this domain class cannot independently
establish server uptime. Explicit/manual earned-period callers remain trusted.

## Editable prototype settings

The optional `fishing-resources` section in `economy.yml` is strictly validated
before database startup. Missing sections in installed files opt out; files are
never rewritten. Bundled settings allow work for a saved habitat-backed firm;
automatic live discovery/founding remains unfinished.

| Setting | Default | Meaning |
|---|---:|---|
| `enabled` | true | Allows the resource calculation |
| `minimum-water-columns` | 16 | Minimum distinct suitable observed columns |
| `stock-per-water-column` | 8 | Abstract capacity per suitable column |
| `maximum-stock` | 4096 | Maximum capacity of one new fishery |
| `initial-stock-percent` | 75 | Initial percentage of new capacity, 0–100 |
| `recovery-basis-points` | 200 | 2% of saved capacity per earned period, 0–10000 |
| `catch-per-worker` | 4 | Maximum food units per qualified worker/period |
| `site-catch-capacity` | 16 | Maximum food units caught at the site/period |

Example: 40 suitable columns give capacity 320, opening availability 240 and
recovery 6.4 units/period. Three qualified workers can catch at most 12 units, so
sustained work reduces supply. Four workers attempting 16 units eventually empty
it and become limited by replenishment. Resting for 50 earned periods restores
an empty habitat to capacity. Storage can impose a smaller catch limit. These
are configurable prototypes, not final balance or a biological population model.
Recovery and initial stock can be zero for an admin's famine experiment.

## Remaining integration

Observe suitable open water without forced chunk loads, reject overlapping resource
claims and retain unknown evidence correctly. Connect qualified founding and
protected grants atomically with the saved resource. Stock/owned food/timed work
and real customers are now connected, with temporary SQLite rollback/restart/pause
coverage. Full Windows Wrapper verify passed1,210 tests; guarded native normal
SQL-owner timer check passed with absence/downtime/disabled restart safety, healthy
schema66 and audited main-save preservation. Native water suitability scanning was
not tested: the helper used explicit fixture columns and changed no world blocks.
Confirmed habitat-loss behavior awaits a user decision before automatic startup.
No human server deployment. Construction/docks and
visible work points remain Phase 4. Phase 3's broader industry milestone remains
incomplete; resource arithmetic alone does not close it.
