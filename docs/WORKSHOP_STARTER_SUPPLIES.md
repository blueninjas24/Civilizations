# Paid first-workshop materials

The user approved a limited, paid first purchase from the village's existing
surplus. This provides a narrow way to start the tool-production chain before a
warehouse and mine exist. It is not an ongoing public supplier.

`BusinessStartupGrants.startWorkshopWithSupplies` composes the existing protected
grant, qualified founder/first job, empty storage and first material purchase in
one SQL-owner transaction. If any check or write fails, none of those changes
remain. The caller must still establish real demand and current workplace evidence;
this trusted persistence seam does not automatically found businesses.

The buyer spends actual business money; the village receives that payment and
loses the exact material bundle. Food cannot be sold. Current food assessment must
cover every living resident, and the grant must belong to that same food period.
The business must be a staffed processing workshop with a living local owner in
a present permanent world. No free skill, stock, customer or cash is created.
Existing production, nonempty stores and previously used entitlements are rejected.

Materials become paid incoming cargo, reserve destination space, and are unavailable
to recipes until normal timed delivery. Existing input purchasing and upstream
demand count the cargo, and resale includes its paid value. Delivery is atomic
with normal industry work/checkpoint. Restart and absent worlds add no work;
accepted cargo remains deliverable if new purchases are disabled. Closing a firm
does not erase its paid assets. New operation IDs cannot refill the entitlement.

Editable `economy.yml` defaults:

```yaml
workshop-starter-supplies:
  enabled: true
  maximum-recipe-batches: 1
  maximum-purchase-hundredths: 4000
  protected-units-per-material: 2
```

Prices reuse `prices.base-trade-value`, converted exactly to hundredths. Protected
materials remain available for public needs and training; admins should increase
this reserve if they raise practice supply costs. Food remains untouched regardless
of price configuration. Missing legacy section disables new purchases. Purchase
size is also bounded by real cash, the caller's starter haul allowance and saved
storage weights/capacity. This startup allowance is separate from recurring local
warehouse shipment windows, and grants keep their original gross budget usage.

Schema 60 adds two empty audit tables: one immutable order per business and its
per-material prices/reserves/before-and-after balances. No existing assets are
converted, deleted or granted an entitlement retroactively. Delivery only sets the
arrival period. Failed migrations preserve the old schema and retry transactionally.

Automatic workshop/warehouse opportunities, initial founder training and native
acceptance remain distinct milestones. Do not report this mechanism alone as a
working autonomous fresh-village industry chain.

Verification: 13 new temporary SQLite cases and two configuration cases; final
`./mvnw clean verify` (Windows `mvnw.cmd`) passed 1,043 tests with no failures,
errors or skips (6:08). The checked-in disposable-only WorkshopStarterProbe also
passed under Paper 26.2-129, with no forced chunks or block/entity/inventory edits.
Original fixture assets/money/history were preserved; only normal clocks/tracking
and saved aging changed. This validates the paid mechanism, not automatic founding.
