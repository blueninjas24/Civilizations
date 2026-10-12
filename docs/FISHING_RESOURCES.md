# Renewable fishing resources — calculation foundation

Approved October 12: “Yes the fish stock should ebb and flow just like real life.”
Suitable observed open water supports a saved renewable supply; catches reduce it,
rest allows gradual recovery, and heavy harvesting can temporarily exhaust it.
Abstract catches do not require one visible Minecraft fish entity per food unit.
Server downtime and absent worlds must earn no recovery or fishing work.

## Implemented in this batch

`FishingPolicy` and immutable `FishStock` are plain Java calculations independent
of Paper and SQLite. Opening requires an enabled policy and a caller's complete,
suitable, distinct open-water column count. This is a trusted adapter contract,
**not an implemented water survey or permission to claim player ponds**. Opening
rejects insufficient/incomplete evidence; capacity and starting availability are
bounded and separate from catch speed.

Each call advances exactly one earned active-world industry period, recovering
stock first and then calculating catches. Catch is limited by available stock,
requested work, actual qualified workers, site throughput and free food storage.
The future adapter must establish the worker/storage facts; the arithmetic cannot
independently certify employment, stock ownership or reservations.
The result explains all limits and returns resource stock, recovered units, caught
food units and required storage space. No cash is created or spent. No Bukkit
entities, inventories, blocks, chunks or database rows are changed by this code.

Fractional recovery is retained in integer basis-point remainder, so small habitats
do not lose renewal to rounding. Capacity cannot be exceeded. A full habitat drops
unused recovery rather than banking it for later catches. An exhausted fishery can
recover without workers; catches still require workers. Disabled policy or absent
world returns the exact unchanged state. Retuning opening abundance never alters
an existing state's capacity or refills it. The future owner-clock integration must
also avoid calling this transition for server downtime, unknown availability or
unearned periods; this domain class cannot independently establish server uptime.

## Editable prototype settings

The optional `fishing-resources` section in `economy.yml` is strictly validated
before database startup. Missing sections in installed files opt out; files are
never rewritten. Bundled settings describe the calculation foundation and do not
currently enable a live fishery.

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

Persist site identity, capacity, availability, fractional recovery and audited
period results with transactional migration/restart/replay coverage. Observe
suitable open water without forced chunk loads, reject overlapping resource
claims and retain unknown evidence correctly. Connect qualified founding, actual
business-owned food output, protected grants, ordinary industry clocks, real
customers and diagnostics. Verify rollback, absence, downtime and native adapter
behavior before claiming fishing operates in Minecraft. No schema change or live
server deployment belongs to this calculation batch. Construction/docks and
visible work points remain Phase 4. Phase 3's broader industry milestone remains
incomplete; resource arithmetic alone does not close it.
