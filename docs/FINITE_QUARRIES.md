# Finite stone and sand foundation

Approved October12: exposed existing stone/sand may supply quarry/pit evidence,
excluding recorded player placements. Minecraft cannot prove old block origins.
These are abstract finite deposits, not one item per physical block. Phase3 does
not dig, move blocks or grant rights over buildings.

## October12: native finite-source provenance connected

Existing bounded OrePlacementTracker now includes raw STONE, DEEPSLATE, SAND and
RED_SAND in native placement/break/piston/explosion capture. Entity block changes
(including falling or carried sand) retire confirmed original shares and exclude
new destinations; newly formed stone is excluded from natural discovery, so a
generator cannot create fresh quarry reserves. Cancelled/unchanged events do
nothing. Existing immutable coordinate transfer, atomic SQL mutation, pending
queue cap and fail-closed gap protection are retained. No reserve is transferred
or refilled by movement; other coordinates and extracted goods remain unchanged.
This extends the existing coordinate-bound removal/no-refill rule, not physical
quarry development. Old block origins still cannot be proven retrospectively.

PaperOreSource.quarries supplies an explicit immutable snapshot mode for raw
stone/deepslate and sand/red sand; default coal/iron behavior stays unchanged.
This classifier is not a live scheduled quarry survey or startup. Automatic
observation still needs known neighbor coverage, bounded current family-specific
evidence, real customer/site/founder checks and timed orchestration.

Eight new adapter tests; focused33 and full1,189 Wrapper tests passed. Guarded
native real-block/snapshot and synthetic event-handler checks proved share loss,
moved/generated exclusions and preserved extracted stock. Three temporary blocks
were restored exactly and the fixture ticket released; normal shutdown and save
audit passed. This does not claim real piston/falling-block physics simulation or
automatic resource opportunity selection. See PROJECT_STATUS for exact results.

The trusted `MiningStartupRepository`/`BusinessStartupGrants.startMine` composition
now accepts matching quarry/stone and sandpit/sand requests, as well as existing
mining/coal/iron. Startup needs the normal actual food assessment, protected
treasury, shared grant budget, saved job qualifications and available first worker.
Its caller must establish real demand, current suitable evidence and provenance.
Registration, debit, cash/store, finite coordinate reserves and employment commit
together. Incomplete, placed or removed source evidence and mismatched industry
fail without partial assets. This seam is not an automatic founding command.

`IndustryRepository.mine` uses the existing finite production/storage calculation
for all three extraction industries. Quarries/pits require the matching saved
coordinate-backed resource at their site. Work drains deterministic block shares;
removal loses only that share, leaving other sources and extracted stock intact.
Existing replay, period ordering, storage, closed/paused business and mutation-gap
guards apply. Rediscovery retains original reserves/yields; newly linked coordinates
add no reserves. Cash is not created by extraction.

`OreScan.quarries` is a bounded immutable-source stone/sand mode. It requires known
air exposure, excludes supplied placement/removal coordinates and reports incomplete
evidence on unknown/overflow. Default ore mode remains coal/iron only. It does not
classify biome, natural origin or ownership. A live adapter must supply known
neighbor coverage; unknown chunk edges must not be represented as absence.

## Editable prototype terms

`economy.yml` now validates optional `quarry-resources`; missing old sections opt
out and installed files are never rewritten. New opening terms are:

| Resource | Reserve batches/block | Units/batch | Batches/worker | Site batches |
|---|---:|---:|---:|---:|
| stone |64|3|6|32|
| sand |64|4|12|32|

One observed stone block can support192 total abstract units, one sand block256,
spread over work periods and limited storage. Numbers are prototype settings;
abundance and work speed differ from prices. Saved deposit reserves/yields survive
retuning. The policy supplies terms to future orchestration; trusted explicit
startup/production APIs still take explicit terms. No automatic quarry scheduler
was connected by the initial backend batch. Native placement/source-loss coverage
was subsequently added as described above; automatic quarry scheduling remains absent.

## Migration and verification

Schema64 transactionally rebuilds only `mineral_discovery` to admit stone/sand.
Existing coal/iron identity, evidence, shares, output, history and other table rows
are copied unchanged. SQLite parent rebuilding temporarily disables FK enforcement
before the upgrade transaction when needed, checks every FK before commit, then
restores the original enabled setting on success/failure. Invalid existing foreign
keys fail the upgrade rather than silently certify a damaged save. Rollback restores
the old constraint and version; no assets or sources are fabricated.

Tests use temporary SQLite databases: both producers' real grant/output/removal/
restart/no-refill, partial/placed/mismatched evidence, late source-write rollback,
whole-old-row upgrade preservation and both pre/post-rebuild failure/retry. Pure
tests cover scan bounds, exclusion, unknown/overflow, unchanged ore mode and separate
labor/storage/abundance. Configuration tests cover edits, legacy opt-out and strict
errors. Exact full verification is recorded in PROJECT_STATUS/DEVLOG.

Next: scheduled main-thread loaded snapshots and complete bounded survey persistence,
then current-fact
qualified automatic startup for genuinely funded customers. Household/public
material customers require the separate needs-based purchasing decision. No
warehouse catalog rewrite, fictional shoppers, physical digging or forced chunks.

Verified October12:134 focused tests and full Windows `.\mvnw.cmd clean verify`
passed1,163 tests (1,161 unit +2 packaged integration), zero failures/errors/skips,
6:11 at14:34:42. No new Paper world API behavior; no additional native run. Human
server/config/database untouched. Phase3 broad checklist counts remain unchanged.
