# Qualified business succession

Approved order: eligible industry-qualified family first, then a qualified current
employee. Relatives include recorded partners, parents/children and shared-parent
siblings; shared housing does not invent family. Multiple eligible candidates use
stable UUID order as a deterministic prototype tie-break, not a claim about seniority.

BusinessRepository.findFounding returns original accepted founding terms with the
founder as original owner. findBusiness/findBusinesses return current ownership.
The immutable founding input can replay without a second startup charge after an
owner transfer. Current industry operations and diagnostics read current ownership.

BusinessSuccession.succeed requires a deceased saved owner, local living recipient,
the saved founder age/skill requirements, and no open job in another industry.
It neither teaches skills nor creates employment, funds or goods. Existing jobs,
stores, cargo, sites, business UUID and founder persist. Existing-world runtime
refresh attempts succession only for present worlds. Failed audit writes roll back
the ownership update. Exact accepted operations replay; changed terms are rejected.
No eligible successor leaves the deceased ownership unchanged pending resale work.

Schema 38 adds business_ownership and an indexed per-business event history; older
rows/accounts remain unchanged. This is a transactional additive migration. The
37->38 failure/retry test keeps founding and treasury intact.

Family estate funds, business sale, configurable buyer withdrawals, sale deadlines,
closure/competition and skill development remain upcoming. No physical building
change/reuse is implemented here; Phase 4 handles suitable permitted vacancies.

Verification October 9: 53 focused tests and Wrapper clean verify 698 passed;
six new temp SQLite cases. Actual Paper PopulationSession mine fixture verifies
qualified heir, one ownership event, immutable founder, finite production, world
pause and restart. Fresh/main integrity and foreign keys healthy. Initial probe
read-only assertion connection lacked FK enforcement and was corrected before the
successful native run. No block writes/tickets or user-server mutation.
