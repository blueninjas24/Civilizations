# Food bargaining prototype

Hold a plain supported food stack near a recognized village and use
`/civ bargain 5` to ask for five emeralds for that whole stack. The village checks
its current shortage, food value, remaining purchase need and available funds.
It accepts an affordable price or counters with the current maximum whole-emerald
payment. Nothing moves during this discussion. Use `/civ bargain accept` to confirm
the latest agreement/counter before it expires. An optional village UUID selects
one village when nearby settlements overlap.

Confirmation uses the existing saved-player-proof purchase pipeline. The same
food type/count must still be held, without custom metadata; changing the stack,
walking away, changed need/budget, an expired or superseded offer can prevent the
sale. Money and food exchange only after the owner revalidates terms and a saved
PREPARED record is independently verified. The accepted stack becomes emeralds
in its original slot. Completion credits future food once. See FOOD_PURCHASES.md
for recovery and cross-save compatibility limits.

## Editable server settings

In `plugins/Civilizations/economy.yml`, edit while stopped and restart:

```yaml
bargaining:
  enabled: true
  maximum-failed-offers: 3
  cooldown-seconds: 60
  offer-seconds: 45
```

These prototype defaults are enabled in new files. Existing files are preserved;
add the complete optional section to opt in. Attempts accept 1..100, cooldown
1..86400 seconds, and offer validity 1..3600 seconds. Unknown keys, missing fields
and invalid values fail configuration validation. Purchases and food previews
must also be enabled; their existing prices, exchange rate, need target and budget
still control affordability. No extra currency or recurring money is created.

An excessive asking price counts as one failed alignment. Further excessive
offers before the current conversation expires retain the count and original
deadline. At the configured limit, discussion closes for a saved player/village
cooldown. Reopening menus, changing food, reconnecting or restarting cannot clear
an active cooldown; quick menu sales also respect it. A different village has its
own conversation. After expiry or cooldown, a new discussion can begin. A price
alignment resets failed attempts. There is no reputation penalty for bargaining.

SQLite schema 23 appends discussion history and its actual policy, requested
price, counter, deadlines and immutable goods terms. Reserving an accepted offer
marks it RESERVED in the same transaction as its treasury reservation. Saved
history is not a second payment ledger. Retry of the same operation does not
consume another attempt. Existing reserved purchases can recover even when new
bargaining is disabled. Back up SQLite and Minecraft player/world data consistently
before upgrading. Existing identities, population, welfare and finance history
are retained; no reset or old configuration rewrite is needed.

## Scope and validation

This first bargaining interface uses commands; the supplies menu still provides
quick acceptance. Only food purchases for emeralds are implemented. Barter,
production-specific merchants, business income, reputation attribution and broader
autonomous response learning remain later work. Whole stacks are required: split
a large stack if it exceeds the village's remaining need.

Domain, temporary SQLite and player-adapter tests cover price acceptance/counters,
patience, expiry, rollback-safe history/funds, restart, stale/changed terms,
duplicate acceptance, disabled-setting recovery and strict admin tuning. The
disposable native Paper probe also checks a counter surviving restart and the
confirmed saved inventory exchange. Human client and inventory-plugin compatibility
still require testing before a balanced survival release.
