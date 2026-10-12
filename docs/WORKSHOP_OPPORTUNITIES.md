# First-workshop investment evidence

`WorkshopOpportunities` reviews a narrow first blacksmith/toolkit investment.
It does not create a firm, hire anyone, enroll a course, reserve money or claim
blocks. `PopulationSession.currentWorkshopOpportunities` runs it on the normal
SQL owner. The plugin reads the optional policy at startup. The normal automatic
workshop path now consumes this evidence, sponsors real first-owner study and
requires final loaded-table/SQL acceptance. See AUTOMATIC_WORKSHOPS.md and
WORKSHOP_FOUNDER_TRAINING.md. The read-only API remains an internal review, without
a player command or direct spending.

Candidate evidence requires all of the following:

- A present permanent world and recent complete, unclaimed, unambiguous nearby
  smithing-table marker. Beds/occupied anchors and excessive review pages defer.
- A current productive canonical farm with qualified staff, real toolkit wear/
  service need, fully supplied current population and an affordable protected
  village equipment budget. Missing or changed food evidence cannot be replaced
  with a speculative future customer.
- Uncovered actual toolkit demand after existing local warehouse stock. A saved
  blacksmith identity defers first-firm bootstrap, including closed firms that need
  proper lifecycle/reuse review. This is not general competition or a one-shop law.
- A configured blacksmith recipe producing toolkits, a complete valid storage
  catalog and room for both its starter inputs and a full output batch.
- Current protected startup grant affordability and a complete paid village
  surplus bundle under the approved one-time starter policy. No free goods or
  capital, new private accounts or higher public grant limits.

`WorkshopStarterPlan` shares the exact bounded surplus/price/space calculation
between this prospective review and real paid acceptance. Review does not guarantee
that later acceptance will succeed; every mutable fact must be rechecked.
Qualified founder selection, future-only supply-backed training and main-thread
current-block revalidation remain before automatic founding.

Editable `economy.yml` prototype section:

```yaml
workshop-opportunities:
  enabled: true
  markers-per-review: 8
  evidence-lifetime-millis: 60000
  horizontal-range: 96
  vertical-range: 48
  startup-hundredths: 500
  capital-hundredths: 4000
  storage-capacity: 1024
```

Missing legacy section disables the review. The separate workplace recognition
policy must also permit smithing tables in the application adapter. Costs use
existing Trade Value hundredths, recipe inputs/prices and storage weights; they
are prototype terms, not new canonical economic rates. No schema migration.

No world blocks, entities or inventories are read or changed by the SQL review.
The saved marker comes from the already bounded main-thread snapshot adapter.
Source evidence is not a building-ownership grant, a guarantee of hidden resources
or permission to move/modify player structures.

Verification: nine temporary SQLite cases and two configuration cases; final
Wrapper `mvnw.cmd clean verify` passed 1,054 tests with no failures/errors/skips,
5:40. The updated disposable WorkshopStarterProbe passed both real purchase and
normal PopulationSession read-only opportunity checks under Paper 26.2-129.
100/103 original fixture tables remained unchanged; only three normal time/tracking
tables changed. Assets, money, residents and histories were preserved. No forced
chunks; original settings restored and backups retained. Schema60 unchanged.

October 12 approved operational rule: newly marker-founded firms pause after a
confirmed missing/wrong marker and resume after restoration at the same anchor.
See WORKPLACE_OPERATIONS.md for the explicit startup binding and loaded point checks.
Qualified founder preparation and automatic acceptance scheduling remain unfinished.

October 12 update: marker removal policy is now approved: pause until restored at
the saved spot. The explicit marker startup and point-confirmation seams are
described in WORKPLACE_OPERATIONS.md; automatic founder selection/training remains.

October12 investment update: qualified-owner preparation and rechecked paid
acceptance are described in WORKSHOP_INVESTMENT.md. Automatic first-owner training
and the normal server startup scheduler remain unfinished.
