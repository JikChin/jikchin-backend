package com.jikchin.jikchinbackend.domain.eventactivity;

import static org.assertj.core.api.Assertions.*;

import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "activity-log.enabled=true")
@Sql("/event-activity-schema.sql")
class EventActivityIntegrationTest {
  @Autowired MatePostService posts;
  @Autowired MatePostRepository postRepository;
  @Autowired MemberRepository members;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactionManager;

  private Member member() {
    String id = UUID.randomUUID().toString();
    return members.save(Member.create(id + "@example.com", "encoded", id, null, null, null, null));
  }

  private MatePostCreateRequest request() {
    return new MatePostCreateRequest(10L, "직관 모집", "함께 응원해요", 3, "ANY", 20, 40, "1루");
  }

  @Test
  void writesActivityWithBusinessTransaction() {
    Member member = member();
    var post = posts.create(member.getMemberKey(), request());
    var rows =
        jdbc.queryForList("SELECT * FROM event_activity_logs WHERE mate_post_id = ?", post.id());
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst())
        .containsEntry("activity_type", "MATE_POST_CREATED")
        .containsEntry("score_delta", 5)
        .containsEntry("actor_id", member.getId());
  }

  @Test
  void outerRollbackAlsoRollsBackJdbcLog() {
    Member member = member();
    long before = postRepository.count();
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status -> {
                          posts.create(member.getMemberKey(), request());
                          assertThat(
                                  jdbc.queryForObject(
                                      "SELECT COUNT(*) FROM event_activity_logs", Long.class))
                              .isEqualTo(1);
                          throw new IllegalStateException("rollback");
                        }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(postRepository.count()).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM event_activity_logs", Long.class))
        .isZero();
  }

  @Test
  void logFailureRollsBackBusinessInsert() {
    Member member = member();
    long before = postRepository.count();
    jdbc.execute(
        "ALTER TABLE event_activity_logs ADD CONSTRAINT reject_activity CHECK (score_delta < 0)");
    try {
      assertThatThrownBy(() -> posts.create(member.getMemberKey(), request()))
          .isInstanceOf(RuntimeException.class);
      assertThat(postRepository.count()).isEqualTo(before);
    } finally {
      jdbc.execute("ALTER TABLE event_activity_logs DROP CONSTRAINT reject_activity");
    }
  }

  @Test
  void monthlyAggregationAndCursorHaveStableBoundaries() {
    Clock fixed = Clock.fixed(Instant.parse("2026-09-16T00:00:00Z"), ZoneOffset.UTC);
    EventActivityQueryService service = new EventActivityQueryService(jdbc, fixed);
    for (int i = 0; i < 3; i++) {
      jdbc.update(
          "INSERT INTO event_activity_logs (occurred_at,event_id,mate_post_id,activity_type,score_delta,source_type,source_id,idempotency_key,created_at) VALUES (?,?,?,?,?,?,?,?,?)",
          LocalDateTime.of(2026, 9, 1, 0, 0),
          10L,
          1L,
          "REVIEW_CREATED",
          3,
          "REVIEW",
          i,
          "review:" + i,
          LocalDateTime.of(2026, 9, 1, 0, 0));
    }
    var scores = service.monthly(YearMonth.of(2026, 9), 20);
    assertThat(scores).containsExactly(new EventActivityQueryService.MonthlyScore(10L, 3, 9));
    var from = LocalDateTime.of(2026, 9, 1, 0, 0);
    var to = from.plusMonths(1);
    var first = service.find(from, to, null, null, null, null, 2);
    assertThat(first.hasNext()).isTrue();
    var next =
        service.find(
            from, to, null, null, first.nextCursor().occurredAt(), first.nextCursor().id(), 2);
    assertThat(next.items()).hasSize(1);
    assertThat(next.hasNext()).isFalse();
    assertThat(first.items()).doesNotContainAnyElementsOf(next.items());
    assertThatThrownBy(() -> service.monthly(YearMonth.of(2025, 8), 20))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }
}
