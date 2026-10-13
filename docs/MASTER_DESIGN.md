# Civilizations — canonical master design

October13 subsequent approval: bakeries and the single general craftsman may use
the existing shared startup-grant limits and real food-protected village budget.
No separate industry allowance or new money. Installed settings remain unchanged;
qualified owners, actual inputs/customers and safe workplace acceptance still apply.
Read-only bakery prospects are a prerequisite, not automatic live founding.
See PROCESSING_WORKSHOPS.md for current implementation and verification boundaries.

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

**Vision:** A persistent, autonomous Minecraft civilization simulation, not a village house-spawner. **We write the rules; the world writes the story.** The player participates in a world that would continue developing without them. *Truman Show principle:* simulate enough truth that the visible world plausibly supports everything abstracted behind it.

## Locked design laws
1. Simulate causes, not predetermined results; interacting systems produce unexpected but intelligible histories.
2. Simulate globally; materialize locally. Offscreen settlements are data, not thousands of ticking villagers or loaded chunks.
3. Physicalized abstraction: economic units, resources and trade may be abstract, but consequential industries, buildings and routes have visible evidence.
4. Player is not god. Influence is earned per settlement through trade, aid, harm, construction and reputation.
5. Player-built blocks are sovereign: no autonomous alteration of confidently player-built work without opt-in.
6. Persistent UUID identity for settlements, individuals, businesses, sites and significant infrastructure; names/borders/governments can change, history remains.
7. Specialization is an outcome of geography, investment, opportunity and history—not a fixed settlement class.
8. Geography creates constraints and opportunities, not destiny. Terrain may be adapted but not ignored.
9. Randomness chooses among reasonable alternatives, never impossible or nonsensical ones.
10. Uneven outcomes are desirable: many modest villages, a few large cities, occasional collapse and resettlement; rare 500+ population hubs.
11. Java + SQLite + Paper run everything without an LLM. Chronicler/T.O.D.D. may later narrate structured events but is optional.
12. Gameplay believability outranks strict historical timescales. Distinguish civilization calendar from construction/economic pacing.
13. Default diplomacy is uneventful; meaningful circumstances cause escalation, not arbitrary border contact.
14. The system should allow outcomes the designers did not explicitly author.

## Settlement identity and discovery
- Each settlement has a permanent UUID, historical record, name(s), location/footprint, world identity, residents, businesses, resources, buildings, territory and relationships.
- Vanilla village/POI discovery can seed a settlement, but discovery must not equate arbitrary player coordinates or a 128-block region to permanent identity.
- Organic footprints and functional districts develop from roads, terrain, homes, commerce, farms, mines and public structures.
- Settlements can rename, grow, split/merge (rarely), decline, abandon and resettle while preserving historical identity. Mergers preserve predecessor history.
- Exact POI-clustering, center evolution, overlap and player-base recognition rules remain open implementation/design decisions.

## Population, people and households
- Every simulated resident is a persistent individual in data: name, age, family/household, home, occupation/skills, basic prosperity and notable events.
- Simulated population is authoritative; physical villagers are a contextual subset of those real residents, never disposable invented strangers.
- Population changes through births, deaths, immigration and emigration. Most migration connects real settlements; occasional external arrivals avoid a closed world.
- Families track parent/child, spouse/partner, siblings; households track shared housing. No dating/divorce/emotional Sims simulation.
- Housing demand is household-driven, not simply one bed per resident. Labor uses age, skills, opportunity and jobs at real businesses. Employment influences prosperity and production, not individual salary/payroll systems.
- Visible activity scales with day/night, markets, events, population and performance: a 300-resident city should look meaningfully busier at noon than midnight.

## Needs and economy
- Population, industries, construction, trade and events create demand; production, imports and reserves create supply.
- Track important commodities (e.g., timber, food/grain, iron, coal, stone, livestock products) separately; aggregate trivial subcomponents. Display Severe Shortage / Shortage / Balanced / Surplus / Abundant.
- Shortages are resource-specific and duration-sensitive. Food crisis gradually affects prices, growth, migration and only eventually mortality; timber delays construction; iron constrains industry.
- Production depends on labor, capacity, infrastructure and resource potential. Geological deposits are abstract but physically plausible; seams may be discovered or depleted.
- **Treasury** = liquid settlement spending capacity; **prosperity** = broader economic health. Household prosperity is abstract, not bank accounts.
- Trade uses abstract **Trade Value**, not emerald-only accounting. Emeralds are broadly accepted/default; barter is supported. Player-facing prices respond to local scarcity. Each merchant offers a contextual subset, not the entire commodity catalog.

## Decision Engine
- Systems generate pressures and opportunities: overcrowding, food shortage, surplus iron, unemployment, route traffic, mine discovery, infrastructure failure.
- Generate feasible candidate responses; score urgency, expected benefit, cost, labor, materials, terrain, relationships, government/leader traits, past outcomes and bounded randomness.
- Do not always choose the top score; sample among reasonable candidates. Expose explainability and repeatable seeded tests.
- Decisions create projects and historical events; prior outcomes influence future weights without machine learning.

## Industry and business
- October13 approved hybrid farming: bounded contributions toward actual village
  meals/reserves in exchange for protected tool support; future surplus belongs
  to the farm and earns real sales income. Additional food is purchased collectively.
  Preserve old public stocks/history and count each harvest once. Basic chicken/
  dairy livestock and one general craft workshop join P3; no spoilage or separate
  craft occupations. Breeding reserves, feed, time and space constrain slaughter
  and renewal. These new integrations are planned, not implemented by this note.
- October13 approved: track future grain ingredients separately for bakeries,
  preserve all existing generic food and its history, and keep basic meals feasible
  without a bakery. Wheat-field evidence alone is not harvested ingredient stock;
  food must never be silently reclassified into invented grain. See GRAIN_EVIDENCE.md and GRAIN_HARVESTS.md.
- October 12 approved: confirmed loss of supporting fishery water reduces habitat
  capacity and loses only fish above the reduced limit. Restoration permits gradual
  recovery without an instant refill; unknown evidence never establishes loss.
- October 12 approved: fishing uses habitat-backed renewable stock, gradual
  configurable recovery and temporary exhaustion under heavy harvesting. Catches
  require actual qualified work and storage; server/world absence pauses both
  recovery and harvesting. Visible fish entities are not required per abstract
  food unit. See FISHING_RESOURCES.md for current implementation boundaries.
- October 9 clarification: minerals are finite and non-renewable; discovery evidence
  represents a plausible deposit, not a one-block/one-item conversion. Ore and local
  rock byproducts share extraction work. Neither restart nor repeat discovery refills
  a saved deposit. Trees/crops follow separate growth rules.
- Abstract business inventories have finite storage. Local warehouses may buy/own
  goods and resell to shops/industrial customers using real purchasing funds. Goods
  in transit have one owner and cannot be sold at either endpoint before delivery.
  Storage buildings are Phase 4; regional routes are Phase 5. The economy should
  respond to resident/business demand as well as players, with recorded causal uses.
- Chains: mining→ore→blacksmith/tools; farming→crops→milling/baking; logging→timber→carpentry/construction; livestock→food/leather; fishing→food/trade.
- Businesses persist with name, founder/owner, industry, building/site, employees, capacity, health/prosperity and history.
- Founding requires plausible working age, skill/experience, capital/resources and opportunity. Businesses can thrive, compete abstractly, expand, shrink, close, change owners, pass to family/employees, relocate or be repurposed.
- Rare multi-settlement firms can emerge as significant historical exceptions. Closed buildings remain for reuse, preservation, decay or demolition.

October 9 approved lifecycle clarification (planned): eligible industry-qualified
family inherits first, then a qualified employee; otherwise family attempts a paid
sale to a qualified affordable buyer under editable prices/deadline. Proceeds stay
in a separate persistent family estate fund for later eligible business investment.
Unsuccessful sale closes operation while retaining site, assets and history.
Businesses/families prefer suitable vacant premises before new construction.
Player removal of supporting ore retires only the mined coordinate's source,
preserving the rest of the vein and already extracted goods without regeneration.

### October 9 industry development clarification

Resource development is bounded and suitable to its location across industries,
with editable non-1:1 yields and timed work. Exploration can fail and must reassess
within labor/material/distance budgets; miners do not know hidden ore locations.
Newly discovered finite reserves differ from refilling old deposits. Material
resistance, geological yield and price are separate settings. Stone quarries and
sand pits can be primary industries. Useful reserves grow with real expected demand
and funded work, within finite storage. Industry mixes emerge from opportunity,
history and competition for capital/labor, not rigid biome classes or a guaranteed
checklist. Nearby workplace activity uses actual saved employees. Mine layouts,
entrances/supports and work points are Phase 4; regional transport is Phase 5;
retained business/site history survives closure and later physical deterioration.
These clarify planned behavior, not features already implemented by the scanner.

## Construction and physical development
- Loop: need→proposed project→site evaluation→funding/materials/labor→visible staged construction→active use.
- Use authored structure templates with rotation/mirroring, palette/style variants, terrain/foundation adaptation and entrance/road metadata; do not generate arbitrary architecture from Java.
- Future in-game template toolkit: Creative-built structure, bounding selection, entrances, beds, storage, workstations, road connector and rotations, saved without recompiling Java.
- Land suitability scores slope, biome, water, roads, nearby uses, existing sites, territory, protected player structures, historic significance and environmental cost. Organic soft districts emerge; no universal grid.
- Roads generally precede buildings. Remote farms/mines/logging camps can create feeder paths. Grade small hills; terrace/detour/tunnel/bridge where justified; do not casually erase mountains, rivers or oceans.
- Building lifecycle: planned→under construction→active→aging→renovated/repurposed/abandoned/demolished/historic. Age alone does not force destruction. Gradual physical decay is limited to Civ-owned structures.

## Roads, transport and trade
- Roads/bridges persist as infrastructure with endpoints, type, builder, usage, age, condition, importance and history. Trails improve to dirt/gravel/paved medieval-style streets as justified; never modern highways.
- Bridge versus detour uses terrain, traffic, value, treasury, labor and engineering capability. Routes have reliability, safety and efficiency; disruption changes trade.
- Long-distance caravans/trade resolve in data. Near players, occasional merchants, pack animals and route activity visibly corroborate the system. Player-built routes may change path preference.

## Territory, diplomacy, government
- Organic soft sovereignty grows from settlement core, homes, roads, outposts, farms and resources. Civilizations' rules constrain NPC decisions, not player block placement.
- Relationships range hostile→unfriendly→neutral→friendly→allied with causal history; dependencies may be asymmetric. Aid, trade and infrastructure matter; treaties are exceptional and practical.
- Lightweight government types (council, mayor/republic, monarchy, autocracy; merchant council possible later) mildly modify priorities/stability/decision pace, not detailed elections, taxes or ideologies.
- Leaders are persistent residents with 2–3 modest traits and government-appropriate succession. Major changes become historical events. Player leadership requires exceptional earned standing.
- Rare settlement incorporation/mergers preserve identities and histories.

## History, growth, decline and events
- Record structured events with timestamps and stable entity IDs, not only generated prose. Importance emerges from actual deeds, longevity, crises, founders, structures, businesses and routes.
- Historical significance may progress Notable→Significant→Historic→Legendary, enabling archives, landmarks, preservation, monuments and museums when warranted.
- Trajectory labels (booming, growing, stable, stagnating, declining, collapsing, abandoned) **describe** interacting conditions, not dictate them.
- Maintenance burden matters: declining revenue/labor weakens roads, bridges and buildings, causing feedback loops. Abandoned settlements leave physical ruins, keep their history and can be resettled.
- Rare context-eligible events: fire, flood, drought, crop failure, industrial collapse/discovery, leader death, migration, unrest, route disruption, disputes and war. Conflict escalates gradually; offscreen warfare resolves abstractly, with limited nearby physical combat.

## Players and other plugins
- Per-settlement reputation responds proportionally to trading, donations, infrastructure repairs, damage, violence and construction. High reputation unlocks influence rather than omnipotence.
- Player settlements can join regional economy, accept residents/businesses and trade; autonomous block placement in player areas requires explicit opt-in development zones and styles.
- Simulation needs may create real opportunities for players (timber delivery, bridge repair) without fake quest requirements.
- Civilizations emits structured events for future optional Chronicler/T.O.D.D. integration; no hard dependency.

## Performance and scale
- Active nearby: contextual NPCs, schedules, building progress, merchants, visible events.
- Loaded distant: minimal physical work.
- Unloaded: data-only, no forced chunk loading.
- October 9 approval: civilization pauses while the server is stopped or its world
  is absent. Unloaded chunks inside a loaded world continue the abstract simulation.
  Returning to an old world resumes its saved state without absence catch-up.
- Reconcile already earned work upon return in bounded tick budgets. Target many settlements (20+), with rare very large cities, stress-tested rather than hard-coded population uniformity.
