# Automatic business resale

When a saved owner dies, eligible qualified family members inherit first, then
qualified employees. If neither qualifies, the present-world SQL owner can create
a sale offer and review actual local buyers. This does not found new industries.

The user approved unlimited private business ownership on October 10, 2026.
Qualification, actual funds and one worker job per resident still apply. Government
exceptions belong to Phase 6; no government behavior is implemented here.

## Editable prototype settings

`src/main/resources/economy.yml` documents `automatic-business-resale`. Existing
owner files without this section stay disabled until explicitly configured.
Defaults are a minimum site value of 1,000 hundredths, 50% recovery of saved startup
cost, ten minutes of eligible online sale time and 32 buyers per business review.
The review budget limits work per pulse; it does not cap population or ownership.

Offer price is the greater of minimum site value or recovered startup cost, plus
saved business cash, owned paid cargo at invoice cost and actual stock at configured
unit values. Unextracted reserves and speculative future profit are not valued.
Unpriced positive stock or numeric overflow defers valuation with an explanation.
Accepted prices and deadlines survive admin retuning and restarts.

Funding uses the existing `business-resale-funding` limits. A buyer can use one eligible
family estate and one owned local operating business, retaining its configured
cash buffer. The buyer cannot use the selling family's proceeds to finance that
same sale. The transferred business keeps its own cash, stock, pending cargo,
immutable founder, site and history. No personal wallets or new grants are added.

## Safety and diagnostics

Buyer review rotates through a saved UUID cursor and uses the seed to order each
bounded page. No qualified affordable buyer means waiting, not invented wealth.
Listing, payment, estate proceeds, current owner and review commit together.
Already expired offers close under the existing retained-assets rule before a
purchase is attempted. A failed sale-clock boundary disables automated purchasing
until reviewed restart. Absent worlds and server downtime do not consume deadlines.

Schema 44 adds latest lifecycle review/cursor data and a local portfolio index.
It preserves existing business, skill, estate and accounting tables. Per-business
diagnostics explain price, reviewed candidates, insufficient funding or refusal.
The latest review is replaceable; ownership, payments and accepted offer history
remain saved independently.

## Limits

This is a fixed-price prototype sale process, not an auction or negotiation model.
Automatic review spends from one business and one estate; it does not aggregate
an entire portfolio. There is no forced sale of a living owner's business, worker
reassignment, salary system, government simulation or physical building reuse.
Automatic first mines/workshops, career selection and wider competition remain
separate Phase 3 work. Physical growth remains Phase 4.


Validation on October 10: Windows Wrapper clean verify passed 783 tests (781 unit,
two packaged integration), no failures/errors/skips. Guarded disposable Paper passed
automatic four-firm acquisition, five total owned firms, conserved funds, absence
and restart plus prior industry/study/lifecycle checks. Schema44 integrity/FKs and
all 16 preexisting asset/skill/audit table snapshots passed after normal shutdown.
Human server unchanged. This does not complete the full Phase3 competition gate.
