# Livestock care and business succession

The live herd clock now applies the existing approved work-learning rules to
actual animal care. This closes a qualification gap: a caretaker can start with
basic adult-care eligibility, earn farming experience through real work, and
later qualify for ownership or employee succession. Family-first succession,
real owner qualifications, unlimited ownership and one worker job remain intact.
Ownership alone earns no care experience.

`work-learning.industry-skills.livestock: farming` enables the mapping. The
existing gain, ceiling and worker-review limits apply; there is no new free
training grant or separate pace setting. New defaults include the mapping.
Installed files are preserved: add that line to opt existing settings in, with
the server stopped. Removing the mapping or disabling learning stops future
awards without erasing earned skills.

`LivestockLearning` uses the newly saved herd period's actual `work_used`. No
work means no award: empty feed, full output storage, absent worlds, untrusted
capture and confirmed-away animals cannot manufacture learning. Awards require
current qualified living employees in the same town. At least one real work
unit is required per learner; scarce work rotates among eligible workers in
stable ID order. Exceeding the review budget defers all awards, rather than
silently awarding only a partial page. Existing experience above a new lower
admin ceiling is retained.

Schema 89 adds `livestock_learning_reviews` and `livestock_learning_entries`.
Reviews reference real herd periods; entries record each learner's before/after
experience. No prior herd, animal, goods, owner, skill or history is reset or
reclassified. Old periods receive no retroactive experience. Learning, feed,
products, births and the industry checkpoint commit or roll back together;
restarts and retries cannot double-award. The additive migration is transactional.

Focused tests cover paid care/replay/restart, disabled/capped learning, rollback
after skill writes, preservation and failed upgrade from populated schema 88,
and a real employee earning the business's founder qualification before inheriting.
Full clean checkpoint passed 1,816 tests. Final affected package checks passed
113 tests after the resale-default correction. Packaged Paper 26.2 build 129
verified real owner-thread care/skills/audits, populated schema88 upgrade, restart
and absence/capture-gap pause with conserved cash/goods. See PROJECT_STATUS.md
for exact commands, times and preservation scope. Human server untouched.

This addresses qualification and succession for the operating herd loop. It
does not implement slaughter, pen donation or bakery milk/egg consumption.

## Producer stock resale

New default liquidation values cover grain100, milk300, eggs100 and sand100
hundredths per unit. These are editable abstract business-sale values, not player
retail quotes or minted money. Add them under
`automatic-business-resale.stock-value-hundredths-per-unit` in an existing file;
installed settings are not rewritten. Unknown custom goods still defer valuation
until an admin supplies a value. Accepted historical prices remain unchanged.
A funded business-sale test preserves the exact goods and total real money.
