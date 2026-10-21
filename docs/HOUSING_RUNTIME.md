# Live housing transition and repossession

October20 current addition: explicit NEW apartment declarations now feed bounded
inspection, confirmed donation and the normal whole-family runtime. Private units,
multi-room spaces, shared corridors and separate plots are editable before review.
Registered layouts are not revised. See APARTMENT_DONATIONS.md; older checkpoint
limits below describe their dated implementation stage.

The user approved preserving existing families/housing while gradually assigning
whole households to inspected donated or Civ-built homes. Normal construction is
still off. The current runtime uses explicitly donated registered housing only;
it never claims an unrecorded player building or teleports residents.

## Shared capacity authority

Schema110 adds household_capacity_sources and dwelling_bed_claims. Complete legacy
observations record each bed head's exact household contribution. Partial frames
retain the previous P1 rule: legacy capacity is unknown, not zero. Families and
resident IDs remain unchanged. Old raw capacity is not backfilled with guessed bed
ratios; it stays available until an explicit transition touches those beds.

Population and household food accounting now use the same HousingCapacity reader.
For a transitioned family, its current permitted, reviewed unit supplies capacity;
old bed-group capacity cannot return after a rescan, move or reclamation. Complete
known damage supplies zero capacity without deleting the household. Unknown evidence
preserves the last known review. Recorded reviews remain last-known state between
bounded observations; the30-second limit gates new assignments, not forced eviction
when a chunk is unloaded. Revoked/excluded permission supplies no managed capacity.

Exact saved legacy bed contributions are subtracted when a unit takes those beds.
If old accounting is ambiguous, the affected legacy capacity is unknown until a
complete rescan rather than invented or counted twice. Permanent unit bed claims
survive reclamation, including never-assigned donated homes, preventing legacy
scans from silently granting use again. Settlement/bed identity links remain.
Upgrade110 backfills claims from retained109 assignment history transactionally.

Village enrollment binds all observed bed identities but groups only legacy beds.
Complete and partial rescans cannot replace managed family capacity or create a
second household from its claimed beds. Known residents retain their families.
New unrelated people near managed homes remain unassigned instead of being inserted
into an established family. Assigning those ungrouped people is a later housing task.

## Automatic loaded-only review

PaperHousingRuntime pages at most8 building records from the SQL owner at a time.
Absent worlds and buildings lacking village-use permission are excluded. One survey
is active globally; two bounded passes compare classified housing evidence. Changed,
missing, unloaded or expired evidence cannot authorize a new assignment. All native
reads occur on the main thread and never force-load chunks. All JDBC is queued on
the existing owner. Accepted SQL work drains during shutdown.

The SQL review preserves a living incumbent legacy family when it fits the unit.
Multiple incumbent families are not merged or split, and an oversized family keeps
its legacy housing until suitable space is available. Otherwise an eligible known
overcrowded/unhoused saved family can occupy the vacancy, with stable deterministic
ties and at most4 assignments per reviewed building. Existing adequate homes are
left alone. Empty units require saved zero living members; entity visibility does
not establish vacancy. Broken homes can lead to another suitable saved vacancy;
the runtime neither constructs a replacement nor deletes anyone.

config.yml housing-runtime defaults: enabled=true, cells-per-tick=128,
review-seconds=15. Budget range16..256, interval5..600 seconds. Invalid housing
settings disable this runtime with a log warning while building controls remain.
building-protection.enabled=false also disables live donation/reclamation/runtime.
Existing files use the new defaults. Default native housing reads total at most384
cells/tick:128 inspection +128 donation +128 runtime. The runtime uses one budget
across both passes, not128 per pass. Large structures may exceed the30-second survey
window under a small budget and remain unverified; increasing the budget requires
performance judgment. At the maximum setting, combined reads are at most512/tick.

Two passes detect differing observations; they are not an atomic lock against every
other plugin or a proof of historical authorship. Advanced editor/write-boundary
acceptance remains a P4 gate. A later world edit is seen on a subsequent review;
there is no promise of instantaneous capacity changes.

## Donor repossession

Use /civ building reclaim <building UUID> in its saved world. The donor, or current
administrator, receives an expiring preview of assigned households and other legacy
families whose bed contributions are affected. Confirm through the separate button
or /civ building confirmreclaim <token>; cancelreclaim discards unsubmitted work.
No sign removal or ordinary click implicitly withdraws permission.

Confirmation checks current actor/world/permission, exact previous consent and the
same active assignment/legacy-family sets. Changed occupants require a new preview.
Permission withdrawal, permanent bed claims and assignment departures share one
transaction. Capacity disappears without reviving old bed counts. Retain residents,
family identity, contents, budgets, structure, donor and history. The building's
saved footprint remains protected from new Civilizations construction. Rehousing
requires another suitable permitted vacancy. No demolition, item movement, sale,
private title transfer, rent or landlord system is introduced.

The separate civilizations.building.reclaim permission defaults true; ordinary
players still cannot reclaim another donor's building. building.admin remains
operator-only. Accepted saves drain across disconnect/stop. Exact retries preserve
the original transaction; they cannot revoke a later redonation. One player has one
SQL-owner building-consent preview; starting another replaces the old preview.

/civ debug housing is an operator diagnostic describing the last global review,
assignments and empty-household releases. It is not a per-family address book.

Donors can use /civ building inspect <building UUID> for their own saved home's
family size/effective capacity alongside current bed/access/cover evidence. The
saved status is explicitly last-reviewed data, not a visible-entity count or an
atomic guarantee against later changes. Only the donor/current administrator can
read it in the saved present world. Donation or reclamation permission is enough;
plot-protection permission is not required or granted. Output is capped at8 units
and the existing global128 inspection cells/tick. Inspection changes no records.

## Verification and remaining scope

See DEVLOG.md for final commands/results. Temporary SQLite checks cover shared
capacity, scan/restart behavior, known/unknown bed provenance, whole-family vacancy
selection, damage relocation, upgrade failure/backfill and reclamation-impact races.
Paper adapter checks cover shared budgets, async draining, permissions, expiry and
separate confirmations. src/test/paper/HousingRuntimeProbe.java is guarded to the
marked empty loopback disposable server and restores its144 modified cells.

Apartment boundary editing, automatic physical construction, visible home-specific
villager pathing, unassigned-person household formation and broader editor/scale
acceptance remain unfinished. This batch does not complete all vacancy reuse for
businesses, every permission/write boundary or another whole P4 checklist item.
