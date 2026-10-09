# Bounded learning from verified food deliveries

Villages can now give seeking food a modest extra weight when earlier verified
deliveries reduced unmet demand. This influences a seeded choice among feasible
priorities; it never creates food, a supplier, money, building permission or jobs.

Edit the server's `plugins/Civilizations/economy.yml` while stopped, then restart:

```yaml
priorities:
  # Keep the other existing priorities keys too.
  learning:
    window-periods: 12
    maximum-food-bonus-percent: 25
```

The window accepts 1–100 completed economic periods; the maximum bonus accepts
1–100 percent. Set **both to zero** to disable. New default files use 12 and 25.
Existing owner files without this subsection retain the old choices until opted
in; Civilizations does not rewrite them. Missing/unknown/partial/unsafe keys fail
validation before database startup. These are editable prototypes, not final balance.

## Evidence and choice

At each ordinary priority review, look back through the configured number of
**earlier** periods. Current-period results cannot influence that same review.
Only periods whose recorded imports exactly match applied delivery receipts count;
pending donations, reservations, hypothetical imports and unmatched input history
contribute nothing. Donations, purchases and barter share the same verified delivery
accounting. Their combined marginal impact is explained in FOOD_IMPORT_OUTCOMES.md.

The bonus is the floor of `maximum bonus × unmet units prevented / imported units`.
With no verified imports, it is zero. Twelve imported units preventing four unmet
units earn an 8% bonus under a 25% cap. All five imported units preventing five
unmet units earn the full 25%. Reserve-only arrivals earn no immediate bonus, but
are not treated as failures: later use may still be valuable. Lack of player visits
or failed price alignment creates no learning penalty or reputation change.

Enabled learning uses weights at 100 times their base resolution, so an 8% bonus
to a base weight of 10 becomes 1080 rather than rounding away. All other weights
receive the same 100-times scale; relative eligibility stays comparable. The food
crisis multiplier still applies. Disabled reviews retain the original integer
weights and exact seeded replay. Turning learning on is a forward policy change
and may change seeded choices even where evidence is initially empty.

Learning never makes a supplied village seek food, never makes unknown housing
crowded, and never bypasses affordability, stock, cooldown, permissions or saved
inventory proof. It changes a planning priority, not a guaranteed purchase. It
does not claim an appeal attracted a supplier or allocate credit to one player.
It learns that verified incoming food had an accounting benefit; it does not learn
which payment method or supplier was best.

Evidence changes alone do not trigger reviews every tick. Existing cadence and
pressure transitions still govern reconsideration. Settings changes apply at the
next completed-period review without rewriting old reviews or discarding food time.
Old evidence expires from the rolling window; no permanent escalating bonus exists.

## Saved history and safety

Transactional schema 25 adds `village_priorities.learning_replay` (old rows default
to zero), `priority_learning` and an applied-delivery lookup index. New enabled
reviews retain replay version 1 for the original choice kernel plus version 1 for
its learning extension, the window/cap and exact imported/prevented aggregates.
Old reviews retain their original facts, chosen actions and explanations. New
reviews replay from saved aggregates, not today's settings or mutable receipts.
Missing required learning evidence is an explicit error, even if a legacy choice
would happen to select the same action. No backfilled learning or past rewards.

Food stocks, needs, choice, learning metadata and receipt application share the
existing owner transaction. A failed learning write rolls all period effects back
and leaves the delivery retryable. A failed upgrade remains on the old schema;
retained legacy decisions remain readable for diagnostics. Current-schema missing
tables are errors. Downgrading to an older plugin requires a consistent pre-upgrade
backup; older versions reject schema 25. No worlds or production data are reset.

The SQL owner evaluates at most 100 historical food periods per review, with indexed
applied-delivery matching. Cached debug views show saved candidate weights, bonus,
window and rationale; no Paper world access or forced chunk loading is involved.
Large-server profiling and human-client economy balance remain future validation.

Tests cover exact ratios/caps, blocked actions over 1,000 seeds, reproducible changed
choices, disabled legacy behavior, strict configuration, migration rollback,
missing evidence, receipt mismatch, expiration, restart, forward disabling and a
240-period verified-delivery scenario. The native Paper fixture checks saved
delivery evidence affecting a later shortage review across restarts.

This completes bounded food-priority learning. The follow-up FOOD_RESPONSES.md
implements and verifies the broader feasible-response comparison. Phase 2's
prototype gate is now 4/4 (100%); see PHASE2_ACCEPTANCE.md. Industry, construction
and regional suppliers remain in their canonical later phases.
