# Atomic village enrollment prototype

Schema 12 adds optional household links to saved bed evidence. Existing identities
and population/history are preserved; no old home mapping is invented by migration.
`VillageEnrollmentRepository.enroll` explicitly consumes a bounded recognized
candidate and commits settlement/world evidence, home/bed links, imported residents,
entity bindings, housing observations, prototype partnerships and saved headcount
together. Failure rolls the whole import back. It does not access Bukkit or run
automatically in the current plugin.

Repeat visits and world renames reuse saved identities. Entity bindings prevent
double importing and retain deceased people without resurrecting them. Existing
residents keep names, ages, homes and simulated status; loaded entity counts never
replace their authoritative saved population. New resident IDs derive from world
and source-entity UUIDs; custom names are preserved. Generated names are temporary
identity-based labels. A new settlement/home gets a persisted UUID once successful.

Editable prototype defaults: connected beds within 8 blocks form a shared home,
two residents of capacity per observed bed, imported adults approximately 20 years
(240 months), existing children provisionally age 0 (unknown exact age), and enabled
adult partner formation with minimum age 18 (216 months). Geometry is an initial
room grouping heuristic, not proof of a real building or locked occupancy rule.
Partners share a saved home; existing children receive no invented parents.
Family/housing defaults are documented for later tuning, not claims of balance.

Complete observations replace housing capacity with current positive evidence;
homes not evidenced remain unknown. Partial observations invalidate this town's
capacity rather than treating missing beds as zero or retaining stale fertility
permission. Bed identity links remain intact even when capacity becomes unknown.
The future Paper observer must accurately define completeness; an incomplete scan
must not mark itself complete. People are never deleted due to absent observations.

Multi-town matches and newly connected groups spanning multiple saved households
fail with a diagnostic; there is no implicit town/home merge. New enrollment stops
if any old settlement lacks a world binding, because its world/identity cannot be
safely inferred from a folder name or proximity. Explicit legacy adoption is still
required. Existing trusted matches can continue. No development DB was reset.

Eleven temporary-DB cases verify initial import, names/ages/capacity/family, restart
and rename, incomplete observations, deceased identity, late-save rollback/retry,
protected legacy data, child ancestry/disabled pairing, separate homes/transaction
ownership, saved calendar-driven births independent of visible census, protected
home overlap, and schema-11 upgrade retry. Windows wrapper clean verify passed
165 cases (163 unit, two integration), zero failures/errors/skips.

Pending: runtime configuration, bounded main-thread observations, dedicated DB
owner/scheduling, reconciliation/diagnostics, explicit legacy adoption and Paper
population playtesting. Monthly partner formation remains unwired; current replay
3 is unchanged. Existing listener is still registered until the replacement is ready.
