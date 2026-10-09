# Finance transaction and quote foundation

Village budgets and prices use exact hundredths of abstract Trade Value. Treasury
is spending capacity; it is not personal wealth or a prosperity score. This batch
prepares finance mechanics, without enabling payments or giving live villages money.
The user has now approved a moderate configurable initial financial position and
both emerald payments and contextual barter. The funding and payment adapters
remain to be implemented; no live account is seeded by the current code.

## Exact quotes

TradeQuotes accepts the existing scarcity price per commodity unit, units represented
by one ordinary item, a stack quantity and an optional observed village budget.
It reports full-stack cost and the maximum whole-item quantity that budget can afford.
Unknown budget remains unknown, distinct from a measured zero. An insufficient budget
never rounds up to afford another item. Partial affordability is information for a
future seller confirmation; it does not silently take part of a player's stack.

Existing economy.yml prices round to hundredths once per commodity unit. Quotes
multiply those exact cents by item units and quantity, without another rounding
policy. Invalid fractional-cent prices, negative budgets, invalid quantities and
integer overflow fail explicitly. No floating-point money is used. No emerald
exchange rate, payout rounding or barter valuation is chosen in this foundation.

A quote reserves nothing. Future live transactions must recompute/check authoritative
price, inventory consent and available money on the owner thread before committing.
A displayed quote is not an accepted trade or promise of payment. Quotations are plain
Java calculations; no new menus, commands or paid suppliers are exposed yet.

## Shared treasury transaction

Standalone treasury initialize/post APIs preserve their existing behavior: each
owns a transaction and rejects nesting. Internal initializeRows/postRows variants
require an existing SQL-owner transaction and never commit or roll it back. This
allows a future verified goods/payment receipt and its budget change to share one
atomic commit, rather than independently saving money and delivery.

The owner must roll back the entire surrounding operation on any failure; it must
not catch a failed receipt write and then commit a pending debit. The variants keep
existing insufficient-funds, unknown-account, opening, overflow and operation-ID
checks. Repeating an identical operation returns its original receipt; changed
inputs fail. Receipt results retain their original balance, not the current balance.

Temporary-SQLite integration tests combine a fixture treasury debit with the existing
food-delivery ledger. An injected delivery failure followed by caller rollback leaves
neither a debit nor goods; retry commits both, and food enters a future period once.
These fixtures are not a player transaction, payout, live supplier or funding policy.
Opening/credits/debits also roll back together; committed receipts survive restart.

Schema remains 20. No live/server database, world, owner configuration or Minecraft
inventory is changed by this batch. Tests cover exact/partial/unknown budgets, 10,000
seeded affordability cases, integer boundaries, existing scarcity price integration,
shared rollback, retry, restart and unchanged standalone transaction contracts.
Paper/client payout and funding tests are required when those adapters are built.


## Approved next finance direction

Residents may begin financially neutral/moderate. A configurable one-time village
opening budget will represent initial spendable capital; its numeric scale is a
prototype, independent of qualitative household prosperity. Subsequent spending
and actual revenue will change the account. Reloading or recreating visible
villagers must never reset or replenish it. This is initial-condition seeding, not
a recurring income claim or detailed salary/tax system.

Players should be offered emerald payment or relevant barter goods from actual
available production stock. Timber for a logger and mining goods or fish for their
respective producers are intended future contexts, not currently assigned businesses.
Trade Value remains the common internal valuation; emerald exchange rates and item
prices will be configurable. Example stack quantities are not guaranteed exchange
rates. A merchant's profession alone cannot create goods or a source of income.

See DECISIONS_AND_OPEN_QUESTIONS.md for the user clarification and implementation
boundaries. These approved directions have not yet passed implementation gates.
