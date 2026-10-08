# Civilizations — locked decisions and unresolved questions

## Locked / approved
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
