# Barter accounting foundation

The foundation commit `8e20726` introduced internal food-for-material accounting
without a player interface. The subsequent player adapter now exposes `/civ barter`
with explicit confirmation and independent Minecraft save proof; see PLAYER_BARTER.md.
This document describes its underlying stock/receipt accounting. New files now
enable barter; older owner files are preserved and require explicit opt-in.

## Rules

The village must be seeking food with complete field evidence, known unmet demand
and no queued catch-up. Quotes use local food scarcity value and the outgoing
material's Trade Value. Payment floors to whole items, capped by actual disposable
stock. The Phase 2 prototype supports timber as oak logs and stone as cobblestone.
It exports only materials with no demand or shortage in the latest recorded period.
These are existing saved opening supplies, not forestry/mining production or goods
inferred from villager professions. Industry-specific output belongs to Phase 3.

A quote changes no goods, stock or money. Reservation atomically records exact
terms and removes outgoing units from available stock. Other trades see the reduced
balance. Incoming reserved barter food counts against need alongside pending
donations and emerald purchases. One player cannot hold a reserved barter and a
reserved emerald purchase simultaneously. Saved bargaining cooldowns also constrain
barter; switching payment type must not bypass them. Barter does not use treasury
funds or award reputation. A cash-poor village may exchange actual supplies.

Trusted internal completion must only follow verified physical EXCHANGED proof.
It records one future food receipt, finalizes the reservation and removes no goods
twice. Cancellation must only follow durable proof that no exchange occurred: it
restores the original units once, even after elapsed periods. Ambiguous physical
exchange will need to hold the reservation for administrator review. Recovery uses
immutable accepted terms despite new-offer disabling or conversion retuning.
The live adapter enforces these proof requirements; see PLAYER_BARTER.md for its
inventory recovery stages and compatibility limits.

## Editable settings

```yaml
barter:
  enabled: true
  food-reserve-periods: 3
  maximum-items: 64
  goods:
    timber:
      item: OAK_LOG
      units-per-item: 1
      keep-units: 2
    stone:
      item: COBBLESTONE
      units-per-item: 1
      keep-units: 2
```

Edit `plugins/Civilizations/economy.yml` while stopped and restart. Existing files
are preserved; an absent optional section is disabled. Remove a commodity entry
to disallow it. Material mappings are deliberately restricted for this prototype;
arbitrary valuable items or cross-commodity conversions fail validation. Food target
allows 1..100 demand periods, item cap 1..64, units/item 1..1000000 and keep units
0..1000000000. Material prices reuse the existing `prices` settings. No automatic
stock refill or opening-supply reset is introduced.

## Persistence and validation

Transactional schema 24 adds `food_barters` for immutable goods/receipt status and
`barter_stock_moves` for ordered reserve/refund movements with before/after units
and the completed economic period. These explicit movements explain changes between
periods; they do not rewrite earlier period reports or pretend to be period exports.
Current available stock can differ from the last period's closing stock. Live
offers use current availability; historical reports retain their earlier facts.
Completion transfers held goods; cancellation is a recorded return.

Tests use temporary SQLite exclusively. They cover valuation, protected floors,
competing offers, shared food need, mutual payment reservations, changed terms,
disabled-setting recovery, rollback of all legs, restart, migration failure and a
seeded 240-period famine/finite-stock conservation scenario. Back up consistent
SQLite/player/world data before upgrade. No world or production reset is required.
Live physical exchange is now implemented and verified on disposable Paper.
Bargaining over barter quantities, human client testing and inventory-plugin
compatibility remain unfinished.
