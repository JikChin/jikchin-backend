-- Same query that ReviewRepository.countByScoreForReviewee sends.
SET @reviewee_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');

-- Condition A should show key = idx_reviews_reviewee and Extra containing "Using temporary".
-- Condition B should show key = idx_reviews_reviewee_score and Extra = "Using index" only.
EXPLAIN
SELECT score, COUNT(*) AS review_count
FROM reviews
WHERE reviewee_id = @reviewee_id
GROUP BY score;

-- Actual rows and time. Both conditions still read every row of the group: the index lowers
-- the cost per row (no random clustered-index lookups) but cannot change the O(N) scan.
EXPLAIN ANALYZE
SELECT score, COUNT(*) AS review_count
FROM reviews
WHERE reviewee_id = @reviewee_id
GROUP BY score;
