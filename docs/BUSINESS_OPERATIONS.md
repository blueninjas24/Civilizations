# Existing staff after an owner's death

User approved keeping staffed businesses operating while the family seeks a buyer.
This opt-in operation rule supports the pending succession/resale period; automatic
listing, valuation, deadline and closure remain separate unfinished work.

BusinessOperations reads the current owner and saved jobs. A living local owner
retains normal operation. With the new rule enabled, a deceased owner permits work
only if an actual living local employee meets the saved worker age/skill requirements.
That employee need not meet the higher founder requirements and is not promoted.
A moved living owner is not treated as a deceased owner. No qualifying staff means
no operation. Existing availability, supplies, storage and online-world constraints
still apply; production and input purchases use the existing conserved transactions.

Mines and workshops apply this availability check to their timed work. A staffed
warehouse with a deceased owner may supply an actual workshop order; an unstaffed
one cannot automatically sell under this rule. Already paid cargo still arrives
through its existing clock and owns exactly the same goods. Continued operations
keep their money in business accounts, without new estate credits or grants.

Automatic farms retain qualified existing labor after death, reconcile the deceased
worker's job and avoid automatic replacement hiring until a living local owner
returns through qualified succession/sale. Actual crop capacity still limits output.
Current-owner reads prevent the original deceased founder from blocking an inherited
farm's later staffing. Founding history and original startup charge stay unchanged.

In plugins/Civilizations/economy.yml:

```yaml
business-operations:
  enabled: true
  continue-staffed-after-owner-death: true
```

Set the second value false to pause affected firms until a living local owner takes
over, retaining stock, funds and employment history. Disabled/missing old sections
retain the legacy behavior (mines/workshops pause, field farms can continue), without
rewriting existing settings. Admins opt in by adding the complete section. Malformed
keys/types fail before database startup. Stop/edit/restart. Enabled farm rule changes
are recorded in the existing forward economy tuning signature; old period history
does not change. Schema stays 39; no new balances, tables or physical changes.

This is not a closure/competition system, a payroll system, a new owner identity or
an implementation of unattended business expansion. Buildings/reuse stay Phase 4.

Verification October 9: focused 91 passed; Windows Wrapper clean verify passed
725 tests (723 unit/two packaged integration), zero failures/errors/skips. Ten new
tests cover existing-staff mining, optional pause, moved/underage refusal, paid
staffed warehouse/workshop conservation, unstaffed supplier refusal, reduced farm
labor, current-owner staffing after inheritance and strict config validation.
Initial farm assertions counted the deceased founder as still working; corrected
expectations to match actual employment, without changing application behavior.

Real Paper session fixture passed two staffed mines, including a deceased owner's
mine whose employee has worker skills but not founder skills. The owner remained
deceased, with no invented promotion; three coal were produced and its reserve
fell five -> two. Other mine inheritance, paid sale/replay, pause/restart and
conserved 8500 treasury + 750 business cash + 250 estate also passed. No world
writes/tickets or user-server changes. Native stopped normally, probe archived,
settings restored, pre-existing dev assets retained; integrity/FKs healthy.
