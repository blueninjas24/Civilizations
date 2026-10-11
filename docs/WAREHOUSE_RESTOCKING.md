# Existing warehouse restocking

Existing staffed warehouses can buy actual extracted goods from operating local
mines and configured workshops' actual output to cover funded recipe and tool
maintenance gaps. See BUSINESS_TOOLS.md for the real maintenance customer. This connects the first
mine → warehouse → blacksmith chain without manual stocking. It does not found any
of these firms, create money or invent customers for finished tools.

## Admin settings

Add this optional section to `plugins/Civilizations/economy.yml` while stopped,
then restart. New files include it; older files stay disabled until explicitly opted in.

```yaml
warehouse-supply:
  enabled: true
  customers-per-review: 16
  producers-per-review: 16
```

Both review budgets must be 1–64. Timed industry and `industry-supply` must also be
enabled. Existing input-period buffers, base Trade Value prices, storage weights,
cargo delay and village-wide haul limits are reused. Prototype upstream and
downstream prices are identical: there is no markup, wage, separate transport fee
or assumed profit. A warehouse must already have its own real purchasing capital.
These settings are review limits, not ownership caps.

## Conservation and scheduling

- Review actual eligible workshop staff, cash, storage and recipe needs. Subtract
  owned and paid pending workshop inputs, then count local warehouse stock once.
- Subtract paid pending imports to operating warehouses once across the village,
  for ordering only. Cargo cannot be resold or used before arrival. This includes
  legacy paid cargo, whose original delivery caller remains responsible.
- Buy only operating local mines' currently owned yields or configured workshop
  output. Producers retain one due maintenance kit. Remaining ore reserves,
  future production and village commodity stock are not merchant inventory.
- Require at least one qualified saved warehouse employee. Normal owner/closure
  checks apply; approved qualified staffing can keep a deceased owner's firm running.
- Bound purchases by actual warehouse cash, free space including incoming reservations,
  shared haul allowance and 128 pending receipts. Missing prices/storage support skip
  those goods. Incomplete customer/producer reviews defer without inferring shortages.
- Order goods and suppliers deterministically by commodity and saved UUID. Partial
  purchases are possible when funds or capacity are limited; a blacksmith still
  requires a complete delivered recipe bundle before producing anything.
- Money, stock, cargo, history and the warehouse period checkpoint commit together.
  Replay/restart cannot pay twice. Paid cargo arrives even after new restocking is
  disabled or the owner becomes unavailable. Existing clocks remain saved.
- Server downtime and absent worlds earn no work. Unloaded chunks in a present world
  need no Paper block/entity operations or chunk loading for this data-only chain.

Warehouse trading earns no production skill awards and consumes no imaginary
finished goods. Stable supplier ordering is a prototype allocation rule, not the
final competition/specialization engine. No automatic warehouses, payroll,
regional transport, physical storage buildings or construction are introduced.
Warehouse restocking reuses existing shipping receipts and clock tables without
backfill or destructive conversion. Tool upkeep adds schema 51's empty service
and audit tables separately. Runtime recipe industry `warehouse` is reserved
for this purchasing role rather than a production recipe.

## Validation

Temporary SQLite tests cover two paid legs, delayed production, conservation,
restart, absence, disable, shared pending coverage, staff/customer funding,
producer-review limits, unavailable stock, hauling and rollback/retry. Plain Java
tests cover deterministic bounded planning and strict configuration.
`WarehouseSupplyProbe` separately exercises the actual PopulationSession owner
inside marked disposable Paper, using synthetic firms in its own random database.
It edits no blocks/entities or human server data. See DEVELOPMENT.md and DEVLOG.md
for completed build and native results.

## Public farm equipment compatibility

A public farm tools-only store holds one toolkit and zero private cash, with an
actual saved farm_tool_purchases receipt. This is equipment storage, not a raw
material supplier. Warehouse producer discovery now excludes exactly that audited
layout, matching the existing local input-demand exclusion. Without this guard,
a valid public farm order could cause the warehouse's producer review to throw
an unsupported-industry error and roll back unrelated real upstream purchases.

Unknown farm stores, extra commodities, changed capacity or private cash remain
explicitly unsupported. The exclusion happens before producer limits, so farm
equipment stores cannot consume a bounded producer slot. It creates no demand,
stock, grants or free tools; ordinary supplier/customer funding and paid deliveries
remain unchanged. No migration. A regression exercises an actual mined-iron
purchase with an audited farm present, then confirms a changed layout still defers.
A test-only FarmWarehouseProbe repeats public upkeep and actual upstream purchase
on disposable Paper. Verification results follow in PROJECT_STATUS and DEVLOG.
