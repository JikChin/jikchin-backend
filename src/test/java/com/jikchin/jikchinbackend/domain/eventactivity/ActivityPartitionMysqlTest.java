package com.jikchin.jikchinbackend.domain.eventactivity;

import static org.assertj.core.api.Assertions.*;

import com.jikchin.jikchinbackend.domain.eventactivity.partition.ActivityPartitionManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Uses a fresh randomly named schema only, never application/benchmark tables. */
@EnabledIfEnvironmentVariable(named = "ACTIVITY_MYSQL_TEST_URL", matches = ".+")
class ActivityPartitionMysqlTest {
  @Test
  void bootstrapCreatePruneRetryAndLockOnRealMysql() throws Exception {
    var ds = new DriverManagerDataSource(System.getenv("ACTIVITY_MYSQL_TEST_URL"), "root", "");
    String schema = "activity_test_" + UUID.randomUUID().toString().replace("-", "");
    JdbcTemplate admin = new JdbcTemplate(ds);
    admin.execute("CREATE DATABASE " + schema);
    try {
      // Pin one real connection so DATABASE() and GET_LOCK() use the selected schema.
      try (var connection = ds.getConnection()) {
        connection.setCatalog(schema);
        JdbcTemplate jdbc =
            new JdbcTemplate(
                new org.springframework.jdbc.datasource.SingleConnectionDataSource(
                    connection, true));
        String script =
            Files.readString(
                    Path.of("docs/sql/event-activity/V001__create_event_activity_logs.sql"))
                .replace("INTERVAL 12 MONTH", "INTERVAL 14 MONTH");
        script =
            script
                .lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(java.util.stream.Collectors.joining("\n"));
        for (String statement : script.split(";"))
          if (!statement.isBlank()) jdbc.execute(statement);
        var manager = new ActivityPartitionManager(jdbc);
        YearMonth now = YearMonth.now(java.time.ZoneOffset.UTC);
        manager.maintain(now, false);
        String expired =
            "p"
                + now.minusMonths(13)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String retained =
            "p"
                + now.minusMonths(12)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        for (int offset : new int[] {13, 12}) {
          var at = now.minusMonths(offset).atDay(1).atStartOfDay();
          jdbc.update(
              "INSERT INTO event_activity_logs (occurred_at,event_id,mate_post_id,activity_type,score_delta,source_type,source_id,idempotency_key,created_at) VALUES (?,1,1,'REVIEW_CREATED',3,'REVIEW',?,?,?)",
              at,
              offset,
              "review:" + offset,
              at);
        }
        assertThat(
                jdbc.queryForObject(
                    "SELECT COUNT(*) FROM event_activity_logs PARTITION (" + expired + ")",
                    Long.class))
            .isEqualTo(1);
        manager.maintain(now, true);
        manager.maintain(now, true);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM event_activity_logs", Long.class))
            .isEqualTo(1);
        assertThat(
                jdbc.queryForObject(
                    "SELECT COUNT(*) FROM event_activity_logs PARTITION (" + retained + ")",
                    Long.class))
            .isEqualTo(1);
        // Another physical connection holds the lock: maintenance must skip.
        try (var other = ds.getConnection()) {
          other.setCatalog(schema);
          JdbcTemplate blocker =
              new JdbcTemplate(
                  new org.springframework.jdbc.datasource.SingleConnectionDataSource(other, true));
          String lock =
              jdbc.queryForObject(
                  "SELECT CONCAT('activity:', LEFT(SHA2(DATABASE(), 256), 48))", String.class);
          blocker.queryForObject("SELECT GET_LOCK(?,0)", Integer.class, lock);
          manager.maintain(now.plusMonths(1), true);
          assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM event_activity_logs", Long.class))
              .isEqualTo(1);
          blocker.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, lock);
        }
      }
    } finally {
      admin.execute("DROP DATABASE " + schema);
    }
  }
}
