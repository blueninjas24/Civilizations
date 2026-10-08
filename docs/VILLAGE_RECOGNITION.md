# Automatic village observation foundation

User approved automatically recognizing both natural villages and player-built
areas containing beds and villagers. There is no manual join step. Beds alone
do not grant enrollment or block-edit permission.

`VillageObservation` carries immutable bed coordinates and villager identities,
positions, adult status and optional existing name, with world UUID/name and
timestamp. The completeness flag survives recognition: partial loaded observations
must never overwrite authoritative population or imply missing homes/deaths.
No Bukkit objects or database connection cross this boundary.

`VillageRecognition` is a bounded pure candidate detector, not the runtime importer
or permanent identity matcher. Editable experimental inputs default to two beds
and two nearby villagers, connected bed spacing up to 32 blocks, and a 32-block
3D resident-to-bed distance. Natural and player-built areas use the same rule.
One-bed/one-villager areas can qualify by changing caller inputs. These thresholds
are prototype detection inputs, not locked design canon or enabled runtime config.

Coordinates and UUIDs are sorted for reproducibility. Villagers choose their
nearest observed bed; equal distances resolve in coordinate order. Connected bed
components become candidates only when both thresholds pass. Candidates remain
separate when no connecting beds are observed; no fixed 128-block grid or player
position defines identity. Radius values are bounded; each invocation accepts at
most 512 beds and the configured villager limit (prototype 256, hard ceiling 1024).
Over-budget scans fail explicitly instead of silently truncating a complete census.
Duplicate bed or villager observations fail rather than double-counting.

Seven tests verify ordering/old-grid boundary, bed-only/villager-only rejection,
separate clusters, nearest/tie/height rules, partial observations, limits/duplicates,
editable thresholds and distinct world identity. The detector does not access or
load Minecraft chunks. Quadratic bed comparisons are bounded by the input cap;
this is not a measured main-thread performance claim.

Still pending: a bounded loaded-chunk Paper observer, stable saved world/bed/entity
bindings, overlap/changed-footprint reconciliation, transactional import, home
grouping/capacity, nearby projection and diagnostics. Existing listener remains
unchanged until the adapter is ready. Chain-connected beds can form broad clusters;
conflicts with multiple existing settlements must be diagnosed, not silently merged.

User also approved configurable prototype partner formation among unrelated adults
sharing a home, without inventing parentage for existing children. This approval is
recorded; automatic formation is not yet implemented by this recognition batch.
