# Saved resource and workforce review

Use `/civ debug resources` with civilizations.debug permission to read saved trust
and resource counts. Mineral coordinates include retained exhausted history; counts
are not a fresh survey. Placement/removal exclusions are deduplicated. At most32
world/chunk/resource scopes are listed, with the exact number omitted. World status
comes only from the supplied loaded-world set; absence is never source loss.

Use `/civ debug workforce <settlement-uuid>` to see operating firms, vacancies,
qualified idle candidates, and known business funds. Counts can overlap: the same
resident can qualify for several jobs and can still hold only one worker job.
Missing cash records are unknown. The report selects nobody and makes no wage,
closure, funding, training or hiring changes. Current bounded hiring still follows
its existing page order. Large/unknown reviews fail explicitly without showing a
partial comparison as complete. Limits:32 firms,512 idle candidates,512 saved
worker assignments and bounded profile/requirement rows.

Commands require admin debug permission. JDBC reads stay on the SQL owner and
messages return on Paper's main thread. No block/entity access, chunk loading,
resource refill, protection removal, migration or trust reset occurs. Tests compare
all persisted rows before/after reporting. Actual native command delivery passed;
see PROJECT_STATUS.md and DEVLOG.md for evidence and known boundaries.

The user approved explicit admin acceptance plus a complete current scan, accepting
that snapshots cannot reconstruct every edit during a tracking gap. The reports
above remain read-only. Recovery is a separate explicit command:

- `/civ debug recover-ore list` lists up to32 saved deposit IDs.
- `/civ debug recover-ore review <deposit-uuid>` scans every saved coordinate and
  previews its reserve and confirmed loss. All source chunks must remain loaded.
- `/civ debug recover-ore confirm <token>` accepts the stated historical uncertainty
  for that deposit, rescans and applies only if evidence, saved balances and current
  capture are unchanged. The token is bound to that admin and expires in120 seconds.

This restores existing mineral work only. Missing/replaced or protected coordinates
lose only their remaining shares. Previously earned goods, money, identities,
exclusions, exhausted coordinates and history stay recorded; no reserves refill.
Old coordinate evidence without shares adopts only its existing balance under the
already-approved even-share rule. Abstract deposits with no coordinates cannot be
reviewed. Nothing is inferred from unloaded chunks or absent worlds.

Schema95 adds initially empty continuity and mineral recovery audit tables. A clean
restart retains a scoped approval; an unclean stop or later capture gap invalidates
it. The global trust fence remains false, keeping unreviewed resources and new
discovery/founding paused. Unsupported editors must be removed or supported capture
restored before a review; this command does not certify editor compatibility.

Budgets:4096 source coordinates per deposit,128 live block reads per tick, four
concurrent scans,16 in-flight/prepared reviews; scans expire after30 seconds. Oversize
or incomplete resources fail explicitly. Current material families: coal, iron,
stone/deepslate and sand/red sand, matching existing discovery. No chunk loads,
world edits, drops or ownership claims occur.

Forest recovery is now available through `/civ debug recover-forest list`,
`review <site-uuid>` and `confirm <token>`, with the same explicit acceptance and
admin-bound120-second confirmation. It compares complete loaded snapshots covering
all original supports and the configured natural-canopy geometry. Excluded/retired
trunks and protected canopy cannot become positive evidence. Missing/replaced
trunks get permanent source-loss records; canopy-only damage reduces habitat without
inventing trunk deaths. Both capacity and available timber are clamped at or below
their saved values during recovery. Subsequent ordinary productive regrowth stays
bounded by saved habitat; approval itself adds no timber. Earned stock is untouched.

Forest budgets:512 original supports, at most9 already-loaded chunk snapshots,
200,000 sampled cells and4,096 scan steps per tick, two concurrent scans,16 total
prepared/scanning reviews. Unknown geometry, a tree extending beyond the scan bounds,
an unloaded margin, expired evidence or changed capture refuses approval entirely.
Schema96 adds one empty forest_recoveries audit table; existing resources/history
are preserved. Current capture, per-site approval and the global discovery fence
are separate. Clean shutdown retains approvals; later gaps/crashes revoke them.

Focused temporary-SQLite checks and actual Paper review/confirm passed October18;
see PROJECT_STATUS/DEVLOG for exact evidence and fixture-only limitations. Cross-firm
ranking, worker transfers and living-owner closure remain separate design work.
