# Forestry implementation checkpoint

October17: plain-Java environmental recognition and renewable timber calculations
are implemented and tested. They are not yet scheduled live logging businesses.
No blocks, animals, player inventories or databases are changed by this slice.

ForestScan consumes a caller-supplied immutable loaded snapshot source. It finds
vertical trunks with nearby leaves, excludes supplied known player log/leaf
placements, and reports exact, sorted supporting coordinates. A bare log pillar
does not qualify without canopy evidence. Historical natural origin cannot be
proven from shape; this is environmental evidence, never property or felling rights.
Live provenance capture is still required before automatic logging adoption.

Capture and geometry evaluation both use the caller's incremental budget. The
source is read once per cell; later geometry uses that immutable buffer. Bounds
contain at most one million cells, output at most512 trunk coordinates. Missing
data and output overflow make the evidence incomplete. Outer-margin trees whose
geometry cannot be fully inspected are ignored without invalidating an interior
tree. Positive sightings do not authorize negative habitat/removal transitions.
No chunks are loaded by these domain classes.

ForestStock models renewable timber separately from finite geological reserves.
One earned active-world period replenishes gradually, then work is limited by
actual requested output, qualified workers, site capacity and available storage.
Absent worlds pause both work and renewal; there is no elapsed-clock catch-up.
Capacity cannot bank extra renewal. Confirmed support loss reduces capacity and
retained stock; restoring support never immediately refills timber. This calculation
does not implement a physical tree growth or felling event.

ForestryPolicy.prototype currently provides experimental calculation defaults:
six observed trunk coordinates,16 abstract units per coordinate,8,192 maximum,
100% initial standing stock,1% capacity renewal per earned period,4 units/worker,
16 units/site. Geometry defaults are three vertical logs and a three-block
horizontal/two-block vertical canopy window. These Java prototypes are not yet
server economy.yml switches. Admin-file settings belong with the live integration;
existing server settings remain untouched. Values are tunable prototype terms,
not locked balance. A resource's eventual saved opening terms must prevent
revisits or retuning from refilling existing stock.

Seven focused tests cover canopy/placements/unknown evidence, bounded deterministic
batching, edge trees, repeated observation, non-1:1 capacity, work/storage limits,
gradual fractional renewal, absence and confirmed habitat loss/restoration.
Windows Wrapper focused package passed7 tests, zero failures/errors/skips,
16.628 seconds, October17 22:21:13 EDT. An initial test-only static method-reference
compile error was fixed before that passing run. No new Paper API or schema was
introduced, so the preceding quarry native/migration checkpoint was not repeated.

Next: saved per-chunk forestry evidence and placement/growth provenance, persistent
habitats/work receipts, qualified/funded startup using real timber customers,
paid warehouse/public supply and live diagnostics. No new gameplay decision blocks
those approved non-destructive Phase3 steps. Logging is still an unchecked live
industry; Phase4 physical felling, planting and property claims remain separate.
