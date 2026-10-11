# Per-block mineral reserve accounting

The user approved even allocation of existing remaining reserves on October 10,
2026. Schema 46 adds coordinate shares, a conversion record, extraction entries
and permanent removal records. It preserves deposit identity, total remaining
reserve, existing goods, money and historical discovery/production receipts.

For a legacy vein with 93 remaining batches and four saved coordinates, the
conversion records shares of 24, 23, 23 and 23 in world/x/y/z order. It does not
invent past extraction. Deposits without saved coordinates retain their existing
aggregate balance; there is no invented geological evidence. A deposit with over
65,536 coordinates fails migration transactionally and requires review.

Fresh discovery divides its actual reserve across original ore coordinates.
Later overlapping evidence adds identity with zero additional reserve; revisits,
replacement blocks and changed abundance settings cannot refill an existing vein.

Actual mining drains positive shares evenly, with deterministic remainder order.
The algorithm uses bulk rounds, never one loop per resource unit. Goods, aggregate
reserve, per-coordinate balances, production receipts and source extraction audit
commit together. Replaying an accepted operation changes nothing. Inconsistent
source balances or any SQL failure roll back the operation.

An explicit removal retires only that coordinate's remaining share and decreases
the deposit by the same amount. Other sources and previously extracted goods are
preserved. Batch removal is atomic, bounded to 512 unique coordinates and replay
safe. Removed coordinates, including ones not yet discovered, remain permanent
exclusions in later surveys. Placing ore there cannot restore its natural reserve.
Within a batch, adoption and sum validation run once per affected deposit rather
than rescanning an entire vein for every removed block.

## Scope and next integration

This batch implements persistence and accounting only. The Paper placement
tracker is not yet connected to source retirement for player breaks, explosions
or piston movement. No automatic first-mine founding or physical excavation is
enabled. Native block-removal capture, queue failure/unclean-stop protection and
production trust checks must be verified before advertising live source loss.
Unsupported editors and offline world edits remain outside mutation guarantees.
The human development server has not been upgraded by this batch.

Tests use temporary SQLite databases and explicit saved geological evidence.
They cover allocation, even extraction, single/batch retirement, restart/replay,
replacement, linked evidence, world isolation, exhaustion, injected SQL failures,
conservation and schema 45 conversion failure/retry. A seeded domain test compares
500 small cases against literal round-robin depletion, including zero/unequal
shares; another checks a Long.MAX_VALUE balance without per-unit work.


Verification October 10: Windows `.\mvnw.cmd clean verify` passed 806 tests
(804 unit/two packaged integration), zero failures/errors/skips. The separately
compiled guarded Paper probe passed packaged extraction, single-coordinate loss,
conservation and restart checks; prior industry/lifecycle probes also passed. It
used a separate database and explicit synthetic coordinates, never native block
removal. The marked fixture's schema 45 -> 46 retained existing goods/money/history,
with healthy integrity and foreign keys. It stopped normally; original properties
were restored and the probe archived. The human server remains unchanged.
