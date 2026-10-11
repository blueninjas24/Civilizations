# Learning through actual industry work

User approved gradual skill development on October 9. Learning by employees of
existing timed mines/workshops and automatic farm businesses is now verified.
Supply-backed study for unemployed adults is a separate batch (RESIDENT_STUDY.md).
Automatic career choice and nonfood founding remain unfinished. Learning does not
create an already skilled founding population.

Each new productive business period can give its actual living local qualified
employees a small experience gain in the configured industry skill. An owner who
is not an employee does not receive an award. Paused businesses, missing inputs,
zero output, absent worlds and closed firms do not create experience. A productive
team period earns experience per employee, not per item or claimed individual
animation. No salary, estate credit, extra worker, ownership transfer or free
resource is created. Work already uses its actual labor/material/reserve costs.

Production, resource depletion, skill changes, participant before/after audit and
runtime checkpoint commit together. Failure restores all of them. Accepted reviews
keep their settings and participants after retuning/restart; historic receipts
without learning do not gain retroactive awards. No downtime or world absence is
credited. Skills can eventually satisfy the saved founder requirements, after which
the existing qualified succession rules apply. Learning does not waive those rules.

## Editable settings

In `plugins/Civilizations/economy.yml`:

```yaml
work-learning:
  enabled: true
  experience-per-productive-period: 1
  maximum-experience: 100
  maximum-workers-per-review: 64
  industry-skills:
    mining: mining
    blacksmith: smithing
    farming: farming
    warehouse: commerce
```

These are prototype rates, not years of experience. Period speed follows the
existing `industry-runtime` cadence for mines/workshops; farming uses the food
economy cadence. See FARM_WORK_LEARNING.md for reduced productive slots and
transactional food-period awards. Gains are 1..1000; ceiling 1..1,000,000,000;
worker review budget 1..1000. Lowering the ceiling never removes existing experience.
Missing old sections keep learning disabled; edit stopped/restart to opt in. Remove
an industry entry or disable the section to stop its future learning. Invalid edits
fail before the database starts. Existing owner files are never overwritten.

If a team exceeds the review budget, production still occurs but no partial skill
awards are made for that period; the saved diagnostic explains the deferral. Raise
the budget deliberately for larger teams. The prototype has no per-day cap apart
from genuine productive periods and the saved skill ceiling.

## Persistence and verification

Transactional schema 42 adds accepted industry-learning reviews and actual employee
entries linked to production receipts. No old skills or historical receipts are
rewritten. Temporary SQLite tests cover actual mine/workshop output, supplied
inputs, unproductive work, limits, absence/restart, retuning, inheritance thresholds,
atomic rollback, conservation and failed schema-41 upgrade/retry. See PROJECT_STATUS
and DEVLOG for completed full-build and native Paper results.

Verified October 10: focused 79 tests and Wrapper clean verify 756 tests passed,
including two packaged integration cases. Isolated Paper awarded exactly eight
productive employee gains, preserved 15/8 mining experience after restart/absence
and closure, and retained existing development assets/skills. Test server stopped;
human server unchanged. Schema 42 upgrade integrity and foreign keys passed.


October 10 farm extension: schema 45 links farm participant audits to actual food
periods. Nine new temporary-DB cases and real Paper session verification passed;
full Wrapper clean verify passed 792 tests (790 unit/two integration), no failures/
errors/skips. Old files need the explicit farming mapping; they are not rewritten.
No new skill-based production bonus or automatic career/founding policy is added.

October 11 commerce extension: actual new paid warehouse upstream orders can train
current qualified employees once per work period. Separate schema56 audits preserve
real purchase identity; no phantom production or past-sale backfill. Sales/delivery
alone do not yet award experience. See COMMERCE_LEARNING.md.
