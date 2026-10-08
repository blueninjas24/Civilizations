# Prototype adult partnerships

User authorized simple configurable partnerships among unrelated adults sharing
a home. `PartnershipFormation.form` is an explicit pure operation with enabled
flag and minimum logical age. It is not automatically called by the current
plugin or monthly replay; demographic replay 3 and persisted settings stay intact.

Eligible living, housed adults are considered in stable UUID order. Each gets at
most one living partner. Adults with an existing living partner remain exclusive
even if they currently live apart. Known direct ancestors/descendants or shared
ancestors (including cousins) are excluded. Unknown ancestry is left unknown;
the operation creates partner facts only, never fabricated parents for existing
children. No gender, romance, divorce, matchmaking score or marriage ceremony.

A deceased partner remains in historical links; a survivor may form a new living
partnership. Living partners in different homes are retained without dissolution.
Repeat application adds no duplicate ties. Existing partnerships and individuals
are never rewritten. Output includes the added ties so an eventual importer can
persist them in its transaction. Pairing does not itself create a child or consume
housing; existing birth rules still require eligible ages and observed capacity.

Seven tests cover deterministic/exclusive/repeated pairing, enable/age/status/home
rules, known ancestry/siblings/cousins, existing living partners, retained deceased
partners, unknown child ancestry and an actual birth after pairing. No Paper or DB
access, NPC edits, invented job or unapproved family ancestry introduced.

Monthly formation and runtime configuration/persistence are integration work.
Any change to the monthly algorithm/settings must advance replay conventions
explicitly; this helper does not silently activate new behavior in existing saves.
