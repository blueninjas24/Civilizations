# Explicit building and empty-plot protection

Implemented normal-server controls save a player-declared off-limits plot. They
do not donate buildings, activate housing, adopt livestock, prevent other players'
edits, evict existing residents or stop vanilla villagers walking through it.
Normal automatic construction remains disabled; these controls establish its
saved protection boundary. Player buildings still need explicit donation before
future adoption even if they have no extra exclusion sign.

## Select and confirm

1. Stand at one plot corner; run `/civ building corner1`.
2. Stand at its opposite corner; run `/civ building corner2`.
3. Run `/civ building protect` (or `preview`). Green particles mark the rectangle.
4. Read the inclusive X/Z bounds. Protection covers those columns at every height.
5. Click **Confirm: I own this plot**, explicitly declaring ownership. Nothing is
   saved until this step. Wait for `Building plot protected: <ID>`.

Optional sign: first line `[Civ Building]`, second line `protect`, on either side.
Right-click it after selecting corners. The sign must lie inside the selected
three-dimensional bounds and you must be beside it. It requests the same preview;
it never guesses boundaries or assumes that everything behind a fence is claimed.
Removing the sign does not erase a saved exclusion.

Before confirmation, repeat corner commands to edit the selection; old preview
tokens are invalidated. Selection changes across worlds clear the other corner.
You must return beside the selected plot in its world to confirm. A confirmation
from another player, an expired/replaced token, absent world or lost permission
cannot save it. A pending submitted save cannot be undone with cancel.

## Find, release and resize

- `/civ building here`: list active saved exclusion IDs covering your X/Z position.
- `/civ building release <ID>`: review the exact saved exclusion.
- Click **Confirm release**: remove only that exclusion. Original receipts remain.
- `/civ building cancel`: discard an unsubmitted selection/preview.

Only the recorded author or an administrator in that world can release. Removing
one exclusion does not remove other exclusions, protected pens, registered player
building footprints or project reservations. To resize with continuous protection,
confirm a replacement exclusion first, then release the old one. Overlap is allowed
for exclusions; a released receipt is never silently reactivated by replay.

Clickable commands use exact tokens. Manual equivalents are
`/civ building confirm <preview ID> owned` and
`/civ building confirmrelease <release token>`. They are not general donation or
reclamation commands.

## Server settings and permissions

`config.yml` optional `building-protection` section:

| Setting | Default | Meaning |
|---|---|---|
| enabled | true | Enable these controls only |
| maximum-edge | 128 | Maximum X or Z span, inclusive |
| maximum-area | 4096 | Maximum selected X/Z columns |
| maximum-requests | 16 | Global concurrent player selections/reviews/saves |
| preview-seconds | 60 | Confirmation lifetime |
| selection-seconds | 300 | Inactive selection lifetime |

Missing old-file values use defaults without rewriting the server's settings.
Malformed values disable building controls while population/economy remain active.
These settings cannot enable automatic construction. Restart after editing.

`civilizations.building.protect` defaults true for ordinary players; it permits
their explicit ownership declarations and own releases.
`civilizations.building.admin` defaults op and permits confirmed release of another
player's exclusion. This follows the approved trust policy; it is not proof of old
Minecraft building authorship or integration with external claim plugins.

## Threading and recovery

Selection, sign reads, permission checks, messages and bounded68-particle outlines
stay on the Paper thread. Selection performs no block/chunk scans. JDBC uses the
existing SQL-owner queue with a fresh present-world check at save/release. Polling
and memory are capped by configured request limits. Disconnect/cancel/stop clear
unsubmitted previews; accepted SQL work drains normally. Exact save/release retries
preserve original IDs and timestamps. Expected refusals do not mark simulation
health failed; real SQL failures remain visible and do not announce success.

Schema remains108. No real server/world/save is deployed or modified by development
verification. Household occupancy, donation signs/selection, full habitability and
normal block placement remain separate unfinished gates.

## Verification

Focused Wrapper clean verify:56 unit +3 packaged integration checks passed33.211s,
zero failures/errors/skips. Seven new UI/setting checks plus an owner-queue integration
check cover matching explicit confirmations, edits/expiry/world/permission changes,
pending cancellation, retries, release authority, bounded geometry and configuration.
Existing pen/registry/consent/session checks also pass. The initial test compile used
an older Adventure click-event accessor; corrected against the installed source API.

Actual checksum-verified Paper26.2 build129 passed BuildingProtectionProbe: installed
defaults/permission nodes/router/completion, actual sign interaction with a synthetic
non-admin player, no claim on preview, explicit save, sign-removal survival of25
protected columns, revoked release refusal, confirmed owner release and retained
history. The original sign cell was restored. Server stopped/helper archived;
production/helper fixture databases were108, integrity-ok and FK-clean. First fixture
setup stopped safely when its delayed spawn chunk had unloaded; corrected staging
on the first tick, with no production chunk-loading change or forced loads.

Final focused verification also covers refusal to overlay another saved donor,
active exclusion, protected pen or reserved site, and freezes release retry time
and authorization while still checking current authority before submission. Native
Paper proof preceded these final SQL/retry guards; the final focused checks cover
those guards. A further native rerun was not executed: automatic approval review
could not complete because the account usage limit was reached.

## Covered-bed setting

config.yml building-protection.require-covered-beds defaults true. The existing
read-only housing report applies this policy and identifies uncovered beds. Both
halves require safe overhead cover inside the declared structure with clear bed
headroom. Set false and restart to permit outdoor beds; support/access/real paired
beds still apply. Old configs get true without being rewritten. This setting
cannot enable donation controls, occupancy or normal construction.

## Single-home follow-up

Corner commands also accept exact x/y/z to include physical floor and roof blocks.
The separate civilizations.building.donate permission can use selection without
granting exclusion authority. /civ building home <village UUID> performs an explicit
one-home donation review and separate ownership confirmation; see HOME_DONATIONS.md.
Existing protection/release commands still do not donate homes or adopt animals.
