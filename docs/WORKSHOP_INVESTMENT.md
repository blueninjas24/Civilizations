# First-workshop investment preparation and acceptance

This is a trusted SQL-owner application path, not a new gameplay command or an
automatic scheduler. It connects current first-workshop opportunities to a genuinely
qualified owner, fresh Paper block evidence and one atomic paid startup. First-owner
training and the ordinary server scheduling adapter are still separate work.

## Qualification and configuration

Optional `economy.yml` workshop-founding section:

```yaml
workshop-founding:
  enabled: true
  founders-per-review: 64
  minimum-age-months: 216
  founder-smithing-experience: 10
  worker-smithing-experience: 1
  worker-capacity: 2
```

These are editable prototype settings. Existing files lacking the section opt out.
Minimum age also respects configured population adulthood. No workstation grants
skills. Founders need saved smithing experience and must be living local adults,
without a current employee job or active study course. Ownership is not employment;
no ownership-count limit is added. Qualification filtering precedes the bounded SQL
page, followed by normal domain eligibility validation. Feasible markers/owners
are selected deterministically using the simulation seed and settlement UUID.

Existing workshop-opportunities controls actual startup/capital/storage costs,
marker range and evidence lifetime. Starter-supply prices/reserves/caps, protected
shared grants, normal transport delay/haul limits and recipes remain their existing
editable policies. This adds no free capital, goods, skills or personal bank account.

## Handoff and accounting

1. SQL owner prepares a Draft using current marker evidence, actual productive
   farm toolkit need, uncovered local demand, protected food/grant affordability,
   real surplus materials, supported storage and a qualified available owner.
   Preparation saves/spends/reserves nothing. Stable business/site/order UUIDs
   derive from town, permanent world and fixed anchor; repeats do not reroll.
2. Main-thread adapter checks the actual expected smithing table in a loaded chunk
   and provides its point Confirmation. The database never calls Paper.
3. SQL acceptance rechecks current opportunities, exact configured proposal/recipe/
   survey terms, worker availability and ordinary founding qualifications before
   funding. A newer survey, changed need, food, capital, stock, policy or job makes
   the old proposal retry rather than trusting stale facts. Fresh world/point proof
   is mandatory. Session acceptance defers while population work or economy failure
   leaves the current evidence unsuitable.
4. Grant, site/firm, first job, storage, real starter payment and reserved cargo,
   permanent marker attachment and audit commit together. Failure leaves none of
   the new effects. Existing accepted terms can replay without charging, refilling,
   resetting a confirmed pause or binding a legacy business.

Paid inputs arrive through the normal timed industry path. A productive recipe
receipt creates real toolkits and learning; a working public farm can buy one using
its existing protected upkeep policy. Ownership and cash transfers are accounted
for, with delayed cargo reserving the farm's finite storage. A temporary SQLite
scenario verifies the entire acceptance-to-toolkit-to-funded-farm purchase path.

## Verification and remaining work

Ten isolated temporary SQLite cases cover read-only preparation, deterministic
selection, qualification/page limits, current food/funds/materials, changed jobs/
study/age/skills/observations, replay/restart/pause, final-trigger rollback, actual
SQL-owner API and the productive paid farm purchase. Two strict configuration cases
cover editable/legacy/invalid settings. Native WorkshopInvestmentProbe uses a saved
explicitly qualified test resident and synthetic working-farm proof, then the actual
loaded Paper table and normal SQL-owner acceptance/point-monitor paths. Its temporary
block is restored and its test-only ticket released. It does not demonstrate gradual
qualification of an ordinary newly discovered village; no skill is awarded by the
plugin merely for this test setup. Final results are recorded in status/devlog.

Schema61 unchanged. Automatic first-owner training and startup scheduling, first
warehouse startup, additional industries and specialization/long-run acceptance
remain. This path is deliberately narrow: first blacksmith/toolkits for an actual
public farm customer, not a rule that every village must have every business.

Final verification: `.\mvnw.cmd clean verify` passed 1,079 tests (1,077 unit +2
packaged integration), zero failures/errors/skips, 6:34. Native loaded table proof,
SQL-owner acceptance and marker lifecycle passed. Normal stop, zero forced chunks,
99/106 original main-save tables unchanged; remaining changes only normal clocks,
tracking, masks, resident age and farm observation time. Identity/assets/cash/history
unchanged; integrity/FK healthy. Helper archived, exact properties restored,
backups retained. Human MinecraftDevServer not modified.
