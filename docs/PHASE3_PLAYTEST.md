# Phase 3 playtest: what to try now

Current October 18 prototype. This guide describes implemented behavior, not a
claim that Phase 3 or its human acceptance testing is complete. The latest tested
runtime is Paper 26.2 build 129 with Java 25. Players need a matching Minecraft
client; no client mod is required.

## Install safely

1. Stop your test server normally. Back up its worlds, plugin settings and saved
   database together while stopped. Keep a copy of the old plugin JAR.
2. Replace the Civilizations JAR in `plugins` with the newly verified
   `target/civilizations-0.0.1-SNAPSHOT.jar`. Keep only one Civilizations JAR.
3. Review settings before starting. Installed configuration files are preserved;
   newly added features may require copying their individual sections from the
   shipped `src/main/resources/economy.yml`. Preserve your existing tuning.
4. Start normally, then check the console for startup or migration failures.
   Do not use a plugin hot-reload tool or delete the database to fix a warning.

The development build does not automatically install itself on your server.
Running a live SQLite database inside OneDrive risks sync conflicts and incomplete
backups. Prefer a test server outside synchronized folders; move it only while
stopped, after making a consistent backup.

## Find your village and its reports

Visit a village with actual villagers and beds, including a player-built village.
Recognition is automatic; there is no joining command. Keep the area loaded while
the bounded observer finishes. A running calendar with zero residents does not
prove a village has been recognized yet.

These commands require admin debug permission:

| Command | What it tells you |
|---|---|
| `/civ debug status` | Whether simulation is running and its latest failure |
| `/civ debug observation` | What loaded-area scans have seen or are waiting for |
| `/civ debug villages` | Saved village names and full IDs, at most 32 per report |
| `/civ debug settlement <UUID>` | Saved population and household information |
| `/civ debug explain <UUID>` | Population decisions and their reasons |
| `/civ debug economy <UUID>` | Food needs, reserves, funds and economic decisions |
| `/civ debug businesses <UUID>` | Businesses, workers, stock and operational reasons |
| `/civ debug workforce <UUID>` | Qualified labor and competing vacancies |
| `/civ debug resources` | Saved resource tracking and uncertainty |

For commands needing a village ID, press Tab after the command and a space.
Completion offers full saved IDs; type a prefix to narrow the list. A shortened ID
alone is not accepted. Long reports can be read in the server console/log instead
of captured in screenshots. Reports do not reset the simulation.

## Try player interactions

- Hold plain supported food and **sneak-right-click a managed villager** to open
  village supplies. Donate a small stack first. Wait for the delivery confirmation;
  food becomes available in a future food period.
- When the village needs food and can afford it, use the sale option. Keep the
  offered stack in your main hand until confirmed. Compare both your food and
  emerald counts before and after. A whole stack can exceed the village's need;
  split it before offering.
- `/civ aid` previews the nearby held-food offer. `/civ bargain <emeralds>` proposes
  a price and `/civ bargain accept` accepts a current offer. `/civ barter timber`
  or `/civ barter stone`, then `/civ barter accept`, uses available owned goods.
  Need, stock, funds and permissions can legitimately prevent a trade.
- `/civ market` accesses available nearby staffed livestock milk/egg stock, not a
  universal item shop. Bring emeralds, inventory space and an empty bucket for each
  milk purchase. No free bucket is supplied. Village-owned animals do not also
  provide duplicate vanilla milk/egg products; player-owned animals behave normally.
- To protect a suitable enclosed player pen, use `[Civ Protect]`, right-click the
  sign from inside and review the preview before `/civ pen confirm`. Claiming needs
  admin permission. `/civ pen here` inspects the saved protection. A fence or sign
  by itself does not claim everything behind it. Large unmarked pastures need
  explicit protection because automatic recognition uses a bounded local scan.

## Watch industry develop

Farms, fisheries, livestock, mines, stone/sand operations, logging, warehouses,
blacksmiths, bakeries and the general craftsman have implemented simulation paths.
This is not a guarantee every village can open every business. Suitable evidence,
qualified adults, protected startup funds, materials and actual paying customers
matter. Training takes time and consumes real supplies.

Existing smithing tables, barrels, smokers and crafting tables can serve as
configured workplace markers. They do not grant ownership of player buildings or
permission to take chest/barrel contents. Inspect the business report before
assuming an observed marker has become an operating business.

Look for a causal chain: an opportunity and qualified founder, a funded business,
workers, production, stock movement and real payments. Warehouses buy goods;
workshops consume inputs; farms/businesses use tools; village customers purchase
needed food. Toolkits and storage fittings are abstract business goods. This is
not yet a full physical sword/armor/furniture shop catalog.

Urgent assessed food shortages give existing farms first access to qualified idle
help. Other existing vacancies compare urgency and funded demand. Mines,
quarries/sandpits, logging and marker-backed workshops/warehouses compare viable
startup proposals before receiving one protected grant. Fishing and livestock
still need the combined competition connection. Existing workers are not forcibly moved, and selection creates no
money or supplies.

Minerals, stone and sand have finite saved reserves; observing them again or
restarting never refills them. Player removal loses the remaining share behind
that coordinate. Trees and fish use separately bounded renewable stocks. Unknown
capture history can pause work safely. Admin `/civ debug recover-ore` and
`/civ debug recover-forest` provide scoped review/confirmation workflows; read
[RESOURCE_REVIEW.md](RESOURCE_REVIEW.md) before accepting uncertainty. Recovery
does not erase protection or restore lost reserves.

## Settings worth knowing

`config.yml` controls the population calendar (`population.millis-per-month`). `economy.yml`
contains food cadence (`pace.seconds-per-period`), industry work cadence
(`industry-runtime.real-millis-per-period`), resources, recipes, grants, training,
tool upkeep and `business-allocation` priorities. Defaults are prototypes, not
locked balance. Read the relevant section comments before editing; some saved
terms intentionally remain fixed. Avoid speeding up an established save by
blindly changing several clocks. Use a separate disposable test for accelerated
experiments. Restart normally after edits.

Server downtime and absent worlds pause progress. Unloaded chunks are not proof
of death or destruction. The plugin does not force-load terrain for simulation.

## Human acceptance checklist — still to do

- [ ] Recognition settles on one persistent village and records its residents.
- [ ] Donation, emerald sale and a feasible bargain/barter change inventory exactly
      once and show a final confirmation or a clear refusal.
- [ ] A suitable village develops an actual paid production chain; report reasons
      explain any waiting stage. A contrasting village need not get the same mix.
- [ ] Livestock protection, products and bucket exchange work with your server's
      plugins, without duplicate drops or taking protected animals.
- [ ] Normal stop/restart preserves residents, business ownership, goods, funds and
      finite reserves. Returning to an absent world does not add offline decades.
- [ ] No unexpected block changes, chunk-loading spikes or repeating console errors.

Save the server log, build/commit identifier and the relevant report when something
fails. Describe what you expected and what actually happened. Do not publish logs
containing private addresses or player information.

Phase 3 remains incomplete: combined startup competition, broader useful crafts,
supported bulk-editor capture and longer specialization/balance acceptance remain.
Physical construction, farm expansion, tree felling, mine shafts and excavation
belong to Phase 4. This version can perform data-backed jobs using existing world
evidence, but it does not yet build or dig visible workplaces.
