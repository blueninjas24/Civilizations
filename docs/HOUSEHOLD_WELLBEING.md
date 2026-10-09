# Basic household wellbeing

This Phase 2 prototype measures food/housing welfare, separate from treasury or
personal wealth. It does not add bank accounts, salaries, unemployment, starvation
deaths or population penalties. Business/employment context belongs to Phase 3.

## Editable settings

New `plugins/Civilizations/economy.yml` files include:

```yaml
wellbeing:
  enabled: true
  starting-score: 50
  recovery-per-period: 1
  food-shortage-penalty: 3
  food-crisis-penalty: 5
  crowding-penalty: 1
```

All scores/deltas allow 0..100; crisis penalty must be at least shortage penalty.
Existing files without the complete optional section keep assessments disabled.
Partial/unknown/invalid keys fail before database startup. Stop, edit and restart;
files are never overwritten. Retuning does not reset saved scores or food clocks.
Historical records retain the policy actually used. Disabled periods do not invent
missing personal observations; re-enabling continues from recorded history.

## What changes a family’s condition

Newly assessed people begin at the configurable neutral score. Each occupied home
uses the mean of its actual residents' last saved scores (whole-number floor).
New residents joining a family contribute their initial score. This is an abstract
shared welfare assessment, not cash transferred between residents.

- Supplied food and known sufficient housing: recovery, capped at 100.
- Any unfulfilled food need: shortage penalty. Once at least one current resident
  has the configured `shortages.severe-after-periods` of recorded undersupply,
  use the crisis penalty instead.
- Known crowding adds its penalty. Scores cannot fall below zero.
- Unknown food/housing is not evidence of comfort or homelessness; no comfort
  recovery is awarded. Known pressure can still be reported alongside unknown facts.

Conditions are SECURE, PRESSURED, FOOD_CRISIS or UNKNOWN, explained from evidence;
they are not wealth tiers. No-household residents retain the existing separate
needs pool; the plugin does not invent a home or pretend to assess their housing.

Current household IDs describe homes. Schema 22 saves the actual residents attached
to each wellbeing observation, including their own shortage streaks. Moving into a
different observed home retains people's scores/streaks. New occupants cannot
inherit a previous family's history merely because they use the same beds.

## Persistence and diagnostics

Wellbeing, membership, food stocks, needs, priorities and delivery application commit
in one SQL-owner transaction. A failed membership write rolls back the whole period;
repeating a completed period cannot improve/damage welfare twice. Historical records
retain the original home, residents, policy and evidence. Schema 21 upgrades without
fabricated past welfare or reset people/balances. No Paper API is used in calculation.

`/civ debug economy <village-id>` reads cached latest observations, capped at 20 homes.
Seeded tests exercise 10,000 periods of famine, recovery and changing pressure per
seed. These prove bounded/repeatable prototype behavior, not survival-world balance.
See PROJECT_STATUS.md for verified build/server results and remaining Phase 2 gates.
