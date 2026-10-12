# Civilizations progress tracker

Updated October 12, 2026. Based on verified work, not planned features.

Approved next step: villages buy real food when hungry and practice materials
when current trainees need them. Payments use existing village money, with
editable spending limits; deliveries arrive before use. All 1,181 tests and the
isolated server check passed, including pause/restart. Broader industry
recognition/founding still needs work, so milestone counts have not changed.

Stone and sand now have tested finite production foundations: each source holds
more than one unit, workers extract gradually, and removing one supporting block
loses only its remaining supply. Restart never refills it. Admins can edit abundance
and work speed separately. All1,163 tests passed. Automatic quarry discovery/founding
and real customers remain unfinished; checklist counts stay unchanged. Your server
has not been changed.

You approved logging, fishing, stone quarries and sand pits sharing the existing
startup budget. Their funding eligibility is connected, with no extra village
money or weaker food protection. Their resource and production adapters are still
unfinished, so the Phase3 checklist counts have not increased. All1,148 tests
passed, with the final shared-budget assertions also rechecked separately.

Villages can now recognize nearby barrels and smithing tables as possible
workplaces, while leaving blocks and their contents alone. Saved evidence survives
restart; removed markers stop being candidates. All 1,138 tests and the disposable
Paper check passed. Your playable server has not been changed.

New marker-founded firms now pause if their table/barrel is confirmed missing and
resume when it returns to the same spot. Their workers, money, supplies and history
survive. Unknown/unloaded blocks are left alone. This is verified startup/operation
support; qualified first workshops and warehouses now start automatically.
Their gradual first-owner training is connected and verified.

A qualified workshop owner can now go through a tested paid startup: real customer,
food funds, materials and actual table are checked again before spending. Tests
also verify the resulting toolkit can be bought by a working farm. Automatic first
owner training and the ordinary startup scheduler are now connected and verified.
An unemployed adult learns gradually using real supplies before the business can open.

Warehouse planning can now check real workshop input needs, observed ore, a nearby
barrel and enough protected money for the whole starter chain. This creates
no assets by itself. Qualified merchants can now pass tested paid warehouse startup;
merchant training and automatic opening are now connected and verified.
An unemployed adult earns real commerce experience before opening a funded empty
warehouse; mines must still produce actual stock for it.

Phase 3 has one of four major groups fully complete. The partial work inside the
other groups is now shown here instead of being hidden behind a fixed25% bar:

| Phase3 group | Completed checklist entries | Partial checklist progress |
|---|---:|---:|
| Businesses, owners, workers and sites |4/4|100%|
| Resources and connected production |27/33|82%|
| Business lifecycle and competition |14/15|93%|
| Specialization and final acceptance |0/5|No complete entries yet|

Equal weight per group gives about69% partial checklist completion. Entries differ
substantially in size; this is not engineering effort, remaining time or a claim
that an ordinary village can already start every industry. These counts come from
PHASE3_TODO.md. The four-group phase-gate bar below still counts whole groups.

First mining-owner training is now connected: real opportunities sponsor gradual
practice using actual supplies, then a qualified graduate can found a funded mine.
Productive public farms now drive funded warehouse toolkit restocking, with
real paid goods and delayed delivery. A 50-period connected mine/tool/farm
regression now checks finite depletion and money/goods conservation. This does
not finish multi-year specialization. First warehouse training/scheduling is now verified; wider
industries, general competition and specialization remain.
A new paid starter-material mechanism now protects village supplies, records real
payment and delayed cargo, and prevents duplicate purchases after restart. It is
verified in SQLite and Paper. First-workshop investment checks now also verify a
real funded customer, current marker and supplies without spending or inventing
skills. Automatic first-workshop training/startup and approved marker removal
pause/restoration are now connected and verified.
See WORKSHOP_STARTER_SUPPLIES.md and MINE_FOUNDER_TRAINING.md. Your playable server has not been changed.

Bars count completed milestones in the checklist below. Milestones differ in
size, so percentages are not estimates of hours, release dates or total project
completion. A phase is complete only after its canonical roadmap gate is passed.
Partial work does not fill a completed milestone. These are reporting groups,
not a replacement for ROADMAP.md or its design requirements.

| Phase | What you will experience | Milestones complete | Progress |
|---|---|---|---|
| 1. Foundation & Population | Villages remember their people; families and population change over time | 10 / 10 | `██████████` 100% — prototype gate passed |
| 2. Needs & Economy | Villages track shortages, learn from food deliveries and offer affordable responses | 4 / 4 | `██████████` 100% — prototype gate passed |
| 3. Businesses & Industry | Farms, mines and workshops create jobs and shape each town | 1 / 4 | `██▌░░░░░░░` 25% |
| 4. Physical Growth | Villages build and improve structures gradually | 0 / 4 | `░░░░░░░░░░` 0% |
| 5. Trade & Roads | Settlements trade, connect by roads and depend on each other | 0 / 4 | `░░░░░░░░░░` 0% |
| 6. Identity & History | Leaders, reputation and relationships create memorable local histories | 0 / 4 | `░░░░░░░░░░` 0% |
| 7. Growth & Decline | Towns thrive, struggle, leave ruins and sometimes recover | 0 / 4 | `░░░░░░░░░░` 0% |
| 8. Advanced Civilization | Territory, player settlements and rare crises enrich the world | 0 / 4 | `░░░░░░░░░░` 0% |
| 9. Polish & Release | Many settlements run smoothly in a balanced, tested survival world | 0 / 4 | `░░░░░░░░░░` 0% |

## Phase 1 — prototype gate passed

- [x] Reliable plugin build and automated testing.
- [x] Settlement saving and safe database upgrade machinery.
- [x] Repeatable simulation test runner, separate from Minecraft.
- [x] Validate automatic online builds, disposable server setup and backup recovery. Hosted Windows/Linux CI passed on approved snapshot e85416e; later local work passed Wrapper and real Paper checks. Latest feature branch remains unpushed.
- [x] Save named individual residents across restarts. Headless storage and actual Paper projection/restart verified.
- [x] Households, basic family ties and the foundation for jobs. Saved households/relationships/skills and explainable age/skill qualification checks verified. Actual businesses/hiring belong to later phases.
- [x] Aging, births and deaths. Saved headless rules, visible babies/adulthood, native deaths and simulated death removal verified on Paper.
- [x] Immigration and emigration. Saved external arrivals and whole-household moves verified; five migrants materialized near their destination with permanent person IDs.
- [x] Civilization calendar and saved simulation state. Owner-thread timer,
  bounded earned-work replay and diagnostics verified. October 9 approved policy:
  server downtime/absent worlds pause; loaded-world unloaded chunks still simulate.
- [x] Connect simulated people to visible villagers and in-game diagnostics; validate the population experience on Paper. Safe nearby spawning, names/ages, pending/activation restart recovery, native breeding coordination, approved zombie-conversion prevention, migration and death reconciliation verified with real Paper entities.

First playable prototype reached: named residents persist, families/population change,
and nearby villagers reflect saved births, ages, deaths and moves. The real Paper
probe uses synthetic observation positions; a human Minecraft client playtest is still
needed. This is the Phase 1 foundation gate, not a balanced survival release. See
PHASE1_PLAYTEST.md for setup and material limitations, including trade resets on moves.

## Phase 2 — prototype gate passed

- [x] Commodity supplies, demand, reserves and shortages. Saved accounting/history,
  actual field-backed food, shortage duration/recovery and forward famine/feast tuning.
- [x] Basic labor, treasury and household welfare. Adult/field capacity, finite funds,
  real spending/refunds, family food/housing welfare separate from money.
- [x] Feasible autonomous decisions with explanations. Seeded priorities, bounded
  learning from verified earlier deliveries, saved donation/emerald/timber/stone/wait
  comparison, blocked reasons and player-visible preferences. Confirmed exchanges
  retain actual execution receipts and exact future import outcomes.
- [x] Player trade/prices and long-running stability tests. Donations, finite-budget
  emerald sales, bargaining/cooldowns, finite-stock barter, multiple 240-period
  conservation/learning/publication/restart scenarios and native Paper exchanges.

**All four prototype groups are verified.** Final Windows Wrapper clean verify
passed 494 cases (492 unit/two packaged integration), zero failures/errors/skips.
Pinned disposable Paper passed publication, learning, native donations, bargaining,
food-for-emerald/log exchange, failed-save/duplicate/restart protection, welfare
recovery and unloaded operation. Final normal startup/diagnostics/shutdown and
read-only migration/integrity/history checks passed. See PHASE2_ACCEPTANCE.md.

Players can see what a village prefers and choose another available option.
Every exchange rechecks current stock, funds, food need and consent. Pending food
reduces additional need. No supplies, income or successful supplier are invented.
Admin settings remain editable in economy.yml; old owner files remain unchanged
and explicitly opt in to new sections. See FOOD_RESPONSES.md and PRIORITY_LEARNING.md.

This completes the food/housing economy foundation, not a balanced survival release.
Human client UX, inventory-plugin/native-death compatibility, cross-save backup
restoration, long-server profiling and balance remain release checks. No starvation
mortality, actual business employment, recurring nonfood industry, physical housing
construction, regional trade or reputation is claimed here. Those remain in the
canonical later phases or require separate consequential tuning.

Phase 3 — persistent businesses and industry — is underway at 1/4 (25%). Phases 4–9 remain 0%.
Percentages count verified reporting groups, not hours or overall plugin completion.

Earlier batch notes below describe progress at that time; the current Phase 2 gate
above supersedes their partial percentages.

Verified paid-food batch: protected Sell confirmation, configurable prices/need target,
reserved funds, saved item exchange, refund/retry and restart passed 401 automated
cases and disposable Paper. Native fixture exchanged 8 wheat for 3 emeralds once.
This advances part of player trade; it does not complete the barter/bargaining,
feasible-response and wider balance gates. The subsequent welfare batch passed
419 automated cases, including a 240-period finite-budget exchange/restart scenario,
and real Paper famine/recovery. See HOUSEHOLD_WELLBEING.md.

The bargaining batch adds `/civ bargain <price>` and explicit acceptance: current
value/budget counters, editable patience/deadlines and restart-safe cooldowns.
Wrapper passed 435 automated cases; disposable Paper verified a saved counter
surviving restart and one native 8-wheat/3-emerald exchange. Barter and the other
unfinished gates keep the phase at 2/4 (50%). See FOOD_BARGAINING.md.

The accounting batch established real timber/stone reservations, protected
stock floors and once-only cancellation/completion, including a 240-period finite-
stock famine/restart scenario. The 450-case Wrapper build and existing Paper economy
regressions passed. See BARTER_ACCOUNTING.md for stock semantics.

The subsequent player barter batch implements `/civ barter timber|stone` and
explicit acceptance, with saved inventory proof, stock reservation/refund and
restart recovery. Actual Paper exchanged 8 wheat for 6 oak logs once without
spending money. A mixed two-village 240-period scenario accounts for every food,
currency and material unit across donations, paid sales, barter and restart. The
final Wrapper build passed 460 cases, with zero failures/errors/skips. The
trade prototype group is complete, advancing Phase 2 to 75%; broader autonomous
responses/outcome learning still block 100%. Human survival/client/plugin testing
remains required for release. See PLAYER_BARTER.md.

## Phase 3 — automatic farm businesses: 1/4 prototype groups verified

- [x] Persistent businesses, owners, workers and sites. Automatic field enterprise verified on actual Paper; other industries remain next.
- [ ] Resources and connected production chains.
- [ ] Business founding, competition, closure and succession.
- [ ] Emergent specialization and tested business turnover.

First milestone now has an automatic farm prototype: complete crop surveys locate
a real site, a qualified available resident starts a farm with actual village funds,
and assigned workers produce field-bounded food. Saved ownership/jobs, once-only
startup spending, restart and replacement staffing are verified. No worker or food
is counted twice. Admin diagnostics explain blockers and production. Wrapper passed
553 tests; the real Paper fixture verified 32 crops, two workers, eight food units,
one startup charge and absent-world pause. Existing owner configs stay unchanged and
must opt in. No buildings appear yet. See AUTOMATIC_FARMS.md.

The remaining groups still require connected input/output industries, business
competition/closure/succession and emergent specialization/turnover. This 25% counts
one verified reporting group, not a quarter of total code or a full Phase 3 gate.

Resource/production group now also has verified partial work: finite mining with
shared rock byproducts, bounded business storage and actual local warehouse buying/
resale, reserved cargo and recorded causal-use APIs. Editable commented defaults
are supplied. Final Wrapper passed 522 tests; disposable Paper schema-28 startup/
shutdown and preserved history/config passed. Automatic mine recognition, business
operation, NPC shopping and production chains remain unwired, so the group stays
unchecked. See FINITE_MINING_AND_WAREHOUSING.md. This is concrete backend progress,
not a finished player-visible industry system.

Connected workshop processing now adds editable recipes, transactional input/output
history and actual worker/storage limits. A paid 240-cycle mine/warehouse/blacksmith
fixture and native packaged Paper fixture passed; Wrapper total is 571. Ordinary
villages do not automatically establish mines or blacksmiths yet; live demand,
discovery/founding and operation remain required before checking this group.

Coordinate discovery now protects finite minerals across overlapping surveys, site
conflicts, world renames and restarts, with transactional evidence and no refill.
Wrapper passed 586 cases and the native synthetic coordinate fixture passed. Actual
ore scans and automatic nonfood businesses are still unfinished; progress remains
1/4 (25%) while that next reporting group is partly implemented.

## Phase 4 — planned

- [ ] Building templates and authoring foundation.
- [ ] Terrain-aware layouts and road access.
- [ ] Player-build protection and safe staged construction.
- [ ] Upgrades, reuse, decay and disposable-world safety tests.

## Phase 5 — planned

- [ ] Inter-settlement imports/exports and economic dependency.
- [ ] Persistent roads, bridges and routes.
- [ ] Nearby merchants and abstract long-distance trade/migration.
- [ ] Route maintenance/disruption and unloaded-region tests.

## Phase 6 — planned

- [ ] Governments, leaders and succession.
- [ ] Settlement relations and limited diplomacy.
- [ ] Earned player reputation and causal structured histories.
- [ ] Notable people/infrastructure and identity-preserving rename/merger tests.

## Phase 7 — planned

- [ ] Prosperity and decline emerge from interacting conditions.
- [ ] Maintenance problems and gradual deterioration.
- [ ] Abandonment, ruins, preservation and recovery.
- [ ] Repeatable long histories and recovery scenarios.

## Phase 8 — planned

- [ ] Organic territory, outposts and discovery.
- [ ] Cultural/historic structures and rare contextual crises.
- [ ] Abstract conflict with appropriate nearby evidence.
- [ ] Player settlements and earned influence.

## Phase 9 — planned

- [ ] Performance at 20+ settlements, including rare large cities.
- [ ] Compatibility, migrations, recovery and administration.
- [ ] Templates/content and balanced simulation.
- [ ] Documentation and long survival playtesting; all release gates verified.

For detailed requirements see ROADMAP.md. For exact tests, limitations and code
status see PROJECT_STATUS.md. Future batches update this checklist from verified
implementation; planning or a synthetic test alone does not complete gameplay.

<details>
<summary>Earlier verified development updates (historical)</summary>

These snapshots describe earlier builds. Their "next" tasks and test totals are
historical; use the current summary and checklists above for today's status.

Earlier update: farm equipment storage no longer prevents warehouses buying real mined
supplies. Unsupported stores still stay protected. All 952 tests and the final
isolated Paper check passed. Your server is unchanged. Phase 3 remains 25% by
completed groups; automatic first industries and specialization are next.

Earlier update: an actual supplier can now help cover a farm tool bill from earned spare
cash, keeping its operating reserve and the village's food money protected.
Nothing is minted or collected when funds suffice. All 951 tests and isolated
Paper passed. Your server is unchanged. Automatic first industries/specialization
remain; Phase 3 stays 25% by complete milestone groups. Next: fix warehouse
restocking around farms' tools-only stores, then connect genuine upstream demand.

Earlier update: working farms now wear out tools and buy real replacements from local
suppliers using village money that protects food funds. Tools last through actual
work, not downtime; farms without tools work more slowly. All 940 tests and the
final rebuilt-JAR Paper check passed. Your server is unchanged. Automatic first mines/
workshops and specialization remain; Phase 3 is still 25% by complete groups.

Earlier update: the payment safeguards for village-funded farm tools are complete.
They protect food money, pay a real supplier once and prevent duplicate deliveries.
All 931 tests passed. Automatic farm wear/orders are next; food ownership and your
server are unchanged. Phase 3 remains 25% by completed groups, with automatic first
industries and specialization still unfinished.

Earlier update: qualified villagers can now fill vacancies at existing mines, workshops
and warehouses, without abandoning another job. Worker replacement, restart and
world pause passed the isolated Paper check; all 917 automated tests passed.
Your server is unchanged. Phase 3 remains 25% by completed groups; automatic
first industries and specialization remain. Next: approved village-funded farm
tools with food money protected.

Earlier update: warehouses can receive limited startup funds from the same village budget
as mines and workshops. Food money stays protected, and retries cannot pay twice.
All 908 tests and the isolated Paper funding check passed. Your server is unchanged.
Automatic first industries and specialization remain; Phase 3 is 25% by complete
groups. Next: qualified residents filling vacancies at existing businesses.

Earlier update: mines and workshops buy replacement tools using their own money.
Tools wear through actual work; missing tools slow work, giving blacksmiths real
customers. Rates and startup grace are editable. All 902 tests and both isolated
Paper checks passed. Your server is unchanged. Phase 3 remains 25% by complete
groups; automatic first industries and specialization remain. Next: approved
warehouse startup funding under the shared protected village grant budget.

Earlier update: existing warehouses can now stock themselves by paying real local mines.
Blacksmiths buy the delivered materials and make tools; money and goods cannot
duplicate through restart. All 886 tests and the isolated Paper chain check passed.
Your server is unchanged. Automatic new industry and genuine finished-goods needs
remain before the connected-production group is complete. Phase 3 remains 25% by
complete groups. You approved business tool upkeep; that is the next scoped batch.

Earlier update: the mine planner can now check what existing workshops actually need and
can afford. Goods already owned, deliveries on the way and shared warehouse stock
reduce that need; nearby coal is ignored if only iron is missing. This is a checked
planning step, not automatic mine opening yet. All 872 tests and isolated Paper
paid-goods/restart checks passed. Your server is unchanged. Phase 3 remains 25% by
complete groups; real customer needs, trained founders and automatic industry are next.

Earlier update: villages now distinguish ore they currently see from ore they saw earlier.
A new empty scan clears startup candidates without deleting history; old scans and
restart cannot bring those candidates back. Expired evidence, absent worlds and
removed/claimed ore wait for safe revalidation. All 857 tests and the isolated
Paper restart check passed. Your server is unchanged. Phase 3 remains 25% by complete
groups; actual demand, career choice and automatically funded industries are next.

Earlier update: mines/workshops now have checked startup funding that protects food money,
respects editable village limits and cannot pay twice after restart. The funding
backend is ready; automatic business selection still needs connecting. All 844
tests and the isolated Paper funding check passed. Your server is unchanged.
Phase 3 remains 25% by complete groups. Next: qualified residents, suitable resource
opportunities and the first automatically funded businesses with real demand.

Earlier update: villages now remember observed coal/iron locations after restart. Revisits
do not duplicate them, and placed/removed ore stays excluded. A shutdown save race
was found and fixed. All 828 tests and the isolated live scanner check passed.
Your server is unchanged. You approved limited startup grants that protect food
money; those grants and automatic mine/workshop founding are next. Phase 3 remains
25% by complete reporting groups; connected production and specialization remain.

Earlier update: supporting ore now responds to successful block breaks, explosions and
piston movement. Only the removed block's remaining share is lost; existing goods
stay safe. If saving fails, mining pauses instead of silently using stale sources.
All 819 tests and isolated Paper event checks passed; your server is unchanged.
Older saved veins need a reviewed baseline under the stronger tracking contract.
Phase 3 remains 25% by complete groups. Automatic first mines/workshops and connected
production chains are next.

Earlier update: finite ore now has a saved balance for each supporting block. Removing
one through the accounting API loses only its remaining share; goods already mined
stay safe. All 806 tests and the isolated packaged Paper check passed. Connecting
real player block breaks is next; your server is unchanged. Phase 3 remains 25%
because the complete industry-chain and specialization groups are unfinished.

Earlier update: farmers now gain experience from actual productive work, keeping it through
restart. Fewer fields mean fewer learning slots; idle/closed farms award nothing.
All 792 tests and the isolated Paper session check passed. Your server is unchanged.
Phase 3 still has 1/4 complete groups (25%): automatic career choice, first mines/
workshops and wider competition remain. This advances the unfinished skills gate.

Earlier update: businesses now find qualified buyers automatically after inheritance fails.
A villager may own unlimited firms, but purchases need real money and each resident
still holds one worker job. Prices/deadlines are editable and survive restart.
All 783 tests and the isolated Paper check passed; your server is unchanged.
Phase 3 remains 1/4 groups (25%): broader skill/investment/competition and automatic
first mines/workshops remain before another complete group can be counted.

Earlier update: unemployed adults now have saved, supply-funded training courses. Hungry
villages wait; practice uses real supplies, and skills survive restart without
repeating costs. All770 tests and the isolated Paper check passed. Career choice
and automatic first mines/workshops remain next, so Phase 3 still has one complete
reporting group out of four (25%). These changes are not installed on your server.

Earlier update: employees can gradually learn through successful mining/workshop production.
They keep their skills after restart; idle or closed businesses award nothing.
All 756 tests and the isolated Paper check passed. Unemployed-adult training and
automatic first mines/workshops are next. Phase 3 remains 25% because the broader
milestone groups are unfinished. Your server still has its earlier playtest build.

Earlier update: an unsold business now closes after its saved deadline. Workers can look
for other jobs; the business keeps its goods, money, remaining ore and history.
Restarting cannot bring it back accidentally. All 744 tests and the real Paper
check passed. Automatic prices/buyers and wider industry founding still remain,
so Phase 3 is still one fully finished reporting group out of four (25%).
You approved gradual skill learning; that is the next development work, not yet live.

Earlier update: a business sale offer now remembers its price and deadline. Time away from
the server or world does not use up that deadline; a sale saves the payment and
new owner together. All 734 tests and the real Paper check passed. Choosing buyers
automatically and closing an unsold business are still next. Phase 3 remains 25%
because the full business lifecycle group is unfinished.

Earlier update: your approved rule now lets qualified existing staff keep a business working
after its owner dies, while ownership is resolved. Admins can choose to pause it
instead. Actual workers, supplies, storage and finite resources still limit output.
All 725 tests and a real Paper check passed. Automatic listings/deadlines/closure
remain next; Phase 3 still has one complete milestone group out of four (25%).

Earlier update: qualified buyers can pay for a business with real business cash and family
estate money. Payments and ownership save together and cannot repeat after restart.
Admins can edit spending limits. All 715 tests and a real Paper check passed.
Automatic sale listings, paused deadlines and closure are next; Phase 3 stays 25%
because the whole lifecycle/competition group is not finished.

Earlier update: a deceased business owner's qualified family member can inherit; otherwise
a qualified employee can take over. Children/unqualified relatives cannot bypass
requirements, and workers do not silently change industries. The business keeps its
founder, money, supplies and history. Automated tests and a real Paper mine/restart
check passed. Business sales, estate funds, closure and competition are next; the
lifecycle group is still incomplete, so Phase 3 remains 25%.

Human playtest passed: villages register residents, bread donations remove the right
items, food sales pay the agreed emeralds, and villagers remain saved after restart.
The reported slow trade confirmation now has a tested shorter polling interval;
the updated JAR is installed, with actual human response-time recheck pending. Normal food
speed has been restored after the accelerated test. Inheritance, resale, vacant
building reuse and individual ore-source loss now have approved rules; implementation
is next. These approvals do not complete another Phase 3 milestone.

Latest foundation safeguard: villages pause while their world is absent or the
server is stopped. They keep their people, supplies and history. Worlds with
unloaded chunks still simulate. Latest Wrapper verification passed 819 tests.
See ONLINE_WORLD_PAUSE.md for the pause policy.

Latest safeguard: changing industry speed no longer makes yesterday's shipments
use up today's transport allowance. Paid deliveries keep their goods, money and
original timing. Automated checks and a copied old database upgrade passed on
Paper. The updated local player test build is installed; Phase 3 still has one
complete reporting group, with connected industry work partly implemented.

Villages can now look for exposed coal/iron in nearby already-loaded chunks,
instead of just the home chunk. Bounded observation preserves capture time and
keeps the last result readable. Actual Paper checks passed. This does not start
mines yet; per-block resource loss is now approved, while funded opportunity
selection and safe source-retirement implementation remain unfinished.
The current player build is ready for farms, families and food trade testing.

Latest industry step: an existing workshop can pay a local warehouse for actual
materials, wait for delivery and make tool supplies. It cannot spend money twice
after a restart or use goods still in transit. The real Paper application check
passed. Automatic creation of these nonfood businesses and upstream supply are
still unfinished, so Phase 3 remains one of four complete groups (25%). Settings
are editable; old files opt in. See WORKSHOP_SUPPLIES.md.

Latest Phase 3 step: villages can automatically start a funded farm, hire real
residents and produce food from their observed fields. Owners, workers and sites
persist across restarts. The real Paper fixture verified founding, output, once-only
spending and world pause. Old owner settings explicitly opt in. See AUTOMATIC_FARMS.md.

Latest connected-industry work: a blacksmith recipe can turn actual delivered iron
and coal into tool supplies, bounded by workers and storage. Paid mine/warehouse/
workshop chains, restart and material/money conservation are tested. Automatic
nonfood discovery/operation is still unfinished, so this is partial progress toward
the next milestone, not another completed group. See WORKSHOP_PRODUCTION.md.

Latest mining safeguard: permanent ore coordinates stop alternate sites and repeat
surveys from creating another reserve pool from the same resource. Depletion and
identity survive restart and tuning. You approved using exposed existing ore; the
scanner now detects exposed coal/iron with bounded work and supplied exclusions.
Placement exclusions now save across restarts and reject stale discovery evidence;
real Paper snapshot/exclusion checks passed with all fixture blocks restored.
Native player placements now save automatically, with crash/failure discovery
pauses and a verified clean restart. Piston movement now retains conservative
exclusions without cancelling movement or loading chunks. Bulk-editor coverage and connecting real
observation/founding/operation are next. Automatic mining is still unfinished.
See MINERAL_IDENTITY.md, LIVE_ORE_TRACKING.md and PHASE3_TODO.md. This advances a partial milestone;
Phase 3 stays 25% until the complete gameplay group is verified.

Observed ore can now be separated into connected patches instead of treating
every ore block in a chunk as one deposit. Separate patches retain their finite
reserves through mining and restart tests. This is another component toward live
resource opportunities; it does not automatically start mines. See RESOURCE_PATCHES.md.

Live read-only ore surveys now run in bounded steps around saved village homes,
checking placed/moved ore exclusions and old deposits. The actual scheduled Paper
pipeline passed with fixture blocks restored. Existing owner files explicitly opt
in through economy.yml. Automatic nonfood funding/operation remains unfinished.
See LIVE_RESOURCE_SURVEYS.md; this still advances part of the second reporting group.

Mine startup now saves the business, finite reserves, storage, funding and first
worker together. Failed startup leaves no partial business or missing money;
retries preserve depleted ore and existing stock. Automated and packaged Paper
checks passed. Automatic mine selection and production are still unfinished.
See ATOMIC_MINE_STARTUP.md.

Unsupported recognized world editors now pause new ore discovery instead of
allowing bulk-placed ore to create reserves. Admins can add other editor names in
economy.yml. Existing villages and saved deposits remain; restart retains warnings.
The actual Paper enable event and recovery checks passed. This does not certify
unknown editors or enable automatic mines. See ORE_EDITOR_GUARD.md.

Existing funded mines and workshops now work over time using their real workers,
finite resources, delivered ingredients and available storage. Saved clocks pause
for server/world absence; failed work can retry without duplicating goods. Automated
and Paper session checks passed. Automatic founding and local customers remain
unfinished, so this is still partial progress toward the second group.
See TIMED_INDUSTRY.md and TONIGHT_PLAYTEST.md.

</details>
