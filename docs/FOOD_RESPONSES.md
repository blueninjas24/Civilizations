# Published feasible food responses

When a village's saved priority is SEEK_FOOD and its latest food demand is unmet,
Civilizations now compares voluntary donations, emerald offers, timber barter,
stone barter and waiting. A seeded weighted choice publishes a **preference**, not
a promise of payment, delivery, supplier arrival or a completed trade.

The village supplies menu shows the saved preference and reminds players that other
available options remain optional. Existing Donate/Sell confirmations, `/civ bargain`
and `/civ barter timber|stone` still perform their own current checks. No preference
forces a player into a donation or a particular currency. Admin diagnostics show
every candidate's feasibility, weight, eligibility and retained explanation.

## Real prerequisites

- Donations require an active food priority, supported food, enabled donations and
  supply below the configurable future reserve target. They promise no rewards.
- Emerald offers require enabled purchases, a known treasury that can pay at least
  one emerald, reliable completed food data, completed catch-up and enough remaining
  need for at least one actual supported whole stack to earn that payment.
- Barter requires enabled mappings, actual idle non-shortage stock above its keep
  floor, the configured whole-item conversion and sufficient remaining food need/value
  for a supported stack to earn at least one item. Unknown materials are blocked.
- Pending deliveries and reserved purchase/barter food reduce remaining need. Cash
  reservations already reduce spendable treasury; goods reservations reduce stock.
- Waiting spends nothing and remains the safe fallback. Fully supplied villages or
  another selected priority do not initiate a food procurement preference.
- Candidate feasibility is village capability for a potential supported stack, not
  permission or acceptance for any particular player. Player cooldowns, permissions,
  held items, save proof and the exact quote are checked at confirmation.

There are no invented donors or recurring material production. Actual goods still
move only after explicit consent and verified player saves. A preferred response can
receive no takers; that is not recorded as a successful delivery or player penalty.

## Editable prototype policy

New server `economy.yml` files include:

```yaml
responses:
  enabled: true
  donation-weight: 6
  emerald-weight: 4
  barter-weight: 4
  wait-weight: 1
  minimum-relative-percent: 25
```

Weights accept 1–1000; relative eligibility accepts 0–100. Only feasible responses
within the configured percentage of the best weight enter the seeded draw. Edit
while stopped, then restart. Existing files without this optional section remain
disabled and are never rewritten. Partial, unknown or unsafe settings fail before
database startup. Separate donation/purchase/barter settings still control those
actions; enabling responses never enables an otherwise disabled payment feature.

The governing priority already incorporates bounded learning from earlier verified
deliveries (PRIORITY_LEARNING.md). This comparison does not claim to learn the best
supplier or payment method. Candidate weights describe configurable preferences
among affordable options, not predicted player acceptance or a full industry model.

## Saved publication, execution and outcomes

Schema 26 appends food_responses and food_response_candidates, linked to their
completed needs period. Every enabled completed period retains its seed, replay
version, eligibility threshold, all five raw candidate weights/feasibility/reasons
and selected response. Replay reconstructs eligibility and selection from these
saved inputs without reading today's settings or money. New policy changes apply
forward. Disabled and historical periods receive no fabricated comparisons.

The publication commits with food, household needs, priority, learning, appeals and
applied receipts. Failed candidate writes roll the whole period back, leaving food
retryable. Missing candidate evidence or a selection inconsistent with replay fails
explicitly. A failed upgrade leaves schema 25 and its existing histories intact.
Previous plugin versions reject schema 26; downgrade requires a consistent backup.

Publications are intentions. Existing purchase/barter receipts retain accepted
periods, actual terms, reservations/refunds and completion; food delivery receipts
retain their actual application period. A player can execute another available
response. Exact import outcomes distinguish immediate relief from added reserves;
no comparison is falsely marked as executed because stock or demand later changed.

Comparison runs once per completed economic period on the SQL owner, evaluates five
candidates and at most six supported food conversions, and publishes an immutable
cached view. It accesses no Bukkit world/entity/chunk API and forces no chunk loads.
Financial/stock changes between periods can make a published snapshot stale, so
confirmation always rechecks them. This is bounded prototype behavior; large-server
profiling and human-client balance/compatibility remain release validation.
History retains one comparison and five candidates per enabled completed period;
storage grows over time. Retention/archival and large-server profiling remain future
work; this batch does not silently prune economic history.

## Phase 2 scope

Combined with prior verified supplies, field-backed production, treasury, household
welfare, paid trade/barter/bargaining, saved priorities, actual receipt outcomes and
bounded learning, this comparison closes the basic food-economy decision loop.
It does not build housing, establish industries, create regional suppliers, choose
routes, award reputation or cause starvation deaths. Those remain in the canonical
later work or require separately approved consequential tuning.
