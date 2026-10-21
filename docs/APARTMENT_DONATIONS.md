# Explicit new apartment declarations

Implemented: a bounded unsaved editor for NEW buildings, explicit private units,
multi-room units, shared circulation and a separately selected plot. Frozen review
uses current real block evidence and the existing ownership/donation confirmation.
No guessed apartments, rent, title sale or alteration of registered geometry.

## Player workflow

Use the existing /civ building corner1 and corner2, with optional exact X Y Z.
Select the complete structure including floor/roof; stand at its main entrance.

- apartments <village UUID>: start a new draft; freeze structure/world/main entrance.
- unit <name>: use current selected box and standing position as private space/entry.
- room <name>: append another selected private box to an existing unit.
- removeunit <name>: remove an unsaved unit, allowing its boundaries/entry to be redrawn.
- shared: append the selected shared corridor/stair box.
- clearshared: remove unsaved shared boxes for correction.
- apartmentplot: set selected surrounding plot, which must contain the structure.
- apartmentstatus: show the draft, with at most8 unit details in one response.
- reviewapartments: freeze the draft and start a bounded loaded-only region review.
- cancelapartments: discard the unsaved editor; cancelhome handles a submitted review.

Prefix each with /civ building. Unit names are1..24 lowercase letters, digits,
underscores or hyphens. Names are editor labels used to derive stable unit UUIDs;
the registered layout retains unit IDs, not a separate display-name catalogue.
All private/shared boxes must be disjoint and inside the structure. Coordinates
are inclusive; touching the same block is overlap. Rooms may be irregular unions;
stacked units may share X/Z coordinates with separate Y ranges. No automatic unit
inference from beds, doors or wall shapes. Existing geometry ceilings still apply.

Review finds bed feet in each explicitly declared unit, requires complete bed pairs
inside it, then checks all units' support/headroom/cover/access. Shared-area beds
provide no private capacity. A route cannot cross another unit. Partial-shape
stairs still have conservative native route classification; geometry alone does
not prove every staircase usable. Incomplete/stale/wrong-world evidence refuses
the preview. No save, land claim or capacity is created on review.

The existing clickable ownership confirmation or confirmhome <token> owned saves
exact reviewed geometry and consent. Saved donor, world, protection, pen and reserved
project conflicts are rechecked on the SQL owner. No first-donation admin approval
is required under the approved explicit player ownership declaration policy.
New declarations cannot overwrite a registered building or its history.

## Lifetime, budgets and housing

Four unsaved editors maximum; ten idle minutes, disconnect, world change or lost
donation permission discards a draft. Restart also discards it. Four home reviews
maximum share128 native cell reads/tick with single-home donations, rather than
adding another scan allowance. Scan deadline30 seconds; confirmation60 seconds.
Corner changes after review affect future selections, not the frozen apartment
preview. Failed submission preserves the unsaved draft; accepted SQL work drains
even across disconnect. Cancellation cannot undo a committed donation.

Normal housing reviews can assign suitable whole saved households afterward, one
per unit. No splitting families, automatic new households, teleportation or free
extra capacity. Reclamation currently withdraws the whole building using its saved
ID; per-unit reclamation and registered-boundary revisions remain unfinished.
No database migration: schema110 already supports multiple units and consent.

## Verification

Windows Wrapper focused clean verify: ApartmentDeclarationTest,
PaperApartmentDraftsTest, HomeDeclarationTest, HomeDonationPreviewTest,
PaperHomeDonationTest, PaperBuildingProtectionTest, DwellingOccupancyTest,
PaperHousingRuntimeTest and BuildingConsentRequestsTest:46 unit +3 packaged
integration checks passed40.930s, zero failures/errors/skips. New coverage checks
distinct stable units, shared/private access, immutable correction, overlap/name/
pair/roof/stale/world failures, editor budgets/expiry and actual temporary-SQLite
permission persistence. No full-suite rerun is claimed.

Actual private Paper26.2 build129: ApartmentDonationProbe passed a donation-only
injected actor through the live editor, real two-unit bed/access/roof survey,
preview-only refusal to save, explicit confirmation, exact saved geometry and SQL
session restart. Restored144 fixture cells; stopped server/helper archived. Both
fixture databases: schema110, integrity ok, zero foreign-key violations. The first
fixture helper lacked the existing two-bed recognition minimum and was corrected;
no application behavior was changed to accommodate it. This is not a human-client
or arbitrary editor-plugin compatibility certificate. No user's server/save touched.
