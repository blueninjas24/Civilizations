# Saved workplace operation checks

October 12 approved rule: confirmed removal pauses a newly marker-founded firm's
production and new buying/selling until the required marker returns at its exact
saved world/position. Tables are blacksmith markers; barrels are warehouse markers.
This recognizes a workplace, not ownership of player blocks or container contents.

## Configuration and evidence

`economy.yml`:

```yaml
workplace-operations:
  pause-on-missing: true
```

Restart after editing. False bypasses the pause, preserving the same fixed anchor
and transition history; changing back restores enforcement of saved evidence.
Missing sections default true. Legacy firms have no attachments and are unaffected.

PaperWorkplaceOperations requests deterministic SQL-owner pages of up to 64 anchors.
On the main server thread it reads only actually loaded, in-height blocks, without
loading/ticketing chunks, changing blocks or reading inventories. Unknown locations
produce no confirmation. Observations must be from a currently present world, no
more than 60 seconds old and not in the future. Older evidence is ignored; equal
capture times with conflicting contents fail transactionally. Normal unloaded-world
simulation uses the last confirmed state; downtime earns no work. Checks are periodic,
so a removal pauses when the scheduled point check confirms it, not synchronously
with a block event. A distant unloaded removal is discovered on a later loaded check.

Do not infer negative evidence from a general chunk marker scan: its vertical range
may omit the saved anchor. Restoration means the expected block type at the fixed
anchor, not a nearby substitute. Only availability changes; no family/worker firing,
site move, new grant, free supplies, invented ownership or reopening a closed firm.

## Persistence and startup composition

Schema61 adds business_workplace_anchors and workplace_operation_history, initially
empty, plus one operation-setting row. Historical firms/sites/assets stay unchanged.
Migration is versioned/transactional; failed upgrades can retry without asset loss.
Fresh trusted marker workshop/warehouse startup composes anchor attachment with the
actual existing grant/founding/job/storage transaction. Workshops also pay for real
starter supplies. Late attachment failure rolls all those effects back. Accepted
startup replay does not reset marker state, refill cargo or bind a legacy firm.

BusinessOperations and the direct IndustryRepository production/purchase seams
check this pause. Already paid cargo can still arrive in a present world, converting
reserved space to actual stock; no new purchase/production occurs while paused.
No productive receipt means no work experience; ownership/resale/history remain.

## Verification

Tests use isolated JUnit temporary SQLite databases: removal/restoration, wrong kind
or position, restart, absent/unknown world, stale/future/conflicting captures,
whole-page rollback, paid cargo, direct buyer/seller guards, zero paused productive
learning, actual SQL-owner API, admin override, legacy preservation and schema61
failure/retry. Native WorkplaceOperationProbe uses one temporary air block only in
the explicitly marked disposable localhost server, then restores it and releases
its test chunk ticket; production does neither operation. Full results are recorded
in PROJECT_STATUS and DEVLOG after verification.

Automatic first-founder selection/training and investment scheduling remain separate
roadmap work. These trusted persistence/adapter seams do not claim a fresh ordinary
village will already found a workshop or warehouse automatically.

Final verification: ` .\mvnw.cmd clean verify` passed 1,067 tests (1,065 unit +
2 packaged integration), zero failures/errors/skips, 5:25. Native scheduled point
check passed and temporary block restored; stopped with zero forced chunks.
96/103 original fixture tables unchanged; remaining changes only ordinary clocks,
tracking, active-world masks, resident age and farm observation time. Schema61
main-save attachment/history tables empty; integrity/FK healthy; properties restored.

October12 investment update: qualified-owner preparation and rechecked paid
acceptance are described in WORKSHOP_INVESTMENT.md. Automatic first-owner training
and the normal server startup scheduler remain unfinished.
