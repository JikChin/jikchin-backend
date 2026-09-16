package com.jikchin.jikchinbackend.domain.eventactivity;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ActivityConfiguration {
  @Bean
  public Clock activityClock() {
    return Clock.systemUTC();
  }
}
