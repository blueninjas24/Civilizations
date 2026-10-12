# First-workshop owner training

The automatic Paper workshop review now calls the SQL owner's
`reviewAutomaticWorkshopInvestment`. Read-only `prepareWorkshopInvestment`
remains read-only. If no qualified available founder can be proposed, an actual
viable workshop opportunity may sponsor one unemployed adult's smithing course.

The opportunity still requires a working farm's genuine funded toolkit demand,
current complete unclaimed nearby smithing evidence, protected grant money and
real paid starter surplus. Enrollment reserves no site, grant or inventory and
awards no experience. Living local age/one-job/active-course/open-owner checks,
bounded candidate pages and the existing course budget apply. Qualified residents
are filtered out of the training page. Reviews and cursor survive restart.

Training uses the existing editable `resident-study` and `career-study` policies,
and `workshop-founding` qualifications. The career industry is `blacksmith`; the
course skill is `smithing`. Prototype study gains one experience per productive
food period toward ten, consuming one actual stone and one actual timber unit per
period after food/material reserves. No personal payroll, tuition revenue, free
practice materials or instant Minecraft profession conversion is introduced.
Accepted courses retain their saved target/gain/material terms.

Enrollment first accrues the economy clock and saves its earned target as the
eligibility boundary. Previously earned backlog cannot teach a new learner.
Study spending, experience, status, food accounting and checkpoint commit together;
a failure retries without double learning or partial goods consumption.

Every practice period rechecks the saved prospective table and the real economic
opportunity. Stale/partial/absent or otherwise unavailable evidence, disabled
settings, exhausted funding/surplus, and lost demand pause practice without
inventing removal, discarding the course or spending supplies. The learner remains
in that saved course while it waits. Restored viable evidence can resume it.
Another blacksmith taking the first-firm opportunity cancels the prospect's course
and releases the job lock; existing learned experience and history survive.
Ordinary employment, death or migration still enforce the existing eligibility
cancellation. Reaching the course target makes the resident available for normal
qualification/current-block/funding/paid-startup acceptance.

Schema62 adds initially empty `workshop_founder_reviews` and
`workshop_founder_courses`, with stable course/world/settlement links, a deferred
review foreign key and an index. Existing skills, course terms, firms, assets and
history are unchanged. Upgrade is transactional, tested for failure and retry.
Missing old optional founding/career/study settings keep sponsorship disabled.

This does not found warehouses, add new industries, build structures, guarantee
that a village has enough food/materials/money, or complete emergent specialization.
Tests use explicit temporary fixture funds/supplies; native checks are disposable.
Exact verification results are recorded in PROJECT_STATUS and DEVLOG.
