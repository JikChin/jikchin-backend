-- Rebuilds review_stats from reviews. Run once after the app has created the table (2단계 이후),
-- and again whenever seeded/k6 reviews are added or removed behind the application's back.
-- Production would run the same statement once as a backfill migration.
DELETE FROM review_stats WHERE reviewee_id > 0;

INSERT INTO review_stats
  (reviewee_id, total_count, score_sum, count_1, count_2, count_3, count_4, count_5)
SELECT
  reviewee_id,
  COUNT(*),
  SUM(score),
  SUM(score = 1),
  SUM(score = 2),
  SUM(score = 3),
  SUM(score = 4),
  SUM(score = 5)
FROM reviews
GROUP BY reviewee_id;

UPDATE members m
JOIN review_stats s ON s.reviewee_id = m.id
SET m.manner_score = ROUND(s.score_sum / s.total_count, 2);

-- Cross-check against the source of truth: both rows must match for the benchmark reviewee.
SET @reviewee_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');
SELECT 'review_stats' AS source, total_count, score_sum, count_1, count_2, count_3, count_4, count_5
FROM review_stats WHERE reviewee_id = @reviewee_id
UNION ALL
SELECT 'reviews', COUNT(*), SUM(score), SUM(score = 1), SUM(score = 2), SUM(score = 3), SUM(score = 4), SUM(score = 5)
FROM reviews WHERE reviewee_id = @reviewee_id;
