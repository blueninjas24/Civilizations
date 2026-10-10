# Live read-only resource surveys

PaperOreObserver is registered with the plugin and observes already-loaded chunks
around each town's canonical lowest saved-home coordinate. Bounded center-first
rings extend the observation footprint, not the settlement boundary or rights
to extract. This is not an omniscient underground search. No chunk is force-loaded.
Snapshots are captured on the main thread and traversed over bounded ticks.
The current catalog remains exposed normal/deepslate coal and iron.

The optional ore-observer section in economy.yml sets enabled, vertical-radius,
cells-per-tick, interval-ticks and horizontal-radius-chunks (0–2). New-file defaults:
radius one chunk (nine targets maximum), enabled, 24 blocks vertically
around the home, 512 cells per tick and 400 ticks between observation rounds.
Missing old-file sections stay disabled, with no automatic owner-file rewrite.
An old enabled section missing only the new horizontal setting retains its previous
home-chunk-only scope. Radius zero uses one target; radius two uses at most 25.
Malformed/partial settings fail before database startup. Maxima cap input budgets;
these settings do not change existing deposits, extraction yields or prices.

Only one scan and one SQL future are pending. The queue is bounded at 1024 towns,
each with at most 25 chunk targets. Unloaded targets are skipped, not loaded;
each survey is capped at 512 combined positive coal/iron coordinates. Unloading
before traversal finishes, unreliable border evidence, scan overflow, capture gaps and failed
owner validation discard/defer evidence. Unloading is not resource depletion.
Unknown is never converted to zero by a partial scan. No placement exclusion is
assumed from a stale snapshot: the SQL owner rechecks current sticky exclusions.
Evidence retains the timestamp sampled just before snapshot capture, even when
traversal spans many ticks. Completion does not make an old snapshot fresh.
The previous completed result remains visible alongside ongoing scan status,
with its town, chunk and capture time; another target cannot erase its explanation.
Nearby chunks remain independent complete observations; they are not silently stitched
into one village-wide or cross-border deposit. Automatic founding still needs
freshness, funded need, suitable sites and actual eligibility revalidation.

PopulationSession.surveyMinerals passes immutable data to MineralSurveyReader.
The owner requires healthy live tracking, a present world and matching saved town
identity. The reader validates complete matching coal/iron evidence, shared timestamp,
coordinate uniqueness and bounded size, filters saved mutation exclusions, then
groups actual admitted patches. Existing deposit identity is resolved per coordinate;
multiple deposits or changed mineral identity report conflict. Separated patches
can still link to the same saved deposit. No old reserve is refilled or split.

This batch reports provisional patch counts in /civ debug observer diagnostics.
It creates no businesses, extraction terms, stocks, funds or saved opportunities.
Returned patches are transient, not durable identities or a promise of feasible
industry. Actual founding must revalidate coordinates, site suitability, labor,
funding, demand and observation freshness transactionally. Bulk-editor provenance
and automatic nonfood chains remain separate unfinished work. No world ownership
or autonomous block edits are granted by these observations. Physical mining is
Phase 4. Server downtime and absent worlds still pause civilization normally.

Temporary database/configuration/session tests cover excluded bridges, separate
patches sharing one depleted identity, conflicting deposits/minerals, wrong worlds,
partial/mixed/oversized inputs, capture gaps, owner filtering, absent-world return
and strict editable settings. Native LiveOreSurveyProbe is a separate guarded
test-only plugin: synthetic saved homes/people in its own database, four actual
ore/air fixture blocks in an already-loaded chunk, one saved exclusion, and the
production observer's real scheduled snapshot/owner pipeline. It creates no actual
villagers or player inventory changes; blocks are restored on disable. Never ship
or deploy that probe on a real server. Verification results are recorded after runs.

Windows Wrapper clean verify passed 640 cases (638 unit/two packaged integration),
zero failures/errors/skips. Nine new cases cover reader/config/owner-world guards.
The guarded Paper pipeline fixture passed, restoring all four blocks. No actual
villagers were created; the separate probe DB retained two synthetic residents,
one exclusion, no businesses, and a clean/trusted tracking fence. Main startup
retained month 1676; online reached 1677. Read-only main industry/funds/exclusions,
both databases' integrity/FKs and unchanged owner economy.yml passed. Test loading
ticket removed, probe archived outside plugins, server stopped normally with exit 0.
Schema remains 34. No natural-world or human-client mine acceptance is claimed.
