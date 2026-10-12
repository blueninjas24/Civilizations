# Opportunity-backed mining founder training

This connects the approved gradual supply-backed training to the first owner of a
real mining opportunity. It does not automatically open first warehouses/workshops
or assign free professions to existing villagers.

The same current selection checks used for mine opening must pass: observed exposed
coal/iron, provenance/freshness/world/range/anchor guards, actual funded workshop
customers, a qualified staffed warehouse buyer, no sufficient existing mine capacity,
and affordable protected startup funding. No hypothetical employer is created.

When no qualified unemployed founder exists, at most one eligible adult prospect
per town studies mining. Existing `automatic-career-study`, `resident-study` and
`automatic-mines` settings control enrollment budgets, minimum age/experience,
practice materials, learning speed and grant terms. The mining course must reach
the configured founder requirement. Missing/disabled legacy settings do not enroll.

Enrollment saves a source anchor and a review but creates no skills, job, mine,
stock or spending. Study starts only after all already earned food periods; each
future eligible period consumes real timber/stone under existing food/material
reserves and student budgets. Partial learned skills remain if a course is cancelled.
No operating owner is drafted into study; qualified owners retain unlimited private
ownership and still may hold only one worker job.

Each learning transaction rechecks the linked opportunity. Lost ore, customers,
funding or source trust cancels the course before practice goods are spent. Absent
worlds do not process courses. Completion makes the resident eligible for ordinary
mine selection, which rechecks demand, sources, funds and employment before saving
all mine assets and grant payment. Another eligible founder may take the opportunity;
no permanent ownership promise or startup-money reservation is made by training.

Schema 59 adds empty `mine_founder_reviews` and `mine_founder_courses` tables with
world/course/review foreign keys and a bounded enrollment index. Existing courses,
assets, balances and history are retained. Retry/rollback and restart must preserve
identity and once-only supply consumption.

## Verification

Ten tests added; focused46, then23 mine tests passed. Full Windows Wrapper verify:
1,019 passed (1,017 unit/two packaged integration), zero failures/errors/skips.
The compiled native Paper probe also verified real SQL-owner training, twenty
material-spending entries, qualified mine startup, absent-world pause and restart.
See PROJECT_STATUS.md for fixture corrections and complete restoration audit.
