-- Queries that ReviewRepository sends for GET /api/members/{id}/reviews?cursor=&size=20.
SET @reviewee_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');
-- A review roughly in the middle of the 1M seed, used as a deep-page cursor.
SET @cursor_id = 500000;
SET @cursor_created_at = (SELECT created_at FROM reviews WHERE id = @cursor_id);

-- First page. Without the composite index: key = idx_reviews_reviewee, Extra = Using filesort
-- over the whole group. With it: key = idx_reviews_reviewee_created, Backward index scan, 21 rows.
EXPLAIN
SELECT * FROM reviews
WHERE reviewee_id = @reviewee_id
ORDER BY created_at DESC, id DESC
LIMIT 21;

EXPLAIN ANALYZE
SELECT * FROM reviews
WHERE reviewee_id = @reviewee_id
ORDER BY created_at DESC, id DESC
LIMIT 21;

-- Deep page (keyset). Cost must not grow with the cursor position when the index exists.
EXPLAIN
SELECT * FROM reviews
WHERE reviewee_id = @reviewee_id
  AND (created_at < @cursor_created_at OR (created_at = @cursor_created_at AND id < @cursor_id))
ORDER BY created_at DESC, id DESC
LIMIT 21;

EXPLAIN ANALYZE
SELECT * FROM reviews
WHERE reviewee_id = @reviewee_id
  AND (created_at < @cursor_created_at OR (created_at = @cursor_created_at AND id < @cursor_id))
ORDER BY created_at DESC, id DESC
LIMIT 21;
