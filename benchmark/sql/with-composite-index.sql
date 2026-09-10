-- Keep the same foreign-key index that exists in the no-composite-index condition.
SET @add_sport_id = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE events ADD INDEX idx_events_sport_id (sport_id)', 'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'events'
    AND index_name = 'idx_events_sport_id'
);
PREPARE add_sport_id FROM @add_sport_id;
EXECUTE add_sport_id;
DEALLOCATE PREPARE add_sport_id;

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

ALTER TABLE events ADD INDEX idx_events_sport_starts_at (sport_id, starts_at);
ANALYZE TABLE events;
