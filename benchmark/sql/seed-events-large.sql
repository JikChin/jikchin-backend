-- Local benchmark only. Run clear-seeded-events.sql first.
-- One million events are distributed across four sports in the same two-month window.
-- This makes the difference between a sport-only index and (sport_id, starts_at) observable.
SET @venue_id = 1;
SET @event_count = 100000;
SET @window_start = '2026-10-01 00:00:00';
SET @window_end = '2026-12-01 00:00:00';
SET @window_minutes = TIMESTAMPDIFF(MINUTE, @window_start, @window_end);
SET @events_per_minute = CEIL(@event_count / @window_minutes);

INSERT INTO events (
  sport_id, venue_id, home_team_id, away_team_id, league_name, starts_at, status, created_at, updated_at
)
WITH digits AS (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
), sequence_numbers AS (
  SELECT d0.n + d1.n * 10 + d2.n * 100 + d3.n * 1000 + d4.n * 10000 + d5.n * 100000 AS n
  FROM digits d0
  CROSS JOIN digits d1
  CROSS JOIN digits d2
  CROSS JOIN digits d3
  CROSS JOIN digits d4
  CROSS JOIN digits d5
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
  'Large Seeded Benchmark League',
  DATE_ADD(@window_start, INTERVAL FLOOR(sequence_numbers.n / @events_per_minute) MINUTE),
  'SCHEDULED',
  NOW(6),
  NOW(6)
FROM sequence_numbers
JOIN benchmark_catalog catalog ON catalog.sport_index = MOD(sequence_numbers.n, 4)
WHERE sequence_numbers.n < @event_count;

SELECT
  COUNT(*) AS total_events,
  COUNT(DISTINCT sport_id) AS sport_count,
  MIN(starts_at) AS first_starts_at,
  MAX(starts_at) AS last_starts_at
FROM events
WHERE league_name = 'Large Seeded Benchmark League';
