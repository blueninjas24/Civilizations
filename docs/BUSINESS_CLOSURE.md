# Retained unsold-business closure

This implements the approved outcome of an expired saved sale offer. It does not
yet create offers automatically, set prices, choose buyers or simulate general
business competition. Offers currently enter through the trusted SQL-owner API.

The bounded present-world business refresh first reconciles staffing and attempts
qualified family/employee succession. If ownership is still with the deceased
seller, the saved offer has expired and no qualified successor exists, it records
closure and ends every remaining open employment episode in one transaction.
Failed job saving restores both the closure and all separations. Future job/offer
dates cannot be rewritten as an invented past closure. Historical replay retains
the original closure date rather than creating a second event.

An expired offer stops new production, hiring, trade and working-capital allocation
immediately, even before the next bounded closure visit. Inheritance into a living
qualified owner's hands can still take priority during that review. Once closure
is recorded, later training does not silently resurrect the firm. Explicit future
asset/building reuse needs its own retained transition; physical reuse is Phase 4.

Closure preserves the business identity, founder/current seller, premises, stock,
cash, mineral reserves, accepted sale offers, receipts and job history. It creates
no estate proceeds because no buyer paid. Employees become available for actual
other jobs; they are not automatically assigned to a fabricated replacement firm.
Goods already purchased remain owned cargo and can arrive on their original clock.
Exact historic production/payment/employment receipts can still replay safely,
without resuming work or paying again. Closed farms cannot silently rehire or found
a replacement under the same stable automatic-farm identity.

Business diagnostics now show the retained closure reason/date. The stale message
claiming succession was unimplemented has been corrected. No world blocks, chunks,
entities or buildings are removed by this work.

Schema 41 additively creates `business_closures` and an indexed closure history,
linked to the retained business/seller offer. Failed schema-40 upgrades roll back
and retry without losing stock, offers or employment. New temporary SQLite tests
cover expiry, qualification priority, absence, failures, chronology, actual paid
cargo, blocked new work/capital, restart and legacy-policy farm closure. See
PROJECT_STATUS.md and DEVLOG.md for completed build/native verification results.
