# Livestock newborn physical identity handoff

## Contract

An already paid, saved simulated birth may claim one prepared native cow/chicken
UUID before that entity enters its present origin world. The SQL owner commits
the ownership provenance, immutable physical alias and current CLAIMED pointer
together, then publishes the protected animal ID before completing its future.
The main-thread caller must separately prove safe loaded placement. Claiming a
body adds no logical head, goods or money; the original birth transaction did
that accounting. Existing animal/resident UUIDs cannot become newborn bodies.

Only the exact current CLAIMED body can become ACTIVE. Missing/unloaded bodies
never authorize replacement. A positive same-operation proof that a prepared
body was never added may mark it FAILED; another prepared UUID can then claim
the same logical birth. Retain all failed aliases and ownership provenance.
Stale callbacks cannot activate a replacement or revive a confirmed death.
An uncertain handoff after a crash stays claimed for reconciliation, not retry.

Actual captured death of a current CLAIMED/ACTIVE offspring atomically marks it
DECEASED and records an idempotent loss against that birth's saved maturity.
The herd loses exactly that adult or young cohort member immediately; the next
production checkpoint applies the receipt once. Other children, original animals,
cash and stock are untouched. Ordinary death drops are unchanged. Missing animals,
failed candidates and old aliases do not become deaths. This is not slaughter.

The owner accepts new claims only with trusted death-capture history and the
actual origin world present. Exact committed recovery can remain readable through
a capture gap, without clearing that gap. Older anonymous births block new claims
until explicit reconciliation; coverage counts alone do not prove physical state.

## Migration and scope

Additive transactional schema86 creates livestock_birth_entities,
livestock_birth_projections and livestock_birth_losses, initially empty. All old
birth records, headcounts, stock, ownership and financial histories are preserved.
No automatic backfill, reset, animal spawn or replacement accompanies migration.

This batch supplies the durable SQL/application handoff and confirmed child-loss
accounting. The excluded Paper fixture exercises real native bodies and deaths;
it is not the production spawning/reconciliation scheduler. Automatic adoption,
safe loaded placement, age-lock reconciliation, the approved working-radius rule,
scheduled feed/output and broader bakery ingredients remain separate work.

## Verification

Added24 cases across projection, offspring-loss and SQL-owner tests. Focused
affected migration/newborn suite:163 passed. Windows .\mvnw.cmd clean verify -B
-ntp passed1,718 tests (1,716 unit/two packaged integration), zero failures,
errors or skips;11:59, finished2026-10-16T23:14:31-04:00.

Guarded Paper26.2 build129 probe passed23:15:00: actual native prepared cows,
ownership publication before addEntity, positive unspawned retry, real child
death, preserved sibling/stock/cash and SQL-owner restart. The test save upgraded
85->86 with143 old tables preserved,139 exact; four ordinary clock/runtime/
capture/ore metadata changes and no aging. New main tables empty; integrity/FKs
clean. Fixture stopped, original settings restored, helper archived; backups and
synthetic database retained. Human server untouched. This is not an automatic
spawning/adoption scheduler, general reconciliation or actual crash test.
