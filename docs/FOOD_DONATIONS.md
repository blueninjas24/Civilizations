# Village food donations

Phase 2 prototype: hold ordinary supported food, **sneak-right-click a Civilizations
villager**, then click **Donate held food** in Village supplies. This explicitly gives
the whole held stack to that village. Closing the menu or using `/civ aid` gives nothing.
Normal right-click villager trading remains available. Donations do not pay emeralds,
create treasury income, buy reputation or implement general barter.

The menu names the village, shows its food situation and states the exact stack/food
unit conversion. Changed held items, another world, unavailable village evidence,
permissions, already-pending aid or overloaded queues prevent a new debit. Custom
item metadata remains excluded. Menu clicks/drags are protected, duplicate confirms
are ignored and inventory changes run on the next Paper tick, not inside a click event.

## Configuration and permissions

Conversion/range use the editable `aid-preview` section documented in PLAYER_FOOD_AID.md.
New default economy.yml additionally has:

```yaml
donations:
  enabled: true
```

Existing owner files **without** this section retain disabled donations. Add it and
restart to opt in; files are never replaced. The menu indicates availability and
`civilizations.aid.donate` defaults true. `civilizations.aid.preview` controls menu
access; administrative debugging still requires its own permission. Disabling new
donations does not discard pending recovery or change its originally saved conversion.

## Saved handoff and recovery

Minecraft inventory and SQLite cannot share a transaction. The plugin therefore:

1. Records an immutable operation/player/village/quantity receipt in player persistent
   data and removes the confirmed stack on the main server thread.
2. Asks Paper to save player data, then reads the saved receipt on a dedicated worker.
   A void saveData return, missing file, wrong receipt or failed save is not proof.
3. Only after proof submits the same operation to the SQL-owner food ledger. Accepted
   receipts enter a future food period once; duplicate retries do not import again.
4. Clears the player marker after ledger confirmation. If this later save fails, the
   stale marker safely retries the already-accepted operation on return/restart.

Pending receipts retry while online. Leaving, shutdown or a deferred database write
retains recovery information without another item removal. Until the ledger accepts
it, the food is held for delivery; it is not yet village supply. Failed save verification
may delay delivery. Malformed/foreign markers require admin review; never erase them
or manually refund goods while receipt application is uncertain.

The receipt uses `civilizations:pending_food_aid` in root BukkitValues. The read-only
adapter is verified against **Paper 26.2-129 / Minecraft 26.2**, whose primary level
stores saves under `players/data`; it does not assume the outdated playerdata path.
It checks the complete gzip/NBT stream, exact operation and canonical encoded inputs,
rejects symlink files, and bounds file size/inflation/nesting/tag count. It never writes
player files itself: mutations and saves use Paper APIs. This is a compatibility
boundary that must be revalidated when upgrading the server format.

Do not independently roll back SQLite and player saves: restore a consistent complete
server backup. Verified process restart recovery does not promise protection against
hardware power loss, external save editors, inventory-replacing plugins or inconsistent
backups. Test plugin compatibility before established survival saves. No real players'
files or production database were used in development checks.

## Scope

A donation supplies real player goods and can clear the existing village food shortage,
which can change its saved priority. Farm output is unchanged. No merchant payout,
barter, transportation, food-driven mortality, reputation or autonomous purchase is
implemented by this feature. Human-client UX, broad compatibility and scale profiling
remain separate checks. SQLite schema stays 18: existing people, histories, reserves
and receipt ledgers remain intact.
