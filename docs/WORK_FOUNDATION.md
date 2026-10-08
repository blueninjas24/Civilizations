# Work and skill foundation

Schema v9 stores nonnegative 64-bit experience counters per resident/skill.
Counters belong to permanent resident UUIDs, survive moves/restarts/snapshots and
remain after death. `recordSkillExperience` writes the supplied total, so repeating
a save does not double learning; no training rate is assumed. Skills are caller
keys, not a locked commodity/industry catalog. Missing skills mean zero experience.

`WorkProfile` and `JobRequirements` are immutable plain-Java inputs. `WorkEligibility`
rejects deceased/underage/unqualified candidates and lists unmet requirements in
stable order. Minimum age and skill thresholds come from the actual future job
definition; there is no invented retirement age, salary or employer.

This completes the Phase 1 qualification foundation alongside saved households
and family links. It does **not** assign occupations or hire people: a real job
must refer to an actual business/site/opportunity when those systems arrive.
Phase 2 labor/economy and Phase 3 businesses remain separate roadmap gates.
Learning/training, prosperity and employment effects are not yet simulated.

Five domain and five new persistence tests cover exact thresholds, deceased/young
candidates, missing skills, explanation order, immutable inputs, identity mismatches,
restart/retry, large counters, snapshots, invalid storage and v8 upgrade recovery.
The migration integration test also verifies workers retain skills after moving.
All database tests use temporary directories; no Minecraft/server DB is touched.
