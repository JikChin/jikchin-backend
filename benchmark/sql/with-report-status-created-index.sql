-- Condition B for the admin report list: add (status, created_at) so
-- "WHERE status = ? ORDER BY created_at, id LIMIT n" walks one status range in order and stops
-- after n rows. InnoDB appends the PK (id) to every secondary index entry, so the id tie-break is covered.
-- idx_reports_status (status only) stays in both conditions so the composite index is the only variable.
SET @add_status = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE reports ADD INDEX idx_reports_status (status)', 'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'reports'
    AND index_name = 'idx_reports_status'
);
PREPARE add_status FROM @add_status;
EXECUTE add_status;
DEALLOCATE PREPARE add_status;

SET @drop_composite = (
  SELECT IF(COUNT(*) = 0, 'SELECT 1', 'ALTER TABLE reports DROP INDEX idx_reports_status_created')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'reports'
    AND index_name = 'idx_reports_status_created'
);
PREPARE drop_composite FROM @drop_composite;
EXECUTE drop_composite;
DEALLOCATE PREPARE drop_composite;

ALTER TABLE reports ADD INDEX idx_reports_status_created (status, created_at);
ANALYZE TABLE reports;
