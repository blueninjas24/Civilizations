# Phase 3 — businesses and industry checklist

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


October13 hybrid farm allocation/payment batch verified1,378 full cases and a
real Paper timer/customer/pause/restart check. Expanded scope adds six resource
entries:29/39 now complete; earlier70% was the old denominator. Livestock
ownership is the pending decision. See HYBRID_FARMS.md.

Already-established stone quarries and sand pits now work on the live simulation
timer. Sand is extracted faster than stone, using editable settings. Supplies stay
finite: removing one supporting block loses only its share, and restarts cannot
refill it. Workers improve through real work. All1,333 full-build tests and the
isolated Paper timer/pause/restart proof passed.

Automatic selection and founding of new quarries/pits remain unfinished, alongside
forestry, wider recipes and specialization acceptance. No decision is pending. Your
playable server is unchanged; Phase3 remains1/4 whole groups/~70% partial.
See TIMED_QUARRIES.md. Older notes describe earlier snapshots.

Villages can now train their first fishing owner using real supplies, then pay to
open a fishery beside suitable water. Catches can be sold and delivered into the
village's food reserve. Workers improve through real catches, and damaged banks
can use a safe replacement without losing the business. No free skill or money.

All 1,319 full-build tests passed. The isolated Paper test started with zero fishing
experience and passed training, startup, paid food delivery, recovery and restart.
The verified current customer/recipe milestone is complete; forestry, remaining
stone/sand/bakery connections and specialization acceptance still remain. Phase 3
is 1/4 whole groups/~70% partial. No decision is pending; your playable server is
unchanged. See FISHERY_FOUNDER_TRAINING.md. Older notes describe earlier snapshots.

Fishing workers now improve through actual catches. Existing fisheries can train
new employees using real supplies, then hire them once qualified. Full storage,
unsafe banks and absent worlds cannot generate free experience. All 1,308 full-build
tests passed, and the isolated server verified real skill gains and restart safety.
Automatic training for the FIRST fishing owner remains next; no skill is invented.
Other basic industry connections and specialization acceptance still remain open.
No decision is pending, and your playable server is unchanged. Phase 3 remains
1/4 whole groups/~69% partial. See FISHING_SKILLS.md.

Qualified residents can now open shoreline fisheries automatically when the village
has suitable water, a safe bank, real demand and enough protected money. Damaged
banks can move to a safe nearby spot without losing the business. Lost fishing water
reduces the fish supply; restoring it does not instantly refill fish. Unknown or
unloaded blocks do not count as damage. Buildings and docks remain Phase 4.

All 1,302 full-build tests passed, and the isolated Paper server passed actual startup,
catching, persisted recovery, money conservation and pause/restart checks. The native
check uses explicit test skills: automatic first-owner fishing training remains next.
The playable server is unchanged. No decision is pending. The broader industry and
specialization checklist remains open; Phase 3 is 1/4 whole groups/~69% partial.
See FISHERY_WORKPLACES.md. Older batch notes below describe their earlier snapshots.

You approved adapting after shoreline damage. The new recovery planner looks for
another safe nearby bank beside the same fishing water, or flags a repair need.
It never closes the business or resets its fish. Unknown/unloaded blocks cannot
be treated as damage. Physical repairs and docks remain Phase4 work.
All1,287 full-build tests passed. The isolated Paper check found a real safe
replacement under simulated missing-block input, without breaking world blocks.
Automatic saved workplace recovery/founding is still unfinished; this is a tested
planning step. Your playable server is unchanged. No decision is pending for this
work. Phase3 remains1/4 whole groups/~69% partial.

Fishery preparation now chooses a genuinely qualified adult using saved fishing
skills. It cannot take someone from an existing worker job or active studies, and
does not invent experience, money, a workplace or a business. Existing owners may
qualify for another business, preserving your unlimited-ownership rule.
All1,280 full-build tests passed, plus the isolated server API/accounting/
delivery/pause/restart check. Progress notes and development log are updated.
Actual automatic founding/training and fresh shoreline/water acceptance remain
unfinished. The removed-standing-spot decision is pending; docks remain Phase4.
Your playable server is unchanged. Phase3 stays1/4 whole groups/~69% partial.

Fishery planning now checks that opening costs leave enough real village money
to buy the catch, and that existing suppliers do not already cover that need.
It finds bounded nearby water candidates without opening businesses or changing
blocks. Eight added checks passed within the focused122-test run.
All1,272 full-build tests passed. The isolated server passed identity safeguards
and reserve/payment/delivery/pause/restart regression checks.
Qualified founders/training, fresh shoreline/water acceptance and automatic startup
are still next. The removed-standing-spot rule is awaiting your answer; docks remain
Phase4. Your playable
server is unchanged. Phase3 remains1/4 whole groups/~69% partial.

Villages can now buy a small emergency food reserve before anyone is hungry.
Purchases use real money and real food; what is stored or already on the way reduces
orders. Admins can edit the reserve size in economy.yml. Old files keep their rules
until explicitly opted in. This makes a legitimate customer for future fisheries
while keeping food-protected startup funding. Focused124 tests passed.
All1,264 full-build tests passed. The isolated server also passed real reserve
purchases, delivery, money conservation and pause/restart checks.
Automatic fisheries still need qualified/trained founders, fresh shoreline/water
acceptance and scheduling. Docks remain Phase4. No decision is pending for that
work. Your playable server is unchanged. Phase3 stays1/4 whole groups/~69% partial.

Fishery startup now saves its funding, qualified business, worker and fish supply
as one operation: any failure cancels the whole investment. Restarting cannot
charge twice or refill used fish. Six new temporary-database tests passed;
all1,256 full-build tests passed with zero failures/errors/skips.
Automatic fisheries are still unfinished. One economy decision is pending: may
villages buy a small emergency food reserve before a shortage? Currently they only
buy when hungry, but new-business funding is blocked then. Shoreline fishing is
approved; dock construction and its efficiency upgrade remain Phase4.
Your playable server is unchanged. Phase3 stays1/4 whole groups/~69% partial.

Shoreline fishing starts are approved. Safe standing-point checks now pass1,250
full-build tests and a real Paper check finding11 suitable shoreline positions.
Automatic business startup still needs qualification, customers and funding;
docks and their substantial efficiency upgrade are planned for Phase4, prioritized
once affordable. Your playable server is unchanged. No design question is pending;
Phase3 milestone counts remain1/4 whole groups/~69% partial.

The plugin can now save nearby water findings and choose connected fishing
locations without combining separate puddles or claiming a whole ocean. All1,245
full-build tests and the real Paper connected-patch/pause/restart check passed.
Automatic fishing startup still needs safe workplace recognition, qualified owners,
real customers and protected funding. Shoreline-versus-dock choice is pending.
Phase3 counts remain1/4 whole groups/~69% partial; your playable server is unchanged.

Water checks now run on the server and save fresh findings safely. All1,238
full-build tests and the isolated Paper scanner/restart check passed. Water alone
still creates no business or money: connected site selection, qualified owners
and protected startup funding are next. Phase3 checklist counts stay unchanged.
Your playable server has not been changed. See WATER_OBSERVATION.md.

October12: bounded full-height read-only WaterScan/PaperWaterSource evidence added;
immutable world/chunk/time/depth/column status, absolute negative Y, unknown safety
and explicit per-call budgets. Eight new tests; focused38/full1,229 passed. Native
actual snapshot reference passed:220 suitable columns,512 reads/tick; normal shutdown.
Read-only save audit passed. Cleanup initially deferred under approval-review usage
limit; resumed approved audit/cleanup succeeded, original settings restored, helper
archived and fixture stopped. No migration, scheduler,
ownership claim or automatic fishery startup. Player ponds approved alongside natural
water; no design choice pending. Live evidence/qualified funding integration
remains next; broader renewable industry milestone and counts stay unchanged.

October12: approved confirmed habitat-loss/restoration implemented in the explicit
SQL-owner fishery transition. Schema67 preserves original footprint/abundance;
reduced effective capacity loses excess only, restoration does not refill, dry
timers cannot catch/recover, unknown/stale/incomplete evidence cannot remove fish.
Five domain/six temporary-SQL cases added; focused53/full1,221 passed, zero failures/
errors/skips. Guarded native dry/restoration/normal-timer/pause/restart passed;
main save audited, settings restored and helper archived. Automatic water observation/qualified founding remains next;
the broader renewable-industry milestone and all counts remain unchanged.

October12: persistent habitat stocks/column identity, atomic qualified catches/
owned food and ordinary industry clocks added in schema66. Ten new temporary SQL
cases passed, including real public food purchase/arrival/consumption, rollback,
absence/restart and retune/no-refill. Full1,210 tests and the guarded native normal
owner-timer/pause/restart check passed; main save audited, fixture properties restored.
Automatic water
observation/qualified startup remains unfinished, so broad renewable catalog
milestone and counts are unchanged. See FISHING_RESOURCES.md.
The habitat-loss choice recorded at that checkpoint has now been approved above.

October12: fish-stock renewal approved. Added bounded immutable resource/catch
calculations and strict editable fishing-resources terms: fractional recovery,
temporary exhaustion, exact absent-world pause, no refill from opening retunes,
qualified worker/site/storage limits. Eleven new tests; focused87/full1,200 passed,
zero failures/errors/skips. Schema65 unchanged; no live fishing production or DB/world
changes. Persistence, water evidence/identity, qualified founding and ordinary
timed owned-food output remain next. The broader industry milestone remains open;
counts unchanged. See FISHING_RESOURCES.md. Earlier pending choice below is historical.

October12 verified: native stone/deepslate/sand/red-sand placement/removal/movement
and formation safeguards, bounded SQL mutation queue and explicit quarry snapshot
classification. Eight new tests; focused33/full1,189 passed. Native real-block/
snapshot and synthetic event-handler proof passed; extracted stock preserved,
exact temporary block restoration/ticket release, main save audited. Schema65
unchanged. Current quarry surveys/opportunities/founding/runtime remain open;
no whole checklist milestone completed. Fishing supply recovery decision pending.

October12: approved real village food/practice-material customer path connected,
with real treasury/supplier goods, shared budgets, protected food cash, owned
delayed imports, normal SQL-owner scheduling and read-only funded demand. Schema65;
focused114 and full1,181 passed; isolated normal-session arrival/practice/absence/
restart passed, exact save audit/restoration completed. Broader live industry
observation/founding/production still incomplete; checklist counts unchanged.
See VILLAGE_SUPPLY_CUSTOMERS.md. Earlier pending-customer/source notes below are
historical checkpoints; these decisions have now been approved.

Verified October12: approved stone/sand baseline, bounded pure quarry scan,
matching real-grant startup and finite non-1:1 coordinate-backed extraction;
transactional schema64 parent upgrade/FK checks/rollback/retry preserve old assets.
1,163 full Wrapper tests passed. No live quarry observer/mutation adapter/automatic
founding/customer path yet. Broad catalog entry remains unchecked; counts unchanged.
See FINITE_QUARRIES.md. Needs-based public customer funding decision pending.

Verified October12: logging/fishing/quarry/sandpit grant eligibility under unchanged
shared food-protected limits, preserved installed opt-ins and transactional/restart
regressions. Full Wrapper1,148 passed plus final29 grant cases. No new resource or
producer adapter, no migration and no new checklist entry complete; counts unchanged.
Stone/sand source-baseline decision pending. See STARTUP_GRANTS.md.

Verified October12: ordinary automatic warehouse startup and real first-merchant
commerce study/current prospect reassessment; schema63 transactional links and
rollback/retry. 1,138 full Wrapper tests plus final packaged positive Paper startup/
marker/restart check passed. Current coal/iron/toolkit catalog automatic first
nonfood founding and bounded founder investment entries now complete. Counts
4/4,27/33,14/15,0/5 (~69% partial); broader catalogs/competition/specialization remain.
See AUTOMATIC_WAREHOUSES.md. Wider-industry startup funding is now approved under
unchanged shared caps; resource adapters remain unfinished. Counts unchanged.

Verified October12: qualified first-warehouse preparation/current acceptance,
real protected grant/capital/empty store/job/fixed barrel and atomic rollback/replay.
1,124 Wrapper tests passed; schema62 unchanged. Automatic merchant study and normal
scheduler remain next. Broad counts unchanged. See WAREHOUSE_INVESTMENT.md.

Verified October12: first mineral-chain warehouse prospect review for actual paid
workshop needs, complete suitable finite sources and protected whole-chain budgets.
1,110 Wrapper tests + packaged Paper regression passed; schema62 unchanged.
Read-only evidence/API only; merchant preparation/training/acceptance/scheduling
remain next. Broad counts unchanged. See WAREHOUSE_OPPORTUNITIES.md.

Verified October12: automatic first-workshop owner study using real future-period
practice supplies, current opportunity pause/reassessment and normal paid startup.
1,095 full Wrapper tests plus unskilled-owner normal timer/automatic Paper startup
passed; schema62 transactional migration verified. First warehouses and broader
industries remain; broad counts unchanged. See WORKSHOP_FOUNDER_TRAINING.md.

Verified October12: ordinary scheduled first-workshop startup for genuinely qualified
owners, one pending SQL task/loaded point per pass, real protected paid startup,
no duplicate investment. 1,085 Wrapper tests and actual scheduled Paper passed.
Schema61 unchanged; first-owner training remains next. Broad counts unchanged.
See AUTOMATIC_WORKSHOPS.md.

October12 verified next batch: qualified first-workshop proposal and current-fact
atomic acceptance, actual paid toolkit production/farm purchase, SQL-owner/Paper
handoff and restart/pause/replay guards. Wrapper: 1,079 tests, zero failures; native
actual table proof/acceptance and marker lifecycle passed. Schema61 unchanged.
Automatic founder training and normal scheduling remain next; broad milestones
and partial counts unchanged. See WORKSHOP_INVESTMENT.md.

October12 verified: approved new marker-founded business pause/restoration, atomic
startup binding, real production/trade guards and retained assets/history. Full
Wrapper verification: 1,067 tests, zero failures; actual disposable Paper scheduled
point checks and restart passed. Schema61 migration/retry covered. First founder
preparation and automatic acceptance remain next; no whole checklist milestone
has newly completed. See WORKPLACE_OPERATIONS.md.

Verified first-workshop investment preparation: actual farm toolkit need, current
marker, real shared supply/grant/paid-starter feasibility, bounded read-only owner
review, no free firms/skills/funds. 1,054 Wrapper tests plus disposable Paper passed;
schema60 unchanged. Actual founder preparation/current-block acceptance and marker
removal policy remain; automatic nonfood founding stays incomplete. See
WORKSHOP_OPPORTUNITIES.md.

Verified approved one-time paid workshop starter surplus: real stock/cash, protected
reserves, atomic grant/startup/purchase, reserved delayed cargo, replay and sale
value. 1,043 Wrapper tests plus disposable Paper passed; schema60 adds empty audit
tables. This is a trusted startup seam; automatic workshop opportunities/training
and first warehouse founding remain. Checklist counts are unchanged. See
WORKSHOP_STARTER_SUPPLIES.md.

Verified public farm upstream equipment demand: actual work/current food proof,
real protected cash, owned/incoming kit guards, shared once-counted stock, bounded
review and paid producer-to-warehouse-to-farm delivery/use. Eight new tests;
1,027 total + disposable Paper passed. Schema59 unchanged. This narrows the real
customer gap; first nonfood founding/wider recipes still remain. See
PUBLIC_FARM_INDUSTRY_DEMAND.md.

Verified first mining founder training for current fundable real opportunities,
future-only actual practice materials, cancellation, qualification and atomic
startup. Schema59;1,019 tests and native Paper passed. Other first founders and
industrial investment remain; see MINE_FOUNDER_TRAINING.md.

Verified read-only real startup affordability, shared acceptance evidence and
funding-before-founder mine diagnostics. Six tests added; 1,009 Wrapper tests and
updated native Paper probe passed. No migration or new grant settings. First
warehouse/workshop founder opportunities remain next; see STARTUP_GRANTS.md.

Verified approved barrel/smithing-table workplace observation: bounded loaded
snapshots, current candidates and retained history, removal/conflict/occupation
guards, schema58 migration and legacy opt-out. 1,003 tests plus scheduled Paper
probe passed. No inventories or property claims; automatic first warehouse/workshop
founding remains unfinished. See WORKPLACE_MARKERS.md.

Verified current-evidence automatic coal/iron mine startup for existing actual
funded workshop/warehouse demand: qualified founder, protected real shared grant,
finite coordinate reserve, seeded bounded selection and existing-capacity guard.
990 tests and actual SQL-owner Paper passed; schema57 unchanged. First warehouses,
workshops and founder training remain. See AUTOMATIC_MINES.md.

Verified actual-vacancy career study: saved bounded choices, real practice supplies,
normal qualified hiring, vanished-opening cancellation, atomic rollback and
restart/world-pause safety. 977 tests and native Paper passed; schema57 preserves
old course/history. First founders remain; see VACANCY_CAREERS.md.

Verified October11 commerce batch: editable supply-backed courses and actual paid
warehouse-order experience, transactional schema56 audits, 963 passing tests and
isolated Paper checks. Sales-only learning, career choice and first merchant
founding remain. Reporting remains25% by complete groups.

Updated October 11, 2026. Reporting progress remains one of four complete groups
(25%). Completed backend components do not complete live gameplay milestones.

Verified vacancy hiring: bounded, qualified unemployed local workers at existing
live-owner nonfarm firms; saved employment/audit, death replacement, restart and
world-pause checks. 917 tests and isolated Paper passed. Farm equipment funding
is approved; its protected public-budget adapter is next. This does not complete
automatic careers/founding or specialization.

Verified public farm equipment funding seam: actual working fields/current food
proof, one real toolkit, protected actual treasury/period budget, paid cargo,
rollback/replay and preserved legacy catalogs. 931 tests passed. Actual wear,
food-clock deliveries, automatic replacements and funded upstream pressure remain;
this seam does not complete the connected-production group.

Verified actual public farm upkeep: food-clock delivery/use/service, fractional
slow work, zero-output protection and bounded real funded replacements. 940 tests
and actual PopulationSession on isolated Paper passed. Recurring farm equipment
customers are now real; automatic first qualified industries/opportunity selection
and wider specialization remain.

Verified approved supplier upkeep contributions: exact actual invoice shortfall,
sales-backed surplus after original capital + editable reserve, saved caps,
transactional conservation/replay and active-world pause. 951 tests + isolated
Paper passed; schema55 adds empty audit. No blanket levy, minted cash or net-profit
claim. Nonfood founders and specialization remain. Warehouse producer scanning
needs a narrow audited-farm equipment exclusion; then public farm upstream demand.

Verified warehouse compatibility: narrow audited farm kit-store exclusion lets
actual upstream purchases proceed without hiding unknown stored industries.
952 tests and actual PopulationSession/IndustryRuntime isolated Paper passed.
No migration. Automatic first nonfood founders and specialization still remain.

Verified bounded funded-supplier choice: a poor stocked supplier no longer delays
an otherwise fundable actual farm purchase. 953 tests and actual PopulationSession
Paper passed; no migration. Gradual commerce study and real transaction learning are now approved; the verified
first batch supports paid warehouse upstream orders. Automatic founders remain
unimplemented; no instant profession conversion. See COMMERCE_LEARNING.md.

## 1. Persistent businesses, owners, workers and sites — verified

- [x] Save identities, founders, owners, workplace sites and employment history.
- [x] Require eligible residents and real startup capital; prevent double jobs.
- [x] Automatically found/staff real observed farms and produce worker-backed food.
- [x] Validate founding, retries, restarts and absent-world pause on disposable Paper.

## 2. Resources and connected production — in progress

- [x] Hybrid farm contributions and owned surplus: capped actual allocation,
      finite storage, tools/skills, replay and preservation of old public goods.
- [ ] Paid farm food/ingredient customers, protected real funds, causal delivery
      and income; local food priority without confiscation or invented shoppers.
- [ ] Basic livestock herds, feed/capacity, breeding reserves, bounded slaughter,
      renewable eggs/milk, saved ownership/history and no spoilage.
- [ ] Suitable livestock observation, qualified/funded startup and live timed
      operation; no assumed animals, forced chunks or duplicate physical herds.
- [ ] Connected bakeries with real grain/egg/milk ingredients, appropriate recipes,
      qualified staff, owned stock, actual purchases and collective food customers.
- [ ] One general craft workshop with editable useful recipes and actual household/
      business demand; specialized craft occupations deferred.

- [x] Finite geological reserves, shared ore/rock work, bounded business storage.
- [x] Paid local warehouse transactions and unavailable-until-delivered cargo.
- [x] Workshop recipes consume owned inputs with saved conservative accounting.
- [x] Permanent ore-coordinate identity and no restart/revisit refill.
- [x] Approved even legacy reserve conversion, per-coordinate depletion and atomic
      explicit retirement; verified conservation/rollback/restart and packaged Paper.
- [x] Connect native break/explosion/piston removal capture and immediate/persisted
      production trust guards; verified cancelled/destructive events and actual
      registered listener/SQL-owner wiring on disposable Paper with restored blocks.
- [ ] Add reviewed recovery/baseline tooling for old coordinate saves or capture gaps;
      do not silently certify missing mutation history or reset reserves.
- [x] Bounded exposed coal/iron scanner with supplied exclusion coordinates.
- [x] Save player-placement exclusions and enforce them against stale discovery.
- [x] Capture native placements with clean-stop/failure protection and verify on Paper.
- [x] Verify conservative piston movement exclusions on Paper.
- [x] Conservatively fence recognized/custom unsupported bulk-editor presence and
      later enable events; verify real event and retained recovery gap on Paper.
- [ ] Certify supported bulk-edit mutation integration before relaxing that fence;
      unknown editors/offline edits remain outside provenance guarantees.
- [x] Split complete observed coal/iron evidence into deterministic connected patches;
      retain coordinate identity and finite reserves on replay.
- [ ] Connect safe loaded-chunk observation and coherent resource opportunities.
- [x] Save latest complete survey membership separately from historical sightings;
      newer empty frames clear candidates, older frames never restore them, and
      read-only owner reviews fence stale/future/absent-world/claimed/excluded ore.
      Demand, permitted site assessment and connected funded selection remain.
- [x] Retain validated positive coal/iron evidence by town/world/coordinate across
      restart with capture times, atomic rollback, live mutation exclusions and
      bounded diagnostics; 828 tests and scheduled packaged Paper persistence passed.
      Fresh suitability, demand, funding and automatic selection remain.
- [x] Register bounded read-only saved-home ore observation with SQL-owner identity
      and mutation checks; verify the scheduled pipeline on disposable Paper.
      Wider coverage, suitability, durable opportunity selection and funding remain.
- [x] Add bounded editable nearby loaded-chunk rings, preserve capture time and
      retain completed diagnostics; verify neighboring ore/exclusions on Paper.
      Durable suitable funded opportunity selection remains unfinished.
- [x] Atomic trusted mine startup joins founding, finite discovery, empty storage,
      actual capital and first worker; verify rollback/replay and packaged paid chain.
      Automatic opportunity selection and operation remain unfinished.
- [x] Timed operation of existing funded/staffed mines/workshops through the live
      SQL owner, with atomic receipts/checkpoints, absent-world/downtime pause and
      unloaded data-only work; verify failure isolation and actual session on Paper.
- [x] Narrow automatic mine founding for actual existing funded workshop needs and
      staffed warehouse buyers; qualified owner/first worker, protected grant,
      current observed ore, no refill or speculative stock. Verified on Paper.
- [x] Automatically select/fund/staff nonfood industries under editable settings.
      Current coal/iron/toolkit catalog: real-demand mines, funded toolkit workshops
      and mineral-chain warehouses now have normal qualification, gradual first
      owners, protected grants and observed-site acceptance. Wider resource catalogs
      remain a separate unchecked entry; no universal industry or profit guarantee.
- [x] Approved limited configurable grants, current food-money protection, real
      treasury, shared economic-period/lifetime quotas and atomic startup/capital/
      first-worker receipts; 844 tests and packaged Paper restart/accounting passed.
      No ownership cap or automatic startup; missing old settings remain disabled.
- [x] Connect real local customers and recipes; demand-based reserves, no fake buyers.
      Verified current mineral/tool and food/practice catalogs: paid owned goods,
      protected spending, once-only delivery and actual consumption/reserves.
      Broader resource/recipe catalogs remain the separate unchecked item below.
- [x] Read actual staffed funded workshop input gaps using editable recipe/buffer/
      prices, owned/pending goods, finite storage and once-counted warehouse stock;
      match only currently observed needed minerals. This is a read-only review,
      not finished-goods demand, orders, production or automatically founded mines.
- [x] Existing workshops automatically buy complete affordable recipe inputs from
      actual local warehouse stock; verify payments, delayed cargo, input work,
      rollback, legacy clock isolation, world pause and restart on real Paper.
      Upstream stocking is verified below; actual finished-goods demand remains.
- [x] Existing staffed warehouses buy real extracted local mine output for funded
      workshop needs; shared available/pending stock, real funds, delayed deliveries,
      hauling, rollback/restart/disable verified in SQLite and actual Paper owner.
      No automatic founding, markup or imaginary finished-goods consumers.
- [x] Approved public farm equipment upkeep, actual work/service, protected real
      treasury, paid cargo and automatic bounded replacement purchases; verify
      rollback, zero work, disable, absence/restart and actual session on Paper.
- [x] Approved configurable business-paid tool wear/replacements, startup grace
      and slower productive work without tools; record genuine maintenance demand.
      902 tests and actual Paper owner wear/cargo/restart checks passed. Farms'
      separate cycle remains. Real workshop output now supplies warehouses too.
- [x] Extend approved protected startup grants to warehouses under shared caps;
      real empty-store capital/qualified first worker, 908 tests and Paper passed.
      Suitable sites, skilled founders and automatic startup opportunities remain.
- [ ] Add suitable renewable forestry/fishing opportunities and finite stone/sand
      production as scoped catalog/observation work; do not infer resources from a
      biome alone or claim ores beyond the implemented coal/iron catalog.
- [ ] Verify live production chains, capacity/depletion and unloaded operation.
      Current coal/iron/toolkit operations have focused native checks plus a
      50-period connected SQLite depletion/accounting regression; wider catalogs
      and full acceptance remain. See CONNECTED_INDUSTRY_SCENARIO.md.

## 3. Business lifecycle and competition — in progress

Approved October 9: eligible industry-qualified family first, then a qualified
employee, otherwise an affordable qualified sale with configurable valuation and
deadline. Save proceeds in a separate family estate fund; failed sale closes
operation but preserves assets/site/history. Separate immutable founder from current
owner before implementing transfer. Test qualification, capital conservation,
retry/rollback/restart, estate eligibility and no invented funds. Suitable vacant
premises are preferred; physical building reuse remains Phase 4.

- [ ] Explainable pause/closure/reassessment and competition for workers/capital.
- [x] Preserve closed firms, sites, inventories and employment history.
- [x] Saved unsold-offer expiry closes operation/releases jobs atomically while
      retaining stock/cash/resources/sites/history; immediate expiry guards,
      paid cargo/replay, legacy farms, temp migration/failure tests and Paper restart
      passed. Automatic valuation/listing/buyer review now verified below; general competition remains.
- [x] Tested ownership succession with explicit recipient eligibility and accounting.
- [x] Atomic family-first/qualified employee inheritance, immutable founding/current
      owner separation and present-world runtime integration; temp SQL rollback/retry
      and actual Paper mine/restart checks passed. Resale/estate/closure remain.
- [x] Paid acquisition backend with real buyer business/estate funds, configurable
      limits, atomic proceeds/audit/ownership and SQL-owner world/date gates;
      temporary SQLite and actual Paper replay/conservation passed. Automatic
      listings/valuation/deadlines and unsold closure remain unfinished.
- [x] Configurable approved existing-staff operation after owner death, including
      real paid warehouse/workshop inputs, qualified finite mining and inherited
      farm staffing; temporary DB and Paper pause/restart/conservation verified.
- [x] Actual timed mine/workshop employees gain bounded configurable skills, saved
      with production/audits/checkpoint; rollback/replay/retuning/absence/restart
      and qualified succession verified in temporary SQLite and Paper.
- [x] Saved unemployed-adult courses consume real village practice goods with food/
      material priority, rotating slots, skills/status/spending/period atomicity,
      absence/backlog/restart guards and diagnostics; temp DB/Paper passed.
- [x] Farm employees gain experience with actual food production, limited by
      observed productive slots; transaction rollback, pause/restart, retained
      ceilings verified in temporary SQLite and actual Paper PopulationSession;
      earned succession qualification covered in SQLite. Legacy adults do not learn.
- [x] Automatic existing-vacancy study with real job requirements and later ordinary
      hiring; cancellation, supplies, saved pages and actual Paper verified.
- [x] Bound broader skill development and investment; no manufactured profit or payroll.
      Current farm/mining/smithing/commerce catalog has saved actual work learning,
      bounded supply-backed courses, current first-founder opportunities and real
      qualified investment integration. Gradual first mine/workshop/warehouse
      founders verified; no free skills, money or bypass of requirements. Wider
      new industry catalogs and general competition remain separate work.
- [x] Saved explicit sale offers, immutable accepted price/deadline, online-only
      elapsed time and atomic paid completion; temp SQL failure/restart/migration
      cases and actual Paper passed. Automatic valuation/buyers and retained closure now verified below.
- [x] Automatic offers and qualified buyers, immutable configurable prices, saved
      buyer pages and real estate/business payments; transactional failure/retry,
      migration, unlimited ownership and actual PopulationSession Paper restart
      passed. No forced living-owner sales or extra worker jobs.
- [x] Restart-safe turnover and failed-transition recovery scenarios for implemented
      inheritance/resale/closure paths; seeded multi-year specialization remains group4.

## 4. Emergent specialization and acceptance — pending

- [ ] Opportunity selection compares viable industries, demand, labor and capital.
- [ ] Different geography/history produces different mixes; no universal industry list.
- [ ] Seeded multi-year scenarios cover shortages, failed opportunities, depletion,
      closures, succession and exceptional firms without guaranteed outcomes.
- [ ] Actual disposable Paper startup/runtime/restart, diagnostics and profiling.
- [ ] Document playable commands/settings, limitations and a human-client checklist.

## Shared rules and phase boundaries

All industries need suitable sites, labor, timed bounded output and editable default
ratios. Renewal depends on the resource: crops/trees differ from finite minerals,
stone and sand. Deposit yield and material work resistance are separate from price.
Exploration has a budget and can fail; no unlimited digging or omniscient ore search.

Funded expected needs should drive stock targets. Construction demand enters only
when actual projects exist; never create fake consumption to clear full storage.
Stone/sand can be primary products, not permanent waste labels. Disposal remains
an explicit costly proposal until separately implemented and tested.

Physical shafts/branches/quarries, entrances/supports/cart installations, authored
structure pieces and work positions belong to Phase 4. Nearby workers will be saved
employees, not duplicates. Regional routes belong to Phase 5; cave-ins/fires and
physical deterioration require later protection/failure-safety gates. Retained sites
and history do not guarantee that every physical block survives damage forever.

Aim for a useful Phase 3 playtest, without promising 100% before its actual gates
pass. For implemented behavior and latest verification see PROJECT_STATUS.md;
ROADMAP.md remains the canonical phase definition.
