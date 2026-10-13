# Grain ingredient preparation

Subsequent staffed future harvest accounting passed schema72 verification; see
GRAIN_HARVESTS.md. The evidence-only batch described below did not change output.

The user approved separate future grain ingredients while preserving all existing
food and keeping basic meals possible without bakeries. This batch implements the
first prerequisite: actual wheat-field evidence. It does not produce grain stock,
change food harvests, activate bakeries or convert past food into ingredients.

The bounded crop scanner distinguishes wheat planted on farmland from carrots,
potatoes and beetroot. All growth stages still represent planted capacity, not an
immediate physical harvest. Non-farmland plants are excluded. Paper snapshots and
world access remain on the server thread; observations never change blocks or
force chunks. Grain positions follow the same distance, overlap and partial-frame
rules as other crops, and are a subset of the accepted crop footprint.

Transactional schema 71 adds `farm_grain_evidence`. Existing farm rows, food
balances, historical food periods, money and identities remain unchanged. Existing
rows start with unknown grain composition, not zero and not an inferred fraction.
A fresh typed frame stores its wheat count together with the farm observation.
Older/equal frames cannot overwrite it. A newer untyped legacy frame clears the
composition to unknown. Partial evidence remains explicitly partial and cannot
authorize output. Counts cannot exceed total crops, and mismatched saved frame
timestamps are rejected. Rollback covers the farm and grain evidence together.

Next integration must partition actual future staffed harvest work into basic
meals and grain ingredients with editable rates, protect village food needs,
record bounded real stock/history and supply bakers through paid real transfers.
No ingredient may be created from old generic food. Broader recipes and bakery
founding still need their own verified integration; physical bakery construction
remains Phase 4.

Verified October13: focused116 and full Windows Wrapper clean verify1,346
cases passed (1,344 unit/two packaged integration), no failures/errors/skips.
The guarded Paper observer/restart probe matched actual zero-crop terrain and
preserved all24 food units. Positive material/field cases are covered by unit
and temporary-database tests, not claimed as native positive harvesting.
Old-save audit116/120 tables exact, four ordinary clock/evidence changes;
schema71 integrity/FK healthy, original properties restored and helper archived.
No playable-server changes. Do not count this as completed bakery gameplay.
