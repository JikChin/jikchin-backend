-- Stage 2 onwards: the query ReviewStatsRepository.findById sends. Expect type = const, rows = 1.
SET @reviewee_id = (SELECT id FROM members WHERE email = 'admin@jikchin.com');

EXPLAIN
SELECT reviewee_id, total_count, score_sum, count_1, count_2, count_3, count_4, count_5
FROM review_stats
WHERE reviewee_id = @reviewee_id;

EXPLAIN ANALYZE
SELECT reviewee_id, total_count, score_sum, count_1, count_2, count_3, count_4, count_5
FROM review_stats
WHERE reviewee_id = @reviewee_id;
