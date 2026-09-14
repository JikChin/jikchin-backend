-- Removes only the seeded benchmark reports. Real reports are kept.
DELETE FROM reports
WHERE id > 0
  AND detail = 'Large Seeded Benchmark Report';
