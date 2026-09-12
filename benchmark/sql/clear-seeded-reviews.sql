-- Removes only the seeded benchmark reviews. Real reviews and the fixture mate post are kept.
DELETE FROM reviews
WHERE id > 0
  AND content = 'Large Seeded Benchmark Review';
