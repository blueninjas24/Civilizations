# Automatic first-toolkit workshop startup

The normal plugin now schedules PaperWorkshopStartup after the SQL owner is ready.
It reviews towns with saved living residents in stable rotating UUID order. Only
one proposal or acceptance is outstanding; futures are polled without blocking the
server thread. Each pass can check at most one proposed workplace block.

SQL WorkshopInvestment remains responsible for actual qualified founders, current
working-farm tool demand, protected shared grants, real starter surplus, storage,
jobs and transactional acceptance. The main-thread adapter checks the already
loaded world/chunk and exact smithing table before submitting point evidence.
It never loads/tickets chunks, alters blocks or reads container inventories.
Missing or unknown markers spend nothing. Changed facts defer the investment.
Expected refusals appear in `/civ debug observation`; unexpected failures also log
a warning and leave the cycle ready to retry. Shutdown cancels the adapter before
closing the SQL owner. Accepted firm identity prevents duplicate first-firm grants.

Existing optional `workshop-founding`, `workshop-opportunities`,
`workshop-starter-supplies`, workplace recognition and industry/grant settings still
control feasibility. No new configuration or migration is introduced; schema61 is
unchanged. Cadence is one pass each second, with separate SQL/point/acceptance passes.
Different towns share that bounded rotating review rather than parallel SQL jobs.

This batch connects already-qualified owners. Automatic first-workshop founder
training remains separate next work. It does not grant existing villagers skills,
found warehouses, build premises, or claim completion of Phase3 specialization.

Validation: 1,085 full Wrapper tests passed, zero failures/errors/skips; actual
scheduled startup plus marker/absence/restart checks passed on disposable Paper.
Original main-save assets/history remained intact; exact properties restored.
