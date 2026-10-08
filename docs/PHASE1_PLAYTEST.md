# Phase 1 population prototype — playtest notes

October 8, 2026: all ten Phase 1 foundation milestones passed. The nine-phase roadmap
remains unchanged; economy, businesses, construction and roads are future work.

## What you can see

- Natural villages and player-built areas with beds and villagers participate automatically.
- Villagers receive their saved names and baby/adult ages. Names, people, households
  and basic family ties survive restarts.
- Saved births and occasional immigrants can appear near players at valid loaded homes.
- Overcrowded households can move to observed vacant homes in another settlement.
- Simulated deaths remove their physical villagers; actual deaths retain the person
  in saved history. Managed villagers do not turn into zombies, but zombies can kill them.
- Villages keep aging while unloaded, without simulation force-loading their chunks.

## Try it

1. Use Paper 26.2 build 129 and Java 25 in a fresh disposable test world. Back up any
   existing source/world/config/SQLite first using DEVELOPMENT.md.
2. Build with `.\mvnw.cmd clean verify -B -ntp`, copy
   `target/civilizations-0.0.1-SNAPSHOT.jar` into that server's `plugins` folder and restart.
   Do not install `population-probe.jar` for normal play.
3. Visit a village with at least two bed heads and two villagers. Recognition scans
   incrementally; allow several seconds or a later revisit for complete loaded coverage.
4. As an operator use `/civ debug status`, `/civ debug observation`,
   `/civ debug settlement <UUID>` and `/civ debug explain <UUID>`. New village UUIDs
   appear in the server log. Commands are read-only, not population cheats.
5. Restart and revisit. Report duplicate/missing people, incorrect age/name, repeated
   births/deaths, unexpected chunk loading, or failed save/projection diagnostics.

Prototype time is one month per 30 real seconds (six minutes/year), adulthood 18
years. Births are probabilistic and require eligible partners plus observed space;
they are not guaranteed every visit. Partial village scans make capacity unknown,
which defers births/moves until reliable housing evidence returns.

New spawns require a player within 64 blocks, a loaded bed and supported air space.
The default loaded managed-villager budget is 32/day, eight/night per world. The night
budget limits additional spawns; it does not cull existing villagers. Normal villager
AI/HOME behavior handles activity. Dense city schedules and markets remain future work.

## Material limitations

- **Use disposable worlds.** Moves recreate physical villagers. The saved person and
  family/age/skills remain, but vanilla profession, trade offers, trade experience and
  inventory are not copied to a recreated incarnation. Preserve these before broad
  established-world deployment; this is not an approved eventual trade-reset rule.
- Other native transformations (such as lightning into a witch), third-party direct
  entity removal and live name-tag renaming are not fully reconciled. Missing active
  entities are deliberately not respawned or presumed dead: uncertain evidence must
  not invent a replacement or destroy history.
- Original villagers keep their AI/gravity/invulnerability/trades during ordinary name
  and age updates. Temporary spawn freezing applies only to plugin-created pending
  projections and is removed after durable activation, including restart recovery.
- No guaranteed tick deadline or 20-settlement/500-person-city performance claim.
  Two inspected chunks/sixteen resident decisions per tick bound routine work; one
  dense Paper enumeration cannot be preempted. Large-scale profiling remains necessary.
- Reconciliation does not globally despawn vanilla villagers. Proximity/budget gates
  constrain new spawns. Physical density, numerical balance and actual client play
  need tuning; Phase 1 completion is a foundation gate, not release readiness.
- Existing saves reject changed seed/cadence/demographic rules; do not silently reuse
  a save under different settings. Prototype test scenarios use separate fresh DBs.

## Evidence

Windows Wrapper `clean verify` passed 224 cases (222 unit/two packaged integration),
zero failures/errors/skips. Seeded long-run/restart/rollback tests remain in the suite.
The real Paper test-only probe passed hazardous campfire-floor deferral, babies and
adulthood, saved names, pending/activation restart windows, managed/unmanaged native
breeding, actual zombie infection cancellation and death saving, five-person migration,
simulated death removal and a death before enrollment publication.

The probe uses real Paper entities and production adapter classes with a synthetic
nearby-observer predicate, not an authenticated Minecraft client. It checks the marked
disposable environment/no online players, uses separate scenario DBs, and explicitly
loads fixture chunks as a test helper. Production adapters do not use chunk tickets.
The probe is compiled from `src/test/java`, packaged separately with
`src/test/paper/plugin.yml`, and excluded from the shipped plugin JAR. Real probe
results are not counted as JUnit cases or hosted-CI results.

Normal plugin-only startup, commands, unloaded fixtures and absence of forced chunks
were checked separately. Schema 13 integrity/foreign keys passed for the primary dev
DB and four scenario databases. All servers stopped cleanly; consistent development
DB backup preceded deployment. Latest work remains local on a feature branch.
