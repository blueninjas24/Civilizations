# Civilizations

Civilizations is a PaperMC plugin that transforms vanilla Minecraft villages into persistent, evolving settlements.

The long-term goal is villages that grow, construct buildings, develop economies,
trade and form interconnected regional civilizations. Construction is planned; the implemented prototype provides persistent people,
population and early food economy accounting.

## Playable population prototype

Villages join automatically when beds and villagers are recognized. Named people,
households and families persist; the calendar drives births, aging, deaths and moves.
Nearby villagers reflect saved residents. Managed villagers cannot convert to zombies,
but still take damage. Administrative diagnostics explain saved population outcomes.

Use a disposable play world: vanilla trades/professions/inventory are not preserved
when a moving person's physical villager is recreated. This is a tested foundation,
not a balanced survival release. See [Phase 1 playtest notes](docs/PHASE1_PLAYTEST.md).

Phase 2 now has tested, persistent commodity accounting: reserves, explicit supplies
and demand, shortage duration and recovery. Live food accounting now uses observed planted fields and saved time; treasury,
household wellbeing and player trade remain pending. See [accounting scope](docs/COMMODITY_ACCOUNTING.md).

Prototype economy tuning is in a separate commented `economy.yml`, created under
`plugins/Civilizations` on first startup. It includes simple famine/feast examples
and strict validation. Live food supports recorded forward tuning on restart; see [tuning guide](docs/ECONOMY_TUNING.md).

Household needs now retain food demand, housing pressure and shared food conditions
for each saved economic period. Explicit treasury accounting is separate; automatic
funding, full prosperity and player transactions remain pending. See
[household needs and treasury scope](docs/HOUSEHOLD_NEEDS_AND_TREASURY.md).

Villages now select saved planning priorities from food deficits and observed crowded
homes, with seeded choices and readable explanations. Actual purchase/building execution
remains pending. See [village priorities](docs/VILLAGE_PRIORITIES.md).

Explicit food delivery accounting now supports saved, retry-safe supplies that
relieve shortages in future periods. Confirmed player menu donations now feed this
accounting; `/civ aid` remains a read-only preview. See
[food delivery scope](docs/FOOD_RELIEF.md).

Players can now hold plain food near a village and use `/civ aid` to preview its
food value and the village's latest food situation. This takes no items or money;
donations use a separate confirmed village menu. Conversions
and range are editable in `economy.yml`. See [player food-aid guide](docs/PLAYER_FOOD_AID.md).

Sneak-right-click a managed villager while holding plain food, then click **Donate
held food** to give that stack to the village. Normal vanilla trading stays available.
A saved recovery receipt prevents credit before verified inventory saving. Existing
owner configs must opt in with `donations.enabled: true`; new configs include it.
See [food donations and recovery](docs/FOOD_DONATIONS.md). No payout/barter yet.

Villages that choose to seek food now display a voluntary appeal with their latest
shortage. Sneak-right-click even with an empty hand to read it; reopen holding plain
food to donate. Appeals remember changing conditions and close after recovery.
See [village food appeals](docs/FOOD_APPEALS.md).

## Core Philosophy

- Keep the experience recognizably vanilla Minecraft.
- Settlements should evolve because of actual world conditions.
- Growth should physically change the world.
- Villages should build over time rather than instantly spawning structures.
- Player creations should be respected and protected.
- The simulation should create stories naturally rather than relying on scripted events.

## Planned Features

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
