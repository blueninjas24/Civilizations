# Shoreline recovery planning

Approved October12: an accidental player/creeper break should not kill the industry.
Search for a safe replacement beside the same original saved fishing habitat, or
pursue a feasible protected repair/dock project. Preserve business/site identity,
workers, money, goods, remaining fish and history. Repairs must obey Phase4 funding,
materials/labor, construction and player-build protection; no free block placement.

ShorelineRecovery now provides a bounded plain-Java read-only decision:

- KEEP: current dry footing and two clear standing cells remain safe.
- RELOCATE: current point is confirmed unsafe; choose the nearest confirmed safe
  bank beside the original footprint within caller-supplied horizontal distance.
- WAIT_UNKNOWN: wait for missing evidence; never infer damage from unloaded blocks.
- NEEDS_REPAIR: complete nearby evidence offers no safe replacement. Retain the
  industry and request a future feasible repair/dock; this is not a closure command.

Choices use Manhattan distance including height, then coordinate ordering, so ties
repeat deterministically. The caller supplies rise0–4 and horizontal distance0–256;
these are bounded inputs, not permanent gameplay defaults. Original standing points
must border that original footprint within the opening rise terms. No relocation to
new water, habitat refill, property claim, world mutation or SQL write occurs.

ShorelineScan now exposes immutable per-position SAFE/BLOCKED/UNKNOWN assessments.
Unknown is kept separate even when another cell seems blocked. Scanning remains
bounded by actual cell reads, with at most256 water supports/4096 candidate points.
No incomplete scan produces a decision. Integer-edge coordinates cannot wrap.

Decision.confirmedSafe describes fresh geometry evidence, not immediate permission
to mutate the business. Decision.currentConfirmedUnsafe distinguishes confirmed
damage from missing data. An unknown original point leaves the last verified saved
operational state intact: otherwise healthy offscreen businesses must keep data-only
simulation. A confirmed unsafe original point waits for safe recovery; unknown
alternative banks cannot license repair or close the industry.

This planner is not yet scheduled by operating fisheries. The future caller must
bind permanent world/site identity and saved recognition terms, verify actual
supporting water/freshness, recheck the chosen point before transactional acceptance,
and preserve an audit of relocation. No binding/migration/automatic founding or
repair construction is enabled in this batch; existing legacy fisheries are unchanged.

Seven new headless cases cover damage recovery, safe/unknown originals, incomplete
alternatives, repair/restoration, obstruction, nearest tie ordering, strict budgets,
distance limits, footprint boundaries and integer overflow. Focused12 cases passed
with zero failures/errors/skips,12.766s at00:00:11 ET on October13.

Full Windows .\mvnw.cmd clean verify passed1,287 (1,285 unit/two packaged
integration), zero failures/errors/skips,7:02 at00:07:16 ET October13. Native ordinary
water/SQL-owner probe passed00:08:10:220 independently verified columns,11 safe banks,
read-only damage-mask replacement with independently checked actual footing/headroom,
unknown-mask wait, budgets and absence/restart. It did not break world blocks or
persist a relocation. Save audit passed; exact settings restored, helper archived
and fixture stopped. Human playable server unchanged. No schema migration.
