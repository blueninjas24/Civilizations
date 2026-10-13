# Fishing experience and real-vacancy training

Implemented: fishing employees gain configurable experience from actual new catches through the existing transactional work-learning system. Empty catches, full storage, unsafe banks, absent worlds, disabled or unmapped learning and historical replay do not award experience. Catch, habitat stock, skill, learning audit and timer checkpoint roll back together after a failure. Retuning does not remove old skill or manufacture past experience.

Default work-learning maps fishing to fishing, gives one experience per productive period, and keeps the existing ceiling and worker budget. Normal skill/capital ownership requirements still apply. Learning is not salary or personal bank money.

An existing live-owner fishery with a genuine vacancy can sponsor bounded gradual adult fishing study through the existing career-study mechanism. Enrollment awards no skill or job. Future practice consumes real public timber and respects food needs, material reserves, student limits, living/unemployed status and actual remaining vacancies. Once a learner meets the worker requirements, ordinary qualified staffing can hire them; the study stops consuming supplies after they become employed.

Default fishing study target is 10 experience with one timber per productive practice period, using the existing one-experience study rate. All settings are editable under resident-study, automatic-career-study, automatic-business-staffing and work-learning in economy.yml. Existing files keep their saved maps/lists until administrators explicitly opt in; these changes do not overwrite files or upgrade existing accepted course terms.

Automatic sponsorship for the FIRST fishing owner is still unfinished. Existing-vacancy training cannot bootstrap a business that does not exist, and water or a vanilla fisherman profession does not grant free skill. A suitable, funded first-owner course requires its own durable habitat/bank opportunity association and current-evidence checks. This remains the next fishing integration task.

No migration is added by this batch. Existing schema 69 saves and accepted courses keep their identities, terms and history. Tests use temporary databases only. The native probe uses explicit initial fixture fishing skill, then verifies that real catches earn further experience and that absent worlds/restarts do not grant past work; it does not claim automatic first-owner training.

Validation: full Windows Wrapper clean verify passed 1,308 cases with zero failures/errors/skips at 02:11:40 ET October 13. The marked Paper fixture passed at 02:12:59 ET, including actual earned fishing experience. Its data/config audit and restoration passed after normal shutdown.
