# Civilizations Player and Server Guide

Living draft 0.5 | 20 October 2026 | Player and server owner edition

Civilizations is a Minecraft server plugin that gives villages persistent people, households and a connected local economy. Businesses need workers, supplies, storage and paying customers. Your food deliveries and purchases can help towns operate.

This guide explains what you can do now, why the systems behave as they do, and how they connect. It also documents the intended P4 housing and construction controls. Planned features are labelled throughout; their proposed signs and commands are not working instructions.

### TL DR

- Players join the server with its matching Java Minecraft client. No Civilizations client mod is required.
- Villages containing villagers and beds are recognized automatically after complete loaded-area observation. There is no join command.
- Hold supported plain food and sneak-right-click a managed villager to open village supplies. Wait for the final donation or trade confirmation.
- Use /civ market near a qualifying livestock business to buy stocked milk or eggs. Bring your own empty bucket for milk.
- Industry simulation works today. Ordinary automatic construction, apartments, building donations and repossession are unfinished.
- Server downtime and absent worlds pause new simulation time. An unloaded chunk does not prove death or destruction.

### Read the status first

Available means implemented, subject to settings and eligibility. Development only means backend or disposable-server work that is not ordinary gameplay. Planned means accepted direction or a proposal whose controls do not exist yet.

Snapshot: P3 prototype gate complete; P4 foundation at 4 of 16 checklist items. Save schema 108. Baseline 8ca9f86 plus verified covered-bed qualification; housing adoption remains unfinished. Human survival balance and other plugin combinations still need playtesting. This is a living draft, not a release certification.

## Chapter list

<!-- TOC -->

- 1 Your first village session
- 2 Settlements worlds and time
- 3 People households and jobs
- 4 Homes and apartments
- 5 Donate protect and reclaim buildings
- 6 Food reserves and needs
- 7 Donate sell bargain and barter food
- 8 Money ownership and succession
- 9 Businesses and supply chains
- 10 Resources production and storage
- 11 Livestock and product purchases
- 12 Protect an existing livestock pen
- 13 Construction and shared structures
- 14 Install update and preserve a server
- 15 Tune the simulation with text files
- 16 Troubleshoot and report problems
- 17 Player command glossary
- 18 Admin diagnostic command glossary
- 19 Protection preview and recovery commands
- 20 Terms and a connected village example
- 21 Roadmap and maintaining this guide
- 22 Protect a building plot

## 1 Your first village session

Status: Available, with individual features controlled by server settings.

### Find a recognized town

Visit a village containing actual villagers and beds. Player-built village areas can participate too. Keep the area loaded while observation finishes. Default recognition needs at least two villagers and two bed heads, with sufficient surrounding evidence. A running calendar alone does not mean a town was enrolled.

An admin can run /civ debug status and /civ debug observation, then /civ debug villages to find the full village ID. Saved living residents should appear after successful enrollment. Recognition and permission to adopt a building are separate things.

### Try a small delivery

Hold ordinary bread, wheat, carrots, potatoes, baked potatoes or beetroot. Sneak-right-click a managed villager, inspect village supplies, and donate a small stack. Wait for the final confirmation. Delivered food becomes available in a future food period; the effect need not be immediate.

When a village needs food and can afford it, try selling a small stack for emeralds. Keep the offered stack in your main hand until completion. Large stacks may exceed demand. A refusal can reflect a genuine lack of need, money or available goods.

### What to expect today

Saved families, shortages, businesses, goods and money can change. Existing industry works abstractly without excavating mines or constructing premises. Do not yet expect a new bakery building to appear during ordinary gameplay.

Related chapters: people, food, trading and troubleshooting. Sources: PHASE3_PLAYTEST.md, VILLAGE_RECOGNITION.md and PLAYER_FOOD_AID.md.

## 2 Settlements worlds and time

Status: Available.

### Saved identity

Settlements and residents have permanent IDs. Matching uses a world's permanent ID and saved bed/villager evidence. Revisiting the same village should not create a second town. Ambiguous overlaps are diagnosed instead of silently merging settlements.

A genuinely new world has a different identity. Returning to an original saved world can resume its towns if the matching plugin database and world files were preserved together. A world name alone is not its identity.

### Different clocks do different jobs

Population months control aging and demographic updates. Food periods control needs and food accounting. Industry periods control productive work. Their configurable speeds do not have to match Minecraft daylight or each other.

Stopping the server or removing a world earns no new time for that world. Already earned queued work can still need bounded processing. The shared calendar is an administrative date; it does not mean every absent resident aged by that amount.

Unloaded chunks in a present world can receive data-only simulation. Physical effects need suitable loaded conditions. The plugin does not force-load terrain for simulation.

### Practical effect

A faster food clock accelerates both consumption and production opportunities. A faster population clock changes generational pace. There is no universal speed button, and restarting never refills reserves or cash.

Related chapters: resources and administration. Sources: ONLINE_WORLD_PAUSE.md, CIVILIZATION_CALENDAR.md and VILLAGE_IDENTITY.md.

## 3 People households and jobs

Status: Persistent population and employment available. P4 physical home assignment is unfinished.

### Families and households

Residents have saved ages and status. Households group people for living and needs; family relationships record partners, parents, children and siblings. Shared housing does not invent parentage. Configured rules support aging, births, deaths and migration, with editable prototype rates.

Adult partner pairing is configurable. Existing children are not assigned invented parents. A visible body being missing or unloaded is not enough evidence to erase a resident.

### Employment has requirements

Owners and workers need appropriate age, saved skills and location. Study and actual work can build experience using real supplies and configured limits. A miner does not become a qualified shopkeeper merely because the job is vacant.

Business ownership and employment are separate. Eligible residents can own multiple businesses under the approved direction while holding only one worker job. This creates no extra labor, capital or skills. Existing workers are not forcibly transferred whenever another business scores higher.

### How it connects

Population determines food demand, housing needs and the potential workforce. Qualifications constrain which businesses can open. Households need suitable space; a free bed does not automatically justify splitting a family.

Related chapters: housing, business ownership and industry. Sources: POPULATION_RUNTIME.md, PARTNERSHIP_FORMATION.md, WORK_FOUNDATION.md and RESIDENT_STUDY.md.

## 4 Homes and apartments

Status: P4 design and planning foundation. Live apartment recognition and assignment are not implemented.

### Separate the land building and homes

A plot is surrounding land. A building is the physical structure. A dwelling unit is one household's home inside it. An apartment building can contain several independent units plus shared stairs, corridors and a courtyard. Several bedrooms in one family home do not automatically make several apartments.

The initial direction is village ownership without rent or landlords. A household acquires occupancy through assignment, as with a village-owned home. This does not establish a private sale price or transfer property title; private purchasing needs a separate economic decision.

### Recognizing a unit

Authored plans would declare units, beds and entrances. Donated buildings would be inspected, with proposed units previewed for confirmation. Editable three-dimensional boundaries are proposed so upstairs and downstairs apartments can share the same ground footprint.

A unit needs suitable usable capacity, a reachable entrance and non-overlapping private space. Shared access should not require crossing another household's apartment. Exact validation controls remain to be built.

### Inspect saved housing

/civ building inspect <building UUID> reviews an already registered housing layout in your current world. Only its recorded donor or an administrator may inspect it. It reports accessible declared beds and reasons such as broken pairs, missing cover or unreachable units. Missing chunks produce an unverified report, not lost housing.

This command cannot register a new house. Donation controls remain unfinished, so most existing servers have no eligible saved layouts yet. Covered beds are required by default; both halves need overhead cover inside the declared building. Admins can change require-covered-beds under building-protection in config.yml. Inspection still grants no occupancy or usable capacity.

### What empty means

A vacancy should mean no household is assigned, not that nobody is standing inside. Going to work, logging out or unloading a chunk must not create a vacancy. Suitable permitted vacancies should be reused before new construction. Families should fit as a whole.

Related chapters: donation and construction. Sources: DECISIONS_AND_OPEN_QUESTIONS.md, CONSTRUCTION.md and HousingProjectPlanner.

## 5 Donate protect and reclaim buildings

Status: Explicit off-limits plot protection is available. Building donation, apartment selection and housing reclamation remain planned.

### Explicit consent

Player-built homes, workplaces and decorative buildings are off limits without a clear confirmed donation. Beds, workstations and location inside a village are not consent. Old uncertain structures must not be guessed into permission.

Players will explicitly declare that an unrecorded building belongs to them; first donation will not require admin approval. This trust policy cannot override another recorded donor or saved protection. A sign alone grants nothing. The proposed flow is to mark the donation, declare its use, inspect the footprint and confirm its scope. Final sign names and commands remain unfinished.

### Boundaries and extra protection

For an off-limits plot, select opposite corners and preview its visible outline before confirming ownership. Chapter 22 gives the commands and optional protection sign. Selected X/Z columns are excluded at every height. Boundaries for donated buildings and separate apartments remain planned; more land does not create beds or production.

Saved protection survives sign removal. Private buildings still need explicit donation even without this extra marker. Protection governs Civilizations construction; it does not prevent other players editing or vanilla villagers walking through. Releasing an exclusion changes construction permission; housing reclamation remains planned.

### Repossession changes village capacity

Reclaiming a donated home should withdraw its village capacity while keeping the physical building intact. Households need another home. Reclaiming a workplace may pause business work or create demand for replacement premises. Residents, business assets and history remain saved; unrelated cash and goods do not automatically become the player's property.

Recorded-donor/admin confirmation, per-unit reclamation and permissions surviving accidental sign removal are proposed safeguards. A preview should show affected households before confirmation.

Related chapters: housing and protection. Source: DECISIONS_AND_OPEN_QUESTIONS.md.

## 6 Food reserves and needs

Status: Available. Automatic starvation mortality is not enabled in the current economy prototype.

### Count usable food

Food units are abstract measures for village accounting. One bread item can provide several configured units. Food units are not emeralds or population capacity. Each food period compares adult/child demand against production, reserves and eligible deliveries.

Reserves buffer poor output. Sustained shortfalls create shortage information, price changes and response pressures. A fully supplied period can clear a shortage streak. A shortage report does not cause instant deaths.

### Farmers contribute and earn income

Hybrid farms contribute a bounded share toward village meals and protected reserves in exchange for supported tools. Remaining harvest can be sold. Village customers and other businesses pay with real funds; the village cannot silently take every unit of output to solve a shortage.

Different sources can meet general food demand, but recipes still require actual ingredients. Wheat in a field is not already stored grain or bread. Bakeries, livestock and fisheries connect through goods, ingredients and purchases.

### How players affect the system

Confirmed donations add future supplies. Affordable sales and barter exchange actual inventory. A village can refuse food because it has enough, lacks funds or barter goods, or cannot yet assess the offer. Split a large stack and read the explanation.

Related chapters: trade, industry and tuning. Sources: HOUSEHOLD_NEEDS_AND_TREASURY.md, HYBRID_FARMS.md and FOOD_DONATIONS.md.

## 7 Donate sell bargain and barter food

Status: Available when enabled and permitted.

### Donation or ordinary sale

Hold supported plain food and sneak-right-click a managed villager. Inspect the village-supplies menu, choose an action and wait for final confirmation. A preview or initial click is not proof that goods or money were credited.

Keep the offered stack in your main hand until completion. Custom metadata can make an item ineligible. /civ aid previews the village and held food; it does not donate or remove anything.

### Negotiate the whole stack

Use /civ bargain 5 to request five emeralds for the whole held stack, not five per item. The village may accept, counter or refuse based on value and its actual budget. /civ bargain accept accepts a current offer before expiry. Too many failed offers can end negotiations under the configured limits.

For goods, use /civ barter timber or /civ barter stone, inspect the current offer, then /civ barter accept. These use supported real stocks; they are not a universal exchange for every Minecraft item.

### Recipient and recovery

An optional full village UUID can resolve nearby ambiguity. It does not bypass distance, world or readiness checks. If confirmation is delayed, keep inventory stable and read messages before repeating the action. Do not delete player data or SQLite rows to clear a pending transfer.

Related chapters: command reference, money and troubleshooting. Sources: PLAYER_FOOD_AID.md, FOOD_BARGAINING.md, PLAYER_BARTER.md and FOOD_PURCHASES.md.

## 8 Money ownership and succession

Status: Business finance and turnover implemented. Housing rents and landlord systems are deferred.

### Separate funds

The village treasury is its spending budget. Businesses own their cash and goods. Saved family estates preserve approved family proceeds. None is a universal personal bank account, and treasury balance is not a prosperity score.

Emerald payments and internal values connect through configurable conversion terms. Prices, reserves, grants and upkeep are editable. Replaying a saved transaction must not pay twice or create goods.

### Real startup and operating costs

A business needs a suitable opportunity, qualified people, supplies, customers and capital. Limited village grants can help eligible firms while protecting food funds and sharing spending caps. A retry is not another grant.

Tools can wear out and need paid replacements. Limited storage upgrades require real money and materials. Configured upkeep contributions can return spare business cash to village upkeep while preserving operating needs.

### When an owner dies

Succession checks eligible qualified family first, then qualified current employees. If neither can inherit, eligible funded buyers can get a sale opportunity. Approved sale proceeds enter saved estate accounting. Unsold firms may close while their assets, sites and history remain recorded. Qualified staff can continue a listed firm under the approved policy.

Related chapters: people and industry. Sources: BUSINESS_SUCCESSION.md, AUTOMATIC_BUSINESS_RESALE.md, STARTUP_GRANTS.md and UPKEEP_CONTRIBUTIONS.md.

## 9 Businesses and supply chains

Status: Saved industry implemented. Physical P4 premises, excavation and felling remain unfinished.

| Industry | Contribution |
|---|---|
| Crop farm | Village food contribution, paid surplus and configured grain |
| Livestock farm | Feed-backed milk, eggs, wool and managed herd products |
| Fishery | Fish/food under work, habitat and renewable-stock limits |
| Logging | Timber from suitable sources with bounded renewal |
| Mine | Finite mineral output and configured accompanying material |
| Quarry and sand pit | Finite stone or sand from suitable evidenced sources |
| Blacksmith | Configured toolkits supporting business upkeep |
| Bakery | Food from actual configured ingredients |
| General craftsman | Configured storage fittings and supply-chain inputs |
| Warehouse | Paid purchase, storage and local movement of owned goods |

### More than a job label

Production depends on skills, workers, evidence, money, inputs, tools, storage and paying demand. Existing smithing tables, barrels, smokers and crafting tables can be configured workplace markers. They grant no ownership of player buildings or permission to use chest contents.

Implemented bakery support does not enable every possible cake recipe. The blacksmith is not yet a complete physical sword/armor shop, and the craftsman is not a universal furniture retailer. Consult the actual recipe settings and business report.

Related chapters: resources and finance. Sources: PHASE3_PLAYTEST.md, PROCESSING_WORKSHOPS.md and WORKSHOP_PRODUCTION.md.

## 10 Resources production and storage

Status: Abstract production and persistence available. Physical excavation and felling are unfinished P4 work.

### Not one block per item

Observed resources support configurable abstract yields extracted over time. A mineral block can back many units, but its reserve is finite. Re-observation, restarting or changing a default must not refill an existing source.

Removing supporting ore loses the remaining share behind that coordinate; neighboring blocks keep their shares. Quarries and sand pits also need suitable finite sources. Selling warehouse goods does not respawn ore.

Trees and fisheries have separate bounded renewable stocks. Habitat, work and policy affect output and recovery. Incomplete or unloaded observations do not establish habitat destruction.

### Goods have owners and capacity

Abstract storage has finite saved capacity. A miner owns its output, a warehouse pays for goods, and a workshop buys inputs. Cargo has an owner and destination and cannot be spent before delivery. Simulation does not rummage through an arbitrary player chest to fill an order.

Missing workers, space, tools, inputs, money or demand can slow or stop production. Older terrain or missed edits can leave resource provenance uncertain; diagnostics explain the pause. Do not reset the database to force adoption.

Related chapters: diagnostics and recovery. Sources: MINERAL_SOURCE_LEDGER.md, FORESTRY.md, FISHING_RESOURCES.md and RESOURCE_REVIEW.md.

## 11 Livestock and product purchases

Status: Guarded adoption, saved herd work and milk/egg retail implemented. Sheep/wool support is part of the P4 foundation.

### Animal ownership matters

Automatic recognition uses bounded evidence. Names, leads, player-origin evidence, saved protection and uncertainty can exclude animals. A huge unmarked pasture may extend beyond a local fence scan; explicit protection provides a safeguard.

Herd work needs suitable owned animals, real feed, labor and storage. Breeding/maturity rules and reserves protect productive populations. Positive evidence that an animal moved too far can pause its contribution without erasing ownership. Unloaded bodies are not proof of death.

### Buy stocked products

Use /civ market near a qualifying operating business. Bring plain emeralds, main-inventory space and a plain empty bucket for each milk purchase. Eggs need no bucket. Opening the menu quotes stock, not an infinite supply. The business receives actual confirmed payment.

Village-owned animals do not also give duplicate vanilla products. Village sheep have guarded shearing while their feed-backed wool is accounted for in business stock. Player-owned animals retain normal behavior. The retail menu currently covers milk and eggs, not all livestock products.

### Pending purchase goods

Only unfinished purchase goods receive temporary retention through death until confirmation. This is not general keep-inventory. Preserve their markers and saved evidence if recovery is needed.

Related chapters: pen controls and trading. Sources: LIVESTOCK_ADOPTION.md, LIVESTOCK_MARKET.md and LIVESTOCK_SLAUGHTER.md.

## 12 Protect an existing livestock pen

Status: Available pen controls. Separate building-plot protection is covered in Chapter 22; housing adoption remains planned.

### Inspect before confirmation

Place [Civ Protect] on the first line of a sign beside a suitable enclosed pen boundary. Stand inside on supported flat ground near it and right-click. An admin receives an area/animal preview and boundary particles. A sign alone claims nothing.

Use /civ pen confirm before expiry. Geometry, animals, position and permission are rechecked. The current prototype records the confirming admin as owner; it does not infer the original builder. Coordinate ownership with your server admins.

The enclosure must be finite and supported. A walkway fence does not claim everything behind it. Open, oversized, ambiguous or incompletely loaded geometry can be refused. Gates remain structural boundaries even when open.

### Find or release a record

/civ pen here shows active saved IDs. Use /civ pen release <pen UUID>, inspect the preview, then /civ pen confirmrelease. Saved owner/admin authority is required. /civ pen cancel cancels an unsubmitted preview, not a committed save.

Releasing an area does not erase recorded animal ownership. Breaking a sign does not silently remove saved protection. Pen protection also supplies construction exclusion evidence, but it is not a general building claim or anti-grief system.

Related chapters: planned building protection and commands. Sources: PEN_CONTROLS.md and PEN_PROTECTION_LIFECYCLE.md.

## 13 Construction and shared structures

Status: P4 in progress. Ordinary automatic physical construction is not enabled.

### Current foundation

Three original editable starter plans exist: path, timber storehouse and four-bed home. Admin catalogue/site previews are read-only. Frozen proposals, saved sites, paid supplies, earned deliveries and public stage labor have backend support. Disposable tests placed/restored a paid path and tested a home across restarts.

This is development evidence, not a player build command. A preview spends no money, starts no project and assigns no home. The physical development writer is restricted to marked disposable test servers.

### The intended chain

Need -> permitted vacancy review -> suitable site -> real funding/materials -> available labor -> staged work -> physical completion -> usable capacity. A draft or frame cannot count as a completed home.

Businesses are intended to fund their premises. Villages fund basic homes/local roads after protecting food and upkeep. Public-work accounting avoids double farm/study labor. Private employee diversion, automatic scheduling and normal-server write authority remain unfinished.

### Agreed structure catalogue

Housing and existing industries' premises are the functional core. Infrastructure includes local paths, docks, small crossings, steps, retaining structures, entrances, gates and modest walls. Civic targets include squares, parks/gardens, wells/fountains, market shelters, meeting/town halls and churches/chapels.

Physical churches, halls or walls do not by themselves introduce religion, government or warfare. Regional networks belong to later phases. Reuse, maintenance and deterioration should retain site/history rather than make old places vanish without explanation.

Sources: CONSTRUCTION.md, PHASE4_CHECKLIST.md and MASTER_DESIGN.md.

## 14 Install update and preserve a server

Status: Operator procedures for this development snapshot.

### Recorded environment

The tested development combination is Paper 26.2 build 129 and Java 25; Maven targets Java 25. Use a matching Minecraft client. Plugin metadata alone is not a compatibility matrix. Players do not install the Civilizations JAR as a client mod.

### Update while stopped

- Stop normally and preserve worlds, plugin data/settings and the old JAR together.
- Install only the verified target/civilizations-0.0.1-SNAPSHOT.jar in plugins. Keep one Civilizations JAR.
- Preserve existing configuration and review optional new sections rather than overwriting your tuning.
- Start normally, read the console, check recognition and a small trade, then test another normal restart.

Never install src/test/paper helper plugins on a human server. Do not hot-reload or erase the database to bypass an error. The development build does not deploy itself to your server.

### Back up matching files

SQLite lives under plugins/Civilizations. Current schema 108 includes construction and building-consent history. Upgrade tests use temporary databases; real saves are not disposable. An older plugin can refuse newer schemas. Rollback requires a consistent matching backup, not just the previous JAR.

A live server/database inside OneDrive risks sync conflicts and incomplete backups. Prefer a server outside synchronized folders; relocate it only while stopped after a consistent backup.

Sources: DEVELOPMENT.md, PHASE3_PLAYTEST.md, PROJECT_STATUS.md and pom.xml.

## 15 Tune the simulation with text files

Status: Available. Older installations may need explicit opt-in for new optional features.

| File in plugins/Civilizations | Main purpose |
|---|---|
| config.yml | Population time, recognition, observation, projection and pen controls |
| economy.yml | Food, recipes, prices, resources, grants, training and business rules |
| construction.yml | P4 funding, labor, land, housing-proposal and supply terms |
| construction/*.yml | Starter plan geometry, stages, materials and functional anchors |

### Make controlled changes

Stop, back up, edit with a text editor and restart normally. YAML indentation and value types matter. There is no general live reload command. Installed files are preserved; a missing optional section may leave a feature disabled even if fresh installations enable it.

food.output-percent uses 100 for baseline, 25 for quarter output and 200 for double. This is one crop-output lever, not a universal famine switch: fisheries, livestock, bakeries, reserves and purchased food affect the broader chain. Suitable evidence and workers are still required.

population.millis-per-month, pace.seconds-per-period and industry-runtime.real-millis-per-period control separate clocks. Faster periods change both production and consumption opportunities. Try acceleration in a disposable save.

### Defaults do not refill assets

Changing a new-store capacity or new-resource default is not a refill command. Accepted founding, receipt and reserve terms can remain saved for consistency. Compare reports after one controlled adjustment; do not expect retroactive free money, goods or supply resets.

Sources: shipped configuration, ECONOMY_TUNING.md and PHASE3_PLAYTEST.md.

## 16 Troubleshoot and report problems

Status: Read-only reports available. Recovery confirmation is a separate action.

| Symptom | First check |
|---|---|
| Calendar runs but zero residents | /civ debug observation; villagers, bed heads and complete loaded evidence |
| Food sale refused | Actual demand, funds, held item, stack size and permissions |
| Market has no offers | Nearby eligible staffed livestock firm, stock and settings |
| Business produces nothing | /civ debug businesses <UUID>; workers, inputs, tools, space and sources |
| No new buildings | Ordinary automatic P4 construction is not enabled |
| Old resources not adopted | /civ debug resources; uncertain history may need reviewed recovery |
| Village ID refused | Use its full UUID, not a shortened chat label |

### Report useful evidence

Describe what you did, expected and observed. Include build/commit, server/Java versions, relevant IDs, settings changes and a timestamped log excerpt. Use the console/log for long reports. Redact private addresses and player information before public sharing.

### Preserve pending state

Do not repeatedly replace inventory during a pending exchange, remove markers or delete database/player records. A wait can be intentional safety behavior. Stop normally and preserve corresponding world, database and player saves for investigation.

Incomplete surveys must not be forced into dead/depleted/empty results just to make a report finish. Review scoped recovery consequences before confirming.

Sources: PHASE3_PLAYTEST.md, RESOURCE_REVIEW.md and DEVELOPMENT.md.

## 17 Player command glossary

Status: Implemented. Square brackets indicate optional input; angle brackets indicate a value to replace. Do not type those brackets literally.

| Command | Meaning and effect |
|---|---|
| /civ aid [village UUID] | Read-only held-food/nearby-village preview; does not donate |
| /civ bargain <emeralds> [village UUID] | Ask that payment for the whole held stack; example /civ bargain 5 |
| /civ bargain accept [village UUID] | Accept the current price offer and begin guarded confirmation |
| /civ barter timber [village UUID] | Request an offer using real available timber |
| /civ barter stone [village UUID] | Request an offer using real available stone |
| /civ barter accept [village UUID] | Accept the current supported goods offer |
| /civ market | Open nearby stocked milk/egg offers; buying requires payment, room and a bucket for milk |

### Menu and permissions

Sneak-right-click a managed villager with supported plain food for donation/sale controls. Read the final message; opening a menu does not complete a transfer.

civilizations.aid.preview permits previews; civilizations.aid.donate permits donations; civilizations.trade permits trading and stocked livestock purchases. They default to ordinary players, but server owners can change permissions/settings.

Off-limits building plot selection/protection/release commands are available in Chapter 22. Building-donate, apartment, repossess and rent controls remain unfinished.

Sources: plugin.yml, FoodAidPreviewCommand, FoodBargainingCommand, FoodBarterCommand and LivestockMarketMenu.

## 18 Admin diagnostic command glossary

Status: Implemented. Requires civilizations.debug, defaulting to operators. These report commands do not reset the simulation.

| Command | What it explains |
|---|---|
| /civ debug status | Readiness, population, calendar queue and reported failures |
| /civ debug observation | Scan progress and incomplete discovery evidence |
| /civ debug villages | Saved names and full village UUIDs |
| /civ debug settlement <UUID> | Population and household records |
| /civ debug explain <UUID> | Population decisions and reasons |
| /civ debug economy <UUID> | Food, funds, needs, reserves and responses |
| /civ debug businesses <UUID> | Businesses, workers, stock and operating reasons |
| /civ debug workforce <UUID> | Qualifications and competing labor opportunities |
| /civ debug resources | Resource history, tracking and uncertainty |
| /civ debug construction | Saved delivery diagnostic; not a build/start command |

### Find the right ID

Use /civ debug villages or press Tab after a supported village-report command and a space. Commands use full saved UUIDs. Friendly short labels are not complete IDs. Use console output or logs for copying long reports.

Resource recovery is different: recover-ore and recover-forest include explicit mutating confirmation. See the next chapter. Operator permission does not itself establish that uncertain terrain should be trusted.

Sources: PopulationDebugCommand, PopulationDiagnostics and PHASE3_PLAYTEST.md.

## 19 Protection preview and recovery commands

Status: Implemented limited controls, not general plot ownership or normal construction.

| Command | Meaning and effect |
|---|---|
| /civ pen confirm | Recheck/save an eligible pen preview; admin permission required |
| /civ pen here | Read saved pen IDs with release/admin permission |
| /civ pen release <pen UUID> | Preview release of an area you own or administer |
| /civ pen confirmrelease | Confirm area release; animal ownership/history remain |
| /civ pen cancel | Cancel an unsubmitted preview, not an already submitted save |
| /civ build templates | Read installed plans with construction preview permission |
| /civ build preview <template> [rotation] [mirror] | Read-only site inspection eight blocks ahead; player required; rotation 0, 90, 180 or 270 |

### Scoped resource recovery

- /civ debug recover-ore list shows scoped ore candidates.
- /civ debug recover-ore review <deposit UUID> inspects one saved deposit.
- /civ debug recover-ore confirm <token> accepts the reviewed scope within its lifetime.
- /civ debug recover-forest list shows scoped forest candidates.
- /civ debug recover-forest review <site UUID> inspects one saved forest site.
- /civ debug recover-forest confirm <token> accepts the reviewed scope within its lifetime.

Read each review before confirming. These use resource IDs, not village IDs.

Recovery does not refill extracted reserves, erase protected sources or authorize taking player land. Read RESOURCE_REVIEW.md and the relevant recovery guide before confirming uncertainty.

### Permissions

civilizations.pen.admin permits pen confirmation. civilizations.pen.release still checks owner/admin authority for release. civilizations.construction.preview defaults to operators and permits inspection only. Breaking a sign does not release saved protection.

Sources: PaperPenControls, PaperConstructionPreview, PaperMineralRecovery and PaperForestRecovery.

## 20 Terms and a connected village example

Status: Plain-language reference. The example is explanatory, not a guaranteed outcome for every town.

| Term | Meaning |
|---|---|
| Settlement | A persistent village identity tied to a world |
| Household | People grouped for living/needs, not automatically a family tree |
| Dwelling unit | Proposed separately assignable home within a building |
| Treasury | Village budget, separate from business cash and prosperity |
| Trade Value | Internal accounting value connected to prices by server policy |
| Reserve | Goods or cash kept for future needs |
| Period | One earned update of a particular food/industry clock |
| Provenance | Evidence of origin and later changes |
| Cargo | Owned goods in transit, unavailable until delivery |
| Capacity | A usable limit on storage, work, production or housing |
| Stage escrow | Delivered materials set aside once for a frozen construction stage |
| UUID | Full permanent identifier used by records and diagnostics |

### Follow one chain

A farm contributes food and can sell surplus. A warehouse pays for actual goods. A bakery buys eligible ingredients and sells food to a paying customer. Payments move existing cash, and production consumes supplies. Nothing is manufactured by simply labelling a building a bakery.

Tools create demand for blacksmiths. Materials and storage can support logging, mining or a craftsman where suitable funded opportunities exist. Missing money, workers or space makes the chain wait. P4 should eventually turn genuine demand into paid physical premises and housing.

Sources: HYBRID_FARMS.md, BAKERY_SUPPLY_CHAIN.md, BUSINESS_TOOLS.md and CONSTRUCTION.md.

## 21 Roadmap and maintaining this guide

Status: Product direction. Prototype gates are not public-release certification.

| Phase | Focus |
|---|---|
| 1 Foundation and Population | Persistent people, recognition, families and reliable saves |
| 2 Needs and Economy | Food, funds, shortages and explainable responses |
| 3 Businesses and Industry | Ownership, jobs, resources and production chains |
| 4 Physical Growth | Sites, useful buildings, housing, reuse and visible work |
| 5 Trade and Roads | Regional routes and interdependent settlements |
| 6 Identity and History | Leaders, relations, diplomacy and reputation |
| 7 Growth and Decline | Prosperity, abandonment, ruins and recovery |
| 8 Advanced Civilization | Territory, discovery and contextual crises/conflict |
| 9 Polish and Release | Scale, compatibility, balance and survival validation |

### Update alongside development

The editable source is docs/guide/PLAYER_AND_SERVER_GUIDE.md. Regenerate with tools/build_player_guide.py. For each feature batch, verify command syntax, permissions, status labels and examples against current code. Add screenshots only after controls exist. Planned controls belong outside the working command glossary.

Each chapter should explain purpose, use, what changes or pauses the feature, and its connections. Keep edition notes and distinguish accepted design from implementation.

### Source authority and edition notes

Code and PROJECT_STATUS.md establish current behavior. MASTER_DESIGN.md, ROADMAP.md, ARCHITECTURE.md, SIMULATION_RULES.md and DECISIONS_AND_OPEN_QUESTIONS.md define intent. Older feature documents retain dated checkpoints; prefer current code/status when wording conflicts.

Edition 0.5 adds configurable covered-bed qualification to saved-housing reports. Housing donation/assignment remains unfinished. Village-owned apartments defer rent and landlords.


## 22 Protect a building plot

Status: Available. Saves a construction exclusion; does not donate buildings, adopt animals or activate housing.

### Select and confirm

1. Stand at opposite plot corners and run /civ building corner1, then corner2.
2. Run /civ building protect. Review the X/Z bounds and green outline.
3. Click Confirm: I own this plot. Wait for the final protected message.

Nothing is saved on preview. Protection covers those columns at every height. Your ownership declaration cannot override another recorded donor, protected owner or reserved construction.

Optional sign: put [Civ Building] on line one and protect on line two. Right-click it inside your selected plot to preview. Removing the sign does not remove saved protection.

### Command glossary

| Command | Meaning |
|---|---|
| /civ building corner1 | Set first corner at your feet |
| /civ building corner2 | Set opposite corner |
| /civ building protect | Outline and preview; save nothing yet |
| /civ building preview | Same as protect |
| /civ building here | Show active exclusion IDs here |
| /civ building inspect <ID> | Saved housing bed/access report; donor/admin only |
| /civ building release <ID> | Preview release of your exclusion |
| /civ building cancel | Discard an unsubmitted selection/preview |
| /civ building confirm <ID> owned | Manual ownership confirmation |
| /civ building confirmrelease <token> | Manual release confirmation |

Editing corners invalidates old buttons. Resize by protecting a replacement before confirming release of the old exclusion. Other protections remain. Submitted saves can finish after disconnect; use here to check.

### Settings and authority

config.yml building-protection defaults: 4,096 columns, edge 128, 16 players, 60-second confirmations and 300-second selections. Invalid settings disable only these controls. Restart after editing.

civilizations.building.protect defaults true; civilizations.building.admin defaults op and permits confirmed release of another player's exclusion. This is not general anti-grief protection or proof of authorship. See BUILDING_PROTECTION.md.
