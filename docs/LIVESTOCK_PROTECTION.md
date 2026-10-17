# Livestock protection and work foundation

## October17 — automatic saved-herd work and paid feed verified

Existing saved livestock businesses now use the shared industry clock: buy real
feed with business money, wait for cargo, then consume it for milk/eggs and saved
births under actual qualified work/storage limits. Existing newborn/age adapters
can reconcile those births. World/server absence and capture gaps earn no work.
Delivery, purchases, production and checkpoints roll back together on failure;
old manual herd periods are not replayed. New editable livestock-runtime limits
opt installed files out when absent. Schema88 unchanged; no migration/reset.

Five focused scenario/config cases added. Final scoped Wrapper verify passed126
unit plus2 packaged integration tests (128 total), zero failures/errors/skips;
43.518s, finished2026-10-17T12:26:27-04:00. No new full-suite claim. Initial checks
caught/fixed a fixture cash-column typo and new config root-key registration.
Guarded Paper26.2 build129 passed14:44:59 EDT: actual packaged owner-thread work,
paid feed/cargo/milk/births/conservation, restart, absent-world and capture-gap
pause. Private synthetic herd; no block/animal mutations. All149 main tables
retained/145 exact; ordinary clock/runtime/capture/ore metadata only, no aging.
Integrity/FKs clean; fixture stopped/settings restored/helper archived/backups
kept. Human server untouched. Full CI follows feature backup.

Changed: herd persistence composition, shared industry runtime, paid-feed helper,
editable policy/configuration, plugin/owner wiring, diagnostics, focused tests and
excluded LivestockRuntimeProbe. See LIVESTOCK_RUNTIME.md for settings/contracts.
P3's livestock milestone remains partial: automatic eligible animal adoption and
funded founding, physical slaughter, and bakery milk/egg consumers still remain.
This closes scheduled operation for already saved herds; no unimplemented startup
is counted complete. P3 stays~68% partial/30of39 resource entries/1of4 whole groups.
Next: bounded eligible-animal observation and atomic farm opening. No new answer
needed for this scheduled-work batch; no duplicate slaughter drops is approved.

## October17 — usable pen protection controls verified

Admins can now right-click a [Civ Protect] sign inside a supported flat enclosure,
review its area/animal count and private boundary outline, then explicitly confirm.
The plugin recaptures evidence and saves real animal owners. /civ pen here finds
saved IDs; owner/admin release also requires confirmation and retains each animal's
ownership/history. Sign placement alone claims nothing; existing owners cannot
be overwritten. Limits are editable in config.yml. No donation or automatic herd
founding/feed/output enabled yet. No migration; schema88 retained.

Following the user's faster-development instruction, final scoped Wrapper verify
passed67 affected unit plus2 packaged integration tests (69 total), zero failures/
errors/skips;26.961s, finished2026-10-17T11:44:00-04:00. No new full-suite run is
claimed; the prior checkpoint passed1,788. Guarded Paper26.2 build129 passed
11:44:59 EDT with a real sign/pen/cow and synthetic player: explicit confirmation,
owner/controller restart, lookup and release retain ownership/history. Temporary
blocks restored/test cow removed. All149 main tables retained/145 exact, ordinary
clock/runtime/capture/ore metadata only, no aging; integrity/FKs clean. Fixture
stopped/settings restored/helper archived/backups kept. Human server untouched.

New files: PaperPenControls, PaperPenCapture, PenProtectionSettings, eight focused
control/config tests, excluded PenControlsProbe and PEN_CONTROLS.md. Plugin/router,
config/permissions and owner handoffs updated. No-duplicate slaughter drops now
explicitly approved and recorded; no question remains for that rule. Full-suite
CI follows feature backup. Local checks stay focused between larger checkpoints.

P3 remains about68% partial (30/39 resource entries,1/4 complete groups). This
finishes an interaction prerequisite within the incomplete livestock milestone;
no percentage credit for unimplemented adoption/feed/slaughter. Next: automatic
livestock loop and bakery ingredients. See PEN_CONTROLS.md for actual commands,
authority and limitations; broader templates/construction remain Phase4.

## October17 — pen area release and retained animal ownership verified

Pen protection now separates an active area from its permanent history. An
explicit owner/admin release retains original animal ownership and provenance;
old protection retries cannot reclaim released land. Adjacent pens may share a
fence, while overlapping interiors are refused. Schema88 preserves original
pen records and backfills current claims without inventing ownership.

Added12 lifecycle/owner-queue cases; focused50 passed. Windows .\mvnw.cmd clean
verify -B -ntp passed1,788 tests (1,786 unit/two packaged integration), zero
failures/errors/skips;11:39, finished2026-10-17T11:01:25-04:00. Source/test Java
remained fixed during the final full run. The excluded native helper was then
compiled against the verified packaged JAR.

Guarded Paper26.2 build129 passed11:01:48 EDT: private populated schema87 upgrade,
shared fence, authority checks, release/retry/reclaim/restart, preserved original
pen/animal history and clean SQLite integrity/FKs. All147 main predecessor tables
retained;143 exact, only ordinary clock/runtime/capture/ore metadata changed,
no aging. The two new main tables are empty. Fixture stopped/settings restored/
helper archived/backups kept. No world/entity edits or human-server changes.
This certifies packaged persistence, not a live sign interaction or human client.

No player-facing pen controls or automatic adoption enabled. P3 stays about68%
partial (30/39 resource milestones,1/4 whole groups). Next: bounded authorized
pen interactions before automatic livestock founding/feed/output. The slaughter-
product question remains pending; no answer assumed. See PEN_PROTECTION_LIFECYCLE.md.
Feature backup/hosted CI follows the verified checkpoint; main stays unchanged.

## October17 — existing farm animals now use the saved growth clock

Animals initially assigned to a farm now grow at their saved dates, just like
newborns. Late observations cannot turn an adult back into a baby. Names and
ordinary movement/protection settings stay intact; unknown animals stay unknown.

All1,776 full-build tests and the isolated real initial-animal/growth/restart/
serialization check passed. No save migration or human-server change. The prior
newborn build also passed GitHub on Windows/Linux. P3 stays about68% partial.
Next: bulk pen protection before automatic adoption. The slaughter-product
question is pending, and independent work continues.

## October17 — saved animal births now appear safely in Minecraft

Recorded newborns can now appear on safe loaded ground near players. They keep
permanent identities, grow at their saved dates and survive restarts without
copies. An animal missing from view is never automatically replaced. One real
death removes only that animal; the farm's goods and money remain accounted for.

All1,763 full-build tests and the isolated real-cow birth/growth/restart/missing/
death check passed. Earlier radius work also passed GitHub on Windows/Linux.
Your server is untouched. P3 remains about68% partial: initial animal aging,
automatic livestock businesses/feed/output, wider products and specialization
still remain. Next: initial herd aging. The slaughter-product question is pending;
other work continues while awaiting that answer.

## October17 — animals can pause work without disappearing

A cow or chicken confirmed far from its farm can stop working until it returns,
without losing its ownership or place in the herd. Growing animals still age;
other animals keep working. Unloaded animals keep their last known status.
Admins can edit the working radius in economy.yml; the prototype is32 blocks.

All1,739 full-build tests and the isolated real-cow away/return/restart check
passed. Existing test-save records were preserved; your server is untouched.
Earlier newborn checkpoints also passed GitHub's Windows/Linux builds.
P3 remains about68% partial: automatic adoption, safe newborn placement, scheduled
feed/output and broader bakery ingredients remain. Next: safe newborn placement.
No answer needed.

## October 16 — permanent logical newborn records verified

New actual simulated births now save stable identities/maturity within their
production transaction. Schema 85 adds an empty registry without changing old
rows or guessing physical animals for old anonymous cohorts. All 1,694 Wrapper
tests and the native market/record restart regression passed; details in status.
No physical offspring are spawned yet. Claims, individual offspring losses,
reconciliation and live adoption/feed/output remain. See LIVESTOCK_BIRTH_IDENTITIES.md.

## October16 — recoverable player retail and selective pending-item retention verified

/civ market, SQL-owner acceptance/site recheck and durable once-only purchase
handoff are now connected. Newly purchased goods occupy separate marked stacks;
only their exact quantity receives temporary death retention. Normal other items,
XP and server keepInventory rules remain. Movement, interactions, consumption and
recipe-book autofill cannot spend unconfirmed goods. Exact quantity is checked
before seller credit; uncertain state requires review, never a second delivery.

All1,681 Wrapper tests and guarded native Paper save/menu/retention/restart checks
passed; details in PROJECT_STATUS. Native death/respawn is an explicit synthetic
event model, not a human client or actual crash. Human server untouched. Schema84
unchanged. Older entries below describe their historical checkpoints. Automatic
adoption, permanent physical newborns and live feed/output remain unfinished.
See LIVESTOCK_MARKET.md for settings, recovery and current limits.

## October16 — nearby retail quotes and guarded storage plans verified

LivestockMarketPolicy exposes editable introductory milk/egg prices, egg order
count, shared emerald cash conversion, local radius, candidate cap and quote
lifetime. LivestockMarket queries actual saved same-world nearby staffed herds
on the SQL owner, then revalidates current terms/location/stock for new reservations.
Accepted reservations retain their exact terms for recovery; the future Paper
controller must still recheck permissions/location before inventory mutation.
Owner APIs do not read world/entity/inventory objects. Old installed settings
without livestock-market remain disabled; no stock or money is created by quotes.

LivestockInventory plans only main storage: real plain emeralds, one real plain
bucket per milk bucket, and destination capacity. Egg orders split using actual
server item limits. Existing custom items are opaque and retained. The Paper
LivestockSaleInventory snapshot validates exact apply/restore and cannot overwrite
new property after a stale preflight. No armor/offhand/chest/drop fallback.

Full Wrapper1652 tests and guarded Paper inventory probe passed; details in status.
No player menu/controller is wired, and no native player save/death flow is claimed.
The approved next safeguard retains only exact unfinished purchase goods across
death; ordinary inventory and confirmed purchases follow normal server death rules.
Schema remains84. Live adoption, physical newborn reconciliation and feed/output
remain unfinished; neither editable prices nor an inventory plan enables a herd.

## October16 — receipt proof key support

SavedAidProof now explicitly allows PendingLivestockSale.KEY. Its parser and NBT
format remain unchanged; arbitrary keys remain refused. Three new temporary
synthetic-save tests cover exact values/players/stages/products, nested or other
keys, duplicated proofs, corrupted compression and truncated saves. Focused14
passed with existing parser/receipt coverage. Final Windows Wrapper clean verify
passed1,630 tests (1,628 unit/two packaged integration), zero failures/errors/skips;
07:46, finished2026-10-16T19:16:50-04:00. No source/test changes during final run.
No live player exchange, native client save/crash recovery or menu is certified
by these tests. The future adapter still needs durable inventory proof, owner
handoff, local eligibility/quote checks and explicit uncertain-state recovery.
Before enabling exchange, extend the pending inventory guard for this key, including
milk consumption and crafting/container transfers. An unverified filled bucket
must not be moved into separately saved state or used while recovery is pending.
The current food-trade guard does not yet protect this new receipt type.

## October16 — saved product sale accounting (not a player menu)

Schema84 adds an initially empty `livestock_product_sales` ledger. The explicit
SQL-owner `LivestockProductSales` API accepts exact `LivestockProductSale.Terms`
for a saved, operating, qualified staffed herd in its actual village/present world.
Dairies sell only recorded milk; chicken herds sell only recorded eggs. Reserving
removes actual goods from sellable stock without revenue. Held goods retain their
storage space, so later production or incoming cargo cannot occupy cancellation
room. Completing credits the agreed real player payment once and releases room;
cancelling returns goods once. Conflicting terms or terminal outcomes are refused.
Accepted reservations can recover after absence or staff departure; new orders
must satisfy current eligibility. Arithmetic/SQL failures roll back all effects.

`PendingLivestockSale` is a canonical, versioned player-receipt format, not an
installed listener. One prototype milk stock unit materializes as one milk bucket,
using one player-supplied empty bucket; milk is offered singly to respect its item
stack limit. Eggs permit up to64 ordinary eggs per order and need no container.
That is an order quantity cap, not a stack-size declaration: the future inventory
adapter must split egg deliveries using the pinned server's actual item stack
limit and refuse insufficient destination space. Never create an oversized stack.
Numeric payment/rate terms are explicit, bounded and not fixed prices or an active quote.
This item conversion does not change abstract animal-production yields.

No public command, menu, price defaults or player inventory exchange is enabled.
The future Paper adapter must prove independently saved player intent before
reservation, revalidate empty bucket/payment/destination space before exchange,
and prove the saved exchange before completion. Cancellation requires proof that
no delivery happened. REVIEW outcomes must not automatically refund delivered
items. This backend deliberately cannot infer payment from an SQL reservation.
No price-setting authority, player proximity/permission check or crash-safe live
inventory exchange is claimed by calling this trusted backend alone.

Validation: full Windows Wrapper clean verify passed1,627 tests (1,625 unit/two
packaged integration), zero failures/errors/skips;08:47, finished19:07:55 ET. Seventeen added
cases cover both products, all receipt stages, malformed/changed terms, restart
replay, competing buyers, cancellation room versus real herd production, real
staff/stock/world checks, injected transactional failures, overflow and intact
schema83 upgrade/retry. Tests use temporary SQLite databases only. No Paper API
or player-facing runtime change; no new native event/animal behavior is claimed.

## October16 — buyer-supplied milk buckets approved

Players bring an empty bucket for each purchased milk bucket. Recorded dairy stock
supplies the milk; the container is the player's own item. No bucket means no sale
or payment debit. Stock, payment, container and destination space must be checked
again at exchange, with once-only recovery across retries/restarts. Eggs require
no container. This is the approved adapter contract, not an enabled trade command.

## October16 — durable capture eligibility and pending-loss fencing

Schema83 adds initially empty singleton `livestock_capture`: clean-stop flag,
trusted-history flag and observation timestamp. Before live capture can be eligible,
the SQL owner persists a dirty marker. A first empty history can be trusted;
existing herds with no fence cannot acquire guessed past coverage. An unclean
previous stop, explicit gap or unresolved saves remains untrusted through later
clean restarts. No automatic reset/review permission or animal repair is exposed.

`PopulationSession.livestockCaptureReady` additionally requires session readiness,
no capture gap and no staged unsaved loss. Every known native loss is staged in
memory before waiting in the bounded Paper handoff, not just when its SQL request
is submitted. Committing the first of several losses cannot briefly certify the
remaining queue. Exact successful retry clears its pending identity; failed or
rejected work stays pending. Shutdown draining precedes the fence; unresolved
work makes even a clean stop untrusted. Lost/expired unpublished candidates before
readiness mark an explicit gap; repeated gap notifications coalesce to one write.
Confirmed losses remain recordable when history is untrusted.

`/civ debug status` reports capture eligibility via cached state, without JDBC or
world access. This is a prerequisite for the future live herd adapter, not an
active scheduler or proof of physical reconciliation. The trusted standalone herd
backend remains available for explicit tests/composition; callers must gate any
future live operation on eligibility AND fresh safe physical/identity evidence.
A trusted marker does not certify physical newborn mappings, external world edits,
world/DB restores, plugin-free play or unseen missing animals. Those still require
review/reconciliation; absence alone never establishes death or a replacement.

The owner-close regression now waits for an already-started drain when close is
called again outside its owner. Owner-thread callbacks still return without
waiting for themselves. This fixes a Windows file-lock race exposed by the extra
fence write; population rules and the existing timeout remain unchanged. Fence
failures retain exception diagnostics and cannot falsely certify a clean capture.

Eleven new executed cases (nine repository/owner/migration, two handoff-gap/staging).
Old-save tests preserve their predecessor comparisons and explicitly require the
new table empty. Latest focused44 and final full1,610 passed. Guarded native
owner/status/staged pending/gap/restart/callback close and simulated dirty-marker
checks passed, with all predecessor data preserved. No actual JVM crash, physical
reconciliation or live-production certification; see PROJECT_STATUS/DEVLOG.

## October16 — hatched and explicitly spawned livestock origin

The protective adapter now observes successful cow/chicken CreatureSpawnEvent
origins EGG, BREEDING, SPAWNER_EGG, DISPENSE_EGG and COMMAND. This closes the egg-
hatching route that need not emit EntityBreedEvent. An anonymous BYTE under
`civilizations:livestock_human_origin` records protective human/uncertain origin,
not a player identity or owner. Egg dispensers are conservative protection too;
no nearby player is guessed as owner. Normal hatching, counts, species, items and
experience are unchanged. Cancelled and unsupported spawns do not write tags.

`recorded` accepts either protective PDC key and falls back to Paper's original
spawn reason for already existing untagged animals with those known origins.
Existing values of any type are preserved. Explicit player breeding still keeps
its separate `livestock_player_bred` evidence. Known saved village ownership
retains precedence in the existing adoption calculator; tags never perform a
transfer or override an individually saved player claim.

Immutable animal observations now also copy the raw native spawn-reason name,
using UNKNOWN for absent information. The DTO remains plain Java. NATURAL or
CHUNK_GEN is evidence only: safe continuous observation, location/pen authority,
existing ownership, names/leashes and viability remain separate mandatory gates.
CUSTOM/DEFAULT/UNKNOWN is not silently relabeled as natural. No automatic adoption,
retroactive entity scan, owner creation or live herd output is enabled here.

Four new executed cases cover spawn-origin tagging/idempotency, cancellations/
unsupported/natural/uncertain sources, retained malformed values and old untagged
hatching in immutable capture. Focused30 and final full1,599 passed; native
explicit-origin spawns/tagging/fallback/serialization/capture and milk/egg
regressions passed. No thrown-projectile/client-trading claim. Death handoff
warnings now retain their exception cause for diagnosis;
retry and shutdown behavior are unchanged. No schema migration; remains82.

## October16 — confirmed losses of original herd animals

`LivestockLosses` accepts trusted physical death observations for saved initial
animal UUIDs only. Unknown, player-owned and village animals not assigned to a
herd create no loss, ownership or new herd. Record the actual observed world and
native damage-type key without attacker/player identity. Death in another world
still removes that exact animal; original ownership/world history stays intact.
An exact retry returns its immutable receipt; changed evidence/operation refuses.

Pending losses overlay the current saved headcount. Original animals whose saved
maturity has passed reduce adults; otherwise remove exactly one from their saved
young cohort. They do not consume feed, create goods, advance time or alter prior
production/cohort receipts. The next actual production transaction saves the
reduced state and marks those losses applied together. Failure of either write
rolls back feed, output, cohorts and application; retry cannot subtract twice.
Disabled/absent worlds retain external confirmed loss but earn no production/time.

Schema82 adds initially empty `livestock_animal_losses` and its pending lookup
index, with references to original animals and before/application checkpoints.
No prior animal, stock, money, person, job or history conversion. Every older-save
comparison retains its prior row checks and explicitly asserts this new table
empty. New upgrade failure/rollback/retry is covered separately.

`ManagedLivestockDeaths` captures only confirmed, uncancelled cow/chicken death
events on the main thread. It does not cancel deaths or change loot/experience.
Immutable observations cross to the existing SQL-owner queue. One outstanding
request, exact retry with delay and graceful shutdown draining preserve accepted
evidence. Pending positive identities are bounded by the saved ownership roster;
unpublished candidates use a256-entry/30-second window and never establish death
from absence. Diagnostics show pending/unbound counts in debug observation output.
Physical loss recording still runs when production is disabled.

Current scope is original UUIDs, not physical offspring or authorized transfers.
The queue is in memory until the owner commits. A hard crash in that handoff needs
an unclean-capture fence/reconciliation before live production can safely resume;
this batch does not certify that crash gap or enable automatic adoption/output.
Missing/unloaded animals must never be silently removed or respawned. Eleven new
cases, focused111 and final full1,595 passed. Actual native adult/young/player
deaths, reduced survivor production, restart/retry/absence and schema81->82
preservation passed on guarded Paper. See PROJECT_STATUS/DEVLOG for exact limits.

## October16 — approved village product control and player-breeding evidence

User approved recorded trade as the initial route for village milk/eggs. The
separate `ManagedLivestockProducts` listener cancels empty-bucket cow interaction
in either hand, and prevents egg drops only for positive saved village IDs. Specifically, egg prevention requires a chicken source and EGG,
BROWN_EGG or BLUE_EGG output; unrelated drops, player-dropped eggs and other
species remain normal. The handler cancels before vanilla adds the item; it does
not delete arbitrary existing items, alter an inventory, debit stock, award money
or suppress death drops. Naming/leashing stay available. There is no new stock-
backed collection UI, milk/egg trade command or automatic herd enabled by this rule.

It uses the same immutable SQL-owner ID publication and `livestock.enabled` plus
session readiness as the breeding guard. Missing old-file livestock sections opt
out; installed YAML is not overwritten. The readiness window is not certification
for live herds: cold-start freeze/reconciliation and commit-before-publication for
future adoption must still be connected before automatic live production.

`LivestockBreedingProvenance` records a BYTE protection flag under the entity PDC
key `civilizations:livestock_player_bred` for successful player-bred cows/chickens.
Existing protected-parent evidence propagates to offspring even if a breeder is
unavailable. Cancelled/unsupported births are untouched. This listener records
evidence even when livestock production is disabled; it changes no breeding,
items, experience or ownership. It neither reads nor stores player identity.
Existing values, including malformed/legacy types, stay protective and are never
overwritten. The flag follows normal entity/world saving; it is not a separate
SQLite owner or an authority to claim/transfer animals. Authorized transfers still
need their own saved ownership/history and must never be inferred from this flag.

`PaperLivestockSource` copies that flag into immutable observations on the main
thread. The existing adoption calculator protects a tagged unclaimed animal.
Missing tags do not prove natural origin or allow uncertain old pens to be taken;
old animals, player ownership, names/leashes, enclosure authority and continuous
safe observation still require their separate checks. No existing entity is scanned
or tagged retroactively. No schema change; remains81. Twelve new executed cases;
focused28 and full1,584 passed. Native three-colour egg laying, both-hand event
dispatch, normal unclaimed drops and actual PDC serialization/capture passed.
No human-client or completed stock-trading claim; see PROJECT_STATUS/DEVLOG.

## October16 — approved protected grants and trusted atomic opening

Dedicated livestock businesses are eligible in the new-file startup-grants list.
The grant ceiling, shared per-period/lifetime caps, food assessments and retained
food money are unchanged. Existing installed lists opt in explicitly; missing
sections and opt-outs stay disabled. Funding eligibility alone starts no business.

`BusinessStartupGrants.startLivestock` atomically saves an explicitly qualified
business/site/store/first worker, its real village grant and the already confirmed
initial herd. `LivestockRepository.openRows` joins that owner transaction without
committing it independently; the public standalone herd-opening contract remains.
An animal must already have matching saved village/world ownership, and trusted
species/remaining-maturity observations. Player/unrecorded animals, wrong worlds,
unsupported stores, unqualified work and failed final checkpoints leave prior rows
unchanged. Separate legacy grant/herd setups require an explicit future transition;
a startup replay cannot retrofit guessed facts. Exact accepted replay returns
current assets/cohorts after production and restart without refilling them.

Opening supplies zero feed/products and creates no animals. Existing actual local
purchases can pay an owned supplier for real grain and deliver it before the herd
uses it. Those ordinary purchases are not a new automatic starter-feed policy or
part of a live opening planner. Future automatic founding must establish real
customers, ongoing feed, suitable site/qualified workers, fresh observations and
safe physical reconciliation before calling this trusted backend. There is no
new schema migration; version remains81 and prior records remain intact.

## October16 — bounded read-only live animal evidence

`PaperLivestockSource` captures one supplied already-loaded entity chunk on the
main thread. It never obtains/loads chunks itself, schedules a scan, changes
entities, assigns ownership or writes SQL. Immutable `LivestockObservation`
records world UUID, chunk, time, actual cow/chicken IDs, positions, raw age ticks
and name/leash flags. No player name, name text or invented breeding provenance
is recorded. Age>=0 identifies an observed adult; conversion to simulation
maturity requires the future trusted adapter's explicit policy.

Wrong world/thread fails before entity access. Unloaded entity chunks, exceeded
entity/animal budgets, duplicate identities, unsupported entity implementations
or crossed/inconsistent locations return unknown with no partial roster. Normal
empty observations do not establish deaths or clear a herd. Mooshrooms/other
species are excluded. Defaults/limits are caller-supplied, bounded at4096 raw
entities/256 animals per capture; a future live observer still needs per-tick
scheduling and total village budgets. No live observation loop/adoption is enabled.
Ownership, recorded player breeding, pen geometry/protection and authority remain
separate mandatory checks before adoption. See PROJECT_STATUS/DEVLOG for verified
unit/native results and the pending milk/egg player-interaction decision.

## Persistent herd/product backend — schema76

`LivestockRepository` is an SQL-owner backend, not a live adopter or scheduler.
Opening requires a dedicated `livestock` business/store and already confirmed
village ownership matching the saved town/world. Trusted observations supply
species and each original animal's adult status or remaining active periods to
maturity. Ownership alone is not species/age evidence. Initial UUIDs can seed one
herd only; exact retries return current state without replenishment. One herd per
business keeps crop-farm labor/catalogue changes explicit. Unit-sized feed and
milk/egg catalogue entries must already exist; opening supplies no goods/money.

Actual eligible local jobs and editable per-worker/site work limits bound care.
Real stored feed pays for outputs and births; pending deliveries reserve capacity.
The conservative output budget uses current free space only: a full store idles
without assuming consumption will free room. Transactionally save feed debit,
owned milk/egg credit, period receipt and cohorts. Receipts include constrained
before/after stock, available work/output space and diagnostic explanations. Absent/disabled mode accrues
nothing; skipped/stale periods and changed operation terms fail. Exact historical
replay survives later periods and restarts without duplicating work or inventory.
Unavailable businesses have no productive labor; active-world cohorts still age.

Schema76 creates `livestock_herds`, `livestock_initial_animals`,
`livestock_periods`, `livestock_young`, all initially empty. No existing animals,
stock, cash, crop catalogues or histories are converted. Tested migration failure
rolls back all new tables/version; retry preserves prior ownership. Slaughter is
not exposed: physical retirement/player drops require a safe handshake first.
Logical birth cohorts also need physical identity mapping before live activation.
Player-death handling, startup freeze/reconciliation, adoption/transfer publication,
pen signs and actual scheduled business founding/production remain unfinished.

Twelve temporary-database cases include240 periods/restarts, real paid grain and
milk delivery with conserved cash, no-labor/shortage/storage limits, ownership/
reuse/catalogue guards, late write rollback/retry and migration recovery. Focused40
passed. Full .\mvnw.cmd clean verify passed1,451 (1,449 unit/two packaged integration),
zero failures/errors/skips;8:06 at15:16:14 ET. Guarded Paper26.2 build129 packaged
SQL backend proof passed15:17:04 ET: actual feed/cohort/products, restart replay
and absent-world pause. Schema75-to76 audit retained every predecessor table;
126/129 exact, only normal clock/runtime/ore bookkeeping changed; no aging.
New main-fixture herd tables empty. Fixture stopped, properties restored and helper
archived. No world/entity mutations by the helper; no client/physical birth proof.
Existing Windows OSHI performance-counter warning appeared; no Civilizations,
probe or listener error. No human server changes. No human-server edits.

## Approved herd control and cohort calculation batch

User approved simulation-managed breeding/slaughter. `LivestockHerd` computes
only actual active periods: immutable normalized young cohorts mature gradually,
fed surviving adults can breed after saved cooldown, births require additional
real caller-supplied feed/work and free herd capacity. Capacity loss retains old
animals. Adult slaughter uses the existing protected reserve and cannot slaughter
young; milk/egg and meat units stay abstract until owned inventory integration.
No feed-linked deaths/spoilage or stock transfer is invented. Replay is deterministic.
This is not a persisted herd ledger; caller must not treat budget inputs as actual
stock without a future shared transaction and roster/projection reconciliation.

Strict optional `livestock` YAML: enabled, maturity12 active periods, breeding delay6,
feed4/work2 per birth, max1 birth/period. Cow/chicken reserve2, feed1/adult, renewable
output1/adult, meat4/cow or2/chicken, slaughter max1. All editable prototype terms;
zero birth cap disables breeding. Stored cohort mature-period schedules apply
forward; new terms do not retroactively reschedule existing cohorts. Current live
use is the guard enable flag; herd rates are calculator settings awaiting integration.
Missing old-file section opts out and files are never overwritten.

`AnimalOwnershipRepository.villageAnimals` returns immutable positive village-only
IDs. `PopulationSession` publishes them on its SQL owner at startup. The new Paper
guard uses that view and readiness, with no SQL/world access per event. It blocks
outside EntityBreedEvent births when either supported parent is village-owned;
intercepts actual cow/chicken food tags in either hand before vanilla consumption.
The breeding guard itself leaves buckets, naming and leashing available; the
separate products listener above now enforces the approved village milk rule.
Player/unclaimed/unsupported animals keep normal behavior.
No live adoption/transfer exists yet; future claims must publish after commit and
before physical control. Cold-start/failed readiness leaves the guard inactive;
startup reconciliation/freeze for recovered real herds is still required before
live adoption. Slaughter, player-death updates and herd products are not wired.

Fourteen added tests: six pure cases with2,400-period conservation/replay; six
birth/early-interaction cases; one strict YAML case; one village-only SQL identity
publication case. Focused21 passed; full .\mvnw.cmd clean verify passed1,439
(1,437 unit/two packaged integration), zero failures/errors/skips,7:37 at13:12:55 ET.
Guarded Paper26.2 build129 proof passed13:44:11 ET: SQL-owner identity publication,
actual cow/chicken food tags, synthetic native event dispatch, player/other-item
availability and unchanged fake-player feeding stacks. First fake player lacked PDC
and caused existing trade-guard listener errors; the audit refused its pass marker.
Fixed only the helper, then reran with no event errors. Real temporary animals
removed; fixture stopped, settings restored and helpers archived. Retry audit
verified one normal calendar month and two living residents aging one month; dead
rows, history, money/assets retained,123 other predecessor tables exact. This is
synthetic event/identity proof, not a client packet or actual vanilla birth-cycle
playtest. Production source stayed unchanged after full verification. Schema75 unchanged,
no human server edits. Production livestock and sign UI remain incomplete.

## Subsequent schema75 pen protection and snapshot classifier

`PenProtection` validates bounded connected geometry, an interior seed, an adjacent
sign coordinate and capped initial roster. `PenProtectionRepository` stores owner,
world/standing level, geometry and initial roster with new animal ownership in one
transaction. Exact retries replay; changed inputs, overlapping protected columns
or another player/village's animal refuse and roll back everything. Existing animal
provenance is retained. Protection lookup uses saved coordinates, not a current sign.
Authority is a trusted adapter input; this backend is not exposed to players.
Unknown existing pens still require admin/certified authority; sign placement alone
is never certification. Shared boundary columns conservatively prevent adjacent
claims in this initial backend; expanding that requires explicit safe overlap review.

Additive schema75 creates empty protected_pens/columns/animals and preserves all
predecessor rows. Eight temporary-DB tests cover restart/idempotence, denied authority,
changed owner/roster, overlaps, conflicting animals/atomic rollback, original provenance,
late failure/retry, migration74 rollback/retry and forged/disconnected geometry.
Focused91 older-save checks and focused29 geometry/preview/store/Paper cases passed.
Full .\mvnw.cmd clean verify passed1,425 cases (1,423 unit/two packaged integration),
zero failures/errors/skips,7:54 at12:25:29 ET. Native Paper26.2 build129 passed
12:29:13 ET: actual closed/open connected gates, exact nine-column interior, missing
fence rejection and unknown snapshot. Initial attempt stopped before any block
changes because no chunks were loaded; test-only setup now explicitly loads one
fixture chunk if needed. Production source never loads a world/chunk. Temporary
sky pen blocks restored, no animals edited. First schema73→75 and retry schema75
save audits passed:125 and129 predecessor tables respectively, with only normal
clock/economy/ore updates; ownership/pen rows empty, integrity/FK healthy. Settings
restored, backed-up history preserved, helpers archived and isolated server stopped.
No playable server or production database edited. Native helper is test-only and
not shipped; source/tests/diff reviewed before commit.

`PaperPenSource` reads only immutable snapshots captured on Paper's main thread;
it never calls World or loads chunks. Correct world UUID/metadata, chunk coordinates,
height bounds and snapshot count are validated. Three vertical levels plus bounded
connection reads certify simple flat common-cube footing, two air blocks, fences
with actual reciprocal connections, structurally connected gates (open or closed),
and two-block common-cube building edges. Unsupported walls/doors/ramps/slabs/materials,
wet/disconnected fences and missing terrain remain UNKNOWN. Known missing footing
is OPEN. This is conservative evidence, not all-shape collision/ownership inference.
Six proxy cases cover geometry, connections/openness independence, unknown/negative
chunks and wrong identity/height. No live observer/sign menu is wired yet.

Saved initial records/pen protection are backend-only. Automatic adoption, continuous
observation/player breeding capture, donation transfers, live signs/particles/menu,
claims-plugin adapters and simulation-controlled herds remain next. Herd authority
has been asked explicitly; no vanilla animal behavior is changed while pending.

## Subsequent schema74 ownership persistence batch

`AnimalOwnershipRepository` now records immutable first player/village ownership,
origin-world UUID, recording time and source per animal UUID. Exact retries replay;
changed ownership or provenance cannot overwrite the record. Public calls require
no active transaction; internal rows participate in a future caller-owned pen
transaction without independently committing. Loaded location/sign observations
are not a transfer. Origin world is provenance, not a constantly updated position.

Additive transactional schema74 creates initially empty `animal_ownership` and
its village-owner index. Village owners require an actual saved settlement bound
to that origin world; player records require an observed world. No existing animals
are inferred/adopted. Seven temporary-SQLite cases cover restart, retry/conflicts,
world constraints, failed inserts, batch rollback, migration73 failure/retry and
invalid owners. Focused94 passed with affected older-save regression cases; full
Windows Wrapper .\mvnw.cmd clean verify passed1,411 cases (1,409 unit/two packaged
integration), zero failures/errors/skips,8:05 at11:37:25 ET. No real server database
was modified. Source/tests and migration diff reviewed.

This backend accepts only trusted caller-established identity/authority/evidence.
It is not exposed to players and does not implement sign claiming, pen persistence,
land-claim permissions, player breeding capture, explicit transfers or adoption.
A future donation must use a distinct authorized/audited transition, not first
registration. Live adapter still needs main-thread snapshots, safe 3D classification,
continuous observation and disposable native verification before activation.

The initial domain batch below describes schema73 at that earlier commit; the
verified current artifact upgrades to74 when deployed; no server deployment occurred.

## Approved interaction direction

Free-range eligible animals are adopted automatically after reliable observation.
There is no per-animal player authorization requirement. Existing unknown pens
stay protected. Proposed `[Civ Protect]` / `[Civ Livestock]` signs request a bounded
pen preview; only explicit authorized confirmation establishes a claim/donation.
Exact sign syntax and menu are not yet implemented. Show particles, area and eligible
animal count before accepting; a closed courtyard can look exactly like a pen.

Recorded owners/delegates or a compatible land-claim manager may authorize known
ownership; admin resolution is required for unknown older ownership. Placement or
proximity alone grants no authority. Names, leashes and recorded player breeding
prevent automatic adoption. Village-pen designation covers eligible animals and
future offspring, not an individually player-owned animal. Saved ownership survives
escape, opened gates and missing signs. Ownership changes must be explicit/audited.

## Implemented plain-Java components

* `PenScan`: incremental deterministic four-neighbor horizontal flood fill over
  caller-certified columns. At most the supplied source-read budget per advance.
  Prototype engineering limits: radius32, maximum1024 interior columns; caller can
  select validated limits. Open terrain reaches the bounds and is refused, never
  treated as the rest of the world belonging to a fence. Unknown terrain rejects
  incomplete footprints. Invalid seeds, coordinate overflow and area caps refuse.
  Rejected results expose no usable partial footprint. Accepted sets are immutable.
* `PenClaimPreview`: binds complete geometry and animal IDs to one actor, permanent
  world UUID, standing level and expiration. Confirmation requires fresh identical
  footprint/membership and a fresh authority check. Changed evidence/animals,
  another player/world/level, expired preview or revoked authority are refused.
  A READY result is not a saved claim; persistence still needs its own transaction.
* `LivestockAdoption`: read-only reason-coded eligibility. Recorded ownership wins
  over location. Protected/unknown pens, named/leashed/player-bred unclaimed animals
  cannot be adopted automatically. Incomplete evidence waits; free range requires
  the supplied observation duration. Already-village animals are not adopted twice.
* `LivestockWork`: deterministic prototype work using actual adults, feed, labor,
  storage and a requested slaughter amount. Renewable output consumes real feed
  and work without killing animals; slaughter consumes work and reduces adults
  subject to a protected breeding reserve and per-period cap. Full storage/labor
  shortage cannot generate output. No consumer, money, spoilage or breeding invented.
  Renewable work has prototype priority over slaughter; no live rule/config is wired.

## Required adapter contract and remaining work

Source must be a stable captured view, not unsafe async live Bukkit reads. An
INTERIOR column certifies suitable footing/headroom at the chosen animal level;
BARRIER certifies a fence/wall/gate or adequate solid building edge. Gates are
structural barriers even while open. Known vertical escape is OPEN; ambiguous
heights, ramps, doors, diagonals or unloaded chunks are UNKNOWN until safely
classified. This scanner alone cannot prove a three-dimensional pen, attachment of
its sign, block origins, actual ownership or land-claim permission. Adapter must
check those before preview and recheck on confirmation. Do not force-load chunks.

No Paper listener/menu/particles, block classifier, config section, persistent
pen footprint records and live animal capture, continuous observation tracker, land-plugin integration,
herd juvenile cohorts/breeding, startup or live livestock production are wired.
The initial domain batch changed no animals or databases and retained schema73.
The subsequent additive schema74 batch above is temporary-DB tested only; worlds stay untouched.
Neither player pen construction nor a sign silently donates animals.

## Verification

26 added domain tests:20 geometry/preview/eligibility and six livestock-work tests.
Includes a walkway between buildings, closed courtyard, hole/vertical escape,
unknown chunk edge, caps/overflow, exact tick-read budgets, immutable previews,
changed membership/permission/identity/expiry, escaped ownership and uncertain pens.
The livestock-work sweep runs13,122 input combinations (9 adults ×9 feed ×9 labor ×18 space), checking
animal/feed/work/storage conservation and breeding/slaughter bounds. Extreme longs
also checked. Focused26 passed; full Windows Wrapper clean verify passed1,404
cases (1,402 unit/two packaged integration), zero failures/errors/skips,9:13
at11:24:55 ET. No native Paper smoke is needed for this unwired pure domain batch;
the future adapter requires disposable Paper checks before enabling adoption.
