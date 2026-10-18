# Automatic stone quarries and sandpits

Implementation checkpoint: verified October17 by the Windows Wrapper build,
focused final-source tests and an actual isolated Paper26.2 quarry production chain.
See PROJECT_STATUS.md for exact results and remaining Phase3 boundaries.

## What happens

Villages can consider an existing exposed stone/deepslate or sand/red-sand patch
for a new extraction business. This produces finite saved materials over time;
it does not remove blocks, excavate terrain or claim a player's building. Actual
digging is Phase4. Known placed/formed/moved/removed sources are excluded through
the existing mutation tracker; old block origins cannot be proven, as approved.

A complete, current survey, actual local demand, real protected startup money and
a qualified adult owner are required. The owner takes one real first worker job.
Existing productive mines/quarries and their owned stock count toward supply;
stone byproducts can therefore make a separate quarry unnecessary. No customer or
profit is invented to make every village open every industry.

Real customers include funded workshop inputs through staffed warehouses, or
current public practice-material orders after village stock, deliveries and
protected spending. Public customer forecasts account for the proposed startup
debit. Existing customers still compete for finite budgets and supplies. Default
craftsman recipes use stone. Sand needs an actual configured customer; this batch
does not introduce a glass industry or construction demand ahead of its phase.

Unqualified prospective owners may earn mining experience through the existing
paid-in-materials study rules. Eligibility, limits, age, speed and practice costs
remain configurable. A vanished resource/customer or unavailable protected funds
cancel linked study rather than create free progress. Both manual qualification
and earned experience remain saved. Ownership alone never grants experience.

## Settings and existing saves

New economy.yml files enable quarry-observer, quarry-founding and quarry-resources.
Older installed files are never rewritten; missing sections opt out. Also retain
the existing industry-runtime, industry-supply, warehouse-supply, startup-grants,
career-study and study settings needed for your chosen production/customer chain.
Add quarry/sandpit to appropriate staffing and career lists to enable those paths;
work-learning can map them to mining. The owner needs the configured mining skill.

quarry-observer uses independent loaded centre/cardinal snapshots, bounded cell
work and complete stone/sand pairs. Missing chunks or oversized/incomplete evidence
never authorize a partial startup. It does not force chunks to load. Its current
frames are separate from coal/iron AND keyed to each scanned chunk: a newer empty
neighbor never invalidates a prospect elsewhere. A newer empty original frame
does remove its current candidates. Only unexpired complete frames contribute;
old sightings never restore them. Snapshot age survives SQL queue delay.
Each chunk supports up to512 coordinates; reviews consider up to64 current frames
and16,384 current coordinate rows. Stale historical frames do not use that budget.

quarry-founding controls village/patch/founder review budgets, resource age/range,
minimum age and mining skill, workers and startup/working capital. It shares the
existing grant limits and food-money protection; no new budget or money source.
quarry-resources sets finite reserve batches per observed block, output per batch,
work per worker and site work capacity. New founding saves those reserve/yield
terms; changing the file never refills or rescales old deposits.

Defaults: stone64 reserve batches/block,3 units/batch,6 batches/worker,32 site
batches; sand64,4,12,32 respectively. These are editable prototype values, not a
one-block/one-item rule or a balanced-release promise. Tool upkeep remains under
the existing separately editable business-tools industry list.

Business-only sand price defaults to100 hundredths/unit and storage weight to1.
Do not add sand to public starting-goods or village price catalogues merely to
turn on business trading. Existing warehouses gain an audited EMPTY sand slot only
when finite quarry work, configured sand storage and an actual sand-input recipe
are opted into. Existing units, capacity, cash, original terms and saved unit
weights remain unchanged. A previously audited slot disappearing requires review.
The same preserved empty-slot helper retains existing bakery receipt identities.

## Saves, diagnosis and remaining limits

Schema91 adds five empty tables: quarry_opportunities, quarry_surveys,
quarry_survey_members, quarry_founder_reviews and quarry_founder_courses. It changes
no existing table or row and creates no resources, grants, courses or history
retroactively. Actual formation, source claims, stock opening, real funds and the
first job commit in one transaction. Exact retries cannot refill or double-charge.

/civ debug observation shows the stone/sand observer state. /civ debug status
includes automatic quarry/pit founding's latest reason; /civ debug businesses <ID>
shows saved businesses. Discovery, ownership qualification, current demand, food
funds, staffing or retained storage support may explain why nothing opens.

Absent worlds and server downtime pause active simulation. Already-loaded worlds
can continue saved data-only production without forcing their chunks. Removing
one supporting block loses only its remaining saved share, preserving extracted
goods and the rest of the deposit. Closed firms/sites/history remain recorded.
This does not complete forestry, broader craft recipes, construction or full P3
specialization and long-run acceptance. Human survival balance remains playtesting.
