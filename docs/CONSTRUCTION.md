# Construction foundation — current prototype

Phase 4 has started. The plugin cannot place blocks yet. Its first tools let an
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

Every site currently reports missing development-area permission. Saved pen and
land protections are not checked yet; the command says so explicitly. A preview
is a series of observations, not an atomic promise that blocks will remain safe.
Future construction must recheck current world, protections, entities and exact
before-states at the actual write boundary.

## Editable starter files

On first startup, the plugin copies two original files to
`plugins/Civilizations/construction/`: `starter_path.yml` and
`starter_storehouse.yml`. Existing files are preserved. Stop the server to edit,
then restart; there is no live reload yet. An invalid template disables construction
previews with a logged reason while population/economy remain available.

Format 1 contains an ID, revision, name, attribution, purpose, dimensions, palette,
ordered stages and anchors. Each stage has work, abstract commodity costs and
horizontal layers. A layer specifies `y` and exactly `size.z` strings, each
`size.x` characters wide. Palette symbols are one letter/digit; a dot skips a
coordinate. A skipped interior coordinate must still be clear at a new site.
Separate stages cannot overwrite the same coordinate.

The initial native palette supports cobblestone, oak planks/logs/doors and barrels.
Unknown types/properties and unsupported materials are rejected before preview.
This is an intentionally small supported catalogue, not a schematic importer.
More palettes, structures, terrain adaptation and authoring tools remain future work.
Starter designs are original, with attribution in each file. Keep attribution and
distribution rights when importing other people's buildings in a future importer.

Costs are editable abstract units, not one physical item per placed block. The
storehouse has 83 blocks across four stages, 90 work units, 8 stone, 20 timber and
1 storage fitting. These are plan metadata; no funding or material transfer occurs.

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
