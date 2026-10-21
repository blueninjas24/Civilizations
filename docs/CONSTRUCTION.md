# Construction foundation — current prototype

Phase 4 now includes automatic paid family-home construction when explicitly
enabled. See AUTOMATIC_CONSTRUCTION.md for scope, settings and safety;
CONSTRUCTION_RECOVERY.md covers saved inspection and confirmed unused supplies.
The preview commands below remain read-only and grant no construction authority.

## Commands

The pure road-first site planner also accepts the configured surrounding-land
buffer for every rotation and mirror. It checks actual transformed dimensions,
connector coverage, nearby obstacles and protected cells before ranking viable
alternatives. Missing or contradictory classified terrain is incomplete evidence.
The same global candidate/cell-review limits include the buffer; rejected
candidates retain specific land findings. This preparation API does not enable
automatic scheduling, imply land permission or alter the native writer boundary.

- `/civ build templates`: list installed plans, sizes, stages and abstract costs.
- `/civ build housing <village UUID>`: read saved whole-family decisions in your
  current world. Use `/civ debug villages` for the full saved ID. Explains existing
  housing, permitted vacancy reuse, proposed homes and world/project/survey holds.
  This does not scan terrain, start a project, claim land or spend anything. Four
  reports may queue; one per player,30-second expiry. Permission/world/disconnect
  are rechecked on the server thread before delivery; SQL stays on its owner.
- `/civ build preview starter_storehouse [0|90|180|270] [mirror]`: inspect a plan
  centered eight horizontal blocks ahead, with its floor at the player's feet.
  Rotation is clockwise; mirroring flips the original east/west axis first.
- `/civ build preview starter_path`: inspect the small path segment.

`civilizations.construction.preview` defaults to operators. It grants read-only
inspection, never block placement, land ownership or financial authority. Console
can list plans; site inspection requires a player. Previews expire after 30 seconds,
use a shared 64-cell/tick budget and allow at most eight simultaneous requests,
one per player. They do not load chunks. Existing blocks, entities, out-of-height
cells, unloaded chunks, unsuitable ground and missing entrance paths are reported.
The initial support whitelist is deliberately conservative; no excavation or
grading is performed. Existing buildings cannot be approved through this tool.

A player preview itself grants no validated construction permission. Saved confirmed
fenced plots are checked through the SQL owner queue; empty plots remain protected
and released claims retain their history. Other claim plugins are not integrated. A preview
is a series of observations, not an atomic promise that blocks will remain safe.
Future construction must recheck current world, protections, entities and exact
before-states at the actual write boundary.

## Retained home projects

`/civ build funding <project UUID>` reads actual material/escrow and protected
village money/hauling readiness; it chooses no seller and spends nothing.
`/civ build projects <village UUID>` reads saved project/family/site and asset
status in the current world. `/civ build cancelproject <project UUID>` previews
unused cancellation, with separate `/civ build confirmcancel <token>` and
`/civ build cancelrequest` controls. Management defaults to operators and is
separate from read-only preview permission. Paid supplies, unreturned stock and
prepared physical work hold for reviewed recovery; history always remains.
See HOUSING_PROJECTS.md and handbook Chapters28,31 and32 for the enabled home
coordinator and separately confirmed recovery.

## Editable starter files

On startup, the plugin installs five original files when absent in
`plugins/Civilizations/construction/`: `starter_path.yml`, `starter_storehouse.yml`,
`starter_home.yml`, `family_home_six.yml` and `family_home_eight.yml`. Existing files are preserved. Stop the server to edit,
then restart; there is no live reload yet. An invalid template disables construction
previews with a logged reason while population/economy remain available.

Format 1 contains an ID, revision, name, attribution, purpose, dimensions, palette,
ordered stages and anchors. Each stage has work, abstract commodity costs and
horizontal layers. A layer specifies `y` and exactly `size.z` strings, each
`size.x` characters wide. Palette symbols are one letter/digit; a dot skips a
coordinate. A skipped interior coordinate must still be clear at a new site.
Separate stages cannot overwrite the same coordinate.

User approved automatic growth on suitable nearby clear land, with bounded checks
for buildings, farms, fences and saved protection. Unknown evidence pauses. Unmarked
empty player fields cannot be identified retroactively; reserved empty plots can use
confirmed fenced-area protection. The first automatic home-site recognizer is now connected under HOUSING_PROJECTS.md.
Ordinary gameplay will not require admin-approved build zones under this decision.

The initial native palette supports cobblestone, oak planks/logs/doors, barrels and white beds.
Unknown types/properties and unsupported materials are rejected before preview.
This is an intentionally small supported catalogue, not a schematic importer.
More palettes, structures, terrain adaptation and authoring tools remain future work.
Starter designs are original, with attribution in each file. Keep attribution and
distribution rights when importing other people's buildings in a future importer.

Costs are editable abstract units, not one physical item per placed block. The
storehouse has 83 blocks across four stages, 90 work units, 8 stone, 20 timber and
1 storage fitting. The home has 120 blocks, 120 work, 12 stone, 36 timber and 12 wool. These
are editable abstract costs, with no free material supply. Player previews do
not reserve stock or transfer funds.

## Saved proposals

SQLite migration 100 adds `construction_drafts`, tied by a foreign key to the
existing permanent world/village identity. A frozen version-1 semantic snapshot
preserves every stage, cost, coordinate, block state and anchor with a SHA-256
fingerprint. Subsequent template edits do not rewrite a saved draft. Exact retries
are idempotent; reusing an ID with changed terms or loading a corrupt snapshot fails.

Player site previews remain unsaved. The separate automatic home-planning path
can now save verified need/site proposals under HOUSING_PROJECTS.md. A draft creates no land rights, budget, material reservation, work credit
or finished building. Back up before installing newer schema versions: older plugin
versions correctly refuse a newer database. No development save is reset automatically.

See PHASE4_CHECKLIST.md for the remaining gates. P3 remains the separate completed
prototype checkpoint; postponed human survival testing is still valuable.

## Budgets and recovery foundation

`plugins/Civilizations/construction.yml` is installed only when absent. Cash is in
hundredths of an emerald (100 = one emerald). Business and village sections each
set allowed building purposes, purchase/period caps and minimum reserves. Village
food reserve is protected PLUS essential upkeep and already committed funds.
Business operating/known essential obligations are protected as well. These are
spending eligibility checks, not fees: existing materials are not charged again,
and real future purchases must pay a supplier. No money moves from a quote and no
new wage/personal-wallet system is introduced. Invalid settings disable previews
with a logged reason; existing files are preserved.

Schema101 adds an ordered before/after block journal. Each intent is committed
before any future write; acknowledgements occur separately. Exact existing output
can be acknowledged after restart without another write. Different current blocks
cause a retained conflict; neither buildings nor history are silently overwritten.
Missing world/chunks/authority or entity safety means wait. Journal entries provide
no funding, work credit or land rights. A future explicit repair workflow is pending.

The only current physical writer factory verifies a marked localhost disposable
server, zero online players and an explicit test footprint. No normal command or
simulation can invoke building work. Its acceptance helper placed/restored five
blocks, exercised exact retries and a database-reopen acknowledgement interruption,
preserved a changed block and left a remote chunk unloaded. This is not a full
process-restart or complete-building acceptance. Production writes must additionally
coordinate resource provenance (including placed logs), fresh claims, paid materials,
real available labor, completion checks and active use before the live gate opens.

## Guarded land, labor and owned stock (not a live builder)

Optional land/labor settings default safely for existing construction.yml files;
files are never silently rewritten. A bounded four-block buffer rejects nearby
non-ground structures, crops, fences, water and unknown evidence. Complete saved
protected columns remain excluded. This flat-site screen performs no grading.

Labor proposals cap public workers and private staff time, select deterministically
and preserve total available effort. They do not book labor or reduce production
yet; both must be connected together before work credits can be earned.

Schema102 adds private-business material reservation receipts/items. A fresh
trusted site check and actual owned stock are required for new reservations.
Stock moves atomically into a recorded onsite pile, releasing business capacity;
no cash is charged for goods already owned. Exact retry returns the same receipt.
Public procurement and stage consumption are not implemented. Unstarted private
stockpiles now have the all-or-wait return path below. Prepared authored logs receive sticky forestry exclusions in the
same transaction as their write intent, before native placement.

Basic sheep/wool support is now implemented as the approved supplier extension.
Wool requires actual herd feed/labor/storage; beds are never seeded for free.
Construction purchases and home occupancy activation remain subsequent work.

## Saved sites and full-house recovery

Schema104 saves scheduler reservations for frozen footprints, without granting
land ownership or physical write permission. Active reservations exclude their
columns from other projects/previews, across heights. New reservations require
fresh complete evidence tied to the exact draft and world, plus current saved
protection checks. Overlaps are refused atomically. Unused reservations can be
released with history retained; funded/started sites cannot be released through
that shortcut. There is no player-facing reservation/release command yet.

Material debits now require the matching active reservation and recheck newly saved
player protection inside the same transaction. Existing receipt replay changes
no stock and does not refresh physical authority. Unused held assets now have the return path below; started projects still need
reviewed physical recovery.

ConstructionHomeRestartProbe demonstrated120 actual native home blocks and four
bed pairs across two JVM runs, including a placed-but-unacknowledged block. It
restored all121 original cells afterward. Only the marked private fixture stages
its bounded previously generated chunks on restart; the actual writer never
loads chunks. The test grants no completed home capacity, paid work or normal
server construction authority.

## Returning unused materials

Schema105 records complete returns with before/after stock quantities and permanent
links to original reservation items. An unstarted project's goods return to their
source store exactly once, with no fee or cash transfer. Incoming cargo reservations
count against capacity. If the source lacks space, the whole pile stays saved onsite
and the operation remains unclaimed for a later retry. There is no overflow disposal
or partial return. A prepared block intent refuses this shortcut even when its
native outcome is uncertain. Original transfer receipts remain immutable history;
available() excludes fully returned goods. Only a complete unused-material return
unlocks the unused-site release path. This API is not a live cancellation command,
and does not authorize refunding consumed materials or removing buildings.

## Whole-household proposal planning

HousingProjectPlanner is a bounded deterministic domain API, not a live assignment
or building scheduler. It offers a known suitable unreserved village-owned vacancy
before a new whole-household plan, never reusing one vacancy twice in a review.
Private or wrong-world homes are excluded. Existing household projects suppress
duplicates, incomplete vacancy surveys block new construction proposals, and
absent worlds pause them. A family larger than every supported plan waits for a
fitting plan rather than splitting or constructing an unusable home.

construction.yml housing.maximum-new-proposals defaults to1, supports1..64 and
uses that safe default without rewriting old files missing the optional section.
Proposals grant no funds, work, permission, occupancy or capacity. Live household/
registered-home evidence and owner validation are still required.

A HOME template must have distinct anchored bed feet with complete matching head
blocks and native facing, no occupied states, and no extra unanchored beds. The
original home verifies four real pairs in all eight orientations. This validates
authored planned capacity; only a future completed-world check can activate it.


## Real supplier purchases (schema106)

ConstructionSupplyOrders buys only frozen material needs from qualified operating
local suppliers using actual stock and real private/village funds. Funding source
is fixed by the first purchase. Owned-stock and paid-supply routes cannot mix in
one project; this conservative prototype prevents double funding. Paid orders are
bounded to128/project and cannot exceed the plan even while cargo is pending.
Fresh exact-draft evidence, active site and current saved protections are required.
Public spending requires a current complete fed population assessment; existing
food/upkeep and period spending limits apply. No extra project wallet or clock.

Cargo shares ordinary industry hauling capacity (including storage upgrades), and
arrives only after the purchaser's existing earned industry/food periods advance.
Absent worlds do not deliver. Stock appears at the project, never as a second copy
in the purchaser's normal store. Exact replay preserves the original payment and
arrival even after restart or disabling new purchasing. Late SQL failures roll
back payer, supplier, stock and receipt together. supplies.enabled and positive
supplies.prices in construction.yml are editable, with defaults for old files.

Historical schema106 boundary: purchasing initially existed only as a backend
API. The current automatic family-home coordinator now connects payment, earned
delivery/labor and guarded native writes. Confirmed unused recall/return controls
retain saved assets/history; started physical work cannot be refunded.

The normal PopulationSession owner pulse now settles up to16 ready prepaid orders
in one transaction after existing simulation clocks advance. Ready orders are
selected across explicitly present worlds; absent or early cargo cannot block
other ready orders. A late failure rolls back the entire arrival batch. This
delivery-only adapter cannot purchase goods. Disabling new purchasing preserves
already-paid cargo. Headless sessions without a world availability snapshot do
not infer loaded worlds. /civ debug construction reports the last review without
JDBC access on the main thread or authorizing a building.

Verified46 unit +3 packaged checks, plus actual disposable Paper supply defaults,
admin diagnostic permissions/routing and native templates; zero construction writes.


## Earned public labor and stage escrow (schema107)

The normal food-period owner transaction can now book public construction effort
for funded paid-supply projects. It uses the editable labor terms loaded at startup,
not another clock. It reviews at most16 local active projects and1024 eligible
residents. Only current fully fed periods qualify; no retrospective catch-up job
assignments. Dedicated farm jobs must be enabled: legacy all-adult field output
already uses unemployed adults' time. Current employees, operating business owners,
active students and people who actually studied in this period are excluded.
Age/local identity/work eligibility still apply; jobs are never transferred.

Complete stage materials must have arrived in an earlier earned period. The first
positive work escrows that stage's entire frozen cost; stock() now reports only
unallocated delivered materials. Supplier payment and arrival remain immutable
history. Work receipts preserve each actual resident/period/effort; one person
cannot be counted twice in a period. Existing worker limits apply across projects.
Unused fractions at the end of a stage are not banked as free later work. Prior
stages must have all physical intents acknowledged before later effort is earned.
Saved protection, insufficient supplies, absent worlds and food shortage pause
work. Food checkpoint, stage stock and labor receipts commit or roll back together.

ConstructionBlockJournal.preparePublic validates full stage effort against its
resident receipts and backed material escrow, fresh exact-draft/site evidence,
current saved protection and ordered prior acknowledgements. It confers no native
writer/ownership permission or completed capacity. The generic journal entry point
remains for explicitly bounded development probes; current normal family-home
construction instead uses the full public gate and guarded writer. Private
employee diversion and broader premises remain future work. No family gets a home merely
because abstract work is saved.

Focused verification86 unit +3 packaged passed59.632s; earlier affected migration/
forest/study verification78+3 passed51.257s. Actual disposable Paper paid for two
stone units from four explicitly seeded fixture units, earned delivery and five
construction work units over three actual food periods, and then placed five
native path blocks through the public economic gate plus development-only writer.
Payment replay, restart-window retry, changed block conflict and unloaded-target
waiting preserved assets/world state; original cells restored. Both DBs at107
passed integrity/FK checks, server stopped and helper archived. This test does not
claim automatic discovery, free seed supplies in gameplay, private labor or home
capacity activation.

Migration guard: same-numbered public-work receipts follow the resident across
villages. Such a receipt excludes them from that period's new builder pool rather
than causing a late uniqueness failure that rolls back food. Two-village migration
coverage passed in the focused27+3 clean verify39.336s. Current prototype clock
identity remains conservative; no extra effort is awarded by relocating a worker.


## Saved housing candidate connection (read-only API)

HousingPlanning now reads effective saved family capacity and current explicitly
permitted vacancies before using HousingProjectPlanner. Unknown capacity is held
separately; stale/unreviewed housing cannot justify bypassing reuse. Active saved
home projects hold further proposals conservatively until household-beneficiary
mapping exists. No new command or scheduling, spending, assignment, land or block
permission. Inputs/results are bounded; actual SQL-owner calls require saved world/
village identity. Deterministic candidates still need actual funded safe sites.
Focused clean verify25 unit +3 packaged checks passed38.110s, including zero-write
and real temporary-SQLite unknown/stale/world/project guards. No migration or
Paper operation; no additional native proof was needed for this data-only batch.
