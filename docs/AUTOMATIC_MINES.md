# Automatic observed-resource mine founding

This Phase 3 adapter starts a saved, finite coal or iron operation when **existing**
businesses establish a real local market. It does not construct an entrance, dig,
create ore, grant land rights, or bootstrap an entire industry chain.

## Preconditions and transaction

- Initialized village in an active permanent world; completed food assessment
  covers current living residents, with no unmet needs.
- Complete, current, trusted exposed coal/iron survey. Only latest-frame members
  qualify; placed, removed and previously claimed coordinates remain excluded.
- Actual affordable workshop input deficit, after owned/pending/warehouse supplies.
- Existing operating warehouse with qualified staff, money for the relevant
  purchase and storage space. No imagined consumer or free buyer capital.
- Unemployed qualified local adult, not currently studying. Ownership of another
  firm is allowed; simultaneous worker jobs are not.
- Resource anchor within configured horizontal/vertical range and not an existing
  saved workplace or bed. This is a data anchor, not a construction suitability
  assessment or permission to excavate player property.
- Existing operating mine stock and bounded potential output cannot already cover
  that input buffer. This planning estimate uses qualified staff, finite reserves
  and free storage; it is not owned stock or a guaranteed delivery and does not
  model tool slowdown in the prospective estimate.
- Approved real startup grant fits shared period/lifetime limits and protects the
  village's food funds. No new treasury balance is invented.

Demand/resource selection, eligibility, site, business, finite coordinate shares,
store, first job, grant expense and working capital save in one transaction. An
injected receipt failure or immediate ore-trust change before commit rolls it all
back. Actual faults remain visible; ordinary ineligible opportunities explain why
they defer. Production starts through the existing timed industry runtime, not
through founding. Stable coordinate-derived IDs and retained source claims prevent
revisit/restart from creating duplicate firms or refilling reserves.

## Editable defaults in economy.yml

`automatic-mines` is enabled in bundled defaults; existing files without that
section remain disabled and are not overwritten. Copy the section deliberately
before enabling it in an older installation. Shared `startup-grants`,
`industry-runtime`, `workshop-supply` and `warehouse-supply` must also permit the
operation; their existing protections still apply.

Defaults review two villages per pulse, eight connected resource patches and up
to 64 qualified founder candidates; survey lifetime 60 seconds; range 96 blocks
horizontally/48 vertically; minimum age 216 months, mining experience 10; two
worker places; startup expense 500 hundredths and working capital 2000 hundredths.
Patch choice is seeded by village identity, simulation seed and saved food period.
Village review pages wrap; this page cursor is transient. Accepted assets and
identity are persistent. No chunk is force-loaded.

Each observed block uses the existing configurable 150 extraction-batch default.
Automatic coal yields two coal units per batch; iron yields one iron unit. Thus
defaults yield 300 coal or 150 iron per supporting block over time. Configured
non-coal/iron byproducts (normally three stone per batch) remain. A coal survey
cannot create iron, nor an iron survey coal. Saved deposits keep opening terms;
editing defaults never refills or reprices an existing deposit. Source removal
continues to retire only that coordinate's remaining share.

## Remaining Phase 3 work

New villages do not yet automatically train first founders or found their first
warehouse/workshop. Vacancy training serves actual existing jobs. Other industries,
broader specialization/competition, balanced long-running chains and exceptional
firms remain. Mine entrances, shafts, expansion and physical terrain changes are
Phase 4. This component does not complete an entire Phase 3 reporting group.

## Validation for this batch

Windows Maven Wrapper clean verify passed 990 tests (988 unit, two packaged
integration), with zero failures/errors/skips. Twelve new temporary-SQLite cases
exercise startup, rollback, final trust loss, stale/absent/future evidence,
qualification, funded buyers, anchor bounds, food protection, growth, restart,
legacy opt-out and the actual SQL-owner timed flow; strict config testing is added.
The native AutomaticMineFoundingProbe passed on the marked disposable Paper server
through PopulationSession: one 2500-hundredths grant, 500 expense, 2000 capital,
eight finite extraction batches and sixteen coal. Absence/restart conserve assets.
Main fixture original tables were compared; its only resident change was ordinary
aging, with no business/financial/source/history changes. No human server deployment.
