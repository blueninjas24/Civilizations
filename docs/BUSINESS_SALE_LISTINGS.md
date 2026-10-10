# Saved business sale offers

This is the offer and timing layer for the approved business resale rules. It does
not yet automatically value or advertise a business or select a buyer. Retained
unsold closure is now connected; see BUSINESS_CLOSURE.md. Qualified inheritance
still takes priority. Existing qualified staff
can continue working under the separately configurable business-operations policy.

## Accepted terms and payment

The internal SQL-owner `PopulationSession.openBusinessSale` API accepts an explicit
positive price and a deadline of 1 second through 24 hours. These are trusted caller
arguments, not new admin commands or automatic valuation settings. A new offer
requires a deceased current owner, no qualified heir/employee and a present world.
Replay keeps the saved original price, deadline and elapsed time; changed terms
are refused. Historical offers remain tied to the business and seller identities.

For a listed business, `sellBusiness` requires the exact saved price and an open,
unexpired offer. Buyer eligibility and actual business/estate funds are checked
again. Payment, estate credit, ownership, audit and SOLD status commit together.
Any failed completion restores all of them, including if a save trigger removes
the accepted offer. Exact accepted-payment replay never pays again. Explicit
unlisted backend sales remain supported for existing internal callers.

Ordinary eligibility, affordability and expired-offer refusals are distinct from
SQL failures; they do not incorrectly report a population save failure.

## Deadline policy

Earned time advances only while the server and business world are available.
Startup moves observation anchors without adding downtime. World boundaries
account for the prior world set before changing it; absent intervals advance the
anchor without spending the deadline. Unloaded chunks in a present world do not
pause the data-only clock. Backwards clock readings cannot subtract earned time;
large forward intervals clamp to the saved deadline without overflow.

A failed pulse rolls back clock changes for retry. A failed world boundary cannot
safely reconstruct an interval, so new sales pause until the cause is fixed and a
reviewed restart occurs. Existing accepted payment replay remains safe. The internal
sale diagnostic reports this condition. Each observation reviews at most 4,096 open
offers; exceeding that limit defers the whole review rather than partially spending
deadlines. A future larger-server implementation needs a bounded paging strategy.

An owner transfer marks the old offer RESOLVED. Deadline expiry marks it EXPIRED
and refuses new payment. The separate bounded closure review now records retained
closure and ends jobs after inheritance priority; expired firms stop new work
immediately. Neither layer deletes assets. Automatic offer creation, editable
valuation/deadline policy and qualified funded buyer selection remain next.
No automatic live offers exist yet.

## Persistence and verification

Transactional schema 40 adds `business_sale_listings` and an open-offer index.
It retains all schema-39 business, estate, payment, site and stock history. Failed
upgrade/retry and SQLite foreign-key integrity are covered by temporary databases.
Tests use separate temporary saves, never a Minecraft server database.

Nine added cases cover saved terms, paid completion, inheritance priority,
online/absent/restart timing, expiry, backwards/large time, migration recovery,
failed world boundaries and atomic payment rollback. See PROJECT_STATUS.md and
DEVLOG.md for the completed full-build and native Paper results.
