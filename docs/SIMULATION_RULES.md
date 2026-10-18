# Civilizations — simulation rules and testable examples

October18 forestry implementation: current positive canopy evidence, protected
sources, real funded customers, earned qualifications and existing capacity now
support automatic logging. Real-supply study pauses for unknown restart evidence;
confirmed source loss stops the linked opportunity. Actual paid timber/warehouse/
craftsman flow is verified without physical felling or land claims. Admin terms
remain editable; existing configs opt in. See FORESTRY.md for verified boundaries.

October17 forestry backend: saved capacity never refills on repeat opening or
retuning; earned active-world periods renew gradually and harvest under actual
qualified work/storage constraints. Real customers pay for owned delivered timber.
Explicit complete authoritative habitat loss/return preserves support identity and
never instantly refills stock. Automatic tree adoption is still disconnected.
See FORESTRY.md for current settings, migration and validation boundaries.

October17 quarry implementation: each current loaded-chunk stone/sand frame is
independent of neighboring and coal/iron frames. Real customers, qualifications,
food-protected shared grants and finite per-coordinate reserves gate automatic
startup. Existing productive mine stone counts toward demand; no invented sand
buyer. No physical excavation. See AUTOMATIC_QUARRIES.md for verified boundaries.

October 17 current implementation: bounded approved natural free-range adoption,
qualified/funded atomic herd startup, paid warehouse/crop-farm feed and timed
saved-herd production are now connected and verified. Protection, named/human/
unknown-origin exclusions and absent-world pause remain. See LIVESTOCK_ADOPTION.md
and PROJECT_STATUS.md; older dated notes below describe their earlier checkpoints.
Latest verified addition: paid milk/egg bakery recipes and collective food
customers; bread remains the fallback. See BAKERY_SUPPLY_CHAIN.md. Controlled physical
slaughter is now verified (schema90); see LIVESTOCK_SLAUGHTER.md. Authorized
bulk pen donation remains unfinished.

October17 implementation note: recorded paid births can now materialize on safe
loaded ground near players and reconcile saved maturity without another head,
goods or payout. Missing bodies remain unknown, never replacements. Initial
animal age and shared stale-view protection are also verified; live adoption and
production scheduling remain unfinished. See LIVESTOCK_NEWBORNS.md and
LIVESTOCK_SAVED_AGES.md for verification boundaries and interruption handling.

October16 approved livestock working radius: only animals positively observed
outside the configurable farm radius become unavailable for products/breeding;
positive return observations restore eligibility. Retain their ownership,
headcounts and aging. Unknown/unloaded observations do not change eligibility
or infer death. Other eligible animals continue; absent worlds still pause time.
Saved positive sightings and configured herd-work eligibility are verified by
1,739 Wrapper tests and a real-cow away/return/restart check. Automatic adoption,
newborn reconciliation and scheduled feed/output remain unfinished; see
LIVESTOCK_WORKING_AREA.md.

October16 approved unfinished-purchase rule: retain only the exact pending
purchase goods through player death until confirmation. Other inventory follows
normal death rules; confirmed goods lose the temporary protection. No broad
keep-inventory or invulnerability. The adapter is now implemented and verified by
Wrapper/native synthetic-player checks; see LIVESTOCK_MARKET.md. Human-client
death/crash compatibility remains release validation.

October16 approved milk-container rule: players must bring an empty bucket for
each milk bucket purchased. Fill only from recorded business milk stock under
agreed payment terms; never include a free iron bucket. Eggs need no container.
Stock-backed player trading is now connected; live adoption/feed/output remain
unfinished. See LIVESTOCK_MARKET.md for current implementation limits.

October16 approved village livestock product rule: initially buy/trade recorded
milk/egg stock; prevent separate vanilla milking and egg laying drops for saved
village-owned animals. Player-owned animals keep normal behavior. This grants
neither automatic adoption nor a completed product-trading UI. Player-bred animal
provenance remains protective; missing old tags cannot establish natural origin.
See LIVESTOCK_PROTECTION.md and PROJECT_STATUS.md for implementation/verification.

October13 subsequent approval: bakeries and the single general craftsman share
the existing protected village startup grants. Period/lifetime caps are common to
all eligible industries, not per-business allowances. Future bakery investment
must retain real funds for a meal customer after paying startup, use real protected
surplus ingredients and consider available qualified local food suppliers first.
Read-only prospects reserve nothing; actual founding rechecks skills and evidence.
See PROCESSING_WORKSHOPS.md; live founding/product defaults remain unfinished.

October13 approved bakery/craftsman markers: configurable existing smokers and
crafting tables are location evidence only, with no inventory or building ownership.
Four-kind observation/identity is now implemented; actual businesses, products and
safe replacement remain separate gates. Marker recognition is not construction.

October13 subsequent schema76: dedicated herds now persist young cohorts and
once-only production receipts. The backend consumes real business feed and credits
owned milk/eggs under actual qualified work and storage limits. It remains unwired
to live adoption, physical offspring/deaths and scheduling; slaughter is not exposed
until physical retirement prevents duplicate harvesting. Existing crop farms and
assets are retained. See LIVESTOCK_PROTECTION.md for current limits/verification.

October13 approved livestock ownership direction: automatic eligible free-range
adoption, protection for recorded player animals and uncertain existing pens,
and explicit authorized bulk pen designation. Protection/donation signs request
bounded enclosure previews and confirmation; fences/signs alone claim nothing.
Opening gates, escapes or broken signs do not erase saved animal ownership.
Current plain-Java eligibility/geometry/work calculations are foundations only;
saved protective pen footprints and initial animal rosters now persist atomically.
Simple flat-pen immutable Paper evidence passed native closed/open gate checks;
live sign interactions, simulation births and adoption remain unimplemented.
Subsequent schema74 records trusted initial animal ownership; automatic capture
and explicit authorized transfers are still unwired. Village herd breeding authority
was approved: simulation-managed breeding/slaughter for village-owned animals,
with normal player-owned behavior and actual deaths reducing herds. The village-only guard
and cohort calculator passed full/native checks; persistent herds, live simulation
births/adoption and product inventory integration remain next.
See LIVESTOCK_PROTECTION.md for limits and verification status.

October12 approved shoreline recovery: accidental workplace damage suspends unsafe
work while the business searches for a safe nearby bank beside its original saved
habitat or pursues a feasible protected repair/dock project in Phase4. Preserve
identity, workers, goods, cash and fish supply. Relocation never claims new water or
refills resources; unknown/unloaded evidence cannot establish damage. Automatic
saved shoreline recovery passed October13 verification; physical repairs remain
Phase4 work. See FISHERY_WORKPLACES.md for implementation limits.

October12 approved: villages maintain a configurable emergency food reserve based
on actual resident needs, purchasing real owned food with the existing bounded
village food budget. Stored/incoming/reserved food reduces orders; no invented
consumption or money. Startup grants still protect food and refuse actual shortages.
See VILLAGE_SUPPLY_CUSTOMERS.md for prototype settings and implementation limits.

October12 approved fishing direction: fisheries may start on safe existing
shorelines. Prioritize dock construction in Phase4 once resources/funding/labor
make it feasible; docks should substantially improve fishing efficiency under
editable terms. This is approved intent, not implemented construction or a locked
numeric multiplier. Habitat capacity/recovery and real-resource constraints remain.

These are design contracts, **not finalized numeric tuning values**. Avoid hardcoding guessed rates or thresholds as permanent canon.

## Population
- Residents are persisted individuals, with age, home/household, relationships, occupation, skills and history. Simulated headcount is not the number of currently loaded villagers.
- Population(t+1) = population(t) + births + immigrants − deaths − emigrants; immigration usually sourced from actual settlements with occasional external arrivals.
- Household availability drives housing pressure. Beds are evidence of housing capacity but do not necessarily imply one household per bed. Exact occupancy and equality threshold remain open.
- Jobs correspond to actual businesses/industries; age/skill and labor availability constrain hires/founders. Skills can change gradually with experience/training.
- Offscreen individuals age/change jobs/migrate through headless simulation. Nearby residents materialize with context-sensitive density; night should be quieter than markets at noon.
- Approved October 9: no civilization time is earned while the server is stopped
  or a saved world's UUID is absent. Population and economic state resume without
  absence catch-up. Unloaded chunks in a loaded world continue data-only simulation.
  Preserve identities/history and already earned pending work; never reset an old
  world merely because a different world is loaded.

## Resources and production
- October13 approved hybrid farm accounting: a configurable capped contribution
  serves village meals/reserves; additional future output is owned business stock.
  Collective purchases pay real producers for remaining food needs. No compulsory
  transfer of old stock, free cash, duplicated harvest or individual payroll.
  Livestock protects breeding reserves, consumes feed and breeds over time within
  capacity; slaughter reduces animals, renewable outputs do not. No spoilage.
- Approved October13: future grain ingredients are separate from generic food.
  Existing food/history stay intact and basic meals remain feasible without bakers.
  Observed wheat capacity cannot establish a harvested ingredient balance; actual
  staffed future harvest accounting and editable processing terms must supply it.
- Approved October 12: confirmed fishery habitat loss lowers effective capacity;
  retained fish = min(previous fish, new capacity). Restored capacity creates no
  fish immediately. Original support identity/abundance remain saved. Zero capacity
  pauses both catches and recovery; incomplete or unknown evidence leaves state intact.
- Approved October 12: suitable observed open water supports a renewable fish
  stock. Catches reduce it; gradual configurable recovery can restore an exhausted
  fishery. Actual staffed work, site/storage limits and real demand still apply.
  No recovery or catch is earned during server downtime or world absence. This
  direction is approved; FISHING_RESOURCES.md distinguishes saved stocks/timed
  business work from unfinished automatic observation and founding.
- Use abstract units of significant commodities, reserves and capacity. Supply = local output + imports + reserves released; demand = households + business inputs + construction + trade + events.
- Labels Severe Shortage / Shortage / Balanced / Surplus / Abundant derive from quantitative ratios and persistence; exact thresholds are tunable.
- Shortage consequences are causal, commodity-specific and time-dependent. Food shortages initially raise prices/import pressure; prolonged crises depress growth and may lead to mortality. Timber shortages defer building; iron shortages reduce tool/industry throughput.
- Resource deposits have plausible geological potential, exploration/discovery and gradual depletion. Physical mine/quarry/logging/farm must plausibly corroborate output.
- Approved October 9: mineral reserves never regenerate. Mining consumes finite
  extraction bundles whose ore/rock yields share labor, site and storage limits.
  Business-owned inventories and warehouse purchases conserve goods and money;
  pending cargo reserves destination space and is unavailable for resale before
  actual recorded arrival. No fake shopper or unexplained stock deletion is a
  substitute for household, industry or maintenance demand. Ratios remain editable.

October 9 approved industry direction: timed work, suitable evidence-backed sites
and editable abstract yields apply across industries. Crop/tree renewal is distinct
from finite minerals/stone/sand. Exploration spends limited work and can fail; hidden
geology is revealed only through actual exploration, not omniscient planning.
Expected real demand/funded projects plus an editable margin drive useful reserves;
disposal cannot invent income or customers. Closed businesses retain site/history.
Material resistance changes work cost, deposit yield changes finite abundance and
base value changes price; none silently changes saved old deposits. Physical mine
layouts, excavation, hazards and nearby work-point activity require later adapters.

## Decision Engine
1. Gather observable pressures/opportunities and prerequisites.
2. Generate feasible candidate actions (build housing, import grain, expand mine, repair bridge, do nothing, etc.).
3. Score urgency, benefit, costs, labor, geography, capacity, treasury, relations, government, leader traits, previous outcomes.
4. Use seeded bounded randomness to choose among **feasible** candidates; not always argmax.
5. Persist decision/reasoning and convert it into project/economic action/history.
6. Re-evaluate when facts change; avoid oscillating decisions every tick.

Explain output should show candidate feasibility, top contributing factors, selected action and reasons. Test that impossible actions are never selected regardless of seed.

## Wealth and trading
- Treasury is spendable settlement budget; prosperity is aggregate welfare/health. A city may have high prosperity and low treasury, or vice versa.
- Underlying Trade Value is not emerald-denominated; emeralds are commonly accepted and fairly stable. Goods may be bartered. Local prices reflect scarcity, availability and settlement needs.
- Civ-to-Civ shipments are abstract and update both parties/route state. Nearby caravans are occasional visible evidence, not mandatory entities traveling entire routes.

## Buildings and infrastructure
- Approved October 9: households and businesses consider suitable permitted vacant
  premises before new construction. Capacity/condition/function must meet needs;
  retained vacancy does not license unsuitable occupation or player-build changes.
- Projects require need/opportunity, feasible site, resources, labor and funding. Road access generally precedes construction. Buildings emerge in stages over gameplay-relevant time.
- Geography imposes construction cost: mild grading possible, major terrain generally detoured/terraced/bridged/tunneled with appropriate resources.
- Buildings and roads have persistent ownership/condition/importance. Maintenance deficits can cause gradual physical decay of Civ-owned blocks, never unapproved player builds.
- Roads improve with actual traffic and economic importance; no modern asphalt highways or vehicles.

## History, diplomacy and crisis
- Approved October 9: qualified eligible family, then qualified employees, may
  succeed an owner. Otherwise attempt an affordable qualified sale with editable
  valuation/deadline and separate persistent family estate proceeds. Failure closes
  operation while preserving business/site/remaining assets/history. Not implemented yet.
- Every notable event is structured with time, entity IDs, type and context. Significance derives from outcomes, not a direct random “legendary” roll.
- Neutral relations are common. Friendly/hostile shifts arise from aid, trade, resource disputes, disruption or attacks. Escalation to conflict is rare and contextual.
- Trajectory labels are computed descriptions. Mine depletion can cause unemployment→emigration→closures→maintenance decline; new industry, trade or player help can reverse it.
- Abandoned settlements remain discoverable with ruins and persistent history. Recovery is possible, not automatic.

## Scenario acceptance examples
- **Mikeville iron boom:** discovery→viable mine→employment→migration→blacksmith→surplus→trade→roads. None of those steps should be guaranteed solely by a scripted year.
- **Food crisis:** reserve drawdown/import search precedes population collapse; nearby supplier can help.
- **Mine exhaustion:** reduced production, employment loss and potential long decline; no instant forced collapse.
- **Player restoration:** repair a burned civic structure→reputation improves; town can recover if economic prerequisites exist.
- **Unloaded centuries:** loaded-world data-only simulation and accelerated tests
  produce deterministic events without force-loading chunks. Server downtime and
  absent worlds earn no centuries of progress; return reconciliation stays bounded.
- **Multiple seeds:** some towns prosper, some stagnate, some decline; no uniform mega-city outcome.
