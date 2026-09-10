SET @drop_starts_at = (
  SELECT IF(COUNT(*) = 0, 'SELECT 1', 'ALTER TABLE events DROP INDEX idx_events_starts_at')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'events'
    AND index_name = 'idx_events_starts_at'
);
PREPARE drop_starts_at FROM @drop_starts_at;
EXECUTE drop_starts_at;
DEALLOCATE PREPARE drop_starts_at;

SET @drop_sport_starts_at = (
  SELECT IF(COUNT(*) = 0, 'SELECT 1', 'ALTER TABLE events DROP INDEX idx_events_sport_starts_at')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'events'
    AND index_name = 'idx_events_sport_starts_at'
);
PREPARE drop_sport_starts_at FROM @drop_sport_starts_at;
EXECUTE drop_sport_starts_at;
DEALLOCATE PREPARE drop_sport_starts_at;

ANALYZE TABLE events;
