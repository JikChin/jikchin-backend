-- Condition A for the admin report list: only the indexes the entity already declares.
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

ANALYZE TABLE reports;
