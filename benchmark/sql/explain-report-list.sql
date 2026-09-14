-- Queries behind GET /api/admin/reports?status=PENDING (before and after cursor paging).
-- Deep-page cursor: the 95,000th PENDING report in created_at order (95% of the PENDING backlog).
SET @cursor_id = (
  SELECT id FROM (
    SELECT id FROM reports WHERE status = 'PENDING' ORDER BY created_at, id LIMIT 1 OFFSET 95000
  ) AS deep
);
SET @cursor_created_at = (SELECT created_at FROM reports WHERE id = @cursor_id);

-- (1) Before: the whole PENDING list. Without the composite index: key = idx_reports_status,
-- Using filesort over 100,000 rows.
EXPLAIN
SELECT * FROM reports WHERE status = 'PENDING' ORDER BY created_at;
EXPLAIN ANALYZE
SELECT * FROM reports WHERE status = 'PENDING' ORDER BY created_at;

-- (2) First page. With (status, created_at): key = idx_reports_status_created, no filesort, 21 rows.
EXPLAIN
SELECT * FROM reports WHERE status = 'PENDING' ORDER BY created_at, id LIMIT 21;
EXPLAIN ANALYZE
SELECT * FROM reports WHERE status = 'PENDING' ORDER BY created_at, id LIMIT 21;

-- (3) Deep page (keyset). With the index: range scan from the cursor, still 21 rows.
EXPLAIN
SELECT * FROM reports
WHERE status = 'PENDING'
  AND (created_at > @cursor_created_at OR (created_at = @cursor_created_at AND id > @cursor_id))
ORDER BY created_at, id LIMIT 21;
EXPLAIN ANALYZE
SELECT * FROM reports
WHERE status = 'PENDING'
  AND (created_at > @cursor_created_at OR (created_at = @cursor_created_at AND id > @cursor_id))
ORDER BY created_at, id LIMIT 21;
