# Construction purchase recovery

Current backends: `ConstructionSupplyRecalls.recallPending` and
`ConstructionSupplyReturns.returnUnused`, serialized through PopulationSession.
Operator preview/confirmation commands are now connected; no automatic recovery
caller. Started work still requires manual review.

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

## Arrived materials: paid return cargo

Schema115 supports an original-supplier buyback once ALL public project inputs
have arrived. No stage effort, native intent or owned-stock mixing may exist.
Use current actual refund cash, destination space and shared haul. An entire
return is atomic; missing cash/space/haul or a late failure retains all goods.

Original suppliers pay the exact original price from their real cash, credited
to the original village. They own the return cargo; project goods immediately
become unavailable for construction. Supplier storage is reserved, but stock is
not credited until the saved later owner period arrives. Return hauling uses
the same editable limits as other local cargo. No instant reverse transport,
new customer, repricing income or extra clock is invented.

The existing PopulationSession delivery loop settles at most16 ready returns per
pulse, using permanent present-world IDs and actually earned owner periods.
Accepted arrival terms survive restart, policy changes and site/family retirement.
No absence/downtime catch-up. Destination storage/catalog failure rolls back the
arrival rather than losing cargo. Replays never pay or deliver twice.

Both recall and dispatch permanently stop new procurement/public work on that
draft. Separate unused cancellation may release family/site after ownership has
moved into saved return cargo; original sale, refund, shipment and cancellation
history remain. Never delete the original paid order.

Private-business purchases and physically started/stage-work projects remain
held for future reviewed recovery. Operator controls use the wrapper below;
there is no automatic recovery or demolition.

Validation: ConstructionSupplyRecallsTest uses only temporary SQLite databases for
conservation, exact restart retry, late rollback, unavailable world/time/delivery,
seller cash/storage, public-only scope,113 upgrade rollback/retry and household
retirement. This backend changes no Paper world or native entity.

ConstructionSupplyReturnsTest additionally covers exact money/goods conservation,
pending destination capacity, earned delayed arrival, shared haul, late dispatch/
arrival rollback, site retirement/restart, actual session downtime/world pause,
unsupported started goods and114->115 migration rollback/retry.

## Confirmed operator controls

Current commands, as a player in the original world:

- `/civ build inspectproject <project UUID>`: read-only saved work, origin, stage
  and exact unresolved native intent/coordinates/before/after; no native reads.
- `/civ build recoverproject <project UUID>`: read-only exact supplier-recovery
  preview. Shows recall or arrived buyback, order count, abstract units and actual
  original refund total/village. No payment on preview.
- `/civ build confirmrecovery <token>`: confirm that exact preview within60s.
- `/civ build cancelrequest`: discard an unsubmitted review, never accepted saves.
- After recovery, existing `cancelproject`/`confirmcancel` may separately release
  an eligible unused family/site reservation. Recovery alone never cancels these.

Inspection uses civilizations.construction.preview. Recovery preview/confirmation
uses civilizations.construction.manage (operators by default). Tokens bind player,
world, action and SHA256 of the original paid order terms. Another action/token,
actor/world/permission change, expiry, altered orders or new work cannot redirect
or force a refund. Maximum4 pending users; read requests30s; previews60s. An
accepted save drains across disconnect/close. Repeated confirmation cannot enqueue
another concurrent save. Exact operation/terms replay returns its original refund.

ConstructionRecoveryReview performs a read-only current snapshot: formal family
binding, no native/stage/owned-source work, original order modes/period, real
aggregate supplier cash/space and actual shared return hauling. Confirmation
repeats exact terms and live readiness inside the serialized owner, then calls
the existing atomic recall/return ledger. Queue submission time is authorization
metadata, never an economic clock: a new acceptance uses current owner execution
time. Exact receipt replay retains its original event and never repays, even if
time or hauling settings later change. No schema change beyond116.

Stop/edit/restart if you need to pause activity for recovery: automatic-construction
controls new purchases/native steps; labor.enabled separately controls future
public effort. Accepted cargo can still arrive. A pending recall has a narrow
original-period window; if it expires, let all goods arrive and review again for
buyback. Never delete rows or force a mixed/started project into the unused route.
Normal construction may legitimately advance between review and confirmation;
that final refusal preserves all paid work and requests another review.

Current validation: Wrapper35 unit+3 packaged passed41.178s for backend/adapter/
recall/return/native-survey checks; final queued-time/confirmation15+3 passed32.163s.
Actual Paper fixture routed saved inspection and explicit recovery using a
synthetic Player adapter/private paid save; read-only permission could not recover,
wrong-action token could not cancel, duplicate click/disconnect accepted one
refund, and a real session restart replay preserved cash/stock/site/family.
Three real original supplier refunds totaled2940; no world-block or inventory
operations. Both116 DBs healthy/FK-clean, server stopped/helper archived. Human
client input/compatibility still needs playtesting. Living handbook Chapter32.
