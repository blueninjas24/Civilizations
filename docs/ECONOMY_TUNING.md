# Tune Civilizations for your server

The economy settings live in **`plugins/Civilizations/economy.yml`** on a server.
The plugin creates the commented file on first startup and preserves your edits.
The repository copy is `src/main/resources/economy.yml`.

**Current scope:** automatic food accounting runs after reliable planted-field evidence.
It uses saved adult/child demand, worker capacity and a separate economic clock.
Fields are observed without harvesting or editing blocks. No automatic starvation
mortality or merchant transactions are implemented. See LIVE_FOOD_ECONOMY.md.

## Normal, famine and feast

The simplest knob is `food.output-percent`:

| Setting | Meaning for a facility with available workers |
|---|---|
| `100` | Normal prototype output |
| `25` | One-quarter output: useful for a famine scenario |
| `200` | Double output: useful for a feast scenario |
| `0` | No food output |

For example, under `food:` change this line:

```yaml
  output-percent: 25
```

Production still needs evidence of a real facility and available working adults.
Changing a multiplier cannot conjure farms. Percent output rounds down to whole
food units each period; tiny operations may produce zero at low percentages.

For a harder scenario, increase `units-per-adult` from `2` to `3`. For an easier
scenario, decrease it to `1`. Children default to `1`. Setting a consumption value
to `0` disables that group's food demand in the calculation.

## Other settings, in plain English

- **Pace:** one economic period is 60 real seconds. These are separate from population
  months. A shorter period accelerates both consumption and production; it does not
  by itself create a famine. Offline catch-up defaults to at most 120 periods
  (two hours at this pace); zero disables it. Two periods per batch is a work limit,
  not a guarantee of server tick time.
- **Food output:** a worker produces four units per period at 100%, bounded by the
  smaller of available adult workers and evidenced facility worker slots.
- **Starting supplies:** food defaults to 12 units per living resident, about six
  adult periods without production. Timber/stone each start at four, iron/coal each
  one, livestock products zero. These seed an inventory only once on explicit initial
  reliable field enrollment. Editing the file or restarting never refills existing reserves.
- **Shortages:** reserves are used first. Deficits become severe after three consecutive
  periods with coverage at most 50%. Any fully supplied period clears the streak.
  Coverage at least 125% is surplus; at least 200% is abundant. These labels explain
  accounting conditions, not instant population penalties.
- **Prices:** one food unit has base Trade Value two. Shortage prices are 150% of base,
  severe shortage 250%, balanced 100%, surplus 85%, abundant 70%. Food's resulting
  values are 3.00, 5.00, 2.00, 1.70 and 1.40. Trade Value is abstract, not an emerald
  count or an offer currently attached to a vanilla merchant.

Commodity units are abstract quantities, not items taken from chests or harvested
by modifying blocks. Base Trade Values allow up to two decimal places. Quotes round
to hundredths, with a minimum positive quote of 0.01. Adding a commodity key requires
matching entries under starting supplies and base prices; actual item/facility support
for that commodity remains separate implementation work.

## Editing safely

1. Stop the server and back up your settings and save.
2. Open `economy.yml` in a text editor. Keep its indentation; use numbers without
   quotes or percent signs. Comments beginning with `#` explain each section.
3. Change a few values at a time. Leave `format-version: 1` unchanged.
4. Restart. Startup logs confirm the selected economic pace and food-output percentage.
   Invalid/missing/unknown keys fail with a diagnostic instead of silently using defaults.
   Correct the indicated file entry and restart; the plugin does not reset saved data.

There is no live reload command yet. Restart applies changes forward and records
a tuning transition; old reserves and period explanations remain intact. The offline
interval and partial period at the transition are skipped and logged in the save.
If old economic work is queued, startup rejects retuning: restore the old file,
let catch-up finish, then stop and make the edit. Never edit SQLite to bypass it.
A price-only edit also starts a recorded settings boundary. Changing starting
supplies does not refill accounts. Changing the commodity catalog of an existing
account requires an explicit inventory transition and is currently rejected.

The optional `farms` section uses `radius-blocks: 32`, `vertical-radius-blocks: 16`
and `crops-per-worker: 16`. These editable observation/capacity defaults support the
prototype without claiming farmland ownership or assigning business jobs. Old
format-1 files missing the entire section retain these defaults; partial/invalid
sections fail clearly. Missing coverage or overlapping town fields pauses work.

## Verified examples

With four adults, two evidenced worker slots and 48 starting food units, the same
20-period scenario ends with 48 units at 100%, an explained severe shortage at 25%,
and 208 units at 200%. These are controlled automated fixtures, not a balance claim
for a survival world.

Windows Wrapper `clean verify` passed 274 cases (272 unit/two packaged integration).
The real Paper food probe verified planted-field supply, depletion/severe shortage,
forward normal-to-feast restart tuning, unchanged residents/history and continued
accounting without loaded chunks. Normal startup also preserves old owner files
without the farms section. Schema 15 retains history and identities; no production
save or world was reset. These are controlled fixtures, not survival balance claims.
