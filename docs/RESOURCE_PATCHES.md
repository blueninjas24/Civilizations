# Observed resource patches

OreClusters splits a complete bounded coal/iron observation into face-connected
patches. Distant blocks, diagonals and missing blocks are not assumed to be one
deposit. Each coordinate appears exactly once; no hidden coordinates are invented.
Output order and anchors are canonical, independent of input ordering. World UUID,
mineral, timestamp and completeness are preserved; collections are immutable.

This is a conservative engineering grouping of visible evidence, not a final
geological model or a new mine founding rule. No abundance, extraction speed,
price, ownership, block mutation, skill, investment or resource renewal terms
change. Configurable numeric production terms remain separate. Physical exploration
and mine layouts remain Phase 4; no ore beyond the exposed observation is revealed.

At most 512 input coordinates and six neighbor probes per coordinate are used.
Neighbor arithmetic cannot wrap across integer limits. Incomplete evidence fails;
complete empty evidence yields no opportunity. Chunk borders are not geological
borders: the function accepts adjacent observed coordinates across them, but does
not obtain snapshots, load chunks or assemble separately timed surveys itself.
Live observation still needs explicit bounded evidence assembly and completeness.

Patches are provisional opportunities, not persistent deposit IDs. Before founding,
the caller must resolve existing permanent coordinate claims and sites. Two patches
can already belong to one saved deposit; splitting evidence does not split its
identity or give it another reserve pool. Exclusions/removal can make a connected
patch appear disjoint, so grouping must not imply new rights or independent reserves.
MineralDiscoveryRepository continues to reject conflicting sites/claims, retain
saved yields/depletion and forbid refill. Unsupported or partial surveys are not
made trustworthy by this utility. No automatic mine observer/founding is enabled.

Six deterministic domain cases cover branches/distant patches, gaps/diagonals,
partial/empty evidence, 512-coordinate seeded reorderings, integer boundaries and
cross-chunk metadata. A temporary SQLite scenario establishes two independent
patches at explicit trusted sites, conserves initial reserves, mines one, reopens
the database and checks replay/retuning and duplicate-site rejection. These tests
do not modify the server database or claim natural discovery or actual mine activity.

Windows `.\mvnw.cmd clean verify -B -ntp` passed 631 cases (629 unit/two packaged
integration), zero failures/errors/skips. This domain-only batch introduces no
Paper adapter; the preceding piston listener's guarded native check is recorded
in LIVE_ORE_TRACKING.md. No native automatic-mine acceptance claim is made.
