# Player food-aid preview

Phase 2 now provides a read-only `/civ aid` command. Hold an ordinary food stack
near a village home. The command names the saved village, converts the held stack
into configured food units and shows its latest saved food situation.

It **does not donate**, remove items, submit a delivery, pay emeralds/Trade Value or
award reputation. Confirmed transfers now use the separate village menu in FOOD_DONATIONS.md. It is safe to repeat.
The separate FOOD_RELIEF.md ledger prepares eventual receipt accounting.

## Recipient and evidence

The nearest saved home with a living resident and a ready food economy is eligible.
The prototype uses 32 blocks of three-dimensional distance and the permanent world
UUID. Multiple equidistant towns require an explicit saved settlement UUID:
`/civ aid <settlement UUID>`. A requested ID still has to be nearby and in this world;
it cannot bypass range or missing evidence. An administrator can inspect IDs through
existing debug commands. Selecting a recipient does not enroll, merge or rename towns.

Missing or incomplete farm evidence excludes a village; it never becomes zero food.
A newly initialized economy reports that its first food period is still pending.
Older completed reports are labelled with their period, not promised as instant stock.
All recipient/name/food data comes from immutable SQL-owner caches. The Paper thread
reads only player location and held items; it does not scan blocks/entities, load chunks,
open JDBC or wait for a future. Large-population command throughput still needs profiling.

## Easy server-owner tuning

An optional `aid-preview` section in `plugins/Civilizations/economy.yml` controls:

```yaml
aid-preview:
  enabled: true
  radius-blocks: 32
  units-per-item:
    WHEAT: 1
    BREAD: 3
    CARROT: 1
    POTATO: 1
    BAKED_POTATO: 2
    BEETROOT: 1
```

These are editable prototype conversions, not nutrition measurements or merchant
prices. Disable a food by omitting its key. Radius is 1..128, units are whole numbers
1..1000000, stacks are 1..64. Only the six listed foods currently have adapters.
Custom item metadata is rejected to avoid valuing plugin items as ordinary food.
Wrong types, unknown keys and incomplete sections fail startup clearly.

Existing format-1 files without this entire section get the documented defaults;
owner files are not replaced. Restart applies preview edits without changing economic
history, reserves or pending deliveries. Permission `civilizations.aid.preview`
defaults to true; `civilizations.debug` remains operator-only. Removing the shared
command's debug gate does not remove the debug handler's permission checks.

## Actual transfers now use a separate menu

The original preview was deliberately read-only while inventory/SQLite recovery
was unresolved. Confirmed donations now verify the receipt in the saved Minecraft
26.2 player file before crediting supplies. `saveData()` returning void alone still
is not proof. See FOOD_DONATIONS.md for the menu, server opt-in and recovery limits.
`/civ aid` remains read-only and does not become a mutation command.

## Verification

Headless tests cover exact configurable conversion, ranges/world IDs, tie handling,
input rejection, repeatable selection, old config compatibility, permissions, startup,
partial evidence, custom items and unchanged inventory/economy caches. Test player
facades reject every unexpected player/inventory/world method.
The disposable Paper probe uses a synthetic read-only player facade with native Paper
ItemStacks/world locations and actual saved village food. It checks conversion,
custom-item rejection and no stock/item writes, then runs existing farm/crisis/restart/
relief/unloaded checks. This is not a human client playtest or successful donation.
Schema remains 18; no new migration or historical backfill.
