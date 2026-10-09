# Unsupported bulk-editor provenance fence

OreEditorGuard inspects installed plugin descriptor names before the live SQL owner
starts, and listens for later Paper PluginEnableEvent notifications. WorldEdit,
FastAsyncWorldEdit and VoxelSniper names are matched case-insensitively. Installed
disabled editors also pause new ore discovery: their earlier edits are not certified.
Names match exactly, not prefixes. Population, existing deposits, funds and history
remain; the editor itself is never disabled or prevented from editing.

This is conservative presence handling, not an editor operation integration or a
certificate of natural ore. Unknown/renamed plugins, offline editors, arbitrary
plugin world writes and hot reload remain outside verified provenance. Future editor
integration must certify supported mutation paths before relaxing this fence.
Removing/disabling a plugin cannot repair an earlier missing interval.

The existing tracking fence immediately blocks session discovery in memory;
startup/shutdown persist the gap, and dirty startup protects crash recovery. A clean
restart keeps trust false pending reviewed recovery. No trust-reset command or new
schema is introduced (schema 34). Status names the unsupported editor or the retained
recovery requirement, without player data or private addresses.

Admin text settings can add up to 32 plugin descriptor names:

```yaml
ore-provenance:
  additional-bulk-editor-plugins: [MyWorldEditor]
```

Names are 1..64 letters, digits, underscore or hyphen. Invalid types/keys fail before
database startup. An absent section in old economy.yml means no additional names;
built-in guards still apply. Owner files are not rewritten. This list extends the
guard; it is not an allowlist or safety-disable switch. Use the actual plugin
descriptor name, rather than command aliases or downloaded JAR filenames.

Primary descriptor references checked during implementation:
[WorldEdit](https://github.com/EngineHub/WorldEdit/blob/version/7.4.x/worldedit-bukkit/src/main/resources/plugin.yml),
[FastAsyncWorldEdit](https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/main/worldedit-bukkit/src/main/resources/plugin.yml).
No new dependency or downloaded editor is installed.

Four new automated cases cover installed disabled names, deterministic single
warnings, custom/case-insensitive/exact matching, built-ins and strict immutable
optional configuration. Windows Wrapper clean verify passed 653 tests (651 unit,
two packaged integration), zero failures/errors/skips. Unit tests avoid installing a
global fake Bukkit server; the actual event is verified by a disposable Paper probe.

src/test/paper/OreEditorGuardProbe.java registers the production guard against its
own temporary owner and explicitly configured test-plugin name. Its real enable
event pauses discovery; clean reopen without the guard retains the gap. The empty
probe database has a clean/untrusted fence. Main data/config remain intact with
clean/trusted history; integrity/FKs passed. No world edits, chunk tickets or players
are required. Normal shutdown succeeded and the probe was archived. This does not
test real WorldEdit/FAWE operations or certify external editor compatibility.
