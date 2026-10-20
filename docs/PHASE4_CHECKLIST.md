# Phase 4 — physical growth prototype gates

This checklist expands Phase 4 of ROADMAP.md; it does not move later phases forward.
Each checked item needs implementation and verification. Four groups, sixteen items;
current completion **4/16 (25%), 1/4 complete groups**. Partial work is described
but does not count as a completed item. This measures scope, not elapsed time.

## 1. Authored plans and frozen proposals — 4/4

- [x] Bounded, versioned, editable template format with staged costs/work and functional anchors.
- [x] Original small path/storehouse/four-bed-home starter plans, attribution and strict validation.
- [x] All eight geometry/block-state orientations, tested against native Paper doors, beds and pillar axes.
- [x] Read-only admin catalogue/site command plus immutable, hashed SQLite draft snapshots tied to permanent world/village IDs.

The site command is operational but cannot authorize or start construction. Draft
storage is a tested SQL-owner API; the command does not yet save drafts. Saved
proposals preserve reviewed inputs through file edits and restarts. They reserve
no land, goods, cash, workers or capacity. No existing business is activated by one.

## 2. Sites, roads and permission — 0/4

- [ ] Persistent explicit development-area approval and player/pen/protected-land checks at every write boundary.
- [ ] Bounded deterministic road-first local site/path selection, with explainable rejected alternatives.
- [ ] Terrain-aware permitted site adaptation and loaded-chunk revalidation, including entities and neighboring edits.
- [ ] Suitable vacant-premises reuse before new construction, with occupancy/permission records and collision prevention.

Partial: a conservative pure site review and bounded main-thread read-only scan
check air/interior space, ground support, entities and a dirt-path entrance.
Unloaded chunks are incomplete evidence. No land approval is inferred. Saved confirmed fenced-area
protections are now evaluated through the SQL owner queue. Automatic land screening
and current-write authority are unfinished, so a preview still cannot authorize work.

## 3. Paid staged work and useful buildings — 0/4

- [ ] Need-driven, locally funded projects with reserved real materials, available labor and protected operating budgets.
- [ ] Durable bounded block-write journal with interrupted/repeated-work recovery and conflict-safe pause/repair.
- [ ] Civic, industrial and residential construction activates actual capacity/function only after safe completion.
- [ ] Upgrade/reuse/maintenance/physical deterioration preserve sites and history; no automatic disappearance.

## 4. Acceptance and handoff — 0/4

- [ ] Real disposable-world path/building construction and restart/failure/rollback acceptance.
- [ ] Protected player land, changed blocks/entities, unavailable worlds/chunks and editor compatibility acceptance.
- [ ] Existing P1–P3 economy/population regression plus hosted Windows/Linux green for the completed phase candidate.
- [ ] Administrator settings/playtest guide and accurate progress/status/release candidate, with remaining survival tuning stated.

## Partial work after the first gate

Road-adjacent candidate search is deterministic, bounded by64 road seeds,256
candidates and65,536 cell reviews, and explains rejected candidates. Unknown
protection evidence blocks the search. It is not yet the live automatic land
recognizer or a road/path expansion planner.

Ordered SQLite block intents and a development-only native writer now demonstrate
physical placement, exact retry, interrupted acknowledgement and changed-block
conflict handling. The sole writer factory requires the marked localhost disposable
server and explicit test bounds. No normal server writer, demolition, repair approval
workflow or building activation is enabled. Private owned-material reservation
is a tested SQL API, not live project funding.

construction.yml contains approved editable funding eligibility for private business
projects and village homes/roads, with food plus essential upkeep/committed funds
protected. Quotes do not spend. Read-only preview checks now include saved confirmed
fenced plots via the SQL owner, including empty plots and release history. Broader
land recognition and current-write protection verification remain incomplete.

Earlier focused construction build:58 unit +3 packaged integrations, zero failures/errors/skips,
37.359s. Actual private Paper: five blocks placed, exact retries, DB reopen after
write/before ack, conflicting block retained, remote chunk stayed unloaded. Original
cells restored, test tickets released, helper archived/server stopped, schema101
integrity/FKs healthy. This is not a full process-restart or building acceptance.
An earlier native fixture unloaded its targets and had failed cleanup; only that
verified disposable process was stopped. Corrected test holds already-loaded chunks.
No human server or main modifications. These partial results do not complete any
additional whole gate yet; completion remains4/16 (25%).

## Evidence — first batch, October 19

Focused Windows Wrapper clean verify: 28 unit + 3 packaged integration checks,
zero failures/errors/skips, 21.630 seconds at 17:37:56 ET. Includes schema migration,
temporary-database reopen/idempotence/wrong-world/corruption checks and bounded
snapshot parsing. A previous restricted-sandbox packaged backup check failed on a
Windows temporary-file move; the approved normal-environment rerun passed.

Actual isolated localhost Paper 26.2 build 129: `ConstructionFoundationProbe`
passed at 17:35:44 ET, checking native orientations, installed templates, admin
routing/completion/permissions, invalid native materials and restored edited
fixture files. No block reads/writes or entity changes by the probe. Server stopped.
This is not a live-player scan test or construction acceptance. Schema 100 adds
only immutable draft storage; existing rows are preserved. No human server touched.

October19 supplier follow-up: sheep/wool shares existing livestock accounting,
protection and saved identity. Focused73 unit +3 packaged checks and actual Paper
SHEEP paid-feed/clock/conservation/pause/shear-guard checks pass. No house demand,
public procurement, booked construction labor or automatic building activation yet.

October19 site/restart follow-up: exact-draft saved site reservations reject
overlap and newly protected plots before material debit, with unused-only release
and retained history. Focused77+3 and exact-proof51+3 builds pass. Actual native
four-bed home completed120 blocks over two JVM starts after a60-write/59-ack
checkpoint; all121 originals restored. Both fixture DBs healthy/FK-clean at104.
This strengthens recovery evidence but does not finish funded/labor/occupancy
construction acceptance or enable normal growth; whole-gate count stays4/16.

October19 unused-stock follow-up: complete returns preserve goods/history and
wait for source capacity. Prepared native work cannot take the shortcut. Passing
80+3 focused build includes rollback/restart/version104 preservation; schema105
adds return receipts only. No demand/work/activation gate completed by this API.

October19 housing follow-up: pure whole-household proposals reuse verified permitted
vacancies first and wait for complete evidence, fitting plans and absent worlds.
Bed capacity requires complete distinct anchored pairs. Focused55+3 and native
all-orientation/settings checks pass at schema105. No live housing assignment or
autonomous demand-to-funded-project gate is claimed; count remains4/16.
