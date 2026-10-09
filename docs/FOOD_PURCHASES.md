# Budget-backed food purchases

## Current implementation

Schema 21 adds persistent RESERVED / COMPLETED / CANCELLED food purchase records.
Plain Java terms identify an operation, player, village, ordinary food quantity,
whole emerald payment and exact Trade Value per emerald. Current prototype inputs
use 8.00 Trade Value per emerald, three food-demand periods as the purchase target
and at most 64 emeralds per exchange. Admins edit the optional `purchases` section
of economy.yml while stopped, then restart. New files enable purchases; existing
files missing this entire section keep sales disabled without being overwritten.

Reservation validates current completed needs and SEEK_FOOD priority, reliable field
evidence, no queued economic catch-up, conversion rules, perceived local value and
actual available budget. Already pending donations and all reserved purchases count
against remaining need. Two players cannot buy the same deficit or spend the same
funds. Only one RESERVED purchase per player is allowed across all villages.
Payment floors to whole emeralds; treasury cost is the actual promised emerald count
times its saved exact rate, not the unrounded indicative goods value.

Reservation debits spendable funds and saves immutable terms together. It does not
credit food or give currency items. Duplicate identical operation IDs replay the
original state; changed terms fail. Accepted terms survive retuning; disabling new
offers must not disable recovery. Reserving unknown/zero/insufficient funds fails.

Only a trusted inventory adapter may complete after independently verifying that
Minecraft saved both the goods debit and payment. Completion saves its delivery
receipt and COMPLETED state together, with no second treasury debit. Supplies enter
a future economic period, never old queued catch-up. Duplicate completion is inert.
Only proof of an unexchanged deal permits cancellation: state and refund commit
together once. A completed deal cannot be refunded or a cancelled deal completed.
Cancellation of an operation never reserved returns false without creating money.
Do not infer a safe refund from timeout, disconnection, failed void save or missing
in-memory work. Reserved funds remain held until verified resolution.

## Player exchange and recovery

Sneak-right-click a managed villager and choose Sell in the protected supplies menu.
The player needs `civilizations.trade` permission. Only an unchanged whole plain
vanilla food stack is accepted; split large stacks when they exceed village need.
Ordinary right-click vanilla trades and free donations remain separate.

The adapter saves PREPARED terms and independently reads Minecraft's complete gzip
player file before reserving funds. On the main thread it replaces the confirmed
food stack with emeralds in the same slot, avoiding overflow drops. Saved EXCHANGED
proof gates village food credit. Changed held items cancel before exchange; saved
CANCELLED proof gates the refund. Pending operations retry on reconnect/restart,
including when admins disable new sales or change rates.

Proof workers and active transfers are bounded. An EXCHANGED or REVIEW marker
temporarily blocks inventory moves, drops, hand swaps and interactions while recovery
is unresolved. Ambiguous inventory-provider failures retain REVIEW and held funds
for administrator review instead of guessing a refund or issuing more currency.

## Safety tests and remaining work

Temporary SQLite tests cover competing budgets/needs, one active deal per player,
restart, fixed accepted terms, insufficient funds, invalid/stale offers, history-
preserving migration, failed reservation/completion/refund rollback and safe retries.
Schema 20 upgrades transactionally without fabricated purchases or altered identities.

Barter, bargaining and learning/reputation attribution remain unimplemented.
Human client and inventory-plugin compatibility remain validation gates. SQLite,
player inventories and world files do not share one transaction: retain consistent
backups. Native death and server/plugin interference during pending exchanges need
further validation; this is not a guarantee against every crash or external edit.
Treasury is shared budget, not personal salary.
