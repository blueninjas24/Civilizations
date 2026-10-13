# Saved shoreline fisheries

Implemented on the feature branch: a qualified, unemployed adult can open the village's first fishery when there is fresh connected nearby water, a safe dry bank, actual funded food demand remaining after startup spending, protected village funds, and enabled settings. The founder must already have saved fishing experience; automatic first-owner fishing training is still pending. No vanilla profession grants free experience.

PaperFisheryStartup reads only loaded chunks on the main thread. It rechecks the original columns for open sky, saved surface level and minimum depth, scans bounded bank candidates, then rechecks the selected footing/headroom before serialized SQL acceptance. Read budgets are editable. It does not load chunks, move blocks, claim player buildings, or use container contents.

Paid acceptance atomically saves the firm, first worker, treasury debit, actual operating cash, empty stock, original renewable fish habitat, immutable recognition terms and opening bank. A failure rolls back the entire investment. Replays cannot charge again, create a second firm, refill caught fish or move a previously relocated bank back.

Migration 69 adds initially empty fishery_workplaces and fishery_workplace_history tables. It does not rewrite existing resources or infer shoreline terms for legacy fisheries. Existing unbound fisheries retain their behavior; their terms cannot be guessed by an automatic replay.

PaperFisheryWorkplaces periodically checks the same saved water support and nearby bank candidates. A confirmed unsafe standing point moves to the nearest suitable bank within saved opening bounds beside the same original habitat. If none is known safe, the business retains its people, goods, cash and history but catching pauses. An unloaded/unknown original standing point preserves its last confirmed operational state. Ordinary unloaded-chunk simulation continues; an absent world pauses time. Healthy/unhealthy restoration never refills fish immediately.

Original water-column loss lowers habitat capacity and retires excess stock; restoration raises capacity without inventing fish. Bank movement and habitat updates commit together. Stable checks update one latest frame; only bank movement or operational-state transitions add shoreline history. Removed supports are not replaced with newly claimed water.

No physical repair or dock construction is implemented. A NEEDS_REPAIR result retains the business for a future feasible, funded and protected Phase 4 project. Workplace checks are bounded round-robin and do not provide instantaneous destructive-event response. A large-server latency/profile acceptance gate remains open.

Settings live in economy.yml: fishery-opportunities, fishery-founding, fishery-shoreline, water-observer, fishing, public supply orders and startup grants all apply. Missing optional sections in existing admin files opt out; files are never overwritten. Opening depth/rise/relocation bounds remain saved even if admins tune future startups. Diagnostics show the separate startup and saved-workplace checks.

Validation uses temporary databases and a marked disposable localhost Paper fixture. The native helper supplies explicit fixture actors, earned-skill setup and treasury; it proves ordinary automatic startup and timed work, not automatic skill acquisition, natural villager assignment, dock building or a broad balanced economy.

Full clean verify: 1,302 passing cases, zero failures/errors/skips, October 13 at 01:53:19 ET. Final guarded native proof passed 01:58:12 ET, including persisted relocation under read-only input masking. The native helper waits for actual timer registration before advancing its clock. Existing fixture data/config were audited and restored after normal shutdown.
