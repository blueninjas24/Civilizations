# Forestry implementation checkpoint

## October17 — live environmental observation verified

PaperForestObserver now scans already-loaded chunks near saved village homes,
using immutable center/neighbor snapshots and bounded incremental geometry work.
PaperForestSource accepts vertical logs and nonpersistent leaves; horizontal logs
and persistent placed leaves do not qualify. Unknown cells/overflow reject a
partial frame; outer geometry margins are not negative evidence. Production code
never loads chunks or modifies blocks. Capture timestamps survive the scan queue.

/civ debug observation reports canopy-backed log counts, chunk and original capture
time, explicitly marked as environmental evidence with uncertified provenance.
The latest sample is an immutable in-memory diagnostic, not a saved business or
claim. Restart clears that sample; saved forestry habitats/work/assets are retained.
Recorded log placement/growth/removal tracking is still required before automatic
adoption. Geometry alone cannot certify old natural origin or player permission.

New-file forestry-observer defaults: enabled:true, horizontal-radius-chunks:1,
vertical-radius:24, cells-per-tick:512, interval-ticks:200,
minimum-vertical-logs:3, horizontal-canopy-radius:3, vertical-canopy-radius:2,
maximum-trunk-coordinates:512. All are strict editable settings within bounded
engineering limits. Missing old-file sections disable observation; installed files
are preserved. This enables surveys only, not automatic logging businesses.

Windows Wrapper affected verify/package passed12 unit +2 packaged integration
tests, zero failures/errors/skips,24.396s, October17 22:42:20 EDT. Only two new
configuration tests were added; existing geometry/resource/config cases were
reused. No schema change (92), full-suite repetition or speculative gameplay rule.
Actual pinned Paper26.2 build129 passed22:44:58: the ordinary observer reported
exactly the three expected trunk coordinates and original private saved-home/time,
rejected persistent leaves/horizontal logs and unknown neighboring margins, and
created no businesses. Nine temporary high-sky fixture blocks were verified AIR
before editing and restored exactly before shutdown; helper-only chunk tickets
were released. No production force-loading or human-world edits were introduced.

The main disposable fixture retained161 predecessor tables,154 exactly; seven
routine clock/capture/age tables changed by one normal population month. Resident
identity/status and assets were retained; integrity/FKs clean. Private proof DB
also had zero businesses/forests/work. Existing economy.yml was byte-identical,
original port restored, helper archived and backups retained. Main/human server
unchanged. Full Linux CI passed the preceding resource checkpoint2f6450b; its
Windows job was still running here. Final observer hosted verification follows.

| Detailed forestry connection | Status |
|---|---|
| Bounded canopy recognition | Verified |
| Saved renewable habitats and earned work | Verified |
| Paid timber delivery and real customers | Verified |
| Live loaded-chunk environmental observer | Verified |
| Native placement/growth/damage provenance | Pending |
| Automatic qualified, funded forest businesses | Pending |
| Complete live acceptance and industry mix | Pending |

These four of seven detailed steps do not complete the weighted Phase3 forestry
catalogue gate. No new gameplay decision blocks the approved protection work.

## October17 — saved resources and paid timber chain verified

The trusted backend now saves forestry habitats and runs registered logging firms
on the ordinary active-world industry clock. Qualified employees produce actual
owned timber within habitat, labor and storage limits. Warehouses pay for that
stock for real workshop needs, then customers pay for delivered inputs. Work can
earn configurable forestry experience. No player purchase or fake shopper is
needed for this chain; no personal wage/bank-account system was introduced.

Automatic forest observation/adoption is still disconnected. A trusted caller must
certify current provenance, suitability and actual customers before opening a
resource. The normal founding/employment rules enforce its saved qualification
requirements. There is no player command that bypasses those checks or instantly
creates a forest business. Live placement/growth capture and observed support
loss are required before automatic startup is enabled. No physical felling,
planting, world edits, land claim or use of player chest contents in this slice.

New economy.yml files contain forestry-resources: enabled, minimum-trunk-coordinates,
stock-per-trunk, maximum-stock, initial-stock-percent, recovery-basis-points,
units-per-worker and site-work-capacity. These retain the prototype numbers below;
zero recovery is allowed for admin scenarios. Missing old-file sections opt out,
and installed files are never rewritten. Work rates/recovery apply forward;
saved opening capacity/stock are retained through retuning/revisits/restart.
New defaults also map logging work to forestry experience and permit normal
vacancy staffing. Tool support remains under the separate editable business-tools
industry list. Unqualified or habitat-less work cannot trigger logging upkeep.

Schema92 adds four empty tables: forestry_resources, forestry_supports,
forestry_work and forestry_habitat_changes. Original support coordinates are
globally reserved to one saved site, including after habitat damage; closed sites
and history are retained. Opening composes the real shared food-protected grant,
empty store, first worker and habitat in one transaction. Existing generic startup
grants cannot be retrofitted as an invented original forestry opening.
Harvest, stock, period, receipt and experience commit together. Exact retries
cannot refill, repay or duplicate timber. An explicit habitat transition needs
fresh authoritative verdicts for every original support; a positive-only scan is
insufficient. Support return restores capacity, then gradual renewal, not stock.

Windows Wrapper combined affected verify passed170 unit +2 packaged integration
tests, zero failures/errors/skips,47.667s, October17 22:34:45 EDT. Eleven new tests
cover saved resource/startup, migration failure/retry with every old table intact,
habitat transitions, paid logger/warehouse/craftsman production, clock/restart/
absence, rollback and strict file settings. Existing related tests also ran.
Initial fixtures reused a workplace coordinate and incorrectly rejected zero
renewal; corrected. The chain test also caught and fixed the missing timber
resale allowance for registered forest producers. No assertions were weakened.
The clean command preceded those fixes; the final affected verify/package above
passed. Full Windows/Linux verification is delegated to the ordinary hosted build;
no local full-suite result is claimed for this source.

Actual pinned Paper26.2 build129 booted/upgraded the marked disposable schema91
fixture, reached population ready and stopped normally at22:36. All157 predecessor
tables survived,153 exactly; only four routine metadata tables changed, no aging.
Four new tables were empty, integrity/FKs clean, existing economy.yml byte-identical,
capture clean/trusted. Original port restored and backups retained. This was a
bootstrap/preservation smoke check, not live tree recognition or logging adoption.
The human server and main branch remain unchanged.

The remaining steps at the end of this document now concern live evidence and
automatic qualified startup; persistent habitats, earned work and paid supply
are implemented. Logging receives full checklist credit only after live acceptance.

## Earlier checkpoint — pure calculations

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
