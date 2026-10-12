# Connected industry endurance check

The temporary SQLite scenario
`FarmToolsRuntimeTest.repeatedMineWarehouseSmithAndFarmChainConservesCashGoodsAndExhaustsFiniteSource`
runs the normal PopulationSession for 50 economic periods. It complements the
disposable Paper checks of individual paid chains.

The fixture has four explicit residents, a staffed farm, blacksmith, warehouse
and finite iron mine. One retained manufactured toolkit makes an actual paid
startup sale; the other two existing fixture kits are explicitly consumed for
prior maintenance. Real warehouse capital comes out of the village treasury.
The mine has ten finite extraction batches, each yielding one iron and one stone.
A simple fixture recipe consumes one iron to make one toolkit. These controlled
test numbers are not changes to the server's default balance.

The ordinary industry runtime extracts finite goods, pays for warehouse inputs,
delivers them and lets the staffed blacksmith buy inputs and manufacture tools.
Productive public farm work drives genuine replacement demand. Approved supplier
contributions move only actual earned spare cash toward protected farm invoices;
they create no money. Existing local supplier selection may serve the farm from
a warehouse or directly from its blacksmith.

The scenario verifies repeated actual farm payments, real upkeep contributions,
source exhaustion and preservation of all ten stone byproducts. Village and
business balances still total the fixture's original 30,000 hundredths. Exactly
eleven toolkits remain accounted for across actual farm use, owned inventory and
paid incoming cargo: one retained starting kit plus ten finitely produced kits.
Repeated pulses preserve the result. A later restart with the world absent makes
no new farm purchase and does not refill the exhausted source. SQLite integrity
and foreign keys are checked.

This is a connected operation/depletion regression. Businesses and resource
evidence are seeded explicitly; it does not certify automatic first founding,
natural-village startup, economy tuning, wider resource catalogs, physical growth,
emergent specialization or the required multi-year acceptance scenarios.
No application behavior, configuration or migration is changed by this batch.

Verified October 11, 2026: focused scenario passed; final Wrapper clean verify
passed 1,028 tests (1,026 unit + 2 integration), zero failures/errors/skips, 5:11.
The prior native Paper check passed for the unchanged production code; the
50-period extension itself ran in temporary SQLite through normal SQL owners.
