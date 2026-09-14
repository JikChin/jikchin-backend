-- Local benchmark only. Requires the k6 account (benchmark/http/k6-auth.http signup, admin@jikchin.com).
-- Creates the fixture mate post that every seeded report references (same post the review benchmark uses,
-- so both seeds can share one database), then inserts one million reports.
-- One million reports so that the admin list GET /api/admin/reports?status=PENDING has to filter and sort
-- a large table. Status mix: PENDING 10%, RESOLVED 70%, REJECTED 20% (a backlog behind a long history).
SET @admin_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');
INSERT INTO mate_posts (
  user_id, event_id, title, content, max_members, current_members,
  preferred_gender, min_age, max_age, seat_info, status, created_at, updated_at
)
SELECT
  @admin_id, 1, 'Benchmark Review Fixture', 'Seeded reviews and reports reference this post.', 10, 1,
  'ANY', NULL, NULL, NULL, 'OPEN', NOW(6), NOW(6)
FROM DUAL
WHERE @admin_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM mate_posts WHERE title = 'Benchmark Review Fixture');
SET @mate_post_id = (SELECT id FROM mate_posts WHERE title = 'Benchmark Review Fixture');
SET @report_count = 1000000;
-- reporter_id / reported_user_id are plain columns (not foreign keys); the offset keeps the unique key
-- (mate_post_id, reporter_id, reported_user_id, reason) satisfied without creating member accounts.
SET @reporter_offset = 20000000;

INSERT INTO reports (mate_post_id, reporter_id, reported_user_id, reason, detail, status, created_at, processed_at)
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
  @reporter_offset + n,
  1,
  ELT(MOD(n, 5) + 1, 'NO_SHOW', 'ABUSE', 'HARASSMENT', 'SPAM', 'ETC'),
  'Large Seeded Benchmark Report',
  CASE WHEN MOD(n, 10) = 0 THEN 'PENDING' WHEN MOD(n, 10) < 8 THEN 'RESOLVED' ELSE 'REJECTED' END,
  -- Oldest report first: created_at rises with n so the admin queue (created_at ASC) is in insert order.
  DATE_SUB(NOW(6), INTERVAL (@report_count - n) SECOND),
  CASE WHEN MOD(n, 10) = 0 THEN NULL ELSE DATE_SUB(NOW(6), INTERVAL (@report_count - n - 1) SECOND) END
FROM sequence_numbers
WHERE n < @report_count;

SELECT status, COUNT(*) AS report_count, MIN(created_at) AS oldest, MAX(created_at) AS newest
FROM reports
WHERE detail = 'Large Seeded Benchmark Report'
GROUP BY status;
