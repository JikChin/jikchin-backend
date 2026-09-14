-- Condition A: only the single-column index that the entity already declares.
-- Keep idx_reviews_reviewee in both conditions so the composite index is the only variable.
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

SET @drop_reviewee_score = (
  SELECT IF(COUNT(*) = 0, 'SELECT 1', 'ALTER TABLE reviews DROP INDEX idx_reviews_reviewee_score')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'reviews'
    AND index_name = 'idx_reviews_reviewee_score'
);
PREPARE drop_reviewee_score FROM @drop_reviewee_score;
EXECUTE drop_reviewee_score;
DEALLOCATE PREPARE drop_reviewee_score;

ANALYZE TABLE reviews;
