# Saved ages for existing herd animals

Current project status: approved automatic free-range adoption, funded startup
and paid scheduled feed/output are now connected and verified. See
LIVESTOCK_ADOPTION.md and LIVESTOCK_RUNTIME.md. The earlier scope notes below
describe this age-specific checkpoint; slaughter remains a separate integration.

Both initial herd members and projected newborns use their saved herd period and
permanent maturity period. The bounded positive-sighting adapter reconciles only
actual, living, already-owned loaded cow/chicken UUIDs. It never derives ownership
from a name, tag, location or resemblance; player/unclaimed animals are not paged.
Unknown bodies receive no mutation or inferred death. World absence and disabled
production retain the saved period. No wall-clock animal aging is introduced.

A shared main-thread age reconciler locks native age and selects baby/adult from
saved periods. It leaves names, leashes, AI, gravity, invulnerability, ownership,
stocks and money untouched. The newborn handoff retains its separate temporary
movement release. This also corrects initially adopted young animals that would
otherwise grow using Minecraft's independent timer.

## Ordering and native metadata

The native civilizations:livestock_age_clock string is versioned/canonical:
v1|business-UUID|saved-maturity-period|last-applied-herd-period. It certifies only
the most recent applied view of an already authoritative SQL member, never an
ownership claim. Two observers cannot make an adult young again by applying an
older in-flight view. Same-period checks can repair native drift without rewriting
the clock. Different business/maturity, malformed types, unknown versions or
noncanonical counters are reported for review instead of overwritten.

Age conflicts are isolated: valid positive positions still save and other animals
continue. Diagnostics/logs retain the last age issue and affected animal ID.
Missing herd checkpoints are rejected rather than guessed as period zero.
Schema87 stays unchanged; no migration, historical backfill, data reset or new
goods/cash/animals. The member read adds an indexed latest-period lookup.

## Verification and scope

Added13 temporary-SQLite/Paper age/observer cases; focused52 passed, including
prior newborn/presence regression. Windows .\mvnw.cmd clean verify -B -ntp
passed1,776 tests (1,774 unit/two packaged integration), zero failures/errors/
skips;12:31, finished2026-10-17T10:32:48-04:00. Source/test Java frozen.

Guarded Paper26.2 build129 probe passed10:35:01 EDT: three actual initial cows
corrected from intentionally divergent native ages, initial youngster held at
period8/matured at9, unchanged names/AI/gravity/invulnerability, real native stale
age rejection, and unspawned EntitySnapshot(World) round-trip of age/lock/PDC.
Two real newborns, owner/controller restart, real feed/output, missing-body and
confirmed death accounting remain correct. Not automatic adoption, actual chunk
unload, human client or JVM crash proof. All147 main tables retained/143 exact;
ordinary clock/runtime/capture/ore metadata only, no aging. Schema87/integrity/FKs
unchanged/clean. Fixture stopped/settings restored/helper archived/backups kept.
Only exact private test bodies were removed. Human server untouched.

Automatic adoption/founding, real feed scheduling, safe intentional slaughter and
broader bakery ingredients remain separate work. The slaughter-product question
is pending; this batch assumes no answer and implements no slaughter.
