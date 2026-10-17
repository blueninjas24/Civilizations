# Pen protection controls

This protects a bounded livestock pen and records its initial cows/chickens as
player-owned. It does not donate animals, start a farm or claim building blocks.
Existing animal owners cannot be overwritten. Unknown pens require explicit
admin resolution; sign placement and proximity alone grant no authority.

## Use in game

1. Place a sign with `[Civ Protect]` on its first line, beside the pen boundary.
   Either side of the sign is recognized. The sign must adjoin the saved fence
   or building boundary. Stand on flat ground inside the pen, near the sign.
2. Right-click it. An admin receives a bounded preview: floor area, current
   cow/chicken count, proposed owner (the confirming admin) and boundary particles.
   Nothing is claimed yet. The sign is not edited by the plugin.
3. Use `/civ pen confirm` within the preview lifetime. Fresh geometry and animal
   evidence must match. Permission, world and standing level are checked again.
4. A saved confirmation supplies the permanent pen UUID. `/civ pen here` can
   recover active IDs at your position, including both pens on a shared fence.
5. To release an area, use `/civ pen release <UUID>`, read the preview, then
   `/civ pen confirmrelease`. Only its saved owner or an admin may release it.
   Animal ownership and original pen history remain. Reclaiming the area later
   requires another explicit preview/new pen ID.

`/civ pen cancel` cancels an unsubmitted preview. It cannot undo a save already
submitted to the database owner. Uncertain save results are never automatically
retried; inspect `/civ pen here` before trying again. A disconnect after submission
does not roll back a committed protection record.

`civilizations.pen.admin` defaults to operators and permits explicit resolution
of uncertain pen ownership. It does not override existing animal owners.
`civilizations.pen.release` defaults to true but still requires saved ownership
for release; it also permits reading IDs at the current position. No claims-plugin
integration/delegated authority or player-to-village donation is implemented yet.

## Editable limits

`config.yml` has a `pen-protection` section. Missing settings use the prototype
without rewriting installed files: enabled,32-block scan radius,1,024 floor
columns,2 chunk snapshots/reads and64 geometry cells per controller tick,
128 animals,8 simultaneous requests/previews and60-second confirmation lifetime.
One capture job advances each tick; its internal30-second deadline bounds stale
work. Preview particles are private to the requester and capped at128 points.
Larger settings can need more time/memory; exceeding a limit declines the request.

Only already loaded chunks are inspected. Missing chunks, unsupported terrain,
oversized/open enclosures and incomplete entity frames cannot supply a partial
claim. Gates remain structural even when open. Supported flat geometry is the
existing `PaperPenSource` contract; ramps, ambiguous walls/doors/footing remain
unknown. Confirmation recaptures geometry and animal membership, not a frozen
world transaction. The sign must remain present during capture. There are no
block edits, animal mutations, forced chunk loads or global nearby scans in the
production controls. World registry observations and ownership writes use the
existing SQL-owner queue. Schema remains88.

## Verified scope

Eight new focused command/configuration cases cover preview/consent, revoked
authority, changed geometry/animals, expiry/movement/disconnect, SQL refusal,
two-step owner/admin release, cancel/shutdown and typed settings. Router coverage
is included. Final scoped Windows Wrapper verify passed67 unit plus2 packaged
integration tests (69 total), zero failures/errors/skips;26.961 seconds, finished
2026-10-17T11:44:00-04:00. This is not a full-suite run. The prior full checkpoint
passed1,788 tests. Hosted CI runs the full suite after the feature push.

Guarded Paper26.2 build129 passed11:44:59 EDT: actual sign event, actual flat
nine-column pen/one cow, admin refusal, explicit fresh confirmation, private
SQL/owner/controller restart, here lookup, release and retained animal ownership/
history. The fixture uses a synthetic player; human-client validation remains.
Its temporary pen blocks were restored and its exact test cow removed. Main
test save retained all149 tables/145 exact; only normal clock/runtime/capture/ore
metadata changed, no aging. Integrity/FKs clean, server stopped, original settings
restored, helper archived and backups retained. Human server untouched.

Next: connect protected habitat evidence to automatic livestock founding/feed/
production. Deliberate slaughter must implement the newly approved once-only
business-stock products without duplicate ground drops before it is enabled.
