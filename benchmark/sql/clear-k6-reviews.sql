-- Removes reviews written by benchmark/k6/review-write.js so the same fixture posts can be reused.
-- Run before every write run; the unique key (mate_post_id, reviewer_id, reviewee_id) rejects
-- a second review on the same post otherwise. Seeded and real reviews are kept.
DELETE FROM reviews
WHERE id > 0
  AND content LIKE 'k6-write-marker-%';

SET @reviewee_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');
UPDATE members
SET manner_score = (SELECT ROUND(AVG(score), 2) FROM reviews WHERE reviewee_id = @reviewee_id)
WHERE id = @reviewee_id;

-- From stage 2 on, review_stats exists and must be brought back in line with reviews.
-- The table is absent in stages 0-1, so the rebuild is skipped when it does not exist.
SET @rebuild_stats = (
  SELECT IF(COUNT(*) = 0, 'SELECT 1', 'DELETE FROM review_stats WHERE reviewee_id = @reviewee_id')
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'review_stats'
);
PREPARE rebuild_stats FROM @rebuild_stats;
EXECUTE rebuild_stats;
DEALLOCATE PREPARE rebuild_stats;

SET @rebuild_stats = (
  SELECT IF(
    COUNT(*) = 0,
    'SELECT 1',
    'INSERT INTO review_stats (reviewee_id, total_count, score_sum, count_1, count_2, count_3, count_4, count_5)
     SELECT reviewee_id, COUNT(*), SUM(score), SUM(score = 1), SUM(score = 2), SUM(score = 3), SUM(score = 4), SUM(score = 5)
     FROM reviews WHERE reviewee_id = @reviewee_id GROUP BY reviewee_id')
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'review_stats'
);
PREPARE rebuild_stats FROM @rebuild_stats;
EXECUTE rebuild_stats;
DEALLOCATE PREPARE rebuild_stats;
