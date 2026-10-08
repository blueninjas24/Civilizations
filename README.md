# Civilizations

Civilizations is a PaperMC plugin that transforms vanilla Minecraft villages into persistent, evolving settlements.

Villages can grow, construct buildings, develop economies, form relationships, trade with other settlements, and eventually create interconnected regional civilizations.

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

Development began September 27, 2026.
