# Supported WorldEdit resource capture

The narrow adapter targets WorldEdit Bukkit `7.4.4+7546-f9e033f`, without FAWE.
WorldEdit remains optional and is never bundled in Civilizations. Other versions,
disabled/late-enabled editors, VoxelSniper and configured additional editors retain
the conservative resource fence. No editor is prevented from editing.

Normal startup registers the synchronous BEFORE_CHANGE extent hook before the SQL
owner starts. Actual resource writes save placements/removals, retain exclusions
through undo, and retire only the affected ore block's remaining share. Pending
saves block resource spending; failures, ambiguous outcomes, unloaded-chunk writes
and off-thread calls fence both resource kinds immediately.

WorldEdit can process queued chunk deletions in its own onLoad, before Civilizations
can observe them. Therefore **every supported-editor startup quarantines all earlier
resource provenance**. It revokes earlier capture certificates while preserving
their rows, businesses, goods, cash and history. Only positively observed new
generation under this boot's uninterrupted capture can qualify automatically.
Loading old terrain never certifies it. Existing explicit reviewed recovery remains
available. A clean editor restart does not promise automatic continuity for an old
mine or forest; safety takes priority over silently assuming its blocks survived.

The reviewed command scope is `//set` and `//undo`, plus selection/read-only commands.
WorldEdit's command bus and Bukkit dispatch guard fence unreviewed commands before
execution, including regeneration, chunk deletion, scripts and paste. Bound brushes,
tools and super-pickaxe interactions are conservatively outside this scope and
also fence capture. Restart/review is required after a gap. Shutdown detaches the
hook before closing the database owner; disabling/re-enabling WorldEdit is not a
supported continuity path.

This does not certify arbitrary third-party plugin writes, renamed editors,
offline modifications, hot reload, asynchronous editors or all WorldEdit features.
Admins must list other known bulk writers in the existing provenance settings.
It is a bounded compatibility adapter, not an ownership or plugin security system.

Verification: GeneratedTerrainSessionTest covers bootstrap/restart certificate
revocation and fresh-only capture; WorldEditCapturePolicyTest and OreEditorGuardTest
cover exact version, command scope and conservative exemptions. The separate
WorldEditResourceCaptureProbe exercises the actual pinned editor/Paper runtime,
normal startup, raw writes, removal, undo and failure fences. Final run results are
recorded in PROJECT_STATUS.md and DEVLOG.md; this document alone is not a test result.
