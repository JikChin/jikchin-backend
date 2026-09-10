package com.jikchin.jikchinbackend.domain.event.repository;

import com.jikchin.jikchinbackend.domain.event.entity.Event;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
  @EntityGraph(attributePaths = {"sport", "venue", "homeTeam", "awayTeam"})
  List<Event> findAllByOrderByStartsAtAsc();

  @EntityGraph(attributePaths = {"sport", "venue", "homeTeam", "awayTeam"})
  List<Event> findBySportIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
      Long sportId, LocalDateTime from, LocalDateTime to, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"sport", "venue", "homeTeam", "awayTeam"})
  java.util.Optional<Event> findById(Long id);
}
