# Public farm equipment funding

The approved funding rule is implemented as an explicit SQL-owner purchase
adapter. Food still goes straight into village supplies. Farms gain no private
food sales, bank balance, payroll or automatic public startup subsidy.

```yaml
farm-tool-budget:
  enabled: true
  tool-trade-value-hundredths: 4000
  per-period-limit-hundredths: 4000
  minimum-treasury-hundredths: 2500
  food-reserve-periods: 2
  food-unit-value-hundredths: 200
```

These are editable prototype settings, separate from startup grants. A missing
legacy section disables new purchases. The protected treasury amount is the
larger of the minimum cash reserve and current food demand × reserve periods ×
food-unit value. A current food shortage, changed population since assessment,
unknown food period or exhausted upkeep budget prevents spending.

`FarmToolPurchases.purchase` accepts one toolkit for the actual saved village
farm, in a present world, with an operating owner/staff policy, qualified current
workers, complete crop evidence and actual food production in the current period.
The supplier must be a local blacksmith or warehouse with real goods. Existing
nonfarm businesses cannot claim this equipment budget.

An empty one-tool equipment store is opened only within the accepted payment
transaction. Exact invoice funding is debited from village treasury, passed
through the existing purchase transaction, and paid to the supplier. No cash
remains at the farm. Seller stock is reduced once; paid cargo reserves the farm's
one space and cannot be used until delivery. Another order is rejected while a
toolkit is owned or incoming. Existing farm cash/catalogs are preserved and require
an explicit transition instead of silent conversion.

Payment, seller cash, goods, shipment and immutable budget receipt commit together.
Failure rolls everything back, including a newly opened equipment store. Replay
after restart or disabling is read-only and cannot pay again; reusing a payment ID
with different shipment terms fails. Accepted cargo retains its original due
period. Upkeep spending is saved by village and completed food period.

Schema 53 adds empty `farm_tool_purchases`, referencing actual shipments and saved
businesses, with a village/period budget index. No old stock, cash or employment
is backfilled. Audited one-tool public equipment stores are recognized separately
by the nonfood input planner; other unsupported catalogs still defer its review.

This milestone does **not** schedule automatic farm orders, consume tools, slow
food production, create a new blacksmith, or create warehouse demand. The next
adapter must tie tool use and wear to actual farm work in the food transaction,
deliver accepted equipment under the farm clock, and then schedule bounded
funded replacements. Existing food behavior is unchanged until that work is
implemented and verified. Temporary SQLite tests verify current payment,
delivery, restart, conservation, refusal, rollback and migration behavior;
DEVLOG.md records completed verification.
