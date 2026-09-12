-- Condition B: add the covering composite index (reviewee_id, score) on top of condition A.
-- score is included so the GROUP BY query never touches the clustered index (Using index)
-- and the rows arrive already grouped, which removes the temporary table.
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

ALTER TABLE reviews ADD INDEX idx_reviews_reviewee_score (reviewee_id, score);
ANALYZE TABLE reviews;
