# Qualified first-fishery owner preparation

FisheryInvestment.prepare is read-only SQL-owner preparation, not automatic
founding. It combines the current funded water/customer opportunity with actual
saved fishing skills, living local identity and adulthood. Founders cannot already
hold a worker job or an active study course. Owning other businesses is allowed:
the user approved unlimited ownership, while each resident keeps one worker job.
The proposed founder must satisfy both owner and first-worker requirements.

A bounded qualified founder page is sorted by saved UUID. Stable seeded sampling
chooses among current feasible water candidates and eligible founders. Repeating
or restarting unchanged preparation does not reroll or mutate any money, goods,
skills, courses, employment, site, habitat or history. A proposal carries the water
opportunity, founder, logical founding step and qualifications; it invents no dry
standing point or business/site identity. Fresh shoreline/water proof, final current
acceptance, resource recognition terms and scheduling remain unfinished.

Optional strict economy.yml fishery-founding prototype: founders-per-review64,
minimum-age-steps216, founder-fishing-experience10, worker-fishing-experience1,
worker-capacity2. Server adulthood is always enforced as the larger minimum;
worker skill cannot exceed owner skill. Missing old sections opt out; installed
files are not rewritten. These settings enable preparation, not live founding.
PopulationSession.prepareFisheryInvestment exposes preparation on its serialized
SQL owner. No Paper world access or migration; schema68 is unchanged.

No skill comes from finding water or a vanilla profession. Automatic supply-backed
fishing training is not yet connected, and the current default study catalog has
no fishing course. This preparation therefore waits for genuinely qualified saved
residents. The removed-standing-spot rule is awaiting a decision; no guessed rule
is applied to legacy fisheries. See DECISIONS_AND_OPEN_QUESTIONS.md.

Seven temporary-SQL cases cover seeded repeat/restart/read-only behavior, actual
skill thresholds, unrelated skill rejection, adulthood/death, existing worker jobs,
unlimited ownership, active study preservation, bounded qualified founder pages
and serialized owner/world pause. A strict editable/default/missing/invalid config
case is also added. Focused95 tests passed with zero failures/errors/skips.

Full Windows .\mvnw.cmd clean verify passed1,280 tests (1,278 unit/two packaged
integration), zero failures/errors/skips,7:28 at22:03:19 ET. Native API/identity and
reserve/payment/delivery/pause/restart regression passed22:04:33. Main save audited,
exact settings restored, helper archived and fixture stopped; human server untouched.
Native positive automatic founding/training remains unimplemented/unverified.
