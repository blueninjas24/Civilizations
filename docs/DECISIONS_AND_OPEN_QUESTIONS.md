# Civilizations — locked decisions and unresolved questions

## Locked / approved
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
2. Center/border computation, nearby overlap and merging/splitting criteria, world UUID versus name.
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
