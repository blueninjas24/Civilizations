# Saved mineral opportunity evidence

The live exposed coal/iron observer now saves positive, validated evidence through
the existing SQL owner. Schema 48 adds `mineral_opportunities`, keyed by settlement,
permanent world UUID and coordinate. It creates no deposits, businesses, inventory,
money, jobs or permission to alter land.

Each row retains the first accepted capture timestamp and the latest accepted
capture timestamp. Repeated or older observations do not duplicate coordinates or
move the latest timestamp backward. A changed mineral at a retained coordinate is
rejected atomically. New schema installs and upgrades start with no fabricated
opportunities or backfilled historical observations.

Validation and insertion share one transaction. Complete, consistent coal/iron
pairs must match the saved settlement's world and fit the existing 512-coordinate
budget. Tracking gaps and conflicting saved deposit identity reject the whole
survey. Placement/removal exclusions are checked before saving. Failed insertion
rolls back every row from that survey; safe retry is idempotent.

An empty or smaller observation does not erase earlier evidence. The scanner sees
only exposed ore in currently loaded chunks; absence from a later snapshot could
mean occlusion, a different chunk or changed coverage. Removed/placed coordinates
remain as historical observations and are marked excluded by current saved mutation
evidence. Saved deposit links are derived from authoritative coordinate claims at
read time, without replenishing an old reserve or creating a second deposit.

`PopulationSession.mineralOpportunities(town, limit)` returns an immutable diagnostic
page of up to 512 coordinates, ordered by world/x/y/z. A full page is not a claim of
complete coverage or a startup candidate selection. Historical diagnostics remain
readable during tracking gaps or world absence; new observations remain paused.
This bounded seam does not yet have a player-facing command.

The scheduled Paper diagnostic confirms that positive evidence was saved. The
adapter still captures world/chunk state only on the main thread and adds no chunk
tickets. All evidence crossing into persistence is immutable Java data.

Automatic founding still requires current revalidation, suitability, actual demand,
qualified available residents, storage and real funding. Saved evidence is not an
extraction authorization. Exploration, broader minerals, physical excavation and
land ownership remain separate work; Phase 4 supplies physical growth.

The user approved limited configurable village startup grants with food-budget
protection on October 10. Grant eligibility/accounting and automatic founding are
the next implementation, not behavior added by this evidence table.
