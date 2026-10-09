# Persistent business and staffing foundation

This describes the first Phase 3 backend batch. Subsequent automatic field
enterprises connect these APIs to funded founding, staffing and food output;
see AUTOMATIC_FARMS.md for current behavior and owner opt-in. Other industries
remain backend work. No buildings, personal wallets or salaries are created.

## Implemented records

- Business UUID, settlement, name, original founder/current initial owner, industry
  key, site UUID, worker capacity and founding logical month.
- Site UUID, permanent world UUID, coordinates, observation time and trusted evidence.
  A site describes an existing workplace. Evidence is not ownership or permission
  to change player blocks. Duplicate anchor coordinates cannot create two sites.
- Immutable founding age/skill requirements, explicit startup Trade Value cost and
  opportunity reason. These are caller inputs; no numerical gameplay defaults or
  automatic founding thresholds have been chosen in this batch.
- Employment UUIDs, resident/business references, start/end logical months and
  separation reasons. Ended jobs retain their identity and history.

`BusinessRepository` runs only on the JDBC owner's thread. `found` checks actual
saved founder age, life status, settlement and skill totals through the existing
`WorkEligibility`, and checks the town's permanent world binding. The caller must
establish reliable physical site evidence and opportunity first; passing text is
not a world scan, land-rights check or resource-discovery implementation.

Registration and all qualification rows share a transaction with an explicit
settlement-treasury debit when startup cost is positive. Missing/insufficient funds
fail without a partial business, site or debit. A zero-cost request must be supplied
explicitly; it is not an automatic free-business policy. Positive debits use a
namespaced business-derived operation UUID. Retrying exactly the same business UUID
replays the founding terms even after later death/movement, with no second debit;
changed terms under that UUID fail. It does not seed additional funds or claim
business income. Which future business opportunities receive village investment
remains a planner/policy concern.

Hiring checks actual local living residents against that business's saved worker
requirements, its founding date and finite capacity. Ownership alone is not a job.
A partial unique index permits one current primary job per resident, preventing
double-counted labor. This is a provisional staffing boundary, not a permanent ban
on future part-time work or multi-business ownership. One registered site belongs
to one business initially; shared premises/relocation require deliberate later work.

Exact employment-operation retries do not hire twice or reopen an ended job.
Separation is retry-safe under identical dates/reasons. `currentWorkers` immediately
excludes dead/migrated residents; historical open assignments are not productive
labor. The future owner loop must call `reconcileEmployment` before new hiring or
production to end stale assignments and free reserved capacity. It records the
caller-supplied reconciliation month, not a fabricated exact past departure date.
Death/migration themselves remain authoritative in population storage.

## Migration and verification

Schema 27 adds four tables and staffing/settlement indexes transactionally. It
neither seeds businesses nor rewrites earlier population/economy history. Foreign
keys retain founders, owners, residents and sites. Older plugin versions reject
schema 27: downgrade requires a consistent pre-upgrade backup, not deletion of
business history. No production database is used in tests.

Temporary-SQLite tests cover complete restart/snapshot persistence, finite startup
capital, ledger-write rollback/retry, eligibility, wrong worlds/towns, duplicate
sites, double jobs, capacity, separation/rehiring, death/migration reconciliation,
failed writes, nested transactions and schema-26 failed-upgrade recovery. A seeded
240-month staffing turnover fixture retains every episode and verifies capacity
and once-only startup spending through restart. This is a staffing test, not a
claim of simulated industry turnover or long-run economic balance.

## Next steps and limits

Verified October 9, 2026: Windows `.\mvnw.cmd clean verify -B -ntp` passed 506
cases (504 unit/two packaged integration), zero failures/errors/skips. Disposable
pinned Paper schema-26-to-27 startup/restart, status/observation diagnostics and
final clean stop passed. Read-only integrity/FKs and checked old identity/history
preservation passed; no businesses were auto-created. An initial noninteractive
console launch closed stdin and required stopping that verified disposable process;
the corrected persistent-console launch and normal shutdown passed afterward.

The subsequent BUSINESS_RUNTIME.md batch wires bounded immutable business views
and automatic invalid-assignment reconciliation to the owner/diagnostics. Next,
observe reliable existing sites, configure prototype founding/hiring opportunities in the
admin economy file, and apply eligible saved assignments automatically. Then add
resource-supported production and its integration with commodity accounting,
without double-counting the existing field-backed food source.

Resource potential/depletion, production chains, working experience, prosperity,
competition, closure/succession and specialization are still planned. There is no
resource output or recurring revenue from registration alone. The foundation read
API currently reconstructs each business's saved requirements individually; bound
live queries/caches and measure larger workloads when wiring the scheduler. Full
historical age/skill snapshots per hire, site footprints/revalidation and shared
premises are not implemented. Existing immutable requirements explain qualification
rules but are not a complete historical skill ledger. Keep the Phase 3 gameplay
milestone unchecked until runtime and relevant Paper gates pass.
