-- Condition B for the review list: add (reviewee_id, created_at) so the keyset query
-- "WHERE reviewee_id = ? AND (created_at, id) < cursor ORDER BY created_at DESC, id DESC LIMIT n"
-- reads the index backwards and stops after n rows (no filesort). InnoDB appends the PK (id)
-- to every secondary index entry, so the id tie-break is covered without listing it.
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

ALTER TABLE reviews ADD INDEX idx_reviews_reviewee_created (reviewee_id, created_at);
ANALYZE TABLE reviews;
