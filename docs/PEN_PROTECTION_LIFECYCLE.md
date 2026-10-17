# Saved pen protection lifecycle

This batch prepares protection controls before automatic livestock adoption.
It does not enable signs, commands, donation, breeding or new automatic farms.

## Recorded areas and animal owners are separate

A confirmed pen retains its original ID, owner, footprint, sign evidence, animal
roster and timestamp. Opening a gate, removing a sign, unloading a chunk or an
animal escaping never removes these records. Physical geometry alone grants
neither land authority nor permission to take another owner's animal.

An explicit area release records its operation ID, actor, pen, timestamp and
reason. Only the saved owner or a trusted adapter's freshly certified admin
authority may request it. Releasing the area keeps every animal's original
ownership and provenance. It neither donates livestock nor grants permission to
edit buildings. Later protection of that area requires a new confirmed pen ID.

An exact release retry returns the same receipt. Changed terms, another pen's
operation ID, dates before original protection and unauthorized actors are
refused. Replaying the original protection request after release returns the
historical record without reactivating it. A released pen ID cannot be reused.

Active adjacent pens can share boundary fence columns. Any intersection involving
either pen's interior is refused. `atAll` returns all active claims at a column
in stable UUID-text order; the existing `at` convenience returns the first. A
future UI must use all matches where a shared boundary would be ambiguous.

## Storage and threading

Version88 transactionally rebuilds `protected_pen_columns` with all original
rows intact, removing the old global coordinate uniqueness from historical
data. `active_pen_claims` initially includes every existing saved pen; an indexed
location query and insertion trigger enforce active area conflicts. Initially
empty `pen_releases` stores explicit releases. No historical ownership, goods,
funds, residents or animals are reset, moved or reclassified.

Protection saves and release receipts each commit atomically with their active
area changes. Failed writes restore the prior state. Old protected pen/animal
records remain queryable after release. Application handoffs use the existing
bounded SQL-owner queue and immutable values; expected permission/conflict
refusals do not replace its database-failure diagnostic.

The booleans accepted by repository/application APIs are trusted adapter evidence,
not authorization checks against Minecraft players. The forthcoming sign flow
must recheck actor, world, level, fresh complete enclosure/animal membership,
preview expiry and actual land/admin authority before submitting a new claim.
No public player API is enabled by this backend.

## Verification

Focused50 tests passed, including11 new temporary-SQLite lifecycle cases and
one owner-queue case. Windows .\mvnw.cmd clean verify -B -ntp passed1,788 tests
(1,786 unit/two packaged integration), zero failures/errors/skips;11:39,
finished2026-10-17T11:01:25-04:00. Source/test Java remained fixed during the
full run. Tests use private temporary saves, never the human server database.

The helper was compiled against the verified packaged JAR and passed on guarded
Paper26.2 build129 at11:01:48 EDT. The main test save upgraded87 to88 with all147
predecessor tables retained/143 exact; only ordinary clock/runtime/capture/ore
metadata changed, no aging. New main tables empty, integrity/FKs clean. The
server stopped/settings restored/helper archived/backups kept. No blocks or
entities were modified. See PROJECT_STATUS for the checkpoint.

The excluded `src/test/paper/PenLifecycleProbe.java` checks populated migration,
shared fences, authority, area release/reclaim, retained animal ownership and
restart in its own synthetic database under the marked empty loopback fixture.
It makes no world/entity changes and does not certify native pen geometry,
player sign interactions or a crash between independent world/database saves.
