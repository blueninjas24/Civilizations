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

Recovery policy is pending. Current snapshots cannot reconstruct every edit during
a tracking gap. The user has been asked whether explicit admin approval plus a
complete current scan can restore reviewed existing resources, or whether trusted
backup/history evidence is required. Until answered, these reports apply nothing.
Neither option authorizes refilling reserves, clearing exclusions, changing owner
identity, replacing history or certifying unreviewed areas. Cross-firm ranking,
worker transfers and living-owner closure rules remain separate design work.
