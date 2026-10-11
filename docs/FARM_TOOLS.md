# Farm tool work and public replacements

Working village farms now create an actual reason to buy blacksmith tools. Food
still enters village supplies directly; equipment is paid from the approved
food-protected village upkeep budget in FARM_TOOL_FUNDING.md. No private farm
income, household salaries, fabricated customers or free tools are introduced.

New economy.yml files include:

```yaml
farm-tools:
  enabled: true
  startup-grace-periods: 3
  work-periods-per-tool: 8
  missing-tools-output-percent: 50
  suppliers-per-review: 8
```

Grace and service count **productive food periods**, not elapsed time or the
civilization calendar. Normal food capacity still depends on actual farm workers
and observed fields. After grace, missing equipment reduces output by the editable
percentage, preserving fractional food output between periods. Zero output does
not consume a kit, wear its service or award work experience.

One delivered toolkit begins service on the first positive work period needing
it. Its stock is consumed once and saved service wears only with later productive
work. A replacement becomes due at one remaining service period, allowing delivery
without an avoidable interruption. Held or paid incoming equipment blocks another
order. No store/catalog/cash is silently converted: unfamiliar old farm stores keep
their existing food behavior and report that a transition is needed.

After committed work, a bounded rotating local supplier page looks for actual
blacksmith/warehouse tool stock under the existing owner-operation rules. A real
public invoice must pass current farm work, staff, world and food/budget checks.
It uses the configured local delivery delay and food-period haul capacity. Cargo
cannot be used early. Regional transport, pricing margins and a unified hauling
budget across different industry/food cadences remain later work.

Paid arrivals, tool use/service/fraction, food production, experience, needs and
the food checkpoint share the food transaction. Audit failure rolls them all back.
Purchases have their own conserved payment transaction after food commits: a crash
there cannot undo food or duplicate its work, and still-due equipment can be reviewed
on the next productive period. SQL faults propagate; expected budget/food refusals
produce wait explanations. `/civ debug` farm diagnostics include actual food output,
tool use, remaining service and the latest order decision.

Absent worlds and server downtime earn no farm periods or deliveries. Disabling
new upkeep preserves saved service, kits and histories; already paid cargo can
still arrive under an active farm food clock. Re-enabling resumes saved service.
Settings retuning follows existing economy forward-transition rules: finish any
earned backlog under its old settings before changing the file and restarting.

Missing legacy sections stay disabled. Edit while stopped and restart; existing
server files are never overwritten. Enabling wear before local tool suppliers exist
can slow farms after grace: this is an experimental scarcity setting, not a promise
that a new village automatically opens a blacksmith. Automatic skilled founders,
first nonfood industries, wider industries and specialization remain unfinished.

Schema 54 adds empty `farm_tool_work` tied to actual food-period receipts. Service
uses the existing business_tools state table; neither migration backfills goods,
cash or work. Temporary SQLite tests and the test-only FarmToolsProbe verify food,
funding, conserved manufactured tools, rollback, restart, absence and disabling.
The probe operates through actual PopulationSession owners on the marked disposable
Paper fixture, without world/entity edits or forced chunks. See DEVLOG.md for results.

Approved real spare-business contributions cover genuine invoice cash shortfalls
from the actual supplier under editable reserves and caps. Contribution and real
goods/payment share one transaction; no income is minted. This does not guarantee
unlimited work or net profit. See UPKEEP_CONTRIBUTIONS.md for scope and verification.
