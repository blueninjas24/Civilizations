# Headless simulation foundation

Phase 1 now has a plain-Java stepping runner and a packaged diagnostic probe.
It does not implement population dynamics, time dilation, economic ticks, world
reconciliation or an in-game `/civ debug` command.

## Runner contract

- A `SimulationCheckpoint<S>` carries the seed, completed logical step count and
  domain state. The caller must supply immutable state; Java cannot enforce deep
  immutability for arbitrary `S`.
- `SimulationRunner.advance` runs at most the caller-supplied positive step budget
  toward an explicit target, returning a new checkpoint and remaining work.
  It refuses backward targets. Completed steps are numbered from one and are
  never repeated when resuming the returned checkpoint.
- A pure step returns new immutable state. It must not mutate the old state,
  perform external side effects, access Bukkit/world data, use current time or
  global randomness, or depend on unstable collection iteration order.
- Step randomness is derived from seed and logical step number using fixed
  64-bit mixing and Java Random's specified algorithm. Integer wraparound in seed
  mixing is intentional. No PRNG cursor lives between batches. Same initial
  state, seed and step function produce the same result across batch boundaries
  and reconstructed checkpoints.
- If a pure step throws, no new checkpoint is returned and the original can be
  retried. This is not rollback for mutations, files, DB writes or world changes.
- The budget bounds **number of steps**, not wall-clock execution time. Domain
  steps must themselves be bounded. No Paper scheduling or CPU-time budget is
  implemented; do not attach an expensive catch-up loop to a server tick.
- Logical steps have no chosen relationship to Minecraft ticks, days, years or
  real time. Seeds, target and budget are caller parameters; no gameplay tuning
  defaults are established.

Checkpoints are currently in-memory values, not serialized save data. Persistent
checkpoints and future replay compatibility/versioning need tested storage before
the runner drives real settlements. Adding new random draws or changing domain
rules can change outcomes; the fixed probe fixture guards the present convention.

## Packaged diagnostic probe

After `.\mvnw.cmd clean verify`:

```powershell
java -cp .\target\civilizations-0.0.1-SNAPSHOT.jar `
  com.blueninjas24.civilizations.simulation.SimulationHarness 42 10000 17
```

The arguments are seed, total synthetic steps and batch size. Output:

```text
mode=synthetic-probe seed=42 completedSteps=10000 checksum=-6876560382178007112
```

Changing batch size to 1000 produces identical output. The probe accumulates random
values into a checksum; it does not invent residents, interpret steps as years,
read a database, load chunks or start a Minecraft server. The CLI deliberately
finishes the requested work in a loop for testing; a future server adapter must
yield between bounded batches.

## Verified coverage

Tests cover seeded replay across several batch sizes, reconstructed checkpoints,
explicit remaining work, sequential steps, no repeated completed work, failure
retry with immutable input, distinct seeds, rejection of invalid targets/budgets,
null output, the `long` step boundary and repeatable one-million-step acceleration.
A fixed checksum fixture guards replay behavior. A Failsafe test launches the
packaged probe in separate JVMs and compares results across batch sizes.

This is infrastructure validation, not the roadmap's eventual multi-year
population/economy scenario acceptance gate or a measured server performance claim.
