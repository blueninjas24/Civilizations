# Civilizations Development Log

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

Nine added tests (eight SQLite runtime and one strict configuration). Initial
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
