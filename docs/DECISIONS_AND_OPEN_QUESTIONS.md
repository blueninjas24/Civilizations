# Civilizations — locked decisions and unresolved questions

## Locked / approved
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
  offline cap 120 and batch two, adult/child food demand 2/1, output four per evidenced
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
