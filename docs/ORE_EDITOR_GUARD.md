# Unsupported bulk-editor provenance fence

## Current integration status (October19)

The normal presence fence below remains active. An optional explicit-registration
WorldEdit Bukkit7.4.4 adapter now has a provided-only API dependency and was verified
with actual edits, raw placements, ore removal, undo and failure fencing on isolated
Paper26.2-129. It is not yet registered by ordinary plugin startup; unsupported
editors, later enable and uncertain lifecycle coverage still fence current capture.

Schema99 stores positive new-generation evidence for exact world/chunks/resource
kinds and healthy capture epochs. Older chunks are never backfilled. Normal new-
chunk observation is bounded and never loads chunks. Scoped SQL authorization
requires all resource supports, appropriate observation times and current capture;
placed/removed exclusions and explicit old-resource recovery are retained. A clean
restart preserves certificates; a capture gap revokes them without deleting history.
The live new-terrain scanner/startup path is being verified independently of editor
lifecycle. These additions do not certify arbitrary plugin writes, offline edits,
hot reload or older WorldEdit history. See PROJECT_STATUS.md for current checks.

The historical implementation/verification notes below describe the original fence.

Lifecycle audit of the checksum-verified7.4.4 runtime confirmed onLoad may invoke
`ChunkDeleter.runFromFile` for queued chunk deletions. This bypasses EditSession
setBlock hooks. No deletion was executed during the audit. The ordinary presence
fence must remain until startup/shutdown and non-extent mutations are covered;
successful set/raw-set/undo capture is necessary but insufficient for full support.

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
