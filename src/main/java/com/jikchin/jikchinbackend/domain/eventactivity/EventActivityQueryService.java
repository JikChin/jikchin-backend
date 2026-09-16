package com.jikchin.jikchinbackend.domain.eventactivity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EventActivityQueryService {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public EventActivityQueryService(JdbcTemplate jdbc, @Qualifier("activityClock") Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  public record Activity(
      Long id,
      LocalDateTime occurredAt,
      Long eventId,
      Long actorId,
      Long subjectId,
      Long matePostId,
      String activityType,
      int scoreDelta,
      String sourceType,
      Long sourceId) {}

  public record Cursor(LocalDateTime occurredAt, Long id) {}

  public record Slice(List<Activity> items, boolean hasNext, Cursor nextCursor) {}

  public record MonthlyScore(Long eventId, long activityCount, long scoreDelta) {}

  public Slice find(
      LocalDateTime from,
      LocalDateTime to,
      Long eventId,
      ActivityType type,
      LocalDateTime cursorAt,
      Long cursorId,
      int size) {
    validateRange(from, to);
    validateSize(size);
    if ((cursorAt == null) != (cursorId == null)) throw bad("커서 시각과 ID를 함께 지정하세요.");
    if (cursorAt != null && (cursorId <= 0 || cursorAt.isBefore(from) || !cursorAt.isBefore(to)))
      throw bad("조회 범위 안의 유효한 커서를 지정하세요.");
    if (eventId != null && eventId <= 0) throw bad("경기 ID는 양수여야 합니다.");
    StringBuilder sql =
        new StringBuilder(
            """
        SELECT id, occurred_at, event_id, actor_id, subject_id, mate_post_id,
               activity_type, score_delta, source_type, source_id
        FROM event_activity_logs WHERE occurred_at >= ? AND occurred_at < ?
        """);
    List<Object> args = new ArrayList<>(List.of(from, to));
    if (eventId != null) {
      sql.append(" AND event_id = ?");
      args.add(eventId);
    }
    if (type != null) {
      sql.append(" AND activity_type = ?");
      args.add(type.name());
    }
    if (cursorAt != null) {
      sql.append(" AND (occurred_at < ? OR (occurred_at = ? AND id < ?))");
      args.add(cursorAt);
      args.add(cursorAt);
      args.add(cursorId);
    }
    sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT ?");
    args.add(size + 1);
    List<Activity> rows =
        jdbc.query(
            sql.toString(),
            (rs, n) ->
                new Activity(
                    rs.getLong("id"),
                    rs.getObject("occurred_at", LocalDateTime.class),
                    rs.getLong("event_id"),
                    rs.getObject("actor_id", Long.class),
                    rs.getObject("subject_id", Long.class),
                    rs.getLong("mate_post_id"),
                    rs.getString("activity_type"),
                    rs.getInt("score_delta"),
                    rs.getString("source_type"),
                    rs.getLong("source_id")),
            args.toArray());
    boolean hasNext = rows.size() > size;
    List<Activity> items = List.copyOf(rows.subList(0, Math.min(size, rows.size())));
    Activity last = items.isEmpty() ? null : items.getLast();
    return new Slice(items, hasNext, hasNext ? new Cursor(last.occurredAt(), last.id()) : null);
  }

  public List<MonthlyScore> monthly(YearMonth month, int limit) {
    validateSize(limit);
    LocalDateTime from = month.atDay(1).atStartOfDay();
    LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
    validateRange(from, to);
    return jdbc.query(
        """
        SELECT event_id, COUNT(*) AS activity_count, SUM(score_delta) AS score_delta
        FROM event_activity_logs WHERE occurred_at >= ? AND occurred_at < ?
        GROUP BY event_id ORDER BY score_delta DESC, event_id ASC LIMIT ?
        """,
        (rs, n) -> new MonthlyScore(rs.getLong(1), rs.getLong(2), rs.getLong(3)),
        from,
        to,
        limit);
  }

  private void validateRange(LocalDateTime from, LocalDateTime to) {
    YearMonth current = YearMonth.now(clock.withZone(ZoneOffset.UTC));
    if (from == null
        || to == null
        || !from.isBefore(to)
        || from.isBefore(current.minusMonths(12).atDay(1).atStartOfDay())
        || to.isAfter(current.plusMonths(1).atDay(1).atStartOfDay()))
      throw bad("UTC 기준 보관 기간 안에서 시작 시각 이상·종료 시각 미만 범위를 지정하세요.");
  }

  private void validateSize(int size) {
    if (size < 1 || size > 100) throw bad("조회 크기는 1~100입니다.");
  }

  private ResponseStatusException bad(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
  }
}
