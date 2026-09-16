package com.jikchin.jikchinbackend.domain.eventactivity.partition;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityPartitionManager {
  private final JdbcTemplate jdbc;

  @Transactional(propagation = Propagation.NEVER)
  public void maintain(YearMonth currentMonth, boolean deleteEnabled) {
    jdbc.execute(
        (ConnectionCallback<Void>)
            connection -> {
              if (!connection.getAutoCommit())
                throw new IllegalStateException("파티션 DDL은 autocommit 연결이 필요합니다.");
              JdbcTemplate session =
                  new JdbcTemplate(new SingleConnectionDataSource(connection, true));
              String database = session.queryForObject("SELECT DATABASE()", String.class);
              if (database == null) throw new IllegalStateException("대상 데이터베이스가 없습니다.");
              String lock =
                  session.queryForObject(
                      "SELECT CONCAT('activity:', LEFT(SHA2(DATABASE(), 256), 48))", String.class);
              Integer acquired =
                  session.queryForObject("SELECT GET_LOCK(?, 0)", Integer.class, lock);
              if (!Integer.valueOf(1).equals(acquired)) {
                log.info("활동 로그 관리 작업이 이미 실행 중입니다.");
                return null;
              }
              Long oldTimeout = null;
              try {
                oldTimeout =
                    session.queryForObject("SELECT @@SESSION.lock_wait_timeout", Long.class);
                session.execute("SET SESSION lock_wait_timeout = 5");
                var boundaries =
                    session.query(
                        """
            SELECT PARTITION_NAME, PARTITION_DESCRIPTION, PARTITION_METHOD, PARTITION_EXPRESSION
            FROM information_schema.PARTITIONS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_activity_logs'
            ORDER BY PARTITION_ORDINAL_POSITION
            """,
                        (rs, n) ->
                            new PartitionPlan.Boundary(
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getString(4)));
                PartitionPlan plan = PartitionPlan.from(boundaries, currentMonth);
                for (YearMonth month : plan.create()) {
                  String name = "p" + month.format(DateTimeFormatter.ofPattern("yyyyMM"));
                  execute(
                      session,
                      "ALTER TABLE event_activity_logs REORGANIZE PARTITION pmax INTO (PARTITION "
                          + name
                          + " VALUES LESS THAN ('"
                          + month.plusMonths(1).atDay(1)
                          + "'), PARTITION pmax VALUES LESS THAN (MAXVALUE))");
                }
                for (String name : plan.drop()) {
                  if (deleteEnabled)
                    execute(
                        session, "ALTER TABLE event_activity_logs DROP PARTITION `" + name + "`");
                  else
                    log.info(
                        "활동 로그 삭제 후보: partition={}, cutoffMonth={}",
                        name,
                        currentMonth.minusMonths(12));
                }
                Boolean unexpected =
                    session.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM event_activity_logs PARTITION (p_before) LIMIT 1)",
                        Boolean.class);
                if (Boolean.TRUE.equals(unexpected))
                  log.warn("p_before에 예외 데이터가 있습니다. 보관 정책과 적재 경로를 확인하세요.");
              } finally {
                try {
                  if (oldTimeout != null)
                    session.execute("SET SESSION lock_wait_timeout = " + oldTimeout);
                } finally {
                  session.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, lock);
                }
              }
              return null;
            });
  }

  private void execute(JdbcTemplate session, String sql) {
    long start = System.nanoTime();
    session.execute(sql);
    log.info("활동 로그 파티션 관리 완료: sql={}, elapsedMs={}", sql, (System.nanoTime() - start) / 1_000_000);
  }
}
