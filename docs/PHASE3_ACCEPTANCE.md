# Phase 3 acceptance evidence

This is the initial industry/business prototype gate. It does not mean final
survival balance, universal editor compatibility or Phase4 construction is finished.
The current build result and milestone decision are in PROJECT_STATUS.md.

## Saved simulation scenarios

| Scenario | What runs and what it establishes |
|---|---|
| ResourceGeographyScenarioTest | Identical seed, residents and funds with coal, stone or neither; actual paid startup differs. Eighty industry periods respect capacity, finite sources, paid customers, world absence and restart. |
| IndustryHistoryScenarioTest | Two ten-year demographic histories: qualified family inheritance versus unqualified/unfunded retained closure, using ordinary aging and mortality. |
| IntegratedIndustryAcceptanceTest | Ten calendar years through PopulationSession: genuine historical food deficits, finite extraction, ordinary elder death, a qualified well-funded same-world branch buyer, local assets, absence and midpoint restart. Every pulse must report no population failure; total cash and sale receipts are checked. |
| CrossVillageBusinessTest | Actual branch payment, one primary job, local workers/stock, different-world and disabled-scope refusal, failed-save rollback, replay and qualified family succession. |
| FarmToolsRuntimeTest connected scenario | Fifty economic periods exhaust a finite source through the paid mine/warehouse/blacksmith/farm-tool chain; goods and real upkeep contributions balance. |

Initial people, skill history, capital and resource observations are declared test
fixtures. No later cash, made-up purchases or free goods are injected to guarantee
success. The wealthy branch buyer represents an exceptional starting history;
ordinary/unfunded histories retain their separately tested failure outcomes. These
scenarios demonstrate conditional emergence, not a promised frequency of rich firms.
No configured government/monopoly system is added in P3.

## Actual Paper verification

Paper26.2 build129, Java25, fresh marked localhost fixtures with no players, outside
OneDrive. Each helper uses a separate SQLite save; no human server/world is opened.
Helpers are compiled separately and never bundled in the Civilizations plugin.

`PhaseThreeAcceptanceProbe` combines eight saved businesses: finite coal mine and
stone quarry, renewable logging and fishing, two warehouses, one general craftsman
and one bakery. Real supplied grain becomes bakery food; real resource goods are
bought locally; fitting production serves an actual crowded business and installs
one paid storage upgrade. Cash, capacity, earned receipts, absence and restart are
checked together. Abstract work does not edit or claim world blocks. The final
variant explicitly checks all eight workplace chunks remain unloaded throughout.

The first40-pulse mixed run measured average244ms and maximum847ms wall time per
awaited database-owner pulse at an accelerated one-second cadence. This includes
SQL/queue waiting and is not Paper main-thread time. Production defaults remain
30 seconds per industry period, with an eight-business pulse budget. It is a small
development workload measurement, not a20-village scale or latency guarantee.
The final unloaded run measured average323ms and maximum1203ms while the full Maven
regression build was running concurrently. These are owner-pulse wall times, not
server tick times. No performance optimization or production cadence was changed.

`WorldEditResourceCaptureProbe` verifies ordinary pinned-editor startup, older
terrain quarantine, actual fresh chunk generation, raw placements, block-specific
ore retirement, undo exclusions, unsafe-command/off-thread fences and shutdown.
All five temporary AIR blocks are restored and the held ticket is released.
Earlier terrain and unsupported editor operations retain their explicit fences;
see WORLDEDIT_CAPTURE.md for the conservative compatibility boundary.

All inspected fixture databases remain schema99 with clean SQLite integrity and
foreign keys. No new migration is required for this completion batch. No resource
balance is reset and no server save is deleted. Disposable servers stop normally;
helper JARs are archived outside their plugins folders after verification.

## What still needs a person

PHASE3_PLAYTEST.md retains a human-client checklist for this server's plugin mix,
inventory feel, village behavior and balance. Native/automated checks do not claim
that a human survival session has been completed. Physical expansion, visible mine
excavation, buildings and furniture projects are Phase4; inter-village goods routes
are Phase5. The implemented initial catalogue and documented compatibility limits
remain explicit rather than implying every vanilla product or industry exists.
