# Civilizations — implementation status and handoff

## October 11, 2026 — shared warehouse startup grants verified

Approved warehouses now have an explicit transactional startup adapter sharing
mine/workshop food protection and period/lifetime caps. Real treasury funds become
recorded startup expense and empty-store purchasing capital; a qualified first
worker is required. Existing configs keep their saved industry lists. New defaults
include warehouse without changing any numerical cap. No automatic firms/buildings
or new migration; schema remains 51. See STARTUP_GRANTS.md.

Six added tests; focused 27 before the final config case, full Windows Wrapper
clean verify passed 908 (906 unit/two integration), zero failures/errors/skips,
6:12. Packaged Paper grant probe passed shared-budget refusal, empty warehouse,
real funding, qualification and restart/disabled replay. Own DB has two grants,
9000 cash + 1000 recorded startup expenses = 10000 original funding; integrity/FKs
healthy. Main 84/87 tables unchanged; only clocks/ore bookkeeping differed, no
population month or asset/history changes. Fixture stopped normally, exact
properties restored, test JAR archived, no forced chunks. Human server untouched.
Next: bounded vacancy hiring at existing live-owner businesses, then opportunity
training/automatic nonfood founding. Phase 3 remains 25% by complete groups.

## October 11, 2026 — business tool upkeep verified

Existing timed mines/workshops buy real replacement tools with business money;
tools wear only during positive production. Editable grace, wear, slowdown and
price are in economy.yml. Warehouses can buy configured workshops' actual output.
Maintenance and recipe demand share funds/storage; depleted/untrusted mines create
no tool pressure. Service/fractions, use receipts and production commit together.
Legacy files stay opted out; retained unsupported store catalogs are preserved.
Schema 51 adds two empty service/audit tables without free kits or asset backfill.

16 tests added; final focused 23 and Windows Wrapper clean verify passed 902
(900 unit/two packaged integration), zero failures/errors/skips, 6:13. Actual
PopulationSession on isolated Paper passed upkeep and prior warehouse checks:
two real tool uses, reserve four, cash 10000 conserved, saved service/fractions,
restart and absent-world pause. SQLite integrity/FKs healthy. Main 79/85 old
tables unchanged; other differences verified as exactly one normal population
month plus clock/tracking bookkeeping. All assets/history preserved. Fixture
stopped, exact settings restored, probe archived; no forced chunks/probe world
writes. Human server untouched. See BUSINESS_TOOLS.md and DEVLOG.md.

Phase 3 remains 1/4 complete groups (25%): automatic nonfood founding/careers,
wider industries and specialization remain. Approved warehouse startup grants
under existing shared food-protected limits are next.

## October 11, 2026 — upstream warehouse restocking verified

Existing staffed warehouses now buy actual extracted local mine goods for funded
workshop input gaps. Real business cash/stock transfer, delayed delivery, shared
hauling and village-wide pending coverage connect mine → warehouse → blacksmith
without manual stocking. Qualified staff, bounded complete reviews and storage
are required. No new firms, money, markup or finished-goods buyers are invented;
missing old warehouse-supply settings stay disabled. Schema remains 50.
See WAREHOUSE_RESTOCKING.md. Business tool upkeep was approved during this batch
and is the next implementation, not a feature of this build.

14 tests added; focused 38 and Windows `.\mvnw.cmd clean verify` passed all 886
(884 unit/two packaged integration), zero failures/errors/skips, 6:13. Packaged
Paper actual PopulationSession chain/replay/world-pause/restart/disable passed:
four paid shipments, one tool, finite reserve eight, total cash 10000 conserved.
Both SQLite saves healthy. Only main clock/ore-tracking bookkeeping changed;
all main businesses, goods, finances, resources, residents and history unchanged.
Fixture stopped normally, exact settings restored, probe archived, no forced
chunks. Human server untouched. Automatic nonfood founding, actual finished-tool
needs, wider industries and specialization still prevent the next Phase 3 gate.

## October 11, 2026 — funded industry input review verified

Existing workshops now have a read-only input-demand review using actual qualified
staff, business money, storage, owned/pending goods, configured recipes and editable
input buffers/base prices. Available warehouse goods cover village gaps once across
customers. No purchase, reservation, production, new consumer or money is generated.
The SQL-owner bridge matches only uncovered mineral demand to current valid evidence;
partial/disabled/absent-world reviews defer. No new automatic scheduler or setting
default; schema remains 50. This is candidate pressure, not startup authorization or
finished-goods demand. Suitable sites, career/founder choice, existing producer/
transport alternatives and automatic investment still need connecting.

15 added tests; focused 21 before the final selection case, full Windows
`.\mvnw.cmd clean verify` passed 872 (870 unit/two packaged integration), zero
failures/errors/skips, 5:43. Guarded packaged Paper paid-stock/pending-cargo/money/
actual-owner-reopen/world-pause probe passed. Reopened synthetic DB integrity/FKs
healthy, conserved total 10000 cash, finite reserve eight, two paid pending shipments
with three goods, no tools/grants. Not natural startup/client/customer proof.

Stopped disposable fixture backed up; main remains schema 50. 74/86 tables unchanged;
remaining changes verified as exactly one normal month/food period with prior history
retained. Business assets, financial, skill, family and identity data unchanged.
Normal stop, exact settings restored, probe archived, no forced chunks reported.
Human server untouched; local feature branch only, no push or main merge. See
INDUSTRY_DEMAND.md and root DEVLOG.md for scope and corrected test/setup assumptions.
Phase 3 remains 1/4 complete groups (25%); actual customer needs and automatic careers/
funded opportunity selection remain next.

## October 11, 2026 — current mineral surveys verified

Schema 50 distinguishes the latest complete coal/iron survey from retained positive
sighting history. Existing observer saves both atomically. Newer empty/smaller frames
replace current membership without erasing history; older scans cannot restore it.
Same capture replay is immutable, conflicting raw coordinates roll back. Legacy
sightings receive no fabricated current frame. No business/resource/cash rewrite.

The SQL-owner review now gates candidate coordinates on known world availability,
healthy mutation tracking, caller-supplied freshness, nonfuture capture, exclusions
and global deposit claims. It cannot authorize a mine, infer hidden ore or establish
real demand/site permission/qualifications. Automatic founding remains unfinished.

13 new tests; focused 42 and Windows `.\mvnw.cmd clean verify` passed 857 (855 unit,
two packaged integration), zero failures/errors/skips, 6:02. Isolated packaged Paper
owner/world-pause/expiry/empty-frame/older-replay/history/restart check passed; reopened
probe retained capture 2000, no current members and one historical sighting, no money
or businesses. Main migration integrity/FKs healthy, 81/84 old tables unchanged;
only normal time/runtime/tracking updates. All existing business, resident, resource,
financial and identity history preserved. Fixture stopped; exact properties restored,
probe archived, no forced chunks reported. Human server untouched, no push/main merge.
See root DEVLOG.md for setup corrections and MINERAL_OPPORTUNITIES.md for contracts.

Phase 3 remains 1/4 complete groups (25%). Next: actual demand, qualified careers,
coherent suitable resource patches and funded automatic industry selection.

**Verified against local source, Git, Wrapper and disposable Paper on October 10, 2026.** Phase 1 prototype gate passed (10/10 milestones). Phase 2 prototype gate passed (4/4 reporting milestones). Implemented features include live field-backed food, household allocations, saved priorities and appeals, verified donations, once-only opening funds, budget-backed emerald food purchases, saved emerald bargaining, finite-stock player barter and resident-linked food/housing welfare. Bounded food-priority learning and saved feasible donation/emerald/material/wait response comparisons are verified, with actual confirmed trade receipts and exact food outcomes. Phase 3 now has automatic funded field enterprises, persistent owners/sites, qualified staffing and worker-backed food; its first reporting group is verified (1/4, 25%). Existing funded mines/workshops now run timed worker-backed production. Workshops can automatically purchase actual local warehouse inputs. Automatic nonfood founding, upstream warehouse supply, finished-goods customers, wider competition and specialization remain unfinished. Qualified succession, paid resale, retained unsold closure, actual mine/workshop/farm employee work learning, supply-backed study and automatic funded resale are verified. Phases 4–9 remain planned. A human client verified recognition, donations, emerald sales and resident restart persistence; broader inventory-plugin compatibility, barter/bargaining client coverage and survival balancing remain validation limits. This is not a balanced release.




## October 10–11, 2026 — guarded village startup grants

Approved public startup funding now has editable settings and atomic mine/workshop
startup composition. Schema 49 retains one receipt/business with actual expense/cash
allocation, protected food money, food evidence, before/after treasury and policy.
Current unmet/missing/stale food assessment or changed living population defers
startup. Single/current-food-period/lifetime budgets survive restart; accepted terms
replay after disable without new money, quota, stock or ore refill. Failed startup
or receipt insertion rolls back the whole operation; no retroactive grants.

Defaults: mining/blacksmith requests,50 Trade Value single/current village food
period and 200 lifetime, keeping max(25 Trade Value, two assessed food-demand periods
estimated at 2 Trade Value/unit). Caps limit public spending, not private ownership.
Old files remain disabled until opted in. No automatic caller, public command,
private-startup funding, loan/payroll or world modification added. Caller still
establishes actual demand/current suitable evidence. Automatic career/founding next.

Sixteen added tests; focused 83 and Windows `.\mvnw.cmd clean verify` passed 844
(842 unit/two packaged integration), zero failures/errors/skips,6:12. Guarded
packaged Paper defaults/refusal/conserved startup/mining/disabled restart replay
passed in a separate synthetic DB, with healthy reopened integrity/FKs and original
receipt/cash/job/stock/reserve. It is not natural ore/automatic founding/client proof.
Test-only compilation issues corrected before successful runs; application source
unchanged after full build. See root DEVLOG.md for exact failures/corrections.

Stopped marked dev schema 48 backed up; upgrade 49 healthy and 76/82 old tables
unchanged. Only normal age/clock/runtime/world/tracking elsewhere; existing assets,
food/trade/financial/identity/skill/opportunity history preserved, main grant table
empty. No world edits/tickets, no forced chunks verified, normal stop exit 0, exact
settings restored and probe archived. Human server/world/DB untouched. Local feature
branch only; no push/main merge. Phase 3 remains 1/4 groups(25%). See STARTUP_GRANTS.md.

## October 10, 2026 — durable mineral opportunities

The live coal/iron survey now retains positive evidence across restart in schema48:
settlement/world UUID, coordinate, mineral and first/latest capture timestamps.
Repeat and older evidence do not duplicate observations; incomplete/empty scans
do not erase history. Validation and saving are atomic. Current saved deposit links
and placement/removal exclusions explain historical evidence at read time, without
refilling reserves or granting a mine/site. A bounded immutable SQL-owner diagnostic
page supports the next opportunity-selection work; no new player command yet.

Nine added tests cover temporary SQLite rollback/replay/restart/exclusions/gaps,
bounded diagnostics, migration failure/retry and accepted owner work during shutdown.
The first full build exposed a readiness/shutdown race in the existing survey seam;
fixed accepted work to drain using owner initialization/healthy tracking, while new
submissions still reject and gaps still pause. Focused 33 before the final owner test,
then corrected focused 29. Final Windows `.\mvnw.cmd clean verify` passed 828
(826 unit/two packaged integration), zero failures/errors/skips, 5:38.

Guarded packaged Paper scheduled snapshot/exclusion/SQL-owner persistence passed.
Reopened probe DB retained exactly one expected coal coordinate/capture time and
no business/deposit; integrity/FKs healthy. Fixture blocks restored, temporary
console chunk tickets removed and verified absent, normal stop exit0. Main dev
schema47 backup upgraded to48 with 67/81 old tables identical; normal age/time/world,
one food period and observation updates explain the rest. Business goods/cash,
mineral reserves, family/skills, trade receipts and treasury stayed unchanged.
Main opportunity table empty; synthetic evidence remains in a separate probe DB.
Settings restored exactly and probe archived; human server/world/DB untouched.

Limited configurable village startup grants with food-budget protection explicitly
approved and recorded, not yet implemented. Next: grant eligibility/accounting,
funded career choice and automatic mine/workshop selection with actual demand.
Phase 3 remains 1/4 complete groups (25%). Local feature-branch work only; no push
or main merge. See MINERAL_OPPORTUNITIES.md and root DEVLOG.md for full verification.

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


## October 9, 2026 — qualified business succession

Latest follow-on: unsold expiry now records retained closure and releases all open
jobs atomically after qualified inheritance review. Expired firms stop new work
before their bounded closure visit. New mining/recipes/hiring/trade/capital are
blocked; paid cargo and exact historic receipts remain safe. Farms cannot silently
restart under their stable identity. Diagnostics retain the reason/date and no
longer falsely say succession is unimplemented. Schema 41 is additive. Nine new
closure cases plus one farm regression; final focus 43 passed, broader pre-final
focus 73 passed; Windows Wrapper clean verify passed 744 (742 unit/two packaged
integration), zero failures/errors/skips. Native Paper passed staffed work until
expiry, ended jobs, retained coal3/reserve2 and stopped output across restart;
other mine coal5/reserve5, inherited owner and paid replay retained. Funds stay
8500 treasury/750 cash/250 estate. First harness read connection omitted required
foreign keys; corrected only the harness and reran. Both runs stopped normally,
settings restored, probes archived; old dev assets retained, integrity/FKs healthy.
User-server unchanged. Automatic valuation/listing creation/buyer selection and
wider competition remain. Phase 3 stays 25%. See BUSINESS_CLOSURE.md.

Next qualification work: user approved gradual resource-backed training for
unemployed adults and learning through actual employee work, with editable costs,
speed and limits. Ownership thresholds remain required. No training is implemented
by this closure batch; automatic nonfood founding remains unfinished.

Latest follow-on: schema 40 saves explicit sale offers with immutable price/deadline,
online/present-world elapsed time, resolved ownership and atomic SOLD completion.
Normal refusal no longer falsely reports a population save failure. Failed world
boundaries suspend new sales until reviewed restart; funds/history remain unchanged.
Focus 55 passed; Windows Wrapper clean verify passed 734 (732 unit/two packaged
integration), zero failures/errors/skips. Native Paper passed exact saved 250-price
sale, inheritance/staffed finite work, replay, absence/restart and conserved
8500 treasury/750 business cash/250 estate. Normal stop, restored settings, archived
probe; schema-39 dev assets retained and schema-40 integrity/FKs healthy. User-server
unchanged. Automatic offer creation/valuation/buyers and retained closure remain;
EXPIRED records a deadline, not closure. Phase 3 stays 25%. See BUSINESS_SALE_LISTINGS.md.

Latest follow-on: explicitly approved qualified-existing-staff operation after
owner death is configurable across timed mines/workshops, warehouse supply and
automatic farms. No staff/owner/estate money is invented. Farms reconcile lost labor,
retain remaining staff and use current ownership for later staffing after succession.
Schema stays 39. Wrapper clean verify passed 725 (723 unit/two packaged integration);
focus 91 passed. Native Paper verifies a deceased-owner mine producing three coal
with a worker qualified for work but not ownership, no promotion, finite reserve
five -> two, plus inherited mine/sale/replay/world pause/restart and conserved funds.
Normal stop, probe archived, settings restored; dev assets retained, integrity/FKs
healthy. User's own server unchanged. See BUSINESS_OPERATIONS.md. Automatic listings,
valuation/deadlines/closure/competition still unfinished; Phase 3 remains 25%.

Latest follow-on: paid resale accounting and separate family estate funds are
verified. Qualified buyers use real owned-business spare cash and/or eligible
prior-sale estate proceeds under editable limits. Inheritance takes priority.
Payment, estate credit, audit and owner change are atomic; retry does not pay twice.
Internal session API rejects new absent-world/future-date sales. Schema 39 retains
prior assets. Wrapper clean verify passed 715 (713 unit/two packaged integration);
focus 81 passed. Paper owner fixture passed a 250 cash-to-estate purchase, replay,
pause/restart and 3 coal/7 reserve, retaining treasury 8500 and cash/estate total 1000.
Marked dev assets retained; normal stop, settings restored, probe archived. No own
server/world changes. Automatic listings/valuation/deadlines/closure/competition and
estate-backed new founding remain unfinished. See BUSINESS_RESALE.md. Phase 3 25%.

Business founding remains immutable and separate from current ownership. Present
worlds' bounded business refresh now transfers a deceased owner's firm to an
eligible industry-qualified recorded family member first, then a qualified current
employee. Alive/moved owners are not treated as deceased. Age, skills, settlement
and current industry employment are checked. Multiple eligible relatives use stable
UUID ordering; unrelated housemates are not family. No successor leaves ownership
unchanged pending resale. No invented training, employment, grants or money transfer.

Schema 38 transactionally adds ownership audit events/index. Owner update and audit
commit together; failure rolls both back. Original founding replay charges nothing
again after transfer. Current diagnostics/industry operations read the current owner.
Six new temporary SQLite cases cover family priority, employee fallback,
qualification/occupation refusal, rollback/retry, absence/runtime integration and
37->38 failed migration/recovery. Focused 53 passed; full Windows Wrapper clean
verify passed 698 (696 unit/two packaged integration), zero failures/errors/skips.

Actual Paper owner fixture passed qualified inheritance, exactly one event, three
coal/seven remaining bundles, world pause and restart. Initial native assertion
connection omitted foreign-key enforcement and was corrected; application behavior
was not relaxed. Both runs stopped normally; successful probe archived, original
network properties restored. Fresh fixture integrity/FKs healthy, treasury 8500 and
business capital 1000 retained. Marked dev industry/funds/identity matched pre-upgrade
backup; its existing resident/calendar advanced normally while online. No probe block
writes/tickets, and no changes to the user's own server. See BUSINESS_SUCCESSION.md.

Automatic resale, closure/competition, per-block resource retirement and
automatic wider industries remain upcoming. Buyer estate/business spare-cash sources
with configurable limits are approved. Phase 3 remains 1/4 complete groups (25%).

## October 9, 2026 — human playtest and trade responsiveness

Human playtest passed village recognition (seven imported residents), a two-bread
donation worth six food units, and a two-bread sale for three emeralds with correct
inventory changes. Normal restart retained six living residents; the log recorded
one prior native zombie death. Old unbound legacy development settlement data
initially blocked enrollment; with the server stopped and user approval, its database
was archived and fingerprint-verified, preserving world and settings. Existing
populated saves are not generally disposable. Economy speed temporarily changed
60 -> 10 seconds for testing and restored to 60 while stopped, with config backups.

The observed roughly 15-second trade wait matches five-second polling across
multiple asynchronous proof/ledger stages. Purchase, barter and donation adapters
now poll every five ticks (250 ms at 20 TPS), retaining four-player work caps,
main-thread inventory handling and verified-save/SQL recovery checks. This reduces
scheduler-induced waits; disk/SQL load and player count still affect actual latency.
Focused transfer verification passed 29 cases, including a new real scheduled-run
path case proving one payout, debit, delivery and confirmation under repeated polls.
Windows Wrapper clean verify passed 692 tests (690 unit/two packaged integration),
zero failures/errors/skips. Updated JAR installed while the user's dev server was
stopped; previous JAR backed up, installation hash verified, database/settings
fingerprints unchanged. User-client confirmation timing on this build remains pending.
Schema remains 37; this fix changes no saved transfer format or database schema.

Qualified family -> qualified employee -> affordable qualified sale -> closure is
approved, with editable valuation/deadline and a separate persistent family estate
fund. Suitable permitted vacant premises take priority over new construction;
physical reuse is Phase 4. Player-mined ore retires only the affected coordinate's
source, not the full vein. These rules are recorded but not yet implemented.
Next: immutable founder/current-owner separation, tested succession/resale accounting
and per-coordinate reserve retirement. Phase 3 remains 1/4 reporting groups (25%).

## October 9, 2026 — transport clock retuning safeguard

Branch `codex/phase3-haul-clock-retuning`. A reproduced bug made an old shipment
falsely exhaust today's transport after industry speed changed: window numbers
alone alias different actual times. New automatic shipment terms and quota queries
include cadence plus window. Shared limits still bind buyers under the same rate;
immutable replay refuses changed terms. Manual quota remains separate. Retuning
starts a new rate namespace without changing paid cargo's ownership, prices or
arrival clock. Schema 37 transactionally adds bounded nullable haul_cadence and
an index; old automatic rows keep unknown cadence, not guessed current settings.

Three new temporary SQLite cases cover the actual regression, quota/replay and
failed 36->37 migration with later legacy arrival. Corrected focus 10 passed;
final Windows Wrapper clean verify passed 691 (689 unit/two packaged integration),
zero failures/errors/skips. An extra fixture initially shared period zero with its
setup's real shipments; corrected to an independent period, preserving application
quota behavior. Actual Paper owner supply/restart/world-pause/cadence checks passed.
A copied known schema-36 paid-cargo fixture upgraded with all original table/column
data retained; original fingerprint unchanged. Main dev 36->37 retained industry,
funds/settings/provenance; all integrity/FKs healthy. No world writes or tickets
in this probe. Normal stop 0, dev properties restored, probe archived. Final tested
JAR installed in the stopped player server; settings and save retained.

Phase 3 remains 1/4 complete groups. Current build supports player testing of
families, farms, food aid/trade and diagnostics. Existing funded nonfood firms have
timed work and local input purchasing; natural nonfood founding, upstream supply,
other industries, lifecycle and specialization remain. Pending ore-removal and
ownership choices precede their corresponding mine/lifecycle work. No main merge
or push. See TONIGHT_PLAYTEST.md and WORKSHOP_SUPPLIES.md.

## October 9, 2026 — nearby resource observation

Branch `codex/phase3-nearby-resource-surveys`. Bounded center-first loaded-chunk
surveys now extend around saved village homes: new radius one (nine targets),
editable 0–2, at most 25 targets/town and 1024 towns. Old enabled sections missing
the new key retain one home chunk; absent sections stay disabled. No chunk loading,
extraction or automatic founding. Immutable evidence retains original capture
time, not traversal completion time. Last completed explanations retain town,
chunk and timestamp alongside ongoing work. Native testing exposed results being
overwritten immediately by the next target; corrected and verified their retention.

Seven new geometry/config cases; focused 67 passed. Final functional Windows
Wrapper clean verify passed 688 (686 unit/two packaged integration), zero failures,
errors or skips. Clarified YAML comments repackaged afterward with values/code
unchanged. Actual Paper fixture passed nearby ore beyond an empty home chunk,
exclusion filtering, capture time after clock advance and retained results.
Four temporary blocks restored/checked; two console-only fixture tickets removed.
Separate synthetic DB: two residents, one exclusion, no businesses, clean/trusted
fence. Main industry/settings/provenance retained; integrity/FKs healthy. Schema
remains 36. Test used 25578, then original dev properties restored after normal
stop 0; probe archived. Updated only the stopped player-server JAR, not its save
or settings. Source guide now explains starting/joining the local test.

Phase 3 remains 1/4 complete groups. Ore removal and succession decisions are
pending before their corresponding automatic mine/lifecycle work. Wider survey
coverage is an observation component, not completion of connected production.

## October 9, 2026 — paid automatic workshop inputs

Branch `codex/phase3-workshop-supply-planning`. Existing staffed/funded workshops
now buy complete affordable recipe bundles from actual same-village warehouse
stock, pay once, reserve cargo space, wait for delivery and consume owned inputs.
No invented supplier, income, finished-goods customer or automatic founding.
New-file supply defaults: enabled, two periods of inputs, 16 warehouses reviewed;
prices reuse editable base Trade Value. Old files without industry-supply remain
disabled. Already paid automatic cargo still arrives when new purchasing is off.
Legacy cargo counts for ordering but retains its original delivery caller/clock.
Transport uses bounded shared windows rather than unrelated buyer period numbers.

Schema 36 binds automatic shipments to the actual buyer's saved clock, preserving
existing funds, stock, reserves, owners, history and legacy cargo. Payments,
arrivals, recipe work and checkpoint share rollback/retry boundaries. Fifteen new
cases: seven planner, seven temporary SQLite, one config. Focused 84 passed;
Windows Wrapper clean verify passed 681 (679 unit/two packaged integration), zero
failures/errors/skips. Guarded actual PopulationSession Paper probe passed four
paid cargo records, two tool units, conserved 9000 treasury/1000 business funds,
world pause and restart; unloaded synthetic sites, separate DB, no world writes.
Native main 35->36 retained industry/settings and clean/trusted provenance; only
the normal clean-stop timestamp refreshed. Integrity/FKs passed, normal stop 0,
probe archived. Updated the stopped separate player server's plugin JAR only;
its existing optional supply settings still require opt-in if absent.

Phase 3 remains 1/4 complete reporting groups (25%). Automatic nonfood founding,
upstream warehouse supply, renewable/primary industries, finished-goods demand,
lifecycle/succession and specialization remain. Ownership answer remains pending;
it blocks succession only. See WORKSHOP_SUPPLIES.md and PHASE3_TODO.md.

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

## Live read-only resource survey pipeline — October 9, 2026

- Branch `codex/phase3-live-resource-surveys`: registered main-thread bounded
  snapshot traversal at the canonical saved-home chunk. One scan/future at a time,
  max 1024 queued towns and 512 combined positive coordinates. Text settings in new
  economy.yml; missing old sections remain disabled with no owner-file rewrite.
  Unknown/partial/unloaded traversal is deferred, not treated as resource loss.
- SQL-owner bridge requires healthy capture, present/matching world and complete
  consistent evidence, filters current sticky exclusions and resolves saved claims.
  Conflicting deposits/minerals are rejected; separated patches may retain one
  depleted identity. Only provisional results/diagnostics, no persisted opportunities,
  funding, businesses, stocks, mining or new schema. Automatic mines remain disabled.
- Nine new reader/config/session cases. Windows Wrapper clean verify passed 640
  (638 unit/two packaged integration), zero failures/errors/skips. Guarded scheduled
  Paper pipeline passed with four ore/air blocks restored and synthetic saved homes
  in a separate DB: two residents, one exclusion, zero businesses, clean/trusted fence.
  Main startup 1676 retained; online 1677. Normal stop exited 0; test loading ticket
  removed and probe archived. Read-only old industry/funds/exclusions, integrity/FKs,
  clean/trusted fences and unchanged owner settings passed. No human playtest claim.
- LIVE_RESOURCE_SURVEYS/config/TODO/status/progress/devlog updated. Phase 3 stays
  1/4 (25%). Next: actual opportunity suitability, atomic funded founding and timed
  worker-backed nonfood chains; bulk-editor provenance remains separate. Ownership
  succession choice still pending. Local feature commit only; no main merge/push.

## Bounded observed resource patches — October 9, 2026

- Branch `codex/phase3-resource-grouping`: complete bounded observations split into
  face-connected positive evidence, deterministic ordering/anchors, copied metadata
  and immutable output. Distant/diagonal/unobserved gaps remain separate; no hidden
  coordinates are invented. At most 512 coordinates/six neighbor probes each;
  integer limits cannot wrap. Chunk borders do not impose geological borders.
- Six domain tests plus one temporary SQLite scenario: independent patches conserve
  initial reserves; depletion, reopen/retuning and duplicate-site rejection retain
  identities without refill. Focused 22 cases passed; full Windows Wrapper clean
  verify passed 631 (629 unit/two packaged integration), zero failures/errors/skips.
  No new world adapter, production
  behavior or migration; schema 34, no server DB/config changed by this batch.
- Grouping is a provisional evidence utility, not final geology or mine founding.
  Resolve saved identities first: two provisional patches may share an old deposit.
  Live assembly/safety, automatic funded nonfood chains and lifecycle still pending.
  RESOURCE_PATCHES/scanning/decisions/TODO/status/progress/devlog updated. Phase 3
  remains 1/4 (25%); local feature commit only, no main merge/push.

## Conservative piston resource provenance — October 9, 2026

- Branch `codex/phase3-piston-provenance`: successful extension/retraction saves
  moved ore sources and both possible adjacent axis destinations. Immutable bounded
  evidence only; no destination block reads, forced chunks, event cancellation,
  reserve refill or changed extraction terms. Cancelled/non-ore events ignored.
  Existing queue/failure fence reused; malformed multi-place capture also fenced.
- Five new adapter cases; Windows `.\mvnw.cmd clean verify -B -ntp` passed 624
  (622 unit/two packaged integration), zero failures/errors/skips. Schema stays 34.
  Native synthetic place/extend/retract dispatch saved seven fixture exclusions,
  restored every ore/air block, and retained paid-chain/restart accounting checks.
  Six new piston exclusions persisted. Read-only old industry/funds/exclusions,
  clean/trusted fence, integrity/FKs and unchanged economy settings passed.
- First launch omitted an interactive console; exact disposable JVM termination
  exercised a genuine dirty restart. Discovery paused, including after normal stop.
  Known empty test interval was reviewed against backup; untrusted state backed up
  and only marked-dev trust rearmed. No automatic recovery feature added. Corrected
  probe and probe-free restarts stopped normally; no loading tickets, month 1675
  retained at startup (online reached 1676). Probe archived, server stopped.
- Actual piston physics/bulk-editor compatibility remain unverified. Automatic
  resource opportunities/founding/production remain unfinished; Phase 3 stays 25%.
  Next: coherent observation grouping, then safe observation and funded operation.
  Local feature commit only; no merge/push, main or production changes.

## Live native ore placement capture and recovery fence — October 9, 2026

- Branch `codex/phase3-live-ore-tracking`: registered MONITOR listener captures
  successful native ore placements/multi-place positions, ignoring cancelled,
  forbidden and non-ore events. Includes named ore variants/ancient debris for
  future provenance, without enabling their production catalog. Immutable UUID/XYZ
  records only; no player inventory/identity or Paper object crosses the owner queue.
  Bounded pending saves, synchronous/async error logging and immediate gap state;
  no main-thread SQLite, player placement cancellation, world edits or forced chunks.
- Schema 34 adds explicit singleton clean-stop/trust fence. Live start commits
  dirty before capture; shutdown drains accepted saves before a clean fence. Crash,
  save/capture failure and early-start gap cannot silently regain trust on restart.
  Persisted gaps block coordinate discovery; immediate session readiness must gate
  future live observers. Existing populations, finite deposits and histories remain.
  `/civ debug status` explains healthy capture or reviewed-recovery pause. No recovery
  reset/adoption command implemented; migration invents no old placement history.
- Fourteen added cases (five fence repository, five adapter, three owner-session,
  one discovery). Windows `.\mvnw.cmd clean verify -B -ntp` passed **619** (617 unit/
  two packaged integration), zero failures/errors/skips. Final run includes the review
  fix retaining an early failure explanation and fencing world-observation failures.
  Tests cover overload, cancelled/multi-place data, clean/unclean restart, rollback,
  schema-33 recovery, shutdown drain and preservation of people/deposits.
- Consistent marked-dev schema-33 backup preceded additive upgrade. Positive native
  fixture dispatched a synthetic event through the actual registered listener and
  verified its queued saved exclusion after restoring all six blocks. Dummy player
  is not an online client, no inventory/player data changed. Separate probe DB also
  passed paid chain, saved rejection/replay/restart and five claims/20 initial/16
  remaining/two tools. Main placement row represents this actual fixture event.
- Test loading ticket removed and probe archived. Probe-free restart retained month
  1670 and healthy capture; status showed no failure, console confirmed no forced
  chunks, both normal stops exited zero. Read-only schema-34 clean/trusted fence,
  exactly one new fixture placement, old identities/industry/funds, probe accounting,
  integrity/FKs and unchanged owner economy.yml hash passed. Probe excluded from
  production JAR; marked dev server stopped. No real player acceptance claim.
- Updated LIVE_ORE_TRACKING/provenance/scanning/mineral/development guides, TODO,
  status/progress/devlog. Piston/bulk-editor provenance, coherent opportunities and
  funded automatic nonfood operation remain next; automatic mines still disabled.
  Phase 3 stays 1/4 (25%) until a complete gameplay group passes. Succession preference
  remains pending separately; no answer assumed. Local feature commits only, no
  merge/push, main or production save changes. Physical development remains Phase 4.

## Resource provenance persistence and industry direction — October 9, 2026

- Branch `codex/phase3-resource-provenance`: recorded user-approved common industry
  direction: suitable sites, paid bounded exploration, timed non-1:1 output,
  resistance/yield/value separation, finite stone/sand industries, useful demand-based
  reserves, retained closed sites/history and nearby real employees. MASTER_DESIGN,
  SIMULATION_RULES and decisions clarify planned behavior without changing ROADMAP.
  PHASE3_TODO.md separates verified components from missing live gameplay gates.
- Schema 33 adds permanent-world/XYZ player_ore_placements. Bounded transactional
  batches are sticky and idempotent; first timestamps survive replay/restart/rename.
  Immutable chunk reads fail on budget overflow instead of admitting missing
  exclusions. PopulationSession queues copied evidence on its SQL owner even before
  a town exists; accepted writes drain at normal shutdown. No player identity stored.
- Coordinate discovery checks exclusions inside its transaction, rejecting stale
  or bypassed surveys entirely without partial reserves, altered claims or erased
  history. Legacy site-only API is still trusted, not suitable for automatic scans.
- Eight new cases (five temporary repository, two discovery, one owner-session),
  plus stronger uncertain-neighbor coverage. Windows `.\mvnw.cmd clean verify -B
  -ntp` passed **605** (603 unit/two packaged integration), zero failures/errors/skips.
  Initial focused compilation used a nonexistent snapshot accessor in a new test;
  corrected to the existing settlementNames API. Final full run followed the public
  read-only PaperOreSource adapter change for the separate native fixture.
- Consistent marked-dev schema-32 backup preceded schema-33 startup. Initial native
  probe correctly refused when no chunks were loaded; test-only probe now waits for
  a console-loaded fixture chunk. Positive real snapshot scan/exclusion passed after
  six original blocks were restored and checked. Its separate SQL-owner DB passed
  sticky placement/rejection/restart, unchanged five claims/20 initial/16 remaining
  reserves, paid chain and two tools. No production listener/automatic mine enabled.
- Test loading ticket removed, probe archived outside plugins, server stopped with
  exit zero. Main diagnostics showed no failure and offline restart retained month
  1667; online progress reached 1668. Read-only old identities/industry/funds retained,
  no main placements invented, integrity/FKs valid and owner config hash unchanged.
  Read-only tool audit corrected arbitrary-first-store selection to total stock.
  The synthetic fixture is not natural discovery or a human-client gameplay check.
- Updated RESOURCE_PROVENANCE/scanning/mineral/development guides, TODO, status,
  progress and devlog. Phase 3 remains 1/4 (25%). Next: crash-safe live placement
  capture, coherent resource opportunities, automatic funded staffing/production.
  Ownership succession preference was asked separately; no answer assumed. Physical
  works remain Phase 4; no merge/push, main or production save changes.

## Approved exposed ore and bounded scanner — October 9, 2026

- Branch `codex/phase3-exposed-ore-scan`: user approved exposed existing ore as
  finite prototype evidence, excluding newly tracked player placements. Unknown
  old origin is accepted; ownership, block excavation and refill remain prohibited.
- Plain-Java OreScan visits bounded cells (at most seven source reads each), accepts
  negative coordinates/heights, copies exclusion positions and detects six-face
  known-air exposure. Buried ore is ignored. Unknown/uncertain or over-512 evidence
  is incomplete, preventing truncated discovery. Results are immutable and stable
  across batch sizes; publication before traversal completes fails explicitly.
- PaperOreSource reads snapshot block types with absolute Y; normal/deepslate coal
  and iron, all air variants, fluids and unsupported minerals are handled explicitly.
  Unavailable coordinates never read the snapshot. No world writes/forced loading.
  Initial focused run caught Material.isAir requiring live Paper registries; explicit
  air classification removed that dependency before final verification.
- Eleven new tests (nine domain, two snapshot adapter). Windows
  `.\mvnw.cmd clean verify -B -ntp` passed **597** (595 unit/two packaged integration),
  zero failures/errors/skips. Diff reviewed; no schema change (still 32), real DB,
  world, server or owner configuration modifications. No live Paper ore scan was
  run this batch; component tests do not certify natural origin/live discovery.
- Updated decision/mineral/scanning guides, progress/status/devlog. Runtime observer,
  durable player-placement tracking, coherent deposit grouping and automatic mine
  founding/operation remain unfinished. Automatic mining stays disabled until those
  protections are connected. Phase 3 remains 1/4 (25%), advancing a partial group.
  Next: permanent placement exclusions and bounded live opportunity observation.
  Local feature commit only; no merge/push or main modification. No user decision
  currently blocks that scoped work.

## Coordinate-backed finite mineral identity — October 9, 2026

- Branch `codex/phase3-mineral-identity`: bounded canonical coal/iron observations
  match deposits by permanent world/ore coordinates. Alternate sites cannot claim
  the same evidence through this path. Multiple deposits report conflict without
  merge/transfer/new reserves. Linked surveys add sticky identity evidence without
  changing initial/remaining reserves or yields. Incomplete, disjoint, predating or
  changed-mineral evidence fails explicitly; world renames preserve identity.
- Schema 32 adds discovery metadata and unique coordinates transactionally with
  deposit/yields. Failed extensions preserve old state. Legacy deposits keep their
  history without invented coordinates and require future explicit adoption. The
  old trusted API retains site-only protection; automatic observers must use the new
  path. Internal discoverRows extraction preserves stored term encoding/replay.
- Fifteen added cases (12 temporary-SQLite, three domain). Windows Wrapper
  `.\mvnw.cmd clean verify -B -ntp` passed **586** (584 unit/two integration),
  zero failures/errors/skips. Coverage includes depletion/restart/retuning, linked
  surveys, competing sites/multiple pools, world independence/rename, invalid/legacy
  evidence, timestamp/mineral identity, transaction rollback and schema-31 recovery.
- Consistent marked-dev schema-31 backup preceded schema-32 startup. Separate native
  Paper probe used synthetic ore coordinates: four positions fixed 20 bundles,
  extraction left 16, linked fifth position and new rate did not refill reserves.
  Paid workshop/replay/restart passed. No world/entity changes. Main startup retained
  month 1662 after downtime; status/no failure and normal stop exited zero. Read-only
  old IDs/industry/funds and both DB integrity/FKs/counts passed; owner config hash
  unchanged, no main-DB coordinates invented. Probe archived, server stopped, test
  code excluded from production JAR. This does not prove actual ore snapshots.
- Updated MINERAL_IDENTITY.md, decisions/mining/development notes, status/progress/
  devlog. No automatic ore scan, natural-origin certification or mine production
  enabled here. Ore origin was pending at this milestone, subsequently approved above.
  Phase 3 remains 1/4 (25%), with discovery identity advancing a partial group.
  No production mutation, merge/push or main change.

## Connected workshop recipe accounting — October 9, 2026

- Branch `codex/phase3-workshop-production`: trusted SQL-owner processing consumes
  delivered business inputs and adds complete-batch outputs, bounded by actual
  workers, site/request limits, scarce inputs and net storage growth. Pending cargo
  remains unavailable and reserves space. Ownership supplies no free labor; dead/
  moved employees cease contributing. No money or village opening goods are minted.
- Text-editable recipes catalog starts with smith_tools: two iron + one coal ->
  one abstract tools unit, two batches/worker, site cap eight. Strict validation
  rejects incomplete/unsafe settings before startup. Parsing does not activate a
  scheduler or establish a workshop. Business stores must explicitly support every
  ingredient/product; old owner files are unchanged. Tools are not player item trades.
- Schema 31 adds versioned recipe_runs/recipe_flows. Inputs, outputs, exact saved
  request/workforce/recipe flows, receipt and shared business-period production
  marker commit together. Zero-work results, retries, changed-term rejection and
  rollback protect labor/material accounting. Historical recipes survive retuning.
  Cached admin business diagnostics now show the latest saved industrial explanation,
  capped at 768 characters, and no longer incorrectly claim automatic farms are absent.
- Eighteen new cases: five domain (including 10000 deterministic seeded plans),
  ten temporary-SQLite repository scenarios and three config cases. Paid 240-cycle
  mine -> warehouse -> blacksmith -> explicitly established test maintenance demand
  conserves capital, finite deposit, stone and tool flows across midpoint restart.
  Initial focused failures were test fixtures: an obsolete empty-cache assertion
  and using ordinary update for a resident transfer; corrected to current diagnostic
  and the existing explicit migration API. Final Windows `.\mvnw.cmd clean verify
  -B -ntp` passed **571** (569 unit/two packaged integration), no failures/errors/skips.
- Consistent marked-dev schema-30 backup preceded native schema-31 startup. Separate
  test-only Paper probe used explicit synthetic firms/sites/deposit and a dedicated
  SQL owner: paid deliveries, four iron/two coal -> two tools, capital conservation,
  duplicate/restart safety, dead-worker exclusion and production diagnostics passed.
  No world blocks/entities changed. Main plugin retained month 1661 after downtime,
  then advanced normally online with no diagnostic failure; normal stop exited zero.
  Read-only identities/industry/funds and integrity/FKs passed; no main-DB recipe
  runs were fabricated, owner economy.yml hash unchanged. Probe archived outside
  plugins and excluded from shipped JAR; test server stopped.
- WORKSHOP_PRODUCTION.md documents backend API, admin recipe text and limitations.
  This advances the connected-industry group but does not complete automatic
  nonfood recognition/founding/operation, live NPC demand, competition, closure/
  succession or emergent specialization. Phase 3 remains 1/4 (25%). Next: reliable
  nonfood resource/workplace adapters and causal operation. No new design question,
  production mutation, push/merge or main change.

## Automatic field enterprises — October 9, 2026

- Branch `codex/phase3-automatic-farms`: complete actual Paper crop evidence can
  establish one persistent village-wide field enterprise, select an available adult
  founder, pay actual startup funds once and hire eligible residents. Stable site,
  business and employment episode identities preserve history and retry safety.
  No crop ownership, block harvesting, construction or invented money is introduced.
- Optional editable farm-businesses defaults: enabled for new files, four workers,
  2500 hundredths (25 Trade Value), at least adulthood/216 months, zero beginner
  farming experience. Existing owner files remain unchanged and disabled until
  explicitly opted in. Forward tuning retains old history and existing firm terms.
- Assigned workers replace legacy all-adult farm labor; output is constrained by
  actual observed field slots and settings. Other open jobs exclude candidates.
  Dead/moved workers retain ended history and can be replaced. Smaller fields lower
  production without silently dismissing workers. Absent worlds/server downtime pause.
- Schema 30 adds nullable site coordinates transactionally. Legacy counts have no
  fabricated site and require a fresh complete survey for founding. Founding/site/
  capital share a transaction; hiring and food periods have their own transactions.
  Failed food periods retain valid founding/jobs and retry without double charges.
- Ten temporary-SQLite farm scenarios and two configuration cases added, including
  240-period conservation, restart, partial/missing evidence, absent worlds, funding/
  skills, worker replacement, injected failures and migration rollback. Two older
  migration tests now inspect compatible records directly while deliberately holding
  an old schema. Final Windows `.\mvnw.cmd clean verify -B -ntp` passed **553** cases
  (551 unit/two packaged integration), zero failures/errors/skips.
- Marked disposable schema-29 DB backed up consistently before schema-30 deployment.
  Separate native Paper probe used real 32-crop snapshots and two test villagers:
  one farm/two workers, output eight, treasury 10000 -> 7500 once; absent-world
  periods/stocks stayed frozen. Main plugin diagnostics reported no failure and
  retained month 1660 after downtime. Normal shutdown exited zero. Read-only checks
  preserved prior IDs/history/funds, passed integrity/FKs in both databases, and
  verified the saved actual crop site. Owner economy.yml hash was unchanged.
  Probe was archived outside plugins; test server is stopped. Test-only chunk
  tickets/entities were released; only the marked disposable crop fixture remains.
- AUTOMATIC_FARMS.md documents settings, scope and limitations. First Phase 3
  reporting group is complete for the farm prototype; the full canonical industry
  gate is not passed. Nonfood industry, skills progression, business revenue,
  expansion, closure/succession, chains and specialization remain next scoped work.
  No user decision blocks the next milestone; no merge/push or main change.

## Live business staffing bridge and diagnostics — October 9, 2026

- Branch `codex/phase3-business-runtime`: existing businesses now receive owner-thread
  invalid-job reconciliation after population pulses. Dead/moved residents' episodes
  end transactionally with observed separation reasons, preserving employment history
  and releasing assignments. Absent worlds skip staffing writes; return resumes them.
  Owner death is reported, without inventing succession or closure.
- Stable UUID rotation visits eight businesses per pulse, retains at most 1024 cached
  details, and wraps so late firms are visited. `/civ debug businesses <town UUID>`
  shows cached founder/owner, local living workers/capacity/vacancies, open assignments,
  site/founding month, world availability and latest separation. Admin-only commands
  read immutable views and show at most 20 businesses; they do not access SQL/worlds.
  Cached worker/owner details may lag until the firm's next turn. Firm/result counts
  are bounded; individual SQL latency/scan cost has no measured performance guarantee.
- Seven added cases cover active vs paused reconciliation, owner death, immutable
  snapshots, bounded rotation/eviction, rollback/retry, session startup/world return,
  empty/nested operation and command permission/startup/ID safety. Final Windows
  `.\mvnw.cmd clean verify -B -ntp` passed 541 (539 unit/two packaged integration),
  zero failures/errors/skips. No schema migration; current schema remains 29.
- Backed up the verified disposable schema-29 DB before actual pinned Paper startup.
  Readiness retained month 1659 after downtime; new empty-business and malformed-ID
  command paths passed; normal status/no failure and clean stop exited zero. Final
  read-only schema/integrity/FK/previous identity and exact treasury checks passed.
  No businesses auto-created. Positive staffing/diagnostics use temporary SQLite
  fixtures; this native smoke is not automatic founding, industry or client gameplay.
- Added BUSINESS_RUNTIME.md; updated command declaration, foundation notes, progress
  and devlog. Existing food/labor/stock/funds behavior remains unchanged. Automatic
  site recognition, founding/hiring and production integration are still next work,
  including preventing double-counted food/labor. Phase 3 remains partial (0/4 full
  gameplay groups); nine-phase roadmap unchanged. No design answer needed, no push/merge.

## Approved server/world pause policy — October 9, 2026

- Branch `codex/online-world-pause`: live Paper earns no time during server downtime
  or for absent saved world UUIDs. Loaded worlds without players and unloaded chunks
  still simulate. Population eligibility excludes paused towns from age/death/birth/
  migration/arrival; live food/needs clocks and period execution pause too.
- Schema 29 adds an audited online adoption and saved compressed world masks.
  Accrual/masks commit atomically; pending replay retains eligibility over restart.
  First adoption records and retires unverifiable uncompleted legacy demographic
  backlog; completed history/ages/identities are not reset. Subsequent earned work
  survives. Already earned economy queues wait for their world, not new downtime.
- Twelve new temporary-DB/domain/Paper-adapter cases. Final Windows
  `.\mvnw.cmd clean verify -B -ntp` passed 534 tests (532 unit/two packaged
  integration), zero failures/errors/skips. Initial focused assertion incorrectly
  expected already earned economic queue not to finish after return; clarified the
  scenario, explicitly drained earned work, and added separate queue-pause coverage.
- Marked disposable schema-28 DB backed up consistently before Paper upgrade to 29.
  First native startup retained completed month 1658 despite the stopped interval;
  normal online activity advanced to 1659 and diagnostics reported no failure.
  Read-only integrity/FKs and previous settlement/resident/household IDs passed.
- Second native restart after more than a minute stopped stayed at month 1659,
  with zero queued months/no failure, then exited normally. Final read-only checks
  retained old population/native-event/finance/family history and exact treasury
  balances; integrity ok/no FK violations. Absence/load/unload transitions are
  automated domain/SQLite/Paper-adapter tests, not a native multi-world playtest.
- Shared calendar remains administrative chronology, not each paused world's age.
  Population partial-month boundaries are shared; per-world demographic fractional
  calendars remain unimplemented. Existing owner YAML is not rewritten; legacy
  offline caps remain only for explicit headless harnesses. See ONLINE_WORLD_PAUSE.md.
- Nine-phase roadmap unchanged. Phase 3 business/production groups remain partial
  with 0/4 gameplay groups complete. Next: reliable site/resource evidence and
  automatic founding/staffing/production using eligible economic periods. Human
  client/compatibility/balancing still pending. No design answer needed for this batch.

## Finite mining and local warehouse accounting — October 9, 2026

- Branch `codex/phase3-deposits-storage`: finite site/deposit identity retains
  discovery terms and a geological ore/rock bundle. Explicit actual mining workers,
  one plan per business/period, site/work/storage limits constrain extraction.
  Byproducts share the same budget; depleted deposits do not regrow, restart-refill
  or reinitialize under changed IDs/terms. Actual world discovery remains unwired.
- Business-owned stores start empty, with finite weighted capacity and retained
  catalogs. Explicit working capital transfers actual village funds once; no new
  grants/salaries/personal wallets. Warehouse purchases transfer real capital and
  stock, reserve buyer storage and retain buyer-owned in-transit goods. Same-town
  haul limits/delivery delay apply. Arrival is once-only; pending cargo cannot resell.
  Regional routes and physical buildings remain their original later phases.
- Explicit causal consumption primitive supports household/industry/maintenance
  input use with source/reason and protected retries. It is not automatic NPC shopping
  or invented demand to empty stock. Current live field food/player transactions
  remain unchanged; transfers to village reserves must be integrated conservatively.
- Commented optional `industry` YAML provides complete editable abundance, labor,
  site/storage/haul/delay and coal/rock bundle inputs. Default discovery: observed
  ore * 150 finite bundles; three workers at eight each yield up to 24 coal + 72
  stone, bounded by actual conditions. Admins may add rock keys/space costs. No
  renewable/infinite mineral setting. Old owner configs remain untouched; settings
  validate now but do not enable automatic business production. Stored openings
  preserve original terms; capacity expansion requires a future explicit transition.
- Transactional schema 28 appends mineral/store/receipt/production/shipment tables
  and indexes without resetting earlier history. Tests cover depletion, bundles,
  storage/reservations, paid resale profits/total money, consumption, retry/rollback,
  impossible/stale actions, store funding, migration failure and independent snapshot
  cargo recovery. A 240-period chain conserves every coal/rock unit through
  mine→warehouse→shop→explicit input use; 10000 seeded calculations enforce limits.
- Sixteen added cases; final Windows `.\mvnw.cmd clean verify -B -ntp` passed 522
  (520 unit/two packaged integration), zero failures/errors/skips. Initial focused
  compile found a test-only reused lambda variable, corrected before successful
  focused 27-case verification and final complete run. All automated DB writes
  target test-owned temporary files; no production database/world mutation.
- Backed up marked disposable schema-27 dev DB before pinned Paper startup at 28.
  Normal owner/observation diagnostics reported no failure, then normal shutdown
  exited zero. Read-only integrity/FKs, checked old identities/history and empty
  new industry tables passed. Existing server economy.yml hash stayed identical.
  Baseline OSHI/native-access/manifest warnings remain. No automatic industry or
  native mine/warehouse gameplay is claimed by this startup-only smoke test.
- Approved finite minerals/byproducts/storage/warehouse purchase direction recorded
  in master/simulation rules and decisions; nine-phase ROADMAP unchanged. Business
  and production reporting groups remain partial (0/4 completed gameplay groups).
  Next: reliable sites, bounded owner views, automatic founding/staffing and causal
  production/demand integration. Wider shop product eligibility remains future work.
  World memory uses plugin DB + permanent world IDs. At this mining snapshot the
  pause choice was pending; the subsequent approved pause batch supersedes it.
  Local commit only, no merge/push. See FINITE_MINING_AND_WAREHOUSING.md.

## Phase 3 business and staffing foundation — October 9, 2026

- Branch `codex/phase3-business-foundation`: immutable business/site/founding and
  employment records retain real founder/initial owner/resident/site UUIDs. Explicit
  trusted-owner APIs qualify actual saved age/skills, require permanent same-world
  sites, enforce finite capacity and one current primary job per person. Ownership
  alone is not employment; site registration grants no autonomous block permission.
- Startup investment is an explicit actual treasury debit in the registration
  transaction; absent/insufficient funds or failed ledger writes leave no partial
  business/site/requirements/spend. Exact identity retries never charge/hire twice
  or resurrect old jobs. Caller inputs choose requirements/cost; no automatic policy,
  additional opening grants, salaries, personal wallets or production implemented.
- Retained job separations permit rehiring. Current workers exclude dead/emigrated
  people immediately; explicit owner reconciliation ends stale episodes without
  deleting their history or inventing an exact historical departure month. Automatic
  runtime integration, closure and succession remain next/later scoped work.
- Additive transactional schema 27 adds business sites, businesses, qualification
  requirements and employment, plus indexes. No existing history rewrite, seed/reset,
  production change or owner-config edit. Older binaries reject 27; downgrade needs
  the consistent pre-upgrade snapshot rather than dropping business history.
- Twelve added cases test full restart/snapshot preservation, qualifications,
  capital/rollback, duplicate site/job rejection, capacity, separation/rehiring,
  death/migration and failed reconciliation, nested calls, schema-26 failure recovery
  and a seeded 240-month staffing turnover fixture. Final Windows
  `.\mvnw.cmd clean verify -B -ntp` passed 506 cases (504 unit/two packaged integration),
  zero failures/errors/skips. These are staffing scenarios, not simulated industry
  turnover/production or a scale-performance bound.
- Marked disposable pinned Paper was backed up at schema 26, upgraded to 27 and
  restarted successfully. Status/observation diagnostics reported no plugin failure;
  final stop saved worlds and exited zero. Read-only integrity/FKs, retained IDs and
  checked family/skill/economy/choice/learning/treasury histories passed; new business
  tables remain empty as intended. An initial launch omitted a persistent console
  and closed its stdin, requiring termination of the verified disposable process;
  subsequent console restart and clean shutdown passed. No production server/world
  was touched. Baseline OSHI Windows-counter warning remains unrelated.
- Phase 3's first gameplay milestone is partial; it remains 0/4 completed groups,
  with concrete backend progress described in PROGRESS.md. Next: owner/cache business
  diagnostics, reliable existing sites and admin-editable automatic opportunity and
  staffing rules, then resource-supported output without double-counting field food.
  No new consequential answer is needed for this batch. Source roadmap unchanged;
  local feature-branch commit only, no merge/push. See BUSINESS_FOUNDATION.md.

## Phase 2 prototype gate passed — October 9, 2026

- Branch `codex/phase2-response-gate`: villages now save a seeded weighted preference
  among feasible donation requests, emerald offers, real timber/stone barter or wait.
  Actual treasury, stock floors, idle/non-shortage evidence, supported stack value,
  pending/reserved food, selected priority, settings and catch-up constrain options.
  No imaginary supplies, automatic player debit or promise of a supplier.
- Strict commented `responses` settings expose weights/eligibility and enable/disable;
  new files enable the prototype, old owner files remain unchanged/disabled. Players
  see the preference in supplies menus and may choose other available options.
  Existing confirmations/cooldowns/permissions/saved inventory proof recheck reality.
- Schema 26 appends parent/candidate comparison history tied to food periods.
  Versioned seeds/raw weights/feasibility/reasons reproduce old choices independently
  of later tuning/funds. Period effects and publication commit together; failed writes
  leave receipts retryable. Existing identities/history/replay stay intact. Old plugin
  downgrade requires a consistent pre-upgrade backup; no database/world reset.
- Ten added cases cover seeded alternatives/blocked choices, strict admin settings,
  actual paid execution, pending food/cash exhaustion, corruption, migration rollback,
  disabled forward settings and a seeded 240-period response/receipt/restart scenario.
  Initial test-only import ambiguity and restart seed error were corrected; the seed
  protection remained intact. Final Windows `.\mvnw.cmd clean verify -B -ntp` passed
  494 cases (492 unit/two packaged integration), zero failures/errors/skips.
- Disposable pinned Paper verified saved donation/cash/timber candidates published
  before explicit actual food-for-emerald/log exchanges, plus prior donation, failed
  save, duplicate, bargaining, learning, welfare, restart and unloaded regressions.
  Consistent marked schema-25 dev snapshot preceded upgrade. Probe archived; normal
  plugin-only startup/diagnostics/shutdown passed. Read-only integrity/FK and checked
  old identity/economy/learning history comparisons passed. No production changes,
  owner-config rewrite, merge or push. Native saved candidate completeness/feasibility
  and one cash/barter completion were independently checked.
- Phase 2 advances to 4/4 (100%) of its prototype reporting milestones. See
  PHASE2_ACCEPTANCE.md and FOOD_RESPONSES.md for actual gates/limits. Human client,
  inventory-plugin/native-death/cross-save compatibility, profiling and survival
  balance remain release work. No industries/jobs, construction, regional suppliers,
  reputation or starvation deaths are invented. Next is Phase 3 businesses/industry;
  Phases 3–9 remain planned. Earlier entries below are chronological batch snapshots.

## Configurable saved food-priority learning — October 9, 2026

- Branch `codex/phase2-priority-learning`: prior verified deliveries can modestly
  reinforce SEEK_FOOD at a future scheduled review. Optional strict editable
  `priorities.learning` uses a 12-period window and 25% maximum food bonus in new
  files; old files stay disabled. Both zero disables. No penalties, invented
  suppliers, extra money, payments, reputation or construction permissions.
- Only earlier period imports exactly matching applied receipts count. Saved
  unmet-demand prevention determines a bounded bonus; reserve-only deliveries and
  absent player visits are not failures. Food must still be unmet; existing
  cadence/pressure transitions prevent per-tick reselection. Enabled weights use
  100-times resolution; disabled legacy seeded choices/explanations are preserved.
- Transactional schema 25 adds a learning replay marker, versioned evidence/policy
  rows and indexed applied-receipt lookup. Old reviews default to no learning and
  are not rewritten; new choices replay their saved evidence through later tuning
  and restart. Missing required evidence fails explicitly. Failed upgrade recovery
  retains old-schema diagnostics; current-schema missing tables remain faults.
- Fourteen added cases cover domain ratios/blocked seeded choices, strict admin
  tuning, migration/replay/rollback/expiry/mismatch and a seeded 240-period scenario.
  Windows `.\mvnw.cmd clean verify -B -ntp` passed 484 cases (482 unit/two packaged
  integration), zero failures/errors/skips. First full run caught two old-schema
  diagnostic regressions during intentionally failed upgrades; repaired and reran.
- Pinned disposable Paper verified a native earlier twelve-unit delivery prevented
  four unmet units, earned an 8% future food bonus and retained evidence across
  restarts. Existing payment/bargaining/barter/welfare/famine/unloaded regressions
  passed. Consistent marked dev schema-24 snapshot preceded upgrade. No world reset,
  production changes, owner-config rewrite, merge or push. See PRIORITY_LEARNING.md.
  Test-only probe archived; final plugin-only startup/diagnostics/shutdown passed.
  Read-only schema/integrity/FK checks confirmed all checked prior identity/economy
  history and legacy review fields retained, with old owner learning still disabled.
  Native fixture's versioned twelve/four evidence and required metadata also checked.
- Phase 2 stays 3/4 (75%): bounded food learning is implemented. The remaining
  scope is explicit broader feasible-response comparison/execution explanations,
  rather than industry, building or regional supplier simulation from later phases.

## Exact food import outcome evidence — October 9, 2026

- Branch `codex/phase2-import-outcomes`: new plain-Java FoodImportImpact derives
  actual imports' marginal shortage relief and extra closing reserves from saved
  period inputs. A twelve-unit delivery against four unmet units reports four
  prevented units and eight added reserves. Independent farm recovery receives no
  import credit; later reserve consumption does not credit the import again.
- The cached economy view and `/civ debug economy` expose the comparison without
  SQL on the Paper thread. It holds opening reserves, production and demand fixed;
  it does not prove an appeal caused delivery, assign individual player benefit,
  invent alternate histories or award learning/reputation bonuses.
- Nine domain tests include exact maximum quantities, invalid evidence and 2,400
  seeded periods. One new temporary-SQLite case reconstructs the original outcome
  across restart and later consumption; existing delivery tests now assert pending
  receipts receive no credit and actual relief gets exactly one period's credit.
- Final Windows `.\mvnw.cmd clean verify -B -ntp` passed 470 cases (468 unit/two
  packaged integration), zero failures/errors/skips. Disposable pinned Paper also
  verified the native twelve-unit saved donation's exact four/eight impact and
  printed the new admin explanation. See FOOD_IMPORT_OUTCOMES.md.
  All prior native purchase/bargaining/barter/welfare/unloaded regressions passed;
  the probe was archived, then normal startup/diagnostics/shutdown passed. Read-only
  integrity/FK checks and pre-existing identity/economy history comparisons passed.
- Schema stays 24; no migrations, config rewrite, world reset or production changes.
  Existing reports and priority replay version 1 remain intact. A consistent marked
  dev snapshot preceded the server check. Phase 2 stays 3/4 (75%): trustworthy
  aggregate outcome evidence is now implemented; broader executed responses and
  bounded configurable learning from attributable action outcomes remain.

## Playable finite-stock food barter — October 9, 2026

- Branch `codex/phase2-player-barter`: `/civ barter timber|stone` previews a whole
  held food stack for current available oak logs/cobblestone; `/civ barter accept`
  explicitly confirms. Optional village UUID resolves overlap. Prices, conversion,
  stock floor, item cap and food target are editable. No recurring industry supplies.
- New files enable the optional barter section; old owner files stay disabled until
  explicitly opted in. Unaccepted quotes are temporary/expire using the editable
  bargaining offer deadline. Restart discards them without goods lost. Accepted
  immutable receipts recover even if new barter is disabled. No new migration after
  schema 24; no world/DB reset or owner-config rewrite.
- Exact PREPARED save proof precedes reservation. Whole food becomes goods in the
  same slot; EXCHANGED saved proof precedes one future food receipt. Changed goods
  cancel/refund stock once. Ambiguous inventory failures retain supplies for review.
  Pending trade markers block other donations/purchases/negotiations; unverified
  payouts lock item movement. Treasury is unchanged by barter.
- Nine added player/command/receipt cases plus one mixed two-village 240-period
  scenario. The latter conserves every food import, emerald debit and timber/stone
  unit across all payment types, cancellations, duplicate completions and restart.
  First focused run exposed the new receipt missing from the saved-proof allowlist;
  added only its explicit supported key, retaining bounded full-file proof rules.
  Final Windows `.\mvnw.cmd clean verify -B -ntp` passed 460 cases (458 unit/two
  packaged integration), zero failures/errors/skips. No test uses a real server DB.
- Native pinned disposable Paper passed 8 wheat -> 6 saved oak logs, precisely one
  six-unit stock debit, unchanged money, failed-save withholding, double acceptance,
  future food once, restart and all prior economy/welfare/unloaded regressions.
  Marked dev snapshot taken first; no production changes, merge or push.
  Final normal startup/command routing/diagnostics/shutdown and read-only integrity/
  FK/history checks passed; test-only probe archived outside active plugins.
- Phase 2 advances to 3/4 groups (75%): the player trade/prices/long-run stability
  prototype gate is verified. Autonomous executed responses and attributable outcome
  learning remain; industry-specific merchants, barter quantity negotiation and
  human client/inventory-plugin/release balance stay outside the verified prototype.
  See PLAYER_BARTER.md. Next: feasible responses and explained outcome feedback.

## Protected barter accounting — October 9, 2026

- Branch `codex/phase2-barter-accounting`: internal food-for-timber/stone quotes,
  exact goods reservations, cancellation and future food completion now use actual
  available supplies. No live barter menu/command or physical payout in this batch.
  The optional editable section starts disabled; existing owner configs stay untouched.
- Prototype exports only idle, non-shortage timber/OAK_LOG and stone/COBBLESTONE,
  with editable whole-item conversion, keep-stock floor, food target and item cap.
  No profession-generated goods or industry output. Quotes move no goods or funds;
  cash-poor villages can barter actual supplies. Failed bargaining cooldowns remain.
- Schema 24 adds immutable barter receipts and ordered reserve/refund stock movements
  with before/after quantities and completed-period context. Earlier reports remain
  intact; current supplies may differ from the last closing report. Shared pending
  food need and cross-payment player/operation guards prevent spending twice.
- Windows Wrapper clean verify passed 450 cases (448 unit/two packaged integration),
  no failures/errors/skips. Fifteen added cases cover exact floors/value, competing
  offers, stock/need conservation, cancellation/completion, changed terms, rollback,
  migration failure, admin settings and a seeded 240-period finite-stock famine
  scenario with restart. Previous Paper donation, purchase, bargaining, welfare,
  famine/recovery and unloaded regression probe passed under schema 24.
- Consistent marked dev schema-23 snapshot preceded upgrade. No production/world
  reset, main change, merge or push. Normal final plugin startup/diagnostics/shutdown
  passed; integrity/FKs valid and resident/home/family/finance/welfare/bargaining
  history retained. Accelerated dev population continues normally, without a reset.
  Phase 2 remains 2/4 groups (50%); physical
  barter confirmation/recovery, broader responses/learning and balance remain.
  See BARTER_ACCOUNTING.md. Next: player-facing saved inventory exchange.

## Saved emerald bargaining — October 8, 2026

- Branch `codex/phase2-food-bargaining`: `/civ bargain <emeralds>` proposes a price
  for a whole plain food stack; `/civ bargain accept` explicitly confirms the latest
  agreement/counter. Current need, scarcity value and available funds set offers.
  No goods or funds move during discussion. Ordinary village trades remain intact.
- Optional commented bargaining section: three failed prices, 60-second cooldown,
  45-second offer deadline. Strict editable limits; old owner files stay untouched
  and disabled without the new section. Menu/reconnect/restart cannot bypass an
  active player/village cooldown; quick purchases also respect it. No reputation penalty.
- Schema 23 adds saved discussion terms/history/policy and marks a consumed bargain
  in its purchase reservation transaction. Expired, superseded, changed or unaffordable
  offers cannot spend. Reserved transfer recovery retains the existing independent
  Minecraft save proof and remains possible with new bargaining disabled.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 435 cases (433 unit/two packaged
  integration), zero failures/errors/skips. Sixteen added cases cover domain rules,
  temporary SQLite migration/rollback/restart, current funds, patience/cooldowns,
  command confirmation, changed held goods, duplicates, permission and admin tuning.
- Pinned disposable Paper passed a current-value counter surviving restart, explicit
  double acceptance protection, withheld payment on failed save and one native
  8-wheat/3-emerald exchange/debit/future delivery. Prior welfare, famine/recovery,
  donations and unloaded-operation checks passed. Schema-22 dev backup preceded
  upgrade. Normal plugin-only startup, diagnostics and shutdown passed; integrity/FKs
  valid and original resident/home/family/projection/finance/welfare history retained.
  No production data/world reset, main change, merge or push.
- Phase 2 remains 2/4 reporting groups (50%); this completes another piece inside
  player trade. Barter/outbound supplies, broader executed responses/learning and
  integrated balance/compatibility remain. Human client tests remain deferred.
  See FOOD_BARGAINING.md for behavior, configuration and limits.

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

## Budget-backed emerald food purchases — October 8, 2026

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

## Configurable opening funds — October 8, 2026

- Branch `codex/phase2-configurable-opening-funds`: admins control one-time opening
  capital in the commented server economy.yml. New files enable 24.00 Trade Value
  per living resident; existing files without finance retain disabled grants.
- Reliable economic enrollment and its opening receipt share a transaction.
  Explicit opt-in also funds existing runtimes without treasuries at startup,
  based on activation-time living people. Every initialized account, including zero
  or a manually administered account, is preserved through retuning/re-observation,
  population changes, spending and restart. No recurring income/personal payroll.
- Eight added automated cases; Windows Wrapper clean verify passed 377 cases
  (375 unit/two packaged integration), zero failures/errors/skips. Covers admin
  validation, fractional values, disabled/zero grants, opt-in, restart, retained
  accounts, overflow and injected save failure rollback/retry. Initial verification
  found two new fixture errors (nonexistent settings factory and missing world binding);
  corrected fixtures without weakening application behavior.
- Schema remains 20, existing ledger used; no reset or automatic owner file rewrite.
  Funding-only edits leave food-clock history/catch-up intact. Paid exchanges,
  protected barter reserves and negotiation are upcoming, with file-first admin tuning.
  Phase 2 stays 1/4 full groups; no design question currently blocks the next batch.
- Disposable pinned Paper passed configured 48.00 opening capital, unchanged balance
  after restarting with 99.00 per resident, and prior food/donation/famine/unloaded
  checks. Native save showed exactly one opening receipt and valid integrity/FKs.
  Normal single-plugin startup, read-only diagnostics and clean shutdown passed;
  original saved people/households/family/projection identities retained. Consistent
  schema-20 dev backup taken first; old server config left untouched with grants off.
  Production JAR excludes probes/JUnit. No human client payment/bargaining test yet;
  no production data, main, merge or push changed.

## Finance direction clarified — October 8, 2026

The user approved moderate initial finances as the starting basis, configurable
opening capital, emerald payments and optional barter using contextual real stocks.
The prior funding/currency questions are resolved. These are approved plans, not
implemented payments or starting budgets. Persistent workplace/business assignments
remain Phase 3; current housing and observed farm labor do not imply an employer.
Repeated entity loads/spawns must preserve finances rather than seed more capital.
No numeric grant, exchange rate or guaranteed stack-for-stack ratio is locked.

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

## Initial audit
Prototype contained six Java source files: `Civilizations`, `DatabaseManager`, `VillageDiscoveryListener`, `Settlement`, `SettlementNameGenerator`, `NeedsEngine`. Discovery via player chunk movement/HOME POIs, simple settlement persistence, population/bed census and binary housing shortage. No actual economy, business, roads, physical construction, history or simulation yet.

Audit identified: malformed INSERT; player-centered settlement/census; global permanent region suppression; ambiguous nearby lookup; new settlements not immediately censused; silent migration errors; synchronous DB/world work in movement listener; no tests or Maven Wrapper initially.

## Verified persistence/build foundation
- Foundation work prepared on `foundation/persistence-build`; local commits are now authorized. No merge or push to main is authorized.
- Added Maven Wrapper pinned 3.9.11, JUnit 5 and test class `DatabaseManagerTest`.
- Fixed INSERT to persist all nine model fields, including beds; added isolated test constructor.
- Updated `pom.xml`, `.gitignore`, README.
- `.\mvnw.cmd clean verify -B -ntp` passed on Windows/Java 25: six tests, zero failures/errors/skips. Earlier `clean package` also passed; packaged JAR shaded SQLite and excluded test/JUnit/Paper API.
- Outstanding reported warnings: Maven/SQLite Java 25 and Shade manifest overlap. Discovery and migration behavior not changed.
- Both wrappers are present at the repository root, readable and not hidden: `mvnw` (11,790 bytes), `mvnw.cmd` (8,481 bytes); Maven 3.9.11 is pinned in `.mvn/wrapper/maven-wrapper.properties`.
- Checkout is under `C:\Users\noahw\OneDrive\Desktop\IdeaProjects\Civilizations`; wrapper files have OneDrive reparse attributes. Older compiler records referenced a different checkout path. Explorer visibility itself has not been inspected.
- Prefer a local checkout and server outside cloud sync. `.gitignore` does not exclude files from OneDrive. Cloud sync is not an atomic backup of Git metadata or an open SQLite DB. No relocation or server-data reset has been performed.
- No Paper server smoke test has been run for this foundation; headless persistence tests are verified.

## Immediate next work
1. Phase 3 business storage/staffing is implemented. Next: bounded owner/cache views,
   reliable existing-site observation and editable automatic founding/hiring, then
   resource-backed production. Preserve the completed Phase 2 economy accounting;
   do not infer taxation, wages, unlimited goods or income from a profession label.
2. Human client playtest can follow later; user authorized Phase 2 without it.
   Preserve vanilla profession/trades/inventory when physical villagers are recreated,
   and handle other native transformations before recommending established survival worlds.
3. Current roadmap phase: businesses and industry; no
   construction or canonical roadmap changes. Latest feature work has not been pushed.

## Confirmed player food donations — October 8, 2026

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

## Player food-aid previews — October 8, 2026

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

## Explicit food delivery accounting — October 8, 2026

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

## Evidence-based village priorities — October 8, 2026
- Branch `codex/phase2-village-priorities`: villages choose MONITOR, SEEK_FOOD or
  INVESTIGATE_HOUSING from saved unmet food and observed household crowding. Unknown
  housing cannot justify crowding; infeasible priorities are excluded under every seed.
- Editable prototype priority weights/cadence in optional `economy.yml` section.
  Minimum relative weight excludes irrelevant alternatives before a seeded weighted
  draw. Five-period default reviews reconsider early on appearance/severity/recovery,
  crowding appearance/recovery or priority-setting changes. No invented leader/jobs.
- Schema 17 saves review inputs, policy, seed, replay version and chosen priority.
  Historical candidate explanations reconstruct from original inputs. Food/needs/
  reviews commit together; retries cannot duplicate effects. Policy tuning is forward
  and does not discard food-clock time. Old periods get no fabricated decisions.
- Twelve new cases; Windows Wrapper clean verify passed 300 cases (298 unit/two
  packaged integration), no failures/errors/skips. Blocked choices over 1,000 seeds,
  reasonable alternatives, crisis filtering, 1,200-period replay, rollback/retry,
  schema-16 upgrade failure/recovery and existing treasury retention verified.
- Guarded Paper probe passed actual food crisis selecting SEEK_FOOD, original choice
  surviving restart/feast retuning and recovery returning to MONITOR, plus earlier
  food/needs/unloaded scenarios. Marked dev schema-16 snapshot taken first; original
  identities/families preserved and integrity/FKs checked. No production data reset.
- Priority decisions are nonbinding planning intentions. No suppliers, purchases,
  stock transfers, jobs or construction are fabricated. Execution, outcome feedback,
  broader wellbeing and player trade remain. Phase 2 stays 1/4 full reporting groups;
  PROGRESS now lists verified partial pieces rather than hiding them behind 25%.
  See VILLAGE_PRIORITIES.md. No push, merge or main modification.

## Household needs and treasury accounting — October 8, 2026
- Branch `codex/phase2-household-needs`: food periods now retain household adults,
  children, configured demand, observed housing and unassigned living residents.
  Unknown capacity is distinct from crowded housing. Shared food pressure is not
  presented as proof of individual rations or complete prosperity.
- Needs and food effects/checkpoint share one transaction. Historical housing and
  crisis evidence remain intact through restart, retuning and later observations.
  Schema 16 adds needs snapshots and an independent exact Trade Value treasury
  ledger; old periods are not backfilled with invented household observations.
- Explicit treasury opening/credit/debit APIs require a source/reason and operation
  UUID. Idempotent retries, rollback, insufficient funds and overflow are tested.
  No automatic live account, money creation, income, tax, salary or personal wallet.
- Fourteen added automated cases; Wrapper clean verify passed 288 cases (286 unit,
  two packaged integration), no failures/errors/skips. Includes two repeatable
  1,200-period needs scenarios and schema-15 upgrade failure/retry.
- Guarded real Paper food probe passed needs output, actual household demand/housing,
  preserved crisis assessment after restart/retuning, no invented treasury and the
  earlier food/unloaded behaviors. Consistent schema-15 dev snapshot first. Original
  people/families retained, integrity/FKs verified; no production save/world reset.
- `/civ debug economy` uses cached period evidence, limits household output to twenty
  entries and distinguishes spending budget from prosperity. Large-city profiling,
  rationing, full wellbeing, actual budget flows and player trade remain outstanding.
- Phase 2 stays 1/4 (25%) by reporting groups. The second group is partial: observable
  needs and budget machinery do not complete the broader labor/prosperity gameplay.
  Next gate: feasible decision pressures/responses with tested causal prerequisites.
  See HOUSEHOLD_NEEDS_AND_TREASURY.md. No merge/push/main changes.

## Live food economy — October 8, 2026
- Branch `codex/phase2-live-food`: loaded main-thread crop snapshots around saved
  homes feed the existing SQL owner. Saved living adults supply bounded prototype
  labor; children consume food. Default capacity is one worker per 16 planted crops.
  No harvesting, ownership claim, player-block edit or invented offscreen facility.
- Schema 15 adds evidence, saved independent economic clocks and recorded forward
  tuning. Per-period policies preserve old explanations across threshold edits.
  Stocks/inputs/checkpoint commit together; partial/conflicting scans pause work.
  Unloaded villages use last reliable capacity without forcing chunks.
- New read-only `/civ debug economy <UUID>` shows reserves, evidence, shortage
  explanations and indicative prices. These are not merchant offers.
- Windows Wrapper clean verify: 274 cases (272 unit/two integration), no failures,
  errors or skips. Tests cover actual saved demographics, temporary databases,
  clock/restart limits, pauses, failed-save rollback/retry, history-preserving tuning,
  migration backfill, negative-height crop scanning and old owner configuration.
- Guarded real Paper probe passed planted-field supply, depleted food/severe shortage,
  forward feast retuning, identity/history restart preservation and unloaded accounting.
  Consistent disposable schema-14 backup taken first. No production reset or world
  deletion. Test helper is excluded from the production JAR.
- Phase 2 stays 1/4 (25%): the second milestone is partial. Treasury, family allocation
  and wellbeing, business hiring, autonomous responses and player trade remain.
  No starvation population penalty. Catch-up uses current saved demographics and last
  reliable field evidence rather than full historical event interleaving. Scale and
  human survival playtesting remain pending. See LIVE_FOOD_ECONOMY.md.

## Editable economy prototype settings — October 8, 2026
- User authorized configurable economy defaults and requested easy famine/feast tuning.
  `economy.yml` is created only if missing and validated before database startup.
  Comments, owner edits and matching custom commodity stock/price entries are preserved.
- Plain-Java EconomySettings computes explicit first-enrollment supplies, adult/child
  demand, production bounded by workers plus evidenced capacity, and exact abstract
  scarcity prices. No fabricated facility, block change or merchant transaction.
- Defaults/recipes/ranges documented in ECONOMY_TUNING.md. Nineteen new cases test
  the shipped file, malformed/partial/unsafe edits, exact prices, unknown capacity,
  overflow, immutable settings and distinct normal/famine/feast accounting scenarios.
- Wrapper clean verify passed 261 cases (259 unit/two integration), no failures/errors/
  skips. Paper first-run creation, 25% edit/comment surviving restart byte-for-byte,
  normal population diagnostics/unloaded fixture and clean shutdown verified. Restored
  test settings to normal; schema remains 14 and no economy accounts were auto-created.
- At that batch settings/calculations were prepared, not live consumption or trading.
  Recorded tuning transitions and live food were subsequently implemented above. No approval blocker remains for prototype tuning.
  Phase 2 stays 1/4 (25%) with partial labor/price work; no merge/push/main changes.

## Phase 2 commodity accounting — October 8, 2026
- Branch `codex/phase2-commodity-accounting`: plain-Java commodity keys, exact stock
  conservation, explicit incoming/demand categories, reserve use, per-commodity
  shortage duration, protected caller policy and quantitative report explanations.
- Schema 14 adds accounts/stocks/retained period inputs; no automatic inventory
  seeding or live economy scheduling. No inferred production or starvation effects.
  Initialization, advance, history and checkpoint operations are transactional.
- Eighteen new tests; Windows Wrapper clean verify passed 242 cases (240 unit,
  two packaged integration), no failures/errors/skips. Pure and saved 1,200-period
  crisis/recovery fixtures, mid-crisis restart, conservation, write rollback/retry,
  stale-period and tuning rejection, upgrades/malformed schema verified.
- Backed up marked disposable schema-13 DB; real Paper schema-14 startup/commands,
  unloaded fixture, no forced chunks and clean shutdown verified. Integrity/FKs OK;
  original identities/families preserved; zero economy accounts as intended.
- At this accounting milestone Phase 2 was 1/4 (25%), not a running/balanced economy.
  Tuning approval was pending then; subsequently approved and implemented above.
  Human client playtest deferred explicitly; known Phase 1 recreation limits remain.
  No production reset, construction, main modification, remote push or merge.
  See COMMODITY_ACCOUNTING.md for contract and remaining gates.

## Live population and approved zombie rule — October 8, 2026
- User chose to disable zombie conversion for managed villagers. Registered INFECTION
  guard leaves unmanaged villagers, curing, lightning and unrelated transforms alone.
  Verified lethal zombie attacks still kill people and save death/history; no general immunity.
- Paper projector applies saved names/age locks; nearby births/arrivals materialize only
  at loaded safe homes. Retired/moved/deceased incarnations reconcile incrementally.
  One asynchronous claim/add/activation handshake at a time; persistent freeze marker
  recovers shutdown between activation commit and main-thread acknowledgement.
- Vanilla breeding for managed parents cannot bypass saved families/calendar. Actual
  deaths retry on SQL owner, including deaths during asynchronous enrollment. Accepted
  shutdown saves drain before DB close. Entity removal for simulated death does not
  emit a second saved native death.
- Editable projection limits: radius 64, day budget 32/world, night budget eight,
  two inspected chunks/sixteen residents per tick. Night budget defers additional
  spawns; existing villagers use vanilla behavior and are not culled at sunset.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 224 tests (222 unit/two integration),
  no failures/errors/skips. Real Paper probe passed hazardous placement deferral,
  babies/adulthood/names, restart/freeze/claim recovery, native breeding/infection/death,
  five-person migration, simulated deaths and enrollment/death race. Probe never ships
  in production JAR; guarded by disposable marker/no online players.
- Normal plugin-only startup/commands/unloaded simulation checked separately; test
  fixture stayed unloaded and no forced chunks remained. SQL integrity/FKs passed for
  all scenario DBs. Owner batch observed 9.215 ms on three people: not a tick or scale
  bound. Large-server profiling and human client/balance testing remain.
- Schema remains 13; no reset, new migration or production/main/remote changes.
  Vanilla trade/profession/inventory preservation on recreation is not implemented;
  use disposable worlds for playtesting. Source/history/roadmap remain preserved.

## Resident projection preparation — October 8, 2026
- Branch `codex/phase1-resident-projection`: schema 13 incarnation registry,
  claim-before-spawn APIs, retained aliases, actual-death ledger and immutable owner
  views. Pure planner covers unloaded/pending/moved/deceased identities. No entity
  mutation adapter or death listeners are wired yet.
- Fixed stale settlement counts after demographic outcomes, including a town emptied
  by migration; unbound legacy census rows remain preserved.
- Read-only `/civ debug explain <uuid>` shows cached saved reasons, migration origins
  and identity conflicts; complete history remains in SQLite.
- Wrapper clean verify: 215 tests (213 unit, two integration), no failures/errors/skips;
  eighteen added cases. Real disposable Paper schema-12-to-13 upgrade, command,
  unloaded simulation and clean restart verified with backup first. Original IDs,
  families/homes preserved; month-45 adult arrival explained without loading its area.
  SQLite integrity/FK checks passed; server stopped cleanly.
- At this preparation milestone the zombie decision was pending; subsequently approved
  and implemented above. The following milestone history describes earlier snapshots.

## Transactional schema migration milestone
- Work continues on `foundation/schema-migrations`, branched from the committed persistence/build foundation and canonical documents. Main remains unchanged; nothing pushed or merged.
- `SchemaMigrator` uses SQLite `PRAGMA user_version`: 1 = base settlement identity, 2 = population, 3 = beds. All pending DDL and version markers share one transaction.
- Unversioned prototype databases are adopted by inspecting columns, preserving existing rows and adding only missing census columns with zero defaults. No reset is required.
- Newer schema versions and missing required columns fail explicitly. Failed initialization closes the connection. No arbitrary migration exceptions are swallowed.
- Ten migration test cases plus the six persistence tests pass with Windows `.\mvnw.cmd clean verify -B -ntp`: fresh install/reopen, three legacy shapes, incremental upgrades, rollback/recovery, newer-version rejection, malformed schema and connection cleanup.
- Tests use only temporary SQLite files. Actual server DBs were not opened or upgraded. No gameplay behavior, world identity, discovery or housing rule was redesigned.
- Migration limits: no migration smoke test on Paper yet; column presence is checked, not exhaustive validation of legacy constraints/data types. Schema steps are intentionally small and internal, without a migration framework. Standalone snapshot tooling was added in the subsequent milestone below.

## CI, backups and development procedures milestone
- CI/backup milestone branch: `foundation/ci-backups`, based on the schema-migration branch. No merge or push performed; main is untouched.
- GitHub Actions workflow verifies Windows and Linux with JDK 25 and the Maven Wrapper. Official actions are pinned to commit IDs; permissions are read-only, checkout credentials are not retained, and no publishing/deployment runs.
- `.gitattributes` explicitly keeps the Unix wrapper LF and the Windows wrapper CRLF. No broad source renormalization performed.
- Workflow validated with actionlint 1.7.7 (shellcheck/pyflakes disabled). Hosted Windows/Linux CI has not run because the branch remains local.
- Standalone `DatabaseBackup` uses a separate read-only SQLite connection, `VACUUM INTO`, integrity checking and a private staging file. It refuses existing destinations and never initializes/migrates/restores the source. No in-game backup command or scheduler yet.
- Seven new backup tests cover committed WAL data, exclusion of uncommitted writes, independent restored content, schema version retention, source/destination protection, missing paths and corrupt-source cleanup. A Failsafe integration test launches the CLI from only the packaged JAR.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 23 unit cases plus one packaged-JAR integration case. A second `clean verify` with a new empty temporary Maven repository also passed all 24 cases; dependency resolution does not rely on the old IntelliJ cache.
- `docs/DEVELOPMENT.md` records exact-build/JDK/checksum requirements for a disposable localhost Paper environment, source bundles, stopped-server world/config backups, consistent SQLite snapshots and restore drills.
- No actual Paper server created or launched, EULA accepted, live DB backed up, worlds copied, production data changed or checkout moved. Recovery procedures are documented, not claimed as a completed real-world drill.
- Schema remains version 3. Prototype discovery/population/housing rules remain unchanged; persistent residents/households, simulation clock and materialization are still pending. Maven/SQLite Java 25 and Shade manifest warnings remain non-failing.

## Deterministic headless runner milestone
- Headless runner milestone branch: `foundation/simulation-harness`, based on `foundation/ci-backups`.
- Added plain-Java `SimulationCheckpoint`, `SimulationRunner` and `SimulationHarness`, with no Paper/SQL/world/time dependencies in the simulation package.
- Explicit seed and completed-step checkpoints, deterministic per-step randomness, caller-supplied targets/budgets, and explicit remaining catch-up work. Pure immutable steps can resume or retry without hidden PRNG state.
- Packaged CLI runs a labeled synthetic checksum probe only; it does not simulate residents or convert logical steps to years. No in-game command, scheduler or persistent simulation checkpoint yet.
- Fourteen runner unit cases and one new packaged-harness case pass. Full Windows `.\mvnw.cmd clean verify -B -ntp`: 37 unit cases plus two integration cases, zero failures/errors/skips.
- Tests include batch-size independence, checkpoint reconstruction, failure retry, step ordering, invalid inputs, the long boundary, a fixed replay fixture and repeatable one-million-step probes. This is not real multi-century population simulation or a Paper performance guarantee.
- `docs/SIMULATION_HARNESS.md` documents immutable/pure steps, stable ordering, random replay conventions, bounded-step versus CPU-time limits and exact CLI usage.
- Schema remains version 3; no live database/world/server changes. Household capacity, time scale, demographic rates, authoritative seeding and recognition remain open. Paper smoke testing and hosted CI still pending.

## Persistent people and households milestone
- Resident milestone branch: `population/resident-storage`, based on the progress-tracker branch. Main untouched; no merge or push.
- Plain-Java immutable Resident/Household records save UUID identity, names, settlement/household membership, logical age and living/deceased status. No calendar ratios or demographic probabilities chosen.
- Transactional schema version 4 adds population tables/indexes while preserving legacy settlements/census. SQLite foreign keys enforce settlement/household references, including same-settlement household membership; protected referenced rows do not cascade away.
- PopulationRepository supports stable immutable reads, insertion, resident updates, living counts and atomic household/member insertion. Duplicates, mismatched references, unknown updates, implicit transfers and nested batch transactions fail explicitly.
- Deceased people remain in stored history-ready records but are excluded from living counts. Families, jobs, skills/prosperity, real homes and simulated demographic events remain pending.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 56 unit cases plus two packaged-JAR integration cases (58 total), zero failures/errors/skips. New tests cover persistence/restart, constraints, atomic rollback/retry, protected identities, schema-v3 upgrades/failure recovery, and population snapshots.
- All tests use temporary SQLite DBs; no production/server data, villagers or worlds changed. No Paper smoke test of schema v4 yet. Existing listener census remains separate and unchanged; no authoritative residents automatically seeded.
- Phase 1 tracker is four of ten reporting milestones complete (40% by milestone count, not time). See docs/POPULATION.md for boundary contracts and pending gameplay gates.

## Family memory milestone
- Family milestone branch: `codex/population-family-links`, based on resident storage. Schema v5 adds parent/child and canonical partner links; siblings derive from shared parents.
- Constraints reject missing people, self-links, reversed duplicates, ancestry cycles and in-place overwrites. Links survive cross-settlement residence, death, restarts and snapshots.
- Six new temporary-database tests pass, including v4 migration failure/retry. Full Windows `.\mvnw.cmd clean verify -B -ntp`: 62 unit and two integration cases (64 total), no failures/errors/skips.
- No family generation, parent/partner counts, marriage lifecycle or age/rate policies introduced. No Paper test, live DB change, merge or push. Phase 1 remains 4/10 (40%); jobs still needed for the household/family/job milestone.

## Saved logical aging milestone
- Current branch: `codex/population-saved-aging`. Schema v6 adds explicit age-only seed/progress/replay-version clocks; existing settlements, residents and family records preserved.
- Pure immutable aging and transactional persistence resume within caller-provided step budgets. Completed targets do not double-age; failure rolls back progress and all person updates. Deceased people retain ages and family links.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 74 unit plus two integration cases (76 total), no failures/errors/skips. Tests include restart, snapshots, write/overflow rollback, v5 upgrade recovery and a repeated 500-person/1,000-step pure probe.
- No real-server scheduler or births/deaths/migration yet. Logical age-only replay is version 1 and not silently repurposed as a calendar/demographic engine. Phase 1 remains 40%; partial work advanced two milestone groups.
- User clarified automatic village participation and authorized configurable prototype population settings. See docs/POPULATION_SIMULATION.md and the decisions log. No actual world/server DB changes, merge or push.

## Headless birth/death prototype
- Demographic domain milestone branch: `codex/population-demographics`. Pure immutable monthly population step emits birth/death explanations, retains deceased people and uses observed household capacity.
- User-approved editable prototype policy documented in POPULATION_SIMULATION.md; no closed numeric canon. No automatic partnership formation, crisis modifiers, demographic persistence or scheduler yet.
- Nine new tests, including two 100-year (1,200-month) accounting/replay scenarios. Windows `.\mvnw.cmd clean verify -B -ntp`: 83 unit plus two integration (85 total), no failures/errors/skips.
- Phase 1 remains 40% pending demographic persistence and remaining milestone gates. No server/world/actual database changes, merge or push.

## Atomic demographic saving milestone
- Demographic storage milestone branch: `codex/population-demographic-storage`. Schema v7 adds a global monthly clock, capacity observations and sequenced birth/death events. Additive upgrades preserve existing people/family/settlements.
- People, newborn identities, parent links, event reasons and progress commit together. Retry/restart/snapshot replay preserves outcomes; settings/replay changes and mixed aging/demographic clocks fail explicitly.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 92 unit and two integration (94 total), no failures/errors/skips. Tests include a snapshot-restored 100-year run across batch sizes, failure after newborn/link writes and safe event pagination.
- Phase 1: 5/10 reporting milestones (50% by count). Headless saved aging/birth/death is complete; runtime calendar/scheduler, import/projections, migration, job foundation and Paper validation remain. Global snapshot/writes require profiling before tick-loop integration.
- No actual server database/world changes, merge or push. Demographic engine remains explicit opt-in through Java APIs; original listener unchanged.

## Saved migration milestone
- Migration milestone branch: `codex/population-migration`. Schema v8 preserves/rebuilds event history with structured migration origins and external arrivals. Current demographic replay convention is 3; older clocks/settings fail explicitly rather than changing silently.
- Pure seeded migration resolves observed housing pressure, moves whole living households to vacant same-world housing and retains every identity/family link. Occasional external adults require observed space. No world or entity work.
- Seven new domain and six new storage cases. Windows `.\mvnw.cmd clean verify -B -ntp`: 105 unit plus two integration (107 total), no failures/errors/skips. Restored 100-year combined replay, every event page, rollback/retry, v7 upgrade, cross-world safety and move-then-death in a single batch verified.
- Phase 1: 6/10 (60% by reporting milestone count). Job foundation, runtime calendar/catch-up, import/projection/diagnostics and real Paper/CI/recovery validation remain.
- No actual server/world/production DB changes, merge or push. Prototype uses legacy stored world names for boundaries; stable world identity remains a discovery integration decision. No automatic partnership formation or proven long-term balance yet.

## Work qualification foundation milestone
- Current branch: `codex/population-work-foundation`. Schema v9 adds resident skill counters without changing existing population/history. Saved skill totals are idempotent and retain identity across movement/death/restarts/snapshots.
- Immutable WorkProfile/JobRequirements and explainable eligibility reject deceased, underage and unqualified candidates. No fabricated employer/occupation/hiring, salary or retirement rule; real business/labor integration remains in later phases.
- Five new domain and five new storage tests, plus migration skill-retention coverage. Windows `.\mvnw.cmd clean verify -B -ntp`: 115 unit and two integration (117 total), no failures/errors/skips.
- Phase 1 7/10 (70%): household/family/job qualification foundation complete. Calendar scheduling, automatic village import/projection/diagnostics and real Paper/CI/recovery remain.
- No server/world/actual DB changes, merge or push. See WORK_FOUNDATION.md for exact scope.

## Disposable Paper and recovery verification
- Branch `codex/paper-test-preparation`: guarded launcher validates test marker,
  pinned JAR checksum, accepted EULA, loopback properties and Java 25.
- Following explicit user EULA acceptance, exact Paper 26.2 build 129 started,
  restarted and started from a restored test-world/config/SQLite backup; all
  stopped cleanly. Plugin bd8f4ce enabled and initialized schema 9 successfully.
- Verified complete source Git bundle and archive, all 68 restored world/config
  files, SQLite integrity, and recovered-source wrapper build: 117 tests passed.
- Only new disposable temporary directories outside OneDrive were used. No
  production save, existing world, main branch, remote push or merge changed.
- See PAPER_SMOKE_TEST.md for download recovery and non-fatal Windows warnings.
  Hosted CI and integrated population playtesting remain pending. Phase 1 stays 70%.

## Saved calendar foundation
- Branch `codex/civilization-calendar`; schema 10 adds a saved elapsed-time anchor,
  fractional month and queued demographic target. Existing identities/history retained.
- Pure calendar conversion handles backward wall clocks, explicit elapsed caps and
  overflow. Queued targets survive demographic failure and bounded restart catch-up.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 129 unit and two integration tests
  (131 total), zero failures/errors/skips. Fourteen new clock cases cover restart,
  snapshots, fractional time, rollback, caps, overflow and schema-9 upgrade retry.
- Calendar APIs remain explicit opt-in; runtime scheduler/configuration and village
  import/projection are pending. Phase 1 remains 70% until the whole calendar gate passes.
- User approved automatic recognition of both natural villages and player-built
  areas containing beds and villagers. No server/world/production DB changed in
  this calendar batch; no merge or push.

## Automatic village observation foundation
- Branch `codex/village-observation`: immutable UUID-tagged observations and bounded,
  deterministic populated-bed cluster candidates. No Paper/chunk/SQL calls.
- Editable detection inputs, duplicate/budget rejection, nearest-bed ties and partial
  observation safety verified by seven new tests. Windows wrapper clean verify passed
  136 unit and two integration tests (138 total), zero failures/errors/skips.
- Runtime importer/identity matching, household observation and projections remain
  pending. Existing listener unchanged; schema 10. Phase 1 stays 70%.
- User approved configurable prototype adult partnerships; implementation upcoming.
  No new world/server/production DB changes, merge or push in this batch.

## Approved partnership prototype
- Branch `codex/population-partner-formation`: explicit pure pairing of unrelated,
  unpartnered living adults sharing a home. Enable/minimum-age inputs configurable;
  repeatable UUID order, retained deceased partners, no invented existing child ancestry.
- Seven new tests, including birth integration. Windows wrapper clean verify passed
  143 unit and two integration cases (145 total), zero failures/errors/skips.
- Import/monthly runtime integration remains pending; existing demographic replay 3
  is unchanged. No schema change or actual DB/world/server change in this batch.
- User approved permanent world UUID and saved bed/villager identity matching;
  overlapping existing towns will be diagnosed instead of automatically merged.
- Phase 1 remains 70%; no merge or push.

## Permanent village identity storage
- Branch `codex/village-identity-storage`; schema 11 adds world UUID/name evidence,
  settlement bindings, bed ownership and entity/resident links, preserving all old data.
- Repeat visits/renames match identities; conflicting town evidence is reported and
  never merged. Bindings cannot silently change owners. Legacy rows are not auto-bound.
- Migration uses bound world UUIDs and prevents same-folder-name cross-world moves;
  old unbound rows retain separated legacy boundaries.
- Nine new temporary-DB tests. Windows wrapper clean verify passed 152 unit and
  two integration cases (154 total), zero failures/errors/skips.
- Runtime importer, observer, projections, calendar scheduling and diagnostics remain
  pending. Phase 1 stays 70%; no actual DB/world/server changes, merge or push here.

## Schema-11 Paper follow-up
- Source 62550ee/154-test JAR enabled on the authorized disposable Paper server;
  schema 9 upgraded to 11, integrity verified and clean stop confirmed.
- Signature audit found no common GitHub/AWS credential/private-key patterns in
  reachable tracked history; no remote publication. Hosted CI remains pending.
- Population import/scheduling/projection remain upcoming; Phase 1 stays 70%.

## Atomic village enrollment and hosted CI
- Branch `codex/village-enrollment`; schema 12 adds optional bed/home links.
  One transaction imports names/ages/identities, shared homes, housing evidence,
  approved prototype partners and authoritative headcount. Failure rolls all back.
- Eleven new temporary-DB cases include restart/rename, partial observations,
  deceased identities, final-write failure/retry, protected old/overlapping homes
  and actual calendar-driven saved births independent of visible census.
- Windows wrapper clean verify passed 163 unit and two integration cases (165 total),
  zero failures/errors/skips. Observer/runtime scheduling/projection still unwired.
- User approved pushing exact snapshot e85416e on `codex/phase1-foundation-ci`.
  Both hosted Windows and Ubuntu jobs passed:
  https://github.com/blueninjas24/Civilizations/actions/runs/37725399200
- That snapshot has 154 tests; newer enrollment work remains local. Main unchanged,
  no merge. Phase 1 now 8/10 (80%), with the CI/Paper/recovery gate verified.
- No actual DB/world/server change in this enrollment batch. See VILLAGE_ENROLLMENT.md.

## Dedicated population owner and timer
- Branch `codex/population-runtime-clock`: PopulationSession owns JDBC and imports
  on one thread, queues immutable observations, publishes immutable snapshots and
  runs bounded calendar catch-up on a real fixed-delay timer.
- Editable documented test cadence/cap, protected saved settings, historical import
  delay, explicit 64-operation overload rejection, failed-write retry and safe shutdown.
- Ten new temporary-DB tests. Windows wrapper clean verify passed 173 unit and
  two integration cases (175 total), zero failures/errors/skips.
- Plugin wiring/config/commands/observer/projection still pending, so Phase 1 stays
  80%. No new world/server/actual database changes, merge or push in this batch.

## Paper calendar and read-only diagnostics
- Branch `codex/paper-population-clock`: asynchronous owner-thread startup, strict
  editable config, main-thread initialization polling and shutdown. No shared JDBC.
- Read-only `/civ debug status` and `/civ debug settlement <uuid>` inspect immutable
  snapshots with operator/console permission. No mutation or reload command.
- Old census listener retained but unregistered to protect authoritative residents;
  automatic observer/import/projections still pending.
- Windows wrapper clean verify passed 188 tests (186 unit, two integration), zero
  failures/errors/skips. Thirteen new config/permission/command/diagnostic cases.
  Initial sandbox build failed to resolve OneDrive target with AccessDeniedException;
  expanded-access build passed without build/OS/location changes.
- Backed up marked disposable schema-11 DB before deployment. Pinned Paper 26.2-129
  enabled the JAR, created config, advanced a real month, served status/validation
  messages, stopped and restarted at saved month 1. Second run advanced to month 3
  and stopped cleanly. Only approved temporary dev files changed; schema now 12.
- No populated village/player scenario, production change, merge, push or main
  modification. Phase 1 now 9/10 reporting milestones (90%).

## Automatic Paper village observation
- Branch `codex/paper-village-observer`: main-thread incremental chunk snapshots,
  loaded-entity guards, bed-head deduplication and coverage/border completeness checks
  now feed the existing SQL-owner importer automatically. No chunk tickets, POI
  searches, block writes, entity mutations or loaded-census replacement.
- Configurable budgets and read-only `/civ debug observation`; scan/import/cooldown
  queues bounded. Partial windows invalidate housing confidence. Indivisible API calls
  mean the time budget is a target, not a hard deadline; seed fairness needs profiling.
- Windows wrapper clean verify passed 197 tests (195 unit, two integration), zero
  failures/errors/skips. Nine new cases cover scan budgets, negative heights,
  bed halves, boundary closure, observer config and command diagnostics.
- Real Paper caught negative-height snapshot section indexing. Inspected pinned
  implementation, added adapter/regression test and reran successfully. Rejected
  initial frame performed no import.
- Disposable fixture automatically imported two beds/two adult villagers as one
  settlement/home with housing capacity four. Repeat loaded scans preserved people.
  Removing console test tickets unloaded the area; calendar continued to month 27
  with two saved residents and a partial-frame diagnostic, without loading it again.
- Final JAR restart/reload/re-observation retained exact settlement, household,
  resident and entity-link IDs. Read-only DB comparison: ages advanced exactly eight
  months with clock 27 to 35; two residents, schema 12, integrity ok, no FK errors.
- No actual client play, NPC projection or demographic balance claim. Test server
  stopped and console loading tickets removed. Only approved disposable dev files
  changed; no schema change, production data, merge or push. Phase 1 stays 90%.

## User workflow
`docs/PROGRESS.md` tracks verified roadmap milestones in plain English. Current snapshot: Phase 1 has ten of ten prototype milestones complete; Phase 2 has four of four prototype reporting groups complete; Phase 3 has one of four groups complete with automatic farm businesses. Phases 4–9 have none complete. These counts are not time estimates or release-readiness percentages. Future batches update it alongside this status file.

This ChatGPT thread is product/design HQ. Codex is implementation agent. It may proceed autonomously on scoped engineering work, committing to feature branches, but must escalate consequential design decisions and destructive operations. Preserve the full nine-phase roadmap.
