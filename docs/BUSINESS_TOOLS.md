# Business tool upkeep

The user approved productive businesses paying for gradually worn tools, editable
rates, a startup grace period and slower work when tools are unavailable. Existing
timed mines/workshops now implement this rule. This establishes a real maintenance
customer for blacksmith output; it never consumes goods simply to clear a full shop.

## Easy tuning

New economy.yml files include this section. Existing files remain disabled until
the owner adds it while stopped and restarts:

```yaml
business-tools:
  enabled: true
  startup-grace-periods: 3
  work-periods-per-tool: 8
  missing-tools-output-percent: 50
  tool-value-hundredths: 4000
  industries: [mining, blacksmith]
```

One abstract toolkit services a firm's productive work periods, rather than one
physical pickaxe per worker/block. Three successful periods give startup breathing
room; each actual toolkit then supports eight successful work periods. No tools
means 50% of the eligible worker/site batch limit. Fractions are retained, so a
one-batch firm can still work every other period. Grace 0–100000, service 1–100000,
output 1–100% and price 1–100000000 hundredths are validated. Industries are a
distinct bounded list of timed production kinds. Farms' separate food cycle and
warehouses are outside this first maintenance adapter.

4000 hundredths = 40 Trade Value. This tool setting supplies the same price on
both producer → warehouse and warehouse → customer legs, and overrides the tools
entry for industry purchasing when upkeep is enabled. Other editable base prices
are unchanged. There is no markup, transport fee, salary, subsidy or individual
wallet. It adds no tools to village starting reserves. New default industry storage
catalogs support tools with weight one; retained stores are never rewritten.
An existing store without tool support keeps its old work behavior with a diagnostic;
it needs a separately reviewed explicit store transition before joining upkeep.

## Saved work and genuine demand

Startup grace counts only positive production receipts after enabling upkeep.
Idle/failed periods cannot consume a kit or increase productive periods. Partial
slow-work fractions are bounded below one batch; they are not stock or sales.
Original offline/world-pause, staff, capacity, source-trust and finite-input guards
still apply. Depleted/ambiguous/untrusted mines create no replacement demand.

When saved service is near expiry, an eligible staffed firm can buy one owned or
paid pending spare. It pays from its own available business cash, supports the
commodity and has actual storage. Only delivered goods can start service. Paid
pending goods reduce ordering pressure without being usable. A new kit is consumed
only alongside a positive productive receipt, with a distinct maintenance receipt.
Existing stock/service is used before another purchase. Future production or
promised mining reserves cannot fund a payment.

Funded maintenance and recipe input demands share business money/storage, so they
cannot both claim the same cash. Warehouse available/pending stock is counted once
across customers. Warehouses may buy configured workshops' actual output as well
as saved mineral-yield stock from mines. They do not resell producers' ingredients
or their required maintenance kit. A staffed tool maker retains one due kit for
its own work. Unsupported source/customer review defers conservatively.

Money, cargo, actual work, tool consumption, service/fractions, audit and checkpoint
commit together. A failed checkpoint rolls the whole period back. Restart/world
absence grants no wear, production or payment. Disabling upkeep preserves state
and accepted cargo; re-enabling continues service. Price/rate edits apply forward;
an existing kit retains its remaining service. Raising grace deliberately extends
the forward grace threshold rather than resetting counters. Saved reviews retain
the policy used. Existing historical work does not acquire invented wear records.

Recipes in an upkeep industry cannot also use tools as an ingredient; that needs
a separate reservation/consumption design. Outputting tools is supported. Existing
store terms, finite reserves and old receipts remain immutable. This is not private
business founding, mine excavation, every industry, wages or regional transport.

## Persistence and validation

Schema 51 adds empty `business_tools` and `business_tool_reviews` tables in the
versioned transaction. There is no backfill, manufactured kit or asset rewrite.
Temporary SQLite tests cover real funds/goods, delayed replacement, fractional
work, idle/depleted operations, rollback/replay, restart/disable/world absence,
pending coverage, retained catalogs, producer protection and schema50 failure/retry.
Domain tests cover grace/wear/fractions/retuning and configuration bounds.
The separate WarehouseSupplyProbe adds an independent tool-run DB and uses actual
PopulationSession owners inside disposable Paper. See DEVLOG.md for completed results.
