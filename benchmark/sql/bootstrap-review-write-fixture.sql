-- Local benchmark only. Run after signing up both k6 accounts
-- (admin@jikchin.com = reviewee, k6-writer@jikchin.com = reviewer).
-- POST /api/reviews accepts one review per (mate_post, reviewer, reviewee), so the write test
-- needs a fresh mate post for every request. This seeds @post_count posts owned by the reviewee
-- and joins both accounts as ACTIVE mate members of each one.
SET @reviewee_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');
SET @writer_id = (SELECT id FROM members WHERE email = 'k6-writer@jikchin.com');
SET @post_count = 3000;

INSERT INTO mate_posts (
  user_id, event_id, title, content, max_members, current_members,
  preferred_gender, min_age, max_age, seat_info, status, created_at, updated_at
)
WITH digits AS (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
), sequence_numbers AS (
  SELECT d0.n + d1.n * 10 + d2.n * 100 + d3.n * 1000 AS n
  FROM digits d0 CROSS JOIN digits d1 CROSS JOIN digits d2 CROSS JOIN digits d3
)
SELECT
  @reviewee_id, 1, 'Benchmark Review Write Fixture', 'k6 write test target post.', 10, 2,
  'ANY', NULL, NULL, NULL, 'OPEN', NOW(6), NOW(6)
FROM sequence_numbers
WHERE n < @post_count
  AND @reviewee_id IS NOT NULL
  AND @writer_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM mate_posts WHERE title = 'Benchmark Review Write Fixture');

INSERT INTO mate_members (mate_post_id, user_id, joined_at, status)
SELECT p.id, m.user_id, NOW(6), 'ACTIVE'
FROM mate_posts p
CROSS JOIN (SELECT @reviewee_id AS user_id UNION ALL SELECT @writer_id) m
WHERE p.title = 'Benchmark Review Write Fixture'
  AND NOT EXISTS (
    SELECT 1 FROM mate_members mm WHERE mm.mate_post_id = p.id AND mm.user_id = m.user_id
  );

-- POST_ID_START for benchmark/k6/review-write.js. Posts are contiguous from this id.
SELECT
  @reviewee_id AS reviewee_id,
  @writer_id AS writer_id,
  MIN(id) AS post_id_start,
  COUNT(*) AS post_count
FROM mate_posts
WHERE title = 'Benchmark Review Write Fixture';
