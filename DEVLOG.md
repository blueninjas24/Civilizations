# Civilizations Development Log

## 2026-10-21 — bounded automatic home-planning feed

HousingPlanningJobs pages at most8 permanent saved villages, skips absent worlds
and villages at their configured active-home cap, then emits one deterministic
verified whole-family PROPOSE job. Unknown/stale housing and suitable vacancies
never become construction demand. Identity derives from household/world/month
and retained proposal count; repeat reads are identical and cancellation cannot
reactivate an old identity. Limited preparation rechecks active count inside its
existing transaction; the generic preparation API retains a bounded64 default.
Serialized PopulationSession feed/limited-preparation calls freeze template input.

Focused Wrapper clean verify40 unit +3 packaged checks passed39.566s. Two real
temporary-SQLite cases cover no-write deterministic feed, absent/stale holds,
pagination, cap rejection without orphan records and fresh post-cancellation
identity. Native survey scheduling is next; no production timer or world changes
in this batch. Schema113 and P4 at25% remain.

## 2026-10-21 — live retained-project inspection and unused cancellation

Added PaperHousingProjectControls, production lifecycle/routing/tab completion,
operator-default civilizations.construction.manage permission, owner-queued
individual project inspection and actual read-only status. `/civ build projects`
uses existing preview permission. Separate cancelproject/confirmcancel/cancelrequest
controls preserve frozen player/world identity,60-second confirmation, current
authority and bounded four-actor queues. Exact accepted-save retries retain token
and submitted time; disconnect/shutdown never cancels an already accepted SQL save.
Unsubmitted requests/previews are discarded on authority/world loss. Paid/started
work and unreturned materials are rechecked by the atomic backend; no refund,
demolition, money/goods/people deletion or automatic building introduced.

Focused Wrapper35 unit +3 packaged checks passed37.673s. Final controller plus
individual SQL status/foreign-world check5 +3 passed28.131s. Actual approved
disposable Paper routed project reporting, default operator management permission
and refusal of cancellation with read-only permission passed alongside apartment,
renovation, housing report and SQL-session restart proof. Actual cancellation was
verified by controller and real temporary-SQLite tests, not claimed as a native
client playtest. Restored144 cells; stopped/archived helper; both DBs113/ok/FKs0.
Handbook0.14:30 pages/28 chapters, all rendered/visually reviewed, new page30 full
size; contents/footers checked. Schema113/P4 at25% remain; real server untouched.

## 2026-10-21 — retained project status and atomic unused retirement

HousingProjectStatus reads up to8 active-first saved family projects, frozen plan,
site hold, paid orders, durable intents, current need pause and unused-cancellation
eligibility. Additional history is explicitly retained; absent worlds cannot
claim current cancellation eligibility. No writes occur during inspection.
HousingProjects.retireUnused composes site release and family cancellation in
one owner transaction. Paid supply, prepared work or unreturned owned stock hold
for reviewed recovery. Exact receipt retries never revive a cancelled project.
PopulationSession exposes serialized reads and trusted-adapter retirement.

Focused `.\mvnw.cmd -Dtest=DwellingOccupancyTest,ConstructionSiteReservationsTest,ConstructionSupplyOrdersTest,ConstructionMaterialsTest clean verify` passed50 unit
+3 packaged checks51.789s. Two temporary-SQLite cases prove read-only status,
current-need/absent-world reporting, cancelled history, final-update rollback,
exact retry, active-first order and refusal to discard prepared work. No migration
or server change. No player command in this backend batch; admin controls next.
P4 remains25%/4 of16; schema113 preserved.

## 2026-10-21 — historical rollback assertion after hosted regression

Read the hosted Linux failure for e036932:2181 unit cases, zero assertion
failures, one error in ResidentProjectionRepositoryTest.failedMigrationRollsBack...
Its injected migration correctly rolls all the way back to schema12, then its
assertion called today's DemographicRepository reader, which correctly expects
new managed-housing tables. Replaced only that historical assertion with a full
legacy PopulationState assembled from schema12 people, households, relationships
and raw capacity. The version/table-rollback/recovery assertions remain; no
production migration, exception handling or historical save behavior changed.

Focused `.\mvnw.cmd -Dtest=ResidentProjectionRepositoryTest clean verify` passed
9 unit +3 packaged checks35.089s. Hosted Windows for e036932 remained running
and newer branch candidates queued; no latest full CI green claimed. GitHub CLI
is unavailable, so read-only Actions API/logs were used; credentials stayed in
memory and were never printed or stored. Main and real server remain untouched.

## 2026-10-21 — recheck need before committing unstarted homes

HousingProjects now revalidates household location, living membership, effective
known capacity and whole-family plan fit before unstarted site/material authority,
public labor or first economic block intent. Existing site/cargo/material/history
remain when need changes. Restored need can resume after normal authority checks.
Any durable physical intent keeps the existing recovery/completion path because
a lost acknowledgement could mean blocks already exist. Cancelled projects still
never reactivate. No automatic reassignment, refunds or demolition introduced.
Recommendation recorded under the user's delegated P4 prototype authority.

Focused `.\mvnw.cmd -Dtest=DwellingOccupancyTest,ConstructionPublicWorkTest,ConstructionSupplyOrdersTest,ConstructionMaterialsTest,ConstructionSiteReservationsTest clean verify` passed56 unit +3 packaged checks55.343s. Two temporary SQLite
cases cover rehousing/unknown capacity/family growth/saved death and retained
physical intent; one actual food-period integration case proves paid stock pauses
unspent and work resumes only when the housing need returns. No world/server
changes or migration; schema113 and P4 at25% remain.

## 2026-10-21 — in-game housing decision report and handbook0.13

Added PaperHousingCandidates and `/civ build housing <village UUID>` routing with
the existing construction-preview permission. Four one-per-player owner-queued
reads,30-second expiry, main-thread result delivery, changed-world/authority and
disconnect/close safeguards. Shows real saved vacancy-first family decisions and
unknown/project holds; starts no scan, land claim, proposal, purchase or capacity.

Focused `.\mvnw.cmd -Dtest=PaperHousingCandidatesTest,CivilizationsCommandTest,DwellingOccupancyTest clean verify` passed27 unit +3 packaged checks36.586s.
Actual approved disposable Paper26.2 build129 routed controller/read-only report
and permission rejection passed alongside apartment/renovation/session-restart
proof. Restored144 fixture cells, stopped server, archived only helper JAR; both
fixture databases schema113/integrity ok/FKs0. No real server changes.

Handbook0.13 remains29 pages/27 chapters, with updated command glossary, planning
explanation and schema113 baseline. All29 pages rendered and visually reviewed;
changed pages1/15/21 inspected at full size, contents/bookmarks/footers checked.
P4 remains25%/4 of16; actual automatic construction remains ahead.

## 2026-10-21 — atomic need-driven home preparation

HousingProjectPreparation connects the real saved-family planner to buffered
road-first selection and one atomic frozen-draft/beneficiary/site reservation.
Saved protections are queried again on the SQL owner, and native cells outside
that reviewed envelope cannot supply unchecked margins. Exact replay returns
retained history only, including released/cancelled proposals. Existing draft,
binding and site repositories expose package-private transaction row operations;
their public standalone transactions remain unchanged. PopulationSession provides
a serialized immutable-input preparation API. No funds, workers, beds or blocks
are created. The search envelope implements approved guarded automatic growth
for dry scheduling only; it is not native write permission. No new schema beyond113.

Focused `.\mvnw.cmd -Dtest=DwellingOccupancyTest,ConstructionDraftRepositoryTest,ConstructionSiteReservationsTest,ConstructionSitePlannerTest,ConstructionMaterialsTest,ConstructionSupplyOrdersTest,ConstructionBlockJournalTest,ConstructionPublicWorkTest clean verify` passed67 unit +3 packaged checks56.954s.
Three temporary-SQLite cases cover whole pipeline/restart/cancellation, rollback
of earlier writes when the final site save fails, current exclusions, stale
frames and unchecked/missing margin refusal. Existing material/supply/labor/journal
checks pass after transaction composition. No Paper adapter or actual server
changed; production timer/native writer/completed capacity remain unfinished.
P4 stays25%/4 of16.

## 2026-10-21 — buffered road-first site alternatives

Connected transformed ConstructionLandPolicy requirements to the deterministic
ConstructionSitePlanner. Every rotation/mirror now checks clear surroundings
before viable sites are ranked, retaining exact land findings. Missing/conflicting
terrain classifications cannot masquerade as safe air; protected margins count.
The existing65,536 total cell-review budget includes buffer work. Legacy explicit
development review remains available; no production permissions or world edits
are inferred. Rectangular eight-orientation checks and candidate fallback/unknown/
protection coverage added. Focused `.\mvnw.cmd -Dtest=ConstructionSitePlannerTest,ConstructionLandPolicyTest,ConstructionPreviewTest clean verify` passed14 unit
+3 packaged checks27.281s. First protected fixture accidentally excluded its road;
fixture corrected to exercise candidate rejection. No server changes or migration.
P4 remains25%/4 of16; safe automatic scheduling/write authority remains unfinished.

## 2026-10-21 — household-bound home proposals

Schema113 adds durable whole-household/plan/village/world proposal identity,
serialized PopulationSession operations, exact historical retries and explicit
unused cancellation. HousingPlanning now holds only mapped families; conservative
whole-village holds remain for historical active projects with unknown beneficiaries.
New proposals require current known need, verified vacancy-first planning, actual
saved calendar, a fitting frozen plan and no prior funding/site/work. Cancellation
requires released/unused sites and fully returned owned materials; paid supply or
prepared blocks require reviewed recovery. Cancelled drafts cannot reacquire land
or material authority. No world edits, money, residents or assignments are created.

Focused `.\mvnw.cmd -Dtest=DwellingOccupancyTest,HousingProjectPlannerTest,ConstructionSiteReservationsTest,SchemaMigratorTest,ForestryFounderStudyTest,ForestryPersistenceTest,ForestryProvenanceTest,GeneratedTerrainMigrationTest clean verify`
passed55 unit +3 packaged checks in48.957s. Four new real temporary-SQLite tests
cover restart, family isolation, safe holds, cancellation, SQL rollback and
preserving upgrade112->113. An initial test initialized its own calendar twice;
fixture corrected before the passing build. Historical additive-migration fixtures
include the new empty table. No Paper adapter or actual server changed in this batch.
P4 remains25%/4 of16 checked items.

## 2026-10-21 — separate owner renovation consent

Implemented the user's separate-consent decision in BuildingRenovations schema112,
RenovationConsentRequests, serialized PopulationSession APIs and live
PaperRenovationConsent controls. Donation remains permission to use; owner-only
renovation grant and donor/admin withdrawal require separate frozen previews and
confirmation. Current donation/layout/history/world/authority are checked again.
New default-true renovate permission cannot override saved donor identity. Inspection
shows saved consent, including for renovation-only donors without plot protection.

Permanent ordered grant/revoke receipts bind the exact use epoch/layout and previous
renovation decision. SQL failure rolls back; exact retries freeze submitted time/
authority. Old receipts never restore later-revoked grants. Reclaim/re-donation
invalidates old grants. Found and fixed a withdrawal edge: temporary plot exclusion
must not prevent permanent revocation, or lifting the exclusion could revive a grant.
No physical building change, inventory, ownership, housing or extra land is granted.

Focused Wrapper clean verify78 unit +3 packaged checks passed57.313s for full
service/controller/migration coverage. Final `.\mvnw.cmd -Dtest=BuildingRegistryTest,HomeDonationPreviewTest,PaperRenovationConsentTest,PaperBuildingProtectionTest,PaperDwellingReviewTest clean verify`
passed33 unit +3 packaged checks31.345s after protected-plot withdrawal and inspection
permission checks. New unit/integration coverage uses only temporary DBs; historical
migration fixtures recognize the new empty table. No broad-suite rerun claimed.

Actual private Paper26.2 build129: default permission installed, two-unit donation,
separate native controller grant/revoke confirmations and saved allowed/denied
status for a donor lacking admin/protection. Two saved units and both consent
receipts survived SQL-session restart. Restored144 cells; stopped fixture/helper
archived; both DBs schema112/integrity ok/FKs0. Real user's server untouched.

Updated status/progress/checklist/decisions and BUILDING_RENOVATIONS documentation.
Handbook0.12 is29 pages/27 chapters with working command explanations, linked
contents and separate-use/renovation limits. All pages rendered/reviewed. P4 remains
25% by complete gates. Next: stable household/project binding before autonomous
housing procurement/construction. User delegated remaining prototype recommendations;
locked design, actual resources, player land and main/server safeguards remain.

## 2026-10-20 — approved new adult households and scan-safe housing need

Implemented the user's approved single-adult/already-recorded-partner household
formation. Complete permitted live housing reviews call the serialized transaction,
using existing adulthood age and configurable enabled/month/candidate limits.
Defaults:2 households per village civilization month,128 founder candidates/pass;
repeated scans share the saved cap. Stable derived identity and schema111 formation/
member receipts preserve history and prevent duplicates. Existing families, children,
people, money and housing rights are untouched. Ambiguous/housed partners and known
unassigned dependent children hold for later review rather than forcing family moves.

Formation, membership, known zero capacity and history commit together. Fixed an
integration risk found during review: later enrollment refreshes raw bed-group
capacity, so explicitly formed households now retain known zero through that refresh.
Active inspected assignments still supply actual capacity. Atomic failure/retry,
month cap, partner/family/child holds, additive110 upgrade and real vacancy assignment
are covered by focused temporary-SQLite tests. No production DB reset or server copy.

First focused run exposed three old migration snapshot SQL exclusion lists that
needed the two new empty tables; updated fixture lists/rewind order without changing
application behavior. Final migration/population clean verify74 unit +3 packaged
checks passed1:06. After the capacity safeguard, `.\mvnw.cmd -Dtest=HouseholdFormationTest,HouseholdFormationConfigurationTest,DwellingOccupancyTest,HomeDonationPreviewTest,SchemaMigratorTest,DemographicRepositoryTest,VillageEnrollmentRepositoryTest clean verify`
passed60 unit +3 packaged checks54.475s, zero failures/errors/skips. No new broad-suite
run is claimed. Updated native helper initially mixed resident/entity UUIDs; fixed
only the helper, preserving real enrollment identity rules, and repeated successfully.

Actual private Paper26.2 build129: existing family kept intact; a separately imported
adult formed one zero-capacity household; native beds/roof/access, damage/repair,
absent world, SQL-session restart and donor reclamation retained three people and
one formation history. Restored144 cells; fixture stopped/helper archived; both DBs
schema111/integrity ok/FKs0. Actual user's server untouched. Updated current status,
progress, checklist, formation documentation and handbook0.11 (28 pages/26 chapters),
with all pages rendered/reviewed. P4 remains25% until complete gates pass. Automatic
construction, saved-boundary revisions and business-premises reuse remain ahead.

## 2026-10-20 — saved housing candidates before construction

Connected current effective capacity, living saved households and permitted fresh
unit vacancies to HousingProjectPlanner through HousingPlanning and the serialized
PopulationSession owner API. Unknown household capacity is held separately rather
than converted to zero; unassigned people are counted but no family is invented.
Incomplete/stale vacancy reviews cannot bypass reuse by proposing a new building.
A retained active home project conservatively holds proposals because the current
draft schema has no household-beneficiary link. This limitation is explicit.

Read-only, bounded result/input budgets and diagnostics; no new command, automatic
scheduling, occupancy, spending, world operation or migration. Existing planner
proposal limits, seed and period remain authoritative. No title transfer is inferred
from permission to use a donated vacancy.

`.\mvnw.cmd -Dtest=DwellingOccupancyTest,HomeDonationPreviewTest,HousingProjectPlannerTest,ConstructionSiteReservationsTest clean verify`
passed25 unit +3 packaged checks38.110s, zero failures/errors/skips. New real temp
SQLite coverage proves no writes, safe vacancy reuse, unknown/stale/absent-world
holds and retained-project deduplication. SQL-owner application integration passes.
No extra native rerun needed for this Paper-independent read-only connection; no
new broad-suite run. P4 remains25%; no additional whole checklist item complete.

User approved configurable new adult households/recorded-partner grouping after
this exposed unassigned adults waiting for housing. That next feature is approved,
not yet implemented by the candidate report. Existing families stay intact and
children gain no invented parentage. Living handbook already accurately describes
this unfinished boundary; no new user command or PDF revision in this batch.

## 2026-10-20 — explicit apartment declarations and confirmed donation

Added ApartmentDeclaration immutable drafts/review and a bounded Paper apartment
editor connected to the existing donation controller/SQL-owner consent workflow.
Players mark new private units, additional rooms, shared circulation and separate
plots. All real bed pairs/access/roof must qualify; no inferred units or rights.
Unit identity is deterministic within a frozen building; existing layouts/history
are never overwritten. No rent, title sale, family splitting or additional scan
budget. Normal housing can use the saved vacancies. Schema110 unchanged.

Four unsaved editors expire after ten idle minutes, world change, disconnect or
permission loss. Failed edits preserve the previous immutable draft. Submitted
reviews freeze their own geometry; accepted saves drain normally. Whole-building
reclamation remains available; per-unit withdrawal/saved revisions remain future.

`.\mvnw.cmd -Dtest=ApartmentDeclarationTest,PaperApartmentDraftsTest,HomeDeclarationTest,HomeDonationPreviewTest,PaperHomeDonationTest,PaperBuildingProtectionTest,DwellingOccupancyTest,PaperHousingRuntimeTest,BuildingConsentRequestsTest clean verify`
passed46 unit +3 packaged checks40.930s, no failures/errors/skips. Six new focused
checks plus expanded actual temporary-SQLite owner integration. No broad rerun.
Actual disposable Paper26.2 build129 passed two-unit editor, real native review,
explicit ownership confirmation, stable saved geometry and SQL-session restart;
restored144 cells, stopped server/helper archived, schema/integrity/FKs healthy.
First helper fixture lacked the existing two-bed recognition minimum; corrected
only the fixture and repeated successfully. Actual user's server untouched.

Updated status/progress/checklist and new APARTMENT_DONATIONS documentation.
Corrected checklist land-authority wording to the already approved automatic-growth
policy, without changing the roadmap or marking a new gate complete. P4 remains
4/16 checklist items,1/4 groups25%. Regenerated handbook0.10,27 pages/25 chapters;
all pages rendered/reviewed, bookmarks/footer and the worked apartment example
verified. Saved-layout revisions, business reuse and physical construction remain.

## 2026-10-20 — donor-visible family housing status

Continued after pushed housing/reclamation milestone0d31fa7. Saved-home inspection
now reports assigned living-family size/effective capacity or no assigned household,
separately labelled last-reviewed data beside the current native bed/access/cover
report. Eight units maximum; the existing128 inspection-cell budget is unchanged.
Read-only SQL stays on the owner; no migration or housing mutation.

Fixed a permission mismatch: donors allowed to donate/reclaim could be blocked from
inspection when plot-protection permission was disabled. Routing/completion and the
inspection controller now accept those permissions while preserving donor/admin
identity, current world/authority, enabled/readiness and all bounds. No land rights
or another donor's records are granted. Shared owner authorization covers both
layout lookup and saved summary. Added controller/router boundary checks and actual
temporary-SQLite owner identity/world refusal and assigned-family reporting.

`.\mvnw.cmd -Dtest=DwellingOccupancyTest,HomeDonationPreviewTest,PaperDwellingReviewTest,PaperBuildingProtectionTest,PaperHomeDonationTest,PaperHomeReclamationTest,PaperHousingRuntimeTest clean verify`
passed40 unit +3 packaged integration checks35.414s, zero failures/errors/skips.
Expanded actual disposable Paper proof passed donation-only injected-player native
inspection plus the real two-resident/four-capacity saved report, then the existing
adoption/damage/repair/absent-world/session-restart/reclamation flow. Restored144
cells; fixture stopped/helper archived; schema110 integrity/FK checks clean.
This remains fixture actor evidence, not human-client compatibility certification.
No new broad full-suite run or actual user-server/world/database change.

Updated HOUSING_RUNTIME, status/progress and handbook0.9 (26 pages/24 chapters),
rendered/reviewed with linked contents and0.9 footers. P4 remains4/16 (25%); this
improves usability without inventing another completed construction milestone.

## 2026-10-20 — live family housing, safe legacy transition and donor repossession

User approved gradual transition: preserve existing village families/housing and
assign whole families to inspected donated/Civ-built homes as needed. Implemented
HousingCapacity as shared population/food authority. Schema110 adds exact complete
legacy bed contributions and permanent unit claims, backfilling historical109
assignments transactionally. Old raw capacity remains until an explicit transition;
ambiguous touched accounting becomes unknown, never guessed or counted twice.
Managed capacity survives full/partial enrollment scans, damage, moves and restart.
Reclamation cannot revive old legacy bed capacity, including never-assigned homes.
Families and all old history remain; no reset or actual user-server/save change.

DwellingHousing preserves suitable incumbent legacy families, refuses splitting or
displacing other incumbent families, and considers suitable vacancies for known
crowded/unhoused whole households. Confirmed damage can cause a saved vacancy move;
unknown frames preserve last-known evidence. Unrelated new villagers are not inserted
into managed families. Empty occupancy requires saved zero living members. No
physical villager movement, new household grouping, apartment editing or ordinary
automatic construction is enabled.

PaperHousingRuntime pages8 saved buildings at a time, skips absent worlds and homes
without current permission, and shares one loaded-only main-thread cell budget.
Two passes compare classified cells; edits between observations cannot grant fresh
authority. They are not an atomic world lock or complete external-editor solution.
config.yml housing-runtime defaults enabled/128 cells per tick/15 seconds; invalid
runtime settings disable only that runtime. Combined default housing tools read at
most384 cells/tick. Large scans may time out under small budgets; no incomplete
activation. /civ debug housing explains the last global review. JDBC stays queued
on the existing owner; accepted saves drain on stop.

Added /civ building reclaim, separate confirmreclaim and cancelreclaim. Ordinary
donors use a default-true reclaim permission; administrators retain their explicit
override. Expiring previews show assigned-household and legacy-family impact.
Current authority/world, consent and exact occupant sets are checked before save;
changed occupants require a new preview. Withdrawal, bed claims and departures
commit together. Keep residents, structures, contents, donor and history. Existing
footprints stay protected. Accepted saves drain across disconnect/close; committed
but unacknowledged withdrawals return their historical receipt on explicit retry.

Verification: focused Maven builds used test-owned temporary SQLite only. Added
scan/restart capacity integration; provenance/double-count checks; genuine109-to110
backfill/failure recovery; whole-family vacancy/damage moves; frozen reclamation
occupants; legacy-only reclamation; committed-but-unacknowledged retry; runtime
budget/cadence/disabled and UI permission/expiry/confirmation/draining coverage.
Updated old migration fixture additive lists and changed command-message expectations.
Final
`.\mvnw.cmd -Dtest=DwellingOccupancyTest,VillageEnrollmentRepositoryTest,DemographicRepositoryTest,HomeDonationPreviewTest,PaperHousingRuntimeTest,PaperHomeDonationTest,PaperHomeReclamationTest,PaperDwellingReviewTest,PaperBuildingProtectionTest,PopulationDebugCommandTest,BuildingProtectionSettingsTest,BuildingConsentRequestsTest,SchemaMigratorTest,BuildingRegistryTest,ForestryFounderStudyTest,ForestryPersistenceTest,ForestryProvenanceTest,GeneratedTerrainMigrationTest clean verify`
passed116 unit +3 packaged integration checks in57.283s, zero failures/errors/skips.
Final delivery rerun after removing a redundant bed-identity write passed the same
116 +3 checks in1:11, zero failures/errors/skips; Wrapper package succeeded.
No second broad full-suite run is claimed. Initial live-adapter tests exposed only
two changed message expectations; corrected them rather than changing behavior.

Actual private Paper26.2 build129 native proof passed real four-bed home donation,
whole incumbent-family adoption, old eight-capacity becoming four, roof loss zero,
repair four, changed two-pass evidence unknown, absent-world filtering, session
restart preserving residents/assignment, and donor-confirmed reclamation yielding
zero without legacy fallback/resident loss. Restored144 actual cells; fixture stopped
and helper archived; both schema110 databases integrity-ok/FK-clean. The native
flow uses trusted fixture actors/owner APIs, not a human-client click-through test.
The Paper controllers have separate injected-player checks. The final lost-commit-
acknowledgement guard has an additional real-SQLite injected-commit regression;
the native world flow was not repeated for that SQL receipt-only change. Actual user server
untouched. Human survival/editor/scale acceptance remains.

Added HOUSING_RUNTIME.md and updated canonical status/decisions/progress/checklist,
registry/occupancy/donation notes and handbook0.8. The guide is26 pages/24 chapters
with linked contents and reviewed layouts. P4 remains4/16 (25%): this advances useful
housing, but does not complete business-premises reuse or all write-boundary gates.
Next: household formation for unassigned people, editable apartment declarations,
and vacancy-first home-project activation under guarded physical construction.

## 2026-10-20 — saved whole-household dwelling occupancy

Continued from pushed home-control checkpoint f0d67b2 on
codex/phase4-construction-foundation. Added DwellingOccupancy, separate from
existing bed-group capacity: immutable complete housing reviews, current unit
status, exclusive whole-household assignments and retained departure history.
Assignments require current permission, same present world/village, current cover
policy, fresh qualified beds and room for every saved living family member.
Unknown frames cannot erase a review or occupancy. Damaged complete evidence can
record zero beds without deleting the family. Vacancy uses saved assignments,
not visible entities. Moves and reclamation withdraw/replace assignments atomically;
injected SQL failures retain the prior home/consent. Historical retries never
restore old occupancy or evict later redonations. Only a saved zero living-member
count permits empty-household release. No Paper calls or chunk loading.

Schema109 adds six empty tables and one history index transactionally. All older
rows, household_capacity, resident/family IDs, buildings, goods and budgets remain.
A package-scoped historical-schema test harness creates genuine108 fixtures using
the real ordered DDL; failed109 migration rolls back before a clean successful
retry. Updated old migration rewind/snapshot fixtures for the six additive tables.
Temporary SQLite tests cover seven occupancy/upgrade scenarios, including reopen,
exclusive units, whole-family eligibility, unknown/stale/policy/damage evidence,
move failure/retry, reclamation rollback/redonation and death/reuse.

Initial focused run exposed an old fixture still expecting schema108; corrected
that test-owned rewind and expected version. The next focused run passed35 unit
checks but failed a packaged backup file rename under Windows sandbox permissions.
No application workaround was introduced. The full host-permission run also found
four older forestry/generated-terrain migration fixtures whose additive table
lists stopped at108. Extended their expected empty-table sets through109 while
retaining exact predecessor-row comparisons. Final results are recorded below;
no unrun success is claimed.

Full `.\mvnw.cmd clean verify` completed2143 unit tests in16:37:2139 passed,
four failed only on those pre109 table-list expectations; no errors/skips. After
the test-only corrections, final
`.\mvnw.cmd -Dtest=DwellingOccupancyTest,SchemaMigratorTest,BuildingRegistryTest,BuildingConsentRequestsTest,HomeDonationPreviewTest,ForestryFounderStudyTest,ForestryPersistenceTest,ForestryProvenanceTest,GeneratedTerrainMigrationTest clean verify`
passed49 unit +3 packaged integration checks in37.179s, zero failures/errors/skips.
This includes every failed fixture and the packaged backup/simulation harness.
The whole suite was not rerun after those test-only fixture corrections.

Added DWELLING_OCCUPANCY.md; updated registry notes, status, checklist, progress
and handbook0.7. Rendered/reviewed all25 pages,23 linked chapter bookmarks and0.7
footers. This batch adds backend records, not live relocation/capacity, apartment
editing, reclamation UI or automatic construction. P4 remains4/16 (25%).
No native Paper rerun for this SQL-only batch; no user server/database/world touched.

Pending user choice: preserve existing families and legacy housing while gradually
assigning inspected donated/Civ-built homes (recommended), or immediately require
every old home to qualify. Existing enrollment rewrites bed-group capacity; simply
adding P4 capacity would be overwritten/double-counted. No silent transition is
enabled. Next is the chosen capacity/enrollment reconciliation and live assignment.

## 2026-10-20 — player-facing single-home donation permission

Continued after the covered-home checkpoint0c6abdd (pushed). Added PaperHomeDonation:
/civ building home <village UUID>, bounded scan, one-family preview, separate
clickable/explicit ownership confirmation and saved village-use permission.
Known saved IDs Tab-complete; no admin-first requirement. Selection can use exact
x/y/z, avoiding standing inside walls/floor/roof blocks. Permission defaults true
under civilizations.building.donate; donors can select without gaining exclusion
rights. building-protection.enabled gates availability. No donation sign, apartment
inference, title transfer, inventory changes, household movement or active capacity.
No reclamation UI before displacement/capacity reconciliation exists.

Four donor reviews maximum, one per actor. Inspection and donation each have128
cell reads/tick, total live housing maximum256 regardless of player count. Edited
selection, world/permission changes, expiry and disconnect invalidate unsubmitted
work. Already accepted saves drain, never a pretend cancellation. Expected failures
are explicit; failed saves retain the exact frozen owner preview for a deliberate
retry. Close cancels only unsubmitted previews while Session drains accepted work.

Final `.\mvnw.cmd -Dtest=PaperHomeDonationTest,HomeDonationPreviewTest,HomeDeclarationTest,DwellingInspectionTest,PaperDwellingReviewTest,PaperBuildingProtectionTest,BuildingConsentRequestsTest clean verify`
passed34 unit +3 packaged integration checks23.261s, zero failures/errors/skips.
Four new controller checks plus exact-coordinate selection coverage included;
existing actual temporary-SQLite consent/ownership/qualification checks also pass.
Actual private Paper26.2 build129 verified startup/default donation permission and
configured home-route completion, plus the existing real-sign owner/release/history
lifecycle. Native run did not execute the complete home-donation UI; real-region
roof/declaration proof preceded this adapter and synthetic-Player/SQL tests cover
its workflow. Human client coverage remains. Fixture restored/stopped/helper
archived; post-stop databases108/integrity-ok/FK-clean. No actual user server touched.

Updated HOME_DONATIONS.md, registry/inspection/protection notes, status/progress/
checklist and handbook0.6 alongside the new controls. Rendered/reviewed25 pages,
checked23 linked chapter bookmarks and matching0.6 footers. P4 stays4/16 (25%); the useful
building/occupancy milestone remains open. Next: persisted household occupancy,
safe reclamation/capacity reconciliation and vacancy-first automatic home projects.

## 2026-10-20 — configurable covered beds and inspected home declarations

Recorded the user's yes to covered sleeping areas. Added DwellingInspection.Policy
and BED_UNCOVERED: both real bed halves need safe observed overhead cover inside
the declared structure. One bounded cover-column index avoids per-bed world scans.
Safe glass/slabs/stairs can provide roof-only evidence; hazards/waterlogged shapes
cannot. Partial roofs do not become traversable floor geometry. The old bed/access
rules still apply when admins explicitly disable cover. Assessment retains its
applied policy, and the live read-only report identifies cover failures.
config.yml building-protection.require-covered-beds defaults true; missing old
keys get defaults, malformed values fail safely. Existing saves are untouched.

Added HomeDeclaration: explicit one-family region -> stable one-unit layout ->
current bed/access/cover assessment, never apartment inference or automatic consent.
PaperDwellingSurvey can collect a bounded raw region before a layout exists.
The SQL-owner previewSingleHomeDonation bridge checks village/world membership,
present world, permission, complete/fresh evidence and existing protections before
issuing a separate ownership confirmation. Preview creates no building; explicit
confirmation registers reviewed geometry/use permission only. No live donation
UI, occupancy/capacity activation or normal automatic block placement yet.

Focused `.\mvnw.cmd -Dtest=HomeDeclarationTest,HomeDonationPreviewTest,DwellingInspectionTest,HomeBuildingLayoutTest,PaperDwellingReviewTest,PaperBuildingProtectionTest,PopulationSessionTest clean verify`
passed51 unit +3 packaged checks in30.476s, zero failures/errors/skips. Two new pure
declaration scenarios, a temporary-SQLite preview/explicit-confirm integration,
roof/override/per-unit and strict settings checks included. Corrected an ambiguous
Java Policy import and an initially underpopulated test village; application
recognition rules unchanged. Earlier roof-only focus48+3 passed29.947s.

Final actual checksum-verified Paper26.2 build129 passed broken bed/missing roof,
native glass/stair roofs, waterlogged refusal, explicit outdoor override and raw
region -> stable one-home/four-bed declaration. Absent worlds and remote chunks
stayed incomplete/unloaded. Restored144 original cells, stopped fixture, archived
helper; post-stop schema108 integrity/FKs clean. Earlier roof-only native check
also passed. No user-server/world/save or main changes. Schema remains108.

Updated canonical decision/status/progress/checklist, inspection/config notes and
living handbook0.5. Rendered and reviewed all24 PDF pages;22 chapter links/bookmarks
and consistent0.5 footers checked. P4 stays4/16 milestones (25%); the roof decision is closed.
Next: live selection/donation confirmation, edit-aware qualification, saved
household occupancy/reclamation and vacancy reuse before automatic home demand.

## 2026-10-20 — bounded live saved-housing inspection reports

Completed the previously local plot-controls commit ec4d74e and pushed it to the
existing feature branch; main unchanged. Added PaperDwellingReview and routed
/civ building inspect <building UUID> through the live building controller.
Saved donor/current admin only, saved world only. Four total requests, one per
actor and one shared256-cell/tick advance keep Paper work bounded. SQL layout
lookup and pure path review stay on PopulationSession's worker. Results explain
per-unit bed/access issues, cap chat output and explicitly distinguish accessible
beds from roof qualification, consent, occupancy and usable housing capacity.
Missing chunks and other incomplete evidence never erase housing state. No new
donation/assignment, normal construction, database migration or server deployment.

Four new adapter tests and expanded temporary-SQLite owner checks cover global
budget/queue limits, authority/world/disconnect/expiry, close, partial evidence,
failures and retained consent. `.\mvnw.cmd -Dtest=PaperDwellingReviewTest,PaperBuildingProtectionTest,DwellingInspectionTest,HomeBuildingLayoutTest,PopulationSessionTest clean verify`
passed47 unit +3 packaged integration checks in30.231s, zero failures/errors/skips.
No full-suite or new native Paper check claimed. Schema108 remains unchanged.
Updated inspection/status/progress/checklist notes and living guide0.4 alongside
the new command. Rendered/reviewed all24 pages, checked22 linked chapter bookmarks
and consistent0.4 footers; no overflow remains. Roof eligibility remains a blocking choice before live assignment;
free third-party templates are optional and do not block the original plans.

## 2026-10-20 — live explicit off-limits building plots

Added PaperBuildingProtection and BuildingProtectionSettings, routed through
/civ building. Players select opposite corners, review bounded green particles
and explicitly confirm ownership before an exclusion is saved. Optional
[Civ Building]/protect signs request the selected preview only. Saved all-height
X/Z exclusions survive sign removal. Here reports active IDs; recorded owners or
admins separately confirm release, retaining history and other protections.
New exclusions cannot overlay another recorded donor, active foreign exclusion,
protected pen or reserved construction. Own replacement-before-release permits
safe resizing. Explicit bounds/expiry/request settings and permission defaults
are in config.yml/plugin.yml. Invalid settings disable only these controls.
All JDBC remains on PopulationSession's owner queue; no chunks are force-loaded.
No donation, household capacity, housing reclamation, money/items or normal
construction is activated. Schema remains108; real server/world saves untouched.

Final command: `.\mvnw.cmd -Dtest=PaperBuildingProtectionTest,PaperPenControlsTest,BuildingRegistryTest,BuildingConsentRequestsTest,PopulationSessionTest clean verify`.
Passed56 unit +3 packaged integration checks in33.211s, zero failures/errors/skips.
Seven new UI/settings checks, owner-queue integration and registry additions cover
explicit matching tokens, current authority/world, edited/expired selections,
retry identity, queued saves, overlap rights and safe release. Native Paper26.2
build129 probe passed installed settings/permissions/routing, real sign interaction,
non-admin confirmation,25 protected columns surviving sign removal, authority
refusal and confirmed release/history. Original sign cell restored; fixture stopped,
helper archived; databases108/integrity-ok/FK-clean. Initial delayed fixture staging
found an unloaded spawn chunk and stopped safely; first-tick staging corrected
that without force-loading. Final SQL overlap and retry-authorization guards were
added after native proof and verified by the final focused run. A further native
rerun was not executed: automatic approval review failed at account usage limit.

Updated status/progress/checklist/registry notes and BUILDING_PROTECTION.md.
Handbook0.3 adds live plot controls and command/settings glossary; rendered and
reviewed24 PDF pages with22 linked chapter bookmarks and consistent edition footers.
P4 stays4/16 milestones (25%); automatic construction remains disabled. Covered
sleeping-area eligibility was asked before assigning households; answer pending.
Latest work is local/uncommitted; last recorded pushed feature head6d1499e.
Git commit/push remains pending approval-review availability. Main is unchanged.

## 2026-10-20 — player-declared ownership and bounded consent confirmation

Recorded the user's players choice: authenticated players explicitly declare that
an old/unrecorded building belongs to them; first donation does not require admin
approval. No inferred authorship or bypass of saved donor/protection/overlap rules.
Added BuildingConsentRequests with player-bound immutable-layout previews, bounded
expiry/replacement/cancellation, explicit declaration at confirmation, present-world
and current-permission gates, exact previous-consent checks and frozen retry identity.
Registry provides read-only preflight and distinguishes expected Declined conflicts
from actual database/corruption failures. PopulationSession exposes queued data-only
preview/confirm/cancel APIs and does not report expected refusals as simulation failure.
No live sign/selection UI, occupancy/capacity, block, inventory or money changes.

Six new temporary-database request tests and one owner-queue integration check cover
non-admin declarations, wrong actors/tokens/worlds, lost permissions/expiry/cancel,
late protection, SQL rollback and later retry, stale reclamation/redonation and
bounded requests. `.\mvnw.cmd -Dtest=BuildingConsentRequestsTest,BuildingRegistryTest,PopulationSessionTest clean verify`
passed39 unit +3 packaged checks in32.970s, no failures/errors/skips. Schema108
unchanged; preceding full schema/native checkpoint retained, no redundant rerun.
Updated canonical decision/status/progress/checklist and registry notes. Updated
the living handbook to0.2 with the player declaration policy and current schema,
keeping sign/plot/assignment controls explicitly unfinished. Rendered and visually reviewed all23 final PDF pages; checked21 linked chapter
bookmarks and matching0.2 footers on every page. Fixed the builder to derive edition/
date from the source rather than a stale hardcoded footer. No user-server/save/main changes. Next: live boundary
preview and confirmed building controls, then transactional housing assignment.

## 2026-10-20 — saved building consent and real dwelling bed/access inspection

Added schema108 BuildingRegistry: immutable hashed building/layout/unit identity,
append-only donation/reclamation receipts with exact expected prior consent,
trusted authenticated-confirmation assertions, explicit old-building exclusions and
authorized once-only releases. Exact old retries cannot resurrect later-withdrawn
rights. Registration refuses saved pens, reserved projects, exclusions and other
building plots. Layout/unit-index corruption fails closed. Construction protection
includes registered plots after reclaim and clips queries to the existing bounded
footprint. Existing population, beds, funds, ownership and history are preserved;
late failure rolls layout/unit/consent inserts back together. Updated legacy schema
fixtures/snapshots for the additive five empty tables. Unknown old-building initial
donation authority was asked and recorded pending; no live sign adapter is enabled.

Added DwellingInspection with complete fresh world/layout evidence, distinct
paired-bed material/facing/support/headroom checks, explicit per-unit/shared access,
blocked private-room shortcuts and supported cardinal steps. HomeBuildingLayout
binds all8 orientations of the four-bed authored home to one stable unit. Added
PaperDwellingSurvey: main-thread, bounded incremental reads, cursor-based capture,
loaded/height/border/expiry checks and no block writes or chunk loading. Accessible
bed review is not full enclosure/habitability, consent or active village capacity;
native partial collision shapes and edit-aware live activation remain ahead.

Verification: initial affected schema/registry build59 unit +3 packaged checks
passed46.473s. Schema checkpoint `.\mvnw.cmd clean verify` passed2096 unit +3
packaged checks in16:19, no failures/errors/skips. After the final authority guards
and inspection additions, the focused command
`.\mvnw.cmd -Dtest=BuildingRegistryTest,BuildingLayoutTest,DwellingInspectionTest,HomeBuildingLayoutTest,HousingProjectPlannerTest,ConstructionLandRepositoryTest,ConstructionSiteReservationsTest,SchemaMigratorTest clean verify`
passed49 unit +3 packaged checks in26.678s. Eight registry tests and ten new
inspection/home-layout tests are included; the full checkpoint preceded those last
inspection additions and was not redundantly repeated.

Real isolated Paper26.2 build129 DwellingInspectionProbe passed: four real paired
beds/closed wooden-door access, broken bed-half detection, incomplete absent worlds
and unloaded targets with no force-load. Restored144 original cells, stopped the
private server and archived its helper. Read-only fixture DB audit:108, integrity
ok, FK-clean, building tables empty. Baseline host/perflib/flat-generator messages
remained; explicit native PASS and orderly shutdown confirmed the result.

Updated BUILDING_LAYOUTS, BUILDING_REGISTRY, DWELLING_INSPECTION, current status,
progress/checklist and the open ownership question. The living handbook's planned
controls remain accurate; no new player command or sign is documented as live.
No user-server/world/save/main changes. P4 remains25%,4/16 complete items.
Next: resolve first-donation authority, then editable confirmation/consent controls
and transactional household assignments before enabling normal construction.

## 2026-10-20 — explicit building and dwelling-unit geometry

Added immutable plain-Java BuildingLayout and bounded versioned snapshots with
stable building/village/world/unit identities and semantic fingerprints. Separate
plot/structure/shared/private geometry supports stacked apartments and irregular
multi-room single-family declarations. Reject overlapping regions, duplicate unit
identity, invalid entrances, incomplete/reused bed pairs and oversized/overflowing
bounds. Geometry grants no ownership, donation consent or usable housing capacity.

Added nine focused tests. Windows Wrapper command:
`.\mvnw.cmd -Dtest=BuildingLayoutTest,HousingProjectPlannerTest,ConstructionTemplateTest,ConstructionTemplateSnapshotTest clean verify`
passed18 unit +3 packaged integration checks in21.721s, no failures/errors/skips.
No Paper API changes/native run; no schema migration (still107), live server,
world/save or main changes. Added BUILDING_LAYOUTS.md and updated status/progress/
checklist; the handbook's planned-controls labels remain accurate. P4 remains25%,
4/16 complete items. Next: current-world inspection and transactional permission/
occupancy records before exposing donation or apartment controls.

## 2026-10-20 — first living player and server handbook

Marked PDF artifacts binary in .gitattributes to prevent Windows line-ending
conversion; verified the committed PDF bytes match the visually reviewed output.

Created a23-page PDF with overview/TL DR, linked chapter list,21 feature/reference
chapters, player/admin/protection/recovery command glossaries, terms, a connected
village example, permissions, tuning, troubleshooting and safe update/backup
procedures. Kept editable Markdown and a lightweight ReportLab builder in Git so
future feature batches can update the guide rather than rewrite it. Current,
development-only and planned behavior are explicitly separated; proposed donation,
reclamation/plot and apartment controls are not working instructions. Recorded the
user's no-rent/no-landlord village-owned apartment direction without adding private
purchase accounting or changing gameplay.

Used bundled Python/ReportLab/Poppler, rendered and visually inspected all23 pages,
then re-rendered the completed recovery-command section; pixel comparison found
only page21 changed and it was inspected at full render size. PDF checks confirmed
23 pages,21 correctly targeted chapter bookmarks and command references. Source
syntax/permissions were reviewed against handlers, plugin.yml and current status.
No Maven build was needed for this documentation-only batch. No schema, world,
server, database or gameplay code changes. P4 remains25%,4/16 checklist items.

## 2026-10-20 — record civic catalogue scope and apartment design discussion

Recorded explicit user agreement to P4 civic/infrastructure extras alongside the
core housing/industry catalogue. Recorded the apartment question and proposed
building/unit/shared-space distinction without claiming implementation or silently
adding rent, landlords, religion, government or military mechanics. Updated current
status/progress/checklist intent; no completed gate or percentage change. Docs-only
review via git diff --check; no code, schema, world or database changes or build.

## 2026-10-20 — record donation-only building use and reclamation direction

Recorded the user's player-building opt-in rule, withdrawal of donated housing
with corresponding village capacity loss, and an additional explicit exclusion
safeguard. Replaced the pending automatic-empty-home reuse proposal; no protection
sign is not consent. Editable plot selection, typed donation use and donor/admin
reclamation are interface proposals, not implemented functionality or locked new
civic mechanics. No code, migration, world or database changes; documentation-only
review with git diff --check, no redundant build. P4 remains25%,4/16 checklist items.

## 2026-10-20 — migrant labor receipts cannot block food accounting

Final review found a resident moving between villages could be selected for a
same-numbered period already present in their saved public work history. SQLite
correctly refused duplicate labor, but the late refusal would also roll back the
destination's food period. Eligibility now excludes that saved receipt before
selection. A two-village same-world migration test verifies the old history stays
intact, no duplicate work is awarded and destination food accounting continues.
Focused clean verify27 unit +3 packaged checks passed39.336s with zero failures,
errors or skips. Initial second-village fixture reused workplace coordinates;
separated its sites and ensured cleanup before the passing run. No migration
change; schema107 remains. Existing-home reuse choice is pending, not implemented.

## 2026-10-20 — earned public building labor, stage escrow and paid native acceptance

Schema107 adds public resident/period work receipts and frozen stage material
escrow. Actual earned current food periods book only eligible unemployed non-owner
adults; farm employees, active/just-completed students and legacy all-adult farm
labor cannot double as builders. Fed population, earlier delivered full material
cost, available world and current saved protection gate work. One resident/period
cannot be spent twice, worker caps apply across local projects, and later stages
wait for prior physical acknowledgements. Food/work/escrow roll back together.

Public economic journal preparation now requires full receipted effort and backed
escrow plus fresh exact-project/site proof. It grants no ordinary writer or home
capacity. Private staff diversion and automatic project/order coordination remain
future work. Existing construction settings are loaded at startup without changes
to canonical rules. Historical migration fixtures exclude/verify the additive empty
tables; version106 failure/retry preserves paid cargo and balances.

Focused clean verify86 unit +3 packaged passed59.632s; affected migration/forest/
study batch78+3 passed51.257s, all zero failures/errors/skips. Actual disposable
Paper paid a supplier for two stone units out of four explicitly seeded test units,
earned delivery and five work units over three real food periods, then wrote five
native blocks. Exact replay, DB-reopen crash window, changed-block preservation and
unloaded-target no-force-load passed. Original five cells restored; both schema107
DBs integrity/FK-clean, server stopped/helper archived. No real server/database/main
changes; P4 remains25%,4/16 items. Hosted85419c6 passed both OS jobs; later candidate
results must be checked separately.

## 2026-10-20 — settle construction cargo through the existing owner loop

PopulationSession settles up to16 ready prepaid construction orders atomically
after existing simulation clocks advance. No inferred worlds, extra project clock,
new spending or ordinary block writer. Early/absent cargo cannot head-block ready
orders; a late receipt update rolls back the entire batch. Added permission-checked
/civ debug construction without main-thread JDBC. Focused Wrapper clean verify46
unit +3 packaged checks passed36.068s, zero failures/errors/skips. Initial owner
fixture used a different priority seed; corrected it to the saved seed rather than
weakening the application's explicit-transition guard. Actual disposable Paper
passed installed supply defaults, diagnostic routing/permissions and native
template validation, zero construction writes; schema106 integrity/FKs healthy,
server stopped/helper archived. No migration change or human server/main changes.

## 2026-10-20 — real construction supplier purchases and earned delivery

Added schema106 supplier receipts, protected actual private/public payment and
stock transfer, fixed funding identity, exact replay, bounded frozen-need orders
and project-owned cargo. Uses existing earned purchaser clocks and shared local
hauling, including ordinary industry/storage upgrades; no extra transport budget
or project clock. Missing/short public food assessment and stale/mismatched site
evidence refuse spending. New editable construction.yml supply prices preserve
old config defaults. Paid assets cannot release their site or double-fund an owned
stock reservation. Cancellation, stage consumption and automatic orders remain
unfinished; normal world placement remains disabled.

Focused Windows Wrapper clean verify passed82 unit +3 packaged checks in64s,
zero failures/errors/skips. Temporary SQLite tests cover both payers, actual owner
runtime delivery, restart, no duplication, limits, shared hauling, late-trigger
rollback and version105 migration failure/retry preserving existing data. Initial
fixture compile error and a wrong pre-existing treasury expectation were corrected
before the passing run. Prior checkpoints b204a22 and288801d passed full hosted
Windows/Linux verification. No real server/database/main changes. P4 stays25%.

## 2026-10-19 — let hosted Windows verification finish between backups

Feature pushes repeatedly cancelled the longer Windows migration job before it
could finish, while Linux completed. Changed workflow concurrency to retain an
already-running verification across newer pushes. The per-ref concurrency group,
read-only permissions, pinned actions, both OS jobs and full clean verify command
are unchanged. Reviewed the one-boolean YAML change; no redundant code tests.
Current hosted candidate results remain pending, not reported as passed.

## 2026-10-19 — whole-household vacancy-first proposals and verified bed capacity

Added a bounded deterministic housing planner that reuses verified permitted
village-owned vacancies before new whole-household proposals. Existing projects
prevent duplicates; unknown vacancy surveys and absent worlds pause new demand.
No family is split to fit a too-small template, and no player/private/wrong-world
home can be appropriated. The editable housing.maximum-new-proposals cap defaults
to1 for old files without rewriting them. This planner changes no live household,
occupancy, money, site or world; the real data/coordinator adapter remains future.

Home capacity now counts distinct complete native bed pairs with matching foot
anchors/facing. Missing halves, duplicate/misleading anchors, occupied beds and
unanchored extra beds cannot advertise capacity. Every native orientation is
validated before a HOME template is offered. Focused Wrapper clean verify55 unit
+3 packaged checks passed32.984s, zero failures/errors/skips. An initially malformed
test was already rejected by the old template constructor; corrected the fixture
and reran the affected build. Actual disposable Paper passed all native rotations,
mirrors, installed settings, catalogue/router/permissions and invalid edit checks.
No block writes; server stopped, helper archived, schema105 DB integrity/FKs clean.
No migration change or human server/main modification. P4 stays25%,4/16 items.


## 2026-10-19 — return unused construction stock without loss or duplication

Schema105 adds saved material return rows with original-reservation foreign keys,
exact operation identity and commodity before/after quantities. Unstarted projects
return all goods atomically to their source, or wait intact for unreserved storage
space. Cash does not change. Late failures roll back stock and receipts together;
restart retries cannot return twice. Any prepared physical intent blocks this
shortcut. Available stock is separate from immutable initial transfer history.
An unstarted funded site may release only after its complete return is recorded.
No automatic cancellation, disposal, payment/refund rule or native write is added.

Focused Wrapper clean verify80 unit +3 packaged integration checks passed44.340s,
zero failures/errors/skips. Added exact replay/restart, full-store wait, rollback,
started-work refusal and version104 preservation/recovery cases. Historical
migration fixtures separately assert the additive empty returns table. Human
server/world/database/main untouched; P4 stays25%,4/16 whole checklist items.


## 2026-10-19 — saved construction sites and full home/JVM recovery

Schema104 adds permanent world/draft-linked scheduler site reservations. Overlaps
are refused, empty protected player plots stay excluded, reservations appear in
read-only previews, and only an unused proposal can release its footprint.
Funded/started sites retain history until explicit future returns/ownership handling.
New material reservations now require the exact draft's active site and fresh
world/site evidence, with saved player protections rechecked in the same SQL
transaction. A receipt replay is not fresh physical authority or permission.

Focused Wrapper clean verify passed77 unit +3 packaged checks,44.448s; exact-draft
proof follow-up passed51 unit +3 packaged checks,33.368s. All zero failures/errors/
skips. Historical migration snapshots retain old rows and assert the new empty
reservation table separately. Schema103 collision/rollback recovery is covered.

Actual marked disposable Paper built a120-block/four-bed home across two JVM
runs. First run stopped after60 native writes/59 acknowledgements; second recovered
the pending after-state once, retained the site, completed all120 states and restored
all121 original cells. Both fixture DBs passed integrity/FK checks at104; all120
journal entries are applied. Server stopped, helper archived. This is physical
recovery evidence, not paid construction or housing-capacity activation.

The first restart attempt correctly refused unloaded chunks. Corrected test-only
staging reopens at most four already-generated saved-footprint chunks; the writer
and normal simulation still never load chunks. Failed evidence retained, helper
archived. Human server/main unchanged. P4 stays25%,4/16 complete checklist items;
procurement, booked labor, real completion/occupancy and growth remain unfinished.

## 2026-10-19 — approved sheep/wool supplier extension

Approved sheep support now shares existing natural-animal recognition, saved
ownership/protected pens, real startup funding, paid feed/work/storage, breeding
reserves, saved age/birth/presence and deliberate-death accounting. Wool is an
abstract colour-neutral commodity. Saved village sheep cannot duplicate it through
player or dispenser shearing; unclaimed/player sheep remain vanilla. The milk/egg
retail menu excludes sheep rather than treating them as chickens. Actual construction
procurement/demand and home activation are still unfinished; no automatic houses yet.

Migration103 transactionally expands four species CHECK constraints, preserving
rows, keys, indexes, triggers and historical default receipt fingerprints. Actual
version102 populated cow history survives the upgrade and an injected late failure
rolls back safely with foreign-key enforcement restored. No existing herd reset.
Old economy.yml files lacking sheep use validated defaults without being rewritten;
existing missing whole livestock/runtime/adoption sections retain their opt-outs.

Focused Windows Wrapper clean verify passed73 unit +3 packaged integration checks,
zero failures/errors/skips,42.991s. Earlier broader livestock run passed351/353;
two integration issues were fixed and affected checks rerun. The corrected sandbox
run passed unit tests but hit a backup-file move restriction; normal-environment
verification above passed. Actual isolated Paper SHEEP runtime passed paid feed,
wool/births, saved cash/goods conservation, owner restart, absence/downtime and
capture-fence pauses, plus native player/dispenser shear guards. Fixture stopped;
both private DBs passed integrity/FKs at103. This is not human playtesting or native
sheep newborn/slaughter acceptance. P4 remains25%,4/16 full checklist items.

## 2026-10-19 — P4 home, land screen, labor proposals and material reservations

Added an original four-bed home and native bed palette; actual disposable Paper
verified all rotations/mirrors, catalogue/routing and invalid-edit rejection.
No block writes in this probe; clean shutdown and schema102 integrity/FK checks.
Added conservative buffered land previews, configurable temporary labor proposals
and atomic private-business stock reservations. Reserved units remain in an onsite
stockpile; no cash/materials are invented. Stage consumption, cancellation returns,
public procurement and actual labor booking are subsequent work. Schema102 adds
reservation receipts/items only. Prepared authored logs get forestry exclusions
atomically, including rollback on exclusion failure.

Focused Wrapper clean verify68 unit +3 packaged integration tests passed38.279s,
zero failures/errors/skips. Fixed three CI-discovered forestry snapshot fixtures
without weakening row-preservation assertions; hosted P4 rerun remains required.
Recorded user approvals for temporary labor, village-owned housing and basic sheep.
P4 stays25%,4/16 items. Human server/main remain untouched.

## 2026-10-19 — P4 ordered writes, native recovery and editable budget foundation

Added bounded deterministic infill search beside observed roads, saved before/after
block intents and a development-only native step writer. Schema101 appends journal
storage; schema100 drafts remain unchanged. Exact retries preserve identity, prior
blocks must be acknowledged, stale/changed geometry/materials fail closed, conflicts
stay recorded and future work waits on unavailable world/chunk/authority/entity
safety. The normal plugin has no placement command or automatic writer activation.

Actual marked Paper fixture placed five blocks, retried exactly, reopened SQL after
write/before ack, retained a changed block as a conflict, and left a remote target
unloaded. Originals restored, test-only already-loaded chunk tickets released,
helper archived/server stopped. Both private DBs healthy/FK-clean at schema101.
An earlier fixture unloaded before writing; its cleanup failed and only that
verified disposable process was terminated. Corrected helper preserves its target
chunks without changing the production no-force-load rule. No human server touched.

User approved protected configurable funding, automatic nearby land growth and
bounded recognition of unmarked clear land. Added commented construction.yml and
strict spending-quote policy: food PLUS upkeep/commitments protected, real purchases
only, owned materials not charged twice, no new wages or wallets. Saved confirmed
fenced plots now feed read-only previews through the SQL owner queue, including
empty plots and released-claim history. These are not general claim-plugin hooks
or a completed automatic land recognizer. Updated additive historical test fixtures.

Focused Wrapper clean verify58 unit +3 packaged integrations passed37.359s,
zero failures/errors/skips. Native placement evidence predates only the additive
budget/protection-preview follow-up; those follow-ups have focused unit/build checks.
P4 remains4/16 (25%); demand/funding/work/site/activation/physical lifecycle gates
are still open. Main remains unchanged. Feature backup/hosted results follow.


## 2026-10-19 — Phase 4 authored-plan foundation

Started `codex/phase4-construction-foundation` from P3 085b351 after verifying
its hosted Windows/Linux green. Added immutable versioned construction domain,
bounded strict YAML reader, two original starter resources, native orientation
adapter, read-only admin catalogue/site scan and frozen proposal repository.
Migration 100 adds only `construction_drafts` with permanent world/village FK;
no reset, land permission, spending, block editing or building activation.

Focused Wrapper clean verify passed 28 unit +3 packaged integration checks,
zero failures/errors/skips, 21.630s at 17:37:56 ET. Initial sandbox-restricted
backup integration failed on a Windows temporary-file move; normal-environment
rerun passed. Native disposable Paper probe passed all eight door/geometry
orientations, pillar rotation, installed resources, router permissions/completion
and invalid edited-material rejection. Fixture file restored; no block/entity
changes by helper. Human server/main untouched. Added construction guide and
P4 milestone checklist: 4/16, 25%, 1/4 complete groups. Human playtest deferred.


## 2026-10-19 - Phase3 prototype gate complete

Completed63 checklist entries across4/4 groups (100% of the initial P3 scope).
Full Windows Wrapper clean verify passed1997 unit +3 packaged integrations, zero
failures/errors/skips,15:11,16:25:34ET. Final review closed off-world estate spending
in local-only purchase mode and added a real automatic-review/eligible-funding
regression. Thirty-six affected unit cases pass across focused independent-source
runs; final repository rebuild4 ownership +3 integrations passed22.121s at16:26:23.
This final accounting follow-up is separate from the preceding full run; no whole
suite was repeated. Current target JAR includes the follow-up.

Actual mixed8-business/40-pulse Paper catalogue ran with all8 workplace chunks
unloaded; real production/trade/craft upgrade/bakery inputs, capacity/cash and
absence/restart passed. Ordinary pinned-editor startup/fresh generation/edit/undo
and actual process restart passed. All24 terrain-history rows retained, prior
authority revoked, no chunk loads in the restart helper. Final databases healthy,
schema99 unchanged, servers stopped/helpers archived/ports free; five test blocks
restored/ticket released. No human server or main edits.

Updated README/current progress/playtest scope and acceptance/portfolio/editor
references. Human survival balance/plugin-mix checklist stays explicitly pending.
Physical construction/excavation/felling remains Phase4; regional shipping Phase5.
No P3 decision needed. Feature backup and final hosted state follow below.

Delivery branch: `codex/phase3-company-expansion`; commits ac9aac4 and9fedbc0 were
pushed, and this final accounting/completion batch is backed up on the same branch.
Earlier ac9aac4 Windows/Linux checks passed; 9fedbc0 Linux passed and Windows was
still running at the last observation. Hosted checks rerun on the final commit;
they are not claimed green before results arrive. Final local/native gates above
are complete. Main remainsc12f7f6 and is neither merged nor pushed.
Artifact SHA256:399effd646de21b5a684837efcb8cf5bde3f9a8075578be835efd260268088be.

## 2026-10-19 - P3 completion candidate: mixed runtime and editor bootstrap

Pinned WorldEdit now registers at ordinary startup with fail-closed command/tool/
disable scope. Every bootstrap quarantines earlier provenance before a new healthy
epoch; this covers unobserved onLoad queued deletions without blessing old terrain.
PopulationSession bootstrap regression checks revoke old certificates and permit
new-only capture. No migration/reset/asset deletion. Exact limits documented.

New ten-year actual-owner scenario passed: real shortages/extraction, ordinary
elder death, qualified rich remote acquisition, real funds, absence/restart/history.
Fixture corrections kept its saved seed consistent and gave the rare buyer enough
actual declared capital to cover valued goods. Historical food deficits remain
evidence even when later population demand changes. Targeted1 unit +3 integration
verify passed41.039s. Full final regression is running, not claimed passed yet.

Actual mixed Paper8-business/40-pulse acceptance passed real production/paid trade/
craft storage installation/bakery grain use, capacity/cash, absence/restart and
all8 workplace chunks remaining unloaded. Average323ms/max1203ms owner wait wall
time under concurrent Maven load; default30-second cadence unchanged/no scale claim.
Separate WorldEdit/Paper ordinary bootstrap/fresh generation/raw/set/removal/undo/
failure checks passed. Five test blocks restored, ticket released,4 finalDBs
integrity/FK/schema99 good, helpers archived, servers stopped/ports free.
Human server/main untouched. Previousac9aac4 Windows/Linux CI confirmed green.
Phase acceptance/status checklist waits for the final full build.

## 2026-10-19 - real paid branch ownership and initial craft catalogue

Approved same-world cross-village purchases now use qualified buyers and actual
owned business/eligible estate cash, preserving local employees, stock, accounts,
shipments and founder history. Runtime/staffing/competition allow valid remote
owners; qualified family succession preserves the branch. Missing old YAML key
remains local-only; new defaults enable purchases. No migration or regional shipping.
Added3 temporary SQLite cases covering automatic payment, world/scope refusal,
failure rollback, replay and inheritance. General craft defaults add alternate
stone fittings alongside timber/stone, using real business storage customers;
no invented household purchases before Phase4 construction.

Affected178 unit checks pass across focused run/corrections; successful reruns
also passed3 packaged integrations (ownership15.358s, defaults16.900s). Corrected
fixture treasury setup and legacy catalogue expectation; no failed case hidden.
Previous3b29bd3 hosted Windows/Linux success confirmed. New batch full CI pending.
P3 initial craft gate complete,88.7% partial/2 complete groups; remaining acceptance
continues. Human server and main untouched; schema99 unchanged.

## 2026-10-19 - fix accepted shutdown-drain regression

Hosted Linux caught one existing PopulationSession shutdown-drain case after live
scoped wiring; local reproduction confirmed23 passed/1 error. Owner observation/
demand reads no longer confuse public shutdown readiness with accepted queue work.
Real capture gaps still refuse evidence; pending mutations still block spending
and apply sticky exclusions in queue order. Final clean verify30 unit +3 packaged
checks passed26.963s; no whole-suite repetition or new native run. Hosted builds
remain pending on the corrected commit, not falsely green.

Pinned WorldEdit bytecode audit also confirmed queued chunk deletion in onLoad,
outside ordinary EditSession hooks. Kept normal editor presence fenced; no chunk
deletion was run. Asked the new cross-village purchase rule needed for rare firms:
same-world qualified paid acquisitions with separate local assets, shipping later.
No answer assumed or gameplay implemented for that question. No migration, human
server or main changes. P3 stays88.1% partial,2/4 groups.

## 2026-10-19 - wire live scoped surveys and funded industry startup

Connected SQL-owner chunk preflight to the ordinary bounded ore observer and
current-capture guards to surveys/demand/paid startups. Forestry admission checks
whole current frames, and prospective-owner study resolves actual cached evidence
instead of its identity-only timestamp-zero placeholder. Unknown evidence waits;
global historical trust stays false. Focused clean verify47 unit +3 packaged checks
passed44.680s. Peer review corrected only diagnostic wording; refreshed package
passed17.128s, no repeated tests.

Fresh no-editor Paper26.2-129 scheduled observer passed actual new generation,
old-chunk refusal, one excluded coal and one timestamped unclaimed opportunity.
No business, reserves or cash created. Private DB has18 certificates (new neighbors
also generate events), main34; both healthy current epochs/integrity/FKs, private
global ore history still untrusted. Four AIR fixtures restored, tickets released,
helper archived/server stopped/worlds retained. No migration, human server change
or WorldEdit presence exemption. P3 stays88.1%; editor lifecycle remains next.

## 2026-10-19 - filter scoped opportunities and prevent retroactive trust

Current mineral/quarry views filter exact generated coordinates under sticky global
gaps, retain older history and derive eligible quarry capture time. Positive admission
requires complete proof; empty frames only clear candidates. Generation timestamps
must precede resource observations; saved resource production retains its original
opening cutoff, so later certificates cannot silently authorize older sources.
Focused clean verify37 unit +3 packaged checks passed28.796s; one additional saved-
source chronology regression passed10.472s. Three new cases; no whole suite repeat,
new migration/native run or live guard exemption. P3 stays88.1%; ordinary adapter/
founder wiring and optional editor lifecycle remain next. Human server untouched.

## 2026-10-19 - apply complete generated coverage to resource opening/work

Added complete saved mineral/forest coverage reads with bounded coordinates and
permanent-world checks. Paid resource opening and subsequent work may use current
healthy generated-terrain proof without clearing sticky global trust, exclusions or
recovery requirements elsewhere. Partial coverage, placed ore and epoch gaps remain
blocked; replay cannot refill reserves or treasury. Three new actual paid-start/work
cases passed. Initial37 checks exposed one outdated schema91 snapshot; hosted Linux
also found schema93's equivalent. Corrected both predecessor snapshots and explicitly
asserted the new table empty. Final clean verify7 unit +3 packaged checks passed
21.777s; corrected founder-study class3 checks passed12.099s. The other resource/
recovery checks passed in the initial run and were not repeated. No migration,
ordinary survey/editor guard exemption, human server change or new milestone credit.

## 2026-10-19 - capture real freshly generated terrain

Added bounded main-thread positive-new-chunk observation, immutable owner-queue
handoff, per-kind readiness/revision/active-world checks and debug counts. Ordinary
loads, unavailable capture and overflow stay unknown, with no force loads or backfill.
Focused clean verify9 unit +3 packaged checks passed21.450s. Fresh no-editor
Paper26.2-129 proved real new generation, old-load rejection, clean session reopen
and gap revocation without deleting evidence. Main/private DBs each retained two
certificates and no people/firms; integrity/FKs clean. Helper archived, server stopped,
worlds retained; human server untouched. No migration or relaxed discovery guard.
P3 stays88.1%; coordinate eligibility and optional editor lifecycle remain next.

## 2026-10-19 - verify optional WorldEdit mutation adapter

Added provided-only WorldEdit Bukkit7.4.4 API, soft dependency, explicit-registration
loaded-world write adapter and guarded native helper. Actual Bukkit before/after
states confirm resource mutations before owner-queue saves; return values alone
cannot certify writes. Unchanged repeats are idempotent; raw edits and undo are
captured; ambiguous/off-thread/detached writes fence both histories. No main guard
exemption or ordinary registration yet; lifecycle and generated-terrain wiring follow.

Focused clean verify8 unit +3 packaged checks passed21.105s; live-read tightening
packaged17.110s. Fresh disposable Paper26.2-129 + checksum-verified WorldEdit7.4.4
passed real edit/raw removal/placement/undo/repeat and off-thread fencing. Removing
one coal block retired only its10-unit share; other10 retained, zero bonus goods/
certificates, expected funds preserved. Main/private DB integrity/FKs clean. Restored
all five AIR fixtures, released ticket, archived helper and stopped server. Existing
test save, human server and main unchanged. No milestone credit: P3 stays88.1%.

## Latest verified batch - scoped terrain evidence and external-write barrier

Recorded the approved edited-world rule. Schema99 adds one initially empty
`generated_resource_chunks` table; no historical terrain is backfilled. Certificates
bind exact world/chunk/resource kind to current healthy capture epochs. Negative
coordinates use floor division; replay retains the first timestamp, and a later gap
revokes coverage without permitting the same old chunk to certify itself anew.
Global history, placement exclusions, resource balances and recovery rights stay intact.
This backend is not yet connected to generation events or automatic discovery.

External adapters now have a bounded pre-write reservation that holds ore/forest
readiness until their accepted owner-queue saves finish. Explicit unchanged outcomes
write nothing; exceptions, unconfirmed outcomes, failed handoffs and interrupted
shutdown remain fenced. Reservation and shutdown ordering are synchronized. No Paper
object crosses the owner queue; no current native listener/guard is relaxed.

Focused first checks passed49 units,25.847s. Final Wrapper clean verify/package
passed31 units +2 packaged integrations, zero failures/errors/skips,27.072s.
Eight new cases cover generated-terrain persistence/epoch gaps/rollback, complete
schema98 preservation/failure/retry and external-write outcomes/actual session
persistence/shutdown. Existing migration/recovery/startup regressions pass.
No repeated complete local suite; hosted checks follow feature push.

Actual isolated Paper26.2-129 upgraded98->99 and passed the ordinary paid alternate
craft startup/production/customer/replay/absence check. All173 predecessor tables
preserved:168 exact, five routine clock/capture tables changed, no resident aging.
The new table remains empty; no terrain was certified. Integrity/FKs clean, original
one-block fixture restored, ticket released, server stopped, original port restored,
helper archived and backups/audit retained. Human server/main untouched.

P3 stays88.1% partial,2/4 whole groups: this is verified foundation, not certified
WorldEdit support. Next is the optional actual edit hook and complete lifecycle/
generation wiring; older unsupported editor presence still fences discovery.
No new design answer required.

## Latest - optional editor provenance decision pending

WorldEdit integration research identified a necessary trust-scope decision before
relaxing the existing conservative presence guard. The user has been asked whether
previously unverified edited terrain stays blocked while newly generated terrain
under uninterrupted capture can become eligible. Existing source-by-source admin
recovery is unchanged. No answer is assumed; no editor dependency, installation,
new certificate or guard exemption has been added. The pending question and primary
API references are recorded in DECISIONS_AND_OPEN_QUESTIONS.md. This does not block
playing the current supported non-editor setup. P3 remains88.1% partial,2/4 groups.

## Latest verified batch - geography and family history change industry outcomes

Two seeded ten-year scenarios now run actual monthly demographic aging/mortality,
bounded industry work, ordinary family-first succession, qualification-based hiring,
real buyer review, saved sale deadlines and retained closure. The same seed42,
people and geology diverge only in saved heir mining experience: a qualified heir
keeps the mine; an unqualified family with no actual funded qualified buyer cannot,
and its unsold business closes. The elderly fixture uses unmodified prototype
mortality; no death is forced. Sale deadline acceleration is fixture-only.

Actual mine work earns goods before the scenario. Close/reopen halfway retains
people/clock, resources, cash and work receipts without offline accrual. Immutable
founder, one grant, ownership/closure/job history, retained store/source and exact
99500 total village/business/estate cash remain. Closed work cannot consume its
reserves or assets. Foreign keys clean. No application, schema or human save changes.

Final focused Wrapper `-Dtest=IndustryHistoryScenarioTest clean verify` passed2
unit scenarios +2 packaged integrations, zero failures/errors/skips,50.234s.
Initial scenario run also passed2 cases,38.938s; a final assertion was narrowed to
the closed firm's actual assets so other businesses may legitimately keep working.
Combined with the earlier three geography counterfactuals, this verifies different
geography/history producing different industry outcomes without universal lists.
Final group advances2/5->3/5; equal group progress88.1% partial,2/4 whole groups.
The broad multi-year shortage/exceptional-firm and integrated mixed-catalog native/
profiling gates still remain open. Useful craft breadth/editor compatibility remain.
No new gameplay decision required. Main and human server untouched.

## Latest verified batch - one craftsman can use multiple editable recipes

Removed the one-recipe restriction for the general craftsman (32 total runtime
recipes remain the cap). Stable recipe IDs break ties. Current owned ingredients
and storage choose productive work first; paid incoming ingredients guide future
orders without becoming production inputs. A bounded complete warehouse review
can select an affordable alternate; otherwise the supported baseline establishes
normal input demand. Only saved supported products participate: old store catalogues
are never silently expanded. Other industry recipe limits remain unchanged.

First opening now checks actual fitting customers and each fitting recipe's paid
starter feasibility, rather than giving up on an unavailable first recipe. No new
occupation, free material, guaranteed sales, household wallet, default price or
construction permission. The default product remains useful storage fittings;
broader furniture/customer coverage remains open, so P3 stays83.1% partial.

Final focused Wrapper clean verify/package passed14 units +2 packaged integrations,
zero failures/errors/skips,26.998s. Earlier focused regression passed32 units +2
integrations,31.362s (craft training, configuration and bakery/livestock chain),
plus the initial13 craft/config cases,22.319s. Three new cases cover alternative
paid opening/production/customer/conservation/replay, real affordable warehouse
choice, pending-goods protection, retained catalogues and strict YAML parsing.
No whole local suite repeated. Schema remains98.

Final isolated Paper26.2-129 ordinary SQL-owner/Paper startup passed: without timber,
a configured alternate bought three real stone for900, manufactured one fitting
and sold it for2000 to the actual crowded warehouse. Exactly one500 startup fee;
20400 village cash +9100 business cash =29500 retained. Replay/absence, integrity/
FKs passed. One original AIR block restored, fixture ticket released, server stopped.
Main disposable DB173 tables:168 exact, only five normal clock/capture tables
changed; no resident aging or asset changes. Port restored/helper archived,
backups/audit retained. Human server and main untouched. No decision required.

## Latest verified batch - geography and finite-resource longer scenarios

Three temporary SQLite counterfactuals use the same seed, people, customers and
budget, changing only current geology: coal opens a mine, stone a quarry, neither
opens neither. Eighty real industry periods exercise paid warehouse/processing
flows, finite drawdown, a twenty-period absent-world interval and an offline
close/reopen. No extra startup fee, downtime backlog, revisit refill or invented
consumer/cash. The quarry exhausts; coal byproduct storage/customer constraints
can legitimately retain some reserve. Rediscovery preserves balances and goods.

Focused Wrapper `-Dtest=ResourceGeographyScenarioTest,ResourceInvestmentTest clean
verify` passed8 unit cases +2 packaged integrations, zero failures/errors/skips,
47.978s. An initial scenario assumption that every mine must fully exhaust was
incorrect: finite customer/storage limits legitimately stop extraction. Corrected
the fixture expectation rather than removing goods or changing the application.
No application, schema or human-server change. These are industry periods, not
population years; multi-year demographic/history and full mixed-catalog acceptance
remain open. P3 stays83.1% partial,2/4 whole groups. No new decision needed.

## Latest verified batch - livestock allocation completes basic competition

Separated a read-only qualified herd/budget preparation from ownership/grant
writes. At the existing actual Paper adoption handoff, a freshly delivered eligible
herd competes with current resource, marker and water proposals. No speculative
animals/ownership are saved or queued. Normal adoption revalidates every original
origin/protection/range/nearest-village/qualification/capture/funding condition
before claiming animals. Ownership, paid grant/store/job/herd and selection history/
cursor share one transaction; failure restores all, retry opens once.

Preserved the already-approved first-herd eligibility without an established buyer:
its funded value is zero, behind every positive funded alternative. No fake demand,
consumer, profit or new budget. Existing species/grant limits and protected food
funds remain. Other startup adapters retain their actual customer requirements.
Available known observations are compared; unseen future opportunities cannot be.
Existing farm-food scheduling and no-poaching/one-worker-job protections remain.

Focused Wrapper clean verify/package passed48 units +2 packaged integrations,
zero failures/errors/skips,32.505s. Three new temporary SQLite cases prove read-only
preparation/zero-demand compatibility, real funded-priority deferral without claims,
and complete late-audit rollback/retry. Existing adoption, protection, source/
marker/fishery selection and session regressions pass. No schema migration (98).
Refreshed the final JAR after diagnostic wording only: package with tests skipped
passed17.180s; the48+2 checks above were not unnecessarily repeated.

Actual isolated Paper26.2-129 passed ordinary repeated natural-animal adoption:
an actual fence blocked adoption; after removal only the two unnamed eligible cows
joined, while the named third stayed protected. One real2500 grant/debit (500 fee,
2000 business cash), one actual worker, durable exact ownership/herd and one zero-
demand selection record verified after private close/reopen. Three exact fixture
bodies removed,324 original block states verified restored, test ticket released.
Server stopped. Main disposable DB173 tables,165 exact, eight normal clock/capture/
resident tables changed with one normal age month and exact retained identities/
statuses/ages. Integrity/FKs/config/capture and private assets/audit clean. Original
port restored, helper archived, backups/audit retained. Human server/main untouched.

Basic lifecycle/competition15/15 and bounded viable opportunity selection now pass.
Core4/4, resources36/39, final specialization/acceptance2/5: equal group weighting
83.1% partial P3,2/4 whole groups. This is not full Phase3 or human/long-run acceptance.
Remaining resources: useful craft breadth, supported bulk-editor capture, broad
catalog acceptance; remaining final entries: contrasting geography/history,
seeded multi-year scenarios and integrated native profiling/acceptance.
Latest full hosted checks follow push. No new design decision required.

## Latest verified batch - fisheries join shared startup allocation

A qualified funded water proposal now competes with resource and marker startups.
Its stable selection key identifies the observed water footprint; actual existing
bank-derived business/site UUID rules are unchanged. A winner waits for the normal
Paper water/shoreline proof. Final acceptance compares again, then revalidates and
commits the grant, first job, empty stock, bounded habitat and selection audit/cursor
in one transaction. Failure rolls back all; exact existing startup replay does not
rerank or add an audit. No physical construction, docks, resource refill or new money.

Kept public food customers independent of the private automatic-input-purchase
switch: explicitly disabled private purchases contribute no private demand while
public funds/assessment checks remain. Enabled incomplete reviews still defer.
This also applies to shared existing-business staffing. Fishery acceptance retains
valid saved/headless food-assessment support instead of requiring a separate
running food engine; actual current population/backlog/failure guards remain.

Focused clean Wrapper verify/package passed56 units +2 packaged integrations,
zero failures/errors/skips,37.570s. Three new temporary-SQLite cases cover real
water/bank startup with private buying disabled, full late-audit rollback/retry,
and funded public-food-driven hiring with private purchases off. Two ran in the
main verify; the last additional hiring case passed separately in10.615s. Existing
fishery, mixed resource/marker selection, competition and session regressions passed.
Initial compile found a missing charset import; fixed before verification.

Actual isolated Paper26.2-129 passed ordinary water observation, qualified automatic
shoreline startup, one paid selection/grant, timed catches/earned experience,
same-habitat bank recovery, absent-world pause and restart. No block/entity edits;
fixture-only ticket removed, server stopped. Main disposable remains schema98:
173 prior tables,163 exact, ten ordinary clock/capture/same-field observation
tables changed; one normal age month, identical resident identities/status and
exact corresponding ages. Integrity/FKs/config/capture clean; private grant/job/
habitat/selection/history and9500 conserved remaining cash verified. Original port
restored, helper archived, backups/audit retained. Native helper's old schema70
assertion updated to98; an initial private-class compile reference was corrected
before any server setup, without exposing application internals.

No migration or human-server/main change. Full hosted verification follows push.
P3 remains76.4% partial,1/4 whole groups; livestock startup allocation and broader
specialization acceptance remain. No new design answer required.

## Latest verified batch - marker workplaces share protected startup selection

Connected the four existing qualified marker adapters (blacksmith, warehouse,
bakery, craftsman) to resource startup comparison. Up to one proposal per adapter
joins the bounded mineral/quarry/logging choices, valued against actual funded
unmet needs after the proposed public startup debit. Recipes contribute their
prepared outputs; warehouses their admitted catalog. No new grant allowance,
skills, sources, goods, course enrollment, property rights or Paper chunk loading.

A selected provisional marker waits for its existing main-thread loaded point
check. Acceptance compares again on the SQL owner; normal current marker, owner,
inputs, budget and capture checks remain. All four existing transactions now
compose the selection audit with the paid grant/first job/store/cargo. A mismatched
actual industry/business refuses the audit. Exact accepted replay preserves its
existing receipt without another ranking or cursor advance. Existing schema98
selection tables admit these industry kinds already; no migration/data rewrite.

Focused clean Wrapper verify/package passed59 units +2 packaged integrations,
zero failures/errors/skips,58.457s (latest log). Added3 real-workshop temporary DB
cases,1 mixed marker/resource adapter comparison and an additional shared-enabled
variant of the actual PopulationSession training/startup scenario. They prove
waiting/no reservations, missing proof refusal, late-audit full rollback/retry,
one grant/audit/replay, real SQL-owner wiring and an actual resource winning after
a higher-priority marker proposal is withdrawn. Existing four adapter suites pass.
Two initial compile checks found constructor punctuation and a missing Set import;
fixed before verification. No passing claim for those failed compile attempts.

Actual isolated Paper26.2-129 passed: ordinary timed supply-backed training,
Paper workshop startup review/point confirmation, real paid founding, marker
removal/wrong replacement/restoration, absent-world pause and restart. One shared
blacksmith selection/audit survived, money29500 after the500 actual startup expense,
two actual jobs, one starter cargo and20 practice-material spending entries.
The original AIR block restored; fixture-only ticket removed; server stopped.
Main disposable DB remains98 with173 tables,166 exact; seven changed tables contain
normal clocks/capture and same-field reobservation metadata, zero aging. Initial
audit conservatively refused the farm observation changes; read-only inspection
proved only an old partial->complete rescan and fresh timestamps, with identical
fields/grain counts/identities. Strict recheck passed integrity/FKs/config/capture
and private business invariants. Failed initial audit and final proof retained.
Port restored/helper archived. Human server/main unchanged.

Previous b0cf334 full hosted verification is now confirmed SUCCESS on Windows and
Linux (run39181828920). Latest marker work gets a new full hosted run after push.
P3 remains76.4% partial,1/4 whole groups: fishing/livestock startup priority and
broader specialization acceptance still remain. No new gameplay decision.

## October18, 20:40 EDT - full-regression findings corrected

The full local Wrapper clean verify ran1951 units:1928 passed,23 failed, no errors
or skips,13:52 at20:37:36. Twenty-two failures were stale predecessor snapshot
expectations after additive schemas95-98; one was a real legacy hiring regression
introduced by the new shared student reservation. This run failed and did not
reach packaging/integration tests; it is not recorded as a green full suite.

Updated17 migration fixture suites. The common helper requires all seven additive
tables to exist and be empty before removing them from an earlier-schema snapshot;
all predecessor row/constraint/rollback assertions remain. The warehouse fixture
takes a current snapshot before rewinding to62, so its current-only ore epoch is
explicitly distinguished: it cannot survive a simulated schema that never had it,
and upgrading must create an empty epoch table. All other saved assets stay equal.
No application migration, production-data reset or relaxed runtime guard.

Restored disabled/missing shared-allocation legacy hiring: an already-qualified
trainee can take a real vacancy and the existing study loop then cancels their
course. Shared allocation still reserves active students, matching its complete
workforce comparison. Added one temporary-SQLite regression proving both paths;
existing fishing study/hiring regression passes. Selection diagnostics now name
the chosen village/business to avoid confusing a shared result with every page firm.

Final focused Wrapper clean verify/package passed60 unit +2 packaged integrations,
zero failures/errors/skips,1:03 at20:39:36. It reran every failed case and complete
directly affected staffing/career/competition/report classes; no repeated full local
suite. Full hosted Windows/Linux verification follows the feature push. This is
cumulative coverage, not a claim that the corrected full hosted run is already green.
No new Paper API adapter/native run, schema remains98, human server/main untouched.

P3 stays76.4% partial,1/4 whole groups. Next: remaining marker/animal startup
competition and broader specialization acceptance. No new design decision.

## October18 - bounded hosted failure counts

Added a failure-only CI summary for existing JUnit reports on Windows/Linux. It
publishes at most20 source-verified test class names and integer failure/error counts.
Names must match a Java identifier pattern and an existing test-source path.
No case names, arguments, failure messages, fixture data or test output are exported.
No credential access or action dependency. Disposable PowerShell smoke passed:
source/count selection, private argument/message omission, unknown-source refusal
and missing-report fallback. No Maven rerun was needed for this workflow-only slice.

The older fa486ca hosted run failed on both platforms. Public annotations gave only
the exit code. Approval review rejected Git credential extraction and the initial
proposal to publish failure messages. Neither action ran. Continued with local
reproduction and reduced this change to source class names and aggregate counts.
The full local run identified stale predecessor snapshot expectations; their fixes
are still being validated. No gameplay, schema, human server or gate change.
P3 remains76.4% partial.

## October18 - current Phase3 playtest guide and saved-village lookup

Added admin-only /civ debug villages using the ready session cache, with32 sorted
saved IDs and an exact omitted count. Report arguments now offer full-ID prefix
completion (maximum32); unauthorized users receive no saved IDs. Startup lookup
does not create/open a database. No world access, SQL on the Paper thread, gameplay
change or schema migration; remains98.

PHASE3_PLAYTEST.md consolidates safe installation, actual sneak-right-click food
interaction, bargaining/barter, livestock market/buckets/protection, diagnostics,
editable clock/configuration boundaries, current industry behavior and an explicitly
uncompleted human checklist. Older TONIGHT_PLAYTEST notes now point to this guide.
Corrected stale workforce documentation about legacy versus configured shared hiring.

Focused Wrapper clean verify/package passed10 command unit tests +2 packaged
integration tests, zero failures/errors/skips,19.173s at20:18:06 EDT. Two new cases
cover startup/permission safety and35 saved villages with bounded lookup/completion.
This cache-only command change adds no new Paper world adapter; no new native
runtime or human acceptance is claimed. Full hosted suites follow the feature push.
Human server and main untouched.

The documented-playtest acceptance entry is now complete: final group1/5; resources
36/39, lifecycle14/15, core4/4. Equal group weighting gives about76.4% partial P3,
still1/4 whole groups complete. This is a documentation milestone, not a claim
of additional gameplay or finished long-run acceptance. Remaining combined startup
competition, useful crafts, bulk-editor capture and specialization checks stay open.
No new design question; continue scoped competition and acceptance work.

## October18, 20:16 EDT - urgent existing farms get first access to idle labor

Connected approved urgent-food priority to the existing farm adapter before other
business vacancy allocation. At most two villages per owner pulse; only an existing
operating farm, present world, complete crop evidence, real unfilled observed slots
and a food assessment matching current resident demand qualify. The farm keeps its
saved skills/capacity and ordinary employment history. No new farm, money, production,
time advancement, job transfer or invented food is authorized by this hook.

New-file urgent-industries now defaults to farming, bakery and fishing. Existing
installed lists stay unchanged. Active students are excluded from farm hiring,
matching non-farm staffing/founder protection. Balanced fields and unrelated
industries retain their ordinary scheduling. No schema migration; remains98.

Focused clean Wrapper verify/package passed47 unit +2 packaged integration tests,
zero failures/errors/skips,42.000s at20:13:31. Three new temporary-SQLite scenarios
prove scarce idle labor reaches the urgent farm before another vacancy, no extra
food/time/spending, and refusal for absent worlds, partial fields or changed
population. Existing farm, selection, config and session regressions passed.
This is SQL-owner/application scheduling without a new Paper API adapter; no new
native run was claimed for this slice. Earlier staffing and resource-selection
Paper acceptance remains separately recorded above. Human server and main untouched.

P3 remains about71.4% partial,36/39 resources,14/15 lifecycle and0/5 final acceptance:
marker/animal startup priority and broader specialization remain. Next is current
playtest documentation and remaining startup adapters. No new decision needed.

## October18, 20:09 EDT - shared resource startup investment verified

Resource startups now compete through the approved priorities instead of fixed
mine/logging/quarry invocation order. Comparison uses each adapter's current viable
proposal, qualified idle founder, protected real grant and actual funded demand.
Public need is recomputed after that proposal's startup debit. Existing source,
warehouse, capacity, placement/provenance and range checks remain; the selected
adapter repeats them before committing. Founder study remains on its normal path.
One initialized village per owner pulse, bounded5-second village reviews; no
exhaustive ranking of every ore patch or invented resource opportunities.

Schema98 adds two empty resource investment state/audit tables. Actual grant,
startup spending, empty store capital, first job, business/source history, selection
explanation and saved tie cursor commit together. A late audit failure rolls all
back. Old missing allocation sections retain separate resource startup scheduling.
Marker-confirmed workshops/warehouses, fishing/livestock acceptance and farm staffing
still need their shared integrations. No new governance, wages or closure rules.

Focused broad check covered89 units:86 passed, three new fixture failures showed
food assessment stale after adding a sixth resident. Refreshed the fixture food
period using its original deterministic priority seed; all four new resource
investment cases passed on final verify, with two packaged integrations. Final
BUILD SUCCESS20:06:37,16.889s. Tests prove funded mine300 beats feasible quarry200,
quarry can win when the mining customer lacks funds, grant/resource/job/cursor
rollback, interval and no-world/no-skill/no-money/stale evidence refusal. Existing
85 resource/session/migration cases remained passing; no full local-suite repeat.

Native isolated Paper26.2 build129 passed20:07:12: enabled shared selection on the
existing ordinary forest observer/private PopulationSession fixture. Selected and
audited one logging startup, then paid logger->warehouse->craftsman deliveries
produced fittings in five fixed-clock periods. Exactly one grant and six original
supports,32,500 total cash after the known500 expense; no refill or extra funds.
All nine original AIR fixture states restored, test-only3x3 tickets removed.
Main disposable schema97->98 audit171 predecessor tables166 exact, only five normal
clock/capture metadata changes, no aging, integrity/FKs clean, config byte-identical,
port restored/helper archived/backups retained. Human server and main untouched.

Shared staffing b3fa3f1 is committed/pushed on codex/phase3-industry-competition.
This batch continues that feature branch. P3 remains36/39 resources,14/15 lifecycle,
0/5 acceptance, about71.4% partial and1/4 whole groups: combined capital/worker
competition is not falsely checked while other adapters remain independent.
Next: remaining farm and marker/animal startup paths, then broader acceptance.
No new consequential decision is needed for the approved priority rule.

## October18, 19:59 EDT - shared existing-business hiring verified

Implemented the user's approved urgent-needs, funded-demand and fair-tie priorities
for existing non-farm vacancies. The complete village comparison chooses before
hiring, including businesses outside the current runtime page. It filters real
vacancies, qualified idle adults, living local owners, source/habitat availability
and actual funded demand. Existing jobs stay in place; no wages, trades, money,
forced transfers or closure rules added. Students are consistently excluded from
hiring and comparison. Five-second throttling also bounds empty reviews.

Schema97 adds two empty allocation state/audit tables. Job, hiring audit, selection
explanation and saved tie cursor commit together; failure rolls all back. Defaults
in new economy.yml opt into shared allocation, urgent bakeries/fisheries,5-second
reviews and bounded reference values; old missing sections retain legacy behavior.
Default staffing also recognizes bakery/craftsman/livestock vacancies. Invalid edits
fail before DB startup. See BUSINESS_ALLOCATION.md for budgets and limits.

First focused clean Wrapper verify passed53 unit +2 packaged integration tests,
38.427s at19:54:31. After bounded empty-review throttling/diagnostic adjustments,
final affected verify/package passed12 units +2 integrations,31.131s at19:56:37.
Seven new cases cover funded ranking over UUID order, urgency/ties/value bounds,
restart fairness/interval, no poaching/spending, atomic audit rollback, no-money/
world/exhaustion refusal, full-village selection outside a page and strict config.
Predecessor schema snapshots now exclude only the two new tables; old rows remain
compared and schema94->current verifies empty additive tables. No full local repeat.

Native isolated Paper26.2 build129 passed19:57:26: the actual PopulationSession pulse
selected real iron customer demand400 instead of earlier-ID coal demand100; one
qualified new job, existing smith retained, treasury unchanged. No world edits or
chunk tickets. Main disposable schema96->97 audit169 predecessor tables164 exact;
only five routine clock/capture metadata tables changed, no aging, integrity/FKs
clean, config byte-identical, port restored/helper archived/backups retained.
Human server and main untouched. Recovery fa486ca hosted checks remain in progress.

Startup investment and farm staffing are not yet covered by this common comparison.
The combined competition and specialization gates remain open; P3 stays36/39
resources,14/15 lifecycle,0/5 acceptance, about71.4% partial and1/4 whole groups.
Next: compare viable resource startup investments, then other startup adapters and
broader acceptance. The priority decision is approved; no new question is needed.

## October18, 19:39 EDT - scoped forest recovery verified

Added /civ debug recover-forest list, review <site-uuid>, confirm <token>. Complete
loaded snapshots include every saved support and configured natural-canopy geometry,
with nearby sticky exclusions. A second unchanged scan and explicit same-admin
approval are required. Missing trunks receive permanent loss records; lost canopy
reduces habitat without declaring a trunk dead. Review cannot increase saved habitat
capacity or timber balance, clear protections, alter earned goods or unlock new
forestry discovery. Unknown/missing chunks and truncated trees refuse approval.

Schema96 adds one initially empty forest_recoveries audit table. Source losses,
habitat change, approval and balance changes commit together. Same per-site trust
is used by timed logging, production and tool replacement. Current capture health
is separate from historical trust; clean restart retains scoped approval, later
gaps/crashes revoke it. No generic trust reset, editor certification or block edits.
Budgets512 supports,9 snapshots,200,000 cells,4,096 scan steps/tick, two simultaneous
scans,16 reviews,30-second scan freshness and120-second admin preview expiry.

Five new temporary-SQLite cases verify actual scoped logging and revocation,
clean-restart continuity, retained goods/history, permanent coordinate loss,
canopy-only damage, no refill on reapproval, incomplete/stale/unhealthy/expired
refusal and audit-failure rollback. Affected verification covered58 unit cases
across checkpoints:54/55 passed first, the old-schema comparison was corrected,
all4 cases in that class plus3 founder cases passed; two packaged integration tests
passed. Final Wrapper verify/package BUILD SUCCESS19:37:28,18.277s. Updated three
older migration snapshot filters to exclude only newly added tables, preserving
all predecessor-row comparisons; migration94->current also compares all prior rows.
No full local-suite repeat. Hosted CI will run the complete cross-platform suite.

Actual isolated Paper26.2 build129 passed19:37:59. A private saved six-support
forest had one missing trunk: preview and confirmation reduced96 habitat/timber
to48 because only the other complete tree retained suitable canopy. Exactly one
trunk retired, all six source identities retained, one approval, global discovery
still fenced. All eight original AIR states restored after seven temporary log/
leaf edits; one fixture-only ticket removed. Main disposable schema95->96 audit:
168 predecessor tables,163 exact; only five routine clock/capture tables changed,
zero aging, clean integrity/FKs, config unchanged, port restored and helper archived.
Human server/main untouched. Private test database and backups retained.

Combined reviewed resource recovery milestone is now checked: resources36/39
(92%), about71.4% equally weighted Phase3 partial checklist,1/4 whole groups. The
remaining resource gates are broader useful crafts, supported bulk-editor capture,
and broader live/long-run catalog acceptance. Competition and specialization remain.
The next allocation rule has been asked: prioritize urgent village needs then real
funded unmet demand, editable priorities/fair tie rotation, with no forced worker
moves or closures. Approval is pending; no ranking policy applied yet.

## October18, 19:31 EDT - scoped mineral recovery verified

Approved admin acceptance plus complete current scans is implemented for existing
mineral deposits. /civ debug recover-ore list, review <deposit-uuid>, confirm <token>
keeps preview and explicit approval separate. The second scan must match; saved
balances, exclusions and current capture must also remain unchanged. Missing or
protected coordinates lose only their remaining shares. No earned goods, money,
identity or history is deleted; no refill or global trust reset occurs.

Schema95 adds two initially empty tables: resource_recovery_epochs and
mineral_recoveries. Approvals apply only to the selected deposit and current capture
generation. Clean stops preserve approval; crashes/new capture gaps invalidate it.
Current operational capture is distinct from historical trust. Accepted ore saves
now fence work immediately while pending, matching the forest safety pattern.
Discovery/founding remain globally fenced. No forest recovery yet; metadata alone
certifies no logging. Unsupported bulk editors remain unsupported.

Focused Wrapper verification covered107 affected unit tests across the correction
checkpoints:106 passed in the broad run, one old-schema comparison corrected and
all6 cases in its class passed on final rerun; two packaged integration tests passed.
Twelve new recovery/continuity cases cover scoped actual mining, revocation, exact
loss, sticky protections, read-only preview, incomplete/stale evidence, rollback,
replay/expiry and schema94->95 prior-row preservation. Initial compiler imports and
old-schema fixture tables/comparison were corrected without weakening FK/migrations.
Final verify/package BUILD SUCCESS19:29:42,17.382s; no repeated full local suite.

Actual isolated Paper26.2 build129 passed19:30:47: private saved four-source coal
deposit previewed25 lost, explicit confirmation changed100->75, only the AIR
coordinate retired; global discovery stayed fenced. Main-thread messages verified.
First native setup refused an unloaded spawn chunk before edits; retry used one
explicit test-only ticket, removed afterward. All four original AIR states restored,
three temporary ore blocks changed, no drops. Production adapter never loads chunks.
Private test DB retained. Main disposable fixture schema95 audit168 predecessor
tables163 exact; only five routine clock/capture tables changed, zero aging, clean
integrity/FKs, config byte-identical, port restored, helper archived/backups retained.
The human server and main remain untouched. Actual registered recovery listing is
wired; full native review/confirm invokes the same adapter on a private session.

P3 resources35/39, about71% partial and1/4 whole groups unchanged: the combined
recovery gate still needs forests. Next: forest recovery, broader useful crafts,
competition and multi-village acceptance. No new gameplay decision required.
Prior commits4462dae and5508353 now have full successful Windows/Linux CI runs
39153111684 and39152003696. New recovery work is on codex/phase3-resource-recovery.

## October18, 14:12 EDT - read-only resource and workforce review verified

Added admin-only /civ debug resources and /civ debug workforce <settlement-uuid>.
The first reports separate saved ore/forest trust, remaining mineral shares,
retirements, deduplicated placement/removal exclusions, timber habitat balances,
and at most32 world/chunk scopes with an exact omitted count. Active capture's
clean_stop=false is not mislabeled a crash. It reports saved data, not current
block truth or recovery approval. Live capture diagnostics remain separate.

The workforce comparison shows saved vacancies, overlapping qualified idle pools,
actual known business cash and operating blockers. Unknown accounts stay unknown;
unknown towns and review-budget overflow show no partial comparison. Budgets cover
32 firms,512 idle candidates and512 saved assignments; profiles/requirements are
batched and eligibility is compared in memory. It does not pick a winner or change
hiring order, wages, spending, closure, skills or courses. No player wallet added.

Both reads run on the SQL owner; permission-checked results return via Paper's
main-thread scheduler. Added seven database report tests and two command cases;
existing command checks reused. Final affected Wrapper verify/package passed15
unit +2 packaged integration tests, zero failures/errors/skips,29.947s at14:10:03.
Checks caught an invalid fixture store deletion and the new report's zero-vs-unknown
cash bug; corrected without weakening FK enforcement or financial guards. Updated
an existing usage-text expectation for the new commands. No full local suite repeat.

Actual marked Paper26.2 build129 passed14:10:40: registered resources command queued
its read and delivered bounded messages only on the main thread. Schema remains94;
all166 predecessor tables retained,161 exactly; only five normal clock/capture
metadata tables changed, no aging. Integrity/FKs/capture clean; config unchanged,
zero probe block changes, original port restored, helper archived, backups retained.
Human server and main untouched. Read-only report does not certify historical
provenance or restore trust. Actual recovery remains blocked on the pending user
choice recorded in DECISIONS_AND_OPEN_QUESTIONS.md. No partial recovery code applied.

Resources35/39, about71% partial Phase3 and1/4 whole groups remain: diagnostics do
not finish recovery or actual cross-firm competition. Forestry7/7 is verified in
preceding5508353, pushed to codex/phase3-forestry-runtime. This batch lives on
codex/phase3-resource-review. Next: approved scoped recovery, then remaining crafts,
production competition and multi-village acceptance. Canonical roadmap unchanged.

Hosted forestry run39152003696 (5508353) remains in progress; no full green result
is claimed. Earlier safety run39150215584 (fb1c06e) passed both platforms. This report
branch receives its own full hosted checks after push.

## October18, 13:56 EDT - automatic forestry and paid timber chain verified

Villages can now use current loaded-chunk canopy evidence to open a logging
business when real timber demand, a qualified idle owner and protected startup
funds are available. Excluded/claimed trees, stale or conflicting frames, wrong
world/chunk, absent worlds and insufficient funds cannot create a firm. A change
to nearby canopy after capture also requires a new survey. Existing staffed
logging capacity counts before opening more firms. No trees are felled or claimed.

Prospective owners earn forestry skill gradually using real timber and stone,
with normal food/material reserves. Unknown evidence after restart pauses study;
fresh evidence of a lost opportunity cancels it. Server/world absence adds no
catch-up. Other occupations retain their original one-point study speed.

Schema94 adds two empty founder-course/review tables; captured sources, goods,
accounts, employment and history are preserved. New-file economy.yml has strict
editable forestry-founding terms and a forestry course (2 timber +1 stone per
active study period, target10, shared gain1). Old missing sections opt out and
installed files are never overwritten. See FORESTRY.md for limits and settings.

The combined affected run executed330 units:326 passed, four new fixture failures
were corrected (cash funding/overdraw and refusal-text assumptions), along with
a missing test import. No production guard was weakened. Final affected Wrapper
verify/package passed34 units +2 packaged integration tests, zero failures/errors/
skips,25.242s at13:51:56 EDT. Eight startup/study tests and three configuration cases
were added. Existing predecessor-row preservation suites passed in the combined
run. Full hosted Windows/Linux verification follows push; it is not claimed here.

Actual Paper26.2 build129 passed13:54:35: ordinary forest observer -> session ->
qualified funded startup -> paid logger/warehouse/craftsman deliveries -> crafted
storage fittings in five fixed-clock periods. Exactly six saved supports; all
cash conserved at32,500 after a real500 startup expense from33,000 fixture funds.
The private proof accelerated cadence and disabled unrelated tool/storage spending;
normal configurable upkeep has separate existing tests.
Nine original AIR blocks restored exactly. A preceding attempt stopped safely
because the helper's separate read-only connection lacked foreign-key enforcement;
that helper setting was fixed, failure logs/backups retained, then the full proof
passed. No production source change was needed for that helper error.

The original93 fixture upgrade retained164 predecessor tables (159 exact); only
five routine clock/capture metadata tables changed, no aging. Final94 rerun
retained166 tables (161 exact), clean integrity/FKs/capture. Config byte-identical,
original port restored, helpers archived, backups retained. Human server/main
untouched. Private fixture proof does not replace a human playtest or prove broad
village specialization/long-run balance. Forestry7/7 connections verified; the
combined basic resource-opportunity gate is now complete. Resources35/39, about71%
equally weighted partial Phase3,1/4 whole groups. Next: remaining crafts, reviewed
resource recovery, lifecycle/competition and multi-village acceptance. No new
gameplay decision was needed for this batch.

Preceding checkpoint fb1c06e passed both hosted Windows/Linux jobs in run39150215584.
This automatic-startup batch will receive its own full hosted run after push.

## October18, 13:34 EDT - forestry provenance verified

Native tree placement/growth/removal records now protect saved sources independently
of ore history. Confirmed loss retires only named supports, reduces standing capacity
without refilling it, and keeps harvested goods, ownership and business history.
Saved placement exclusions feed the bounded loaded-chunk forest observer. Untrusted
capture or pending native saves pause registered logging clocks/work without freezing
other industries; exact receipt replay and earned backlog remain intact. Legacy saved
forests without capture history require reviewed recovery rather than silent trust.

Schema93 adds three initially empty tracking/exclusion/source-loss tables. No old
rows are rewritten. Added8 focused tests plus a disposable-only native helper;
updated predecessor-preservation fixtures for the three additions. The initial
clean affected run executed220 unit tests with one incorrect new timeline expectation;
219 passed. Corrected that test's expected4 rather than5 periods, without changing
production rules. The affected rerun passed27 units +2 packaged integration tests,
zero failures/errors/skips,13.359s at13:32:28 EDT. A preceding sandbox-only packaged
backup failure was a Windows file-move denial; the same check passed outside that
sandbox against temporary databases. Full hosted suites follow the feature push.

Actual marked Paper26.2 build129 passed13:33:16: accepted growth saved exactly three
excluded coordinates, cancelled growth saved none, and the ordinary observer then
rejected that trunk. Nine high-sky fixture blocks restored exactly. Schema92->93
retained161 predecessor tables,157 exactly; only four normal clock/capture metadata
tables changed, no aging. Integrity/FKs clean, both captures clean/trusted, old config
byte-identical, original port restored, helper archived, backups retained. The human
server and main remain untouched. Native proof covers dispatch/observer integration;
transactional tests cover stock loss, rollback and restart. No physical logging,
planting, ownership claim or automatic logging startup is implemented by this batch.

Forestry5/7 detailed steps verified. Overall resources remain34/39 and Phase3 about
70% partial,1/4 whole groups until the combined live catalogue gate passes. Next:
qualified/funded automatic logging and real-material founder training, then live
startup/paid customer acceptance. No new design decision is blocking that work.

## October18, 02:50 EDT — usage-limited forestry protection handoff (UNVERIFIED WIP)

Delegated workers reported the account usage limit. Current local work compiles:
`.\mvnw.cmd -B -ntp -DskipTests compile` succeeded,7.648s. This was a compile check
only: no new tests, clean verify, packaged build or native acceptance were run for
this batch. Do not install the working tree as a verified release. Last verified
and pushed source remains ac33ed7 on codex/phase3-forestry-runtime; main and the
human server were not changed. WIP remains local and uncommitted.

Draft schema93 adds empty forestry_tracking, forestry_exclusions and
forestry_source_losses tables, independent from ore capture. Added native tree
mutation listener, SQL-owner save/read hooks, clean-stop tracking, sticky source
exclusions and individual saved-support retirement. New resource opening rejects
known exclusions/untrusted saved capture; habitat restoration rejects retired
coordinates. Existing migration fixtures were updated for the three additions;
these fixture changes have not been tested yet. No live database was migrated.

NEXT REQUIRED before verification/commit: complete independent IndustryRuntime
forestry accrual/work fence (configureForestry currently retains the old one-arg
call); prevent direct new logRows extraction/tool spending on untrusted forestry
history while preserving replay/earned stock; feed saved exclusions into the
observer (currently environmental-only Set.of()). Review native multi-place/new
state event details, async drain/pending failure paths, and coordinate budgets.
Add focused provenance/tracking/transactional92->93 preservation tests and native
Paper capture proof. Then docs/diff/feature commit/push; continue qualified,
funded automatic logging startup and live acceptance. No design answer is needed.

Progress receives no new credit: forestry4/7 verified; resources34/39; Phase3 about
70% partial and1/4 whole milestone groups. Automatic logging remains unfinished.

## October17, 22:46 — live read-only forest observation verified

Villages can now report nearby canopy-backed standing trunks through the ordinary
Paper observer and /civ debug observation. Snapshots come only from already-loaded
chunks; bounded work preserves capture time, rejects unknown/oversized evidence,
and excludes horizontal logs/persistent placed leaves. Observer geometry and pacing
are editable in economy.yml; missing old sections opt out. Samples are transient
environmental diagnostics, not ownership proof, saved resources or businesses.

Windows Wrapper affected verify/package passed12 unit +2 packaged integration
tests, zero failures/errors/skips,24.396s,22:42:20 EDT. Two new configuration tests;
existing geometry/resource cases reused. Actual Paper26.2 build129 passed22:44:58:
ordinary observer found exactly the expected trunk, identity/time matched,
decorative/persistent examples and missing margins were rejected. Nine fixture
blocks restored exactly, helper-only tickets released. No production chunk loading
or world mutation. Native probe has an explicit disposable/localhost/zero-player
guard; never install probe JARs for player use.

Schema remains92. Main fixture retained161 tables,154 exactly; seven normal clock/
capture/age tables changed by one normal month, identity/status/assets preserved,
integrity/FKs clean. Private DB created no business/resource/work. Existing config
byte-identical, original port restored, helper archived, backups retained. Human
server and main untouched. Backend2f6450b is committed/pushed and its full Linux CI
passed; Windows remains in progress. Final observer hosted checks follow push.

Forestry details are4/7 verified: geometry, saved resource/work, real paid customers
and live environment observation. Next: native placement/growth/damage protection,
then automatically qualified/funded startup and live acceptance. No new gameplay
decision needed. The combined catalogue gate remains unfinished, so resources stay
34/39 and P3 about70% partial,1/4 whole groups. See FORESTRY.md for exact boundaries.

## October17, 22:37 — saved forestry and real timber customers verified

Registered logging firms now have persistent canopy-backed habitat, non-1:1 timber
stock, gradual renewal, normal work clocks, qualified employees and saved forestry
experience. Actual timber is paid for and delivered through warehouses to funded
craftsmen. Shared protected startup money, empty stock, first worker and habitat
commit together; retries, restart, absence, retuning and late failure preserve
goods/cash/history. Fixed the missing registered-logging timber resale allowance.
Unqualified or habitat-less work does not request logging tool upkeep.

Schema92 adds four EMPTY resource/support/work/habitat-audit tables. No old rows or
businesses are retrofitted. Added11 focused tests; existing preservation fixtures
still compare every predecessor row/table. Forestry resource terms are now editable
in economy.yml; missing old sections disable the new backend, and installed files
are preserved. Live automatic tree adoption/founding remains off until native
provenance and current observation are connected. No physical felling or land claim.

Combined Windows Wrapper affected verify passed170 unit +2 packaged integration
tests, zero failures/errors/skips,47.667s,22:34:45 EDT. Initial test-only duplicate
workplace/zero-renewal assumptions were corrected; the chain test caught the actual
timber resale omission, fixed before the passing rerun. No repeated complete local
suite; full Windows/Linux CI follows the feature push. Exact final command:
.\mvnw.cmd -B -ntp verify "-Dtest=ForestryPersistenceTest,ForestryRuntimeTest,ForestryConfigurationTest,ForestScanTest,ForestStockTest,DedicatedProducerChainTest,BusinessStartupGrantsTest,IndustryRuntimeConfigurationTest,EconomyConfigurationTest,QuarrySurveyTest".

Actual marked disposable Paper26.2 build129 reached ready and stopped22:36:00.
Upgrade91->92 retained157 old tables,153 exactly; only four routine metadata tables
changed, no resident aging. Integrity/FKs clean, four additions empty, capture clean/
trusted, economy.yml byte-identical, original port restored, backup retained.
Human server untouched; no live forest-adoption/native-harvesting result is claimed.

Resources remain34/39, about70% equally weighted partial Phase3,1/4 whole groups:
the forestry catalog gate still needs live acceptance. Broader crafts, recovery,
competition and specialization acceptance also remain. No new gameplay answer is
needed for non-destructive forestry observation/provenance. Next: that live capture
and qualified/funded automatic startup, then integrated long-run acceptance.
Feature branch codex/phase3-forestry-runtime; main unchanged. See FORESTRY.md for
files, config, migration, behavior and limits. Preceding7dd5643 Linux CI passed;
its full hosted Windows job was still running when this entry was recorded.

## October17, 22:22 — forestry recognition and timber calculations

Added bounded incremental canopy-backed tree recognition with exact supporting
coordinates, supplied placement exclusions, explicit incomplete evidence and safe
outer margins. Added renewable timber calculations with non-1:1 opening capacity,
gradual fractional renewal, real requested output/work/storage limits, absent-world
pause, and explicit conservative habitat changes. No live logging business is
claimed: this slice has no Paper, database, inventory or world mutation. Server
text-file configuration and saved provenance/habitats remain integration work.

Seven focused tests and Wrapper package passed, zero failures/errors/skips,
16.628s,22:21:13 EDT; corrected a test-only static method reference before passing.
No repeated full suite or native run for these isolated domain additions. Schema
remains91. See FORESTRY.md for changed classes, prototype terms and limits.
Progress remains34/39 resources, about70% partial Phase3,1/4 whole gates: logging
needs actual live integration before receiving checklist credit. Quarry source
263fbe3 is backed up to GitHub; Windows/Linux run39089622441 is in progress, not
yet claimed passing. Main and the human server remain unchanged. Next: persistent
forestry evidence/provenance and actual paid timber supply. No decision needed.

## October17, 22:17 — automatic stone/sand businesses verified

Completed bounded loaded-chunk stone/sand observation, separate current per-chunk
frames, actual funded customer selection, gradual real-material founder training,
and atomic qualified quarry/sandpit startup under the existing protected grant
limits. Finite reserves feed ordinary timed work and paid warehouse/customer
deliveries. Existing mine stone counts toward supply; sand needs a real configured
buyer. No excavation, land claim, invented customers or new money. Settings remain
editable; older installed files are preserved and missing sections opt out.

Schema91 adds five empty quarry evidence/frame/training tables; no previous rows
or tables are changed by migration. Existing configured warehouse sand slots are
added empty with an audit only when a sand customer exists. The preserved bakery
empty-slot helper retains its existing receipt identities. Neighboring empty
surveys no longer erase another chunk's prospect, and stale historical scopes do
not exhaust the current-frame review budget.

Windows clean verify passed1,868 unit +2 packaged integration tests, zero failures,
errors or skips,14:23, finished22:13:25 EDT. The final per-chunk correction and two
regressions followed that full run; affected final-source verify/package passed
56 unit +2 packaged tests, zero failures/errors/skips,39.607s,22:15:27. No final-source
full-suite result is claimed. Actual Paper26.2 build129 passed22:16:20: ordinary
read-only observer matched independent terrain; private SQLite founded an actual
candidate, ran five production periods, delivered paid cargo through a warehouse
to a producing customer, conserved money after the startup sink, and preserved
absent-world assets. Fixture-only imports/Windows Java quoting were corrected.
The stopped main disposable fixture retained152 predecessor tables,148 exactly;
only four routine metadata tables changed, no resident aging, integrity/FKs clean.
Settings restored, helper archived, backups retained. Human server untouched.

Resources34/39 (87%), about70% equally weighted partial Phase3; whole groups remain
1/4. Forestry, broader general crafts, reviewed resource recovery/compatibility,
competition and emergent-specialization acceptance remain. Next: non-destructive
forestry environmental evidence and real timber supply. No gameplay answer needed.
Feature branch: codex/phase3-quarry-startup; main unchanged. GitHub backup follows.
See AUTOMATIC_QUARRIES.md for files/settings, migration and current limits.

## October 17 — usage-limit handoff: quarry startup work is UNVERIFIED

Both delegated workers stopped with the account usage-limit error during the next
stone/sand milestone. This is unfinished working-tree work, not a playable feature
or a passing migration. No build/tests/native server/database operations were run
for this batch. Do not release or commit it as complete without finishing review.
Last verified/pushed source remains3cfbc10 on codex/phase3-live-livestock; target
JAR still comes from that prior passing build. Main and the human server unchanged.

Draft changes: independent QuarrySurveys schema (proposed91, three additive empty
quarry_* evidence/frame/membership tables), MineralOpportunities.quarries(Connection)
and MineralSurveyReader.quarries(Connection) factories keeping coal/iron frames
separate, proposed schema validation, QuarryFoundingPolicy and partial strict
configuration readers. Config defaults/YAML/tests may be incomplete. No actual
Paper quarry observer or automatic quarry startup has been connected yet.

Resume: inspect all six draft source files/diff; finish quarry config/defaults and
regressions; update SchemaFixtures and old preservation expectations for the three
new empty tables; add survey isolation/rollback tests; implement bounded loaded
stone/sand observation plus actual customer/skill/shared-grant startup; verify full
migration build and native integration before any milestone credit/commit/push.
Do not discard the existing drafts or silently certify them. P3 stays33/39 resources,
about69.5% partial,1/4 whole groups. No consequential gameplay question is pending.
Latest user reply reconfirms bounded automatic natural-animal recognition and
[Civ Protect] for guaranteed large-pasture protection; preserve that existing rule.

## October 17, 17:53 — dedicated producers no longer block local trade

Fixed warehouse/demand classification that rejected registered fisheries,
quarries and sandpits as unknown industries. Saved fishery food and matching
finite-deposit yields can now supply actual funded customers. Existing earned
stock stays tradable; the fix creates no resource, customer, capital or automatic
quarry. Unknown/unregistered businesses still defer for review. Configurable tool
needs are retained, and exhausted quarries/sandpits use the same no-replacement
check as exhausted mines. Immediate extraction trust gaps suppress new tool
pressure without deleting already-earned goods.

Changed WarehouseSupplies, LocalIndustryDemand and BusinessTools; added eight
DedicatedProducerChainTest cases for actual fishery/finite extraction, real paid
warehouse orders, delayed delivery, exact replay, registration guards and positive
then depleted quarry tool demand. Updated TONIGHT_PLAYTEST.md and milk/egg/bakery
guides so preserved old configs and unfinished bulk pen donation are explicit.
Schema remains90; no migration, world edits or human-server deployment this batch.

Windows Wrapper affected verify passed58 unit +2 packaged integration tests (60),
zero failures/errors/skips,33.897 seconds. Initial restricted run hit a Windows
sandbox denial moving the packaged backup test's temporary file; the ordinary
permission-enabled rerun passed without skipping tests or changing backup code.
No new Paper API behavior was introduced; preceding actual Paper slaughter and
bakery proofs remain the native checkpoints. Full Linux CI for preceding c4ddeec
passed; its Windows job was still running at this checkpoint. Feature backup and
fresh hosted full verification follow. No unrun hosted result is claimed.

Checklist credit stays33/39 resources, about69.5% partial P3,1/4 whole groups:
this repairs connected behavior rather than adding an uncompleted industry.
Next scoped industry work: actual trusted stone/sand surveys and qualified funded
quarry/sandpit startup, then broader forestry/craft and specialization acceptance.
No new gameplay answer is needed for this fix. Main remains unchanged.

## October 17, 17:40 — controlled livestock slaughter verified

Village livestock businesses can now turn eligible surplus adults into saved food
without duplicate ground drops. Exact loaded animal identity, breeding reserves,
unused earned work, qualified staff and real storage space are checked first.
Cancelled deaths keep the animal and add no food. Ordinary kills keep vanilla
loot and receive no business food. Pending outcomes survive restart and reserve
space; an unprovable outcome stays paused for reviewed recovery, never inferred
from a missing or unloaded animal. Defaults and limits are editable; older files
without livestock-slaughter remain opted out. See LIVESTOCK_SLAUGHTER.md.

Schema 90 adds one transactional permit/audit table; existing histories and
assets are preserved. Fixed quick shutdown during database startup so it drains
capture state and closes cleanly. Also corrected a stale CI catalogue expectation
for the already-added milk/egg weights; no economy behavior changed for that fix.

Windows clean verify passed 1,837 unit + 2 packaged integration tests (1,839),
zero failures/errors/skips, 12:22, finished 17:31:35 EDT. Two later regressions
cover manual-budget exclusion and immediate startup shutdown; final affected
verify/package passed 57 unit + 2 packaged tests (59), zero failures/errors/skips,
35.949s, finished 17:36:23 EDT. Those two tests were not in the earlier full run.
Packaged Paper 26.2 build 129 passed at 17:39:36: actual cancelled death,
intentional death with no ground loot/XP, ordinary death with normal loot, exact
saved head/food counts and replay protection. Main disposable fixture retained
all 152 tables/148 exact; only four routine metadata tables changed, no aging,
integrity/FKs clean. Fixture-only startup/timing and revive-health assumptions
were corrected before the successful proof. Server stopped, settings restored,
helper archived and backups retained. Human server untouched.

Resources advance to 33/39 (85%); equally weighted partial Phase 3 progress is
about 69.5% (roughly 70%), with 1/4 whole groups complete. Detailed livestock
connections are 10/11; authorized bulk pen donation remains. Broader industry,
competition, specialization and final acceptance still prevent calling P3 done.
Next: check remaining producer-to-warehouse connections and pen donation.
No new gameplay decision is needed. Feature-branch backup/CI follows; main stays
unchanged.

## October 17, 17:02 — paid livestock-to-bakery food chain verified

Bakeries now buy delivered milk and eggs through paid local warehouses and make
richer meals from actual ingredients. Bread remains the simple startup/fallback;
one recipe uses the existing worker/tool budget per earned period. Defaults are
editable: 3 grain + 1 milk + 1 egg -> 8 abstract food units. No physical cake item.
The integrated scenario continues through a real paid village food order.

Business-only ingredient prices avoid changing saved public food catalogues.
Configured existing bakery/warehouse stores gain audited empty ingredient slots;
units, cash, capacity, original terms and saved unit weights are retained. No
schema change (89), world edits or human-server deployment. Installed config is
never rewritten; see BAKERY_SUPPLY_CHAIN.md for opt-in and exact boundaries.

Windows affected verify/package passed 234 unit + 2 packaged tests (236), zero
failures/errors/skips, 1:31, finished 17:02:13 EDT. Initial checks caught a fixture
accessor and incompatible public-price catalogue additions; corrected before
shipping. Packaged Paper 26.2 build129 passed 17:02:51: 39 accelerated actual
PopulationSession periods, real herd products and both paid delivery legs, recipe
flows, conservation, exact receipt replay, restart and absent-world pause. Main
disposable fixture retained all 151 tables/147 exact; four ordinary clock/capture/
ore metadata changes, no aging; integrity/FKs clean. Server stopped, settings
restored, helper archived, backups retained. Feature push/hosted CI follows.

Resources advance to 32/39 (82%); equally weighted partial Phase3 progress is
about69%, with 1/4 whole groups complete. Controlled slaughter, authorized pen
donation, broader craft/industry coverage, competition and specialization remain.
Next: controlled livestock slaughter with exact native identity/drop accounting.
No new gameplay decision is needed for this batch.

## October 17 — livestock qualifications, succession and resale defaults verified

Real feed-backed animal care now earns configurable farming experience for actual
qualified employees. Owners earn nothing merely for owning a business. Awards
respect real work, worker limits and ceilings, rotate when work is scarce, and
commit with feed/products/births/the clock. No work or replay means no free skills.
A scenario proves an initially unqualified employee earns the actual founder
qualification and can then inherit under the existing family-first rules.

Fixed another lifecycle gap: default resale values now cover grain, milk, eggs
and sand. Stocked producers can be valued and sold instead of waiting forever
for a missing quote. Real buyer budgets and accepted sale prices stay intact.
Installed files remain unchanged; admins opt into the livestock learning mapping
and add the new editable resale values. No wallets, payroll or new money.

Schema 89 adds two learning audit tables only. Existing herd/animal/owner/skill/
stock/history rows survive; no retroactive awards or reset. Updated historical
migration fixtures to require exactly the two new empty tables while comparing
every predecessor row unchanged. See LIVESTOCK_LEARNING.md. Changed: livestock
learning helper, shared runtime, migration validation, learning/resale defaults,
focused and preservation tests, excluded native probe and livestock guides.

Windows .\mvnw.cmd clean verify -B -ntp passed 1,814 unit + 2 packaged integration
tests (1,816), zero failures/errors/skips, 12:19, finished 16:26:28 EDT. After the
small resale-default correction, final affected verify/package passed 111 unit +
2 packaged tests (113), zero failures/errors/skips, 23.605s, finished 16:28:21 EDT.
No claim that the three subsequently added resale tests were in the earlier full
run. Initial checks caught outdated table-list expectations and an underfunded
buyer fixture; both were corrected without weakening preservation/spending rules.

Guarded packaged Paper 26.2 build 129 passed 16:28:43 EDT: populated private
schema88 upgrade, actual PopulationSession care/skills/audits, restart, absent-
world and capture-gap pause, cash/goods conservation. No block/animal changes.
Main dev fixture retained all 149 predecessor tables/145 exact; ordinary clock/
economy/capture/ore metadata only, no aging. Two new tables empty; integrity/FKs
clean. Server stopped/settings restored/helper archived/backups kept. Human
server untouched. Feature backup/hosted CI follows; main stays unchanged.

Phase 3 remains in progress: resources 31/39 (79%), about 68% partial across
four groups, 1/4 groups complete. The automatic observed/funded herd-operation
entry is complete; physical slaughter without duplicate drops, authorized pen
donation, bakery milk/egg consumers and remaining industry/specialization gates
remain. These qualification/lifecycle fixes do not inflate checklist credit.
No design answer is needed for this batch.

## October 17 — automatic natural livestock startup and the feed chain verified

Villages can now start a real dairy/chicken business from repeated bounded
observations of eligible natural animals. Names, leashes, human/unknown origins,
saved owners, nearby barriers and saved protected pens exclude adoption. A real
farming-qualified owner, available adult worker and food-protected shared startup
grant are required. Claims, firm/site, job, cash and initial ages commit together;
failed funding or saving leaves no orphan ownership. No animal teleport or chunk
loading. Existing worlds/saves and closed firms retain identity/history.

Warehouses now recognize registered herds, plan their actual funded feed needs,
and buy grain from real crop-farm surplus. Cash, cargo, feed use and output are
conserved; herds cannot resell purchased grain as producer output. New strict
livestock-adoption settings are editable; installed files missing them opt out.
No migration/reset: schema 88 retained. See LIVESTOCK_ADOPTION.md.

Affected Wrapper verify: 364 unit + 2 packaged integration tests passed, zero
failures/errors/skips, 1:44, finished 2026-10-17T16:02:53-04:00. After tightening
the uncertain-observation reset, final package + 14 focused unit/2 packaged checks
passed, 21.041 seconds, finished 16:03:39 EDT. This is focused validation, not a
new full-suite claim. Guarded Paper 26.2 build 129 passed 16:04:27 EDT using real
natural cows and the actual observer/PopulationSession: fence exclusion, repeated
clear proof, named-animal protection, paid opening and exact durable ownership.
Test blocks restored/exact bodies removed; private DB retained. Main fixture:
149 tables retained/142 exact, ordinary metadata and one normal population month
only; integrity/FKs clean. Server stopped/settings restored/helper archived.
Human server untouched. Previous 39fd240 hosted Windows and Linux jobs both passed.

One full resource checklist entry is now complete: observed/funded live livestock
startup and timed operation. Resources advance to 31/39 (79%); whole groups remain
1/4, with about 68% equally weighted partial progress. Physical slaughter without
duplicate drops, bakery milk/egg consumption and authorized pen donation remain;
no credit for those unfinished integrations. No decision is pending for this batch.

## October17 — automatic saved-herd work and paid feed verified

Existing saved livestock businesses now use the shared industry clock: buy real
feed with business money, wait for cargo, then consume it for milk/eggs and saved
births under actual qualified work/storage limits. Existing newborn/age adapters
can reconcile those births. World/server absence and capture gaps earn no work.
Delivery, purchases, production and checkpoints roll back together on failure;
old manual herd periods are not replayed. New editable livestock-runtime limits
opt installed files out when absent. Schema88 unchanged; no migration/reset.

Five focused scenario/config cases added. Final scoped Wrapper verify passed126
unit plus2 packaged integration tests (128 total), zero failures/errors/skips;
43.518s, finished2026-10-17T12:26:27-04:00. No new full-suite claim. Initial checks
caught/fixed a fixture cash-column typo and new config root-key registration.
Guarded Paper26.2 build129 passed14:44:59 EDT: actual packaged owner-thread work,
paid feed/cargo/milk/births/conservation, restart, absent-world and capture-gap
pause. Private synthetic herd; no block/animal mutations. All149 main tables
retained/145 exact; ordinary clock/runtime/capture/ore metadata only, no aging.
Integrity/FKs clean; fixture stopped/settings restored/helper archived/backups
kept. Human server untouched. Full CI follows feature backup.

Changed: herd persistence composition, shared industry runtime, paid-feed helper,
editable policy/configuration, plugin/owner wiring, diagnostics, focused tests and
excluded LivestockRuntimeProbe. See LIVESTOCK_RUNTIME.md for settings/contracts.
P3's livestock milestone remains partial: automatic eligible animal adoption and
funded founding, physical slaughter, and bakery milk/egg consumers still remain.
This closes scheduled operation for already saved herds; no unimplemented startup
is counted complete. P3 stays~68% partial/30of39 resource entries/1of4 whole groups.
Next: bounded eligible-animal observation and atomic farm opening. No new answer
needed for this scheduled-work batch; no duplicate slaughter drops is approved.

## October17 — usable pen protection controls verified

Admins can now right-click a [Civ Protect] sign inside a supported flat enclosure,
review its area/animal count and private boundary outline, then explicitly confirm.
The plugin recaptures evidence and saves real animal owners. /civ pen here finds
saved IDs; owner/admin release also requires confirmation and retains each animal's
ownership/history. Sign placement alone claims nothing; existing owners cannot
be overwritten. Limits are editable in config.yml. No donation or automatic herd
founding/feed/output enabled yet. No migration; schema88 retained.

Following the user's faster-development instruction, final scoped Wrapper verify
passed67 affected unit plus2 packaged integration tests (69 total), zero failures/
errors/skips;26.961s, finished2026-10-17T11:44:00-04:00. No new full-suite run is
claimed; the prior checkpoint passed1,788. Guarded Paper26.2 build129 passed
11:44:59 EDT with a real sign/pen/cow and synthetic player: explicit confirmation,
owner/controller restart, lookup and release retain ownership/history. Temporary
blocks restored/test cow removed. All149 main tables retained/145 exact, ordinary
clock/runtime/capture/ore metadata only, no aging; integrity/FKs clean. Fixture
stopped/settings restored/helper archived/backups kept. Human server untouched.

New files: PaperPenControls, PaperPenCapture, PenProtectionSettings, eight focused
control/config tests, excluded PenControlsProbe and PEN_CONTROLS.md. Plugin/router,
config/permissions and owner handoffs updated. No-duplicate slaughter drops now
explicitly approved and recorded; no question remains for that rule. Full-suite
CI follows feature backup. Local checks stay focused between larger checkpoints.

P3 remains about68% partial (30/39 resource entries,1/4 complete groups). This
finishes an interaction prerequisite within the incomplete livestock milestone;
no percentage credit for unimplemented adoption/feed/slaughter. Next: automatic
livestock loop and bakery ingredients. See PEN_CONTROLS.md for actual commands,
authority and limitations; broader templates/construction remain Phase4.

## October17 — pen area release and retained animal ownership verified

Pen protection now separates an active area from its permanent history. An
explicit owner/admin release retains original animal ownership and provenance;
old protection retries cannot reclaim released land. Adjacent pens may share a
fence, while overlapping interiors are refused. Schema88 preserves original
pen records and backfills current claims without inventing ownership.

Added12 lifecycle/owner-queue cases; focused50 passed. Windows .\mvnw.cmd clean
verify -B -ntp passed1,788 tests (1,786 unit/two packaged integration), zero
failures/errors/skips;11:39, finished2026-10-17T11:01:25-04:00. Source/test Java
remained fixed during the final full run. The excluded native helper was then
compiled against the verified packaged JAR.

Guarded Paper26.2 build129 passed11:01:48 EDT: private populated schema87 upgrade,
shared fence, authority checks, release/retry/reclaim/restart, preserved original
pen/animal history and clean SQLite integrity/FKs. All147 main predecessor tables
retained;143 exact, only ordinary clock/runtime/capture/ore metadata changed,
no aging. The two new main tables are empty. Fixture stopped/settings restored/
helper archived/backups kept. No world/entity edits or human-server changes.
This certifies packaged persistence, not a live sign interaction or human client.

No player-facing pen controls or automatic adoption enabled. P3 stays about68%
partial (30/39 resource milestones,1/4 whole groups). Next: bounded authorized
pen interactions before automatic livestock founding/feed/output. The slaughter-
product question remains pending; no answer assumed. See PEN_PROTECTION_LIFECYCLE.md.
Feature backup/hosted CI follows the verified checkpoint; main stays unchanged.

## October17 — initial herd ages and monotonic native views verified

Known initial animals now reconcile from the saved herd period and original
remaining maturity, alongside newborns. Shared versioned native age metadata
prevents older in-flight views making adults young again. Same-period repair
avoids unnecessary clock writes; contradictory/malformed scope is reported,
not overwritten. Valid positions and other animals continue after an age issue.
Names, leashes, AI/gravity/invulnerability, ownership and assets are untouched.

Member paging adds an indexed latest-period read; missing checkpoints refuse
instead of manufacturing period zero. Schema87 unchanged; no migration/backfill/
reset. Added13 SQLite/age/observer cases. Focused52 passed. Windows .\mvnw.cmd
clean verify -B -ntp passed1,776 tests (1,774 unit/two packaged integration), zero
failures/errors/skips;12:31, finished2026-10-17T10:32:48-04:00. Source/test Java
frozen during full verification.

Guarded Paper26.2 build129 passed10:35:01 EDT: three real initial cows corrected
from intentionally wrong native ages, exact youngster period8/9 behavior, preserved
names/movement/protection flags, actual native stale-view rejection and unspawned
EntitySnapshot(World) age/lock/PDC round-trip. Automatic newborn/restart/real paid
feed/output/missing/confirmed-death regression also passed. Not automatic adoption,
actual chunk unload, human-client or JVM crash proof. All147 main tables retained,
143 exact; ordinary clock/runtime/capture/ore metadata only, no aging. Integrity/
FKs clean; native server stopped/settings restored/helper archived/backups kept.
Only exact private test animals cleaned up. Human server untouched.

Prior11ad06b passed hosted Windows/Linux, run39039309999. Current feature backup/
CI follows this checkpoint. P3 remains about68% partial (30/39 resources,1/4 whole
groups): pen interactions, automatic adoption/founding/feed/output, intentional
slaughter, wider products and specialization remain. Next: authorized bounded
bulk pen protection. Slaughter product-accounting answer is pending; no answer
assumed and independent work continues. See LIVESTOCK_SAVED_AGES.md.

## October17 — safe automatic physical newborns verified

Saved paid births now project as actual nearby cows/chickens on safe loaded
terrain. One birth handoff/eight-job pages, ownership commit/publication before
native add, activation before pending movement release, saved age lock/maturity,
exact retry/positive recovery and no missing-body replacement. No adoption,
terrain/chunk loading, free head/goods/cash or invulnerability grant. Shutdown
and unloaded-world cleanup retain only immutable identity work in SQL-owner order.

Added24 state-machine/temporary-SQLite/Paper geometry-age cases. Focused38 and40
passed. Three initial headless fixture errors required native block registry
seams, retaining default production properties. Initial full run intentionally
stopped for a discovered prepared-world-reference edge case; only that Maven
process/test child were interrupted. Final Windows .\mvnw.cmd clean verify -B
-ntp passed1,763 tests (1,761 unit/two packaged integration), zero failures/errors/
skips;12:29, finished2026-10-17T10:03:05-04:00. Source/test Java frozen.

Guarded Paper26.2 build129 passed10:03:55 EDT: controlled nearby gating, two real
cows automatically claimed/added/activated on existing safe terrain, normal
movement release/no granted invulnerability, owner/controller restart without
another UUID, actual paid active-period feed/products, saved individual maturity,
missing body never replaced, actual sibling death and exact assets/headcounts.
Not actual client/chunk unload/JVM crash, initial animal age or automatic adoption
proof. Schema87 unchanged:147 existing main tables retained/143 exact; ordinary
clock/runtime/capture/ore metadata only, no aging. Integrity/FKs clean. Fixture
stopped/settings restored/helper archived/backups retained; only its explicitly
identified animals cleaned up. Human server untouched. See LIVESTOCK_NEWBORNS.md.

Priorb00baa1 passed hosted Windows/Linux checks, run39036771535. Current backup/CI
follows this checkpoint. P3 stays about68% partial (30/39 resources,1/4 whole
groups). Next: initial adopted-animal age, automatic adoption/feed/output and real
bakery ingredients. Business-slaughter product accounting question is pending;
no answer assumed, and these independent tasks can continue.

## October17 — confirmed livestock distance and retained herds verified

Saved farm animals positively observed outside an editable working radius pause
products/breeding until return. Ownership/headcounts/aging remain; other eligible
animals work. Unknown/unloaded sightings alone never establish departure or death.
Bounded main-thread observer looks up16 saved living UUIDs per page, never scans
terrain/loads chunks/adopts animals, and retains exact failed-save batches.

Optional economy.yml livestock-working-area.radius-blocks defaults to32 (inclusive
3D,1..256); legacy files are not rewritten. SQL-owner work uses configured radius,
actual world presence and trusted death capture. Cohorts still mature while away;
real feed/output/storage/reserve accounting and receipt replay remain intact.
Additive schema87 creates an empty latest-positive-position table, retaining old
animals/births/assets/history without guessing old sightings or repairing gaps.

Added21 automated cases. Focused52 and151 passed. Windows .\mvnw.cmd clean verify
-B -ntp passed1,739 tests (1,737 unit/two packaged integration), zero failures/
errors/skips;12:24, finished2026-10-16T23:36:49-04:00. Source/test Java frozen.
Guarded Paper26.2 build129 passed2026-10-17 09:28:12 EDT: actual newborn/death
regression, bounded positive sightings, own test cow moved away/returned inside a
loaded chunk, saved away evidence through owner restart before new observation,
unchanged heads/ownership/goods/cash. Not automatic production/adoption, actual
chunk unload or JVM crash proof. Test save86->87 retained146 predecessor tables,
142 exact; only ordinary clock/runtime/capture/ore metadata changed, zero aging.
Integrity/FKs clean; new main presence empty. Fixture stopped/settings restored,
helper archived/backups retained. Human server untouched.

Earlier936fd54 and9c1a415 both passed hosted Windows/Linux checks (runs38996032941,
38998105994). Current feature backup/CI follows this checkpoint. P3 stays about68%
partial (30/39 resources,1/4 whole groups). Next: safe physical newborn placement/
reconciliation, automatic adoption/feed/output and real bakery ingredient customers.
No decision pending. See LIVESTOCK_WORKING_AREA.md for files/settings/limits.

## October16 — physical newborn identity handoff verified

Schema86 adds claim-before-spawn identities, retained failed aliases and individual
offspring death receipts. The SQL owner publishes protection before returning a
committed claim. Missing bodies never authorize replacement. Confirmed offspring
losses remove their exact saved cohort member and survive restart without adding
goods or money. New claims require trusted capture history and the origin world.

Added24 temporary-SQLite/owner cases. Focused163 affected migration/newborn tests
passed. Windows .\mvnw.cmd clean verify -B -ntp passed1,718 tests (1,716 unit/two
packaged integration), zero failures/errors/skips;11:59, finished
2026-10-16T23:14:31-04:00. Source/test Java frozen throughout verification.

Guarded Paper26.2 build129 probe passed23:15:00: real prepared UUID committed and
published before native addEntity, once-only activation, positive never-spawned
failure/retry with retained alias, actual native offspring death, exact surviving
sibling/head/goods accounting and SQL-owner restart. Not automatic adoption,
general reconciliation or an actual JVM crash test. Marked test save upgraded
85->86:143 predecessor tables retained,139 exact; only ordinary clock/runtime/
capture/ore metadata changed, no aging. Three new main tables empty; integrity/FKs
clean. Fixture stopped/settings restored/helper archived/backups retained; human
server untouched. See LIVESTOCK_BIRTH_PROJECTIONS.md.

Prior936fd54 hosted Linux passed; Windows still running when checked. Current
feature backup/hosted checks follow this checkpoint. P3 stays about68% partial
(30/39 resources,1/4 whole groups). The user approved pausing products/breeding
only for animals positively observed outside an editable working radius,
retaining ownership and treating unloaded observations as unknown. Next: this
eligibility rule and safe live herd integration; no decision is pending.


## October 16 — permanent newborn identities verified

Each new simulated birth now receives a deterministic permanent identity in the
same SQL transaction as its real feed/work costs, products and herd checkpoint.
Business ID, birth period and sibling ordinal establish identity; saved maturity
stays fixed through retuning and restart. Bounded chronological pages prepare the
physical adapter. No Paper animal, ownership, new birth or free goods are created
by recording these already calculated outcomes.

Additive schema 85 creates an initially empty livestock_births table. Older
anonymous births, headcounts, goods and histories stay intact and are reported as
unmapped. Coverage is a count diagnostic, not physical-presence certification;
validated identities and real reconciliation are still required before activation.
No automatic historical backfill, missing-animal replacement or data reset.

Added 13 domain/temporary-SQLite cases for stable IDs, sibling paging, bounds,
rollback/retry, restart/replay/retuning, legacy coverage and migration/corruption.
Focused 102 checks passed, then 77 affected migration/newborn checks passed.
The first full run exposed seven old comparisons in five fixture lists missing
only the new empty table. Verified every old value identical, extended explicit
empty-table assertions and retained every prior-row comparison. Final Windows
.\mvnw.cmd clean verify -B -ntp passed 1,694 tests (1,692 unit/two packaged
integration), zero failures/errors/skips; 10:43, finished 2026-10-16T22:40:40-04:00.
Source/test Java stayed frozen throughout that final run.

Guarded Paper 26.2 build 129 regression passed at 22:41:56: actual market/player
saves, bucket/egg separation, selective synthetic death/respawn, once-only seller
credit and owner/controller restart remain correct. Separate fixture herds retain
exact stable newborn IDs/maturity and zero unmapped births after restart. This is
not physical offspring, actual client death/crash or automatic adoption proof.
Main disposable save upgraded 84->85: all 142 predecessor tables preserved, 138
exact; only ordinary clock/runtime/capture/ore metadata changed, no aging. The new
main registry is empty; integrity/FKs clean. Fixture stopped, original settings
restored, helper archived, backups/synthetic saves retained. Human server untouched.

Previous market commit cbafaac passed both hosted Windows/Linux builds:
https://github.com/blueninjas24/Civilizations/actions/runs/38994123234 . Current
batch hosted verification follows its feature-branch backup. P3 remains about
68% partial (30/39 resources, 1/4 whole groups). Changed groups: domain birth IDs,
SQL registry/production transaction, schema migration, migration fixtures/tests,
native regression and project records. Next: physical claims and confirmed
individual offspring losses, then safe live adoption/feed/output and real bakery
customers. No new decision is required. See LIVESTOCK_BIRTH_IDENTITIES.md.

## October16 — recoverable milk/egg player market verified

Connected /civ market to nearby eligible recorded herd stock. A protected 54-slot
menu shows bounded expiring offers, editable prices and buyer-supplied buckets.
Explicit one-click orders persist intent, recheck saved supplier site/permission/
payment/room, deliver once, prove the saved exchange, then credit the seller once.
Failed saves retry the current stage; accepted orders recover after restart or
market disablement. Exact marked quantity is checked before requesting seller
credit. Uncertain inventory/restore or ledger disagreement retains REVIEW instead
of manufacturing another payout/refund. Dead players wait until respawn.

New goods occupy separate marked stacks. Only the exact operation/buyer/product/
quantity receive approved temporary death retention. Older goods, XP and normal
server keepInventory rules stay ordinary. Already-kept copies are not duplicated;
extra unexpected marked quantities remain ordinary drops. Confirmation removes
only the temporary marker. Guards cover movement, vanilla interactions, milk
consumption and recipe-book autofill. Food aid/sale/barter starts cannot overlap.

Added29 automated cases: storage separation, routing, exact death/consumption/
recipe-book behavior, saved proof and purchase recovery. Focused61 and42 checks
passed. Final Windows .\mvnw.cmd clean verify -B -ntp passed1,681 tests (1,679
unit/two packaged integration), zero failures/errors/skips;09:23, finished
2026-10-16T22:11:50-04:00. Initial full run intentionally stopped to add the
pre-credit quantity guard; source/test Java frozen during final verification.

Guarded Paper26.2 build129 market probe passed22:13:02: actual native item/PDC/
player files and separate SQL owner, menu double-click, withheld preparation and
exchange saves, synthetic selective death/respawn, real buckets/egg separation,
native reload and owner/controller restart. Exact two completed orders, seller
cash1600/800 and milk1/eggs1 closing stock; integrity/FKs clean. Not a human-client
death, actual JVM crash or automatic herd test. First probe setup incorrectly
shared two business addresses; corrected only the excluded helper and reran.
No application behavior changed to bypass that valid uniqueness constraint.

Main disposable fixture remains schema84:142 predecessor tables preserved/135
exact, only ordinary clock/active-world/capture/ore metadata and one age month.
Existing identities/goods/cash/history retained. Fixture stopped, original25577
settings restored, helpers archived, backups/synthetic saves retained. No helper
blocks/animals/online players changed. Human server untouched; no migration here.
Previous product-sale1722790 hosted CI passed (run38980634799); foundation38f9763
Linux passed/Windows still running at last check. Current batch hosted run pending.

P3 stays~68% partial (30/39 resources,1/4 whole groups). Player retail is connected;
automatic adoption, permanent physical offspring/reconciliation, scheduled feed/
output and bakery customers remain. Changed groups: market controller/menu,
selective item guard/storage planning, owner-site DTO, command/startup routing,
old food-transfer mutual exclusion, tests and docs. Next: newborn identity and
safe herd activation. No design decision pending. See LIVESTOCK_MARKET.md.

## October16 — configurable nearby market and inventory exchange foundation

Implemented bounded SQL-owner dairy/chicken quotes from actual eligible staffed
business stock in present worlds. Fresh reservation rechecks saved supplier,
position, editable order/payment terms and stock. Accepted orders retain their
exact terms for recovery after settings/availability changes. Unknown cancelled
intent creates neither a receipt nor goods/cash. No Paper/JDBC crossing of threads.

New economy.yml livestock-market section controls introductory milk/egg prices,
egg order count, radius, candidate cap and offer lifetime. Cash conversion reuses
purchases.trade-value-per-emerald. Defaults: one milk for2 emeralds,4 eggs for1;
these are editable introductory quotes, not milk/egg bargaining. Missing section
in installed files stays disabled. Startup wires these settings to the owner API;
no player command/menu or purchase controller is exposed yet.

Pure storage planning debits exact plain emeralds and one real bucket for milk,
then merges/splits deliveries within actual item limits. Custom items stay intact;
insufficient room refuses before mutation. Paper snapshots verify complete writes,
restore exact contents after provider failures and never overwrite a changed
inventory when no apply occurred. Armor/offhand/container inventories are untouched.
Initial headless adapter tests encountered a Paper block-registry lookup through
Material.isAir; removed that unnecessary lookup by comparing the three air constants.
No existing application behavior was weakened for test convenience.

Added22 tests, including a seeded3,000-inventory conservation sweep. Focused114
initial and35 boundary/owner tests passed. Windows .\mvnw.cmd clean verify -B -ntp
passed1,652 tests (1,650 unit/two packaged integration), zero failures/errors/skips;
08:19, finished2026-10-16T19:58:48-04:00. Source/tests frozen during final verification.

Guarded Paper26.2 build129 inventory probe passed21:44:00: real ItemStacks and
virtual CraftInventory snapshots, native egg maximum16/milk1, metadata retention,
full-space refusal, multi-stack eggs, stale protection and partial-provider restore.
Not a live-player delivery/death/save test. No helper world/entity/player mutations.
Fixture83->84 uses the previously implemented empty sale-ledger migration;141
predecessor tables retained/137 exact, only clock/runtime/capture/ore metadata,
age delta0; integrity/FKs clean. Fixture stopped, properties restored/helper archived,
backups retained. Human server untouched. Schema stays84; no new migration here.

P3 stays~68% partial (30/39 resources,1/4 whole groups). Next: actual purchase
controller/menu and approved selective pending-goods death retention, then safe
live adoption/feed/output. User approved the death rule while verification finished;
no decision remains pending. No confirmed player trading or completed livestock gate.

## October16 — saved player proof for livestock receipts

The existing bounded, read-only SavedAidProof parser now accepts only the new
PendingLivestockSale key in addition to its three existing food-transfer keys.
It still requires exact full-file root BukkitValues evidence. The NBT parser,
compression, file limits, path rules and supported format are unchanged. No live
player inventory/command/listener or automatic sale was enabled.

Three added temporary synthetic-save cases exercise all products/stages, exact
player/key/value matching, nested and other-key spoofs, unsupported-key refusal,
duplicated proofs and corrupt/truncated compressed files. Focused14 tests passed,
including the seven existing parser tests and four receipt tests. Final Windows
.\mvnw.cmd clean verify -B -ntp passed1,630 tests (1,628 unit/two packaged
integration), zero failures/errors/skips;07:46, finished2026-10-16T19:16:50-04:00.
No source/test changes during that final run;20 new tests across this batch.
Schema remains84; no new migration or actual server/world/database operations.
Native player save/crash delivery remains an unfinished integration gate; this
is new-key coverage for the existing format, not proof of a live inventory exchange.
P3 remains~68% partial. Next: owner handoff, configurable sale quoting, inventory
exchange/recovery and the player menu, alongside safe live herd adoption/output.

## October16 — milk and egg sale ledger

Added saved reservations/completion/cancellation for real herd product sales,
exact buyer-supplied milk-bucket receipt terms, and storage protection so later
work cannot occupy the space needed to cancel an order. No seller revenue before
confirmed exchange; retries/restarts cannot repeat a completed payment or goods
return. Wrong supplier/world/product, unavailable staff/stock and changed terms
are refused. SQL or arithmetic failures roll back the entire transition.

Schema84 is additive and starts with an empty sale ledger. Old-schema regression
fixtures now explicitly assert that new table empty while retaining every old-row
comparison. Existing goods, cash, herd identity, histories and world files remain.
No actual server database was touched. No new inventory/menu/command is enabled;
the future Paper adapter must verify saved player proof, inventory and price terms.
Automatic adoption, physical newborn reconciliation and live feed/output remain
unfinished. P3 stays~68% partial (30/39 resources,1/4 whole groups).

Final Windows .\mvnw.cmd clean verify -B -ntp passed1,627 tests (1,625 unit/two
packaged integration), zero failures/errors/skips;08:47, finished2026-10-16
19:07:55-04:00. Seventeen new cases passed. Focused57 trade/herd/recovery/storage
cases passed before two final storage/quantity cases. One initial fixture omitted
the required employment-end reason; switched it to the existing termination API,
without changing application behavior. Source/tests frozen during final run.
No native smoke needed for this SQL/domain-only change; no Paper API changed.
Prior81be4a0 source passed hosted Windows and Linux verification:
https://github.com/blueninjas24/Civilizations/actions/runs/38967519430 .
Earlier80fe3f0 Windows was cancelled, not certified; later81be4a0 passed both.

## October16 — milk container decision recorded

User approved buyer-supplied empty buckets for milk purchases. Canonical design,
simulation rules, livestock adapter contract and current checklist now agree:
one supplied bucket per milk bucket, real recorded milk stock and payment,
no free iron container, no payment taken when the required bucket is missing.
Eggs need no container. The container question is resolved; live livestock
production and player product trading remain unfinished. P3 stays~68% partial.

Documentation only: no Java/config/schema/artifact changes, no database/server
operations and no new test/build run. Previous verified source remains939f805
with1,610 local tests. Latest hosted verification could not be refreshed because
GitHub DNS was unavailable in this sandbox; no hosted success is inferred.

## October16 — hosted verification allowance after measured Windows slowdown

Raised only the CI job allowance45->60 minutes; retained both operating systems,
all wrapper checks, timeouts in tests and dependency pins. The older7f523ff Windows
job explicitly exceeded45 minutes while still progressing through passing tests
(WarehouseSuppliesTest at cancellation); its Linux job passed. The later03b4d86
loss workflow passed both platforms. This is measured hosted disk variability,
not evidence that a test should be removed or that all future runs will finish.
Evidence: https://github.com/blueninjas24/Civilizations/actions/runs/38960407636 .

Application/test source and packaged artifact remain exactly the locally/native
verified939f805 recovery revision: final1,610 tests, schema83. Validated the workflow
diff changes only its numeric allowance. No extra Maven rerun for this CI/docs-only
commit. New hosted verification remains pending; no two-platform pass claimed for
this latest head. P3 remains~68% partial; livestock substeps are in PROGRESS.md.

## October16 — durable capture eligibility and safe owner shutdown

Schema83 adds initially empty livestock_capture metadata. Startup writes a dirty
capture marker; only new empty history or an already trusted clean stop can be
eligible. Legacy herds with no fence, unclean stops, explicit gaps and unresolved
saves cannot silently regain trust. No automatic repair/reset permission. Cached
status additionally requires no unsaved staged loss; every known death is staged
before its Paper queue delay, so the first saved loss cannot briefly certify the
others. Exact successful retry clears its pending identity. Pre-readiness capture
loss/expiry marks a durable gap; repeated gap notifications coalesce.

Confirmed death saves stay available when untrusted. Shutdown drains accepted work
before persisting its fence. A repeated close outside the owner now waits for the
original drain; owner callbacks still return without waiting for themselves. This
fixes an existing Windows database-file lock race exposed by the extra write.
/civ debug status reads cached capture eligibility without JDBC/world operations.
The trusted herd backend remains explicit; future live callers must require this
gate plus safe physical reconciliation. Neither adoption nor live output is enabled.

Eleven new executed cases: nine repository/owner/migration and two handoff gap/
staging cases. Older-save tests retain predecessor comparisons and explicitly
assert the new table empty. Focused148 passed before final staging coverage;
latest focused44 and final Windows .\mvnw.cmd clean verify passed1,610
(1,608 unit/two packaged integration), zero failures/errors/skips;07:52, finished
2026-10-16T17:03:38-04:00. Production/test Java frozen for that final run. Earlier
1,608 full pass was superseded after review found the pre-owner staging gap.
One new fixture initially assumed legacy trust; corrected only its explicit prior
clean-history seed. The existing callback-close test exposed the real shutdown race.

Guarded Paper26.2 build129 passed17:04:31: actual owner/start/status, multi-observation
pending gate, immediate gap, owner-callback/external close drain, durable restart
warning and simulated prior dirty marker. No deliberate JVM crash or physical
reconciliation certification. No helper world/entity mutations. Schema82->83 audit
retained140 predecessor tables/137 exact; normal clock/runtime/ore metadata only,
age delta0, integrity/FKs clean. Main new marker is clean/trusted for its empty
herd history. Fixture stopped, properties restored/helper archived/backups retained.
Existing OSHI warning only; no Civilizations/probe/listener failure. Human server
untouched. Branch codex/phase3-livestock-recovery; main unchanged.

P3 stays30/39 resources,1/4 whole groups/~68% partial. Livestock foundation substeps
are shown in PROGRESS; automatic adoption, physical newborn UUIDs/reconciliation,
scheduled founding/feed/output and milk/egg customers remain. Capture trust does
not certify plugin-free play, external world edits/restores or unseen animals.
Before player milk trades, the pending container rule needs a decision; no free
iron buckets may be created. CI allowance maintenance follows separately: the
older harvesting Windows log shows continuing passing tests until45-minute
cancellation, Linux passed, and the later loss workflow passed both platforms.
See LIVESTOCK_PROTECTION.md and DECISIONS_AND_OPEN_QUESTIONS.md.

## October16 — hatched and explicitly created animals keep protective origins

Supported EGG/BREEDING/SPAWNER_EGG/DISPENSE_EGG/COMMAND spawn events now retain
anonymous protective origin evidence. Normal spawning/hatching, amounts, items
and experience are unchanged. Cancelled/unsupported events are untouched; no
player owner is guessed. Original native reasons also protect older untagged
animals from these sources. Existing/malformed protective values are retained.

Captures now copy the native reason name into plain-Java immutable observations;
absent information is UNKNOWN. No missing tag or uncertain CUSTOM/DEFAULT source
is reinterpreted as natural. Real ownership/pen authority and safe observation
remain mandatory; this does not enable adoption. Physical-loss deferred/shutdown
logging now retains exception causes without changing exact retry behavior.

Four new executed cases; focused30 passed. Full Windows .\mvnw.cmd clean verify
passed1,599 (1,597 unit/two packaged integration), zero failures/errors/skips;
07:42, finished2026-10-16T16:37:40-04:00. Production/test Java frozen during final
verification; no behavior changes for test convenience. Schema unchanged82.

Guarded Paper26.2 build129 passed16:38:12: actual native explicit-origin spawns,
production event tags, original-reason fallback with tags removed, PDC serialization
and immutable origin capture. Existing three-colour actual egg laying/drop control,
synthetic both-hand bucket/breeding and player-normal behavior checks passed too.
This is explicit native spawn-reason coverage, not a thrown-projectile hatching or
human-client/stock-trading test. All helper animals/items removed, ticket released,
no block changes. All140 predecessor tables retained/137 exact; only ordinary
clock/runtime/ore metadata changed; age delta0, integrity/FKs clean. Fixture stopped,
properties restored/helper archived/backups retained. Existing OSHI warning only;
no Civilizations/probe/listener failure. Human server untouched.

P3 stays30/39 resources,1/4 whole groups/~68% partial. Next: unclean-capture recovery
before safe physical herd reconciliation/live production. Physical newborn UUIDs,
automatic opening/feed/customer loops and wider industry/specialization remain.
No decision needed. Branch codex/phase3-livestock-origin; main unchanged.
Prior harvesting7f523ff hosted Linux passed; Windows still running at16:38. No
hosted pass claimed for newer loss/origin revisions. See LIVESTOCK_PROTECTION.md.

## October16 — confirmed original-herd losses reach saved production

Confirmed uncancelled native cow/chicken deaths now cross the main-thread/SQL-owner
boundary as immutable observations. Original saved UUIDs lose one adult or their
actual young cohort. Unknown/player/unassigned animals create no loss or ownership;
missing observations never establish death. Cross-world death records the actual
location world without changing original ownership/world history or earning time.

Loss receipts and old production history remain immutable. Pending losses reduce
current heads immediately after commit; future paid production checkpoints save
the reduced state and apply losses atomically. Exact event/work replay and restart
do not subtract twice, refill animals or create goods/cash. Failed loss/application
writes roll back. Disabled/absent worlds earn no work or aging from the event.
Main-thread capture changes no native death, loot or XP; bounded unbound buffering,
serial owner requests, delayed exact retry and graceful draining include diagnostics.

Schema82 adds one initially empty loss table/index referencing original animals
and before/application periods. All prior rows remain. Every affected old-save
comparison still checks every predecessor row and explicitly asserts the new table
empty; upgrade failure/retry tested. No production/world reset or YAML changes.

Eleven new executed cases: seven accounting/migration/restart/failure cases, four
capture/retry/readiness/shutdown cases. Focused111 passed; final Windows
.\mvnw.cmd clean verify passed1,595 (1,593 unit/two packaged integration), zero
failures/errors/skips;08:09, finished2026-10-16T16:26:19-04:00. Production/test Java
frozen during final verification. Helper-only cleanup tags its own disposable loot.

Guarded Paper26.2 build129 passed16:27:16: actual native adult/young/player cow
deaths, real SQL-owner listener, one retained survivor, reduced feed/output,
absence, exact work/death replay and restart. No human client, automatic adopter,
physical offspring or crash-gap certification. All helper animals/tagged loot
removed; no blocks changed, ticket released. Schema81->82 audit retained139
predecessor tables/136 exact; only normal clock/runtime/ore metadata changed;
age delta0, integrity/FKs clean. New main-fixture loss table empty. Fixture stopped,
properties restored/helper archived/backups retained; OSHI warning only, no
Civilizations/probe/listener failure. Human server untouched. Branch
codex/phase3-livestock-losses; main unchanged.

P3 stays30/39 resources,1/4 whole groups/~68% partial. Next: egg/spawn-origin
protection, then unclean-capture recovery and physical herd reconciliation before
live adoption/output. The death queue is in memory until SQL commit: do not infer
crash-gap safety. Newborn physical UUIDs, authorized transfers, automatic funding/
feed/output and milk/egg trading remain separate gates. No decision needed.
See LIVESTOCK_PROTECTION.md.

## October16 — village milk/eggs and player-bred animal protection

Implemented the approved village-only product rule: an empty bucket in either
hand cannot milk a saved village cow, and saved village chickens cannot lay a
second physical supply of white/brown/blue eggs. Player/unclaimed/unsupported
animals, other items and naming/leashing remain normal. No inventory edits,
SQL/world scans per event, arbitrary item deletion, stock credits or death-drop
changes. Existing livestock enable/readiness gates apply; old files opt out.

Successful player-bred cow/chicken offspring now retain anonymous entity-PDC
protection evidence. Protected-parent evidence propagates; cancelled/unsupported
births remain untouched and existing values are preserved. No player IDs/names,
ownership, items or experience are changed. Captures include an immutable flag;
missing old tags never prove natural origin. Protection capture remains active
when production is disabled so future adoption cannot ignore those births.

Twelve new executed cases (seven product guards, four provenance, one captured
adoption-protection case). Focused28 passed. Full Windows .\mvnw.cmd clean verify
passed1,584 (1,582 unit/two packaged integration), zero failures/errors/skips;
07:46, finished2026-10-16T11:15:33-04:00. Production/test Java frozen during this
run. Excluded helper initially had ambiguous Plugin/Listener cleanup overload;
fixed its explicit Plugin cast, without any application/test changes.

Guarded Paper26.2 build129 passed16:10:23: saved SQL-owner identities and production
listener registration, actual three-colour native chicken laying/cancellation,
normal physical eggs for unclaimed hens, synthetic both-hand bucket/breeding
event dispatch, real PDC serialization and immutable capture. This is not a human
client milking or completed stock-trading check. All created animals/items removed,
chunk ticket released, no blocks changed. Schema81/no migration; all139 predecessor
tables retained/136 exact, only ordinary clock/runtime/ore metadata changed; age
delta0, integrity/FKs clean. Fixture stopped/properties restored/helper archived,
backups retained. Existing OSHI warning only; no Civilizations/probe/listener failure.
Human server untouched. Prior6556e2c hosted Windows/Linux checks both succeeded:
https://github.com/blueninjas24/Civilizations/actions/runs/38917154524 . Those results
belong to the preceding startup revision, not this newer harvesting revision.

P3 remains30/39 resources,1/4 whole groups/~68% partial. Collection decision resolved;
next: confirmed physical losses and safe herd reconciliation. Live automatic
adoption/founding, physical offspring, output scheduling, milk/egg trading and
broader industry/specialization gates remain. Cold-start freeze/publication must
precede live herd control; no live adopters enabled. Branch
codex/phase3-livestock-harvesting; main unchanged. See LIVESTOCK_PROTECTION.md.

## October16 — protected livestock startup and real animal evidence

Approved chicken/dairy businesses now share existing startup grants: unchanged
common spending caps and food-money protection, no extra money. Installed YAML
lists remain intact. A trusted atomic opening saves the paid business, site,
store, original worker and already-confirmed animal roster together. No free feed,
products, animals or skills. Real ordinary feed purchases and delayed delivery
can supply that herd; this is not an automatic starter-feed policy or live founder.

Failed final writes roll back every opening row and payment. Exact restart replay
returns current assets/cohorts and an ended original job without rehiring. Separate
legacy grant/herd setups cannot retrofit this opening or acquire a new job through
replay. Existing standalone herd opening retains its contract. Schema stays81;
no migration or existing-stock conversion.

Paper now captures bounded immutable cow/chicken identity, position, age and
name/leash evidence on the main thread from an already-loaded entity chunk.
No chunk loading, SQL, claims or animal mutations. Incomplete/over-budget/crossed
observations are unknown, never proof of death. Ownership, player breeding,
protected pens, authority and safe physical reconciliation remain separate gates.

Validation: 14 new executed cases (eight atomic funding/herd, one configuration,
five Paper capture); final focused47 passed. Full Windows .\mvnw.cmd clean verify
passed1,572 tests (1,570 unit/two packaged integration), zero failures/errors/skips.
Final build duration 09:36 min; finished 2026-10-16T10:34:41-04:00. Production/test Java frozen
for final verification. An earlier full pass preceded the final legacy replay guard
and was superseded by this run; no old result is claimed for the latest source.

Guarded Paper26.2 build129 passed actual main-thread cow/chicken capture, named/
young evidence, trusted fixture ownership, real protected grants, paid feed and
delivery, abstract milk/egg output, restart replay and absent-world pause. Six
helper-created animals were removed and the test-only chunk ticket released;
no blocks changed. 139 predecessor tables retained/136 exact; ordinary changes civilization_time, economy_runtime, ore_tracking; normal age-month delta0. Exact audit, integrity/FKs clean; fixture stopped,
properties restored, helper archived, backups retained. Existing OSHI Windows
counter warning only; no Civilizations/probe/listener failure. Human server untouched.

Previous craftsman revision d53fbea passed both hosted Windows/Linux checks:
https://github.com/blueninjas24/Civilizations/actions/runs/38908644113 . Windows
26:37/Linux2:40 confirms the earlier timeout correction without removing tests.
These hosted results do not claim verification of this newer livestock revision.

P3 stays30/39 resources,1/4 whole groups/~68% partial. Live automatic adoption,
founding, physical births/deaths, harvesting and product-customer integration are
unfinished. Before live milk/egg output, player harvesting versus stock-backed
trade needs the explicit decision in DECISIONS_AND_OPEN_QUESTIONS. No bucket/drop
behavior changed. Branch codex/phase3-livestock-startup; main unchanged.
See LIVESTOCK_PROTECTION.md for exact boundaries/settings.

## October16 — villages can train their first craftsman

Connected one prospective first-craftsman course to real current fitting customers,
feasible startup, observed crafting-table evidence and future practice supplies.
Existing food/material reserves, student limits, age, employment and active-owner
guards apply. Editable new-file settings consume one real timber per productive
economic period for one experience toward 10. No free initial skill or retrospective
backlog practice. Absence, stale/missing markers, lost customers/funding and supply
shortages pause safely; a taken first firm releases the learner.

SQL-owner automatic review now sponsors training when appropriate, then uses the
existing main-thread confirmation and paid opening after qualification. Actual
productive craft work earns bounded once-only further experience. Installed YAML
lists, saved catalogues and accepted course/history terms remain intact. Extra
employee hiring remains an explicit staffing-list opt-in; first paid inputs still
require public surplus. Wider products/household furnishing demand are unfinished.
See PROCESSING_WORKSHOPS.md for exact settings, opt-ins and limits.

Schema 81 adds two initially empty founder review/course tables and their index,
with deferred sponsorship links. No old people, jobs, cash, goods or history are
rewritten. Transactional migration/enrollment/practice failure and retry preserve
all prior rows. Historic migration tests explicitly assert new tables empty while
retaining every predecessor-row comparison.

Validation: nine new cases (eight training/migration/pause/rollback/SQL-owner,
one old-config opt-out); actual productive crafting learning also verified.
Focused 47 passed. Full Windows .\mvnw.cmd clean verify passed 1,558 tests
(1,556 unit/two packaged integration), zero failures/errors/skips; 7:52, finished
09:29:19 ET. Production/test Java frozen during final verification; later edits
only docs and the excluded helper. One early new config test changed the wrong
key (career-study rather than automatic-career-study); corrected its fixture
without changing application behavior.

Guarded Paper 26.2 build 129 passed 09:31:03 ET: real SQL-owner training with ten
actual practice expenses, restart, main-thread crafting-table confirmation,
automatic paid opening, produced fitting customer, further productive work learning,
replay and absent-world pause. The first helper advanced its test clock beyond
real time, so the saved-clock guard correctly deferred the customer's work.
Corrected only its time origin and reran; application behavior unchanged.
Initial schema 80->81 audit retained 137 predecessor tables/134 exact; retry retained
139/136 exact. Only normal civilization_time/economy_runtime/ore_tracking metadata
changed; zero resident aging, clean integrity/FKs and empty new main-fixture tables.
Temporary air block restored, chunk ticket removed, fixture stopped, properties
restored, helpers archived and backups retained. Existing OSHI Windows counter
warning; no Civilizations/probe/listener failure on the passing run. Human server untouched.

CI fix 6706540 raises the job allowance from 20 to 45 minutes, retaining all tests.
The previous hosted Windows check explicitly timed out; Linux succeeded. New
hosted verification remains pending; no two-platform hosted pass is claimed.

P3 remains 30/39 resources,1/4 whole groups/~68% partial checklist completion:
training/opening is now connected, but broader products, livestock and specialization
remain. Next automatic livestock founding needs the pending decision: may chicken/
dairy businesses share existing food-protected startup grants under unchanged
common limits? No livestock grant eligibility change is implemented. Feature
branch codex/phase3-craftsman-training; main unchanged.

## October16 — hosted persistence verification time allowance

GitHub's Windows job for3ecd4d3 was cancelled after20:06. Its check annotation
explicitly reports exceeding the20-minute job limit; Linux succeeded in4:24.
Raised the job allowance to45 minutes without skipping tests or changing the
verification command, matrix, permissions or branch protections. This is a time
allowance, not a claim that hosted Windows verification has passed. The same
revision passed the complete Windows suite locally. Main remains unchanged.

## October16 — qualified craftsmen open for real storage customers

Connected bounded first-craftsman opportunities, seeded qualified founder selection,
SQL-owner prepare/accept and the main-thread crafting-table opening cycle. Actual
current storage pressure and purchasing cash justify a prospect; staffed local
fitting stocks are considered first. A forecast neither reserves cash nor promises
a sale. Protected shared startup caps, current food assessment, real paid public
surplus, qualification, employment/study, marker freshness and storage/haul checks
apply. The entire business/site/worker/grant/material order/marker is atomic;
rollback/retry/restart preserves assets and prevents duplicate startup payments.

New-file craftsman qualification/opportunity sections are editable; old missing
sections opt out. Reuses small workshop bounds rather than adding a framework.
A qualified craftsman can now open through Paper and make/sell the fitting recipe.
No free experience or automatic vanilla profession change. First-owner study and
crafting work-learning remain next; an unskilled village still waits. The first
starter currently requires real protected public surplus, without a private fallback.
Wider useful products/household demand remain. Schema unchanged at 80.

Validation: 16 new cases (eight investment/persistence/session, six opening-cycle,
two configuration). Focused 154 passed. Full Windows .\mvnw.cmd clean verify
passed 1,549 tests (1,547 unit/two packaged integration), zero failures/errors/skips.
Build duration 07:37 min; finished 2026-10-16T08:44:08-04:00.
Production/test Java unchanged throughout final verification. Initial new fixtures
assumed unit storage weights, absent-world marker replay and an incomplete job end;
corrected to existing weighted storage, fresh present marker, assessed food and
employment contracts without changing application rules. The excluded native
helper initially attempted package-private stock inspection; corrected to read-only
SQL. No application visibility or behavior changed for helper convenience.

Guarded Paper 26.2 build 129 passed 08:44:38 ET: actual SQL owner, loaded main-thread
crafting-table confirmation, automatic qualified paid opening, paid public starter,
real timed production and business fitting sale/installation, replay and absence.
No training proof is claimed. Native save audit: 137 predecessor tables retained, 134 exact; ordinary changes civilization_time, economy_runtime, ore_tracking; resident aging 0 months. Integrity/FKs clean.
Temporary disposable air block restored, test chunk ticket removed, server stopped,
properties restored, helper archived and backups retained. Existing OSHI Windows
counter warning; no Civilizations/probe/listener failure. Human server untouched.

P3 stays 30/39 resources,1/4 whole groups/~68% partial checklist completion. These
are verified substeps within the still-open full craftsman gate. Next: supply-backed
craftsman training, livestock physical integration, wider products and specialization
acceptance. No design decision pending; main unchanged. Feature branch
codex/phase3-craftsman-investment. See PROCESSING_WORKSHOPS.md and STORAGE_UPGRADES.md.

## October16 — real paid craftsman storage customers (schema80)

Implemented the approved limited storage upgrades. Actual staffed businesses with
current storage pressure pay real craftsman/warehouse cash for produced fittings;
capacity increases only after delivery. Input/output stock and old catalogues stay
intact. Configurable retained cash, occupancy, count, original-baseline growth,
absolute capacity, source-review and hauling limits prevent unlimited expansion.
Paid cargo and installed improvements remain with the business in resale value.
Atomic receipts/replay, absent-world pause, current-clock spending and rollback
protect money and goods. Local demand includes real fitting customers without
counting a warehouse's own need twice or spending already-planned input money.

New-file editable recipe: three timber + one stone -> one storage fitting.
Default fitting invoice is20 Trade Value versus15 ingredient value; this is a
prototype gross ingredient margin, not guaranteed sales/profit. Missing upgrade
sections disable new purchases; installed files and old catalogues are preserved.
Schema80 adds one empty receipt table/index, rewriting no old economic assets.
Farms/herds require separate adapters and cannot use this generic capacity path.

Validation:16 new executed cases (14 persistence/runtime/migration/resale/demand,
two configuration), final focused154 passed. Full Windows .\mvnw.cmd clean verify
passed1,533 tests (1,531 unit/two packaged integration), zero failures/errors/skips,
in7:55 at08:27:28 ET. One earlier new test referenced the wrong receipt table;
corrected its SQL to recipe_flows without changing application behavior. Final
production/test Java remained frozen through verification.

Guarded Paper26.2 build129 passed08:28:00 ET: actual ingredient purchase/crafting,
SQL-owner automatic fitting payment, delayed installation, absence/restart and
exact disabled replay, preserving cash/stock and the buyer's old catalogue. No
helper world/entity/block mutations. Schema79->80 audit retained all136 predecessor
tables,133 exactly; only three normal clock/runtime/ore bookkeeping tables changed,
zero aging. Integrity/FKs clean. Fixture stopped, original properties restored,
helper archived/backups retained. Existing OSHI counter warning; no plugin failure.
Human server untouched. Earlier bakery GitHub CI: Linux succeeded, Windows was
cancelled; do not report a two-platform hosted pass for that revision.

P3 remains30/39 resource entries,1/4 whole groups/~68% partial checklist progress:
this completes storage customers, not the whole craftsman gate. Automatic craftsman
opportunities, qualified founding and gradual training remain next, then livestock
physical integration, wider products and specialization acceptance. No new decision
is pending. Feature branch codex/phase3-storage-upgrades; main unchanged.
See STORAGE_UPGRADES.md and PROCESSING_WORKSHOPS.md.

## October 15 — trained bakers can now open real paid bread businesses

Implemented the approved competition rule: a bounded forecast of actual recent
paid food orders can justify a bakery even when existing suppliers cover them.
Current population, post-startup funds, shared food budgets and hauling still
limit the forecast. It creates no purchases, consumers, money or guaranteed sales.
Admins can edit/disable the lookback, market share and unit cap in economy.yml.
Old installed files retain their settings and uncovered-demand behavior.

Connected gradual first-baker study and the live opening loop. One eligible
unemployed adult earns actual baking experience through future supply-backed
practice. Food/material reserves, known opportunities, staffing and study limits
apply; absence/unknown evidence pauses, old queued time awards nothing, and a
taken first-bakery opportunity releases the learner. Paper confirms a real loaded
smoker on its main thread; the SQL owner then rechecks and saves the paid opening.
Production runtime never loads chunks or alters blocks. Additional bakery employee
hiring remains an explicit existing staffing-list opt-in; this opens the working
founder. Cakes, milk/egg recipes and broader product selection remain unfinished.

Schema 79 adds empty founder course/review tables and an indexed paid-food history
lookup. No old people, goods, cash, ownership or history are rewritten. Migration
failure/retry, enrollment/practice rollback, quiet-market averaging, current cash,
world/population limits, cancellation, backlog and old-config behavior are tested.
History review is bounded at 3,200 receipts and defers instead of inferring demand
from a partial sample. Existing real food-order rotation remains unchanged.

Validation: 14 new focused cases passed. Final Windows .\mvnw.cmd clean verify
passed 1,517 tests (1,515 unit and two packaged integration), zero failures/errors/
skips, in 7:30 at 22:47:57 ET. An added test initially rewrote household totals
inconsistently; corrected it to advance an ordinary assessed period. No application
rule changed for that fixture correction. All production/test Java stayed frozen
through final verification; subsequent edits were docs and the excluded probe.

Guarded Paper 26.2 build 129 passed at 22:49:48 ET: actual SQL-owner study with ten
real practice expenses, restart, normal Paper bakery cycle/point confirmation,
private supplier payment, timed bread, exact replay and absent-world pause.
The first helper follow-up incorrectly supplied an absent world for fresh marker
confirmation; the guard correctly declined. Corrected only the excluded helper
and reran. Initial schema 78-to-79 audit retained all 134 predecessor tables,
131 exactly; retry retained all 136/133 exactly. Both changed only three normal
clock/runtime/ore bookkeeping tables, with zero resident aging. Integrity/FKs pass.
One existing disposable chunk was held by a test-only ticket; the temporary air
block was restored and ticket removed. Server stopped, original properties
restored, helpers archived and backups retained. Known OSHI Windows counter warning;
no Civilizations/probe/listener failure on the passing run. Human server untouched.

Paid farm food/ingredient customers now satisfy their checklist entry: owned crop
surplus earns actual village, warehouse and bakery payments, with causal delivery,
protected reserves and conserved cash/grain in the 80-period chain and starter
checks. P3 is 30/39 resource entries, 1/4 whole groups, about 68% partial checklist
completion. This is not a finished bakery/livestock/production phase gate.
Next: general craftsman customers, physical livestock integration, remaining
industry/product scope and specialization acceptance. One decision is pending:
may real businesses buy limited storage upgrades from the craftsman before Phase 4?
No storage-upgrade rule or new craft customer is implemented yet. Main unchanged;
feature branch codex/phase3-bakery-customers. See PROCESSING_WORKSHOPS.md.


## October15 — paid private bakery starters and replay-safe schema78

After backing up2522ee8, continued on codex/phase3-bakery-private-inputs. Added a
narrow actual farm/warehouse grain quote as fallback to preserved public surplus.
Real capital pays the source; qualified current staff/world/catalogue/stock and
retained reserve are required. Stock/world/registration filtering precedes a
bounded64-source review; overflow defers with no partial selection. No new cash,
public procurement, player container access or ownership conversion.

Schema78 adds one empty private-starter receipt table with buyer-linked shipment
FK and seller/versioned terms. Startup and its first delivery clock, shipment,
payment and final receipt commit together; clock starts now with no past work.
Resolved the enrollment FK dependency in focused tests before the complete run.
Exact replay remains historical; changed source/price/recipe or public/private
route cannot create another payment/bundle. New test fixtures cover both kinds,
normal timed bread, rollback/retry, migration, source guard/cap and route protection.

Seven new executed cases. Focused56 passed. Initial full run revealed explicit
old-migration table lists and manual old-schema fixtures needing the new empty
table; updated those expectations/reconstruction, retaining all row comparisons.
Targeted84 migration cases passed. Final clean verify passed1,503 (1,501 unit/two
packaged integration), zero failures/errors/skips;7:07 at21:43:51 ET. Main source
remained unchanged through native verification.

Native private probe separately compiled; marker/hash/EULA/ports and consistent
schema77 save/JAR/properties backup guarded the authorized fixture. Paper26.2
build129 proof passed21:44:30 ET: real SQL owner, private payment/cargo, restart
replay, timed bread, paid food, absence. No world/entity mutations. Save upgraded
77→78: all133 predecessor tables retained,130 exact, only normal clock/runtime/ore
metadata changed; age delta0 and new private receipt table empty. Fixture stopped,
properties restored/helper archived. Existing OSHI counter warning only; no plugin/
probe/listener failure. Human server untouched; no main merge/push.

Recorded/sent a genuine commercial question before choosing training/opening
behavior: allow bakers to compete for existing paid food orders while farms cover
needs, or retain uncovered-demand-only investment? Current behavior unchanged,
answer pending. Status/progress/guide updated; P3 remains~67% partial. Passing
private-input feature is ready for its logical commit and authorized GitHub backup.

## October15 — qualified bakery startup and paid farm grain integration

Continued unfinished codex/phase3-bakery-investment from882819d. Added seeded
qualified founder selection, atomic current revalidation, replay/rollback guards,
SQL-owner preparation/acceptance and strict editable bakery/default bread settings.
New-file grain stock starts0; old config/catalogues never silently extend.
Fixed registered farms halting LocalIndustryDemand and warehouse producer review;
validated hybrid farm grain/food can supply actual paid workshop demand, never tools.
Added80-period farm→warehouse→bakery→village scenario tracking actual harvests,
stored/in-transit/processed grain, meals and total cash. Fixed starter purchases
overlooking old grain_stocks; shared public balance view debits canonical harvest
stock first, preserves generic stock/history and rolls both ledgers back together.

Fourteen additional executed cases. Focused143 passed; actual SQL-owner10 passed.
Initial full run caught a stale new-file storage expectation; amended it with an
explicit installed-catalogue opt-out check (targeted3 passed). Final clean verify
passed1,496 (1,494 unit/two packaged integration), zero failures/errors/skips;
7:35 at21:10:54 ET. Main production code remained unchanged afterward.

Excluded native probe separately compiled against the packaged artifact; checksum/
marker/EULA/port guards and consistent backups preceded launch. Paper26.2 build129
proof passed21:12:10 ET for packaged defaults, actual SQL owner, saved grain, timed
bread, paid food, historical replay and absence. No helper world/entity mutations.
Main save audit retained all133 tables,130 exact, only normal clock/runtime/ore
metadata changed; ages unchanged. Properties restored, fixture stopped and helper
archived. Existing Windows OSHI counter warning; no plugin/probe/listener failure.
Human server untouched; no migration (schema77), main merge or main push.

Status/progress/processing guide updated. Automatic private starter purchase,
founder study and opening loop remain next; no new product question blocks them.
P3 stays1/4 groups/29-of39 resources/~67% partial; no completed gameplay gate claimed.

## October13 — approved processing grants and bakery investment evidence

User approved bakery/craftsman shared startup grants: “Yes. For sure. Ok keep going”.
Created codex/phase3-processing-startup from c3c9dd7. Extended default/new-file
eligibility only, preserving all existing spending caps, food protection and
installed opt-outs. Updated the decision and admin documentation.

Added BakeryOpportunities as a SQL-owner read-only first-firm review: fresh smoker,
explicit meal recipe, protected grant, affordable paid surplus inputs, actual
funded meal/reserve customer after the full startup debit, and available qualified
local food supply first. Supplier cap overflow defers; retained old bakery identity
cannot be bypassed. No reservation, skill, goods, money, block or automatic firm
created. Qualified founder/loaded marker/atomic acceptance remain next.

Twenty additional executed cases: four extended industry grant parameters, one
configuration case and15 processing/prospect cases. Full-table snapshots prove
rejection/late-anchor rollback and read-only reviews; paid cargo/processing/village
food sale conserve assets across restart/replay. Skills are not waived by grants;
paused sellers cannot trade. Fixture recipes/stock/skills live only in TempDir.
Focused128 and135 passed; subsequent full .\mvnw.cmd clean verify passed1,482
(1,480 unit/two packaged integration), zero failures/errors/skips;8:20 at16:56:17 ET.
Final15-case class passed5.363 seconds in that full run. Production source frozen
through verification. Reviewed diff and whitespace checks.

No schema migration (still77), catalogue transition, deployment or server/world/DB
mutation. No new Paper adapter; native test not rerun for this SQL/config batch.
Live founding/product defaults, ongoing chains, craftsman demand and livestock
reconciliation remain. P3 stays1/4 whole groups/29-of39 resources/~67% partial;
no full gameplay milestone claimed. Status/progress/plan and processing guide
updated. Feature commit/authorized GitHub backup follow verification; main untouched.

## October13 — GitHub backup and approved bakery/craftsman markers

User authorized GitHub backup: pushed codex/phase3-livestock-protection through
ce98f79 and verified its remote SHA. Main unchanged. User approved recommended
smoker/crafting-table recognition. Continued on codex/phase3-bakery-craftsman-markers:
four-kind bounded observation, industry mapping, loaded point checks and editable
YAML; old lists preserved, no inventory access/ownership or invented firms/goods.
Scoped candidate kinds before budgets to prevent new markers crowding out first
smith/warehouse opportunities. Schema77 retains parent/child history, indexes and
triggers; copy before restoring triggers avoids artificial audit history. Refuse
unknown inbound foreign keys before DROP can cascade into extension data.

Eleven tests added. Initial focused45 passed, final focused47 and targeted13 passed.
Restarted obsolete compiled runs while adding the two safeguards. First complete
run caught a quarry diagnostic contract (expected "foreign keys") and one ten-second
pulse timeout in the long farm/tool chain. Aligned diagnostic wording; unchanged
timeout case passed targeted rerun in7.987 seconds. No timeout increase/behavior
weakening. Final clean verify passed1,462 (1,460 unit/two packaged integration), zero
failures/errors/skips;8:10 at16:07:56 ET. Production source unchanged afterward.

Native marked Paper26.2 build129 proof passed16:09:31 ET: four real blocks, unchanged
own barrel/smoker items, empty-frame clearing/history, absent world/restart, zero
firms, restored four blocks and released chunk ticket. Server stopped/settings
restored/helper archived. Schema76-to77:133 old tables retained,128 exact; three
normal bookkeeping changes plus two farm timestamp refreshes. First audit refused
those timestamps; explicit field comparison proved all crop/identity/location/
completeness fields unchanged before accepting the result. No blanket whitelist.
Known Windows OSHI counter warning; no plugin/probe/listener errors.

Human server untouched; no main merge/push. Actual bakery/craftsman founding,
products/replacement and live livestock remain. Marker choice resolved; asked new
public startup-grant extension decision before automatic founding. P3 stays~67%
partial. This passing batch is prepared for authorized feature-branch GitHub backup.

## October13 — persist herds and real paid livestock output

Added schema76 and SQL-owner `LivestockRepository`: initial owned UUIDs cannot seed
multiple herds; real feed, qualified work, owned milk/eggs and cohort checkpoints
commit together. Receipts record constrained before/after feed/product balances,
actual available work/output space and an explanation. Historical retries/restarts cannot duplicate goods or feed costs.
Old hybrid crop-farm catalogue/labor and all existing assets remain unchanged.
Automatic adoption/physical births/deaths/live scheduling remain incomplete, and
slaughter is not exposed until physical retirement can prevent double harvesting.

Added twelve temporary-SQLite cases including240 periods/replay/restarts and real
paid feed/milk deliveries. Focused40 passed. First focused run caught an old schema
assertion; next caught a fixture expecting two adults when its young animal had
matured. Corrected expectations without weakening production guards. Full regression exposed older migration
comparisons missing the four new empty tables; extended their emptiness assertions
while preserving exact predecessor-row comparisons. An intermediate rerun was
stopped after the seventh comparison had already compiled; final clean verification
was restarted against all corrected sources. Added constrained before/after stock
receipts and explanations before that final compilation. Full .\mvnw.cmd clean verify passed1,451 (1,449 unit/two packaged integration),
zero failures/errors/skips;8:06 at15:16:14 ET. Guarded Paper26.2 build129 packaged
SQL backend proof passed15:17:04 ET: actual feed/cohort/products, restart replay
and absent-world pause. Schema75-to76 audit retained every predecessor table;
126/129 exact, only normal clock/runtime/ore bookkeeping changed; no aging.
New main-fixture herd tables empty. Fixture stopped, properties restored and helper
archived. No world/entity mutations by the helper; no client/physical birth proof.
Existing Windows OSHI performance-counter warning appeared; no Civilizations,
probe or listener error. No human server changes. P3 stays1/4 groups/~67% partial.
Bakery/craftsman marker answer remains pending; no human server/main merge/push.

## October13 — approved herd authority, cohort calculator and village-only breeding guard

User approved simulation-managed breeding/slaughter for village-owned livestock.
Implemented deterministic active-period young cohorts, paid feed/labor birth costs,
cooldowns, gradual maturity, head/capacity limits and protected adult slaughter.
Capacity loss preserves existing animals; absent worlds/disabled mode preserve the
exact state. No food-linked death, spoilage, invented feed or consumer is introduced.
This calculation is not a persistent herd ledger or connected production yet.

Added optional strict livestock YAML with editable cow/chicken care/output/birth
terms. New-file defaults enable control for positively saved village-owned cows/
chickens; absent old sections disable without rewriting. SQL owner loads immutable
village animal IDs at startup, excluding player ownership. Main-thread event guard
blocks outside vanilla births for either village parent and intercepts breeding
food in either hand before vanilla item consumption; buckets/names/leashes remain
available. No SQL/world scan per event. Adoption/transfer publication still needs
its future commit/physical-claim handshake; no live adoption API exists yet.

Fourteen added cases: six herd calculations including2,400 active periods/replay/
head/feed/work/storage conservation, six event/interaction guards, one strict YAML
case and one immutable village-only SQL registry case. Focused21 passed. Full
Wrapper .\mvnw.cmd clean verify passed1,439 (1,437 unit/two packaged integration),
zero failures/errors/skips,7:37 at13:12:55 ET. Guarded Paper26.2 build129 proof
passed13:44:11 ET: actual SQL-owner cached identities, actual food tags and synthetic
birth/interaction dispatch with real temporary cows/chickens and a fake player.
Verified village birth/feeding blocks, player/other-item availability and unchanged
feeding stacks. First fixture had missing fake-player PDC and listener errors;
audit refused its own pass marker. Fixed only the test helper and reran cleanly.
Removed temporary actors, stopped fixture, restored properties and archived helpers.
Retry audit verified one normal month: two living residents aged one, dead/history/
money/assets retained;123 other predecessor tables exact. No client/packet or
actual vanilla breeding-cycle proof is claimed. No broad profiling claim.
Schema remains75; no migration or human server edits. Existing saved village
identities are the only control evidence; cold startup/failed readiness leaves
control inactive until the identity view is ready. Livestock ledger/startup/death
reconciliation, safe adoption, signs/transfer UI and actual products remain next.
P3 remains29/39 resources,1/4 whole groups/~67% partial; no herd gate claimed.
Next requested decision: editable smoker/crafting-table recognition for bakeries
and craftsman shops, or explicit designated workplaces. This extends the specific
smithing-table/barrel approval and is not silently selected. See
LIVESTOCK_PROTECTION.md. No merge or push.


## October13 — saved protective pens and conservative flat-pen evidence

Implemented confirmed protective pen storage and atomic initial animal ownership,
plus immutable Paper snapshot classification for simple flat pens. A pen save
records the finite footprint, sign coordinate, owner and initial roster; overlaps,
changed replay inputs or another owner's animal cancel the whole transaction.
Existing same-owner animal provenance is retained. Disconnected/forged geometry
and distant signs are refused. Saved protection does not depend on today's sign.

Schema75 adds initially empty protected_pens, protected_pen_columns and
protected_pen_animals. Old-world/owner/history rows stay intact. Eight new
SQLite cases passed within focused91 older-save checks; six Paper-source cases
passed within focused29 geometry/preview/store checks. Full Windows Wrapper
.\mvnw.cmd clean verify passed1,425 (1,423 unit/two packaged integration), zero
failures/errors/skips,7:54 at12:25:29 ET. Guarded Paper26.2 build129 check passed
12:29:13 ET: closed/open gate exact footprint, missing fence and unknown snapshot.
Initial native check stopped before edits because empty server had no loaded chunks;
fixed the test harness to explicitly prepare one chunk (production source never
loads chunks). All temporary sky-pen blocks restored; no animals edited. Both
migration73→75 and retry schema75 save audits preserved every predecessor row
except ordinary civilization_time/economy_runtime/ore_tracking updates. New ownership/
pen tables remain empty in primary fixture, integrity/FK healthy. Original properties
restored, backups retained, helpers archived, fixture stopped. Human server untouched.
This is verified backend/classifier work, not complete live livestock/sign adoption.

The Paper source is an unwired read-only adapter over caller-captured immutable
loaded chunk snapshots. It certifies common full-cube flat footing/two air blocks,
connected fences/gates or two-block full-cube edges. Open gates count structurally;
isolated/disconnected fences, missing chunks, ramps/slabs/doors/walls and unsupported
materials stay unknown. It cannot certify ownership or land-claim permissions.
No live listener/menu/adoption, donation transfers, breeding or player server edits.
Adjacent pens sharing boundary columns conservatively conflict for now.

Pending user rule: simulation-managed breeding/slaughter for village-owned animals
versus retaining vanilla breeding and observing it. This affects live herd control;
player animals stay ordinary and cannot be taken. Pen verification proceeds while
pending. P3 remains29/39 resources,1/4 whole groups/~67% partial; livestock entries
stay incomplete. See LIVESTOCK_PROTECTION.md. Feature branch unchanged; no push/merge.


## October13 — persistent initial livestock ownership (schema74)

Continued after the verified protection-domain commit1873626. Added immutable
first ownership provenance keyed by animal UUID: origin world, player or village
owner, time and source. Exact retries replay; conflicting owner/provenance is
refused. No observation, escape, sign break or restart is an ownership transfer.
Known world and actual same-world town are required for village registration.
Public writes own a transaction; internal rows compose with future pen transactions.
Late failures roll back; no silent overwrite, fabricated animal or cash is created.

Schema74 adds an initially empty animal_ownership table/index. It does not adopt
existing animals or change predecessors. Seven temporary-DB tests cover restart,
retry/conflict refusal, world/town constraints, late failure/recovery, composed
rollback, migration73 failure/retry/preservation and invalid owners. Focused94
passed, including affected older-save fixtures. Full Windows Wrapper
.\mvnw.cmd clean verify passed1,411 cases (1,409 unit/two packaged integration),
zero failures/errors/skips,8:05 at11:37:25 ET. No playable server or non-test
database changed. Diff reviewed; migration exercised only in temporary test DBs.

Live sign claims/particles/menu, persistent pen footprints, authority integration,
explicit audited ownership transfers, breeding cohorts, observation and live
startup/production remain next. First records are trusted backend inputs, not a
player-accessible claim shortcut. P3 remains29/39 resources,1/4 whole groups/~67%
partial; incomplete livestock entries stay unchecked. No decision is pending.
See LIVESTOCK_PROTECTION.md. No push/merge; feature branch unchanged.


## October13 — livestock protection and bounded work foundation

Approved automatic free-range adoption and authorized per-pen sign preview/
confirmation direction, resolving the earlier livestock ownership question.
Implemented plain-Java bounded incremental enclosure detection, actor/world/level/
expiry-bound previews, fresh evidence/membership/authority checks, saved-ownership
priority eligibility and feed/labor/storage/reserve-bounded livestock work.
Twenty-six new focused tests passed, including13,122 small work combinations.
Full Windows Wrapper .\mvnw.cmd clean verify passed1,404 cases (1,402 unit
and two packaged integration), zero failures/errors/skips,9:13 at11:24:55 ET.

This is backend preparation, not live adoption. No Paper sign listener/menu/
particles, persistent pen/animal records, breeding cohorts, actual herd startup,
config section or claims-plugin integration yet. Schema remains73; no world,
animal or human server changes. P3 checklist remains29/39 resources,1/4 whole
groups/~67% partial: neither livestock entry is complete. Next: saved ownership
and safe loaded-world recognition before enabling player interactions/adoption.
No consequential product decision is currently pending.
See LIVESTOCK_PROTECTION.md. Branch codex/phase3-livestock-protection; no push/merge.


## October 13, 2026 — hybrid farm contributions and actual paid surplus

Implemented the approved hybrid farming model on codex/phase3-farm-market.
Future actual basic food contributes at most an editable share toward real
meals/reserves; remaining accepted food/grain is business-owned. Old public
food, grain, identities and period history stay public/intact. Collective food
orders now pay the farm through existing real budget/stock/cargo checks; no
salary wallets, free money, duplicate food or confiscation. Grain buffer prevents
unsold ingredients crowding all basic food out. Storage backpressure idles work
rather than bypassing the contribution cap. Default50% share, two reserve periods,
256 commercial spaces plus one protected toolkit slot,16-unit grain target.
Installed files opt out without rewriting; configured grain rates/yields remain.

Transactional schema73 adds initially empty farm_market_stores/harvests. Empty
or recognized one-kit/zero-cash legacy farm stores adopt explicitly during a
successful food transaction, preserving tools/cargo and prior opening terms.
Unknown private stores are refused. No capital is generated; store opening
capacity stays saved. Future allocation/rates/targets tune with existing audited
settings transitions and backlog protection. Harvest proof, owned stock, public
food and checkpoint commit together; failed periods retry without repeating the
already successful business startup. Tools/skills use actual accepted work.

Fixed a confirmed grain regression: public tool funding compared food production
with total harvest learning, incorrectly blocking productive grain farms. It now
checks the actual shared harvest. Public kit invoices also preserve the farm's
real sales cash, and one-kit quota includes pending cargo despite larger storage.

Eighteen added cases: five pure allocation cases,11 temporary-DB cases, one YAML
case and one grain-tool eligibility regression. Includes200-period paid-food
conservation, actual capped output under overpopulation, cash poverty/no free
orders, migration failure/retry, late write/adoption rollback, legacy tool/public
stock/history preservation, absent-world/restart, unknown store refusal, paid kit
income preservation and an actual funded grain-delivery/bread/public-food order
backend chain. Initial test compile found one compound var declaration; corrected
test syntax without changing production guards. Focused200 passed before two
additional chain/refusal cases; final targeted36 passed. Full Windows Wrapper
.\mvnw.cmd clean verify passed1,378 (1,376 unit/two packaged integration), zero
failures/errors/skips,7:14 at09:46:44 ET. No broad multi-town performance claim.

Guarded Paper ordinary-owner timer/customer proof passed09:47:38 ET: six settled
food periods, capped contributions, real private food/grain and two paid200 food
orders, one pending owned cargo, actual farm skills and resumed work after
absence/restart. Conserved17500 cash after2500 startup from explicitly configured
20000 fixture opening funds. Trusted crop/site inputs are not natural discovery;
funded fixture does not prove every tiny starting village can afford the loop.
No blocks, entities or chests edited; no chunk tickets. Main save audit120/123
predecessor tables exact; three ordinary clock/economy/ore changes, all identity,
assets/money/history retained. Schema73 integrity/FK healthy; new main market
rows empty under old-file opt-out. Original properties restored, helper archived,
consistent backups retained; playable human server unchanged. No merge or push.

Recorded expanded P3 scope: livestock, real bakery ingredients and one general
craftsman, with no spoilage or separate craft professions. Six new resource entries
expand the old33-entry group to39. This batch verifies hybrid allocation:29/39;
whole groups still1/4 and equal-group partial checklist now~67% under NEW scope,
not the previous70% denominator. Automatic livestock/bakeries/craftsman, further
resource founding and specialization/long-run acceptance remain unfinished.
Paid bakery chain is backend fixture proof, not automatic shop founding.

Next livestock startup requires the user's ownership decision: existing nearby
chickens/cows (including player pens) automatic adoption versus explicit opt-in.
Recommend opt-in; no answer assumed and no animals changed. Village funds remain
finite; failed affordability creates a real shortage, not invented income.
See HYBRID_FARMS.md and DECISIONS_AND_OPEN_QUESTIONS.md.

## October 13, 2026 — protected actual future grain harvests

Staffed farms now allocate an editable part of actual future wheat work to separate
public grain. The same labor/output budget, complete typed wheat fraction, actual
meals plus future food reserve and finite grain storage limit it. Unknown/partial
fields, no wheat, no staff, food pressure, full storage and absent worlds cannot
mint ingredients. Default25% wheat share, two future food periods,256 grain space
and one grain unit per diverted work unit; installed files without the section
opt out and are never rewritten. Food/history are never converted or reseeded.

Transactional schema72 adds initially empty grain_stocks/grain_harvests. Food,
grain allocation/history/stock and checkpoint commit together; late faults roll
back everything. Historical yields remain saved; lower capacity preserves stock.
Existing forward tuning refuses unfinished old-policy backlog. Tool service and
farming learning use the same total actual harvest, including grain-only work;
there is no extra labor, free tool work or duplicate experience. Current-period
admin food diagnostics explain grain allocation/storage and basic food output.

Fourteen added cases: four pure calculations, nine temporary-DB runtime cases and
one strict YAML case. Covers food/reserve protection, wheat fractions/yields/space,
unknown/partial/unstaffed/disabled modes, replay/absence/restart, late failure,
settings/backlog transitions, shrinking wheat fields, grain-only tools/learning,
and migration71 failure/retry preserving old food. Focused226 passed before the
final grain-only case and work-log changes; final targeted37 passed. Full Windows
Wrapper .\mvnw.cmd clean verify passed1,360 (1,358 unit/two packaged integration),
zero failures/errors/skips,7:24 at08:40:41 ET. Helper precompile initially pointed
at the not-yet-packaged JAR; corrected the precheck classpath and recompiled
against the verified packaged JAR for the actual native run. No production fix
or weakened assertion was needed.

Guarded native ordinary-owner timer proof passed08:42:06 ET: actual funded farm
with two assigned workers; each8 future work split6 food/2 grain,2500 actual
startup debit from4800 opening funds, real farming skills, absence/restart pause,
then one resumed harvest. These are trusted explicit crop/site fixture inputs,
not a natural positive-field discovery claim. No blocks/entities edited or chunk
tickets used. Save audit115/121 predecessor tables exact; six ordinary calendar,
economy, tracking, active-world and resident-age changes. Existing identities,
assets, cash and history retained; schema72 integrity/FK healthy. Exact original
properties restored, helper archived and backups retained. Playable server unchanged.

Next bakery supply integration needs the user's answer on recurring paid public
grain sales. Prior approval covers one-time starter material sales; no ongoing
market is assumed. Configurable real cash payments to village treasury with
training reserves are recommended. This question is recorded/pending. Automatic
quarry/pit founding, forestry, wider recipes/competition and specialization gates
also remain. Phase3 is still1/4 whole groups/~70% partial. No merge/push.
See GRAIN_HARVESTS.md and DECISIONS_AND_OPEN_QUESTIONS.md.

## October 13, 2026 — saved wheat-field evidence without food conversion

The bounded farm scanner now identifies wheat-on-farmland separately from other
food crops, retaining the same footprint/overlap/partial rules and read budget.
All growth stages remain planted capacity, not an immediate physical harvest.
Transactional schema71 adds farm_grain_evidence, initially empty for old worlds.
Legacy composition remains unknown; typed zero is distinct. Older/equal frames
cannot replace current counts, newer untyped evidence clears composition, and
partial evidence stays partial. Food output, stocks, purchases and history are
unchanged. This is bakery preparation, not harvested ingredients or bakery startup.

Seven added cases cover typed/legacy bounded scans, real classifier distinctions,
restart and preserved food/history, stale/zero/partial/unknown frames, late write
rollback/retry, version70 upgrades without invented grain, failed migration/retry,
and invalid subsets/corrupt frame pairing. Older-schema preservation tests now
explicitly expect the additional empty table. Focused116 passed; full Windows
Wrapper .\mvnw.cmd clean verify passed1,346 cases (1,344 unit/two packaged
integration), no failures/errors/skips,7:38 at08:19:23 ET. Guarded Paper probe
passed08:19:58 ET: ordinary observer matched actual empty terrain (zero crops/
wheat, not a positive field proof), persisted known zero over restart and retained
all24 food units. Positive wheat/material cases are covered by automated tests.
No blocks/entities changed; helper-only tickets released. Save audit116/120 old
tables exact with four ordinary clock/evidence changes; schema71 integrity/FK
healthy, existing identity/assets/money/history retained. Properties restored,
helper archived and backups preserved. Playable server unchanged; no merge/push.

Approved grain direction is recorded in canonical design/rules; stale shoreline
recovery implementation wording is corrected without changing its approved rule.
Next: actual staffed future grain harvest accounting, paid bakery ingredients,
automatic quarry/pit founding, forestry and specialization acceptance. Phase3
remains1/4 whole groups/~70% partial; no gameplay question pending.
See GRAIN_EVIDENCE.md. This batch does not finish a broader industry milestone.

## October 13, 2026 — bounded loaded-neighbor mineral observations

The ore observer now uses the centre and at most four already-loaded cardinal
chunk snapshots. Missing faces remain unknown; no chunks are forced or neighbor
resources included as centre members. Separate quarry classification can now
confirm exposed boundary stone/sand without guessing about missing faces.

Six new tests cover exposure, classifier separation, missing faces, identity,
negative coordinates and overflow. Full Windows Wrapper clean verify passed
1,339 cases (1,337 unit/two packaged integration), zero failures/errors/skips,
7:16 at04:22:30 ET. The marked disposable Paper probe passed08:04:41 ET:
five actual snapshots matched32 independently checked stone/sand positions,
and the ordinary observer/SQL owner saved current ore evidence. No blocks or
entities edited; helper-only tickets released. Save audit116/120 tables exact,
four normal clock/evidence changes; identity/assets/cash/history preserved,
SQLite integrity/FK healthy. Exact properties restored and helper archived.

No migration: schema70. At most five snapshot captures cost more than one;
broad multi-town profiling remains open. Automatic quarry surveys/founding,
forestry, wider recipes and specialization remain unfinished. Grain accounting
is approved but not implemented: preserve old food, track future ingredients
separately and retain basic meals without a bakery. Phase3 remains1/4 whole
groups/~70% partial; no gameplay question pending. No merge/push or playable
server changes. See MINERAL_NEIGHBORHOOD.md.

## October 13, 2026 — live finite quarry and sand-pit operation

Existing funded/staffed quarry and sandpit firms now enroll in the normal SQL-owner
industry timer. Resource-specific editable rates apply: stone six batches/worker,
three units/batch; sand twelve/four. Finite saved source shares/yields, storage,
actual workers, tools and history-trust guards remain authoritative. New productive
work earns bounded mining experience. Missing old settings opt out; disabling saved
clocks records paused periods without backfilling production on reenable. Work-speed
retuning does not rewrite abundance/yields or refill deposits. No migration: schema70.

Fourteen new temporary-database cases (both kinds) cover work/replay/absence/restart,
disabling/old-file opt-out, capture gaps, per-block loss with other shares working,
late checkpoint rollback and speed-retuning invariants. Test setup initially omitted
tracking initialization before a simulated gap; corrected the fixture, retaining the
guard. Focused120 passed before the two final opt-out cases. Full Windows Wrapper
.\mvnw.cmd clean verify passed1,333 (1,331 unit/two packaged integration), zero
failures/errors/skips,7:22 at03:37:47 ET. Guarded native ordinary SQL-owner timer
probe passed03:39:45 ET: two explicit funded/qualified firms produced18 stone/48
sand, spent exactly18 finite batches, gained actual skills, conserved startup money
and retained state over absence/restart. Sites/actors/resources are trusted explicit
fixtures, not automatic terrain discovery. No blocks/entities edited or chunks forced.
Save audit117/120 tables exact; three ordinary clock/economy-runtime/ore changes,
identity/assets/cash/history retained, integrity/FK healthy. Exact properties restored,
helper archived and consistent backups retained. Your playable server is unchanged.

Next: actual family-specific quarry/pit surveys and real customer/site/founder selection,
renewable forestry, wider recipes and competition/specialization acceptance. Existing
vacancy services remain configurable; this batch does not claim automatic new firms.
No gameplay question pending. Phase3 remains1/4 whole groups/~70% partial. No merge/push.
See TIMED_QUARRIES.md and FINITE_QUARRIES.md.

## October 13, 2026 — automatic first fishing-owner training and paid food chain

Connected normal water/bank observation to one real future fishing-owner course when
no qualified owner exists. Permanent world/habitat identity, current protected funding,
real funded food need, safe-bank proof, actual adult availability, practice supplies and
study budgets apply. Enrollment gives no skill, job or money. Real future timber practice
qualifies the owner before the unchanged paid startup path opens the fishery. Unknown,
stale, unfunded or disabled opportunities pause courses; redundant sponsorship is refused.
Migration 70 adds one initially empty course association table, retaining all old rows.

A scoped read-only training preview may validate exactly one currently processing food
period. Actual purchases and startup still require a complete current assessment without
backlog. This distinction is necessary to prevent linked practice from pausing forever
mid-period; it cannot authorize old purchases or manufacture demand/funds. Tests verify
this boundary, original safe footing, enrollment/practice rollback, stale proof, backlog,
restart/absence and policy disabling. Eleven new temporary-database cases. Focused 69
passed before the two final guard cases; full Windows .\mvnw.cmd clean verify passed
1,319 (1,317 unit/two packaged integration), zero failures/errors/skips, 6:50 at 02:36:45 ET.

Final guarded native first-owner chain passed 03:05:00 ET: zero initial fishing skill,
ten real timber practice steps, qualification, ordinary protected startup, catches and
further earned experience, an actual paid public food order delivered once into reserve,
conserved money, saved bank relocation under read-only damage masking, absence pause
and restart. Native setup/clock/observation assertions were corrected after they exposed
seed mismatch, an overpaced clock, already-earned work and split payment snapshots;
production seed, purchase, backlog and money guards were not weakened. Consistent
read snapshots and fully observed/settled baselines distinguish real behavior from test
races. Native actors/farm/practice stocks/treasury are explicit isolated fixtures; actual
water/banks are read from Paper. This is not a natural survival-balance or scale gate.
Save audit 113/120 tables exact; seven ordinary clock/freshness/ore/age changes only,
identities/assets/cash/history retained, integrity/FK healthy. Exact settings restored,
helpers archived and consistent backups retained; no helper block/entity edits.

The verified mineral/tool and food/practice customer/recipe milestone is now checked;
broader catalogs remain separately open. Phase 3: 1/4 whole groups/~70% partial
(4/4 business foundation, 28/33 resources, 14/15 lifecycle, 0/5 specialization/acceptance).
Next: renewable forestry and remaining stone/sand/bakery connections, competition and
multi-year geographic scenarios, broader native/profile/client acceptance. No gameplay
answer pending. Your playable server is unchanged; no merge/push.
See FISHERY_FOUNDER_TRAINING.md, FISHERY_WORKPLACES.md and FISHING_SKILLS.md.

## October 13, 2026 — earned fishing skills and real-vacancy study

Connected fishing to configurable productive-work experience, supply-backed fishing
courses, automatic existing-vacancy study and ordinary qualified staffing defaults.
Actual catches earn bounded experience; zero catches/full storage/unsafe banks,
absence, disabled/unmapped policies and historical replay do not. Catch, habitat,
skill, audit and checkpoint roll back together. Existing vacancies can sponsor real
future timber practice with protected food/material reserves, then hire qualified
learners. Enrollment creates neither skill nor employment. No ownership waiver.
Existing installed maps/lists and accepted course terms remain unchanged; admins opt
in explicitly. No migration added: schema 69 retained.

Six added temporary-database cases cover work/replay/restart/absence, damage recovery,
late learning rollback, disabled/unmapped work, full storage and actual practice-to-
hiring. Focused 120 passed before adding the storage case; test setup initially had
an import/catalog mismatch, corrected only in the fixture, preserving application
inventory guards. Full .\mvnw.cmd clean verify passed 1,308 cases (1,306 unit/two
packaged integration), zero failures/errors/skips, 7:24 at 02:11:40 ET.
Guarded native ordinary startup/catch/earned experience/recovery/accounting/absence/
restart probe passed 02:12:59 ET. Save audit 112/119 tables exact; seven ordinary
clock/freshness/ore/age changes, identity/assets/cash/history retained, integrity/FK
healthy; exact properties restored and helper archived. No helper block/entity edits.

Automatic training of the FIRST fishery founder is still unfinished: existing-vacancy
study cannot bootstrap a nonexistent business. Next is a durable real habitat/bank
opportunity association for that course, then remaining basic industries and final
specialization/scenario/profile gates. No gameplay decision pending. Your playable
server is unchanged; no merge/push. Phase 3 remains 1/4 whole groups/~69% partial.
See FISHING_SKILLS.md and FISHERY_WORKPLACES.md.

## October 13, 2026 — automatic qualified shoreline fisheries and saved recovery

Connected the ordinary main-thread water observer to qualified first-fishery startup,
exact live support/depth/open-sky and dry-bank checks, final standing-point recheck,
and atomic real protected startup funding, first job, empty storage and habitat.
Migration 69 adds two initially empty shoreline binding/history tables; legacy
resources remain intact and unbound. Saved recognition bounds survive retuning.
Loaded original habitat/bank checks relocate to a suitable nearby bank, pause catches
on confirmed unrecoverable damage, and retain the last confirmed state on unknown
blocks. Capacity loss/restoration and bank movement commit together, with no instant
fish refill. Stable frames do not add unlimited timer-history rows. No blocks,
containers or player buildings are changed; physical repair/docks remain Phase 4.

Full Windows .\mvnw.cmd clean verify passed 1,302 cases (1,300 unit/two packaged
integration), zero failures/errors/skips, 7:36 at 01:53:19 ET. Corrected an accelerated
native helper race by waiting for the real industry clock registration before advancing
fixture time; no production behavior was changed to accommodate the helper.
Final guarded Paper probe passed 01:58:12 ET: ordinary automatic startup, catches,
real money conservation, persisted same-habitat relocation under a read-only damage
mask, absent-world pause and restart. Fixture actors, skill and treasury are explicit
setup; automatic first-owner skill acquisition is not claimed. No helper blocks or
entities were edited. Save audit 110/119 existing tables exact; nine reviewed ordinary
clock/freshness/ore/population changes included a real simulated external arrival.
Existing identity, assets, cash and history retained; integrity/FK healthy, exact
settings restored, helpers archived and consistent backups retained. Your playable
server was untouched; no merge/push. See FISHERY_WORKPLACES.md.

Phase 3 remains 1/4 whole groups/~69% partial: the broader resource catalog and final
specialization gate remain incomplete. Next: fishing skill/course and vacancy wiring,
automatic first-owner training, other basic industries, competitive multi-year
scenarios and broader native/profile acceptance. No gameplay answer is pending.

## October13, 2026 — approved adaptive shoreline recovery planner

User chose nearby replacement or repair rather than permanent fixed-spot pausing.
Recorded in canonical design/simulation/decisions. ShorelineScan now exposes immutable
per-position safe/blocked/unknown evidence; ShorelineRecovery plans KEEP, nearest
bounded RELOCATE, WAIT_UNKNOWN or NEEDS_REPAIR around the same original habitat.
No closure, new resource claim, fish refill, cash/job changes or block edits occur.
Confirmed damage is distinct from missing evidence; unknown original geometry must
preserve last verified operational state and normal unloaded-chunk data simulation.
Caller-supplied rise/distance, strict read budgets, deterministic ties and overflow
protection apply. Repairs/docks still need Phase4 funding/labor/materials/protection.

Seven added headless cases; focused12 passed, zero failures/errors/skips,12.766s
at00:00:11 ET. An initial full run was intentionally interrupted when review found
that fresh-safe evidence needed explicit separation from operational permission;
validation restarted after clarifying confirmed damage versus unknown flags.
Full Windows .\mvnw.cmd clean verify passed1,287 (1,285 unit/two packaged
integration), zero failures/errors/skips,7:02 at00:07:16 ET.
Guarded native scheduled water/SQL-owner/read-only shoreline probe passed at
00:08:10 ET:220 independently checked water columns,11 actual safe banks, loss-mask
selection of an independently checked safe replacement and unknown-mask waiting.
Strict128 cell checks/tick, newer capture, absent-world pause and restart passed.
Damage was represented by read-only input overrides; no helper blocks/entities were
changed and no actual relocation was persisted. Normal shutdown00:08:10. Save audit
113/117 tables exact; four ordinary clock/economy/farm-freshness/ore changes only,
all identities/assets/cash/history retained. Schema68 integrity/FK healthy; exact
settings restored, helper archived, helper-only chunk ticket released and consistent
backups retained.
No migration (schema68), persisted workplace binding, automatic founding/recovery
scheduler or physical repair is enabled. Next: save/validate opening shoreline and
habitat recognition terms, final acceptance and qualified training/startup scheduling.
The shoreline decision is answered; no new question pending. Human playable server
unchanged; no merge/push. Phase3 remains1/4 whole groups/~69% partial.
See SHORELINE_RECOVERY.md for the exact implemented/planned boundary.

## October12, 2026 — qualified read-only fishery owner preparation

FisheryInvestment chooses an actual qualified living local unemployed adult from
a bounded UUID-sorted page using stable seeded sampling. Server adulthood, saved
fishing experience and both owner/first-worker requirements apply; active learners
and existing workers cannot be reassigned. Unlimited ownership is preserved.
Proposal contains current funded water evidence and qualification terms but no
invented shoreline anchor, new job, resource, money, skill or reservation.

Optional strict fishery-founding prototype and SQL-owner/plugin configuration wired;
missing old sections opt out. This is preparation only, not automatic founding or
fishing training. Current study defaults still lack a fishing course. Seven added
temporary-database cases plus one config case; focused95 passed, zero failures/
errors/skips,12.799s at21:54:16 ET. An initial study test had a fixture catalog mismatch;
corrected its explicit transactional enrollment without weakening production guards.
Full Windows .\mvnw.cmd clean verify passed1,280 (1,278 unit/two packaged
integration), zero failures/errors/skips,7:28 at22:03:19 ET.
Guarded native SQL-owner owner-preparation/first-firm identity safeguards passed
at22:04:33 ET, alongside ordinary reserve/practice purchases, real money/goods
conservation, delivery, absence pause and disabled-order restart. This proves safe
API integration/identity rejection, not native positive founding or training.
Normal shutdown22:04:33; save audit111/117 tables exact, six ordinary clock,
demographic/resident-age, active-world, economy-runtime and ore-tracking changes;
all identity/assets/cash/history retained. Schema68 integrity/FK healthy, exact
settings restored, helper archived and consistent backups preserved.
No migration (schema68), automatic scheduler or dock work. Removed-shoreline
standing-point rule still awaits the user; no elapsed time is approval. Next after
that choice: current anchor/habitat acceptance, saved recognition terms, appropriate
supply-backed fishing training and scheduling. Human playable server untouched;
no merge/push. Phase3 remains1/4 whole groups/~69% partial, broader catalog/
competition/specialization and acceptance still open. See FISHERY_OWNER_PREPARATION.md.

## October12, 2026 — bounded funded first-fishery prospects

FisheryOpportunities connects current connected unclaimed water, real food customers
and food-protected startup feasibility in a read-only SQL-owner review. Proposed
startup/capital costs are deducted in a demand preview before checking customers,
preventing counting the same public money twice. Actual eligible staffed local
food stocks reduce remaining need. Over-budget supplier sets defer conservatively;
all chosen water supports fit the saved town range. Saved fishery identities cannot
be bypassed by first-firm bootstrap. No assets, jobs, grants or resources are created.

Optional strict fishery-opportunities config and normal plugin/PopulationSession
wiring added; missing old sections opt out. Numeric editable prototype terms in
FISHERY_OPPORTUNITIES.md. Seven temporary-database cases and one configuration case
added; focused122 passed, zero failures/errors/skips,23.432s at21:37:53 ET.
Full Windows .\mvnw.cmd clean verify passed1,272 (1,270 unit/two packaged
integration), zero failures/errors/skips,6:55 at21:46:00 ET.
Guarded native new SQL-owner first-firm identity/configuration safeguard passed
at21:46:54 ET, alongside ordinary emergency-food/practice purchases, money/goods
conservation, delivery, absence pause and disabled-order restart. This native check
does not claim a newly founded fishery or positive live economic candidate. Normal
shutdown21:46:54; main-save audit114/117 tables exact, only clock/economy runtime/
ore changes; identity/assets/cash/history retained. Schema68 integrity/FK healthy;
exact settings restored, helper archived and consistent backups preserved.
No migration (schema68), automatic founding/training or physical dock work enabled.
Next: qualified owner/training preparation, fresh shoreline/water acceptance and
normal startup scheduling. The removed-shoreline standing-point rule is now awaiting
a user decision; preparation can continue independently. Human playable server
unchanged; no merge/push. Phase3 stays1/4 whole groups/~69% partial; broader renewable
catalog, competition/specialization and final acceptance remain unfinished.

## October12, 2026 — approved emergency food reserve customers

User approved a needs-based reserve. VillageSupplyPolicy and VillageSupplyPurchases
now target current unmet food plus actual assessed demand times an editable future
reserve, counting existing public food, paid public cargo, donations and reserved
player purchase/barter food. Bundled economy.yml uses two periods;0 or a missing
new food-stock-reserve-periods key preserves shortage-only behavior. This is separate
from food-reserve-periods cash protection. Strict0–100 validation, overflow-safe
targets, existing budgets/haul/minimum cash and actual seller eligibility remain.
No money, consumption, stock or customer is fabricated. Existing shortage-only
regressions explicitly retain that configuration; the fishery catch/arrival case
now verifies a legitimate second reserve purchase after hunger is satisfied.

Eight added automated cases include healthy funded customer plus protected startup,
real startup/catch/payment, donations/cargo, rollback/replay, budget exhaustion,
current population/world/cash guards, normal runtime buying before shortage and
strict editable/missing/invalid settings. Focused124 passed, zero failures/errors/
skips,22.725s at21:23:49 ET. Initial test runs exposed obsolete shortage-only order
expectations and a fixture configured after runtime use; corrected fixtures and
assertions, without relaxing production guards.
Full Windows .\mvnw.cmd clean verify passed1,264 (1,262 unit/two packaged
integration), zero failures/errors/skips,7:04 at21:31:29 ET.
Guarded native ordinary SQL-owner timer probe passed at21:32:09 ET: healthy
village bought emergency food, delivered/consumed actual goods, conserved real
cash, paused absent world and resumed with new orders disabled after restart.
Normal shutdown21:32:09; main-save audit114/117 tables exact, only ordinary clock,
economy runtime and ore-tracking changes; identities/assets/cash/history retained.
Schema68 integrity/FK healthy, exact settings restored, helper archived and consistent
backups preserved. No helper world blocks/entities/chunks changed.
No schema migration (schema68), automatic fishery scheduler or dock construction.
Human playable server unchanged; no merge or push. The reserve decision is answered;
next is automatic qualified shoreline investment/training and fresh acceptance.
Phase3 remains1/4 whole groups/~69% partial, broader industries/specialization open.

## October12, 2026 — atomic paid fishery startup

On codex/phase3-fishery-startup, BusinessStartupGrants.startFishery commits the
real food-protected village grant, qualified business/site, first worker, empty
food store and saved habitat together. Failed water admission rolls back the
whole investment. Restart/replay preserves spent fish, caught food and cash under
retuned/disabled policies; changing supports cannot reset an existing resource.
Six temporary-SQL tests passed, zero failures/errors/skips,10.024s at21:08:25 ET.
Full Windows .\mvnw.cmd clean verify passed1,256 (1,254 unit/two packaged
integration), zero failures/errors/skips,6:46 at21:17:37 ET.
No schema migration (schema68), live scheduler hookup, dock construction or change
to the human playable server. Existing generic producer worker IDs are retained.
Paper-facing adapters are unchanged from the prior native shoreline validation;
this database-only batch does not claim a new native automatic-startup check.

Automatic founding still needs current loaded-world/support/shoreline proof,
qualification/training and a real funded customer. A genuine rule conflict needs
a decision: existing village food customers buy only during shortages, while
startup grants are blocked during shortages. Proposed bounded emergency-food
reserve purchasing was sent to the user and recorded in the decisions document;
it is not implemented or approved. Phase3 remains1/4 whole groups/~69% partial.
No merge or push.

## October12, 2026 — approved shoreline starts and priority docks

User approved shoreline starts, quickly prioritized affordable docks in Phase4 and
substantially more efficient dock fishing under editable terms. Recorded in master
design, simulation rules and decisions without locking a multiplier/deadline or
claiming construction is implemented. ShorelineScan/PaperShorelineSource now provide
bounded read-only safe standing-point checks: dry solid opaque footing excluding
magma, two air cells, caller-selected bank rise, no chosen-water footing, no unknown
positions, overflow safeguards and strict main-thread/already-loaded access.
No chunk loads, block/inventory/entity changes, resource/rate changes or migration.
Five added headless cases cover strict budgets, footing/headroom, unknown and
obstructed banks, raised banks/cliffs, coordinate overflow and Paper thread/loaded
height guards. Focused11 passed, zero failures/errors/skips,12.903s at20:53:29 ET.
An initial headless material-registry check failed because Paper requires live
RegistryAccess; actual material safety is verified natively instead. Full Windows
.\mvnw.cmd clean verify passed1,250 (1,248 unit/two packaged integration), zero
failures/errors/skips,6:24 at21:00:56 ET. Guarded native ordinary water scanner/SQL
owner found220 suitable columns and11 independently checked safe shoreline positions,
567 shoreline reads under128/tick, at21:01:58; newer capture/absence/restart passed.
No helper blocks/entities or invented resources/businesses. Normal shutdown21:01:58.
Main-save audit113/117 old tables exact; four ordinary clock/economy/farm-freshness/
ore changes, all identity/assets/cash/history preserved; schema68 integrity/FK
healthy. Exact properties restored, helper archived, consistent backups retained;
human playable server untouched. No merge/push.

Automatic shoreline startup still requires fresh world/support/footing revalidation,
qualified residents/training, real demand and protected funding. This adapter is
verified but not yet scheduled by automatic founding. Physical docks/rate upgrade
persistence remain Phase4 work. The prior shoreline-versus-dock decision is answered;
no new design question pending. Phase3 stays1/4 whole groups/~69% partial.

## October12, 2026 — connected bounded fishing candidates

Branch codex/phase3-fishing-site-selection. WaterPatch selects deterministic,
cardinally connected same-level water footprints from current eligible evidence.
It rejects combining puddles, stale/future supports or duplicate coordinates;
bounded large-body selection avoids claiming an entire ocean. Nearest saved-town
center/coordinate ordering, oldest support timestamp and integer-edge safeguards
preserve explainability. SQL-owner selection applies configured fishing minimum
columns, enabled/depth/world/quality/conflict/claim gates; no startup or stock is
created. Six domain cases include100 seeded input-order shuffles, cardinal
connectivity, component limits, freshness and extrema; one SQL-owner integration
case checks partial/absence/configured-minimum safety. No new migration.
Focused37 tests passed, zero failures/errors/skips,20.179s at20:05:28 ET.
Full Windows .\mvnw.cmd clean verify passed1,245 (1,243 unit/two packaged
integration), zero failures/errors/skips,6:54 at20:12:51 ET. Guarded native ordinary
Paper scanner/SQL owner selected one bounded32-column patch from220 independently
confirmed suitable columns at20:13:35, with newer-capture/absence/restart checks;
no invented businesses/resources or helper block/entity changes. Normal shutdown
20:13:35. Main-save audit113/117 old tables exact; four ordinary clock/economy/
farm-freshness/ore changes, all identities/assets/cash/history preserved; schema68
integrity/FK healthy. Exact original settings restored, helper archived, consistent
backups retained. Human playable server untouched; no merge/push.

Automatic founding still needs a fresh actual workplace, skill/training, demand
and shared protected funding. The natural-shore versus existing-dock workplace
rule is undefined; recommendation/question recorded in DECISIONS_AND_OPEN_QUESTIONS
and sent to the user under AGENTS.md. Dependent startup remains paused for this
choice, not for failing tests. Phase3 remains1/4 whole groups/~69% partial; broader
renewable catalog, competition/specialization and final acceptance remain open.

## October12, 2026 — live current water evidence

Branch codex/phase3-live-water-evidence. Connected main-thread loaded-only
PaperWaterObserver, incremental full-height budgets, strict optional editable
water-observer settings, SQL-owner submission and debug diagnostics. Schema68
adds two initially empty current-evidence tables/index, preserving all old resource
stocks and identities. Complete captures replace only their chunk; unknown retains
old positives but invalidates eligibility. Exact replay, freshness/depth/world,
conflict/permanent-claim/budget exclusions and transactional rollback protect
candidate review. No blocks/inventories/chunk loading or money/resource grants.
Focused70 tests passed, zero failures/errors/skips,27.880s at19:54:03 ET.
Full Windows .\mvnw.cmd clean verify passed1,238 (1,236 unit/two packaged
integration), zero failures/errors/skips,6:58 at20:01:16 ET. Guarded native
PaperWaterObserver/normal SQL-owner check passed20:02:20:220 actual suitable columns
matched independent snapshot reference, newer capture/absence/restart preserved,
no invented businesses/resources or helper block/entity changes. Normal shutdown
20:02:21. Main-save audit111/115 old tables exact; four ordinary clock/economy/
farm-freshness/ore changes, all identities/assets/cash/history preserved; schema68
integrity/FK healthy, new water and fishery tables empty. Exact original settings
restored, helper archived, backups retained; human server untouched.

Automatic contiguous selection, fresh live revalidation, saved recognition terms,
qualified training/funding and fishery founding remain next. No new gameplay
answer pending. Phase3 remains1/4 whole milestone groups/~69% partial; this
infrastructure does not finish the broader renewable-industry checklist.
See WATER_OBSERVATION.md for defaults and limitations.

## October12, 2026 — water observation batch resumed and cleaned up

User resumed after approval-review capacity was restored. The approved stopped-fixture
audit/cleanup completed:108/115 old tables exact, only the previously reviewed normal
bookkeeping/age/farm-freshness changes; schema67 integrity/FK healthy, all old assets/
identity/cash/history retained. Exact original properties restored, helper archived,
backups retained, fixture stopped; no human-server changes. Source remains the same
as the1,229-test full Wrapper build and native actual-snapshot proof. Diff review/check
passed. The earlier pending cleanup checkpoint below is historical and now resolved.

## October12, 2026 — bounded water snapshot evidence

Branch codex/phase3-water-observation. Added plain-Java WaterScan and snapshot-only
PaperWaterSource, with full-height absolute-Y traversal, explicit budgets, exposed
consecutive-depth classification and per-column unknown safety. Immutable evidence
retains capture identity/time/bounds/depth; unfinished/forged empty observations
reject. No live scanner scheduling, database changes, chunk loading, block/entity
mutation or business founding in application code. Eight new domain/adapter tests;
focused38 passed, zero failures/errors/skips,19.764s at17:38:13 ET. Full Windows
Wrapper clean verify passed1,229 (1,227 unit/two packaged integration), zero failures/
errors/skips,6:37 at17:46:19 ET. Native actual-snapshot reference passed17:48:52:
98,304 reads,256 columns,220 suitable,512 cells/tick; no helper block/entity changes,
normal shutdown17:48:53. Read-only main-save audit108/115 old tables exact, seven
permitted clock/mask/ore/farm-freshness/resident-age-only changes, identities/assets/
cash/history preserved, fishery/public-order tables empty, schema67 integrity/FK healthy.
Automatic approval review failed from its usage limit before cleanup mutation;
not executed, not an unsafe-action rejection. Fixture remains stopped/prepared on
localhost25578 with helper present; backups retained. Exact properties restoration/
helper archival/local commit pending approved execution; do not bypass review.
Human server untouched, no merge/push. User approved player ponds alongside natural
water (“Whatever you recommend”); no gameplay choice pending. Schema67/Phase3
counts unchanged. Updated status/progress/checklist and water observation guide.

## October12, 2026 — confirmed fishery habitat capacity and loss

Branch codex/phase3-fishery-habitat-loss. Recorded approved recommendation.
Added explicit complete original-support health evidence and pure capacity/loss
calculation, including zero-capacity pause. FisheryRepository conserves original
claims/terms and owned assets, atomically persists effective stock/time with replay
audit; stale/unknown/incomplete/wrong-world/changed-footprint evidence rejects.
Schema67 is an additive nullable-capacity column/history table/index, retaining
all old rows without refill/backfill. Historical schema fixtures updated for67.
Five domain/six @TempDir SQL cases added. Focused53 passed, zero failures/errors/
skips,24.444s at17:22:26 ET. Full Windows .\mvnw.cmd clean verify passed1,221
(1,219 unit/two packaged integration), zero failures/errors/skips,6:57 at17:29:57.
Extended guarded Paper probe passed at17:31:19, normal shutdown: dry normal timer
pause, restored capacity without refill, gradual recovery/catches, conserved loss
audit/owned food/cash and absent-world/downtime/disabled restart safety. Explicit
fixture support evidence, not native water classification; no world mutations.
Main save111/114 old tables exact; three normal clock/economy/ore changes, old
identity/assets/cash/history preserved, fishery tables empty, schema67 integrity/FK
healthy. Exact properties restored, helper archived, consistent backups retained.
Diff review/check passed. No live water
classifier/automatic founding, human-server changes, merge or push. Counts unchanged.

## October12, 2026 — persistent renewable fisheries and timed owned catches

Branch codex/phase3-persistent-fisheries. Schema66 adds initially empty resource,
column-identity and work-audit tables/index. FisheryRepository preserves saved
capacity/remainder/terms and rejects duplicate/changed-footprint claims atomically.
IndustryRepository catch transaction validates actual staff/storage and conserves
resource/owned food; receipt/period/work rows roll back together. Existing owner
clock and plugin config wiring handle saved fisheries, with absence/downtime/
disabled-period safety and supported existing upkeep/learning. No automatic water
scanning or startup enabled. Ten temporary SQLite cases passed at16:59:24 ET,
12.138s, including genuine public-food customer/import consumption. Corrected test
helper API/declarations, actual one-worker cap and previous-world boundary-mask
assumptions without changing existing behavior. Updated old-schema fixtures for66.
Full .\mvnw.cmd clean verify passed1,210 (1,208 unit/two packaged integration),
zero failures/errors/skips,6:44 at17:07:24 ET. Native guarded normal-owner timer proof
passed at17:08:19, normal shutdown: real resource/catch/food/cash conservation,
absence/downtime and disabled-policy restart freeze, resume without refill. Trusted
explicit fixture columns, not native water observation; no world mutations. Main
save105/111 old tables exact; six normal clock/mask/ore changes and resident age only,
old assets/identity/cash/history preserved; three new main resource tables empty,
schema66 integrity/FK healthy. Properties restored byte-exact, helper archived,
backups retained. The isolated fixture stopped normally. Asked consequential habitat-loss
rule before enabling automatic fisheries; answer pending, no loss behavior chosen.
No human DB/server/world changes, merge or push. Updated resources, decisions,
status/progress/checklist. Broader Phase3 counts unchanged.

## October12, 2026 — approved renewable fish-stock calculation foundation

Branch codex/phase3-renewable-fish-stocks. Recorded fish recovery approval in canonical
decisions/master/rules. Added FishingPolicy/FishStock: habitat-limited opening stock,
integer fractional renewal, capacity cap without banked growth, bounded catches,
exact pause and preserved existing capacity under opening retunes. Configurable
prototype defaults are documented/commented in economy.yml, strict startup validation,
old missing sections disabled. Nine domain/two configuration cases added; focused87
passed at16:40:35 ET,13.687s,zero failures/errors/skips. Windows .\mvnw.cmd clean
verify passed1,200 (1,198 unit/two packaged integration), zero failures/errors/skips,
6:48 at16:48:06 ET. Reviewed diff/check, schema65 unchanged, no native world
adapter/server deployment/actual database writes, merge or push. No Paper smoke run
for this calculation-only batch. Water observation, transactional saved resource/
owned output and normal clocks are unfinished; no claim of in-game fishing.
Updated status/progress/checklist/FISHING_RESOURCES. Phase3 counts unchanged.

## October12, 2026 — native quarry source safeguards verified

Extended existing bounded native mutation capture to raw stone/deepslate and
sand/red sand. Breaks/pistons/explosions and entity source changes retire only
confirmed original coordinate shares; moved or newly formed destinations remain
excluded from natural discovery. Added explicit quarry snapshot classification,
preserving old coal/iron mode. Eight new adapter tests; no migration beyond65.
Automatic quarry surveying/founding/timed orchestration remain next.

Focused33 passed; Windows .\mvnw.cmd clean verify passed1,189 (1,187 unit/two
packaged integration), zero failures/errors/skips, 6:16 at15:53:56 ET. Native
synthetic event-entry/real Paper block/snapshot test passed at15:56:20:54 ->36 ->18
->0 remaining shares, extracted6 retained, moved/generated exclusions, exact three
temporary block restoration and ticket release. First trial's read-only checking
connection omitted FK enablement and correctly failed the application guard;
fixed only that probe, restored/audited and retried with fresh backups. Normal
shutdown, final main-save107/111 tables exact, four normal clock/farm freshness/ore
changes; identity/assets/cash/history exact, schema65 integrity/FK healthy.
Original properties restored, helper archived, backups retained. Human server
unchanged. Fishing recovery decision asked; no new fishing resource rule enabled.

## October12, 2026 — real village supply customer implementation

Recorded public food/training customer approval. Added separate editable budgets,
food-protected material funding, actual local qualified supplier checks, owned
delayed cargo and exact receipt identities. Connected automatic current-period
orders, atomic imports/consumption and food-response incoming commitments through
the normal SQL owner; read-only demand evidence shares real budgets/cash/hauling.
Schema65 adds an empty order ledger/index without new assets or rewritten history.

Fourteen new temporary SQLite purchasing/demand tests, two policy tests and two
configuration tests. Fixed only test catalog/ended-job fixture mistakes; delivery
rollback assertion now distinguishes the existing separately retained earned clock
from period effects. The first full run reported70 old-schema fixture errors due
to the rewind helper retaining the new table; corrected that helper without
weakening migration checks. Focused114 passed; Windows .\mvnw.cmd clean verify
passed 1,181 (1,179 unit/two packaged integration), zero failures/errors/skips,
6:14 at15:38:37 ET. Guarded native normal-session scheduling passed at15:39:21:
paid materials, actual arrival/practice, conserved money, absent-world pause and
restart/new-order opt-out. No blocks/entities/chunks changed. Normal shutdown,
107/110 original main-fixture tables exact; three normal time/ore bookkeeping
changes, assets/history exact, new public orders empty. Schema65 integrity/FK
healthy; original properties restored, helper archived, backup retained.
Human server unchanged, no merge or push.

## October12, 2026 — finite stone/sand persistence and extraction verified

Recorded approval for existing exposed stone/sand, excluding recorded placements;
non-1:1 finite coordinate reserves, no refills and no Phase3 block changes. Added
QuarryPolicy/economy.yml terms, Commodity.SAND and bounded quarry scan mode while
retaining default coal/iron scanning. Existing trusted startup/grant/extraction
seams accept matching quarry/stone or sandpit/sand and conserve real cash/output.
Worker/site/storage limits, per-coordinate depletion/removal, replay and retained
old yields/reserves use the existing tested calculations/history. This is backend
support, not automatically founded/live-observed quarries or a block-rights grant.

Schema64 widens mineral_discovery's resource constraint via transactional parent
rebuild. Old data unchanged; enforcement temporarily off before required upgrade
transaction, all FKs checked before commit, original enforcement restored. Both
pre-rebuild failure and post-rebuild invalid-FK failure roll back DDL/version/rows;
healthy retry preserves all assets/history. New15 tests: nine temporary SQL cases,
four scan/production cases and two config cases.134 focused passed; full Windows
.\mvnw.cmd clean verify passed1,163 tests (1,161 unit +2 packaged integration), zero
failures/errors/skips,6:11 at14:34:42. No Paper-facing world/entity change in this
batch, so no additional native server run; human server/config/DB untouched.

Updated FINITE_QUARRIES, decisions, status, progress and checklist. Counts remain
4/4,27/33,14/15,0/5 (~69% partial): broader renewable/catalog/live acceptance remains.
Next: bounded live observations/mutations (including falling sand), current demand
and qualified founding; asked whether public food/training needs may buy actual
local output with real bounded village money and food protection. That economic
rule is pending; no new customer/payment enabled. Local branch
codex/phase3-finite-quarry-sources; no merge/push/deploy.

## October12, 2026 — broader producers share protected startup budgets

Recorded explicit approval to include logging, fishing, quarry and sandpit under
existing startup-grant limits. Changed StartupGrantPolicy and bundled economy.yml;
all numeric budgets/food floors remain identical and installed industry lists are
preserved. No new funds, migration, terrain certificate or automatic producer.

Added eight parameterized temporary SQLite cases covering shared mine/new-producer
period/lifetime caps, actual empty-store capital, food shortage, late SQL failure
rollback and restart/disabled replay. Added domain budget/food boundary and installed
configuration opt-in preservation cases. Updated an old fishing-disabled assertion
to unapproved bakery.105 focused passed; full Windows Wrapper clean verify passed
1,148 (1,146 unit +2 packaged integration), zero failures/errors/skips,6:09 at14:13:03.
After that build, strengthened the reverse-budget assertions to ensure a new
producer's receipt reduces later mine allowance too; recompiled29 grant tests passed
at14:13:29,14.524s. No application changes after full build. No Paper-facing change,
so no additional native run; human server/DB untouched. Schema63 unchanged.

Updated decision/admin/status/progress/checklist documentation. Broad P3 counts
4/4,27/33,14/15,0/5 (~69% partial) unchanged: granting eligibility does not complete
new resource/catalog gameplay. Asked the exposed-existing stone/sand baseline
question before enabling quarry/sandpit discovery. Known placement exclusions,
fixed coordinate reserves/no refill and no Phase3 block edits remain safeguards.
Local feature branch codex/phase3-broader-startup-grants; no merge/push/deploy.

## October12, 2026 — automatic first warehouses and earned merchant startup

Added WarehouseFounderStudy, WarehouseStartupCycle/PaperWarehouseStartup, actual
SQL-owner automatic review, independent course-state composition, startup/stop
wiring and observation diagnostics. Schema63 adds empty durable course/review
links, deferred FK and index; rollback/retry/exact old-row preservation covered.
Uses approved gradual commerce study and existing protected whole-chain prospects,
real grants, fresh barrel proof, qualification and one-job rules. Missing legacy
sections opt out. No free skills/goods, financial redesign or block ownership.

Ten temporary SQLite study/actual-cycle cases +four polling cases added;48 scoped
checks passed. Fixture expectations corrected for already-consumed starter coal
and saved period review before proceeding. Full Wrapper clean verify1,138 tests
(1,136 unit +2 integration), zero failures/errors/skips,6:15. Final cosmetic budget
message correction rebuilt/rechecked: four cycle +two packaged integration passed.
Native final packaged WarehouseStartupProbe passed at11:52:51: normal SQL timer,
real ten-period timber practice, automatic loaded-barrel founding for an actual
funded customer/observed mineral chain, no free inventory, removal/wrong block/
restoration and restart retained real cash/jobs/history. Four temporary air blocks
restored exactly. JVM terminal-option quoting corrected before startup; helper
requested normal shutdown, all worlds saved and exit0. Audit104/108 original
main tables unchanged, four normal simulation/observation updates, two new tables
empty. Schema63 integrity/FK healthy, exact properties restored, helper archived,
backups/source/test databases/worlds retained. Playable server untouched.

Updated automatic/investment/opportunity/status/progress docs. Completed current
catalog automatic nonfood founding and bounded first-founder skill/investment
entries:4/4,27/33,14/15,0/5 (~69% partial); wider industries/general competition/
specialization remain. Awaited grant extension is recorded/asked, not assumed.
Local feature commit only; no merge/push to main or installation on human server.

## October12, 2026 — qualified warehouse preparation and atomic acceptance

Built WarehouseInvestment/WarehouseFoundingPolicy, strict optional configuration
and PopulationSession prepare/accept APIs. Extracted a transaction-sharing warehouse
grant seam so current prospect revalidation commits with real funding, empty
storage, worker and fixed barrel. No free stock, site reservation, skills, payroll,
ownership count limit or legacy rebinding. Schema62 unchanged. Tests cover real
paid customer, current budgets/sources, eligibility/study/job changes, exact fresh
point, rollback/retry, restart/pause/replay, terms and unlimited ownership. Initial
fixture compilation/assessment and configuration-root omissions corrected before
passing checks, using normal food assessment rather than bypassing invariants.

129 scoped tests and1,124 full Wrapper tests (1,122 unit +2 integration) passed,
zero failures/errors/skips,6:02. Native packaged workshop/disabled-chain regression passed at11:32:43;
not positive warehouse native acceptance. Closed stdin required a guarded
temporary JVM-directory/marker/localhost/zero-player checked attach helper to
schedule main-thread Paper shutdown; all worlds saved normally at11:35:03.
Audit101/108 tables unchanged, seven normal simulation/observation tables only.
Reviewed normal complete1-to0 farm observation against unchanged crop/site/world
fields before permitting it in the audit. Integrity/FK healthy, exact properties
restored and helper archived; originals/backups retained. Updated investment/opportunity/status/progress docs. First merchant
study and ordinary warehouse scheduler remain next; broader checklist unchanged.
Local feature branch only; human server untouched, no main merge/push.

## October12, 2026: read-only first mineral-chain warehouse prospects verified

The SQL owner can now review a first warehouse for actual staffed/funded workshop
input gaps after owned/incoming goods, a current unclaimed nearby barrel, trusted
suitable coal/iron for every input and protected public grant feasibility. Whole
initial purchases must fit real warehouse capital/storage/haul. Whole startup
cash and remaining lifetime grants must cover warehouse plus one needed mineral
supplier per input, without guessing future public income. Each source needs
sufficient finite potential in a connected patch; disconnected deposits cannot
be pooled into one affordable mine. Current-period grants remain shared: a paid
workshop can exhaust the allowance until a real later food period. No review
creates/reserves funds, stock, employment, sites, deposits, courses or businesses.
Existing warehouse identity, including closed firms, prevents first-firm bypass.

Added WarehouseOpportunities/WarehouseOpportunityPolicy, strict optional editable
economy.yml section, startup configuration and read-only PopulationSession API.
Schema62 unchanged; missing legacy section disables review. Updated stale bundled
comments and workshop integration docs. This is prospect evidence, not automatic
warehouse founder study, final fresh-block acceptance or scheduling. Wider catalogs,
competition and specialization remain. Prototype terms reuse approved grant/
marker/economy direction; no new payment, ownership or resource regeneration rule.

Thirteen temporary SQLite tests and two config cases verify actual paid toolkit
sale leading to raw input need, no-write determinism/restart, shared budgets,
whole-chain cash/lifetime limits, finite connected potential, missing/unsafe/
placed/range-excluded/stale/future/absent sources, changed customers, retained
warehouse identity, whole-page bounds, legacy opt-out and actual SQL-owner calls.
92 targeted tests passed. Final Windows .\mvnw.cmd clean verify passed 1,110 tests
(1,108 unit + 2 integration), zero failures/errors/skips, 6:22. Earlier full run
also passed before the additional whole-chain guards. Initial fixture/API call
issues were corrected; real period-budget refusal was retained, not bypassed.

Final packaged native probe repeated unskilled-owner timed study/automatic
workshop startup plus marker/absence/restart/conservation. It also called the new
warehouse API and checked a disabled upstream chain produces no prospect; positive
warehouse prospect verification is SQLite/actual SQL-owner, not native founding.
Disposable Paper stopped normally; no force-loaded chunks, temporary block/ticket
restored/released. 101/108 original main-save tables unchanged; seven normal
clock/tracking/world-mask, resident age and farm observation changes only. Identity,
assets, money/history and empty schema62 founder tables preserved, integrity/FK
healthy. Exact properties restored, helper archived and backups retained. Human
server untouched. No main merge or push. Current branch: codex/phase3-warehouse-opportunities.

Phase3 partial checklist stays about66%; no broad gate is falsely completed.
Next: eligible merchant preparation/training, fresh barrel acceptance and ordinary
warehouse startup, then connected first mining chain and wider industries. No new
user decision blocks that scoped work. See WAREHOUSE_OPPORTUNITIES.md.

## October 12, 2026: automatic supply-backed first-workshop owner training verified

Ordinary scheduled workshop reviews can now sponsor one eligible unemployed adult
for a real viable first-workshop opportunity, using saved smithing courses and
actual practice goods in future food periods. No skills, firm, funds, site rights
or goods are created at enrollment. Existing editable study/career/qualification
limits apply; food and material reserves take priority. Saved rotating reviews,
course/marker links and future eligibility survive restart. Current opportunity
is rechecked each period: unknown/stale/disabled/unfunded/lost demand pauses study;
a different blacksmith taking the first opportunity releases the learner through
cancellation. Existing learning/history remains. Qualified graduates flow through
normal Paper point evidence and atomic paid startup; read-only preparation stays
read-only. Automatic diagnostics now explain disabled recognition, population
backlog and economy failures separately.

Added WorkshopFounderStudy, SQL-owner automatic review and economy practice wiring.
Schema62 adds empty review/course link tables and index with transactional upgrade.
Ten temporary SQLite cases cover gradual real spending/qualification/startup,
unknown/missing/funds pauses and restoration, earned-backlog exclusion, transaction
rollback/retry, absence/restart, disabled settings, insufficient targets/supplies,
taken opportunity, migration failure/recovery and actual PopulationSession calls.
86 targeted tests passed; final Windows .\mvnw.cmd clean verify passed 1,095 tests
(1,093 unit + 2 integration), zero failures/errors/skips, 5:42. Initial test fixture
food/population/seed mismatches were corrected without relaxing app safeguards.

Native WorkshopInvestmentProbe passed with an initially unskilled adult: ten real
two-material practice periods through normal SQL timer, automatic loaded-table
review/paid startup, marker removal/wrong-kind/restoration and absence/restart.
Real synthetic working-farm customer and explicit fixture funds/supplies; no seeded
skills. Conserved money/jobs/cargo/history, temporary air block restored and helper
ticket released. Disposable Paper stopped normally; zero force-loaded chunks.
99/106 prior main-save tables unchanged; seven normal clock/tracking/world masks,
resident age_steps and farm capture timestamp/completeness changes only. A normal
farm scan completed from0 to1; crops/location/world unchanged. New schema62 tables
empty; integrity/FK healthy, exact properties restored, helper archived/backups kept.
Human MinecraftDevServer untouched. No main merge or push.

Phase3 remains about66% partial checklist: this closes the first-workshop training
connection, not the broader nonfood founding/specialization gate. Next: current
first-warehouse opportunities and real merchant startup/training. No new question
blocks that scoped work. See WORKSHOP_FOUNDER_TRAINING.md and AUTOMATIC_WORKSHOPS.md.

## October 12, 2026: scheduled first-workshop startup verified

Normal Paper startup now runs bounded rotating first-workshop reviews for towns
with saved living residents. One proposal/acceptance is outstanding at a time;
completed futures are polled on the main thread, and at most one already loaded
smithing-table point is checked per pass. Current SQL qualification, real customer,
protected funding, paid surplus, storage and jobs are rechecked before acceptance.
Unknown/missing markers spend nothing; changed facts retry without partial startup.
Expected refusals are diagnostic; unexpected faults are logged. Disable cancels the
adapter before closing SQL. No new settings, behavior rates or migration; schema61.

Added WorkshopStartupCycle, PaperWorkshopStartup and startup/debug/shutdown wiring.
Four coordinator tests and two actual temporary SQLite cases cover asynchronous
bounds, rotating towns, failures, missing/unknown blocks, changed funds, paid startup
and no duplicate investment. Windows .\mvnw.cmd clean verify passed 1,085 tests
(1,083 unit + 2 integration), zero failures/errors/skips, 5:39. Native updated
WorkshopInvestmentProbe used the actual scheduled Paper adapter, then retained
marker pause/restoration, absence/restart and real workers/money/cargo/history.
It explicitly seeds qualified fixture experience; automatic training is not claimed.

Disposable server stopped normally; no force-loaded chunks, temporary air block
restored, helper ticket released, helper archived, exact server.properties restored.
99/106 original save tables unchanged; seven normal clock/tracking/world-mask,
resident age_steps and farm observed_millis changes only. Identity/assets/cash/history
unchanged, schema/integrity/FK healthy; consistent backup retained. Human server
untouched. Phase3 broad checklist counts stay about66%, with ordinary first-owner
training, warehouses, broader resources/competition/specialization still unfinished.
Next: supply-backed first-workshop founder study; no new question blocks it.
See AUTOMATIC_WORKSHOPS.md. No main merge or push.

## October 12, 2026: qualified first-workshop investment acceptance verified

The existing real opportunity review now prepares a deterministic proposal with a
qualified available smithing owner, permanent marker/site/business/order IDs and
exact editable investment terms. Preparation saves/spends/reserves nothing.
After a fresh loaded Paper table check, SQL acceptance rechecks current customer,
food/population proof, protected funds, real surplus, recipe/storage, marker survey
and owner age/skills/jobs/study before one atomic funded startup. Changed facts or
late SQL failures leave no partial business/payment/job/order/attachment. Accepted
replay after restart cannot charge again, refill cargo or reset confirmed pause.
Ownership remains unlimited; one employee job still constrains worker availability.

Added WorkshopInvestment and WorkshopFoundingPolicy, strict optional economy.yml
qualification settings, startup configuration and normal PopulationSession owner
preparation/acceptance APIs. Extracted the existing marker startup rows so the
current recheck and complete paid startup share one transaction. No new migration;
schema61 unchanged. Qualification filtering precedes the bounded candidate page,
then domain eligibility checks and stable seeded selection among feasible options.
Prototype owner smithing10, employee smithing1, age216 months or configured adulthood,
capacity2 and page64 are editable; existing files without the section opt out.

Final Windows Wrapper clean verify passed 1,079 tests (1,077 unit + 2 integration),
zero failures/errors/skips, 6:34. Ten temporary SQLite cases and two configuration
cases cover qualification/determinism, real budgets/customers/stocks, changed facts,
study/jobs, rollback, restart/replay/pause and actual SQL-owner calls. A complete
accepted-workshop scenario produces one real toolkit from paid inputs and sells it
to the actual productive farm with protected real funds and delayed owned cargo.
Initial targeted fixture issues (a full farm and insufficient food money for a
larger test population) were corrected without relaxing application behavior.

Native WorkshopInvestmentProbe passed SQL-owner preparation, actual loaded table
confirmation and atomic acceptance for an explicitly qualified synthetic fixture
resident/working farm, then real marker removal/wrong-kind/restoration, world pause,
restart and retained money/jobs/cargo/history. It does not claim automatic ordinary
villager training. Temporary air block restored, test ticket released, normal stop,
zero force-loaded chunks. 99/106 original main-save tables unchanged; seven changes
only normal clocks/tracking/world masks, resident age_steps and farm observed_millis.
Identity, assets, cash and histories unchanged; integrity/FK healthy. Exact server
properties restored, helper archived and all backups retained. Human server untouched.

This is tested preparation/acceptance, not the ordinary automatic first-founder
training/scheduling loop or finished warehouse startup. Those remain next, followed
by wider industries, specialization and long-run acceptance. Phase3 partial checklist
stays about66%; no whole gate is falsely completed. No main merge or push. Current
branch: codex/phase3-workshop-investment-acceptance. See WORKSHOP_INVESTMENT.md.

## October 12, 2026: approved fixed-workplace pause and restoration verified

New explicitly marker-founded workshops/warehouses keep a permanent required table/
barrel anchor. Confirmed removal or wrong block type pauses production and new
buying/selling; the right marker at the same spot resumes operation. Unknown chunks
and absent worlds do not invent removal. Jobs, money, stocks, paid cargo, ownership
and history remain; deliveries already paid for can arrive in a present world.
No productive receipt means no free learning. Legacy firms acquire no requirement.
Editable economy.yml pause-on-missing applies on restart without deleting evidence.

Added WorkplaceOperationPolicy, WorkplaceOperations, bounded main-thread
PaperWorkplaceOperations and SQL-owner APIs. BusinessOperations and direct industry
production/purchase paths enforce the guard. New trusted marker startup composes
binding atomically with actual workshop/warehouse grants, founding, staffing and
paid workshop inputs. Accepted replay never resets pause or refills supplies.
Schema61 adds two initially empty anchor/history tables plus one setting row;
versioned transactional migration failure/retry preserves old assets.

Final Windows Wrapper clean verify passed 1,067 tests (1,065 unit + 2 integration),
zero failures/errors/skips, 5:25. Twelve temporary SQLite cases plus one strict
configuration case cover state, replay, cargo, both trade endpoints, timed learning,
SQL-owner/world guards, overrides, rollback and migration recovery. Earlier full run
also passed; native helper was recompiled against the final packaged artifact.
Native WorkplaceOperationProbe passed actual scheduled loaded-block reads, removal,
wrong kind, exact restoration, absent-world guard and owner-session restart. One
explicit temporary air-block fixture restored exactly, its helper ticket released;
production performs no block/inventory writes or chunk loading/ticketing.

Disposable Paper stopped normally, zero force-loaded chunks in all dimensions.
96/103 original save tables unchanged. Seven changes are ordinary clocks/tracking,
active-world masks, resident age_steps only and farm observed_millis only; identity,
assets, money and histories unchanged. New main-save anchor/history tables empty;
SQLite integrity/FK healthy. Exact server properties restored, helper archived and
all backups retained. Human MinecraftDevServer untouched. No main merge or push.
Phase3 partial checklist remains about66%; this does not yet finish automatic
first-owner preparation or first workshop/warehouse selection/acceptance scheduling.
Next: connect real first-workshop founder preparation and fresh-block acceptance.
No additional design decision blocks that work. See WORKPLACE_OPERATIONS.md.
Current branch: codex/phase3-workplace-operation-pause.

## October 11, 2026: first-workshop investment opportunities verified

A read-only first blacksmith/toolkit review now joins current nearby smithing-table
markers, actual productive farm replacement demand, shared existing warehouse
stock, current food-protected real grant affordability, complete storage and the
approved paid surplus bundle. Existing workshop identities, stale/partial/future/
absent evidence, changed population, budgets and missing materials defer. Repeated
reviews do not spend, reserve, found, hire or award skills. Acceptance and planning
share WorkshopStarterPlan rather than separate surplus/price/storage calculations.

Added WorkshopOpportunities, WorkshopOpportunityPolicy and WorkshopStarterPlan;
connected strict optional economy.yml settings, startup configuration and the
normal PopulationSession SQL-owner read API. Updated the purchase seam to use the
shared calculation. Nine temporary SQLite cases plus two configuration cases
cover real evidence, denial/no-write conditions and restart/world guards.
Final Wrapper clean verify passed 1,054 tests (1,052 unit + 2 integration), zero
failures/errors/skips, 5:40. Updated disposable Paper probe passed both the earlier
paid starter case and actual SQL-owner opportunity review: current synthetic
marker, real working farm/customer, affordable surplus, repeated reads and world
pause, no spending/founding/free skill. Stopped normally; all dimensions have zero
forced chunks. 100/103 original fixture tables unchanged; only civilization_time,
economy_runtime and ore_tracking changed. Assets, money, residents and histories
unchanged; exact properties restored and helper archived. All backups retained.

Schema60 unchanged. This is trusted read-only preparation, not automatic first
workshop/warehouse founding, completed skill training, a player command or current
block acceptance. Qualified founder preparation and Paper revalidation remain
next. Need the operational rule for confirmed removal of a new workplace marker
before enabling that live lifecycle. No rule has been silently chosen. Wider
industries/specialization remain; partial checklist stays about66%, one whole
Phase3 group complete. No human-server or main/push changes. See
WORKSHOP_OPPORTUNITIES.md. Current branch:
codex/phase3-first-workshop-opportunities.

## October 11, 2026: approved paid workshop starter supplies

A new publicly funded workshop can buy one real recipe bundle from village
surplus. Actual business cash returns to the treasury; food is never sold and
editable material reserves remain. Grant, founding, first job, purchase and cargo
are atomic. New IDs cannot refill the one-per-business entitlement. Paid incoming
goods reserve storage, suppress duplicate demand and retain their resale value;
normal timed delivery/production survives restart and absent-world pause.

Added WorkshopStarterPolicy, WorkshopStarterSupplies and the transactional startup
composition, strict optional economy.yml settings and two empty schema60 audit
tables. Extended storage, purchasing/demand and resale accounting for this cargo.
Older stores remain readable after failed upgrades; a migration test now compares
successful upgrade with CURRENT_VERSION rather than a hardcoded previous version.
13 temporary SQLite tests and two configuration tests cover real conservation,
limits/food proof, replay, delayed work, sale assets and migration/write failures.
Final Wrapper clean verify passed 1,043 tests (1,041 unit + 2 integration), zero
failures/errors/skips, 6:08. Earlier verification exposed legacy inspection/test
assumptions; corrected and rerun fully. Native WorkshopStarterProbe passed under
pinned disposable Paper: real starter purchase, restart/absence, one toolkit,
no duplicate or minted money, SQLite integrity/FK. Stopped normally; no forced
chunks. 95/101 original tables unchanged; six normal clock/tracking/aging tables
changed, residents only in age_steps. Two new main-save audit tables are empty.
Exact properties restored, helper archived, all backups retained.

Automatic first-workshop opportunity selection/training and first warehouse startup
remain next. This persistence seam does not automatically create firms, establish
property rights or complete fresh-village acceptance. Phase3 counts remain 26/33
in connected production and about66% across partial groups; no whole group newly
complete. No human-server, main, push or canonical-roadmap change. See
WORKSHOP_STARTER_SUPPLIES.md. Current feature branch:
codex/phase3-workshop-starter-supplies.

## October 11, 2026: connected finite industry endurance regression

Added a normal PopulationSession scenario spanning 50 economic periods: a finite
iron/stone source, staffed mine, warehouse and blacksmith, real productive farm
replacement demand and approved earned-surplus upkeep contributions. The scenario
checks source exhaustion, repeated farm payments, retained stone byproducts,
conservation of the original 30,000 hundredths and all eleven available toolkits
across use, ownership and paid cargo. Repeated pulses and absent-world restart
must not create orders or refill the source. Integrity and foreign keys checked.
Final Wrapper clean verify passed 1,028 tests (1,026 unit + 2 integration),
zero failures/errors/skips, 5:11. The focused scenario also passed.

This is test-only operation/depletion coverage, using explicit fixture businesses,
supplies and controlled test ratios. The prior native Paper check already verified
the unchanged application code and paid farm chain. This new 50-period scenario
is not a multi-year specialization test or proof of automatic first founding.
Schema59 unchanged; no migration, settings or human-server changes. Next: first
warehouse/workshop viable opportunities and qualified founders, then wider
resource operations and seeded specialization. See CONNECTED_INDUSTRY_SCENARIO.md.

## October 11, 2026: public farm equipment demand connected to warehouses

A real productive farm can now drive warehouse restocking for its next toolkit.
Read-only demand requires current complete fields, actual food output and matching
history, qualified staff, a present world, fully supplied current residents and
real protected village funds. Existing service/owned/paid incoming kits suppress
new pressure. Shared local stock is counted once; bounded overflow defers.
Actual restocking pays for existing producer goods with warehouse cash, delivers
cargo normally and lets the public farm buy/use the toolkit under its established
upkeep budget. No free stock, simulated customer, extra levy or reserved money.

Eight new temporary SQLite tests cover missing work/funds/food, changed population,
service/incoming goods, legacy catalog, mismatched history, stock/bounds and the
paid chain through normal SQL-owner restart/absent-world resumption. Wrapper clean
verify passed 1,027 tests (1,025 unit + 2 integration; zero failures/errors/skips),
5:03. Native Paper passed prior upkeep checks plus paid public-farm demand, restart,
absence and replay. Stopped normally; no forced chunks. 98/101 original fixture
tables unchanged; three normal clock/tracking tables changed. Exact original
properties restored; test-only helper archived. Schema 59 unchanged; no migration,
new settings, world/block/inventory edits or changes to the human server.

First warehouse/workshop opportunities and founders remain next. Wider resource
industries, competition, specialization and multi-year acceptance remain Phase 3
requirements. Partial checklist progress stays about 66% (4/4,26/33,13/15,0/5);
whole-group gate remains 1/4. See PUBLIC_FARM_INDUSTRY_DEMAND.md.

## October 11, 2026: opportunity-backed first mining founder training verified

When current observed ore, real funded workshop demand, a staffed warehouse buyer
and protected startup funds are available, an unskilled village can now sponsor one
prospective mining founder. Existing editable career/study/mine settings govern
age, skills, supplies, pacing and budgets. Enrollment creates no business, cash,
goods, job or skill; future food periods spend actual timber/stone under reserves.
Current opportunity loss cancels linked study before practice goods are spent.
Partial earned skills remain; absence pauses. A qualified graduate uses ordinary
atomic mine founding and grant checks; no guaranteed ownership or reserved money.
First warehouse/workshop founders remain next; no construction or hidden ore.

Schema59 adds two empty source-linked founder review/course tables. Existing
courses, assets and history remain. Ten tests added: gradual qualification/startup,
source/customer/funding loss, future-only backlog, rollback/retry, restart/absence,
settings/supplies, migration failure/recovery and actual SQL-owner opt-in/legacy
opt-out. Focused46 passed, then all23 mine tests passed. Windows .\mvnw.cmd clean
verify passed1,019 (1,017 unit/two packaged integration), zero failures/errors/skips,
5:09. Test fixtures initially used the wrong world-update method, omitted new
commodity prices and processed too few bounded backlog pulses; corrected fixtures.

Updated standalone AutomaticMineFoundingProbe compiled and passed on pinned marked
Paper: ten future supply-backed study periods, twenty practice-material spending
entries, earned mining qualification, exactly one real funded mine and conserved
absence/restart state. Existing finite-production/budget checks still passed.
Probe compilation first used a package-private constructor; changed to the public
DB accessor. The previous fixture was inadvertently launched after compilation
failed, stopped normally, then setup/launch were made conditional on success.
A subsequent fresh sub-fixture reused a future capture timestamp and correctly
failed to found by its expected deadline; reset its fixture capture clock and reran.
No production guards weakened. Probe compiled before the successful installation.

Server stopped normally with no forced overworld chunks. Original properties
restored byte-exact, helper archived, old99 tables compared:93 unchanged; six
changes are two ordinary age-only months1733-to1735 and normal clock/active-world/
ore bookkeeping. Food period29 unchanged; prior businesses, stock, money, grants,
skills, courses and history unchanged. New main-fixture founder tables remain
empty under legacy opt-out. Human server untouched; no push or main merge.

Phase3 remains1/4 fully complete groups; partial checklist progress is now shown
separately in PROGRESS.md (about66% on equal-group checklist arithmetic). That is
not remaining-time or playability measurement. Next: real public-farm tool demand
in warehouse purchasing, then first workshop/warehouse founder opportunities.

## October 11, 2026: read-only startup affordability and funding-first diagnosis verified

Industry opportunity checks can now inspect actual startup affordability without
inventing a business or reserving funds. Preview and acceptance share the current
food/population assessment, real treasury, shared period/lifetime spending and
editable grant policy. Ordinary shortages/missing evidence return blocked reasons;
actual SQL faults propagate. Acceptance still rechecks everything atomically.
Automatic mines check funding before founder selection, avoiding misleading skill
obstacles when protected food money is the actual constraint. Existing accepted
receipts/replay and grant defaults are preserved. Ore observation diagnostics no
longer incorrectly claim all automatic founding is disabled. Schema stays 58.

Six tests added, including all-table read-only comparisons, restart, missing/changed
food evidence, disabled industry/caps, non-reservation and changed funds before
acceptance, invalid terms and SQL failure recovery. Focused 34 passed. Windows
.\mvnw.cmd clean verify passed 1,009 (1,007 unit/two packaged integration), zero
failures/errors/skips, 4:59. Sandbox clean was denied; normal-permission rerun passed.

Updated AutomaticMineFoundingProbe compiled and passed on the marked disposable
Paper fixture: preview makes no assets/spending; protected-cash refusal prevents
startup; restored real funding permits exactly one mine via actual SQL owner;
eight finite batches yield sixteen coal; absent-world pause/restart preserve assets.
Native setup first expected the wrong fixture JAR filename and stopped before
launch; corrected to Civilizations.jar. This launch lacked an interactive console,
so stop input was unavailable. A temporary guarded JDK attach helper verified the
exact disposable world directory, dev marker, localhost port and no players, then
scheduled Bukkit shutdown on the main thread. Normal world saves completed; no
forced termination. Future launches must use an interactive terminal.

Properties restored byte-exact, probe archived, full database audit: 93/99 existing
tables unchanged; six differences are four ordinary age-only months 1729-to1733
and clock/active-world/ore bookkeeping. Food period stayed 29. Original businesses,
stock, treasury, grants, skills, courses, resident identity/status/history unchanged;
legacy workplace tables remain empty. Human server untouched; no push/main merge.
Phase 3 stays 25% by completed groups. Next: fundable first-founder opportunities,
gradual real-supply training and automatic first warehouses/workshops.

## October 11, 2026: approved workplace marker observation verified

Nearby loaded barrels and smithing tables can now be recognized as data-only
warehouse/workshop candidates. Editable economy.yml settings bound scan range,
work per tick and marker types. Main-thread immutable snapshots never inspect
inventories, force-load chunks, place blocks or claim player property. Complete
new scans replace current evidence; empty scans clear candidates while retaining
positive sighting history. Partial, stale, future, absent-world, overlapping-town
and already occupied anchors cannot authorize selection. Representative-home
coverage is intentionally bounded rather than an exhaustive village survey.

Schema 58 transactionally adds three empty workplace evidence/history tables.
Existing businesses, goods, money, identities and courses are unchanged. Legacy
economy files without workplace-observer remain disabled. Recognition does not
open businesses: real demand, qualified founders, protected funding and final
live-block revalidation are still required in the next founding adapters.

Thirteen automated tests added. Focused tests: 38 passed. Windows
.\mvnw.cmd clean verify: 1,003 passed (1,001 unit and two packaged integration),
zero failures/errors/skips, 4:46. A test initially expected the wrong exception
for coordinate overflow; corrected to match exact arithmetic, then verified.
WorkplaceMarkerProbe compiled and passed on the marked disposable Paper server:
scheduled scanner and SQL owner recognized both markers; three wheat in the test
barrel remained untouched; complete empty re-observation and restart preserved
history without recreating candidates. Both temporary test blocks were restored,
and the helper released its temporary test-only chunk ticket.

Server stopped normally; original properties restored byte-exact; helper archived.
All 96 original database tables compared: 92 unchanged; four differences were
clock/food-clock timestamps, current farm evidence and ore-trust bookkeeping.
No calendar month or food period advanced; original business assets, finances,
residents, skills, courses and history were unchanged. New main-fixture workplace
tables stayed empty under legacy opt-out. Human server untouched; no push/merge.

Phase 3 remains 25% by completed roadmap groups. Next: first warehouse/workshop
founders, actual demand and protected startup funding; then wider industries and
specialization. Physical building remains Phase 4. See WORKPLACE_MARKERS.md.

## October 11, 2026: demand-backed automatic coal/iron mine founding verified

Current trusted exposed ore can now support an automatic finite mine when actual
funded workshops need inputs and an operating staffed warehouse can buy them.
A qualified unemployed local adult becomes owner/first worker; existing business
ownership remains unlimited, while worker employment remains exclusive. Protected
real village grants pay the recorded startup expense and working capital. Existing
viable mine stock/capacity suppresses redundant founding; larger funded demand can
support another operation. No free qualification, capital, inventory or hidden ore.

Selection, source claims, empty store, job and grant save together. Final receipt
failure or immediate mutation-trust loss rolls everything back. Latest-frame,
world/freshness, range/occupied-anchor and food-protection checks remain. Seeded
bounded pages choose resource patches; accepted coordinate IDs and opening terms
survive restart. Automatic coal/iron yield is separately editable; configured
byproducts remain without creating the other unobserved mineral. No construction,
excavation or land rights. Old economy files without automatic-mines stay disabled.
Schema remains 57; no migration or rewriting of existing sources/assets.

Thirteen tests added (12 automatic founding/owner-flow cases and strict config).
Focused48 passed, then owner-flow11 passed before the final trust-loss case. Initial
fixtures reused a capture time and mismatched the persisted economy seed; corrected
fixtures, no weakening of guards. Windows .\mvnw.cmd clean verify passed 990 tests
(988 unit/two packaged integration), zero failures/errors/skips, 4:50. Initial
sandbox clean could not remove target; normal-permission rerun succeeded.

AutomaticMineFoundingProbe compiled before installation and passed on marked
pinned disposable Paper through actual PopulationSession: one mine, actual 2500
public outflow (500 expense/2000 business capital), eight finite batches/sixteen
coal; absent-world pause and restart preserve assets. Native server stopped
normally with no forced overworld chunks. Properties restored byte-exact, helper
archived and all prior database tables compared: 90/96 unchanged. Six differences
are one normal age-only month 1728-to1729 and associated clock/active-world/ore
bookkeeping. Food period stayed 29; all prior assets, finances, courses, skills and
history unchanged. Human server untouched; no push or main merge.

Phase 3 remains 25% by complete milestone groups. First warehouse/workshop founding,
first-founder career opportunities, wider industries and emergent specialization
remain. Physical entrances/digging remain Phase 4. See AUTOMATIC_MINES.md.

## October 11, 2026: automatic real-vacancy career training verified

Existing living-owner mines, smiths and warehouses can choose unemployed local
adults for actual skill-requiring vacancies. Ordinary qualified hiring runs first.
Courses must be able to meet saved job requirements, share village student/course
budgets and cannot overbook openings. Saved per-food-period reviews/candidate pages
avoid duplicate enrollment and survive restart. Training spends real protected
practice goods in future food periods; enrollment gives no skill, job or money.
Filled/closed/unavailable-owner opportunities cancel before spending; manual
courses remain independent. No first founders, imaginary employer or salary.

Schema57 adds empty linked career review/enrollment tables without backfilling old
courses or skills. Course, saved opportunity/cursor and time accrual commit together.
Untimed status refreshes cannot enroll into previously unobserved earned time.
Strict automatic-career-study settings opt legacy files out; owner files preserved.

Fourteen tests added. Focused104 passed before the final saved-page test; final
Windows .\mvnw.cmd clean verify passed977 (975 unit/two packaged integration), zero
failures/errors/skips, 4:27. Initial tests had a wrong fixture column and assumed
queued periods drain faster than the bounded runtime; corrected test expectations.
Native helper initially used package-private constructors; fixed test-only code to
read SQL. Launch mistakenly proceeded with the older plugin after helper compile
failure; stopped that run, installed successful helper/current verified JAR, reran.
No production source changed after full verification.

CareerStudyProbe passed on marked pinned Paper with actual PopulationSession:
one selected adult, two actual practice periods, two retained skill points, one
normal hire, one saved course, four total practice units and unchanged10000 cash.
Repeated pulse, world absence and restart neither spend nor award twice. Integrity
and foreign keys healthy. Human server untouched; no push/main merge.

Disposable fixture backed up before both native launches. Current schema57 healthy;
81/94 original tables unchanged. Thirteen changes exactly one normal age-only month
1727-to1728, one food period28-to29 (zero stock/output, shortage22-to23), one ordinary
SEEK_FOOD decision and associated needs/appeal/clock/world/ore bookkeeping. All old
reports/decisions, assets, finances and identity/history retained; new career audits
empty under legacy opt-out. Both launches stopped normally with no forced chunks;
original properties restored byte-exact and successful helper archived.

Phase3 remains25% by complete milestone groups. Existing-job career response now
works automatically; safe suitable funded first industries and broader industry
mixes/long-run specialization still remain. Next: observed demand-backed mines.
See VACANCY_CAREERS.md. No new consequential decision needed for this batch.

## October 11, 2026: approved gradual commerce learning verified

Added editable commerce study and actual paid warehouse-order experience. Each
productive review awards a current living local qualified employee at most one
configured gain, regardless of order count. Ownership alone, idle/delivery-only
periods, absent worlds, old receipts and retries award nothing. Courses consume
real protected village practice materials; prototype commerce target10/timber1
remains editable. No automatic career enrollment or first merchant founding.

Schema56 transactionally adds three empty commerce audit tables linked to actual
purchase receipts and industry checkpoints. No asset/history/skill backfill or
phantom production. Failure rolls back goods, money, experience and audit together.
Existing owner files require explicit commerce settings; they are not overwritten.

Ten tests added: actual new orders, one gain for multiple orders, owner exclusion,
idle/restart/absence, disabled/unmapped/budget/over-ceiling behavior, checkpoint and
audit rollback, failed schema55 upgrade/retry, real course supplies and editable
config. Focused47 passed. Windows .\mvnw.cmd clean verify passed963 (961 unit/two
packaged integration), zero failures/errors/skips, 4:35. Final packaged JAR passed
CommerceLearningProbe on marked pinned localhost Paper using actual PopulationSession:
two actual paid orders, one employee gain, 10000 total cash, delayed goods/real
workshop output, no extra awards after idle/restart/absence. Integrity/FKs healthy.

Stopped disposable fixture backed up before schema55-to56; 79/91 original tables
unchanged. Twelve differences are exactly one ordinary age-only population month
1726-to1727, one food period27-to28 (zero stock/output; prior shortage21-to22), and
associated needs/appeal/clocks/world/ore bookkeeping. Old reports retained; business
assets, treasury, estate and identity/history preserved. New commerce audits remain
empty in the legacy fixture. Normal shutdown/no forced chunks; exact original
properties restored and native helper archived. Human server untouched. No push or
main merge. No production source change after full verification.

Phase3 remains25% (one complete group/four), not inflated by partial backend work.
Approved commerce now unblocks subsequent career/opportunity selection. Sales-only
learning, automatic first qualified industries and broader specialization remain.
See COMMERCE_LEARNING.md. No new design question needed for this batch.

## October 11, 2026: bounded funded supplier selection verified

An expected supplier-specific contribution refusal now permits the next stocked
operating supplier in the current bounded page. The first failed offer fully rolls
back before the next begins. Shared food/work/public budget/config refusals still
stop and real SQL faults propagate. Successful payment stops search. Existing
rotating cursors and supplier budgets remain; no new migration or financial rule.

Added a deterministic temporary SQLite scenario with a poor first smith and a
sales-funded second smith: same food review buys the second supplier's actual kit,
first supplier retains cash/stock, repeated pulse creates no extra charge. Focused
32 passed. Final Windows .\mvnw.cmd clean verify passed 953 (951 unit/two packaged
integration), zero failures/errors/skips, 4:22. Test-only FundedSupplierProbe on
pinned marked Paper confirmed actual PopulationSession same-review selection,
paid arrival/use, repeated pulse, restart and absent-world pause. Total cash 30000
remains 2500 village + 27500 funded supplier; poor supplier remains 0. Six actually
manufactured kits = four retained by poor supplier + one warehouse pending + one
used by farm after two food periods. Contribution 2500 saved exactly once; all
integrity/FK checks healthy. No free stock or income.

Before native run, stopped fixture schema 55 backed up; 85/91 original tables
unchanged. Six differences exactly one normal population month 1725 to 1726 (resident
age only) and its civilization/economy runtime, active-world and ore bookkeeping;
no food period, treasury, industry assets, estates or identity/history changed.
Legacy fixture contributions remain 0. Normal shutdown, no forced chunks, exact
properties restored and native test JAR archived. Human server unchanged; no push
or main merge. No source change after final full verification.

Phase 3 remains 25% by complete groups. Automatic first nonfood industries and
specialization remain. Asked the user to settle merchant/warehouse ownership
qualification: proposed editable commerce study and real transaction experience
avoids instant untrained miner-to-shopkeeper conversion. No commerce rule or
automatic merchant founding implemented pending answer. Canonical roadmap unchanged.
Progress tracker now puts the current summary and nine phase bars first, retaining
prior verified updates in a clearly historical collapsible section.

## October 11, 2026: farm equipment no longer blocks warehouse restocking

Fixed WarehouseSupplies producer discovery to exclude only an audited public farm
kit store (farming, capacity1, zero private cash, exactly one tools commodity with
unit space1, saved farm invoice). It matches existing input-demand safeguards and
filters before bounded producer slots. Unfamiliar stores still defer; no general
farming exemption, new stock, demand, grants or migration.

Added a temporary SQLite regression proving actual mined iron is paid into warehouse
cargo while a public farm kit store exists, then retaining the unknown-layout fence.
Focused 38 passed; Windows .\mvnw.cmd clean verify passed 952 (950 unit/two packaged
integration), zero failures/errors/skips, 4:27. First regression setup used duplicate
site coordinates; corrected the test sites before verification.

Final packaged JAR passed actual PopulationSession upkeep plus IndustryRuntime
upstream purchase on isolated pinned Paper. Four manufactured kits and original
30000 cash remain accounted for; warehouse paid100, producer received100, cargo
contains one real iron unit and preserves an older independent paid toolkit.
First native assertion expected only one pending space and overlooked that older
cargo; corrected the test-only assertion to two and reran successfully without
changing production code. Both retained synthetic saves pass integrity/FKs.

Marked main fixture backed up before both runs. Across the pair, 79/91 tables
unchanged; differences exactly one normal population month1724-to1725 (living age
579-to580), one ordinary food period26-to27 (zero stock/output, existing food shortage
20-to21, no other commodity use) and their needs/appeal/clock/world/ore bookkeeping.
All treasury, business/assets, estate funds, prior receipts and identity retained;
all farm/upkeep audits stay empty under legacy opt-out. Final rerun alone preserved
88/91 tables, only civilization_time/economy_runtime/ore bookkeeping changed, no
new population/food period. Normal shutdown, no forced chunks, exact settings
restored and probe archived. Human server untouched; no push/main merge.

Phase3 remains25% by complete groups. Next: automatic first qualified industries,
suitable opportunity selection and genuine upstream demand; no completion claim
for those systems. See WAREHOUSE_RESTOCKING.md.

## October 11, 2026: verified real spare-business farm upkeep contributions

Implemented the approved cash feedback as an exact invoice-shortfall transfer
from the actual operating local supplier. Original village-funded store capital
plus editable operating reserve, percentage of spare cash, sales-backed cumulative
limit and saved business/village food-period caps stay protected. No collection
when public funds suffice; no partial transfer, household wallets, free money or
net-profit claim. Contribution, public payment, real stock/cargo and both audits
share one transaction. Existing files opt out; accepted invoices replay safely
while disabled/absent. Session/Paper config uses forward food-tuning rules.

Schema 55 adds empty upkeep_contributions and budget/supplier-sales indexes;
no asset/history backfill. Eleven tests added: actual sales/invoice conservation,
reserve/quota guards, rollback, current food/world proof, replay/restart, runtime
pause, strict config, numeric bounds and failed schema54 upgrade/retry.
Focused 40 passed. Windows .\mvnw.cmd clean verify passed 951 (949 unit/two packaged
integration), zero failures/errors/skips, 4:23. Initial focused compilation needed
test constructor/signature updates; corrected before successful verification.

Final packaged JAR passed test-only UpkeepContributionsProbe on pinned marked
localhost Paper: actual PopulationSession four food periods, three invoices,
10500 total supplier contributions, two used manufactured kits, restart/absence
and repeated pulse. Four original kits = two used + one farm pending + one warehouse
pending; cash remains 30000 (2500 village + 27500 supplier). Integrity/FKs healthy.
Stopped main fixture backed up before migration54-to55: 87/90 original tables
unchanged, only civilization_time/economy_runtime/ore_tracking bookkeeping changed;
no population month, food period, assets or history changed. New audit empty under
legacy opt-out settings. Normal server exit, no forced chunks, exact properties
restored and probe archived. Human server unchanged. No push/main merge.

Phase 3 remains 25% by complete groups; automatic qualified nonfood founding,
resource opportunity selection and specialization remain. See UPKEEP_CONTRIBUTIONS.md.
Review found a related pre-existing warehouse producer-query omission: audited
public farm equipment stores can be mistaken for unsupported producers. Narrow
restocking regression fix is next; unfamiliar stores must remain explicitly fenced.

## October 11, 2026 — actual farm tool work and replacements verified

Added FarmTools/FarmToolUpkeepPolicy and food-runtime/session/Paper configuration
wiring. Actual field/employee-backed food uses editable productive grace, kit
service and fractional slower output without tools. Zero output never consumes
or wears a kit or awards work experience. Paid arrivals, actual kit consumption,
service/fraction, food, learning and checkpoint share the food transaction.
After committed food, a bounded rotating supplier page can pay an actual local
smith/warehouse from the approved protected public budget. Held/pending equipment
prevents duplicates; missing/stale food proof, staff, world or budget defers orders.
Legacy sections stay opted out and unfamiliar stores are preserved. Disabling
keeps service/history and permits already-paid arrivals; retuning is forward-only.
Schema 54 adds empty actual food-work audit; no free kit or state backfill.
See docs/FARM_TOOLS.md and FARM_TOOL_FUNDING.md.

Eleven added tests (eight SQLite runtime and one strict configuration). Initial
focused 50 passed; final focused 17 and final Windows .\mvnw.cmd clean verify
passed 940 (938 unit/two packaged integration), zero failures/errors/skips, 4:28.
The initial full 939 run passed before review added stable scalar-only farm
transport fingerprints and a regression case: unrelated mining maps/settings
cannot cause a false farm retune across Java processes. Actual PopulationSession on pinned
isolated Paper passed four productive periods, three real invoices, two kit uses,
restart, world absence and repeated-pulse idempotence. Save: 16 food units,
18000 village cash + 12000 supplier cash = 30000 original funding; three manufactured
kits = two used + one paid incoming. Saved service (4 productive periods, one service
period, zero remainder); integrity/FKs healthy. First native check failed because
the test-only read connection omitted foreign-key enforcement; fixed the probe
and reran successfully. Production code was unchanged by that repair.

Stopped marked main fixture migrated 52→54. 82/88 original tables unchanged;
six differences were exactly one normal population month (1722→1723, living age
577→578) and its active-world/clock/ore bookkeeping. No food period advanced;
assets, prior history and identities retained. Both new audits empty because
old fixture configs opt out. Both synthetic saves healthy, including the retained
partial first probe. Server stopped normally, no forced chunks, exact properties
restored and test JAR archived. Final rebuilt-JAR Paper check also passed with
identical synthetic conservation. Before that final run, the stopped 54 save was
backed up: 84/90 tables unchanged, six differences exactly one additional ordinary
month (1723→1724, age 578→579) and clock/world/ore bookkeeping. No food period or
asset/history change; all three synthetic saves healthy (two complete and the
retained partial reader-failure run). Final settings restored and probe archived.
Human server/worlds/database untouched.

Phase 3 remains 25% by completed groups. This supplies a genuine recurring farm
equipment customer; automatic qualified first nonfood founders, upstream opportunity
selection, broader industries and specialization remain. No push/main merge. A consequential treasury-feedback choice is now pending:
current village funds are finite, food trades are outflows, and business sales
keep money privately. Proposed editable contributions from actual spare business
cash would protect operating reserves; no recurring income/levy is implemented
without the user's direction. See DECISIONS_AND_OPEN_QUESTIONS.md.

## October 11, 2026 — protected public farm equipment invoices verified

Implemented FarmToolPurchases and FarmToolBudgetPolicy: a real working village
farm can receive one actual local toolkit paid by village treasury, after current
food/population proof and editable reserve/period-budget checks. Exact invoice
funds leave no farm cash; supplier goods/cash, paid cargo, new empty equipment
store and budget receipt commit or roll back together. Held/incoming equipment
prevents another order. Accepted payments replay after restart/disable without
spending again. Unfamiliar old farm catalogs/cash are preserved. The nonfood
planner recognizes audited public equipment stores without ignoring other unknown
catalogs. Config is strict and missing legacy sections stay disabled.
Schema 53 adds an empty payment audit and village-period index; no asset backfill.
See docs/FARM_TOOL_FUNDING.md.

14 tests added (12 temporary-SQLite payment cases, one budget and one config case).
Initial focused 18 passed; final focused 29 passed before the final eligibility
case. Final Windows .\mvnw.cmd clean verify passed 931 (929 unit/two packaged
integration), zero failures/errors/skips, 4:51. Earlier full run passed before
review added farm-work proof, public-store compatibility and qualification guards;
the final full run includes all changes. Rollback/upgrade/restart, real recipe
output, payment/stock conservation and food ownership are covered. No Paper/server
launch or deployment for this explicit database adapter; Minecraft-facing runtime
wiring is not introduced. Human server, worlds and database remain untouched.

This is a verified funding seam, not automatic tool consumption, repeat orders,
new blacksmiths or warehouse demand. Next: actual farm work/wear and paid deliveries
under the food clock, then bounded funded replacements and first-industry planning.
Phase 3 remains 25% by complete groups; no canonical roadmap changes.

## October 11, 2026 — bounded business vacancy hiring verified

Existing live-owner mines, workshops and warehouses now fill saved vacancies
with qualified unemployed local residents. Candidate/hire budgets are editable;
current workers are never poached, skill is not invented, and each employment
and its explanation audit commit together. Death reconciliation permits a real
replacement; repeated reviews/restart cannot duplicate jobs. Legacy configuration
stays disabled. Farms retain their existing separate staffing adapter.
Schema 52 adds an empty hiring-audit table; existing jobs/assets are not backfilled.
See docs/BUSINESS_VACANCY_HIRING.md.

Nine tests added. Final focused run passed 21; Windows .\mvnw.cmd clean verify
passed 917 (915 unit, two packaged integration), zero failures/errors/skips,
6:04. Initial test compilation used a private listing API; corrected the test
before the successful final run. Disposable Paper actual PopulationSession passed
qualified hiring, death/replacement, restart and absent-world pause. Probe save:
two employment episodes/two audits, one active replacement, reserve eight,
coal two and total cash 10000; integrity/FKs healthy.
Main save schema 51→52: 78/87 old tables unchanged; the other nine differences
were exactly one ordinary food period (25→26), its stock/needs/appeal receipts,
and clock/tracking bookkeeping. All earlier history remained; population,
business assets and identities unchanged; new hiring audit empty. Fixture stopped
normally, exact properties restored, probe archived, no forced chunks or human
server changes. Phase 3 remains 25% by complete groups. Next: approved public
farm-tool upkeep, then opportunity training/automatic first industries.

## October 11, 2026 — warehouses share protected startup funding

Added explicit WarehouseRequest/startWarehouse composition, reusing the existing
grant review and stocked-business startup transaction. Village startup expense,
real purchasing capital and qualified first employment roll back together. Empty
stock only; no production recipe, physical building or automatic opportunity.
New bundled industry list includes warehouse; saved lists and all numerical caps
remain unchanged. Mine/workshop/warehouse grants share actual period/lifetime
sums and food protection. No migration; schema 51 retained.

Six added tests cover cash/empty stock/replay, combined caps, legacy lists, food
protection, qualification/one job and receipt rollback. Focused 27 before final
config case; full .\mvnw.cmd clean verify passed 908 (906 unit/two packaged IT),
zero failures/errors/skips, 6:12. Extended standalone StartupGrantProbe passed on
marked disposable Paper: refused shared-cap bypass, accepted two real grants,
reopened disabled without extra cash/jobs. Own DB 9000 cash plus 1000 recorded
startup expenses equals 10000; integrity/FKs healthy. Main 84/87 tables unchanged,
only civilization/economy/ore tracking bookkeeping differed. No population month,
asset/history change or probe world write. Backed up fixture stopped normally,
no forced chunks, exact properties restored, probe archived. Human server untouched.
Next scoped work: bounded live-owner vacancy hiring; broader career/startup
opportunities remain. Phase 3 remains 25% by complete groups.

## October 11, 2026 — genuine business tool maintenance demand

Added ToolUpkeepPolicy/ToolUpkeep and transactional BusinessTools. Existing timed
firms pay for real replacement tools, consume kits only with positive production
and save service/fractional slowdown. Configurable grace/wear/output/value/industries;
no free opening kits. Maintenance shares recipe funds/storage; untrusted/depleted
mines create no demand. Warehouses buy actual producer yields/configured outputs,
protecting a staffed smith's due kit. Legacy sections opt out, catalogs stay immutable.
Schema 51 adds empty service/audit tables transactionally, no asset backfill.

16 tests added. Initial focused migration failures required the central rewind
fixture to drop new tables; first full run's sole failure was an outdated bundled
storage expectation. Legacy defaults stayed unchanged. Final focused 23 and Windows
.\mvnw.cmd clean verify passed 902 (900 unit/two packaged integration), zero
failures/errors/skips, 6:13. Real PopulationSession on marked disposable Paper passed
new upkeep (cash 10000, reserve four, two kit uses, saved service/fractions, restart,
world pause) and prior warehouse chain (cash 10000, reserve eight). Both DBs healthy.
Probe packaging initially assumed descriptor outside classes; corrected before
deployment. Main snapshot flagged a normal month; verified sole living age +1,
unchanged identities/history/assets and expected clocks. 79/85 old tables unchanged,
new tables empty. Normal stop, no forced chunks, exact properties restored, probe
archived. Windows OSHI warning environmental; human server untouched. Approved
warehouse grants under shared protected limits are next; Phase 3 remains 25%.

## October 11, 2026 — paid upstream warehouse restocking

Added WarehouseSupplyPolicy, deterministic WarehouseInputPlan and owner-transaction
WarehouseSupplies. Existing warehouse clocks buy only available local mine output
against real funded workshop gaps; operating warehouses' pending imports cover
future orders once across the village. Qualified staff, own funds, finite space,
receipt budgets and shared hauling constrain purchases. Existing tagged delivery
and checkpoint transactions preserve paid cargo on restart/disable/owner loss.
No production learning is awarded for purchasing. Strict optional economy.yml
settings enable new files, keep older files opted out. Schema unchanged at 50.

14 new tests, focused 38 passed; full Windows Wrapper clean verify passed 886
(884 unit + two packaged integration), zero failures/errors/skips, 6:13.
WarehouseSupplyProbe verified real PopulationSession two-leg paid transport,
delayed tools, once-only payments, shared haul cap, world absence and clean reopen
with restocking disabled. Final own DB: four shipments, one tool, reserve eight,
10000 total cash. Integrity/FKs healthy. Main 82/85 tables unchanged; only
civilization_time/economy_runtime/ore_tracking bookkeeping updated, all real assets
and historical identities preserved. Marked fixture backed up, stopped, original
properties restored byte-for-byte, probe archived; no forced chunks/world writes.
Initial javac path escaping and missing foreign-key pragma on the probe's read-only
validator were corrected before native PASS; production code did not require repair.
Existing Windows OSHI counter warning remains environmental. Human server untouched.

User approved configurable business-paid tool wear/replacements, startup grace
and slower work without tools. Recorded for the next scoped implementation;
no tools are consumed or imaginary finished-goods customers created by this batch.

## October 11, 2026 — funded local industry input demand

Added plain-Java complete input-buffer estimates and bounded read-only village
reviews from actual saved workshops. Eligible staff, real business cash, saved
capacity, owned goods and paid pending cargo constrain demand using existing
editable recipe/buffer/base-price settings. Shared available warehouse stock is
counted once across customers in deterministic UUID order. No imaginary sellers,
finished-goods customers, purchase orders, reservation, consumption or profit.

The SQL-owner bridge matches only positively uncovered input demand to current
fresh unclaimed/unexcluded coal/iron sightings. Disabled/unknown/absent or incomplete
reviews defer rather than inventing zero demand. Existing owner-death operation and
qualified labor rules apply; budget overflow does not invent a shortage from partial
pages. This is not a startup decision: producer/transport alternatives, coherent
permitted sites, qualified founders and actual funding still require connection.
Initial finished-goods demand/workshop founding remain unfinished. No migration;
schema stays 50. Existing operation, trade, source balances and settings unchanged.

Added 15 tests: six pure demand cases and nine temporary SQLite/owner cases. Focused
21 passed before adding the final mineral-specific selection case. Initial focused
compile failed because a test fixture omitted the existing clean-stop health
argument; corrected before the passing run. Final Windows `.\mvnw.cmd clean verify`
passed 872 (870 unit/two packaged integration), zero failures/errors/skips, 5:43.
No application/test changes after the full build; standalone probe excluded from JAR.

Guarded packaged Paper 26.2 build 129 probe compiled and passed on its first run.
Actual world UUID but synthetic explicit firms/reserve/evidence in a separate DB:
funded input gap, paid finite-mine goods to warehouse, warehouse coverage, paid
undelivered workshop cargo, conserved money, actual PopulationSession reopen and
world pause. Reopened integrity/FKs healthy: three firms, reserve eight, treasury
8000 plus business cash 2000, four paid shipments/two pending with three goods,
zero tools/grants. This does not prove natural automatic startup or customers.

Marked stopped schema-50 dev fixture backed up first; no schema change. Initial
comparison assumed only clock/tracking updates, but the running fixture also earned
one normal food period. Read-only follow-up verified exact month 1720→1721, food
period 24→25, one living age increment, one shortage increment and append-only prior
needs/reports/appeals. 74/86 tables identical; all business/resource/financial/skill/
family/identity history preserved. No source/stock/payment reset. Normal stop exit 0,
no forced chunks reported, exact settings restored and probe archived outside plugins.
Human server/world/DB untouched. Existing Windows OSHI warning remains environmental.

Updated INDUSTRY_DEMAND, development/checklist/status/progress docs. Phase 3 remains
1/4 complete groups (25%); next actual finished-goods needs, qualified career choice,
existing producer/transport comparison and automatically funded suitable industry.
Local branch codex/phase3-funded-industry-demand; no push or main merge.

## October 11, 2026 — current mineral survey evidence

Closed an automatic-startup prerequisite: positive sighting history did not say
which ore belonged to the latest complete scan. Schema 50 adds a current header
and bounded membership, atomically saved by the existing observer/SQL owner with
historical evidence. Newer smaller/empty frames replace current candidates only;
older frames cannot restore them. Identical captures replay, conflicting raw
evidence at the same timestamp rolls back, and original capture time is retained.
Legacy history is preserved without a fabricated current frame.

Read-only owner reviews require caller-supplied freshness and known active-world
identity, healthy mutation history, nonfuture capture and current global claims/
placement/removal exclusions. Missing/expired/unavailable evidence produces no
candidates. Candidate coordinates are not a viable patch or a mine authorization:
real demand, suitable permitted site, qualifications, capital and coherent patch
selection remain. No world edits, automatic careers/founding, goods or money added.

Added 13 tests: 12 temporary SQLite survey/migration cases and one session case.
Focused CurrentMineralSurveyTest,MineralOpportunitiesTest,PopulationSessionTest
passed 42; Windows `.\mvnw.cmd clean verify` passed 857 (855 unit/two packaged
integration), zero failures/errors/skips, 6:02. No application changes after that
build. Test-only probe review corrected its synthetic population to the existing
two-villager recognition minimum before its first run.

Guarded packaged Paper 26.2 build 129 check passed: own synthetic observations/DB,
actual world UUID, fresh candidate, world absence, expiry, newer empty frame, older
replay, retained historical sighting and actual owner restart. Read-only reopened
probe: capture 2000, zero current members, one historical sighting and no firms,
deposits, treasury or grants. This is not natural discovery or automatic founding.

Setup corrections: initial preflight used the wrong Paper filename; backup succeeded
but no server started. A later mistyped checksum literal stopped staging; corrected
the filename/hash and verified the pinned file before staging/start. Native probe
compiled and passed on its first run. Existing Windows OSHI performance-counter
warning remains environmental. No application/test failures remain.

Stopped marked schema 49 fixture backed up before upgrade. Schema 50 integrity/FKs
healthy; 81/84 old tables byte-value identical, only civilization_time,economy_runtime
and ore_tracking changed through normal runtime. New main survey tables empty; all
resident/business/resource/food/finance/identity/skill history preserved. No forced
chunks reported, normal stop exit 0, exact original properties restored, probe
archived outside plugins. Human server/world/DB untouched; no push or main merge.

Updated mineral evidence/development/checklist/status/progress docs. Phase 3 remains
one of four complete groups (25%); next connect real demand and qualified career/
funded opportunity selection. Local feature branch codex/phase3-current-mineral-surveys.

## October 10–11, 2026 — guarded village startup grants

Implemented approved public startup funding for mines/workshops with strict editable
economy.yml settings. Defaults: 50 Trade Value single/current food-period and 200
lifetime village caps; keep max(25 Trade Value, two assessed food-demand periods
estimated at 2 Trade Value/unit). Unmet/missing/stale food assessment or changed living
population makes startup wait. Limits constrain spending, not unlimited ownership.
Old files remain disabled until opt-in. No server configuration rewritten.

BusinessStartupGrants joins real startup/working-capital debits, registration, empty
storage, first qualified worker and finite mine discovery in one transaction.
Schema 49 stores one receipt/business with policy, food evidence, protected cash and
before/after balances. Refusal/failure rolls back everything. Accepted terms replay
after restart/disable without money, quota, goods or ore refill; changed terms still
reject. Existing firms receive no retroactive grants. Existing mine transaction rows
extracted for composition, without changing legacy terms. Automatic choice/founding,
private startup funding, public commands and world edits remain outside this batch.

Sixteen tests added (11 temporary SQLite, three domain, two config); focused 83 passed.
Windows `.\mvnw.cmd clean verify` passed 844 (842 unit/two packaged integration), zero
failures/errors/skips, 6:12. Initial focused compile caught a reassigned test-lambda
variable; corrected before focused verification. Standalone native probe initially
needed a FarmObservation import; corrected and separately compiled against the final
JAR. No application source changed after the successful full build.

Guarded packaged Paper StartupGrantProbe passed using actual bundled YAML and its
own executor/random synthetic DB: protected-food refusal, real startup/capital/job,
mining and restart replay after disabling new grants. Reopened SQLite retained one
receipt (startup 500/capital 1000, cash 10000->8500), business cash 1000, remaining ore 8,
coal 2, one job/three treasury entries; integrity/FKs healthy. This is not automatic
founding, natural ore or human client validation. No block/entity/chunk-ticket edits;
no forced chunks verified, normal stop exit 0, settings restored byte-exact, probe
archived outside plugins. Human server/world/DB untouched.

Stopped marked schema 48 dev fixture backed up first; upgrade 49 healthy,76/82 old
tables identical. Only normal age/clock/runtime/world/tracking changed. Existing
business/resource/skill/family/food/trade/treasury/opportunity data preserved, main
grant table empty. Local feature-branch work only; no merge/push. Phase 3 stays 1/4
complete groups (25%). Next: actual opportunity/career selection and automatic
first funded mines/workshops. No blocking design question. See docs/STARTUP_GRANTS.md.

## October 10, 2026 — durable mineral opportunities

Schema 48 adds positive coal/iron observation evidence by settlement/world UUID
and coordinate, with first accepted and latest capture times. The live scheduled
scanner now saves through the SQL owner in one validation/write transaction.
Revisit/restart/older scans do not duplicate rows or move latest evidence backward;
empty or partial observations cannot erase history. Saved claims and sticky native
placement/removal exclusions are derived at diagnostic read time. This creates no
business, reserve, money or land permission. Automatic founding still needs fresh
revalidation, demand, qualification, real funding and storage. A bounded immutable
owner diagnostic page is available internally, without a new player command.

Nine tests added: eight temporary-SQLite cases and one actual owner/restart case,
covering replay/timestamps, exclusions, conflict/write rollback, tracking gaps,
bounds, additive migration failure/retry and shutdown. Initial focused 33 passed.
The first full run exposed an existing shutdown race: public readiness flips false
before already accepted survey work drains. Owner work now checks initialized state
and healthy tracking directly, preserving close rejection of new submissions and
gap guards. Corrected focused 29 passed. Final Windows `.\mvnw.cmd clean verify`
passed 828 (826 unit/two packaged integration), zero failures/errors/skips, 5:38.
The initial full run failed with that one error; it is not counted as a pass.

Packaged guarded LiveOreSurveyProbe passed on marked disposable Paper: scheduled
nearby loaded-chunk snapshot, original capture time despite clock advancement,
known placement exclusion, actual durable coordinate 22/capture 8000 and retained
diagnostic. Reopened probe SQLite confirmed exactly one coal opportunity and no
business/deposit, healthy integrity/FKs. Blocks restored exactly on disable.
Console-only fixture chunk tickets removed and absence verified, normal stop exit0.

Backed up the stopped schema47 fixture first. Main upgrade schema48 healthy; 67 of
81 old tables identical. Fourteen differences are ordinary clock/age/world progress,
one food/needs/priority period and observation timestamps; no business identity,
business inventory/cash, mineral reserve, skill, family, trade receipt or treasury
changes. Main opportunities
remain zero; only the separate probe save has the synthetic ore evidence. Exact
server settings restored, test-only JAR archived. Human server/world/DB untouched.

User approved limited configurable village startup grants from real treasury with
food-budget protection. Recorded approval; grants are next work, not implemented
by the opportunity table. Phase 3 remains 1/4 complete groups (25%); funded career
choice, automatic first mines/workshops and connected customers remain. Source,
schema, observer diagnostic, tests/probe and docs changed on the local feature
branch. No merge/push. See docs/MINERAL_OPPORTUNITIES.md.

## October 10, 2026 — native ore-source removal and production trust protection

The registered Paper tracker now records successful ore breaks, destructive
block/entity explosions and actual piston origins. Cancelled events, non-ore
blocks and keep/trigger explosions do not retire sources. Immutable coordinates
cross the bounded SQL-owner queue; placements and source losses from one event
commit together. Piston destination exclusions never drain neighboring sources.
The plugin changes no drops, cancels no world action, loads no chunks and performs
no physical excavation. Capture/save/queue failure pauses new discovery and
coordinate-backed mining immediately and durably; healthy close cannot repair a
gap. Direct mining checks saved trust, and accepted historic receipts still replay.
Paused periods explain themselves without creating goods or a later windfall.

Schema 47 fences older saved coordinate reserves because placement-only tracking
cannot certify their continued supporting blocks. It preserves goods, money,
identity and receipts; an empty save has no automatic coverage gap. Explicit
reviewed baseline/recovery tooling remains unfinished. No trust reset, database
reset or human-server upgrade was performed. Other village/workshop simulation
continues; unsupported commands/editors/direct block writes remain outside coverage.

Thirteen tests added across event adapters, temporary SQLite and actual owner
queues: cancellation/destructive lists, piston origins/destinations, combined
rollback/replay, immediate/persisted gaps, shutdown drain, coverage migration and
failure/retry. Focused 86 passed before the final three tests; Windows
`.\mvnw.cmd clean verify` passed 819 (817 unit/two packaged integration), zero
failures/errors/skips, 5:36. Guarded packaged Paper synthetic native events on real
loaded blocks passed reserve 93 -> cancelled break 93 -> break 70 -> piston 47 -> block
explosion 24 -> immediate/restart gap 24 -> entity explosion 0; coal 7/stone 14 stayed
unchanged. All four source retirements totaled 93, with no repeated production.
Prior packaged farm/industry learning, study, resale, succession and closure passed.
This verifies actual registered event/owner wiring, not a human client or physical
piston/explosion engine. Probe blocks were restored exactly. A console-only
chunk ticket in the disposable fixture was removed and absence verified before
normal stop (exit0). Production code and probe itself add no chunk tickets.

Backed up the stopped marked schema 46 fixture first. Schema 47 integrity/FKs healthy;
72 of 81 old table snapshots unchanged. Differences were normal ages/clocks/world
progress, ore/farm observation timestamps and deliberate test mutation evidence.
The first snapshot check omitted the normal farm observation timestamp refresh;
read-only comparison confirmed no crop/capacity changes and corrected the check.
Existing identities, assets and history stayed fixed. Exact original settings
restored and both test JARs archived. Human server/world/database untouched.

Source/application/listener, tests, guarded probe and docs updated on the local
feature branch; no push/main merge. Phase 3 remains one of four complete reporting
groups (25%): safe native source loss finishes a resource prerequisite, not the
whole connected production/specialization gate. Next: durable suitable resource
opportunities, funded career choice and automatic first mines/workshops. No
blocking gameplay question. See LIVE_ORE_REMOVAL.md and MINERAL_SOURCE_LEDGER.md.


## October 10, 2026 — approved per-block mineral reserve ledger

Schema 46 records even allocation of legacy remaining reserves across saved ore
coordinates without changing total reserve, goods, money or historical receipts.
Actual mining drains positive shares evenly; explicit removal retires only the
removed coordinate's remaining share. Replacement/replay cannot refill it. Later
linked evidence gets zero extra reserve; coordinate-free legacy deposits keep
aggregate accounting without invented evidence. Bounded batch retirement and
mining update reserve, source balances and audits transactionally. A later failed
coordinate rolls back the whole batch. Source inconsistency fails closed.

Added 12 temporary-SQLite tests and two domain tests (500 seeded comparisons plus
huge/zero/unequal balances). Initial focused 42 passed; after batch optimization and
three added rollback/world/tiny-reserve cases, Windows `.\mvnw.cmd clean verify`
passed 806 (804 unit/two packaged integration), zero failures/errors/skips, 5:40.
Guarded packaged Paper probe passed actual reserve 100 -> extraction 7 -> reserve 93
-> one-source loss 23 -> reserve 70 -> restart/replay -> extraction 10 -> reserve 60,
with coal 17/stone 34 conserved. Prior farm/industry learning, study, resale,
succession and closure probes passed. No world edits or forced chunks.

Backed up the stopped marked disposable fixture before schema 45 -> 46. Normal stop
exit0; integrity/FKs healthy, conversion totals conserved. Of 76 existing table
snapshots, 70 unchanged; resident identities/status stayed fixed, with only bounded
age ticks plus normal clocks/world-progress/ore-tracking timestamps changed.
The first read-only check assumed a nonexistent schema-history table; corrected to
actual PRAGMA user_version. The first age check incorrectly expected all residents
(including deceased) to age; corrected the check, without application changes.
Original server settings restored exactly and probe archived outside plugins.
Human server/database/world untouched. Local feature branch only; no push/main merge.

This is accounting only: live break/explosion/piston source retirement and trust
fencing still require integration. First automatic mines/workshops and physical
excavation remain unfinished. Phase 3 remains 1/4 complete reporting groups (25%).
Next: bounded native removal capture on the main thread, SQL-owner recording and
failure/unclean-stop protection before enabling safe automatic mine opportunities.
No unresolved design choice for this batch. See MINERAL_SOURCE_LEDGER.md.






## October 10, 2026 — source-loss accounting decision requested

Read-only inspection confirmed that existing mineral reserves are aggregate;
coordinate claims do not save a per-block depletion history. Asked whether to
allocate existing remaining reserves evenly across supporting coordinates or
leave legacy veins unchanged and apply the ledger only to future discoveries.
The approved one-block-loss rule needs this legacy allocation choice before
implementation. Recorded both paths in DECISIONS_AND_OPEN_QUESTIONS.md; no source,
world, reserve or database change made by this follow-on inspection. Farm learning
remains committed and verified at 792 passing tests/Paper; Phase 3 remains 25%.

## October 10, 2026 — farm employees learn through actual food production

Automatic field-enterprise employees now gain configurable farming experience in
new productive food periods. Food, skills, before/after participant audit, needs/
decisions and checkpoint commit together; a later failure rolls all of them back.
Owners without jobs and legacy adult-only field labor do not learn. Saved jobs in
excess of reduced observed field slots rotate through only the productive slots.
Zero output, closed farms, incomplete observations, absent worlds and downtime
award nothing. Existing history is not backfilled; existing skills are never reduced.

The existing work-learning section gains a farming mapping in new defaults, with
one point per productive food period and ceiling 100. Old owner files opt in without
being rewritten. Skill growth does not increase output, create money, add jobs or
waive founding thresholds. A tested working employee can become qualified for
succession through actual work. Schema45 adds review/participant tables tied to
saved food periods, preserving existing farms, food history, money and skills.
Application wiring passes the same approved policy to industry and food runtimes;
farm diagnostics explain latest awards, zero production or budget deferral.

Nine temporary-DB tests added. First focused run caught three fixture errors:
wrong requirement/need table names and a zero-crop observation claiming a crop
position. Corrected fixtures only; focused 90 passed. Full Windows Wrapper command
`.\mvnw.cmd clean verify` passed 792 (790 unit/two packaged integration), zero
failures/errors/skips, 4:26. Guarded Paper PopulationSession checks passed actual
output8, once-only employee awards, restart/absence, reduced-slot rotation and
zero-output pause, with unchanged startup spending; prior resale/inheritance/
industry learning/study/closure probes passed. Initial standalone probe compile
referenced a package-private repository constructor; replaced only the harness
read with SQL and compiled against current classes, without widening the API.

The isolated fixture stopped normally (exit0); schema 45 integrity/FKs healthy,
all 20 preexisting asset/skill/course/audit snapshots unchanged. Original network
settings restored byte-for-byte and test probe archived. Human server/database/
world unchanged. Source/tests/config and documentation updated on the local
feature branch; no push/main merge. See FARM_WORK_LEARNING.md and WORK_LEARNING.md.

Phase 3 remains 1/4 complete groups (25%). Farm learning finishes another skills
component; opportunity-based career choice, initial nonfood founding, broader
labor/capital competition and seeded acceptance remain before another group gate.
Next: connect safe, evidence-backed opportunities to training and real funded
industry founding; per-coordinate supporting ore loss is an approved prerequisite.
No blocking design question for the completed farm batch.


## October 10, 2026 — automatic qualified, funded business resale

Present-world business maintenance now creates offers after qualified family and
employee succession fail. Seeded bounded buyer pages retain a restart-safe cursor;
actual eligible estate/business funds finance purchases under editable limits.
The user approved unlimited private ownership, with one worker job per resident;
government exceptions remain Phase 6. Prices use editable saved startup recovery,
actual cash/stock and paid cargo; accepted terms never reset. Unpriced goods defer
valuation. Listing/payment/estate/current owner/review commit together. Expired
unsold firms retain closure/assets/history; absent worlds and downtime pause sales.
Additive schema 44 saves lifecycle diagnostics/cursors and a local portfolio index.
Existing saves and immutable history are preserved. Old owner settings opt in.

Added ten temporary-SQLite resale cases, one policy case and two config cases:
atomic rollback/retry, qualification priority, real estate/cash funding, paid cargo,
retained terms, paged search, unlimited ownership, world/clock guards, retained
closure and schema 43 failure/retry. Focus 96 passed. Windows `.\mvnw.cmd clean verify`
passed 783 (781 unit/two packaged integration), zero failures/errors/skips, 6:29.
Actual Paper PopulationSession check passed four automatic purchases, five firms
owned by one resident, exact money conservation, absent-world and restart safety;
prior inheritance/production/learning/study/closure checks also passed. Two initial
probe assertions needed correction: enable FK checks on its read-only connection,
and count store funding as a treasury debit, not new money. Only the test harness
changed after Wrapper verification and was recompiled against the packaged JAR.

Stopped the marked isolated fixture normally; integrity/FKs healthy at schema 44,
all 16 preexisting saved asset/skill/audit tables unchanged, original network settings
restored byte-for-byte, probe archived. First console stdin closed unexpectedly;
a target-guarded temporary helper scheduled normal Bukkit shutdown, without killing
a process. Subsequent runs used a persistent console. Human server/world/database
unchanged. Local feature branch only; no push/main merge. See AUTOMATIC_BUSINESS_RESALE.md.

Phase 3 remains 1/4 complete groups (25%): this completes automatic turnover work
inside the lifecycle group, not its broader skills/investment/competition gate.
Next: farm skill learning and opportunity-backed career/founding integration;
nonfood supply chains, geography selection and seeded long-run acceptance remain.
No blocking design question; private ownership/government approval is recorded.


## October 10, 2026 — saved supply-backed adult study

Approved unemployed-adult training now has persistent internal enrollment and
execution in real village food periods. Only living local unemployed adults without
an operating owned firm qualify; hiring, movement or death cancels later reviews
without deleting earned skills/history. Genuine nonfood practice supplies are
consumed as recorded event demand. Food/material buffers, rotating student slots,
current ceilings and retained course terms prevent free skills or double spending.
Already earned backlog, downtime, absent worlds and incomplete observations cannot
create study. Legacy field output counting all adults defers study to avoid double
labor. Village diagnostics explain waiting/disabled/budget/supply conditions.

Defaults: one experience point, mining/smithing target10, stone1+timber1 per success,
two students/period, eight active courses/village, one future food-demand period and
two material units retained. All editable in economy.yml; old files opt in. No
personal wallets, salaries, invented teachers, cash charges or industry founding.
Transactional additive schema 43 retains accepted courses, material terms, actual
period reviews and spending linked to economy history; existing saves preserved.

Eleven temporary-DB study cases, one policy case and two config cases added. Focus
110 passed before final diagnostics; initial full run found its old-schema test
assertion incorrectly querying new diagnostic tables during deliberate downgrade.
Corrected only the test stock read and added a budget diagnostic assertion. Final
Windows `.\mvnw.cmd clean verify` passed770 (768 unit/two packaged integration),
zero failures/errors/skips,4:42. Native Paper passed hungry pause, actual two stone/
two timber consumed, 0→1→2 skill and retained completion through absence/restart,
plus prior employee learning/inheritance/resale/closure checks. Integrity/FKs healthy.
Normal stop exit0, original network properties restored exactly, probe archived;
all 16 preexisting dev asset/skill/audit snapshots unchanged and no main-dev courses
invented. Human server/database/world unchanged; local feature branch only.

Internal backend does not yet select careers or automatically start mines/workshops.
Connect suitable observed opportunities, funded founding and upstream/finished-goods
demand next; farm work learning, automatic valuation/buyers and wider competition
remain. Phase 3 stays1/4 groups (25%). See RESIDENT_STUDY.md. No new design answer needed.


## October 10, 2026 — learning from actual productive work

Employees of existing timed mines/workshops now gain configurable experience only
from actual successful production. Output, finite depletion, skills, immutable
participant audits and checkpoints commit together. Replay, restart, absent worlds,
zero output and closed businesses cannot create extra experience. Owners without
jobs do not learn; qualifying later does not waive ownership requirements.
Prototype defaults: one point per productive period, ceiling 100, worker budget 64,
mining/smithing mappings. Old settings files opt in; existing skills are never reduced.
Schema 42 adds reviews/participant entries without rewriting old skills or receipts.

Focus 79 passed; Windows `.\mvnw.cmd clean verify` passed 756 tests (754 unit,
two packaged integration), zero failures/errors/skips. Isolated Paper passed eight
once-only awards, mining skills 10→15 and 5→8, qualified inheritance, conserved
resale, absence/restart and retained closure. Initial probe compilation attempted a
package-private constructor; corrected only the harness read query, preserving the
application API. Server stopped normally; original network settings restored exactly,
probe archived, all 14 saved development asset/skill snapshots unchanged; SQLite
integrity/FKs healthy. Human server/world/database unchanged. No main merge/push.

Unemployed-adult supply-backed study, farm learning, automatic nonfood founding and
automatic sale valuation/buyers remain unfinished. Phase 3 stays 1/4 reporting groups
(25%); this verified component advances work inside an unfinished group.


## October 9, 2026 — retained unsold closure and released employment

Schema41 additively retains closure reason/date linked to a saved expired offer.
Bounded present-world refresh attempts qualified inheritance first, then atomically
closes unsold operation and ends open jobs. Expired firms stop new work immediately
before their bounded visit. Closed mining/recipes/hiring/trade/new capital are
blocked, while accepted historic receipts and already-owned cargo remain safe.
Stable automatic-farm identity cannot rehire/resurrect; no physical deletion,
asset loss, invented estate proceeds or reassigned employees. Corrected stale
business diagnostics claiming succession was unimplemented.

Nine new closure tests plus one farm regression. Initial fixture lacked actual
starting treasury; corrected setup without relaxing money checks. Broad pre-final
focus73 and final focus43 passed; Windows Wrapper clean verify744 passed (742 unit,
two integration), no failures/errors/skips,4:00. Native first harness connection
omitted foreign keys; corrected the harness only and reran. PASS: staff worked until
expiry, then jobs ended/output stopped across restart, coal3/reserve2 retained;
other inherited mine coal5/reserve5 and paid replay/funds8500/750/250 retained.
Both runs stopped normally exit0, failed evidence/passing probe archived, original
network properties restored, dev schema40 assets retained through schema41 upgrade,
integrity/FKs healthy. User-server unchanged; no main merge/push. Phase 3 remains25%.
User approved gradual resource-backed adult training/work learning next; not yet
implemented. Automatic valuation/listing/buyers and wider competition remain.

## October 9, 2026 — saved offers and absence-safe sale deadlines

Branch codex/phase3-qualified-succession. Schema 40 additively saves accepted offer
price/deadline and elapsed online/present-world time. Startup skips downtime;
world boundaries preserve prior availability and failed boundaries suspend new
payments until reviewed restart. Exact listed price, real payment, estate credit,
owner/audit and SOLD completion are atomic. Ordinary refusal is distinguished from
save failure. Qualified inheritance resolves the old offer without payment.
Expiry retains assets/jobs and is not yet business closure. API takes explicit
trusted price/deadline; automatic valuation/buyer selection/closure remain next.

Nine added tests; focus 55 passed; Windows Wrapper clean verify passed 734
(732 unit/two packaged integration), zero failures/errors/skips, 3:58. Native Paper
passed exact saved 250 offer and single paid resale/replay, inheritance, qualified
staffed deceased-owner mine, finite output, absence/restart and conserved treasury
8500/cash750/estate250. Consistent dev schema-39 backup first; original asset rows
retained after upgrade, schema40 integrity/FKs healthy. Normal shutdown exit0,
properties restored byte-for-byte and probe archived. User-server/world untouched.
Docs/tracker updated; Phase 3 still25%, no main merge/push or new decision needed.

## October 9, 2026 — qualified staff continue after owner death

User explicitly approved continued operation while the family seeks a buyer.
Added opt-in editable business-operations policy/shared availability checks for
timed mines/workshops, automatic warehouse supply and field enterprises. Deceased
owners need actual qualified existing staff; moved living owners are not deceased.
Farms reconcile deceased labor without automatically replacing staff while ownership
is unresolved. After inheritance, current-owner reads restore normal staffing.
No promotion, estate credit, grants, wages or physical changes. Old sections keep
legacy behavior; enabled farm retuning preserves immutable past economy periods.
Schema remains 39. Automatic listing/deadlines/closure remain next.

Ten new tests, focus 91 passed; full Windows Wrapper clean verify 725 passed
(723 unit/two packaged integration), no failures/errors/skips. Farm fixture initially
counted its deceased founder as productive; corrected assertions without relaxing
behavior. Native Paper passed a deceased-owner mine staffed by a worker lacking
founder skill: 3 coal, reserve 5->2, owner retained. Other inheritance, paid resale/
replay and absence/restart checks passed; funds 8500 treasury/750 cash/250 estate
conserved. Normal stop exit 0, properties restored, probe archived, dev assets
retained and integrity/FKs healthy. User-server unchanged. Phase 3 remains 25%;
no main merge/push. See BUSINESS_OPERATIONS.md. No new decision needed.

## October 9, 2026 — conserved business resale and family estates

Branch codex/phase3-qualified-succession. Schema 39 additively records estates,
paid sales and before/after payments/proceeds. Qualified inheritance takes priority;
eligible buyers use real owned-business spare cash and/or recorded family estate
proceeds. Editable limits retain 10.00 Trade Value, spend at most 25% of surplus cash
and 100% of estate cash per purchase. No grants, salary, personal wallets or stock
conversion. Firm site/founder/assets/jobs/cargo persist. SQL-owner API rejects new
absent-world/future-date purchases and preserves historical accepted replay.

Seventeen new tests; focus 81 passed. Windows Wrapper clean verify passed 715
(713 unit/two packaged integration), zero failures/errors/skips. Initial fixtures
made a paying customer underage and used updateResident for a move; corrected test
setup without changing application behavior. Native Paper PASS: inheritance then
paid acquisition, replay after restart, pause, 3 coal/7 reserve, 750 working cash +
250 estate and unchanged 8500 treasury. Deployment initially used a relative build
path from the server folder; hash check halted startup, corrected to absolute path
before successful run. Normal stop exit 0, settings restored, probe archived, dev
assets retained and integrity/FKs healthy. User's server untouched. Automatic listings,
valuation/deadlines, closure/competition and estate-backed new founding remain next.
Phase 3 stays 25%; no main merge/push or blocking design question.

## October 9, 2026 — qualified inheritance and immutable founding

Branch `codex/phase3-qualified-succession`. Added current business reads independent
of historical founding, switched industry/current diagnostics to current ownership,
and added atomic family-first/employee-next succession in bounded present-world
refresh. Requires deceased owner and saved local age/skill/industry qualifications;
no successor leaves ownership unchanged until resale. Preserves employment, funds,
stock, sites and founder identity. Schema 38 adds immutable indexed ownership events.

Six new temp SQLite cases; focused 53 passed. Windows Wrapper clean verify passed
698 (696 unit/two packaged integration), zero failures/errors/skips. Actual guarded
Paper fixture verifies qualified heir, one transfer, 3 coal/7 remaining bundles,
absence/restart and retained 8500 treasury/1000 working capital. First native probe
read-only connection omitted foreign-key enforcement; corrected the harness and
reran successfully. First run's execution stdin was unavailable; a guarded temporary
helper dispatched the normal stop command on the marked localhost/no-player server.
Second run used an interactive execution console and normal stop, exit 0. No kill,
world writes/tickets or user-server updates. Probe archived, original properties
restored, main dev industry/funds/identity retained, integrity/FKs healthy. Its
calendar/resident aging progressed normally during online testing.

Buyer funding from estate and owned-business spare cash under configurable limits
approved; sale/deadline/estate accounting remains next. No main merge/push. Phase 3
still 25%; inheritance alone does not complete the lifecycle/competition group.

## October 9, 2026 — human playtest follow-up

Branch `codex/playtest-trade-responsiveness`. User verified village recognition,
two-bread donation/six food units, two-bread sale/three emeralds and exact inventory
effects, plus persistent residents after normal restart. A legacy unbound dev town
blocked enrollment; approved stopped-server database archive retained the original
file with verified hash and preserved world/settings. No world deletion or implicit
legacy adoption. Temporary 10-second food pacing restored to default 60 seconds
with config backup; no database/world edit during pace changes.

Found a five-second poll interval at each asynchronous purchase/recovery stage,
consistent with the reported ~15-second wait. Reduced aid/purchase/barter polling
to five ticks, preserving each adapter's four-player cap, SQL/save proof gates and
main-thread inventory operations. New repeated scheduled-run test verifies a single
exchange/debit/pending food delivery and confirmation; focused transfer suite 29 passed.
Windows Wrapper clean verify passed 692 (690 unit/two packaged integration), zero
failures/errors/skips. Installed verified JAR in stopped user dev server after old
artifact backup; saved database/settings fingerprints unchanged during installation.
Actual new-build human response time still needs checking. No schema migration.

Recorded approvals: eligible qualified family, then employee succession; affordable
qualified resale with editable price/deadline and family estate fund; unsuccessful
sale closes operation while retaining site/assets/history. Prefer suitable permitted
vacancies before construction. Player mining removes only the affected ore source.
These are planned, not implemented. No main merge/push; Phase 3 remains 25%.

## October 9, 2026 — cadence-aware local hauling

Branch `codex/phase3-haul-clock-retuning`. Reproduced a genuine speed-tuning bug:
old window 99 at 1000 ms blocked current window 99 at 2000 ms despite being from a
different time. Added cadence to new immutable automatic purchase terms/quota
identity. Same-rate shared capacity and replay still enforced; manual/legacy
namespaces retained. No repricing, cargo cancellation, grants or new customer.
Schema 37 adds nullable constrained cadence/index transactionally. Older rows
remain deliverable with unknown cadence; no historical setting inferred or backfilled.

Three new temp SQLite cases. Original regression failed as intended, then passed;
wider initial 41 passed. Added manual-namespace fixture collided with its period-0
setup shipments and correctly refused; changed the test to independent period 99.
Corrected 10 focused passed; final Windows Wrapper clean verify passed 691 (689
unit/two packaged integration), zero failures/errors/skips. Actual packaged Paper
owner checks passed four cadence-tagged cargos, two tools, conserved 9000 treasury/
1000 business capital, absence and restart. Copied known legacy schema-36 cargo
fixture upgraded with all original table/column data retained and original file
fingerprint unchanged. Fresh/copy/main integrity/FKs passed. Main 36->37 industry,
settings and provenance retained. No probe world writes/tickets; port 25578 kept
player address free, normal stop 0, original properties restored, probe archived.
Final player JAR updated only while stopped; world/save/settings retained.

Source changes: repository/runtime/supply helper, migration/test schema fixture,
three supply regression cases, native probe, guides/status/progress/devlog.
Phase 3 remains 25%. Pausing at pending ore-removal and ownership design choices
before their automatic mine/lifecycle work; no answer assumed, main merge or push.

## October 9, 2026 — bounded nearby resource surveys

Branch `codex/phase3-nearby-resource-surveys`. Added deterministic center/ring
geometry, editable 0–2 horizontal chunk radius, legacy scope compatibility,
capture-time preservation and retained completed diagnostics with town/chunk.
One snapshot/future remains pending; no forced chunks, extraction or new firms.
New-file radius one; old missing key keeps zero. Seven new tests, focused 67
passed; final functional Windows Wrapper clean verify passed 688 (686 unit/two
packaged integration), zero failures/errors/skips. YAML comment-only clarification
then repackaged with unchanged values/application; skipped tests only on that
comment packaging step, not the full verification above. Whitespace cleanup only
in the previous supply fixture. No schema change (36).

First native attempt timed out. Added fixture diagnostics; they exposed completed
results overwritten by subsequent scans. Fixed publication and persistent last
result, repeated full verification, then actual Paper fixture passed nearby chunk
1,0 beyond empty home 0,0, original capture timestamp after clock advance, saved
exclusion and retained explanations. Four blocks restored/checked, two test-only
loading tickets removed, separate DB healthy with two people/one exclusion/no firms.
Main industry/settings/provenance preserved; integrity/FKs passed. Port 25578 kept
player address free; normal stop 0, original dev settings restored, probe archived.
Player JAR updated only while stopped; source player guide expanded. No main merge
or push. Phase 3 still 25%; ore-removal and succession decisions remain pending.

## October 9, 2026 — paid automatic workshop inputs

Branch `codex/phase3-workshop-supply-planning`. Added deterministic complete-bundle
planning, editable supply policy/base-price adapter and SQL-owner automatic local
purchases/deliveries inside timed recipe periods. Only actual warehouse stock,
buyer funds, eligible labor, reserved storage and bounded transport; no fake buyers,
grant, payroll, world changes or new firms. Deterministic receipts protect once-only
payments. Entire period rollback includes cargo, money and material work. Legacy
shipments retain their original schedule and independent quota namespace. Already
owned automatic cargo arrives even with new supply off or owner unavailable.

Schema 36 adds buyer-clock cargo identity with compound foreign key and query
indexes. No legacy backfill or stock/fund/history change. Fifteen new cases;
focused 84 passed; complete Windows Wrapper clean verify passed 681 (679 unit/two
packaged integration), zero failures/errors/skips. Initial focused run exposed
duplicate test site coordinates; corrected the fixture without changing application
identity constraints. Actual Paper application probe passed two delayed tool batches,
four cargo records, conserved treasury/business capital, world absence and restart.
Synthetic separate database/unloaded sites, no blocks/entities/tickets/player data.
Stopped dev schema 35 backup retained; native 35->36 comparisons passed, clean-stop
timestamp refreshed as expected, integrity/FKs healthy. Normal stop exited 0; test
probe archived. Updated only the stopped separate player-test plugin JAR.

Docs/settings/domain/runtime/repository/migration/tests/native probe updated.
Phase 3 still 25%; automatic nonfood founding/upstream supply, other primary
industries, actual finished-goods demand and lifecycle/specialization remain.
No main merge/push. Succession decision remains pending, not assumed.

## October 9, 2026 — timed existing industry operation

Branch `codex/phase3-timed-industry-runtime`. Existing funded/staffed mines and
workshops now execute bounded online work through PopulationSession's SQL owner.
New-file defaults: 30 real seconds/period, eight firms/owner pulse; old files without
industry-runtime remain disabled. Saved clocks retain fractions/earned queues,
world absence and downtime add no production, unloaded sites need no chunks. Firms
begin above legacy production history, without pre-enrollment backfill. Finite
reserves, byproducts, actual workers, delivered inputs and storage bound output.
Owner absence pauses without assuming inheritance. No automatic founding, buyers,
new money, blocks, worker entities, forced chunks or regional routes.

Schema 35 adds constrained industry clocks/history/tuning transactionally; existing
identities, stock, funds, reserves and provenance remain. Work/input/output/depletion,
receipt, history and checkpoint commit together. Final-save failure retries intact.
Cadence tuning requires an empty queue and records the discarded fraction. Bounded
round-robin production and a 4096-row clock cap; no large-server performance claim.
Production errors remain diagnosed/retryable without blocking food/population. A
failed world boundary suspends industry until corrected/restarted while population
still receives the new world mask; uncertain elapsed time is not invented.

Eleven temporary SQLite runtime/application cases and two config/policy cases added.
Focused final 37 passed; final Windows Wrapper clean verify passed 666 (664 unit,
two packaged integration), zero failures/errors/skips. Actual Paper accelerated
PopulationSession probe passed: three coal, seven remaining batches, three history
checkpoints, preserved 8500 treasury/1000 working capital, world absence and restart.
Site chunk was explicitly unloaded; no world writes/tickets. Main schema 34->35
retained industry/funds/history and unchanged owner settings, with zero automatic
clock enrollments and clean/trusted provenance. Integrity/FKs passed. Startup month
1685 retained; online 1686. Normal stop exited 0 and probe archived.

Fresh separate localhost player server prepared outside OneDrive using verified
Paper 26.2 build 129, Java 25 and existing accepted EULA. Only cached runtime assets
copied, no existing worlds/databases. Fresh defaults/schema 35 startup and normal
stop passed with zero people/towns/businesses, no force-loading/probes and healthy
provenance. Verified plugin installed; stopped, with a double-click starter. Player
checklist in TONIGHT_PLAYTEST.md; human client validation still pending.

TIMED_INDUSTRY, playtest/development notes, status/progress/checklist/devlog updated.
Phase 3 remains 1/4 reporting groups (25%). Automatic nonfood opportunities/funding,
local purchasing/customers, renewable/stone/sand coverage, lifecycle and specialization
remain. Inheritance recipient choice remains pending. Local feature commit only;
main unchanged, no merge/push.

## October 9, 2026 — unsupported bulk-editor safeguard

Branch `codex/phase3-editor-provenance-guard`. Installed WorldEdit/FAWE/VoxelSniper
names and later enable events conservatively pause new ore discovery. Disabled
installed editors also require reviewed recovery; removal does not clear gaps.
Existing villages/deposits/history and editors themselves remain. Optional strict
ore-provenance text settings add descriptor names, without removing built-in guards
or rewriting owner files. Unknown/renamed editors, offline edits and hot reload are
not certified; this is presence handling rather than mutation integration.

Four new adapter/config cases; Windows Wrapper clean verify passed 653 (651 unit,
two packaged integration), zero failures/errors/skips. Native test-plugin enable
fenced its separate temporary owner; clean reopen retained untrusted history. Empty
probe DB and main DB integrity/FKs passed; main industry/funds/exclusions unchanged,
main clean/trusted fence and owner economy hash retained. No world edits/tickets.
Startup month 1684 retained; online 1685. Normal stop exited 0; probe archived.
An initial fixture deployment used an incorrect relative artifact path and was
stopped normally; corrected absolute deployment supplied the passing native result.
No production DB/world changes, new schema, real editor integration or human test.

ORE_EDITOR_GUARD, development guide, tracking notes, checklist/status/progress/devlog
updated. Phase 3 remains 1/4 (25%): safe nonfood opportunities/operation, renewable
industry coverage, real customers, lifecycle and specialization remain. Succession
needs the user's recipient choice before ownership implementation. Local feature
commit only; no main merge/push.

## October 9, 2026 — atomic funded mine startup

Branch `codex/phase3-atomic-mine-startup`. MiningStartupRepository now joins
business/site founding, finite coordinate discovery, empty storage, actual working
capital and one qualified first employee in one owner transaction. Failure rolls
back every component and debit; replay preserves depleted reserves and owned stock.
A changed first employee is rejected. Existing public operations keep their idle
transaction contracts; package-local row helpers require an active transaction.
No automatic mine policy, physical mining or new schema is enabled (schema 34).

Nine temporary SQLite cases cover success/restart, injected storage/hiring failure,
combined affordability, invalid evidence, transaction contracts, storage/qualification,
changed first worker and occupied primary jobs. Windows Wrapper clean verify passed
649 (647 unit/two packaged integration), zero failures/errors/skips. The packaged
Paper paid-chain fixture passed atomic startup/replay, depletion, delivery, recipes
and restart with capital conserved minus its explicit startup expenditure. Six
fixture blocks restored; normal shutdown exited 0, no force-loaded chunks remained,
probe archived. Main industry/funds/history retained; integrity/FKs, clean/trusted
fence and unchanged owner economy file checked. Startup month 1677 retained; online
1684. No human playtest claim or production world/database changes.

ATOMIC_MINE_STARTUP, Phase 3 checklist, status/progress/devlog updated. Phase 3
remains 1/4 reporting groups (25%): this makes safe startup possible but does not
complete automatic nonfood operation. Next: mutation compatibility safeguards and
actual suitable, demand-backed opportunities. Ownership succession remains pending.
Local feature commit only; no main merge/push.

## October 9, 2026 — registered live resource surveys

Branch `codex/phase3-live-resource-surveys`. Read-only PaperOreObserver now captures
one already-loaded saved-home chunk per town, traverses bounded ticks, then queues
immutable evidence to the SQL owner. Healthy tracking, matching/present world,
complete pair/bounds and current exclusions are required. Existing claims resolve
before reporting provisional patches; conflicts fail without reserve/business
changes. No force loading, physical mining, money, persisted opportunities or new
schema. Editable new-file budgets, old owner files explicitly opt in; strict keys
validated before startup. At most 1024 towns, 512 positives, one scan/future pending.

Nine new temporary-reader/config/owner-world tests. Wrapper clean verify passed 640
(638 unit/two packaged integration), zero failures/errors/skips. Guarded native
scheduled pipeline passed with actual snapshot and current exclusion; all four
blocks restored, synthetic homes/people in separate DB only, no actual villagers.
Read-only main industry/funds/exclusions, both DB integrity/FKs and clean/trusted
fences, unchanged owner settings passed. Startup 1676 retained; online 1677. Ticket
removed, probe archived and normal stop 0. No human/natural mine acceptance claim.

Updated LIVE_RESOURCE_SURVEYS/TODO/status/progress and config. Phase 3 still 1/4
(25%); wider suitable opportunities, funding/staffing, timed chains, lifecycle and
specialization remain. Succession choice asked; no answer assumed. Local passing
feature commit only, no production DB/world reset, push/merge or main change.

## October 9, 2026 — bounded observed resource patches

Branch `codex/phase3-resource-grouping`. OreClusters groups only complete bounded
coal/iron evidence into face-connected patches, with deterministic canonical
anchors/order and immutable metadata. Separate areas, diagonals and unknown gaps
are not silently one deposit. Every observed coordinate occurs exactly once; six
neighbor probes per coordinate, integer-overflow-safe, at most 512 input positions.
No hidden ore, world work, chunk loading, reserve refill or new business behavior.
Existing coordinate identity must be resolved before founding; two provisional
patches may still belong to one saved deposit. No geological/founding policy locked.

Six domain cases plus one temporary SQLite two-patch/depletion/restart scenario
added. Focused 22 cases passed; Windows Wrapper clean verify passed 631 (629 unit/
two packaged integration), zero failures/errors/skips. No Paper adapter changed;
preceding native piston fixture remains the adapter check. Updated RESOURCE_PATCHES, exposed scanning, decisions,
TODO, status/progress. Moved the preceding piston log entry into chronological
position and removed its trailing blank line. Schema 34 unchanged; no server data
changed by this domain batch. Live adapters/opportunities/funded production and
bulk-editor integration remain; Phase 3 stays 1/4 (25%). Physical work remains Phase 4.

## October 9, 2026 — conservative piston provenance

Branch `codex/phase3-piston-provenance`. Extension/retraction MONITOR listeners
exclude moved ore sources and both possible adjacent axis destinations, preventing
movement from presenting tracked ore as fresh evidence. All ore movement is
conservatively excluded; cancelled/non-ore events ignored. Bounds, immutable owner
queue, overflow/error pause and sticky persistence reused. No destination reads,
force loading, event cancellation, migration, reserve refill or world rights.

Added five adapter tests. Wrapper clean verify passed 624 (622 unit/two packaged
integration), zero failures/errors/skips. Guarded Paper synthetic place/extend/
retract fixture passed and restored every block; seven saved exclusions, six new.
Paid-chain/restart checks retained. First launch omitted interactive stdin;
terminated only verified disposable JVM, confirmed dirty restart pauses discovery
and clean stop cannot repair it. Backed up untrusted state after verifying known
empty interval and unchanged industry/funds/exclusions; reviewed marked-dev trust
rearm only. No production reset or automatic recovery capability. Corrected probe
and probe-free runs stopped zero; test ticket removed and probe archived. Read-only
schema 34, clean/trusted fence, unchanged industry/funds/settings, retained exclusions
and integrity/FKs passed. Startup1675 retained; online1676. Build/probe packaging
needed elevation after sandbox temp-JAR rename denial; completed successfully.

Updated status/progress/TODO/live tracking/development/decisions. Phase 3 remains
1/4 (25%); bulk editors, resource grouping, automatic funded chains and lifecycle
remain. Actual piston physics/external plugin ordering are not certified. Local
feature commit only; no push/merge/main changes. Physical construction stays Phase 4.

## Live native ore capture and fail-closed recovery — October 9, 2026

- Registered OrePlacementTracker for successful native/multi-place ore events;
  immutable bounded owner saves, broad future ore provenance, no inventory/identity
  storage, cancelled/non-buildable filtering, bounded failure reporting and discovery
  pause. No block edits, force loading, main-thread SQL or player cancellation.
- Schema 34 adds dirty-start/clean-stop fence. Accepted saves drain before closing;
  unclean stops and capture gaps remain untrusted after restart, preserving people,
  reserves and history. Coordinate SQL rejects persisted gaps; future automatic
  adapters must also check immediate session readiness. Admin status explains state.
- Fourteen added tests (five repository/five adapter/three session/one discovery).
  Review fixed early-gap explanation overwrite and world-observation error fencing.
  Final Windows Wrapper clean verify passed 619 (617 unit/two packaged integration),
  zero failures/errors/skips, including rollback/migration/overload/restart cases.
- Marked-dev consistent schema-33 backup before upgrade. Real event-dispatch fixture
  reached production listener, saved one sticky placement and restored six original
  blocks. Dummy player has no client/inventory data. Separate DB retained five claims,
  20/16 reserve and two tools through paid-chain/replay/restart checks. Test chunk
  ticket removed; probe archived/excluded from production JAR. Normal probe-free
  restart retained month 1670 and trusted capture; no failure or forced chunks.
  Both normal stops exited zero. Read-only identities/industry/funds, one actual
  fixture event row, clean/trusted fence, integrity/FKs and owner config hash passed.
- Updated live/provenance/scanner/mineral/development guides, TODO/status/progress.
  Phase 3 stays 1/4 (25%); next piston/editor coverage, coherent opportunities and
  automatic funded nonfood operations. Ownership succession preference pending;
  no answer assumed. Local feature branch only, no merge/push/main or production
  mutation; human playtest remains required. Nine phases unchanged, no Phase 4 works.

## Persistent resource provenance and Phase 3 checklist — October 9, 2026

- Recorded approved industry direction across suitable resource areas, limited
  exploration, timed extraction/yields, demand-based reserves, stone/sand industries,
  retained closed sites and actual nearby employees. Canonical nine phases unchanged;
  physical workings/scenery still later. Added PHASE3_TODO.md with honest live gates.
- Schema 33 adds sticky player placement coordinates. OrePlacementRepository saves
  bounded batches atomically, preserves first timestamps and refuses truncated chunk
  reads. Owner-session APIs copy/queue evidence and drain accepted shutdown work.
  Discovery rejects saved placements inside the same transaction as resource claims.
- Eight new temporary-DB/session cases plus stronger unknown-neighbor scan coverage.
  Focused test accessor compile error corrected. Final Windows Wrapper clean verify
  passed 605 (603 unit/two packaged integration), zero failures/errors/skips.
- Consistent marked-dev backup before schema 33. Empty server initially had no loaded
  chunk; fixture refused safely. Revised test-only probe waits for explicit console
  chunk loading. Real positive snapshot/exclusion passed with six original blocks
  restored/checked; separate DB passed saved placement rejection/restart, five claims,
  20/16 finite reserve, paid processing and two tools. Production source remains
  read-only; no automatic observer/placement listener/mine behavior enabled.
- Removed test loading ticket; normal stop exit zero, no diagnostic failure, absent
  interval retained month 1667 and online advanced 1668. Read-only old IDs/industry/
  funds, empty main exclusions, integrity/FKs and owner-config hash passed. Corrected
  audit to total tools across stores. Probe archived and excluded from production JAR.
- Guides/status/progress updated; Phase 3 stays 1/4 (25%). Next: crash-safe live capture
  and actual industry opportunity/operation bridge. Asked consequential ownership
  succession preference independently; no answer assumed. Local feature commit only,
  no push/merge/main or production mutation. Human playtesting remains pending.

## Approved exposure policy and bounded ore scans — October 9, 2026

- Recorded user answer "Allow exposed existing ore": accept unknown old origin as
  finite prototype evidence, exclude newly tracked placements, preserve block
  sovereignty and no-refill/no-forced-load rules.
- Added OreScan domain traversal and PaperOreSource snapshot adapter. Known-air
  face exposure, normal/deepslate coal/iron, copied absolute exclusions, negative
  heights, bounded reads, immutable deterministic output and conservative partial/
  overflow handling implemented. Chunk-wide evidence does not define one deposit.
- Eleven new cases (nine domain/two adapter). Initial focused run caught a live
  registry dependency in Paper Material.isAir; explicit air variants resolved it.
  Final Windows Wrapper clean verify passed 597 (595 unit/two packaged integration),
  zero failures/errors/skips. Diff reviewed. No migration, actual database/world,
  owner configuration or server change; live Paper ore scan not run this batch.
- Updated decision/scanning/mineral guides, status and progress. Durable placement
  tracking and real observation/founding/operation remain next; automatic mines stay
  disabled. Phase 3 stays 1/4 (25%) until its complete gameplay group is verified.
  Local feature branch only, no merge/push or main change; no blocking design question.

## Mineral coordinate identity and overlap protection — October 9, 2026

- Closed a prerequisite gap: raw discovery protected a saved site but alternate
  sites had no shared ore evidence. Added canonical bounded MineralObservation and
  MineralDiscoveryRepository. Linked coordinates protect repeat approaches without
  extending reserves; competing sites/multiple pools conflict rather than merge.
- Schema 32 records permanent-world metadata and unique sticky ore coordinates
  atomically with deposit/yields. Legacy deposits retain history/depletion without
  fabricated evidence and need future explicit adoption. Extracted discoverRows
  preserves old term encoding/replay. No natural certification, Paper scan, block
  rights, automatic mine founding or production enabled by this batch.
- Added 15 cases (12 temporary-SQLite, three domain): depletion/restart/tuning,
  linked surveys, site/pool conflict, world independence/rename, incomplete/legacy
  evidence, kind/timestamp identity and write/migration rollback. Windows Wrapper
  clean verify passed 586 (584 unit/two integration), no failures/errors/skips.
- Marked-dev consistent backup before schema 32. Separate SQL-owner native Paper
  fixture established four synthetic ore coordinates/20 bundles, extracted four,
  linked a fifth coordinate and retained 16 despite retuning. Paid workshop/replay/
  restart passed. Main startup retained month 1662; status/no failure and normal stop
  passed. Read-only old IDs/industry/funds and both DB integrity/FKs passed; owner
  config hash unchanged, probe archived, server stopped, fixture outside shipped
  JAR. No world/entity or production data changes. Actual resource snapshots untested.
- Updated mineral guide, decisions/mining/development notes, status/progress/devlog.
  Phase 3 stays 1/4 (25%). At that milestone, pending gameplay choice: allow existing exposed
  ore of unknown origin as finite evidence (exclude newly tracked placements), or
  require admin approval. Subsequently approved; see the exposure batch above.
  Coherent local feature commit only, no merge/push or main modification.

## Workshop recipes and paid connected production — October 9, 2026

- Added deterministic complete-batch manufacturing from actual owned/delivered
  stock. Request, saved local living employees, site, ingredients and net storage
  growth constrain output. Input consumption frees real space; pending cargo neither
  supplies inputs nor relinquishes reserved space. No block work or invented money.
- Editable recipe catalog defaults: smith_tools, iron two + coal one -> tools one,
  two batches/worker/site cap eight. Business/store compatibility required. This is
  backend processing, not an automatic nonfood business/discovery/scheduler feature.
- Schema 31 retains versioned request/workforce/recipe/material history atomically
  with stocks, receipt and shared period-work marker. Duplicate/stale/changed requests,
  zero work and failures cannot reuse labor, consume twice or reset old history.
  Immutable bounded diagnostics show latest industrial explanation; fixed stale
  empty-business text that wrongly said no automatic founding was active.
- Added 18 cases: five domain, ten repository, three config. 10000 seeded domain
  plans and paid 240-cycle mine/warehouse/blacksmith/test maintenance scenario prove
  materials, geology, storage and money conservation across restart. Focused fixture
  corrections used the existing resident migration API and updated stale text.
  Required Windows Wrapper clean verify passed 571 (569 unit/two integration),
  zero failures/errors/skips. No unrun test or full industry gate claimed.
- Consistent marked-dev snapshot before schema 31; dedicated-owner test-only Paper
  probe with separate DB passed paid chain, two tools from four iron/two coal,
  conserved capital, replay, restart, dead-worker exclusion and diagnostic snapshot.
  Main startup retained prior month/no failure; stop exited zero. Read-only previous
  identities/industry/funds, integrity/FKs and isolated recipe flow checks passed.
  Existing owner configuration hash unchanged; native probe excluded from production
  JAR, archived outside plugins and server stopped. No world/entity changes.
- Updated workshop guide, config text, runtime/mining/development notes, decisions,
  progress/status/devlog. Phase 3 stays 1/4 (25%): chain group partial until reliable
  nonfood discovery/founding/operation. No consequential user answer needed; next
  scoped adapters precede turnover/specialization. Local feature commit, no push/merge.

## Automatic funded farms and assigned production — October 9, 2026

- Connected real crop observations to one persistent village field enterprise,
  qualified available founder/workers, actual once-only startup debit, saved site
  and worker-backed food. No duplicate legacy adult output or double primary jobs.
  Configurable defaults live in economy.yml; old files opt in and are not rewritten.
- Added schema 30 nullable crop coordinates, deterministic founding/employment IDs,
  replacement staffing, explicit blockers and admin food/business explanations.
  Existing founding terms/history survive restart/tuning; smaller fields cap output.
  Server/world pause remains intact. Nonfood automation/construction remain later.
- Ten temporary SQLite scenarios plus two config cases cover funding, qualifications,
  incomplete/legacy fields, shared labor, replacement, failure/retry, restart/pause,
  migration and 240-period conservation. Initial validation caught two old migration
  tests using newest runtime readers against deliberately rewound schemas; switched
  checks to the compatible history repositories. Final Wrapper clean verify passed
  553 tests (551 unit/two integration), zero failures/errors/skips.
- Separately compiled test-only Paper fixture after excluding dependency source JARs
  from manual javac classpath. Native real crop snapshots produced one farm, two
  workers and eight food units; startup charged once and absent-world pause passed.
  Consistent marked-dev backup preceded upgrade; normal shutdown exited zero.
  Read-only old IDs/history/funds, schema 30, integrity/FKs and saved probe site passed.
  Owner configuration hash unchanged; probe archived, server stopped. No production
  world/data or main change. FarmBusinessProbe's own test crop fixture is disposable.
- Updated automatic farm guide, owner settings, decisions, development instructions,
  foundation/runtime notes, status and progress. Phase 3 first prototype reporting
  group complete: 1/4 (25%); chains, turnover and specialization remain unfinished.
  Next: connect another justified industry and its actual input/output accounting.
  No new consequential design choice or user answer required; local feature commit only.

## Live staffing maintenance and business diagnostics — October 9, 2026

- Connected saved businesses to the existing SQL-owner runtime: transactional
  observed job separations for dead/moved workers, retaining history; absent worlds
  skip maintenance. Owner death reports unavailable without automatic succession.
- Bounded stable-UUID rotation (eight/pulse), immutable retained cache (1024 max)
  and admin `/civ debug businesses <town UUID>` report founders/owners/workers/sites,
  vacancies, pause state and latest separation. Commands show at most 20 businesses
  and never query SQL or worlds. Cached details can lag until a firm's next turn;
  larger-workload profiling and SQL latency remain future checks.
- Seven added automated cases verify maintenance, pauses, owner death, old snapshot
  immutability, page fairness/eviction, injected SQL rollback/retry, actual session
  startup/world return and permission/ID/startup command safety. Required Windows
  Wrapper clean verify passed 541 cases (539 unit/two packaged integration), zero
  failures/errors/skips. No migration; schema 29 and old records preserved.
- Consistent snapshot before verified marked disposable Paper startup. Readiness
  stayed at month 1659 after downtime; empty-business/malformed-ID/status commands
  passed, then clean shutdown exited zero. Read-only integrity/FKs/old identities
  and exact treasury balance checks passed; no businesses were created automatically.
  Positive business fixtures are temporary SQLite tests, not native industry gameplay.
- Updated status/progress/foundation notes and BUSINESS_RUNTIME.md. No automatic
  recognition/founding/hiring/output, world edits, salaries, spending or new economic
  policy. P3 remains 0/4 completed gameplay groups, with concrete first-group progress.
  Next: reliable sites and configurable opportunity/hiring/production integration.
  No new design decision needed; local feature-branch commit only, no merge/push.

## Server downtime and absent-world pause — October 9, 2026

- Implemented explicit user decision to pause absent worlds/server downtime without
  resetting identity/history. Main-thread Paper world IDs feed the DB owner; no
  chunk loading or async world access. Canceled world unloads leave eligibility intact.
- Transactional schema 29 stores adoption audit and compressed active-world month
  masks. Live startup rebases anchors; population replay retains earned masks while
  paused towns skip demographic effects. Food/needs accounts retain inventory,
  funds, fractions and earned queues while absent; economic execution resumes when
  the world returns. Missing masks and legacy unrestricted demographic replay fail.
- Twelve added tests: active vs absent worlds, centuries of controlled downtime,
  population/family/life/arrival preservation, partial months, queued replay across
  restart, food stock/queued period freeze, rollback and migration retry, immutable
  Paper snapshots and canceled unload. Fixed initial test expectation about earned
  pending food work, then full final Wrapper verify passed 534 cases, no failures,
  errors or skips (532 unit/two packaged integration).
- Backed up the marked disposable schema-28 server DB before actual Paper upgrade.
  Native first readiness stayed at month 1658 instead of adding offline months;
  normal online activity advanced to 1659. Diagnostics/no failure, clean stop and
  read-only integrity/FKs/old identity retention verified. No production DB/worlds
  touched. Baseline Windows OSHI/native access/manifest warnings remain.
- A second native restart after more than a minute stopped stayed at month 1659,
  zero queued months/no failure, with clean shutdown. Final read-only history/
  family/finance retention and unchanged treasury balances passed. World-absence
  transitions are automated tests; no native multi-world/client playtest claimed.
- Recorded approved rule in master/architecture/simulation/decisions; updated status,
  progress and ONLINE_WORLD_PAUSE.md. Shared calendar is administrative; population
  fractional boundaries remain shared, unlike independent economic clocks. Later
  automatic industry must use eligible town periods. Roadmap/P3 counts unchanged.

## Finite minerals and local warehouse accounting — October 9, 2026

- Recorded user approval: finite non-renewable deposits, shared abstract ore/rock
  output, editable defaults, bounded abstract storage and warehouse purchase/resale.
  Master/simulation rules and decisions reflect approval; ROADMAP unchanged.
- Added plain-Java MineProduction/IndustryPolicy and owner-thread IndustryRepository:
  stable once-only discoveries, actual-worker/site/storage-limited extraction,
  per-period receipts, empty business stores, real treasury-funded working capital,
  goods/money-conserving delayed local purchases, incoming-space reservations,
  protected arrivals and explicit household/industry/maintenance-use receipts.
- Schema 28 adds transactional finite-deposit/store/operation/shipment storage;
  no refills, unlimited goods/money, fake consumers, personal wallets or salaries.
  Existing live food/player economy remains unchanged. Backend operations only;
  automatic discovery/business production and NPC shopping remain next scoped work.
- Optional industry YAML exposes complete coal/rock, abundance, labor/site,
  weighted storage and local haul/time defaults. Old owner files are untouched;
  saved openings/discoveries retain terms through edits/restart. No mineral-regrowth
  toggle. Store expansion/shared village accounting need explicit future integration.
- Sixteen tests added: mining limits/10000 seeded calculations, depletion/restart,
  full/pending storage, actual warehouse resale profit/money conservation, causal
  use, write rollback/retry, capital opening, stale/impossible work, schema-27 failure,
  independent snapshot cargo recovery and 240-period exact goods-flow scenario.
  Corrected a test-only lambda capture compile error. Focused 27 cases then passed;
  final Windows Wrapper `clean verify -B -ntp` passed 522 cases (520 unit/two packaged
  integration), zero failures/errors/skips. Test writes use temporary owned DBs.
- Consistent marked dev backup preceded Paper schema-27-to-28 startup. Protected
  normal diagnostics and clean shutdown passed. Read-only integrity/FKs, checked
  prior identities/history and unchanged owner YAML hash passed; zero auto-created
  industry records. No production world/DB, main, merge or push changes.
- Guide/status/progress updated; Phase 3 first two groups partial, still 0/4 completed
  gameplay milestones. Next: sites/owner views/founding/hiring and actual demand/
  production integration. Product ranges should respond to capacity/skills/supplies.
  Asked absent-world pause versus continued evolution; no answer or per-world pause
  assumed. Preserve matching world UUID/data plus plugin DB to return to old towns.

## Phase 3 business persistence and staffing foundation — October 9, 2026

- Added plain-Java business, site, immutable founding terms and employment records;
  owner-thread BusinessRepository supports qualified registration/hiring, finite
  capacity, retained separations and explicit dead/migrated-worker reconciliation.
- Business/site/requirements and actual explicit startup treasury spending commit
  together. Exact retries preserve original terms and never spend/hire twice or
  resurrect ended episodes. No automatic business creation, production, salaries,
  grants, site/block rights or changes to existing food/player trades.
- Transactional additive schema 27 preserves existing population/economy histories;
  test schema fixtures rewind only temporary databases. Older binaries require a
  consistent pre-upgrade backup to downgrade. Added DatabaseManager repository access.
- Twelve new cases cover full fields/restart/snapshot, funding/ledger rollback,
  qualifications/worlds/sites, double jobs, capacity/rehiring, death/migration,
  failed reconciliation, nested transactions, schema-26 rollback/retry and seeded
  240-month staffing turnover. Focused 24 cases passed; final Windows Wrapper
  `clean verify -B -ntp` passed 506 (504 unit/two packaged integration), zero failures,
  errors/skips. All automated writes use test-owned temporary SQLite databases.
- Backed up confirmed disposable Paper DB before schema-26-to-27 upgrade. Initial
  launch omitted tty, closed console input and required terminating the explicitly
  verified dev process. Corrected persistent-console restart, status/observation and
  final normal shutdown passed; read-only integrity/FKs, original IDs and checked
  history retention passed, with zero automatic businesses. Baseline OSHI warning
  unchanged. No production files/worlds/database, owner config, main, merge or push.
- BUSINESS_FOUNDATION guide, status, progress and decisions updated. Phase 3 started,
  first milestone partial; 0/4 gameplay groups remain complete pending runtime sites,
  founding/hiring/diagnostics. No new answer needed. Next: bounded cached business
  views and reliable-site integration, then editable opportunities and production.

## Phase 2 response gate and prototype completion — October 9, 2026

- Branch `codex/phase2-response-gate`: plain-Java comparison of actual donation,
  finite-budget emerald, idle-stock timber/stone and wait capabilities. Explicit
  blocked reasons, supported minimum stack value, pending/reserved food, catch-up,
  governing priority, funds and keep floors constrain seeded optional preferences.
  Players retain choice; existing exact quote/consent/proof safeguards remain.
- Strict commented response weights/settings; new files enable, old files unchanged/
  disabled. Session/runtime policy wiring, cached admin explanations and visible
  village menu preferences. No Paper access from domain/SQL owner or forced chunks.
- Transactional schema 26 appends replayable period comparison/candidate tables.
  Old histories stay intact; failed publication rolls food/needs/learning/receipt
  use back. Missing/corrupt candidate evidence fails; policy changes apply forward.
- Ten new cases cover seeded feasible alternatives, settings, actual paid execution,
  finite funds/pending supply, missing evidence, migration rollback and 240-period
  delivery/publication/restart history. Test-only ambiguous import/treasury naming
  were corrected; a restart test's changed seed was caught and corrected while
  retaining the application seed guard. Final Wrapper clean verify passed 494 cases
  (492 unit/two packaged integration), zero failures/errors/skips.
- Marked dev schema-25 consistent snapshot preceded upgrade. Pinned real Paper
  passed published donation/cash/timber options, actual one cash/log exchange each,
  saved proof/failure/duplicates, bargaining, learned outcomes, welfare, restart and
  unloaded checks. Probe archived; regular plugin startup/diagnostics/shutdown passed.
  Read-only schema/integrity/FK, old identity/economy/learning history and native
  candidate completeness/selected feasibility/completion checks passed.
- Phase 2 prototype gate advances to 4/4 (100%). Status/progress, acceptance and
  response guide updated. Human client/compatibility, cross-save restoration,
  profiling/history growth and survival balance remain release work; industry,
  construction, regional suppliers/reputation remain later phases. No production
  reset, owner-config rewrite, merge/push or canonical roadmap change. Next: Phase 3.

## Configurable saved food-priority learning — October 9, 2026

- Branch `codex/phase2-priority-learning`: configurable earlier verified import
  outcomes reinforce food-priority weight by a bounded percentage. New defaults
  look back 12 economic periods with a 25% cap; old files stay disabled. Strict
  validation, explicit two-zero disable, no player penalties/rewards or new payments.
- Prior applied receipt totals must match each historical period's imports. Marginal
  unmet-demand prevention sets the bonus; pending/current-period/unmatched inputs
  do not count. Existing review cadence remains. Enabled integer weights use
  100-times resolution; unchanged disabled legacy reviews replay exactly.
- Schema 25 adds explicit learning replay markers, versioned evidence/policy and
  applied-delivery index. Old choices/history remain intact; enabled missing evidence
  fails even if a legacy action would match. Learning write failure rolls back food,
  needs, review and receipt use. Forward settings/restart preserve old evidence.
- Fourteen new tests include 1,000-seed feasible choices, ratios/caps, admin settings,
  migration/recovery, restart/disable, expiry/mismatch and 240 verified-delivery
  periods. Full Wrapper verification passed 484 cases (482 unit/two packaged IT).
  First full run caught two intentionally failed-upgrade diagnostic regressions;
  fixed old-schema read compatibility without concealing current-schema corruption.
- Pinned disposable Paper confirmed native twelve-unit delivery/four prevented
  unmet units -> 8% bonus at a later review, retained across restarts; previous
  donation/purchase/bargaining/barter/welfare/famine/unloaded checks passed.
  Consistent marked dev schema-24 snapshot preceded upgrade; no production/world
  reset or owner-config rewrite. No merge/push. Updated an obsolete import-impact
  diagnostic sentence so it no longer claims adaptive weights are absent.
  Final full Wrapper rerun after that wording correction passed; probe archived and
  final plugin-only startup/diagnostics/shutdown passed. Read-only schema-24/25
  comparison retained old IDs, review fields and economic history; integrity/FKs
  valid, old owner settings disabled, native learned evidence/metadata verified.
- Status/progress/decisions/guides updated. Phase 2 stays 3/4 (75%): food learning
  is complete, broader explicit feasible-response comparison/execution explanations
  remain. No new design question blocks the next scoped work.

## Exact food import outcome evidence — October 9, 2026

- Branch `codex/phase2-import-outcomes`: FoodImportImpact compares each retained
  food period with identical opening stock, production and demand but no imports.
  It explains prevented unmet demand and additional closing reserves. Their sum
  equals imported units; farm recovery and later reserve consumption get no false
  import credit. The cached view and admin economy diagnostics expose the result.
- Nine new domain cases (including 2,400 seeded periods) and one temporary-SQLite
  restart/history case; strengthened existing applied/pending delivery assertions.
  Final Windows `.\mvnw.cmd clean verify -B -ntp` passed 470 cases (468 unit/two
  packaged integration), zero failures/errors/skips. Initial full verification
  passed; reran after strengthening the native diagnostic and runtime assertions.
- Pinned disposable Paper's verified twelve-unit native donation reported exactly
  four prevented unmet units and eight extra reserves, and printed the admin
  explanation. Consistent marked dev snapshot taken before server work.
  Full native purchase/bargaining/barter/welfare/unloaded regression probe passed.
  Archived the test-only probe; normal plugin startup/diagnostics/shutdown passed.
  Read-only schema/integrity/FK and saved identity/economy history checks passed.
- No schema change (24), saved priority replay change, owner-config rewrite, player
  reputation, world reset or production DB modification. Existing saved inputs
  already support deterministic reconstruction. No merge or push.
- Phase 2 stays 3/4 (75%). This is outcome evidence, not adaptive action selection;
  broader executed responses and bounded configurable learning still need their
  acceptance gate. Status/progress/decisions and FOOD_IMPORT_OUTCOMES.md updated.

## Playable finite-stock player barter — October 9, 2026

- Branch `codex/phase2-player-barter`: `/civ barter timber|stone` previews real
  outgoing supplies for a whole held food stack; explicit `/civ barter accept`
  confirms it. New commented files enable barter; old owner files stay unchanged.
  Quotes expire using the editable existing offer deadline and move no items/funds.
- Saved PREPARED proof precedes stock reservation; exact food -> material in the
  same slot; verified EXCHANGED proof precedes one future food receipt. Changed goods
  cancel/refund once, uncertain inventory failures retain supplies for review,
  and disabled-new-trade recovery remains. Pending markers block other transfers;
  unverified payouts lock item movement. No treasury charge or industry invention.
- First focused adapter run refused the new receipt because it was not yet in the
  proof reader's explicit allowlist. Added only the barter key; full bounded NBT,
  root-marker/EOF/CRC verification remains. Rerun passed, retaining failure tests.
- Ten new automated cases: nine player/command/recovery cases plus a mixed two-
  village 240-period donation/paid/barter/cancel/duplicate/restart scenario. Final
  Wrapper clean verify -B -ntp passed 460 cases (458 unit/two packaged integration),
  zero failures/errors/skips. All imports, cash debits and materials conserved.
- Native disposable Paper passed 8 wheat -> 6 saved oak logs, stock8->2 once,
  unchanged treasury, withheld payout on failed save, protected double acceptance,
  future food once, restart/duplicate completion and prior welfare/economy/unloaded
  regressions. Normal final startup/command routing/diagnostics/shutdown passed;
  SQLite integrity/FKs, stable IDs and histories retained; consistent dev snapshot
  first, probe archived outside plugins. No production reset, merge, main or push.
- Phase 2 advances to 3/4 (75%): player trade/prices/stability prototype group
  passed. Broader autonomous executed responses and attributable outcome feedback
  remain. Human client, inventory-plugin compatibility and survival balance remain
  release limits. Next: explained feasible responses and outcome learning.

## Protected barter accounting — October 9, 2026

- Branch `codex/phase2-barter-accounting`: real saved timber/stone can fund an
  internal food quote even without cash. Whole-item value, actual need, protected
  remaining stock and prior material demand constrain offers. No new industry.
- Transactional schema 24 reserves goods immediately, records before/after movements,
  restores cancellations once and completes one future food receipt. Competing
  offers, emerald reservations and UUID reuse cannot spend the same supplies twice.
  Existing history, treasury and population are retained. Disabling new offers does
  not prevent reserved recovery. Completion still requires a future trusted adapter
  to prove the saved physical exchange; no player barter interface claimed here.
- Optional commented file-first tuning starts disabled and preserves old configs.
  Wrapper clean verify passed 450 automated cases, including fifteen new cases and
  seeded 240-period finite-stock conservation/restart. Disposable Paper's existing
  donation/purchase/bargaining/welfare/famine/unloaded regression probe passed schema24.
- Schema-23 marked development snapshot taken first. No main/merge/push or production
  reset. Progress remains 50% by full groups; physical barter and broader decisions
  remain. See BARTER_ACCOUNTING.md for history semantics and next adapter contracts.

## Saved emerald bargaining — October 8, 2026

- Branch `codex/phase2-food-bargaining`: food asking prices now get an agreement or
  counter from current shortage/value/funds; explicit acceptance uses the existing
  saved inventory purchase pipeline. Discussion never debits goods or funds.
- Editable optional economy.yml patience/cooldown/deadline defaults: 3/60s/45s.
  Saved per-player/village terms survive restart; menus and quick purchases cannot
  bypass active cooldown. No reputation penalty. Old configs remain unchanged.
- Transactional schema 23 preserves histories/identities and consumes accepted
  bargains with their funds reservation. Added expiry/supersession/changed goods,
  retry, rollback, migration-failure and disabled-new-offer recovery checks.
- Wrapper clean verify -B -ntp passed 435 tests (433 unit/two packaged integration),
  no failures/errors/skips. Sixteen new cases. During focused development, helper
  extraction exposed a stale clock variable and a test tried a new price before
  processing due food catch-up; fixed the code reference and used the real pulse
  in that fixture. Final verification retained both safeguards.
- Actual marked disposable Paper passed saved counter/restart and explicit native
  exchange of 8 wheat for 3 emeralds once, failed-save withholding, one debit and
  future delivery; previous donation, famine, welfare and unloaded checks passed.
  Consistent schema-22 dev snapshot taken before upgrade. No production changes.
- Phase 2 remains 50% by whole milestones; bargaining is additional verified work
  inside unfinished trade. Barter, broader executed responses/learning and integrated
  balance/compatibility remain. No merge/push/main change. See FOOD_BARGAINING.md.

## Basic household wellbeing — October 8, 2026

- Branch `codex/phase2-household-wellbeing`: occupied families now have a saved,
  explained basic food/housing welfare score and SECURE/PRESSURED/FOOD_CRISIS/UNKNOWN
  condition. Treasury stays separate. No jobs/salaries, starvation deaths or changes
  to existing birth/death/migration rules were introduced.
- New commented optional wellbeing section: prototype neutral 50/100, recovery +1,
  undersupply -3, sustained crisis -5 and crowding -1. Admins can edit each 0..100
  value while stopped and restart. Old files without the section stay disabled;
  existing people never reset from a starting-score edit. Unknown housing is not zero.
- Schema 22 preserves welfare observations, their actual residents and each resident's
  shortage streak in the food owner's transaction. Current household IDs describe
  homes: movement follows resident IDs, not a guessed permanent family/home identity.
  New occupants do not inherit the previous family's score or crisis. Mixed/new
  residents use the floor mean of retained/initial scores. Disabled periods do not
  fabricate unrecorded personal history; old observations/policy remain intact.
- Eighteen added automated cases. Final Windows Wrapper clean verify -B -ntp passed
  419 cases (417 unit/two packaged integration), zero failures/errors/skips. Tests
  cover 10,000-period seeded famine/recovery per seed, actual migration adapter,
  new occupants, membership/write rollback, restart, admin tuning, migration failure
  and a 240-period integrated finite-budget purchase/cancel/famine/restart scenario.
  Two early fixture errors (missing household name; invalid direct home move under
  resident/home FK constraints) were corrected to use real migration behavior.
- Pinned disposable Paper passed supplied-family score, sustained food crisis,
  unchanged welfare across restart/retuning, gradual recovery, and all prior food,
  donation/payment and unloaded checks. Normal plugin startup/diagnostics/shutdown
  and schema-22 integrity/FKs verified; schema-21 dev snapshot taken first. Original
  resident/home/family/projection identities and treasury history preserved; older
  owner config stayed untouched, with assessment disabled.
- Phase 2 advances to 2/4 reporting groups (50%): basic labor, food production,
  treasury/spending and household food/housing wellbeing are verified. Broader
  employment/business prosperity remains Phase 3 context; learning/response
  execution, barter/bargaining and balance/compatibility gates remain unfinished.
  No production data reset, main change, merge or push. See HOUSEHOLD_WELLBEING.md.

## Recoverable village food payments — October 8, 2026

- Branch `codex/phase2-purchase-accounting`: sneak-right-click a managed villager,
  then explicitly Sell a supported plain whole food stack. A hungry village pays
  whole emeralds from actual funds; regular vanilla trades and free donations remain.
- Optional commented economy.yml purchases section: enabled in new files, disabled
  in older files lacking the section; editable 8.00 Trade Value/emerald, three demand
  periods as target and 64-emerald cap. Accepted terms survive disable/retuning.
- Schema 21 records immutable reservations/completion/cancellation. Funds and
  reservation share a transaction; pending donations/purchases count against need.
  Independent complete Minecraft player-file proof precedes reservation and food
  credit. Exact food stack becomes emeralds in the same slot; retries never pay twice.
  Changed stacks cancel/refund once. Ambiguous inventory failures hold for review.
- Windows Wrapper clean verify passed 401 cases (399 unit/two packaged integration),
  zero failures/errors/skips. Added 24 cases for temporary SQLite, saved proof,
  failed saves, inventory failures, restart, duplicates, held funds and admin settings.
  Initial verification exposed nested transaction use; corrected repository read
  inside its owner transaction, retaining the rollback checks.
- Pinned disposable Paper passed native menu/double-click, failed-save withholding,
  8 wheat for 3 saved emeralds, exactly one 24.00 debit, future food credit, restart
  and duplicate completion. Previous famine/donation/allocation/unloaded checks pass.
  Normal plugin-only startup/read-only diagnostics/clean shutdown passed. Schema-20
  dev snapshot taken first; schema-21 integrity/FKs valid; original IDs/history kept.
- Human client/inventory-plugin/native-death and cross-save crash compatibility remain
  prototype validation limits; SQLite/player/world files need consistent backups.
  No barter, bargaining, personal payroll or reputation attribution claimed.
  Phase 2 remains 1/4 completed groups with substantial partial work. No production
  data reset, main change, merge or push. See FOOD_PURCHASES.md and ECONOMY_TUNING.md.

## Admin-controlled opening capital — October 8, 2026

- Branch `codex/phase2-configurable-opening-funds`: approved file-first economy
  control; optional finance section enables one-time funds and an exact editable
  Trade Value per living resident. New-file prototype 24.00; missing old sections
  keep grants disabled. Comments and ECONOMY_TUNING explain opt-in/ranges/restart.
- SQL-owner runtime opens capital with reliable enrollment or explicit startup
  activation for unfunded economies. Population/rate/amount retained in receipt;
  initialized zero/manual/spent balances survive new people, retuning and restart.
  Failure rolls back opening plus enrollment together; no recurring income/payroll.
- Eight new temporary-DB/config cases; final Windows Wrapper clean verify -B -ntp
  passed 377 (375 unit/two packaged integration), zero failures/errors/skips.
  Early runs exposed two new fixture mistakes; corrected nonexistent factory and
  missing world binding without application changes. No schema migration (still 20).
- Backed up confirmed-dev DB; pinned Paper passed 48.00 once for two people,
  restart at 99.00/resident without refill, one ledger receipt, food/famine/donation/
  recovery and unloaded checks. Normal plugin-only startup/diagnostics/shutdown
  passed. Original identities/family/projection rows retained; integrity/FKs valid.
  Old server economy file untouched, grants disabled. Probe archived outside plugin
  folder and absent from production JAR. No production data, main, merge or push.
- Recorded approved negotiation/reserves/cooldown/admin direction; these remain
  planned, not wired. Next: recoverable budget-backed emerald exchanges, followed by
  protected barter and bargaining with corresponding editable settings. Phase 2
  stays 1/4 full reporting groups. Human client, balance and scale checks remain.

## Initial finances and contextual payment direction — October 8, 2026

- User resolved the finance questions: moderate initial finances, then causal gains/
  losses; emerald payments or optional barter from the village/merchant's actual
  production. Example quantities are illustrative; keep prototype tuning editable.
- Clarified current housing/farm labor versus Phase 3 employers/workplaces. No
  invented business, profession-based stock, salary or recurring income added.
- Recorded one-time opening capital direction; visible entity loads/recreation do
  not reset wealth or create new funds. Treasury remains separate from wellbeing.
- Updated canonical decisions, finance guide, status and plain-English tracker.
  Documentation-only batch; no implementation, schema/world/DB/config change or
  new build/test run. Last verified code remains 369 passing tests. Phase 2 count
  stays 1/4; next implementation is initial funding and recoverable payment/stock
  exchange. No new decision is required before that scoped work.

## Exact finance quotes and shared treasury transactions — October 8, 2026

- Branch `codex/phase2-finance-transaction-foundation`: pure TradeQuotes provides
  exact full-stack Trade Value cost and maximum affordable whole items. Unknown
  budget differs from a measured zero; partial affordability never rounds up.
  Existing scarcity price rounding is reused once, with overflow/invalid inputs
  rejected. Quotes neither reserve funds nor accept goods or promise a payout.
- Treasury standalone APIs preserve their behavior. Internal row operations now
  join an existing SQL-owner transaction, allowing money and receipt writes to
  commit together. They never finish the caller's transaction. Tests combine an
  explicit fixture debit and food-delivery receipt; injected receipt failure rolls
  back both, retry retains both and later food accounting consumes the receipt once.
- Twelve new cases; final Windows Wrapper `clean verify -B -ntp` passed 369 tests
  (367 unit/two packaged integration), no failures/errors/skips. Covers 10,000 seeded
  affordability cases, scarcity conversion, cent/long boundaries, insufficient funds,
  shared opening/credit/debit rollback, operation retries and restart.
- Initial verification exposed an intermittent existing preview fixture failure:
  Paper Location retains a weak world reference and the unregistered proxy could be
  collected. Test now retains its fixture worlds strongly; pinned Paper source
  confirmed the lifetime contract. No gameplay behavior was changed for that fix.
- Schema remains 20. No real/server DB, world, inventory or owner config changed;
  all writes used temporary test databases. No new Paper-facing behavior or server
  smoke run this batch; funding/payment adapters will need native Paper/client tests.
- Updated finance guide, status, decisions, devlog and progress detail. Phase 2
  remains 1/4 full reporting groups. Initial funding source and player currency
  questions are pending; no starting money, income, emerald exchange rate, live
  purchase or barter policy has been assumed. Next: approved funding and payment
  handoff, then budget-backed purchase execution and integrated economy balance.

## Voluntary village food appeals — October 8, 2026

- Branch `codex/phase2-village-food-appeals`: a selected SEEK_FOOD priority now
  publishes the actual latest-period deficit in Village supplies. Empty-hand or
  unsupported/custom-item viewing is read-only; reopen with plain food for the
  existing confirmation. Disabled transfers/permissions retain a protected button.
- New period snapshots retain OPEN/NONE/RESOLVED/WITHDRAWN status, observed pressure
  improvement/worsening, actual production/imports and the governing priority review.
  Pending deliveries are separate; queued catch-up is disclosed. No invented quest
  target, payout, stock creation, supplier or building consent. Recovery observations
  do not prove the appeal caused it and do not train weights without attribution.
- Schema 20 transactionally adds food_appeals without backfilling past observations.
  Appeal save failure rolls back stocks, needs, priority and receipt use together;
  restart preserves exact history. No production reset or owner config rewrite.
- Twelve added cases; final Windows Wrapper `clean verify -B -ntp` passed 357 tests
  (355 unit/two packaged integration), zero failures/errors/skips. Includes all appeal
  states, mixed/conflicting evidence, non-causal local-production recovery, a repeated
  1,200-period scenario, temporary-SQLite restart, upgrade and write rollback/retry.
- Disposable native Paper checks passed empty-hand report/card and protected click,
  actual confirmed player-file debit, famine appeal across restart, recovery with
  twelve real imported food units and no duplicate receipt. Farm/unloaded checks
  still passed. Normal single-plugin startup/status/shutdown passed; no forced chunks.
- Consistent confirmed-dev schema-19 backup first; schema 20 integrity/FKs verified.
  All original settlement/resident/household IDs and family/entity links retained.
  Production JAR excludes probes/JUnit. Human client UX and scale profiling remain
  pending. Treasury funding/spending, paid trade/barter, broader action feasibility
  and attributable outcome feedback remain ahead. Phase 2 stays 1/4 full groups.

## Household food allocation — October 8, 2026

- Branch `codex/phase2-household-food-allocation`: completed food periods now divide
  fulfilled supplies proportionally between occupied families and the unassigned
  resident pool. Whole-unit largest remainders conserve food; permanent IDs resolve
  exact ties. Adult/child demand remains editable. This is simulated coverage, not
  physical meals, invented wealth or a new mortality/fertility rule.
- Saved SUPPLIED/UNDERSUPPLIED diagnostics and consecutive unmet family periods
  survive restart and clear with relief. Historic schema-18 allocation stays UNKNOWN;
  disappeared/reoccupied households begin a new consecutive streak. Exact tie bias
  and future social rationing policy are documented rather than silently locked.
- Transactional schema 19 adds four nullable fields; explicit-column INSERTs prevent
  schema-extension breakage. Allocation shares the food/needs/priority/receipt commit.
  No reset, forced chunk loads, owner config rewrite or production save changes.
- Eight added automated cases; final Windows Wrapper `clean verify -B -ntp` passed
  345 cases (343 unit/two packaged integration), zero failures/errors/skips. Includes
  10,000 seeded conservation scenarios, integer overflow boundaries, ordering/ties,
  a 1,200-period crisis/recovery, real family persistence and migration failure/retry.
- Disposable Paper probe passed supplied family, famine streak, unchanged history
  on restart and recovery after a verified native food donation; unloaded accounting
  still passed. Normal single-plugin startup/status/shutdown passed, no forced chunks.
- Consistent schema-18 confirmed-dev backup first; schema 19 integrity/FKs checked.
  Original settlement/resident/household IDs and family/entity links retained.
  Production JAR excludes probes/JUnit. Human client playtesting and scale profiling
  remain pending, alongside treasury funding/spending, autonomous response execution,
  outcome feedback and paid trade/barter. Phase 2 remains 1/4 complete reporting groups.

### Confirmed player food donations — October 8, 2026

- Branch `codex/phase2-player-food-donations`: sneak-right-click a saved living
  villager with ordinary food, then explicitly confirm the whole held stack in Village
  supplies. Ordinary right-click trading and read-only `/civ aid` remain available.
- Recovery receipt and inventory debit share Minecraft player data. A bounded worker
  verifies the full saved gzip/NBT receipt before the existing SQL owner credits food.
  Failed void saves, repeat clicks, pending operations and stale saved markers cannot
  create duplicate credit or a second debit. Offline/shutdown work resumes from markers.
- Editable donation enable flag; new configs true, existing files without it false.
  Pending recovery continues when new donations are disabled. Protected menu clicks/
  drags and next-tick confirmation; player permissions separate from admin debugging.
- Fourteen added automated cases; final Wrapper clean verify passed 337 (335 unit/two
  packaged integration), no failures/errors/skips. Covers bounded/corrupt/truncated NBT,
  receipt identity, failed saves, retries/shutdown, stale markers and consent guards.
- Real Paper native menu/inventory/player-data checks passed with a synthetic offline
  player facade: intentionally disabled save prevented credit, then saved debit/marker,
  reload, one receipt, crisis relief and duplicate rejection passed. Actual 26.2 path
  players/data verified after first attempt correctly withheld credit on the old path.
- Schema remains 18. Confirmed-dev backup first; original identities/links and
  integrity/FKs retained. Normal single-plugin startup, diagnostics, unloaded fixture
  and shutdown passed. No actual player/production save edits.
  Donation does not pay money/reputation or implement barter/autonomous purchasing.
  Pinned-format upgrade compatibility, human client UX, inventory plugins, scale and
  consistent cross-save backup recovery remain limits. See FOOD_DONATIONS.md.
- Phase 2 still 1/4 full reporting groups; actual player aid is now a verified partial
  feature. Next: household food allocation/wellbeing and broader economic responses.


### Player food-aid previews — October 8, 2026

- Branch `codex/phase2-food-aid-preview`: `/civ aid` now previews an ordinary held
  food stack for the nearest ready village home, names the saved village and shows
  its latest food report. It takes no items, creates no receipt and pays nothing.
- Editable optional `aid-preview` economy.yml section: enabled, same-world 3D range
  (default 32), six ordinary plant-food conversions. Custom items, partial evidence,
  unsupported quantities and ambiguous town ties fail clearly. Explicit target IDs
  still require nearby evidence. Old owner files retain edits and documented defaults.
- Public preview permission defaults true; administrative debug permission remains
  op-only inside its handler. Cached name/identity/food reads avoid main-thread SQL,
  block scans, chunk loads or waits. Schema remains 18; no migration/reset.
- Sixteen added automated cases; final Windows Wrapper clean verify passed 323
  (321 unit/two packaged integration), zero failures/errors/skips. Earlier test fixture
  used a live-registry-dependent ItemStack constructor; fixed the headless facade.
- Real disposable Paper passed native item conversion/custom rejection with a
  synthetic read-only player facade and unchanged goods/food, plus earlier food,
  needs, relief, restart and unloaded scenarios. This is not a human client test. Confirmed-dev schema-18 backup taken first;
  original IDs/family/entity links and integrity/FKs retained. Normal single-plugin
  startup, console routing, unloaded fixture and clean shutdown also passed. Existing
  economy.yml remains untouched. Production JAR excludes probes/JUnit.
- Actual donation/merchant inventory handoff remains blocked on durable cross-save
  recovery. Pinned Paper saveData returns void and PlayerDataStorage may return on
  disabled saving or log a save exception; merely calling it cannot certify a durable
  debit. No actual player data files edited. See PLAYER_FOOD_AID.md.
- Phase 2 stays 1/4 full groups; player-facing preview is a verified partial piece,
  not completed trade/barter. Next: a recoverable actual inventory transfer and
  broader household/economic responses. No new gameplay decision needed here.


### Explicit food delivery accounting — October 8, 2026

- Branch `codex/phase2-food-relief`: trusted internal adapters can record explicit
  delivered food with source, quantity and operation ID. Pending goods enter a future
  food period; retries cannot duplicate imports and new arrivals cannot rewrite old
  catch-up periods. Receipt processing is bounded to 64 per village period.
- Food, needs, priorities, checkpoint and applied receipt marks share one transaction.
  Failure rolls back effects for retry; partial evidence pauses delivery use. Schema 18
  adds the ledger without backfilling or resetting people, histories or reserves.
- Seven new temporary-SQLite cases cover relief, duplicates, arrival timing, rollback,
  restart/pause, input validation, batch limits and schema-17 migration recovery.
  Windows Wrapper clean verify passed 307 cases (305 unit/two packaged integration),
  zero failures/errors/skips. Production JAR excludes probes/JUnit.
- Disposable Paper probe passed a synthetic 12-unit shipment surviving restart,
  clearing zero-production shortage and rejecting duplication, followed by farm
  recovery/unloaded operation. Consistent schema-17 dev backup first; original people,
  households/family/entity links retained, schema 18 integrity/FKs verified. Normal
  single-plugin startup, unloaded fixture and clean shutdown also verified.
- This is backend delivery accounting, not a player inventory handoff, donation
  command, merchant or autonomous supplier. No normal gameplay caller submits goods.
  Player item exchange and full economic responses remain; Phase 2 stays 1/4 full
  reporting groups with these verified partial pieces listed in PROGRESS.md.
  See FOOD_RELIEF.md. No production reset, push, merge or main modification.


### Editable economy prototype — October 8, 2026

- User authorized prototype defaults and easy server-owner famine/feast tuning.
  Added commented economy.yml and strict startup parser; existing edits never overwritten.
- Added independent immutable settings, demand/initial-supply calculations, food output
  bounded by evidenced worker slots/labor, and exact abstract scarcity prices. Recipes
  use 100% normal, 25% famine, 200% feast; no invented farms or automatic deaths.
- Nineteen new cases; Wrapper clean verify passed 261 (259 unit/two integration),
  all passing. Real Paper created the file, accepted 25% edit, preserved its hash/comment
  on restart, served population diagnostics and stopped cleanly. Restored normal settings.
- Schema remains 14, no automatic inventory seeding/live economy/merchant behavior.
  Saved threshold transition still pending; current API rejects silent history changes.
  Phase 2 stays 25% with partial labor/price work; next is observed facility evidence,
  saved economic pacing and runtime integration. No decisions needed for approved tuning.
  Documentation/status/progress updated; no production reset, merge or push.

### Phase 2 commodity accounting — October 8, 2026

- Built commodity reserves, explicit supply/demand flows, duration-sensitive shortage
  reports and exact conservation without Minecraft dependencies or invented production.
- Schema 14 adds explicit account initialization, saved stocks, retained period inputs
  and atomic checkpoints. Rejects repeated/skipped periods and silent policy changes.
- Eighteen added tests: Windows Wrapper clean verify passed 242 (240 unit/two packaged
  integration), all passing. Two 1,200-period crisis/recovery scenarios include saved
  mid-crisis restart; rollback/retry, overflow, immutable ordering and upgrade checks.
- Backed up confirmed disposable DB, deployed on pinned Paper, verified schema-14
  startup/diagnostics/unloaded fixture/clean stop. Original identities/families retained,
  integrity/FKs OK, zero invented economy accounts. No production/world reset.
- Phase 2 25% by one of four reporting milestones; live economy remains pending.
  User deferred personal playtest. Stopping at economic tuning approval before choosing
  consumption/cadence/price rules; no gameplay economy defaults silently selected.
  Feature work local, no merge/push; canonical roadmap unchanged.

### Phase 1 live population gate — October 8, 2026

- Implemented approved managed-villager zombie conversion prevention. Paper verified
  infection canceled and lethal damage still saved the resident's actual death.
- Wired names/age locks, safe nearby births/arrivals, household migration and simulated
  death removal. Native breeding coordinates with the saved calendar/families.
- Fixed accepted-save shutdown draining, enrollment/death publication race and the
  activation-before-unfreeze restart window. Added persistent freeze marker, retry
  backoff and editable nearby/day/night reconciliation budgets. No new schema beyond 13.
- Wrapper clean verify: 224 tests (222 unit, two integration), zero failures/errors/skips;
  nine new automated cases. Real Paper probe passed safe-placement deferral, names,
  babies/adulthood, restart/pending/freeze recovery, native breeding/infection/death,
  five-person migration, simulated death and enrollment/death race. Test code excluded
  from production JAR; normal plugin-only restart/commands/unloaded checks also passed.
- Phase 1 now 10/10 (100% of prototype milestones). Human client/balance/scale checks
  and vanilla trade-state preservation on recreation remain explicit limits; use a
  disposable play world. No merge/push/main/production/world deletion/roadmap change.
- Final deployment review found an older duplicate Civilizations JAR in the disposable
  server. Archived it outside plugins, confirmed the remaining JAR matches the build,
  and repeated the complete Paper probe and ordinary plugin-only restart successfully.

### Resident projection preparation — October 8, 2026

- Schema 13 current incarnations, retained aliases, durable spawn claims, actual-death
  history and immutable owner views. Deterministic reconciliation handles unloaded,
  pending, moved, deceased and ambiguous identities; actual Paper mutation remains pending.
- Fixed stale settlement population after demographic changes, including emptied
  source towns. Read-only debug explanations show saved reasons and migration paths.
- Windows wrapper clean verify: 215 tests (213 unit, two integration), all passing;
  eighteen added cases. Disposable Paper upgrade/restart preserved identities/families,
  showed a month-45 saved arrival while its area stayed unloaded. Integrity/FKs passed,
  backup taken first, server stopped. No production/main/remote changes.
- Phase 1 remains 90%. Zombie conversion/curing choice pending before native lifecycle
  integration; no consequential rule silently chosen. See docs/RESIDENT_PROJECTION.md.

### Work qualification foundation — October 7, 2026

- Schema v9 stores resident skill totals across restarts, migration, death and backups.
  Added immutable job requirements and explainable living/age/skill qualification checks.
- No fabricated employers, hiring, salaries, occupations or learning rates. This is
  the Phase 1 qualification base for later real labor/business systems.
- Windows wrapper clean verify passed 117 cases (115 unit, two integration), including
  skill snapshots/upgrade failure/retry and migrant skill retention.
- Phase 1 7/10 (70%); calendar and Minecraft integration/validation remain. No actual
  DB/world/server changes, merge or push. Centralized temporary prior-schema fixtures.

### Saved migration — October 7, 2026

- Added deterministic household movement driven by observed housing pressure and
  occasional external adults. Families move together; identities and family links stay.
- Schema v8 preserves event sequences and records migration origins. Replay 3 guards
  algorithm/settings changes. All changes/progress/events share one transaction.
- Windows wrapper clean verify passed 107 cases (105 unit, two integration), including
  restored century replay/accounting, whole-family rollback and migration-then-death.
- Phase 1 now 6/10 (60% by checklist); jobs, runtime calendar and Minecraft integration
  plus real-server validation remain. No actual DB/world/server changes, merge or push.

### Atomic demographic persistence — October 7, 2026

- Schema v7 saves monthly clock/settings, observed housing capacity and sequenced
  birth/death explanations. People/links/events/progress commit together or roll back.
- Settings and replay protection prevent silent changes; age-only/global engines
  cannot activate together. Event paging retains every event within a shared month.
- Windows wrapper clean verify passed 94 cases (92 unit, two integration), including
  snapshot-restored century replay across batches and event-write rollback/retry.
- Phase 1 advances to 5/10 (50%): saved headless aging/birth/death verified. Minecraft
  import/projection, jobs, migration, calendar scheduling and server validation remain.
- No actual server DB/world changes, merge or push. Global snapshot cost still needs
  measurement before tick-loop integration.

### Headless births and deaths — October 7, 2026

- Added immutable population snapshots, editable prototype monthly demographic
  policy, eligible housing-constrained births and age-dependent deaths.
- Seeded newborn identities and parent links; structured reasons; deceased records
  retained. Missing capacity blocks births; impossible close-relative births rejected.
- Windows wrapper clean verify passed 85 cases (83 unit, two integration), with
  100-year accounting and replay scenarios. This is not a balanced live simulation.
- Phase 1 remains 40%; demographic saving/runtime integration next. No server/world
  or actual DB changes, merge or push. Settings follow user's prototype authorization.

### Saved logical aging — October 7, 2026

- Added pure immutable resident aging and opt-in schema v6 seed/progress clocks.
  Ages/checkpoints commit together; completed targets never double-age, failures retry.
- Windows wrapper clean verify passed 76 cases (74 unit, two integration), including
  restart/snapshot, write/overflow rollback and a 500-person/1,000-step pure replay.
- Phase 1 remains 40%; aging/catch-up groundwork advances, full demographic and
  calendar/runtime milestones still pending. No server/world/actual DB operations.
- User clarified automatic village participation and approved configurable,
  documented experimental population tuning. No manual join step will be required.

### Family memory — October 7, 2026

- Added saved parent/child and canonical partner relationships; shared parents
  identify siblings across households/settlements. Deceased relatives remain linked.
- Schema v5 protects identities and prevents self-links, duplicate/reversed partners,
  ancestry cycles and in-place overwrites. No demographic or marriage policy chosen.
- Windows wrapper clean verify passed 64 cases (62 unit, two integration), including
  family restart/snapshot preservation and v4 upgrade rollback/retry.
- Phase 1 remains 40%: family portion advanced; jobs and in-game integration pending.
- No actual Minecraft/server database changes, merge or push.

A running record of Civilizations development, major decisions, experiments, successes, and spectacular failures.

---

## 2026-09-27 — Day 1: It Begins

Development of Civilizations officially started.

### The Idea

Make vanilla Minecraft villages feel alive.

Instead of villages remaining static forever, settlements will grow and evolve based on their population, resources, safety, economy, environment, and relationships with other settlements.

### Early Design Decisions

- Civilizations will remain separate from Chronicler/T.O.D.D.
- Built for PaperMC 26.2.
- SQLite will provide persistent settlement data.
- Autonomous construction is an early priority.
- Buildings will be constructed gradually rather than appearing instantly.
- Roads will connect new development.
- Buildings will have functional effects on the settlement simulation.
- Player-created structures must be protected from autonomous development.
- Eventually, settlements will trade, specialize, form relationships, and connect into regional networks.

### First Major Goal

A vanilla village recognizes that it needs additional housing, chooses an appropriate location, and autonomously constructs its first house over time.

If that works, Civilizations is alive.

### Project Foundation

- Created the Maven project using Java 25.
- Initialized Git and published the repository to GitHub.
- Added README and development log.
- Configured the Paper API.
- Paper API pinned to `26.2.build.129-stable` to match the server.
- Confirmed the project successfully builds with Maven.

**Status:** BUILD SUCCESS
### First Successful Server Launch

Civilizations was packaged and installed on a local Paper test server.

The plugin loaded successfully and reported:

> Civilizations is awakening...

Civilizations is officially running inside Minecraft.

### First Settlement Discovered

Civilizations successfully detected its first vanilla settlement in-game.

The initial prototype detects nearby HOME POIs when a player crosses a chunk boundary and announces a newly discovered settlement.

This is intentionally a temporary detection method. Future settlement identification will use multiple signals such as villagers, beds, job sites, bells, and POI clustering.

**Milestone:** Civilizations can now recognize settlement activity in the Minecraft world.

### Stonevale — First Persistent Settlement

The first permanently recognized settlement was created: **Stonevale**.

Civilizations now:
- Generates settlement identities and names.
- Stores settlements persistently in SQLite.
- Records their world, center coordinates, UUID, and discovery time.
- Searches for existing nearby settlements before creating new ones.
- Preserves settlement identity across server restarts.

Stonevale survived its first restart and was correctly recognized by name.

### Stonevale Gets a Census

Civilizations can now track settlement population.

Population was added to the persistent settlement model and SQLite database, including a migration path for existing databases.

When a known settlement is encountered, Civilizations:
- Counts nearby villagers.
- Updates the settlement's population.
- Saves the census to SQLite.
- Displays the current population to the player.

Stonevale's first recorded census reported **11 residents**.

The current census uses nearby villager entities and is intentionally approximate. Future residency logic will associate villagers more precisely with settlement boundaries and POIs.

### Next Goal

Give settlements awareness of their housing capacity.

Once Civilizations understands both **population** and **available beds**, Stonevale can identify a housing shortage — the first step toward autonomous construction.

### Stonevale Develops Needs

Civilizations can now evaluate basic settlement housing needs.

Settlements track:
- Population
- Available beds
- Housing status

Stonevale's first housing census recorded:
- Population: 11
- Beds: 13
- Housing: ADEQUATE

A new Needs Engine compares population against housing capacity. When population meets or exceeds available beds, the settlement identifies a `HOUSING_SHORTAGE`.

This establishes the first autonomous decision-making system in Civilizations.

### Next Goal

Turn a housing shortage into action:

`HOUSING_SHORTAGE → Construction Request → Site Selection → House Construction`

This will be the beginning of autonomous settlement development.

## 2026-10-07 — Reproducible persistence foundation

- Fixed the settlement INSERT to persist all nine model fields, including beds.
- Added the official Maven Wrapper pinned to Maven 3.9.11, JUnit 5 and pinned build plugins.
- Six temporary-SQLite tests cover initialization, insertion, restart persistence, retrieval and isolated census updates.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed: six tests, zero failures/errors/skips.
- Read and verified AGENTS.md and all six canonical design documents. The nine-phase roadmap supersedes the historical construction-next goal above: foundation and persistent population come first.
- Verified both wrapper scripts exist and are not hidden in the OneDrive checkout. Recommend keeping active Git and live server databases outside cloud sync; no relocation or live database changes performed.
- Next engineering milestone: explicit transactional schema migrations with fresh-install, legacy-upgrade and failure/recovery tests. Gameplay semantics remain open as documented.

### Transactional schema migrations

- Added ordered SQLite user-version migrations for base settlement identity, population and beds. Existing prototype rows are preserved rather than reset.
- Removed exception-swallowing ALTER logic; upgrades commit atomically and roll back on failure. Unsupported newer schemas fail clearly, and initialization closes failed connections.
- Added ten migration cases using temporary DBs: fresh/reopen, legacy and incremental upgrades, rollback/recovery, malformed/newer schema rejection and connection cleanup.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed all 16 tests with zero failures/errors/skips. No Paper smoke test or live database upgrade performed.
- Next milestone: CI and backup/development-environment procedures, before simulation gameplay work.

### CI, SQLite snapshots and recovery procedures

- Added Windows/Linux JDK 25 wrapper CI with pinned official action commits, read-only permissions and no deployment. actionlint 1.7.7 passed; hosted CI has not run on this local branch.
- Added explicit cross-platform wrapper line endings and Git ignores for local dev servers, backup directories and Git bundles.
- Built a standalone read-only SQLite snapshot tool using VACUUM INTO and integrity checks, with refusal to overwrite destinations. It never initializes/migrates/restores the source DB.
- Added seven temporary-database backup tests and one packaged-JAR CLI integration test via Maven Failsafe. Normal and empty-cache Windows wrapper `clean verify` builds passed all 24 cases.
- Documented exact Paper build/checksum setup, localhost isolation, source/world/config/SQLite backup procedures and restoration into a new disposable directory.
- No live database/world/server operation or Paper smoke test performed. No merge or push. Phase 1 continues with headless simulation/debug tooling and persistent population foundations; consequential discovery/occupancy/time rules remain open.

### Deterministic headless simulation runner

- Added an immutable-checkpoint stepping API with explicit seeds, logical targets, per-call step budgets and remaining work. Random draws are stable across batching and reconstructed checkpoints.
- Added a packaged synthetic checksum harness without Paper/world/database/time access. It is a diagnostic probe, not implemented population gameplay.
- Fourteen runner unit cases cover replay, bounded work, failure retry, ordering, long boundaries, invalid inputs and one-million-step acceleration; one packaged-harness case checks separate JVM runs across budgets.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 39 cases (37 unit, two integration), zero failures/errors/skips. A fixed probe fixture guards the replay convention.
- Documented pure immutable step requirements, CPU-budget limitations, CLI usage and outstanding persistence/versioning needs. Schema unchanged; no world/server/production DB changes.
- Next: persistent resident/household domain and storage. Birth/death/migration rates, calendar pacing and discovery/seeding policy are not silently chosen.

### Plain-English roadmap tracker

- Added docs/PROGRESS.md with bars for every phase and checklists explaining the eventual Minecraft experience.
- Initial reporting snapshot: three of ten Phase 1 milestones complete; later phases have no completed reporting milestones. Percentages count milestones, not hours or deadlines.
- Added an AGENTS.md maintenance instruction and linked the tracker from README. Canonical roadmap requirements and gameplay behavior are unchanged.
- Reviewed tracker against ROADMAP and verified PROJECT_STATUS. Documentation-only change; no build/test rerun needed.

### Persistent residents and household identities

- Added immutable Resident/Household records and a focused population repository. People retain stable identities, names, logical age, household assignment and living/deceased status across restarts.
- Schema v4 adds indexed population tables with foreign key/age/status constraints. Household member batches commit atomically; duplicate/missing/mismatched identities fail, and failed batches can retry.
- Existing settlement rows/census preserved; no resets, automatic villagers-to-residents conversion, live DB operations or new gameplay tuning.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 58 cases (56 unit, two integration), including restart, constraints, rollback/retry, v3 upgrade/recovery and population backup round-trip coverage.
- Phase 1 advances to four of ten reporting milestones complete (40% by checklist). Household storage is present; family ties/jobs and live gameplay integration remain pending. Docs/POPULATION.md records limitations.

## October 7, 2026 — disposable Paper startup and recovery

- User accepted the EULA for the isolated localhost test. Guarded launcher checks
  the disposable marker, pinned Paper checksum, Java 25 and isolation properties.
- Paper 26.2-129 enabled schema-9 Civilizations, restarted, and ran a restored
  source/world/config/SQLite copy. Clean shutdown verified after each run.
- Verified Git bundle, 68 world/config file hashes, SQLite integrity and restored
  source wrapper build (117 passing tests). No production data or main/remote changed.
- Incomplete Minecraft download retried with exact required checksum; non-fatal
  Windows performance-counter error documented without OS changes.
- Phase 1 remains 70%; hosted CI and real population integration remain pending.

## October 7, 2026 — saved civilization calendar

- Schema 10 adds elapsed-time anchor, partial month and durable queued target.
  Population catch-up can fail/restart without losing elapsed time or double-aging.
- Pure year/month conversion, backward-clock handling, explicit elapsed caps and
  overflow failure; runtime scheduling remains pending, so progress stays 70%.
- Fourteen new tests; Windows wrapper clean verify passed 131 cases (129 unit,
  two integration), zero failures/errors/skips. Temporary databases only.
- User approved automatic participation for natural and player-built populated
  villages. No merge/push, production database or world modification.

## October 7, 2026 — automatic village observation foundation

- Added immutable world/bed/villager snapshots and pure bounded cluster recognition,
  including deterministic nearest-bed assignment and partial-observation flags.
- No fixed grid/player position identity, duplicate counting, bed-only enrollment
  or silent oversized scan truncation. Existing runtime listener remains unchanged.
- Seven new tests; Windows wrapper clean verify passed 138 cases (136 unit,
  two integration). Phase 1 remains 70% pending Minecraft integration.
- User approved configurable adult partner formation without invented child ancestry.
  No new server/world/production DB changes, merge or push.

## October 7, 2026 — approved adult partnership prototype

- Explicit pure configurable adult pairing, with known-blood-relative exclusion,
  living-partner exclusivity and retained deceased relationship history.
- Existing child ancestry remains unknown. Seven tests include repeatability,
  eligibility, historical partners and birth integration. Wrapper clean verify:
  145 passing cases (143 unit, two integration), zero failures/errors/skips.
- Not yet wired into import/monthly simulation; existing replay/schema unchanged.
- User approved world UUID and saved bed/villager matching; ambiguous town overlaps
  must produce diagnostics, not an automatic merge. Phase 1 stays 70%.

## October 7, 2026 — permanent village identity storage

- Schema 11 saves world UUID/display-name evidence and immutable-owner settlement,
  bed and entity/resident links. Matching reports new/existing/conflicting evidence.
- No implicit merge, legacy adoption, census overwrite or absent-data deletion.
  Migration respects permanent world IDs rather than duplicate/renamed folders.
- Nine new temporary-DB tests; wrapper clean verify passed 154 cases (152 unit,
  two integration), zero failures/errors/skips. Runtime integration remains pending.
- No actual DB/world/server change, merge or push; Phase 1 remains 70%.

## October 7, 2026 — schema-11 Paper follow-up

- 154-test JAR from 62550ee enabled on the authorized disposable Paper server.
  Test database upgraded from schema 9 to 11; read-only integrity check ok.
- Server reached Done and stopped cleanly. No production data changed.
- Reachable tracked history checked for common credential/private-key signatures;
  no matches. Hosted CI/publication still pending; gameplay integration still upcoming.

## October 8, 2026 — atomic enrollment and hosted CI

- Schema 12/atomic enrollment imports stable people, shared homes, housing evidence,
  approved prototype partners and headcount. Repeat visits never overwrite saved ages
  or resurrect deceased people. Partial observations invalidate housing confidence.
- Eleven new tests include rollback/retry, explicit legacy/overlap protection and
  calendar-driven saved births. Wrapper clean verify passed 165 cases (163 unit,
  two integration), zero failures/errors/skips; Paper runtime integration remains.
- With explicit approval, pushed only e85416e on codex/phase1-foundation-ci. Hosted
  Windows/Linux both passed run 37725399200. Enrollment remains local; main unchanged.
- CI/Paper/recovery milestone complete: Phase 1 8/10 (80%). No merge or production change.

## October 8, 2026 — dedicated population owner and timer

- One thread owns JDBC/imports and a real fixed-delay monthly catch-up timer.
  Immutable snapshots, documented editable test pacing/offline cap, bounded request
  queue and explicit failure/backlog diagnostics; no Bukkit calls.
- Historical catch-up precedes new enrollment; lifecycle/callback shutdown avoids
  waiting on itself. Ten new temporary-DB tests, including timer and write retry.
- Wrapper clean verify passed 175 cases (173 unit, two integration). Plugin wiring
  remains upcoming; Phase 1 stays 80%. No new server/real data/remote/main changes.

## October 8, 2026 — Paper calendar and inspection

- Wired asynchronous owner-thread startup/shutdown, strict configurable prototype
  inputs and read-only admin status/settlement commands. Old census listener retained
  but unregistered; observer/import/projections remain pending.
- Thirteen new configuration/permission/diagnostic cases; wrapper clean verify passed
  188 tests (186 unit, two integration), no failures/errors/skips. Initial sandbox
  target resolution denied; expanded-access verification passed.
- Disposable DB snapshot before deployment; pinned Paper startup, real month advance,
  commands, clean stop/restart at saved month 1, then month 3 verified. No player or
  projection test yet; no production/main/remote change. Phase 1 now 9/10 (90%).

## October 8, 2026 — automatic loaded-world village import

- Bounded main-thread bed snapshots and loaded-entity reads automatically feed the
  existing identity/enrollment owner. Clear-border/loaded coverage checks distinguish
  known housing from partial evidence; no chunk tickets or world/entity modifications.
- Read-only observation diagnostics and configurable adapter budgets. Caught/fixed
  pinned Paper negative-height section indexing during the first real smoke test;
  added regression coverage. Nine new cases; clean verify passed 197 tests
  (195 unit, two integration), no failures/errors/skips.
- Disposable Paper fixture imported two people/one household/one settlement, capacity
  four. Re-observation/restart retained IDs and links; saved ages advanced exactly
  eight months while clock progressed 27 to 35. Unloaded area stayed unloaded while
  simulation continued. Schema 12/integrity/FK checks passed; server stopped and
  manually applied fixture tickets removed. No production/main/remote changes.
- Phase 1 remains 90%; visible resident projection and vanilla lifecycle coordination
  are the next batch. Large-server profiling and actual client play remain pending.

## October 8, 2026 — live field-backed food economy

- Saved independent food clock and transactional period effects now use actual saved
  living residents plus loaded crop snapshots near household homes. No harvesting,
  player-block changes, invented farms, business jobs or chunk force-loading.
- Schema 15 retains evidence/clocks and forward tuning history, backfills original
  period policies and preserves old explanations. Partial/conflicting evidence pauses
  accounting. Offline villages use their last reliable observed capacity.
- Read-only economy diagnostics; commented farm settings with old-file compatibility.
  Added 13 automated cases. Windows Wrapper clean verify passed 274 cases (272 unit,
  two packaged integration), zero failures/errors/skips.
- Consistent disposable schema-14 backup before deployment. Guarded real Paper probe
  passed actual field supply, food depletion/severe shortage, forward feast recovery,
  restart person/history preservation and accounting while chunks remained unloaded.
  Probe archived outside plugins afterward; normal startup/shutdown also verified.
- Phase 2 remains 25% by completed reporting groups. Treasury/wellbeing, autonomous
  decisions and player transactions remain; no starvation deaths. Current demographic
  checkpoint is used during food catch-up, not exact historical interleaving. Large
  city profiling and human client play remain outstanding. No push/merge/main changes.

## October 8, 2026 — household needs and explicit treasury accounting

- Food periods now save household demand/age counts, known/unknown/crowded housing
  and unassigned living residents atomically with stocks/history/checkpoint. Food
  coverage remains shared village evidence, not invented family rations/prosperity.
- Transactional schema 16 preserves old data without fabricating past needs. Added
  exact Trade Value treasury accounts/ordered ledger: explicit sources, operation-ID
  retry protection, no negative funds or guessed live income/personal wallets.
- Fourteen added automated cases; Windows Wrapper clean verify passed 288 cases
  (286 unit/two integration), no failures/errors/skips. Long 1,200-period crisis/
  recovery replay, historic evidence, failed-save rollback/retry and migration tested.
- Real guarded Paper probe passed household diagnostics and preserved needs during
  restart/feast retuning alongside actual field/food/unloaded checks. Dev schema-15
  snapshot first; identities/families preserved, SQLite integrity and FKs verified.
- Status/progress/docs updated. Phase 2 remains 25%: broader wellbeing and live budget
  flows, autonomous responses and player trade remain. No demographic penalties,
  production save reset, remote push/merge or main modification.

## October 8, 2026 — explainable village priorities

- Pure evidence-to-priority selection now chooses seek food, investigate observed
  crowding or monitor. Feasibility is mandatory; seeded weighted draws choose among
  reasonable alternatives. Editable cadence/weights live in an optional economy
  section; old owner files remain untouched and compatible.
- Schema 17 retains facts/settings/seed for exact explanation replay, transactionally
  with food and needs. Changed pressures trigger early reviews; recovery clears crisis
  focus. No real purchases, supplier invention, project permissions or block changes.
- Twelve added cases; Wrapper clean verify passed 300 cases (298 unit/two integration),
  zero failures/errors/skips. Seed sweeps, alternative selection, long replay, rollback,
  upgrade failure/retry and original treasury balances/receipts tested.
- Guarded disposable Paper probe passed crisis priority, saved restart identity/history
  and feast recovery to monitoring, alongside actual food/household/unloaded checks.
  Consistent schema-16 dev backup first; original identity/family data preserved.
- Updated plain-English tracker with partial milestones for all four P2 groups. Phase
  gate still partial: execution/outcome feedback, full wellbeing, actual budget flows
  and player trade remain. No production reset, remote push, merge or main change.
