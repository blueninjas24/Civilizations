# Civilizations — implementation status and handoff

## Current — Phase4 construction foundation; paid supplies and earned public work

Branch: codex/phase4-construction-foundation; main untouched. Phase4 stays25%,
4/16 complete checklist items,1/4 complete groups. Phase3 prototype is complete;
human survival testing remains deferred. No deployment to the user's server.

Implemented and verified:
- Public construction work now runs inside actual earned food periods. Only
  eligible unemployed non-owner adults with unspent time can work; farm employees
  and students are excluded. Legacy all-adult farm output cannot double as builders.
  Food shortage, absent worlds, incomplete supplies and saved protection pause
  work. First earned stage effort escrows its frozen material cost once; later
  stages wait for prior physical acknowledgements. Work, food checkpoint and
  material escrow roll back together. This currently covers public paid-supply
  projects only; private staff diversion is still ahead.
- A public economic journal gate now requires full earned stage work and backed
  escrow, fresh exact-project evidence and earlier acknowledgements. This grants
  neither world-write authority nor completed building/housing capacity.
- Normal simulation pulses now settle up to16 ready prepaid cargo receipts using
  existing earned clocks. Unknown/absent worlds and paused simulation cannot
  deliver; new spending stays disabled in the delivery-only adapter. A late batch
  failure rolls back every arrival. /civ debug construction is read-only.
- Construction supplier purchases atomically debit real private/village money and
  supplier stock, preserving protected operating/food reserves and current fed
  population checks. Prepaid cargo uses existing owner clocks and shared local
  haul limits; only earned delivery makes it available at the project. Exact
  replay/restart and late SQL failures do not duplicate money or goods. Editable
  supplies prices use defaults for old files. This is a backend API, not automatic
  project founding, orders or block placement. Paid cargo is retained; cancellation
  and automatic order selection are still ahead; stage escrow now exists for public work.
- Three original editable plans: path, storehouse and four-bed home; strict native
  palettes and all eight rotations/mirrors, read-only bounded admin previews.
- Frozen world/village-linked drafts, deterministic road-adjacent site search,
  saved plot exclusions and conservative buffered clear-land screening.
- Durable ordered block intents, authored-log forestry exclusions and a writer
  obtainable only inside marked, explicitly bounded disposable test servers.
- Editable protected village/private spending eligibility and temporary labor
  proposals. Quotes do not spend; private labor remains proposed, while public
  work is now booked in the real owner transaction described above.
- Exact-draft site reservations refuse overlap. Protected empty player plots and
  later protections block material debits; unused releases retain site history.
- Whole-household housing proposals reuse confirmed village-owned vacancies first,
  retain pending projects, pause on incomplete surveys/absent worlds, refuse plans
  too small for a whole family and obey an editable proposal cap. This is a pure
  planner, not an occupancy or live discovery adapter. Authored housing capacity
  requires complete distinct bed pairs with matching anchors/facing.
- Real owned private-business materials move into recorded onsite stockpiles.
  Unused goods can return exactly once, waiting intact if source storage is full.
  Only after full return can an unstarted site release; any prepared block intent
  requires reviewed physical recovery. Original transfer receipts remain history;
  available() reports current held goods. No cash or materials are created.
- Approved sheep/wool extension reuses ownership/protection, actual grants/feed/
  labor/storage, breeding reserves, age/birth/presence and deliberate-death rules.
  Player/dispenser shearing is blocked only for positively saved village sheep.
  Old cow/chicken receipt defaults and data are preserved; milk/egg retail excludes
  sheep. Wool is colour-neutral; old files missing only sheep use editable defaults.

Validation: latest migrant-work guard Wrapper clean verify27 unit +3 packaged
checks passed39.336s, zero failures/errors/skips; same-world migration preserves
source work and does not block destination food. Earlier86+3
checks passed59.632s, zero failures/errors/skips; preceding public-work migration/
forest/study batch passed78+3 in51.257s. Actual disposable Paper completed a paid
public path: real protected supplier payment, earned delivery, five saved work
units over three food periods, once-only stage escrow, five native blocks, exact
retries/DB-reopen recovery, changed-block preservation and no unloaded-target
force-load. All original cells restored; stopped fixture DBs at107 healthy/FK-clean.
Earlier real Paper verified sheep, native templates and a120-block home over two
JVM runs. Ordinary construction remains disabled; those were explicitly bounded
private tests. Hosted85419c6 passed full Windows/Linux; later commits need their
own hosted result.
See DEVLOG.md and CONSTRUCTION.md for detailed evidence and earlier checkpoints.

Schema100 drafts,101 journal,102 held stock,103 preserved species-constraint
upgrade,104 site reservations,105 material returns,106 supplier orders,107 public work/stage escrow. No automatic DB resets.
Migration rollback and preservation tests use temporary databases. Back up before
upgrading; older plugin versions refuse newer schemas.

Remaining: live housing demand/vacancy/occupancy adapters, automatic procurement
selection/scheduling, private employee labor diversion, later-stage/native reconciliation, current-write production protections/editor compatibility,
completed building/housing activation, vacancy reuse, upgrades/maintenance and
full autonomous growth acceptance. The paid path test verified funded public work,
not home occupancy or ordinary autonomous construction. Normal gameplay still cannot place construction blocks.
Player-built buildings now require explicit donation consent; lack of a protection
sign is not permission. User also approved repossession with real capacity loss
and explicit building exclusions. These are recorded design decisions, not built
controls. Live donation/plot/reclamation and vacancy adapters remain ahead; proposed
editable boundaries and typed use must not claim later-phase civic mechanics.
Feature pushes are
permitted; never merge/push main. Hosted current candidate status must be checked
by commit; local passing tests are not a claim of hosted Windows/Linux green.
Workflow concurrency now lets an existing run finish across new backup pushes,
so frequent commits no longer continually cancel the slower Windows checks.

## Current - Phase3 prototype complete,100%,4/4 milestone groups

All63 current P3 checklist entries are verified: core4/4, resources39/39,
lifecycle15/15, specialization/acceptance5/5. The initial industry catalogue,
hybrid farm/livestock/bakery/craft chains, qualified funded competition and
succession/resale, conditional multi-village ownership, finite/renewable resource
rules and conservative supported-editor boundary are implemented. Physical growth
remains Phase4; regional goods/routes remain Phase5. Canonical roadmap unchanged.

Validation: repository `.\mvnw.cmd clean verify` passed1997 unit +3 packaged checks,
zero failures/errors/skips,15:11 at16:25:34ET. A final review then closed off-world
estate funding even with new remote purchases disabled, and automatic reviews skip
invalid estates while retaining legitimate same-settlement legacy accounts.
Affected36 unit cases pass across focused independent-source runs; final repository
`-Dtest=CrossVillageBusinessTest verify` passed4 ownership +3 packaged checks,
22.121s at16:26:23ET, rebuilding the current artifact. No redundant second full run.

Actual Paper mixed8-business/40-pulse, unloaded workplace, paid local trade/craft
storage/bakery ingredient, capacity/cash, absence/restart checks passed. Ordinary
pinned-editor bootstrap/fresh generation/edit/undo checks and a real process restart
passed. All24 old terrain records remained while their previous authority was
revoked. Separate DB integrity/FK/schema99 audits passed; no migration this batch.
All disposable servers stopped/helpers archived/ports free. Main remainsc12f7f6;
human server/world/database/config untouched. See PHASE3_ACCEPTANCE.md for workload
measurements and explicit compatibility boundaries.

Changed areas: ownership/funding/runtime, editable craft recipes, optional editor
startup/command/tool guard and bootstrap quarantine, scenario/native helpers,
README/playtest/progress/decisions/devlog. Human-client survival balance and this
server's plugin mix still need playtesting; no human test is claimed. No current
P3 design question remains under the user's delegated recommendations. Next phase
is the protected physical-growth foundation; no Phase4 block editing has started.
Final feature backup/hosted CI status is recorded in the closing devlog entry.

Current JAR: `target/civilizations-0.0.1-SNAPSHOT.jar`, SHA256
`399effd646de21b5a684837efcb8cf5bde3f9a8075578be835efd260268088be`.
Final combined saved reports cover1998 unit +3 packaged cases across the full run
and focused follow-up, with zero failures/errors/skips. Helpers, JUnit and external
WorldEdit libraries are excluded from the production artifact; bundled new-file
recipe/purchase defaults were checked. Old installed server files remain unchanged.

## Completion candidate - connected industry and guarded editor runtime verified

Ordinary startup now installs the pinned synchronous WorldEdit adapter. Every
editor bootstrap quarantines earlier resource history (including potential queued
onLoad deletion), rotates capture epochs and permits only actual fresh generation.
Unreviewed commands/tools, off-thread/unloaded writes, disable/reload and unsupported
editors remain fenced. The exact supported boundary is in WORLDEDIT_CAPTURE.md.
No schema change, trust reset, resource refill or deletion of old assets/history.

The new ten-year PopulationSession scenario passed with genuine food deficits,
finite production, ordinary elder mortality, a sufficiently funded qualified
same-world branch buyer, real sale/estate money, absence and midpoint restart.
Existing coal/stone/neither and qualified/unqualified family scenarios provide the
contrasting outcomes. The targeted scenario Wrapper verify passed1 unit +3 packaged
checks in41.039s. Earlier focused editor/capture checks passed; final full regression
is running before accepting the whole phase.

Actual Paper26.2-129/Java25 mixed run passed:8 real saved businesses,40 timed pulses,
finite coal/stone, renewable logging/fish, real paid warehouse inputs, craft fitting
production/customer installation, bakery grain processing, bounded storage and
conserved cash. All8 workplace chunks remained unloaded. Average323ms/max1203ms
awaited SQL-owner pulse wall time during concurrent full Maven build; production
defaults remain30 seconds,8 firms/pulse. This is not Paper tick time or a scale claim.

Separate actual pinned-editor run passed ordinary startup and positive fresh chunk
generation under sticky historical distrust, raw/set/removal/undo exclusions and
unsafe-operation fences. Five fixture AIR blocks restored/ticket released. All4
final fixture databases passed schema99/integrity/foreign-key audit; both servers
stopped, helpers archived outside plugins, ports free. Human server/main untouched.
See PHASE3_ACCEPTANCE.md. Final checklist/phase acceptance waits for the full build;
the prior milestone report remains88.7% until that result is known. ac9aac4 has
confirmed successful hosted Windows/Linux builds; this newer batch is not yet
claimed as passing hosted CI.

## Latest verified batch - qualified ownership across villages and craft defaults

The user approved same-world cross-village acquisition and delegated remaining P3
prototype decisions. Qualified buyers can now fund an existing branch with real
owned business cash/eligible estate money. Local jobs, stock, accounts, deliveries
and founding history remain; acquisition creates no shipping or extra worker job.
Runtime/staffing/competition recognize valid remote ownership. Qualified family
can inherit a saved branch. Old files opt in with the new funding key; new files
enable it. Different-world/disabled purchases, failed payments, replay and family
succession are covered by three new temporary-database cases. No schema change.

The general craftsman now bundles two editable fitting recipes for actual funded
business storage customers. Household furnishings await actual construction demand
in Phase4; no fictional household spending is added to complete a catalogue.
See BUSINESS_PORTFOLIOS.md and PROCESSING_WORKSHOPS.md. The initial craft entry is
complete; combined catalogue/native and editor certification gates remain.

Affected178 unit cases pass across the focused run and targeted corrections/reruns;
the successful reruns also passed all3 packaged integration checks. The first run
caught a missing fixture treasury and an outdated legacy-recipe test expectation;
both were fixed. Cross-village rerun15.358s; defaults rerun16.900s. Previous source
commit3b29bd3 has confirmed successful Windows/Linux hosted checks. This is not yet
a full-suite result for the new batch. P3 is88.7% partial,2/4 whole groups.
Main and the human server remain untouched. Combined acceptance work continues.

## Latest verified follow-up - retain accepted shutdown observations

Hosted Linux found one PopulationSessionTest error after scoped live wiring: public
readiness correctly became false on shutdown, but an already accepted survey then
refused to drain. Reproduced that class locally (23 passed,1 error). Owner-queue
observation/demand reads now retain the accepted-work contract while rejecting real
capture gaps. Later queued mutations still filter saved evidence; paid startup/work
guards retain their pending-mutation barrier. Current capture continuity is separate
from public readiness in owner callbacks. New submissions stay rejected on close.

Final focused Wrapper clean verify passed30 unit +3 packaged checks, zero failures/
errors/skips,26.963s, including the existing actual drain/removal/restart regression.
No duplicate full local suite or new native run. Hosted builds must rerun on the
corrected feature commit; previous Linux failure is not reported as passing.
No migration; main and human server untouched. P3 remains88.1% partial,2/4 groups.

Remaining editor audit found the pinned WorldEdit runtime can process queued chunk
deletions during onLoad, outside the EditSession block hook. Ordinary editor presence
therefore remains fenced until lifecycle and non-extent operations are covered; a
successful set/undo probe alone cannot certify full editor compatibility. Nothing
was deleted or executed by this audit; it inspected the checksum-verified artifact.

A product question is pending for the remaining rare multi-settlement-firm gate:
may qualified owners buy an existing business in another village in the same world,
using approved real business/estate funding while local staff/goods/accounts remain
separate? No remote purchases or automatic goods transport are implemented or assumed.

## Latest verified batch - live scoped surveys and industry startup

Live ore/quarry scans now preflight otherwise-untrusted chunks on the SQL owner,
retain the target while that bounded query runs, and scan only currently permitted
loaded terrain. Old/absent/unavailable chunks stay skipped. Session survey/demand/
startup callbacks distinguish healthy current capture from historical trust; SQL
current-coordinate and paid-opening checks remain authoritative. Forestry filters
entire current frames and resumes prospective-owner study only from the actual
complete cached frame, never the saved course's timestamp-zero identity placeholder.
Unavailable/uncertified evidence waits. Diagnostics explain scoped availability.

Focused clean verify passed47 unit +3 packaged checks, zero failures/errors/skips,
44.680s. Added actual shared paid mine investment and scoped qualified logging
founding cases, plus session preflight/time/diagnostics checks. Existing mine/quarry/
forestry startup and study regressions passed. Read-only peer review found only a
diagnostic wording issue; corrected it and refreshed package successfully,17.128s,
without repeating tests.

Actual fresh no-editor Paper26.2-129 passed the scheduled scoped ore observer:
real new-generation events permit a fresh chunk while old loaded terrain remains
blocked under sticky-untrusted global history; one protected coal coordinate stayed
excluded and exactly one unclaimed opportunity retained its real capture timestamp.
No business, source reserve or cash created. Private DB retains two synthetic people,
one exclusion and18 generation certificates; main DB34 certificates, no people/firms.
Neighbor generation legitimately produces additional positive events. Both DBs are
schema99, integrity/FKs clean, current epochs healthy; private ore global trust stays
false. Four original AIR blocks restored, both fixture tickets released, helper
archived, server stopped and worlds retained. Human server/main untouched.

No migration. Ordinary WorldEdit presence still fences capture: its bootstrap/late-
enable/shutdown integration remains unfinished. Scoped new-terrain support is now
wired and verified separately; no bulk-editor milestone is claimed early. P3 remains
88.1% partial,2/4 whole groups. Next: complete optional editor lifecycle safely, then
remaining useful craft breadth and combined long-run/native acceptance.

## Latest verified batch - scoped current opportunities and evidence timing

Current mineral/quarry reviews now return only candidate coordinates with current
generation proof when global history is untrusted. Older sightings remain stored;
neighboring fresh frames cannot authorize them. Positive survey admission requires
complete coverage; empty frames can safely remove candidates without establishing
resources. Quarry review preserves the actual capture time of eligible frames.

Tightened chronology: source evidence must follow its chunk's generation evidence.
A later certificate cannot bless an older survey or saved mine. Saved mineral opening
time and original forest workplace time remain the cutoff for scoped production;
explicit recovery is still required for older saved resources. No global trust reset,
resource refill, cash transfer or recovery approval is introduced.

Focused clean verify passed37 unit +3 packaged checks, zero failures/errors/skips,
28.796s. Two new opportunity cases cover old/new mixed terrain, capture times,
protection and empty frames. One added old-source chronology case passed separately,
10.472s; no already-passing suite was repeated. Existing mineral/quarry validation,
current-frame, paid opening/work cases passed. No new native run or migration claimed.
Live adapter/founder/editor lifecycle gates remain conservative and unfinished;
P3 stays88.1% partial,2/4 groups. Next: connect those gates using the verified SQL
eligibility rather than weakening historical trust. Human server/main untouched.

## Latest verified batch - complete scoped resource eligibility

Paid mineral discovery and new forest opening now accept complete current generated-
terrain coverage when global history is sticky-untrusted. Production requires every
saved source/support coordinate to belong to a certified chunk in the current healthy
epoch; partial coverage cannot unlock a vein or forest. Source/world identity, placed/
removed exclusions, paid startup, retained reserve/yield terms, replay and explicit
admin recovery remain authoritative. A new gap revokes scoped permission immediately
through the existing runtime barrier and persistently through epoch invalidation.

Three new temporary-database cases verify partial cross-chunk refusal/atomicity,
fully covered paid opening and extraction, no refill on replay, placed-ore refusal,
forest work, conserved cash and gap revocation without creating recovery approvals.
Initial focused run37 cases:36 passed; one old schema91 snapshot expected no schema99
table. Hosted Linux independently identified that and a schema93 snapshot equivalent.
Both fixtures now exclude the later table from predecessor comparisons and explicitly
assert it starts empty. Final focused clean verify passed7 unit +3 packaged checks,
21.777s; the second affected class passed3 cases,12.099s. The remaining resource/
recovery regressions passed in the initial run and were not repeated.

No migration. Main survey/review/founding and editor-presence guards remain conservative;
this batch completes SQL opening/work authorization, not automatic edited-world startup.
No further native server run claimed for these SQL-only changes. Human server/main
untouched. P3 stays88.1% partial,2/4 groups. Next: enforce coordinate coverage in current
opportunity reads, then connect live adapters and finish editor lifecycle coverage.

## Latest verified batch - actual new-generation capture

Registered a bounded Paper new-chunk observer. Only positive `isNewChunk()` events
after current capture is ready may enqueue immutable world/coordinate/time evidence;
ordinary loads, startup-before-readiness, pending writes, gaps, absent worlds,
changed mutation revisions and overflow leave terrain unknown. At most eight
generation saves are in flight; no retry/backfill or production chunk loads.
The SQL owner checks current per-kind epochs; ore/forest eligibility is independent.
Debug observation diagnostics show saved/pending/unknown counts for this run.

Focused Wrapper clean verify passed9 unit +3 packaged checks, zero failures/errors/
skips,21.450s. Two new session tests prove actual queue persistence, negative chunks,
absent worlds, pending writes, clean reopen, gaps, no old recertification and no
global trust reset. Fresh disposable Paper26.2-129 without WorldEdit passed real
generation, existing-load rejection, clean private-session restart and gap revocation.
Both main/private databases retained two certificates each, no people/businesses;
integrity/FKs clean. Main epochs healthy; deliberately gapped private epochs unhealthy.
Helper archived, server stopped, worlds retained; human server/main untouched.

No migration. Certificates are still evidence only: discovery/production paths
keep their current history gates pending full coordinate-based enforcement. Optional
editor bootstrap/shutdown also remains conservative. P3 stays88.1% partial,2/4 groups.

## Latest verified batch - optional WorldEdit write adapter

Added an explicit-registration adapter for WorldEdit Bukkit7.4.4 (Minecraft26.2,
Java25), with provided-only API dependency and optional load ordering. It observes
actual loaded Bukkit block states before/after synchronous editor writes, including
raw writes and undo. Finite ore/stone/sand and tree changes use the pre-write save
barrier; unchanged repeats add nothing. Off-thread, unloaded, ambiguous, detached
or incomplete capture fences both resource histories. It never cancels editor work.

Focused Wrapper clean verify passed8 unit +3 packaged integration checks,21.105s;
the tightened live-world read then packaged successfully,17.110s. Packaging verifies
WorldEdit/EngineHub/Guava are not bundled. Actual checksum-verified WorldEdit7.4.4
and Paper26.2-129 in a fresh marked localhost fixture passed ore removal (only its
ten-batch share), raw iron placement, finite stone placement/repeat, tree placement/
undo, unchanged coal and unsupported off-thread fencing. No free goods, money or
terrain certificates. Both schema99 databases passed integrity/FK checks; all five
original AIR blocks restored, ticket released, helper archived, server stopped.

Main's existing editor-presence guard is deliberately unchanged and this adapter
is not yet registered by normal startup: complete lifecycle/generation integration
is still required before claiming supported editor discovery. No migration or human
server changes. P3 remains88.1% partial,2/4 whole groups. Next: bounded generation
event capture and lifecycle-safe use of scoped certificates. No new design question.

## Latest verified batch - scoped terrain evidence and external-write barrier

Recorded the approved edited-world rule. Schema99 adds one initially empty
`generated_resource_chunks` table; no historical terrain is backfilled. Certificates
bind exact world/chunk/resource kind to current healthy capture epochs. Negative
coordinates use floor division; replay retains the first timestamp, and a later gap
revokes coverage without permitting the same old chunk to certify itself anew.
Global history, placement exclusions, resource balances and recovery rights stay intact.
This backend is not yet connected to generation events or automatic discovery.

External adapters now have a bounded pre-write reservation that holds ore/forest
readiness until their accepted owner-queue saves finish. Explicit unchanged outcomes
write nothing; exceptions, unconfirmed outcomes, failed handoffs and interrupted
shutdown remain fenced. Reservation and shutdown ordering are synchronized. No Paper
object crosses the owner queue; no current native listener/guard is relaxed.

Focused first checks passed49 units,25.847s. Final Wrapper clean verify/package
passed31 units +2 packaged integrations, zero failures/errors/skips,27.072s.
Eight new cases cover generated-terrain persistence/epoch gaps/rollback, complete
schema98 preservation/failure/retry and external-write outcomes/actual session
persistence/shutdown. Existing migration/recovery/startup regressions pass.
No repeated complete local suite; hosted checks follow feature push.

Actual isolated Paper26.2-129 upgraded98->99 and passed the ordinary paid alternate
craft startup/production/customer/replay/absence check. All173 predecessor tables
preserved:168 exact, five routine clock/capture tables changed, no resident aging.
The new table remains empty; no terrain was certified. Integrity/FKs clean, original
one-block fixture restored, ticket released, server stopped, original port restored,
helper archived and backups/audit retained. Human server/main untouched.

P3 stays88.1% partial,2/4 whole groups: this is verified foundation, not certified
WorldEdit support. Next is the optional actual edit hook and complete lifecycle/
generation wiring; older unsupported editor presence still fences discovery.
No new design answer required.

## Latest - optional editor provenance decision pending

WorldEdit integration research identified a necessary trust-scope decision before
relaxing the existing conservative presence guard. The user has been asked whether
previously unverified edited terrain stays blocked while newly generated terrain
under uninterrupted capture can become eligible. Existing source-by-source admin
recovery is unchanged. No answer is assumed; no editor dependency, installation,
new certificate or guard exemption has been added. The pending question and primary
API references are recorded in DECISIONS_AND_OPEN_QUESTIONS.md. This does not block
playing the current supported non-editor setup. P3 remains88.1% partial,2/4 groups.

## Latest verified batch - geography and family history change industry outcomes

Two seeded ten-year scenarios now run actual monthly demographic aging/mortality,
bounded industry work, ordinary family-first succession, qualification-based hiring,
real buyer review, saved sale deadlines and retained closure. The same seed42,
people and geology diverge only in saved heir mining experience: a qualified heir
keeps the mine; an unqualified family with no actual funded qualified buyer cannot,
and its unsold business closes. The elderly fixture uses unmodified prototype
mortality; no death is forced. Sale deadline acceleration is fixture-only.

Actual mine work earns goods before the scenario. Close/reopen halfway retains
people/clock, resources, cash and work receipts without offline accrual. Immutable
founder, one grant, ownership/closure/job history, retained store/source and exact
99500 total village/business/estate cash remain. Closed work cannot consume its
reserves or assets. Foreign keys clean. No application, schema or human save changes.

Final focused Wrapper `-Dtest=IndustryHistoryScenarioTest clean verify` passed2
unit scenarios +2 packaged integrations, zero failures/errors/skips,50.234s.
Initial scenario run also passed2 cases,38.938s; a final assertion was narrowed to
the closed firm's actual assets so other businesses may legitimately keep working.
Combined with the earlier three geography counterfactuals, this verifies different
geography/history producing different industry outcomes without universal lists.
Final group advances2/5->3/5; equal group progress88.1% partial,2/4 whole groups.
The broad multi-year shortage/exceptional-firm and integrated mixed-catalog native/
profiling gates still remain open. Useful craft breadth/editor compatibility remain.
No new gameplay decision required. Main and human server untouched.

## Latest verified batch - one craftsman can use multiple editable recipes

Removed the one-recipe restriction for the general craftsman (32 total runtime
recipes remain the cap). Stable recipe IDs break ties. Current owned ingredients
and storage choose productive work first; paid incoming ingredients guide future
orders without becoming production inputs. A bounded complete warehouse review
can select an affordable alternate; otherwise the supported baseline establishes
normal input demand. Only saved supported products participate: old store catalogues
are never silently expanded. Other industry recipe limits remain unchanged.

First opening now checks actual fitting customers and each fitting recipe's paid
starter feasibility, rather than giving up on an unavailable first recipe. No new
occupation, free material, guaranteed sales, household wallet, default price or
construction permission. The default product remains useful storage fittings;
broader furniture/customer coverage remains open, so P3 stays83.1% partial.

Final focused Wrapper clean verify/package passed14 units +2 packaged integrations,
zero failures/errors/skips,26.998s. Earlier focused regression passed32 units +2
integrations,31.362s (craft training, configuration and bakery/livestock chain),
plus the initial13 craft/config cases,22.319s. Three new cases cover alternative
paid opening/production/customer/conservation/replay, real affordable warehouse
choice, pending-goods protection, retained catalogues and strict YAML parsing.
No whole local suite repeated. Schema remains98.

Final isolated Paper26.2-129 ordinary SQL-owner/Paper startup passed: without timber,
a configured alternate bought three real stone for900, manufactured one fitting
and sold it for2000 to the actual crowded warehouse. Exactly one500 startup fee;
20400 village cash +9100 business cash =29500 retained. Replay/absence, integrity/
FKs passed. One original AIR block restored, fixture ticket released, server stopped.
Main disposable DB173 tables:168 exact, only five normal clock/capture tables
changed; no resident aging or asset changes. Port restored/helper archived,
backups/audit retained. Human server and main untouched. No decision required.

## Latest verified batch - geography and finite-resource longer scenarios

Three temporary SQLite counterfactuals use the same seed, people, customers and
budget, changing only current geology: coal opens a mine, stone a quarry, neither
opens neither. Eighty real industry periods exercise paid warehouse/processing
flows, finite drawdown, a twenty-period absent-world interval and an offline
close/reopen. No extra startup fee, downtime backlog, revisit refill or invented
consumer/cash. The quarry exhausts; coal byproduct storage/customer constraints
can legitimately retain some reserve. Rediscovery preserves balances and goods.

Focused Wrapper `-Dtest=ResourceGeographyScenarioTest,ResourceInvestmentTest clean
verify` passed8 unit cases +2 packaged integrations, zero failures/errors/skips,
47.978s. An initial scenario assumption that every mine must fully exhaust was
incorrect: finite customer/storage limits legitimately stop extraction. Corrected
the fixture expectation rather than removing goods or changing the application.
No application, schema or human-server change. These are industry periods, not
population years; multi-year demographic/history and full mixed-catalog acceptance
remain open. P3 stays83.1% partial,2/4 whole groups. No new decision needed.

## Latest verified batch - livestock allocation completes basic competition

Separated a read-only qualified herd/budget preparation from ownership/grant
writes. At the existing actual Paper adoption handoff, a freshly delivered eligible
herd competes with current resource, marker and water proposals. No speculative
animals/ownership are saved or queued. Normal adoption revalidates every original
origin/protection/range/nearest-village/qualification/capture/funding condition
before claiming animals. Ownership, paid grant/store/job/herd and selection history/
cursor share one transaction; failure restores all, retry opens once.

Preserved the already-approved first-herd eligibility without an established buyer:
its funded value is zero, behind every positive funded alternative. No fake demand,
consumer, profit or new budget. Existing species/grant limits and protected food
funds remain. Other startup adapters retain their actual customer requirements.
Available known observations are compared; unseen future opportunities cannot be.
Existing farm-food scheduling and no-poaching/one-worker-job protections remain.

Focused Wrapper clean verify/package passed48 units +2 packaged integrations,
zero failures/errors/skips,32.505s. Three new temporary SQLite cases prove read-only
preparation/zero-demand compatibility, real funded-priority deferral without claims,
and complete late-audit rollback/retry. Existing adoption, protection, source/
marker/fishery selection and session regressions pass. No schema migration (98).
Refreshed the final JAR after diagnostic wording only: package with tests skipped
passed17.180s; the48+2 checks above were not unnecessarily repeated.

Actual isolated Paper26.2-129 passed ordinary repeated natural-animal adoption:
an actual fence blocked adoption; after removal only the two unnamed eligible cows
joined, while the named third stayed protected. One real2500 grant/debit (500 fee,
2000 business cash), one actual worker, durable exact ownership/herd and one zero-
demand selection record verified after private close/reopen. Three exact fixture
bodies removed,324 original block states verified restored, test ticket released.
Server stopped. Main disposable DB173 tables,165 exact, eight normal clock/capture/
resident tables changed with one normal age month and exact retained identities/
statuses/ages. Integrity/FKs/config/capture and private assets/audit clean. Original
port restored, helper archived, backups/audit retained. Human server/main untouched.

Basic lifecycle/competition15/15 and bounded viable opportunity selection now pass.
Core4/4, resources36/39, final specialization/acceptance2/5: equal group weighting
83.1% partial P3,2/4 whole groups. This is not full Phase3 or human/long-run acceptance.
Remaining resources: useful craft breadth, supported bulk-editor capture, broad
catalog acceptance; remaining final entries: contrasting geography/history,
seeded multi-year scenarios and integrated native profiling/acceptance.
Latest full hosted checks follow push. No new design decision required.

## Latest verified batch - fisheries join shared startup allocation

A qualified funded water proposal now competes with resource and marker startups.
Its stable selection key identifies the observed water footprint; actual existing
bank-derived business/site UUID rules are unchanged. A winner waits for the normal
Paper water/shoreline proof. Final acceptance compares again, then revalidates and
commits the grant, first job, empty stock, bounded habitat and selection audit/cursor
in one transaction. Failure rolls back all; exact existing startup replay does not
rerank or add an audit. No physical construction, docks, resource refill or new money.

Kept public food customers independent of the private automatic-input-purchase
switch: explicitly disabled private purchases contribute no private demand while
public funds/assessment checks remain. Enabled incomplete reviews still defer.
This also applies to shared existing-business staffing. Fishery acceptance retains
valid saved/headless food-assessment support instead of requiring a separate
running food engine; actual current population/backlog/failure guards remain.

Focused clean Wrapper verify/package passed56 units +2 packaged integrations,
zero failures/errors/skips,37.570s. Three new temporary-SQLite cases cover real
water/bank startup with private buying disabled, full late-audit rollback/retry,
and funded public-food-driven hiring with private purchases off. Two ran in the
main verify; the last additional hiring case passed separately in10.615s. Existing
fishery, mixed resource/marker selection, competition and session regressions passed.
Initial compile found a missing charset import; fixed before verification.

Actual isolated Paper26.2-129 passed ordinary water observation, qualified automatic
shoreline startup, one paid selection/grant, timed catches/earned experience,
same-habitat bank recovery, absent-world pause and restart. No block/entity edits;
fixture-only ticket removed, server stopped. Main disposable remains schema98:
173 prior tables,163 exact, ten ordinary clock/capture/same-field observation
tables changed; one normal age month, identical resident identities/status and
exact corresponding ages. Integrity/FKs/config/capture clean; private grant/job/
habitat/selection/history and9500 conserved remaining cash verified. Original port
restored, helper archived, backups/audit retained. Native helper's old schema70
assertion updated to98; an initial private-class compile reference was corrected
before any server setup, without exposing application internals.

No migration or human-server/main change. Full hosted verification follows push.
P3 remains76.4% partial,1/4 whole groups; livestock startup allocation and broader
specialization acceptance remain. No new design answer required.

## Latest verified batch - marker workplaces share protected startup selection

Connected the four existing qualified marker adapters (blacksmith, warehouse,
bakery, craftsman) to resource startup comparison. Up to one proposal per adapter
joins the bounded mineral/quarry/logging choices, valued against actual funded
unmet needs after the proposed public startup debit. Recipes contribute their
prepared outputs; warehouses their admitted catalog. No new grant allowance,
skills, sources, goods, course enrollment, property rights or Paper chunk loading.

A selected provisional marker waits for its existing main-thread loaded point
check. Acceptance compares again on the SQL owner; normal current marker, owner,
inputs, budget and capture checks remain. All four existing transactions now
compose the selection audit with the paid grant/first job/store/cargo. A mismatched
actual industry/business refuses the audit. Exact accepted replay preserves its
existing receipt without another ranking or cursor advance. Existing schema98
selection tables admit these industry kinds already; no migration/data rewrite.

Focused clean Wrapper verify/package passed59 units +2 packaged integrations,
zero failures/errors/skips,58.457s (latest log). Added3 real-workshop temporary DB
cases,1 mixed marker/resource adapter comparison and an additional shared-enabled
variant of the actual PopulationSession training/startup scenario. They prove
waiting/no reservations, missing proof refusal, late-audit full rollback/retry,
one grant/audit/replay, real SQL-owner wiring and an actual resource winning after
a higher-priority marker proposal is withdrawn. Existing four adapter suites pass.
Two initial compile checks found constructor punctuation and a missing Set import;
fixed before verification. No passing claim for those failed compile attempts.

Actual isolated Paper26.2-129 passed: ordinary timed supply-backed training,
Paper workshop startup review/point confirmation, real paid founding, marker
removal/wrong replacement/restoration, absent-world pause and restart. One shared
blacksmith selection/audit survived, money29500 after the500 actual startup expense,
two actual jobs, one starter cargo and20 practice-material spending entries.
The original AIR block restored; fixture-only ticket removed; server stopped.
Main disposable DB remains98 with173 tables,166 exact; seven changed tables contain
normal clocks/capture and same-field reobservation metadata, zero aging. Initial
audit conservatively refused the farm observation changes; read-only inspection
proved only an old partial->complete rescan and fresh timestamps, with identical
fields/grain counts/identities. Strict recheck passed integrity/FKs/config/capture
and private business invariants. Failed initial audit and final proof retained.
Port restored/helper archived. Human server/main unchanged.

Previous b0cf334 full hosted verification is now confirmed SUCCESS on Windows and
Linux (run39181828920). Latest marker work gets a new full hosted run after push.
P3 remains76.4% partial,1/4 whole groups: fishing/livestock startup priority and
broader specialization acceptance still remain. No new gameplay decision.

## October18, 20:40 EDT - full-regression findings corrected

The full local Wrapper clean verify ran1951 units:1928 passed,23 failed, no errors
or skips,13:52 at20:37:36. Twenty-two failures were stale predecessor snapshot
expectations after additive schemas95-98; one was a real legacy hiring regression
introduced by the new shared student reservation. This run failed and did not
reach packaging/integration tests; it is not recorded as a green full suite.

Updated17 migration fixture suites. The common helper requires all seven additive
tables to exist and be empty before removing them from an earlier-schema snapshot;
all predecessor row/constraint/rollback assertions remain. The warehouse fixture
takes a current snapshot before rewinding to62, so its current-only ore epoch is
explicitly distinguished: it cannot survive a simulated schema that never had it,
and upgrading must create an empty epoch table. All other saved assets stay equal.
No application migration, production-data reset or relaxed runtime guard.

Restored disabled/missing shared-allocation legacy hiring: an already-qualified
trainee can take a real vacancy and the existing study loop then cancels their
course. Shared allocation still reserves active students, matching its complete
workforce comparison. Added one temporary-SQLite regression proving both paths;
existing fishing study/hiring regression passes. Selection diagnostics now name
the chosen village/business to avoid confusing a shared result with every page firm.

Final focused Wrapper clean verify/package passed60 unit +2 packaged integrations,
zero failures/errors/skips,1:03 at20:39:36. It reran every failed case and complete
directly affected staffing/career/competition/report classes; no repeated full local
suite. Full hosted Windows/Linux verification follows the feature push. This is
cumulative coverage, not a claim that the corrected full hosted run is already green.
No new Paper API adapter/native run, schema remains98, human server/main untouched.

P3 stays76.4% partial,1/4 whole groups. Next: remaining marker/animal startup
competition and broader specialization acceptance. No new design decision.

## October18 - bounded hosted failure counts

Added a failure-only CI summary for existing JUnit reports on Windows/Linux. It
publishes at most20 source-verified test class names and integer failure/error counts.
Names must match a Java identifier pattern and an existing test-source path.
No case names, arguments, failure messages, fixture data or test output are exported.
No credential access or action dependency. Disposable PowerShell smoke passed:
source/count selection, private argument/message omission, unknown-source refusal
and missing-report fallback. No Maven rerun was needed for this workflow-only slice.

The older fa486ca hosted run failed on both platforms. Public annotations gave only
the exit code. Approval review rejected Git credential extraction and the initial
proposal to publish failure messages. Neither action ran. Continued with local
reproduction and reduced this change to source class names and aggregate counts.
The full local run identified stale predecessor snapshot expectations; their fixes
are still being validated. No gameplay, schema, human server or gate change.
P3 remains76.4% partial.

## October18 - current Phase3 playtest guide and saved-village lookup

Added admin-only /civ debug villages using the ready session cache, with32 sorted
saved IDs and an exact omitted count. Report arguments now offer full-ID prefix
completion (maximum32); unauthorized users receive no saved IDs. Startup lookup
does not create/open a database. No world access, SQL on the Paper thread, gameplay
change or schema migration; remains98.

PHASE3_PLAYTEST.md consolidates safe installation, actual sneak-right-click food
interaction, bargaining/barter, livestock market/buckets/protection, diagnostics,
editable clock/configuration boundaries, current industry behavior and an explicitly
uncompleted human checklist. Older TONIGHT_PLAYTEST notes now point to this guide.
Corrected stale workforce documentation about legacy versus configured shared hiring.

Focused Wrapper clean verify/package passed10 command unit tests +2 packaged
integration tests, zero failures/errors/skips,19.173s at20:18:06 EDT. Two new cases
cover startup/permission safety and35 saved villages with bounded lookup/completion.
This cache-only command change adds no new Paper world adapter; no new native
runtime or human acceptance is claimed. Full hosted suites follow the feature push.
Human server and main untouched.

The documented-playtest acceptance entry is now complete: final group1/5; resources
36/39, lifecycle14/15, core4/4. Equal group weighting gives about76.4% partial P3,
still1/4 whole groups complete. This is a documentation milestone, not a claim
of additional gameplay or finished long-run acceptance. Remaining combined startup
competition, useful crafts, bulk-editor capture and specialization checks stay open.
No new design question; continue scoped competition and acceptance work.

## October18, 20:16 EDT - urgent existing farms get first access to idle labor

Connected approved urgent-food priority to the existing farm adapter before other
business vacancy allocation. At most two villages per owner pulse; only an existing
operating farm, present world, complete crop evidence, real unfilled observed slots
and a food assessment matching current resident demand qualify. The farm keeps its
saved skills/capacity and ordinary employment history. No new farm, money, production,
time advancement, job transfer or invented food is authorized by this hook.

New-file urgent-industries now defaults to farming, bakery and fishing. Existing
installed lists stay unchanged. Active students are excluded from farm hiring,
matching non-farm staffing/founder protection. Balanced fields and unrelated
industries retain their ordinary scheduling. No schema migration; remains98.

Focused clean Wrapper verify/package passed47 unit +2 packaged integration tests,
zero failures/errors/skips,42.000s at20:13:31. Three new temporary-SQLite scenarios
prove scarce idle labor reaches the urgent farm before another vacancy, no extra
food/time/spending, and refusal for absent worlds, partial fields or changed
population. Existing farm, selection, config and session regressions passed.
This is SQL-owner/application scheduling without a new Paper API adapter; no new
native run was claimed for this slice. Earlier staffing and resource-selection
Paper acceptance remains separately recorded above. Human server and main untouched.

P3 remains about71.4% partial,36/39 resources,14/15 lifecycle and0/5 final acceptance:
marker/animal startup priority and broader specialization remain. Next is current
playtest documentation and remaining startup adapters. No new decision needed.

## October18, 20:09 EDT - shared resource startup investment verified

Resource startups now compete through the approved priorities instead of fixed
mine/logging/quarry invocation order. Comparison uses each adapter's current viable
proposal, qualified idle founder, protected real grant and actual funded demand.
Public need is recomputed after that proposal's startup debit. Existing source,
warehouse, capacity, placement/provenance and range checks remain; the selected
adapter repeats them before committing. Founder study remains on its normal path.
One initialized village per owner pulse, bounded5-second village reviews; no
exhaustive ranking of every ore patch or invented resource opportunities.

Schema98 adds two empty resource investment state/audit tables. Actual grant,
startup spending, empty store capital, first job, business/source history, selection
explanation and saved tie cursor commit together. A late audit failure rolls all
back. Old missing allocation sections retain separate resource startup scheduling.
Marker-confirmed workshops/warehouses, fishing/livestock acceptance and farm staffing
still need their shared integrations. No new governance, wages or closure rules.

Focused broad check covered89 units:86 passed, three new fixture failures showed
food assessment stale after adding a sixth resident. Refreshed the fixture food
period using its original deterministic priority seed; all four new resource
investment cases passed on final verify, with two packaged integrations. Final
BUILD SUCCESS20:06:37,16.889s. Tests prove funded mine300 beats feasible quarry200,
quarry can win when the mining customer lacks funds, grant/resource/job/cursor
rollback, interval and no-world/no-skill/no-money/stale evidence refusal. Existing
85 resource/session/migration cases remained passing; no full local-suite repeat.

Native isolated Paper26.2 build129 passed20:07:12: enabled shared selection on the
existing ordinary forest observer/private PopulationSession fixture. Selected and
audited one logging startup, then paid logger->warehouse->craftsman deliveries
produced fittings in five fixed-clock periods. Exactly one grant and six original
supports,32,500 total cash after the known500 expense; no refill or extra funds.
All nine original AIR fixture states restored, test-only3x3 tickets removed.
Main disposable schema97->98 audit171 predecessor tables166 exact, only five normal
clock/capture metadata changes, no aging, integrity/FKs clean, config byte-identical,
port restored/helper archived/backups retained. Human server and main untouched.

Shared staffing b3fa3f1 is committed/pushed on codex/phase3-industry-competition.
This batch continues that feature branch. P3 remains36/39 resources,14/15 lifecycle,
0/5 acceptance, about71.4% partial and1/4 whole groups: combined capital/worker
competition is not falsely checked while other adapters remain independent.
Next: remaining farm and marker/animal startup paths, then broader acceptance.
No new consequential decision is needed for the approved priority rule.

## October18, 19:59 EDT - shared existing-business hiring verified

Implemented the user's approved urgent-needs, funded-demand and fair-tie priorities
for existing non-farm vacancies. The complete village comparison chooses before
hiring, including businesses outside the current runtime page. It filters real
vacancies, qualified idle adults, living local owners, source/habitat availability
and actual funded demand. Existing jobs stay in place; no wages, trades, money,
forced transfers or closure rules added. Students are consistently excluded from
hiring and comparison. Five-second throttling also bounds empty reviews.

Schema97 adds two empty allocation state/audit tables. Job, hiring audit, selection
explanation and saved tie cursor commit together; failure rolls all back. Defaults
in new economy.yml opt into shared allocation, urgent bakeries/fisheries,5-second
reviews and bounded reference values; old missing sections retain legacy behavior.
Default staffing also recognizes bakery/craftsman/livestock vacancies. Invalid edits
fail before DB startup. See BUSINESS_ALLOCATION.md for budgets and limits.

First focused clean Wrapper verify passed53 unit +2 packaged integration tests,
38.427s at19:54:31. After bounded empty-review throttling/diagnostic adjustments,
final affected verify/package passed12 units +2 integrations,31.131s at19:56:37.
Seven new cases cover funded ranking over UUID order, urgency/ties/value bounds,
restart fairness/interval, no poaching/spending, atomic audit rollback, no-money/
world/exhaustion refusal, full-village selection outside a page and strict config.
Predecessor schema snapshots now exclude only the two new tables; old rows remain
compared and schema94->current verifies empty additive tables. No full local repeat.

Native isolated Paper26.2 build129 passed19:57:26: the actual PopulationSession pulse
selected real iron customer demand400 instead of earlier-ID coal demand100; one
qualified new job, existing smith retained, treasury unchanged. No world edits or
chunk tickets. Main disposable schema96->97 audit169 predecessor tables164 exact;
only five routine clock/capture metadata tables changed, no aging, integrity/FKs
clean, config byte-identical, port restored/helper archived/backups retained.
Human server and main untouched. Recovery fa486ca hosted checks remain in progress.

Startup investment and farm staffing are not yet covered by this common comparison.
The combined competition and specialization gates remain open; P3 stays36/39
resources,14/15 lifecycle,0/5 acceptance, about71.4% partial and1/4 whole groups.
Next: compare viable resource startup investments, then other startup adapters and
broader acceptance. The priority decision is approved; no new question is needed.

## October18, 19:39 EDT - scoped forest recovery verified

Added /civ debug recover-forest list, review <site-uuid>, confirm <token>. Complete
loaded snapshots include every saved support and configured natural-canopy geometry,
with nearby sticky exclusions. A second unchanged scan and explicit same-admin
approval are required. Missing trunks receive permanent loss records; lost canopy
reduces habitat without declaring a trunk dead. Review cannot increase saved habitat
capacity or timber balance, clear protections, alter earned goods or unlock new
forestry discovery. Unknown/missing chunks and truncated trees refuse approval.

Schema96 adds one initially empty forest_recoveries audit table. Source losses,
habitat change, approval and balance changes commit together. Same per-site trust
is used by timed logging, production and tool replacement. Current capture health
is separate from historical trust; clean restart retains scoped approval, later
gaps/crashes revoke it. No generic trust reset, editor certification or block edits.
Budgets512 supports,9 snapshots,200,000 cells,4,096 scan steps/tick, two simultaneous
scans,16 reviews,30-second scan freshness and120-second admin preview expiry.

Five new temporary-SQLite cases verify actual scoped logging and revocation,
clean-restart continuity, retained goods/history, permanent coordinate loss,
canopy-only damage, no refill on reapproval, incomplete/stale/unhealthy/expired
refusal and audit-failure rollback. Affected verification covered58 unit cases
across checkpoints:54/55 passed first, the old-schema comparison was corrected,
all4 cases in that class plus3 founder cases passed; two packaged integration tests
passed. Final Wrapper verify/package BUILD SUCCESS19:37:28,18.277s. Updated three
older migration snapshot filters to exclude only newly added tables, preserving
all predecessor-row comparisons; migration94->current also compares all prior rows.
No full local-suite repeat. Hosted CI will run the complete cross-platform suite.

Actual isolated Paper26.2 build129 passed19:37:59. A private saved six-support
forest had one missing trunk: preview and confirmation reduced96 habitat/timber
to48 because only the other complete tree retained suitable canopy. Exactly one
trunk retired, all six source identities retained, one approval, global discovery
still fenced. All eight original AIR states restored after seven temporary log/
leaf edits; one fixture-only ticket removed. Main disposable schema95->96 audit:
168 predecessor tables,163 exact; only five routine clock/capture tables changed,
zero aging, clean integrity/FKs, config unchanged, port restored and helper archived.
Human server/main untouched. Private test database and backups retained.

Combined reviewed resource recovery milestone is now checked: resources36/39
(92%), about71.4% equally weighted Phase3 partial checklist,1/4 whole groups. The
remaining resource gates are broader useful crafts, supported bulk-editor capture,
and broader live/long-run catalog acceptance. Competition and specialization remain.
The next allocation rule has been asked: prioritize urgent village needs then real
funded unmet demand, editable priorities/fair tie rotation, with no forced worker
moves or closures. Approval is pending; no ranking policy applied yet.

## October18, 19:31 EDT - scoped mineral recovery verified

Approved admin acceptance plus complete current scans is implemented for existing
mineral deposits. /civ debug recover-ore list, review <deposit-uuid>, confirm <token>
keeps preview and explicit approval separate. The second scan must match; saved
balances, exclusions and current capture must also remain unchanged. Missing or
protected coordinates lose only their remaining shares. No earned goods, money,
identity or history is deleted; no refill or global trust reset occurs.

Schema95 adds two initially empty tables: resource_recovery_epochs and
mineral_recoveries. Approvals apply only to the selected deposit and current capture
generation. Clean stops preserve approval; crashes/new capture gaps invalidate it.
Current operational capture is distinct from historical trust. Accepted ore saves
now fence work immediately while pending, matching the forest safety pattern.
Discovery/founding remain globally fenced. No forest recovery yet; metadata alone
certifies no logging. Unsupported bulk editors remain unsupported.

Focused Wrapper verification covered107 affected unit tests across the correction
checkpoints:106 passed in the broad run, one old-schema comparison corrected and
all6 cases in its class passed on final rerun; two packaged integration tests passed.
Twelve new recovery/continuity cases cover scoped actual mining, revocation, exact
loss, sticky protections, read-only preview, incomplete/stale evidence, rollback,
replay/expiry and schema94->95 prior-row preservation. Initial compiler imports and
old-schema fixture tables/comparison were corrected without weakening FK/migrations.
Final verify/package BUILD SUCCESS19:29:42,17.382s; no repeated full local suite.

Actual isolated Paper26.2 build129 passed19:30:47: private saved four-source coal
deposit previewed25 lost, explicit confirmation changed100->75, only the AIR
coordinate retired; global discovery stayed fenced. Main-thread messages verified.
First native setup refused an unloaded spawn chunk before edits; retry used one
explicit test-only ticket, removed afterward. All four original AIR states restored,
three temporary ore blocks changed, no drops. Production adapter never loads chunks.
Private test DB retained. Main disposable fixture schema95 audit168 predecessor
tables163 exact; only five routine clock/capture tables changed, zero aging, clean
integrity/FKs, config byte-identical, port restored, helper archived/backups retained.
The human server and main remain untouched. Actual registered recovery listing is
wired; full native review/confirm invokes the same adapter on a private session.

P3 resources35/39, about71% partial and1/4 whole groups unchanged: the combined
recovery gate still needs forests. Next: forest recovery, broader useful crafts,
competition and multi-village acceptance. No new gameplay decision required.
Prior commits4462dae and5508353 now have full successful Windows/Linux CI runs
39153111684 and39152003696. New recovery work is on codex/phase3-resource-recovery.

## October18, 14:12 EDT - read-only resource and workforce review verified

Added admin-only /civ debug resources and /civ debug workforce <settlement-uuid>.
The first reports separate saved ore/forest trust, remaining mineral shares,
retirements, deduplicated placement/removal exclusions, timber habitat balances,
and at most32 world/chunk scopes with an exact omitted count. Active capture's
clean_stop=false is not mislabeled a crash. It reports saved data, not current
block truth or recovery approval. Live capture diagnostics remain separate.

The workforce comparison shows saved vacancies, overlapping qualified idle pools,
actual known business cash and operating blockers. Unknown accounts stay unknown;
unknown towns and review-budget overflow show no partial comparison. Budgets cover
32 firms,512 idle candidates and512 saved assignments; profiles/requirements are
batched and eligibility is compared in memory. It does not pick a winner or change
hiring order, wages, spending, closure, skills or courses. No player wallet added.

Both reads run on the SQL owner; permission-checked results return via Paper's
main-thread scheduler. Added seven database report tests and two command cases;
existing command checks reused. Final affected Wrapper verify/package passed15
unit +2 packaged integration tests, zero failures/errors/skips,29.947s at14:10:03.
Checks caught an invalid fixture store deletion and the new report's zero-vs-unknown
cash bug; corrected without weakening FK enforcement or financial guards. Updated
an existing usage-text expectation for the new commands. No full local suite repeat.

Actual marked Paper26.2 build129 passed14:10:40: registered resources command queued
its read and delivered bounded messages only on the main thread. Schema remains94;
all166 predecessor tables retained,161 exactly; only five normal clock/capture
metadata tables changed, no aging. Integrity/FKs/capture clean; config unchanged,
zero probe block changes, original port restored, helper archived, backups retained.
Human server and main untouched. Read-only report does not certify historical
provenance or restore trust. Actual recovery remains blocked on the pending user
choice recorded in DECISIONS_AND_OPEN_QUESTIONS.md. No partial recovery code applied.

Resources35/39, about71% partial Phase3 and1/4 whole groups remain: diagnostics do
not finish recovery or actual cross-firm competition. Forestry7/7 is verified in
preceding5508353, pushed to codex/phase3-forestry-runtime. This batch lives on
codex/phase3-resource-review. Next: approved scoped recovery, then remaining crafts,
production competition and multi-village acceptance. Canonical roadmap unchanged.

Hosted forestry run39152003696 (5508353) remains in progress; no full green result
is claimed. Earlier safety run39150215584 (fb1c06e) passed both platforms. This report
branch receives its own full hosted checks after push.

## October18, 13:56 EDT - automatic forestry and paid timber chain verified

Villages can now use current loaded-chunk canopy evidence to open a logging
business when real timber demand, a qualified idle owner and protected startup
funds are available. Excluded/claimed trees, stale or conflicting frames, wrong
world/chunk, absent worlds and insufficient funds cannot create a firm. A change
to nearby canopy after capture also requires a new survey. Existing staffed
logging capacity counts before opening more firms. No trees are felled or claimed.

Prospective owners earn forestry skill gradually using real timber and stone,
with normal food/material reserves. Unknown evidence after restart pauses study;
fresh evidence of a lost opportunity cancels it. Server/world absence adds no
catch-up. Other occupations retain their original one-point study speed.

Schema94 adds two empty founder-course/review tables; captured sources, goods,
accounts, employment and history are preserved. New-file economy.yml has strict
editable forestry-founding terms and a forestry course (2 timber +1 stone per
active study period, target10, shared gain1). Old missing sections opt out and
installed files are never overwritten. See FORESTRY.md for limits and settings.

The combined affected run executed330 units:326 passed, four new fixture failures
were corrected (cash funding/overdraw and refusal-text assumptions), along with
a missing test import. No production guard was weakened. Final affected Wrapper
verify/package passed34 units +2 packaged integration tests, zero failures/errors/
skips,25.242s at13:51:56 EDT. Eight startup/study tests and three configuration cases
were added. Existing predecessor-row preservation suites passed in the combined
run. Full hosted Windows/Linux verification follows push; it is not claimed here.

Actual Paper26.2 build129 passed13:54:35: ordinary forest observer -> session ->
qualified funded startup -> paid logger/warehouse/craftsman deliveries -> crafted
storage fittings in five fixed-clock periods. Exactly six saved supports; all
cash conserved at32,500 after a real500 startup expense from33,000 fixture funds.
The private proof accelerated cadence and disabled unrelated tool/storage spending;
normal configurable upkeep has separate existing tests.
Nine original AIR blocks restored exactly. A preceding attempt stopped safely
because the helper's separate read-only connection lacked foreign-key enforcement;
that helper setting was fixed, failure logs/backups retained, then the full proof
passed. No production source change was needed for that helper error.

The original93 fixture upgrade retained164 predecessor tables (159 exact); only
five routine clock/capture metadata tables changed, no aging. Final94 rerun
retained166 tables (161 exact), clean integrity/FKs/capture. Config byte-identical,
original port restored, helpers archived, backups retained. Human server/main
untouched. Private fixture proof does not replace a human playtest or prove broad
village specialization/long-run balance. Forestry7/7 connections verified; the
combined basic resource-opportunity gate is now complete. Resources35/39, about71%
equally weighted partial Phase3,1/4 whole groups. Next: remaining crafts, reviewed
resource recovery, lifecycle/competition and multi-village acceptance. No new
gameplay decision was needed for this batch.

Preceding checkpoint fb1c06e passed both hosted Windows/Linux jobs in run39150215584.
This automatic-startup batch will receive its own full hosted run after push.

## October18, 13:34 EDT - forestry provenance verified

Native tree placement/growth/removal records now protect saved sources independently
of ore history. Confirmed loss retires only named supports, reduces standing capacity
without refilling it, and keeps harvested goods, ownership and business history.
Saved placement exclusions feed the bounded loaded-chunk forest observer. Untrusted
capture or pending native saves pause registered logging clocks/work without freezing
other industries; exact receipt replay and earned backlog remain intact. Legacy saved
forests without capture history require reviewed recovery rather than silent trust.

Schema93 adds three initially empty tracking/exclusion/source-loss tables. No old
rows are rewritten. Added8 focused tests plus a disposable-only native helper;
updated predecessor-preservation fixtures for the three additions. The initial
clean affected run executed220 unit tests with one incorrect new timeline expectation;
219 passed. Corrected that test's expected4 rather than5 periods, without changing
production rules. The affected rerun passed27 units +2 packaged integration tests,
zero failures/errors/skips,13.359s at13:32:28 EDT. A preceding sandbox-only packaged
backup failure was a Windows file-move denial; the same check passed outside that
sandbox against temporary databases. Full hosted suites follow the feature push.

Actual marked Paper26.2 build129 passed13:33:16: accepted growth saved exactly three
excluded coordinates, cancelled growth saved none, and the ordinary observer then
rejected that trunk. Nine high-sky fixture blocks restored exactly. Schema92->93
retained161 predecessor tables,157 exactly; only four normal clock/capture metadata
tables changed, no aging. Integrity/FKs clean, both captures clean/trusted, old config
byte-identical, original port restored, helper archived, backups retained. The human
server and main remain untouched. Native proof covers dispatch/observer integration;
transactional tests cover stock loss, rollback and restart. No physical logging,
planting, ownership claim or automatic logging startup is implemented by this batch.

Forestry5/7 detailed steps verified. Overall resources remain34/39 and Phase3 about
70% partial,1/4 whole groups until the combined live catalogue gate passes. Next:
qualified/funded automatic logging and real-material founder training, then live
startup/paid customer acceptance. No new design decision is blocking that work.

## October18, 02:50 EDT — usage-limited forestry protection handoff (UNVERIFIED WIP)

Delegated workers reported the account usage limit. Current local work compiles:
`.\mvnw.cmd -B -ntp -DskipTests compile` succeeded,7.648s. This was a compile check
only: no new tests, clean verify, packaged build or native acceptance were run for
this batch. Do not install the working tree as a verified release. Last verified
and pushed source remains ac33ed7 on codex/phase3-forestry-runtime; main and the
human server were not changed. WIP remains local and uncommitted.

Draft schema93 adds empty forestry_tracking, forestry_exclusions and
forestry_source_losses tables, independent from ore capture. Added native tree
mutation listener, SQL-owner save/read hooks, clean-stop tracking, sticky source
exclusions and individual saved-support retirement. New resource opening rejects
known exclusions/untrusted saved capture; habitat restoration rejects retired
coordinates. Existing migration fixtures were updated for the three additions;
these fixture changes have not been tested yet. No live database was migrated.

NEXT REQUIRED before verification/commit: complete independent IndustryRuntime
forestry accrual/work fence (configureForestry currently retains the old one-arg
call); prevent direct new logRows extraction/tool spending on untrusted forestry
history while preserving replay/earned stock; feed saved exclusions into the
observer (currently environmental-only Set.of()). Review native multi-place/new
state event details, async drain/pending failure paths, and coordinate budgets.
Add focused provenance/tracking/transactional92->93 preservation tests and native
Paper capture proof. Then docs/diff/feature commit/push; continue qualified,
funded automatic logging startup and live acceptance. No design answer is needed.

Progress receives no new credit: forestry4/7 verified; resources34/39; Phase3 about
70% partial and1/4 whole milestone groups. Automatic logging remains unfinished.

## October17, 22:46 — live read-only forest observation verified

Villages can now report nearby canopy-backed standing trunks through the ordinary
Paper observer and /civ debug observation. Snapshots come only from already-loaded
chunks; bounded work preserves capture time, rejects unknown/oversized evidence,
and excludes horizontal logs/persistent placed leaves. Observer geometry and pacing
are editable in economy.yml; missing old sections opt out. Samples are transient
environmental diagnostics, not ownership proof, saved resources or businesses.

Windows Wrapper affected verify/package passed12 unit +2 packaged integration
tests, zero failures/errors/skips,24.396s,22:42:20 EDT. Two new configuration tests;
existing geometry/resource cases reused. Actual Paper26.2 build129 passed22:44:58:
ordinary observer found exactly the expected trunk, identity/time matched,
decorative/persistent examples and missing margins were rejected. Nine fixture
blocks restored exactly, helper-only tickets released. No production chunk loading
or world mutation. Native probe has an explicit disposable/localhost/zero-player
guard; never install probe JARs for player use.

Schema remains92. Main fixture retained161 tables,154 exactly; seven normal clock/
capture/age tables changed by one normal month, identity/status/assets preserved,
integrity/FKs clean. Private DB created no business/resource/work. Existing config
byte-identical, original port restored, helper archived, backups retained. Human
server and main untouched. Backend2f6450b is committed/pushed and its full Linux CI
passed; Windows remains in progress. Final observer hosted checks follow push.

Forestry details are4/7 verified: geometry, saved resource/work, real paid customers
and live environment observation. Next: native placement/growth/damage protection,
then automatically qualified/funded startup and live acceptance. No new gameplay
decision needed. The combined catalogue gate remains unfinished, so resources stay
34/39 and P3 about70% partial,1/4 whole groups. See FORESTRY.md for exact boundaries.

## October17, 22:37 — saved forestry and real timber customers verified

Registered logging firms now have persistent canopy-backed habitat, non-1:1 timber
stock, gradual renewal, normal work clocks, qualified employees and saved forestry
experience. Actual timber is paid for and delivered through warehouses to funded
craftsmen. Shared protected startup money, empty stock, first worker and habitat
commit together; retries, restart, absence, retuning and late failure preserve
goods/cash/history. Fixed the missing registered-logging timber resale allowance.
Unqualified or habitat-less work does not request logging tool upkeep.

Schema92 adds four EMPTY resource/support/work/habitat-audit tables. No old rows or
businesses are retrofitted. Added11 focused tests; existing preservation fixtures
still compare every predecessor row/table. Forestry resource terms are now editable
in economy.yml; missing old sections disable the new backend, and installed files
are preserved. Live automatic tree adoption/founding remains off until native
provenance and current observation are connected. No physical felling or land claim.

Combined Windows Wrapper affected verify passed170 unit +2 packaged integration
tests, zero failures/errors/skips,47.667s,22:34:45 EDT. Initial test-only duplicate
workplace/zero-renewal assumptions were corrected; the chain test caught the actual
timber resale omission, fixed before the passing rerun. No repeated complete local
suite; full Windows/Linux CI follows the feature push. Exact final command:
.\mvnw.cmd -B -ntp verify "-Dtest=ForestryPersistenceTest,ForestryRuntimeTest,ForestryConfigurationTest,ForestScanTest,ForestStockTest,DedicatedProducerChainTest,BusinessStartupGrantsTest,IndustryRuntimeConfigurationTest,EconomyConfigurationTest,QuarrySurveyTest".

Actual marked disposable Paper26.2 build129 reached ready and stopped22:36:00.
Upgrade91->92 retained157 old tables,153 exactly; only four routine metadata tables
changed, no resident aging. Integrity/FKs clean, four additions empty, capture clean/
trusted, economy.yml byte-identical, original port restored, backup retained.
Human server untouched; no live forest-adoption/native-harvesting result is claimed.

Resources remain34/39, about70% equally weighted partial Phase3,1/4 whole groups:
the forestry catalog gate still needs live acceptance. Broader crafts, recovery,
competition and specialization acceptance also remain. No new gameplay answer is
needed for non-destructive forestry observation/provenance. Next: that live capture
and qualified/funded automatic startup, then integrated long-run acceptance.
Feature branch codex/phase3-forestry-runtime; main unchanged. See FORESTRY.md for
files, config, migration, behavior and limits. Preceding7dd5643 Linux CI passed;
its full hosted Windows job was still running when this entry was recorded.

## October17, 22:22 — forestry recognition and timber calculations

Added bounded incremental canopy-backed tree recognition with exact supporting
coordinates, supplied placement exclusions, explicit incomplete evidence and safe
outer margins. Added renewable timber calculations with non-1:1 opening capacity,
gradual fractional renewal, real requested output/work/storage limits, absent-world
pause, and explicit conservative habitat changes. No live logging business is
claimed: this slice has no Paper, database, inventory or world mutation. Server
text-file configuration and saved provenance/habitats remain integration work.

Seven focused tests and Wrapper package passed, zero failures/errors/skips,
16.628s,22:21:13 EDT; corrected a test-only static method reference before passing.
No repeated full suite or native run for these isolated domain additions. Schema
remains91. See FORESTRY.md for changed classes, prototype terms and limits.
Progress remains34/39 resources, about70% partial Phase3,1/4 whole gates: logging
needs actual live integration before receiving checklist credit. Quarry source
263fbe3 is backed up to GitHub; Windows/Linux run39089622441 is in progress, not
yet claimed passing. Main and the human server remain unchanged. Next: persistent
forestry evidence/provenance and actual paid timber supply. No decision needed.

## October17, 22:17 — automatic stone/sand businesses verified

Completed bounded loaded-chunk stone/sand observation, separate current per-chunk
frames, actual funded customer selection, gradual real-material founder training,
and atomic qualified quarry/sandpit startup under the existing protected grant
limits. Finite reserves feed ordinary timed work and paid warehouse/customer
deliveries. Existing mine stone counts toward supply; sand needs a real configured
buyer. No excavation, land claim, invented customers or new money. Settings remain
editable; older installed files are preserved and missing sections opt out.

Schema91 adds five empty quarry evidence/frame/training tables; no previous rows
or tables are changed by migration. Existing configured warehouse sand slots are
added empty with an audit only when a sand customer exists. The preserved bakery
empty-slot helper retains its existing receipt identities. Neighboring empty
surveys no longer erase another chunk's prospect, and stale historical scopes do
not exhaust the current-frame review budget.

Windows clean verify passed1,868 unit +2 packaged integration tests, zero failures,
errors or skips,14:23, finished22:13:25 EDT. The final per-chunk correction and two
regressions followed that full run; affected final-source verify/package passed
56 unit +2 packaged tests, zero failures/errors/skips,39.607s,22:15:27. No final-source
full-suite result is claimed. Actual Paper26.2 build129 passed22:16:20: ordinary
read-only observer matched independent terrain; private SQLite founded an actual
candidate, ran five production periods, delivered paid cargo through a warehouse
to a producing customer, conserved money after the startup sink, and preserved
absent-world assets. Fixture-only imports/Windows Java quoting were corrected.
The stopped main disposable fixture retained152 predecessor tables,148 exactly;
only four routine metadata tables changed, no resident aging, integrity/FKs clean.
Settings restored, helper archived, backups retained. Human server untouched.

Resources34/39 (87%), about70% equally weighted partial Phase3; whole groups remain
1/4. Forestry, broader general crafts, reviewed resource recovery/compatibility,
competition and emergent-specialization acceptance remain. Next: non-destructive
forestry environmental evidence and real timber supply. No gameplay answer needed.
Feature branch: codex/phase3-quarry-startup; main unchanged. GitHub backup follows.
See AUTOMATIC_QUARRIES.md for files/settings, migration and current limits.

## October 17 — usage-limit handoff: quarry startup work is UNVERIFIED

Both delegated workers stopped with the account usage-limit error during the next
stone/sand milestone. This is unfinished working-tree work, not a playable feature
or a passing migration. No build/tests/native server/database operations were run
for this batch. Do not release or commit it as complete without finishing review.
Last verified/pushed source remains3cfbc10 on codex/phase3-live-livestock; target
JAR still comes from that prior passing build. Main and the human server unchanged.

Draft changes: independent QuarrySurveys schema (proposed91, three additive empty
quarry_* evidence/frame/membership tables), MineralOpportunities.quarries(Connection)
and MineralSurveyReader.quarries(Connection) factories keeping coal/iron frames
separate, proposed schema validation, QuarryFoundingPolicy and partial strict
configuration readers. Config defaults/YAML/tests may be incomplete. No actual
Paper quarry observer or automatic quarry startup has been connected yet.

Resume: inspect all six draft source files/diff; finish quarry config/defaults and
regressions; update SchemaFixtures and old preservation expectations for the three
new empty tables; add survey isolation/rollback tests; implement bounded loaded
stone/sand observation plus actual customer/skill/shared-grant startup; verify full
migration build and native integration before any milestone credit/commit/push.
Do not discard the existing drafts or silently certify them. P3 stays33/39 resources,
about69.5% partial,1/4 whole groups. No consequential gameplay question is pending.
Latest user reply reconfirms bounded automatic natural-animal recognition and
[Civ Protect] for guaranteed large-pasture protection; preserve that existing rule.

## October 17, 17:53 — dedicated producers no longer block local trade

Fixed warehouse/demand classification that rejected registered fisheries,
quarries and sandpits as unknown industries. Saved fishery food and matching
finite-deposit yields can now supply actual funded customers. Existing earned
stock stays tradable; the fix creates no resource, customer, capital or automatic
quarry. Unknown/unregistered businesses still defer for review. Configurable tool
needs are retained, and exhausted quarries/sandpits use the same no-replacement
check as exhausted mines. Immediate extraction trust gaps suppress new tool
pressure without deleting already-earned goods.

Changed WarehouseSupplies, LocalIndustryDemand and BusinessTools; added eight
DedicatedProducerChainTest cases for actual fishery/finite extraction, real paid
warehouse orders, delayed delivery, exact replay, registration guards and positive
then depleted quarry tool demand. Updated TONIGHT_PLAYTEST.md and milk/egg/bakery
guides so preserved old configs and unfinished bulk pen donation are explicit.
Schema remains90; no migration, world edits or human-server deployment this batch.

Windows Wrapper affected verify passed58 unit +2 packaged integration tests (60),
zero failures/errors/skips,33.897 seconds. Initial restricted run hit a Windows
sandbox denial moving the packaged backup test's temporary file; the ordinary
permission-enabled rerun passed without skipping tests or changing backup code.
Final command: `.\mvnw.cmd -B -ntp "-Dtest=DedicatedProducerChainTest,WarehouseSuppliesTest,LocalIndustryDemandTest,BusinessToolsTest,QuarryRuntimeTest" verify`.
No new Paper API behavior was introduced; preceding actual Paper slaughter and
bakery proofs remain the native checkpoints. Full Linux CI for preceding c4ddeec
passed; its Windows job was still running at this checkpoint. Feature backup and
fresh hosted full verification follow. No unrun hosted result is claimed.

Checklist credit stays33/39 resources, about69.5% partial P3,1/4 whole groups:
this repairs connected behavior rather than adding an uncompleted industry.
Next scoped industry work: actual trusted stone/sand surveys and qualified funded
quarry/sandpit startup, then broader forestry/craft and specialization acceptance.
No new gameplay answer is needed for this fix. Main remains unchanged.

## October 17, 17:40 — controlled livestock slaughter verified

Village livestock businesses can now turn eligible surplus adults into saved food
without duplicate ground drops. Exact loaded animal identity, breeding reserves,
unused earned work, qualified staff and real storage space are checked first.
Cancelled deaths keep the animal and add no food. Ordinary kills keep vanilla
loot and receive no business food. Pending outcomes survive restart and reserve
space; an unprovable outcome stays paused for reviewed recovery, never inferred
from a missing or unloaded animal. Defaults and limits are editable; older files
without livestock-slaughter remain opted out. See LIVESTOCK_SLAUGHTER.md.

Schema 90 adds one transactional permit/audit table; existing histories and
assets are preserved. Fixed quick shutdown during database startup so it drains
capture state and closes cleanly. Also corrected a stale CI catalogue expectation
for the already-added milk/egg weights; no economy behavior changed for that fix.

Windows clean verify passed 1,837 unit + 2 packaged integration tests (1,839),
zero failures/errors/skips, 12:22, finished 17:31:35 EDT. Two later regressions
cover manual-budget exclusion and immediate startup shutdown; final affected
verify/package passed 57 unit + 2 packaged tests (59), zero failures/errors/skips,
35.949s, finished 17:36:23 EDT. Those two tests were not in the earlier full run.
Packaged Paper 26.2 build 129 passed at 17:39:36: actual cancelled death,
intentional death with no ground loot/XP, ordinary death with normal loot, exact
saved head/food counts and replay protection. Main disposable fixture retained
all 152 tables/148 exact; only four routine metadata tables changed, no aging,
integrity/FKs clean. Fixture-only startup/timing and revive-health assumptions
were corrected before the successful proof. Server stopped, settings restored,
helper archived and backups retained. Human server untouched.

Resources advance to 33/39 (85%); equally weighted partial Phase 3 progress is
about 69.5% (roughly 70%), with 1/4 whole groups complete. Detailed livestock
connections are 10/11; authorized bulk pen donation remains. Broader industry,
competition, specialization and final acceptance still prevent calling P3 done.
Next: check remaining producer-to-warehouse connections and pen donation.
No new gameplay decision is needed. Feature-branch backup/CI follows; main stays
unchanged.

## October 17, 17:02 — paid livestock-to-bakery food chain verified

Bakeries now buy delivered milk and eggs through paid local warehouses and make
richer meals from actual ingredients. Bread remains the simple startup/fallback;
one recipe uses the existing worker/tool budget per earned period. Defaults are
editable: 3 grain + 1 milk + 1 egg -> 8 abstract food units. No physical cake item.
The integrated scenario continues through a real paid village food order.

Business-only ingredient prices avoid changing saved public food catalogues.
Configured existing bakery/warehouse stores gain audited empty ingredient slots;
units, cash, capacity, original terms and saved unit weights are retained. No
schema change (89), world edits or human-server deployment. Installed config is
never rewritten; see BAKERY_SUPPLY_CHAIN.md for opt-in and exact boundaries.

Windows affected verify/package passed 234 unit + 2 packaged tests (236), zero
failures/errors/skips, 1:31, finished 17:02:13 EDT. Initial checks caught a fixture
accessor and incompatible public-price catalogue additions; corrected before
shipping. Packaged Paper 26.2 build129 passed 17:02:51: 39 accelerated actual
PopulationSession periods, real herd products and both paid delivery legs, recipe
flows, conservation, exact receipt replay, restart and absent-world pause. Main
disposable fixture retained all 151 tables/147 exact; four ordinary clock/capture/
ore metadata changes, no aging; integrity/FKs clean. Server stopped, settings
restored, helper archived, backups retained. Feature push/hosted CI follows.

Resources advance to 32/39 (82%); equally weighted partial Phase3 progress is
about69%, with 1/4 whole groups complete. Controlled slaughter, authorized pen
donation, broader craft/industry coverage, competition and specialization remain.
Next: controlled livestock slaughter with exact native identity/drop accounting.
No new gameplay decision is needed for this batch.

## October 17 — livestock qualifications, succession and resale defaults verified

Real feed-backed animal care now earns configurable farming experience for actual
qualified employees. Owners earn nothing merely for owning a business. Awards
respect real work, worker limits and ceilings, rotate when work is scarce, and
commit with feed/products/births/the clock. No work or replay means no free skills.
A scenario proves an initially unqualified employee earns the actual founder
qualification and can then inherit under the existing family-first rules.

Fixed another lifecycle gap: default resale values now cover grain, milk, eggs
and sand. Stocked producers can be valued and sold instead of waiting forever
for a missing quote. Real buyer budgets and accepted sale prices stay intact.
Installed files remain unchanged; admins opt into the livestock learning mapping
and add the new editable resale values. No wallets, payroll or new money.

Schema 89 adds two learning audit tables only. Existing herd/animal/owner/skill/
stock/history rows survive; no retroactive awards or reset. Updated historical
migration fixtures to require exactly the two new empty tables while comparing
every predecessor row unchanged. See LIVESTOCK_LEARNING.md. Changed: livestock
learning helper, shared runtime, migration validation, learning/resale defaults,
focused and preservation tests, excluded native probe and livestock guides.

Windows .\mvnw.cmd clean verify -B -ntp passed 1,814 unit + 2 packaged integration
tests (1,816), zero failures/errors/skips, 12:19, finished 16:26:28 EDT. After the
small resale-default correction, final affected verify/package passed 111 unit +
2 packaged tests (113), zero failures/errors/skips, 23.605s, finished 16:28:21 EDT.
No claim that the three subsequently added resale tests were in the earlier full
run. Initial checks caught outdated table-list expectations and an underfunded
buyer fixture; both were corrected without weakening preservation/spending rules.

Guarded packaged Paper 26.2 build 129 passed 16:28:43 EDT: populated private
schema88 upgrade, actual PopulationSession care/skills/audits, restart, absent-
world and capture-gap pause, cash/goods conservation. No block/animal changes.
Main dev fixture retained all 149 predecessor tables/145 exact; ordinary clock/
economy/capture/ore metadata only, no aging. Two new tables empty; integrity/FKs
clean. Server stopped/settings restored/helper archived/backups kept. Human
server untouched. Feature backup/hosted CI follows; main stays unchanged.

Phase 3 remains in progress: resources 31/39 (79%), about 68% partial across
four groups, 1/4 groups complete. The automatic observed/funded herd-operation
entry is complete; physical slaughter without duplicate drops, authorized pen
donation, bakery milk/egg consumers and remaining industry/specialization gates
remain. These qualification/lifecycle fixes do not inflate checklist credit.
No design answer is needed for this batch.

## October 17 — automatic natural livestock startup and the feed chain verified

Villages can now start a real dairy/chicken business from repeated bounded
observations of eligible natural animals. Names, leashes, human/unknown origins,
saved owners, nearby barriers and saved protected pens exclude adoption. A real
farming-qualified owner, available adult worker and food-protected shared startup
grant are required. Claims, firm/site, job, cash and initial ages commit together;
failed funding or saving leaves no orphan ownership. No animal teleport or chunk
loading. Existing worlds/saves and closed firms retain identity/history.

Warehouses now recognize registered herds, plan their actual funded feed needs,
and buy grain from real crop-farm surplus. Cash, cargo, feed use and output are
conserved; herds cannot resell purchased grain as producer output. New strict
livestock-adoption settings are editable; installed files missing them opt out.
No migration/reset: schema 88 retained. See LIVESTOCK_ADOPTION.md.

Affected Wrapper verify: 364 unit + 2 packaged integration tests passed, zero
failures/errors/skips, 1:44, finished 2026-10-17T16:02:53-04:00. After tightening
the uncertain-observation reset, final package + 14 focused unit/2 packaged checks
passed, 21.041 seconds, finished 16:03:39 EDT. This is focused validation, not a
new full-suite claim. Guarded Paper 26.2 build 129 passed 16:04:27 EDT using real
natural cows and the actual observer/PopulationSession: fence exclusion, repeated
clear proof, named-animal protection, paid opening and exact durable ownership.
Test blocks restored/exact bodies removed; private DB retained. Main fixture:
149 tables retained/142 exact, ordinary metadata and one normal population month
only; integrity/FKs clean. Server stopped/settings restored/helper archived.
Human server untouched. Previous 39fd240 hosted Windows and Linux jobs both passed.

One full resource checklist entry is now complete: observed/funded live livestock
startup and timed operation. Resources advance to 31/39 (79%); whole groups remain
1/4, with about 68% equally weighted partial progress. Physical slaughter without
duplicate drops, bakery milk/egg consumption and authorized pen donation remain;
no credit for those unfinished integrations. No decision is pending for this batch.

## October17 — automatic saved-herd work and paid feed verified

Existing saved livestock businesses now use the shared industry clock: buy real
feed with business money, wait for cargo, then consume it for milk/eggs and saved
births under actual qualified work/storage limits. Existing newborn/age adapters
can reconcile those births. World/server absence and capture gaps earn no work.
Delivery, purchases, production and checkpoints roll back together on failure;
old manual herd periods are not replayed. New editable livestock-runtime limits
opt installed files out when absent. Schema88 unchanged; no migration/reset.

Five focused scenario/config cases added. Final scoped Wrapper verify passed126
unit plus2 packaged integration tests (128 total), zero failures/errors/skips;
43.518s, finished2026-10-17T12:26:27-04:00. No new full-suite claim. Initial checks
caught/fixed a fixture cash-column typo and new config root-key registration.
Guarded Paper26.2 build129 passed14:44:59 EDT: actual packaged owner-thread work,
paid feed/cargo/milk/births/conservation, restart, absent-world and capture-gap
pause. Private synthetic herd; no block/animal mutations. All149 main tables
retained/145 exact; ordinary clock/runtime/capture/ore metadata only, no aging.
Integrity/FKs clean; fixture stopped/settings restored/helper archived/backups
kept. Human server untouched. Full CI follows feature backup.

Changed: herd persistence composition, shared industry runtime, paid-feed helper,
editable policy/configuration, plugin/owner wiring, diagnostics, focused tests and
excluded LivestockRuntimeProbe. See LIVESTOCK_RUNTIME.md for settings/contracts.
P3's livestock milestone remains partial: automatic eligible animal adoption and
funded founding, physical slaughter, and bakery milk/egg consumers still remain.
This closes scheduled operation for already saved herds; no unimplemented startup
is counted complete. P3 stays~68% partial/30of39 resource entries/1of4 whole groups.
Next: bounded eligible-animal observation and atomic farm opening. No new answer
needed for this scheduled-work batch; no duplicate slaughter drops is approved.

## October17 — usable pen protection controls verified

Admins can now right-click a [Civ Protect] sign inside a supported flat enclosure,
review its area/animal count and private boundary outline, then explicitly confirm.
The plugin recaptures evidence and saves real animal owners. /civ pen here finds
saved IDs; owner/admin release also requires confirmation and retains each animal's
ownership/history. Sign placement alone claims nothing; existing owners cannot
be overwritten. Limits are editable in config.yml. No donation or automatic herd
founding/feed/output enabled yet. No migration; schema88 retained.

Following the user's faster-development instruction, final scoped Wrapper verify
passed67 affected unit plus2 packaged integration tests (69 total), zero failures/
errors/skips;26.961s, finished2026-10-17T11:44:00-04:00. No new full-suite run is
claimed; the prior checkpoint passed1,788. Guarded Paper26.2 build129 passed
11:44:59 EDT with a real sign/pen/cow and synthetic player: explicit confirmation,
owner/controller restart, lookup and release retain ownership/history. Temporary
blocks restored/test cow removed. All149 main tables retained/145 exact, ordinary
clock/runtime/capture/ore metadata only, no aging; integrity/FKs clean. Fixture
stopped/settings restored/helper archived/backups kept. Human server untouched.

New files: PaperPenControls, PaperPenCapture, PenProtectionSettings, eight focused
control/config tests, excluded PenControlsProbe and PEN_CONTROLS.md. Plugin/router,
config/permissions and owner handoffs updated. No-duplicate slaughter drops now
explicitly approved and recorded; no question remains for that rule. Full-suite
CI follows feature backup. Local checks stay focused between larger checkpoints.

P3 remains about68% partial (30/39 resource entries,1/4 complete groups). This
finishes an interaction prerequisite within the incomplete livestock milestone;
no percentage credit for unimplemented adoption/feed/slaughter. Next: automatic
livestock loop and bakery ingredients. See PEN_CONTROLS.md for actual commands,
authority and limitations; broader templates/construction remain Phase4.

## October17 — pen area release and retained animal ownership verified

Pen protection now separates an active area from its permanent history. An
explicit owner/admin release retains original animal ownership and provenance;
old protection retries cannot reclaim released land. Adjacent pens may share a
fence, while overlapping interiors are refused. Schema88 preserves original
pen records and backfills current claims without inventing ownership.

Added12 lifecycle/owner-queue cases; focused50 passed. Windows .\mvnw.cmd clean
verify -B -ntp passed1,788 tests (1,786 unit/two packaged integration), zero
failures/errors/skips;11:39, finished2026-10-17T11:01:25-04:00. Source/test Java
remained fixed during the final full run. The excluded native helper was then
compiled against the verified packaged JAR.

Guarded Paper26.2 build129 passed11:01:48 EDT: private populated schema87 upgrade,
shared fence, authority checks, release/retry/reclaim/restart, preserved original
pen/animal history and clean SQLite integrity/FKs. All147 main predecessor tables
retained;143 exact, only ordinary clock/runtime/capture/ore metadata changed,
no aging. The two new main tables are empty. Fixture stopped/settings restored/
helper archived/backups kept. No world/entity edits or human-server changes.
This certifies packaged persistence, not a live sign interaction or human client.

No player-facing pen controls or automatic adoption enabled. P3 stays about68%
partial (30/39 resource milestones,1/4 whole groups). Next: bounded authorized
pen interactions before automatic livestock founding/feed/output. The slaughter-
product question remains pending; no answer assumed. See PEN_PROTECTION_LIFECYCLE.md.
Feature backup/hosted CI follows the verified checkpoint; main stays unchanged.

## October17 — initial herd ages and monotonic native views verified

Known initial animals now reconcile from the saved herd period and original
remaining maturity, alongside newborns. Shared versioned native age metadata
prevents older in-flight views making adults young again. Same-period repair
avoids unnecessary clock writes; contradictory/malformed scope is reported,
not overwritten. Valid positions and other animals continue after an age issue.
Names, leashes, AI/gravity/invulnerability, ownership and assets are untouched.

Member paging adds an indexed latest-period read; missing checkpoints refuse
instead of manufacturing period zero. Schema87 unchanged; no migration/backfill/
reset. Added13 SQLite/age/observer cases. Focused52 passed. Windows .\mvnw.cmd
clean verify -B -ntp passed1,776 tests (1,774 unit/two packaged integration), zero
failures/errors/skips;12:31, finished2026-10-17T10:32:48-04:00. Source/test Java
frozen during full verification.

Guarded Paper26.2 build129 passed10:35:01 EDT: three real initial cows corrected
from intentionally wrong native ages, exact youngster period8/9 behavior, preserved
names/movement/protection flags, actual native stale-view rejection and unspawned
EntitySnapshot(World) age/lock/PDC round-trip. Automatic newborn/restart/real paid
feed/output/missing/confirmed-death regression also passed. Not automatic adoption,
actual chunk unload, human-client or JVM crash proof. All147 main tables retained,
143 exact; ordinary clock/runtime/capture/ore metadata only, no aging. Integrity/
FKs clean; native server stopped/settings restored/helper archived/backups kept.
Only exact private test animals cleaned up. Human server untouched.

Prior11ad06b passed hosted Windows/Linux, run39039309999. Current feature backup/
CI follows this checkpoint. P3 remains about68% partial (30/39 resources,1/4 whole
groups): pen interactions, automatic adoption/founding/feed/output, intentional
slaughter, wider products and specialization remain. Next: authorized bounded
bulk pen protection. Slaughter product-accounting answer is pending; no answer
assumed and independent work continues. See LIVESTOCK_SAVED_AGES.md.

## October17 — safe automatic physical newborns verified

Saved paid births now project as actual nearby cows/chickens on safe loaded
terrain. One birth handoff/eight-job pages, ownership commit/publication before
native add, activation before pending movement release, saved age lock/maturity,
exact retry/positive recovery and no missing-body replacement. No adoption,
terrain/chunk loading, free head/goods/cash or invulnerability grant. Shutdown
and unloaded-world cleanup retain only immutable identity work in SQL-owner order.

Added24 state-machine/temporary-SQLite/Paper geometry-age cases. Focused38 and40
passed. Three initial headless fixture errors required native block registry
seams, retaining default production properties. Initial full run intentionally
stopped for a discovered prepared-world-reference edge case; only that Maven
process/test child were interrupted. Final Windows .\mvnw.cmd clean verify -B
-ntp passed1,763 tests (1,761 unit/two packaged integration), zero failures/errors/
skips;12:29, finished2026-10-17T10:03:05-04:00. Source/test Java frozen.

Guarded Paper26.2 build129 passed10:03:55 EDT: controlled nearby gating, two real
cows automatically claimed/added/activated on existing safe terrain, normal
movement release/no granted invulnerability, owner/controller restart without
another UUID, actual paid active-period feed/products, saved individual maturity,
missing body never replaced, actual sibling death and exact assets/headcounts.
Not actual client/chunk unload/JVM crash, initial animal age or automatic adoption
proof. Schema87 unchanged:147 existing main tables retained/143 exact; ordinary
clock/runtime/capture/ore metadata only, no aging. Integrity/FKs clean. Fixture
stopped/settings restored/helper archived/backups retained; only its explicitly
identified animals cleaned up. Human server untouched. See LIVESTOCK_NEWBORNS.md.

Priorb00baa1 passed hosted Windows/Linux checks, run39036771535. Current backup/CI
follows this checkpoint. P3 stays about68% partial (30/39 resources,1/4 whole
groups). Next: initial adopted-animal age, automatic adoption/feed/output and real
bakery ingredients. Business-slaughter product accounting question is pending;
no answer assumed, and these independent tasks can continue.

## October17 — confirmed livestock distance and retained herds verified

Saved farm animals positively observed outside an editable working radius pause
products/breeding until return. Ownership/headcounts/aging remain; other eligible
animals work. Unknown/unloaded sightings alone never establish departure or death.
Bounded main-thread observer looks up16 saved living UUIDs per page, never scans
terrain/loads chunks/adopts animals, and retains exact failed-save batches.

Optional economy.yml livestock-working-area.radius-blocks defaults to32 (inclusive
3D,1..256); legacy files are not rewritten. SQL-owner work uses configured radius,
actual world presence and trusted death capture. Cohorts still mature while away;
real feed/output/storage/reserve accounting and receipt replay remain intact.
Additive schema87 creates an empty latest-positive-position table, retaining old
animals/births/assets/history without guessing old sightings or repairing gaps.

Added21 automated cases. Focused52 and151 passed. Windows .\mvnw.cmd clean verify
-B -ntp passed1,739 tests (1,737 unit/two packaged integration), zero failures/
errors/skips;12:24, finished2026-10-16T23:36:49-04:00. Source/test Java frozen.
Guarded Paper26.2 build129 passed2026-10-17 09:28:12 EDT: actual newborn/death
regression, bounded positive sightings, own test cow moved away/returned inside a
loaded chunk, saved away evidence through owner restart before new observation,
unchanged heads/ownership/goods/cash. Not automatic production/adoption, actual
chunk unload or JVM crash proof. Test save86->87 retained146 predecessor tables,
142 exact; only ordinary clock/runtime/capture/ore metadata changed, zero aging.
Integrity/FKs clean; new main presence empty. Fixture stopped/settings restored,
helper archived/backups retained. Human server untouched.

Earlier936fd54 and9c1a415 both passed hosted Windows/Linux checks (runs38996032941,
38998105994). Current feature backup/CI follows this checkpoint. P3 stays about68%
partial (30/39 resources,1/4 whole groups). Next: safe physical newborn placement/
reconciliation, automatic adoption/feed/output and real bakery ingredient customers.
No decision pending. See LIVESTOCK_WORKING_AREA.md for files/settings/limits.

## October16 — physical newborn identity handoff verified

Schema86 adds claim-before-spawn identities, retained failed aliases and individual
offspring death receipts. The SQL owner publishes protection before returning a
committed claim. Missing bodies never authorize replacement. Confirmed offspring
losses remove their exact saved cohort member and survive restart without adding
goods or money. New claims require trusted capture history and the origin world.

Added24 temporary-SQLite/owner cases. Focused163 affected migration/newborn tests
passed. Windows .\mvnw.cmd clean verify -B -ntp passed1,718 tests (1,716 unit/two
packaged integration), zero failures/errors/skips;11:59, finished
2026-10-16T23:14:31-04:00. Source/test Java frozen throughout verification.

Guarded Paper26.2 build129 probe passed23:15:00: real prepared UUID committed and
published before native addEntity, once-only activation, positive never-spawned
failure/retry with retained alias, actual native offspring death, exact surviving
sibling/head/goods accounting and SQL-owner restart. Not automatic adoption,
general reconciliation or an actual JVM crash test. Marked test save upgraded
85->86:143 predecessor tables retained,139 exact; only ordinary clock/runtime/
capture/ore metadata changed, no aging. Three new main tables empty; integrity/FKs
clean. Fixture stopped/settings restored/helper archived/backups retained; human
server untouched. See LIVESTOCK_BIRTH_PROJECTIONS.md.

Prior936fd54 hosted Linux passed; Windows still running when checked. Current
feature backup/hosted checks follow this checkpoint. P3 stays about68% partial
(30/39 resources,1/4 whole groups). The user approved pausing products/breeding
only for animals positively observed outside an editable working radius,
retaining ownership and treating unloaded observations as unknown. Next: this
eligibility rule and safe live herd integration; no decision is pending.

## October 16 — permanent newborn identities verified

Each new simulated birth now receives a deterministic permanent identity in the
same SQL transaction as its real feed/work costs, products and herd checkpoint.
Business ID, birth period and sibling ordinal establish identity; saved maturity
stays fixed through retuning and restart. Bounded chronological pages prepare the
physical adapter. No Paper animal, ownership, new birth or free goods are created
by recording these already calculated outcomes.

Additive schema 85 creates an initially empty livestock_births table. Older
anonymous births, headcounts, goods and histories stay intact and are reported as
unmapped. Coverage is a count diagnostic, not physical-presence certification;
validated identities and real reconciliation are still required before activation.
No automatic historical backfill, missing-animal replacement or data reset.

Added 13 domain/temporary-SQLite cases for stable IDs, sibling paging, bounds,
rollback/retry, restart/replay/retuning, legacy coverage and migration/corruption.
Focused 102 checks passed, then 77 affected migration/newborn checks passed.
The first full run exposed seven old comparisons in five fixture lists missing
only the new empty table. Verified every old value identical, extended explicit
empty-table assertions and retained every prior-row comparison. Final Windows
.\mvnw.cmd clean verify -B -ntp passed 1,694 tests (1,692 unit/two packaged
integration), zero failures/errors/skips; 10:43, finished 2026-10-16T22:40:40-04:00.
Source/test Java stayed frozen throughout that final run.

Guarded Paper 26.2 build 129 regression passed at 22:41:56: actual market/player
saves, bucket/egg separation, selective synthetic death/respawn, once-only seller
credit and owner/controller restart remain correct. Separate fixture herds retain
exact stable newborn IDs/maturity and zero unmapped births after restart. This is
not physical offspring, actual client death/crash or automatic adoption proof.
Main disposable save upgraded 84->85: all 142 predecessor tables preserved, 138
exact; only ordinary clock/runtime/capture/ore metadata changed, no aging. The new
main registry is empty; integrity/FKs clean. Fixture stopped, original settings
restored, helper archived, backups/synthetic saves retained. Human server untouched.

Previous market commit cbafaac passed both hosted Windows/Linux builds:
https://github.com/blueninjas24/Civilizations/actions/runs/38994123234 . Current
batch hosted verification follows its feature-branch backup. P3 remains about
68% partial (30/39 resources, 1/4 whole groups). Changed groups: domain birth IDs,
SQL registry/production transaction, schema migration, migration fixtures/tests,
native regression and project records. Next: physical claims and confirmed
individual offspring losses, then safe live adoption/feed/output and real bakery
customers. No new decision is required. See LIVESTOCK_BIRTH_IDENTITIES.md.

## October16 — recoverable milk/egg player market verified

Connected /civ market to nearby eligible recorded herd stock. A protected 54-slot
menu shows bounded expiring offers, editable prices and buyer-supplied buckets.
Explicit one-click orders persist intent, recheck saved supplier site/permission/
payment/room, deliver once, prove the saved exchange, then credit the seller once.
Failed saves retry the current stage; accepted orders recover after restart or
market disablement. Exact marked quantity is checked before requesting seller
credit. Uncertain inventory/restore or ledger disagreement retains REVIEW instead
of manufacturing another payout/refund. Dead players wait until respawn.

New goods occupy separate marked stacks. Only the exact operation/buyer/product/
quantity receive approved temporary death retention. Older goods, XP and normal
server keepInventory rules stay ordinary. Already-kept copies are not duplicated;
extra unexpected marked quantities remain ordinary drops. Confirmation removes
only the temporary marker. Guards cover movement, vanilla interactions, milk
consumption and recipe-book autofill. Food aid/sale/barter starts cannot overlap.

Added29 automated cases: storage separation, routing, exact death/consumption/
recipe-book behavior, saved proof and purchase recovery. Focused61 and42 checks
passed. Final Windows .\mvnw.cmd clean verify -B -ntp passed1,681 tests (1,679
unit/two packaged integration), zero failures/errors/skips;09:23, finished
2026-10-16T22:11:50-04:00. Initial full run intentionally stopped to add the
pre-credit quantity guard; source/test Java frozen during final verification.

Guarded Paper26.2 build129 market probe passed22:13:02: actual native item/PDC/
player files and separate SQL owner, menu double-click, withheld preparation and
exchange saves, synthetic selective death/respawn, real buckets/egg separation,
native reload and owner/controller restart. Exact two completed orders, seller
cash1600/800 and milk1/eggs1 closing stock; integrity/FKs clean. Not a human-client
death, actual JVM crash or automatic herd test. First probe setup incorrectly
shared two business addresses; corrected only the excluded helper and reran.
No application behavior changed to bypass that valid uniqueness constraint.

Main disposable fixture remains schema84:142 predecessor tables preserved/135
exact, only ordinary clock/active-world/capture/ore metadata and one age month.
Existing identities/goods/cash/history retained. Fixture stopped, original25577
settings restored, helpers archived, backups/synthetic saves retained. No helper
blocks/animals/online players changed. Human server untouched; no migration here.
Previous product-sale1722790 hosted CI passed (run38980634799); foundation38f9763
Linux passed/Windows still running at last check. Current batch hosted run pending.

P3 stays~68% partial (30/39 resources,1/4 whole groups). Player retail is connected;
automatic adoption, permanent physical offspring/reconciliation, scheduled feed/
output and bakery customers remain. Changed groups: market controller/menu,
selective item guard/storage planning, owner-site DTO, command/startup routing,
old food-transfer mutual exclusion, tests and docs. Next: newborn identity and
safe herd activation. No design decision pending. See LIVESTOCK_MARKET.md.

## October16 — configurable nearby market and inventory exchange foundation

Implemented bounded SQL-owner dairy/chicken quotes from actual eligible staffed
business stock in present worlds. Fresh reservation rechecks saved supplier,
position, editable order/payment terms and stock. Accepted orders retain their
exact terms for recovery after settings/availability changes. Unknown cancelled
intent creates neither a receipt nor goods/cash. No Paper/JDBC crossing of threads.

New economy.yml livestock-market section controls introductory milk/egg prices,
egg order count, radius, candidate cap and offer lifetime. Cash conversion reuses
purchases.trade-value-per-emerald. Defaults: one milk for2 emeralds,4 eggs for1;
these are editable introductory quotes, not milk/egg bargaining. Missing section
in installed files stays disabled. Startup wires these settings to the owner API;
no player command/menu or purchase controller is exposed yet.

Pure storage planning debits exact plain emeralds and one real bucket for milk,
then merges/splits deliveries within actual item limits. Custom items stay intact;
insufficient room refuses before mutation. Paper snapshots verify complete writes,
restore exact contents after provider failures and never overwrite a changed
inventory when no apply occurred. Armor/offhand/container inventories are untouched.
Initial headless adapter tests encountered a Paper block-registry lookup through
Material.isAir; removed that unnecessary lookup by comparing the three air constants.
No existing application behavior was weakened for test convenience.

Added22 tests, including a seeded3,000-inventory conservation sweep. Focused114
initial and35 boundary/owner tests passed. Windows .\mvnw.cmd clean verify -B -ntp
passed1,652 tests (1,650 unit/two packaged integration), zero failures/errors/skips;
08:19, finished2026-10-16T19:58:48-04:00. Source/tests frozen during final verification.

Guarded Paper26.2 build129 inventory probe passed21:44:00: real ItemStacks and
virtual CraftInventory snapshots, native egg maximum16/milk1, metadata retention,
full-space refusal, multi-stack eggs, stale protection and partial-provider restore.
Not a live-player delivery/death/save test. No helper world/entity/player mutations.
Fixture83->84 uses the previously implemented empty sale-ledger migration;141
predecessor tables retained/137 exact, only clock/runtime/capture/ore metadata,
age delta0; integrity/FKs clean. Fixture stopped, properties restored/helper archived,
backups retained. Human server untouched. Schema stays84; no new migration here.

P3 stays~68% partial (30/39 resources,1/4 whole groups). Next: actual purchase
controller/menu and approved selective pending-goods death retention, then safe
live adoption/feed/output. User approved the death rule while verification finished;
no decision remains pending. No confirmed player trading or completed livestock gate.

## October16 — saved player proof for livestock receipts

The existing bounded, read-only SavedAidProof parser now accepts only the new
PendingLivestockSale key in addition to its three existing food-transfer keys.
It still requires exact full-file root BukkitValues evidence. The NBT parser,
compression, file limits, path rules and supported format are unchanged. No live
player inventory/command/listener or automatic sale was enabled.

Three added temporary synthetic-save cases exercise all products/stages, exact
player/key/value matching, nested and other-key spoofs, unsupported-key refusal,
duplicated proofs and corrupt/truncated compressed files. Focused14 tests passed,
including the seven existing parser tests and four receipt tests. Final Windows
.\mvnw.cmd clean verify -B -ntp passed1,630 tests (1,628 unit/two packaged
integration), zero failures/errors/skips;07:46, finished2026-10-16T19:16:50-04:00.
No source/test changes during that final run;20 new tests across this batch.
Schema remains84; no new migration or actual server/world/database operations.
Native player save/crash delivery remains an unfinished integration gate; this
is new-key coverage for the existing format, not proof of a live inventory exchange.
P3 remains~68% partial. Next: owner handoff, configurable sale quoting, inventory
exchange/recovery and the player menu, alongside safe live herd adoption/output.

## October16 — milk and egg sale ledger

Added saved reservations/completion/cancellation for real herd product sales,
exact buyer-supplied milk-bucket receipt terms, and storage protection so later
work cannot occupy the space needed to cancel an order. No seller revenue before
confirmed exchange; retries/restarts cannot repeat a completed payment or goods
return. Wrong supplier/world/product, unavailable staff/stock and changed terms
are refused. SQL or arithmetic failures roll back the entire transition.

Schema84 is additive and starts with an empty sale ledger. Old-schema regression
fixtures now explicitly assert that new table empty while retaining every old-row
comparison. Existing goods, cash, herd identity, histories and world files remain.
No actual server database was touched. No new inventory/menu/command is enabled;
the future Paper adapter must verify saved player proof, inventory and price terms.
Automatic adoption, physical newborn reconciliation and live feed/output remain
unfinished. P3 stays~68% partial (30/39 resources,1/4 whole groups).

Final Windows .\mvnw.cmd clean verify -B -ntp passed1,627 tests (1,625 unit/two
packaged integration), zero failures/errors/skips;08:47, finished2026-10-16
19:07:55-04:00. Seventeen new cases passed. Focused57 trade/herd/recovery/storage
cases passed before two final storage/quantity cases. One initial fixture omitted
the required employment-end reason; switched it to the existing termination API,
without changing application behavior. Source/tests frozen during final run.
No native smoke needed for this SQL/domain-only change; no Paper API changed.
Prior81be4a0 source passed hosted Windows and Linux verification:
https://github.com/blueninjas24/Civilizations/actions/runs/38967519430 .
Earlier80fe3f0 Windows was cancelled, not certified; later81be4a0 passed both.

## October16 — milk container decision recorded

User approved buyer-supplied empty buckets for milk purchases. Canonical design,
simulation rules, livestock adapter contract and current checklist now agree:
one supplied bucket per milk bucket, real recorded milk stock and payment,
no free iron container, no payment taken when the required bucket is missing.
Eggs need no container. The container question is resolved; live livestock
production and player product trading remain unfinished. P3 stays~68% partial.

Documentation only: no Java/config/schema/artifact changes, no database/server
operations and no new test/build run. Previous verified source remains939f805
with1,610 local tests. Latest hosted verification could not be refreshed because
GitHub DNS was unavailable in this sandbox; no hosted success is inferred.

## October16 — hosted verification allowance after measured Windows slowdown

Raised only the CI job allowance45->60 minutes; retained both operating systems,
all wrapper checks, timeouts in tests and dependency pins. The older7f523ff Windows
job explicitly exceeded45 minutes while still progressing through passing tests
(WarehouseSuppliesTest at cancellation); its Linux job passed. The later03b4d86
loss workflow passed both platforms. This is measured hosted disk variability,
not evidence that a test should be removed or that all future runs will finish.
Evidence: https://github.com/blueninjas24/Civilizations/actions/runs/38960407636 .

Application/test source and packaged artifact remain exactly the locally/native
verified939f805 recovery revision: final1,610 tests, schema83. Validated the workflow
diff changes only its numeric allowance. No extra Maven rerun for this CI/docs-only
commit. New hosted verification remains pending; no two-platform pass claimed for
this latest head. P3 remains~68% partial; livestock substeps are in PROGRESS.md.

## October16 — durable capture eligibility and safe owner shutdown

Schema83 adds initially empty livestock_capture metadata. Startup writes a dirty
capture marker; only new empty history or an already trusted clean stop can be
eligible. Legacy herds with no fence, unclean stops, explicit gaps and unresolved
saves cannot silently regain trust. No automatic repair/reset permission. Cached
status additionally requires no unsaved staged loss; every known death is staged
before its Paper queue delay, so the first saved loss cannot briefly certify the
others. Exact successful retry clears its pending identity. Pre-readiness capture
loss/expiry marks a durable gap; repeated gap notifications coalesce.

Confirmed death saves stay available when untrusted. Shutdown drains accepted work
before persisting its fence. A repeated close outside the owner now waits for the
original drain; owner callbacks still return without waiting for themselves. This
fixes an existing Windows database-file lock race exposed by the extra write.
/civ debug status reads cached capture eligibility without JDBC/world operations.
The trusted herd backend remains explicit; future live callers must require this
gate plus safe physical reconciliation. Neither adoption nor live output is enabled.

Eleven new executed cases: nine repository/owner/migration and two handoff gap/
staging cases. Older-save tests retain predecessor comparisons and explicitly
assert the new table empty. Focused148 passed before final staging coverage;
latest focused44 and final Windows .\mvnw.cmd clean verify passed1,610
(1,608 unit/two packaged integration), zero failures/errors/skips;07:52, finished
2026-10-16T17:03:38-04:00. Production/test Java frozen for that final run. Earlier
1,608 full pass was superseded after review found the pre-owner staging gap.
One new fixture initially assumed legacy trust; corrected only its explicit prior
clean-history seed. The existing callback-close test exposed the real shutdown race.

Guarded Paper26.2 build129 passed17:04:31: actual owner/start/status, multi-observation
pending gate, immediate gap, owner-callback/external close drain, durable restart
warning and simulated prior dirty marker. No deliberate JVM crash or physical
reconciliation certification. No helper world/entity mutations. Schema82->83 audit
retained140 predecessor tables/137 exact; normal clock/runtime/ore metadata only,
age delta0, integrity/FKs clean. Main new marker is clean/trusted for its empty
herd history. Fixture stopped, properties restored/helper archived/backups retained.
Existing OSHI warning only; no Civilizations/probe/listener failure. Human server
untouched. Branch codex/phase3-livestock-recovery; main unchanged.

P3 stays30/39 resources,1/4 whole groups/~68% partial. Livestock foundation substeps
are shown in PROGRESS; automatic adoption, physical newborn UUIDs/reconciliation,
scheduled founding/feed/output and milk/egg customers remain. Capture trust does
not certify plugin-free play, external world edits/restores or unseen animals.
Before player milk trades, the pending container rule needs a decision; no free
iron buckets may be created. CI allowance maintenance follows separately: the
older harvesting Windows log shows continuing passing tests until45-minute
cancellation, Linux passed, and the later loss workflow passed both platforms.
See LIVESTOCK_PROTECTION.md and DECISIONS_AND_OPEN_QUESTIONS.md.

## October16 — hatched and explicitly created animals keep protective origins

Supported EGG/BREEDING/SPAWNER_EGG/DISPENSE_EGG/COMMAND spawn events now retain
anonymous protective origin evidence. Normal spawning/hatching, amounts, items
and experience are unchanged. Cancelled/unsupported events are untouched; no
player owner is guessed. Original native reasons also protect older untagged
animals from these sources. Existing/malformed protective values are retained.

Captures now copy the native reason name into plain-Java immutable observations;
absent information is UNKNOWN. No missing tag or uncertain CUSTOM/DEFAULT source
is reinterpreted as natural. Real ownership/pen authority and safe observation
remain mandatory; this does not enable adoption. Physical-loss deferred/shutdown
logging now retains exception causes without changing exact retry behavior.

Four new executed cases; focused30 passed. Full Windows .\mvnw.cmd clean verify
passed1,599 (1,597 unit/two packaged integration), zero failures/errors/skips;
07:42, finished2026-10-16T16:37:40-04:00. Production/test Java frozen during final
verification; no behavior changes for test convenience. Schema unchanged82.

Guarded Paper26.2 build129 passed16:38:12: actual native explicit-origin spawns,
production event tags, original-reason fallback with tags removed, PDC serialization
and immutable origin capture. Existing three-colour actual egg laying/drop control,
synthetic both-hand bucket/breeding and player-normal behavior checks passed too.
This is explicit native spawn-reason coverage, not a thrown-projectile hatching or
human-client/stock-trading test. All helper animals/items removed, ticket released,
no block changes. All140 predecessor tables retained/137 exact; only ordinary
clock/runtime/ore metadata changed; age delta0, integrity/FKs clean. Fixture stopped,
properties restored/helper archived/backups retained. Existing OSHI warning only;
no Civilizations/probe/listener failure. Human server untouched.

P3 stays30/39 resources,1/4 whole groups/~68% partial. Next: unclean-capture recovery
before safe physical herd reconciliation/live production. Physical newborn UUIDs,
automatic opening/feed/customer loops and wider industry/specialization remain.
No decision needed. Branch codex/phase3-livestock-origin; main unchanged.
Prior harvesting7f523ff hosted Linux passed; Windows still running at16:38. No
hosted pass claimed for newer loss/origin revisions. See LIVESTOCK_PROTECTION.md.

## October16 — confirmed original-herd losses reach saved production

Confirmed uncancelled native cow/chicken deaths now cross the main-thread/SQL-owner
boundary as immutable observations. Original saved UUIDs lose one adult or their
actual young cohort. Unknown/player/unassigned animals create no loss or ownership;
missing observations never establish death. Cross-world death records the actual
location world without changing original ownership/world history or earning time.

Loss receipts and old production history remain immutable. Pending losses reduce
current heads immediately after commit; future paid production checkpoints save
the reduced state and apply losses atomically. Exact event/work replay and restart
do not subtract twice, refill animals or create goods/cash. Failed loss/application
writes roll back. Disabled/absent worlds earn no work or aging from the event.
Main-thread capture changes no native death, loot or XP; bounded unbound buffering,
serial owner requests, delayed exact retry and graceful draining include diagnostics.

Schema82 adds one initially empty loss table/index referencing original animals
and before/application periods. All prior rows remain. Every affected old-save
comparison still checks every predecessor row and explicitly asserts the new table
empty; upgrade failure/retry tested. No production/world reset or YAML changes.

Eleven new executed cases: seven accounting/migration/restart/failure cases, four
capture/retry/readiness/shutdown cases. Focused111 passed; final Windows
.\mvnw.cmd clean verify passed1,595 (1,593 unit/two packaged integration), zero
failures/errors/skips;08:09, finished2026-10-16T16:26:19-04:00. Production/test Java
frozen during final verification. Helper-only cleanup tags its own disposable loot.

Guarded Paper26.2 build129 passed16:27:16: actual native adult/young/player cow
deaths, real SQL-owner listener, one retained survivor, reduced feed/output,
absence, exact work/death replay and restart. No human client, automatic adopter,
physical offspring or crash-gap certification. All helper animals/tagged loot
removed; no blocks changed, ticket released. Schema81->82 audit retained139
predecessor tables/136 exact; only normal clock/runtime/ore metadata changed;
age delta0, integrity/FKs clean. New main-fixture loss table empty. Fixture stopped,
properties restored/helper archived/backups retained; OSHI warning only, no
Civilizations/probe/listener failure. Human server untouched. Branch
codex/phase3-livestock-losses; main unchanged.

P3 stays30/39 resources,1/4 whole groups/~68% partial. Next: egg/spawn-origin
protection, then unclean-capture recovery and physical herd reconciliation before
live adoption/output. The death queue is in memory until SQL commit: do not infer
crash-gap safety. Newborn physical UUIDs, authorized transfers, automatic funding/
feed/output and milk/egg trading remain separate gates. No decision needed.
See LIVESTOCK_PROTECTION.md.

## October16 — village milk/eggs and player-bred animal protection

Implemented the approved village-only product rule: an empty bucket in either
hand cannot milk a saved village cow, and saved village chickens cannot lay a
second physical supply of white/brown/blue eggs. Player/unclaimed/unsupported
animals, other items and naming/leashing remain normal. No inventory edits,
SQL/world scans per event, arbitrary item deletion, stock credits or death-drop
changes. Existing livestock enable/readiness gates apply; old files opt out.

Successful player-bred cow/chicken offspring now retain anonymous entity-PDC
protection evidence. Protected-parent evidence propagates; cancelled/unsupported
births remain untouched and existing values are preserved. No player IDs/names,
ownership, items or experience are changed. Captures include an immutable flag;
missing old tags never prove natural origin. Protection capture remains active
when production is disabled so future adoption cannot ignore those births.

Twelve new executed cases (seven product guards, four provenance, one captured
adoption-protection case). Focused28 passed. Full Windows .\mvnw.cmd clean verify
passed1,584 (1,582 unit/two packaged integration), zero failures/errors/skips;
07:46, finished2026-10-16T11:15:33-04:00. Production/test Java frozen during this
run. Excluded helper initially had ambiguous Plugin/Listener cleanup overload;
fixed its explicit Plugin cast, without any application/test changes.

Guarded Paper26.2 build129 passed16:10:23: saved SQL-owner identities and production
listener registration, actual three-colour native chicken laying/cancellation,
normal physical eggs for unclaimed hens, synthetic both-hand bucket/breeding
event dispatch, real PDC serialization and immutable capture. This is not a human
client milking or completed stock-trading check. All created animals/items removed,
chunk ticket released, no blocks changed. Schema81/no migration; all139 predecessor
tables retained/136 exact, only ordinary clock/runtime/ore metadata changed; age
delta0, integrity/FKs clean. Fixture stopped/properties restored/helper archived,
backups retained. Existing OSHI warning only; no Civilizations/probe/listener failure.
Human server untouched. Prior6556e2c hosted Windows/Linux checks both succeeded:
https://github.com/blueninjas24/Civilizations/actions/runs/38917154524 . Those results
belong to the preceding startup revision, not this newer harvesting revision.

P3 remains30/39 resources,1/4 whole groups/~68% partial. Collection decision resolved;
next: confirmed physical losses and safe herd reconciliation. Live automatic
adoption/founding, physical offspring, output scheduling, milk/egg trading and
broader industry/specialization gates remain. Cold-start freeze/publication must
precede live herd control; no live adopters enabled. Branch
codex/phase3-livestock-harvesting; main unchanged. See LIVESTOCK_PROTECTION.md.

## October16 — protected livestock startup and real animal evidence

Approved chicken/dairy businesses now share existing startup grants: unchanged
common spending caps and food-money protection, no extra money. Installed YAML
lists remain intact. A trusted atomic opening saves the paid business, site,
store, original worker and already-confirmed animal roster together. No free feed,
products, animals or skills. Real ordinary feed purchases and delayed delivery
can supply that herd; this is not an automatic starter-feed policy or live founder.

Failed final writes roll back every opening row and payment. Exact restart replay
returns current assets/cohorts and an ended original job without rehiring. Separate
legacy grant/herd setups cannot retrofit this opening or acquire a new job through
replay. Existing standalone herd opening retains its contract. Schema stays81;
no migration or existing-stock conversion.

Paper now captures bounded immutable cow/chicken identity, position, age and
name/leash evidence on the main thread from an already-loaded entity chunk.
No chunk loading, SQL, claims or animal mutations. Incomplete/over-budget/crossed
observations are unknown, never proof of death. Ownership, player breeding,
protected pens, authority and safe physical reconciliation remain separate gates.

Validation: 14 new executed cases (eight atomic funding/herd, one configuration,
five Paper capture); final focused47 passed. Full Windows .\mvnw.cmd clean verify
passed1,572 tests (1,570 unit/two packaged integration), zero failures/errors/skips.
Final build duration 09:36 min; finished 2026-10-16T10:34:41-04:00. Production/test Java frozen
for final verification. An earlier full pass preceded the final legacy replay guard
and was superseded by this run; no old result is claimed for the latest source.

Guarded Paper26.2 build129 passed actual main-thread cow/chicken capture, named/
young evidence, trusted fixture ownership, real protected grants, paid feed and
delivery, abstract milk/egg output, restart replay and absent-world pause. Six
helper-created animals were removed and the test-only chunk ticket released;
no blocks changed. 139 predecessor tables retained/136 exact; ordinary changes civilization_time, economy_runtime, ore_tracking; normal age-month delta0. Exact audit, integrity/FKs clean; fixture stopped,
properties restored, helper archived, backups retained. Existing OSHI Windows
counter warning only; no Civilizations/probe/listener failure. Human server untouched.

Previous craftsman revision d53fbea passed both hosted Windows/Linux checks:
https://github.com/blueninjas24/Civilizations/actions/runs/38908644113 . Windows
26:37/Linux2:40 confirms the earlier timeout correction without removing tests.
These hosted results do not claim verification of this newer livestock revision.

P3 stays30/39 resources,1/4 whole groups/~68% partial. Live automatic adoption,
founding, physical births/deaths, harvesting and product-customer integration are
unfinished. Before live milk/egg output, player harvesting versus stock-backed
trade needs the explicit decision in DECISIONS_AND_OPEN_QUESTIONS. No bucket/drop
behavior changed. Branch codex/phase3-livestock-startup; main unchanged.
See LIVESTOCK_PROTECTION.md for exact boundaries/settings.

## October16 — villages can train their first craftsman

Connected one prospective first-craftsman course to real current fitting customers,
feasible startup, observed crafting-table evidence and future practice supplies.
Existing food/material reserves, student limits, age, employment and active-owner
guards apply. Editable new-file settings consume one real timber per productive
economic period for one experience toward 10. No free initial skill or retrospective
backlog practice. Absence, stale/missing markers, lost customers/funding and supply
shortages pause safely; a taken first firm releases the learner.

SQL-owner automatic review now sponsors training when appropriate, then uses the
existing main-thread confirmation and paid opening after qualification. Actual
productive craft work earns bounded once-only further experience. Installed YAML
lists, saved catalogues and accepted course/history terms remain intact. Extra
employee hiring remains an explicit staffing-list opt-in; first paid inputs still
require public surplus. Wider products/household furnishing demand are unfinished.
See PROCESSING_WORKSHOPS.md for exact settings, opt-ins and limits.

Schema 81 adds two initially empty founder review/course tables and their index,
with deferred sponsorship links. No old people, jobs, cash, goods or history are
rewritten. Transactional migration/enrollment/practice failure and retry preserve
all prior rows. Historic migration tests explicitly assert new tables empty while
retaining every predecessor-row comparison.

Validation: nine new cases (eight training/migration/pause/rollback/SQL-owner,
one old-config opt-out); actual productive crafting learning also verified.
Focused 47 passed. Full Windows .\mvnw.cmd clean verify passed 1,558 tests
(1,556 unit/two packaged integration), zero failures/errors/skips; 7:52, finished
09:29:19 ET. Production/test Java frozen during final verification; later edits
only docs and the excluded helper. One early new config test changed the wrong
key (career-study rather than automatic-career-study); corrected its fixture
without changing application behavior.

Guarded Paper 26.2 build 129 passed 09:31:03 ET: real SQL-owner training with ten
actual practice expenses, restart, main-thread crafting-table confirmation,
automatic paid opening, produced fitting customer, further productive work learning,
replay and absent-world pause. The first helper advanced its test clock beyond
real time, so the saved-clock guard correctly deferred the customer's work.
Corrected only its time origin and reran; application behavior unchanged.
Initial schema 80->81 audit retained 137 predecessor tables/134 exact; retry retained
139/136 exact. Only normal civilization_time/economy_runtime/ore_tracking metadata
changed; zero resident aging, clean integrity/FKs and empty new main-fixture tables.
Temporary air block restored, chunk ticket removed, fixture stopped, properties
restored, helpers archived and backups retained. Existing OSHI Windows counter
warning; no Civilizations/probe/listener failure on the passing run. Human server untouched.

CI fix 6706540 raises the job allowance from 20 to 45 minutes, retaining all tests.
The previous hosted Windows check explicitly timed out; Linux succeeded. New
hosted verification remains pending; no two-platform hosted pass is claimed.

P3 remains 30/39 resources,1/4 whole groups/~68% partial checklist completion:
training/opening is now connected, but broader products, livestock and specialization
remain. Next automatic livestock founding needs the pending decision: may chicken/
dairy businesses share existing food-protected startup grants under unchanged
common limits? No livestock grant eligibility change is implemented. Feature
branch codex/phase3-craftsman-training; main unchanged.

## October16 — qualified craftsmen open for real storage customers

Connected bounded first-craftsman opportunities, seeded qualified founder selection,
SQL-owner prepare/accept and the main-thread crafting-table opening cycle. Actual
current storage pressure and purchasing cash justify a prospect; staffed local
fitting stocks are considered first. A forecast neither reserves cash nor promises
a sale. Protected shared startup caps, current food assessment, real paid public
surplus, qualification, employment/study, marker freshness and storage/haul checks
apply. The entire business/site/worker/grant/material order/marker is atomic;
rollback/retry/restart preserves assets and prevents duplicate startup payments.

New-file craftsman qualification/opportunity sections are editable; old missing
sections opt out. Reuses small workshop bounds rather than adding a framework.
A qualified craftsman can now open through Paper and make/sell the fitting recipe.
No free experience or automatic vanilla profession change. First-owner study and
crafting work-learning remain next; an unskilled village still waits. The first
starter currently requires real protected public surplus, without a private fallback.
Wider useful products/household demand remain. Schema unchanged at 80.

Validation: 16 new cases (eight investment/persistence/session, six opening-cycle,
two configuration). Focused 154 passed. Full Windows .\mvnw.cmd clean verify
passed 1,549 tests (1,547 unit/two packaged integration), zero failures/errors/skips.
Build duration 07:37 min; finished 2026-10-16T08:44:08-04:00.
Production/test Java unchanged throughout final verification. Initial new fixtures
assumed unit storage weights, absent-world marker replay and an incomplete job end;
corrected to existing weighted storage, fresh present marker, assessed food and
employment contracts without changing application rules. The excluded native
helper initially attempted package-private stock inspection; corrected to read-only
SQL. No application visibility or behavior changed for helper convenience.

Guarded Paper 26.2 build 129 passed 08:44:38 ET: actual SQL owner, loaded main-thread
crafting-table confirmation, automatic qualified paid opening, paid public starter,
real timed production and business fitting sale/installation, replay and absence.
No training proof is claimed. Native save audit: 137 predecessor tables retained, 134 exact; ordinary changes civilization_time, economy_runtime, ore_tracking; resident aging 0 months. Integrity/FKs clean.
Temporary disposable air block restored, test chunk ticket removed, server stopped,
properties restored, helper archived and backups retained. Existing OSHI Windows
counter warning; no Civilizations/probe/listener failure. Human server untouched.

P3 stays 30/39 resources,1/4 whole groups/~68% partial checklist completion. These
are verified substeps within the still-open full craftsman gate. Next: supply-backed
craftsman training, livestock physical integration, wider products and specialization
acceptance. No design decision pending; main unchanged. Feature branch
codex/phase3-craftsman-investment. See PROCESSING_WORKSHOPS.md and STORAGE_UPGRADES.md.

## October16 — real paid craftsman storage customers (schema80)

Implemented the approved limited storage upgrades. Actual staffed businesses with
current storage pressure pay real craftsman/warehouse cash for produced fittings;
capacity increases only after delivery. Input/output stock and old catalogues stay
intact. Configurable retained cash, occupancy, count, original-baseline growth,
absolute capacity, source-review and hauling limits prevent unlimited expansion.
Paid cargo and installed improvements remain with the business in resale value.
Atomic receipts/replay, absent-world pause, current-clock spending and rollback
protect money and goods. Local demand includes real fitting customers without
counting a warehouse's own need twice or spending already-planned input money.

New-file editable recipe: three timber + one stone -> one storage fitting.
Default fitting invoice is20 Trade Value versus15 ingredient value; this is a
prototype gross ingredient margin, not guaranteed sales/profit. Missing upgrade
sections disable new purchases; installed files and old catalogues are preserved.
Schema80 adds one empty receipt table/index, rewriting no old economic assets.
Farms/herds require separate adapters and cannot use this generic capacity path.

Validation:16 new executed cases (14 persistence/runtime/migration/resale/demand,
two configuration), final focused154 passed. Full Windows .\mvnw.cmd clean verify
passed1,533 tests (1,531 unit/two packaged integration), zero failures/errors/skips,
in7:55 at08:27:28 ET. One earlier new test referenced the wrong receipt table;
corrected its SQL to recipe_flows without changing application behavior. Final
production/test Java remained frozen through verification.

Guarded Paper26.2 build129 passed08:28:00 ET: actual ingredient purchase/crafting,
SQL-owner automatic fitting payment, delayed installation, absence/restart and
exact disabled replay, preserving cash/stock and the buyer's old catalogue. No
helper world/entity/block mutations. Schema79->80 audit retained all136 predecessor
tables,133 exactly; only three normal clock/runtime/ore bookkeeping tables changed,
zero aging. Integrity/FKs clean. Fixture stopped, original properties restored,
helper archived/backups retained. Existing OSHI counter warning; no plugin failure.
Human server untouched. Earlier bakery GitHub CI: Linux succeeded, Windows was
cancelled; do not report a two-platform hosted pass for that revision.

P3 remains30/39 resource entries,1/4 whole groups/~68% partial checklist progress:
this completes storage customers, not the whole craftsman gate. Automatic craftsman
opportunities, qualified founding and gradual training remain next, then livestock
physical integration, wider products and specialization acceptance. No new decision
is pending. Feature branch codex/phase3-storage-upgrades; main unchanged.
See STORAGE_UPGRADES.md and PROCESSING_WORKSHOPS.md.

## October 15 — trained bakers can now open real paid bread businesses

Implemented the approved competition rule: a bounded forecast of actual recent
paid food orders can justify a bakery even when existing suppliers cover them.
Current population, post-startup funds, shared food budgets and hauling still
limit the forecast. It creates no purchases, consumers, money or guaranteed sales.
Admins can edit/disable the lookback, market share and unit cap in economy.yml.
Old installed files retain their settings and uncovered-demand behavior.

Connected gradual first-baker study and the live opening loop. One eligible
unemployed adult earns actual baking experience through future supply-backed
practice. Food/material reserves, known opportunities, staffing and study limits
apply; absence/unknown evidence pauses, old queued time awards nothing, and a
taken first-bakery opportunity releases the learner. Paper confirms a real loaded
smoker on its main thread; the SQL owner then rechecks and saves the paid opening.
Production runtime never loads chunks or alters blocks. Additional bakery employee
hiring remains an explicit existing staffing-list opt-in; this opens the working
founder. Cakes, milk/egg recipes and broader product selection remain unfinished.

Schema 79 adds empty founder course/review tables and an indexed paid-food history
lookup. No old people, goods, cash, ownership or history are rewritten. Migration
failure/retry, enrollment/practice rollback, quiet-market averaging, current cash,
world/population limits, cancellation, backlog and old-config behavior are tested.
History review is bounded at 3,200 receipts and defers instead of inferring demand
from a partial sample. Existing real food-order rotation remains unchanged.

Validation: 14 new focused cases passed. Final Windows .\mvnw.cmd clean verify
passed 1,517 tests (1,515 unit and two packaged integration), zero failures/errors/
skips, in 7:30 at 22:47:57 ET. An added test initially rewrote household totals
inconsistently; corrected it to advance an ordinary assessed period. No application
rule changed for that fixture correction. All production/test Java stayed frozen
through final verification; subsequent edits were docs and the excluded probe.

Guarded Paper 26.2 build 129 passed at 22:49:48 ET: actual SQL-owner study with ten
real practice expenses, restart, normal Paper bakery cycle/point confirmation,
private supplier payment, timed bread, exact replay and absent-world pause.
The first helper follow-up incorrectly supplied an absent world for fresh marker
confirmation; the guard correctly declined. Corrected only the excluded helper
and reran. Initial schema 78-to-79 audit retained all 134 predecessor tables,
131 exactly; retry retained all 136/133 exactly. Both changed only three normal
clock/runtime/ore bookkeeping tables, with zero resident aging. Integrity/FKs pass.
One existing disposable chunk was held by a test-only ticket; the temporary air
block was restored and ticket removed. Server stopped, original properties
restored, helpers archived and backups retained. Known OSHI Windows counter warning;
no Civilizations/probe/listener failure on the passing run. Human server untouched.

Paid farm food/ingredient customers now satisfy their checklist entry: owned crop
surplus earns actual village, warehouse and bakery payments, with causal delivery,
protected reserves and conserved cash/grain in the 80-period chain and starter
checks. P3 is 30/39 resource entries, 1/4 whole groups, about 68% partial checklist
completion. This is not a finished bakery/livestock/production phase gate.
Next: general craftsman customers, physical livestock integration, remaining
industry/product scope and specialization acceptance. One decision is pending:
may real businesses buy limited storage upgrades from the craftsman before Phase 4?
No storage-upgrade rule or new craft customer is implemented yet. Main unchanged;
feature branch codex/phase3-bakery-customers. See PROCESSING_WORKSHOPS.md.


## October15 — private bakery starter purchases verified; commercial decision pending

Implemented first grain-batch purchases from actual staffed local warehouses and
valid registered farms when preserved public surplus is insufficient. Real bakery
capital pays the supplier; no public-stock conversion or treasury refund. Shared
grant/food protection, editable starter price/cap/reserve, finite storage and local
hauling still apply. Source/price/recipe/route changes require a new proposal;
accepted public/private routes cannot switch to obtain another bundle.

Schema78 adds only the initially empty bakery_private_starters receipt table,
binding buyer-linked shipment, seller and versioned accepted terms. A new buyer's
delivery clock is enrolled at actual acceptance with target/completed0, allowing
normal timed delivery without inventing past work. Founding, money, grain, worker,
marker, clock, shipment and receipt are atomic, including final-write rollback.
Existing receipts/stores/owners/history remain; migration failure/retry tested.

Seven new cases. Focused56 passed; the first full run exposed historical fixture
assumptions about added tables. Corrected explicit empty-new-table expectations
and old-schema reconstruction without removing any predecessor-row comparisons.
Focused84 migration checks passed. Final .\mvnw.cmd clean verify passed1,503
(1,501 unit/two packaged integration), zero failures/errors/skips;7:07 at21:43:51 ET.
Production code unchanged afterward. Guarded Paper26.2 build129 private-source
proof passed21:44:30 ET: packaged defaults, actual SQL owner, paid private grain,
restart replay, timed bread, real food sale and exact absent-world pause. No helper
world/entity mutations or natural automatic-founding proof. Schema77→78 main-save
audit: all133 predecessor tables retained,130 exact; only normal clock/runtime/ore
bookkeeping changed, age delta0; new private receipt table empty. Fixture stopped,
original properties restored, helper archived. Known OSHI Windows counter warning;
no plugin/probe/listener failure. Human server untouched.

Earlier qualified/public-grain batch2522ee8 is backed up on GitHub. Current feature
branch: codex/phase3-bakery-private-inputs. Update status/progress/devlog and back up
this passing batch; do not merge/push main. P3 remains29/39 resources,1/4 groups/
~67% partial. Next training/opening work awaits the commercial question in
DECISIONS_AND_OPEN_QUESTIONS: may bakers compete for existing real paid village
food orders even when farms cover them? No new competition rule is active. Existing
uncovered-demand behavior remains. Craftsmen, livestock, wider industry and
specialization gates remain unfinished. See PROCESSING_WORKSHOPS.md.

## October15 — qualified bakery investment and actual farm grain supply

Implemented seeded qualified bakery preparation and atomic current acceptance,
including owner/worker experience, age, employment/study, current food customers,
protected funding, real inputs and fresh marker checks. SQL-owner session APIs
prepare/accept without sharing a connection. Default-file bakery terms and bread
recipe are editable; installed files and legacy storage catalogues are preserved.
No free opening grain, automatic vanilla profession change or automatic live
bakery founding. Public-surplus first inputs remain the current startup route.

Fixed registered hybrid farms stopping local industry reviews and being rejected
as warehouse producers. Only valid registered farm catalogues supply owned food/
grain; reserved farm tools are excluded. An80-period actual harvest/warehouse/
bakery/village-customer scenario conserves every grain unit and all actual cash.
Fixed starter planning overlooking preserved public harvested grain: both existing
public ledgers are reviewed, dedicated harvest stock debited first, combined
reserves retained and all payment/stock/identity changes rolled back together.
No balance copies, resets or schema migration; version remains77.

Fourteen added cases. Focused143 and SQL-owner10 passed. The first complete run
caught an old new-file storage expectation; updated it and explicitly tested
unchanged installed/missing-legacy catalogues (targeted3 passed). Final
.\mvnw.cmd clean verify passed1,496 (1,494 unit/two packaged integration), zero
failures/errors/skips;7:35 at21:10:54 ET. Production source unchanged afterward.
Guarded Paper26.2 build129 packaged proof passed21:12:10 ET: resource defaults,
actual SQL-owner founding, preserved grain, timed bread, real paid food, restart/
replay and absent-world pause. Backend proof only; no world/entity mutations or
natural automatic-opening proof. Save audit:133 old tables retained,130 exact,
only normal civilization_time/economy_runtime/ore_tracking changed; no ages changed.
Fixture stopped, original properties restored, helper archived. Existing OSHI
Windows counter warning remains; no plugin/probe/listener failure. Human server untouched.

See PROCESSING_WORKSHOPS.md. P3 remains29/39 resources,1/4 whole groups/~67% partial;
this is still a partial bakery integration gate. Next: first-batch private farm/
warehouse purchase, supply-backed founder study and the loaded-marker opening loop.
Craftsmen, livestock physical reconciliation, wider industries and specialization
acceptance remain. No new product decision is pending. Feature branch:
codex/phase3-bakery-investment; no main merge/push.

## October13 — protected processing startup and real bakery prospects

User approved the same shared protected startup grants for bakeries and the
general craftsman. Added both to new-file/default eligibility without increasing
single, period or lifetime caps. Installed lists and missing legacy opt-outs stay
unchanged. Recorded the decision; no startup-funding question remains pending.

Added read-only BakeryOpportunities: fresh complete nearby smoker, explicit meal
recipe, actual affordable surplus ingredients, protected startup funds and enough
real village meal/reserve demand after deducting the full startup investment.
Qualified operating local food stocks cover customers first. Bounded supplier
overflow defers rather than inferring shortage; old bakery identity cannot be
bypassed. Reviews/restarts mutate no assets and reserve no cash. Acceptance must
still choose a qualified founder, revalidate the opportunity and check Paper's
loaded marker. This is not an automatic founding adapter or a default recipe.

Twenty additional executed cases: shared caps/food protection, config opt-outs,
paid startup/delivery/processing/replay, qualifications, actual paid bakery food
customer, marker/world absence, complete late-write rollback and read-only customer/
ingredient/supplier/restart safeguards. Focused128 then135 passed; final full
.\mvnw.cmd clean verify passed1,482 (1,480 unit/two packaged integration), zero
failures/errors/skips;8:20 at16:56:17 ET. New processing class15 cases passed in
the full run. No Paper adapters changed, native check not rerun for this SQL/config
batch; future live bakery/craftsman adapters still require native verification.

Schema remains77; no migration, old catalogue rewrite or server DB/world changes.
See PROCESSING_WORKSHOPS.md. Updated status/progress/devlog; feature branch
codex/phase3-processing-startup. P3 remains29/39 resources,1/4 complete groups/~67%
partial. Actual funded live founding, sustained ingredient/product/customer chains,
craftsman demand and live livestock reconciliation remain. Next: qualified bakery
preparation/acceptance, editable production defaults and live integration. No new
product decision blocks those scoped steps. Main remains unchanged.

## October13 — approved bakery/craftsman recognition and candidate safeguards

User approved configurable existing smokers for bakery candidates and crafting
tables for the general craftsman. Implemented bounded block-type-only snapshot
recognition, stable industry mapping and loaded fixed-anchor presence checks.
New-file defaults recognize all four marker kinds; installed lists stay unchanged
and missing legacy sections opt out. No container contents, block ownership,
business, goods or money are taken/created by recognition. Existing first smith/
warehouse reviews now filter their own marker kind before the candidate cap, so
additional tables cannot crowd out established industry bootstrap.

Schema77 expands sighting/anchor constraints transactionally, retaining every
parent/child row and fixed identity. Explicit indexes/triggers survive; copied
history does not fire triggers. Late failures roll back rows/constraints/version.
Unknown inbound foreign keys are refused before dropping tables can cascade into
extension data. No automatic reinterpretation of existing markers or funding.

Eleven new tests; focused47 and targeted13 passed. Full .\mvnw.cmd clean verify
passed1,462 (1,460 unit/two packaged integration), zero failures/errors/skips;
8:10 at16:07:56 ET. Native Paper26.2 build129 proof passed16:09:31 ET: four real
block types, untouched own barrel/smoker inventory, newer empty frame, retained
history, absence/restart, zero firms and exact four-block restoration/ticket release.
Schema76-to77 audit retained all133 predecessor tables,128 exact. Five differences:
three normal clock/runtime/ore records and two farm observation timestamps, with
all crop counts, completeness, location and identity fields exact; no ages changed.
Initial audit refused timestamps; strict field-level validation accepted them.
Fixture stopped, settings restored and helper archived. Existing Windows OSHI
counter warning remains; no Civilizations/probe/listener errors. Human server untouched.

Actual bakery/craftsman founding, ingredient/product chains and replacement, plus
live livestock death/birth/adoption reconciliation, remain incomplete. The new
public startup-grant extension is awaiting user approval; markers are approved.
P3 stays29/39 resources,1/4 complete groups/about67% partial. Prior development
through ce98f79 is backed up on GitHub. This passing batch is on
codex/phase3-bakery-craftsman-markers for authorized feature backup; main unchanged.

## October13 — persistent herd and paid product backend

Schema76 adds four initially empty tables for dedicated herd identity, unique
initial animal UUIDs, immutable period receipts and young-cohort checkpoints.
`LivestockRepository` requires saved village-owned animals in the business's
world/town and unit-sized real feed/product storage. Exact opening retries retain
current state; another herd cannot reuse those initial animals. Existing crop-farm
catalogues/labor are unchanged; combined farm/herd transitions are not inferred.

Production spends actual business feed, counts actual eligible local staff,
respects business availability and incoming storage reservations, and credits
cow milk/chicken eggs with cohort changes in one transaction. Receipts retain before/after stock,
actual work/storage budgets and an explanation. Periods cannot be
skipped or replayed under changed terms. Historical replay works after later
periods/restarts. Disabled/absent worlds preserve exact state. Full output storage
conservatively idles work rather than borrowing anticipated feed space. No cash,
meat, mortality or physical entities are created. Slaughter is deliberately not
exposed until safe physical retirement is connected.

Twelve new temporary-database cases cover restart/replay, actual stock/cash
conservation through paid grain/milk deliveries, ownership/reuse/catalogue guards,
eligible staffing and staffed-for-sale operation, shortages/full/reserved storage,
late-write rollback/retry, a240-period scenario and schema75 migration failure/
recovery. Focused40 passed. Full .\mvnw.cmd clean verify passed1,451 (1,449 unit/two packaged integration),
zero failures/errors/skips;8:06 at15:16:14 ET. Guarded Paper26.2 build129 packaged
SQL backend proof passed15:17:04 ET: actual feed/cohort/products, restart replay
and absent-world pause. Schema75-to76 audit retained every predecessor table;
126/129 exact, only normal clock/runtime/ore bookkeeping changed; no aging.
New main-fixture herd tables empty. Fixture stopped, properties restored and helper
archived. No world/entity mutations by the helper; no client/physical birth proof.
Existing Windows OSHI performance-counter warning appeared; no Civilizations,
probe or listener error. No human server changes.

This is a backend, not automatic livestock gameplay. Logical births still need
physical identity/projection reconciliation; player-death accounting, adoption,
pen sign UI and live scheduling remain unfinished. P3 checklist stays29/39
resources,1/4 complete groups/~67% partial. Bakery/craftsman markers remain pending.
Human server untouched. No merge/push.

## October13 — approved herd authority, cohort calculator and village-only breeding guard

User approved simulation-managed breeding/slaughter for village-owned livestock.
Implemented deterministic active-period young cohorts, paid feed/labor birth costs,
cooldowns, gradual maturity, head/capacity limits and protected adult slaughter.
Capacity loss preserves existing animals; absent worlds/disabled mode preserve the
exact state. No food-linked death, spoilage, invented feed or consumer is introduced.
This calculation is not a persistent herd ledger or connected production yet.

Added optional strict livestock YAML with editable cow/chicken care/output/birth
terms. New-file defaults enable control for positively saved village-owned cows/
chickens; absent old sections disable without rewriting. SQL owner loads immutable
village animal IDs at startup, excluding player ownership. Main-thread event guard
blocks outside vanilla births for either village parent and intercepts breeding
food in either hand before vanilla item consumption; buckets/names/leashes remain
available. No SQL/world scan per event. Adoption/transfer publication still needs
its future commit/physical-claim handshake; no live adoption API exists yet.

Fourteen added cases: six herd calculations including2,400 active periods/replay/
head/feed/work/storage conservation, six event/interaction guards, one strict YAML
case and one immutable village-only SQL registry case. Focused21 passed. Full
Wrapper .\mvnw.cmd clean verify passed1,439 (1,437 unit/two packaged integration),
zero failures/errors/skips,7:37 at13:12:55 ET. Guarded Paper26.2 build129 proof
passed13:44:11 ET: actual SQL-owner cached identities, actual food tags and synthetic
birth/interaction dispatch with real temporary cows/chickens and a fake player.
Verified village birth/feeding blocks, player/other-item availability and unchanged
feeding stacks. First fixture had missing fake-player PDC and listener errors;
audit refused its own pass marker. Fixed only the test helper and reran cleanly.
Removed temporary actors, stopped fixture, restored properties and archived helpers.
Retry audit verified one normal month: two living residents aged one, dead/history/
money/assets retained;123 other predecessor tables exact. No client/packet or
actual vanilla breeding-cycle proof is claimed. No broad profiling claim.
Schema remains75; no migration or human server edits. Existing saved village
identities are the only control evidence; cold startup/failed readiness leaves
control inactive until the identity view is ready. Livestock ledger/startup/death
reconciliation, safe adoption, signs/transfer UI and actual products remain next.
P3 remains29/39 resources,1/4 whole groups/~67% partial; no herd gate claimed.
Next requested decision: editable smoker/crafting-table recognition for bakeries
and craftsman shops, or explicit designated workplaces. This extends the specific
smithing-table/barrel approval and is not silently selected. See
LIVESTOCK_PROTECTION.md. No merge or push.


## October13 — saved protective pens and conservative flat-pen evidence

Implemented confirmed protective pen storage and atomic initial animal ownership,
plus immutable Paper snapshot classification for simple flat pens. A pen save
records the finite footprint, sign coordinate, owner and initial roster; overlaps,
changed replay inputs or another owner's animal cancel the whole transaction.
Existing same-owner animal provenance is retained. Disconnected/forged geometry
and distant signs are refused. Saved protection does not depend on today's sign.

Schema75 adds initially empty protected_pens, protected_pen_columns and
protected_pen_animals. Old-world/owner/history rows stay intact. Eight new
SQLite cases passed within focused91 older-save checks; six Paper-source cases
passed within focused29 geometry/preview/store checks. Full Windows Wrapper
.\mvnw.cmd clean verify passed1,425 (1,423 unit/two packaged integration), zero
failures/errors/skips,7:54 at12:25:29 ET. Guarded Paper26.2 build129 check passed
12:29:13 ET: closed/open gate exact footprint, missing fence and unknown snapshot.
Initial native check stopped before edits because empty server had no loaded chunks;
fixed the test harness to explicitly prepare one chunk (production source never
loads chunks). All temporary sky-pen blocks restored; no animals edited. Both
migration73→75 and retry schema75 save audits preserved every predecessor row
except ordinary civilization_time/economy_runtime/ore_tracking updates. New ownership/
pen tables remain empty in primary fixture, integrity/FK healthy. Original properties
restored, backups retained, helpers archived, fixture stopped. Human server untouched.
This is verified backend/classifier work, not complete live livestock/sign adoption.

The Paper source is an unwired read-only adapter over caller-captured immutable
loaded chunk snapshots. It certifies common full-cube flat footing/two air blocks,
connected fences/gates or two-block full-cube edges. Open gates count structurally;
isolated/disconnected fences, missing chunks, ramps/slabs/doors/walls and unsupported
materials stay unknown. It cannot certify ownership or land-claim permissions.
No live listener/menu/adoption, donation transfers, breeding or player server edits.
Adjacent pens sharing boundary columns conservatively conflict for now.

Pending user rule: simulation-managed breeding/slaughter for village-owned animals
versus retaining vanilla breeding and observing it. This affects live herd control;
player animals stay ordinary and cannot be taken. Pen verification proceeds while
pending. P3 remains29/39 resources,1/4 whole groups/~67% partial; livestock entries
stay incomplete. See LIVESTOCK_PROTECTION.md. Feature branch unchanged; no push/merge.


## October13 — persistent initial livestock ownership (schema74)

Continued after the verified protection-domain commit1873626. Added immutable
first ownership provenance keyed by animal UUID: origin world, player or village
owner, time and source. Exact retries replay; conflicting owner/provenance is
refused. No observation, escape, sign break or restart is an ownership transfer.
Known world and actual same-world town are required for village registration.
Public writes own a transaction; internal rows compose with future pen transactions.
Late failures roll back; no silent overwrite, fabricated animal or cash is created.

Schema74 adds an initially empty animal_ownership table/index. It does not adopt
existing animals or change predecessors. Seven temporary-DB tests cover restart,
retry/conflict refusal, world/town constraints, late failure/recovery, composed
rollback, migration73 failure/retry/preservation and invalid owners. Focused94
passed, including affected older-save fixtures. Full Windows Wrapper
.\mvnw.cmd clean verify passed1,411 cases (1,409 unit/two packaged integration),
zero failures/errors/skips,8:05 at11:37:25 ET. No playable server or non-test
database changed. Diff reviewed; migration exercised only in temporary test DBs.

Live sign claims/particles/menu, persistent pen footprints, authority integration,
explicit audited ownership transfers, breeding cohorts, observation and live
startup/production remain next. First records are trusted backend inputs, not a
player-accessible claim shortcut. P3 remains29/39 resources,1/4 whole groups/~67%
partial; incomplete livestock entries stay unchecked. No decision is pending.
See LIVESTOCK_PROTECTION.md. No push/merge; feature branch unchanged.


## October13 — livestock protection and bounded work foundation

Approved automatic free-range adoption and authorized per-pen sign preview/
confirmation direction, resolving the earlier livestock ownership question.
Implemented plain-Java bounded incremental enclosure detection, actor/world/level/
expiry-bound previews, fresh evidence/membership/authority checks, saved-ownership
priority eligibility and feed/labor/storage/reserve-bounded livestock work.
Twenty-six new focused tests passed, including13,122 small work combinations.
Full Windows Wrapper .\mvnw.cmd clean verify passed1,404 cases (1,402 unit
and two packaged integration), zero failures/errors/skips,9:13 at11:24:55 ET.

This is backend preparation, not live adoption. No Paper sign listener/menu/
particles, persistent pen/animal records, breeding cohorts, actual herd startup,
config section or claims-plugin integration yet. Schema remains73; no world,
animal or human server changes. P3 checklist remains29/39 resources,1/4 whole
groups/~67% partial: neither livestock entry is complete. Next: saved ownership
and safe loaded-world recognition before enabling player interactions/adoption.
No consequential product decision is currently pending.
See LIVESTOCK_PROTECTION.md. Branch codex/phase3-livestock-protection; no push/merge.


## October 13, 2026 — hybrid farm contributions and actual paid surplus

Implemented the approved hybrid farming model on codex/phase3-farm-market.
Future actual basic food contributes at most an editable share toward real
meals/reserves; remaining accepted food/grain is business-owned. Old public
food, grain, identities and period history stay public/intact. Collective food
orders now pay the farm through existing real budget/stock/cargo checks; no
salary wallets, free money, duplicate food or confiscation. Grain buffer prevents
unsold ingredients crowding all basic food out. Storage backpressure idles work
rather than bypassing the contribution cap. Default50% share, two reserve periods,
256 commercial spaces plus one protected toolkit slot,16-unit grain target.
Installed files opt out without rewriting; configured grain rates/yields remain.

Transactional schema73 adds initially empty farm_market_stores/harvests. Empty
or recognized one-kit/zero-cash legacy farm stores adopt explicitly during a
successful food transaction, preserving tools/cargo and prior opening terms.
Unknown private stores are refused. No capital is generated; store opening
capacity stays saved. Future allocation/rates/targets tune with existing audited
settings transitions and backlog protection. Harvest proof, owned stock, public
food and checkpoint commit together; failed periods retry without repeating the
already successful business startup. Tools/skills use actual accepted work.

Fixed a confirmed grain regression: public tool funding compared food production
with total harvest learning, incorrectly blocking productive grain farms. It now
checks the actual shared harvest. Public kit invoices also preserve the farm's
real sales cash, and one-kit quota includes pending cargo despite larger storage.

Eighteen added cases: five pure allocation cases,11 temporary-DB cases, one YAML
case and one grain-tool eligibility regression. Includes200-period paid-food
conservation, actual capped output under overpopulation, cash poverty/no free
orders, migration failure/retry, late write/adoption rollback, legacy tool/public
stock/history preservation, absent-world/restart, unknown store refusal, paid kit
income preservation and an actual funded grain-delivery/bread/public-food order
backend chain. Initial test compile found one compound var declaration; corrected
test syntax without changing production guards. Focused200 passed before two
additional chain/refusal cases; final targeted36 passed. Full Windows Wrapper
.\mvnw.cmd clean verify passed1,378 (1,376 unit/two packaged integration), zero
failures/errors/skips,7:14 at09:46:44 ET. No broad multi-town performance claim.

Guarded Paper ordinary-owner timer/customer proof passed09:47:38 ET: six settled
food periods, capped contributions, real private food/grain and two paid200 food
orders, one pending owned cargo, actual farm skills and resumed work after
absence/restart. Conserved17500 cash after2500 startup from explicitly configured
20000 fixture opening funds. Trusted crop/site inputs are not natural discovery;
funded fixture does not prove every tiny starting village can afford the loop.
No blocks, entities or chests edited; no chunk tickets. Main save audit120/123
predecessor tables exact; three ordinary clock/economy/ore changes, all identity,
assets/money/history retained. Schema73 integrity/FK healthy; new main market
rows empty under old-file opt-out. Original properties restored, helper archived,
consistent backups retained; playable human server unchanged. No merge or push.

Recorded expanded P3 scope: livestock, real bakery ingredients and one general
craftsman, with no spoilage or separate craft professions. Six new resource entries
expand the old33-entry group to39. This batch verifies hybrid allocation:29/39;
whole groups still1/4 and equal-group partial checklist now~67% under NEW scope,
not the previous70% denominator. Automatic livestock/bakeries/craftsman, further
resource founding and specialization/long-run acceptance remain unfinished.
Paid bakery chain is backend fixture proof, not automatic shop founding.

Next livestock startup requires the user's ownership decision: existing nearby
chickens/cows (including player pens) automatic adoption versus explicit opt-in.
Recommend opt-in; no answer assumed and no animals changed. Village funds remain
finite; failed affordability creates a real shortage, not invented income.
See HYBRID_FARMS.md and DECISIONS_AND_OPEN_QUESTIONS.md.

Current work: hybrid capped farm contributions, owned future surplus and real paid
food customers passed full/native verification on codex/phase3-farm-market.
The earlier public-grain sale question is superseded by the approved hybrid farm
direction. Existing public food/grain remains untouched. Existing-livestock
ownership is now the pending user decision; no animals have been adopted/changed.
P3 has six additional resource/food-chain checklist entries, so the old ~70%
describes older scope. Current recount is recorded in PROGRESS/PHASE3_TODO.

## October 13, 2026 — protected actual future grain harvests

Staffed farms now allocate an editable part of actual future wheat work to separate
public grain. The same labor/output budget, complete typed wheat fraction, actual
meals plus future food reserve and finite grain storage limit it. Unknown/partial
fields, no wheat, no staff, food pressure, full storage and absent worlds cannot
mint ingredients. Default25% wheat share, two future food periods,256 grain space
and one grain unit per diverted work unit; installed files without the section
opt out and are never rewritten. Food/history are never converted or reseeded.

Transactional schema72 adds initially empty grain_stocks/grain_harvests. Food,
grain allocation/history/stock and checkpoint commit together; late faults roll
back everything. Historical yields remain saved; lower capacity preserves stock.
Existing forward tuning refuses unfinished old-policy backlog. Tool service and
farming learning use the same total actual harvest, including grain-only work;
there is no extra labor, free tool work or duplicate experience. Current-period
admin food diagnostics explain grain allocation/storage and basic food output.

Fourteen added cases: four pure calculations, nine temporary-DB runtime cases and
one strict YAML case. Covers food/reserve protection, wheat fractions/yields/space,
unknown/partial/unstaffed/disabled modes, replay/absence/restart, late failure,
settings/backlog transitions, shrinking wheat fields, grain-only tools/learning,
and migration71 failure/retry preserving old food. Focused226 passed before the
final grain-only case and work-log changes; final targeted37 passed. Full Windows
Wrapper .\mvnw.cmd clean verify passed1,360 (1,358 unit/two packaged integration),
zero failures/errors/skips,7:24 at08:40:41 ET. Helper precompile initially pointed
at the not-yet-packaged JAR; corrected the precheck classpath and recompiled
against the verified packaged JAR for the actual native run. No production fix
or weakened assertion was needed.

Guarded native ordinary-owner timer proof passed08:42:06 ET: actual funded farm
with two assigned workers; each8 future work split6 food/2 grain,2500 actual
startup debit from4800 opening funds, real farming skills, absence/restart pause,
then one resumed harvest. These are trusted explicit crop/site fixture inputs,
not a natural positive-field discovery claim. No blocks/entities edited or chunk
tickets used. Save audit115/121 predecessor tables exact; six ordinary calendar,
economy, tracking, active-world and resident-age changes. Existing identities,
assets, cash and history retained; schema72 integrity/FK healthy. Exact original
properties restored, helper archived and backups retained. Playable server unchanged.

Next bakery supply integration needs the user's answer on recurring paid public
grain sales. Prior approval covers one-time starter material sales; no ongoing
market is assumed. Configurable real cash payments to village treasury with
training reserves are recommended. This question is recorded/pending. Automatic
quarry/pit founding, forestry, wider recipes/competition and specialization gates
also remain. Phase3 is still1/4 whole groups/~70% partial. No merge/push.
See GRAIN_HARVESTS.md and DECISIONS_AND_OPEN_QUESTIONS.md.

Future staffed grain harvest allocation passed full/native verification. Recurring paid
public grain sales for bakeries/warehouses are a pending user decision; no sale
rule is implemented or assumed approved. See DECISIONS_AND_OPEN_QUESTIONS.

## October 13, 2026 — saved wheat-field evidence without food conversion

The bounded farm scanner now identifies wheat-on-farmland separately from other
food crops, retaining the same footprint/overlap/partial rules and read budget.
All growth stages remain planted capacity, not an immediate physical harvest.
Transactional schema71 adds farm_grain_evidence, initially empty for old worlds.
Legacy composition remains unknown; typed zero is distinct. Older/equal frames
cannot replace current counts, newer untyped evidence clears composition, and
partial evidence stays partial. Food output, stocks, purchases and history are
unchanged. This is bakery preparation, not harvested ingredients or bakery startup.

Seven added cases cover typed/legacy bounded scans, real classifier distinctions,
restart and preserved food/history, stale/zero/partial/unknown frames, late write
rollback/retry, version70 upgrades without invented grain, failed migration/retry,
and invalid subsets/corrupt frame pairing. Older-schema preservation tests now
explicitly expect the additional empty table. Focused116 passed; full Windows
Wrapper .\mvnw.cmd clean verify passed1,346 cases (1,344 unit/two packaged
integration), no failures/errors/skips,7:38 at08:19:23 ET. Guarded Paper probe
passed08:19:58 ET: ordinary observer matched actual empty terrain (zero crops/
wheat, not a positive field proof), persisted known zero over restart and retained
all24 food units. Positive wheat/material cases are covered by automated tests.
No blocks/entities changed; helper-only tickets released. Save audit116/120 old
tables exact with four ordinary clock/evidence changes; schema71 integrity/FK
healthy, existing identity/assets/money/history retained. Properties restored,
helper archived and backups preserved. Playable server unchanged; no merge/push.

Approved grain direction is recorded in canonical design/rules; stale shoreline
recovery implementation wording is corrected without changing its approved rule.
Next: actual staffed future grain harvest accounting, paid bakery ingredients,
automatic quarry/pit founding, forestry and specialization acceptance. Phase3
remains1/4 whole groups/~70% partial; no gameplay question pending.
See GRAIN_EVIDENCE.md. This batch does not finish a broader industry milestone.

## October 13, 2026 — bounded loaded-neighbor mineral observations

The ore observer now uses the centre and at most four already-loaded cardinal
chunk snapshots. Missing faces remain unknown; no chunks are forced or neighbor
resources included as centre members. Separate quarry classification can now
confirm exposed boundary stone/sand without guessing about missing faces.

Six new tests cover exposure, classifier separation, missing faces, identity,
negative coordinates and overflow. Full Windows Wrapper clean verify passed
1,339 cases (1,337 unit/two packaged integration), zero failures/errors/skips,
7:16 at04:22:30 ET. The marked disposable Paper probe passed08:04:41 ET:
five actual snapshots matched32 independently checked stone/sand positions,
and the ordinary observer/SQL owner saved current ore evidence. No blocks or
entities edited; helper-only tickets released. Save audit116/120 tables exact,
four normal clock/evidence changes; identity/assets/cash/history preserved,
SQLite integrity/FK healthy. Exact properties restored and helper archived.

No migration: schema70. At most five snapshot captures cost more than one;
broad multi-town profiling remains open. Automatic quarry surveys/founding,
forestry, wider recipes and specialization remain unfinished. Grain accounting
is approved but not implemented: preserve old food, track future ingredients
separately and retain basic meals without a bakery. Phase3 remains1/4 whole
groups/~70% partial; no gameplay question pending. No merge/push or playable
server changes. See MINERAL_NEIGHBORHOOD.md.

Bakery grain accounting is approved: distinct future ingredients, existing food
preserved and basic meals remain feasible. See DECISIONS_AND_OPEN_QUESTIONS.
Wheat-field evidence and grain harvests are implemented; bakery integration remains unfinished. Recurring public grain sales are pending approval.

## October 13, 2026 — live finite quarry and sand-pit operation

Existing funded/staffed quarry and sandpit firms now enroll in the normal SQL-owner
industry timer. Resource-specific editable rates apply: stone six batches/worker,
three units/batch; sand twelve/four. Finite saved source shares/yields, storage,
actual workers, tools and history-trust guards remain authoritative. New productive
work earns bounded mining experience. Missing old settings opt out; disabling saved
clocks records paused periods without backfilling production on reenable. Work-speed
retuning does not rewrite abundance/yields or refill deposits. No migration: schema70.

Fourteen new temporary-database cases (both kinds) cover work/replay/absence/restart,
disabling/old-file opt-out, capture gaps, per-block loss with other shares working,
late checkpoint rollback and speed-retuning invariants. Test setup initially omitted
tracking initialization before a simulated gap; corrected the fixture, retaining the
guard. Focused120 passed before the two final opt-out cases. Full Windows Wrapper
.\mvnw.cmd clean verify passed1,333 (1,331 unit/two packaged integration), zero
failures/errors/skips,7:22 at03:37:47 ET. Guarded native ordinary SQL-owner timer
probe passed03:39:45 ET: two explicit funded/qualified firms produced18 stone/48
sand, spent exactly18 finite batches, gained actual skills, conserved startup money
and retained state over absence/restart. Sites/actors/resources are trusted explicit
fixtures, not automatic terrain discovery. No blocks/entities edited or chunks forced.
Save audit117/120 tables exact; three ordinary clock/economy-runtime/ore changes,
identity/assets/cash/history retained, integrity/FK healthy. Exact properties restored,
helper archived and consistent backups retained. Your playable server is unchanged.

Next: actual family-specific quarry/pit surveys and real customer/site/founder selection,
renewable forestry, wider recipes and competition/specialization acceptance. Existing
vacancy services remain configurable; this batch does not claim automatic new firms.
No gameplay question pending. Phase3 remains1/4 whole groups/~70% partial. No merge/push.
See TIMED_QUARRIES.md and FINITE_QUARRIES.md.

## October 13, 2026 — automatic first fishing-owner training and paid food chain

Connected normal water/bank observation to one real future fishing-owner course when
no qualified owner exists. Permanent world/habitat identity, current protected funding,
real funded food need, safe-bank proof, actual adult availability, practice supplies and
study budgets apply. Enrollment gives no skill, job or money. Real future timber practice
qualifies the owner before the unchanged paid startup path opens the fishery. Unknown,
stale, unfunded or disabled opportunities pause courses; redundant sponsorship is refused.
Migration 70 adds one initially empty course association table, retaining all old rows.

A scoped read-only training preview may validate exactly one currently processing food
period. Actual purchases and startup still require a complete current assessment without
backlog. This distinction is necessary to prevent linked practice from pausing forever
mid-period; it cannot authorize old purchases or manufacture demand/funds. Tests verify
this boundary, original safe footing, enrollment/practice rollback, stale proof, backlog,
restart/absence and policy disabling. Eleven new temporary-database cases. Focused 69
passed before the two final guard cases; full Windows .\mvnw.cmd clean verify passed
1,319 (1,317 unit/two packaged integration), zero failures/errors/skips, 6:50 at 02:36:45 ET.

Final guarded native first-owner chain passed 03:05:00 ET: zero initial fishing skill,
ten real timber practice steps, qualification, ordinary protected startup, catches and
further earned experience, an actual paid public food order delivered once into reserve,
conserved money, saved bank relocation under read-only damage masking, absence pause
and restart. Native setup/clock/observation assertions were corrected after they exposed
seed mismatch, an overpaced clock, already-earned work and split payment snapshots;
production seed, purchase, backlog and money guards were not weakened. Consistent
read snapshots and fully observed/settled baselines distinguish real behavior from test
races. Native actors/farm/practice stocks/treasury are explicit isolated fixtures; actual
water/banks are read from Paper. This is not a natural survival-balance or scale gate.
Save audit 113/120 tables exact; seven ordinary clock/freshness/ore/age changes only,
identities/assets/cash/history retained, integrity/FK healthy. Exact settings restored,
helpers archived and consistent backups retained; no helper block/entity edits.

The verified mineral/tool and food/practice customer/recipe milestone is now checked;
broader catalogs remain separately open. Phase 3: 1/4 whole groups/~70% partial
(4/4 business foundation, 28/33 resources, 14/15 lifecycle, 0/5 specialization/acceptance).
Next: renewable forestry and remaining stone/sand/bakery connections, competition and
multi-year geographic scenarios, broader native/profile/client acceptance. No gameplay
answer pending. Your playable server is unchanged; no merge/push.
See FISHERY_FOUNDER_TRAINING.md, FISHERY_WORKPLACES.md and FISHING_SKILLS.md.

## October 13, 2026 — earned fishing skills and real-vacancy study

Connected fishing to configurable productive-work experience, supply-backed fishing
courses, automatic existing-vacancy study and ordinary qualified staffing defaults.
Actual catches earn bounded experience; zero catches/full storage/unsafe banks,
absence, disabled/unmapped policies and historical replay do not. Catch, habitat,
skill, audit and checkpoint roll back together. Existing vacancies can sponsor real
future timber practice with protected food/material reserves, then hire qualified
learners. Enrollment creates neither skill nor employment. No ownership waiver.
Existing installed maps/lists and accepted course terms remain unchanged; admins opt
in explicitly. No migration added: schema 69 retained.

Six added temporary-database cases cover work/replay/restart/absence, damage recovery,
late learning rollback, disabled/unmapped work, full storage and actual practice-to-
hiring. Focused 120 passed before adding the storage case; test setup initially had
an import/catalog mismatch, corrected only in the fixture, preserving application
inventory guards. Full .\mvnw.cmd clean verify passed 1,308 cases (1,306 unit/two
packaged integration), zero failures/errors/skips, 7:24 at 02:11:40 ET.
Guarded native ordinary startup/catch/earned experience/recovery/accounting/absence/
restart probe passed 02:12:59 ET. Save audit 112/119 tables exact; seven ordinary
clock/freshness/ore/age changes, identity/assets/cash/history retained, integrity/FK
healthy; exact properties restored and helper archived. No helper block/entity edits.

Automatic training of the FIRST fishery founder is still unfinished: existing-vacancy
study cannot bootstrap a nonexistent business. Next is a durable real habitat/bank
opportunity association for that course, then remaining basic industries and final
specialization/scenario/profile gates. No gameplay decision pending. Your playable
server is unchanged; no merge/push. Phase 3 remains 1/4 whole groups/~69% partial.
See FISHING_SKILLS.md and FISHERY_WORKPLACES.md.

## October 13, 2026 — automatic qualified shoreline fisheries and saved recovery

Connected the ordinary main-thread water observer to qualified first-fishery startup,
exact live support/depth/open-sky and dry-bank checks, final standing-point recheck,
and atomic real protected startup funding, first job, empty storage and habitat.
Migration 69 adds two initially empty shoreline binding/history tables; legacy
resources remain intact and unbound. Saved recognition bounds survive retuning.
Loaded original habitat/bank checks relocate to a suitable nearby bank, pause catches
on confirmed unrecoverable damage, and retain the last confirmed state on unknown
blocks. Capacity loss/restoration and bank movement commit together, with no instant
fish refill. Stable frames do not add unlimited timer-history rows. No blocks,
containers or player buildings are changed; physical repair/docks remain Phase 4.

Full Windows .\mvnw.cmd clean verify passed 1,302 cases (1,300 unit/two packaged
integration), zero failures/errors/skips, 7:36 at 01:53:19 ET. Corrected an accelerated
native helper race by waiting for the real industry clock registration before advancing
fixture time; no production behavior was changed to accommodate the helper.
Final guarded Paper probe passed 01:58:12 ET: ordinary automatic startup, catches,
real money conservation, persisted same-habitat relocation under a read-only damage
mask, absent-world pause and restart. Fixture actors, skill and treasury are explicit
setup; automatic first-owner skill acquisition is not claimed. No helper blocks or
entities were edited. Save audit 110/119 existing tables exact; nine reviewed ordinary
clock/freshness/ore/population changes included a real simulated external arrival.
Existing identity, assets, cash and history retained; integrity/FK healthy, exact
settings restored, helpers archived and consistent backups retained. Your playable
server was untouched; no merge/push. See FISHERY_WORKPLACES.md.

Phase 3 remains 1/4 whole groups/~69% partial: the broader resource catalog and final
specialization gate remain incomplete. Next: fishing skill/course and vacancy wiring,
automatic first-owner training, other basic industries, competitive multi-year
scenarios and broader native/profile acceptance. No gameplay answer is pending.

## October13, 2026 — approved adaptive shoreline recovery planner

User chose nearby replacement or repair rather than permanent fixed-spot pausing.
Recorded in canonical design/simulation/decisions. ShorelineScan now exposes immutable
per-position safe/blocked/unknown evidence; ShorelineRecovery plans KEEP, nearest
bounded RELOCATE, WAIT_UNKNOWN or NEEDS_REPAIR around the same original habitat.
No closure, new resource claim, fish refill, cash/job changes or block edits occur.
Confirmed damage is distinct from missing evidence; unknown original geometry must
preserve last verified operational state and normal unloaded-chunk data simulation.
Caller-supplied rise/distance, strict read budgets, deterministic ties and overflow
protection apply. Repairs/docks still need Phase4 funding/labor/materials/protection.

Seven added headless cases; focused12 passed, zero failures/errors/skips,12.766s
at00:00:11 ET. An initial full run was intentionally interrupted when review found
that fresh-safe evidence needed explicit separation from operational permission;
validation restarted after clarifying confirmed damage versus unknown flags.
Full Windows .\mvnw.cmd clean verify passed1,287 (1,285 unit/two packaged
integration), zero failures/errors/skips,7:02 at00:07:16 ET.
Guarded native scheduled water/SQL-owner/read-only shoreline probe passed at
00:08:10 ET:220 independently checked water columns,11 actual safe banks, loss-mask
selection of an independently checked safe replacement and unknown-mask waiting.
Strict128 cell checks/tick, newer capture, absent-world pause and restart passed.
Damage was represented by read-only input overrides; no helper blocks/entities were
changed and no actual relocation was persisted. Normal shutdown00:08:10. Save audit
113/117 tables exact; four ordinary clock/economy/farm-freshness/ore changes only,
all identities/assets/cash/history retained. Schema68 integrity/FK healthy; exact
settings restored, helper archived, helper-only chunk ticket released and consistent
backups retained.
No migration (schema68), persisted workplace binding, automatic founding/recovery
scheduler or physical repair is enabled. Next: save/validate opening shoreline and
habitat recognition terms, final acceptance and qualified training/startup scheduling.
The shoreline decision is answered; no new question pending. Human playable server
unchanged; no merge/push. Phase3 remains1/4 whole groups/~69% partial.
See SHORELINE_RECOVERY.md for the exact implemented/planned boundary.

## October12, 2026 — qualified read-only fishery owner preparation

FisheryInvestment chooses an actual qualified living local unemployed adult from
a bounded UUID-sorted page using stable seeded sampling. Server adulthood, saved
fishing experience and both owner/first-worker requirements apply; active learners
and existing workers cannot be reassigned. Unlimited ownership is preserved.
Proposal contains current funded water evidence and qualification terms but no
invented shoreline anchor, new job, resource, money, skill or reservation.

Optional strict fishery-founding prototype and SQL-owner/plugin configuration wired;
missing old sections opt out. This is preparation only, not automatic founding or
fishing training. Current study defaults still lack a fishing course. Seven added
temporary-database cases plus one config case; focused95 passed, zero failures/
errors/skips,12.799s at21:54:16 ET. An initial study test had a fixture catalog mismatch;
corrected its explicit transactional enrollment without weakening production guards.
Full Windows .\mvnw.cmd clean verify passed1,280 (1,278 unit/two packaged
integration), zero failures/errors/skips,7:28 at22:03:19 ET.
Guarded native SQL-owner owner-preparation/first-firm identity safeguards passed
at22:04:33 ET, alongside ordinary reserve/practice purchases, real money/goods
conservation, delivery, absence pause and disabled-order restart. This proves safe
API integration/identity rejection, not native positive founding or training.
Normal shutdown22:04:33; save audit111/117 tables exact, six ordinary clock,
demographic/resident-age, active-world, economy-runtime and ore-tracking changes;
all identity/assets/cash/history retained. Schema68 integrity/FK healthy, exact
settings restored, helper archived and consistent backups preserved.
No migration (schema68), automatic scheduler or dock work. Removed-shoreline
standing-point rule still awaits the user; no elapsed time is approval. Next after
that choice: current anchor/habitat acceptance, saved recognition terms, appropriate
supply-backed fishing training and scheduling. Human playable server untouched;
no merge/push. Phase3 remains1/4 whole groups/~69% partial, broader catalog/
competition/specialization and acceptance still open. See FISHERY_OWNER_PREPARATION.md.

## October12, 2026 — bounded funded first-fishery prospects

FisheryOpportunities connects current connected unclaimed water, real food customers
and food-protected startup feasibility in a read-only SQL-owner review. Proposed
startup/capital costs are deducted in a demand preview before checking customers,
preventing counting the same public money twice. Actual eligible staffed local
food stocks reduce remaining need. Over-budget supplier sets defer conservatively;
all chosen water supports fit the saved town range. Saved fishery identities cannot
be bypassed by first-firm bootstrap. No assets, jobs, grants or resources are created.

Optional strict fishery-opportunities config and normal plugin/PopulationSession
wiring added; missing old sections opt out. Numeric editable prototype terms in
FISHERY_OPPORTUNITIES.md. Seven temporary-database cases and one configuration case
added; focused122 passed, zero failures/errors/skips,23.432s at21:37:53 ET.
Full Windows .\mvnw.cmd clean verify passed1,272 (1,270 unit/two packaged
integration), zero failures/errors/skips,6:55 at21:46:00 ET.
Guarded native new SQL-owner first-firm identity/configuration safeguard passed
at21:46:54 ET, alongside ordinary emergency-food/practice purchases, money/goods
conservation, delivery, absence pause and disabled-order restart. This native check
does not claim a newly founded fishery or positive live economic candidate. Normal
shutdown21:46:54; main-save audit114/117 tables exact, only clock/economy runtime/
ore changes; identity/assets/cash/history retained. Schema68 integrity/FK healthy;
exact settings restored, helper archived and consistent backups preserved.
No migration (schema68), automatic founding/training or physical dock work enabled.
Next: qualified owner/training preparation, fresh shoreline/water acceptance and
normal startup scheduling. The removed-shoreline standing-point rule is now awaiting
a user decision; preparation can continue independently. Human playable server
unchanged; no merge/push. Phase3 stays1/4 whole groups/~69% partial; broader renewable
catalog, competition/specialization and final acceptance remain unfinished.

## October12, 2026 — approved emergency food reserve customers

User approved a needs-based reserve. VillageSupplyPolicy and VillageSupplyPurchases
now target current unmet food plus actual assessed demand times an editable future
reserve, counting existing public food, paid public cargo, donations and reserved
player purchase/barter food. Bundled economy.yml uses two periods;0 or a missing
new food-stock-reserve-periods key preserves shortage-only behavior. This is separate
from food-reserve-periods cash protection. Strict0–100 validation, overflow-safe
targets, existing budgets/haul/minimum cash and actual seller eligibility remain.
No money, consumption, stock or customer is fabricated. Existing shortage-only
regressions explicitly retain that configuration; the fishery catch/arrival case
now verifies a legitimate second reserve purchase after hunger is satisfied.

Eight added automated cases include healthy funded customer plus protected startup,
real startup/catch/payment, donations/cargo, rollback/replay, budget exhaustion,
current population/world/cash guards, normal runtime buying before shortage and
strict editable/missing/invalid settings. Focused124 passed, zero failures/errors/
skips,22.725s at21:23:49 ET. Initial test runs exposed obsolete shortage-only order
expectations and a fixture configured after runtime use; corrected fixtures and
assertions, without relaxing production guards.
Full Windows .\mvnw.cmd clean verify passed1,264 (1,262 unit/two packaged
integration), zero failures/errors/skips,7:04 at21:31:29 ET.
Guarded native ordinary SQL-owner timer probe passed at21:32:09 ET: healthy
village bought emergency food, delivered/consumed actual goods, conserved real
cash, paused absent world and resumed with new orders disabled after restart.
Normal shutdown21:32:09; main-save audit114/117 tables exact, only ordinary clock,
economy runtime and ore-tracking changes; identities/assets/cash/history retained.
Schema68 integrity/FK healthy, exact settings restored, helper archived and consistent
backups preserved. No helper world blocks/entities/chunks changed.
No schema migration (schema68), automatic fishery scheduler or dock construction.
Human playable server unchanged; no merge or push. The reserve decision is answered;
next is automatic qualified shoreline investment/training and fresh acceptance.
Phase3 remains1/4 whole groups/~69% partial, broader industries/specialization open.

## October12, 2026 — atomic paid fishery startup

On codex/phase3-fishery-startup, BusinessStartupGrants.startFishery commits the
real food-protected village grant, qualified business/site, first worker, empty
food store and saved habitat together. Failed water admission rolls back the
whole investment. Restart/replay preserves spent fish, caught food and cash under
retuned/disabled policies; changing supports cannot reset an existing resource.
Six temporary-SQL tests passed, zero failures/errors/skips,10.024s at21:08:25 ET.
Full Windows .\mvnw.cmd clean verify passed1,256 (1,254 unit/two packaged
integration), zero failures/errors/skips,6:46 at21:17:37 ET.
No schema migration (schema68), live scheduler hookup, dock construction or change
to the human playable server. Existing generic producer worker IDs are retained.
Paper-facing adapters are unchanged from the prior native shoreline validation;
this database-only batch does not claim a new native automatic-startup check.

Automatic founding still needs current loaded-world/support/shoreline proof,
qualification/training and a real funded customer. A genuine rule conflict needs
a decision: existing village food customers buy only during shortages, while
startup grants are blocked during shortages. Proposed bounded emergency-food
reserve purchasing was sent to the user and recorded in the decisions document;
it is not implemented or approved. Phase3 remains1/4 whole groups/~69% partial.
No merge or push.

## October12, 2026 — approved shoreline starts and priority docks

User approved shoreline starts, quickly prioritized affordable docks in Phase4 and
substantially more efficient dock fishing under editable terms. Recorded in master
design, simulation rules and decisions without locking a multiplier/deadline or
claiming construction is implemented. ShorelineScan/PaperShorelineSource now provide
bounded read-only safe standing-point checks: dry solid opaque footing excluding
magma, two air cells, caller-selected bank rise, no chosen-water footing, no unknown
positions, overflow safeguards and strict main-thread/already-loaded access.
No chunk loads, block/inventory/entity changes, resource/rate changes or migration.
Five added headless cases cover strict budgets, footing/headroom, unknown and
obstructed banks, raised banks/cliffs, coordinate overflow and Paper thread/loaded
height guards. Focused11 passed, zero failures/errors/skips,12.903s at20:53:29 ET.
An initial headless material-registry check failed because Paper requires live
RegistryAccess; actual material safety is verified natively instead. Full Windows
.\mvnw.cmd clean verify passed1,250 (1,248 unit/two packaged integration), zero
failures/errors/skips,6:24 at21:00:56 ET. Guarded native ordinary water scanner/SQL
owner found220 suitable columns and11 independently checked safe shoreline positions,
567 shoreline reads under128/tick, at21:01:58; newer capture/absence/restart passed.
No helper blocks/entities or invented resources/businesses. Normal shutdown21:01:58.
Main-save audit113/117 old tables exact; four ordinary clock/economy/farm-freshness/
ore changes, all identity/assets/cash/history preserved; schema68 integrity/FK
healthy. Exact properties restored, helper archived, consistent backups retained;
human playable server untouched. No merge/push.

Automatic shoreline startup still requires fresh world/support/footing revalidation,
qualified residents/training, real demand and protected funding. This adapter is
verified but not yet scheduled by automatic founding. Physical docks/rate upgrade
persistence remain Phase4 work. The prior shoreline-versus-dock decision is answered;
no new design question pending. Phase3 stays1/4 whole groups/~69% partial.

## October12, 2026 — connected bounded fishing candidates

Branch codex/phase3-fishing-site-selection. WaterPatch selects deterministic,
cardinally connected same-level water footprints from current eligible evidence.
It rejects combining puddles, stale/future supports or duplicate coordinates;
bounded large-body selection avoids claiming an entire ocean. Nearest saved-town
center/coordinate ordering, oldest support timestamp and integer-edge safeguards
preserve explainability. SQL-owner selection applies configured fishing minimum
columns, enabled/depth/world/quality/conflict/claim gates; no startup or stock is
created. Six domain cases include100 seeded input-order shuffles, cardinal
connectivity, component limits, freshness and extrema; one SQL-owner integration
case checks partial/absence/configured-minimum safety. No new migration.
Focused37 tests passed, zero failures/errors/skips,20.179s at20:05:28 ET.
Full Windows .\mvnw.cmd clean verify passed1,245 (1,243 unit/two packaged
integration), zero failures/errors/skips,6:54 at20:12:51 ET. Guarded native ordinary
Paper scanner/SQL owner selected one bounded32-column patch from220 independently
confirmed suitable columns at20:13:35, with newer-capture/absence/restart checks;
no invented businesses/resources or helper block/entity changes. Normal shutdown
20:13:35. Main-save audit113/117 old tables exact; four ordinary clock/economy/
farm-freshness/ore changes, all identities/assets/cash/history preserved; schema68
integrity/FK healthy. Exact original settings restored, helper archived, consistent
backups retained. Human playable server untouched; no merge/push.

Automatic founding still needs a fresh actual workplace, skill/training, demand
and shared protected funding. The natural-shore versus existing-dock workplace
rule is undefined; recommendation/question recorded in DECISIONS_AND_OPEN_QUESTIONS
and sent to the user under AGENTS.md. Dependent startup remains paused for this
choice, not for failing tests. Phase3 remains1/4 whole groups/~69% partial; broader
renewable catalog, competition/specialization and final acceptance remain open.

## October12, 2026 — live current water evidence

Branch codex/phase3-live-water-evidence. Connected main-thread loaded-only
PaperWaterObserver, incremental full-height budgets, strict optional editable
water-observer settings, SQL-owner submission and debug diagnostics. Schema68
adds two initially empty current-evidence tables/index, preserving all old resource
stocks and identities. Complete captures replace only their chunk; unknown retains
old positives but invalidates eligibility. Exact replay, freshness/depth/world,
conflict/permanent-claim/budget exclusions and transactional rollback protect
candidate review. No blocks/inventories/chunk loading or money/resource grants.
Focused70 tests passed, zero failures/errors/skips,27.880s at19:54:03 ET.
Full Windows .\mvnw.cmd clean verify passed1,238 (1,236 unit/two packaged
integration), zero failures/errors/skips,6:58 at20:01:16 ET. Guarded native
PaperWaterObserver/normal SQL-owner check passed20:02:20:220 actual suitable columns
matched independent snapshot reference, newer capture/absence/restart preserved,
no invented businesses/resources or helper block/entity changes. Normal shutdown
20:02:21. Main-save audit111/115 old tables exact; four ordinary clock/economy/
farm-freshness/ore changes, all identities/assets/cash/history preserved; schema68
integrity/FK healthy, new water and fishery tables empty. Exact original settings
restored, helper archived, backups retained; human server untouched.

Automatic contiguous selection, fresh live revalidation, saved recognition terms,
qualified training/funding and fishery founding remain next. No new gameplay
answer pending. Phase3 remains1/4 whole milestone groups/~69% partial; this
infrastructure does not finish the broader renewable-industry checklist.
See WATER_OBSERVATION.md for defaults and limitations.

## October12, 2026 — bounded read-only water evidence

Added WaterScan/PaperWaterSource and immutable complete-column snapshot metadata.
Full world-height/absolute-Y traversal checks exposed surface and caller-selected
consecutive depth under explicit cell budgets. Covered/shallow water differs from
unknown; any unknown cell keeps its column unknown. Immutable source reads alone
do not load chunks, change blocks/entities, save resource state or found businesses.
Eight new domain/adapter cases; focused38 passed, zero failures/errors/skips,
19.764s at17:38:13 ET. Full Windows Wrapper clean verify passed1,229 (1,227 unit/
two packaged integration), zero failures/errors/skips,6:37 at17:46:19 ET. Native
actual snapshot check passed at17:48:52:98,304 reads/256 reference columns,220 suitable,
512 cells/tick, no helper block/entity changes; normal shutdown17:48:53. Read-only
main-save audit108/115 tables exact; seven permitted clock/mask/ore/farm-freshness/
resident-age-only changes, all identity/assets/cash/history preserved; empty fishery/
public-order tables, schema67 integrity/FK healthy. Cleanup initially deferred when
automatic approval review reached its usage limit. On user resume, approved audit/
cleanup succeeded: exact original properties restored, helper archived, backups
retained and fixture stopped. No human-server changes.
User approved suitable player-built ponds alongside natural water. No live
observation scheduler or automatic fishery startup enabled. Current evidence/SQL-owner
submission and protected qualified founding remain next. See WATER_OBSERVATION.md.
Phase3 counts unchanged:1/4 whole groups/~69% partial. No design decision pending;
the execution-capacity blocker is resolved.

## October12, 2026 — approved confirmed fishery habitat transitions

User approved the recommended loss/restoration rule. FishStock now represents dry
habitats and proportional effective capacity from original saved abundance/supports.
Only excess fish are lost; restoring capacity never refills. Explicit complete
FisheryHabitatObservation covers every original coordinate, distinguishing absent
from unknown. FisheryRepository validates world/coverage/freshness and commits
effective capacity/stock/fraction/time with an idempotent before/after/loss audit.
Original supports/terms, owned food, cash, workers and business history survive.
Schema67 adds one nullable column and one initially empty history table/index;
old resources retain original capacity without backfill or table rebuild.

Five domain and six temporary-SQL cases added: deterministic 10,000 transitions,
excess-only loss/restoration, dry normal timers, unknown/stale/omitted evidence,
restart/replay/retunes, late audit rollback and schema66 migration failure/retry
with all old rows preserved. Focused53 passed, zero failures/errors/skips,24.444s
at17:22:26 ET. Full Windows .\mvnw.cmd clean verify passed1,221 (1,219 unit/two
packaged integration), zero failures/errors/skips,6:57 at17:29:57 ET. Guarded native
normal-owner dry/restoration/pause/restart check passed at17:31:19 and stopped
normally. Evidence was explicit fixture support status, not a native water survey;
no world mutations. Main disposable save111/114 old tables exact; three ordinary
clock/economy/ore bookkeeping changes, all old identities/assets/cash/history
preserved, fishery tables empty and schema67 integrity/FK healthy. Original
properties restored byte-exact, helper archived, consistent backups retained.
No human server/world/DB changes. Automatic live water
observation/founding remains unfinished. Phase3 stays1/4 whole groups/~69% partial.
The earlier pending habitat question below is answered by this approval.

## October12, 2026 — persistent fisheries and ordinary timed catches

Added FisheryObservation/FisheryRepository and schema66: three initially empty
resource/column/work tables plus an index, preserving all old identities/assets.
Site/world identity, distinct column claims, late-overlap rollback and immutable
opening terms prevent refill/duplicate resources. IndustryRepository now commits
qualified actual catch, habitat recovery/remainder, owned food, receipt/period and
work audit together. Old receipts replay exactly; changed/stale requests reject.
Normal IndustryRuntime/PopulationSession clocks use the existing online/world
availability contract; disabled periods do not later catch up. Resting/unavailable
firms can recover habitat but cannot invent workers/catches. Existing optional
tool upkeep/learning are retained; legacy habitat-free fishing recipes remain.

Ten @TempDir SQLite tests added: restart/retune/replay, incomplete/wrong-world/
overlap, unqualified labor, exhaustion/rest/storage, real paid village-food use,
disabled policy, absence/downtime fractions, late history/checkpoint rollback,
schema65 failure/retry preservation. Focused10 passed at16:59:24 ET (12.138s).
First scoped run exposed two incorrect fixture assumptions: one fisher's four-unit
work cap and observeAvailability's previous-world boundary mask. Corrected the
fixtures, not the existing clock or production rules. Historical schema rewind
helpers now remove new tables before reproducing older schemas.

Full Windows .\mvnw.cmd clean verify passed1,210 (1,208 unit/two packaged
integration), zero failures/errors/skips,6:44 at17:07:24 ET. Guarded Paper normal-owner
probe passed at17:08:19 and stopped normally: conserved saved stock/owned catches/
cash; exact absent-world pause, owner downtime and disabled-policy restart; resumed
without refill. Explicit trusted fixture water evidence, not a native water survey;
no world block/entity/chunk changes. Main disposable save105/111 old tables exact,
six normal clock/mask/ore bookkeeping changes plus resident ages only; old identity/
assets/cash/history retained. New three main fishery tables empty; schema66 integrity/
FK healthy. Exact properties restored, helper archived, consistent backups retained.
No human server/world/database change; no automatic suitable
water observer/founders or new construction. Next: current bounded water evidence
and atomic qualified/protected fishery startup. Asked how confirmed supporting-water
loss should affect capacity/stock before enabling automatic fisheries; answer pending.
Phase3 counts remain1/4 groups/
~69% partial until broader live catalog acceptance completes. See FISHING_RESOURCES.

## October12, 2026 — approved renewable fishing calculation foundation

Recorded the user's approval for fish stock to ebb and recover. Added plain-Java
FishingPolicy/FishStock with habitat-limited opening stock, fractional capped
recovery and catch limited by qualified labor/request/site/storage. Heavy harvesting
can temporarily exhaust supply; disabled/absent-world transitions preserve exact
state. Opening retunes do not refill an existing state. Strict optional commented
economy.yml terms are validated before DB startup; older missing sections opt out.

Nine domain and two configuration tests added, including seeded10,000-period
conservation, exhaustion/rest recovery, fractional renewal, pause and invalid edits.
Focused87 tests passed, zero failures/errors/skips,13.687s at16:40:35 ET.
Windows .\mvnw.cmd clean verify passed1,200 tests (1,198 unit/two packaged
integration), zero failures/errors/skips,6:48 at16:48:06 ET. Diff review/check passed.
No migration: schema65 unchanged. No actual server/DB/world changes, native
world adapter, automatic fishery, merge or push. The calculations do not themselves
save stock or establish uptime; transactional resource/owned-food persistence,
water observation/identity and normal business-clock integration remain next.

See FISHING_RESOURCES.md. Phase3 stays1/4 whole groups/~69% partial, with the
broader industries checklist open. The fishing recovery decision is now approved;
older entries below preserve their original pending-decision checkpoint.

## October12, 2026 — native stone/sand provenance and source-loss safety verified

OrePlacementTracker now captures raw stone/deepslate/sand/red sand placement,
break/piston/explosion, entity changes and new formation through its existing
bounded immutable-coordinate/atomic SQL seam. Confirmed removal retires only the
original remaining share; moved/generated destinations cannot create fresh
reserves. Cancelled/unchanged events do nothing; queue/save failures retain the
existing fail-closed fence. PaperOreSource has explicit quarry snapshot mode;
default coal/iron scanning is unchanged. No automatic quarry observer/founder,
new extraction timer or construction was enabled. Old origins remain unknowable.

Eight new adapter tests; focused33 passed. Windows .\mvnw.cmd clean verify passed
1,189 (1,187 unit/two packaged integration), zero failures/errors/skips, 6:16 at
15:53:56 ET. Guarded native synthetic handler/real-block/snapshot probe passed at
15:56:20: reserve54 ->36 ->18 ->0, extracted6 retained, moved/generated coordinates
excluded, three temporary blocks restored and fixture ticket released. Normal
shutdown. The first probe omitted FK enablement on its read-only check connection;
the application correctly rejected it. Corrected only the probe and reran after
normal restoration/audit, with separate fresh backups. Final main-save107/111
tables exact; four normal clock/farm-freshness/ore bookkeeping changes, all old
identity/assets/cash/history exact; schema65 integrity/FK healthy. Properties
restored byte-exact, helper archived, both backups retained. Human server untouched.

Same Phase3 milestone counts:1/4 complete groups/~69% partial; source safeguards
do not finish broader live industries. Next: coherent current quarry evidence,
founder/consumer/operation wiring and renewable industries. Asked whether fishing
uses habitat-backed saved fish supplies with gradual recovery/temporary depletion;
that consequential resource rule remains pending, with no fish recovery enabled.
See FINITE_QUARRIES.md and DECISIONS_AND_OPEN_QUESTIONS.md.

## October12, 2026 — approved real village supply customers connected

User approved needs-based public food/training purchases from real local firms.
Added VillageSupplyPolicy, strict optional economy.yml configuration, SQL-owner
VillageSupplyPurchases and normal PopulationSession/food-period wiring. Current
recorded food shortages or active productive/supply-starved courses create need;
public stocks, player commitments and owned incoming cargo count first. Qualified
operating local suppliers receive real treasury payments for their actual stock.
Food/material period budgets are separate; materials preserve assessed food cash.
Public cargo is owned once, delayed and imported before consumption in one period
transaction. Current food-response planning includes it; diagnostics show orders.
Read-only funded demand shares real cash/caps/hauling without reserving money or
inventing a business. New supplier founding remains a separate next milestone.

Schema65 adds one empty audit/cargo table and index; retains all old assets and
history. Exact retry terms, unrelated payment-ID refusal, restart, absence,
backlog, cancelled course, late payment/arrival failure and migration recovery
have dedicated temporary SQLite coverage. Missing old configuration opts out;
disabled new orders still deliver owned old cargo. No human server deployment,
world edit, merge or push. See VILLAGE_SUPPLY_CUSTOMERS.md for prototype terms.

Verification: Windows .\mvnw.cmd clean verify passed 1,181 tests (1,179 unit/two
packaged integration), zero failures/errors/skips, 6:14 at15:38:37 ET. Focused114
passed. Guarded isolated Paper normal-session test passed at15:39:21: real paid
practice goods, conserved money, owned arrivals/consumption, absence pause and
restart with new orders disabled. No world operations. Normal shutdown; main
fixture107/110 old tables unchanged, three normal clock/ore bookkeeping changes;
old identity/assets/cash/history exact, new public orders empty under old config.
Schema65 integrity/FK healthy; exact properties restored, helper archived, backups
retained. The first full run exposed70 historical-schema fixture errors:
the rewind helper left the new table present. Corrected that helper, preserving
strict application migration guards; final full verification passed.

Roadmap counts remain1/4 whole Phase3 groups/~69% partial: this customer seam does
not complete automatic broader industries or their live acceptance. Next: resource
observation/provenance, suitable sites, qualified founders and actual timed output
for logging/fishing/quarries, using genuine funded village demand.

## October12, 2026 — finite stone/sand persistence and extraction verified

Recorded approval for existing exposed stone/sand, excluding recorded placements;
non-1:1 finite coordinate reserves, no refills and no Phase3 block changes. Added
QuarryPolicy/economy.yml terms, Commodity.SAND and bounded quarry scan mode while
retaining default coal/iron scanning. Existing trusted startup/grant/extraction
seams accept matching quarry/stone or sandpit/sand and conserve real cash/output.
Worker/site/storage limits, per-coordinate depletion/removal, replay and retained
old yields/reserves use the existing tested calculations/history. This is backend
support, not automatically founded/live-observed quarries or a block-rights grant.

Schema64 widens mineral_discovery's resource constraint via transactional parent
rebuild. Old data unchanged; enforcement temporarily off before required upgrade
transaction, all FKs checked before commit, original enforcement restored. Both
pre-rebuild failure and post-rebuild invalid-FK failure roll back DDL/version/rows;
healthy retry preserves all assets/history. New15 tests: nine temporary SQL cases,
four scan/production cases and two config cases.134 focused passed; full Windows
.\mvnw.cmd clean verify passed1,163 tests (1,161 unit +2 packaged integration), zero
failures/errors/skips,6:11 at14:34:42. No Paper-facing world/entity change in this
batch, so no additional native server run; human server/config/DB untouched.

Updated FINITE_QUARRIES, decisions, status, progress and checklist. Counts remain
4/4,27/33,14/15,0/5 (~69% partial): broader renewable/catalog/live acceptance remains.
Next: bounded live observations/mutations (including falling sand), current demand
and qualified founding; asked whether public food/training needs may buy actual
local output with real bounded village money and food protection. That economic
rule is pending; no new customer/payment enabled. Local branch
codex/phase3-finite-quarry-sources; no merge/push/deploy.

## October12, 2026: broader startup grants approved and verified

Logging, fishing, quarry and sandpit now appear in the bundled startup-grant
industry list and domain prototype. All numeric limits/food protection are unchanged;
existing installed configuration lists and historic receipts are never rewritten.
No migration: schema63 unchanged. Wider automatic producers are not implemented
by this policy change. Focused105 tests passed, including eight new temporary
SQLite cases plus policy/configuration preservation tests. Full Windows
.\mvnw.cmd clean verify passed1,148 tests (1,146 unit +2 packaged integration),
zero failures/errors/skips,6:09 at14:13:03. Final added reverse-budget assertions
were recompiled and verified with all29 BusinessStartupGrantsTest cases passing
at14:13:29 (14.524s); application code did not change after the full build.
The old fishing-disabled assertion now uses unapproved bakery instead.
No Paper-facing change, so no new native server run; human server/DB untouched.

Phase3 counts remain4/4,27/33,14/15,0/5 (~69% partial). Next resource baseline choice
for exposed existing stone/sand was pending at this checkpoint and is now approved;
known placements must remain
excluded, finite sources must never refill and Phase3 must not change blocks.
Feature branch codex/phase3-broader-startup-grants; human server untouched.

## October12, 2026: automatic first warehouses and real merchant study verified

Ordinary bounded Paper warehouse startup now polls one SQL preparation/acceptance
at a time, rotates saved towns and reads one proposed loaded barrel on the main
thread. No chunk loading/tickets, inventory access, block changes or building
ownership. Startup diagnostics are included in debug observation; adapter cancels
before the SQL owner closes. Expected economic declines defer/retry and unexpected
failures log. Qualified preparation/acceptance remains the atomic tested path.

If no available merchant qualifies, a current genuinely fundable mineral-chain
prospect can sponsor one unemployed adult's commerce course, under existing study/
career/founding budgets and qualifications. Default practice uses real timber,
one XP per actual future food period toward10, retaining saved terms/reserves.
No enrollment XP, assets, site/funding reservation, free goods or personal payroll.
Current opportunity checks pause unavailable/unknown/unsafe/unfunded prospects;
a taken first warehouse releases unfinished learners. Death/migration/employment
still cancel normally. Previously earned backlog cannot teach a new learner.
Workshop and warehouse course checks coexist without disabling each other.

Schema63 transactionally adds two initially empty warehouse review/course-link
tables and an index. Existing identity/assets/skills/course terms/history survive.
Failure/retry preserves every old row. Legacy missing optional settings opt out;
files are not overwritten. Broader industry grants were pending at this checkpoint;
they were subsequently approved in the newer entry above. See
AUTOMATIC_WAREHOUSES.md and WAREHOUSE_INVESTMENT.md.

Ten temporary SQLite study/cycle cases +four bounded-cycle cases added;48 scoped
checks passed. Full Windows .\mvnw.cmd clean verify passed1,138 tests (1,136 unit
+2 packaged integration), zero failures/errors/skips,6:15. Corrected initial
fixture coal expectations for an already-paid workshop starter and respected the
saved once-per-food-period review before retrying. A final cosmetic warehouse
budget diagnostic correction was compiled/packaged with four cycle +two packaged
integration checks passing (17.565s); no economic behavior changed after full run.

Final packaged WarehouseStartupProbe passed on pinned disposable Paper26.2-129 at
11:52:51: explicitly unskilled merchant earned ten real prototype timber-practice
periods via normal SQL timer; scheduled Paper barrel proof opened a paid empty
warehouse for an actual paid toolkit customer/observed mineral chain. Removal,
wrong marker, restoration and restart retained real money/jobs/history. Four
original air blocks restored exactly; helper ticket/executor/owner closed; normal
shutdown saved all worlds, exit0. Initial JVM option quoting was corrected before
server startup. Main-save audit104/108 old tables unchanged, four normal clock/
economy/farm freshness/tracking changes only; two new tables empty, schema63
integrity/FK healthy, exact properties restored and helper archived. Originals,
consistent backups, isolated course DBs and logs retained. Human server untouched.

Current core coal/iron/toolkit catalog now has automatically selected/funded/
staffed first mines/workshops/warehouses and bounded real first-founder skills/
investment. Those two scoped checklist entries are complete; broader resource
catalogs and competition/specialization stay open. Counts4/4,27/33,14/15,0/5 (~69%
partial), one of four whole gates complete. Not100% or multi-year acceptance.
Next: forestry/fishing/finite stone/sand and investment comparison/reassessment,
then multi-seed long-run acceptance. The wider-industry grant rule was pending
at this checkpoint and is now approved;
resource/production adapters still remain. No main merge/push/deploy. Feature
branch codex/phase3-automatic-warehouses; only coherent tested local commits.

## October12, 2026: qualified first-warehouse investment verified

Added read-only qualified commerce-founder preparation and current-fact atomic
warehouse acceptance on the SQL owner. Real customer/complete finite-source and
whole-chain food/grant budgets are rechecked with exact terms, qualifications,
one worker job, no active course and fresh matching loaded-barrel proof. The
transaction creates a paid empty store, first worker and fixed marker binding;
no goods, mine, shipment or free skill. Stable town/world/coordinate identities
survive retries. Late binding failure rolls back every debit/asset/job; accepted
replay preserves pause/history and cannot bind a legacy firm or change store terms.
Unlimited ownership remains; one employee job remains. Optional strictly validated
warehouse-founding uses editable commerce10/1, age216, capacity2 and page64; missing
old sections disable founding. No migration: schema62. No ordinary warehouse
scheduler or automatic merchant study is claimed by this batch.

Twelve temporary SQLite tests plus two configuration tests added. Scoped129 tests
passed; .\mvnw.cmd clean verify passed1,124 tests (1,122 unit +2 integration), zero
failures/errors/skips,6:02. Initial fixture enum/config-root and manual assessment
errors were corrected; the fixture now uses the genuine next food assessment,
without relaxing application guards. SQL-owner/restart, real assets/cash, changed
customer/source/food/funds/qualification/course/job/point evidence, edited terms,
late SQL rollback, legacy replay and unlimited ownership covered. See
WAREHOUSE_INVESTMENT.md. The packaged Paper workshop-training/startup/marker regression passed at11:32:43.
It also rechecked the disabled upstream warehouse prospect, not positive native
warehouse acceptance. After the console stdin closed, a temporary guarded attach
helper verified exact JVM directory/marked root/localhost25578/zero players and
scheduled the normal Paper shutdown; all worlds saved at11:35:03, no forced kill.
The save audit found101/108 original tables unchanged; the remaining seven changed
only normal clock/tracking/mask/resident-age/farm freshness/completeness fields.
An initial one-way farm completeness assertion rejected normal1-to0 partial
observation; every crop/site/world field was inspected unchanged before allowing
that normal transition. SQLite integrity/FK healthy; exact properties restored,
helper archived, originals/backups retained. Human server untouched.

Next: gradual commerce-founder study and the ordinary automatic warehouse startup
adapter, then connected first mines and broader industries. Broad Phase3 counts
remain4/4,26/33,13/15,0/5 (~66% partial), not complete specialization. Feature branch
codex/phase3-warehouse-investment; no merge/push/deploy or playable-server changes.

## October12, 2026: read-only first mineral-chain warehouse prospects verified

The SQL owner can now review a first warehouse for actual staffed/funded workshop
input gaps after owned/incoming goods, a current unclaimed nearby barrel, trusted
suitable coal/iron for every input and protected public grant feasibility. Whole
initial purchases must fit real warehouse capital/storage/haul. Whole startup
cash and remaining lifetime grants must cover warehouse plus one needed mineral
supplier per input, without guessing future public income. Each source needs
sufficient finite potential in a connected patch; disconnected deposits cannot
be pooled into one affordable mine. Current-period grants remain shared: a paid
workshop can exhaust the allowance until a real later food period. No review
creates/reserves funds, stock, employment, sites, deposits, courses or businesses.
Existing warehouse identity, including closed firms, prevents first-firm bypass.

Added WarehouseOpportunities/WarehouseOpportunityPolicy, strict optional editable
economy.yml section, startup configuration and read-only PopulationSession API.
Schema62 unchanged; missing legacy section disables review. Updated stale bundled
comments and workshop integration docs. This is prospect evidence, not automatic
warehouse founder study, final fresh-block acceptance or scheduling. Wider catalogs,
competition and specialization remain. Prototype terms reuse approved grant/
marker/economy direction; no new payment, ownership or resource regeneration rule.

Thirteen temporary SQLite tests and two config cases verify actual paid toolkit
sale leading to raw input need, no-write determinism/restart, shared budgets,
whole-chain cash/lifetime limits, finite connected potential, missing/unsafe/
placed/range-excluded/stale/future/absent sources, changed customers, retained
warehouse identity, whole-page bounds, legacy opt-out and actual SQL-owner calls.
92 targeted tests passed. Final Windows .\mvnw.cmd clean verify passed 1,110 tests
(1,108 unit + 2 integration), zero failures/errors/skips, 6:22. Earlier full run
also passed before the additional whole-chain guards. Initial fixture/API call
issues were corrected; real period-budget refusal was retained, not bypassed.

Final packaged native probe repeated unskilled-owner timed study/automatic
workshop startup plus marker/absence/restart/conservation. It also called the new
warehouse API and checked a disabled upstream chain produces no prospect; positive
warehouse prospect verification is SQLite/actual SQL-owner, not native founding.
Disposable Paper stopped normally; no force-loaded chunks, temporary block/ticket
restored/released. 101/108 original main-save tables unchanged; seven normal
clock/tracking/world-mask, resident age and farm observation changes only. Identity,
assets, money/history and empty schema62 founder tables preserved, integrity/FK
healthy. Exact properties restored, helper archived and backups retained. Human
server untouched. No main merge or push. Current branch: codex/phase3-warehouse-opportunities.

Phase3 partial checklist stays about66%; no broad gate is falsely completed.
Next: eligible merchant preparation/training, fresh barrel acceptance and ordinary
warehouse startup, then connected first mining chain and wider industries. No new
user decision blocks that scoped work. See WAREHOUSE_OPPORTUNITIES.md.

## October 12, 2026: automatic supply-backed first-workshop owner training verified

Ordinary scheduled workshop reviews can now sponsor one eligible unemployed adult
for a real viable first-workshop opportunity, using saved smithing courses and
actual practice goods in future food periods. No skills, firm, funds, site rights
or goods are created at enrollment. Existing editable study/career/qualification
limits apply; food and material reserves take priority. Saved rotating reviews,
course/marker links and future eligibility survive restart. Current opportunity
is rechecked each period: unknown/stale/disabled/unfunded/lost demand pauses study;
a different blacksmith taking the first opportunity releases the learner through
cancellation. Existing learning/history remains. Qualified graduates flow through
normal Paper point evidence and atomic paid startup; read-only preparation stays
read-only. Automatic diagnostics now explain disabled recognition, population
backlog and economy failures separately.

Added WorkshopFounderStudy, SQL-owner automatic review and economy practice wiring.
Schema62 adds empty review/course link tables and index with transactional upgrade.
Ten temporary SQLite cases cover gradual real spending/qualification/startup,
unknown/missing/funds pauses and restoration, earned-backlog exclusion, transaction
rollback/retry, absence/restart, disabled settings, insufficient targets/supplies,
taken opportunity, migration failure/recovery and actual PopulationSession calls.
86 targeted tests passed; final Windows .\mvnw.cmd clean verify passed 1,095 tests
(1,093 unit + 2 integration), zero failures/errors/skips, 5:42. Initial test fixture
food/population/seed mismatches were corrected without relaxing app safeguards.

Native WorkshopInvestmentProbe passed with an initially unskilled adult: ten real
two-material practice periods through normal SQL timer, automatic loaded-table
review/paid startup, marker removal/wrong-kind/restoration and absence/restart.
Real synthetic working-farm customer and explicit fixture funds/supplies; no seeded
skills. Conserved money/jobs/cargo/history, temporary air block restored and helper
ticket released. Disposable Paper stopped normally; zero force-loaded chunks.
99/106 prior main-save tables unchanged; seven normal clock/tracking/world masks,
resident age_steps and farm capture timestamp/completeness changes only. A normal
farm scan completed from0 to1; crops/location/world unchanged. New schema62 tables
empty; integrity/FK healthy, exact properties restored, helper archived/backups kept.
Human MinecraftDevServer untouched. No main merge or push.

Phase3 remains about66% partial checklist: this closes the first-workshop training
connection, not the broader nonfood founding/specialization gate. Next: current
first-warehouse opportunities and real merchant startup/training. No new question
blocks that scoped work. See WORKSHOP_FOUNDER_TRAINING.md and AUTOMATIC_WORKSHOPS.md.

## October 12, 2026: scheduled first-workshop startup verified

Normal Paper startup now runs bounded rotating first-workshop reviews for towns
with saved living residents. One proposal/acceptance is outstanding at a time;
completed futures are polled on the main thread, and at most one already loaded
smithing-table point is checked per pass. Current SQL qualification, real customer,
protected funding, paid surplus, storage and jobs are rechecked before acceptance.
Unknown/missing markers spend nothing; changed facts retry without partial startup.
Expected refusals are diagnostic; unexpected faults are logged. Disable cancels the
adapter before closing SQL. No new settings, behavior rates or migration; schema61.

Added WorkshopStartupCycle, PaperWorkshopStartup and startup/debug/shutdown wiring.
Four coordinator tests and two actual temporary SQLite cases cover asynchronous
bounds, rotating towns, failures, missing/unknown blocks, changed funds, paid startup
and no duplicate investment. Windows .\mvnw.cmd clean verify passed 1,085 tests
(1,083 unit + 2 integration), zero failures/errors/skips, 5:39. Native updated
WorkshopInvestmentProbe used the actual scheduled Paper adapter, then retained
marker pause/restoration, absence/restart and real workers/money/cargo/history.
It explicitly seeds qualified fixture experience; automatic training is not claimed.

Disposable server stopped normally; no force-loaded chunks, temporary air block
restored, helper ticket released, helper archived, exact server.properties restored.
99/106 original save tables unchanged; seven normal clock/tracking/world-mask,
resident age_steps and farm observed_millis changes only. Identity/assets/cash/history
unchanged, schema/integrity/FK healthy; consistent backup retained. Human server
untouched. Phase3 broad checklist counts stay about66%, with ordinary first-owner
training, warehouses, broader resources/competition/specialization still unfinished.
Next: supply-backed first-workshop founder study; no new question blocks it.
See AUTOMATIC_WORKSHOPS.md. No main merge or push.

## October 12, 2026: qualified first-workshop investment acceptance verified

The existing real opportunity review now prepares a deterministic proposal with a
qualified available smithing owner, permanent marker/site/business/order IDs and
exact editable investment terms. Preparation saves/spends/reserves nothing.
After a fresh loaded Paper table check, SQL acceptance rechecks current customer,
food/population proof, protected funds, real surplus, recipe/storage, marker survey
and owner age/skills/jobs/study before one atomic funded startup. Changed facts or
late SQL failures leave no partial business/payment/job/order/attachment. Accepted
replay after restart cannot charge again, refill cargo or reset confirmed pause.
Ownership remains unlimited; one employee job still constrains worker availability.

Added WorkshopInvestment and WorkshopFoundingPolicy, strict optional economy.yml
qualification settings, startup configuration and normal PopulationSession owner
preparation/acceptance APIs. Extracted the existing marker startup rows so the
current recheck and complete paid startup share one transaction. No new migration;
schema61 unchanged. Qualification filtering precedes the bounded candidate page,
then domain eligibility checks and stable seeded selection among feasible options.
Prototype owner smithing10, employee smithing1, age216 months or configured adulthood,
capacity2 and page64 are editable; existing files without the section opt out.

Final Windows Wrapper clean verify passed 1,079 tests (1,077 unit + 2 integration),
zero failures/errors/skips, 6:34. Ten temporary SQLite cases and two configuration
cases cover qualification/determinism, real budgets/customers/stocks, changed facts,
study/jobs, rollback, restart/replay/pause and actual SQL-owner calls. A complete
accepted-workshop scenario produces one real toolkit from paid inputs and sells it
to the actual productive farm with protected real funds and delayed owned cargo.
Initial targeted fixture issues (a full farm and insufficient food money for a
larger test population) were corrected without relaxing application behavior.

Native WorkshopInvestmentProbe passed SQL-owner preparation, actual loaded table
confirmation and atomic acceptance for an explicitly qualified synthetic fixture
resident/working farm, then real marker removal/wrong-kind/restoration, world pause,
restart and retained money/jobs/cargo/history. It does not claim automatic ordinary
villager training. Temporary air block restored, test ticket released, normal stop,
zero force-loaded chunks. 99/106 original main-save tables unchanged; seven changes
only normal clocks/tracking/world masks, resident age_steps and farm observed_millis.
Identity, assets, cash and histories unchanged; integrity/FK healthy. Exact server
properties restored, helper archived and all backups retained. Human server untouched.

This is tested preparation/acceptance, not the ordinary automatic first-founder
training/scheduling loop or finished warehouse startup. Those remain next, followed
by wider industries, specialization and long-run acceptance. Phase3 partial checklist
stays about66%; no whole gate is falsely completed. No main merge or push. Current
branch: codex/phase3-workshop-investment-acceptance. See WORKSHOP_INVESTMENT.md.

## October 12, 2026: approved fixed-workplace pause and restoration verified

New explicitly marker-founded workshops/warehouses keep a permanent required table/
barrel anchor. Confirmed removal or wrong block type pauses production and new
buying/selling; the right marker at the same spot resumes operation. Unknown chunks
and absent worlds do not invent removal. Jobs, money, stocks, paid cargo, ownership
and history remain; deliveries already paid for can arrive in a present world.
No productive receipt means no free learning. Legacy firms acquire no requirement.
Editable economy.yml pause-on-missing applies on restart without deleting evidence.

Added WorkplaceOperationPolicy, WorkplaceOperations, bounded main-thread
PaperWorkplaceOperations and SQL-owner APIs. BusinessOperations and direct industry
production/purchase paths enforce the guard. New trusted marker startup composes
binding atomically with actual workshop/warehouse grants, founding, staffing and
paid workshop inputs. Accepted replay never resets pause or refills supplies.
Schema61 adds two initially empty anchor/history tables plus one setting row;
versioned transactional migration failure/retry preserves old assets.

Final Windows Wrapper clean verify passed 1,067 tests (1,065 unit + 2 integration),
zero failures/errors/skips, 5:25. Twelve temporary SQLite cases plus one strict
configuration case cover state, replay, cargo, both trade endpoints, timed learning,
SQL-owner/world guards, overrides, rollback and migration recovery. Earlier full run
also passed; native helper was recompiled against the final packaged artifact.
Native WorkplaceOperationProbe passed actual scheduled loaded-block reads, removal,
wrong kind, exact restoration, absent-world guard and owner-session restart. One
explicit temporary air-block fixture restored exactly, its helper ticket released;
production performs no block/inventory writes or chunk loading/ticketing.

Disposable Paper stopped normally, zero force-loaded chunks in all dimensions.
96/103 original save tables unchanged. Seven changes are ordinary clocks/tracking,
active-world masks, resident age_steps only and farm observed_millis only; identity,
assets, money and histories unchanged. New main-save anchor/history tables empty;
SQLite integrity/FK healthy. Exact server properties restored, helper archived and
all backups retained. Human MinecraftDevServer untouched. No main merge or push.
Phase3 partial checklist remains about66%; this does not yet finish automatic
first-owner preparation or first workshop/warehouse selection/acceptance scheduling.
Next: connect real first-workshop founder preparation and fresh-block acceptance.
No additional design decision blocks that work. See WORKPLACE_OPERATIONS.md.
Current branch: codex/phase3-workplace-operation-pause.

## October 11, 2026: first-workshop investment opportunities verified

A read-only first blacksmith/toolkit review now joins current nearby smithing-table
markers, actual productive farm replacement demand, shared existing warehouse
stock, current food-protected real grant affordability, complete storage and the
approved paid surplus bundle. Existing workshop identities, stale/partial/future/
absent evidence, changed population, budgets and missing materials defer. Repeated
reviews do not spend, reserve, found, hire or award skills. Acceptance and planning
share WorkshopStarterPlan rather than separate surplus/price/storage calculations.

Added WorkshopOpportunities, WorkshopOpportunityPolicy and WorkshopStarterPlan;
connected strict optional economy.yml settings, startup configuration and the
normal PopulationSession SQL-owner read API. Updated the purchase seam to use the
shared calculation. Nine temporary SQLite cases plus two configuration cases
cover real evidence, denial/no-write conditions and restart/world guards.
Final Wrapper clean verify passed 1,054 tests (1,052 unit + 2 integration), zero
failures/errors/skips, 5:40. Updated disposable Paper probe passed both the earlier
paid starter case and actual SQL-owner opportunity review: current synthetic
marker, real working farm/customer, affordable surplus, repeated reads and world
pause, no spending/founding/free skill. Stopped normally; all dimensions have zero
forced chunks. 100/103 original fixture tables unchanged; only civilization_time,
economy_runtime and ore_tracking changed. Assets, money, residents and histories
unchanged; exact properties restored and helper archived. All backups retained.

Schema60 unchanged. This is trusted read-only preparation, not automatic first
workshop/warehouse founding, completed skill training, a player command or current
block acceptance. Qualified founder preparation and Paper revalidation remain
next. Need the operational rule for confirmed removal of a new workplace marker
before enabling that live lifecycle. No rule has been silently chosen. Wider
industries/specialization remain; partial checklist stays about66%, one whole
Phase3 group complete. No human-server or main/push changes. See
WORKSHOP_OPPORTUNITIES.md. Current branch:
codex/phase3-first-workshop-opportunities.

## October 11, 2026: approved paid workshop starter supplies

A new publicly funded workshop can buy one real recipe bundle from village
surplus. Actual business cash returns to the treasury; food is never sold and
editable material reserves remain. Grant, founding, first job, purchase and cargo
are atomic. New IDs cannot refill the one-per-business entitlement. Paid incoming
goods reserve storage, suppress duplicate demand and retain their resale value;
normal timed delivery/production survives restart and absent-world pause.

Added WorkshopStarterPolicy, WorkshopStarterSupplies and the transactional startup
composition, strict optional economy.yml settings and two empty schema60 audit
tables. Extended storage, purchasing/demand and resale accounting for this cargo.
Older stores remain readable after failed upgrades; a migration test now compares
successful upgrade with CURRENT_VERSION rather than a hardcoded previous version.
13 temporary SQLite tests and two configuration tests cover real conservation,
limits/food proof, replay, delayed work, sale assets and migration/write failures.
Final Wrapper clean verify passed 1,043 tests (1,041 unit + 2 integration), zero
failures/errors/skips, 6:08. Earlier verification exposed legacy inspection/test
assumptions; corrected and rerun fully. Native WorkshopStarterProbe passed under
pinned disposable Paper: real starter purchase, restart/absence, one toolkit,
no duplicate or minted money, SQLite integrity/FK. Stopped normally; no forced
chunks. 95/101 original tables unchanged; six normal clock/tracking/aging tables
changed, residents only in age_steps. Two new main-save audit tables are empty.
Exact properties restored, helper archived, all backups retained.

Automatic first-workshop opportunity selection/training and first warehouse startup
remain next. This persistence seam does not automatically create firms, establish
property rights or complete fresh-village acceptance. Phase3 counts remain 26/33
in connected production and about66% across partial groups; no whole group newly
complete. No human-server, main, push or canonical-roadmap change. See
WORKSHOP_STARTER_SUPPLIES.md. Current feature branch:
codex/phase3-workshop-starter-supplies.

## October 11, 2026: connected finite industry endurance regression

Added a normal PopulationSession scenario spanning 50 economic periods: a finite
iron/stone source, staffed mine, warehouse and blacksmith, real productive farm
replacement demand and approved earned-surplus upkeep contributions. The scenario
checks source exhaustion, repeated farm payments, retained stone byproducts,
conservation of the original 30,000 hundredths and all eleven available toolkits
across use, ownership and paid cargo. Repeated pulses and absent-world restart
must not create orders or refill the source. Integrity and foreign keys checked.
Final Wrapper clean verify passed 1,028 tests (1,026 unit + 2 integration),
zero failures/errors/skips, 5:11. The focused scenario also passed.

This is test-only operation/depletion coverage, using explicit fixture businesses,
supplies and controlled test ratios. The prior native Paper check already verified
the unchanged application code and paid farm chain. This new 50-period scenario
is not a multi-year specialization test or proof of automatic first founding.
Schema59 unchanged; no migration, settings or human-server changes. Next: first
warehouse/workshop viable opportunities and qualified founders, then wider
resource operations and seeded specialization. See CONNECTED_INDUSTRY_SCENARIO.md.

## October 11, 2026: public farm equipment demand connected to warehouses

A real productive farm can now drive warehouse restocking for its next toolkit.
Read-only demand requires current complete fields, actual food output and matching
history, qualified staff, a present world, fully supplied current residents and
real protected village funds. Existing service/owned/paid incoming kits suppress
new pressure. Shared local stock is counted once; bounded overflow defers.
Actual restocking pays for existing producer goods with warehouse cash, delivers
cargo normally and lets the public farm buy/use the toolkit under its established
upkeep budget. No free stock, simulated customer, extra levy or reserved money.

Eight new temporary SQLite tests cover missing work/funds/food, changed population,
service/incoming goods, legacy catalog, mismatched history, stock/bounds and the
paid chain through normal SQL-owner restart/absent-world resumption. Wrapper clean
verify passed 1,027 tests (1,025 unit + 2 integration; zero failures/errors/skips),
5:03. Native Paper passed prior upkeep checks plus paid public-farm demand, restart,
absence and replay. Stopped normally; no forced chunks. 98/101 original fixture
tables unchanged; three normal clock/tracking tables changed. Exact original
properties restored; test-only helper archived. Schema 59 unchanged; no migration,
new settings, world/block/inventory edits or changes to the human server.

First warehouse/workshop opportunities and founders remain next. Wider resource
industries, competition, specialization and multi-year acceptance remain Phase 3
requirements. Partial checklist progress stays about 66% (4/4,26/33,13/15,0/5);
whole-group gate remains 1/4. See PUBLIC_FARM_INDUSTRY_DEMAND.md.

## October 11, 2026: opportunity-backed first mining founder training verified

When current observed ore, real funded workshop demand, a staffed warehouse buyer
and protected startup funds are available, an unskilled village can now sponsor one
prospective mining founder. Existing editable career/study/mine settings govern
age, skills, supplies, pacing and budgets. Enrollment creates no business, cash,
goods, job or skill; future food periods spend actual timber/stone under reserves.
Current opportunity loss cancels linked study before practice goods are spent.
Partial earned skills remain; absence pauses. A qualified graduate uses ordinary
atomic mine founding and grant checks; no guaranteed ownership or reserved money.
First warehouse/workshop founders remain next; no construction or hidden ore.

Schema59 adds two empty source-linked founder review/course tables. Existing
courses, assets and history remain. Ten tests added: gradual qualification/startup,
source/customer/funding loss, future-only backlog, rollback/retry, restart/absence,
settings/supplies, migration failure/recovery and actual SQL-owner opt-in/legacy
opt-out. Focused46 passed, then all23 mine tests passed. Windows .\mvnw.cmd clean
verify passed1,019 (1,017 unit/two packaged integration), zero failures/errors/skips,
5:09. Test fixtures initially used the wrong world-update method, omitted new
commodity prices and processed too few bounded backlog pulses; corrected fixtures.

Updated standalone AutomaticMineFoundingProbe compiled and passed on pinned marked
Paper: ten future supply-backed study periods, twenty practice-material spending
entries, earned mining qualification, exactly one real funded mine and conserved
absence/restart state. Existing finite-production/budget checks still passed.
Probe compilation first used a package-private constructor; changed to the public
DB accessor. The previous fixture was inadvertently launched after compilation
failed, stopped normally, then setup/launch were made conditional on success.
A subsequent fresh sub-fixture reused a future capture timestamp and correctly
failed to found by its expected deadline; reset its fixture capture clock and reran.
No production guards weakened. Probe compiled before the successful installation.

Server stopped normally with no forced overworld chunks. Original properties
restored byte-exact, helper archived, old99 tables compared:93 unchanged; six
changes are two ordinary age-only months1733-to1735 and normal clock/active-world/
ore bookkeeping. Food period29 unchanged; prior businesses, stock, money, grants,
skills, courses and history unchanged. New main-fixture founder tables remain
empty under legacy opt-out. Human server untouched; no push or main merge.

Phase3 remains1/4 fully complete groups; partial checklist progress is now shown
separately in PROGRESS.md (about66% on equal-group checklist arithmetic). That is
not remaining-time or playability measurement. Next: real public-farm tool demand
in warehouse purchasing, then first workshop/warehouse founder opportunities.

## October 11, 2026: read-only startup affordability and funding-first diagnosis verified

Industry opportunity checks can now inspect actual startup affordability without
inventing a business or reserving funds. Preview and acceptance share the current
food/population assessment, real treasury, shared period/lifetime spending and
editable grant policy. Ordinary shortages/missing evidence return blocked reasons;
actual SQL faults propagate. Acceptance still rechecks everything atomically.
Automatic mines check funding before founder selection, avoiding misleading skill
obstacles when protected food money is the actual constraint. Existing accepted
receipts/replay and grant defaults are preserved. Ore observation diagnostics no
longer incorrectly claim all automatic founding is disabled. Schema stays 58.

Six tests added, including all-table read-only comparisons, restart, missing/changed
food evidence, disabled industry/caps, non-reservation and changed funds before
acceptance, invalid terms and SQL failure recovery. Focused 34 passed. Windows
.\mvnw.cmd clean verify passed 1,009 (1,007 unit/two packaged integration), zero
failures/errors/skips, 4:59. Sandbox clean was denied; normal-permission rerun passed.

Updated AutomaticMineFoundingProbe compiled and passed on the marked disposable
Paper fixture: preview makes no assets/spending; protected-cash refusal prevents
startup; restored real funding permits exactly one mine via actual SQL owner;
eight finite batches yield sixteen coal; absent-world pause/restart preserve assets.
Native setup first expected the wrong fixture JAR filename and stopped before
launch; corrected to Civilizations.jar. This launch lacked an interactive console,
so stop input was unavailable. A temporary guarded JDK attach helper verified the
exact disposable world directory, dev marker, localhost port and no players, then
scheduled Bukkit shutdown on the main thread. Normal world saves completed; no
forced termination. Future launches must use an interactive terminal.

Properties restored byte-exact, probe archived, full database audit: 93/99 existing
tables unchanged; six differences are four ordinary age-only months 1729-to1733
and clock/active-world/ore bookkeeping. Food period stayed 29. Original businesses,
stock, treasury, grants, skills, courses, resident identity/status/history unchanged;
legacy workplace tables remain empty. Human server untouched; no push/main merge.
Phase 3 stays 25% by completed groups. Next: fundable first-founder opportunities,
gradual real-supply training and automatic first warehouses/workshops.

## October 11, 2026: approved workplace marker observation verified

Nearby loaded barrels and smithing tables can now be recognized as data-only
warehouse/workshop candidates. Editable economy.yml settings bound scan range,
work per tick and marker types. Main-thread immutable snapshots never inspect
inventories, force-load chunks, place blocks or claim player property. Complete
new scans replace current evidence; empty scans clear candidates while retaining
positive sighting history. Partial, stale, future, absent-world, overlapping-town
and already occupied anchors cannot authorize selection. Representative-home
coverage is intentionally bounded rather than an exhaustive village survey.

Schema 58 transactionally adds three empty workplace evidence/history tables.
Existing businesses, goods, money, identities and courses are unchanged. Legacy
economy files without workplace-observer remain disabled. Recognition does not
open businesses: real demand, qualified founders, protected funding and final
live-block revalidation are still required in the next founding adapters.

Thirteen automated tests added. Focused tests: 38 passed. Windows
.\mvnw.cmd clean verify: 1,003 passed (1,001 unit and two packaged integration),
zero failures/errors/skips, 4:46. A test initially expected the wrong exception
for coordinate overflow; corrected to match exact arithmetic, then verified.
WorkplaceMarkerProbe compiled and passed on the marked disposable Paper server:
scheduled scanner and SQL owner recognized both markers; three wheat in the test
barrel remained untouched; complete empty re-observation and restart preserved
history without recreating candidates. Both temporary test blocks were restored,
and the helper released its temporary test-only chunk ticket.

Server stopped normally; original properties restored byte-exact; helper archived.
All 96 original database tables compared: 92 unchanged; four differences were
clock/food-clock timestamps, current farm evidence and ore-trust bookkeeping.
No calendar month or food period advanced; original business assets, finances,
residents, skills, courses and history were unchanged. New main-fixture workplace
tables stayed empty under legacy opt-out. Human server untouched; no push/merge.

Phase 3 remains 25% by completed roadmap groups. Next: first warehouse/workshop
founders, actual demand and protected startup funding; then wider industries and
specialization. Physical building remains Phase 4. See WORKPLACE_MARKERS.md.

## October 11, 2026: demand-backed automatic coal/iron mine founding verified

Current trusted exposed ore can now support an automatic finite mine when actual
funded workshops need inputs and an operating staffed warehouse can buy them.
A qualified unemployed local adult becomes owner/first worker; existing business
ownership remains unlimited, while worker employment remains exclusive. Protected
real village grants pay the recorded startup expense and working capital. Existing
viable mine stock/capacity suppresses redundant founding; larger funded demand can
support another operation. No free qualification, capital, inventory or hidden ore.

Selection, source claims, empty store, job and grant save together. Final receipt
failure or immediate mutation-trust loss rolls everything back. Latest-frame,
world/freshness, range/occupied-anchor and food-protection checks remain. Seeded
bounded pages choose resource patches; accepted coordinate IDs and opening terms
survive restart. Automatic coal/iron yield is separately editable; configured
byproducts remain without creating the other unobserved mineral. No construction,
excavation or land rights. Old economy files without automatic-mines stay disabled.
Schema remains 57; no migration or rewriting of existing sources/assets.

Thirteen tests added (12 automatic founding/owner-flow cases and strict config).
Focused48 passed, then owner-flow11 passed before the final trust-loss case. Initial
fixtures reused a capture time and mismatched the persisted economy seed; corrected
fixtures, no weakening of guards. Windows .\mvnw.cmd clean verify passed 990 tests
(988 unit/two packaged integration), zero failures/errors/skips, 4:50. Initial
sandbox clean could not remove target; normal-permission rerun succeeded.

AutomaticMineFoundingProbe compiled before installation and passed on marked
pinned disposable Paper through actual PopulationSession: one mine, actual 2500
public outflow (500 expense/2000 business capital), eight finite batches/sixteen
coal; absent-world pause and restart preserve assets. Native server stopped
normally with no forced overworld chunks. Properties restored byte-exact, helper
archived and all prior database tables compared: 90/96 unchanged. Six differences
are one normal age-only month 1728-to1729 and associated clock/active-world/ore
bookkeeping. Food period stayed 29; all prior assets, finances, courses, skills and
history unchanged. Human server untouched; no push or main merge.

Phase 3 remains 25% by complete milestone groups. First warehouse/workshop founding,
first-founder career opportunities, wider industries and emergent specialization
remain. Physical entrances/digging remain Phase 4. See AUTOMATIC_MINES.md. The later approved marker observation batch is recorded above; automatic
warehouse/workshop founding remains unfinished.

## October 11, 2026: automatic real-vacancy career training verified

Existing living-owner mines, smiths and warehouses can choose unemployed local
adults for actual skill-requiring vacancies. Ordinary qualified hiring runs first.
Courses must be able to meet saved job requirements, share village student/course
budgets and cannot overbook openings. Saved per-food-period reviews/candidate pages
avoid duplicate enrollment and survive restart. Training spends real protected
practice goods in future food periods; enrollment gives no skill, job or money.
Filled/closed/unavailable-owner opportunities cancel before spending; manual
courses remain independent. No first founders, imaginary employer or salary.

Schema57 adds empty linked career review/enrollment tables without backfilling old
courses or skills. Course, saved opportunity/cursor and time accrual commit together.
Untimed status refreshes cannot enroll into previously unobserved earned time.
Strict automatic-career-study settings opt legacy files out; owner files preserved.

Fourteen tests added. Focused104 passed before the final saved-page test; final
Windows .\mvnw.cmd clean verify passed977 (975 unit/two packaged integration), zero
failures/errors/skips, 4:27. Initial tests had a wrong fixture column and assumed
queued periods drain faster than the bounded runtime; corrected test expectations.
Native helper initially used package-private constructors; fixed test-only code to
read SQL. Launch mistakenly proceeded with the older plugin after helper compile
failure; stopped that run, installed successful helper/current verified JAR, reran.
No production source changed after full verification.

CareerStudyProbe passed on marked pinned Paper with actual PopulationSession:
one selected adult, two actual practice periods, two retained skill points, one
normal hire, one saved course, four total practice units and unchanged10000 cash.
Repeated pulse, world absence and restart neither spend nor award twice. Integrity
and foreign keys healthy. Human server untouched; no push/main merge.

Disposable fixture backed up before both native launches. Current schema57 healthy;
81/94 original tables unchanged. Thirteen changes exactly one normal age-only month
1727-to1728, one food period28-to29 (zero stock/output, shortage22-to23), one ordinary
SEEK_FOOD decision and associated needs/appeal/clock/world/ore bookkeeping. All old
reports/decisions, assets, finances and identity/history retained; new career audits
empty under legacy opt-out. Both launches stopped normally with no forced chunks;
original properties restored byte-exact and successful helper archived.

Phase3 remains25% by complete milestone groups. Existing-job career response now
works automatically; safe suitable funded first industries and broader industry
mixes/long-run specialization still remain. Next: observed demand-backed mines.
See VACANCY_CAREERS.md. No new consequential decision needed for this batch.

## October 11, 2026: approved gradual commerce learning verified

Added editable commerce study and actual paid warehouse-order experience. Each
productive review awards a current living local qualified employee at most one
configured gain, regardless of order count. Ownership alone, idle/delivery-only
periods, absent worlds, old receipts and retries award nothing. Courses consume
real protected village practice materials; prototype commerce target10/timber1
remains editable. No automatic career enrollment or first merchant founding.

Schema56 transactionally adds three empty commerce audit tables linked to actual
purchase receipts and industry checkpoints. No asset/history/skill backfill or
phantom production. Failure rolls back goods, money, experience and audit together.
Existing owner files require explicit commerce settings; they are not overwritten.

Ten tests added: actual new orders, one gain for multiple orders, owner exclusion,
idle/restart/absence, disabled/unmapped/budget/over-ceiling behavior, checkpoint and
audit rollback, failed schema55 upgrade/retry, real course supplies and editable
config. Focused47 passed. Windows .\mvnw.cmd clean verify passed963 (961 unit/two
packaged integration), zero failures/errors/skips, 4:35. Final packaged JAR passed
CommerceLearningProbe on marked pinned localhost Paper using actual PopulationSession:
two actual paid orders, one employee gain, 10000 total cash, delayed goods/real
workshop output, no extra awards after idle/restart/absence. Integrity/FKs healthy.

Stopped disposable fixture backed up before schema55-to56; 79/91 original tables
unchanged. Twelve differences are exactly one ordinary age-only population month
1726-to1727, one food period27-to28 (zero stock/output; prior shortage21-to22), and
associated needs/appeal/clocks/world/ore bookkeeping. Old reports retained; business
assets, treasury, estate and identity/history preserved. New commerce audits remain
empty in the legacy fixture. Normal shutdown/no forced chunks; exact original
properties restored and native helper archived. Human server untouched. No push or
main merge. No production source change after full verification.

Phase3 remains25% (one complete group/four), not inflated by partial backend work.
Approved commerce now unblocks subsequent career/opportunity selection. Sales-only
learning, automatic first qualified industries and broader specialization remain.
See COMMERCE_LEARNING.md. No new design question needed for this batch.

## October 11, 2026: bounded funded supplier selection verified

An expected supplier-specific contribution refusal now permits the next stocked
operating supplier in the current bounded page. The first failed offer fully rolls
back before the next begins. Shared food/work/public budget/config refusals still
stop and real SQL faults propagate. Successful payment stops search. Existing
rotating cursors and supplier budgets remain; no new migration or financial rule.

Added a deterministic temporary SQLite scenario with a poor first smith and a
sales-funded second smith: same food review buys the second supplier's actual kit,
first supplier retains cash/stock, repeated pulse creates no extra charge. Focused
32 passed. Final Windows .\mvnw.cmd clean verify passed 953 (951 unit/two packaged
integration), zero failures/errors/skips, 4:22. Test-only FundedSupplierProbe on
pinned marked Paper confirmed actual PopulationSession same-review selection,
paid arrival/use, repeated pulse, restart and absent-world pause. Total cash 30000
remains 2500 village + 27500 funded supplier; poor supplier remains 0. Six actually
manufactured kits = four retained by poor supplier + one warehouse pending + one
used by farm after two food periods. Contribution 2500 saved exactly once; all
integrity/FK checks healthy. No free stock or income.

Before native run, stopped fixture schema 55 backed up; 85/91 original tables
unchanged. Six differences exactly one normal population month 1725 to 1726 (resident
age only) and its civilization/economy runtime, active-world and ore bookkeeping;
no food period, treasury, industry assets, estates or identity/history changed.
Legacy fixture contributions remain 0. Normal shutdown, no forced chunks, exact
properties restored and native test JAR archived. Human server unchanged; no push
or main merge. No source change after final full verification.

Phase 3 remains 25% by complete groups. Automatic first nonfood industries and
specialization remain. Asked the user to settle merchant/warehouse ownership
qualification: proposed editable commerce study and real transaction experience
avoids instant untrained miner-to-shopkeeper conversion. No commerce rule or
automatic merchant founding implemented pending answer. Canonical roadmap unchanged.
Progress tracker now puts the current summary and nine phase bars first, retaining
prior verified updates in a clearly historical collapsible section.

## October 11, 2026: farm equipment no longer blocks warehouse restocking

Fixed WarehouseSupplies producer discovery to exclude only an audited public farm
kit store (farming, capacity1, zero private cash, exactly one tools commodity with
unit space1, saved farm invoice). It matches existing input-demand safeguards and
filters before bounded producer slots. Unfamiliar stores still defer; no general
farming exemption, new stock, demand, grants or migration.

Added a temporary SQLite regression proving actual mined iron is paid into warehouse
cargo while a public farm kit store exists, then retaining the unknown-layout fence.
Focused 38 passed; Windows .\mvnw.cmd clean verify passed 952 (950 unit/two packaged
integration), zero failures/errors/skips, 4:27. First regression setup used duplicate
site coordinates; corrected the test sites before verification.

Final packaged JAR passed actual PopulationSession upkeep plus IndustryRuntime
upstream purchase on isolated pinned Paper. Four manufactured kits and original
30000 cash remain accounted for; warehouse paid100, producer received100, cargo
contains one real iron unit and preserves an older independent paid toolkit.
First native assertion expected only one pending space and overlooked that older
cargo; corrected the test-only assertion to two and reran successfully without
changing production code. Both retained synthetic saves pass integrity/FKs.

Marked main fixture backed up before both runs. Across the pair, 79/91 tables
unchanged; differences exactly one normal population month1724-to1725 (living age
579-to580), one ordinary food period26-to27 (zero stock/output, existing food shortage
20-to21, no other commodity use) and their needs/appeal/clock/world/ore bookkeeping.
All treasury, business/assets, estate funds, prior receipts and identity retained;
all farm/upkeep audits stay empty under legacy opt-out. Final rerun alone preserved
88/91 tables, only civilization_time/economy_runtime/ore bookkeeping changed, no
new population/food period. Normal shutdown, no forced chunks, exact settings
restored and probe archived. Human server untouched; no push/main merge.

Phase3 remains25% by complete groups. Next: automatic first qualified industries,
suitable opportunity selection and genuine upstream demand; no completion claim
for those systems. See WAREHOUSE_RESTOCKING.md.

## October 11, 2026: verified real spare-business farm upkeep contributions

Implemented the approved cash feedback as an exact invoice-shortfall transfer
from the actual operating local supplier. Original village-funded store capital
plus editable operating reserve, percentage of spare cash, sales-backed cumulative
limit and saved business/village food-period caps stay protected. No collection
when public funds suffice; no partial transfer, household wallets, free money or
net-profit claim. Contribution, public payment, real stock/cargo and both audits
share one transaction. Existing files opt out; accepted invoices replay safely
while disabled/absent. Session/Paper config uses forward food-tuning rules.

Schema 55 adds empty upkeep_contributions and budget/supplier-sales indexes;
no asset/history backfill. Eleven tests added: actual sales/invoice conservation,
reserve/quota guards, rollback, current food/world proof, replay/restart, runtime
pause, strict config, numeric bounds and failed schema54 upgrade/retry.
Focused 40 passed. Windows .\mvnw.cmd clean verify passed 951 (949 unit/two packaged
integration), zero failures/errors/skips, 4:23. Initial focused compilation needed
test constructor/signature updates; corrected before successful verification.

Final packaged JAR passed test-only UpkeepContributionsProbe on pinned marked
localhost Paper: actual PopulationSession four food periods, three invoices,
10500 total supplier contributions, two used manufactured kits, restart/absence
and repeated pulse. Four original kits = two used + one farm pending + one warehouse
pending; cash remains 30000 (2500 village + 27500 supplier). Integrity/FKs healthy.
Stopped main fixture backed up before migration54-to55: 87/90 original tables
unchanged, only civilization_time/economy_runtime/ore_tracking bookkeeping changed;
no population month, food period, assets or history changed. New audit empty under
legacy opt-out settings. Normal server exit, no forced chunks, exact properties
restored and probe archived. Human server unchanged. No push/main merge.

Phase 3 remains 25% by complete groups; automatic qualified nonfood founding,
resource opportunity selection and specialization remain. See UPKEEP_CONTRIBUTIONS.md.
Review found a related pre-existing warehouse producer-query omission: audited
public farm equipment stores can be mistaken for unsupported producers. Narrow
restocking regression fix is next; unfamiliar stores must remain explicitly fenced.

## October 11, 2026 — actual farm tool work and replacements verified

Added FarmTools/FarmToolUpkeepPolicy and food-runtime/session/Paper configuration
wiring. Actual field/employee-backed food uses editable productive grace, kit
service and fractional slower output without tools. Zero output never consumes
or wears a kit or awards work experience. Paid arrivals, actual kit consumption,
service/fraction, food, learning and checkpoint share the food transaction.
After committed food, a bounded rotating supplier page can pay an actual local
smith/warehouse from the approved protected public budget. Held/pending equipment
prevents duplicates; missing/stale food proof, staff, world or budget defers orders.
Legacy sections stay opted out and unfamiliar stores are preserved. Disabling
keeps service/history and permits already-paid arrivals; retuning is forward-only.
Schema 54 adds empty actual food-work audit; no free kit or state backfill.
See docs/FARM_TOOLS.md and FARM_TOOL_FUNDING.md.

Nine added tests (eight SQLite runtime and one strict configuration). Initial
focused 50 passed; final focused 17 and final Windows .\mvnw.cmd clean verify
passed 940 (938 unit/two packaged integration), zero failures/errors/skips, 4:28.
The initial full 939 run passed before review added stable scalar-only farm
transport fingerprints and a regression case: unrelated mining maps/settings
cannot cause a false farm retune across Java processes. Actual PopulationSession on pinned
isolated Paper passed four productive periods, three real invoices, two kit uses,
restart, world absence and repeated-pulse idempotence. Save: 16 food units,
18000 village cash + 12000 supplier cash = 30000 original funding; three manufactured
kits = two used + one paid incoming. Saved service (4 productive periods, one service
period, zero remainder); integrity/FKs healthy. First native check failed because
the test-only read connection omitted foreign-key enforcement; fixed the probe
and reran successfully. Production code was unchanged by that repair.

Stopped marked main fixture migrated 52→54. 82/88 original tables unchanged;
six differences were exactly one normal population month (1722→1723, living age
577→578) and its active-world/clock/ore bookkeeping. No food period advanced;
assets, prior history and identities retained. Both new audits empty because
old fixture configs opt out. Both synthetic saves healthy, including the retained
partial first probe. Server stopped normally, no forced chunks, exact properties
restored and test JAR archived. Final rebuilt-JAR Paper check also passed with
identical synthetic conservation. Before that final run, the stopped 54 save was
backed up: 84/90 tables unchanged, six differences exactly one additional ordinary
month (1723→1724, age 578→579) and clock/world/ore bookkeeping. No food period or
asset/history change; all three synthetic saves healthy (two complete and the
retained partial reader-failure run). Final settings restored and probe archived.
Human server/worlds/database untouched.

Phase 3 remains 25% by completed groups. This supplies a genuine recurring farm
equipment customer; automatic qualified first nonfood founders, upstream opportunity
selection, broader industries and specialization remain. No push/main merge. A consequential treasury-feedback choice is now pending:
current village funds are finite, food trades are outflows, and business sales
keep money privately. Proposed editable contributions from actual spare business
cash would protect operating reserves; no recurring income/levy is implemented
without the user's direction. See DECISIONS_AND_OPEN_QUESTIONS.md.

## October 11, 2026 — protected public farm equipment invoices verified

Implemented FarmToolPurchases and FarmToolBudgetPolicy: a real working village
farm can receive one actual local toolkit paid by village treasury, after current
food/population proof and editable reserve/period-budget checks. Exact invoice
funds leave no farm cash; supplier goods/cash, paid cargo, new empty equipment
store and budget receipt commit or roll back together. Held/incoming equipment
prevents another order. Accepted payments replay after restart/disable without
spending again. Unfamiliar old farm catalogs/cash are preserved. The nonfood
planner recognizes audited public equipment stores without ignoring other unknown
catalogs. Config is strict and missing legacy sections stay disabled.
Schema 53 adds an empty payment audit and village-period index; no asset backfill.
See docs/FARM_TOOL_FUNDING.md.

14 tests added (12 temporary-SQLite payment cases, one budget and one config case).
Initial focused 18 passed; final focused 29 passed before the final eligibility
case. Final Windows .\mvnw.cmd clean verify passed 931 (929 unit/two packaged
integration), zero failures/errors/skips, 4:51. Earlier full run passed before
review added farm-work proof, public-store compatibility and qualification guards;
the final full run includes all changes. Rollback/upgrade/restart, real recipe
output, payment/stock conservation and food ownership are covered. No Paper/server
launch or deployment for this explicit database adapter; Minecraft-facing runtime
wiring is not introduced. Human server, worlds and database remain untouched.

This is a verified funding seam, not automatic tool consumption, repeat orders,
new blacksmiths or warehouse demand. Next: actual farm work/wear and paid deliveries
under the food clock, then bounded funded replacements and first-industry planning.
Phase 3 remains 25% by complete groups; no canonical roadmap changes.

## October 11, 2026 — bounded business vacancy hiring verified

Existing live-owner mines, workshops and warehouses now fill saved vacancies
with qualified unemployed local residents. Candidate/hire budgets are editable;
current workers are never poached, skill is not invented, and each employment
and its explanation audit commit together. Death reconciliation permits a real
replacement; repeated reviews/restart cannot duplicate jobs. Legacy configuration
stays disabled. Farms retain their existing separate staffing adapter.
Schema 52 adds an empty hiring-audit table; existing jobs/assets are not backfilled.
See docs/BUSINESS_VACANCY_HIRING.md.

Nine tests added. Final focused run passed 21; Windows .\mvnw.cmd clean verify
passed 917 (915 unit, two packaged integration), zero failures/errors/skips,
6:04. Initial test compilation used a private listing API; corrected the test
before the successful final run. Disposable Paper actual PopulationSession passed
qualified hiring, death/replacement, restart and absent-world pause. Probe save:
two employment episodes/two audits, one active replacement, reserve eight,
coal two and total cash 10000; integrity/FKs healthy.
Main save schema 51→52: 78/87 old tables unchanged; the other nine differences
were exactly one ordinary food period (25→26), its stock/needs/appeal receipts,
and clock/tracking bookkeeping. All earlier history remained; population,
business assets and identities unchanged; new hiring audit empty. Fixture stopped
normally, exact properties restored, probe archived, no forced chunks or human
server changes. Phase 3 remains 25% by complete groups. Next: approved public
farm-tool upkeep, then opportunity training/automatic first industries.

## October 11, 2026 — shared warehouse startup grants verified

Approved warehouses now have an explicit transactional startup adapter sharing
mine/workshop food protection and period/lifetime caps. Real treasury funds become
recorded startup expense and empty-store purchasing capital; a qualified first
worker is required. Existing configs keep their saved industry lists. New defaults
include warehouse without changing any numerical cap. No automatic firms/buildings
or new migration; schema remains 51. See STARTUP_GRANTS.md.

Six added tests; focused 27 before the final config case, full Windows Wrapper
clean verify passed 908 (906 unit/two integration), zero failures/errors/skips,
6:12. Packaged Paper grant probe passed shared-budget refusal, empty warehouse,
real funding, qualification and restart/disabled replay. Own DB has two grants,
9000 cash + 1000 recorded startup expenses = 10000 original funding; integrity/FKs
healthy. Main 84/87 tables unchanged; only clocks/ore bookkeeping differed, no
population month or asset/history changes. Fixture stopped normally, exact
properties restored, test JAR archived, no forced chunks. Human server untouched.
Next: bounded vacancy hiring at existing live-owner businesses, then opportunity
training/automatic nonfood founding. Phase 3 remains 25% by complete groups.

## October 11, 2026 — business tool upkeep verified

Existing timed mines/workshops buy real replacement tools with business money;
tools wear only during positive production. Editable grace, wear, slowdown and
price are in economy.yml. Warehouses can buy configured workshops' actual output.
Maintenance and recipe demand share funds/storage; depleted/untrusted mines create
no tool pressure. Service/fractions, use receipts and production commit together.
Legacy files stay opted out; retained unsupported store catalogs are preserved.
Schema 51 adds two empty service/audit tables without free kits or asset backfill.

16 tests added; final focused 23 and Windows Wrapper clean verify passed 902
(900 unit/two packaged integration), zero failures/errors/skips, 6:13. Actual
PopulationSession on isolated Paper passed upkeep and prior warehouse checks:
two real tool uses, reserve four, cash 10000 conserved, saved service/fractions,
restart and absent-world pause. SQLite integrity/FKs healthy. Main 79/85 old
tables unchanged; other differences verified as exactly one normal population
month plus clock/tracking bookkeeping. All assets/history preserved. Fixture
stopped, exact settings restored, probe archived; no forced chunks/probe world
writes. Human server untouched. See BUSINESS_TOOLS.md and DEVLOG.md.

Phase 3 remains 1/4 complete groups (25%): automatic nonfood founding/careers,
wider industries and specialization remain. Approved warehouse startup grants
under existing shared food-protected limits are next.

## October 11, 2026 — upstream warehouse restocking verified

Existing staffed warehouses now buy actual extracted local mine goods for funded
workshop input gaps. Real business cash/stock transfer, delayed delivery, shared
hauling and village-wide pending coverage connect mine → warehouse → blacksmith
without manual stocking. Qualified staff, bounded complete reviews and storage
are required. No new firms, money, markup or finished-goods buyers are invented;
missing old warehouse-supply settings stay disabled. Schema remains 50.
See WAREHOUSE_RESTOCKING.md. Business tool upkeep was approved during this batch
and is the next implementation, not a feature of this build.

14 tests added; focused 38 and Windows `.\mvnw.cmd clean verify` passed all 886
(884 unit/two packaged integration), zero failures/errors/skips, 6:13. Packaged
Paper actual PopulationSession chain/replay/world-pause/restart/disable passed:
four paid shipments, one tool, finite reserve eight, total cash 10000 conserved.
Both SQLite saves healthy. Only main clock/ore-tracking bookkeeping changed;
all main businesses, goods, finances, resources, residents and history unchanged.
Fixture stopped normally, exact settings restored, probe archived, no forced
chunks. Human server untouched. Automatic nonfood founding, actual finished-tool
needs, wider industries and specialization still prevent the next Phase 3 gate.

## October 11, 2026 — funded industry input review verified

Existing workshops now have a read-only input-demand review using actual qualified
staff, business money, storage, owned/pending goods, configured recipes and editable
input buffers/base prices. Available warehouse goods cover village gaps once across
customers. No purchase, reservation, production, new consumer or money is generated.
The SQL-owner bridge matches only uncovered mineral demand to current valid evidence;
partial/disabled/absent-world reviews defer. No new automatic scheduler or setting
default; schema remains 50. This is candidate pressure, not startup authorization or
finished-goods demand. Suitable sites, career/founder choice, existing producer/
transport alternatives and automatic investment still need connecting.

15 added tests; focused 21 before the final selection case, full Windows
`.\mvnw.cmd clean verify` passed 872 (870 unit/two packaged integration), zero
failures/errors/skips, 5:43. Guarded packaged Paper paid-stock/pending-cargo/money/
actual-owner-reopen/world-pause probe passed. Reopened synthetic DB integrity/FKs
healthy, conserved total 10000 cash, finite reserve eight, two paid pending shipments
with three goods, no tools/grants. Not natural startup/client/customer proof.

Stopped disposable fixture backed up; main remains schema 50. 74/86 tables unchanged;
remaining changes verified as exactly one normal month/food period with prior history
retained. Business assets, financial, skill, family and identity data unchanged.
Normal stop, exact settings restored, probe archived, no forced chunks reported.
Human server untouched; local feature branch only, no push or main merge. See
INDUSTRY_DEMAND.md and root DEVLOG.md for scope and corrected test/setup assumptions.
Phase 3 remains 1/4 complete groups (25%); actual customer needs and automatic careers/
funded opportunity selection remain next.

## October 11, 2026 — current mineral surveys verified

Schema 50 distinguishes the latest complete coal/iron survey from retained positive
sighting history. Existing observer saves both atomically. Newer empty/smaller frames
replace current membership without erasing history; older scans cannot restore it.
Same capture replay is immutable, conflicting raw coordinates roll back. Legacy
sightings receive no fabricated current frame. No business/resource/cash rewrite.

The SQL-owner review now gates candidate coordinates on known world availability,
healthy mutation tracking, caller-supplied freshness, nonfuture capture, exclusions
and global deposit claims. It cannot authorize a mine, infer hidden ore or establish
real demand/site permission/qualifications. Automatic founding remains unfinished.

13 new tests; focused 42 and Windows `.\mvnw.cmd clean verify` passed 857 (855 unit,
two packaged integration), zero failures/errors/skips, 6:02. Isolated packaged Paper
owner/world-pause/expiry/empty-frame/older-replay/history/restart check passed; reopened
probe retained capture 2000, no current members and one historical sighting, no money
or businesses. Main migration integrity/FKs healthy, 81/84 old tables unchanged;
only normal time/runtime/tracking updates. All existing business, resident, resource,
financial and identity history preserved. Fixture stopped; exact properties restored,
probe archived, no forced chunks reported. Human server untouched, no push/main merge.
See root DEVLOG.md for setup corrections and MINERAL_OPPORTUNITIES.md for contracts.

Phase 3 remains 1/4 complete groups (25%). Next: actual demand, qualified careers,
coherent suitable resource patches and funded automatic industry selection.

**Verified against local source, Git, Wrapper and disposable Paper on October 10, 2026.** Phase 1 prototype gate passed (10/10 milestones). Phase 2 prototype gate passed (4/4 reporting milestones). Implemented features include live field-backed food, household allocations, saved priorities and appeals, verified donations, once-only opening funds, budget-backed emerald food purchases, saved emerald bargaining, finite-stock player barter and resident-linked food/housing welfare. Bounded food-priority learning and saved feasible donation/emerald/material/wait response comparisons are verified, with actual confirmed trade receipts and exact food outcomes. Phase 3 now has automatic funded field enterprises, persistent owners/sites, qualified staffing and worker-backed food; its first reporting group is verified (1/4, 25%). Existing funded mines/workshops now run timed worker-backed production. Workshops can automatically purchase actual local warehouse inputs. Automatic nonfood founding, upstream warehouse supply, finished-goods customers, wider competition and specialization remain unfinished. Qualified succession, paid resale, retained unsold closure, actual mine/workshop/farm employee work learning, supply-backed study and automatic funded resale are verified. Phases 4–9 remain planned. A human client verified recognition, donations, emerald sales and resident restart persistence; broader inventory-plugin compatibility, barter/bargaining client coverage and survival balancing remain validation limits. This is not a balanced release.




## October 10–11, 2026 — guarded village startup grants

Approved public startup funding now has editable settings and atomic mine/workshop
startup composition. Schema 49 retains one receipt/business with actual expense/cash
allocation, protected food money, food evidence, before/after treasury and policy.
Current unmet/missing/stale food assessment or changed living population defers
startup. Single/current-food-period/lifetime budgets survive restart; accepted terms
replay after disable without new money, quota, stock or ore refill. Failed startup
or receipt insertion rolls back the whole operation; no retroactive grants.

Defaults: mining/blacksmith requests,50 Trade Value single/current village food
period and 200 lifetime, keeping max(25 Trade Value, two assessed food-demand periods
estimated at 2 Trade Value/unit). Caps limit public spending, not private ownership.
Old files remain disabled until opted in. No automatic caller, public command,
private-startup funding, loan/payroll or world modification added. Caller still
establishes actual demand/current suitable evidence. Automatic career/founding next.

Sixteen added tests; focused 83 and Windows `.\mvnw.cmd clean verify` passed 844
(842 unit/two packaged integration), zero failures/errors/skips,6:12. Guarded
packaged Paper defaults/refusal/conserved startup/mining/disabled restart replay
passed in a separate synthetic DB, with healthy reopened integrity/FKs and original
receipt/cash/job/stock/reserve. It is not natural ore/automatic founding/client proof.
Test-only compilation issues corrected before successful runs; application source
unchanged after full build. See root DEVLOG.md for exact failures/corrections.

Stopped marked dev schema 48 backed up; upgrade 49 healthy and 76/82 old tables
unchanged. Only normal age/clock/runtime/world/tracking elsewhere; existing assets,
food/trade/financial/identity/skill/opportunity history preserved, main grant table
empty. No world edits/tickets, no forced chunks verified, normal stop exit 0, exact
settings restored and probe archived. Human server/world/DB untouched. Local feature
branch only; no push/main merge. Phase 3 remains 1/4 groups(25%). See STARTUP_GRANTS.md.

## October 10, 2026 — durable mineral opportunities

The live coal/iron survey now retains positive evidence across restart in schema48:
settlement/world UUID, coordinate, mineral and first/latest capture timestamps.
Repeat and older evidence do not duplicate observations; incomplete/empty scans
do not erase history. Validation and saving are atomic. Current saved deposit links
and placement/removal exclusions explain historical evidence at read time, without
refilling reserves or granting a mine/site. A bounded immutable SQL-owner diagnostic
page supports the next opportunity-selection work; no new player command yet.

Nine added tests cover temporary SQLite rollback/replay/restart/exclusions/gaps,
bounded diagnostics, migration failure/retry and accepted owner work during shutdown.
The first full build exposed a readiness/shutdown race in the existing survey seam;
fixed accepted work to drain using owner initialization/healthy tracking, while new
submissions still reject and gaps still pause. Focused 33 before the final owner test,
then corrected focused 29. Final Windows `.\mvnw.cmd clean verify` passed 828
(826 unit/two packaged integration), zero failures/errors/skips, 5:38.

Guarded packaged Paper scheduled snapshot/exclusion/SQL-owner persistence passed.
Reopened probe DB retained exactly one expected coal coordinate/capture time and
no business/deposit; integrity/FKs healthy. Fixture blocks restored, temporary
console chunk tickets removed and verified absent, normal stop exit0. Main dev
schema47 backup upgraded to48 with 67/81 old tables identical; normal age/time/world,
one food period and observation updates explain the rest. Business goods/cash,
mineral reserves, family/skills, trade receipts and treasury stayed unchanged.
Main opportunity table empty; synthetic evidence remains in a separate probe DB.
Settings restored exactly and probe archived; human server/world/DB untouched.

Limited configurable village startup grants with food-budget protection explicitly
approved and recorded, not yet implemented. Next: grant eligibility/accounting,
funded career choice and automatic mine/workshop selection with actual demand.
Phase 3 remains 1/4 complete groups (25%). Local feature-branch work only; no push
or main merge. See MINERAL_OPPORTUNITIES.md and root DEVLOG.md for full verification.

## October 10, 2026 — native ore-source removal and production trust protection

The registered Paper tracker now records successful ore breaks, destructive
block/entity explosions and actual piston origins. Cancelled events, non-ore
blocks and keep/trigger explosions do not retire sources. Immutable coordinates
cross the bounded SQL-owner queue; placements and source losses from one event
commit together. Piston destination exclusions never drain neighboring sources.
The plugin changes no drops, cancels no world action, loads no chunks and performs
no physical excavation. Capture/save/queue failure pauses new discovery and
coordinate-backed mining immediately and durably; healthy close cannot repair a
gap. Direct mining checks saved trust, and accepted historic receipts still replay.
Paused periods explain themselves without creating goods or a later windfall.

Schema 47 fences older saved coordinate reserves because placement-only tracking
cannot certify their continued supporting blocks. It preserves goods, money,
identity and receipts; an empty save has no automatic coverage gap. Explicit
reviewed baseline/recovery tooling remains unfinished. No trust reset, database
reset or human-server upgrade was performed. Other village/workshop simulation
continues; unsupported commands/editors/direct block writes remain outside coverage.

Thirteen tests added across event adapters, temporary SQLite and actual owner
queues: cancellation/destructive lists, piston origins/destinations, combined
rollback/replay, immediate/persisted gaps, shutdown drain, coverage migration and
failure/retry. Focused 86 passed before the final three tests; Windows
`.\mvnw.cmd clean verify` passed 819 (817 unit/two packaged integration), zero
failures/errors/skips, 5:36. Guarded packaged Paper synthetic native events on real
loaded blocks passed reserve 93 -> cancelled break 93 -> break 70 -> piston 47 -> block
explosion 24 -> immediate/restart gap 24 -> entity explosion 0; coal 7/stone 14 stayed
unchanged. All four source retirements totaled 93, with no repeated production.
Prior packaged farm/industry learning, study, resale, succession and closure passed.
This verifies actual registered event/owner wiring, not a human client or physical
piston/explosion engine. Probe blocks were restored exactly. A console-only
chunk ticket in the disposable fixture was removed and absence verified before
normal stop (exit0). Production code and probe itself add no chunk tickets.

Backed up the stopped marked schema 46 fixture first. Schema 47 integrity/FKs healthy;
72 of 81 old table snapshots unchanged. Differences were normal ages/clocks/world
progress, ore/farm observation timestamps and deliberate test mutation evidence.
The first snapshot check omitted the normal farm observation timestamp refresh;
read-only comparison confirmed no crop/capacity changes and corrected the check.
Existing identities, assets and history stayed fixed. Exact original settings
restored and both test JARs archived. Human server/world/database untouched.

Source/application/listener, tests, guarded probe and docs updated on the local
feature branch; no push/main merge. Phase 3 remains one of four complete reporting
groups (25%): safe native source loss finishes a resource prerequisite, not the
whole connected production/specialization gate. Next: durable suitable resource
opportunities, funded career choice and automatic first mines/workshops. No
blocking gameplay question. See LIVE_ORE_REMOVAL.md and MINERAL_SOURCE_LEDGER.md.

## October 10, 2026 — approved per-block mineral reserve ledger

Schema 46 records even allocation of legacy remaining reserves across saved ore
coordinates without changing total reserve, goods, money or historical receipts.
Actual mining drains positive shares evenly; explicit removal retires only the
removed coordinate's remaining share. Replacement/replay cannot refill it. Later
linked evidence gets zero extra reserve; coordinate-free legacy deposits keep
aggregate accounting without invented evidence. Bounded batch retirement and
mining update reserve, source balances and audits transactionally. A later failed
coordinate rolls back the whole batch. Source inconsistency fails closed.

Added 12 temporary-SQLite tests and two domain tests (500 seeded comparisons plus
huge/zero/unequal balances). Initial focused 42 passed; after batch optimization and
three added rollback/world/tiny-reserve cases, Windows `.\mvnw.cmd clean verify`
passed 806 (804 unit/two packaged integration), zero failures/errors/skips, 5:40.
Guarded packaged Paper probe passed actual reserve 100 -> extraction 7 -> reserve 93
-> one-source loss 23 -> reserve 70 -> restart/replay -> extraction 10 -> reserve 60,
with coal 17/stone 34 conserved. Prior farm/industry learning, study, resale,
succession and closure probes passed. No world edits or forced chunks.

Backed up the stopped marked disposable fixture before schema 45 -> 46. Normal stop
exit0; integrity/FKs healthy, conversion totals conserved. Of 76 existing table
snapshots, 70 unchanged; resident identities/status stayed fixed, with only bounded
age ticks plus normal clocks/world-progress/ore-tracking timestamps changed.
The first read-only check assumed a nonexistent schema-history table; corrected to
actual PRAGMA user_version. The first age check incorrectly expected all residents
(including deceased) to age; corrected the check, without application changes.
Original server settings restored exactly and probe archived outside plugins.
Human server/database/world untouched. Local feature branch only; no push/main merge.

This is accounting only: live break/explosion/piston source retirement and trust
fencing still require integration. First automatic mines/workshops and physical
excavation remain unfinished. Phase 3 remains 1/4 complete reporting groups (25%).
Next: bounded native removal capture on the main thread, SQL-owner recording and
failure/unclean-stop protection before enabling safe automatic mine opportunities.
No unresolved design choice for this batch. See MINERAL_SOURCE_LEDGER.md.

## October 10, 2026 — farm employees learn through actual food production

Automatic field-enterprise employees now gain configurable farming experience in
new productive food periods. Food, skills, before/after participant audit, needs/
decisions and checkpoint commit together; a later failure rolls all of them back.
Owners without jobs and legacy adult-only field labor do not learn. Saved jobs in
excess of reduced observed field slots rotate through only the productive slots.
Zero output, closed farms, incomplete observations, absent worlds and downtime
award nothing. Existing history is not backfilled; existing skills are never reduced.

The existing work-learning section gains a farming mapping in new defaults, with
one point per productive food period and ceiling 100. Old owner files opt in without
being rewritten. Skill growth does not increase output, create money, add jobs or
waive founding thresholds. A tested working employee can become qualified for
succession through actual work. Schema45 adds review/participant tables tied to
saved food periods, preserving existing farms, food history, money and skills.
Application wiring passes the same approved policy to industry and food runtimes;
farm diagnostics explain latest awards, zero production or budget deferral.

Nine temporary-DB tests added. First focused run caught three fixture errors:
wrong requirement/need table names and a zero-crop observation claiming a crop
position. Corrected fixtures only; focused 90 passed. Full Windows Wrapper command
`.\mvnw.cmd clean verify` passed 792 (790 unit/two packaged integration), zero
failures/errors/skips, 4:26. Guarded Paper PopulationSession checks passed actual
output8, once-only employee awards, restart/absence, reduced-slot rotation and
zero-output pause, with unchanged startup spending; prior resale/inheritance/
industry learning/study/closure probes passed. Initial standalone probe compile
referenced a package-private repository constructor; replaced only the harness
read with SQL and compiled against current classes, without widening the API.

The isolated fixture stopped normally (exit0); schema 45 integrity/FKs healthy,
all 20 preexisting asset/skill/course/audit snapshots unchanged. Original network
settings restored byte-for-byte and test probe archived. Human server/database/
world unchanged. Source/tests/config and documentation updated on the local
feature branch; no push/main merge. See FARM_WORK_LEARNING.md and WORK_LEARNING.md.

Phase 3 remains 1/4 complete groups (25%). Farm learning finishes another skills
component; opportunity-based career choice, initial nonfood founding, broader
labor/capital competition and seeded acceptance remain before another group gate.
Next: connect safe, evidence-backed opportunities to training and real funded
industry founding; per-coordinate supporting ore loss is an approved prerequisite.
No blocking design question for the completed farm batch.


## October 10, 2026 — automatic qualified, funded business resale

Present-world business maintenance now creates offers after qualified family and
employee succession fail. Seeded bounded buyer pages retain a restart-safe cursor;
actual eligible estate/business funds finance purchases under editable limits.
The user approved unlimited private ownership, with one worker job per resident;
government exceptions remain Phase 6. Prices use editable saved startup recovery,
actual cash/stock and paid cargo; accepted terms never reset. Unpriced goods defer
valuation. Listing/payment/estate/current owner/review commit together. Expired
unsold firms retain closure/assets/history; absent worlds and downtime pause sales.
Additive schema 44 saves lifecycle diagnostics/cursors and a local portfolio index.
Existing saves and immutable history are preserved. Old owner settings opt in.

Added ten temporary-SQLite resale cases, one policy case and two config cases:
atomic rollback/retry, qualification priority, real estate/cash funding, paid cargo,
retained terms, paged search, unlimited ownership, world/clock guards, retained
closure and schema 43 failure/retry. Focus 96 passed. Windows `.\mvnw.cmd clean verify`
passed 783 (781 unit/two packaged integration), zero failures/errors/skips, 6:29.
Actual Paper PopulationSession check passed four automatic purchases, five firms
owned by one resident, exact money conservation, absent-world and restart safety;
prior inheritance/production/learning/study/closure checks also passed. Two initial
probe assertions needed correction: enable FK checks on its read-only connection,
and count store funding as a treasury debit, not new money. Only the test harness
changed after Wrapper verification and was recompiled against the packaged JAR.

Stopped the marked isolated fixture normally; integrity/FKs healthy at schema 44,
all 16 preexisting saved asset/skill/audit tables unchanged, original network settings
restored byte-for-byte, probe archived. First console stdin closed unexpectedly;
a target-guarded temporary helper scheduled normal Bukkit shutdown, without killing
a process. Subsequent runs used a persistent console. Human server/world/database
unchanged. Local feature branch only; no push/main merge. See AUTOMATIC_BUSINESS_RESALE.md.

Phase 3 remains 1/4 complete groups (25%): this completes automatic turnover work
inside the lifecycle group, not its broader skills/investment/competition gate.
Next: farm skill learning and opportunity-backed career/founding integration;
nonfood supply chains, geography selection and seeded long-run acceptance remain.
No blocking design question; private ownership/government approval is recorded.


## October 10, 2026 — saved supply-backed adult study

Approved unemployed-adult training now has persistent internal enrollment and
execution in real village food periods. Only living local unemployed adults without
an operating owned firm qualify; hiring, movement or death cancels later reviews
without deleting earned skills/history. Genuine nonfood practice supplies are
consumed as recorded event demand. Food/material buffers, rotating student slots,
current ceilings and retained course terms prevent free skills or double spending.
Already earned backlog, downtime, absent worlds and incomplete observations cannot
create study. Legacy field output counting all adults defers study to avoid double
labor. Village diagnostics explain waiting/disabled/budget/supply conditions.

Defaults: one experience point, mining/smithing target10, stone1+timber1 per success,
two students/period, eight active courses/village, one future food-demand period and
two material units retained. All editable in economy.yml; old files opt in. No
personal wallets, salaries, invented teachers, cash charges or industry founding.
Transactional additive schema 43 retains accepted courses, material terms, actual
period reviews and spending linked to economy history; existing saves preserved.

Eleven temporary-DB study cases, one policy case and two config cases added. Focus
110 passed before final diagnostics; initial full run found its old-schema test
assertion incorrectly querying new diagnostic tables during deliberate downgrade.
Corrected only the test stock read and added a budget diagnostic assertion. Final
Windows `.\mvnw.cmd clean verify` passed770 (768 unit/two packaged integration),
zero failures/errors/skips,4:42. Native Paper passed hungry pause, actual two stone/
two timber consumed, 0→1→2 skill and retained completion through absence/restart,
plus prior employee learning/inheritance/resale/closure checks. Integrity/FKs healthy.
Normal stop exit0, original network properties restored exactly, probe archived;
all 16 preexisting dev asset/skill/audit snapshots unchanged and no main-dev courses
invented. Human server/database/world unchanged; local feature branch only.

Internal backend does not yet select careers or automatically start mines/workshops.
Connect suitable observed opportunities, funded founding and upstream/finished-goods
demand next; farm work learning, automatic valuation/buyers and wider competition
remain. Phase 3 stays1/4 groups (25%). See RESIDENT_STUDY.md. No new design answer needed.


## October 10, 2026 — learning from actual productive work

Employees of existing timed mines/workshops now gain configurable experience only
from actual successful production. Output, finite depletion, skills, immutable
participant audits and checkpoints commit together. Replay, restart, absent worlds,
zero output and closed businesses cannot create extra experience. Owners without
jobs do not learn; qualifying later does not waive ownership requirements.
Prototype defaults: one point per productive period, ceiling 100, worker budget 64,
mining/smithing mappings. Old settings files opt in; existing skills are never reduced.
Schema 42 adds reviews/participant entries without rewriting old skills or receipts.

Focus 79 passed; Windows `.\mvnw.cmd clean verify` passed 756 tests (754 unit,
two packaged integration), zero failures/errors/skips. Isolated Paper passed eight
once-only awards, mining skills 10→15 and 5→8, qualified inheritance, conserved
resale, absence/restart and retained closure. Initial probe compilation attempted a
package-private constructor; corrected only the harness read query, preserving the
application API. Server stopped normally; original network settings restored exactly,
probe archived, all 14 saved development asset/skill snapshots unchanged; SQLite
integrity/FKs healthy. Human server/world/database unchanged. No main merge/push.

Unemployed-adult supply-backed study, farm learning, automatic nonfood founding and
automatic sale valuation/buyers remain unfinished. Phase 3 stays 1/4 reporting groups
(25%); this verified component advances work inside an unfinished group.


## October 9, 2026 — qualified business succession

Latest follow-on: unsold expiry now records retained closure and releases all open
jobs atomically after qualified inheritance review. Expired firms stop new work
before their bounded closure visit. New mining/recipes/hiring/trade/capital are
blocked; paid cargo and exact historic receipts remain safe. Farms cannot silently
restart under their stable identity. Diagnostics retain the reason/date and no
longer falsely say succession is unimplemented. Schema 41 is additive. Nine new
closure cases plus one farm regression; final focus 43 passed, broader pre-final
focus 73 passed; Windows Wrapper clean verify passed 744 (742 unit/two packaged
integration), zero failures/errors/skips. Native Paper passed staffed work until
expiry, ended jobs, retained coal3/reserve2 and stopped output across restart;
other mine coal5/reserve5, inherited owner and paid replay retained. Funds stay
8500 treasury/750 cash/250 estate. First harness read connection omitted required
foreign keys; corrected only the harness and reran. Both runs stopped normally,
settings restored, probes archived; old dev assets retained, integrity/FKs healthy.
User-server unchanged. Automatic valuation/listing creation/buyer selection and
wider competition remain. Phase 3 stays 25%. See BUSINESS_CLOSURE.md.

Next qualification work: user approved gradual resource-backed training for
unemployed adults and learning through actual employee work, with editable costs,
speed and limits. Ownership thresholds remain required. No training is implemented
by this closure batch; automatic nonfood founding remains unfinished.

Latest follow-on: schema 40 saves explicit sale offers with immutable price/deadline,
online/present-world elapsed time, resolved ownership and atomic SOLD completion.
Normal refusal no longer falsely reports a population save failure. Failed world
boundaries suspend new sales until reviewed restart; funds/history remain unchanged.
Focus 55 passed; Windows Wrapper clean verify passed 734 (732 unit/two packaged
integration), zero failures/errors/skips. Native Paper passed exact saved 250-price
sale, inheritance/staffed finite work, replay, absence/restart and conserved
8500 treasury/750 business cash/250 estate. Normal stop, restored settings, archived
probe; schema-39 dev assets retained and schema-40 integrity/FKs healthy. User-server
unchanged. Automatic offer creation/valuation/buyers and retained closure remain;
EXPIRED records a deadline, not closure. Phase 3 stays 25%. See BUSINESS_SALE_LISTINGS.md.

Latest follow-on: explicitly approved qualified-existing-staff operation after
owner death is configurable across timed mines/workshops, warehouse supply and
automatic farms. No staff/owner/estate money is invented. Farms reconcile lost labor,
retain remaining staff and use current ownership for later staffing after succession.
Schema stays 39. Wrapper clean verify passed 725 (723 unit/two packaged integration);
focus 91 passed. Native Paper verifies a deceased-owner mine producing three coal
with a worker qualified for work but not ownership, no promotion, finite reserve
five -> two, plus inherited mine/sale/replay/world pause/restart and conserved funds.
Normal stop, probe archived, settings restored; dev assets retained, integrity/FKs
healthy. User's own server unchanged. See BUSINESS_OPERATIONS.md. Automatic listings,
valuation/deadlines/closure/competition still unfinished; Phase 3 remains 25%.

Latest follow-on: paid resale accounting and separate family estate funds are
verified. Qualified buyers use real owned-business spare cash and/or eligible
prior-sale estate proceeds under editable limits. Inheritance takes priority.
Payment, estate credit, audit and owner change are atomic; retry does not pay twice.
Internal session API rejects new absent-world/future-date sales. Schema 39 retains
prior assets. Wrapper clean verify passed 715 (713 unit/two packaged integration);
focus 81 passed. Paper owner fixture passed a 250 cash-to-estate purchase, replay,
pause/restart and 3 coal/7 reserve, retaining treasury 8500 and cash/estate total 1000.
Marked dev assets retained; normal stop, settings restored, probe archived. No own
server/world changes. Automatic listings/valuation/deadlines/closure/competition and
estate-backed new founding remain unfinished. See BUSINESS_RESALE.md. Phase 3 25%.

Business founding remains immutable and separate from current ownership. Present
worlds' bounded business refresh now transfers a deceased owner's firm to an
eligible industry-qualified recorded family member first, then a qualified current
employee. Alive/moved owners are not treated as deceased. Age, skills, settlement
and current industry employment are checked. Multiple eligible relatives use stable
UUID ordering; unrelated housemates are not family. No successor leaves ownership
unchanged pending resale. No invented training, employment, grants or money transfer.

Schema 38 transactionally adds ownership audit events/index. Owner update and audit
commit together; failure rolls both back. Original founding replay charges nothing
again after transfer. Current diagnostics/industry operations read the current owner.
Six new temporary SQLite cases cover family priority, employee fallback,
qualification/occupation refusal, rollback/retry, absence/runtime integration and
37->38 failed migration/recovery. Focused 53 passed; full Windows Wrapper clean
verify passed 698 (696 unit/two packaged integration), zero failures/errors/skips.

Actual Paper owner fixture passed qualified inheritance, exactly one event, three
coal/seven remaining bundles, world pause and restart. Initial native assertion
connection omitted foreign-key enforcement and was corrected; application behavior
was not relaxed. Both runs stopped normally; successful probe archived, original
network properties restored. Fresh fixture integrity/FKs healthy, treasury 8500 and
business capital 1000 retained. Marked dev industry/funds/identity matched pre-upgrade
backup; its existing resident/calendar advanced normally while online. No probe block
writes/tickets, and no changes to the user's own server. See BUSINESS_SUCCESSION.md.

Automatic resale, closure/competition, per-block resource retirement and
automatic wider industries remain upcoming. Buyer estate/business spare-cash sources
with configurable limits are approved. Phase 3 remains 1/4 complete groups (25%).

## October 9, 2026 — human playtest and trade responsiveness

Human playtest passed village recognition (seven imported residents), a two-bread
donation worth six food units, and a two-bread sale for three emeralds with correct
inventory changes. Normal restart retained six living residents; the log recorded
one prior native zombie death. Old unbound legacy development settlement data
initially blocked enrollment; with the server stopped and user approval, its database
was archived and fingerprint-verified, preserving world and settings. Existing
populated saves are not generally disposable. Economy speed temporarily changed
60 -> 10 seconds for testing and restored to 60 while stopped, with config backups.

The observed roughly 15-second trade wait matches five-second polling across
multiple asynchronous proof/ledger stages. Purchase, barter and donation adapters
now poll every five ticks (250 ms at 20 TPS), retaining four-player work caps,
main-thread inventory handling and verified-save/SQL recovery checks. This reduces
scheduler-induced waits; disk/SQL load and player count still affect actual latency.
Focused transfer verification passed 29 cases, including a new real scheduled-run
path case proving one payout, debit, delivery and confirmation under repeated polls.
Windows Wrapper clean verify passed 692 tests (690 unit/two packaged integration),
zero failures/errors/skips. Updated JAR installed while the user's dev server was
stopped; previous JAR backed up, installation hash verified, database/settings
fingerprints unchanged. User-client confirmation timing on this build remains pending.
Schema remains 37; this fix changes no saved transfer format or database schema.

Qualified family -> qualified employee -> affordable qualified sale -> closure is
approved, with editable valuation/deadline and a separate persistent family estate
fund. Suitable permitted vacant premises take priority over new construction;
physical reuse is Phase 4. Player-mined ore retires only the affected coordinate's
source, not the full vein. These rules are recorded but not yet implemented.
Next: immutable founder/current-owner separation, tested succession/resale accounting
and per-coordinate reserve retirement. Phase 3 remains 1/4 reporting groups (25%).

## October 9, 2026 — transport clock retuning safeguard

Branch `codex/phase3-haul-clock-retuning`. A reproduced bug made an old shipment
falsely exhaust today's transport after industry speed changed: window numbers
alone alias different actual times. New automatic shipment terms and quota queries
include cadence plus window. Shared limits still bind buyers under the same rate;
immutable replay refuses changed terms. Manual quota remains separate. Retuning
starts a new rate namespace without changing paid cargo's ownership, prices or
arrival clock. Schema 37 transactionally adds bounded nullable haul_cadence and
an index; old automatic rows keep unknown cadence, not guessed current settings.

Three new temporary SQLite cases cover the actual regression, quota/replay and
failed 36->37 migration with later legacy arrival. Corrected focus 10 passed;
final Windows Wrapper clean verify passed 691 (689 unit/two packaged integration),
zero failures/errors/skips. An extra fixture initially shared period zero with its
setup's real shipments; corrected to an independent period, preserving application
quota behavior. Actual Paper owner supply/restart/world-pause/cadence checks passed.
A copied known schema-36 paid-cargo fixture upgraded with all original table/column
data retained; original fingerprint unchanged. Main dev 36->37 retained industry,
funds/settings/provenance; all integrity/FKs healthy. No world writes or tickets
in this probe. Normal stop 0, dev properties restored, probe archived. Final tested
JAR installed in the stopped player server; settings and save retained.

Phase 3 remains 1/4 complete groups. Current build supports player testing of
families, farms, food aid/trade and diagnostics. Existing funded nonfood firms have
timed work and local input purchasing; natural nonfood founding, upstream supply,
other industries, lifecycle and specialization remain. Pending ore-removal and
ownership choices precede their corresponding mine/lifecycle work. No main merge
or push. See TONIGHT_PLAYTEST.md and WORKSHOP_SUPPLIES.md.

## October 9, 2026 — nearby resource observation

Branch `codex/phase3-nearby-resource-surveys`. Bounded center-first loaded-chunk
surveys now extend around saved village homes: new radius one (nine targets),
editable 0–2, at most 25 targets/town and 1024 towns. Old enabled sections missing
the new key retain one home chunk; absent sections stay disabled. No chunk loading,
extraction or automatic founding. Immutable evidence retains original capture
time, not traversal completion time. Last completed explanations retain town,
chunk and timestamp alongside ongoing work. Native testing exposed results being
overwritten immediately by the next target; corrected and verified their retention.

Seven new geometry/config cases; focused 67 passed. Final functional Windows
Wrapper clean verify passed 688 (686 unit/two packaged integration), zero failures,
errors or skips. Clarified YAML comments repackaged afterward with values/code
unchanged. Actual Paper fixture passed nearby ore beyond an empty home chunk,
exclusion filtering, capture time after clock advance and retained results.
Four temporary blocks restored/checked; two console-only fixture tickets removed.
Separate synthetic DB: two residents, one exclusion, no businesses, clean/trusted
fence. Main industry/settings/provenance retained; integrity/FKs healthy. Schema
remains 36. Test used 25578, then original dev properties restored after normal
stop 0; probe archived. Updated only the stopped player-server JAR, not its save
or settings. Source guide now explains starting/joining the local test.

Phase 3 remains 1/4 complete groups. Ore removal and succession decisions are
pending before their corresponding automatic mine/lifecycle work. Wider survey
coverage is an observation component, not completion of connected production.

## October 9, 2026 — paid automatic workshop inputs

Branch `codex/phase3-workshop-supply-planning`. Existing staffed/funded workshops
now buy complete affordable recipe bundles from actual same-village warehouse
stock, pay once, reserve cargo space, wait for delivery and consume owned inputs.
No invented supplier, income, finished-goods customer or automatic founding.
New-file supply defaults: enabled, two periods of inputs, 16 warehouses reviewed;
prices reuse editable base Trade Value. Old files without industry-supply remain
disabled. Already paid automatic cargo still arrives when new purchasing is off.
Legacy cargo counts for ordering but retains its original delivery caller/clock.
Transport uses bounded shared windows rather than unrelated buyer period numbers.

Schema 36 binds automatic shipments to the actual buyer's saved clock, preserving
existing funds, stock, reserves, owners, history and legacy cargo. Payments,
arrivals, recipe work and checkpoint share rollback/retry boundaries. Fifteen new
cases: seven planner, seven temporary SQLite, one config. Focused 84 passed;
Windows Wrapper clean verify passed 681 (679 unit/two packaged integration), zero
failures/errors/skips. Guarded actual PopulationSession Paper probe passed four
paid cargo records, two tool units, conserved 9000 treasury/1000 business funds,
world pause and restart; unloaded synthetic sites, separate DB, no world writes.
Native main 35->36 retained industry/settings and clean/trusted provenance; only
the normal clean-stop timestamp refreshed. Integrity/FKs passed, normal stop 0,
probe archived. Updated the stopped separate player server's plugin JAR only;
its existing optional supply settings still require opt-in if absent.

Phase 3 remains 1/4 complete reporting groups (25%). Automatic nonfood founding,
upstream warehouse supply, renewable/primary industries, finished-goods demand,
lifecycle/succession and specialization remain. Ownership answer remains pending;
it blocks succession only. See WORKSHOP_SUPPLIES.md and PHASE3_TODO.md.

## October 9, 2026 — timed existing industry operation

Branch `codex/phase3-timed-industry-runtime`. Existing funded/staffed mines and
workshops now execute bounded online work through PopulationSession's SQL owner.
New-file defaults: 30 real seconds/period, eight firms/owner pulse; old files without
industry-runtime remain disabled. Saved clocks retain fractions/earned queues,
world absence and downtime add no production, unloaded sites need no chunks. Firms
begin above legacy production history, without pre-enrollment backfill. Finite
reserves, byproducts, actual workers, delivered inputs and storage bound output.
Owner absence pauses without assuming inheritance. No automatic founding, buyers,
new money, blocks, worker entities, forced chunks or regional routes.

Schema 35 adds constrained industry clocks/history/tuning transactionally; existing
identities, stock, funds, reserves and provenance remain. Work/input/output/depletion,
receipt, history and checkpoint commit together. Final-save failure retries intact.
Cadence tuning requires an empty queue and records the discarded fraction. Bounded
round-robin production and a 4096-row clock cap; no large-server performance claim.
Production errors remain diagnosed/retryable without blocking food/population. A
failed world boundary suspends industry until corrected/restarted while population
still receives the new world mask; uncertain elapsed time is not invented.

Eleven temporary SQLite runtime/application cases and two config/policy cases added.
Focused final 37 passed; final Windows Wrapper clean verify passed 666 (664 unit,
two packaged integration), zero failures/errors/skips. Actual Paper accelerated
PopulationSession probe passed: three coal, seven remaining batches, three history
checkpoints, preserved 8500 treasury/1000 working capital, world absence and restart.
Site chunk was explicitly unloaded; no world writes/tickets. Main schema 34->35
retained industry/funds/history and unchanged owner settings, with zero automatic
clock enrollments and clean/trusted provenance. Integrity/FKs passed. Startup month
1685 retained; online 1686. Normal stop exited 0 and probe archived.

Fresh separate localhost player server prepared outside OneDrive using verified
Paper 26.2 build 129, Java 25 and existing accepted EULA. Only cached runtime assets
copied, no existing worlds/databases. Fresh defaults/schema 35 startup and normal
stop passed with zero people/towns/businesses, no force-loading/probes and healthy
provenance. Verified plugin installed; stopped, with a double-click starter. Player
checklist in TONIGHT_PLAYTEST.md; human client validation still pending.

TIMED_INDUSTRY, playtest/development notes, status/progress/checklist/devlog updated.
Phase 3 remains 1/4 reporting groups (25%). Automatic nonfood opportunities/funding,
local purchasing/customers, renewable/stone/sand coverage, lifecycle and specialization
remain. Inheritance recipient choice remains pending. Local feature commit only;
main unchanged, no merge/push.

## October 9, 2026 — unsupported bulk-editor safeguard

Branch `codex/phase3-editor-provenance-guard`. Installed WorldEdit/FAWE/VoxelSniper
names and later enable events conservatively pause new ore discovery. Disabled
installed editors also require reviewed recovery; removal does not clear gaps.
Existing villages/deposits/history and editors themselves remain. Optional strict
ore-provenance text settings add descriptor names, without removing built-in guards
or rewriting owner files. Unknown/renamed editors, offline edits and hot reload are
not certified; this is presence handling rather than mutation integration.

Four new adapter/config cases; Windows Wrapper clean verify passed 653 (651 unit,
two packaged integration), zero failures/errors/skips. Native test-plugin enable
fenced its separate temporary owner; clean reopen retained untrusted history. Empty
probe DB and main DB integrity/FKs passed; main industry/funds/exclusions unchanged,
main clean/trusted fence and owner economy hash retained. No world edits/tickets.
Startup month 1684 retained; online 1685. Normal stop exited 0; probe archived.
An initial fixture deployment used an incorrect relative artifact path and was
stopped normally; corrected absolute deployment supplied the passing native result.
No production DB/world changes, new schema, real editor integration or human test.

ORE_EDITOR_GUARD, development guide, tracking notes, checklist/status/progress/devlog
updated. Phase 3 remains 1/4 (25%): safe nonfood opportunities/operation, renewable
industry coverage, real customers, lifecycle and specialization remain. Succession
needs the user's recipient choice before ownership implementation. Local feature
commit only; no main merge/push.

## October 9, 2026 — atomic funded mine startup

Branch `codex/phase3-atomic-mine-startup`. MiningStartupRepository now joins
business/site founding, finite coordinate discovery, empty storage, actual working
capital and one qualified first employee in one owner transaction. Failure rolls
back every component and debit; replay preserves depleted reserves and owned stock.
A changed first employee is rejected. Existing public operations keep their idle
transaction contracts; package-local row helpers require an active transaction.
No automatic mine policy, physical mining or new schema is enabled (schema 34).

Nine temporary SQLite cases cover success/restart, injected storage/hiring failure,
combined affordability, invalid evidence, transaction contracts, storage/qualification,
changed first worker and occupied primary jobs. Windows Wrapper clean verify passed
649 (647 unit/two packaged integration), zero failures/errors/skips. The packaged
Paper paid-chain fixture passed atomic startup/replay, depletion, delivery, recipes
and restart with capital conserved minus its explicit startup expenditure. Six
fixture blocks restored; normal shutdown exited 0, no force-loaded chunks remained,
probe archived. Main industry/funds/history retained; integrity/FKs, clean/trusted
fence and unchanged owner economy file checked. Startup month 1677 retained; online
1684. No human playtest claim or production world/database changes.

ATOMIC_MINE_STARTUP, Phase 3 checklist, status/progress/devlog updated. Phase 3
remains 1/4 reporting groups (25%): this makes safe startup possible but does not
complete automatic nonfood operation. Next: mutation compatibility safeguards and
actual suitable, demand-backed opportunities. Ownership succession remains pending.
Local feature commit only; no main merge/push.

## Live read-only resource survey pipeline — October 9, 2026

- Branch `codex/phase3-live-resource-surveys`: registered main-thread bounded
  snapshot traversal at the canonical saved-home chunk. One scan/future at a time,
  max 1024 queued towns and 512 combined positive coordinates. Text settings in new
  economy.yml; missing old sections remain disabled with no owner-file rewrite.
  Unknown/partial/unloaded traversal is deferred, not treated as resource loss.
- SQL-owner bridge requires healthy capture, present/matching world and complete
  consistent evidence, filters current sticky exclusions and resolves saved claims.
  Conflicting deposits/minerals are rejected; separated patches may retain one
  depleted identity. Only provisional results/diagnostics, no persisted opportunities,
  funding, businesses, stocks, mining or new schema. Automatic mines remain disabled.
- Nine new reader/config/session cases. Windows Wrapper clean verify passed 640
  (638 unit/two packaged integration), zero failures/errors/skips. Guarded scheduled
  Paper pipeline passed with four ore/air blocks restored and synthetic saved homes
  in a separate DB: two residents, one exclusion, zero businesses, clean/trusted fence.
  Main startup 1676 retained; online 1677. Normal stop exited 0; test loading ticket
  removed and probe archived. Read-only old industry/funds/exclusions, integrity/FKs,
  clean/trusted fences and unchanged owner settings passed. No human playtest claim.
- LIVE_RESOURCE_SURVEYS/config/TODO/status/progress/devlog updated. Phase 3 stays
  1/4 (25%). Next: actual opportunity suitability, atomic funded founding and timed
  worker-backed nonfood chains; bulk-editor provenance remains separate. Ownership
  succession choice still pending. Local feature commit only; no main merge/push.

## Bounded observed resource patches — October 9, 2026

- Branch `codex/phase3-resource-grouping`: complete bounded observations split into
  face-connected positive evidence, deterministic ordering/anchors, copied metadata
  and immutable output. Distant/diagonal/unobserved gaps remain separate; no hidden
  coordinates are invented. At most 512 coordinates/six neighbor probes each;
  integer limits cannot wrap. Chunk borders do not impose geological borders.
- Six domain tests plus one temporary SQLite scenario: independent patches conserve
  initial reserves; depletion, reopen/retuning and duplicate-site rejection retain
  identities without refill. Focused 22 cases passed; full Windows Wrapper clean
  verify passed 631 (629 unit/two packaged integration), zero failures/errors/skips.
  No new world adapter, production
  behavior or migration; schema 34, no server DB/config changed by this batch.
- Grouping is a provisional evidence utility, not final geology or mine founding.
  Resolve saved identities first: two provisional patches may share an old deposit.
  Live assembly/safety, automatic funded nonfood chains and lifecycle still pending.
  RESOURCE_PATCHES/scanning/decisions/TODO/status/progress/devlog updated. Phase 3
  remains 1/4 (25%); local feature commit only, no main merge/push.

## Conservative piston resource provenance — October 9, 2026

- Branch `codex/phase3-piston-provenance`: successful extension/retraction saves
  moved ore sources and both possible adjacent axis destinations. Immutable bounded
  evidence only; no destination block reads, forced chunks, event cancellation,
  reserve refill or changed extraction terms. Cancelled/non-ore events ignored.
  Existing queue/failure fence reused; malformed multi-place capture also fenced.
- Five new adapter cases; Windows `.\mvnw.cmd clean verify -B -ntp` passed 624
  (622 unit/two packaged integration), zero failures/errors/skips. Schema stays 34.
  Native synthetic place/extend/retract dispatch saved seven fixture exclusions,
  restored every ore/air block, and retained paid-chain/restart accounting checks.
  Six new piston exclusions persisted. Read-only old industry/funds/exclusions,
  clean/trusted fence, integrity/FKs and unchanged economy settings passed.
- First launch omitted an interactive console; exact disposable JVM termination
  exercised a genuine dirty restart. Discovery paused, including after normal stop.
  Known empty test interval was reviewed against backup; untrusted state backed up
  and only marked-dev trust rearmed. No automatic recovery feature added. Corrected
  probe and probe-free restarts stopped normally; no loading tickets, month 1675
  retained at startup (online reached 1676). Probe archived, server stopped.
- Actual piston physics/bulk-editor compatibility remain unverified. Automatic
  resource opportunities/founding/production remain unfinished; Phase 3 stays 25%.
  Next: coherent observation grouping, then safe observation and funded operation.
  Local feature commit only; no merge/push, main or production changes.

## Live native ore placement capture and recovery fence — October 9, 2026

- Branch `codex/phase3-live-ore-tracking`: registered MONITOR listener captures
  successful native ore placements/multi-place positions, ignoring cancelled,
  forbidden and non-ore events. Includes named ore variants/ancient debris for
  future provenance, without enabling their production catalog. Immutable UUID/XYZ
  records only; no player inventory/identity or Paper object crosses the owner queue.
  Bounded pending saves, synchronous/async error logging and immediate gap state;
  no main-thread SQLite, player placement cancellation, world edits or forced chunks.
- Schema 34 adds explicit singleton clean-stop/trust fence. Live start commits
  dirty before capture; shutdown drains accepted saves before a clean fence. Crash,
  save/capture failure and early-start gap cannot silently regain trust on restart.
  Persisted gaps block coordinate discovery; immediate session readiness must gate
  future live observers. Existing populations, finite deposits and histories remain.
  `/civ debug status` explains healthy capture or reviewed-recovery pause. No recovery
  reset/adoption command implemented; migration invents no old placement history.
- Fourteen added cases (five fence repository, five adapter, three owner-session,
  one discovery). Windows `.\mvnw.cmd clean verify -B -ntp` passed **619** (617 unit/
  two packaged integration), zero failures/errors/skips. Final run includes the review
  fix retaining an early failure explanation and fencing world-observation failures.
  Tests cover overload, cancelled/multi-place data, clean/unclean restart, rollback,
  schema-33 recovery, shutdown drain and preservation of people/deposits.
- Consistent marked-dev schema-33 backup preceded additive upgrade. Positive native
  fixture dispatched a synthetic event through the actual registered listener and
  verified its queued saved exclusion after restoring all six blocks. Dummy player
  is not an online client, no inventory/player data changed. Separate probe DB also
  passed paid chain, saved rejection/replay/restart and five claims/20 initial/16
  remaining/two tools. Main placement row represents this actual fixture event.
- Test loading ticket removed and probe archived. Probe-free restart retained month
  1670 and healthy capture; status showed no failure, console confirmed no forced
  chunks, both normal stops exited zero. Read-only schema-34 clean/trusted fence,
  exactly one new fixture placement, old identities/industry/funds, probe accounting,
  integrity/FKs and unchanged owner economy.yml hash passed. Probe excluded from
  production JAR; marked dev server stopped. No real player acceptance claim.
- Updated LIVE_ORE_TRACKING/provenance/scanning/mineral/development guides, TODO,
  status/progress/devlog. Piston/bulk-editor provenance, coherent opportunities and
  funded automatic nonfood operation remain next; automatic mines still disabled.
  Phase 3 stays 1/4 (25%) until a complete gameplay group passes. Succession preference
  remains pending separately; no answer assumed. Local feature commits only, no
  merge/push, main or production save changes. Physical development remains Phase 4.

## Resource provenance persistence and industry direction — October 9, 2026

- Branch `codex/phase3-resource-provenance`: recorded user-approved common industry
  direction: suitable sites, paid bounded exploration, timed non-1:1 output,
  resistance/yield/value separation, finite stone/sand industries, useful demand-based
  reserves, retained closed sites/history and nearby real employees. MASTER_DESIGN,
  SIMULATION_RULES and decisions clarify planned behavior without changing ROADMAP.
  PHASE3_TODO.md separates verified components from missing live gameplay gates.
- Schema 33 adds permanent-world/XYZ player_ore_placements. Bounded transactional
  batches are sticky and idempotent; first timestamps survive replay/restart/rename.
  Immutable chunk reads fail on budget overflow instead of admitting missing
  exclusions. PopulationSession queues copied evidence on its SQL owner even before
  a town exists; accepted writes drain at normal shutdown. No player identity stored.
- Coordinate discovery checks exclusions inside its transaction, rejecting stale
  or bypassed surveys entirely without partial reserves, altered claims or erased
  history. Legacy site-only API is still trusted, not suitable for automatic scans.
- Eight new cases (five temporary repository, two discovery, one owner-session),
  plus stronger uncertain-neighbor coverage. Windows `.\mvnw.cmd clean verify -B
  -ntp` passed **605** (603 unit/two packaged integration), zero failures/errors/skips.
  Initial focused compilation used a nonexistent snapshot accessor in a new test;
  corrected to the existing settlementNames API. Final full run followed the public
  read-only PaperOreSource adapter change for the separate native fixture.
- Consistent marked-dev schema-32 backup preceded schema-33 startup. Initial native
  probe correctly refused when no chunks were loaded; test-only probe now waits for
  a console-loaded fixture chunk. Positive real snapshot scan/exclusion passed after
  six original blocks were restored and checked. Its separate SQL-owner DB passed
  sticky placement/rejection/restart, unchanged five claims/20 initial/16 remaining
  reserves, paid chain and two tools. No production listener/automatic mine enabled.
- Test loading ticket removed, probe archived outside plugins, server stopped with
  exit zero. Main diagnostics showed no failure and offline restart retained month
  1667; online progress reached 1668. Read-only old identities/industry/funds retained,
  no main placements invented, integrity/FKs valid and owner config hash unchanged.
  Read-only tool audit corrected arbitrary-first-store selection to total stock.
  The synthetic fixture is not natural discovery or a human-client gameplay check.
- Updated RESOURCE_PROVENANCE/scanning/mineral/development guides, TODO, status,
  progress and devlog. Phase 3 remains 1/4 (25%). Next: crash-safe live placement
  capture, coherent resource opportunities, automatic funded staffing/production.
  Ownership succession preference was asked separately; no answer assumed. Physical
  works remain Phase 4; no merge/push, main or production save changes.

## Approved exposed ore and bounded scanner — October 9, 2026

- Branch `codex/phase3-exposed-ore-scan`: user approved exposed existing ore as
  finite prototype evidence, excluding newly tracked player placements. Unknown
  old origin is accepted; ownership, block excavation and refill remain prohibited.
- Plain-Java OreScan visits bounded cells (at most seven source reads each), accepts
  negative coordinates/heights, copies exclusion positions and detects six-face
  known-air exposure. Buried ore is ignored. Unknown/uncertain or over-512 evidence
  is incomplete, preventing truncated discovery. Results are immutable and stable
  across batch sizes; publication before traversal completes fails explicitly.
- PaperOreSource reads snapshot block types with absolute Y; normal/deepslate coal
  and iron, all air variants, fluids and unsupported minerals are handled explicitly.
  Unavailable coordinates never read the snapshot. No world writes/forced loading.
  Initial focused run caught Material.isAir requiring live Paper registries; explicit
  air classification removed that dependency before final verification.
- Eleven new tests (nine domain, two snapshot adapter). Windows
  `.\mvnw.cmd clean verify -B -ntp` passed **597** (595 unit/two packaged integration),
  zero failures/errors/skips. Diff reviewed; no schema change (still 32), real DB,
  world, server or owner configuration modifications. No live Paper ore scan was
  run this batch; component tests do not certify natural origin/live discovery.
- Updated decision/mineral/scanning guides, progress/status/devlog. Runtime observer,
  durable player-placement tracking, coherent deposit grouping and automatic mine
  founding/operation remain unfinished. Automatic mining stays disabled until those
  protections are connected. Phase 3 remains 1/4 (25%), advancing a partial group.
  Next: permanent placement exclusions and bounded live opportunity observation.
  Local feature commit only; no merge/push or main modification. No user decision
  currently blocks that scoped work.

## Coordinate-backed finite mineral identity — October 9, 2026

- Branch `codex/phase3-mineral-identity`: bounded canonical coal/iron observations
  match deposits by permanent world/ore coordinates. Alternate sites cannot claim
  the same evidence through this path. Multiple deposits report conflict without
  merge/transfer/new reserves. Linked surveys add sticky identity evidence without
  changing initial/remaining reserves or yields. Incomplete, disjoint, predating or
  changed-mineral evidence fails explicitly; world renames preserve identity.
- Schema 32 adds discovery metadata and unique coordinates transactionally with
  deposit/yields. Failed extensions preserve old state. Legacy deposits keep their
  history without invented coordinates and require future explicit adoption. The
  old trusted API retains site-only protection; automatic observers must use the new
  path. Internal discoverRows extraction preserves stored term encoding/replay.
- Fifteen added cases (12 temporary-SQLite, three domain). Windows Wrapper
  `.\mvnw.cmd clean verify -B -ntp` passed **586** (584 unit/two integration),
  zero failures/errors/skips. Coverage includes depletion/restart/retuning, linked
  surveys, competing sites/multiple pools, world independence/rename, invalid/legacy
  evidence, timestamp/mineral identity, transaction rollback and schema-31 recovery.
- Consistent marked-dev schema-31 backup preceded schema-32 startup. Separate native
  Paper probe used synthetic ore coordinates: four positions fixed 20 bundles,
  extraction left 16, linked fifth position and new rate did not refill reserves.
  Paid workshop/replay/restart passed. No world/entity changes. Main startup retained
  month 1662 after downtime; status/no failure and normal stop exited zero. Read-only
  old IDs/industry/funds and both DB integrity/FKs/counts passed; owner config hash
  unchanged, no main-DB coordinates invented. Probe archived, server stopped, test
  code excluded from production JAR. This does not prove actual ore snapshots.
- Updated MINERAL_IDENTITY.md, decisions/mining/development notes, status/progress/
  devlog. No automatic ore scan, natural-origin certification or mine production
  enabled here. Ore origin was pending at this milestone, subsequently approved above.
  Phase 3 remains 1/4 (25%), with discovery identity advancing a partial group.
  No production mutation, merge/push or main change.

## Connected workshop recipe accounting — October 9, 2026

- Branch `codex/phase3-workshop-production`: trusted SQL-owner processing consumes
  delivered business inputs and adds complete-batch outputs, bounded by actual
  workers, site/request limits, scarce inputs and net storage growth. Pending cargo
  remains unavailable and reserves space. Ownership supplies no free labor; dead/
  moved employees cease contributing. No money or village opening goods are minted.
- Text-editable recipes catalog starts with smith_tools: two iron + one coal ->
  one abstract tools unit, two batches/worker, site cap eight. Strict validation
  rejects incomplete/unsafe settings before startup. Parsing does not activate a
  scheduler or establish a workshop. Business stores must explicitly support every
  ingredient/product; old owner files are unchanged. Tools are not player item trades.
- Schema 31 adds versioned recipe_runs/recipe_flows. Inputs, outputs, exact saved
  request/workforce/recipe flows, receipt and shared business-period production
  marker commit together. Zero-work results, retries, changed-term rejection and
  rollback protect labor/material accounting. Historical recipes survive retuning.
  Cached admin business diagnostics now show the latest saved industrial explanation,
  capped at 768 characters, and no longer incorrectly claim automatic farms are absent.
- Eighteen new cases: five domain (including 10000 deterministic seeded plans),
  ten temporary-SQLite repository scenarios and three config cases. Paid 240-cycle
  mine -> warehouse -> blacksmith -> explicitly established test maintenance demand
  conserves capital, finite deposit, stone and tool flows across midpoint restart.
  Initial focused failures were test fixtures: an obsolete empty-cache assertion
  and using ordinary update for a resident transfer; corrected to current diagnostic
  and the existing explicit migration API. Final Windows `.\mvnw.cmd clean verify
  -B -ntp` passed **571** (569 unit/two packaged integration), no failures/errors/skips.
- Consistent marked-dev schema-30 backup preceded native schema-31 startup. Separate
  test-only Paper probe used explicit synthetic firms/sites/deposit and a dedicated
  SQL owner: paid deliveries, four iron/two coal -> two tools, capital conservation,
  duplicate/restart safety, dead-worker exclusion and production diagnostics passed.
  No world blocks/entities changed. Main plugin retained month 1661 after downtime,
  then advanced normally online with no diagnostic failure; normal stop exited zero.
  Read-only identities/industry/funds and integrity/FKs passed; no main-DB recipe
  runs were fabricated, owner economy.yml hash unchanged. Probe archived outside
  plugins and excluded from shipped JAR; test server stopped.
- WORKSHOP_PRODUCTION.md documents backend API, admin recipe text and limitations.
  This advances the connected-industry group but does not complete automatic
  nonfood recognition/founding/operation, live NPC demand, competition, closure/
  succession or emergent specialization. Phase 3 remains 1/4 (25%). Next: reliable
  nonfood resource/workplace adapters and causal operation. No new design question,
  production mutation, push/merge or main change.

## Automatic field enterprises — October 9, 2026

- Branch `codex/phase3-automatic-farms`: complete actual Paper crop evidence can
  establish one persistent village-wide field enterprise, select an available adult
  founder, pay actual startup funds once and hire eligible residents. Stable site,
  business and employment episode identities preserve history and retry safety.
  No crop ownership, block harvesting, construction or invented money is introduced.
- Optional editable farm-businesses defaults: enabled for new files, four workers,
  2500 hundredths (25 Trade Value), at least adulthood/216 months, zero beginner
  farming experience. Existing owner files remain unchanged and disabled until
  explicitly opted in. Forward tuning retains old history and existing firm terms.
- Assigned workers replace legacy all-adult farm labor; output is constrained by
  actual observed field slots and settings. Other open jobs exclude candidates.
  Dead/moved workers retain ended history and can be replaced. Smaller fields lower
  production without silently dismissing workers. Absent worlds/server downtime pause.
- Schema 30 adds nullable site coordinates transactionally. Legacy counts have no
  fabricated site and require a fresh complete survey for founding. Founding/site/
  capital share a transaction; hiring and food periods have their own transactions.
  Failed food periods retain valid founding/jobs and retry without double charges.
- Ten temporary-SQLite farm scenarios and two configuration cases added, including
  240-period conservation, restart, partial/missing evidence, absent worlds, funding/
  skills, worker replacement, injected failures and migration rollback. Two older
  migration tests now inspect compatible records directly while deliberately holding
  an old schema. Final Windows `.\mvnw.cmd clean verify -B -ntp` passed **553** cases
  (551 unit/two packaged integration), zero failures/errors/skips.
- Marked disposable schema-29 DB backed up consistently before schema-30 deployment.
  Separate native Paper probe used real 32-crop snapshots and two test villagers:
  one farm/two workers, output eight, treasury 10000 -> 7500 once; absent-world
  periods/stocks stayed frozen. Main plugin diagnostics reported no failure and
  retained month 1660 after downtime. Normal shutdown exited zero. Read-only checks
  preserved prior IDs/history/funds, passed integrity/FKs in both databases, and
  verified the saved actual crop site. Owner economy.yml hash was unchanged.
  Probe was archived outside plugins; test server is stopped. Test-only chunk
  tickets/entities were released; only the marked disposable crop fixture remains.
- AUTOMATIC_FARMS.md documents settings, scope and limitations. First Phase 3
  reporting group is complete for the farm prototype; the full canonical industry
  gate is not passed. Nonfood industry, skills progression, business revenue,
  expansion, closure/succession, chains and specialization remain next scoped work.
  No user decision blocks the next milestone; no merge/push or main change.

## Live business staffing bridge and diagnostics — October 9, 2026

- Branch `codex/phase3-business-runtime`: existing businesses now receive owner-thread
  invalid-job reconciliation after population pulses. Dead/moved residents' episodes
  end transactionally with observed separation reasons, preserving employment history
  and releasing assignments. Absent worlds skip staffing writes; return resumes them.
  Owner death is reported, without inventing succession or closure.
- Stable UUID rotation visits eight businesses per pulse, retains at most 1024 cached
  details, and wraps so late firms are visited. `/civ debug businesses <town UUID>`
  shows cached founder/owner, local living workers/capacity/vacancies, open assignments,
  site/founding month, world availability and latest separation. Admin-only commands
  read immutable views and show at most 20 businesses; they do not access SQL/worlds.
  Cached worker/owner details may lag until the firm's next turn. Firm/result counts
  are bounded; individual SQL latency/scan cost has no measured performance guarantee.
- Seven added cases cover active vs paused reconciliation, owner death, immutable
  snapshots, bounded rotation/eviction, rollback/retry, session startup/world return,
  empty/nested operation and command permission/startup/ID safety. Final Windows
  `.\mvnw.cmd clean verify -B -ntp` passed 541 (539 unit/two packaged integration),
  zero failures/errors/skips. No schema migration; current schema remains 29.
- Backed up the verified disposable schema-29 DB before actual pinned Paper startup.
  Readiness retained month 1659 after downtime; new empty-business and malformed-ID
  command paths passed; normal status/no failure and clean stop exited zero. Final
  read-only schema/integrity/FK/previous identity and exact treasury checks passed.
  No businesses auto-created. Positive staffing/diagnostics use temporary SQLite
  fixtures; this native smoke is not automatic founding, industry or client gameplay.
- Added BUSINESS_RUNTIME.md; updated command declaration, foundation notes, progress
  and devlog. Existing food/labor/stock/funds behavior remains unchanged. Automatic
  site recognition, founding/hiring and production integration are still next work,
  including preventing double-counted food/labor. Phase 3 remains partial (0/4 full
  gameplay groups); nine-phase roadmap unchanged. No design answer needed, no push/merge.

## Approved server/world pause policy — October 9, 2026

- Branch `codex/online-world-pause`: live Paper earns no time during server downtime
  or for absent saved world UUIDs. Loaded worlds without players and unloaded chunks
  still simulate. Population eligibility excludes paused towns from age/death/birth/
  migration/arrival; live food/needs clocks and period execution pause too.
- Schema 29 adds an audited online adoption and saved compressed world masks.
  Accrual/masks commit atomically; pending replay retains eligibility over restart.
  First adoption records and retires unverifiable uncompleted legacy demographic
  backlog; completed history/ages/identities are not reset. Subsequent earned work
  survives. Already earned economy queues wait for their world, not new downtime.
- Twelve new temporary-DB/domain/Paper-adapter cases. Final Windows
  `.\mvnw.cmd clean verify -B -ntp` passed 534 tests (532 unit/two packaged
  integration), zero failures/errors/skips. Initial focused assertion incorrectly
  expected already earned economic queue not to finish after return; clarified the
  scenario, explicitly drained earned work, and added separate queue-pause coverage.
- Marked disposable schema-28 DB backed up consistently before Paper upgrade to 29.
  First native startup retained completed month 1658 despite the stopped interval;
  normal online activity advanced to 1659 and diagnostics reported no failure.
  Read-only integrity/FKs and previous settlement/resident/household IDs passed.
- Second native restart after more than a minute stopped stayed at month 1659,
  with zero queued months/no failure, then exited normally. Final read-only checks
  retained old population/native-event/finance/family history and exact treasury
  balances; integrity ok/no FK violations. Absence/load/unload transitions are
  automated domain/SQLite/Paper-adapter tests, not a native multi-world playtest.
- Shared calendar remains administrative chronology, not each paused world's age.
  Population partial-month boundaries are shared; per-world demographic fractional
  calendars remain unimplemented. Existing owner YAML is not rewritten; legacy
  offline caps remain only for explicit headless harnesses. See ONLINE_WORLD_PAUSE.md.
- Nine-phase roadmap unchanged. Phase 3 business/production groups remain partial
  with 0/4 gameplay groups complete. Next: reliable site/resource evidence and
  automatic founding/staffing/production using eligible economic periods. Human
  client/compatibility/balancing still pending. No design answer needed for this batch.

## Finite mining and local warehouse accounting — October 9, 2026

- Branch `codex/phase3-deposits-storage`: finite site/deposit identity retains
  discovery terms and a geological ore/rock bundle. Explicit actual mining workers,
  one plan per business/period, site/work/storage limits constrain extraction.
  Byproducts share the same budget; depleted deposits do not regrow, restart-refill
  or reinitialize under changed IDs/terms. Actual world discovery remains unwired.
- Business-owned stores start empty, with finite weighted capacity and retained
  catalogs. Explicit working capital transfers actual village funds once; no new
  grants/salaries/personal wallets. Warehouse purchases transfer real capital and
  stock, reserve buyer storage and retain buyer-owned in-transit goods. Same-town
  haul limits/delivery delay apply. Arrival is once-only; pending cargo cannot resell.
  Regional routes and physical buildings remain their original later phases.
- Explicit causal consumption primitive supports household/industry/maintenance
  input use with source/reason and protected retries. It is not automatic NPC shopping
  or invented demand to empty stock. Current live field food/player transactions
  remain unchanged; transfers to village reserves must be integrated conservatively.
- Commented optional `industry` YAML provides complete editable abundance, labor,
  site/storage/haul/delay and coal/rock bundle inputs. Default discovery: observed
  ore * 150 finite bundles; three workers at eight each yield up to 24 coal + 72
  stone, bounded by actual conditions. Admins may add rock keys/space costs. No
  renewable/infinite mineral setting. Old owner configs remain untouched; settings
  validate now but do not enable automatic business production. Stored openings
  preserve original terms; capacity expansion requires a future explicit transition.
- Transactional schema 28 appends mineral/store/receipt/production/shipment tables
  and indexes without resetting earlier history. Tests cover depletion, bundles,
  storage/reservations, paid resale profits/total money, consumption, retry/rollback,
  impossible/stale actions, store funding, migration failure and independent snapshot
  cargo recovery. A 240-period chain conserves every coal/rock unit through
  mine→warehouse→shop→explicit input use; 10000 seeded calculations enforce limits.
- Sixteen added cases; final Windows `.\mvnw.cmd clean verify -B -ntp` passed 522
  (520 unit/two packaged integration), zero failures/errors/skips. Initial focused
  compile found a test-only reused lambda variable, corrected before successful
  focused 27-case verification and final complete run. All automated DB writes
  target test-owned temporary files; no production database/world mutation.
- Backed up marked disposable schema-27 dev DB before pinned Paper startup at 28.
  Normal owner/observation diagnostics reported no failure, then normal shutdown
  exited zero. Read-only integrity/FKs, checked old identities/history and empty
  new industry tables passed. Existing server economy.yml hash stayed identical.
  Baseline OSHI/native-access/manifest warnings remain. No automatic industry or
  native mine/warehouse gameplay is claimed by this startup-only smoke test.
- Approved finite minerals/byproducts/storage/warehouse purchase direction recorded
  in master/simulation rules and decisions; nine-phase ROADMAP unchanged. Business
  and production reporting groups remain partial (0/4 completed gameplay groups).
  Next: reliable sites, bounded owner views, automatic founding/staffing and causal
  production/demand integration. Wider shop product eligibility remains future work.
  World memory uses plugin DB + permanent world IDs. At this mining snapshot the
  pause choice was pending; the subsequent approved pause batch supersedes it.
  Local commit only, no merge/push. See FINITE_MINING_AND_WAREHOUSING.md.

## Phase 3 business and staffing foundation — October 9, 2026

- Branch `codex/phase3-business-foundation`: immutable business/site/founding and
  employment records retain real founder/initial owner/resident/site UUIDs. Explicit
  trusted-owner APIs qualify actual saved age/skills, require permanent same-world
  sites, enforce finite capacity and one current primary job per person. Ownership
  alone is not employment; site registration grants no autonomous block permission.
- Startup investment is an explicit actual treasury debit in the registration
  transaction; absent/insufficient funds or failed ledger writes leave no partial
  business/site/requirements/spend. Exact identity retries never charge/hire twice
  or resurrect old jobs. Caller inputs choose requirements/cost; no automatic policy,
  additional opening grants, salaries, personal wallets or production implemented.
- Retained job separations permit rehiring. Current workers exclude dead/emigrated
  people immediately; explicit owner reconciliation ends stale episodes without
  deleting their history or inventing an exact historical departure month. Automatic
  runtime integration, closure and succession remain next/later scoped work.
- Additive transactional schema 27 adds business sites, businesses, qualification
  requirements and employment, plus indexes. No existing history rewrite, seed/reset,
  production change or owner-config edit. Older binaries reject 27; downgrade needs
  the consistent pre-upgrade snapshot rather than dropping business history.
- Twelve added cases test full restart/snapshot preservation, qualifications,
  capital/rollback, duplicate site/job rejection, capacity, separation/rehiring,
  death/migration and failed reconciliation, nested calls, schema-26 failure recovery
  and a seeded 240-month staffing turnover fixture. Final Windows
  `.\mvnw.cmd clean verify -B -ntp` passed 506 cases (504 unit/two packaged integration),
  zero failures/errors/skips. These are staffing scenarios, not simulated industry
  turnover/production or a scale-performance bound.
- Marked disposable pinned Paper was backed up at schema 26, upgraded to 27 and
  restarted successfully. Status/observation diagnostics reported no plugin failure;
  final stop saved worlds and exited zero. Read-only integrity/FKs, retained IDs and
  checked family/skill/economy/choice/learning/treasury histories passed; new business
  tables remain empty as intended. An initial launch omitted a persistent console
  and closed its stdin, requiring termination of the verified disposable process;
  subsequent console restart and clean shutdown passed. No production server/world
  was touched. Baseline OSHI Windows-counter warning remains unrelated.
- Phase 3's first gameplay milestone is partial; it remains 0/4 completed groups,
  with concrete backend progress described in PROGRESS.md. Next: owner/cache business
  diagnostics, reliable existing sites and admin-editable automatic opportunity and
  staffing rules, then resource-supported output without double-counting field food.
  No new consequential answer is needed for this batch. Source roadmap unchanged;
  local feature-branch commit only, no merge/push. See BUSINESS_FOUNDATION.md.

## Phase 2 prototype gate passed — October 9, 2026

- Branch `codex/phase2-response-gate`: villages now save a seeded weighted preference
  among feasible donation requests, emerald offers, real timber/stone barter or wait.
  Actual treasury, stock floors, idle/non-shortage evidence, supported stack value,
  pending/reserved food, selected priority, settings and catch-up constrain options.
  No imaginary supplies, automatic player debit or promise of a supplier.
- Strict commented `responses` settings expose weights/eligibility and enable/disable;
  new files enable the prototype, old owner files remain unchanged/disabled. Players
  see the preference in supplies menus and may choose other available options.
  Existing confirmations/cooldowns/permissions/saved inventory proof recheck reality.
- Schema 26 appends parent/candidate comparison history tied to food periods.
  Versioned seeds/raw weights/feasibility/reasons reproduce old choices independently
  of later tuning/funds. Period effects and publication commit together; failed writes
  leave receipts retryable. Existing identities/history/replay stay intact. Old plugin
  downgrade requires a consistent pre-upgrade backup; no database/world reset.
- Ten added cases cover seeded alternatives/blocked choices, strict admin settings,
  actual paid execution, pending food/cash exhaustion, corruption, migration rollback,
  disabled forward settings and a seeded 240-period response/receipt/restart scenario.
  Initial test-only import ambiguity and restart seed error were corrected; the seed
  protection remained intact. Final Windows `.\mvnw.cmd clean verify -B -ntp` passed
  494 cases (492 unit/two packaged integration), zero failures/errors/skips.
- Disposable pinned Paper verified saved donation/cash/timber candidates published
  before explicit actual food-for-emerald/log exchanges, plus prior donation, failed
  save, duplicate, bargaining, learning, welfare, restart and unloaded regressions.
  Consistent marked schema-25 dev snapshot preceded upgrade. Probe archived; normal
  plugin-only startup/diagnostics/shutdown passed. Read-only integrity/FK and checked
  old identity/economy/learning history comparisons passed. No production changes,
  owner-config rewrite, merge or push. Native saved candidate completeness/feasibility
  and one cash/barter completion were independently checked.
- Phase 2 advances to 4/4 (100%) of its prototype reporting milestones. See
  PHASE2_ACCEPTANCE.md and FOOD_RESPONSES.md for actual gates/limits. Human client,
  inventory-plugin/native-death/cross-save compatibility, profiling and survival
  balance remain release work. No industries/jobs, construction, regional suppliers,
  reputation or starvation deaths are invented. Next is Phase 3 businesses/industry;
  Phases 3–9 remain planned. Earlier entries below are chronological batch snapshots.

## Configurable saved food-priority learning — October 9, 2026

- Branch `codex/phase2-priority-learning`: prior verified deliveries can modestly
  reinforce SEEK_FOOD at a future scheduled review. Optional strict editable
  `priorities.learning` uses a 12-period window and 25% maximum food bonus in new
  files; old files stay disabled. Both zero disables. No penalties, invented
  suppliers, extra money, payments, reputation or construction permissions.
- Only earlier period imports exactly matching applied receipts count. Saved
  unmet-demand prevention determines a bounded bonus; reserve-only deliveries and
  absent player visits are not failures. Food must still be unmet; existing
  cadence/pressure transitions prevent per-tick reselection. Enabled weights use
  100-times resolution; disabled legacy seeded choices/explanations are preserved.
- Transactional schema 25 adds a learning replay marker, versioned evidence/policy
  rows and indexed applied-receipt lookup. Old reviews default to no learning and
  are not rewritten; new choices replay their saved evidence through later tuning
  and restart. Missing required evidence fails explicitly. Failed upgrade recovery
  retains old-schema diagnostics; current-schema missing tables remain faults.
- Fourteen added cases cover domain ratios/blocked seeded choices, strict admin
  tuning, migration/replay/rollback/expiry/mismatch and a seeded 240-period scenario.
  Windows `.\mvnw.cmd clean verify -B -ntp` passed 484 cases (482 unit/two packaged
  integration), zero failures/errors/skips. First full run caught two old-schema
  diagnostic regressions during intentionally failed upgrades; repaired and reran.
- Pinned disposable Paper verified a native earlier twelve-unit delivery prevented
  four unmet units, earned an 8% future food bonus and retained evidence across
  restarts. Existing payment/bargaining/barter/welfare/famine/unloaded regressions
  passed. Consistent marked dev schema-24 snapshot preceded upgrade. No world reset,
  production changes, owner-config rewrite, merge or push. See PRIORITY_LEARNING.md.
  Test-only probe archived; final plugin-only startup/diagnostics/shutdown passed.
  Read-only schema/integrity/FK checks confirmed all checked prior identity/economy
  history and legacy review fields retained, with old owner learning still disabled.
  Native fixture's versioned twelve/four evidence and required metadata also checked.
- Phase 2 stays 3/4 (75%): bounded food learning is implemented. The remaining
  scope is explicit broader feasible-response comparison/execution explanations,
  rather than industry, building or regional supplier simulation from later phases.

## Exact food import outcome evidence — October 9, 2026

- Branch `codex/phase2-import-outcomes`: new plain-Java FoodImportImpact derives
  actual imports' marginal shortage relief and extra closing reserves from saved
  period inputs. A twelve-unit delivery against four unmet units reports four
  prevented units and eight added reserves. Independent farm recovery receives no
  import credit; later reserve consumption does not credit the import again.
- The cached economy view and `/civ debug economy` expose the comparison without
  SQL on the Paper thread. It holds opening reserves, production and demand fixed;
  it does not prove an appeal caused delivery, assign individual player benefit,
  invent alternate histories or award learning/reputation bonuses.
- Nine domain tests include exact maximum quantities, invalid evidence and 2,400
  seeded periods. One new temporary-SQLite case reconstructs the original outcome
  across restart and later consumption; existing delivery tests now assert pending
  receipts receive no credit and actual relief gets exactly one period's credit.
- Final Windows `.\mvnw.cmd clean verify -B -ntp` passed 470 cases (468 unit/two
  packaged integration), zero failures/errors/skips. Disposable pinned Paper also
  verified the native twelve-unit saved donation's exact four/eight impact and
  printed the new admin explanation. See FOOD_IMPORT_OUTCOMES.md.
  All prior native purchase/bargaining/barter/welfare/unloaded regressions passed;
  the probe was archived, then normal startup/diagnostics/shutdown passed. Read-only
  integrity/FK checks and pre-existing identity/economy history comparisons passed.
- Schema stays 24; no migrations, config rewrite, world reset or production changes.
  Existing reports and priority replay version 1 remain intact. A consistent marked
  dev snapshot preceded the server check. Phase 2 stays 3/4 (75%): trustworthy
  aggregate outcome evidence is now implemented; broader executed responses and
  bounded configurable learning from attributable action outcomes remain.

## Playable finite-stock food barter — October 9, 2026

- Branch `codex/phase2-player-barter`: `/civ barter timber|stone` previews a whole
  held food stack for current available oak logs/cobblestone; `/civ barter accept`
  explicitly confirms. Optional village UUID resolves overlap. Prices, conversion,
  stock floor, item cap and food target are editable. No recurring industry supplies.
- New files enable the optional barter section; old owner files stay disabled until
  explicitly opted in. Unaccepted quotes are temporary/expire using the editable
  bargaining offer deadline. Restart discards them without goods lost. Accepted
  immutable receipts recover even if new barter is disabled. No new migration after
  schema 24; no world/DB reset or owner-config rewrite.
- Exact PREPARED save proof precedes reservation. Whole food becomes goods in the
  same slot; EXCHANGED saved proof precedes one future food receipt. Changed goods
  cancel/refund stock once. Ambiguous inventory failures retain supplies for review.
  Pending trade markers block other donations/purchases/negotiations; unverified
  payouts lock item movement. Treasury is unchanged by barter.
- Nine added player/command/receipt cases plus one mixed two-village 240-period
  scenario. The latter conserves every food import, emerald debit and timber/stone
  unit across all payment types, cancellations, duplicate completions and restart.
  First focused run exposed the new receipt missing from the saved-proof allowlist;
  added only its explicit supported key, retaining bounded full-file proof rules.
  Final Windows `.\mvnw.cmd clean verify -B -ntp` passed 460 cases (458 unit/two
  packaged integration), zero failures/errors/skips. No test uses a real server DB.
- Native pinned disposable Paper passed 8 wheat -> 6 saved oak logs, precisely one
  six-unit stock debit, unchanged money, failed-save withholding, double acceptance,
  future food once, restart and all prior economy/welfare/unloaded regressions.
  Marked dev snapshot taken first; no production changes, merge or push.
  Final normal startup/command routing/diagnostics/shutdown and read-only integrity/
  FK/history checks passed; test-only probe archived outside active plugins.
- Phase 2 advances to 3/4 groups (75%): the player trade/prices/long-run stability
  prototype gate is verified. Autonomous executed responses and attributable outcome
  learning remain; industry-specific merchants, barter quantity negotiation and
  human client/inventory-plugin/release balance stay outside the verified prototype.
  See PLAYER_BARTER.md. Next: feasible responses and explained outcome feedback.

## Protected barter accounting — October 9, 2026

- Branch `codex/phase2-barter-accounting`: internal food-for-timber/stone quotes,
  exact goods reservations, cancellation and future food completion now use actual
  available supplies. No live barter menu/command or physical payout in this batch.
  The optional editable section starts disabled; existing owner configs stay untouched.
- Prototype exports only idle, non-shortage timber/OAK_LOG and stone/COBBLESTONE,
  with editable whole-item conversion, keep-stock floor, food target and item cap.
  No profession-generated goods or industry output. Quotes move no goods or funds;
  cash-poor villages can barter actual supplies. Failed bargaining cooldowns remain.
- Schema 24 adds immutable barter receipts and ordered reserve/refund stock movements
  with before/after quantities and completed-period context. Earlier reports remain
  intact; current supplies may differ from the last closing report. Shared pending
  food need and cross-payment player/operation guards prevent spending twice.
- Windows Wrapper clean verify passed 450 cases (448 unit/two packaged integration),
  no failures/errors/skips. Fifteen added cases cover exact floors/value, competing
  offers, stock/need conservation, cancellation/completion, changed terms, rollback,
  migration failure, admin settings and a seeded 240-period finite-stock famine
  scenario with restart. Previous Paper donation, purchase, bargaining, welfare,
  famine/recovery and unloaded regression probe passed under schema 24.
- Consistent marked dev schema-23 snapshot preceded upgrade. No production/world
  reset, main change, merge or push. Normal final plugin startup/diagnostics/shutdown
  passed; integrity/FKs valid and resident/home/family/finance/welfare/bargaining
  history retained. Accelerated dev population continues normally, without a reset.
  Phase 2 remains 2/4 groups (50%); physical
  barter confirmation/recovery, broader responses/learning and balance remain.
  See BARTER_ACCOUNTING.md. Next: player-facing saved inventory exchange.

## Saved emerald bargaining — October 8, 2026

- Branch `codex/phase2-food-bargaining`: `/civ bargain <emeralds>` proposes a price
  for a whole plain food stack; `/civ bargain accept` explicitly confirms the latest
  agreement/counter. Current need, scarcity value and available funds set offers.
  No goods or funds move during discussion. Ordinary village trades remain intact.
- Optional commented bargaining section: three failed prices, 60-second cooldown,
  45-second offer deadline. Strict editable limits; old owner files stay untouched
  and disabled without the new section. Menu/reconnect/restart cannot bypass an
  active player/village cooldown; quick purchases also respect it. No reputation penalty.
- Schema 23 adds saved discussion terms/history/policy and marks a consumed bargain
  in its purchase reservation transaction. Expired, superseded, changed or unaffordable
  offers cannot spend. Reserved transfer recovery retains the existing independent
  Minecraft save proof and remains possible with new bargaining disabled.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 435 cases (433 unit/two packaged
  integration), zero failures/errors/skips. Sixteen added cases cover domain rules,
  temporary SQLite migration/rollback/restart, current funds, patience/cooldowns,
  command confirmation, changed held goods, duplicates, permission and admin tuning.
- Pinned disposable Paper passed a current-value counter surviving restart, explicit
  double acceptance protection, withheld payment on failed save and one native
  8-wheat/3-emerald exchange/debit/future delivery. Prior welfare, famine/recovery,
  donations and unloaded-operation checks passed. Schema-22 dev backup preceded
  upgrade. Normal plugin-only startup, diagnostics and shutdown passed; integrity/FKs
  valid and original resident/home/family/projection/finance/welfare history retained.
  No production data/world reset, main change, merge or push.
- Phase 2 remains 2/4 reporting groups (50%); this completes another piece inside
  player trade. Barter/outbound supplies, broader executed responses/learning and
  integrated balance/compatibility remain. Human client tests remain deferred.
  See FOOD_BARGAINING.md for behavior, configuration and limits.

## Basic household wellbeing — October 8, 2026

- Branch `codex/phase2-household-wellbeing`: occupied families now have a saved,
  explained basic food/housing welfare score and SECURE/PRESSURED/FOOD_CRISIS/UNKNOWN
  condition. Treasury stays separate. No jobs/salaries, starvation deaths or changes
  to existing birth/death/migration rules were introduced.
- New commented optional wellbeing section: prototype neutral 50/100, recovery +1,
  undersupply -3, sustained crisis -5 and crowding -1. Admins can edit each 0..100
  value while stopped and restart. Old files without the section stay disabled;
  existing people never reset from a starting-score edit. Unknown housing is not zero.
- Schema 22 preserves welfare observations, their actual residents and each resident's
  shortage streak in the food owner's transaction. Current household IDs describe
  homes: movement follows resident IDs, not a guessed permanent family/home identity.
  New occupants do not inherit the previous family's score or crisis. Mixed/new
  residents use the floor mean of retained/initial scores. Disabled periods do not
  fabricate unrecorded personal history; old observations/policy remain intact.
- Eighteen added automated cases. Final Windows Wrapper clean verify -B -ntp passed
  419 cases (417 unit/two packaged integration), zero failures/errors/skips. Tests
  cover 10,000-period seeded famine/recovery per seed, actual migration adapter,
  new occupants, membership/write rollback, restart, admin tuning, migration failure
  and a 240-period integrated finite-budget purchase/cancel/famine/restart scenario.
  Two early fixture errors (missing household name; invalid direct home move under
  resident/home FK constraints) were corrected to use real migration behavior.
- Pinned disposable Paper passed supplied-family score, sustained food crisis,
  unchanged welfare across restart/retuning, gradual recovery, and all prior food,
  donation/payment and unloaded checks. Normal plugin startup/diagnostics/shutdown
  and schema-22 integrity/FKs verified; schema-21 dev snapshot taken first. Original
  resident/home/family/projection identities and treasury history preserved; older
  owner config stayed untouched, with assessment disabled.
- Phase 2 advances to 2/4 reporting groups (50%): basic labor, food production,
  treasury/spending and household food/housing wellbeing are verified. Broader
  employment/business prosperity remains Phase 3 context; learning/response
  execution, barter/bargaining and balance/compatibility gates remain unfinished.
  No production data reset, main change, merge or push. See HOUSEHOLD_WELLBEING.md.

## Budget-backed emerald food purchases — October 8, 2026

- Branch `codex/phase2-purchase-accounting`: sneak-right-click a managed villager,
  then explicitly Sell a supported plain whole food stack. A hungry village pays
  whole emeralds from actual funds; regular vanilla trades and free donations remain.
- Optional commented economy.yml purchases section: enabled in new files, disabled
  in older files lacking the section; editable 8.00 Trade Value/emerald, three demand
  periods as target and 64-emerald cap. Accepted terms survive disable/retuning.
- Schema 21 records immutable reservations/completion/cancellation. Funds and
  reservation share a transaction; pending donations/purchases count against need.
  Independent complete Minecraft player-file proof precedes reservation and food
  credit. Exact food stack becomes emeralds in the same slot; retries never pay twice.
  Changed stacks cancel/refund once. Ambiguous inventory failures hold for review.
- Windows Wrapper clean verify passed 401 cases (399 unit/two packaged integration),
  zero failures/errors/skips. Added 24 cases for temporary SQLite, saved proof,
  failed saves, inventory failures, restart, duplicates, held funds and admin settings.
  Initial verification exposed nested transaction use; corrected repository read
  inside its owner transaction, retaining the rollback checks.
- Pinned disposable Paper passed native menu/double-click, failed-save withholding,
  8 wheat for 3 saved emeralds, exactly one 24.00 debit, future food credit, restart
  and duplicate completion. Previous famine/donation/allocation/unloaded checks pass.
  Normal plugin-only startup/read-only diagnostics/clean shutdown passed. Schema-20
  dev snapshot taken first; schema-21 integrity/FKs valid; original IDs/history kept.
- Human client/inventory-plugin/native-death and cross-save crash compatibility remain
  prototype validation limits; SQLite/player/world files need consistent backups.
  No barter, bargaining, personal payroll or reputation attribution claimed.
  Phase 2 remains 1/4 completed groups with substantial partial work. No production
  data reset, main change, merge or push. See FOOD_PURCHASES.md and ECONOMY_TUNING.md.

## Configurable opening funds — October 8, 2026

- Branch `codex/phase2-configurable-opening-funds`: admins control one-time opening
  capital in the commented server economy.yml. New files enable 24.00 Trade Value
  per living resident; existing files without finance retain disabled grants.
- Reliable economic enrollment and its opening receipt share a transaction.
  Explicit opt-in also funds existing runtimes without treasuries at startup,
  based on activation-time living people. Every initialized account, including zero
  or a manually administered account, is preserved through retuning/re-observation,
  population changes, spending and restart. No recurring income/personal payroll.
- Eight added automated cases; Windows Wrapper clean verify passed 377 cases
  (375 unit/two packaged integration), zero failures/errors/skips. Covers admin
  validation, fractional values, disabled/zero grants, opt-in, restart, retained
  accounts, overflow and injected save failure rollback/retry. Initial verification
  found two new fixture errors (nonexistent settings factory and missing world binding);
  corrected fixtures without weakening application behavior.
- Schema remains 20, existing ledger used; no reset or automatic owner file rewrite.
  Funding-only edits leave food-clock history/catch-up intact. Paid exchanges,
  protected barter reserves and negotiation are upcoming, with file-first admin tuning.
  Phase 2 stays 1/4 full groups; no design question currently blocks the next batch.
- Disposable pinned Paper passed configured 48.00 opening capital, unchanged balance
  after restarting with 99.00 per resident, and prior food/donation/famine/unloaded
  checks. Native save showed exactly one opening receipt and valid integrity/FKs.
  Normal single-plugin startup, read-only diagnostics and clean shutdown passed;
  original saved people/households/family/projection identities retained. Consistent
  schema-20 dev backup taken first; old server config left untouched with grants off.
  Production JAR excludes probes/JUnit. No human client payment/bargaining test yet;
  no production data, main, merge or push changed.

## Finance direction clarified — October 8, 2026

The user approved moderate initial finances as the starting basis, configurable
opening capital, emerald payments and optional barter using contextual real stocks.
The prior funding/currency questions are resolved. These are approved plans, not
implemented payments or starting budgets. Persistent workplace/business assignments
remain Phase 3; current housing and observed farm labor do not imply an employer.
Repeated entity loads/spawns must preserve finances rather than seed more capital.
No numeric grant, exchange rate or guaranteed stack-for-stack ratio is locked.

## Exact finance quotes and shared treasury transactions — October 8, 2026

- Branch `codex/phase2-finance-transaction-foundation`: pure TradeQuotes provides
  exact full-stack Trade Value cost and maximum affordable whole items. Unknown
  budget differs from a measured zero; partial affordability never rounds up.
  Existing scarcity price rounding is reused once, with overflow/invalid inputs
  rejected. Quotes neither reserve funds nor accept goods or promise a payout.
- Treasury standalone APIs preserve their behavior. Internal row operations now
  join an existing SQL-owner transaction, allowing money and receipt writes to
  commit together. They never finish the caller's transaction. Tests combine an
  explicit fixture debit and food-delivery receipt; injected receipt failure rolls
  back both, retry retains both and later food accounting consumes the receipt once.
- Twelve new cases; final Windows Wrapper `clean verify -B -ntp` passed 369 tests
  (367 unit/two packaged integration), no failures/errors/skips. Covers 10,000 seeded
  affordability cases, scarcity conversion, cent/long boundaries, insufficient funds,
  shared opening/credit/debit rollback, operation retries and restart.
- Initial verification exposed an intermittent existing preview fixture failure:
  Paper Location retains a weak world reference and the unregistered proxy could be
  collected. Test now retains its fixture worlds strongly; pinned Paper source
  confirmed the lifetime contract. No gameplay behavior was changed for that fix.
- Schema remains 20. No real/server DB, world, inventory or owner config changed;
  all writes used temporary test databases. No new Paper-facing behavior or server
  smoke run this batch; funding/payment adapters will need native Paper/client tests.
- Updated finance guide, status, decisions, devlog and progress detail. Phase 2
  remains 1/4 full reporting groups. Initial funding source and player currency
  questions are pending; no starting money, income, emerald exchange rate, live
  purchase or barter policy has been assumed. Next: approved funding and payment
  handoff, then budget-backed purchase execution and integrated economy balance.

## Voluntary village food appeals — October 8, 2026

- Branch `codex/phase2-village-food-appeals`: a selected SEEK_FOOD priority now
  publishes the actual latest-period deficit in Village supplies. Empty-hand or
  unsupported/custom-item viewing is read-only; reopen with plain food for the
  existing confirmation. Disabled transfers/permissions retain a protected button.
- New period snapshots retain OPEN/NONE/RESOLVED/WITHDRAWN status, observed pressure
  improvement/worsening, actual production/imports and the governing priority review.
  Pending deliveries are separate; queued catch-up is disclosed. No invented quest
  target, payout, stock creation, supplier or building consent. Recovery observations
  do not prove the appeal caused it and do not train weights without attribution.
- Schema 20 transactionally adds food_appeals without backfilling past observations.
  Appeal save failure rolls back stocks, needs, priority and receipt use together;
  restart preserves exact history. No production reset or owner config rewrite.
- Twelve added cases; final Windows Wrapper `clean verify -B -ntp` passed 357 tests
  (355 unit/two packaged integration), zero failures/errors/skips. Includes all appeal
  states, mixed/conflicting evidence, non-causal local-production recovery, a repeated
  1,200-period scenario, temporary-SQLite restart, upgrade and write rollback/retry.
- Disposable native Paper checks passed empty-hand report/card and protected click,
  actual confirmed player-file debit, famine appeal across restart, recovery with
  twelve real imported food units and no duplicate receipt. Farm/unloaded checks
  still passed. Normal single-plugin startup/status/shutdown passed; no forced chunks.
- Consistent confirmed-dev schema-19 backup first; schema 20 integrity/FKs verified.
  All original settlement/resident/household IDs and family/entity links retained.
  Production JAR excludes probes/JUnit. Human client UX and scale profiling remain
  pending. Treasury funding/spending, paid trade/barter, broader action feasibility
  and attributable outcome feedback remain ahead. Phase 2 stays 1/4 full groups.

## Household food allocation — October 8, 2026

- Branch `codex/phase2-household-food-allocation`: completed food periods now divide
  fulfilled supplies proportionally between occupied families and the unassigned
  resident pool. Whole-unit largest remainders conserve food; permanent IDs resolve
  exact ties. Adult/child demand remains editable. This is simulated coverage, not
  physical meals, invented wealth or a new mortality/fertility rule.
- Saved SUPPLIED/UNDERSUPPLIED diagnostics and consecutive unmet family periods
  survive restart and clear with relief. Historic schema-18 allocation stays UNKNOWN;
  disappeared/reoccupied households begin a new consecutive streak. Exact tie bias
  and future social rationing policy are documented rather than silently locked.
- Transactional schema 19 adds four nullable fields; explicit-column INSERTs prevent
  schema-extension breakage. Allocation shares the food/needs/priority/receipt commit.
  No reset, forced chunk loads, owner config rewrite or production save changes.
- Eight added automated cases; final Windows Wrapper `clean verify -B -ntp` passed
  345 cases (343 unit/two packaged integration), zero failures/errors/skips. Includes
  10,000 seeded conservation scenarios, integer overflow boundaries, ordering/ties,
  a 1,200-period crisis/recovery, real family persistence and migration failure/retry.
- Disposable Paper probe passed supplied family, famine streak, unchanged history
  on restart and recovery after a verified native food donation; unloaded accounting
  still passed. Normal single-plugin startup/status/shutdown passed, no forced chunks.
- Consistent schema-18 confirmed-dev backup first; schema 19 integrity/FKs checked.
  Original settlement/resident/household IDs and family/entity links retained.
  Production JAR excludes probes/JUnit. Human client playtesting and scale profiling
  remain pending, alongside treasury funding/spending, autonomous response execution,
  outcome feedback and paid trade/barter. Phase 2 remains 1/4 complete reporting groups.

## Initial audit
Prototype contained six Java source files: `Civilizations`, `DatabaseManager`, `VillageDiscoveryListener`, `Settlement`, `SettlementNameGenerator`, `NeedsEngine`. Discovery via player chunk movement/HOME POIs, simple settlement persistence, population/bed census and binary housing shortage. No actual economy, business, roads, physical construction, history or simulation yet.

Audit identified: malformed INSERT; player-centered settlement/census; global permanent region suppression; ambiguous nearby lookup; new settlements not immediately censused; silent migration errors; synchronous DB/world work in movement listener; no tests or Maven Wrapper initially.

## Verified persistence/build foundation
- Foundation work prepared on `foundation/persistence-build`; local commits are now authorized. No merge or push to main is authorized.
- Added Maven Wrapper pinned 3.9.11, JUnit 5 and test class `DatabaseManagerTest`.
- Fixed INSERT to persist all nine model fields, including beds; added isolated test constructor.
- Updated `pom.xml`, `.gitignore`, README.
- `.\mvnw.cmd clean verify -B -ntp` passed on Windows/Java 25: six tests, zero failures/errors/skips. Earlier `clean package` also passed; packaged JAR shaded SQLite and excluded test/JUnit/Paper API.
- Outstanding reported warnings: Maven/SQLite Java 25 and Shade manifest overlap. Discovery and migration behavior not changed.
- Both wrappers are present at the repository root, readable and not hidden: `mvnw` (11,790 bytes), `mvnw.cmd` (8,481 bytes); Maven 3.9.11 is pinned in `.mvn/wrapper/maven-wrapper.properties`.
- Checkout is under `C:\Users\noahw\OneDrive\Desktop\IdeaProjects\Civilizations`; wrapper files have OneDrive reparse attributes. Older compiler records referenced a different checkout path. Explorer visibility itself has not been inspected.
- Prefer a local checkout and server outside cloud sync. `.gitignore` does not exclude files from OneDrive. Cloud sync is not an atomic backup of Git metadata or an open SQLite DB. No relocation or server-data reset has been performed.
- No Paper server smoke test has been run for this foundation; headless persistence tests are verified.

## Immediate next work
1. Phase 3 business storage/staffing is implemented. Next: bounded owner/cache views,
   reliable existing-site observation and editable automatic founding/hiring, then
   resource-backed production. Preserve the completed Phase 2 economy accounting;
   do not infer taxation, wages, unlimited goods or income from a profession label.
2. Human client playtest can follow later; user authorized Phase 2 without it.
   Preserve vanilla profession/trades/inventory when physical villagers are recreated,
   and handle other native transformations before recommending established survival worlds.
3. Current roadmap phase: businesses and industry; no
   construction or canonical roadmap changes. Latest feature work has not been pushed.

## Confirmed player food donations — October 8, 2026

- Branch `codex/phase2-player-food-donations`: sneak-right-click a saved living
  villager with ordinary food, then explicitly confirm the whole held stack in Village
  supplies. Ordinary right-click trading and read-only `/civ aid` remain available.
- Recovery receipt and inventory debit share Minecraft player data. A bounded worker
  verifies the full saved gzip/NBT receipt before the existing SQL owner credits food.
  Failed void saves, repeat clicks, pending operations and stale saved markers cannot
  create duplicate credit or a second debit. Offline/shutdown work resumes from markers.
- Editable donation enable flag; new configs true, existing files without it false.
  Pending recovery continues when new donations are disabled. Protected menu clicks/
  drags and next-tick confirmation; player permissions separate from admin debugging.
- Fourteen added automated cases; final Wrapper clean verify passed 337 (335 unit/two
  packaged integration), no failures/errors/skips. Covers bounded/corrupt/truncated NBT,
  receipt identity, failed saves, retries/shutdown, stale markers and consent guards.
- Real Paper native menu/inventory/player-data checks passed with a synthetic offline
  player facade: intentionally disabled save prevented credit, then saved debit/marker,
  reload, one receipt, crisis relief and duplicate rejection passed. Actual 26.2 path
  players/data verified after first attempt correctly withheld credit on the old path.
- Schema remains 18. Confirmed-dev backup first; original identities/links and
  integrity/FKs retained. Normal single-plugin startup, diagnostics, unloaded fixture
  and shutdown passed. No actual player/production save edits.
  Donation does not pay money/reputation or implement barter/autonomous purchasing.
  Pinned-format upgrade compatibility, human client UX, inventory plugins, scale and
  consistent cross-save backup recovery remain limits. See FOOD_DONATIONS.md.
- Phase 2 still 1/4 full reporting groups; actual player aid is now a verified partial
  feature. Next: household food allocation/wellbeing and broader economic responses.

## Player food-aid previews — October 8, 2026

- Branch `codex/phase2-food-aid-preview`: `/civ aid` now previews an ordinary held
  food stack for the nearest ready village home, names the saved village and shows
  its latest food report. It takes no items, creates no receipt and pays nothing.
- Editable optional `aid-preview` economy.yml section: enabled, same-world 3D range
  (default 32), six ordinary plant-food conversions. Custom items, partial evidence,
  unsupported quantities and ambiguous town ties fail clearly. Explicit target IDs
  still require nearby evidence. Old owner files retain edits and documented defaults.
- Public preview permission defaults true; administrative debug permission remains
  op-only inside its handler. Cached name/identity/food reads avoid main-thread SQL,
  block scans, chunk loads or waits. Schema remains 18; no migration/reset.
- Sixteen added automated cases; final Windows Wrapper clean verify passed 323
  (321 unit/two packaged integration), zero failures/errors/skips. Earlier test fixture
  used a live-registry-dependent ItemStack constructor; fixed the headless facade.
- Real disposable Paper passed native item conversion/custom rejection with a
  synthetic read-only player facade and unchanged goods/food, plus earlier food,
  needs, relief, restart and unloaded scenarios. This is not a human client test. Confirmed-dev schema-18 backup taken first;
  original IDs/family/entity links and integrity/FKs retained. Normal single-plugin
  startup, console routing, unloaded fixture and clean shutdown also passed. Existing
  economy.yml remains untouched. Production JAR excludes probes/JUnit.
- Actual donation/merchant inventory handoff remains blocked on durable cross-save
  recovery. Pinned Paper saveData returns void and PlayerDataStorage may return on
  disabled saving or log a save exception; merely calling it cannot certify a durable
  debit. No actual player data files edited. See PLAYER_FOOD_AID.md.
- Phase 2 stays 1/4 full groups; player-facing preview is a verified partial piece,
  not completed trade/barter. Next: a recoverable actual inventory transfer and
  broader household/economic responses. No new gameplay decision needed here.

## Explicit food delivery accounting — October 8, 2026

- Branch `codex/phase2-food-relief`: trusted internal adapters can record explicit
  delivered food with source, quantity and operation ID. Pending goods enter a future
  food period; retries cannot duplicate imports and new arrivals cannot rewrite old
  catch-up periods. Receipt processing is bounded to 64 per village period.
- Food, needs, priorities, checkpoint and applied receipt marks share one transaction.
  Failure rolls back effects for retry; partial evidence pauses delivery use. Schema 18
  adds the ledger without backfilling or resetting people, histories or reserves.
- Seven new temporary-SQLite cases cover relief, duplicates, arrival timing, rollback,
  restart/pause, input validation, batch limits and schema-17 migration recovery.
  Windows Wrapper clean verify passed 307 cases (305 unit/two packaged integration),
  zero failures/errors/skips. Production JAR excludes probes/JUnit.
- Disposable Paper probe passed a synthetic 12-unit shipment surviving restart,
  clearing zero-production shortage and rejecting duplication, followed by farm
  recovery/unloaded operation. Consistent schema-17 dev backup first; original people,
  households/family/entity links retained, schema 18 integrity/FKs verified. Normal
  single-plugin startup, unloaded fixture and clean shutdown also verified.
- This is backend delivery accounting, not a player inventory handoff, donation
  command, merchant or autonomous supplier. No normal gameplay caller submits goods.
  Player item exchange and full economic responses remain; Phase 2 stays 1/4 full
  reporting groups with these verified partial pieces listed in PROGRESS.md.
  See FOOD_RELIEF.md. No production reset, push, merge or main modification.

## Evidence-based village priorities — October 8, 2026
- Branch `codex/phase2-village-priorities`: villages choose MONITOR, SEEK_FOOD or
  INVESTIGATE_HOUSING from saved unmet food and observed household crowding. Unknown
  housing cannot justify crowding; infeasible priorities are excluded under every seed.
- Editable prototype priority weights/cadence in optional `economy.yml` section.
  Minimum relative weight excludes irrelevant alternatives before a seeded weighted
  draw. Five-period default reviews reconsider early on appearance/severity/recovery,
  crowding appearance/recovery or priority-setting changes. No invented leader/jobs.
- Schema 17 saves review inputs, policy, seed, replay version and chosen priority.
  Historical candidate explanations reconstruct from original inputs. Food/needs/
  reviews commit together; retries cannot duplicate effects. Policy tuning is forward
  and does not discard food-clock time. Old periods get no fabricated decisions.
- Twelve new cases; Windows Wrapper clean verify passed 300 cases (298 unit/two
  packaged integration), no failures/errors/skips. Blocked choices over 1,000 seeds,
  reasonable alternatives, crisis filtering, 1,200-period replay, rollback/retry,
  schema-16 upgrade failure/recovery and existing treasury retention verified.
- Guarded Paper probe passed actual food crisis selecting SEEK_FOOD, original choice
  surviving restart/feast retuning and recovery returning to MONITOR, plus earlier
  food/needs/unloaded scenarios. Marked dev schema-16 snapshot taken first; original
  identities/families preserved and integrity/FKs checked. No production data reset.
- Priority decisions are nonbinding planning intentions. No suppliers, purchases,
  stock transfers, jobs or construction are fabricated. Execution, outcome feedback,
  broader wellbeing and player trade remain. Phase 2 stays 1/4 full reporting groups;
  PROGRESS now lists verified partial pieces rather than hiding them behind 25%.
  See VILLAGE_PRIORITIES.md. No push, merge or main modification.

## Household needs and treasury accounting — October 8, 2026
- Branch `codex/phase2-household-needs`: food periods now retain household adults,
  children, configured demand, observed housing and unassigned living residents.
  Unknown capacity is distinct from crowded housing. Shared food pressure is not
  presented as proof of individual rations or complete prosperity.
- Needs and food effects/checkpoint share one transaction. Historical housing and
  crisis evidence remain intact through restart, retuning and later observations.
  Schema 16 adds needs snapshots and an independent exact Trade Value treasury
  ledger; old periods are not backfilled with invented household observations.
- Explicit treasury opening/credit/debit APIs require a source/reason and operation
  UUID. Idempotent retries, rollback, insufficient funds and overflow are tested.
  No automatic live account, money creation, income, tax, salary or personal wallet.
- Fourteen added automated cases; Wrapper clean verify passed 288 cases (286 unit,
  two packaged integration), no failures/errors/skips. Includes two repeatable
  1,200-period needs scenarios and schema-15 upgrade failure/retry.
- Guarded real Paper food probe passed needs output, actual household demand/housing,
  preserved crisis assessment after restart/retuning, no invented treasury and the
  earlier food/unloaded behaviors. Consistent schema-15 dev snapshot first. Original
  people/families retained, integrity/FKs verified; no production save/world reset.
- `/civ debug economy` uses cached period evidence, limits household output to twenty
  entries and distinguishes spending budget from prosperity. Large-city profiling,
  rationing, full wellbeing, actual budget flows and player trade remain outstanding.
- Phase 2 stays 1/4 (25%) by reporting groups. The second group is partial: observable
  needs and budget machinery do not complete the broader labor/prosperity gameplay.
  Next gate: feasible decision pressures/responses with tested causal prerequisites.
  See HOUSEHOLD_NEEDS_AND_TREASURY.md. No merge/push/main changes.

## Live food economy — October 8, 2026
- Branch `codex/phase2-live-food`: loaded main-thread crop snapshots around saved
  homes feed the existing SQL owner. Saved living adults supply bounded prototype
  labor; children consume food. Default capacity is one worker per 16 planted crops.
  No harvesting, ownership claim, player-block edit or invented offscreen facility.
- Schema 15 adds evidence, saved independent economic clocks and recorded forward
  tuning. Per-period policies preserve old explanations across threshold edits.
  Stocks/inputs/checkpoint commit together; partial/conflicting scans pause work.
  Unloaded villages use last reliable capacity without forcing chunks.
- New read-only `/civ debug economy <UUID>` shows reserves, evidence, shortage
  explanations and indicative prices. These are not merchant offers.
- Windows Wrapper clean verify: 274 cases (272 unit/two integration), no failures,
  errors or skips. Tests cover actual saved demographics, temporary databases,
  clock/restart limits, pauses, failed-save rollback/retry, history-preserving tuning,
  migration backfill, negative-height crop scanning and old owner configuration.
- Guarded real Paper probe passed planted-field supply, depleted food/severe shortage,
  forward feast retuning, identity/history restart preservation and unloaded accounting.
  Consistent disposable schema-14 backup taken first. No production reset or world
  deletion. Test helper is excluded from the production JAR.
- Phase 2 stays 1/4 (25%): the second milestone is partial. Treasury, family allocation
  and wellbeing, business hiring, autonomous responses and player trade remain.
  No starvation population penalty. Catch-up uses current saved demographics and last
  reliable field evidence rather than full historical event interleaving. Scale and
  human survival playtesting remain pending. See LIVE_FOOD_ECONOMY.md.

## Editable economy prototype settings — October 8, 2026
- User authorized configurable economy defaults and requested easy famine/feast tuning.
  `economy.yml` is created only if missing and validated before database startup.
  Comments, owner edits and matching custom commodity stock/price entries are preserved.
- Plain-Java EconomySettings computes explicit first-enrollment supplies, adult/child
  demand, production bounded by workers plus evidenced capacity, and exact abstract
  scarcity prices. No fabricated facility, block change or merchant transaction.
- Defaults/recipes/ranges documented in ECONOMY_TUNING.md. Nineteen new cases test
  the shipped file, malformed/partial/unsafe edits, exact prices, unknown capacity,
  overflow, immutable settings and distinct normal/famine/feast accounting scenarios.
- Wrapper clean verify passed 261 cases (259 unit/two integration), no failures/errors/
  skips. Paper first-run creation, 25% edit/comment surviving restart byte-for-byte,
  normal population diagnostics/unloaded fixture and clean shutdown verified. Restored
  test settings to normal; schema remains 14 and no economy accounts were auto-created.
- At that batch settings/calculations were prepared, not live consumption or trading.
  Recorded tuning transitions and live food were subsequently implemented above. No approval blocker remains for prototype tuning.
  Phase 2 stays 1/4 (25%) with partial labor/price work; no merge/push/main changes.

## Phase 2 commodity accounting — October 8, 2026
- Branch `codex/phase2-commodity-accounting`: plain-Java commodity keys, exact stock
  conservation, explicit incoming/demand categories, reserve use, per-commodity
  shortage duration, protected caller policy and quantitative report explanations.
- Schema 14 adds accounts/stocks/retained period inputs; no automatic inventory
  seeding or live economy scheduling. No inferred production or starvation effects.
  Initialization, advance, history and checkpoint operations are transactional.
- Eighteen new tests; Windows Wrapper clean verify passed 242 cases (240 unit,
  two packaged integration), no failures/errors/skips. Pure and saved 1,200-period
  crisis/recovery fixtures, mid-crisis restart, conservation, write rollback/retry,
  stale-period and tuning rejection, upgrades/malformed schema verified.
- Backed up marked disposable schema-13 DB; real Paper schema-14 startup/commands,
  unloaded fixture, no forced chunks and clean shutdown verified. Integrity/FKs OK;
  original identities/families preserved; zero economy accounts as intended.
- At this accounting milestone Phase 2 was 1/4 (25%), not a running/balanced economy.
  Tuning approval was pending then; subsequently approved and implemented above.
  Human client playtest deferred explicitly; known Phase 1 recreation limits remain.
  No production reset, construction, main modification, remote push or merge.
  See COMMODITY_ACCOUNTING.md for contract and remaining gates.

## Live population and approved zombie rule — October 8, 2026
- User chose to disable zombie conversion for managed villagers. Registered INFECTION
  guard leaves unmanaged villagers, curing, lightning and unrelated transforms alone.
  Verified lethal zombie attacks still kill people and save death/history; no general immunity.
- Paper projector applies saved names/age locks; nearby births/arrivals materialize only
  at loaded safe homes. Retired/moved/deceased incarnations reconcile incrementally.
  One asynchronous claim/add/activation handshake at a time; persistent freeze marker
  recovers shutdown between activation commit and main-thread acknowledgement.
- Vanilla breeding for managed parents cannot bypass saved families/calendar. Actual
  deaths retry on SQL owner, including deaths during asynchronous enrollment. Accepted
  shutdown saves drain before DB close. Entity removal for simulated death does not
  emit a second saved native death.
- Editable projection limits: radius 64, day budget 32/world, night budget eight,
  two inspected chunks/sixteen residents per tick. Night budget defers additional
  spawns; existing villagers use vanilla behavior and are not culled at sunset.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 224 tests (222 unit/two integration),
  no failures/errors/skips. Real Paper probe passed hazardous placement deferral,
  babies/adulthood/names, restart/freeze/claim recovery, native breeding/infection/death,
  five-person migration, simulated deaths and enrollment/death race. Probe never ships
  in production JAR; guarded by disposable marker/no online players.
- Normal plugin-only startup/commands/unloaded simulation checked separately; test
  fixture stayed unloaded and no forced chunks remained. SQL integrity/FKs passed for
  all scenario DBs. Owner batch observed 9.215 ms on three people: not a tick or scale
  bound. Large-server profiling and human client/balance testing remain.
- Schema remains 13; no reset, new migration or production/main/remote changes.
  Vanilla trade/profession/inventory preservation on recreation is not implemented;
  use disposable worlds for playtesting. Source/history/roadmap remain preserved.

## Resident projection preparation — October 8, 2026
- Branch `codex/phase1-resident-projection`: schema 13 incarnation registry,
  claim-before-spawn APIs, retained aliases, actual-death ledger and immutable owner
  views. Pure planner covers unloaded/pending/moved/deceased identities. No entity
  mutation adapter or death listeners are wired yet.
- Fixed stale settlement counts after demographic outcomes, including a town emptied
  by migration; unbound legacy census rows remain preserved.
- Read-only `/civ debug explain <uuid>` shows cached saved reasons, migration origins
  and identity conflicts; complete history remains in SQLite.
- Wrapper clean verify: 215 tests (213 unit, two integration), no failures/errors/skips;
  eighteen added cases. Real disposable Paper schema-12-to-13 upgrade, command,
  unloaded simulation and clean restart verified with backup first. Original IDs,
  families/homes preserved; month-45 adult arrival explained without loading its area.
  SQLite integrity/FK checks passed; server stopped cleanly.
- At this preparation milestone the zombie decision was pending; subsequently approved
  and implemented above. The following milestone history describes earlier snapshots.

## Transactional schema migration milestone
- Work continues on `foundation/schema-migrations`, branched from the committed persistence/build foundation and canonical documents. Main remains unchanged; nothing pushed or merged.
- `SchemaMigrator` uses SQLite `PRAGMA user_version`: 1 = base settlement identity, 2 = population, 3 = beds. All pending DDL and version markers share one transaction.
- Unversioned prototype databases are adopted by inspecting columns, preserving existing rows and adding only missing census columns with zero defaults. No reset is required.
- Newer schema versions and missing required columns fail explicitly. Failed initialization closes the connection. No arbitrary migration exceptions are swallowed.
- Ten migration test cases plus the six persistence tests pass with Windows `.\mvnw.cmd clean verify -B -ntp`: fresh install/reopen, three legacy shapes, incremental upgrades, rollback/recovery, newer-version rejection, malformed schema and connection cleanup.
- Tests use only temporary SQLite files. Actual server DBs were not opened or upgraded. No gameplay behavior, world identity, discovery or housing rule was redesigned.
- Migration limits: no migration smoke test on Paper yet; column presence is checked, not exhaustive validation of legacy constraints/data types. Schema steps are intentionally small and internal, without a migration framework. Standalone snapshot tooling was added in the subsequent milestone below.

## CI, backups and development procedures milestone
- CI/backup milestone branch: `foundation/ci-backups`, based on the schema-migration branch. No merge or push performed; main is untouched.
- GitHub Actions workflow verifies Windows and Linux with JDK 25 and the Maven Wrapper. Official actions are pinned to commit IDs; permissions are read-only, checkout credentials are not retained, and no publishing/deployment runs.
- `.gitattributes` explicitly keeps the Unix wrapper LF and the Windows wrapper CRLF. No broad source renormalization performed.
- Workflow validated with actionlint 1.7.7 (shellcheck/pyflakes disabled). Hosted Windows/Linux CI has not run because the branch remains local.
- Standalone `DatabaseBackup` uses a separate read-only SQLite connection, `VACUUM INTO`, integrity checking and a private staging file. It refuses existing destinations and never initializes/migrates/restores the source. No in-game backup command or scheduler yet.
- Seven new backup tests cover committed WAL data, exclusion of uncommitted writes, independent restored content, schema version retention, source/destination protection, missing paths and corrupt-source cleanup. A Failsafe integration test launches the CLI from only the packaged JAR.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 23 unit cases plus one packaged-JAR integration case. A second `clean verify` with a new empty temporary Maven repository also passed all 24 cases; dependency resolution does not rely on the old IntelliJ cache.
- `docs/DEVELOPMENT.md` records exact-build/JDK/checksum requirements for a disposable localhost Paper environment, source bundles, stopped-server world/config backups, consistent SQLite snapshots and restore drills.
- No actual Paper server created or launched, EULA accepted, live DB backed up, worlds copied, production data changed or checkout moved. Recovery procedures are documented, not claimed as a completed real-world drill.
- Schema remains version 3. Prototype discovery/population/housing rules remain unchanged; persistent residents/households, simulation clock and materialization are still pending. Maven/SQLite Java 25 and Shade manifest warnings remain non-failing.

## Deterministic headless runner milestone
- Headless runner milestone branch: `foundation/simulation-harness`, based on `foundation/ci-backups`.
- Added plain-Java `SimulationCheckpoint`, `SimulationRunner` and `SimulationHarness`, with no Paper/SQL/world/time dependencies in the simulation package.
- Explicit seed and completed-step checkpoints, deterministic per-step randomness, caller-supplied targets/budgets, and explicit remaining catch-up work. Pure immutable steps can resume or retry without hidden PRNG state.
- Packaged CLI runs a labeled synthetic checksum probe only; it does not simulate residents or convert logical steps to years. No in-game command, scheduler or persistent simulation checkpoint yet.
- Fourteen runner unit cases and one new packaged-harness case pass. Full Windows `.\mvnw.cmd clean verify -B -ntp`: 37 unit cases plus two integration cases, zero failures/errors/skips.
- Tests include batch-size independence, checkpoint reconstruction, failure retry, step ordering, invalid inputs, the long boundary, a fixed replay fixture and repeatable one-million-step probes. This is not real multi-century population simulation or a Paper performance guarantee.
- `docs/SIMULATION_HARNESS.md` documents immutable/pure steps, stable ordering, random replay conventions, bounded-step versus CPU-time limits and exact CLI usage.
- Schema remains version 3; no live database/world/server changes. Household capacity, time scale, demographic rates, authoritative seeding and recognition remain open. Paper smoke testing and hosted CI still pending.

## Persistent people and households milestone
- Resident milestone branch: `population/resident-storage`, based on the progress-tracker branch. Main untouched; no merge or push.
- Plain-Java immutable Resident/Household records save UUID identity, names, settlement/household membership, logical age and living/deceased status. No calendar ratios or demographic probabilities chosen.
- Transactional schema version 4 adds population tables/indexes while preserving legacy settlements/census. SQLite foreign keys enforce settlement/household references, including same-settlement household membership; protected referenced rows do not cascade away.
- PopulationRepository supports stable immutable reads, insertion, resident updates, living counts and atomic household/member insertion. Duplicates, mismatched references, unknown updates, implicit transfers and nested batch transactions fail explicitly.
- Deceased people remain in stored history-ready records but are excluded from living counts. Families, jobs, skills/prosperity, real homes and simulated demographic events remain pending.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 56 unit cases plus two packaged-JAR integration cases (58 total), zero failures/errors/skips. New tests cover persistence/restart, constraints, atomic rollback/retry, protected identities, schema-v3 upgrades/failure recovery, and population snapshots.
- All tests use temporary SQLite DBs; no production/server data, villagers or worlds changed. No Paper smoke test of schema v4 yet. Existing listener census remains separate and unchanged; no authoritative residents automatically seeded.
- Phase 1 tracker is four of ten reporting milestones complete (40% by milestone count, not time). See docs/POPULATION.md for boundary contracts and pending gameplay gates.

## Family memory milestone
- Family milestone branch: `codex/population-family-links`, based on resident storage. Schema v5 adds parent/child and canonical partner links; siblings derive from shared parents.
- Constraints reject missing people, self-links, reversed duplicates, ancestry cycles and in-place overwrites. Links survive cross-settlement residence, death, restarts and snapshots.
- Six new temporary-database tests pass, including v4 migration failure/retry. Full Windows `.\mvnw.cmd clean verify -B -ntp`: 62 unit and two integration cases (64 total), no failures/errors/skips.
- No family generation, parent/partner counts, marriage lifecycle or age/rate policies introduced. No Paper test, live DB change, merge or push. Phase 1 remains 4/10 (40%); jobs still needed for the household/family/job milestone.

## Saved logical aging milestone
- Current branch: `codex/population-saved-aging`. Schema v6 adds explicit age-only seed/progress/replay-version clocks; existing settlements, residents and family records preserved.
- Pure immutable aging and transactional persistence resume within caller-provided step budgets. Completed targets do not double-age; failure rolls back progress and all person updates. Deceased people retain ages and family links.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 74 unit plus two integration cases (76 total), no failures/errors/skips. Tests include restart, snapshots, write/overflow rollback, v5 upgrade recovery and a repeated 500-person/1,000-step pure probe.
- No real-server scheduler or births/deaths/migration yet. Logical age-only replay is version 1 and not silently repurposed as a calendar/demographic engine. Phase 1 remains 40%; partial work advanced two milestone groups.
- User clarified automatic village participation and authorized configurable prototype population settings. See docs/POPULATION_SIMULATION.md and the decisions log. No actual world/server DB changes, merge or push.

## Headless birth/death prototype
- Demographic domain milestone branch: `codex/population-demographics`. Pure immutable monthly population step emits birth/death explanations, retains deceased people and uses observed household capacity.
- User-approved editable prototype policy documented in POPULATION_SIMULATION.md; no closed numeric canon. No automatic partnership formation, crisis modifiers, demographic persistence or scheduler yet.
- Eleven new tests, including two 100-year (1,200-month) accounting/replay scenarios. Windows `.\mvnw.cmd clean verify -B -ntp`: 83 unit plus two integration (85 total), no failures/errors/skips.
- Phase 1 remains 40% pending demographic persistence and remaining milestone gates. No server/world/actual database changes, merge or push.

## Atomic demographic saving milestone
- Demographic storage milestone branch: `codex/population-demographic-storage`. Schema v7 adds a global monthly clock, capacity observations and sequenced birth/death events. Additive upgrades preserve existing people/family/settlements.
- People, newborn identities, parent links, event reasons and progress commit together. Retry/restart/snapshot replay preserves outcomes; settings/replay changes and mixed aging/demographic clocks fail explicitly.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 92 unit and two integration (94 total), no failures/errors/skips. Tests include a snapshot-restored 100-year run across batch sizes, failure after newborn/link writes and safe event pagination.
- Phase 1: 5/10 reporting milestones (50% by count). Headless saved aging/birth/death is complete; runtime calendar/scheduler, import/projections, migration, job foundation and Paper validation remain. Global snapshot/writes require profiling before tick-loop integration.
- No actual server database/world changes, merge or push. Demographic engine remains explicit opt-in through Java APIs; original listener unchanged.

## Saved migration milestone
- Migration milestone branch: `codex/population-migration`. Schema v8 preserves/rebuilds event history with structured migration origins and external arrivals. Current demographic replay convention is 3; older clocks/settings fail explicitly rather than changing silently.
- Pure seeded migration resolves observed housing pressure, moves whole living households to vacant same-world housing and retains every identity/family link. Occasional external adults require observed space. No world or entity work.
- Seven new domain and six new storage cases. Windows `.\mvnw.cmd clean verify -B -ntp`: 105 unit plus two integration (107 total), no failures/errors/skips. Restored 100-year combined replay, every event page, rollback/retry, v7 upgrade, cross-world safety and move-then-death in a single batch verified.
- Phase 1: 6/10 (60% by reporting milestone count). Job foundation, runtime calendar/catch-up, import/projection/diagnostics and real Paper/CI/recovery validation remain.
- No actual server/world/production DB changes, merge or push. Prototype uses legacy stored world names for boundaries; stable world identity remains a discovery integration decision. No automatic partnership formation or proven long-term balance yet.

## Work qualification foundation milestone
- Current branch: `codex/population-work-foundation`. Schema v9 adds resident skill counters without changing existing population/history. Saved skill totals are idempotent and retain identity across movement/death/restarts/snapshots.
- Immutable WorkProfile/JobRequirements and explainable eligibility reject deceased, underage and unqualified candidates. No fabricated employer/occupation/hiring, salary or retirement rule; real business/labor integration remains in later phases.
- Five new domain and five new storage tests, plus migration skill-retention coverage. Windows `.\mvnw.cmd clean verify -B -ntp`: 115 unit and two integration (117 total), no failures/errors/skips.
- Phase 1 7/10 (70%): household/family/job qualification foundation complete. Calendar scheduling, automatic village import/projection/diagnostics and real Paper/CI/recovery remain.
- No server/world/actual DB changes, merge or push. See WORK_FOUNDATION.md for exact scope.

## Disposable Paper and recovery verification
- Branch `codex/paper-test-preparation`: guarded launcher validates test marker,
  pinned JAR checksum, accepted EULA, loopback properties and Java 25.
- Following explicit user EULA acceptance, exact Paper 26.2 build 129 started,
  restarted and started from a restored test-world/config/SQLite backup; all
  stopped cleanly. Plugin bd8f4ce enabled and initialized schema 9 successfully.
- Verified complete source Git bundle and archive, all 68 restored world/config
  files, SQLite integrity, and recovered-source wrapper build: 117 tests passed.
- Only new disposable temporary directories outside OneDrive were used. No
  production save, existing world, main branch, remote push or merge changed.
- See PAPER_SMOKE_TEST.md for download recovery and non-fatal Windows warnings.
  Hosted CI and integrated population playtesting remain pending. Phase 1 stays 70%.

## Saved calendar foundation
- Branch `codex/civilization-calendar`; schema 10 adds a saved elapsed-time anchor,
  fractional month and queued demographic target. Existing identities/history retained.
- Pure calendar conversion handles backward wall clocks, explicit elapsed caps and
  overflow. Queued targets survive demographic failure and bounded restart catch-up.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 129 unit and two integration tests
  (131 total), zero failures/errors/skips. Fourteen new clock cases cover restart,
  snapshots, fractional time, rollback, caps, overflow and schema-9 upgrade retry.
- Calendar APIs remain explicit opt-in; runtime scheduler/configuration and village
  import/projection are pending. Phase 1 remains 70% until the whole calendar gate passes.
- User approved automatic recognition of both natural villages and player-built
  areas containing beds and villagers. No server/world/production DB changed in
  this calendar batch; no merge or push.

## Automatic village observation foundation
- Branch `codex/village-observation`: immutable UUID-tagged observations and bounded,
  deterministic populated-bed cluster candidates. No Paper/chunk/SQL calls.
- Editable detection inputs, duplicate/budget rejection, nearest-bed ties and partial
  observation safety verified by seven new tests. Windows wrapper clean verify passed
  136 unit and two integration tests (138 total), zero failures/errors/skips.
- Runtime importer/identity matching, household observation and projections remain
  pending. Existing listener unchanged; schema 10. Phase 1 stays 70%.
- User approved configurable prototype adult partnerships; implementation upcoming.
  No new world/server/production DB changes, merge or push in this batch.

## Approved partnership prototype
- Branch `codex/population-partner-formation`: explicit pure pairing of unrelated,
  unpartnered living adults sharing a home. Enable/minimum-age inputs configurable;
  repeatable UUID order, retained deceased partners, no invented existing child ancestry.
- Seven new tests, including birth integration. Windows wrapper clean verify passed
  143 unit and two integration cases (145 total), zero failures/errors/skips.
- Import/monthly runtime integration remains pending; existing demographic replay 3
  is unchanged. No schema change or actual DB/world/server change in this batch.
- User approved permanent world UUID and saved bed/villager identity matching;
  overlapping existing towns will be diagnosed instead of automatically merged.
- Phase 1 remains 70%; no merge or push.

## Permanent village identity storage
- Branch `codex/village-identity-storage`; schema 11 adds world UUID/name evidence,
  settlement bindings, bed ownership and entity/resident links, preserving all old data.
- Repeat visits/renames match identities; conflicting town evidence is reported and
  never merged. Bindings cannot silently change owners. Legacy rows are not auto-bound.
- Migration uses bound world UUIDs and prevents same-folder-name cross-world moves;
  old unbound rows retain separated legacy boundaries.
- Nine new temporary-DB tests. Windows wrapper clean verify passed 152 unit and
  two integration cases (154 total), zero failures/errors/skips.
- Runtime importer, observer, projections, calendar scheduling and diagnostics remain
  pending. Phase 1 stays 70%; no actual DB/world/server changes, merge or push here.

## Schema-11 Paper follow-up
- Source 62550ee/154-test JAR enabled on the authorized disposable Paper server;
  schema 9 upgraded to 11, integrity verified and clean stop confirmed.
- Signature audit found no common GitHub/AWS credential/private-key patterns in
  reachable tracked history; no remote publication. Hosted CI remains pending.
- Population import/scheduling/projection remain upcoming; Phase 1 stays 70%.

## Atomic village enrollment and hosted CI
- Branch `codex/village-enrollment`; schema 12 adds optional bed/home links.
  One transaction imports names/ages/identities, shared homes, housing evidence,
  approved prototype partners and authoritative headcount. Failure rolls all back.
- Eleven new temporary-DB cases include restart/rename, partial observations,
  deceased identities, final-write failure/retry, protected old/overlapping homes
  and actual calendar-driven saved births independent of visible census.
- Windows wrapper clean verify passed 163 unit and two integration cases (165 total),
  zero failures/errors/skips. Observer/runtime scheduling/projection still unwired.
- User approved pushing exact snapshot e85416e on `codex/phase1-foundation-ci`.
  Both hosted Windows and Ubuntu jobs passed:
  https://github.com/blueninjas24/Civilizations/actions/runs/37725399200
- That snapshot has 154 tests; newer enrollment work remains local. Main unchanged,
  no merge. Phase 1 now 8/10 (80%), with the CI/Paper/recovery gate verified.
- No actual DB/world/server change in this enrollment batch. See VILLAGE_ENROLLMENT.md.

## Dedicated population owner and timer
- Branch `codex/population-runtime-clock`: PopulationSession owns JDBC and imports
  on one thread, queues immutable observations, publishes immutable snapshots and
  runs bounded calendar catch-up on a real fixed-delay timer.
- Editable documented test cadence/cap, protected saved settings, historical import
  delay, explicit 64-operation overload rejection, failed-write retry and safe shutdown.
- Ten new temporary-DB tests. Windows wrapper clean verify passed 173 unit and
  two integration cases (175 total), zero failures/errors/skips.
- Plugin wiring/config/commands/observer/projection still pending, so Phase 1 stays
  80%. No new world/server/actual database changes, merge or push in this batch.

## Paper calendar and read-only diagnostics
- Branch `codex/paper-population-clock`: asynchronous owner-thread startup, strict
  editable config, main-thread initialization polling and shutdown. No shared JDBC.
- Read-only `/civ debug status` and `/civ debug settlement <uuid>` inspect immutable
  snapshots with operator/console permission. No mutation or reload command.
- Old census listener retained but unregistered to protect authoritative residents;
  automatic observer/import/projections still pending.
- Windows wrapper clean verify passed 188 tests (186 unit, two integration), zero
  failures/errors/skips. Thirteen new config/permission/command/diagnostic cases.
  Initial sandbox build failed to resolve OneDrive target with AccessDeniedException;
  expanded-access build passed without build/OS/location changes.
- Backed up marked disposable schema-11 DB before deployment. Pinned Paper 26.2-129
  enabled the JAR, created config, advanced a real month, served status/validation
  messages, stopped and restarted at saved month 1. Second run advanced to month 3
  and stopped cleanly. Only approved temporary dev files changed; schema now 12.
- No populated village/player scenario, production change, merge, push or main
  modification. Phase 1 now 9/10 reporting milestones (90%).

## Automatic Paper village observation
- Branch `codex/paper-village-observer`: main-thread incremental chunk snapshots,
  loaded-entity guards, bed-head deduplication and coverage/border completeness checks
  now feed the existing SQL-owner importer automatically. No chunk tickets, POI
  searches, block writes, entity mutations or loaded-census replacement.
- Configurable budgets and read-only `/civ debug observation`; scan/import/cooldown
  queues bounded. Partial windows invalidate housing confidence. Indivisible API calls
  mean the time budget is a target, not a hard deadline; seed fairness needs profiling.
- Windows wrapper clean verify passed 197 tests (195 unit, two integration), zero
  failures/errors/skips. Nine new cases cover scan budgets, negative heights,
  bed halves, boundary closure, observer config and command diagnostics.
- Real Paper caught negative-height snapshot section indexing. Inspected pinned
  implementation, added adapter/regression test and reran successfully. Rejected
  initial frame performed no import.
- Disposable fixture automatically imported two beds/two adult villagers as one
  settlement/home with housing capacity four. Repeat loaded scans preserved people.
  Removing console test tickets unloaded the area; calendar continued to month 27
  with two saved residents and a partial-frame diagnostic, without loading it again.
- Final JAR restart/reload/re-observation retained exact settlement, household,
  resident and entity-link IDs. Read-only DB comparison: ages advanced exactly eight
  months with clock 27 to 35; two residents, schema 12, integrity ok, no FK errors.
- No actual client play, NPC projection or demographic balance claim. Test server
  stopped and console loading tickets removed. Only approved disposable dev files
  changed; no schema change, production data, merge or push. Phase 1 stays 90%.

## User workflow
`docs/PROGRESS.md` tracks verified roadmap milestones in plain English. Current snapshot: Phase 1 has ten of ten prototype milestones complete; Phase 2 has four of four prototype reporting groups complete; Phase 3 has one of four groups complete with automatic farm businesses. Phases 4–9 have none complete. These counts are not time estimates or release-readiness percentages. Future batches update it alongside this status file.

This ChatGPT thread is product/design HQ. Codex is implementation agent. It may proceed autonomously on scoped engineering work, committing to feature branches, but must escalate consequential design decisions and destructive operations. Preserve the full nine-phase roadmap.
