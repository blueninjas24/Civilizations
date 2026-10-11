# Saved mineral opportunity evidence

## Current surveys versus historical sightings

Schema 50 adds a latest-complete-survey header and its bounded coordinate members.
The existing live observer saves both this current frame and historical sightings
in one transaction. A smaller or empty newer frame replaces only current membership;
it never erases historical sightings or claims that ore outside the frame was mined.
Older frames can add historical evidence but cannot restore current candidates or
refresh expiry. Identical captures replay; different raw coal/iron evidence at the
same capture timestamp is rejected atomically. A versioned SHA-256 fingerprint
uses sorted raw coordinates, so later mutation exclusions do not change replay identity.
Capture time remains the original snapshot time, not the time persistence finished.

`PopulationSession.currentMineralOpportunities(town, maximumAgeMillis)` reviews
the saved frame on the SQL owner. The caller must supply a positive freshness
limit; this batch introduces no implicit gameplay lifetime or automatic scheduler.
Absent/unknown worlds, missing mutation history, tracking gaps, future timestamps
and expired frames return no candidates. Immediate adapter tracking failures reject
the session request. Healthy fresh frames return immutable, bounded coordinates
after rechecking current placement/removal exclusions and global saved deposit claims.
Another village's claimed coordinate is therefore unavailable too. A fresh empty
frame means no candidates in that frame, not no minerals in the whole village.

Freshness and membership survive restart. Legacy observations have no current frame
until a new complete survey is accepted; migration does not fabricate one. No money,
reserve, job, source retirement, world edit or chunk load follows from this review.
These are candidate coordinates, not a connected viable deposit or permission to
found a business. Startup still needs coherent patch revalidation, suitable permitted
sites, real demand, qualified available residents, storage and checked funding.

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
protection on October 10. Guarded grant accounting is implemented separately;
automatic opportunity-based founding remains unfinished. See STARTUP_GRANTS.md.
