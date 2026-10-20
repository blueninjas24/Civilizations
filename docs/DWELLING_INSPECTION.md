# P4 bed and access inspection foundation

Implemented components: HomeBuildingLayout, DwellingInspection and the incremental
PaperDwellingSurvey adapter. These assess declared layouts; they do not discover
apartments, grant consent, register occupancy or add settlement housing capacity.
No command or normal simulation task currently schedules these surveys.

## Authored home declarations

The existing single-family home plan becomes one explicitly identified unit with
four declared bed pairs, not four apartments. Its draft UUID identifies the proposed
building and deterministically derives the unit UUID. Rotation/mirroring transforms
entrances, bed feet and facing together; plot and physical structure remain distinct.
Non-home plans, invalid authored bed pairs, ambiguous primary entrances and plots
too small to contain the home are rejected. This conversion does not complete a
project or give it ownership or capacity.

## Current immutable evidence

DwellingInspection accepts a complete bounded structure observation tied to the
permanent world UUID and exact layout fingerprint. Missing/unknown cells, wrong
worlds, future timestamps, changed layouts and stale observations yield no assessed
units. This is an unverified assessment, not evidence that a saved home disappeared.
Callers must retain previous state when evidence is incomplete.

For each explicitly declared unit, check actual bed foot/head material and facing,
support, clear headroom, no extra undeclared private bed halves, and access to its
entrance and beds. Only that unit's private space and the building's shared space
are traversable during its review. A route across another family's apartment is
rejected. One unit's broken bed need not disable a different valid unit.

Routes require supported two-block-high standing space and cardinal steps of at
most one block with head clearance. No vertical flying or diagonal corner edges.
The shared path-visit budget is262,144 per assessment; a budget-exhausted unit is
explicitly unverified. The result reports accessible bed pairs, not legal or fully
habitable village capacity. Roof/enclosure quality, broader building condition,
permission, occupancy and current protection remain separate gates.

## Paper read boundary

PaperDwellingSurvey requires the main server thread. A caller advances it with
at most256 cell reads per call; it has no hidden recurring task or per-player
multiplier. Future orchestration must share a global tick budget. The constructor
uses a coordinate cursor rather than allocating/sorting every world cell on the
server thread. The pure assessment can run outside the Paper thread afterward.

The survey reads only already-loaded chunks in the named present world, within
height and border limits. Missing chunks/worlds and expiry produce incomplete
evidence. It neither force-loads chunks nor modifies blocks/entities. Completed
observations are immutable and exact retry returns the same observation.

Native classification is deliberately narrow: air and wooden doors can provide
access; actual beds retain material/part/facing; safe non-gravity full occluding
blocks provide support. Known damaging blocks and waterlogged shapes are unsafe.
Unsupported partial shapes, vegetation, fluids and non-wooden doors stay blocked.
Native stair/slab geometry needs a later collision-shape extension; the current
domain route proof supports ordinary full-block steps. This may reject a usable
building, so an unsuccessful review is not demolition or ownership evidence.

An incremental scan is not an atomic world snapshot. A future occupancy/writer
adapter must detect intervening edits and revalidate before activation. Nothing
in this batch enables a permission or physical housing writer.

## Native check

src/test/paper/DwellingInspectionProbe.java is restricted to a marked, empty,
isolated loopback server. It stages an original four-bed home in a disposable
footprint to exercise real block classification; no paid construction or donated
housing is claimed by this fixture. It checks a broken bed half, absent worlds
and unloaded targets, restores its original cells and stops the private server.
See DEVLOG.md for actual run results; source availability is not a passed run.

## Verified October20

Ten new JUnit checks cover the inspection and authored-home bridge, including all8
orientations, separate private access, real paired beds, supported upstairs routes,
per-unit damage, incomplete/stale/wrong-world evidence and extreme coordinate/time
budgets. The final focused Wrapper clean verify passed49 unit +3 packaged checks
in26.678s. The preceding schema108 checkpoint full build passed2096 +3 in16:19.

Actual checksum-verified Paper26.2 build129 passed the restricted probe: four real
bed pairs with a closed wooden doorway; removing a real bed head was detected;
remote chunks remained unloaded; absent worlds stayed incomplete. Restored144
original cells, stopped the private server and archived its helper. Read-only
post-stop audit found schema108 integrity/FKs healthy and empty building tables.
No donation, occupancy, population capacity or earned construction was activated.
