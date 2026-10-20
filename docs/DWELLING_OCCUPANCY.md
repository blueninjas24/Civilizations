# Saved dwelling occupancy — P4 backend

This is the SQL-owner foundation for household assignments, not a live command,
automatic relocation system or active replacement for the older bed-group capacity.
No residents move and no housing capacity changes merely by installing this batch.

## Records and boundaries

Schema109 adds six initially empty tables: dwelling_reviews, dwelling_review_units,
dwelling_current_reviews, dwelling_assignments, dwelling_active_occupancy and
dwelling_departures. Existing people, families, capacity, buildings and consent
history remain unchanged. Version markers and DDL commit together. Older plugin
versions cannot safely open the upgraded save; retain a consistent matching backup.

DwellingOccupancy takes plain Java inspection evidence and a present-world context
on the SQL owner. It never accesses Paper or loads chunks. Only a currently donated,
non-excluded registered home can receive a new assignment. Inspection must match
the saved layout, current consent and covered-bed policy, and be at most30 seconds
old. Inspection predating renewed permission cannot authorize occupancy.

A complete inspection stores immutable per-unit bed counts and reasons. Incomplete,
stale or missing-world evidence cannot replace a known review. A newer complete
inspection can record a broken roof, bed or route as zero usable beds without
deleting the household's occupancy/history. The later reconciliation layer must
respond to actual damage; absence of a visible villager is never vacancy evidence.

## Whole families and vacancy

An assignment requires a saved household in the home's village, at least one living
member, and enough qualified beds for every living member. Each household has at
most one active unit; each unit has at most one household. No family is split and
no parentage is invented. Unit ownership is not sold or transferred.

A move names its exact previous assignment. Leaving the old unit and entering the
new one commit together; any SQL failure retains the original home. Both homes
must belong to the operation's present world. A replay returns its historical
receipt without moving the family back or resurrecting a revoked assignment.

Vacancy is determined from saved occupancy, never from nearby entities. A vacancy
query with unknown or stale inspection says unverified rather than permitting a
new-build bypass. The later housing planner still needs to check suitable vacancies
before requesting construction.

An empty household can free a unit only when its saved living-member count is zero.
Unloaded residents do not count as dead. Reclamation now withdraws all assignments
in the building in the same transaction as the consent event, retaining each
departure and the family itself. Exact retries cannot evict a later redonation's
occupants. Reclamation UI remains disabled until capacity reconciliation is ready.

## Integration still ahead

The existing enrollment system groups observed beds into household_capacity.
Writing the new capacity there directly would be overwritten by subsequent scans
and could double-count beds. A transition policy for existing families/housing is
pending user choice; no silent replacement is made here. Live assignment scheduling,
capacity reconciliation, donor reclamation controls, apartment editing and
vacancy-first automatic home construction remain unfinished.

Native surveys collect evidence incrementally; production activation also needs
current-world/edit reconciliation, not merely a recent timestamp. This backend
does not claim to make a multi-tick block scan an atomic world snapshot.

## Verification scope

DwellingOccupancyTest uses test-owned temporary SQLite databases. Seven scenarios
cover reopening/exclusivity, whole-family eligibility, stale/incomplete/damaged
reviews, atomic move failure, transactional reclamation/redonation, saved deaths
and genuine108-to109 upgrade failure/recovery. The upgrade fixture uses the real
ordered migrations, retains old rows/capacity and checks SQLite integrity/FKs.
No real server database or world is used. See DEVLOG.md for final build results.
