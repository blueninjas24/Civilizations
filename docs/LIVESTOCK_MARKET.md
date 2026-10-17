# Milk and egg market

## Using it

Use `/civ market` near a staffed, operating livestock business with recorded milk
or egg stock. Click an offer once. Bring ordinary emeralds, free main-inventory
space and **your own plain empty bucket for milk**. Eggs need no bucket.
The `civilizations.trade` permission controls access.

The screen quotes actual stock; it cannot reserve goods merely by opening.
Confirmation rechecks current supplier, stock, payment terms, permission, distance,
bucket and room. Expired offers require reopening. Repeated clicks cannot buy twice.
Custom emeralds/buckets are not consumed. Armor and offhand are not payment storage.
Large egg orders split into legal native item stacks; no overflow drops.

Automatic eligible natural-animal adoption, funded herd startup, physical offspring
reconciliation and paid scheduled feed/output are now connected; see
LIVESTOCK_ADOPTION.md and LIVESTOCK_RUNTIME.md. These require their own enabled
settings and real qualification, funding, grain deliveries and work. A village
without an established eligible stocked business still reports no offers. This
market does not manufacture herds, goods, money or buckets.
Real bakery purchases of these products remain a separate integration step.

## Editable settings

New installations include this optional `economy.yml` section. Older installed
files are preserved: absent section means disabled until an admin adds it.
Edit while stopped, then restart; no live reload is provided.

```yaml
livestock-market:
  enabled: true
  milk-emeralds: 2
  eggs-per-order: 4
  egg-order-emeralds: 1
  horizontal-radius: 64
  vertical-radius: 24
  maximum-sellers: 32
  offer-seconds: 30
```

These are introductory fixed, editable retail prices. Milk orders contain one
bucket; egg orders may contain 1–64 items, split using actual item stack limits.
Shared `purchases.trade-value-per-emerald` supplies the accounting conversion;
prices are not additional village grants. Seller business cash receives the real
player payment once. Market bargaining and demand-driven milk/egg pricing are not
implemented. Missing/malformed required fields fail configuration validation.

## Saved recovery and death

The player's versioned receipt and inventory are saved together. A bounded
independent reader checks the saved root receipt before SQL transitions:

1. `PREPARED` proof allows the owner to reserve the exact actual seller stock.
2. Main-thread revalidation makes one storage exchange and saves `EXCHANGED`.
3. Independently verified `EXCHANGED` proof and an exact current pending-goods
   count allow once-only seller credit. Missing/excess goods require review first.
4. Completion removes only the new goods' temporary marker and clears the receipt.
5. `CANCELLED` proof returns a held order only when no confirmed delivery occurred.

Only newly purchased goods occupy marked, separate stacks. Their exact operation,
buyer, material and quantity receive temporary retention through player death,
as approved October16. Older eggs, equipment, money and confirmed purchases follow
normal server death rules. Server keepInventory and experience settings are not
changed. Existing keep entries are not duplicated. Retention ends on confirmation.
The controller waits while the player is dead and resumes after respawn.

Unverified output cannot be consumed, moved, dropped, used in vanilla interactions
or moved through recipe-book autofill. Other Civilizations food transfers cannot
start concurrently. Recovery still runs for accepted orders after market settings
are disabled. It never issues another product or automatically refunds a possibly
delivered order. A failed inventory restore, lost marked quantity, malformed receipt
or ledger disagreement remains `REVIEW` for administrator investigation. There is
no automated review/reset command. External plugins or commands that directly
rewrite inventories/saves require compatibility testing.

## Verification scope

Temporary saved-player proof tests cover handoff/restart/save failures, seller
credit, cancellation, death deferral and mutual exclusion. Separate event/storage
tests cover exact selective retention, other property, already-kept copies, excess
quantities, custom metadata, restore failures, milk use and recipe-book movement.
Existing temporary SQLite sale tests cover transaction/replay and stock accounting.
Schema remains84; this adapter adds no database migration.

Final Windows Wrapper clean verify passed1,681 tests, zero failures/errors/skips,
09:23 at2026-10-16T22:11:50-04:00. Guarded Paper26.2 build129 passed22:13:02,
including actual saved milk/eggs, once-only seller balances, reload and restart.
The first native setup reused a workplace address and was correctly rejected; only
the helper was corrected. The stopped fixture audit preserved142 predecessor
tables, with ordinary metadata/one age month only, then restored settings/archived
the helper. Main schema stays84 and the human server was untouched. The guarded
native helper uses a synthetic player backed by actual Paper inventory/PDC/player
files, a separate test database and a synthetic death/respawn event model. It is
not a human-client death, actual JVM power-loss or automatic livestock adoption
certification. Human survival and multi-plugin playtesting remain release checks.
