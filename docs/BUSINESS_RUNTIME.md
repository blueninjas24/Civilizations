# Live staffing maintenance and business diagnostics

The plugin now visits saved businesses on its database owner thread after each
population pulse. It finishes assignments whose saved resident died or moved out
of the town, preserving the employment episode and an observed separation reason.
Current-worker queries already excluded those residents; this bridge releases the
retained assignment and prevents it blocking later qualified hiring.

Absent worlds skip staffing writes. On return, the owner records the separation
at the current administrative month, rather than inventing the exact past departure
date. Owner death is reported as unavailable; it does not silently choose an heir,
close a firm, transfer assets or delete history. Ownership is not employment.

## In-game inspection

An administrator can use `/civ debug businesses <settlement UUID>` to read:

- Saved business name, industry and identity, plus world availability.
- Founder, owner and whether the owner is still a living resident of that town.
- Living local saved worker count, capacity, vacancies and open assignments.
- Saved site position, founding month and latest retained job separation reason.

Commands read an immutable cache. They do not open SQLite, scan a world, register
businesses, assign jobs, generate goods or spend money. The existing
`civilizations.debug` administrator permission applies. During startup, requests
report that the owner is not ready; malformed IDs have no side effects.

## Bounded work and consistency

Production defaults visit eight businesses per population pulse, using persistent
business UUID order and an in-memory cursor that wraps rather than starving late
firms. Reconstructed details retain at most 1024 businesses, and commands show at
most 20 per town with truncation/cache counts. These are engineering budgets, not
business size or economic balance rules. Constructor budgets permit accelerated
tests. Existing schema caps a firm's workforce at 1000; a single SQL operation may
still take longer under large workload/contention, so these are count bounds, not
a CPU-time guarantee. Larger-server profiling remains required.

Cached details are sampled during that firm's latest turn; an unvisited firm's
worker/owner numbers can temporarily lag a new death or move. Eligibility flags
update across the entire retained cache when world availability changes. The
authoritative worker query excludes dead/moved residents immediately, independent
of this diagnostic cache. No production should infer workers from cached text.

Each business's staffing reconciliation is transactional. A failed separation
retains its old rows and fails explicitly; the owner retries that page. A pulse is
not one transaction across all businesses, and completed population work remains
saved if later staffing maintenance fails. Cache publication occurs only after
a successful refresh. Restart reconstructs the cache from saved records.

No migration is needed: schema 29 and existing employment history supply this
scope. This batch adds no automatic site recognition, founding, hiring, production,
succession, competition, revenue or salary system. Existing food production still
uses its prior field-backed accounting; future business integration must avoid
counting the same labor/output twice.

Tests use temporary databases and controlled session clocks for active/paused
staffing, owner death, immutable snapshots, bounded rotation/eviction, write failure
and retry, actual session startup/world return, and admin command safety. Exact
build and native smoke-test results appear in PROJECT_STATUS and DEVLOG.
