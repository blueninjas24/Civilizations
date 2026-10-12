# Qualified first-warehouse investment

The SQL owner can prepare a first mineral-chain warehouse for a genuinely
qualified adult merchant, then accept it using fresh loaded-barrel proof. This is
the preparation/acceptance path used by automatic merchant training and the
ordinary startup scheduler, now connected in AUTOMATIC_WAREHOUSES.md.

Preparation uses WAREHOUSE_OPPORTUNITIES.md's actual customer, current trusted
finite-source evidence and protected complete-chain budget. It filters living
local adults with saved commerce experience before the bounded founder page,
excludes current jobs and active courses, then chooses deterministically under the
saved seed. Existing ownership does not impose a business-count limit. A founder
takes one worker job in the warehouse, so an existing worker must first leave
their other job through the normal lifecycle.

Preparation changes no rows and reserves no money, goods, ore or site. Business
and site identities are stable for the town, world and barrel coordinate. A
barrel does not grant building ownership or access to its inventory.

Acceptance checks the current opportunity again in the same transaction as
qualification, employment, the real public startup debit/capital transfer, empty
warehouse store and fixed marker binding. It checks exact terms, observed site
freshness and a matching present loaded-world barrel proof. Changed customers,
food assessment, finances, ore exclusions, mutation trust, qualifications, jobs or
courses can decline an old proposal. Refreshed marker evidence requires preparing
again. Failed SQL, including the last marker insertion, rolls everything back.

No stock, customer payment, supplier, extraction or transport is invented. This
accepts the warehouse only; later qualified mines must still open and produce
real goods. Stored terms replay after restart without another payment or job,
without reviving an ended job and without clearing a confirmed marker pause.
Legacy warehouses cannot acquire a new marker requirement by replaying startup.

Optional editable economy.yml section:

```yaml
warehouse-founding:
  enabled: true
  founders-per-review: 64
  minimum-age-months: 216
  founder-commerce-experience: 10
  worker-commerce-experience: 1
  worker-capacity: 2
```

These are configurable prototype terms under the approved gradual qualification
and grant rules. Missing older sections disable founding; invalid types, unknown
keys and inconsistent/bounded values fail before database startup. Settings are
never overwritten. This investment batch introduced no migration; subsequent merchant course
links use schema63 (see AUTOMATIC_WAREHOUSES.md).

`PopulationSession.prepareWarehouseInvestment(town)` is read-only;
`acceptWarehouseInvestment(draft, confirmation)` runs on the SQL owner and waits
for current food/population/recognition availability. Tests use only temporary
SQLite and include a genuine paid workshop/farm customer followed by a normal
food assessment for the merchant. Verification results are in PROJECT_STATUS.
