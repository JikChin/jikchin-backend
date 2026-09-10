ALTER TABLE events DROP INDEX IF EXISTS idx_events_starts_at;
ALTER TABLE events DROP INDEX IF EXISTS idx_events_sport_starts_at;
ANALYZE TABLE events;
