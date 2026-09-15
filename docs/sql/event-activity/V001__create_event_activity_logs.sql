-- Run against the selected service database, NOT the benchmark database.
-- One-time migration; existing tables cause an error instead of being changed.
SET @activity_first_month = DATE_FORMAT(UTC_DATE() - INTERVAL 12 MONTH, '%Y-%m-01');
SET @activity_ddl = CONCAT('
CREATE TABLE event_activity_logs (
 id BIGINT NOT NULL AUTO_INCREMENT,
 occurred_at DATETIME(6) NOT NULL,
 event_id BIGINT NOT NULL,
 actor_id BIGINT NULL,
 subject_id BIGINT NULL,
 mate_post_id BIGINT NOT NULL,
 activity_type VARCHAR(40) NOT NULL,
 score_delta INT NOT NULL,
 source_type VARCHAR(30) NOT NULL,
 source_id BIGINT NOT NULL,
 idempotency_key VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 payload JSON NULL,
 created_at DATETIME(6) NOT NULL,
 PRIMARY KEY (id, occurred_at),
 UNIQUE KEY uk_activity (idempotency_key, occurred_at),
 KEY idx_period_score (occurred_at, event_id, score_delta),
 KEY idx_event_period (event_id, occurred_at)
) ENGINE=InnoDB
PARTITION BY RANGE COLUMNS (occurred_at) (
 PARTITION p_before VALUES LESS THAN (''', @activity_first_month, '''),
 PARTITION pmax VALUES LESS THAN (MAXVALUE)
)');
PREPARE activity_statement FROM @activity_ddl;
EXECUTE activity_statement;
DEALLOCATE PREPARE activity_statement;
-- The maintenance job splits pmax into monthly partitions, including the retained past months.
-- Keep recording disabled until monthly preparation has completed and been verified.
