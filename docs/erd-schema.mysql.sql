-- JikChin target schema (MySQL 8.0+)
-- Design reference only. Convert to a versioned migration after validating existing production data.

CREATE TABLE sports (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(30) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_sports_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teams (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sport_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  short_name VARCHAR(30) NULL,
  logo_url VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_teams_sport_name UNIQUE (sport_id, name),
  CONSTRAINT fk_teams_sport FOREIGN KEY (sport_id) REFERENCES sports (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE venues (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  region VARCHAR(100) NOT NULL,
  address VARCHAR(300) NULL,
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_venues_name_region UNIQUE (name, region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sport_id BIGINT NOT NULL,
  venue_id BIGINT NOT NULL,
  home_team_id BIGINT NOT NULL,
  away_team_id BIGINT NOT NULL,
  external_event_key VARCHAR(100) NULL,
  league_name VARCHAR(100) NOT NULL,
  starts_at DATETIME(6) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uk_events_external_key UNIQUE (external_event_key),
  CONSTRAINT chk_events_different_teams CHECK (home_team_id <> away_team_id),
  CONSTRAINT fk_events_sport FOREIGN KEY (sport_id) REFERENCES sports (id),
  CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues (id),
  CONSTRAINT fk_events_home_team FOREIGN KEY (home_team_id) REFERENCES teams (id),
  CONSTRAINT fk_events_away_team FOREIGN KEY (away_team_id) REFERENCES teams (id),
  INDEX idx_events_starts_at (starts_at),
  INDEX idx_events_sport_starts_at (sport_id, starts_at),
  INDEX idx_events_status_starts_at (status, starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Existing tables should gain these foreign keys after validating all legacy rows:
-- ALTER TABLE mate_posts ADD CONSTRAINT fk_mate_posts_member FOREIGN KEY (user_id) REFERENCES members(id);
-- ALTER TABLE mate_posts ADD CONSTRAINT fk_mate_posts_event FOREIGN KEY (event_id) REFERENCES events(id);
-- ALTER TABLE mate_applications ADD CONSTRAINT fk_mate_applications_member FOREIGN KEY (user_id) REFERENCES members(id);
-- ALTER TABLE mate_members ADD CONSTRAINT fk_mate_members_member FOREIGN KEY (user_id) REFERENCES members(id);
-- ALTER TABLE reviews ADD CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES members(id);
-- ALTER TABLE reviews ADD CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES members(id);
-- ALTER TABLE reports ADD CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES members(id);
-- ALTER TABLE reports ADD CONSTRAINT fk_reports_reported_member FOREIGN KEY (reported_user_id) REFERENCES members(id);

CREATE TABLE chat_rooms (
  id BIGINT NOT NULL AUTO_INCREMENT,
  mate_post_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  closed_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_chat_rooms_mate_post UNIQUE (mate_post_id),
  CONSTRAINT fk_chat_rooms_mate_post FOREIGN KEY (mate_post_id) REFERENCES mate_posts (id),
  CONSTRAINT chk_chat_rooms_status CHECK (status IN ('OPEN', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_room_members (
  id BIGINT NOT NULL AUTO_INCREMENT,
  chat_room_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  left_at DATETIME(6) NULL,
  last_read_message_id BIGINT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_chat_room_members UNIQUE (chat_room_id, member_id),
  CONSTRAINT fk_chat_room_members_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id),
  CONSTRAINT fk_chat_room_members_member FOREIGN KEY (member_id) REFERENCES members (id),
  INDEX idx_chat_room_members_member_room (member_id, chat_room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_messages (
  id BIGINT NOT NULL AUTO_INCREMENT,
  chat_room_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_chat_messages_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id),
  CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES members (id),
  INDEX idx_chat_messages_room_id (chat_room_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event_activity_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  activity_type VARCHAR(40) NOT NULL,
  score_delta INT NOT NULL,
  idempotency_key VARCHAR(120) NOT NULL,
  occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  processed_at DATETIME(6) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(500) NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_event_activity_outbox_idempotency UNIQUE (idempotency_key),
  CONSTRAINT fk_event_activity_outbox_event FOREIGN KEY (event_id) REFERENCES events (id),
  INDEX idx_event_activity_outbox_pending (processed_at, id),
  INDEX idx_event_activity_outbox_event_occurred (event_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
