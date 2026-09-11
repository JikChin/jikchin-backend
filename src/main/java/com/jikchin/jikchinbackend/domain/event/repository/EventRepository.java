package com.jikchin.jikchinbackend.domain.event.repository;

import com.jikchin.jikchinbackend.domain.event.entity.Event;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {
  @EntityGraph(attributePaths = {"sport", "venue", "homeTeam", "awayTeam"})
  List<Event> findAllByOrderByStartsAtAsc();

  @Query(
      """
      select e.id from Event e
      where e.sport.id = :sportId
        and e.startsAt >= :from
        and e.startsAt < :to
      order by e.startsAt asc, e.id asc
      """)
  List<Long> findEventIdsBySportAndPeriod(
      @Param("sportId") Long sportId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      Pageable pageable);

  @EntityGraph(attributePaths = {"sport", "venue", "homeTeam", "awayTeam"})
  @Query("select e from Event e where e.id in :eventIds")
  List<Event> findAllWithDetailsByIdIn(@Param("eventIds") Collection<Long> eventIds);

  @Override
  @EntityGraph(attributePaths = {"sport", "venue", "homeTeam", "awayTeam"})
  java.util.Optional<Event> findById(Long id);
}
