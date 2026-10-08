# Phase 1 disposable Paper smoke test

Verified October 7, 2026: startup, restart and disposable backup recovery passed.
Interactive population gameplay remains unverified.

Official build metadata was fetched from:
https://fill.papermc.io/v3/projects/paper/versions/26.2/builds/129

- Paper 26.2 build 129, STABLE, published September 23, 2026.
- Artifact: paper-26.2-129.jar (64,522,678 bytes).
- SHA256: b1d8f6bfa1b6101fa8e947b53041cb3bdf5540e7b83b6547ca19ba7edefeb083.
- Downloaded exact artifact and independently checked SHA256; no newer build substituted.
- Isolated working directory in Windows temporary storage, outside OneDrive.
- Fresh world name civilizations-phase1-test, seed 872613, loopback 127.0.0.1:25577,
  online authentication enabled, RCON/query disabled. No existing world copied.
- User explicitly accepted the Minecraft EULA and authorized this isolated server.
  EULA was enabled only after that acceptance.

`scripts/start-dev-server.ps1 -Directory '<prepared directory>'` requires a disposable
marker, exact JAR checksum, accepted EULA, isolated properties and Java 25. It does
not accept terms, download/update software, delete worlds or alter existing servers.
The chosen port must be unused before launch. The launcher is an interactive
terminal command; stop cleanly with the Paper console `stop` command.

Local path, Java executable/version, plugin checksum and commit are recorded in the
temporary environment manifest; user/machine-specific details stay out of Git.
Temporary storage may be cleaned by Windows; this is a test environment, not a
durable server or backup. Do not keep valuable worlds here.

## Required results before claiming the gate

1. Record exact Java/Paper/plugin identity and clean startup/database migration logs.
2. Inspect plugin enable/disable and server-thread failures.
3. Validate commands, discovery/import and resident projections after integration.
4. Stop/restart and verify stable identities/calendar/event history.
5. Run unloaded simulation without forced chunk loading and measure bounded catch-up.
6. Verify source/config/world/consistent SQLite backup recovery in another new directory.
7. Record observations and remaining interactive playtest limitations here.

## Recorded results

- Temurin Java 25.0.4.1+1-LTS; exact Paper 26.2 build 129; plugin built from
  bd8f4ce. Civilizations enabled, initialized schema 9 and appeared in `plugins`.
- Fresh startup completed, a second startup reused the saved world/database,
  and a restored copy started successfully. All three shut down cleanly with `stop`.
- Test database reported schema 9 and `integrity_check = ok`; zero settlements
  and residents, as no player/import scenario was run. Restart alone does not
  validate resident/calendar gameplay.
- Recovery snapshot included a verified Git bundle, source archive, stopped-world
  and config archive, and a consistent SQLite snapshot. A new temporary restore
  matched all 68 archived world/config files before startup; recovered source at
  bd8f4ce passed `.\mvnw.cmd clean verify -B -ntp`: 115 unit tests and two packaged
  integration tests, zero failures/errors/skips.
- Initial Paperclip Minecraft download was incomplete and failed its checksum.
  Retrying the exact Mojang artifact passed Paperclip's required SHA256; no
  verification was bypassed. Windows performance-counter lookup logged an OSHI
  error on startup, but Paper reached `Done` and enabled the plugin. No OS settings
  were changed. Existing Maven/SQLite/manifest warnings remain non-failing.
- All server and recovery work used newly created disposable directories outside
  OneDrive. No existing Minecraft world or production database was touched.
- Server processes are stopped. Hosted CI, actual player recognition/import,
  contextual resident projection and bounded runtime catch-up remain pending.

Startup/recovery checks do not complete the population gameplay gate or hosted CI.
