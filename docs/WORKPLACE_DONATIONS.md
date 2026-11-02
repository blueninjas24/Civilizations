# Reviewed workplace donation, reclamation and re-donation

Available November1 for an existing fixed-marker business. Select floor-to-roof
corners, stand at the entrance and use /civ building workplace <business UUID>.
The player must explicitly declare ownership through the confirmation button or
/civ building confirmworkplace <token> owned. A new survey runs after confirmation;
the earlier preview snapshot cannot stand in for that new evidence.

Use /civ building cancelworkplace to discard an unsubmitted review. Donation uses
civilizations.building.donate, matching existing home donation permission. The
parent building controls must be enabled/ready. Both ordinary native passes and
exact marker bracketing remain loaded-only. Four requests, one per player, share
one128-cell advance per tick for this donation controller, across preview and
confirmation. This is an additional manual queue beside workplacecheck, not128
reads for each player/pass. Pending owner lookup does no native reads.

WorkplaceDonationRequests freezes player, business, layout, previous consent,
inspection policy and a60-second deadline. Current player permission/world/
selection, disconnect and expiry are checked. The SQL owner repeats context,
current consent, business/marker/suitability and collision validation. Failed
publication retains the operation for an exact safe retry; accepted SQL work
finishes through disconnect/close. Preview state is transient across restart,
while committed consent/hold history remains durable. Human recovery/compatibility
validation remains necessary; the command does not guarantee cross-plugin behavior.

## Taking back and giving back

/civ building reclaim <building UUID> now previews housing OR workplace withdrawal.
The separate donor/admin confirmation remains required. A workplace preview
explains business-use loss rather than inventing household displacement. Withdrawal
pauses formal premises use, preserving title, people, blocks, contents, business
assets and all history. A marker alone cannot bypass it. No eviction teleport,
property sale, rent, compensation or automatic demolition is introduced.

To re-donate a reclaimed workplace, use /civ building workplace <business UUID>
<building UUID>, selecting exactly its registered bounds and standing at the
original entrance. Registered geometry/donor/identity cannot be silently revised.
The new atomic publication can retire only the same business/building's already
withdrawn hold and replace it with one current hold. Failure rolls the release,
new consent, marker update and new hold back together. The old hold/history stays.
Another firm's reservation, active use or altered geometry is never taken over.

## Editable shelter preference

config.yml building-protection.require-covered-workplaces defaults true (including
an absent key in old files). Stop/edit/restart to change. It controls new diagnostic
and donation inspections; the preview freezes its policy until expiry. It never
waives safe entrance/support/access, current marker, ownership or permission checks.
It does not retroactively inspect saved premises or add productive capacity.
Existing files are preserved; wrong types/unknown keys refuse configuration.

## Final verification and limits

Clean Windows Wrapper70 unit+3 packaged integration checks passed43.393s:

    .\mvnw.cmd clean verify -Dtest=WorkplaceDonationRequestsTest,WorkplaceDonationTest,BusinessPremisesTest,BuildingRegistryTest,WorkplaceOperationsTest,PaperWorkplaceDonationTest,PaperHomeReclamationTest,PaperBuildingProtectionTest,WorkplaceInspectionTest,PaperWorkplaceReviewTest

Tests cover player-bound expiry/ownership/context, fresh-after-preview evidence,
late rollback/retry, accepted-save draining, reclamation impact and atomic
re-donation. Native acceptance on the final packaged JAR used real Paper surveys,
a synthetic player and a separate owner-thread temporary SQLite database. It
saved the donation/hold through the actual controller, confirmed reclamation
pauses eligibility, then explicitly re-donated through a new preview/confirmation.
Two retained receipts, exactly one current hold; no invented stock, treasury,
housing or construction. All100 captured terrain cells restored, server stopped,
helper archived; both120 databases integrity/FK clean. A fixture ticket retained
its already loaded chunk only; production never loads chunks for simulation.
Native reclaim used the real registry; its human-facing controller is covered by
injected-player tests. Human client and other-plugin acceptance remain outstanding.

This does not found/relocate a business or give it extra workers/storage. Automatic
vacant-workplace selection, different-firm reuse and saved operating-condition
reviews remain unfinished. No migration beyond120 or human server/save changes.
P4 stays8/16=50%; handbook0.33 is36 pages/33 chapters with working controls.

## Historical backend checkpoint

# Earlier atomic publication checkpoint

November1: WorkplaceDonation can publish explicit player use consent, current
positive fixed-marker confirmation and the associated business premises hold in
one SQL-owner transaction. No normal donation command is enabled by this backend.
The existing workplacecheck command remains read-only. P4 item2.4 stays unfinished.

The trusted caller supplies an authenticated ownership declaration, a frozen
WORKPLACE layout, existing business, expected previous consent, present permanent
world and fresh computed WorkplaceInspection assessment. Existing donor/layout,
protection, open-business, fixed marker, suitability and occupancy checks remain.
No villagers, goods, cash, labor, capacity, chest access, title or renovation rights
are created. Existing P3 business identity and site are not moved or rewritten.

The donation operation deterministically identifies its premises receipt. Late
failure rolls back building registration, consent, marker freshness/history and
the reservation together. Public single-operation wrappers still reject nested
transactions; package-local row methods compose only under the caller's transaction.
No nested commits or generic transaction framework is introduced.

Exact retry after restart validates the same donor, layout, business and original
accepted timestamp. It returns historical consent and the current retained/released
status of that exact hold, never renewing withdrawn use or creating a replacement.
An existing donation receipt without its matching atomic hold refuses recovery;
the backend cannot silently adopt an unrelated prior operation. Changed terms fail.

Five temporary-SQLite cases cover persistence/restart, exact replay after reclaim/
release, late rollback including marker history, missing authority/world/freshness,
and public transaction boundaries. Clean Windows Wrapper39 unit+3 packaged checks
passed41.592s with WorkplaceDonationTest,BusinessPremisesTest,BuildingRegistryTest,
WorkplaceOperationsTest. No migration beyond120, real server/database or Paper API
change. The backend is not native/player-interaction acceptance.

Next: player-bound expiring frozen preview, a fresh confirmation survey and explicit
ownership confirmation using this atomic publication. Ordinary vacancy selection,
relocation and saved operating-condition reviews remain separate work.
