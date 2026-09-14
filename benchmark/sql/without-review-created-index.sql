-- Condition A for the review list: only the single-column index the entity already declares.
-- Keep idx_reviews_reviewee in both conditions so (reviewee_id, created_at) is the only variable.
SET @add_reviewee = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE reviews ADD INDEX idx_reviews_reviewee (reviewee_id)', 'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'reviews'
    AND index_name = 'idx_reviews_reviewee'
);
PREPARE add_reviewee FROM @add_reviewee;
EXECUTE add_reviewee;
DEALLOCATE PREPARE add_reviewee;

SET @drop_created = (
  SELECT IF(COUNT(*) = 0, 'SELECT 1', 'ALTER TABLE reviews DROP INDEX idx_reviews_reviewee_created')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'reviews'
    AND index_name = 'idx_reviews_reviewee_created'
);
PREPARE drop_created FROM @drop_created;
EXECUTE drop_created;
DEALLOCATE PREPARE drop_created;

ANALYZE TABLE reviews;
