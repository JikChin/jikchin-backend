-- Run once in the local benchmark database after signing up the k6 account (benchmark/http/k6-auth.http).
-- Creates a single mate post that every seeded review references through the mate_post_id foreign key.
-- The k6 account itself is used as the reviewee, so no extra member row is needed.
SET @reviewee_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');

INSERT INTO mate_posts (
  user_id, event_id, title, content, max_members, current_members,
  preferred_gender, min_age, max_age, seat_info, status, created_at, updated_at
)
SELECT
  @reviewee_id, 1, 'Benchmark Review Fixture', 'Seeded reviews reference this post.', 10, 1,
  'ANY', NULL, NULL, NULL, 'OPEN', NOW(6), NOW(6)
FROM DUAL
WHERE @reviewee_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM mate_posts WHERE title = 'Benchmark Review Fixture');

SELECT
  @reviewee_id AS reviewee_id,
  (SELECT id FROM mate_posts WHERE title = 'Benchmark Review Fixture') AS mate_post_id;
