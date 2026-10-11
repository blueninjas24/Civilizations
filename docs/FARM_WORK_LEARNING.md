# Learning through actual farm work

Saved employees of the automatic field enterprise can gain experience through
newly processed productive food periods. Food, skill changes, participant audit,
needs/decisions and the economy checkpoint share one transaction. A failure rolls
them back together. Previously completed food periods are never backfilled.

This uses the approved gradual work-learning rule and existing `work-learning`
settings. New defaults add `farming: farming` to `industry-skills`, with one point
per productive period and ceiling 100. Mining/workshops follow industry work speed;
farm learning follows the existing food economy period. No production multiplier,
money, salary, extra labor or ownership qualification waiver is introduced.

Existing owner files remain unchanged. Add the farming mapping to opt in; a missing
mapping or disabled learning section awards no farming experience. Changing the
ceiling never removes already earned skills. Edit while stopped and restart.

Only actual living, local, qualified employees can learn. Ownership alone does not
count as work. Legacy food accounting that uses adults without business employment
does not award skills. Missing/partial observations, absent worlds, downtime,
closed farms and zero output cannot generate farm experience.

When crops decline, saved employment may exceed the observed productive slots.
Only `min(employees, observed field slots)` participate in learning, rotating in
UUID order by economic period. This determines abstract work participation, not
physical animations or forced job changes. If the worker review budget is exceeded,
all awards defer; food production still follows its existing accounting rules.

Schema 45 adds farm review and participant tables linked to actual saved food
periods. It preserves existing farms, food history, skills, money and identities.
Diagnostics append the latest learning explanation to the farm business status.
Learning can eventually qualify a worker for succession; saved founder thresholds
and family/employee priority still apply.

Automatic career selection, opportunity-funded mines/workshops, broader labor and
investment competition remain separate work. Physical farm expansion is Phase 4.


Validation on October 10: nine temporary SQLite cases cover production, replay,
absence/restart, reduced slots, legacy/missing/disabled settings, ceilings/budgets,
rollback after awards, death/closure, earned succession and failed schema 44 upgrade.
Focus 90 passed. Windows Wrapper clean verify passed 792 (790 unit/two integration),
zero failures/errors/skips. Guarded Paper PopulationSession checks passed with
synthetic field evidence and actual saved output/skills, plus prior industry tests.
Stopped fixture schema 45 integrity/FKs and all 20 saved asset/skill/course/audit
snapshots passed. Human server unchanged. Full Phase 3 gate remains unfinished.
