# Construction foundation — current prototype

Phase 4 has started. Normal gameplay cannot place blocks yet. A guarded writer
exists only for marked disposable tests. Its first tools let an
administrator inspect authored plans and inspect a possible site without changing
blocks, player inventory, accounts or industry capacity.

## Commands

- `/civ build templates`: list installed plans, sizes, stages and abstract costs.
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

Every site currently lacks a validated construction permission. Saved confirmed
fenced plots are checked through the SQL owner queue; empty plots remain protected
and released claims retain their history. Other claim plugins are not integrated. A preview
is a series of observations, not an atomic promise that blocks will remain safe.
Future construction must recheck current world, protections, entities and exact
before-states at the actual write boundary.

## Editable starter files

On first startup, the plugin copies three original files to
`plugins/Civilizations/construction/`: `starter_path.yml` and
`starter_storehouse.yml` and `starter_home.yml`. Existing files are preserved. Stop the server to edit,
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
confirmed fenced-area protection. The automatic recognizer is not connected yet.
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

The repository API is ready; player previews are not yet saved or submitted as
projects. A draft creates no land rights, budget, material reservation, work credit
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
Public procurement, stage consumption and safe cancellation/return capacity are
not implemented. Prepared authored logs receive sticky forestry exclusions in the
same transaction as their write intent, before native placement.
