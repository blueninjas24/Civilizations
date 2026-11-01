# Terrain-aware home-site observation

Implemented October31: configurable vertical observation for already suitable
flat terraces. This is not grading, filling, excavation, retaining walls, sloped
roads or a new land permission. P4 item2.3 remains unfinished.

Under automatic-home-planning in construction.yml, vertical-search adds0..4
layers above and below the existing need.y-3 through need.y+plan.height-1 scan.
New files default2; an absent optional key preserves0 in old files. Wrong types,
unknown keys and out-of-range values refuse configuration. Files are not rewritten.
Handoff of an already frozen home needs its exact retained origin, so it uses the
existing exact-height observation rather than searching for another home location.

Selected template height, radius8..12, total bounded frame, two exact loaded-only
passes, shared reads and existing5-second evidence freshness remain unchanged.
A large/tall scan at low reads-per-tick can safely wait rather than authorize from
stale evidence. Never increase limits or generate land to make a proposal feasible.

The requested range is clipped to the present world's real minimum/exclusive
maximum height before enumerating positions. This fixes a discovered bottom-world
edge case where extra downward coverage could invalidate the entire scene. Cells
outside world height are never fabricated as air/support. A candidate requiring
missing cells still refuses. Absent worlds refuse; no chunks are loaded.

Pure checks cover config compatibility/types, whole higher roofs, height clipping,
extreme coordinates and total-volume bounds. Clean Windows Wrapper21 unit+3
packaged checks passed28.731s. Actual disposable Paper compared the old range with
extra2: old coverage refused the incomplete raised roof; new complete coverage
selected a home two blocks higher; protection refused it again. No project, land,
payment or building rights created. The fixture explicitly prepared its temporary
terrain; buried grass was stabilized to dirt to avoid random tick changes. All
captured terrain restored; server stopped/helper archived, schema119 save healthy.

Next: separately paid, permission-aware physical adaptation and wider premises.
Default building buffer4, road read caps, player protection, finances and simulation
rules are unchanged. No migration or user-server change.
