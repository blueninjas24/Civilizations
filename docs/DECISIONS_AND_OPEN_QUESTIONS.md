# Civilizations — locked decisions and unresolved questions

## Locked / approved
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

### Engineering choices — October 7, 2026
- SQLite schema versioning uses `PRAGMA user_version` with ordered transactional steps: base settlement fields (1), population (2), beds (3). Unversioned prototype schemas are adopted by inspecting column presence, with no destructive reset. Newer versions are rejected rather than downgraded. This changes persistence mechanics, not gameplay rules.
- Active checkout relocation outside OneDrive is recommended but not performed. Do not mistake Git ignore rules for cloud-sync exclusions or copy an open SQLite database as a consistent backup.

Codex may choose internal implementation details that do not change gameplay intent; document the choice. For consequential open rules, present 2–3 concrete options with tradeoffs and a recommendation; wait for user approval. Add approved outcomes here and update other canonical docs. Do not present hypothetical illustrative numbers as settled decisions.
