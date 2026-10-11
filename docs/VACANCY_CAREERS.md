# Automatic study for real job vacancies

This connects approved gradual training to real vacancies at existing mines,
blacksmiths and warehouses. It does not yet choose founders, invent an employer,
open a first industry, assign a salary or turn a profession into free experience.

## How it works

The timed SQL-owner business review runs ordinary qualified hiring first. If a
living local owner still has a real opening, the bounded study selector considers
unemployed local adults without an operating owned business or active course.
It checks the saved job's missing skills against editable courses and their
reachable targets. Already qualified people need hiring, not unnecessary study.
One missing supported skill is chosen in stable order; multiple unsupported gaps
make a candidate infeasible. Courses share the existing village student/course
budgets, and active sponsored courses cannot exceed remaining job openings.

Every business receives at most one saved career review per completed food period.
Its candidate cursor survives restart, so small pages can eventually reach later
residents without repeating the whole population every tick. Enrollment starts
after all already earned economy time. Untimed status refreshes cannot enroll.
Present-world initialized economy and complete food evidence are required.

Enrollment spends no goods, creates no skill and gives no job. Actual future study
uses the existing food and material reserves, actual practice supplies, student
budget and saved course terms. When qualified, normal hiring can fill the job.
Existing workers are not poached; one resident still has only one worker job.
A sponsored course cancels before spending if its job fills, closes or loses its
living local owner. Manual courses remain independent. Previous earned skills and
course history remain. An opening is an opportunity, not a guaranteed job promise.

## Editable settings

New economy.yml files contain:

```yaml
automatic-career-study:
  enabled: true
  candidates-per-review: 64
  enrollments-per-review: 1
  industries: [mining, blacksmith, warehouse]
```

Study must also be enabled under resident-study, with courses able to meet actual
saved job requirements. Automatic hiring needs automatic-business-staffing enabled.
Farm staffing retains its separate existing path. Missing old career sections opt
out. Admin files are never overwritten. Edit stopped/restart; invalid keys, types,
duplicates, farming entries or excessive budgets fail before database startup.
Disabling new automatic enrollment does not erase accepted courses; resident-study
controls their future work. No universal founder threshold is introduced here.

## Persistence and verification

Schema57 adds empty career review and enrollment-link tables, with course, business
and food-period foreign keys. Choices, course terms, candidate cursor and economy
clock accrue atomically. Failed enrollment or audit rolls back all those effects.
Historical manual courses and old businesses are not reinterpreted or backfilled.
Business diagnostics expose why and how many residents enrolled; normal study
summaries explain food/material/budget pauses.

Temporary SQLite tests cover real learning followed by normal hiring, no duplicate
courses or fake employer, disappearing openings, manual-course independence,
owner loss, disable/absence/incomplete evidence, target feasibility, bounded saved
candidate rotation, actual supplies, audit rollback, past-backlog exclusion,
untimed refresh exclusion, restart and failed schema56 upgrade/retry. Full-build
and native PopulationSession results are recorded in PROJECT_STATUS and DEVLOG.

Next: safe suitable opportunities and funded first founders, then broader industry
mixes and multi-year specialization scenarios. The canonical nine-phase roadmap
and physical construction boundary remain unchanged.
