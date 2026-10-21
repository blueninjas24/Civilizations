# New households for already unassigned adults

Implemented under the October20 approval. Complete permitted housing reviews can
form a one-person household for an eligible saved adult, or keep two already
recorded, eligible unassigned partners together. Existing households are never
split or merged. No resident, romance, parentage, money, food or bed is invented.
Normal inspected-vacancy assignment remains separate and must fit the whole group.

## Editable pacing

config.yml household-formation defaults:

- enabled: true
- maximum-per-month:2 households per village civilization month (allowed1..64)
- maximum-candidates:128 founder candidates per pass (allowed1..1024)

Existing config files are preserved; these defaults apply when the section is
absent. Add the section to tune it and restart. Invalid settings disable only
new-household formation; existing housing/population continue. The adulthood age
comes from the existing population settings. No new salary/property economy.

Repeated building reviews share the same saved village/month allowance. Candidate
order and derived household UUIDs are deterministic. Formation requires the current
saved calendar month, current village/world identity and an owning SQL transaction.
The live caller already requires a present world and a complete current permitted
housing review. Unknown chunks, stale reviews, reclaimed homes and absent worlds
cannot trigger this work. Villages with no permitted housing review still wait.

Only alive adults without a household qualify. Both partners must be alive adults,
unassigned and in the same village, with unambiguous saved partner ties. An already
housed/distant partner or multiple living partners holds the case. A known living
unassigned dependent child also holds its parent; this prototype does not create an
orphaned child placement, split a saved family or infer missing parentage. Those
complex regrouping cases remain future work. Children do not form adult households.

## Persistence and housing authority

Schema111 adds household_formations and household_formation_members with permanent
household/member references, founder/optional partner, original world/village,
calendar month, timestamp and policy. Migration is additive and transactional;
no old residents, households, capacities, links, consent or histories are rewritten.
Older plugins correctly refuse a newer schema; rollback needs a matching backup.

Household creation, member assignment, known zero housing and formation history
commit together. A failure leaves no partial household or moved members. Existing
member history prevents duplicate formation on retry/restart. A fresh village scan
may refresh raw bed-group evidence but cannot erase a formed household's known
housing need. Active inspected homes still override zero with real usable capacity;
formation alone never counts as housing or a construction project. Household,
population and food accounting use the same effective capacity authority.

## Verification

Focused Windows Wrapper clean verify74 unit +3 packaged checks passed1:06 after
updating three historical migration snapshot SQL filters to exclude the new empty
tables. Earlier migration checks exposed those fixture expectations, not altered
old rows. After the scan-capacity safeguard, focused clean verify60 unit +3 packaged
checks passed54.475s, zero failures/errors/skips. Coverage includes single/partner
groups, shared month cap, stable restart identity, no new kinship/people, saved
family/child holds, transaction rollback, schema110 upgrade retention, strict settings,
known zero after raw-capacity refresh and actual inspected-vacancy assignment.
No new full-suite rerun is claimed.

Actual marked disposable Paper26.2 build129 passed: whole existing family retained,
one new adult imported separately and formed into its own zero-capacity household,
real bed/roof/access review, damage/repair, absent world, SQL-session restart with
one formation, then donor reclamation retaining all three people and history.
Restored144 fixture cells; server stopped/helper archived. Production/private fixture
DBs are schema111 with integrity ok and zero FK violations. First expanded helper
incorrectly supplied saved resident UUIDs as entity UUIDs; corrected only the helper
and repeated successfully. This is not human-client compatibility certification.
No actual user server/world/database was changed.
