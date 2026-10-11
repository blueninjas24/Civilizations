# Existing business vacancy hiring

Existing nonfood businesses can hire already qualified, unemployed local residents
into their saved vacant worker slots. The current owner must be alive and local.
Absent worlds, closed firms and expired sale deadlines prevent hiring. Deceased
owners' firms can retain their existing staff under the approved operation policy;
this does not grant anyone new hiring authority before succession/sale.

New economy.yml files include:

```yaml
automatic-business-staffing:
  enabled: true
  candidates-per-review: 64
  hires-per-review: 2
  industries: [mining, blacksmith, warehouse]
```

Missing legacy sections remain disabled. Edit while stopped and restart. Candidate
budget 1–512 and hires 1–16 apply per business review, within existing business-page
budgets. Industry names are a distinct bounded list; farms keep their own existing
food-cycle staffing adapter. This adds no firms, wages, money, skill awards or
physical workers. Canonical employment influences production/prosperity rather
than individual payroll; the separate prosperity adapter remains future work.

Candidates are saved living residents in the same settlement with no current job.
Saved working age and every skill requirement must be satisfied. Ownership can
remain unlimited, but each resident still holds one worker job. Current workers
are never poached. Stable UUID order and rotating bounded candidate pages prevent
an early unqualified page from hiding later candidates. This is a prototype scarce
labor allocation, not a complete preference/wage competition model. Cursor state
is bounded and can restart from the beginning; accepted jobs prevent duplication.

Each real hire and its immutable policy/explanation audit commit atomically.
Failed audit insertion rolls back employment and preserves the candidate page for
retry. Rehire creates a distinct employment episode, even within one saved month;
ended jobs are never resurrected. Existing histories and manually accepted jobs
receive no invented audit or new employment. Disabling automatic hiring retains
current workers, employment history and normal reconciliation. A hired adult's
existing study course follows the established cancellation rule at its next review.

Schema 52 adds empty `business_hiring_reviews`, referencing existing employment.
No resident, skill, money, stock, store or old job is rewritten. Temporary SQLite
tests cover qualifications, capacity/budgets, competing firms, page rotation,
audit rollback/retry, absence/owner death, closure, restart/disable and migration.
BusinessStaffingProbe uses actual PopulationSession owners in a separate marked
disposable save to check productive hiring, death replacement and world/restart
pause. DEVLOG.md records completed verification.

Opportunity-based study, selecting founders, automatic first nonfood industries,
profitability-driven staffing adjustments and wider specialization remain separate
milestones. Hiring into a real saved vacancy does not certify a suitable new site,
public startup grant, hidden resource or future business opportunity.
