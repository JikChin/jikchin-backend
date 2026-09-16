package com.jikchin.jikchinbackend.domain.eventactivity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventActivityRecorder {
  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final boolean enabled;

  public EventActivityRecorder(
      JdbcTemplate jdbc,
      @Qualifier("activityClock") Clock clock,
      @Value("${activity-log.enabled:false}") boolean enabled) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.enabled = enabled;
  }

  // JDBC participates in the existing JpaTransactionManager transaction on the same DataSource.
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(
      ActivityType type,
      Long eventId,
      Long actorId,
      Long subjectId,
      Long matePostId,
      Long sourceId) {
    if (!enabled) return;
    LocalDateTime now =
        LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    jdbc.update(
        """
        INSERT INTO event_activity_logs
        (occurred_at, event_id, actor_id, subject_id, mate_post_id, activity_type,
         score_delta, source_type, source_id, idempotency_key, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        now,
        eventId,
        actorId,
        subjectId,
        matePostId,
        type.name(),
        type.score(),
        type.sourceType(),
        sourceId,
        type.name() + ":" + sourceId,
        now);
  }
}
