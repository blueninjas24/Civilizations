# Current prototype: a short player checklist

**For the current consolidated instructions, use [PHASE3_PLAYTEST.md](PHASE3_PLAYTEST.md).**
The dated notes below retain earlier checkpoints; their unfinished-feature wording
may have been superseded. They are not the current release checklist.

## October18 - automatic forestry opt-in and checks

The new build can recognize canopy-backed trees and found logging businesses for
actual funded timber customers. Existing installed economy.yml files are preserved:
while stopped, review/copy the shipped forestry-resources, forestry-observer and
forestry-founding sections to opt in. For first-owner training, also add the shipped
resident-study.skills.forestry course and logging to automatic-career-study. Keep
your chosen economy settings; do not replace the whole file blindly. Restart normally.

Use existing /civ debug status and /civ debug observation to read why logging is
waiting. A forest alone is insufficient: the village needs real funded timber
demand, protected startup cash and an available qualified owner (or real supplies
for gradual training). Existing logging capacity suppresses redundant openings.
A desert without suitable trees should not magically get forestry. Confirm actual
stock, purchases and jobs through the existing business diagnostics. Trees remain
physical; felling/construction/expansion is Phase4. Unknown tracking history requires
review, not deleting the database. The automated Paper proof passed; a human client
check and long-term balance/compatibility are still useful. Nothing was installed
on the human server by this development batch.


Latest forestry checkpoint: /civ debug observation can show bounded loaded-chunk
forest surveys when forestry-observer is enabled. These are canopy/shape sightings,
not proof of ownership or automatic logging. Real saved forestry production and
paid timber customers are verified in isolated scenarios, but automatic village
adoption still awaits placement/growth/damage protection. See FORESTRY.md. Older
economy.yml files are preserved and need explicit opt-in for the survey settings.

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
Existing test folders may contain an older plugin JAR; replace it with the newly
verified target/civilizations-0.0.1-SNAPSHOT.jar while stopped. Existing settings
are preserved. Missing industry-supply stays off; missing horizontal-radius-chunks
keeps home-chunk surveys. See WORKSHOP_SUPPLIES.md and LIVE_RESOURCE_SURVEYS.md to
opt in while stopped. New default settings also enable the implemented automatic
coal/iron, toolkit-workshop and warehouse startup, subject to real opportunities,
training, qualified staff and protected funds. Older files need explicit opt-in.

Use a disposable Paper test server and world. The current prototype has persistent
villagers/families, food shortages, village food deliveries/trading and automatic
farms. Configured Phase 3 businesses also include automatic coal/iron mines,
toolkit workshops, warehouses, bakeries, craftsman storage upgrades, shoreline
fisheries and natural cow/chicken herds. Individual startup still depends on real
observations, skills, supplies, customers and money; not every village gets every
business. Phase 3 remains unfinished. Physical excavation and new buildings
belong to Phase 4.

The latest verified build also supports automatic finite stone quarries and
sandpits; see AUTOMATIC_QUARRIES.md for opt-in settings in older files. A quarry
needs a current exposed source, qualified owner, protected real startup funds and
an actual customer. Existing mine stone can already meet demand. Sandpits need a
configured sand customer; no default glass industry has been invented. Nothing
excavates blocks in Phase3, and these businesses are not guaranteed in every town.

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

## New livestock and bakery checks (October17)

Keep your edited settings and saves. Stop the server, back up its plugin folder,
and compare your economy.yml with src/main/resources/economy.yml. Copy only the
new settings you want; do not replace a tuned file wholesale. In particular,
missing livestock, livestock-adoption, livestock-runtime, livestock-slaughter
and livestock-market sections opt out of their respective new features. Learning
mappings and resale values also need the additions in LIVESTOCK_LEARNING.md.
The bakery recipe, business ingredient prices and storage weights are documented
in BAKERY_SUPPLY_CHAIN.md. Its business ingredients must not be added to the
public village starting-goods/price catalogue merely to enable bakeries.

- Stay near a recognized, funded village with available adults and eligible
  natural cows/chickens. Adoption requires repeated clear observations. Named,
  leashed, player-bred, recorded player-owned and uncertain penned animals are
  protected. [Civ Protect] is protection, not donation; bulk donation is unfinished.
- Look at /civ debug observation and /civ debug businesses <ID> before expecting
  a visible change. Food, qualifications, space or money can prevent startup.
- Confirm saved herds consume real delivered feed and earn milk/eggs. Use
  /civ market near an eligible stocked livestock business for actual products;
  bring your own empty bucket for milk. See LIVESTOCK_MARKET.md.
- Configured bakeries can buy delivered grain/milk/eggs and make saved meals;
  these are abstract food units, not cakes appearing in chests. Bread is fallback.
- With enabled slaughter, only eligible loaded surplus adults can be deliberately
  killed. Breeding reserves remain. No duplicate ground loot accompanies business
  food. Ordinary player kills retain vanilla drops without duplicate business food.
  An unloaded or missing animal is never sufficient proof of death.
- Stop and restart normally. Verify ownership, food, money and histories persist;
  downtime/absent worlds do not progress. Report pending permits or failure messages
  instead of deleting database rows or spawning replacement bodies.

These are playtest observations, not instructions to modify a production world.
The development agent has not installed this batch into your personal server.

Useful feedback: what you expected, what happened, the command/offer wording, and
whether it repeated after a normal restart. In particular check inventory counts,
payments, names, newborns, shortages and recognition of player-built areas. Human
client/inventory-plugin validation is still pending, despite automated Paper checks.

Phase 4 adds physical construction and industry layouts. Current industry stock is
abstract saved data; do not look for a newly dug shaft, built warehouse or worker
animation in this Phase 3 build. No completion date or balanced-release claim is made.
