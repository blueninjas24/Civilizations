# Finite mining, business storage and local warehouse accounting

The subsequent WORKSHOP_PRODUCTION.md batch adds transactional input/output recipes
and a tested paid mine -> warehouse -> blacksmith chain. Automatic nonfood industry
discovery/scheduling remains unwired; the original mining verification below is
historical and is not a claim that mines now appear in ordinary gameplay.

Approved October 9, 2026. This is a backend foundation, not automatic in-game mines,
warehouses, shops, villagers shopping, physical transport or a completed Phase 3
production milestone. Trusted owner callers and temporary tests exercise the chain;
reliable discovery, site/founding adapters, scheduler and player displays remain next.

## Admin settings and output

The optional complete `industry` section in economy.yml has commented prototypes:
150 extraction bundles per observed ore block at first discovery, eight bundles per
actual saved living local employed worker/period, site cap 32 bundles/period, storage
1024 abstract space, one-period local delivery and 128 hauled units/town/period.
Missing old-file section returns the complete prototype inputs without rewriting
the file or enabling any automatic production. Invalid/partial/unknown settings fail
before startup. There is no infinite/renewable mineral option.

The initial coal prototype bundle is one coal + three ordinary stone. Three workers
can extract 24 coal + 72 stone, limited by saved deposit, actual work, site capacity,
requested extraction and free storage. Rock does not get a second independent labor
budget. Admins may add andesite/cobbled_deepslate or change yields, providing positive
storage-space-per-unit costs for every output. These keys are abstract commodities;
this does not implement Minecraft item conversion/player sales for them.

Eight observed ore blocks under the prototype represent 1200 finite bundles, not
eight items or an inexhaustible seam. Discovery terms/yields save once per stable
site/deposit identity. Revisit, restart, alternate UUID at the same site and changed
discovery terms cannot refill reserves. Future observation must establish plausible
natural-resource evidence, spatial matching/overlap and a suitable workplace; these
APIs do not certify a deposit merely because a caller supplied text/quantities.
One completed extraction per business/economic period prevents repeated capacity
use. Stale periods fail. Matching operation retry retains the saved result, including
zero output. Configuration changes never reinterpret old extraction/deposit terms.
New deposit yields may differ; existing ones retain their finite geological bundle.

## Storage, ownership, capital and transport

Each business store starts empty with explicit finite capacity and positive per-good
space weights. Used space = sum(quantity * saved weight). Pending purchases reserve
incoming space. Mining pauses when the complete geological bundle cannot fit; output
does not discard inconvenient byproducts. Store opening terms are immutable initially;
capacity/catalog expansion needs a tested explicit future transition rather than
resetting a full store by editing configuration. The quantities are SQL data, not
thousands of ticking chests/items.

Working capital is an explicit transfer from actual village treasury into a business
account, separate from nonrecoverable startup spending and household welfare. It
does not create money, seed repeated grants, personal resident wallets or salaries.
An exact opening retry never funds twice; changed opening terms fail.

Purchase checks actual seller stock, buyer capital, compatible storage, reserved
incoming capacity and the same settlement. It debits the buyer and credits the seller
once, removes seller goods and records buyer-owned cargo. Capacity limits all local
dispatches in that town/period; each dispatch's complete terms are immutable.
Delivery waits until the saved due period and atomically replaces reserved space with
actual stock. Pending cargo cannot be resold or consumed. Retry/restart cannot pay or
deliver twice. Cross-settlement shipment is rejected; regional routes are Phase 5.
Local haul limits/delay are caller-supplied abstract prerequisites; actual transport
businesses, staffing, distance/route costs, fees and visible carriers remain unwired.
No fake success, merchant income or road is inferred from stock passing through.

`consume` is a retry-safe primitive for explicit household need, industry input or
maintenance use with a real caller-established quantity/reason. It is not a live NPC
shopper, simulated household payment, finished recipe or invented demand just to
empty storage. Future household/business demand must establish cause, affordability
when buying and aggregate accounting before invoking it. Business goods are separate
from existing village reserves; future transfers must conserve both and avoid
counting opening supplies or field-backed food twice.

## Persistence, history and validation

Schema 28 adds mineral deposits/yields, business stores/stocks, immutable industry
operation receipts/production markers and local shipments. All relevant changes
commit together, roll back on failure and use the existing JDBC owner. No seeded
production history, business reset or production database mutation. Older binaries
reject schema 28; restore a consistent pre-upgrade backup for downgrade.

Tests cover shared bundle limits, 10000 seeded calculations, depletion/restart,
duplicates/stale work, full/pending storage, real warehouse buying/resale profit and
capital conservation, causal consumption, write-failure rollback/retry at extraction,
dispatch and arrival, store-funding failure, impossible actions, schema-27 rollback,
snapshot cargo recovery and 240 periods of mine→warehouse→shop→input use. Free
transfers in the long fixture test goods flow separately; the paid fixture proves
actual buyer/seller balances. No business prosperity/industry balance is claimed.

## Shop variety and world switching

Verification October 9, 2026: Windows `.\mvnw.cmd clean verify -B -ntp` passed
522 cases (520 unit/two packaged integration), zero failures/errors/skips. Disposable
pinned Paper schema-27-to-28 startup, owner/observation diagnostics and clean stop
passed after a consistent dev snapshot. Read-only integrity/FKs and checked old
identity/history retention passed; no automatic industry records created and owner
economy.yml hash unchanged. This is schema/startup validation, not native business
production or a client playtest. Larger workloads, business prosperity/capital
balance, runtime transport operators and cross-save recovery remain validation work.

The intended shop range follows skills, real inputs, capacity and demand rather than
a permanent villager trade list or building area alone. Wider product chains and
actual NPC needs/shopping remain implementation work; Jimmy/Stacy examples are
direction, not guaranteed catalog sizes or already implemented shops.

Simulation data is saved in plugins/Civilizations/civilizations.db, linked to permanent
world UUIDs. A new world has a distinct identity; it does not automatically delete
or adopt previous towns. Returning to a preserved old world requires its original
UUID/data plus the corresponding plugin database. Reinstalling an empty plugin data
folder loses that saved simulation. Preserve consistent world/plugin/player-data
backups together, especially when exchanges are pending. The subsequent approved
pause batch stops new population/economy progress during server downtime and world
absence; see ONLINE_WORLD_PAUSE.md. The mining batch itself performed no world
switch/reset/restore and did not implement automatic industry scheduling.
