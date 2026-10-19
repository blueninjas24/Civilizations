# Business storage fittings — Phase 3

## General craftsman product and demand scope

The general craftsman now has two editable new-file recipes for the same
storage-fitting product: three timber plus one stone, or three stone. Both serve
the actual funded business storage-upgrade demand described below. The baseline
recipe ID sorts first and is preferred when its real inputs and storage permit
production; the alternate gives the business another viable input route. These
are abstract goods and existing-capacity improvements, not household furnishings
or physical construction. Existing server configuration files are never rewritten
to add either recipe.

This satisfies the initial Phase 3 craft-workshop scope of editable useful
recipes and real business customers. Household furnishing purchases need a real
household demand/accounting path; physical furniture projects remain Phase 4 work.

Businesses can buy actual craftsman-made fittings to increase the usable capacity
of an existing stockroom. This is the approved limited upgrade rule, not physical
building construction or permission to change player blocks. Gradual first-owner training and qualified automatic founding are now connected.
See PROCESSING_WORKSHOPS.md.

## Real customers and payments

Current staffed, operating nonfarm businesses qualify when their saved stock and
owned incoming cargo occupy the configured share of storage. They must retain
operating cash after paying the full invoice. Another qualified local craftsman
or warehouse must actually own the fittings. No supplier means no purchase.

Payment transfers existing private cash to the seller and removes actual fitting
stock into buyer-owned installation cargo. The receipt retains that asset; it is
not saleable commodity stock at either end. Installation changes capacity only
after the saved delivery period. The entire payment and receipt are atomic, as
are installation and its checkpoint. Shared local hauling includes these orders.
Warehouses do not buy a second fitting to satisfy their own identical upgrade need.
Ordinary input forecasts and upgrade forecasts cannot spend the same cash twice.

Absent worlds and server downtime earn no work. New spending waits for a current
industry clock; old queued work cannot create historical purchases. Accepted
orders retain their terms after disabling new purchases. Replays cannot charge,
deliver or expand twice. Lost staffing or workplace eligibility pauses the order.
Paid pending and installed improvements remain part of business resale valuation.

## Editable defaults and existing servers

New-file `economy.yml` includes:

| Setting under `storage-upgrades` | Prototype default |
|---|---:|
| enabled | true |
| occupancy-percent | 80 |
| capacity-per-upgrade | 256 space units |
| maximum-upgrades | 4 |
| maximum-growth-percent | 100 above original capacity |
| maximum-capacity | 8192 |
| minimum-business-cash | 2500 hundredths of Trade Value |
| fittings-per-upgrade | 1 |
| price-per-fitting | 2000 hundredths (20 Trade Value) |
| delivery-periods | 1 |
| suppliers-per-review | 8 |

All count, original-baseline growth and absolute capacity limits apply together.
For example, a 100-space shop reaches at most 200 under the default growth limit;
a 1024-space store can reach at most 2048 through four 256-space purchases.
Original baseline and accepted increments are retained in the receipt chain.
Unexplained external changes to saved capacity are refused, not silently adopted.

Eligible defaults: mining, blacksmith, warehouse, quarry, sandpit, fishing, bakery
and craftsman. Crop farms and livestock are excluded because their saved capacity
and labor adapters require separate explicit transitions.

The `craft_storage_fittings` recipe consumes three actual timber and one actual
stone to make one fitting, with one batch per qualified worker and a four-batch
site limit. The default ingredient prices total 15 Trade Value and the fitting
sells for 20; this is a prototype gross ingredient margin, not guaranteed profit
or a promise that a buyer always exists. New stores start with zero fittings.

Existing installed files are not overwritten. Missing `storage-upgrades` disables
new purchases. To opt in, copy/tune that section, the fitting recipe, storage
weight, zero opening-stock entry, base price and resale stock-value entry from
the bundled file. Old saved commodity catalogues remain exact: a buyer does not
need a fitting commodity slot to install a paid fitting. A supplier must already
have an explicitly configured fitting catalogue and actual produced stock; this
feature does not silently extend an old producer's catalogue.

## Persistence and remaining work

Schema 80 adds the initially empty `business_storage_upgrades` receipt table and
haul index. It does not rewrite people, businesses, balances, stock or prior
history. Failure rollback/retry preserves predecessor rows. Bounded customer and
supplier reviews defer rather than infer demand from incomplete samples.

The automatic runtime purchase and delayed installation paths are implemented.
Read-only genuine fitting customers also feed local industrial demand. Qualified first-craftsman opportunity review, gradual training and automatic opening
are connected. These paths cannot grant free skills or complete Phase 3.
No physical chests, block placement, inventories, spoilage or extra occupations.
Verification results are recorded in PROJECT_STATUS.md and DEVLOG.md.
