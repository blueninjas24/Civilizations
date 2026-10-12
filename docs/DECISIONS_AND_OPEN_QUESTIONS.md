# Civilizations — locked decisions and unresolved questions

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
