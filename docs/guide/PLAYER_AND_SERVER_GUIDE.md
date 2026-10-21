# Civilizations Player and Server Guide

Living draft 0.17 | 21 October 2026 | Player and server owner edition

Civilizations is a Minecraft server plugin that gives villages persistent people, households and a connected local economy. Businesses need workers, supplies, storage and paying customers. Your food deliveries and purchases can help towns operate.

This guide explains what you can do now, why the systems behave as they do, and how they connect. It also documents the intended P4 housing and construction controls. Planned features are labelled throughout; their proposed signs and commands are not working instructions.

### TL DR

- Players join the server with its matching Java Minecraft client. No Civilizations client mod is required.
- Villages containing villagers and beds are recognized automatically after complete loaded-area observation. There is no join command.
- Hold supported plain food and sneak-right-click a managed villager to open village supplies. Wait for the final donation or trade confirmation.
- Use /civ market near a qualifying livestock business to buy stocked milk or eggs. Bring your own empty bucket for milk.
- Industry simulation, single-home donation, whole-family housing assignments and confirmed donor repossession work today. Explicit apartment donation is available. With automatic construction enabled, funded family-home proposals can now become real houses.
- Server downtime and absent worlds pause new simulation time. An unloaded chunk does not prove death or destruction.

### Read the status first

Available means implemented, subject to settings and eligibility. Development only means backend or disposable-server work that is not ordinary gameplay. Planned means accepted direction or a proposal whose controls do not exist yet.

Snapshot: P3 prototype gate complete; P4 has 5 of 16 checklist items complete. Save schema 116 preserves existing housing, records paid construction recovery, and distinguishes village-built homes from player donations. Donated homes can house eligible saved families, and donors can confirm repossession. Human survival balance and other plugin combinations still need playtesting. This is a living draft, not a release certification.

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
- 23 Donate a single-family home
- 24 Live family housing and repossession
- 25 Declare an apartment building
- 26 New adult households
- 27 Separate renovation consent
- 28 Inspect and cancel unused home projects
- 29 Automatic home-site planning
- 30 Read construction materials and protected funds
- 31 Automatic family-home construction

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

Status: Persistent population, employment and inspected whole-family home assignments are available. Home-specific villager movement remains unfinished.

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

Status: Whole-family housing and explicit new apartment declarations are live. Saved-building boundary revisions remain unfinished.

### Separate the land building and homes

A plot is surrounding land. A building is the physical structure. A dwelling unit is one household's home inside it. An apartment building can contain several independent units plus shared stairs, corridors and a courtyard. Several bedrooms in one family home do not automatically make several apartments.

The initial direction is village ownership without rent or landlords. A household acquires occupancy through assignment, as with a village-owned home. This does not establish a private sale price or transfer property title; private purchasing needs a separate economic decision.

### Recognizing a unit

Players can explicitly mark private units and shared corridors before a new apartment donation. Units can contain several non-overlapping rooms; upstairs and downstairs units may share X/Z coordinates at different heights. Beds do not automatically create apartments. Chapter 25 gives the working editor commands.

A unit needs usable capacity, a reachable entrance and non-overlapping private space. Shared access must not cross another household's private unit. Bed/access/cover validation checks every declared unit. The new-building apartment editor is available; changing an already registered layout remains unfinished.

### Inspect saved housing

/civ building inspect <building UUID> reviews registered housing in your current world. Only its recorded donor or an administrator may inspect it. It shows the saved family size and effective capacity, or that no household is assigned, followed by current bed/access/cover findings. Saved last-reviewed state and current block evidence are labelled separately. Missing chunks mean unverified evidence, not lost housing.

Donors may inspect their own home with donation or reclamation permission even when plot-protection permission is disabled. This grants no land protection, access to another donor's home, or housing changes. The inspection command remains read-only.

This inspection command cannot register a house. Chapter 23 explains the separate single-home donation controls. Covered beds are required by default; both halves need overhead cover inside the declared building. Admins can change require-covered-beds under building-protection in config.yml. Inspection still grants no occupancy or usable capacity.

### What empty means

A vacancy means no saved household is assigned, rather than nobody standing inside. Going to work or unloading a chunk does not create a vacancy. The live housing review preserves incumbent families and can assign known overcrowded families to suitable permitted vacancies. Families must fit as a whole; see Chapter 24.

Related chapters: donation and construction. Sources: DECISIONS_AND_OPEN_QUESTIONS.md, CONSTRUCTION.md and HousingProjectPlanner.

## 5 Donate protect and reclaim buildings

Status: Off-limits protection, single-home donation, whole-family assignments and confirmed housing reclamation are available. Explicit new apartment declarations use Chapter 25; saved-layout revisions remain unfinished.

### Explicit consent

Player-built homes, workplaces and decorative buildings are off limits without a clear confirmed donation. Beds, workstations and location inside a village are not consent. Old uncertain structures must not be guessed into permission.

Players explicitly declare that an unrecorded home belongs to them; first donation does not require admin approval. This trust policy cannot override another recorded donor or saved protection. A sign alone grants nothing. Donation grants use only; physical renovations need separate confirmed consent through Chapter 27. Chapter 23 gives working single-home commands. Apartment declaration commands are in Chapter 25. Business donation controls and donation signs remain unfinished.

### Boundaries and extra protection

For an off-limits plot, select opposite corners and preview its visible outline before confirming ownership. Chapter 22 gives the commands and optional protection sign. Selected X/Z columns are excluded at every height. Single homes and apartment units use explicit floor-to-roof corners. Apartment drafts can also mark a separate surrounding plot; see Chapter 25. More land does not create beds or production.

Saved protection survives sign removal. Private buildings still need explicit donation even without this extra marker. Protection governs Civilizations construction; it does not prevent other players editing or vanilla villagers walking through. Releasing an exclusion changes construction permission. Housing reclamation is a separate confirmed action; see Chapter 24.

### Repossession changes village capacity

Reclaiming a donated home withdraws village housing use while keeping the building intact. Households need another suitable home; residents and history remain saved. Contents and unrelated money do not become the player's property. Business-premises reclamation controls remain future work.

Recorded donors or administrators preview affected households, then confirm separately. Changed occupants require a fresh preview. Removing a sign does not reclaim a home. Individual apartment-unit reclamation remains unfinished.

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

Status: P4 in progress. Automatic family-home construction is available when enabled in construction.yml. Roads, business construction and civic buildings remain unfinished.

### Current foundation

Three original editable starter plans exist: path, timber storehouse and four-bed home. Admin catalogue/site previews are read-only. Saved family-home proposals can now buy actual supplies, wait for delivery and earned public labor, and build in stages through the automatic runtime. Disposable tests also cover a paid path and a complete home across restarts.

A preview spends no money, starts no project and assigns no home. Automatic family-home work uses the guarded normal-server writer when enabled; there is no instant player build command. Standalone development helpers remain restricted to marked disposable test servers.

### Read why a family needs housing

Operators can use /civ build housing <village UUID> while in that village's world. Find the saved ID with /civ debug villages. The report explains suitable existing housing, vacant-home reuse, new-home proposals and why a family must wait. Unknown capacity and stale inspections never count as homelessness. A pending home holds its own family; older unmapped projects remain conservative holds. This reads saved evidence; it does not scan buildings, reserve a plot, spend money or start construction. Reports expire after 30 seconds, with four simultaneous requests. Chapter 28 covers saved project status and safe unused cancellation.

### The intended chain

Need -> permitted vacancy review -> suitable site -> real funding/materials -> available labor -> staged work -> physical completion -> usable capacity. A draft or frame cannot count as a completed home.

Businesses are intended to fund their premises. Villages fund basic homes/local roads after protecting food and upkeep. Public-work accounting avoids double farm/study labor. Automatic public family-home work uses real unemployed adult labor, paid supplies and guarded native writes. Private employee diversion, business premises and new local-road construction remain unfinished.

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

SQLite lives under plugins/Civilizations. Current schema 116 includes paid construction recovery, explicit village-built home identity, separate renovation consent, household-bound proposals and exact bed accounting. Existing families/housing are preserved through a gradual transition. Eligible families can receive inspected donated homes without duplicate bed capacity. Upgrade tests use temporary databases; real saves are not disposable. An older plugin can refuse newer schemas. Rollback requires a consistent matching backup, not just the previous JAR.

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
| No new homes | Check automatic-construction opt-in, actual family need, a safe loaded site, real suppliers, protected funds and labor; Chapter 31 |
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

Off-limits plot commands are in Chapter 22, single-home donation in Chapter 23, and confirmed donor repossession in Chapter 24. New apartment editor commands are in Chapter 25; separate renovation consent is in Chapter 27. Saved-layout editing and rent controls remain unfinished.

Sources: plugin.yml, FoodAidPreviewCommand, FoodBargainingCommand, FoodBarterCommand and LivestockMarketMenu.

## 18 Admin diagnostic command glossary

Status: Implemented. Requires civilizations.debug, defaulting to operators. These report commands do not reset the simulation.

| Command | What it explains |
|---|---|
| /civ debug status | Readiness, population, calendar queue and reported failures |
| /civ debug housing | Last global home review, assignments and empty-home releases |
| /civ debug observation | Scan progress and incomplete discovery evidence |
| /civ debug villages | Saved names and full village UUIDs |
| /civ debug settlement <UUID> | Population and household records |
| /civ debug explain <UUID> | Population decisions and reasons |
| /civ debug economy <UUID> | Food, funds, needs, reserves and responses |
| /civ debug businesses <UUID> | Businesses, workers, stock and operating reasons |
| /civ debug workforce <UUID> | Qualifications and competing labor opportunities |
| /civ debug resources | Resource history, tracking and uncertainty |
| /civ debug construction | Delivery, site-planning and automatic-home work diagnostics; read-only |

### Find the right ID

Use /civ debug villages or press Tab after a supported village-report command and a space. Commands use full saved UUIDs. Friendly short labels are not complete IDs. Use console output or logs for copying long reports.

Resource recovery is different: recover-ore and recover-forest include explicit mutating confirmation. See the next chapter. Operator permission does not itself establish that uncertain terrain should be trusted.

Sources: PopulationDebugCommand, PopulationDiagnostics and PHASE3_PLAYTEST.md.

## 19 Protection preview and recovery commands

Status: Implemented limited controls, not general plot ownership or an instant construction command.

| Command | Meaning and effect |
|---|---|
| /civ pen confirm | Recheck/save an eligible pen preview; admin permission required |
| /civ pen here | Read saved pen IDs with release/admin permission |
| /civ pen release <pen UUID> | Preview release of an area you own or administer |
| /civ pen confirmrelease | Confirm area release; animal ownership/history remain |
| /civ pen cancel | Cancel an unsubmitted preview, not an already submitted save |
| /civ build templates | Read installed plans with construction preview permission |
| /civ build housing <village UUID> | Read saved whole-family housing decisions in your current world; starts no project |
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

Edition 0.16 adds a read-only construction funding/material report. Guarded automatic home-site planning remains available. Retained home-project reports and confirmed unused-project cancellation remain available. The read-only whole-family housing-planning report remains available. Separate renovation-consent controls and saved inspection status remain available. Donation still grants use only. Configurable new households for unassigned adults and recorded partners preserve existing families. Explicit new apartment declarations include private rooms, shared spaces and separate plots. Donor inspection, live whole-family housing and confirmed donor repossession remain available. Normal construction and saved-layout revisions remain unfinished; rent and landlords stay deferred.


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


## 23 Donate a single-family home

Status: Available. Donation saves use permission; enabled automatic reviews can then assign a suitable whole saved family and activate its housing capacity.

### Prepare and review

- Select opposite corners with /civ building corner1 and corner2. With no coordinates these use your feet position. Include the entire building, floor blocks and roof.
- For exact selection use /civ building corner1 <x> <y> <z> and corner2 <x> <y> <z>. This avoids trying to stand inside walls. Minecraft's F3 display helps locate block coordinates.
- Stand at the entrance inside the selection. Run /civ building home <village UUID>; Tab offers saved village IDs.
- Wait for the review. Check the village, three-dimensional bounds and bed count. This declares ONE family home, regardless of bedrooms or beds.
- Click Confirm: I own and donate this home. Wait for the final saved message.

You must own the building. This grants village use, not title or contents. Family assignment follows a separate current housing review. Another recorded donor, protected pen/plot or reserved project can block donation. Do not confirm a building belonging to someone else.

### Commands and safeguards

| Command | Meaning |
|---|---|
| /civ building home <village UUID> | Scan and preview one family-home donation |
| /civ building confirmhome <token> owned | Manual form of the confirmation button |
| /civ building cancelhome | Cancel an unsubmitted scan or preview |

A changed selection, different world, lost permission, expired evidence or missing chunks prevents confirmation. Cancellation cannot undo an already submitted save. A failed save never announces success; review the server log before retrying the same confirmation.

Beds must be real, paired, supported, clear and reachable from the chosen entrance. Covered sleeping areas are required by default. Partial stairs/slabs do not yet prove walking access, so some usable buildings can be conservatively refused.

### Settings and current limits

civilizations.building.donate defaults true; no first-donation admin approval is required. building-protection.enabled controls availability; require-covered-beds controls overhead cover. Restart after editing config.yml. Inspection and donation use up to 256 cells per tick together. The new housing runtime adds one shared budget of 128 by default; see Chapter 24.

Donation signs and normal automatic construction remain unfinished. Explicit new apartments use Chapter 25; revising a saved building remains unfinished. Confirmed reclamation and whole-family housing now work; see Chapter 24. The saved footprint stays protected after reclamation. Related chapters: housing, protection and construction.


## 24 Live family housing and repossession

Status: Available for saved families and explicitly donated homes. No physical construction or automatic villager teleporting. Explicit apartment declarations use Chapter 25.

### After donating a home

Enabled reviews inspect permitted housing in loaded areas. A suitable existing family can keep its home under the new records. Otherwise a known overcrowded or unhoused family may take a suitable vacancy. Whole families must fit; they are never split to fill beds. Existing village housing remains while families transition gradually. A house can remain vacant when nobody suitable needs it.

Saved residents and families remain the same people. New unrelated villagers are not inserted into a managed family simply because they stand nearby. Eligible unassigned adults can now form a one-person household, or share one with an already recorded eligible partner. That creates a housing need, not free beds or an instant building. Existing families are preserved; see Chapter 26.

### Damage and repairs

Complete evidence of a broken bed, roof or route can remove usable capacity. The family stays recorded and may move to another suitable permitted vacancy. Restoring the home allows a later successful review to restore capacity. Missing chunks or an absent world are unknown, not proof that a home was destroyed. Reviews observe changes over time; they are not instantaneous world locks.

### Repossess your donated home

- Run /civ building reclaim <building UUID> in its saved world. Use the building ID from the donation receipt.
- Read the preview: assigned households lose their unit; affected legacy families lose those beds' contribution. Residents, contents, blocks and history remain.
- Click Confirm withdrawal of village use, or run /civ building confirmreclaim <token>. A changed household roster requires a fresh preview.
- Use /civ building cancelreclaim to discard an unsubmitted preview. An accepted save finishes even if you disconnect.

Only the recorded donor or an administrator can reclaim. civilizations.building.reclaim defaults true; it does not authorize reclaiming somebody else's home. The footprint stays protected from Civilizations construction. Rehousing needs another suitable permitted vacancy; nobody is deleted and old bed accounting cannot silently grant use again.

### Server settings and diagnosis

config.yml housing-runtime defaults to enabled, 128 cells-per-tick and review-seconds 15. Both survey passes share that budget. Budget range is 16 to 256; cadence is 5 to 600 seconds. Large structures can time out with a small budget and stay unverified. Default combined housing reads are at most 384 per tick. Restart after editing.

/civ debug housing requires administrator debug permission and explains the last global review. It is not an address book or a promise that someone needs every donated home. Sources: HOUSING_RUNTIME.md and DWELLING_OCCUPANCY.md.


## 25 Declare an apartment building

Status: Available for NEW, explicitly player-owned building declarations. No rent, title sale, automatic apartment guessing or editing of a registered building.

### TL DR

Select the whole building first. Mark each family's private rooms and the shared halls separately. Review all units, then explicitly confirm ownership and donation. The normal housing runtime can assign suitable whole saved families afterward. Extra beds do not create extra families.

### Worked example: two units and a hallway

- Select the entire floor-to-roof structure with /civ building corner1 <x> <y> <z> and corner2 <x> <y> <z>. Stand at the building entrance, then run /civ building apartments <village UUID>. This freezes the structure, entrance and village for this draft.
- Select the first unit's floor-to-roof corners. Stand at that unit's entrance inside its private space. Run /civ building unit west. Repeat with /civ building unit east for the second unit.
- Select the hallway or stairs, then run /civ building shared. Private and shared boxes must not overlap. Coordinates include both boundary blocks: two adjacent boxes must end/start on different block coordinates.
- For another room belonging to west, select its separate box and run /civ building room west. Several rooms still form one family home.
- Optional: select a larger surrounding yard and run /civ building apartmentplot. It must contain the whole frozen structure. Land does not add housing capacity.
- Run /civ building reviewapartments. A bounded loaded-only scan checks every unit's real bed pairs, support, headroom, roof and reachable access. A unit cannot depend on walking through another private home.
- Review the unit count, beds, structure, plot and private boundaries. Click the ownership confirmation or run /civ building confirmhome <token> owned. Wait for the saved building ID. Inspection, normal housing and whole-building reclamation then use that ID.

### Correct or cancel a draft

Use apartmentstatus to view the draft, removeunit <name> to replace a unit, and clearshared to redraw shared boxes. Prefix each with /civ building. cancelapartments discards the editor; cancelhome cancels an unsubmitted review. Accepted saves finish.

Names use 1 to 24 lowercase letters, digits, underscores or hyphens. Four editors maximum; drafts expire after ten idle minutes, disconnect, world change, permission loss or restart. Reviews share the existing 128-cell-per-tick donation budget, expire after 30 seconds, and allow 60 seconds for confirmation. Large scans can time out.

### What happens next

Whole saved families can take suitable vacancies through the normal housing runtime. No suitable family can mean an empty unit. Whole-building reclamation uses Chapter 24. Per-unit reclamation, registered-boundary revisions and physical apartment construction remain unfinished.


## 26 New adult households

Status: Available during complete permitted housing reviews. This groups existing saved people; it does not spawn new residents.

### Why it matters

An adult who arrives near an already assigned home cannot simply be inserted into the incumbent family. The plugin can now form a new one-person household for that adult, or keep two already recorded eligible partners together. The new household can take a suitable inspected vacancy or remain a known housing need. There are no free beds, building, cash or extra food supplies.

### Who qualifies

Adults must be alive, in the same saved village and without a household. Existing household members stay where they are. Only already recorded partners can form a pair; this feature does not create romance. Children do not form adult households and gain no invented parents.

Ambiguous partnerships, a partner already housed elsewhere, or known unassigned children needing care hold the case for later review. The prototype does not split or merge existing families to resolve those cases. No new housing is promised merely because a household is formed.

### Editable limits

config.yml household-formation defaults to enabled true, maximum-per-month 2 and maximum-candidates 128. The monthly limit is shared across the village's reviews and uses civilization months. Repeated scans do not bypass it. Restart after editing. Disabling formation keeps existing families and homes intact; invalid formation settings disable this feature while saved housing continues.

It runs only during complete permitted housing reviews in a present world. Missing chunks, absent worlds and unreviewed homes do not create households. Villages with no permitted housing review still wait. Saved household/member history prevents duplicate formation after retries or restart. Formed households enter the same needs, food and housing accounting as existing households.

Related chapters: families, housing, apartment declarations and server tuning. Whole families still need a suitable permitted vacancy before gaining housing capacity.


## 27 Separate renovation consent

Status: Available as explicit permission controls. Ordinary automatic construction and physical renovations remain unfinished. Granting consent starts no project.

### TL DR

Donating a building lets the village use it. It does not let villagers redesign it. If you want to allow future renovations, confirm that separately. Ownership, contents and normal housing remain separate.

### Allow or deny

- In the building's saved world, run /civ building allowrenovation <building UUID>. Use the ID from your donation receipt.
- Review the frozen building and structure scope, then click Confirm separate renovation decision or run /civ building confirmrenovation <token>. Nothing is saved just by requesting a preview.
- To withdraw this separate permission, run /civ building denyrenovation <building UUID> and confirm its new preview. This keeps the donation and housed families intact.
- /civ building cancelrenovation discards an unsubmitted review. Accepted saves finish even after disconnect. /civ building inspect <building UUID> reports the saved renovation status beside housing records.

Only the recorded donor can grant renovations. Administrators can withdraw consent for safety; being an operator does not let them grant consent for someone else's donation. civilizations.building.renovate defaults true, with the saved donor checked separately. Plot-protection permission is not required to inspect your own saved housing status.

### What the permission means

A grant is bound to the exact saved layout and current donation. Reclamation or re-donation invalidates an old grant. Replaying an old confirmation cannot restore a withdrawn grant. Changed donation/history requires a fresh review; previews last 60 seconds and four player reviews can be open at once.

It grants no access to chest contents, extra land, free materials, labor or money. A future renovation still needs a safe funded project, current protected-land checks and current-world block revalidation. Existing off-limits protections remain effective. No construction happens merely because the status says allowed.

Related chapters: donation, protection, housing, construction and server backups. Schema112 preserves separate consent history; old donations receive no automatic renovation permission.


## 28 Inspect and cancel unused home projects

Status: Available operator controls. A new server may have no saved projects until planning confirms real family need and a safe site. These controls do not start construction.

### Read the saved work

Run /civ build projects <village UUID> in the village's world. Find the full village ID with /civ debug villages. The report shows up to eight retained projects, active first: the family, frozen plan, site reservation, paid supply orders, durable block intents and any current housing-need pause. Additional history stays saved.

Read-only reporting uses civilizations.construction.preview, which defaults to operators. It spends nothing, reads no terrain and loads no chunks. A project can be waiting because its family already has a suitable home, its housing evidence is unknown, or the family no longer fits the plan.

### Cancel only unused work

- Run /civ build cancelproject <project UUID> to request a review. Nothing is cancelled on preview.
- If the project qualifies as unused, read the family and project IDs. Confirm with the clickable button within 60 seconds, or type /civ build confirmcancel <token>.
- The save releases the unused site and family reservation together. If either part cannot save, both remain unchanged.
- /civ build cancelrequest discards your unsubmitted request. An already submitted save finishes independently, including after disconnect.

Cancellation requires civilizations.construction.manage, which defaults to operators. Read-only permission alone cannot cancel. The confirmation belongs to the requesting player and world; changing permissions, leaving the world or expiry invalidates an unsubmitted request.

### What stays protected

Paid supplies, unreturned owned materials and any prepared physical work block this unused-cancellation path. They require a reviewed recovery process; the command cannot delete goods, refund money, remove buildings or erase history. Payment or work that arrives after preview is checked again at confirmation.

A saved cancellation cannot reactivate its old proposal. A new home needs a fresh identity and review. Repossessing an existing donated home uses Chapter 24; the families and building remain recorded. An unstarted home pauses further commitments if its family gains suitable housing; already paid supplies remain saved. Durable physical work keeps its recovery/completion path so partial buildings are not stranded merely because housing needs change.

Four players can hold project requests/previews at once. Read checks expire after 30 seconds. A failed accepted save can retry the same live confirmation token; inspect project status if the result is uncertain. No command here enables the normal-server block writer.

Related chapters: housing, construction, donation, backups and the command glossary. Sources: HOUSING_PROJECTS.md, CONSTRUCTION.md and PaperHousingProjectControls.


## 29 Automatic home-site planning

Status: Available preparation only. A village can save an unused home proposal beside an existing path. This does not place a home, buy materials or grant beds.

### What the village considers

Suitable permitted vacancies come first. Unknown capacity or stale inspections pause planning. A plan must fit the whole family, and an editable cap limits active unfinished home projects.

The village checks flat supported space beside existing dirt paths. Buildings, fences, crops, trees, water, entities or saved protection can reject a rotated plan. This step does not dig, level hills or make roads.

Loaded blocks are checked twice within a shared budget; missing or changed evidence pauses planning. Simulation never loads chunks. Current protections and family need are rechecked before the plan, family and site reservation save atomically.

### Editable settings

New construction.yml files include these settings. Existing files must opt in with the complete section. Stop, edit, then restart.

| Under automatic-home-planning | Default and meaning |
|---|---|
| enabled | true in new files; dry planning only |
| cells-per-tick | 128 shared reads; allowed 64 to 256 |
| review-seconds | 30 between cycles; allowed 10 to 600 |
| maximum-active-homes | 1 per village; allowed 1 to 16 |
| search-radius | 12 blocks; allowed 8 to 12 |

This pass checks six vertical layers. Large plans, hills, missing paths or unloaded areas may leave no safe site. Protect reserved empty player land with confirmed off-limits protection before the village chooses it.

### Check progress and retain control

- /civ debug construction: latest planning result.
- /civ build housing <village UUID>: saved family demand and holds.
- /civ build projects <village UUID>: retained proposals.
- Chapter 28: confirmed unused cancellation.

If need remains, a later review can propose a fresh project. Disable planning to stop proposals entirely. A reservation grants no access to chests or permission to overwrite builds. The separate automatic-construction settings govern paid supplies, earned labor and physical work. A proposal alone grants no free beds or people; see Chapter 31.

Related chapters: housing, protection, construction, recovery and server settings. Sources: HOUSING_PROJECTS.md, CONSTRUCTION.md and AutomaticHomePlanningSettings.


## 30 Read construction materials and protected funds

Status: Available read-only operator report. It does not buy goods or enable automatic construction.

### Use the report

Run /civ build funding <project UUID> in the project's world. Find retained home-project IDs with /civ build projects <village UUID>. The existing civilizations.construction.preview permission allows reading; cancellation permission is separate.

The report shows current village money, protected food/upkeep money, spendable funds and construction spending this food period. Values are expressed as emerald value; actual bookkeeping uses hundredths. It also shows the reference cost of remaining inputs and available shared hauling capacity.

### Understand the material counts

| Report field | Meaning |
|---|---|
| still needed | Frozen requirement minus goods already purchased |
| paid | All actual committed orders, including cargo and delivered goods |
| incoming | Paid cargo that has not completed its delivery |
| delivered | Goods that reached this project's saved stock |
| free | Delivered goods not yet set aside for an earned stage |
| held for stages | Delivered goods already reserved for recorded work |

Example: twelve stone units were paid for and delivered, then reserved for foundation work. Free stock can be zero while still needed is also zero. Buying those twelve again would duplicate spending; stage reserves are already covered.

### Why a quote can wait

A current saved food period and complete fed-population assessment are required. Missing finances, absent worlds, changed housing need, protected money, spending limits, hauling limits or unknown prices can hold the report or its readiness result. An unknown price is never zero or free. The report shows at most eight material types; others remain recorded.

A feasible whole-batch result means the current reference-price budget and haul could cover the remaining inputs. It does not prove any supplier has those goods, grant land rights, reserve stock or authorize payment. Real supplier eligibility, current site evidence and actual payment checks remain necessary.

Related chapters: money, business supply chains, construction, automatic planning and project recovery. Sources: HOUSING_PROJECTS.md and ConstructionSupplyOrders.


## 31 Automatic family-home construction

Status: Available prototype for saved family-home proposals. Existing installations must explicitly opt in. This does not build new roads, businesses, docks, apartments or civic structures yet.

### What you can see

A village with a verified housing need can plan a small home beside an existing dirt path. If it can afford the complete remaining material batch from real qualified local suppliers, it pays from village funds after protecting food and essential upkeep. Goods arrive later through the existing hauling system. No chest is emptied and no free materials are added.

Available unemployed adults contribute recorded work. The foundation, walls, roof and beds appear in stages as their work and supplies become ready. All villages share one bounded builder, so several projects take turns. It does not clear trees, flatten hills, overwrite blocks or load chunks.

The completed home stays village-owned. Registration alone adds no housing capacity: the housing inspector must find actual accessible covered bed pairs, then assign a whole eligible saved family. It creates no extra people. Village-built homes are not player donations and cannot use donor repossession or player renovation-consent controls.

### Turn it on or pause it

Stop the server, back up its save and configuration, and edit plugins/Civilizations/construction.yml. Existing files are not replaced automatically. They need both the automatic-home-planning section from Chapter 29 and the complete automatic-construction section described below. AUTOMATIC_CONSTRUCTION.md in the repository provides a copyable YAML example. New files contain both enabled by default.

| Under automatic-construction | Default and meaning |
|---|---|
| enabled | true in new files; false or absent pauses this runtime |
| cells-per-tick | 128 shared survey reads; allowed 16 to 256 |
| review-seconds | 10 between waiting-project cycles; allowed 1 to 600 |
| block-interval-ms | 500 minimum milliseconds between native attempts; allowed 0 to 60000 |

Restart after editing. Successful projects rotate sooner; waiting projects retry at the review interval. One native step still requires a full bounded final scene check, capped at 256 reads, on a separate tick from the incremental survey. Large custom plans can exceed this starter writer's size limit.

Village funding, supply prices, hauling, labor and food periods retain their own editable settings. Increasing the calendar speed does not conjure missing supplies or unemployed workers. Disable automatic-construction to pause its future purchases and physical steps. Already accepted saves and earned cargo deliveries finish independently; existing people and completed housing remain saved.

### Understand a pause

Use /civ debug construction for the latest reason, /civ build projects <village UUID> for retained project history, and /civ build funding <project UUID> for materials and protected funds. Complete inputs can still be unavailable because qualified suppliers lack stock. Unknown prices, food pressure, cash reserves, hauling, changed family need or no eligible labor can also delay work.

A missing world or unloaded chunk waits. An entity, changed protection, missing road or altered construction block pauses physical work. A restart with an exact saved block present acknowledges it without placing it again. If a prepared block is absent after restart, its history is ambiguous: it may have been harvested already. The builder refuses a free replacement and requires manual review. There is no force-resume, demolition or free refund command. Preserve the save and report the project ID rather than deleting its database rows.

A flat clear site and real supplies remain necessary. The current starter home has four beds and cannot fit every family. Existing adequate housing is preserved; villages do not replace working homes just to demonstrate construction. Confirmed damage or repossession can create need, but do not damage your world merely to force a test.

Related chapters: housing, protection, automatic planning, funding, recovery, server backups and tuning. Source: AUTOMATIC_CONSTRUCTION.md. Wider P4 growth remains in progress.
