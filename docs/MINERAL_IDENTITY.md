# Mineral coordinates and finite discovery identity

Before an automatic observer can establish mining opportunities, separate sites
must not mint duplicate deposits from the same observed ore. The older explicit
IndustryRepository discovery API protects one saved site but knows no coordinates.
This batch adds a coordinate-aware, trusted-owner discovery path. It does not yet
scan Minecraft, establish natural origin, automatically found/operate mines, alter
blocks or grant player-build permissions.

## Observation and matching

MineralObservation holds permanent world UUID, timestamp, completeness, coal/iron
kind and at most 512 unique ore positions. Positions have canonical XYZ order;
duplicates and unsupported kinds are rejected. Partial/empty surveys cannot create
a deposit. Empty evidence is not proof that a saved reserve is exhausted.

MineralDiscoveryRepository requires a registered site in that exact world. New
deposit UUIDs derive from permanent world, mineral and the canonical first observed
ore position. A world's name, approach direction or caller-supplied random ID does
not establish a new geological identity. Indexed world/XYZ evidence matches the
actual original deposit on revisits and after restart.

A coordinate can belong to only one saved deposit in that world. Another site
seeing it must resolve to the existing site first; it cannot take the ore or mint
an alternate deposit. A survey overlapping multiple deposits reports conflict;
there is no automatic merger or combined pool. Identical coordinates in different
permanent worlds are independent. Replacing an ore kind cannot change old identity.

An overlapping trusted survey may link previously unseen coordinates to the same
deposit to protect subsequent visits from another direction. It never increases
initial/remaining reserves, changes the original observed count or reinterprets
saved yields. A disjoint survey cannot extend a registered site's deposit by
assumption. Observations predating the initial discovery are rejected.

These rules do not define geological clustering, mine ownership acquisition,
resource provenance or exploration expansion. The future adapter must establish
plausible coherent surveys; arbitrary separated coordinates supplied by a trusted
caller are not natural-resource certification. No proximity region is adopted as
identity and no regeneration option exists.

## Persistence and compatibility

Schema 32 adds mineral_discovery metadata and globally unique mineral_evidence
coordinates, with world/deposit foreign keys and an index for saved claim counts.
Deposit rows, geological yields, metadata and all initial claims share a single
transaction. Extensions also roll back together on failure. Reserved goods,
business money, population and old histories are unchanged by migration.

Existing legacy deposits have no reliable coordinate evidence. Migration does not
fabricate it or reset depletion. The new path reports that explicit adoption is
required for such a site; a future tested adoption tool is not implemented here.
The old explicit discovery API remains for trusted callers and existing fixtures;
it has site-only protection and must not be used by future automatic observers.

Internal extraction of IndustryRepository.discoverRows lets coordinate discovery
compose old deposit saving in one transaction. Existing deposit term encoding,
UUID replay and finite mining semantics are preserved. New rates/yields apply to
new observed deposits; revisits retain the original geological terms. A lower
binary rejects newer schema versions; downgrade requires a consistent backup.

## Approved origin policy

On October 9 the user approved exposed existing ore as finite prototype evidence,
excluding newly tracked player placements. Old natural/player origin remains
unknown; the approval accepts this limitation without granting block rights.
OreScan and PaperOreSource now implement bounded snapshot classification and
six-face air exposure, with supplied exclusion coordinates and conservative partial
survey handling. They are not registered runtime observers. Durable placement
exclusion storage and transactional discovery rejection now exist; live capture,
coherent deposit grouping, live scheduling, per-mineral tuning and
automatic mine founding/operation remain subsequent work. See
EXPOSED_ORE_SCANNING.md and DECISIONS_AND_OPEN_QUESTIONS.md.

## Verification scope

Temporary SQLite cases test no refill across depletion/restart/retuning, linked
surveys, site conflicts, multi-deposit conflicts, permanent-world independence and
rename, incomplete evidence, legacy compatibility, mineral changes, old timestamps,
rollback/retry and schema-31 failure/recovery. Domain cases test bounded unique
canonical observations. No test touches a real server database.

The marked disposable WorkshopProductionProbe additionally uses this new path with
explicit synthetic iron coordinates, verifies that linked evidence leaves 20
initial/16 remaining batches unchanged, then exercises its paid workshop chain and
restart. This tests packaged persistence on Paper, not actual ore snapshots or
natural-resource discovery. It uses its own database/SQL thread, without world or
entity modifications, and remains outside the shipped plugin JAR.
