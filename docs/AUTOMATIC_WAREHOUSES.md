# First mineral-chain warehouse training and startup

Ordinary Paper scheduling now reviews first-warehouse investments for saved
living residents' towns. It polls one SQL preparation or acceptance at a time,
rotates bounded town pages, and reads at most one proposed barrel per pass. It
checks the main thread, saved world, already loaded chunk and valid height. It
never loads chunks, creates tickets, changes blocks or reads container contents.
Expected economic declines wait and retry; unexpected failures are logged.
Startup diagnostics join `/civ debug observation`; shutdown cancels the adapter
before closing the SQL owner.

`reviewAutomaticWarehouseInvestment(town)` first tries read-only qualified
preparation. If no available merchant qualifies, a real first-warehouse prospect
may sponsor one unemployed local adult's commerce course. The existing approved
`resident-study`, `automatic-career-study`, `warehouse-founding` and
`warehouse-opportunities` settings supply all editable prototype terms.

This prospect must already have genuine staffed/funded workshop input gaps,
trusted current suitable finite ore for each mineral, an unclaimed nearby barrel,
and actual protected money/shared grant allowance for the full initial chain.
See WAREHOUSE_OPPORTUNITIES.md. No site, funding, goods or source is reserved by
enrollment, and no skill is awarded. One active first-merchant prospect, the
village course budget, bounded rotating candidate pages, working age, no current
job/course and no open owned firm constrain sponsorship. An already qualified
owner may still found a warehouse under the unlimited-ownership rule.

The commerce prototype gains one experience per actual future food period toward
ten, consuming one real timber unit per period after food/material protection.
Materials, rates and requirements are editable; accepted courses retain their
saved terms. Previously earned backlog cannot train a new learner. Practice,
experience, status, food accounting and checkpoint commit atomically. No personal
salary, tuition income, free supplies or instant Minecraft profession change.

Every practice period rechecks the saved barrel location and full opportunity.
Unknown/stale/partial/absent evidence, lost demand, disabled settings, unsafe ore
history or unavailable budgets pause the course without spending or canceling it.
A later viable opportunity can resume practice. Another warehouse taking the
first-firm opportunity cancels the unfinished prospect and releases its job lock,
retaining earned skill and history. Normal death, migration and employment
eligibility cancellation still apply. Reaching the target frees the merchant for
ordinary fresh-barrel/current-SQL acceptance; no requirements are bypassed.

Accepted startup transfers real capital into an empty warehouse, records the
startup grant/fee, hires one qualified worker and fixes its marker. Removing that
marker pauses operation; restoration at the same point resumes it. Assets, jobs,
money and history survive. Actual mines must still qualify/open, extract finite
goods and sell them before the warehouse has stock. See WAREHOUSE_INVESTMENT.md.

Schema63 adds initially empty `warehouse_founder_reviews` and
`warehouse_founder_courses` with stable course/town/world/location links, deferred
review foreign key and active-course index. Existing skills, firms, assets,
course terms and history are unchanged. Migration is transactional and tested for
failed DDL/retry. Missing older optional sections disable sponsorship/startup;
existing files are not overwritten. Small villages can legitimately lack workers,
training goods or funding; this path guarantees no industrial outcome.

Scope remains the initial coal/iron/toolkit chain. Wider forestry/fishing/stone/
sand catalogs, investment competition, multi-year specialization and Phase4
buildings/excavation/workplace activity remain separate work. Temporary SQLite
tests and a guarded disposable Paper probe validate this path; exact results and
remaining phase gates are recorded in PROJECT_STATUS and DEVLOG.
