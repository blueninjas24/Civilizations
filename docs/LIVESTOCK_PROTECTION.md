# Livestock protection and work foundation

## October16 — approved protected grants and trusted atomic opening

Dedicated livestock businesses are eligible in the new-file startup-grants list.
The grant ceiling, shared per-period/lifetime caps, food assessments and retained
food money are unchanged. Existing installed lists opt in explicitly; missing
sections and opt-outs stay disabled. Funding eligibility alone starts no business.

`BusinessStartupGrants.startLivestock` atomically saves an explicitly qualified
business/site/store/first worker, its real village grant and the already confirmed
initial herd. `LivestockRepository.openRows` joins that owner transaction without
committing it independently; the public standalone herd-opening contract remains.
An animal must already have matching saved village/world ownership, and trusted
species/remaining-maturity observations. Player/unrecorded animals, wrong worlds,
unsupported stores, unqualified work and failed final checkpoints leave prior rows
unchanged. Separate legacy grant/herd setups require an explicit future transition;
a startup replay cannot retrofit guessed facts. Exact accepted replay returns
current assets/cohorts after production and restart without refilling them.

Opening supplies zero feed/products and creates no animals. Existing actual local
purchases can pay an owned supplier for real grain and deliver it before the herd
uses it. Those ordinary purchases are not a new automatic starter-feed policy or
part of a live opening planner. Future automatic founding must establish real
customers, ongoing feed, suitable site/qualified workers, fresh observations and
safe physical reconciliation before calling this trusted backend. There is no
new schema migration; version remains81 and prior records remain intact.

## October16 — bounded read-only live animal evidence

`PaperLivestockSource` captures one supplied already-loaded entity chunk on the
main thread. It never obtains/loads chunks itself, schedules a scan, changes
entities, assigns ownership or writes SQL. Immutable `LivestockObservation`
records world UUID, chunk, time, actual cow/chicken IDs, positions, raw age ticks
and name/leash flags. No player name, name text or invented breeding provenance
is recorded. Age>=0 identifies an observed adult; conversion to simulation
maturity requires the future trusted adapter's explicit policy.

Wrong world/thread fails before entity access. Unloaded entity chunks, exceeded
entity/animal budgets, duplicate identities, unsupported entity implementations
or crossed/inconsistent locations return unknown with no partial roster. Normal
empty observations do not establish deaths or clear a herd. Mooshrooms/other
species are excluded. Defaults/limits are caller-supplied, bounded at4096 raw
entities/256 animals per capture; a future live observer still needs per-tick
scheduling and total village budgets. No live observation loop/adoption is enabled.
Ownership, recorded player breeding, pen geometry/protection and authority remain
separate mandatory checks before adoption. See PROJECT_STATUS/DEVLOG for verified
unit/native results and the pending milk/egg player-interaction decision.

## Persistent herd/product backend — schema76

`LivestockRepository` is an SQL-owner backend, not a live adopter or scheduler.
Opening requires a dedicated `livestock` business/store and already confirmed
village ownership matching the saved town/world. Trusted observations supply
species and each original animal's adult status or remaining active periods to
maturity. Ownership alone is not species/age evidence. Initial UUIDs can seed one
herd only; exact retries return current state without replenishment. One herd per
business keeps crop-farm labor/catalogue changes explicit. Unit-sized feed and
milk/egg catalogue entries must already exist; opening supplies no goods/money.

Actual eligible local jobs and editable per-worker/site work limits bound care.
Real stored feed pays for outputs and births; pending deliveries reserve capacity.
The conservative output budget uses current free space only: a full store idles
without assuming consumption will free room. Transactionally save feed debit,
owned milk/egg credit, period receipt and cohorts. Receipts include constrained
before/after stock, available work/output space and diagnostic explanations. Absent/disabled mode accrues
nothing; skipped/stale periods and changed operation terms fail. Exact historical
replay survives later periods and restarts without duplicating work or inventory.
Unavailable businesses have no productive labor; active-world cohorts still age.

Schema76 creates `livestock_herds`, `livestock_initial_animals`,
`livestock_periods`, `livestock_young`, all initially empty. No existing animals,
stock, cash, crop catalogues or histories are converted. Tested migration failure
rolls back all new tables/version; retry preserves prior ownership. Slaughter is
not exposed: physical retirement/player drops require a safe handshake first.
Logical birth cohorts also need physical identity mapping before live activation.
Player-death handling, startup freeze/reconciliation, adoption/transfer publication,
pen signs and actual scheduled business founding/production remain unfinished.

Twelve temporary-database cases include240 periods/restarts, real paid grain and
milk delivery with conserved cash, no-labor/shortage/storage limits, ownership/
reuse/catalogue guards, late write rollback/retry and migration recovery. Focused40
passed. Full .\mvnw.cmd clean verify passed1,451 (1,449 unit/two packaged integration),
zero failures/errors/skips;8:06 at15:16:14 ET. Guarded Paper26.2 build129 packaged
SQL backend proof passed15:17:04 ET: actual feed/cohort/products, restart replay
and absent-world pause. Schema75-to76 audit retained every predecessor table;
126/129 exact, only normal clock/runtime/ore bookkeeping changed; no aging.
New main-fixture herd tables empty. Fixture stopped, properties restored and helper
archived. No world/entity mutations by the helper; no client/physical birth proof.
Existing Windows OSHI performance-counter warning appeared; no Civilizations,
probe or listener error. No human server changes. No human-server edits.

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
