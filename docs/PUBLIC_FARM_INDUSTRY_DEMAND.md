# Public farms as real industry customers

Public farm tool needs now participate in warehouse restocking. A farm that has
actually produced food can give local tool producers a customer without inventing
consumption, spending or jobs. This connects existing businesses; it does not yet
open the first warehouse or blacksmith in an ordinary village.

The read-only review requires the canonical saved public farm, qualified current
staff, an operating business and its permanent world present. Actual productive
wear must make a replacement due. Current complete observed fields, positive
food production and matching farm learning history must support the latest food
period. Its assessment must cover all current living residents and fully supply
their food needs. Unknown, mismatched or incomplete evidence creates no customer.

The farm must have enough actual village cash after the existing food reserve,
minimum treasury and per-period equipment limits. Owned or paid incoming farm
equipment covers the need. A retained legacy farm store is preserved rather than
silently converted. The review does not anticipate supplier contributions or
reserve money; final purchases recheck the established payment rules.

Local demand allocates available warehouse stock once across real customers.
Warehouse restocking then subtracts its existing paid cargo and uses ordinary
bounded producer selection, real stock, purchasing cash, storage space and haul
limits. Overflow defers the complete review rather than inferring a shortage from
part of a town. Goods and money change only through the existing paid purchase
and delayed delivery transactions.

Admins use the existing `farm-tools`, `farm-tool-budget` and industry supply
settings in their server's `plugins/Civilizations/economy.yml`. There is no new
setting, tax, currency, recipe or migration. Files that omit the existing upkeep
sections retain their opt-out behavior. Disabling new orders preserves paid cargo
and saved equipment history.

Temporary SQLite tests cover read-only review, actual work and food evidence,
funds, population changes, service, incoming equipment, preserved legacy catalogs,
bounded review, stock allocation and the paid producer → warehouse → farm chain.
The normal PopulationSession test also covers restart, absent-world pause and
fresh observation on return. The independent FarmWarehouseProbe repeats that
chain through actual SQL-owner sessions under disposable Paper. It seeds explicit
fixture businesses and supplies; it is not proof of automatic first founding.

Schema remains 59. No real server database, player inventory, barrel contents,
Minecraft blocks or ownership rights are changed by this review.
