# Gradual commerce skills

Approved October 11, 2026. Commerce is a separate saved skill; mining or smithing
experience does not automatically qualify a merchant. This first batch supports
supply-backed study and experience from real new paid warehouse upstream orders.
Automatic study for actual existing vacancies is described in VACANCY_CAREERS.md.
First warehouse/shop founder selection remains unimplemented.

## Editable settings

New economy.yml files add `work-learning.industry-skills.warehouse: commerce`.
The shared work-learning gain defaults to one experience per productive business
period, ceiling 100 and worker review budget 64. Many orders in one period still
award only one gain per actual qualified employee. Ownership alone earns nothing.
Changing the mapping or disabling work learning stops future awards. Existing
owner files are never overwritten and need an explicit warehouse mapping.

`resident-study.skills.commerce` defaults to target 10 with one timber practice unit
per successful food period. The timber represents expendable handling/packing
practice, not free stock or an accounting fee. Admins can change target, materials,
gain, student budgets and reserves. Accepted courses retain their original terms;
current food/material protection still applies. Enrollment uses the existing
trusted opportunity boundary, now used by the automatic actual-vacancy selector.
No player training command is created. A course target is not yet a universal merchant founding threshold.

## Actual work and accounting

Only fresh warehouse purchase receipts created inside the current warehouse work
transaction qualify. Each has actual producer goods, real payment and reserved
paid cargo. Current living local employees must meet saved worker requirements.
Closed/unavailable firms, absent worlds, idle periods, failed offers, old receipts,
owners without that job and workers beyond the review budget earn no experience.

Schema 56 adds three empty commerce audit tables for review terms, actual purchase
receipts and employee before/after skill changes. No asset, balance, course or past
skill is backfilled. Reviews reference the committed industry checkpoint; each
purchase can enter the commerce audit once. Goods, payment, skill and checkpoint
commit together or all roll back. Production audits remain separate: purchasing
is not manufactured output. No new cash, salary, goods or ownership waiver.

This batch does not yet award sales-only or delivery-only experience, train shop
staff, select first founders or build shops/warehouses. Those need their actual transaction
and opportunity adapters. It does not invent fake customers or use old sales to
train new hires. The canonical nine-phase roadmap remains unchanged.

## Validation

Temporary SQLite tests cover new orders, multiple orders/one gain, idle periods,
owner exclusion, disabled/unmapped learning, worker budget, saved over-ceiling XP,
restart, world absence, checkpoint/audit rollback, failed schema55 upgrade/retry,
actual course materials and strict editable configuration. Full-build and native
Paper results are recorded in PROJECT_STATUS and DEVLOG after verification.
