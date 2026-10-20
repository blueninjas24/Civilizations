# Automatic free-range livestock founding

The approved bounded recognition rule connects loaded natural cows/chickens/sheep to
paid dairy/chicken/sheep businesses. This is Phase 3 operation of existing animals;
it does not build pens, move animals or grant rights over player buildings.

## Recognition and protection

`PaperLivestockAdoption` reviews saved living villages in a bounded round robin.
It reads only already loaded chunks and complete entity frames. A small group
must contain the configured minimum adults of one species, remain close to the
village, and pass repeated complete local barrier scans. Every initial animal
must have positive `NATURAL`/`CHUNK_GEN` origin, no name, leash or protective human
breeding/origin metadata. Unknown/command/egg/bred origins are excluded.

Nearby fences, gates, walls, doors, trapdoors, iron bars and two-high solid
barriers refuse clearance. Unknown chunks refuse local clearance. This is a
conservative local check, not a claim to have detected every distant enclosure:
large unmarked player pastures can extend beyond it. Use the explicit confirmed
`[Civ Protect]` controls in PEN_CONTROLS.md for persistent protection of supported
pens. Both saved ownership and saved active area protection are checked again
in the startup transaction; new animals entering a saved protected area are
excluded even if absent from its initial roster.

The final main-thread capture checks that the same bodies remain alive, eligible
and at the checked positions. Relevant block edits, explosions, piston changes,
player interactions, breeding and chunk unload invalidate the in-flight proof.
Staged UUIDs reach the existing product/death guards before the SQL handoff;
confirmed ownership is published before returning success. No Paper operations
run on the SQL owner. Arbitrary third-party direct world mutations and JVM crashes
are not a cross-system atomic guarantee; the existing capture-gap fence remains.

## Real paid opening and feed

`LivestockInvestment` rechecks the present permanent world, living settlement,
nearest living village, ages, origin, local distance, saved ownership/protection,
open-herd allowance, real founder farming skill and an available qualified adult.
Ownership has no artificial business-count limit. A founder who already works
elsewhere may own the herd if another eligible adult can take its worker job;
no resident receives a second worker job or free skill.

Animal ownership, site, firm, one employment episode, real startup grant, empty
store and initial age cohorts commit together. Food shortages, protected village
money, shared grant limits, invalidated evidence or a later SQL failure leave no
orphan claims or partial business. Retried/previously owned groups cannot claim
another grant. Existing closed businesses and original ownership stay historical.

The empty herd store contains only grain and its species' product. No feed,
milk/eggs or free buckets are supplied at opening. The existing livestock runtime
buys feed with business cash and waits for real cargo before producing/breeding.
Local demand now includes funded staffed herd feed, subtracts held/incoming stock
and respects work, storage, radius and trusted-capture limits. Warehouses may buy
real grain from hybrid crop farms. Registered herds are supported producers, but
only actual milk/eggs qualify as their sale output: purchased feed is not a new
producer resource. Other unknown stored industries still require explicit review.

## Editable prototype settings

New installations include `livestock-adoption` in economy.yml. Installed files
missing the section opt out and are not rewritten. `livestock`, `livestock-runtime`,
industry/economy and applicable grant/supply policies must also be enabled.
Stop the server before editing, then restart; invalid edits fail validation.

Defaults: village radius64, clearance radius8, two adults, at most8 initial
animals, 30seconds between positive repeated proofs, one scan start per100ticks,
one entity chunk and512 clearance cells per tick. Costs are500 startup plus2000
working capital in hundredths of Trade Value, drawn from the real protected
village budget. Capacity16 heads, owner farming experience1, adult worker farming0,
one open herd per species per village. Every setting is typed and bounded.

Bounds are conservative: terrain/ownership uncertainty can delay a business;
natural animals are not guaranteed near every village. The village page currently
supports1024 living settlements and reports overflow rather than silently dropping
others. Observation memory is bounded to256 groups; stale groups expire.

Physical slaughter without duplicate drops, authorized pen donation/ownership
transfer and bakery milk/egg consumption are separate remaining integrations.
Neither this document nor a successful startup marks those complete. No schema
change or database reset is required; existing schema88 identities/assets survive.

## Verification

Affected Wrapper verify passed 364 unit tests plus 2 packaged integration tests;
a final scanner-guard package check passed 14 unit plus 2 packaged tests. Both
had zero failures/errors/skips. The actual packaged observer and PopulationSession
also passed on guarded disposable Paper 26.2 build 129 with natural cows: a real
fence blocked adoption; repeated clear observations after its removal opened one
paid dairy; a named cow stayed excluded. Exact animal identities, real treasury
and startup capital persisted. Test bodies/blocks were restored/removed; the
human server was untouched. These are synthetic setup/live adapter checks, not
a claim of a new human-client playtest or completed slaughter/pen donation.
