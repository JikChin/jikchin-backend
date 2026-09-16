package com.jikchin.jikchinbackend.domain.eventactivity.partition;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ActivityPartitionJob {
  private final ActivityPartitionManager manager;
  private final Clock clock;
  private final boolean deleteEnabled;

  public ActivityPartitionJob(
      ActivityPartitionManager manager,
      @Qualifier("activityClock") Clock clock,
      @Value("${activity-log.partition.delete-enabled:false}") boolean deleteEnabled) {
    this.manager = manager;
    this.clock = clock;
    this.deleteEnabled = deleteEnabled;
  }

  @Scheduled(cron = "${activity-log.partition.cron:-}", zone = "Asia/Seoul")
  public void run() {
    try {
      manager.maintain(YearMonth.now(clock.withZone(ZoneOffset.UTC)), deleteEnabled);
    } catch (RuntimeException e) {
      log.error("활동 로그 파티션 관리 실패. 다음 실행에서 실제 경계를 재확인합니다.", e);
    }
  }
}
