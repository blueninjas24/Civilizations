# P4 saved building permission foundation

BuildingRegistry is a SQL-owner backend, not a player command or sign handler.
It records an explicitly reviewed donation without pretending to authenticate
the caller or inspect Minecraft blocks. User approved players explicitly declaring
ownership for the first donation of an unrecorded building; administrator approval
is not mandatory. A trusted adapter must verify the authenticated actor, permissions,
reviewed scope and separate ownership declaration. This is the approved trust policy,
not proof of old Minecraft authorship. A sign alone grants no consent.

## Retained identity and consent

Schema108 adds five empty tables: building_records, building_units,
building_consent_events, building_exclusions and building_exclusion_releases.
Existing settlement population, bed counts, economics, projects, residents,
animal ownership and all older history remain unchanged. The migration is atomic.

Frozen layout snapshots retain permanent building/village/world/unit UUIDs,
declared purpose, plot and structure geometry, shared/private space and declared
bed pairs. Hash, identity, bounds and unit-index mismatches fail closed on read.
Layouts/donors are immutable in this initial backend; boundary editing/versioning
and ownership transfers still require a subsequent workflow.

Donation/reclamation append receipts instead of deleting structures or history.
Every transition names the exact previous consent operation, preventing a stale
confirmation from revoking a later grant. Exact operation retries return the
saved receipt and never reactivate consent that was subsequently withdrawn.
Reusing an operation with changed terms fails. The original donor or an
authenticated administrator can reclaim; unconfirmed requests fail.

A redonation may restore the same recorded donor/layout after reclamation and
fresh confirmation. It must pass current protection checks. This backend does not
transfer building title, demolish blocks, remove residents, move household funds
or activate usable bed capacity. Household displacement is still to be connected
to a saved occupancy registry before reclamation is offered in normal gameplay.

## Protection and overlap

An explicit exclusion can protect an old structure/plot without inventing a
donor, housing use or apartment arrangement. Release requires confirmation from
its recorded author or an authenticated administrator and retains both receipts.
Replaying the original exclusion does not undo its saved release.

Initial donation/redonation refuses saved protected pens, reserved construction,
active explicit exclusions and another registered building's plot. These are
conservative column checks; two buildings cannot claim the same plot even at
different elevations. Distinct apartments should be units of one building.

ConstructionLandRepository now includes registered building plots and unreleased
explicit exclusions in every existing protection query. Reclaiming housing does
not make the physical player building available for construction over it. Releasing
an exclusion removes that exclusion only; a registered building, another exclusion,
a protected pen or another project continues to protect its own area. Query work
is clipped to the bounded local construction footprint; no world chunks are loaded.

## Not yet available

There is no live donation/reclamation/exclusion sign, plot tool or general building
command. No player can obtain rights through this backend directly. A separate current-world
bed/access inspector now exists (DWELLING_INSPECTION.md), without full habitability
or activation authority. Occupancy assignment, changed-layout revisions and a safe
authenticated confirmation interface are subsequent work. Donation permission is
not current habitability, availability, private property title or block-write consent.

The existing handbook's planned-control labels remain correct; this batch adds no
new player-facing instructions. No production server or save was opened.

## Verification

Eight temporary-SQLite registry checks cover reopen, exact replay, withdrawal and
redonation, authority/staleness/conflicts, exclusion/release, protection overlap,
late-transaction rollback, corrupted snapshots/indexes and107-to108 migration
failure/retry. Schema checkpoint clean verify2096 unit +3 packaged checks passed;
final combined inspection/registry clean verify49 +3 passed26.678s, no failures,
errors or skips. No actual server database was modified.

## Player confirmation workflow

BuildingConsentRequests runs on the SQL owner. Defaults cap64 pending actors with
one60-second preview each; these technical limits are injectable policy, not a new
live configuration section. A preview freezes the authenticated player, permanent
world/building identity, exact immutable layout, recorded donor and previous consent.
A new preview replaces that actor's old token. Previewing writes no registration.

Confirmation requires the matching actor/token, the building's present world,
current permission and an explicit ownership declaration for donation. Reclamation
uses the saved donor or current administrator authority. A non-admin player can
donate without admin review; a declaration cannot replace an already recorded donor.
Registry protection and expected consent are checked again in the save transaction.
Expected conflicts use Declined, distinct from database/corruption failures; they
do not mark population simulation health failed. An actual database failure retains
one frozen operation/time/authority for safe retry, while a definitive declined
transaction can be freshly confirmed within the remaining preview lifetime.

PopulationSession exposes only queued immutable-data preview/confirm/cancel APIs;
no Bukkit object crosses the owner boundary. Preview state is temporary, bounded
and lost safely on restart; saved consent stays durable. Old completed confirmation
replay returns its original receipt without restoring later-reclaimed permission.
The control adapter must present receipts as historical outcomes, not proof of
current permission or occupancy. No inventory, cash, block or housing capacity moves.

Six request tests and a session-owner integration check verify the player policy,
separate declaration, expiry/replacement/cancel, identity/world/permission refusal,
late protection and SQL rollback/retry, donor/admin reclamation and stale redonation.
Focused Wrapper clean verify39 unit +3 packaged checks passed32.970s, no failures,
errors or skips. No migration or native API change required another full/native run.
