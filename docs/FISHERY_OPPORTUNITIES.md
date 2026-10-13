# First-fishery opportunity review

FisheryOpportunities is a bounded, read-only SQL-owner planning API. It does not
train residents, found firms, take property rights, open resources, reserve money
or change world blocks. Automatic scheduler/founder/fresh habitat-and-shoreline
acceptance remain unfinished. Existing saved fishery identity, including closed
firms, prevents this first-firm bootstrap from bypassing succession/reuse/competition.

A candidate needs enabled timed industry, fishing and water recognition; storage
that can hold food; a current complete population/food assessment; protected shared
startup grant eligibility; and actual affordable food demand AFTER its proposed
startup/capital debit. demandAfterSpending is a preview, never a posted debit.
Public stock and incoming/reserved food are already counted in that demand. Goods
owned by eligible staffed operating local suppliers then reduce remaining need.
Closed, unstaffed, paused-marker, wrong-world or unqualified suppliers cannot count
as usable stock. More than64 supplier rows causes a conservative defer rather than
an incomplete investment decision. Future production or profits are not assumed.

WaterPatch chooses connected same-level nearby supports from complete, current,
correct-depth, conflict-free and unclaimed water. The entire chosen footprint must
fit the horizontal radius and surface-height range around the saved town center.
Unknown/absent worlds, stale/future/partial evidence and insufficient footprints
produce diagnostics and no investment. A candidate still needs safe dry shoreline
footing and current water proof at acceptance; this review provides neither.

Editable optional economy.yml fishery-opportunities prototype:

- sites-per-review8, maximum-columns64 (maximum256 for bounded shoreline checks).
- evidence-lifetime-millis60000, horizontal-range96, vertical-range48.
- startup-hundredths500, capital-hundredths1000; real shared existing village money.
- capacity256; actual configured commodity storage weights must fit food.

Missing old sections disable the review; installed files are not rewritten. These
are prototype settings, not permanent balance or a commitment to found fisheries.
PopulationSession.currentFisheryOpportunities serializes the review on the SQL owner;
configuration is wired through normal plugin startup but no automatic caller is
scheduled. No migration; schema68 and existing business/history/assets are retained.

Focused122 temporary-SQL/config/owner regression cases passed. Seven new database
cases cover read-only repeat/restart, real demand after startup, exact funding,
current water/range/world/quality, food protection, existing eligible supply,
first-firm identity, footprint terms and SQL-owner absence behavior. One new config
case covers strict editable/default/missing/invalid settings.

Full Windows .\mvnw.cmd clean verify passed1,272 tests (1,270 unit/two packaged
integration), zero failures/errors/skips,6:55 at21:46:00 ET. Guarded native identity
API and reserve-customer regression passed21:46:54; save audited, exact settings
restored and helper archived. Human server unchanged. No automatic founding enabled.
