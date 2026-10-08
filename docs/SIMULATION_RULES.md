# Civilizations — simulation rules and testable examples

These are design contracts, **not finalized numeric tuning values**. Avoid hardcoding guessed rates or thresholds as permanent canon.

## Population
- Residents are persisted individuals, with age, home/household, relationships, occupation, skills and history. Simulated headcount is not the number of currently loaded villagers.
- Population(t+1) = population(t) + births + immigrants − deaths − emigrants; immigration usually sourced from actual settlements with occasional external arrivals.
- Household availability drives housing pressure. Beds are evidence of housing capacity but do not necessarily imply one household per bed. Exact occupancy and equality threshold remain open.
- Jobs correspond to actual businesses/industries; age/skill and labor availability constrain hires/founders. Skills can change gradually with experience/training.
- Offscreen individuals age/change jobs/migrate through headless simulation. Nearby residents materialize with context-sensitive density; night should be quieter than markets at noon.

## Resources and production
- Use abstract units of significant commodities, reserves and capacity. Supply = local output + imports + reserves released; demand = households + business inputs + construction + trade + events.
- Labels Severe Shortage / Shortage / Balanced / Surplus / Abundant derive from quantitative ratios and persistence; exact thresholds are tunable.
- Shortage consequences are causal, commodity-specific and time-dependent. Food shortages initially raise prices/import pressure; prolonged crises depress growth and may lead to mortality. Timber shortages defer building; iron shortages reduce tool/industry throughput.
- Resource deposits have plausible geological potential, exploration/discovery and gradual depletion. Physical mine/quarry/logging/farm must plausibly corroborate output.

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
- Projects require need/opportunity, feasible site, resources, labor and funding. Road access generally precedes construction. Buildings emerge in stages over gameplay-relevant time.
- Geography imposes construction cost: mild grading possible, major terrain generally detoured/terraced/bridged/tunneled with appropriate resources.
- Buildings and roads have persistent ownership/condition/importance. Maintenance deficits can cause gradual physical decay of Civ-owned blocks, never unapproved player builds.
- Roads improve with actual traffic and economic importance; no modern asphalt highways or vehicles.

## History, diplomacy and crisis
- Every notable event is structured with time, entity IDs, type and context. Significance derives from outcomes, not a direct random “legendary” roll.
- Neutral relations are common. Friendly/hostile shifts arise from aid, trade, resource disputes, disruption or attacks. Escalation to conflict is rare and contextual.
- Trajectory labels are computed descriptions. Mine depletion can cause unemployment→emigration→closures→maintenance decline; new industry, trade or player help can reverse it.
- Abandoned settlements remain discoverable with ruins and persistent history. Recovery is possible, not automatic.

## Scenario acceptance examples
- **Mikeville iron boom:** discovery→viable mine→employment→migration→blacksmith→surplus→trade→roads. None of those steps should be guaranteed solely by a scripted year.
- **Food crisis:** reserve drawdown/import search precedes population collapse; nearby supplier can help.
- **Mine exhaustion:** reduced production, employment loss and potential long decline; no instant forced collapse.
- **Player restoration:** repair a burned civic structure→reputation improves; town can recover if economic prerequisites exist.
- **Unloaded centuries:** simulation produces deterministic events without spawning mobs or force-loading chunks; return reconciliation stays within tick budget.
- **Multiple seeds:** some towns prosper, some stagnate, some decline; no uniform mega-city outcome.
