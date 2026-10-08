# Explicit food deliveries and shortage relief

This Phase 2 batch lets the SQL-owned economy account for explicit externally
delivered food. It prepares the delivery side of player aid; it is **not yet a
player donation command, merchant interface or inventory exchange**. A read-only
`/civ aid` preview is now available; see PLAYER_FOOD_AID.md.

## How supplies enter the economy

A trusted source adapter calls `PopulationSession.recordDeliveredFood` with a
unique operation UUID, settlement UUID, positive food units and source reference.
The adapter must first establish the actual goods and consent. The ledger does
not itself verify inventory, remove items, invent a supplier or charge a treasury.
No normal gameplay listener or public command currently calls this API.

The village must already have a reliably initialized food economy. Unknown villages
do not receive fabricated inventory. Each receipt records its source, amount,
eligibility boundary and eventual applied period. Same-ID/same-input retries are
idempotent, including after restart/application; conflicting reuse fails.

Deliveries become explicit incoming food in a future economic period, then that
period's normal household demand consumes supplies. Existing food history and
reserve-opening values remain unchanged. The period report labels incoming aid as
imports; this does not imply a paid trade, transport route or invented counterpart.
Relief can clear food shortage duration and cause the village to reconsider its
priority without changing farm output or manufacturing a new facility.

## Time and transaction safety

On first receipt, elapsed economic time is accrued and its current queued target
becomes the delivery boundary. Earlier queued catch-up periods cannot use goods
that arrived afterward. Receipt acceptance does not drain that catch-up on the
calling server tick. Duplicate retries retain the original arrival boundary.

At most 64 eligible receipts enter one village period, in recorded arrival order;
remaining receipts stay pending. Food stocks, period inputs, needs, priorities and
applied-receipt markers commit together. A failed final receipt update rolls all
period effects back while retaining the receipt and elapsed target for retry.
Checked integer arithmetic rejects overflow rather than truncating or losing goods.

Partial farm evidence continues to pause accounting. Pending goods survive that
pause and restart, then apply in a real future period after reliable evidence
returns. Entirely unloaded villages continue from saved evidence; delivery processing
does not touch entities, blocks or chunks.

Schema 18 adds the receipt ledger and a pending-delivery index. Existing people,
family links, reserves, histories, priorities and treasury balances are preserved.
There is no historical receipt backfill, reseeding or save reset.

`/civ debug economy` reports pending receipt count and units eligible for the next
period's bounded batch. Pending count can include deliveries waiting behind old
catch-up; it does not imply all pending food is usable in that next period.

## Validation and remaining player-facing work

Temporary-SQLite cases cover crisis relief, no duplicate imports, past-period
protection, final-write rollback/retry, partial evidence plus restart, invalid
sources/amounts/conflicting IDs, the 64-receipt budget and migration recovery.
The marked disposable Paper probe supplies an explicit **synthetic fixture shipment**,
restarts with it pending, clears its real saved shortage with zero farm production,
rejects duplicate aid, then verifies farm recovery and unloaded operation.

These tests do not establish a player inventory transfer. The future player bridge
needs a crash/disconnect-safe inventory handoff, item-to-food conversions and nearby
settlement targeting before enabling delivery commands. No emerald payout, reputation,
barter, taxation, autonomous purchase or construction is implemented here. Overflow
or other rejected effects surface as retryable diagnostics; do not bypass them by
editing SQLite. Large-server profiling and human survival playtesting remain pending.
