# P4 building and dwelling layout foundation

Implemented: immutable, plain-Java declarations of a plot, physical structure,
shared spaces and individually identified dwelling units. This is geometry only;
it does not discover, register, donate, occupy or modify a Minecraft building.

## Separate land, buildings and homes

Each layout retains permanent building, settlement and world UUIDs and an explicit
housing/workplace/civic/infrastructure category. Inclusive three-dimensional plot
bounds contain separate structure bounds. A yard therefore does not automatically
become indoor housing space. Housing explicitly declares one or more units;
non-housing declarations currently contain no units. Mixed-use buildings would
need a later extension rather than an inferred housing entitlement.

Each dwelling has its own UUID, entrance, private space boxes and declared bed
foot/facing pairs. Multiple boxes can describe irregular rooms in one home.
Several bedrooms/beds do not create extra units. Upstairs and downstairs units
can use the same X/Z footprint at different heights. Shared corridors/stairs are
separate from private unit space. No private/shared regions may overlap, including
duplicate space inside one unit. Bed pairs must be complete, distinct and wholly
inside that unit; a shared-area bed cannot increase its declared capacity.

Geometry safety limits: 64 units/building, 16 private boxes/unit, 128 shared boxes,
512 declared beds/building, structure volume 65,536 cells, and each bounds axis
at most 512 cells. These are parser/computation ceilings, not gameplay growth
targets or new admin configuration. Oversized/reversed/overflowing coordinates
are rejected before world inspection. No world scan is performed here.

## Freeze reviewed inputs

BuildingLayoutSnapshot provides a version-1 canonical bounded binary format and
SHA-256 semantic fingerprint. Sorting removes collection-order differences while
retaining unit identities. World changes, unit changes or boundary edits change
the fingerprint. Decoding rejects unsupported versions, excessive counts/bytes,
invalid geometry, truncation, trailing data and non-canonical order.

The snapshot format is now stored by the transactional schema108 BuildingRegistry;
see BUILDING_REGISTRY.md for saved consent and protection history. A layout snapshot
alone is not a substitute for permission, ownership or occupancy records.

## Remaining before usable housing

- Complete habitability and edit-aware activation checks. DWELLING_INSPECTION.md
  now describes verified loaded-world bed/support/headroom/access review; it does
  not establish full enclosure or activate housing.
- Explicit authorized donation/confirmed purpose for player structures; known
  completed Civ construction identity for Civ buildings. Geometry grants neither.
- Saved protections, donor/admin reclamation and editable boundary previews.
- Transactional household assignment, displacement and current usable capacity.
  A declared bed count must never be added directly to settlement capacity.

No rent, landlords, private purchase accounting, automatic apartment guessing,
new building signs/commands or player property claims are introduced. Existing
settlement discovery and the separate vacancy proposal planner are unchanged.

## Verification

Windows Wrapper focused clean verify: BuildingLayoutTest, HousingProjectPlannerTest,
ConstructionTemplateTest and ConstructionTemplateSnapshotTest: 18 unit checks and
3 packaged integration checks, no failures/errors/skips, 21.721 seconds.
Nine new layout checks cover stacked apartments, multi-room family homes,
overlap/capacity/identity/bounds failures, immutable canonical world-linked
round trips and malformed snapshots. No Paper API changes or native test were
needed for this plain-Java geometry batch. No server/world/real save was accessed.
