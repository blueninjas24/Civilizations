# Phase 2 prototype acceptance

This gate covers the existing food/housing economy foundation, not a balanced
survival release. Human client testing was explicitly deferred by the user.
Completion must be supported by final Wrapper and disposable Paper verification.

| Roadmap reporting group | Implemented acceptance evidence |
|---|---|
| Supplies, demand, reserves, shortages | Commodity catalog, exact period accounting, duration/recovery, immutable history and forward tuning; real planted-field observation without forced chunks |
| Basic labor, treasury and welfare | Adult/field capacity limits output; finite once-only opening treasury, actual debits/refunds; family allocation and resident-linked food/housing welfare separate from money |
| Explainable autonomous choices | Real pressure, feasible weighted priorities, bounded review/learning, published donation/cash/material/wait comparison, saved blocked reasons and exact seeded replay; real confirmed responses retain distinct execution receipts and future food outcomes |
| Player prices/trade and long-run stability | Configurable famine/feast and prices, donations, finite-budget emerald sales, bargaining/cooldowns, finite-stock barter; 240-period delivery/learning/publication and mixed two-village cash/material/food conservation fixtures, restart and failure recovery |

All tests use test-owned temporary SQLite databases. Migrations retain identity,
history and prior choices; failure tests roll back effects and retry. Real Paper
validation exercises native menus, saved player proof, failed saves, explicit
confirmation, duplicate protection, actual food/emerald/log exchange, future receipt
application, welfare recovery, learned choices, publication and unloaded operation.

**Gate passed October 9, 2026.** Final Windows `.\mvnw.cmd clean verify -B -ntp`
passed 494 cases (492 unit/two packaged integration), zero failures/errors/skips.
Pinned disposable Paper 26.2 build 129 passed saved feasible response publication,
learned later choices and all native donation/payment/barter/restart/welfare/unloaded
regressions. Final plugin-only startup/diagnostics/shutdown passed. Schema 25 -> 26
integrity/FKs and old identity/economy/learning histories were checked read-only
against a consistent pre-upgrade dev snapshot; no owner config or worlds were reset.
Native comparison completeness/selected feasibility and one cash/barter completion
were independently checked. PROJECT_STATUS.md and PROGRESS.md record 4/4 (100%).

Remaining release and later-phase limits: human UX and inventory-plugin/native-death
compatibility; cross-save backup restoration; sustained server profiling and survival
balance; actual businesses/jobs/nonfood production (Phase 3), physical housing and
construction (Phase 4), regional trade/routes (Phase 5), reputation/government/history
(Phase 6+). No fake industry income, supplier success, automatic player debit or
approved block modification is implied by Phase 2 completion.
