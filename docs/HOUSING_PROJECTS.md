# Household-bound housing proposals

Schema113 adds a preparation API, not an automatic builder or a new player command.
Each frozen HOME draft may identify one whole household, its permanent village/world,
plan fingerprint, living residents, previous known capacity and creation time.
The same household cannot hold two active proposals. Only its own proposal makes
it wait; another family can still receive the next planner recommendation.

The SQL owner rechecks the actual saved calendar and current vacancy-first planner.
Unknown capacity is not homelessness. Stale/unreviewed permitted buildings hold
new construction; suitable vacancies must be reused. A plan must fit the entire
family. Historical active HOME sites without beneficiary records conservatively
hold all new proposals rather than guess where their funds belong. No old records
are rewritten. An existing project also follows its household's current village
when deciding whether that family already has a proposal in the same world.

Bind a new dry draft before its site, materials or native work. The operation
does not claim land, spend money, reserve materials, allocate labor, assign a home
or increase population capacity. Present-world, current site/protection and actual
resource checks remain separate prerequisites at subsequent boundaries.

Unused cancellation retains the frozen draft and binding history. Release its
unused site and return all owned material receipts first. Any paid-supply order
or prepared block requires a future reviewed recovery path; nothing is silently
deleted or refunded. Exact cancellation retries return the saved receipt; an old
binding retry never reactivates it. Cancelled drafts cannot reserve another site
or receive new material authority. A new proposal needs a new identity and review.

Current limitations: no automatic proposal scheduling, physical production writer,
completed-home activation, started-project cancellation or migration of historical
unbound projects. Completed-home lifecycle remains part of the construction gate.
The village retains ownership of future publicly funded homes under the approved
prototype rule. This record is a beneficiary reservation, not private title.

Verification: four temporary-SQLite cases in DwellingOccupancyTest cover independent
families, restart/history, reuse/unknown/stale/absent-world holds, cancelled-draft
land refusal, injected SQL failure and preserving112->113 migration. Focused Wrapper
clean verify passed55 unit and3 packaged checks in48.957s. No actual server database
or Minecraft world is used by these tests. Older plugin versions reject schema113;
back up before upgrading and do not downgrade a live save in place.
