# Saved village identity evidence

Schema 11 preserves existing data and adds observed worlds, settlement/world UUID
bindings, saved bed coordinates and world/entity-to-resident links. User approved
this strategy. No legacy world-name row is silently assigned a world UUID.

`VillageIdentityRepository` updates a world's display name while retaining its ID.
Binding a settlement, bed or entity is repeatable for the same identity; replacing
an existing binding fails explicitly. Bed foreign keys enforce the settlement's
bound world. Entity binding APIs require the resident to belong to that bound world.
Bindings are positive identity evidence, not population or housing capacity counts.
They do not delete absent beds/people from incomplete observations.

Matching collects saved owners of observed beds and the current settlement of
observed bound residents under a coherent read transaction. No match means NEW;
one identity means EXISTING; multiple identities mean CONFLICT with stable sorted
IDs. Nothing is created, merged or moved by matching. Caller-owned import
transactions can include matching without it committing their writes. Limits:
512 beds and 1024 villagers per match; these bound query count, not elapsed CPU time.

Stored deceased residents retain entity identity evidence, preventing a later
visit from inventing a replacement person. The projection layer must reconcile
dead/moved residents before interpreting entities as active residents. No runtime
projection or importer is activated by this batch.

Migration now compares bound world UUIDs when present. Two bound worlds with the
same folder name cannot exchange residents; towns bound to the same world can
exchange residents despite legacy display-name differences. Unbound prototype
rows retain name-based legacy boundaries, clearly separated from bound UUIDs.
Explicit legacy adoption remains necessary before connecting such towns to a
bound-world population. No automatic reset or proximity-based adoption performed.

Nine temporary-database tests verify restart/rename, idempotency, same-name distinct
worlds, bed/entity conflicts, identity fallback, protected missing references,
deceased identity, caller rollback, consistent snapshots, schema-10 upgrade retry
and migration under renamed/duplicate folder names. Full Windows wrapper clean
verify passed 154 cases (152 unit, two integration), no failures/errors/skips.

Pending: transactional village enrollment, household observations, diagnostic
handling of legacy rows and overlaps, Paper adapter, calendar scheduling and NPC
reconciliation. This is identity persistence, not a completed gameplay milestone.
