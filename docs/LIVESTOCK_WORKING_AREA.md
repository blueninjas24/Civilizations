# Livestock working area and positive sightings

The approved rule pauses products and breeding only for saved animals positively
observed away from their farm. Ownership, headcount and aging stay intact. Other
eligible animals continue. A later positive return restores eligibility. Unknown,
unloaded or never-seen bodies alone never establish departure, return or death.
Absent worlds still pause time; normal death capture remains separate.

## Editable prototype

Optional economy.yml section livestock-working-area has radius-blocks:32, an
inclusive three-dimensional distance from the saved farm site. Valid range1..256;
different worlds are outside. Existing files without this section keep32 without
being rewritten. This is an editable prototype under the user's tuning approval.
Invalid, quoted, fractional or misspelled settings fail before database startup.
Retuning evaluates the last positive coordinates under the new radius; it does
not manufacture new position/death evidence.

## Implementation boundary

Additive transactional schema87 creates an initially empty livestock_presence
table for latest positive positions/times only. No migration backfill, animal
adoption, reset or asset changes. Older evidence cannot undo newer sightings;
equal-time contradictory evidence rejects the entire batch. Unknown, player,
failed and deceased aliases cannot become living herd members through sightings.
Saved positions remain historical evidence after death but no longer count as
unavailable living adults. Species mismatches reject rather than changing herds.

The main-thread Paper observer requests at most16 saved living UUIDs at a time,
looks up loaded native bodies and submits immutable positive sightings. It never
scans worlds/chunks, forces a chunk load, teleports animals or changes ownership.
One page/save is in flight; failed saves retain that exact bounded batch for retry.
Missing bodies submit no negative evidence. Shutdown queues retained observations
before the existing SQL owner drains. /civ debug observation includes this status.

Herd work separately counts total adults and confirmed-away adults, including
young members maturing in the next actual period. Unavailable animals consume no
production work/feed and contribute no products/breeding, while their heads stay
saved and their cohorts still mature. Existing deterministic work/reserve/storage
limits remain. Paid feed, products and the checkpoint still commit together.
Receipts preserve historical replay; nondefault radius requests are fingerprinted.

The explicit SQL-owner production handoff uses the configured radius, the actual
present-world mask and trusted death-capture fence. This is not an automatic live
production scheduler. Adoption, safe newborn placement/reconciliation, scheduled
feed/output, and broader bakery ingredients remain unfinished. Position capture
does not repair an untrusted death history.

## Verification

Added21 domain, temporary-SQLite, SQL-owner, Paper-observer and configuration cases.
Focused52 and151 checks passed. Windows .\mvnw.cmd clean verify -B -ntp passed
1,739 tests (1,737 unit/two packaged integration), zero failures/errors/skips;
12:24, finished2026-10-16T23:36:49-04:00. Source/test Java frozen during that run.

Guarded Paper26.2 build129 probe passed2026-10-17 09:28:12 EDT: actual native
newborn/death regression, bounded saved-body lookup, cow moved away/returned
inside an already loaded test chunk, saved away position across SQL-owner restart
before fresh observation, unchanged herd/ownership/stock/cash. This does not
certify automatic adoption/production, actual chunk unload or a JVM crash.

Marked test save upgraded86->87:146 predecessor tables preserved,142 exact;
only ordinary clock/runtime/capture/ore metadata changed, no aging. New main
presence table empty; integrity/FKs clean. Fixture stopped/settings restored,
helper archived, backups/synthetic saves retained. Human server untouched.
