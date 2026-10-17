# Civilizations progress tracker

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

### Livestock connections inside the unfinished Phase 3 milestone

These are details, not extra credit in the four-group percentage.

- [x] Remember animal owners and protect player animals.
- [x] Let admins confirm protected pens and release land without losing animal history.
- [x] Sell recorded milk/eggs to players; milk requires their own bucket.
- [x] Save births, show safe nearby offspring and reconcile their ages.
- [x] Record actual deaths and confirmed departures without guessing from unloaded chunks.
- [x] Buy real grain/feed, wait for delivery and run paid timed care.
- [x] Automatically open qualified, funded businesses from eligible natural animals.
- [x] Let actual caretakers gain skill and qualify for succession; value producer stock for resale.
- [ ] Finish controlled slaughter without duplicate drops.
- [x] Connect bakery recipes that consume milk and eggs.
- [ ] Add authorized bulk pen donation/ownership transfer.

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

## October17 — existing farm animals now use the saved growth clock

Animals initially assigned to a farm now grow at their saved dates, just like
newborns. Late observations cannot turn an adult back into a baby. Names and
ordinary movement/protection settings stay intact; unknown animals stay unknown.

All1,776 full-build tests and the isolated real initial-animal/growth/restart/
serialization check passed. No save migration or human-server change. The prior
newborn build also passed GitHub on Windows/Linux. P3 stays about68% partial.
Next: bulk pen protection before automatic adoption. The slaughter-product
question is pending, and independent work continues.

## October17 — saved animal births now appear safely in Minecraft

Recorded newborns can now appear on safe loaded ground near players. They keep
permanent identities, grow at their saved dates and survive restarts without
copies. An animal missing from view is never automatically replaced. One real
death removes only that animal; the farm's goods and money remain accounted for.

All1,763 full-build tests and the isolated real-cow birth/growth/restart/missing/
death check passed. Earlier radius work also passed GitHub on Windows/Linux.
Your server is untouched. P3 remains about68% partial: initial animal aging,
automatic livestock businesses/feed/output, wider products and specialization
still remain. Next: initial herd aging. The slaughter-product question is pending;
other work continues while awaiting that answer.

## October17 — animals can pause work without disappearing

A cow or chicken confirmed far from its farm can stop working until it returns,
without losing its ownership or place in the herd. Growing animals still age;
other animals keep working. Unloaded animals keep their last known status.
Admins can edit the working radius in economy.yml; the prototype is32 blocks.

All1,739 full-build tests and the isolated real-cow away/return/restart check
passed. Existing test-save records were preserved; your server is untouched.
Earlier newborn checkpoints also passed GitHub's Windows/Linux builds.
P3 remains about68% partial: automatic adoption, safe newborn placement, scheduled
feed/output and broader bakery ingredients remain. Next: safe newborn placement.
No answer needed.

## October16 — physical newborn identity handoff verified

Newborn records can now connect safely to actual Minecraft animals. A retry or
restart cannot create a second body, and one confirmed death removes only that
animal while preserving its siblings and the farm's money/goods. Missing or
unloaded animals never authorize replacement.

All1,718 full-build tests and the isolated real-animal/death/restart check passed.
The additive test-save upgrade retained prior data; your server is untouched.
Automatic herd adoption and production remain unfinished. P3 stays about68%
partial. Next: the approved editable working radius and safe live integration;
no answer needed.

## October 16 — newborn identities are saved safely

Simulated newborn animals now get permanent identities and saved growing-up dates.
Restarting or changing settings cannot reroll those records. Older anonymous
records stay intact and are flagged for reconciliation. This prepares physical
newborns; it does not spawn them yet.

All 1,694 full-build tests and the isolated Paper save/restart regression passed.
The test-save upgrade retained existing records; your server is untouched. The
previous player-market commit also passed GitHub checks on Windows and Linux.
P3 remains about 68% partial. Next: connecting identities to physical animals and
recording their individual deaths, before automatic herds/feed/output. No answer
needed.

## October16 — milk/egg player market verified

Players can now open /civ market to buy recorded milk/eggs from an eligible stocked
business. Milk needs your own empty bucket. Purchases survive interrupted saves
without a second charge or delivery. Only goods from an unfinished purchase get
temporary death protection; older items follow ordinary server rules.

All1,681 full-build tests and the isolated Paper save/menu/retention check passed.
The native test used a synthetic player and death/respawn event model, not a human
client or actual crash. Your server is untouched. P3 stays~68% partial: live herd
adoption, newborn tracking, feed/output and bakery customers remain. Next: newborn
identity and safe herd activation; no answer needed.

## October16 — market prices and inventory foundations verified

Nearby real dairy/chicken stock can now be quoted with editable prices and exact
payment terms. Inventory planning safely handles emeralds, your empty bucket,
custom items and room for legal item stacks. All1,652 tests and the isolated Paper
inventory check passed. The player purchase controller/menu is next; P3 stays~68%
partial. You approved protecting only unfinished purchase goods through death.

## October16 — checking saved milk/egg trade receipts

The plugin can now verify the new trade receipt in a saved player file while
rejecting forged, duplicated or incomplete records. All1,630 tests passed,
including20 new checks across this batch. Player menu/exchange remain unfinished.
P3 stays~68% partial; no new design answer is needed.

## October16 — product trade accounting verified

Milk/egg purchases now have saved accounting for holding actual goods, confirming
payment once, or safely returning unsold goods. Milk receipts require the buyer's
own bucket. The player trade menu is not connected yet. All1,627 tests passed;
P3 stays~68% partial and the livestock gate remains unfinished.

CI note: the previous recovery revision81be4a0 passed both Windows and Linux
hosted verification. The new product-sale batch passed local checks; hosted verification pending.
Phase progress and the unfinished livestock gate below are unchanged.

## October16 — livestock safety foundation verified

Milk/eggs are reserved for recorded village trade, player-created livestock keeps
protective origin evidence, confirmed deaths reduce herds once, and interrupted
capture history now has a saved recovery warning. A shutdown file-lock race is fixed.

All1,610 build tests and the relevant isolated Paper checks passed. Your server is
untouched; test saves preserved. P3 remains~68% partial (`██████▊░░░`),30/39 resources,
1/4 whole groups. These livestock substeps show the work inside the unfinished gate:

- [x] Protected funding and atomic saved herd startup.
- [x] Player-animal protection and native origin evidence.
- [x] Confirmed original-animal deaths and once-only accounting.
- [x] Durable capture eligibility, pending-loss checks and shutdown recovery warning.
- [x] Saved milk/egg sale ledger and bucket receipt terms (menu unfinished).
- [x] Configurable nearby market quotes, owner handoff and verified inventory exchange plans (purchase controller/menu next).
- [ ] Automatic safe animal adoption and business opening.
- [x] Permanent logical newborn identities and saved maturity dates.
- [ ] Physical newborn claims, individual losses and herd reconciliation.
- [ ] Scheduled feed purchases and production.
- [x] Recoverable stock-backed milk/egg player trading and selective pending-goods retention.
- [ ] Connected bakery ingredient customers and live herd supply.

Next: connect live herds safely and their real customers. Milk buyers must bring
an empty bucket; that decision is now approved. Milk/egg player trading remains
unfinished. See LIVESTOCK_PROTECTION.md.

## October16 — player-created livestock keeps its origin evidence

Chickens hatched from eggs and livestock created through breeding, spawn eggs,
dispensers or commands now carry protective evidence for future village adoption.
Normal player animal behavior stays intact. Missing information remains unknown.

All1,599 build tests and the real Paper origin/protection checks passed. Test
animals/items removed; your server untouched. P3 stays~68% partial (`██████▊░░░`),
30/39 resources,1/4 whole groups. Next: interrupted-save recovery, then physical
herd reconciliation and live business/feed/output connections. No decision needed.

## October16 — killed livestock no longer stays in a saved herd

Confirmed deaths of original village herd animals now reduce the saved count and
later production exactly once. Young animals that die do not grow up later in the
simulation. Player-owned animals keep normal deaths and drops.

All1,595 build tests and a real Paper death/survivor/production/restart check passed.
The test-save upgrade preserved existing records; your server is untouched.
P3 stays~68% partial (`██████▊░░░`),30/39 resources,1/4 whole groups. Live herd
adoption/output still needs crash recovery and physical offspring reconciliation.
Next: protect egg-hatched/spawn-created animals too. No decision needed.

## October16 — village products and player-bred animals are protected

Village-owned cows/chickens now reserve milk and eggs for the recorded economy,
while player-owned animals behave normally. Player-bred offspring also keep a
saved protection flag so future village adoption can recognize them.

All1,584 full-build tests and the real Paper egg/protection/event checks passed.
Test animals/items cleaned up; your server is untouched. The product-trading UI
and automatic herds are still unfinished. P3 stays~68% partial (`██████▊░░░`),
30/39 resources,1/4 whole groups. Next: real animal losses and herd reconciliation.
No decision needed. See LIVESTOCK_PROTECTION.md for verified limits.

## October16 — livestock startup money and safe animal observation

Chicken/dairy businesses can now use the same protected startup budget as other
industries. Opening saves the business and confirmed herd together; failures or
restarts cannot duplicate money, feed or animals. The plugin can also safely read
real loaded cows/chickens without taking ownership or changing them.

All1,572 full-build tests and the isolated Paper cow/chicken/payment/feed/output/
restart/pause check passed. Temporary test animals were removed; your server is
untouched. P3 remains~68% partial (`██████▊░░░`),30/39 resources,1/4 whole groups:
automatic adoption/live livestock, broader products and specialization remain.
Player collection of village milk/eggs needs a decision before live production.
See LIVESTOCK_PROTECTION.md. Prior craftsman hosted Windows/Linux checks passed.

## October16 — villages can develop their first craftsman naturally

An eligible adult now learns gradually with real practice materials, opens a
workshop when qualified, makes storage fittings and sells them to businesses
that actually need space. Restarting preserves progress. Missing supplies or
customers pause training; waiting offline never grants free experience.

All 1,558 full-build tests and the disposable Paper training/restart/opening/
production/payment/pause check passed. Save history preserved, test blocks restored.
Your server is untouched. Existing admin settings remain editable and unchanged.
P3 stays ~68% partial (`██████▊░░░`),30/39 resource entries,1/4 whole groups;
wider products, live livestock and specialization remain. Next decision: may
chicken/dairy businesses share the existing protected startup grants? The same
common limits would apply, with no extra money. See PROCESSING_WORKSHOPS.md.

## October16 — qualified craftsmen can open and sell useful fittings

A skilled adult can now open a craft workshop at a suitable crafting table when
real customers, startup funds and materials exist. The workshop makes storage
fittings; crowded businesses pay for them and gain capacity after delivery.
No free materials or money. Villages still need gradual training to develop an
unskilled first owner; that is next. Your server has not been changed.

All 1,549 full-build tests and the disposable Paper opening/production/payment/
restart-style replay/pause check passed. Existing test-save records were retained,
temporary blocks restored and the fixture stopped. No merge or push to main.
P3 remains ~68% partial (`██████▊░░░`),30/39 resource entries,1/4 whole groups:
training, wider craft products, live livestock and specialization remain. No
question needed. See PROCESSING_WORKSHOPS.md for exact settings/limits.

## October16 — businesses can pay the craftsman for useful storage fittings

The first craft product now has real customers. Crowded businesses buy fittings
with their own money, wait for delivery, then gain limited storage. Materials and
cash are conserved; restarting cannot charge or expand twice. Admins can edit
prices/limits. Existing server files stay intact; your server was not changed.

All1,533 full-build tests and the disposable Paper payment/delivery/restart/pause
check passed. The test-save upgrade preserved existing records and was audited.
P3 stays~68% partial (`██████▊░░░`),30/39 resource entries and1/4 whole groups.
Automatic craftsman training/opening is next; this batch does not finish that
whole checklist item. Live livestock, wider products and specialization remain.
Storage approval is resolved; no decision needed. See STORAGE_UPGRADES.md.

## October 15 — first bread bakeries can train and open automatically

Villages can now teach an eligible adult to bake, pay a real grain supplier and
open a bread bakery at an observed smoker when money, skills and customers make
it viable. Bakers can compete for existing paid food orders; nobody gets invented
money or guaranteed sales. Admin settings remain editable. Your server is unchanged.

**P3: about 68% partial checklist progress** (`██████▊░░░`), with one of four whole
groups finished. Paid farm food/ingredient customers now count as complete:
resources 30/39. Bread opening is connected; milk/egg products, the craftsman,
live livestock and specialization still prevent the broader gates from passing.

All 1,517 full-build tests passed, plus real Paper training/opening/production,
restart and pause checks. Test-save history was preserved and the fixture restored.
A decision is pending about allowing limited, paid business storage upgrades as
the craftsman's first customers. See PROJECT_STATUS and PROCESSING_WORKSHOPS.


## October15 — bakeries can pay real suppliers for their first grain

The startup backend can now buy the first bread ingredients from a farm or
warehouse, using the bakery's real startup money. The farmer gets paid and keeps
a reserve. Payments, delivery and opening save together; retrying cannot charge
twice or collect a second batch through another route. Existing grain stays owned
by its current owner.

All1,503 full-build tests and the packaged Paper check passed. The schema78 upgrade
preserved all133 old test-save tables; only three ordinary bookkeeping tables
changed, with no aging. Human server unchanged; no merge/push to main.

P3 remains about67% partial. Automatic baker training/opening still needs connecting.
One decision is pending: should bakeries compete for food orders already going to
farmers, even while the village is well fed? Recommended: yes, with real purchases
and editable limits. The current shortage-only investment rule is unchanged.
Craftsmen, live livestock and specialization remain. See PROCESSING_WORKSHOPS.md.

## October15 — bakery owners, recipes and a working paid grain chain

Qualified bakery opening now checks the owner's real skills, customers, ingredients
and money together. Bread production settings are editable. Farms can sell actual
harvested grain through warehouses to existing bakeries; the village pays for the
food it needs. Fixed two supply gaps that previously blocked that chain, while
retaining old grain and existing server settings.

All1,496 full-build tests passed, including14 new cases and an80-period grain/money
conservation scenario. The packaged plugin passed its actual database-thread check
on disposable Paper. All133 existing test-save tables retained; only three normal
bookkeeping tables changed, with no resident aging. Your server remains unchanged.

P3 stays about67% partial,1/4 whole groups. Automatic opening still needs private
starter purchases and gradual first-owner training connected to the live loop;
these are the next steps. Craftsmen, live livestock and specialization remain.
No decision needed. See PROCESSING_WORKSHOPS.md.

## October13 — safe funding and bakery opening checks

Bakeries and the general craftsman may now use the same protected startup budget
as other industries. The budget did not grow; all businesses share its limits.
Existing server settings stay untouched. Bakery planning now checks whether the
village can afford both opening the business and buying its food, whether real
ingredients exist, and whether existing suppliers can already meet the need.
Failed openings leave money, goods, workers and records unchanged.

Full .\mvnw.cmd clean verify passed1,482 tests with zero failures/errors/skips,
including20 additional cases. Your playable server and its database are unchanged;
no schema upgrade or new native-server test in this backend/config batch.
See PROCESSING_WORKSHOPS.md for what is ready and what is still unfinished.

P3 remains1/4 whole groups,29/39 resource entries/~67% partial. These are opening
prerequisites, not automatically operating bakeries/craftsmen yet. Next: qualified
founders, editable product defaults and live opening/ongoing supply integration.
Live livestock and specialization acceptance also remain. Funding approval is
resolved; no new question is pending. Feature branch: codex/phase3-processing-startup.

## October13 — bakery/craftsman locations recognized

Approved smokers now identify bakery candidates; crafting tables identify general
craftsman candidates. Recognition takes no container contents or building rights.
Admin-editable lists preserve installed settings. Extra markers no longer crowd
out first smith/warehouse reviews. Saved workplace identities/history survive the
schema77 upgrade, including tested failure recovery and extension-data protection.

Eleven new cases; focused47/targeted13 and full .\mvnw.cmd clean verify passed:
1,462 tests, zero failures/errors/skips. Native four-marker/container/restoration
check passed. All133 old test-save tables retained;128 exact, with normal bookkeeping
and specifically validated observation timestamps only. Human server untouched.

P3 remains29/39 resource items,1/4 whole groups/about67% partial. This completes a
recognition prerequisite, not automatic bakery/craftsman businesses. Actual
funded founding/products/replacement and live livestock remain. Marker choice
resolved; startup-grant extension approval pending. Prior development through
ce98f79 backed up on GitHub; this passing batch uses its own feature branch.
Main unchanged.

## October13 — saved herd accounting, still a partial livestock milestone

Herds can now remember young animals and progress across restarts. The tested
backend uses real feed and qualified workers to produce owned milk/eggs, with
once-only updates and protected storage. Twelve added tests include240 production
periods, failed-save recovery and real paid feed/product deliveries; focused40
passed. Full .\mvnw.cmd clean verify passed1,451 tests with zero failures/errors/skips.
Guarded packaged Paper backend check and schema75-to76 audit passed;126/129 old
tables exact, three ordinary bookkeeping changes, all new main herd tables empty.
Test server stopped, properties restored/helper archived. No live animal proof is
claimed; human server untouched.

Automatic animal adoption, physical births/deaths, live herd work and protection
signs remain unfinished. This does not complete either livestock gate. P3 remains
29/39 resource checklist items,1/4 whole groups,about67% partial. No human server
changes or main merge/push. Bakery/craftsman marker decision still pending.

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


Farmers now contribute a capped share of future food and own the surplus. The
village can buy additional food, giving farms real income. Storage, ingredient
buffers and tools are accounted for, while old public supplies stay intact.
All1,378 full-build cases and live paid-food/pause/restart checks passed. A paid
grain-to-bread chain is verified in backend tests; automatic bakeries remain next.
Expanded P3 checklist:~67% (29/39 resource entries; still1/4 whole groups). The
old70% used a smaller scope. Existing livestock ownership needs your decision.
Your playable server is unchanged. See HYBRID_FARMS.md.

P3 now includes hybrid farm finances, livestock, bakery ingredients and a general
craftsman. These add six checklist entries: the earlier ~70% used the old scope.
The current expanded checklist is ~66% before the farm batch passes final checks.
Farm allocation/paid-customer work is under full/native verification; existing
livestock ownership is the pending decision. Your playable server is unchanged.

Farms can now save real future wheat harvests as grain for eventual bakeries,
while protecting village meals and using the same workers and tools. Existing
food stays intact. All1,360 full-build cases and the live timer/pause/restart
check passed; settings are editable and older files opt out. Bakery purchases
need your answer on ongoing paid grain sales. Phase3 remains1/4 whole groups/
~70% partial; forestry, automatic quarry/pit founding and specialization remain.
Your playable server is unchanged. See GRAIN_HARVESTS.md.

Villages can now remember which observed crops are wheat, separately from other
food crops. Old food supplies stay intact; missing crop information stays unknown.
All1,346 full-build cases and the real observer/restart check passed. This prepares
bakery ingredients; grain harvesting and bakeries are still being connected.
Phase3 remains1/4 whole groups/~70% partial. Your playable server is unchanged.
See GRAIN_EVIDENCE.md. No decision pending.

Nearby resource checks now read already-loaded neighboring chunks, so exposed
stone and sand at chunk borders can be checked accurately. Missing terrain stays
unknown. All1,339 full-build cases and the isolated Paper check passed; your
playable server is unchanged. This prepares automatic quarry discovery; it does
not open new quarries yet. Phase3 remains1/4 whole groups/~70% partial.
Bakery ingredients are approved; existing food will be preserved.

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

Updated October 12, 2026. Based on verified work, not planned features.

Water recognition now has a read-only scan that checks depth/cover in small batches
and keeps unknown areas separate. It does not yet start a fishery or change your
world. All1,229 full-build tests and the real Paper snapshot check passed. You
approved suitable player-built ponds as well as natural water. Automatic fisheries
remain next. The temporary approval-review limit is resolved: disposable-server
cleanup finished after you resumed. Your playable server is unchanged.
Phase3 milestone counts remain unchanged.

You approved the water-loss rule. The saved-resource update now reduces fishery
capacity when complete evidence confirms water loss, losing only excess fish.
Restoring water allows gradual recovery without an instant refill. Unknown water
cannot delete fish. All1,221 full-build tests and the isolated Paper dry/recovery/
restart check passed. Automatic water recognition remains next, so your server is unchanged
and Phase3 milestone counts remain unchanged.

Fishing now has saved resource supplies and a timed catch path for a registered,
qualified business. Catching fish lowers its supply and adds owned food together;
restarts preserve both. Tests also show a hungry village can pay for that real food
and consume it after delivery. All1,210 full-build tests and the isolated Paper
timer/pause/restart check passed. Automatic water recognition/founding still
needs work, so the broader Phase3 milestone remains open. Your server is unchanged.
The supporting-water choice at that checkpoint has now been approved and its
saved transition implemented above; automatic live observation remains unfinished.

You approved fish supplies that rise and fall. The new calculation lets heavy
fishing empty a fishery and resting restore it, with editable catch/recovery rates.
Eleven new tests cover these rules and settings; all1,200 full-build tests passed.
Saving fisheries and connecting them to actual working
villagers are still ahead, so Phase3 milestone percentages stay unchanged.
Your playable server has not been changed. See FISHING_RESOURCES.md.

Stone/sand safeguards now recognize player placements, removal and movement,
including falling sand and newly generated stone. They prevent fresh-resource
duplication and preserve goods already extracted. All 1,189 tests and the isolated
Paper block/snapshot check passed. Automatic broader industries remain incomplete,
so milestone percentages are unchanged. Fishing recovery is the next pending
gameplay choice at that checkpoint; it has now been approved above.
Your playable server has not been changed.

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
| Resources and connected production |32/39|82%|
| Business lifecycle and competition |14/15|93%|
| Specialization and final acceptance |0/5|No complete entries yet|

The October13 food-chain additions expand this checklist. Equal weight per group
gives about69% partial checklist completion under the current scope. The former70%
described the earlier scope. Entries differ
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
| 3. Businesses & Industry | Farms, mines and workshops create jobs and shape each town | 1 / 4 | `██▌░░░░░░░` 25% whole gates; **69% partial checklist** |
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
