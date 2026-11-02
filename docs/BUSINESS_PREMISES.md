# Workplace reuse reservations — development backend

Implemented October31 as groundwork for P4 vacancy reuse, not normal automatic
business relocation. Existing business markers remain location evidence; they do
not prove ownership, an empty building, safe interiors or permission to renovate.
November1 addition: /civ building workplacecheck now provides an operator-only
read-only access/shelter preview; reserveInspected can gate holds with its computed
exact-building/marker evidence. See WORKPLACE_INSPECTION.md. No workplace donation
flow or production capacity is enabled. P4 checklist item2.4 remains unfinished.

BusinessPremises runs only on the SQL owner. A new hold requires an explicitly
registered WORKPLACE building, its exact current use-consent epoch and frozen
layout hash, a present permanent world, the same settlement, an open existing
business and its fixed saved site inside the declared structure. Its compatible
marker must be positively confirmed within5 seconds, not missing or future-dated.
The request must be fresh and cannot predate current consent. No site moves.

Another saved firm inside the structure blocks a hold even when it has no premises
record. A firm is not deemed vacant merely because it is unloaded, lacks a visible
owner or has lost its marker. Partial unique indexes enforce one retained hold per
business and per building. The first implementation reserves the whole building;
multi-business units and relocation are not inferred from housing apartment units.

A hold creates no land title, property transfer, contents access, usable work
capacity, renovation grant, materials, money or labor. Whole-building operational
inspection and ordinary selection/scheduling remain prerequisites for activation.
Closed firms retain their original assets, sites and history; none are copied into
a new business. A building hold does not make the old business's goods public.

Release requires the business to be closed/expired or its exact use-consent epoch
to be revoked, excluded or replaced. Ordinary missing markers and absent worlds
do not release a hold. A separate saved operation records the release. Released
history cannot be deleted or reactivated. Exact reserve retries return the retained
receipt, including its release status, without renewing permission; callers must
never treat that receipt as a current operating permit. There is no automatic
release scheduler yet, so a stale retained hold conservatively blocks reuse.

## Persistence and verification

Transactional schema120 adds one initially empty business_premises table, its
foreign keys, exclusive retained-hold indexes and history triggers. Existing data
is preserved without backfill, fabricated consent or automatic reservation. Keep
the normal matching save/config/JAR backup before upgrade; old JARs refuse newer
schemas. No actual server database was opened or changed during development.

Six focused temporary-SQLite cases cover persistence/restart, exact historical
retry, current permission/marker/world checks, legacy occupants, closure/release,
redonation epochs, collisions, late transaction rollback and populated119 upgrade
rollback. The initial closure fixture omitted its required sale-history parent;
that test-only setup was corrected without relaxing application foreign keys.

Clean Windows Wrapper53 unit+3 packaged integration checks passed in1:03:

    .\mvnw.cmd clean verify -Dtest=BusinessPremisesTest,BuildingRegistryTest,BusinessRepositoryTest,HousingRoadProjectsTest,SchemaMigratorTest

Actual disposable Paper started the packaged120 JAR and passed the existing native
housing-queue smoke probe. Its empty120 database was healthy/FK-clean, server stopped
and helper archived. This is startup/regression evidence, not a native workplace
inspection or live premises reservation proof. No terrain changes were made.

Next: explicit workplace donation controls, saved operational reviews and the
ordinary reuse-before-construction coordinator. Preserve fixed
business identity, current player consent and separately approved renovation rights.
