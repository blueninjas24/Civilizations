# Workplace access inspection

Available November1: an operator can inspect a selected existing workplace without
registering, donating, reserving, activating or modifying it. This is preparation
for the approved reuse-before-construction flow, not automatic business relocation.

1. Find the saved business UUID with /civ debug businesses <village UUID>.
2. Select the floor-to-roof structural bounds with /civ building corner1 and corner2.
3. Stand at a safe entrance inside the selection.
4. Run /civ building workplacecheck <business UUID> and retain the selection.

Requires civilizations.building.admin, which defaults to operators, and the enabled
building-protection feature. The business must belong to the current saved world
and its fixed marker must be inside the selected structure. Selection supplies no
ownership or donation evidence. A result reports a reachable standing position or
specific refusal findings; no title, chest access, stock, labor or capacity moves.

## Observation and assessment

PaperWorkplaceSurvey reuses the bounded two-pass structural reader, without
calling dwelling assessment or housing assignment. Exact marker reads bracket
the structural survey within the same caller budget. Replaced or missing markers,
absent worlds, unloaded chunks and expired evidence remain unverified. The four
supported marker types are smithing table, barrel, smoker and crafting table.
No reader loads chunks or reads block inventories.

WorkplaceInspection is Paper-independent. It requires complete matching world/
layout evidence and safe supported two-block entrance clearance. A deterministic
bounded search permits conservative one-block steps with extra head clearance.
It seeks an adjacent same-level standing space at the actual fixed marker. Any
observed hazard refuses. Covered work is the conservative diagnostic preference;
its pure policy is explicit, but no new production rule or editable live setting
is enabled. Roof preference never bypasses support/access/marker checks.

The operator queue accepts four requests, one per player, with one shared128-cell
advance per tick including marker reads. Pending owner lookups do no native reads.
Permission, current world, selection, disconnect,30-second expiry and shutdown
are checked. Domain assessment runs on the existing owner queue; Paper reads stay
on the main thread. The command adds at most128 manual reads per tick while used;
it does not create another simulation clock or background automatic survey.

BusinessPremises.reserveInspected additionally requires a computed suitable
assessment for the exact frozen building and fixed business marker, no older
than5 seconds. Existing transactional consent, closure, positive marker and
occupancy checks still run. It remains a hold only, not an operating permit.

## Verified scope and limitations

Clean Windows Wrapper33 unit+3 packaged integration checks passed33.223s:

    .\mvnw.cmd clean verify -Dtest=WorkplaceInspectionTest,BusinessPremisesTest,DwellingInspectionTest,PaperWorkplaceReviewTest,PaperBuildingProtectionTest

Tests cover safe deterministic access, blocked entrances/routes, hazards, cover,
missing/stale/foreign evidence, exact-marker reservation matching, queue budgets,
permission/world/selection changes, expiry, disconnected players and failed owner
lookups. A test-only missing import was corrected before the final passing build.

Actual disposable Paper on the final packaged JAR passed covered access, a one-read
budget, marker replacement during capture, blocked routes and missing shelter.
All100 captured fixture cells were restored; server stopped and helper archived;
schema120 database healthy/FK-clean. The fixture retained its already loaded chunk
with a plugin ticket; production readers never load/retain simulation chunks.

An initial fixture lost its chunk and correctly refused incomplete evidence.
Cleanup could not restore that unloaded failed fixture; it was stopped/archived
and kept as disposable evidence, without changing any human server. The corrected
fixture and final acceptance restored their own complete captured terrain.

The native probe exercises the actual reader/assessment, not a human command
session. Command lifecycle has injected-player tests; human-client acceptance
remains. This is conservative access/shelter evidence, not structural engineering,
guaranteed villager AI navigation, future entity clearance, inventory permission,
or proof that every industry can operate there. Whole-building donation confirmation,
saved operating reviews, ordinary reuse selection and relocation remain unfinished.
P4 item2.4 stays unchecked;8/16=50%. No migration beyond existing schema120.

## Hosted regression follow-up

The previous schema120 commit65647f6 failed Windows/Linux CI in four historical
migration snapshot cases. Local reproduction confirmed that the new empty
business_premises table was missing from their later-table expectations, rather
than a changed predecessor row. Each fixture now explicitly asserts that the new
table is empty while retaining exact predecessor snapshot comparisons. No schema,
application behavior, foreign key or old-data assertion was relaxed. The final
combined Wrapper run (the five classes above plus GeneratedTerrainMigrationTest,
ForestryProvenanceTest, ForestryPersistenceTest and ForestryFounderStudyTest)
passed47 unit+3 packaged checks in41.629s. New hosted results remain to be checked.
