# Civilizations — Codex operating instructions

Read `docs/MASTER_DESIGN.md`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/SIMULATION_RULES.md`, `docs/DECISIONS_AND_OPEN_QUESTIONS.md`, and `docs/PROJECT_STATUS.md` before substantial work. These are the canonical product intent; actual repository code is the source of truth for current implementation. Do not mistake planned features for implemented features.

## Authority and autonomy
- Implement the next scoped roadmap work autonomously: inspect, plan, edit, test, review diff, commit coherent passing changes on feature branches. Report milestone-level progress, not every file edit.
- Never silently revise locked design principles or invent consequential gameplay decisions. Record proposals/questions in `docs/DECISIONS_AND_OPEN_QUESTIONS.md` and ask only when they block meaningful progress.
- Preserve clean `main`; do not merge to or push `main`, force-push, delete Git history, destroy worlds, or alter production databases without explicit approval.
- Development settlement data is currently disposable **only after confirming the target is a dev DB**. Do not assume future saves are disposable.
- Prefer small, cohesive changes and tests. Avoid premature frameworks, giant god classes, or speculative features.
- Run `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`) when relevant; if unable, explain exactly why. Never claim unrun tests passed.
- Keep simulation domain independent of Paper API where practical. Paper world/entity/chunk operations must obey main-thread API constraints. Do not force-load chunks for simulation.
- Introduce versioned, tested, transactional SQLite migrations; avoid exception swallowing. Preserve identity/history once non-disposable data exists.
- Make simulations deterministic under a seed and accelerated-testable. Provide diagnostics explaining decisions.
- Add or update tests, docs and devlog with each meaningful feature. Mark features as implemented only after code and verification.
- Never expose secrets, private server addresses, or player-specific data in public Git history.

## Priorities
1. Finish and review `foundation/persistence-build` (Maven wrapper, SQL INSERT fix, JUnit tests), then establish versioned migrations, CI, backups, debug harness and repository architecture.
2. Implement persistent population and household simulation before substantial construction.
3. Follow the nine-phase roadmap; use milestone gates rather than rushing ahead.

## Delivery checklist
Summarize changed files, behavior, test commands/results, migration effects, known limitations, next milestone and any design questions. If an instruction conflicts with these docs, flag it explicitly.
