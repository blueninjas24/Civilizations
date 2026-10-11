# Native supporting-ore removal

The production OrePlacementTracker now captures successful native player breaks,
destructive block/entity explosions and piston movement. Capture runs on Paper's
main thread. Only immutable world identity, coordinates, timestamp and reason
cross to the existing SQL owner; there are no async world reads or forced chunks.

Cancelled events, non-ore blocks and explosions that keep or merely trigger
blocks do not retire sources. Explosions use the final event block list. The
plugin does not cancel mining, change drops, place blocks or excavate terrain.
Direct world edits that emit none of these events are outside this coverage.

Breaks and explosions retire each removed coordinate's remaining share, preserving
other shares and already extracted goods. A piston retires only actual moving
ore origins. Both possible axis destinations remain conservative placement
exclusions; those exclusions do not retire adjacent saved ore. Placements and
origin retirements from one event commit together, so a later SQL failure cannot
leave half a mutation saved. Repeat events cannot retire or refill a source twice.

Native lists are capped at 512 blocks and each deduplicated coordinate list at
512 entries. At most 256 capture futures are retained; the existing application
owner queue also rejects overload at its own smaller bound. Any capture, queue or
save failure marks history untrusted immediately. Accepted work drains before
normal close. A clean shutdown never repairs a known gap; unclean restart also
retains the recovery fence.

## Production and older saves

Untrusted history pauses new discovery and coordinate-backed mining, preserving
goods, reserves and history. The live loop checks both the immediate adapter flag
and the saved trust fence; direct persistence mining also checks the saved fence.
Paused periods record an explanation and produce nothing. They do not accumulate
an extra production windfall. Other village simulation and workshops can continue;
existing owned cargo is still delivered under its original rules. Historical
accepted mining receipts remain replayable, and further removal evidence can still
be saved while mining is fenced. Abstract legacy deposits with no coordinates are
not silently assigned block evidence.

Schema 47 applies the stronger coverage contract once. Older databases containing
coordinate shares become untrusted without changing reserves, goods, money or
receipts: placement-only tracking cannot prove that old supporting blocks survived.
Empty saves have no such forced gap. Migration is transactional and retry tested.
Reviewed baseline/recovery tooling is still required before resuming a fenced save;
there is no automatic SQL trust reset or destructive database reset in this batch.
The human development server has not been upgraded.

Unsupported bulk editors, vanilla editing commands, direct plugin block writes and
offline world edits are outside the native-event guarantee. Existing recognized
editor guards remain conservative. This does not enable physical mining, automatic
first mines, new mineral catalogs or Phase 4 construction.

## Verification harness

Unit/persistence tests use temporary databases and immutable/proxy event fixtures.
They cover cancelled/non-destructive events, final explosion lists, piston origins
versus destinations, capture failures/bounds, atomic rollback/replay, owner-queue
shutdown, immediate/persisted gaps and schema-47 failure/retry.

src/test/paper/OreRemovalProbe.java is compiled separately against the packaged JAR.
It refuses every server except the marked localhost disposable fixture on port
25578 without players. It uses its own database and real loaded blocks, dispatches
synthetic native events through registered production listeners, and restores every
fixture block. No client action or physical explosion/piston engine is implied by
that event check. It does not force chunks or add chunk tickets; the operator may
temporarily load a disposable chunk and must remove that ticket before stopping.


Verified October 10, 2026: Windows `.\mvnw.cmd clean verify` passed 819 tests
(817 unit/two packaged integration), zero failures/errors/skips. The packaged
Paper probe passed registered synthetic native events on restored real blocks,
source/output conservation and immediate/restart gaps; the prior industry probe
also passed. The console test ticket was removed and no forced chunks remained;
the fixture stopped normally, schema 47 integrity/FKs and asset/history retention
passed, exact properties restored and probes archived. No human server changes.
