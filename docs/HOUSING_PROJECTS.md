# Household-bound housing proposals

Schema113 adds a preparation API, not an automatic builder or a new player command.
Each frozen HOME draft may identify one whole household, its permanent village/world,
plan fingerprint, living residents, previous known capacity and creation time.
The same household cannot hold two active proposals. Only its own proposal makes
it wait; another family can still receive the next planner recommendation.

The SQL owner rechecks the actual saved calendar and current vacancy-first planner.
Unknown capacity is not homelessness. Stale/unreviewed permitted buildings hold
new construction; suitable vacancies must be reused. A plan must fit the entire
family. Historical active HOME sites without beneficiary records conservatively
hold all new proposals rather than guess where their funds belong. No old records
are rewritten. An existing project also follows its household's current village
when deciding whether that family already has a proposal in the same world.

Bind a new dry draft before its site, materials or native work. The operation
does not claim land, spend money, reserve materials, allocate labor, assign a home
or increase population capacity. Present-world, current site/protection and actual
resource checks remain separate prerequisites at subsequent boundaries.

Unused cancellation retains the frozen draft and binding history. Release its
unused site and return all owned material receipts first. Any paid-supply order
or prepared block requires a future reviewed recovery path; nothing is silently
deleted or refunded. Exact cancellation retries return the saved receipt; an old
binding retry never reactivates it. Cancelled drafts cannot reserve another site
or receive new material authority. A new proposal needs a new identity and review.

Atomic preparation now combines actual saved need and vacancy-first planning with
buffered road-first alternatives, current saved exclusions, a frozen draft and
its family/site reservations. All three records commit or roll back together.
The bounded search envelope conveys no native write permission; evidence outside
the SQL-reviewed envelope is ignored. Historical preparation receipts never
refresh site evidence. Templates, need, funds and native safety must still be
reviewed before any subsequent work. Focused pipeline checks67 +3 passed56.954s.

Before new commitments, an unstarted proposal rechecks the family's current
village, living members, known housing and whole-family plan fit. Rehousing or
unknown/changed need pauses new site/material authority, public effort and the
first economic block intent. Already paid stock and history remain; normal checks
can resume when need returns. Durable native intents retain existing recovery/
completion because a lost acknowledgement could mean world blocks exist already.
No reassignment, deletion or free refund is performed. Wrapper56 +3 passed55.343s.

Read-only project status now reports up to8 retained projects, active first,
with plan/family/site identity, paid orders, durable physical intents, current
need pauses and unused-cancellation eligibility. More history is retained, not
discarded. Atomic unused retirement releases site and household reservation
together; failure rolls both back. Paid supplies, native intents and unreturned
owned materials hold for reviewed recovery. Exact cancellation receipts never
reactivate work. Wrapper50 +3 passed51.789s. Live operator controls now connect these SQL-owner APIs, as described below.

Current limitations: no physical production writer,
completed-home activation, started-project cancellation or migration of historical
unbound projects. Completed-home lifecycle remains part of the construction gate.
The village retains ownership of future publicly funded homes under the approved
prototype rule. This record is a beneficiary reservation, not private title.

Verification: four temporary-SQLite cases in DwellingOccupancyTest cover independent
families, restart/history, reuse/unknown/stale/absent-world holds, cancelled-draft
land refusal, injected SQL failure and preserving112->113 migration. Focused Wrapper
clean verify passed55 unit and3 packaged checks in48.957s. No actual server database
or Minecraft world is used by these tests. Older plugin versions reject schema113;
back up before upgrading and do not downgrade a live save in place.


## Operator controls

Use `/civ build projects <village UUID>` in the saved world for current retained
project status. `civilizations.construction.preview` defaults to operators and
permits this read only. Use the full saved ID from `/civ debug villages`.

`/civ build cancelproject <project UUID>` inspects only; if the project qualifies,
read its frozen family/project and click the separate confirmation within60 seconds.
`/civ build confirmcancel <token>` submits the atomic unused cancellation.
`/civ build cancelrequest` discards an unsubmitted request. Accepted owner saves
finish even after disconnect/shutdown; an exact live token retry retains identity
and submitted time after an unexpected acknowledgement failure.

Cancellation requires `civilizations.construction.manage` (operator default), not
merely read permission. Confirmations are actor/world-bound and checked again.
Four actors may hold requests/previews; read checks expire after30 seconds.
Unknown/changed terms, paid supply, unreturned owned materials or prepared physical
work never become an implicit refund or deletion. No command creates a building
project or enables the production block writer. Detailed user instructions are in
handbook0.14 Chapter28. Focused35 +3/final5 +3 passed; actual disposable Paper
validated routed reports and management permission, not a full live cancellation.


## Automatic planning work feed

The SQL-owner feed is read-only, pages at most8 saved villages, skips absent
worlds and villages at the active-home proposal cap, and selects one verified
whole-family need per village. Unknown/stale capacity or permitted vacancy reuse
holds new-home work. Deterministic proposal identity includes permanent world,
household, calendar month and retained proposal count. Final limited preparation
checks the active-home cap again inside its transaction, preserving exact replay.
The feed alone is read-only; its native scheduler is now connected as described below. Wrapper40
unit +3 packaged checks passed39.566s; schema113 is unchanged.


## Live automatic site preparation

The feed now runs through PaperAutomaticHomePlanning and PaperHomeSiteSurvey.
Only one survey advances under the shared automatic-planning budget, and one
owner save may be pending. Each loaded cell is checked twice; any missing chunk,
absent world, stale or changed evidence holds. The source never loads chunks or
adds chunk tickets. Saved protections and actual needs are checked again on the
SQL owner before the atomic draft/family/site save. Existing dirt paths and flat
supported clear sites only; no new paths, digging, grading, purchases or writing.

New construction.yml files have automatic-home-planning enabled with defaults
128 cells-per-tick,30 review-seconds,1 maximum-active-homes and12 search-radius.
Existing files stay unchanged and explicitly opt in with the complete section.
Strict bounds are64..256,10..600,1..16 and8..12 respectively. Six vertical layers
near the village centre keep native reads bounded; larger/uneven sites may wait.
Debug construction reports the latest cause. See handbook0.15 Chapter29.

Wrapper54 +3 passed39.603s. Actual disposable Paper full automatic native survey,
real capped SQL proposal, confirmed operator unused cancellation and restart
passed; missing-world/changed-pass evidence held. One path marker and one test
air cell restored; both databases113/ok/FKs0. Only test-fixture setup explicitly
prepared4 chunks in its new guarded host-temporary world; production simulation
never loads chunks. Native physical building/material purchases remain unfinished.
