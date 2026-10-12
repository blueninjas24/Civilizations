# First mineral-chain warehouse opportunity review

The SQL owner can now review a narrow first-warehouse prospect without creating,
reserving or spending anything. This connects investment evidence, not automatic
merchant training/ordinary scheduling; qualified acceptance is now implemented
separately in WAREHOUSE_INVESTMENT.md. It is deliberately limited to the existing coal/iron
workshop chain; broader producer catalogs and business competition remain later work.

A prospect requires:

- Enabled timed industry, workshop purchasing, warehouse restocking and automatic mines.
- A current complete nearby unclaimed barrel observation in the saved world.
- No earlier warehouse identity in this town, including a closed one.
- Actual operating/staffed workshop input gaps after owned and incoming goods.
- Real protected public startup funding, under the shared current-period/lifetime caps.
- Enough actual unprotected treasury and remaining lifetime grants for the warehouse
  plus one needed supplier per mineral, without assuming future public income.
- Configured prices, warehouse capital/storage and local hauling that cover the
  whole reviewed gap; future mine extraction bundles must fit their saved catalog.
- Current trusted, unexcluded, unclaimed ore for every missing input, within the
  configured mine range and outside existing business sites, with enough finite
  configured deposit potential in one connected patch per mineral to cover the
  initial input gap. Disconnected deposits are not pooled into one supplier.

Every survey/marker/candidate page is bounded. Unknown worlds, stale/future/partial
evidence, player placement/removal, mutation gaps, unsupported inputs, exhausted
budgets and incomplete source coverage produce explanations rather than guessed
stock, funds or a partial feasible chain. Repeated review and restart retain the
same evidence; review makes no database changes. Warehouse and mine grant previews
are separate current feasibility checks, not reservations or a promise that both
can be funded in one period. A workshop can consume the period's grant allowance;
the next real food period renews the allowance without creating village money.

`warehouse-opportunities` is optional in economy.yml:

```yaml
warehouse-opportunities:
  enabled: true
  markers-per-review: 8
  evidence-lifetime-millis: 60000
  horizontal-range: 96
  vertical-range: 48
  startup-hundredths: 500
  capital-hundredths: 4000
  storage-capacity: 1024
```

Terms are editable prototypes under the approved configurable economy/grant
direction. Missing old sections disable the review; invalid types, missing/extra
keys and bounds fail before database startup. No settings file is overwritten.
Other industry/mine policies still supply prices, yields, observation bounds,
storage weights, transport and upstream grant terms. No migration: schema62.

`PopulationSession.currentWarehouseOpportunities(town)` is a read-only internal
API. No ordinary warehouse-founding scheduler invokes it yet. A positive prospect
does not guarantee later skilled merchants/miners, affordable successive grants,
production, sales or profit. Qualified current-block acceptance is now implemented; real course/ordinary
scheduler integration remains next; no invisible inventory or new construction is claimed.

Tests follow the actual paid workshop-to-farm toolkit transaction, then review its
funded raw-material gap. They use temporary SQLite, explicit observed ore and a
real next food period. Exact results are in PROJECT_STATUS and DEVLOG.
