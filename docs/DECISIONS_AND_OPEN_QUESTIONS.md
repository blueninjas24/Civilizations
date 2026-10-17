# Civilizations — locked decisions and unresolved questions

## October17 — approved bakery scope: prototype implementation

Implemented the approved real grain/milk/egg chain with editable recipes and
prices. Startup uses the simplest meal recipe; runtime prefers feasible recipes
with more ingredient types, then stable recipe ID, and falls back to bread. One
recipe per ordinary worker/tool period. Business-only prices preserve public
food catalogues. Configured bakery/warehouse ingredient slots are additive,
empty, transactional and audited; existing assets/weights/opening terms survive.
These are configurable prototype choices within the approved scope, not locked
balance or physical cake crafting. No further answer needed for this batch.
See BAKERY_SUPPLY_CHAIN.md and current PROJECT_STATUS.md for verification.

## October17 — bounded free-range recognition: approved

Automatic adoption is approved in principle. A finite local scan cannot prove
that a very large unmarked player pasture has no enclosing fence farther away.
The current design protects uncertain existing pens, so the live classifier must
not silently choose how much uncertainty permits taking an animal.

User chose the recommendation: "Do whatever you recommend. Keep going."
Permit repeated complete loaded local scans to classify otherwise
unnamed/unleashed/non-human-origin, known-natural cows/chickens as free-range
when no nearby fence/gate/wall or saved pen protection is seen, under editable
scan limits; require [Civ Protect] for guaranteed protection of larger pastures.
Unknown/native-human origin, individually
recorded player owners and explicit protection remain excluded in either case.

This resolves the bounded-recognition tradeoff. Automatic first ownership is
now connected and verified under the documented settings in LIVESTOCK_ADOPTION.md.
Existing saved-herd paid feed/production is verified in LIVESTOCK_RUNTIME.md. All startup
qualification, real funding, capture trust and individual ownership guards remain.

## October17 — business slaughter products: approved

User confirmed: "Correct, no duplicate drops." When a village business deliberately slaughters its own animal, credit
the configured products to that business once and suppress extra ground drops
for that specific intentional slaughter. Ordinary player/mob kills retain normal
drops, reduce the herd and receive no duplicate business products. Physical
retirement/capture must still make retries and unloaded bodies safe. Existing
breeding reserves, real work and storage constraints remain.

This settles the physical-drop/accounting rule; no slaughter API is enabled yet.
Newborn projection, initial-age reconciliation and saved pen area lifecycle are
verified. Implement physical retirement and once-only product accounting before
enabling managed slaughter. No further answer is needed for this rule.

## October16 — livestock working radius: approved

User chose “Pause only animals confirmed far away.” A saved village animal
confirmed outside its farm's configurable working radius stops contributing
products and breeding until confirmed back. It remains village-owned; eligible
animals continue working. Unloaded chunks, missing observations and absent
worlds never by themselves establish departure or death. Preserve headcounts,
individual identity and aging separately from production eligibility. Saved
positive observation and configured work eligibility are now verified by1,739
Wrapper tests and the real-cow away/return/restart check. Automatic adoption,
physical newborn reconciliation and scheduled feed/output remain unfinished.
See LIVESTOCK_WORKING_AREA.md; no radius decision remains pending.

## October16 — unfinished purchases through player death: approved

User chose “Keep only unfinished purchase items until confirmation”. Only goods
from the unconfirmed purchase may stay with that player across death until the
trade is confirmed. Their other items follow ordinary server death rules. Once
confirmed, the special protection ends. This does not grant broad keep-inventory,
invulnerability, free containers, refunds or protection for ordinary existing goods.
The purchase controller must isolate the exact pending goods and preserve once-only
payment/delivery recovery. Selective retention and the purchase adapter are implemented, verified by1,681
Wrapper tests and actual Paper saves with synthetic death/respawn events. A human
client/actual crash remains release validation; see LIVESTOCK_MARKET.md.
No design question remains pending for this safeguard.

## October16: milk trade containers — approved

User answered: “Yeah they need to bring a bucket.” Players supply an empty
bucket for each milk bucket purchased. Fill it using recorded business milk stock
and the agreed trade payment; no free iron container is included. Eggs require
no container. Revalidate the bucket, stock, payment and destination space before
exchange. A missing bucket must refuse the purchase without taking payment.

This resolves the container question. Durable inventory/ledger recovery must
prevent duplicate milk or containers on retries and restarts. This approval
records the rule; milk trading and automatic livestock production remain
unfinished. It does not enable direct vanilla milking of village-owned cows.

## October16: player collection of village milk/eggs — approved

User answered yes: initially obtain village milk/eggs through recorded trade,
preventing separate vanilla harvesting for village-owned animals. Player-owned,
unclaimed and unsupported livestock keep normal behavior. Stock-backed direct
collection may follow later. This approves village-only bucket/egg prevention,
not taking player animals, suppressing death drops, manufacturing products or
opening an unfinished trading UI. All chicken egg variants need the same rule.
Village-only protection and player-breeding evidence are implemented, verified
by1,584 full-build tests and guarded native egg/event/PDC checks. Live output and
stock-trading UI remain unfinished; no collection decision remains pending.

## October16: livestock startup funding — approved

User answered yes to sharing the existing village startup grants with dedicated
chicken/dairy livestock businesses. The same common period/lifetime spending caps
and food-money protection apply, with no extra money or per-industry allowance.
Installed eligibility lists remain unchanged unless an admin opts in. This grants
funding eligibility only: real animals, trusted ownership, suitable sites, actual
feed, qualified work and safe physical reconciliation remain prerequisites before
live automatic founding/production. Protected eligibility, trusted atomic opening
and bounded real animal capture are verified by1,572 full-build tests and the
isolated Paper capture/payment/feed/output/restart/pause check. This does not
enable live adoption or automatic livestock production. No funding decision remains.

## October16: first craftsman customers — approved

User approved limited, configurable storage upgrades. Existing businesses may
buy actual storage fittings with their own cash when storage is constrained,
under editable prices, retained operating cash, delivery time and hard count/
capacity limits. This gives craft goods a real use before Phase4 physical
building expansion. No imaginary customer, public-stock confiscation, free cash,
player inventory use or block-change permission is granted. Existing goods,
opening terms, ownership and paid improvement history must survive transitions.
Storage customers, paid orders and delayed installation are implemented and
verified initially by 1,533 full-build tests and guarded Paper payment/delivery/
restart/pause checks. Qualified opening and gradual first-owner training are now
connected and verified by 1,558 tests and Paper training/opening/production checks.
Broader craft products and the Phase3 gate remain. See STORAGE_UPGRADES.md.

## October15: bakery competition for existing paid food orders — approved

User answered yes: bakeries may compete for existing paid village food orders
while basic supply is healthy. Eligibility uses configurable recent actual paid
food orders, current assessed resident demand, real post-startup purchasing cash,
food budgets and haul limits. It must not create consumers, food consumption,
cash or compulsory purchases. Existing rotating purchase rules still decide
actual sales; the forecast neither guarantees nor reserves revenue. Farmers may
also earn real grain-sale income. Installed files without the new bakery-market
section retain the earlier uncovered-demand rule. Numeric defaults are editable
prototypes under the user's existing economy tuning authorization.

Training, bounded commercial forecasting and automatic first-bread opening are
implemented and verified by 1,517 full-build tests and the guarded Paper check.
This does not finish the broader bakery/product/livestock gate.

## October13: bakery/craftsman workplace markers — approved

User answered “Whatever you recommend” to the pending workplace-marker decision.
Approved configurable recognition of existing nearby smokers for bakeries and
crafting tables for the single general craftsman, using the same no-container-
contents/no-player-building-ownership protections and safe replacement direction.
Recognition alone grants no business, goods, money or block modification rights.
Current work extends bounded observation and saved marker identity first; actual
founding/recipes and recovery remain separate verified gates. No marker question
remains pending. Existing installed configuration lists are not overwritten.

## October13: bakery/craftsman startup funding — approved

User answered “Yes. For sure. Ok keep going” to the recommended extension.
Bakeries and the general craftsman may use the same limited real village startup
grants, shared period/lifetime caps and food-budget protection already approved for
existing industries. The total spending limits are unchanged. Qualified founders,
real customers, suitable workplace evidence and actual supplies remain required;
grant eligibility alone does not found a business. Installed industry lists are
preserved. No startup-funding question remains pending.

## October13: village herd authority — approved

User answered “Yes, keep going” to simulation-managed breeding/slaughter for
village-owned livestock under saved feed, space and breeding-reserve rules.
Prevent vanilla births outside that accounting for village-owned animals only;
player-owned animals keep normal Minecraft behavior. Real player-caused deaths
still reduce the village herd. This authorizes herd control, not taking player
animals, manufacturing feed/capital, spoilage or mortality from invented shortages.
Implement with configurable prototype terms, identity evidence and native checks.
Current adoption/pen approvals remain intact; no new question is pending.

## October13: automatic livestock adoption and confirmed pen protection — approved direction

User prefers automatic adoption of eligible free-range livestock, rather than
per-animal approval. After discussing multiplayer authority and accidental walkway
claims, user accepted bounded enclosure previews and explicit per-pen confirmation:
“Sounds good. Keep building.” Protection/donation signs request a scan; signs or
fences alone do not establish ownership. A connected building/fence courtyard may
be geometrically enclosed, so its footprint and eligible animal count must be
previewed before confirmation. Open-ended or oversized terrain is refused.

Recorded player ownership, names/leashes and player-breeding evidence protect
animals. Unknown existing pens remain protected until authorized resolution;
old pen origins cannot reliably be inferred as player/natural from their shape.
Recorded owners or delegated/compatible land-claim managers may authorize their
pens; uncertain old ownership requires admin resolution. Proximity or first
arrival is not authority. A designated village pen is bulk consent for eligible
animals and future offspring; it cannot override an individually player-owned
animal. Escapes and broken signs do not clear saved ownership. Gates count as
structural boundaries even when open. Commands/permissions and admin-editable
recognition/adoption limits accompany the future interaction adapter.

The current batch implements plain-Java bounded geometry, actor/world/level-bound
preview checks, adoption eligibility and bounded livestock work only. No live sign
listener, particles/menu, live ownership capture, adoption or animal mutations yet.
Subsequent schema74 saves trusted first ownership records without exposing claims
to players; explicit ownership transfers and persistent pens remain unfinished.
It introduces no reliable natural-pen recognition or claims-plugin integration.
See LIVESTOCK_PROTECTION.md. The former opt-in-versus-automatic question is resolved;
no new product decision blocks the next persistence/adapter work.

## October13: hybrid farm obligation, paid surplus and basic food industries — approved direction

User requested a balance where farmers maintain village food reserves in exchange
for tools and sell surplus, then said the capped-contribution/paid-additional-food
approach makes sense and authorized continuing implementation. Farms contribute
a configurable bounded share toward actual meals/reserves; additional harvested
food and ingredients are business-owned and can be sold using real payments.
Village support retains protected tool budgets, not individual salaries/wallets.
Collective village purchases can meet remaining food needs without confiscating
business goods or creating money. Existing public food/grain/history remain public;
only future harvest allocation changes under explicit opt-in/tuning.

Approved expanded P3 direction: basic chicken/dairy livestock, separately tracked
baking ingredients and one general craftsman with editable recipes. No separate
craft occupations yet; geography-based village industry mixes remain canonical.
Livestock protects breeding reserves and bounds slaughter; breeding needs time,
feed and capacity. Renewable eggs/milk/wool do not slaughter animals. User expressly
excluded spoilage/expiration. Physical barns, docks, buildings and autonomous field
expansion remain Phase4; regional imports/exports Phase5. Random crop disasters
were discussed as future variety, not authorized live RNG in this batch.

Prototype numbers remain editable under prior tuning authorization. The expanded
checklist must be recounted; previous ~70% is an older scope, not current completion.

## October13: recurring purchases of public grain — superseded for future farm surplus

Harvested grain follows the existing public farm-supply direction; it does not
privatize food farms or invent business cash. Connecting bakeries now needs an
ongoing ingredient market. Prior approval covers limited one-time paid starter
materials, not an unrestricted recurring sale of public stock.

Recommendation sent to the user: allow bakeries/warehouses to buy actually owned
harvested grain using their real business cash, pay proceeds to the village
treasury, and use editable prices/purchase limits with training supplies protected.
Subsequent discussion approved the hybrid farm direction above: future surplus
belongs to the farm and buyers pay that business. This supersedes routing all
future grain sales through the village treasury. Existing public grain stays
public; no conversion or unrestricted public-stock sale is silently approved.

## October13: bakery grain accounting — approved

Current farm harvests, wheat/bread player deliveries and fish are recorded in the
same FOOD commodity. A real bakery needs distinct grain ingredients; these cannot
be inferred from existing generic food stocks. User approved the recommended direction: add separately tracked
future grain harvests/inputs, preserve every existing food balance/history, and keep
basic meals feasible without requiring a bakery first. Exact editable processing
coefficients remain prototype tuning under the user's existing authorization.
Existing food cannot be converted into invented grain. Wheat-field composition persists under schema71, and actual future protected grain
harvests passed schema72 full/native verification. Bakery activation and recurring
paid public grain sales remain unfinished; future farm-owned surplus uses the
approved hybrid direction above. Existing public grain is preserved. The user answered: “Go with your recommendation.”
Implement this direction with transactional migrations, actual crop/input evidence and
editable prototype coefficients; keep basic meals feasible and existing history intact.

## October12: shoreline damage recovery — approved

User approved looking for a replacement or trying to repair the workplace: accidental
player/creeper damage should not kill the industry. Temporarily suspend unsafe work;
look for a safe nearby bank beside the same original saved fishing habitat. Keep
business/site identity, workers, cash, owned goods, remaining fish and history.
Relocation cannot claim new water or reset habitat abundance. Unknown/unloaded
standing points do not prove damage; wait for actual loaded evidence.

If no safe replacement is confirmed, retain the business and pursue feasible repair
or dock construction through Phase4 project/material/labor/funding and player-build
protection rules. This is not permission to place free blocks or overwrite protected
player builds. Saved workplace/scheduler integration passed October 13 validation; physical
repairs remain unfinished. The prior fixed-spot-pausing recommendation was not approved and is
superseded by this adaptive recovery direction. No new decision is pending here.

## October12: proactive emergency food reserves — approved

User approved: “Yes, they need a reserve.” Villages may buy a small configurable
emergency food reserve before shortages, based on actual resident food needs.
Use the existing bounded real food budget; stored public food, incoming public
cargo, donated food and reserved player food purchases/barter all reduce the
remaining target. No fictitious consumption, free stock or new village money.

This resolves the customer/funding conflict: shortage-only food purchases could
not justify first fisheries while food-protected grants refused crisis spending.
Startup grants still refuse shortages and retain their shared limits/protection.
Editable prototype target is two future food periods plus current unmet food;
0 retains shortage-only purchases. Missing new keys in installed files default
to0 without rewriting them. This is separate from treasury protection measured
in food costs. Qualified automatic fishery founding, habitat/shoreline revalidation and scheduling
passed October 13 validation. Automatic first-owner fishing training and paid public food delivery passed October 13 native validation.

## October12: shoreline start and priority dock upgrades — approved

User approved starting fisheries on shorelines. Docks should be built quickly once
resources become available, and fishing from docks should be much more efficient.
The approved prototype direction uses safe existing dry footing/clear standing
space without block changes or a claim over player buildings. Recognition limits
and shoreline/dock productivity will be editable. Docks are a high-priority Phase4
upgrade when feasible, not free structures or a promise to ignore funding, labor,
materials, player protection or other urgent needs. No fixed multiplier or deadline
is locked. Shoreline work and dock work use the same habitat-backed finite catches
and gradual recovery: efficiency changes work, never habitat abundance/refills.
Physical dock construction and the production-efficiency transition remain planned;
they are not yet implemented. The workplace decision now unblocks Phase3 startup.

## October12: natural water and player ponds — approved

User answered “Whatever you recommend”: both natural water and sufficiently
large/deep player-built ponds may qualify under editable requirements, with no
block changes or claim over player buildings. Historic origin need not be guessed
from block types. Habitat recovery/loss, actual skill/work/storage/customer checks
and the shared food-protected funding limits still apply. This approves eligibility,
not free startup, block alteration or guaranteed businesses at every body of water.
Bounded snapshot evidence and live SQL-owner submission are verified; contiguous
selection and automatic founding remain unfinished. No new design answer blocks that next integration work.

## October12: confirmed fishery habitat loss — approved

User approved: “Go with your recommended answer.” Confirmed suitable-water loss
reduces capacity, retiring only fish above the new limit. Restoring water restores
capacity without refilling stock; normal recovery is gradual. No remaining habitat
capacity pauses catch and recovery. Incomplete/unloaded evidence cannot establish
loss. Original supports and opening terms stay saved; changes cannot create new
resources or reset extracted goods. The explicit SQL-owner transition and schema67
audit implement this rule; automatic live water observation remains unfinished.
Effective capacity is the floor of original saved capacity times remaining suitable
supports divided by original support count. This preserves capped opening abundance
under admin retunes. Existing catch/recovery settings remain editable.

## October12: fishing resource recovery — approved

User approved: “Yes the fish stock should ebb and flow just like real life.”
The approved model is saved renewable fish supply tied to
actually observed suitable open water, with gradual recovery and temporary
exhaustion from heavy harvesting. Editable capacity/catch/recovery; absent worlds
and server downtime pause it. Visible Minecraft fish entities would not be needed
for every abstract catch. This supplies a real habitat-backed resource rather
than goods created from profession alone. Plain-Java resource calculations,
strict editable prototype terms, transactional saved stocks/owned catches and
normal SQL-owner timed work are verified by1,210 full tests and a guarded native
owner-timer check; automatic live water observation and qualified founding remain
unfinished. No automatic fishing startup has been enabled. See FISHING_RESOURCES.md for
defaults, recovery/catch conservation and remaining verification. Crop/tree renewal
and quarry provenance/source-loss safeguards retain their existing approvals.

## October12: approved gradual merchant training and automatic startup connected

Existing commerce-study/startup-grant/marker approvals now drive actual first
warehouse opportunities. Existing editable study/founding settings define rates,
materials and qualification; no new economic rule or free skill. One prospective
merchant, real future-period protected supplies, current whole-chain feasibility,
saved course/policy links and normal fresh-barrel acceptance are verified. Unknown
opportunities wait; another first warehouse taking the opportunity releases the
unfinished learner. Schema63 preserves identities and old terms/assets/history.
Ordinary Paper scheduler/SQL timer and marker pause/restart passed. Broader producer
funding was subsequently approved below. See
AUTOMATIC_WAREHOUSES.md and PROJECT_STATUS for exact limits/results.

## October12: broader industry startup funding — approved

User approved: “Include them in the same protected grant limits.” Logging, fishing,
finite stone quarries and sand pits join mines/workshops/warehouses in the same
real village startup-grant budget. No increase in maximum, period or lifetime caps,
no separate industry allowance and no new funds. Current food/population assessment,
protected treasury, actual cash, qualifications, suitable resource evidence and
real customers remain necessary. Stable configuration industry keys are logging,
fishing, quarry and sandpit. Existing installed lists are preserved; admins opt in
while stopped. Eligibility is not automatic discovery, production or construction.

## October12: first quarry/sandpit resource baseline — approved

User agreed with the recommendation: discover exposed existing stone/sand
naturally, excluding recorded player placements. Old block origins cannot be
proven. Each admitted coordinate represents a finite abstract reserve, not one
item per block; gradual extraction is bounded by workers, material work rate,
site and storage. Restart/rediscovery never refill it; removal loses only that
coordinate's remaining share. Eligibility and numeric prototype terms are editable.
Phase3 makes no block changes or property claims; actual digging remains Phase4.

Initial editable quarry-resources prototype: stone64 reserve batches/block,
3 units/batch,6 batches/worker,32 site batches; sand64 reserve batches/block,
4 units/batch,12 batches/worker,32 site batches. Abundance and work speed are
separate; these terms are not canonical permanent balance. Missing old sections
opt out. Persistence/scan seams alone do not enable automatic quarries/pits.
Future loaded-world observation/placement capture, real customers, qualified
founders and shared protected funding must be connected before live automation.

## October12: real village customers for food and training — approved

User approved: “Yes to your decision needed. Keep goimg.” Needs-based purchases from actual local firms using real village cash:
fishing food supplies actual village food shortages; logging/quarry output supplies
actual supply-backed training needs. Configurable prices/caps, delivered owned goods
before use, material purchases protect food funds; food purchases use a separately
bounded food budget. No fake shopper, wealth creation, automatic disposal or personal
payroll. Existing player food payments and public farm toolkit purchases do not
implicitly authorize this broader cash/material transfer. Asked under AGENTS.md
before connecting the new economic rule; approval received. Editable numeric
prototype budgets/prices may be chosen under the existing configuration approval.

## October12: first-warehouse qualification prototype connected

Under approved configurable economy, gradual skill and public-grant direction,
warehouse-founding uses editable commerce10 owner/1 worker, age216 (at least
adulthood), page64 and capacity2. Unlimited ownership/one worker job remain. No
new wealth, payroll, property claim or free experience. Qualified preparation and
fresh-barrel/current-SQL acceptance verified; first merchant study/scheduler remain
next. Missing old sections opt out. See WAREHOUSE_INVESTMENT.md and status.

## October12: provisional first mineral-chain warehouse review

Under approved configurable economy/startup-grant/marker direction, read-only
warehouse prospects use editable500 startup/4000 capital hundredths, capacity1024,
page8, evidence60 seconds, range96 horizontal/48 vertical. Whole-chain current
cash/lifetime allowance and finite connected sources are checked conservatively;
no future public income or deposit pooling is assumed. These checks are planning
evidence, not a new automatic startup rule, guarantee of sales/profit or creation
of assets/skills. Existing files opt in. Actual qualification/training and final
point/SQL acceptance remain next. See WAREHOUSE_OPPORTUNITIES.md and status.

## October12: approved first-workshop training direction connected

Existing approval for gradual supply-backed unemployed-adult training now applies
to actual viable first-workshop prospects. Existing resident-study/career-study and
workshop-founding settings supply all numeric prototype terms; no new gameplay
rate, free skill or funding rule. Unknown/stale/unavailable opportunities pause
practice and retain courses; another blacksmith taking the first-firm opportunity
releases the learner. New schema62 link/audit tables retain course identity. Normal
Paper scheduling and actual SQL timer verified; warehouses remain next. See
WORKSHOP_FOUNDER_TRAINING.md and PROJECT_STATUS for exact scope/results.

## October 12: first-workshop qualification prototype — implemented application path

Under the approved editable economy/training direction, the narrow first-toolkit
workshop path uses saved owner smithing10, worker smithing1, age216 months (at least
configured adulthood), capacity2 and qualified-page64. All are editable in
workshop-founding. These are prototype settings, not new locked canon. Existing
files without the section opt out. No ownership count limit; owners may hold many
firms but cannot hold two employee jobs. No new skills, payroll, money or goods.
Real funded farm need and paid starter/public grant/marker approvals still apply.
Preparation and acceptance are verified; gradual first-owner training and the
normal automatic scheduling adapter remain separate unfinished work. See
WORKSHOP_INVESTMENT.md and PROJECT_STATUS for exact results and limits.

## October 12: removal of a new workplace marker — approved

User approved “Pause until restored.” Newly marker-founded workshops and warehouses
pause production and new trades after an actual loaded-block read confirms their
saved smithing table/barrel is missing or the wrong kind. Restore the required
marker at the same saved anchor to resume. Preserve business/site UUIDs, workers,
money, stocks, paid cargo and history. Unknown/unloaded chunks and absent worlds
are not removal evidence. Existing downtime/absent-world time policy still applies.
No closure, relocation, demolition, player property claim, renewed startup grant
or starter entitlement follows from removal/restoration. Legacy manually founded
firms acquire no marker requirement through migration or startup replay.

Editable economy.yml `workplace-operations.pause-on-missing` defaults true; restart
applies edits to existing marker-bound firms without erasing evidence/history.
The narrow trusted startup seam attaches the requirement in the same transaction
as the grant/job/paid supply order; this does not yet choose or train founders.
Implementation and verification are recorded in WORKPLACE_OPERATIONS.md and status.

## October 11: first workshop starter supply decision — approved

Current code has a bootstrap dependency: WorkshopSupplies buys real inputs only
from existing warehouse stock; WarehouseSupplies buys existing producer output;
AutomaticMineFounding requires a staffed funded warehouse and actual existing
workshop demand. A new village has no private producer, warehouse or workshop.
The tested connected chain explicitly seeds these businesses; it does not solve
automatic first founding. Public startup grants supply money, not raw materials.

User approved “Allow limited paid starter supplies.” Permit a limited one-time purchase of actual
surplus village starter materials by a newly founded workshop, using that firm's
real grant-funded capital. Goods leave the saved public commodity reserve and
payment returns to the village treasury. Use configurable prices, purchase caps
and protected material reserves; preserve food and training supplies. No invented
stock, free tools, hidden ore, personal wallets or new money. Normal qualification,
workplace evidence and food-protected shared grant checks remain mandatory.
This is starter support, not an unlimited ongoing public supplier that bypasses
mines or warehouses. Delivery/ownership/storage and replay must be accounted for
and tested before enabling the rule. Existing files must explicitly opt in.

Prototype policy: one recipe batch, at most 4,000 hundredths, leave two units of
every purchased material, reuse editable base trade prices, never sell food.
Accepted purchases remain buyer-owned delayed cargo; one entitlement per business
UUID. Startup, payment and inventory changes share one transaction. Gross grant
limits remain consumed despite payment returning to the village. Existing files
without the explicit section keep this rule disabled. Automatic first-workshop
opportunity selection/training is separate work; this approval does not waive
real customer, qualified founder or workplace evidence checks.

## October 11: approved farm upkeep now drives warehouse restocking

Existing public farm equipment approval now supplies genuine local customer
pressure. Actual productive wear/current food proof and protected funds precede
warehouse orders; existing goods/cargo cover needs. No new revenue rule, taxes,
prices, reserves, fake customers or funding reservations. See
PUBLIC_FARM_INDUSTRY_DEMAND.md. First warehouse/workshop founders remain next.

## October 11: approved gradual training connected to first mining founders

The existing approval now supports a verified narrow mining-founder adapter: real
current customers, observed ore and affordable protected grants precede sponsorship;
actual supplies and future periods precede qualification. Opportunity loss cancels
study without losing earned skills. No invented employer or startup reservation.
Other first-founder careers remain next. See MINE_FOUNDER_TRAINING.md.

## October 11, 2026: existing workplace markers approved

The user answered "Yes" to configurable recognition of existing nearby smithing
tables and barrels as data-only workshop/warehouse workplace markers. Recognition
does not move blocks, access block inventories, convey property ownership or
permit changes to player buildings. Business founding still requires qualification,
actual demand/funding, a suitable unclaimed current marker and revalidation; a
marker is not itself a business or a construction/occupancy permission.

The first scoped batch implements bounded loaded-chunk snapshot observation and
saved current candidates/history. First founders and automatic first workshop/
warehouse founding remain subsequent work; construction stays Phase 4.

## October 11, 2026: approved mine direction connected to real startup

The automatic coal/iron adapter uses approved gradual skill requirements, finite
exposed-ore evidence, protected actual village grants and real industry demand.
It requires an existing funded staffed warehouse; it does not grant first founder
skills or fabricate customers. Editable prototype range/yields/budgets are saved
opening terms, not authority to excavate or occupy player structures. Source
anchors are data-only and checked against saved workplace/bed coordinates.
First warehouse/workshop opportunities and founder training remain subsequent work;
physical fit, land protection and mine construction remain Phase4. No new locked
principle or government/ownership rule was selected. See AUTOMATIC_MINES.md.

## October 11, 2026: gradual commerce qualifications approved

The user answered yes to a separate editable commerce skill learned through gradual
study and actual business transactions. Ownership must remain qualified: mining
experience alone does not become commerce experience. No free initial merchant
skill, automatic profession conversion or ownership cap is authorized.

The first implementation connects actual new paid warehouse upstream orders to
current employee experience. Supply-backed commerce courses use editable practice
materials. Automatic existing-vacancy enrollment now connects the approved courses to real
job requirements; see VACANCY_CAREERS.md. Sales-only learning, founder selection
and first merchant opportunities remain subsequent scoped work. See COMMERCE_LEARNING.md.

## October 11, 2026: real spare-business upkeep contributions approved

The user approved configurable upkeep contributions from actual spare business
cash, with operating funds protected. Opening funds remain once-only; player food
purchases are public outflows and refunds are not income. No household wallets,
minted cash or government/tax system is authorized here.

Implementation is scoped to a verified public farm-tool invoice whose village
cash falls short after food protection. Its operating local supplier may fund
that exact shortage from saved sales-backed cash. Original village-funded capital
plus an editable reserve, spare-cash percentage and saved business/village period
caps stay protected. Contribution and actual goods invoice commit together or
both roll back. Gross sales are not claimed to be net profit. Existing owner files
opt in. See UPKEEP_CONTRIBUTIONS.md for settings, limits and verification.
Automatic first industries, careers and broader specialization remain unfinished.

## October 11, 2026 — public farm equipment funding approved

The user approved village treasury payment for needed farm tools under a small,
editable upkeep budget protecting food funds. Preserve worker-backed food going
straight into village supplies; farms need not become private food sellers.
Real tool goods and actual payments must be conserved and audited; no free tools,
created cash, household payroll or tax system is implied. Recurring upkeep is
separate from the approved shared startup grants. The explicit payment adapter
is implemented and verified: real toolkit, actual public cash, current farm work
and food proof, one-space equipment store, paid delivery and saved budget receipt.
Actual food-clock wear, paid arrivals and bounded replacement orders are now
implemented and verified. Zero output causes no tool use or wear; service is saved
and absent worlds pause. Missing legacy settings opt out. See FARM_TOOL_FUNDING.md
and FARM_TOOLS.md. Automatic first nonfood industries remain unfinished.

## October 11, 2026 — warehouse startup grants approved

The user approved warehouses sharing the already approved mine/workshop startup
grant limits and food-budget protection. Their purchasing capital must be debited
from actual village money, with no separate spending allowance or created money.
This approval does not remove qualification, suitable-site or demand checks.
The guarded warehouse startup adapter now implements this rule; it opens empty
stock, transfers real capital and assigns a qualified first worker atomically.
Existing industry lists stay unchanged, with warehouse enabled only when listed.
Automatic opportunity selection/founding remains unfinished. See STARTUP_GRANTS.md.

## October 11, 2026 — business tool upkeep approved

The user approved gradual tool wear at productive businesses, replacement
purchases paid from their own business money, configurable rates and a startup
grace period. Missing tools should slow work. This establishes actual maintenance
demand for blacksmith output; it does not authorize fake consumers, new money,
individual payroll or public treasury subsidies. BUSINESS_TOOLS.md documents
the implementation and editable prototype settings: three productive grace
periods, eight work periods per toolkit, 50% work without tools and 40 Trade Value
per replacement. Existing files must opt in explicitly. Retained store catalogs
are never rewritten; unsupported stores retain their prior work behavior.

## October 10, 2026 — limited village startup grants approved

The user approved configurable village startup grants for first privately owned
mines/workshops, paid from actual village money after protecting the food budget.
This permits new industry without inventing personal bank accounts, estate funds
or free capital. Grant amounts, limits and food protection must be editable, with
recorded conservation and rechecked qualification/demand/site prerequisites.
It does not authorize an unlimited subsidy, a loan/debt system or government
mechanics ahead of Phase 6. Guarded startup accounting now implements this rule;
automatic opportunity selection/founding remains unfinished. Editable prototype
defaults are 50 Trade Value per grant/current village food period, 200 total per
village, and protection of max(25 Trade Value, two food-demand periods estimated at
2 Trade Value per food unit). Food shortage/missing current assessment or a changed
living population makes startup wait. Limits constrain public spending, not private
ownership. Old files opt in; no loans, payroll or government behavior added. Saved
mineral evidence alone creates no business or money. See STARTUP_GRANTS.md.


## October 10, 2026 — approved legacy per-block reserve accounting choice

Inspection after farm learning: mineral_deposits stores a shared remaining batch
balance. Saved coordinate evidence has no per-coordinate depletion ledger; later
overlapping evidence can add identity without adding reserves. Supporting block
loss and even allocation of the legacy remaining balance are now explicitly approved.

Question sent to the user: may remaining resources in existing veins be divided
evenly across their saved supporting ore coordinates, with subsequent abstract
mining draining those shares evenly? Removing one coordinate would retire only
its remaining share. Conversion must preserve total remaining batches, extracted
goods, payments and immutable discovery/production history, with an adoption audit
and deterministic remainder handling. Alternative: preserve existing aggregate
veins and introduce per-block accounting only for newly created veins.

The user approved even division with recorded conversion. Schema 46 ledger and
explicit retirement are now verified by 806 tests and the packaged Paper probe.
Native break/explosion/piston capture and production trust protection are now
verified by 819 tests and registered Paper listener checks (schema 47). Older
coordinate saves require reviewed coverage recovery; no automatic trust reset.
First automatic mines remain unfinished. No runtime source loss has been deployed
to the human server. The schema 45 farm batch is independent
of this approval.

## October 10, 2026 — unlimited private ownership approved

The user chose unlimited business ownership where private ownership applies.
Every acquisition still requires industry qualification and actual funds; each
resident can hold only one worker job. A computational buyer-review budget must
not become a portfolio cap. Government-specific restrictions are deferred to
Phase 6 government modifiers; no government types or dictatorship rules are
implemented by the Phase 3 resale batch. See AUTOMATIC_BUSINESS_RESALE.md.

## October 9, 2026 — approved industry development direction

User approved bounded, paid exploration that can fail and stops for reassessment;
structured mine networks (main haul tunnels/shafts/branches), terrain/hazard/player
protection constraints; finite newly discovered reserves distinct from no-refill
revisits; timed extraction with editable material resistance and abundance separate
from trade value. Apply suitable locations, non-1:1 output and bounded development
to all industries, preserving crop/tree renewal rather than mineral regeneration.

Business closure retains sites, workings and history. Later physical damage can
alter structures without deleting identity; no autonomous disaster behavior is
enabled now. No guaranteed or uniformly distributed industries: feasible geography,
demand, skills, capital and limited labor create specialization and later diversity.

Stone quarries and sand pits are primary resource industries. Reserves should grow
with actual expected demand, funded projects and an editable safety margin. Surplus
disposal is a proposed explicit capacity/labor-limited cost, never paid demand;
protect essentials/player goods and prefer actual use, trade and storage. Lava
incinerators remain a physical proposal, not an implemented industry or final recipe.

Nearby workplaces should show actual saved employed residents with schedules and
configurable visibility, not duplicate decorative workers. Phase 3 supplies business
and resource rules; Phase 4 supplies physical excavation/layouts/work points; regional
transport remains Phase 5 and physical deterioration/disasters follow later gates.
The canonical nine-phase roadmap is unchanged. See PHASE3_TODO.md.

October 9 playtest decisions, with implementation boundaries:

- Succession first selects eligible, industry-qualified family, then a qualified
  existing employee. Kinship never bypasses age/skill/occupation requirements;
  a miner does not become a qualified shopkeeper just by inheriting.
- With no qualified successor, the family tries to sell to an eligible, qualified
  villager who can actually afford the purchase. Approved configurable prototype
  prices and sale deadline; proceeds belong to a persistent family estate fund,
  separate from village money, for later eligible family business investment.
  No successful sale by the deadline closes the business. Closure ends operation,
  not its saved identity/history or physical building; retain remaining assets.
  Additional approval: qualified buyers may use their family estate fund and their
  existing business's spare cash under configurable limits. Every withdrawal/payment
  must be recorded and conserved; no automatic treasury grant or invented money.
  Qualified inheritance and paid-sale/estate accounting are now verified. Editable
  funding defaults retain 10.00 Trade Value, allow 25% of spare business cash and
  100% of estate cash per purchase. Saved explicit offers and online-only deadlines
  are verified. Retained unsold closure is now verified. Automatic valuation,
  listing creation and qualified funded buyer review are now verified (schema 44).
  Additional explicit approval: qualified existing employees may keep the business
  operating while the family seeks a buyer. Real supplies, finite reserves, storage,
  purchasing funds and world-pause rules still apply. Continued work does not make
  employees qualified owners, mint estate income or grant automatic hiring authority.
  Keep this rule configurable. A moved living owner is not a deceased seller.
- Additional explicit approval: unemployed adults can train gradually using real
  available supplies; employees can gain skills through actual work. Costs, speed,
  limits and qualification requirements must remain editable. Required ownership
  thresholds still apply; no instant career changes or invented productive labor.
  Starting resident profiles currently have no saved mining/smithing experience.
  Actual employee learning in timed mines/workshops (schema 42) and automatic
  field enterprises (schema 45) is now verified;
  unemployed-adult supply-backed study is now verified (schema43). Automatic
  opportunity-based career selection and nonfood founding remain unfinished.
  Approval does not waive saved qualification requirements.
- Businesses and households prioritize available suitable vacant premises before
  new construction. Reuse depends on needs, capacity, condition and permission;
  vacancy alone never justifies an unsuitable move or modifying player buildings.
  Physical building evaluation/reuse/construction remains Phase 4 work.
- Player-mined supporting ore loses only that coordinate's source, not the whole
  connected vein. Future work must preserve already extracted goods and history,
  avoid double loss, and prevent removal/replacement/revisit from refilling reserves.
  Per-source attribution and native removal are now verified in schemas 46/47;
  the human server has not been upgraded. Reviewed older-save recovery is pending.

Schema 37's cadence-aware hauling is an engineering correction under the approved
editable economy authority. Rate/window identify different actual transport times;
retuning starts a forward quota namespace without changing earlier paid terms or
inventing historical cadence for older records. Original cargo, funds and history
remain; no ownership or mineral-depletion answer is inferred from this fix.

Paid workshop supply uses the approved configurable economy prototype authority:
new files enable up to two periods of complete recipe inputs, bounded review of
16 actual local warehouses and existing editable base Trade Value prices. Only
business-owned funds/stock transfer; no new money, individual wallets, speculative
finished-goods consumption or universal industries. Old files explicitly opt in.
Paid automatic cargo has the buyer's persistent clock identity; legacy shipments
retain their original delivery caller. Schema 36 and application/native checks
verify the accounting. See WORKSHOP_SUPPLIES.md. No new answer needed for this scope.

The live placement fence is a conservative engineering safeguard, not a new mineral
regeneration rule: capture failures/unclean stops pause new automatic discovery
pending reviewed recovery. Existing settlements, reserves and history remain.
Native placements and conservative piston exclusions are verified. Recognized
unsupported bulk-editor presence now fences discovery (ORE_EDITOR_GUARD.md); unknown
editors/offline edits remain uncertified and supported operation integration is pending. See LIVE_ORE_TRACKING.md.

## October 9, 2026 — timed existing industry prototype

Approved configurable economy authority supplies a 30-real-second work period and
eight existing firms per pulse for new files. Old files explicitly opt in. Timed
work consumes real saved reserves/ingredients with actual employees/storage; no
new funding grants or invented consumers. World/server absence pauses. Existing
clocks/history survive restart; cadence changes require an empty queue and retain
a transition record. Work/input changes, receipt/history/checkpoint are atomic.
Owner unavailability currently defers operation without selecting an heir. Qualified
succession/resale rules are approved; qualified inheritance and paid-sale accounting
are verified, while automatic resale/closure remains unfinished. Automatic
opportunities/founding, purchasing/customer demand and wider
industry coverage still need scoped work. See TIMED_INDUSTRY.md.

## October 9, 2026 — approved exposed existing ore prototype

The user answered: "Allow exposed existing ore." Exposed pre-existing coal/iron
may count as finite prototype resource evidence even when its old natural/player
origin is unknown. Newly tracked player ore placements are excluded. Minecraft's
current blocks cannot certify old natural origin; this approval accepts that limit.
It does not grant block ownership, allow physical excavation, refill deposits or
permit forced chunk loading. Durable placement tracking and a bounded live observer
are prerequisites before enabling automatic mine opportunities. See
EXPOSED_ORE_SCANNING.md for conservative exposure and completeness rules.

## October 9, 2026 — coordinate identity engineering boundary

Permanent world/ore coordinates now establish one finite saved deposit under a
trusted registered site. Overlapping surveys link identity without extending
reserves; different sites or multiple saved deposits report conflict instead of
refilling, transferring or merging. Missing legacy coordinates are not fabricated.
These are protective internal identity choices, not natural-origin certification,
geological cluster criteria, mine expansion or block rights. Automatic discovery
still requires live observation and placement tracking; the origin choice above is
now approved. See MINERAL_IDENTITY.md.

## October 9, 2026 — workshop recipe prototype accounting

Approved editable economy authority supplies the first blacksmith recipe: two iron
plus one coal yield one abstract tools unit; two batches per actual worker, site cap
eight per requested economic period. These are text-editable prototypes, not item
trade ratios, infinite inputs or guaranteed income. Stored goods must be owned and
delivered; input consumption/output/storage/history share a transaction. This batch
implements explicit owner processing, not automatic workshop discovery/scheduling,
NPC shopping, physical block work or household payroll. Tools are business stock;
village reserves and old owner files are unchanged. No new design answer is needed.
See WORKSHOP_PRODUCTION.md for saved recipe history and deployment limitations.

## October 9, 2026 — automatic field enterprise prototype

Under the user's approval to choose editable economy prototype settings, one
village-wide field enterprise can start from complete actual crop evidence, an
available adult and existing startup funds. New-file defaults: capacity four,
25 Trade Value startup, age at least 216 months/adulthood, beginner experience zero.
Existing owner files explicitly opt in. Assigned workers replace legacy adult
field labor; no double production or invented capital. This aggregates observed
fields for the first farm prototype, not permanent zoning or a per-building rule.
Saved sites grant no crop ownership or block modification rights. Existing founding
terms survive tuning; expansion, closure, succession, wages and nonfood automatic
industries remain later scoped work. No locked design principle changes and no
new user answer is required for this batch. See AUTOMATIC_FARMS.md.

## October 9, 2026 — Phase 3 business persistence boundary

First backend batch uses explicit caller-supplied site/opportunity, age/skill
requirements, capacity and startup cost. Positive investment debits actual village
treasury atomically; no new grants, income, salaries or personal wallets. No defaults
or automatic founding policy chosen. The canonical real-business/labor direction
supplies this scope. Existing configurable economy authority remains available for
the later admin-editable opportunity policy.

One current primary job per resident and one business per site initially prevent
double-counted labor/sites. These are provisional engineering boundaries, not locked
part-time employment/shared-premises laws. Ownership is separate from employment.
Sites grant no block rights. Dead/emigrated people immediately cease counting as
productive workers; owner reconciliation later dates a retained job separation at
the observed month. Succession/closure are not inferred from owner death.
No new consequential answer blocks this storage batch. See BUSINESS_FOUNDATION.md.

## Locked / approved
- October 9 observed-patch engineering boundary: face-connected positive evidence
  is grouped without assuming diagonals, gaps or hidden ore. This conservative
  utility is not a new geological law, founding policy or final exploration rule.
  Existing permanent coordinate claims must be resolved before any founding;
  provisional patches never split old deposit identity or refill reserves. Complete
  bounded observations only, no world API or block changes. See RESOURCE_PATCHES.md.
- October 9 piston provenance engineering boundary: exclude moved ore at its source
  and both adjacent axis coordinates. This accepts one conservative extra exclusion
  rather than guessing destination direction or querying/loading neighboring chunks.
  It never certifies natural origin, transfers deposit identity, refills reserves,
  cancels movement or changes saved extraction terms. Bulk editors/offline world
  changes are still outside verified capture. No new gameplay approval is inferred.
- October 9 world/time clarification: pause absent worlds and server downtime.
  Returning to an original world resumes saved population/economy without crediting
  years of absence. Unloaded chunks in a loaded world still simulate; retain the
  plugin DB, permanent world identity/history and already earned pending work.
- October 9 industry clarification: mineral deposits are finite and never regrow or
  refill on restart/re-observation. Non-1:1 geology-based ore/rock byproducts share
  work. Sizes/capacities/abundance/rates are admin-editable with complete defaults;
  observed ore is evidence of a larger finite deposit, not an unlimited producer.
- October 9 approval: abstract finite storage, warehouse buy/own/resell model and
  local transport accounting. Purchasers require actual funds; goods have one owner
  and in-transit goods are unavailable until arrival. Phase 4 physical storage and
  Phase 5 regional routes remain in the unchanged nine-phase roadmap.
- October 8 negotiation/admin clarification: user approved the proposed need-based
  bargaining direction: accept/counter/refuse, configurable failed-round limit and
  cooldown, protected essential supplies, reasons visible to players, no automatic
  reputation penalty merely for disagreement. Server admins choose economy behavior
  primarily through a commented text file. Numeric defaults remain tunable prototypes.
- October 8 finance clarification: initial residents may begin at a moderate,
  neutral financial position rather than requiring player contributions to bootstrap
  every village. Use documented configurable prototype opening capital as its
  spendable basis; future economic choices/conditions change finances. This does
  not claim existing workplace assignments or introduce personal salary accounts.
- October 8 player payment clarification: support emerald payments AND optional
  barter for goods the village/merchant actually produces and has available.
  Production context determines relevant goods (e.g. timber, mining products, fish),
  with abstract Trade Value as the common valuation basis. Example quantities such
  as five emeralds for 64 wheat are illustrative, not locked prices.
- October 8 economy authorization: Codex may choose documented configurable prototype
  economy settings. Keep tuning in an easy-to-edit server file, including support for
  different famine/feast scenarios; these defaults are experimental, not final canon.
- October 8 authorization: disable zombie conversion for Civilizations-managed
  villagers in the prototype. Cancel INFECTION transforms for saved entity identities;
  do not cancel unrelated conversions or grant general damage immunity.
- October 7 authorization: use permanent Minecraft world UUIDs and saved bed/villager
  identity matching for settlements. Renames/revisits must retain identity; overlap
  with multiple saved towns produces diagnostics, never an automatic merge.
- October 7 authorization: create simple configurable prototype partner pairs among
  unrelated adult residents sharing a home, enabling future births. Do not assign
  invented parentage to already existing children.
- October 7 clarification: recognize both naturally generated villages and
  player-built areas containing beds and villagers automatically. This does not
  authorize autonomous block changes or enroll a bed-only player base.
- October 7 clarification: villages participate automatically by default; no manual
  registration/join step for normal villages. Recognition must still distinguish
  villages from arbitrary player positions/beds, and never grants block-edit consent.
- October 7 authorization: Codex may choose sensible, editable experimental Phase 1
  population settings (clock, adulthood, births/deaths, housing, migration), document
  them and test them for later tuning. Such defaults are prototypes, not locked canon.
- Nine-phase dependency roadmap; foundation/population before construction.
- All **existing development settlement data is disposable**, but do not delete anything until confirming it is a dev DB. Future live worlds and histories require preservation.
- Java 25 and Paper 26.2 build 129 are the **current development environment**, not a guarantee of minimum/maximum supported server versions.
- Persistent settlement UUIDs, persistent individual residents, households and jobs; global simulation/local materialization; no LLM requirement.
- No god-mode players; per-settlement reputation, player-build protection, opt-in player development zones.
- Abstract but physically justified production; trade value independent of emeralds; treasury distinct from prosperity.
- Organic roads/layouts, authored templates and eventual in-game authoring; context-sensitive decay and history.
- Randomness bounded by feasibility; lightweight government; rare wars/disasters; abandoned towns preserve history and may return.
- Public Civilizations source is acceptable after auditing secrets/history; Chronicler source remains private. This repository must not contain hidden Chronicler mechanics.

## Open — do not silently lock numerical/behavioral rules
1. Exact criteria for recognizing a settlement: POI cluster, villagers, vanilla village, player-founded location; distinction from player settlement.
2. Center/border evolution and merging/splitting criteria. World UUID and saved
   bed/villager identity matching approved; multi-town overlap must be diagnosed.
3. Housing unit/capacity formula, treatment of zero/zero, and whether equal occupancy counts as shortage or full capacity.
4. Calendar time dilation, economic tick cadence, migration/birth/death probabilities and physical NPC density budget.
5. Commodity unit scale, reserves, price bands, shortage thresholds, treasury and prosperity equations.
6. Building templates and licensing, road planner algorithms, player-build ownership/protection heuristics.
7. Simulation cap/reconciliation budgets, offline catch-up limits and conflict simulation fidelity.
8. Exact government weights, significance thresholds, event probabilities and influence thresholds.
9. Server support range and plugin API metadata, command syntax/permissions and external integration API.

## Decision process

- Family-memory engineering choice: schema v5 records parent/child and canonical
  partner facts independently of housing; siblings derive from shared parents.
  Ancestry cycles/self-links are invalid. This does not choose fertility, parent
  limits, partner exclusivity, marriage lifecycles or family-generation policy.

### Engineering choices — October 7, 2026
- October 8 approved economy prototype: separate `economy.yml`, one 60-second period,
  legacy headless offline cap 120 and batch two, adult/child food demand 2/1, output four per evidenced
  available worker at editable 100%. Famine/feast recipes use 25%/200% output. Initial
  units per resident: food 12, timber/stone four each, iron/coal one each, livestock zero.
  Severe shortage <=50% coverage for three deficits; surplus >=125%, abundant >=200%.
  Base Trade Values food 2/timber 4/stone 3/iron 12/coal 6/livestock 5; scarcity multipliers
  250/150/100/85/70%. Strict validation and comments; no automatic refilling on restart.
  Tested formulas and startup config are implemented; live scheduler/facilities/merchants,
  demographic crisis effects and recorded existing-save tuning transitions remain pending.
- October 8 Phase 2: schema 14 stores explicit commodity inventory, protected policy,
  independent completed economic periods and retained flow inputs. Accounting replay 1
  uses exact nonnegative integers and deterministic ordering; all initialized goods
  need explicit period evidence. Scarcity labels are caller-tuned; absent accounts
  stay unknown. No runtime defaults, economy scheduler or population effects chosen.
- User authorized Phase 2 development while human client playtesting is deferred;
  automated tests and relevant disposable Paper checks still required.
- October 8 live projection: main-thread create/claim/add/activate handshake with a
  persistent temporary-freeze marker; loaded absence never infers death. Actual native
  deaths and late enrollment evidence save on SQL owner; accepted shutdown work drains.
  Editable radius/density budgets use 64 blocks, 32/day and eight/night per world.
  Night limits new spawns; existing villagers retain vanilla behavior. These are
  prototype performance/context settings, not a new locked population law. Vanilla
  trades/profession/inventory on recreation remain an explicit limitation, not a
  decision to reset trade progression in the eventual survival experience.
- October 8 projection preparation: schema 13 retains aliases and records one current
  CLAIMED/ACTIVE/RETIRED incarnation per person. Claims commit before future Paper
  spawns; active absence never implies death. Pending recovery requires a frozen entity
  and fully loaded spawn location. Actual deaths retain family/age/name with a
  month-zero-capable ledger. Planner/owner APIs implemented; Paper mutations/listeners
  remain unwired. Outcome explanations use immutable caches, not server-thread SQL.
- October 8 adapter integration: strict editable YAML, owner-thread asynchronous
  startup, read-only op/console debug commands. Loaded chunk snapshots seed automatic
  village import; no POI/chunk tickets/entity writes. Configurable bounded windows
  must have loaded coverage and a clear matching-radius border to certify housing;
  otherwise evidence is partial. This is a prototype observation heuristic, not a
  new locked village definition or permission to edit player blocks. See
  PAPER_VILLAGE_OBSERVATION.md for budgets, snapshot limits and remaining projection work.
- Calendar foundation: schema 10 saves elapsed-time anchor, fractional month and
  queued demographic target before bounded population catch-up. Backward clock
  changes freeze accrual; caller-supplied caps report discarded time. Cadence
  changes require explicit transitions. No runtime defaults or scheduler enabled.
- Work foundation: schema v9 records resident skill experience totals; qualification
  requirements are immutable caller inputs and explain failures. No employment or
  fake businesses are created. Skill units/learning rates, actual hiring/prosperity
  remain later roadmap work. Job foundation does not complete the economy/business phases.
- Migration prototype (editable, not canon): 0.02 monthly chance for an observed
  housing-pressured household; move the living household together into vacant
  sufficiently sized same-world housing. External adult arrival chance 0.005 per
  receiving settlement/month with observed space. Unknown housing/worlds block moves.
  IDs/family links persist; events preserve origin/destination. Schema v8 retains old
  history; replay 3 rejects older clocks/settings. No silent live-save transition.
- Schema v7 adds a global monthly demographic clock (replay 2), observed housing
  capacities and birth/death events. No silent transition from age-only clocks;
  mixed engines/settings changes rejected. Runtime policy changes require an explicit
  recorded transition. Snapshot/progress/events save atomically before live scheduling.
- Experimental demographic prototype: 12 monthly steps/year, adulthood 18, parent
  ages 20–40, senior threshold 60; eligible-pair birth chance 0.015/month, baseline
  death chance 0.0001/month plus 0.0005 per year beyond 60. Editable policy inputs
  under user authorization, not locked canon. Births require observed housing space
  and living unrelated partners. Runtime cadence, seeding and housing observation
  remain unwired. See POPULATION_SIMULATION.md for exact limitations.
- Schema v6 stores opt-in age-only clocks, seed/progress and replay version 1.
  Aging and clock changes share a transaction. Future demographic/calendar rules
  require a distinct replay convention; no automatic live scheduling chosen here.
- SQLite schema versioning uses `PRAGMA user_version` with ordered transactional steps: base settlement fields (1), population (2), beds (3). Unversioned prototype schemas are adopted by inspecting column presence, with no destructive reset. Newer versions are rejected rather than downgraded. This changes persistence mechanics, not gameplay rules.
- Active checkout relocation outside OneDrive is recommended but not performed. Do not mistake Git ignore rules for cloud-sync exclusions or copy an open SQLite database as a consistent backup.
- Headless runner checkpoints carry seed, completed logical steps and immutable caller state. Per-step randomness is derived independently to preserve outcomes across batch sizes; budgets cap steps, not CPU time. The synthetic harness does not establish any calendar ratio, birth/death rate or settlement simulation rule. Persistent replay versioning remains future engineering work.
- Schema version 4 stores resident/household identities separately from the old census. Age is an unconverted logical step count; optional household means unassigned housing. Deceased records are retained. No residents are inferred from old counts, no housing capacity is chosen and no implicit settlement transfer is supported. Family, occupation, seeding and demographic rules remain separate upcoming work.

Codex may choose internal implementation details that do not change gameplay intent; document the choice. For consequential open rules, present 2–3 concrete options with tradeoffs and a recommendation; wait for user approval. Add approved outcomes here and update other canonical docs. Do not present hypothetical illustrative numbers as settled decisions.


## October 8, 2026 — approved configurable economy applied to live food

Within the approved prototype settings authority: sixteen planted crops per worker,
32-block horizontal/16-block vertical household observation, one-minute food periods,
and saved adult/child demand now drive live accounting. Capacity evidence does not
claim ownership, harvest blocks or create businesses. Missing/shared evidence pauses
work; entirely unloaded villages retain last reliable capacity. Recorded settings
boundaries apply forward, preserve history/reserves and skip elapsed time at retuning.
Catch-up uses current saved residents rather than exact historical event interleaving.
These prototype approximations are documented in LIVE_FOOD_ECONOMY.md, remain editable,
and do not revise locked design principles. Household rationing, treasury transactions,
food-driven mortality and player merchant rules remain separate future work.


## October 8, 2026 — household needs and treasury boundaries

Engineering implementation records existing household food demand and observed
housing per completed economic period, with shared village food condition separately
identified. It does not assign individual rations or a numeric prosperity score.
Equal occupancy follows the existing prototype's sufficient-capacity convention;
unknown housing is never measured zero. No housing or migration rule is changed.
Treasury uses exact hundredths of abstract Trade Value, with explicit source/reason
and retry-safe operation IDs. No live treasury is initialized or credited by guesswork.
Funding, rationing, food-linked population effects and full prosperity rules remain
open implementation/design work; this accounting does not choose them. See
HOUSEHOLD_NEEDS_AND_TREASURY.md. Existing configurable prototype authority is retained.


## October 8, 2026 — prototype priority decisions

Under approved configurable economy settings, nonbinding priorities use recorded
unmet food and observed crowding. Defaults: review every five economic periods,
food weight ten, severe multiplier three, housing weight six, monitor weight one,
reasonable-choice threshold 25% of best feasible weight. Pressure appearance,
escalation and recovery trigger earlier review. These are editable prototype planning
weights, not locked government/leader rules or completed project execution.
Seeking food/investigating housing require evidence but do not purchase goods,
create suppliers/jobs or authorize construction. Actual action feasibility must later
include real funds, counterparties, labor/materials and safe sites. Stored replay-one
inputs preserve original explanations and apply tuning forward. See VILLAGE_PRIORITIES.md.


## October 8, 2026 — explicit food receipt boundary

Engineering delivery accounting accepts only an explicit trusted source/amount and
unique operation ID; it does not establish physical goods or invent counterparties.
New goods become eligible after the queued target at arrival, preserving historical
catch-up periods. Receipt use shares the food/needs/priority transaction. The cap of
64 receipts per village period bounds SQL work; deferred receipts are retained.
No player-facing donation, item conversion, payout, transport or reputation rule is
chosen here. A future bridge must establish actual goods/consent and a recoverable
inventory handoff before exposing commands. See FOOD_RELIEF.md. This preserves
approved configurable economy authority and the canonical roadmap.


## October 8, 2026 — read-only player aid preview

Within approved configurable prototype economy authority: ordinary wheat/carrot/
potato/beetroot each preview as one food unit; bread three, baked potato two.
Radius defaults 32 blocks in three dimensions around a saved living household home
in the same permanent world. Values/range/enabled are editable in optional economy.yml
section; incomplete/invalid edits fail, absent old-file sections preserve defaults.
Proximity selects an existing recipient, never establishes identity or player-build
consent. Equidistant towns require explicit IDs. Custom item metadata is rejected.
Read-only player permission is independent of protected admin debugging. No item
transfer, payment, merchant, reputation or new starvation rule is approved/implemented
by this preview. Real donation recovery needs reliable inventory-save evidence; the
pinned saveData call alone cannot provide it. See PLAYER_FOOD_AID.md.


## October 8, 2026 — confirmed donation handoff

User preferred interaction through villagers and authorized further Phase 2 work.
Prototype sneak-right-click preserves vanilla trading; the menu explicitly confirms
one whole ordinary held stack. No money/reputation reward or autonomous purchase is
invented. Existing owner files retain disabled donation behavior unless opted in;
new default configs enable it with separate player donation permission. Conversions
stay editable under approved prototype economy authority. An immutable player receipt
plus independently verified saved NBT guards SQL credit and restart retries. This is
pinned to tested Minecraft 26.2 players/data layout; formats/plugins/backups need
compatibility validation, not silent receipt deletion or refund. See FOOD_DONATIONS.md.

## October 8, 2026 — simulated household allocation prototype

Under the approved prototype economy scope, completed food periods now assign their
actual fulfilled units proportionally to configured household adult/child needs,
including a separate pool for residents without a household. Largest remainders
conserve whole units; exact ties follow permanent household UUID order, then the
unassigned pool. This engineering rule is documented and repeatable, not locked
social policy. Food-demand weights remain editable in economy.yml. A configurable
alternative rationing strategy and any demographic or prosperity consequences remain
future decisions. Current allocation is diagnostic and never claims physical meals.
Schema 19 preserves earlier allocation as unknown and records forward unmet streaks.
See HOUSEHOLD_NEEDS_AND_TREASURY.md for tie bias and other limits.

## October 8, 2026 — voluntary village food appeals

A selected SEEK_FOOD priority now exposes the actual unmet units from its latest
completed period in the existing village supplies menu. This scoped response uses
approved prototype food needs, priority weights and donation behavior; it chooses
no new payment, supplier, reserve target or demographic rule. Empty-hand viewing
is informational and cannot debit items. Schema 20 retains each new period's appeal
and observed pressure change, imports and production. Resolved pressure is not
claimed as causal success, and does not silently train decision weights. Paid trade,
actual village funding and adaptive outcome scoring remain separate future work.
See FOOD_APPEALS.md for observation/history and paused/catch-up limits.

## October 8, 2026 — finance questions and independent groundwork

Questions presented to the user: first village funding via voluntary player
contributions, a configurable one-time grant plus contributions, or waiting for
industry income; first payments via physical emeralds with an editable exchange
rate, barter, or an existing server currency plugin. Neither has been assumed
approved. Real business income stays dependent on actual future industries.

Independent engineering work provides exact Trade Value/whole-item affordability
quotes and internal treasury operations that share an owner's receipt transaction.
No live budget, income rule, exchange rate, payout or merchant acceptance is created.
Existing scarcity pricing and standalone treasury behavior remain unchanged.
See FINANCE_FOUNDATION.md. These calculations do not reserve or promise payment.

## October 8, 2026 — finance questions resolved by user

The user approved a moderate initial financial position as the basis for village
spending, followed by causal gains/losses, and both emerald payment and contextual
barter. This supersedes the pending funding/currency questions above. Opening
capital, exchange rates and barter prices should remain easy to tune in economy.yml;
no numerical default or permanent stack-for-stack exchange was specified by the user.

Implementation interpretation: initialize the village's spending account once at
its actual entry into economic simulation, with a documented configurable opening
amount. Entity spawning, projection recreation, reloading and repeated observations
must not mint more money or reset financial history. Detailed immigration capital
and new-household wealth rules remain later work; a newborn is not a fresh grant.
Starting household wellbeing/prosperity and spendable treasury stay separate.

Current code tracks housing where observed and has age/skill qualification machinery,
not persistent employers or workplaces. Food production uses evidenced farm capacity
and adults. Actual industry/workplace assignment and ownership belong to Phase 3.
A profession/name alone cannot establish goods stock or ongoing business income.
Barter must debit real saved, available supplies and respect village needs; it does
not spawn unlimited wood, gems, iron, coal or fish based on a claimed profession.
Purchases must debit available funds and preserve both goods and payment receipts.

This entry records product direction, not an implemented funding or payment feature.
Prototype values and payment/stock safety still require implementation and tests.

## October 8, 2026 — configurable opening funds implementation

Approved prototype authority sets 24.00 Trade Value per living resident once, in the
shared treasury, enabled in new economy.yml files. Existing owner files require adding
the optional finance section to opt in. Explicit opt-in seeds existing economic
runtimes without treasuries at activation using their current living population;
not a fabricated historical income. All initialized balances, including zero and
manual openings, survive retuning, spending, population changes and re-observation.
Zero grant deliberately initializes zero; disabled leaves absent accounts unknown.
Opening receipts retain the population/rate and share the owner transaction. Existing
schema 20 suffices; funding-only edits do not alter food-clock tuning/history.
See FINANCE_FOUNDATION.md and ECONOMY_TUNING.md. Player payouts and negotiation are
not implemented by this funding batch. No personal payroll/wallet design is added;
the master design's abstract household prosperity remains authoritative.


## October 8, 2026 — editable emerald food purchase prototype

Under the user's configurable economy prototype authority, new files enable food
purchases at 8.00 Trade Value per emerald, targeting three food-demand periods and
capping an exchange at 64 emeralds. Older files missing this optional section keep
sales disabled. Current scarcity value floors to actual whole emeralds, limited by
available treasury and unmet need; no guaranteed stack ratio or recurring income.
A village must actually seek food with reliable reports and no queued catch-up.
Existing reserved purchases and donations reduce available need. Whole ordinary
held stacks exchange for currency in the same slot after explicit confirmation.
Saved accepted terms remain immutable through retuning/disable and retry.
Schema 21 and saved player proof protect once-only funds/items/food accounting.
This is a verified emerald purchase prototype, not barter or bargaining, and does
not attribute reputation. Full cross-save backup/native-death/plugin compatibility
remain validation work. No personal wallets, industry income or salary rules added.


## October 8, 2026 — basic wellbeing prototype under configurable economy authority

The user authorized sensible editable economy prototypes. This batch adds food/
housing welfare, separate from treasury and personal wealth: neutral initial 50,
+1 for food plus known adequate housing, -3 for undersupply, -5 for recorded sustained
crisis, and -1 additional crowding, clamped 0..100. All rates are configurable;
crisis duration reuses the editable shortage duration. Existing owner files opt in
with the optional wellbeing section; no file rewrite or population penalties.
Families share the floor mean of actual residents' retained scores; newly assessed
people contribute the neutral initial score. At least one current resident's
recorded sustained food shortage establishes family FOOD_CRISIS. Actual membership
and personal shortage history are saved, because current household records describe
homes. Moves retain people's history; replacement occupants do not inherit it.
No industry/employment assumptions, individual wallets or payroll are introduced.
These are prototype welfare rules, not finalized prosperity balance or starvation.
See HOUSEHOLD_WELLBEING.md. No new user decision blocks further Phase 2 work.

## October 8, 2026 — configurable emerald bargaining prototype

The approved negotiation direction now uses `/civ bargain <emeralds>` and explicit
`/civ bargain accept` for a held ordinary food stack. Current scarcity, actual need
and finite funds determine acceptance/counter. Prototype editable patience is three
failed prices, cooldown 60 seconds, offer validity 45 seconds. Repeated counters
retain the original deadline; a new conversation after expiry starts fresh. Saved
per-player/per-village history prevents menu/reconnect/restart cooldown bypass;
quick menu purchases also respect active cooldowns. Failed alignment gives no
reputation penalty. A counter reserves no goods or money; accepted terms still
require safe purchase confirmation and current affordability. Old configs opt in
with the complete optional section. This is emerald food bargaining, not barter or
business-specific merchants. See FOOD_BARGAINING.md. No new design answer required.

## October 9, 2026 — protected barter accounting prototype

Under approved editable economy authority, first internal outgoing mappings are
timber/OAK_LOG and stone/COBBLESTONE at one commodity unit/item, keeping two units
per commodity, with a 64-item cap and three food-demand-period target. Only actual
idle saved materials can fund a quote; demand/shortage blocks export. The mappings
are intentionally limited until real Phase 3 industries exist. Existing opening
stocks are not recurring production. Schema 24 records reserves/refunds separately
from unchanged period reports; it does not fabricate past exports. Config starts
disabled; a live barter inventory adapter still needs implementation and validation.
No extra treasury funding, reputation, personal wallets or gameplay penalties.
See BARTER_ACCOUNTING.md. No new design answer blocks the next adapter work.

## October 9, 2026 — player barter adapter and trade prototype gate

The approved food-for-goods direction now uses `/civ barter timber|stone` to preview
an actual finite-stock offer and `/civ barter accept` to confirm. New files enable
the optional section; older files remain unchanged/disabled. Quotes expire using
the existing configurable bargaining offer deadline (default 45 seconds); this
does not require enabling emerald bargaining. Unaccepted quotes disappear on
restart without goods moved. The backend still enforces saved village cooldowns,
need/value and stock floors. Confirmation/recovery is independently saved-player
proof gated; no treasury money, recurring production or reputation is fabricated.
Quantity negotiation and industry-specific goods stay later work. Native exchange
and mixed two-village long-run conservation verify the Phase 2 player-trade prototype
gate, advancing reporting to 3/4 (75%). Remaining autonomous outcome learning needs
actual attributable evidence; no causal success is inferred merely from recovery.
Human client/plugin compatibility and survival balance remain release validation.

## October 9, 2026 — attributable aggregate import outcome evidence

An exact period-local comparison now measures imported food's marginal contribution
to prevented unmet demand and closing reserves, holding the saved opening inventory,
production and demand fixed. Existing transactional history supplies the inputs;
schema and priority replay version stay unchanged. No new rates or rules are needed.
This evidence is aggregate across all receipts, not proof the selected appeal caused
delivery or a basis for individual player rewards. Reserve-only arrivals are not
failed transactions; future reserve consumption is not credited as another import.
Bounded configurable action learning still needs explicit implemented feasibility,
response attribution and saved replay inputs before the final reporting gate passes.
No new user decision blocks the next scoped Phase 2 work. See FOOD_IMPORT_OUTCOMES.md.

## October 9, 2026 — configurable bounded food-priority learning

Under the approved editable economy prototype authority, new files now use a
12-economic-period lookback and 25% maximum extra food-priority weight. Existing
files opt in explicitly; both values zero disables. Only earlier imports matching
applied receipts contribute their aggregate immediate unmet-demand prevention.
No player absence or failed bargaining penalty, individual attribution/reputation,
imaginary supplier or automatic payment is introduced. Reserve-only arrivals are
not failures. Learning changes a planning choice, with real exchange feasibility
and player confirmation still enforced independently. Enabled scoring uses scaled
integer resolution and versioned saved evidence; legacy replay remains intact.
No consequential locked rule is changed. Broader explicit response comparison is
the remaining Phase 2 reporting scope; industries/construction/regional suppliers
stay in their canonical later phases. No new user answer required. See
PRIORITY_LEARNING.md for prototype settings and limits.

## October 9, 2026 — Phase 2 feasible response prototype gate

Approved configurable economy prototypes now include optional response weights:
donation 6, emerald 4, barter 4, wait 1, relative threshold 25%. Only actual feasible
capabilities enter the seeded draw. Preference publication does not compel player
donations or currency, reserve anything or guarantee a supplier. Every accepted
exchange retains its separate actual terms/receipt/proof and is checked again.
Pending food, finite funds, idle non-shortage materials, stock floors and supported
stack values explain exclusions. Unknown goods/treasury are not fabricated.
Schema 26 retains immutable comparison/replay and old history without backfill.
Tests and native acceptance close the Phase 2 prototype gate, 4/4 (100%). Later
industry/construction/regional supplier work stays in its canonical phases. Human
client/compatibility, cross-save recovery, profiling/history retention and balance
remain release work; no consequential mortality/reputation rule added. No new
answer blocks the next scoped development work. See PHASE2_ACCEPTANCE.md.
