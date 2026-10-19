# Phase 4 — physical growth prototype gates

This checklist expands Phase 4 of ROADMAP.md; it does not move later phases forward.
Each checked item needs implementation and verification. Four groups, sixteen items;
current completion **4/16 (25%), 1/4 complete groups**. Partial work is described
but does not count as a completed item. This measures scope, not elapsed time.

## 1. Authored plans and frozen proposals — 4/4

- [x] Bounded, versioned, editable template format with staged costs/work and functional anchors.
- [x] Original small path/storehouse starter plans, attribution and strict validation.
- [x] All eight geometry/block-state orientations, tested against native Paper doors and pillar axes.
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
Unloaded chunks are incomplete evidence. No land approval is inferred. Existing
saved protections are explicitly not evaluated by this preview yet, so it always
reports missing development permission and cannot authorize work.

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
