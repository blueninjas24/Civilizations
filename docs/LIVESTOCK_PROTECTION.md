# Livestock protection and work foundation

## Subsequent schema74 ownership persistence batch

`AnimalOwnershipRepository` now records immutable first player/village ownership,
origin-world UUID, recording time and source per animal UUID. Exact retries replay;
changed ownership or provenance cannot overwrite the record. Public calls require
no active transaction; internal rows participate in a future caller-owned pen
transaction without independently committing. Loaded location/sign observations
are not a transfer. Origin world is provenance, not a constantly updated position.

Additive transactional schema74 creates initially empty `animal_ownership` and
its village-owner index. Village owners require an actual saved settlement bound
to that origin world; player records require an observed world. No existing animals
are inferred/adopted. Seven temporary-SQLite cases cover restart, retry/conflicts,
world constraints, failed inserts, batch rollback, migration73 failure/retry and
invalid owners. Focused94 passed with affected older-save regression cases; full
Windows Wrapper .\mvnw.cmd clean verify passed1,411 cases (1,409 unit/two packaged
integration), zero failures/errors/skips,8:05 at11:37:25 ET. No real server database
was modified. Source/tests and migration diff reviewed.

This backend accepts only trusted caller-established identity/authority/evidence.
It is not exposed to players and does not implement sign claiming, pen persistence,
land-claim permissions, player breeding capture, explicit transfers or adoption.
A future donation must use a distinct authorized/audited transition, not first
registration. Live adapter still needs main-thread snapshots, safe 3D classification,
continuous observation and disposable native verification before activation.

The initial domain batch below describes schema73 at that earlier commit; the
verified current artifact upgrades to74 when deployed; no server deployment occurred.

## Approved interaction direction

Free-range eligible animals are adopted automatically after reliable observation.
There is no per-animal player authorization requirement. Existing unknown pens
stay protected. Proposed `[Civ Protect]` / `[Civ Livestock]` signs request a bounded
pen preview; only explicit authorized confirmation establishes a claim/donation.
Exact sign syntax and menu are not yet implemented. Show particles, area and eligible
animal count before accepting; a closed courtyard can look exactly like a pen.

Recorded owners/delegates or a compatible land-claim manager may authorize known
ownership; admin resolution is required for unknown older ownership. Placement or
proximity alone grants no authority. Names, leashes and recorded player breeding
prevent automatic adoption. Village-pen designation covers eligible animals and
future offspring, not an individually player-owned animal. Saved ownership survives
escape, opened gates and missing signs. Ownership changes must be explicit/audited.

## Implemented plain-Java components

* `PenScan`: incremental deterministic four-neighbor horizontal flood fill over
  caller-certified columns. At most the supplied source-read budget per advance.
  Prototype engineering limits: radius32, maximum1024 interior columns; caller can
  select validated limits. Open terrain reaches the bounds and is refused, never
  treated as the rest of the world belonging to a fence. Unknown terrain rejects
  incomplete footprints. Invalid seeds, coordinate overflow and area caps refuse.
  Rejected results expose no usable partial footprint. Accepted sets are immutable.
* `PenClaimPreview`: binds complete geometry and animal IDs to one actor, permanent
  world UUID, standing level and expiration. Confirmation requires fresh identical
  footprint/membership and a fresh authority check. Changed evidence/animals,
  another player/world/level, expired preview or revoked authority are refused.
  A READY result is not a saved claim; persistence still needs its own transaction.
* `LivestockAdoption`: read-only reason-coded eligibility. Recorded ownership wins
  over location. Protected/unknown pens, named/leashed/player-bred unclaimed animals
  cannot be adopted automatically. Incomplete evidence waits; free range requires
  the supplied observation duration. Already-village animals are not adopted twice.
* `LivestockWork`: deterministic prototype work using actual adults, feed, labor,
  storage and a requested slaughter amount. Renewable output consumes real feed
  and work without killing animals; slaughter consumes work and reduces adults
  subject to a protected breeding reserve and per-period cap. Full storage/labor
  shortage cannot generate output. No consumer, money, spoilage or breeding invented.
  Renewable work has prototype priority over slaughter; no live rule/config is wired.

## Required adapter contract and remaining work

Source must be a stable captured view, not unsafe async live Bukkit reads. An
INTERIOR column certifies suitable footing/headroom at the chosen animal level;
BARRIER certifies a fence/wall/gate or adequate solid building edge. Gates are
structural barriers even while open. Known vertical escape is OPEN; ambiguous
heights, ramps, doors, diagonals or unloaded chunks are UNKNOWN until safely
classified. This scanner alone cannot prove a three-dimensional pen, attachment of
its sign, block origins, actual ownership or land-claim permission. Adapter must
check those before preview and recheck on confirmation. Do not force-load chunks.

No Paper listener/menu/particles, block classifier, config section, persistent
pen footprint records and live animal capture, continuous observation tracker, land-plugin integration,
herd juvenile cohorts/breeding, startup or live livestock production are wired.
The initial domain batch changed no animals or databases and retained schema73.
The subsequent additive schema74 batch above is temporary-DB tested only; worlds stay untouched.
Neither player pen construction nor a sign silently donates animals.

## Verification

26 added domain tests:20 geometry/preview/eligibility and six livestock-work tests.
Includes a walkway between buildings, closed courtyard, hole/vertical escape,
unknown chunk edge, caps/overflow, exact tick-read budgets, immutable previews,
changed membership/permission/identity/expiry, escaped ownership and uncertain pens.
The livestock-work sweep runs13,122 input combinations (9 adults ×9 feed ×9 labor ×18 space), checking
animal/feed/work/storage conservation and breeding/slaughter bounds. Extreme longs
also checked. Focused26 passed; full Windows Wrapper clean verify passed1,404
cases (1,402 unit/two packaged integration), zero failures/errors/skips,9:13
at11:24:55 ET. No native Paper smoke is needed for this unwired pure domain batch;
the future adapter requires disposable Paper checks before enabling adoption.
