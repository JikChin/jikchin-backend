-- Replace values with the same values used by k6.
EXPLAIN ANALYZE
SELECT id, sport_id, venue_id, home_team_id, away_team_id, league_name, starts_at, status
FROM events
WHERE sport_id = 1
  AND starts_at >= '2026-10-01 00:00:00'
  AND starts_at < '2026-12-01 00:00:00'
ORDER BY starts_at ASC
LIMIT 20;
