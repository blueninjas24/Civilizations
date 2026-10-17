# Bakery ingredients and paid customers

Bakeries can use real grain, milk and eggs. These are saved business goods, not
items taken from player chests. Herds earn products through the existing feed,
labor, breeding and ownership rules. Warehouses pay producers, then bakeries pay
warehouses for delayed deliveries. Bakery meals use the existing paid village
food-order system. No new customer money, free ingredients or extra production
period is created.

The editable defaults in `economy.yml` are:

| Recipe | Ingredients per batch | Meals per batch |
|---|---|---|
| `bakery_bread` | 3 grain | 3 food units |
| `bakery_cake` | 3 grain, 1 milk, 1 egg | 8 food units |

Cake is an abstract food recipe, not a physical cake item or vanilla crafting
recipe. Business milk does not create buckets. The separate player milk market
still requires the player's own bucket. There is no spoilage.

A new bakery starts with its simplest meal recipe, so livestock is not required
to start baking bread. Existing bakeries prefer recipes with more ingredient
types, then recipe ID for a stable tie-break. Optional purchases require enough
actual held/incoming goods and available local producer/warehouse stock for a
batch, real cash and storage. This is a bounded prototype choice, not a profit
optimizer or guaranteed supply reservation. Actual orders recheck money, goods,
transport and capacity. Production selects a feasible recipe using delivered
stock only. It can make bread while milk/eggs are unavailable or still in transit.
Only one recipe runs per earned business period, using ordinary workers/tools.
Receipts retain the selected recipe and exact consumed/produced quantities.

## Existing installations

Installed files are never rewritten. With the server stopped, back up the plugin
folder and add the desired recipe under `recipes`, plus the new ingredient
weights under `industry.storage-space-per-unit`:

```yaml
milk: 1
eggs: 1
```

Add the optional business-only prices inside the existing `industry-supply`
section (100 hundredths = 1 Trade Value):

```yaml
input-unit-hundredths:
  milk: 150
  eggs: 75
```

Use the bundled `bakery_cake` recipe above as the editable example. Preserve the
existing bread recipe. Do not add milk/eggs to `starting-units-per-resident` or
the public `prices.base-trade-value` just to enable this business trade: doing so
changes the saved public commodity catalogue and requires separate review.

When timed industry starts with configured milk/egg recipes and storage weights,
saved bakery and warehouse stores receive missing **empty** ingredient slots in
the SQL-owner transaction. Each addition records a deterministic zero-quantity
receipt in `industry_operations`, retaining original opening terms. It never
changes existing units, unit weights, cash, capacity, shipments or history.
Repeated starts do not add goods or receipts; changing config weights never
rescales saved stock. If an audited slot subsequently disappears, the transition
fails for reviewed recovery instead of recreating potentially lost goods.
Without opt-in, old catalogues remain unchanged and bread continues normally.
Schema stays 89; this uses the existing transactional stock/operation tables.

## Remaining scope

Physical cake inventories, visible construction and regional deliveries are not
part of this batch. Existing feed/food limits can still cause shortages; optional
recipes do not guarantee bakery profitability. Controlled slaughter and bulk pen
donation remain separate livestock work. Validate server balance through play.
