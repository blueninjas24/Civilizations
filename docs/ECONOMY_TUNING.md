# Tune Civilizations for your server

The economy settings live in **`plugins/Civilizations/economy.yml`** on a server.
The plugin creates the commented file on first startup and preserves your edits.
The repository copy is `src/main/resources/economy.yml`.

**Current scope:** the file is loaded/validated and its consumption, production and
price calculations are tested. Automatic village economy scheduling, observed
production facilities and merchant transactions are not connected yet. Changing
these settings currently prepares the prototype; it does not make villagers starve
or create a live feast. No automatic starvation deaths are implemented.

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
  by itself create a famine. Future offline catch-up defaults to at most 120 periods
  (two hours at this pace); zero disables it. Two periods per batch is a work limit,
  not a guarantee of server tick time.
- **Food output:** a worker produces four units per period at 100%, bounded by the
  smaller of available adult workers and evidenced facility worker slots.
- **Starting supplies:** food defaults to 12 units per living resident, about six
  adult periods without production. Timber/stone each start at four, iron/coal each
  one, livestock products zero. These seed an inventory only once on explicit initial
  enrollment. Editing the file or restarting never refills existing reserves.
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

There is no live reload command yet. Existing accounting APIs protect saved shortage
thresholds and reject silent policy changes. A future explicit, recorded tuning
transition must apply new rules forward without rewriting historical explanations;
that transition is not implemented in this batch. Do not edit SQLite to bypass it.

## Verified examples

With four adults, two evidenced worker slots and 48 starting food units, the same
20-period scenario ends with 48 units at 100%, an explained severe shortage at 25%,
and 208 units at 200%. These are controlled automated fixtures, not a balance claim
for a survival world.

Windows Wrapper `clean verify` passed 261 cases (259 unit/two packaged integration).
Paper verified first-run file creation at 100%, a 25% edit surviving restart byte for
byte including an owner comment, normal population diagnostics/unloaded fixture and
clean shutdown. Restored the disposable test file to normal afterward. Schema remains
14; no existing economy inventories, villagers, histories or production saves reset.
