# Funded local industry input demand

`WorkshopInputDemand` calculates a complete recipe-input buffer that an existing
workshop can afford and store with its actual qualified labor. It uses the existing
editable recipe, `industry-supply.input-periods`, and supply base Trade Value prices.
Owned inputs and paid incoming cargo reduce missing inputs. Pending goods remain
unavailable for production; this is an ordering forecast, not a production receipt.
New inputs and positive recipe output growth must fit alongside already occupied
and reserved storage. Funds must cover the whole missing bundle, not just its coal.
No workers, insufficient cash, unsupported/missing prices or insufficient space
create no new fundable input gap. Nothing is spent, reserved, generated or consumed.

`LocalIndustryDemand` reads actual existing saved workshops, founder/worker terms,
current living local qualified employees, stores, paid undelivered shipments and
available local warehouse stock. Owner/closure rules match existing business
operation policy, including approved qualified-staff work after owner death.
Only supported configured recipe industries participate. No new workshop or
finished-goods customer is imagined to justify input pressure.

Available warehouse goods cover customer gaps once across the village, in stable
business UUID order. This deterministic review allocation is not an inventory
reservation, purchase priority or monopoly rule. Actual future orders must recheck
funds, prices, ownership, capacity and transport. Producer stock, future extraction,
remote stock and pending warehouse imports are not counted as available warehouse
goods. Their omission does not establish that a new mine is preferable to supplying
an existing one; existing producers and transport need separate feasibility review.

Known active-world identity is required. Disabled timed industry/input supply,
unknown/absent worlds, unsupported stored industries or exceeded review budgets
return `complete=false`, not a claim that demand is zero. The caller supplies a
1..64 workshop review budget; warehouse budget uses existing editable supply policy.
Each buyer's pending cargo is bounded at 128; overflow defers the whole review.
No partial warehouse page can manufacture a shortage. Empty complete reviews are
valid when no existing workshops need inputs.

`PopulationSession.miningInputDemand(town, maximumAgeMillis, businessLimit)` runs
on the existing SQL owner and combines this input pressure with the latest complete
mineral survey. Only visible, fresh, unclaimed/unexcluded coal/iron coordinates
with positive uncovered input demand survive. Immediate tracking failures reject
the request; saved gaps, stale/future/missing surveys and absent worlds yield no
mineral candidates. No hidden ore or chunks are queried or loaded.

This read-only diagnostic is not automatic mining, a suitable coherent patch,
permission to modify player land, an actual order, a profitability guarantee or
finished-goods demand. It does not open a business, choose a career, consume a grant,
mint money, refill ore or create abstract sales. Founding still requires suitable
permitted sites, current connected patch validation, free qualified founders/workers,
actual funding and comparison with existing production/transport alternatives.
The original input-demand review added no scheduler or schema migration (schema
50). BUSINESS_TOOLS.md now adds actual, funded maintenance demand from existing
eligible mines/workshops, sharing recipe money and storage rather than claiming
them twice. Immediate ore-capture failures suppress mine maintenance pressure.
Existing warehouses can also buy configured workshops' actual output. These are
genuine maintenance needs, not invented final consumers; initial automatic
workshop/customer selection and business founding remain unfinished.
