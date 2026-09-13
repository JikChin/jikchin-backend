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
