-- Run after the application has created its JPA tables.
-- This script is only for a local benchmark database.

INSERT INTO sports (code, name, active, created_at, updated_at)
VALUES
  ('BASEBALL', '야구', TRUE, NOW(6), NOW(6)),
  ('FOOTBALL', '축구', TRUE, NOW(6), NOW(6)),
  ('BASKETBALL', '농구', TRUE, NOW(6), NOW(6)),
  ('VOLLEYBALL', '배구', TRUE, NOW(6), NOW(6));

INSERT INTO venues (name, region, address, created_at, updated_at)
VALUES ('벤치마크 구장', '서울', '벤치마크 전용', NOW(6), NOW(6));
SET @venue_id = LAST_INSERT_ID();

INSERT INTO teams (sport_id, name, short_name, active, created_at, updated_at)
SELECT id, CONCAT('벤치 홈 ', code), '홈', TRUE, NOW(6), NOW(6)
FROM sports
WHERE code IN ('BASEBALL', 'FOOTBALL', 'BASKETBALL', 'VOLLEYBALL');

INSERT INTO teams (sport_id, name, short_name, active, created_at, updated_at)
SELECT id, CONCAT('벤치 어웨이 ', code), '어웨이', TRUE, NOW(6), NOW(6)
FROM sports
WHERE code IN ('BASEBALL', 'FOOTBALL', 'BASKETBALL', 'VOLLEYBALL');

SELECT s.id AS sport_id, s.code, @venue_id AS venue_id,
       home.id AS home_team_id, away.id AS away_team_id
FROM sports s
JOIN teams home ON home.sport_id = s.id AND home.name = CONCAT('벤치 홈 ', s.code)
JOIN teams away ON away.sport_id = s.id AND away.name = CONCAT('벤치 어웨이 ', s.code)
WHERE s.code IN ('BASEBALL', 'FOOTBALL', 'BASKETBALL', 'VOLLEYBALL')
ORDER BY s.id;
