# Player food barter prototype

Hold a plain supported food stack near a recognized village. Use `/civ barter timber`
or `/civ barter stone` to request oak logs or cobblestone in return. The offer shows
the whole food stack and actual number of outgoing goods. Nothing moves during the
quote. Use `/civ barter accept` to explicitly confirm the latest offer. Append a
village UUID when nearby villages overlap. Permission: `civilizations.trade`.

The village must currently seek food, with reliable evidence and completed catch-up.
Prices use local food scarcity and the existing material Trade Value. Payment floors
to whole goods and actual available stock above a protected floor. Phase 2 supports
only idle timber/stone; it does not conjure a logger/mine or infer production from
villager professions. The current finite supplies were initialized once by existing
economy rules. A village can run out. Barter needs no emerald budget, creates no
money and awards no reputation. Existing emerald purchases and donations remain.

An offer is an indicative quote. Before acceptance it expires using
`bargaining.offer-seconds` (default 45 seconds, even if emerald bargaining is disabled).
Only one current quote/player is kept; requesting another supersedes it. Restart
forgets unaccepted quotes, without goods lost. Acceptance removes the quote and
checks the player's exact plain food type/count and village proximity. The database
owner then revalidates current value, conversion, need, stock and saved cooldown.
Changing a stack, walking away, competing trades, expiry or retuning can refuse it.
Split a large food stack if it exceeds the village's remaining need. Negotiation
over barter quantities is not yet exposed; `/civ bargain` negotiates emerald prices.

## Editable settings and old servers

The commented `barter` section in new economy.yml files is now enabled. Older files
are never rewritten; add the complete optional section to opt in. See
BARTER_ACCOUNTING.md for the prototype mappings, floor, conversion, food target
and item cap. Stop, edit `plugins/Civilizations/economy.yml`, restart. Unsupported
items/commodity mappings and partial or invalid sections fail startup validation.
Disable new barter with `barter.enabled: false`; pending accepted trades still recover.

## Safe exchange and recovery

1. Explicit confirmation writes exact PREPARED terms into Minecraft player data.
2. Independent bounded full-file NBT verification must prove that saved receipt
   before the owner can reserve outgoing supplies. Reservation and its stock movement
   share a transaction. A player can have only one pending donation/purchase/barter.
3. If the held food changed before exchange, save CANCELLED proof, then restore
   reserved goods once. Replacement items are never taken.
4. Otherwise the complete food stack becomes the promised material stack in the
   same slot. There is no dropped overflow. EXCHANGED proof, saved with inventory,
   must be independently verified before recording one future food receipt.
5. Clear the completed marker. Retries/reconnect/restart do not pay or import twice.

EXCHANGED, REVIEW or malformed receipts lock item movements/interactions while
verification is pending. An ambiguous inventory-provider failure retains supplies
for administrator review, rather than automatically refunding or paying twice.
New donations, purchases and negotiations refuse other pending transfer markers.
No native-death immunity is added. Minecraft player/world/chest files and SQLite
are separate stores: plugin interference and cross-save crashes still require
compatibility tests and consistent backups. Never clear markers/refund manually
without reviewing the actual saved inventory and matching receipt.

Schema 24 already provides barter accounting. This adapter requires no new migration
or data reset. Earlier period reports remain unchanged; current available stock
reflects reserve/refund movements separately. See FOOD_PURCHASES.md for shared
save-proof limitations and BARTER_ACCOUNTING.md for exact stock semantics.

Automated tests use temporary databases/player files and cover confirmation,
double acceptance, changed goods/need, permissions, competing markers, save failure,
ambiguous providers, expiry, adapter restart, disabled recovery and malformed records.
The native disposable Paper probe validates actual oak-log inventory save/reload,
finite stock debit, unchanged treasury, future food once and restart. Human client
and inventory-plugin compatibility remain pending before a balanced survival release.
