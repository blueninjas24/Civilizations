# Automatic paid family-home construction

Implemented prototype: `PaperAutomaticConstruction` coordinates the existing
saved home queue, supplier payments, earned cargo/work, guarded native writer and
Civ completion. It does not introduce another money account, timer for labor,
schema, resource source or instant build command. Save schema remains116.

## Opt in and pause

New construction.yml files include the following enabled. Existing installed
files are never overwritten; absence of this entire section means disabled.
Stop the server, back up, add/edit settings, then restart. Automatic site planning
has its own opt-in section; neither switch replaces funding/labor/supply settings.

```yaml
automatic-construction:
  enabled: true
  cells-per-tick: 128
  review-seconds: 10
  block-interval-ms: 500
```

Strict bounds: survey cells16..256, review1..600 seconds, native attempt interval
0..60000 milliseconds. Unknown/missing keys or mistyped values refuse the
construction configuration. New installs default true under the approved automatic
growth direction; an old installation does not gain physical writes silently.
Disable this runtime to stop its future purchases/native steps. Accepted owner
saves drain; earned saved deliveries and existing housing keep their own behavior.

## Current scope

Only formal active household-bound home proposals enter the automatic queue.
Arbitrary paid drafts, paths and private premises do not. Suitable existing homes
still come first; unknown housing evidence is not homelessness. Existing adequate
homes remain. A four-bed starter plan cannot fit every family. There is no clearing,
terrain grading, new road, chest access, property takeover or extra people.

One shared runtime reads at most8 saved jobs at a time. Each project gets one
economic/native action per pass, in saved UUID order; subsequent pages retain the
cursor. Local refusals preserve later projects. Successful passes rotate sooner;
quiescent passes wait the configured interval. Only one survey and owner operation
are outstanding. SQL futures are never waited on by the main thread.

WAIT_MATERIALS needs a complete matching two-pass loaded scene before atomic
whole-remaining-input procurement. Real qualified local suppliers must own the
goods; food/upkeep reserves, actual cash, spending and shared haul still apply.
Paid incoming inputs are not bought again. Delivery/eligible labor come from the
existing active-world owner loops. WAIT_LABOR never invents effort or forces jobs.

READY/RESUME performs another two-pass survey, saves the exact paid intent through
the SQL owner, then tries the current guarded native writer on a separate tick.
The final atomic native scene check has its own256-read cap; plans requiring more
than255 cells wait. One native attempt per runtime invocation and the global
minimum interval apply. Transient owner contention can renew the same exact
intent at most twice within the original5-second evidence lifetime. Every attempt
rechecks the actual world and current owner generation. Renewal does not recreate
consumed first-write proof. After a write or exact existing output, acknowledgement
must finish before another project action begins.

COMPLETE requires fresh native evidence and the existing atomic village-owned
completion. Stable versioned operation IDs make retries exact. The regular housing
runtime subsequently inspects actual paired accessible covered beds and assigns
whole saved families. Completion itself creates no capacity. Player donation,
reclamation and renovation controls cannot claim village-built homes.

## Holds and recovery

Missing worlds/chunks, changed land protection, occupied/altered cells, road loss,
insufficient suppliers/funds/hauling, unknown prices, changed family need and lack
of real labor wait with diagnostics. No chunks are loaded for work. Read:

- `/civ debug construction`: delivery, planning and latest automatic work result.
- `/civ build projects <village UUID>`: retained family/project history.
- `/civ build funding <project UUID>`: material and protected-funds accounting.

Existing recovery preserves exact output after restart. PREPARED plus air after
restart remains ambiguous and cannot authorize a free replacement; changed native
work is not overwritten. Started-work repair/force-resume is not exposed. Keep
the save and project ID for manual review. Unused cancellation remains explicitly
confirmed and refuses paid/started work. No automatic recall/refund/demolition.

The ordinary runtime uses wall time shared with PopulationSession. Accelerated
fixtures must inject the same clock into both; a future/stale timestamp cannot
become trusted evidence merely to make a test pass.

## Verification

Focused Wrapper suites passed:37 unit+3 packaged, then57+3 for wiring/payments/
returns, then27+3 for scoped queues/paced retries/diagnostics; the final clock
overload repeated27+3 successfully. No current-head full-suite claim.

AutomaticHomeCompletionProbe compiled against the final packaged JAR and passed
on pinned Paper26.2build129, in a new marked Host Temp loopback fixture with no
players. It left purchases/work/delivery to the actual configured scheduler and
owner loops, built all120 rotated/mirrored native home blocks, registered village
ownership and released the site. Actual two-pass dwelling inspection assigned
one saved family and four qualified beds. A real session restart and exact
completion replay preserved money, finite goods, people and capacity. Three real
orders,120 journal rows,1 Civ building,1 assignment,4 claims. Captured cells were
restored; both116 DBs integrity ok/FK errors0; server stopped/helper archived.

The initial accelerated fixture correctly refused mismatched real/virtual time
and spent nothing; aligning the injected clock fixed the fixture. No safety rule
was weakened. Actual Minecraft server/world/database untouched. Human survival
and compatibility playtesting, broader physical growth and explicit started-work
repair remain. See the living handbook Chapter31 for plain-English operation.
