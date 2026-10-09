# Village food appeals

A village that selects SEEK_FOOD and has an actual unmet food need now publishes a
voluntary appeal in its supplies menu. Sneak-right-click a managed villager to read
it, even with an empty hand or unsupported/custom items. The card names the latest
shortage in food units and the saved period behind it. Bring ordinary supported
food and reopen to use the existing explicit donation confirmation. Normal
right-click vanilla trading is unchanged. Preview permission/settings control
access; donation permission/settings still control actual transfers independently.

The displayed amount is the last completed period's unmet consumption, not a
cumulative debt, guaranteed future demand, desired reserve or a quest requirement.
Pending deliveries are shown separately; they are not counted as delivered twice or
assumed to fill historical shortages. A catch-up notice warns when queued periods
may change conditions. Menus are snapshots; reopen for fresh information. Appeals
do not promise money, reputation or payment and do not prohibit additional aid.

## Saved response and outcome evidence

One food_appeals row accompanies each newly completed food period. It references
both the household needs period and the actual priority review that governs it.
OPEN requires SEEK_FOOD and a nonzero deficit. Ongoing appeals report whether the
absolute unmet units improved, stayed unchanged or worsened since the previous open
period. Full supply RESOLVES an open appeal; changing priority while still short
WITHDRAWS it, without pretending that hunger ended. Later renewed needs can open
another appeal. Inactive periods record NONE. Historical rows remain immutable
through later deliveries, demographic changes, tuning and restart.

The row records actual imports and local production that period. These facts do
not prove the appeal caused recovery: production, population/demand changes and
other explicit incoming sources can affect outcomes. No reward or adaptive score
is based on that unproven attribution. This is the first executable voluntary aid
response, not an autonomous purchase, supplier search, transport or building action.
Those need real counterparties, funds, permissions and feasibility checks.

## Persistence and limits

Transactional schema 20 adds only food_appeals; older periods are not backfilled.
Food stocks, family allocations, priority review, appeal evidence and receipt use
commit together. Failed appeal writes roll back the period and leave queued work
and receipts retryable. Restart reads exact saved evidence from the SQL owner and
publishes immutable views; Paper menus/diagnostics never query SQL or load chunks.
Previous plugin versions reject schema 20; rollback needs a consistent backup.

Adults/children, production, item conversions, shortage settings and priority
weights remain editable in economy.yml. The appeal adds no separate invented
numeric target. Disabling previews hides the menu; disabling donations still lets
players read it, with a disabled donation button. This does not authorize block
changes, treasury creation, paid trade or mortality.

Tests cover open/inactive/withdrawn/resolved states, improvement/worsening, exact
history, 1,200-period repeatability, temporary-SQLite restart, migration and injected
write failure. A marked disposable Paper fixture checks an empty-hand native menu,
protected report clicks, reopened food confirmation, retained famine appeal after
restart and resolution after a verified actual player-file debit. A human client
UX/compatibility playtest and large-server profiling remain outstanding.
