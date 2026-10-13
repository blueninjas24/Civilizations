# Livestock protection and work foundation

## Approved herd control and cohort calculation batch

User approved simulation-managed breeding/slaughter. `LivestockHerd` computes
only actual active periods: immutable normalized young cohorts mature gradually,
fed surviving adults can breed after saved cooldown, births require additional
real caller-supplied feed/work and free herd capacity. Capacity loss retains old
animals. Adult slaughter uses the existing protected reserve and cannot slaughter
young; milk/egg and meat units stay abstract until owned inventory integration.
No feed-linked deaths/spoilage or stock transfer is invented. Replay is deterministic.
This is not a persisted herd ledger; caller must not treat budget inputs as actual
stock without a future shared transaction and roster/projection reconciliation.

Strict optional `livestock` YAML: enabled, maturity12 active periods, breeding delay6,
feed4/work2 per birth, max1 birth/period. Cow/chicken reserve2, feed1/adult, renewable
output1/adult, meat4/cow or2/chicken, slaughter max1. All editable prototype terms;
zero birth cap disables breeding. Stored cohort mature-period schedules apply
forward; new terms do not retroactively reschedule existing cohorts. Current live
use is the guard enable flag; herd rates are calculator settings awaiting integration.
Missing old-file section opts out and files are never overwritten.

`AnimalOwnershipRepository.villageAnimals` returns immutable positive village-only
IDs. `PopulationSession` publishes them on its SQL owner at startup. The new Paper
guard uses that view and readiness, with no SQL/world access per event. It blocks
outside EntityBreedEvent births when either supported parent is village-owned;
intercepts actual cow/chicken food tags in either hand before vanilla consumption.
Buckets, name tags, leashes, player/unclaimed/unsupported animals stay available.
No live adoption/transfer exists yet; future claims must publish after commit and
before physical control. Cold-start/failed readiness leaves the guard inactive;
startup reconciliation/freeze for recovered real herds is still required before
live adoption. Slaughter, player-death updates and herd products are not wired.

Fourteen added tests: six pure cases with2,400-period conservation/replay; six
birth/early-interaction cases; one strict YAML case; one village-only SQL identity
publication case. Focused21 passed; full .\mvnw.cmd clean verify passed1,439
(1,437 unit/two packaged integration), zero failures/errors/skips,7:37 at13:12:55 ET.
Guarded Paper26.2 build129 proof passed13:44:11 ET: SQL-owner identity publication,
actual cow/chicken food tags, synthetic native event dispatch, player/other-item
availability and unchanged fake-player feeding stacks. First fake player lacked PDC
and caused existing trade-guard listener errors; the audit refused its pass marker.
Fixed only the helper, then reran with no event errors. Real temporary animals
removed; fixture stopped, settings restored and helpers archived. Retry audit
verified one normal calendar month and two living residents aging one month; dead
rows, history, money/assets retained,123 other predecessor tables exact. This is
synthetic event/identity proof, not a client packet or actual vanilla birth-cycle
playtest. Production source stayed unchanged after full verification. Schema75 unchanged,
no human server edits. Production livestock and sign UI remain incomplete.

## Subsequent schema75 pen protection and snapshot classifier

`PenProtection` validates bounded connected geometry, an interior seed, an adjacent
sign coordinate and capped initial roster. `PenProtectionRepository` stores owner,
world/standing level, geometry and initial roster with new animal ownership in one
transaction. Exact retries replay; changed inputs, overlapping protected columns
or another player/village's animal refuse and roll back everything. Existing animal
provenance is retained. Protection lookup uses saved coordinates, not a current sign.
Authority is a trusted adapter input; this backend is not exposed to players.
Unknown existing pens still require admin/certified authority; sign placement alone
is never certification. Shared boundary columns conservatively prevent adjacent
claims in this initial backend; expanding that requires explicit safe overlap review.

Additive schema75 creates empty protected_pens/columns/animals and preserves all
predecessor rows. Eight temporary-DB tests cover restart/idempotence, denied authority,
changed owner/roster, overlaps, conflicting animals/atomic rollback, original provenance,
late failure/retry, migration74 rollback/retry and forged/disconnected geometry.
Focused91 older-save checks and focused29 geometry/preview/store/Paper cases passed.
Full .\mvnw.cmd clean verify passed1,425 cases (1,423 unit/two packaged integration),
zero failures/errors/skips,7:54 at12:25:29 ET. Native Paper26.2 build129 passed
12:29:13 ET: actual closed/open connected gates, exact nine-column interior, missing
fence rejection and unknown snapshot. Initial attempt stopped before any block
changes because no chunks were loaded; test-only setup now explicitly loads one
fixture chunk if needed. Production source never loads a world/chunk. Temporary
sky pen blocks restored, no animals edited. First schema73→75 and retry schema75
save audits passed:125 and129 predecessor tables respectively, with only normal
clock/economy/ore updates; ownership/pen rows empty, integrity/FK healthy. Settings
restored, backed-up history preserved, helpers archived and isolated server stopped.
No playable server or production database edited. Native helper is test-only and
not shipped; source/tests/diff reviewed before commit.

`PaperPenSource` reads only immutable snapshots captured on Paper's main thread;
it never calls World or loads chunks. Correct world UUID/metadata, chunk coordinates,
height bounds and snapshot count are validated. Three vertical levels plus bounded
connection reads certify simple flat common-cube footing, two air blocks, fences
with actual reciprocal connections, structurally connected gates (open or closed),
and two-block common-cube building edges. Unsupported walls/doors/ramps/slabs/materials,
wet/disconnected fences and missing terrain remain UNKNOWN. Known missing footing
is OPEN. This is conservative evidence, not all-shape collision/ownership inference.
Six proxy cases cover geometry, connections/openness independence, unknown/negative
chunks and wrong identity/height. No live observer/sign menu is wired yet.

Saved initial records/pen protection are backend-only. Automatic adoption, continuous
observation/player breeding capture, donation transfers, live signs/particles/menu,
claims-plugin adapters and simulation-controlled herds remain next. Herd authority
has been asked explicitly; no vanilla animal behavior is changed while pending.

## Subsequent schema74 ownership persistence batch

`AnimalOwnershipRepository` now records immutable first player/village ownership,
origin-world UUID, recording time and source per animal UUID. Exact retries replay;
changed ownership or provenance cannot overwrite the record. Public calls require
no active transaction; internal rows participate in a future caller-owned pen
transaction without independently committing. Loaded location/sign observations
are not a transfer. Origin world is provenance, not a constantly updated position.

Additive transactional schema74 creates initially empty `animal_ownership` and
its village-owner index. Village owners require an actual saved settlement bound
to that origin world; player records require an observed world. No existing animals
are inferred/adopted. Seven temporary-SQLite cases cover restart, retry/conflicts,
world constraints, failed inserts, batch rollback, migration73 failure/retry and
invalid owners. Focused94 passed with affected older-save regression cases; full
Windows Wrapper .\mvnw.cmd clean verify passed1,411 cases (1,409 unit/two packaged
integration), zero failures/errors/skips,8:05 at11:37:25 ET. No real server database
was modified. Source/tests and migration diff reviewed.

This backend accepts only trusted caller-established identity/authority/evidence.
It is not exposed to players and does not implement sign claiming, pen persistence,
land-claim permissions, player breeding capture, explicit transfers or adoption.
A future donation must use a distinct authorized/audited transition, not first
registration. Live adapter still needs main-thread snapshots, safe 3D classification,
continuous observation and disposable native verification before activation.

The initial domain batch below describes schema73 at that earlier commit; the
verified current artifact upgrades to74 when deployed; no server deployment occurred.

## Approved interaction direction

Free-range eligible animals are adopted automatically after reliable observation.
There is no per-animal player authorization requirement. Existing unknown pens
stay protected. Proposed `[Civ Protect]` / `[Civ Livestock]` signs request a bounded
pen preview; only explicit authorized confirmation establishes a claim/donation.
Exact sign syntax and menu are not yet implemented. Show particles, area and eligible
animal count before accepting; a closed courtyard can look exactly like a pen.

Recorded owners/delegates or a compatible land-claim manager may authorize known
ownership; admin resolution is required for unknown older ownership. Placement or
proximity alone grants no authority. Names, leashes and recorded player breeding
prevent automatic adoption. Village-pen designation covers eligible animals and
future offspring, not an individually player-owned animal. Saved ownership survives
escape, opened gates and missing signs. Ownership changes must be explicit/audited.

## Implemented plain-Java components

* `PenScan`: incremental deterministic four-neighbor horizontal flood fill over
  caller-certified columns. At most the supplied source-read budget per advance.
  Prototype engineering limits: radius32, maximum1024 interior columns; caller can
  select validated limits. Open terrain reaches the bounds and is refused, never
  treated as the rest of the world belonging to a fence. Unknown terrain rejects
  incomplete footprints. Invalid seeds, coordinate overflow and area caps refuse.
  Rejected results expose no usable partial footprint. Accepted sets are immutable.
* `PenClaimPreview`: binds complete geometry and animal IDs to one actor, permanent
  world UUID, standing level and expiration. Confirmation requires fresh identical
  footprint/membership and a fresh authority check. Changed evidence/animals,
  another player/world/level, expired preview or revoked authority are refused.
  A READY result is not a saved claim; persistence still needs its own transaction.
* `LivestockAdoption`: read-only reason-coded eligibility. Recorded ownership wins
  over location. Protected/unknown pens, named/leashed/player-bred unclaimed animals
  cannot be adopted automatically. Incomplete evidence waits; free range requires
  the supplied observation duration. Already-village animals are not adopted twice.
* `LivestockWork`: deterministic prototype work using actual adults, feed, labor,
  storage and a requested slaughter amount. Renewable output consumes real feed
  and work without killing animals; slaughter consumes work and reduces adults
  subject to a protected breeding reserve and per-period cap. Full storage/labor
  shortage cannot generate output. No consumer, money, spoilage or breeding invented.
  Renewable work has prototype priority over slaughter; no live rule/config is wired.

## Required adapter contract and remaining work

Source must be a stable captured view, not unsafe async live Bukkit reads. An
INTERIOR column certifies suitable footing/headroom at the chosen animal level;
BARRIER certifies a fence/wall/gate or adequate solid building edge. Gates are
structural barriers even while open. Known vertical escape is OPEN; ambiguous
heights, ramps, doors, diagonals or unloaded chunks are UNKNOWN until safely
classified. This scanner alone cannot prove a three-dimensional pen, attachment of
its sign, block origins, actual ownership or land-claim permission. Adapter must
check those before preview and recheck on confirmation. Do not force-load chunks.

No Paper listener/menu/particles, block classifier, config section, persistent
pen footprint records and live animal capture, continuous observation tracker, land-plugin integration,
herd juvenile cohorts/breeding, startup or live livestock production are wired.
The initial domain batch changed no animals or databases and retained schema73.
The subsequent additive schema74 batch above is temporary-DB tested only; worlds stay untouched.
Neither player pen construction nor a sign silently donates animals.

## Verification

26 added domain tests:20 geometry/preview/eligibility and six livestock-work tests.
Includes a walkway between buildings, closed courtyard, hole/vertical escape,
unknown chunk edge, caps/overflow, exact tick-read budgets, immutable previews,
changed membership/permission/identity/expiry, escaped ownership and uncertain pens.
The livestock-work sweep runs13,122 input combinations (9 adults ×9 feed ×9 labor ×18 space), checking
animal/feed/work/storage conservation and breeding/slaughter bounds. Extreme longs
also checked. Focused26 passed; full Windows Wrapper clean verify passed1,404
cases (1,402 unit/two packaged integration), zero failures/errors/skips,9:13
at11:24:55 ET. No native Paper smoke is needed for this unwired pure domain batch;
the future adapter requires disposable Paper checks before enabling adoption.
