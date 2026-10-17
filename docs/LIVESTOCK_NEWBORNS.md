# Saved livestock births in Minecraft

The newborn adapter projects already saved, feed/work-backed births. It never
adopts animals, creates another logical head, supplies feed, pays money, runs a
production period or constructs blocks. Schema87 is unchanged.

## Handoff and recovery

One birth handoff is in flight. At most eight saved births are paged at a time using
a UUID-text cursor. A new body is prepared on the main thread but not added.
SQL commits its exact ownership/alias/current claim and publishes protection
before the main thread can add it. Only then may the same UUID activate. Failed
SQL writes retry that same operation; activation retries never add again.

New placement requires livestock.enabled, the existing projection nearby-player
radius and safe ground in a chunk whose entities are already loaded. It searches
at most27 nearby positions, checks full supporting ground, two air blocks, world
border/height and living-entity clearance, and rechecks before adding. No forced
chunks, terrain changes, adoption or player-building ownership. Existing known
bodies may reconcile without a nearby player; that creates no animal.

A lost safe/nearby location before any native add attempt supplies positive
never-added proof and marks that candidate FAILED. Its alias stays recorded.
After any attempted add, a thrown/cancelled/uncertain outcome retains CLAIMED;
missing/unloaded never authorizes another UUID. Positive existing CLAIMED bodies
can finish activation. ACTIVE bodies retain their exact identities. Confirmed
deaths are excluded from future jobs. Unexpected failed bodies or conflicting
species/birth tags require review instead of deletion or replacement.

Permanent birth and temporary pending tags are evidence alongside authoritative
SQL UUIDs, not permission to adopt arbitrary tagged animals. Pending bodies have
AI/gravity held until activation, with no invulnerability. Release changes those
flags only with the matching pending tag. Ordinary active reconciliation preserves
external movement flags. Saved herd period/maturity controls baby/adult state and
age lock. It never rerolls maturity from the current settings. Initial adopted
animals' age reconciliation remains separate integration work.

Shutdown queues positive never-attempted cleanup after any accepted claim and
before SQL owner draining; callbacks never access Paper. If a world unloads
during a delayed save, drop the prepared native handle immediately and queue
immutable never-attempted cleanup in owner order. No World reference is retained
through an indefinite SQL retry. Added/uncertain bodies
keep their saved claim and pending marker for restart recovery. No world entity
is removed by this adapter. /civ debug observation reports its last decision.

## Verification and limits

Added24 headless state-machine, temporary-SQLite paging and Paper geometry/age
checks. Focused38 passed, then40 including the two unload cases. Three initial
headless fixture errors needed Paper's native block registry; explicit support/
air predicate seams retain native production checks while testing immutable
evidence. First full run intentionally stopped for the discovered unloaded-world
handle fix. Final Windows .\mvnw.cmd clean verify -B -ntp passed1,763 tests
(1,761 unit/two packaged integration), zero failures/errors/skips;12:29, finished
2026-10-17T10:03:05-04:00. Source/test Java frozen during final verification.

Guarded Paper26.2 build129 probe passed10:03:55 EDT: controlled nearby predicate,
automatic claim/add/activation of two actual cows on existing safe ground,
pending release/no granted invulnerability, exact identity through owner/controller
restart, real paid feed/output and saved maturity, missing body never replaced,
actual second child death and exact stock/cash/head balances. No actual human
client, chunk unload, JVM crash, initial animal age or automatic adoption proof.
All147 existing main test tables preserved,143 exact; only ordinary clock/runtime/
capture/ore metadata changed, no aging. Schema87 unchanged; integrity/FKs clean.
Fixture stopped/settings restored/helper archived/backups retained; human server
untouched. Only explicitly tagged private-fixture bodies were removed in cleanup.

Initial saved-animal age reconciliation and shared stale-view protection are now
verified by1,776 Wrapper tests and real initial/newborn age/native-serialization
checks; see LIVESTOCK_SAVED_AGES.md. Automatic livestock adoption, scheduling real
feed/output and broader bakery ingredient consumers remain unfinished. This
adapter does not authorize anonymous historical births or replace missing animals.
