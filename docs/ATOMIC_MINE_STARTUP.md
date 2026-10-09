# Atomic funded mine startup

MiningStartupRepository composes business/site founding, coordinate-based finite
discovery, empty business storage, actual working-capital transfer and one qualified
first worker in one SQL-owner transaction. Any failure rolls back every new part,
including startup expenditure, working-capital debit, partial coordinates, stock
catalog and employment. Retry cannot double-spend or create another reserve pool.

Existing public repository operations continue to require an idle owner and own
their transactions. Small package-local row helpers require an active transaction;
they neither start nor commit it. These helpers are not a public bypass or a new
service framework. Qualification, permanent world identity, capacity, no double
primary jobs, sticky mutation exclusions and saved finite discovery terms remain.

The first-worker operation is determined by business UUID, not whichever candidate
is available on retry. Changing the requested first worker is rejected. Replaying
an ended episode does not resurrect employment. Existing founding/store terms must
match retained history; depletion and owned stock are returned as current state,
never reset to opening state. Legacy site-only geology is not automatically adopted.

Startup expense is an explicit retained treasury expenditure. Working capital is
transferred to the store, not created; stores start with zero goods. Storage must
support every extraction product, including byproducts. Request terms are explicit
trusted caller inputs, not newly chosen gameplay defaults or a command available
to players. Automatic opportunity selection still needs actual demand, observation
freshness, location suitability, founder/labor and funding checks before using this
seam. No automated mine founding, recurring extraction, physical work or new schema
is enabled here. No native block ownership or reserve regeneration rule changes.

Temporary SQLite tests cover atomic successful startup/depletion/restart replay,
injected storage and final-hire failures, combined capital refusal, stale/partial/
wrong-world evidence, active/idle transaction contracts, unsupported byproduct
storage/deceased qualification, changed first worker and competing existing jobs.
The packaged native paid-chain fixture now uses this seam with explicit startup
expense and working capital, then checks replay after extraction, paid delivery,
recipes and capital minus the known startup expenditure. Windows Wrapper clean verify passed 649 tests (647 unit and two packaged
integration), zero failures/errors/skips. The disposable Paper fixture passed with
blocks restored, normal shutdown, no retained force-loading ticket, unchanged main
industry/funds/history and owner settings, and integrity/foreign-key checks.
This proves the trusted startup seam, not automatic natural mine selection.
