# Gradual supply-backed adult study

User approved gradual training with editable costs and limits. This implements
saved courses and their execution inside actual village food periods. Enrollment
is an internal trusted opportunity boundary; it does not yet automatically select
careers, found mines/workshops, create teachers or provide a player training command.

A learner must be a living local adult without an open business job or operating
owned business. One active course per person and a village enrollment budget prevent
unbounded training. Taking a job, moving away or dying cancels the course at its next
eligible review while preserving earned skills and history. Completion retains its
identity. Skills never replace the actual founder qualification or startup capital.

Each successful period consumes actual nonfood practice supplies from the village
commodity inventory. Its economic report records this under event demand; course
participant reviews and material spending explain the exact use. Courses cannot
use goods in transit, reserved player barter, business-owned stock or imaginary
supplies. No cash is created or destroyed, no salary/personal wallet is invented.

Food needs and an editable food buffer come first. Missing practice supplies,
material reserve limits, student budgets, disabled skills or lowered experience
ceilings pause training without spending or gaining experience. Students rotate
in stable order to share limited slots. Lowering the village budget below saved
active-course count defers the entire review rather than granting arbitrary partial
awards; diagnostics explain it. Legacy field output which counts all adults also
defers study to avoid using the same labor twice; actual farm employment avoids this.

Enrollment starts after already earned economy backlog. Server downtime and absent
worlds earn no study; incomplete field evidence pauses the owning economy. Stock,
economic reports, skills, course status, participant/material audits and checkpoint
save atomically. Failures restore all effects; replay cannot charge supplies twice.

## Editable defaults

In `plugins/Civilizations/economy.yml`, new-file prototype settings are:

```yaml
resident-study:
  enabled: true
  gain-per-period: 1
  maximum-experience: 100
  students-per-period: 2
  active-courses-per-village: 8
  food-reserve-periods: 1
  material-reserve-units: 2
  skills:
    mining:
      target-experience: 10
      practice-supplies:
        stone: 1
        timber: 1
    smithing:
      target-experience: 10
      practice-supplies:
        stone: 1
        timber: 1
```

Accepted courses retain target, gain and practice-material terms. Changes apply to
new enrollments; current enable/ceiling/reserve/student budgets govern future reviews
of old courses. Missing old sections disable study. Edit while stopped and restart;
invalid keys, food costs, noninteger rates and excessive budgets fail before DB startup.
These are practice units and prototype pacing, not ore creation or fixed real years.
The future opportunity selector must align course targets with founder requirements.

## Persistence and validation

Transactional additive schema 43 saves courses, accepted material terms, period
reviews and spending linked to real economy history. Existing residents, skills,
goods, estate funds and business history are retained. Temporary-database tests
exercise conservative supplies, food priority, skills/capital eligibility, fairness,
death/hiring, replay/retuning/restart, absent worlds, historical queues, rollback
and schema-42 upgrade failure/retry. PROJECT_STATUS and DEVLOG record final full
Wrapper and isolated Paper verification results; do not infer a pass from this design.

Verified October10: final Windows Wrapper clean verify770 passed (768 unit/two
packaged integration), no failures/errors/skips; initial migration-test assertion
was corrected to read old stock directly. Isolated Paper confirmed hungry pause,
exact two stone/timber consumption, skill0→1→2 and completed/restart/world-pause
behavior. Existing dev assets/skills/history retained; integrity/FKs passed. Test
server stopped normally and settings restored; human server unchanged.
