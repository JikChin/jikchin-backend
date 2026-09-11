-- Local benchmark data only. This keeps catalog records and removes only seeded rows.
-- `id > 0` allows this statement to run with MySQL Workbench safe-update mode enabled.
DELETE FROM events
WHERE id > 0
  AND league_name IN ('Seeded Benchmark League', 'Large Seeded Benchmark League');

SELECT COUNT(*) AS remaining_events
FROM events;
