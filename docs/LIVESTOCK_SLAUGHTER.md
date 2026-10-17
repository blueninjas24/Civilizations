# Controlled livestock slaughter

Village livestock businesses can turn surplus mature animals into saved food.
The village pays for that food through the existing real public-food purchasing
system; there is no new money or imaginary customer. Meat is abstract food in
this batch. Physical player meat trading and leather recipes are separate work.

The existing editable `livestock.cow` and `livestock.chicken` terms set the breeding
reserve, meat units per animal and maximum slaughter per period. Defaults retain
at least two adults, permit at most one intentional slaughter per earned period,
and produce four cow food units or two chicken food units. Milk/egg care and
births use work first. Slaughter must fit actual **unused** work from the current
completed industry period and actual free storage. It cannot borrow future work,
use an old queued period, or harvest an animal merely because the herd is large.
Current qualified staff and normal business operating rules still apply.

`livestock-slaughter` controls dispatch cadence and positive-evidence freshness:

```yaml
livestock-slaughter:
  enabled: true
  interval-ticks: 100
  candidates-per-review: 16
  evidence-max-age-millis: 5000
```

Installed files without this section remain disabled. Files are never rewritten.
Normal livestock work and the trusted native-loss capture must also be enabled.
The controller examines a bounded page of saved animal IDs, using already-loaded
bodies only. It does not load chunks, replace missing animals or guess deaths.
The body must match its saved world/species, be adult and inside the farm's
working radius. Named or leashed bodies are excluded from intentional slaughter.
Both original herd members and positively activated mature offspring can qualify.

## Exact accounting and native behavior

Schema90 adds one empty `livestock_slaughter` table and its indexes. Every older
animal, business, balance and history row is retained. An exact animal UUID,
business, current period, configured yield and policy are saved before any native
effect. Preparing a permit produces no food and removes no head. A missing food
slot is added empty with the permit, preserving cash/capacity/opening terms and
existing weights. Pending food reserves occupy actual stockroom space.

Only the exact intentional native death suppresses vanilla ground drops and XP.
After the native event finishes, a cancelled death retains its living body and
earns no goods. A confirmed death enters the existing retry/shutdown loss queue.
The actual loss and once-only food credit commit together; failed saves roll back
both and retain the exact observation for retry. Replays do not add another head
loss or food credit. Ordinary player/mob kills keep normal vanilla drops, reduce
the herd, and receive no duplicate business food.

While a permit awaits an outcome, that herd pauses its work/clock and retains the
food-space reservation. Other herds need not wait for an unexecuted permit. The
existing global physical-loss capture fence still protects all herd production
while an actual death observation is awaiting a reliable save.

## Interruptions and diagnostics

Restart preserves unresolved permits and reservations. Missing/unloaded bodies
alone never settle them or create meat. A newly confirmed living body can be
rechecked; stale/invalid undispatched attempts are cancelled safely. If the
server crashes after a native effect but before its death observation is saved,
capture trust/review rules apply. An outcome that cannot be proven stays paused
for reviewed recovery; automatic missing-body replacement or presumed-death
recovery is deliberately absent. Do not delete permits or fabricate receipts.

`/civ debug observation` includes controlled-death counts/failures and the saved
pending-permit count, including after restart. SQL history retains exact targets,
periods, quantities and cancellation reasons. This is a prototype balance loop;
playtesting must establish whether feeding, work and stockroom limits feel right.

## Verification

Windows clean verify passed 1,837 unit and 2 packaged tests on October17.
After two additional regressions (exclude manual budgets; close during startup),
final affected verify/package passed 57 unit and 2 packaged tests. Eleven
Slaughter tests cover exact-once confirmation, ordinary/cancelled deaths,
reserves/work/storage, positive evidence, absent worlds, rollback, offspring,
restart and populated schema89 migration preservation.

The final packaged Paper26.2 build129 probe passed at 17:39:36 EDT using three
exact disposable cow identities. A cancelled death retained its body/no food;
one controlled death credited four food once and no native drops/XP; an ordinary
kill kept normal loot/no duplicate food. Main fixture: 152 existing tables retained,
148 exact, four routine metadata changes, no aging, integrity/FKs clean. Settings
restored, server stopped, helper archived, backups kept. Human server untouched.
