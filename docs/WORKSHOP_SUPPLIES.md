# Paid local workshop supplies

Existing funded workshops can buy actual recipe inputs from existing warehouses
in the same village. This runs inside the timed industry's SQL-owner period;
it does not create workshops, warehouses, free stock, new money or customers.

The buyer must have an available owner, actual workers, supported storage and
purchasing funds. Warehouses must have a living owner still in their settlement
and actual owned stock. Plans buy only complete affordable recipe bundles. Missing
iron does not cause a blacksmith to spend its money on unusable extra coal.
Previously held and pending inputs reduce purchasing needs. Output storage,
transport and a bounded order budget also limit purchases. If any requirement
fails, saved diagnostics explain the pause. No consumer clears finished tool stock
merely to make the economy move; full stores can stop production.

Payment transfers working capital to the seller once and removes its goods. The
buyer owns cargo immediately, but cannot consume or sell it until its recorded
arrival. Space is reserved at purchase. Automatic cargo arrives on the buyer's
earned industry clock, including after purchases are disabled or its owner dies;
owner absence still prevents new purchasing/production. World absence and server
downtime earn no delivery time. Unloaded chunks in a present world need no entities
or chunk loading. The output is abstract tool supplies, not Minecraft swords or
an animation. Physical workplaces remain Phase 4.

Purchase, money, reservation, shipment clock identity, eligible arrival, recipe
consumption/output, history and checkpoint share one transaction. A failed final
checkpoint rolls everything in that work period back. Deterministic receipt IDs
and persistent clocks prevent successful work from being paid or produced twice.

## Editable settings

New generated economy.yml files contain:

```yaml
industry-supply:
  enabled: true
  input-periods: 2
  warehouses-per-review: 16
```

Input coverage accepts 1–8 periods; warehouse review accepts 1–64. The first is a
buffer limit, not guaranteed future demand or funding. Prices reuse
prices.base-trade-value (Trade Value with at most two decimal places), converted
to whole hundredths. No second currency or price file. Missing prices prevent new
orders for that input; already owned inputs still work. Supply prices must be
positive and at most 1,000,000 Trade Value. Restart after editing; each successful
shipment preserves its original quantity, price and timing.

industry.local-delivery-periods sets delay and local-haul-units-per-period bounds
shared automatic transport per online wall-time window. Each buyer has a private
production clock, so its period number is not a shared village transport clock.
Queued work cannot bypass the shared limit by replaying many private periods in
one window. The limit measures dispatched commodity units, not storage weight.
There are at most 128 pending shipment records per automatic buyer review and 128
eligible arrivals per work period. An oversized legacy backlog defers new orders
with a diagnostic; it is never deleted. Legacy manual shipments retain their
original caller's delivery schedule and separate quota namespace. They count as
owned pending inputs, but are never adopted by a new automatic clock.

Old owner files without industry-supply stay disabled and are not rewritten.
industry-runtime must also be enabled. Disabling new purchasing does not cancel
already paid automatic deliveries while that industry clock remains enabled.
Disabling the entire runtime pauses those clocks, including deliveries.

## Persistence and limits

Schema 36 adds industry_auto_shipments and supporting indexes transactionally.
Its compound foreign key binds each shipment to its actual buyer's existing
industry clock. Upgrade creates no shipments and changes no saved stock, funds,
reserves, owners or history. Legacy pending cargo is not backfilled into it.

Automatic mine-to-warehouse stocking, nonfood founding, natural site selection,
renewable forestry/fishing, finite primary stone/sand, finished-goods customers,
ownership succession and specialization remain unfinished. This is a verified
component toward the connected-production milestone, not completion of Phase 3.
The native fixture is synthetic, separate from player data, and does not certify
human-client balance or natural village formation.
