# Separate renovation consent

Implemented following the explicit user decision: donation grants village use;
physical renovation needs separate owner consent. Permission controls are live;
ordinary automatic construction/renovations remain unfinished. No command here
starts work, modifies blocks, consumes contents or transfers property title.

## Commands

- /civ building allowrenovation <building UUID>: review a separate grant.
- /civ building denyrenovation <building UUID>: review withdrawal of a grant.
- /civ building confirmrenovation <token>: confirm only the exact stored preview.
- /civ building cancelrenovation: discard unsubmitted work; accepted saves finish.
- /civ building inspect <building UUID>: saved housing report includes renovation status.

Use the saved building's present world. Four controller requests maximum,30-second
load timeout,60-second confirmation previews. Separate owner previews are bounded
at64; replacing a preview invalidates its old token. No world scan or added native
read budget. Permission/world/disconnect/expiry cancel unsubmitted work. Submitted
SQL saves drain; unexpected SQL failures allow an explicit exact-token retry with
the same frozen timestamp/authority. Cancellation never promises undo.

civilizations.building.renovate defaults true, with saved donor identity checked
separately. Only the recorded donor can grant permission. Administrators may revoke
for safety; admin status alone cannot grant renovations for another donor. This is
the conservative recommendation under the user's delegated P4 decisions. Donors
with renovation permission alone can inspect their saved housing status without
plot-protection rights. Other donors' records remain excluded. The existing
building-protection.enabled/readiness gates apply.

## Identity and withdrawal

Schema112 adds building_renovation_events, an ordered permanent consent history.
Each decision binds operation/building, exact layout hash, current use-consent
epoch, expected previous renovation event, actor/admin authority and timestamp.
Existing donations migrate with zero renovation grants. No old records are changed.
Confirmed grant/revoke commits atomically, with exact retry and stale-history refusal.

Reclamation or re-donation invalidates an older grant, even if the same structure
is donated again. Old receipts return history only and never restore withdrawn
permission. Explicit plot protection blocks effective permission; an owner may
still permanently revoke the saved grant while blocked, so releasing the plot
later cannot unexpectedly restore it. A renovation revoke leaves normal donation,
occupancy, residents, buildings and contents intact.

This is only one prerequisite for future work. The builder must still check exact
scope, current use/renovation permission, saved protections, fresh loaded-world
block/entity evidence, paid materials and actual labor at each write boundary.
Permission grants neither extra land nor access to player inventories. Registered
layout revisions and actual renovation projects remain unfinished.

## Verification

Windows Wrapper focused clean verify78 unit +3 packaged checks passed57.313s for
the complete service/controller/migration slice. After renovation-only inspection
and protected-plot revocation safeguards, final focused clean verify33 unit +3
packaged checks passed31.345s, zero failures/errors/skips. Coverage includes owner
vs admin authority, explicit preview/confirm, expiry/identity/world/lost permission,
save retry/rollback, old-receipt non-restoration, re-donation invalidation, existing
donation upgrade retention, protected-plot permanent withdrawal, controller shutdown
draining, command routing and actual SQL-owner application persistence. Historical
migration filters recognize the new empty table. No broad-suite rerun is claimed.

Actual marked disposable Paper26.2 build129 passed installed permission default,
two-unit donation followed by separate grant/revoke controller confirmations and
saved inspection status with no admin/protection permission. Exact layout/unit IDs
and both renovation receipts survived a SQL-session restart. Restored144 fixture
cells; server stopped/helper archived. Both fixture DBs schema112/integrity ok/FKs0.
No real server/world/database changed; human-client compatibility remains deferred.
