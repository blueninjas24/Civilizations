# Business ownership across villages

Qualified owners may buy an existing business in another saved village in the
same Minecraft world. This is an acquisition after an owner's death, after
qualified family and employee succession have been considered. It does not force
living owners to sell, found a free branch, or guarantee a large company.

The buyer must meet the actual industry requirements. Their existing occupation
must remain compatible; ownership does not grant a second worker job. Funding
comes from a genuinely owned business's spare cash and/or an eligible family
estate, under the existing retained-cash and percentage limits. The seller's
family receives the exact payment. Money, ownership, listing completion and audit
entries save together; failed saves roll everything back.

Estate funding always stays within the target's world, including when new remote
purchases are disabled. Automatic reviews skip off-world estates rather than
selecting one and aborting an otherwise valid review. Existing balances are never
changed by this eligibility check; legacy accounts in the same settlement remain
eligible without inventing a world binding.

Each acquired branch retains its village, site, founder, local employees, stock,
cash account and outstanding deliveries. Ordinary local staffing and production
rules continue. Ownership itself does not ship resources between villages.
Regional goods movement remains Phase 5. Saved qualified family can inherit a
remote branch without silently moving there or abandoning a different occupation.

New `economy.yml` files enable this through
`business-resale-funding.allow-cross-village-purchases: true`. Missing old keys
default to local purchases only; installed files are never overwritten. Disabling
new remote purchases does not confiscate existing branches. Villages must have
the same saved permanent world ID; names alone cannot authorize a purchase.

Verification lives in `CrossVillageBusinessTest`: real automatic payment,
retained local assets/jobs, restart replay, disabled/different-world refusal,
transaction failure recovery, qualified family succession and local-only estate
funding boundaries. Broader seeded
acceptance is recorded separately in PROJECT_STATUS.md.
