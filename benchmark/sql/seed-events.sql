-- The four sports and their teams are created by bootstrap-event-catalog.sql.
SET @venue_id = 1;
SET @event_count = 100000;

-- Generates up to 100,000 future events. Run once on an empty benchmark events table.
INSERT INTO events (
  sport_id, venue_id, home_team_id, away_team_id, league_name, starts_at, status, created_at, updated_at
)
WITH digits AS (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
), sequence_numbers AS (
  SELECT d0.n + d1.n * 10 + d2.n * 100 + d3.n * 1000 + d4.n * 10000 AS n
  FROM digits d0 CROSS JOIN digits d1 CROSS JOIN digits d2 CROSS JOIN digits d3 CROSS JOIN digits d4
), benchmark_catalog AS (
  SELECT
    s.id AS sport_id,
    home.id AS home_team_id,
    away.id AS away_team_id,
    ROW_NUMBER() OVER (ORDER BY s.id) - 1 AS sport_index
  FROM sports s
  JOIN teams home ON home.sport_id = s.id AND home.name = CONCAT('벤치 홈 ', s.code)
  JOIN teams away ON away.sport_id = s.id AND away.name = CONCAT('벤치 어웨이 ', s.code)
  WHERE s.code IN ('BASEBALL', 'FOOTBALL', 'BASKETBALL', 'VOLLEYBALL')
)
SELECT
  catalog.sport_id,
  @venue_id,
  catalog.home_team_id,
  catalog.away_team_id,
  'Seeded Benchmark League',
  DATE_ADD('2026-10-01 09:00:00', INTERVAL n MINUTE),
  'SCHEDULED',
  NOW(6),
  NOW(6)
FROM sequence_numbers
JOIN benchmark_catalog catalog ON catalog.sport_index = MOD(sequence_numbers.n, 4)
WHERE n < @event_count;
