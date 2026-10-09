# Workshop recipes and connected industry accounting

This Phase 3 batch implements the processing part of a production chain. A trusted
database owner can run a recipe at an already registered, appropriately staffed
business using its delivered, owned stock. It does not discover or automatically
found a blacksmith, create mines, move villagers to workstations, or construct
buildings. Parsing recipes does not schedule them. Automatic mine/workshop adapters
and demand-driven operation remain next; ordinary villages still automatically run
only the farm prototype.

## Editable recipe catalog

New economy.yml files include a commented prototype:

```yaml
recipes:
  smith_tools:
    industry: blacksmith
    inputs:
      iron: 2
      coal: 1
    outputs:
      tools: 1
    batches-per-worker: 2
    site-batch-capacity: 8
```

Quantities are abstract commodity units, not an item-perfect smelting/trading
recipe. These defaults use the user's approved configurable economy authority;
they are not final balance. One saved living local employee can process at most two
batches per requested period: four iron and two coal become two tool units. Founders
and owners do not supply free labor unless actually employed. Wrong-industry firms
are rejected. Dead/moved employees immediately stop contributing.

Admins may define up to 32 named recipes, each with bounded positive input/output
units and work/site limits. Invalid, incomplete or misspelled sections fail startup
before DB access. A wholly absent section supplies the documented backend prototype;
an explicitly empty recipes section supplies none. Neither enables an automatic
workshop scheduler. Existing files are never rewritten. Old completed runs retain
their recipe, per-batch terms, requested work, actual workforce and material flows.
Reusing an operation ID with changed terms fails instead of reinterpreting history.

Tools live in separate business stores. A store must explicitly support iron,
coal and tools with positive space weights; unsupported goods fail rather than
silently extending/resetting a store. The initial mining default remains coal plus
stone; iron evidence/yields require an actual future iron discovery adapter or an
explicit trusted test setup. This batch does not add tools to village opening
reserves, mint iron, grant currency or expose item sales to players.

## Limits, storage and history

Complete batch count is the minimum of requested batches, living assigned workers
times work rate, site capacity, every delivered input's available quantity, and
storage growth capacity. Inputs in paid pending shipments are not usable yet;
their reserved incoming space also remains unavailable for output.

Consuming inputs frees their actual space before outputs occupy it. A full store
can process a recipe that shrinks or preserves occupied space. A recipe with larger
weighted output is capped by its **net** space growth. Inputs and outputs may share
a commodity; all original inputs must exist before processing and no intermediate
material is invented or inconvenient byproduct discarded.

One production plan per business/period shares the existing mining work marker.
Distinct operation IDs, alternate recipes or stale periods cannot repeatedly use
the same work. A zero-work result also saves its decision; new materials arriving
later cannot reopen that completed period. Exact operation retry returns its original
result, even after workers or inventories change. This is an explicit caller-owned
period API, not a new calendar or an absence/downtime scheduler.

Schema 31 adds recipe_runs and recipe_flows with versioned retained material
history. Input debits, outputs, receipt, work marker and all history commit together.
Any write failure rolls them all back. No old runs are invented. Existing deposits,
stock, firms, population and funds are unchanged by migration. Older binaries reject
newer schemas; downgrade requires a consistent pre-upgrade backup.

Admin `/civ debug businesses <town UUID>` now includes the latest recorded industrial
production explanation for a cached business. It reads an immutable snapshot, with
the existing bounded refresh and 20-firm display. Each production summary is capped
at 768 characters; full recipe/flow history remains saved. Empty-cache text was
corrected to acknowledge that enabled funded farms can already start automatically.

## Validation boundaries

Temporary-DB tests exercise actual finite extraction, paid mine-to-warehouse-to-
blacksmith purchases, delayed delivery, input processing and explicitly established
test machinery-maintenance demand over 240 cycles, including restart. They conserve
all money, minerals, rock and tool production/use. This is a causal test fixture,
not a live NPC shopping or maintenance system. Seeded domain tests check 10000
plans for material/work/storage invariants and repeatable explanations.

Manual WorkshopProductionProbe is outside the production JAR. It accepts only the
marked localhost disposable server without players, uses a separate randomly named
database, and owns SQL on a dedicated thread. Its industries/deposits/sites are
explicit synthetic test setup. It validates packaged processing, capital, replay,
dead-worker exclusion, restart and cached diagnostics on Paper without altering any
world blocks or entities. It does not prove automatic resource/site recognition,
mine operation, client experience, balance or business turnover.
