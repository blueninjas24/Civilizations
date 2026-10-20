# Single-family home donation controls

Implemented: selected original/player-built home -> bounded current region review
-> explicit ownership confirmation -> saved village-use permission. No household
move, usable capacity, title, inventory or automatic block placement is activated.
No apartment inference: selecting several bedrooms/beds still declares one home.

## In game

1. Select opposite corners using /civ building corner1 and corner2. These default
   to feet position. Exact /civ building corner1 <x> <y> <z> and corner2 <x> <y> <z>
   include floor blocks, outer walls and roof without standing inside solid blocks.
2. Stand at the entrance inside the selected volume. Run /civ building home
   <village UUID>. Tab supplies up to32 saved IDs; wrong-world villages are refused.
3. Review the one-family declaration, complete X/Y/Z scope, village and bed count.
4. Click the ownership/donation confirmation. Manual equivalent:
   /civ building confirmhome <token> owned. Wait for the final saved message.

/civ building cancelhome discards only unsubmitted work. An accepted SQL save
already drains across disconnect/shutdown; cancellation cannot promise undo.
The new civilizations.building.donate permission defaults true. Selecting corners
is permitted for donors even without permission to create off-limits exclusions.
The separate protect/admin nodes retain their existing authority. Old unrecorded
ownership uses the approved explicit player declaration policy, not admin approval.

## Qualification and safety

building-protection.enabled gates these controls. require-covered-beds defaults
true and is editable in config.yml; restart after editing. Real bed pairs, support,
headroom and reachable private access always apply. Glass/slabs/stairs can shelter
beds, but partial shapes are not yet standing-route proof. Include overhead cover
inside the selected structure. Missing/unloaded/expired evidence cannot qualify.

Selection limits reuse existing settings; structures are capped at65,536 cells.
Four donor requests total, one per player. Donation scans use128 cell reads/tick
and inspection reports use128, a combined live housing maximum256 for all players.
Scans expire after30 seconds; SQL-owner confirmation previews last60 seconds.
Edited selection, changed world, lost permission, disconnect and expiry discard
unsubmitted reviews. Saved donors, exclusions, pens and reserved projects are
rechecked by the existing serialized registry at preview/confirmation. Failed
saves announce no success and permit explicit exact-token retry while valid.

Paper handles only bounded reads and current actor facts. Complete immutable
region evidence goes to the SQL owner; geometry, qualification and consent work
run there. No asynchronous Bukkit access, forced chunk loading or database migration.
Schema108 unchanged. Saved footprint remains protected; this is not an anti-grief
plugin or proof of historical authorship. No reclamation command is exposed before
household displacement/capacity reconciliation is ready. Donation signs, apartments,
edit-aware revalidation and actual occupancy remain separate unfinished gates.

## Verification

Final focused Wrapper clean verify34 unit +3 packaged checks passed23.261s,
zero failures/errors/skips. Four new controller checks cover matching explicit
ownership tokens, edited/world/permission/expiry/cancel, total queue/read bounds,
accepted-save draining and explicit failed-save retry. Exact coordinate selection
has an additional permission-free-world-read check. Existing temporary-SQLite
preview/confirmation, geometry/roof and owner-consent checks also pass.

Actual private Paper26.2 build129 verified plugin startup, installed donation
permission default and home-route completion, plus existing real-sign protection,
explicit owner save/release and retained history. It did not perform a native
end-to-end home donation interaction. Region/roof native checks passed the preceding
covered-declaration batch; the live controller is covered by focused injected-Player
checks and the actual temporary-SQLite owner integration. Human client testing remains.
