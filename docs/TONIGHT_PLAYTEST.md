# Current prototype: a short player checklist

October 9 human client checks passed recognition, bread donation, emerald food sale
with exact inventory changes, and resident persistence after normal restart. Broader
barter/bargaining/compatibility coverage remains pending. The reported ~15-second
sale wait exposed five-second stage polling; the follow-up build polls each transfer
adapter every five ticks, retaining proof/ledger gates. Actual new-build human latency
still needs a recheck. Default food accounting is 60 seconds/period; a temporary
10-second test interval was restored after the playtest. Population speed is separate.

The prepared local test folder is under Windows %LOCALAPPDATA%\Temp, with a name
starting civilizations-phase3-player-test-. Double-click START-PLAYTEST.cmd there,
wait for the server to say Done, and join from Java Minecraft 26.2 using
127.0.0.1:25577. It is a server plugin; your client does not need a mod installed.
In that server console, type `op YOUR_MINECRAFT_NAME` to use debug commands.
Type `stop` in the console when finished. The folder is outside OneDrive and uses
a separate test world/save. Keep it if you want to resume the same village later.

The prepared settings predate optional workshop supply and nearby-chunk radius.
The artifact is updated after verified batches, but those existing settings are
preserved. Missing industry-supply stays off; missing horizontal-radius-chunks
keeps home-chunk surveys. See WORKSHOP_SUPPLIES.md and LIVE_RESOURCE_SURVEYS.md to
opt in while stopped. This does not create nonfood businesses automatically.

Use a disposable Paper test server and world. The current prototype has persistent
villagers/families, food shortages, village food deliveries/trading and automatic
farms. Phase 3 industry work is still in progress. It does not yet offer automatic
mines/workshops, physical excavation or new buildings.

1. Stop the test server. Install only target/civilizations-0.0.1-SNAPSHOT.jar in its
   plugins folder, then start it normally. Never install src/test/paper probe JARs
   for player testing. Use the verified Paper/Java combination in DEVELOPMENT.md.
   Preserve an older world/config/database with a consistent backup before upgrading.
2. Visit a village, or build an area with at least two bed heads and two villagers.
   Stay nearby while recognition scans the loaded area. Existing and player-built
   villages participate automatically; there is no player enrollment command.
3. As an operator, use /civ debug status and /civ debug observation. Record the saved
   village ID, then use /civ debug economy <ID> and /civ debug businesses <ID>.
   Angle-bracket text means replace it with the ID displayed by diagnostics.
4. Keep actual planted fields near the village. A farm needs enough observed crops,
   an available adult worker and startup money. If it does not start, the economy
   diagnostic should explain the missing requirement. Older economy.yml files need
   an explicit farm-businesses opt-in; see AUTOMATIC_FARMS.md.
5. Hold ordinary wheat, bread, carrots or another supported food. Sneak-right-click
   a managed villager and inspect the aid/trade menu. Read the offer before accepting;
   payment needs available village money or goods. /civ aid previews held food and the
   nearby village without transferring items. See PLAYER_FOOD_AID.md and PLAYER_BARTER.md.
6. Watch food reserves/workers change in diagnostics. Stop the server normally and
   restart it; identities, goods and family history should remain. Downtime adds no
   simulation years. Avoid plugin hot reload or editing the SQLite file directly.

Useful feedback: what you expected, what happened, the command/offer wording, and
whether it repeated after a normal restart. In particular check inventory counts,
payments, names, newborns, shortages and recognition of player-built areas. Human
client/inventory-plugin validation is still pending, despite automated Paper checks.

Phase 4 adds physical construction and industry layouts. Current industry stock is
abstract saved data; do not look for a newly dug shaft, built warehouse or worker
animation in this Phase 3 build. No completion date or balanced-release claim is made.
