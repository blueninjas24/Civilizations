# Atomic workplace donation — development backend

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
