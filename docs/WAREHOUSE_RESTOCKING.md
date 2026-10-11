# Existing warehouse restocking

Existing staffed warehouses can buy actual extracted goods from operating local
mines to cover funded input gaps at existing workshops. This connects the first
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
- Buy only operating local mines' currently owned output. Remaining ore reserves,
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
Schema remains 50; existing shipping receipts and clock tables are reused without
backfill or destructive conversion. Runtime recipe industry `warehouse` is reserved
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
