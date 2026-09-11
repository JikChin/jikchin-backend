package com.jikchin.jikchinbackend.domain.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jikchin.jikchinbackend.domain.event.dto.response.EventResponse;
import com.jikchin.jikchinbackend.domain.event.entity.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:event-read;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.generate_statistics=true"
    })
@Transactional
class EventReadIntegrationTest {
  @Autowired EventService service;
  @Autowired EntityManager em;

  @Test
  void filtersLimitsAndOrdersResultsWithTwoQueriesWithoutLazyLoads() {
    LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
    LocalDateTime to = from.plusMonths(2);
    Sport sport = new Sport(SportCode.BASEBALL, "야구");
    em.persist(sport);
    Venue venue = new Venue("테스트 구장", "서울", "주소");
    em.persist(venue);
    Team home = new Team(sport, "홈팀", "홈", null);
    Team away = new Team(sport, "원정팀", "원정", null);
    em.persist(home);
    em.persist(away);
    save(sport, venue, home, away, from.plusDays(1));
    Event first = save(sport, venue, home, away, from);
    Event second = save(sport, venue, home, away, from);
    save(sport, venue, home, away, from.minusSeconds(1));
    save(sport, venue, home, away, to);
    Sport other = new Sport(SportCode.FOOTBALL, "축구");
    em.persist(other);
    Team otherHome = new Team(other, "축구 홈", "홈", null);
    Team otherAway = new Team(other, "축구 원정", "원정", null);
    em.persist(otherHome);
    em.persist(otherAway);
    save(other, venue, otherHome, otherAway, from);
    em.flush();
    em.clear();
    Statistics statistics =
        em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    var result = service.getEventsBySportAndPeriod(sport.getId(), from, to, 2);

    assertThat(result).extracting(EventResponse::id).containsExactly(first.getId(), second.getId());
    assertThat(result)
        .allSatisfy(
            event -> {
              assertThat(event.sport()).isEqualTo("야구");
              assertThat(event.home().name()).isEqualTo("홈팀");
              assertThat(event.away().name()).isEqualTo("원정팀");
              assertThat(event.venue().name()).isEqualTo("테스트 구장");
            });
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  @Test
  void emptyResultDoesNotFetchDetails() {
    Statistics statistics =
        em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
    assertThat(service.getEventsBySportAndPeriod(-1L, from, from.plusMonths(2), 20)).isEmpty();
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
  }

  private Event save(Sport sport, Venue venue, Team home, Team away, LocalDateTime startsAt) {
    Event event = Event.create(sport, venue, home, away, "테스트 리그", startsAt);
    em.persist(event);
    return event;
  }
}
