# Construction purchase recovery

Current backend: `ConstructionSupplyRecalls.recallPending`, serialized through
PopulationSession. No player command or automatic recovery caller yet.

A public project may withdraw its entire pending local purchase only while every
order is undelivered and its actual economy owner remains in the purchase period,
without backlog. Present permanent world identity is required. No stage work,
native intent or mixed owned-stock reservation may exist. It does not require
the family to remain short of housing: changed need is a reason to recover safely.

Each original supplier must still have the exact refund amount and storage for
its own goods. Goods return to that supplier; real seller cash returns to the
original village treasury. A whole batch is one transaction. A shortage or late
failure retains every original order, good and payment. No debt or money creation.

Schema114 adds immutable per-order recall receipts, before/after seller cash,
stock and buyer money, plus references to actual treasury credits. Original sales
remain intact. Same operation/draft/submission time retries return saved history,
including after restart; different reuse fails. Original spending and haul stay
used, preventing cancellation from reopening those period allowances. This only
withdraws still-pending commitments, not reverse delivery of arrived goods.

Recalled cargo never arrives. That draft cannot purchase or authorize economic
native work again. Existing household status can then authorize separate confirmed
unused cancellation, releasing the family/site without deleting people or history.
The report still shows original paid-order count alongside recall pause status.

Delivered materials, orders from earlier earned periods, private-business funding
and physically started work remain held for future reviewed recovery. They are
never silently refunded, discarded or demolished. Normal automatic purchasing
stays unwired until delivered-stock recovery/native work is ready.

Validation: ConstructionSupplyRecallsTest uses only temporary SQLite databases for
conservation, exact restart retry, late rollback, unavailable world/time/delivery,
seller cash/storage, public-only scope,113 upgrade rollback/retry and household
retirement. This backend changes no Paper world or native entity.
