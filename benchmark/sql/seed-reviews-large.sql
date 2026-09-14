-- Local benchmark only. Run bootstrap-review-fixture.sql first.
-- One million reviews are given to a single reviewee so that GROUP BY score has to aggregate
-- the whole group. This is the N that every rung of the ladder (index -> summary table ->
-- micro batch -> cache) is measured against.
SET @reviewee_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');
SET @mate_post_id = (SELECT id FROM mate_posts WHERE title = 'Benchmark Review Fixture');
SET @review_count = 1000000;
-- reviewer_id is a plain column (not a foreign key); offset keeps the unique key
-- (mate_post_id, reviewer_id, reviewee_id) satisfied without creating reviewer accounts.
SET @reviewer_offset = 10000000;

INSERT INTO reviews (mate_post_id, reviewer_id, reviewee_id, score, content, created_at)
WITH digits AS (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
), sequence_numbers AS (
  SELECT d0.n + d1.n * 10 + d2.n * 100 + d3.n * 1000 + d4.n * 10000 + d5.n * 100000 AS n
  FROM digits d0
  CROSS JOIN digits d1
  CROSS JOIN digits d2
  CROSS JOIN digits d3
  CROSS JOIN digits d4
  CROSS JOIN digits d5
)
SELECT
  @mate_post_id,
  @reviewer_offset + n,
  @reviewee_id,
  -- Skewed like a real rating distribution: 5:50%, 4:30%, 3:10%, 2:5%, 1:5%.
  CASE
    WHEN MOD(n, 20) < 10 THEN 5
    WHEN MOD(n, 20) < 16 THEN 4
    WHEN MOD(n, 20) < 18 THEN 3
    WHEN MOD(n, 20) = 18 THEN 2
    ELSE 1
  END,
  'Large Seeded Benchmark Review',
  DATE_SUB(NOW(6), INTERVAL n SECOND)
FROM sequence_numbers
WHERE n < @review_count;

SELECT
  COUNT(*) AS total_reviews,
  MIN(score) AS min_score,
  MAX(score) AS max_score,
  ROUND(AVG(score), 2) AS avg_score
FROM reviews
WHERE reviewee_id = @reviewee_id
  AND content = 'Large Seeded Benchmark Review';
