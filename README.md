# Civilizations

Civilizations is a PaperMC plugin that transforms vanilla Minecraft villages into persistent, evolving settlements.

The long-term goal is villages that grow, build, trade and form regional
civilizations. The current prototype has persistent people, village economies and
working business simulations. Physical construction comes in Phase4; regional
trade and roads come in Phase5.

Phase4 has begun with editable original starter plans and read-only admin site
previews. Normal gameplay still cannot place blocks; guarded disposable checks can. See [construction tools](docs/CONSTRUCTION.md)
and the [P4 checklist](docs/PHASE4_CHECKLIST.md). P3's human survival playtest remains
pending while protected construction foundations proceed separately.

## Player and server guide

Read the [living PDF handbook](output/pdf/Civilizations_Player_and_Server_Guide.pdf)
for a TL DR, first-session walkthrough, feature chapters, command glossary and
administration/troubleshooting. Planned housing, donation and repossession controls
are labelled separately from working features. Its [editable source and update
workflow](docs/guide/README.md) are maintained alongside development.

## What works now

- Villages are recognized automatically from beds and villagers, including suitable
  player-built settlements. Named residents, households and families persist;
  people age, have children, die and migrate. Managed villagers do not become zombies.
- Villages track food, shortages, supplies and money. Players can donate or sell
  needed food, negotiate emerald prices, barter for available timber/stone, and
  buy available village milk/eggs. Bring an empty bucket for milk.
- Observed farms, basic cow/chicken herds, coal/iron mines, stone/sand extraction,
  logging, fishing, warehouses, blacksmiths, bakeries and a general craftsman can
  participate in local production chains. Industry requires suitable observed
  resources, qualified residents, actual supplies, funding and paying customers.
- Businesses own goods and cash, buy inputs, wait for delivery and respect storage
  limits. Real tool upkeep and limited paid storage upgrades create useful demand.
  Minerals are finite; trees and fish have bounded renewable stocks.
- Businesses retain founders, employees and history. Qualified family and employees
  have succession priority; funded buyers can acquire firms, including same-world
  branches when enabled. Failed sales preserve closed sites and assets.

Server downtime and absent worlds pause progress. Loaded-world businesses can work
with their chunks unloaded. The plugin does not yet construct buildings, excavate
mines or fell trees. Toolkits/storage fittings are abstract business goods, rather
than a complete physical sword, armor or furniture shop catalogue.

Start with a separate test world. Recreating a managed villager during movement
can replace its vanilla trades, profession state and inventory. This remains a
prototype that needs survival playtesting and tuning. See the
[Phase3 playtest guide](docs/PHASE3_PLAYTEST.md) for setup, commands, protection and
known limits, and the [acceptance evidence](docs/PHASE3_ACCEPTANCE.md) for checks.

Population settings live in `plugins/Civilizations/config.yml`; the commented
`economy.yml` controls food, resources, recipes, prices, training, grants and upkeep.
Defaults are supplied, including famine/feast tuning examples. Existing files are
preserved, so older installations may need to opt into added sections. See the
[economy tuning guide](docs/ECONOMY_TUNING.md).

## Core Philosophy

- Keep the experience recognizably vanilla Minecraft.
- Settlements should evolve because of actual world conditions.
- Growth should physically change the world.
- Villages should build over time rather than instantly spawning structures.
- Player creations should be respected and protected.
- The simulation should create stories naturally rather than relying on scripted events.

## Full project vision

- Persistent named settlements
- Population, food, wealth, safety, and resource simulation
- Autonomous building construction
- Roads and settlement expansion
- Regional architecture
- Settlement specialization
- Inter-village trade
- Merchant caravans
- Diplomacy and relationships
- Hamlet → Village → Town → City progression

## Development

See the [plain-English progress tracker](docs/PROGRESS.md) for progress through all nine phases.

Platform: PaperMC 26.2  
Language: Java  
Build system: Maven

### Building and testing

Install a JDK 25 and make it available through `JAVA_HOME` or `PATH`.
The Maven Wrapper pins Maven 3.9.11; IntelliJ and a global Maven installation
are not required. The first run needs internet access to download Maven and dependencies.

On Windows, run from the repository root:

```powershell
.\mvnw.cmd clean package
```

On macOS/Linux, use `sh ./mvnw clean package`.
The package build runs the JUnit 5 tests and produces
`target/civilizations-0.0.1-SNAPSHOT.jar`, including SQLite JDBC.
To run only the tests on Windows, use `.\mvnw.cmd test`.

Persistence tests create SQLite files exclusively in JUnit-managed temporary
directories and close their connections before cleanup. They do not start Paper
or access a Minecraft server's plugin data folder.

Use `.\mvnw.cmd clean verify` for full verification, including the packaged-JAR
backup integration test. See [development and recovery procedures](docs/DEVELOPMENT.md)
for CI, a disposable local Paper setup, and consistent SQLite snapshots.
The [headless simulation harness](docs/SIMULATION_HARNESS.md) exercises seeded,
bounded stepping independently of Minecraft; it does not yet simulate population.

Development began September 27, 2026.
