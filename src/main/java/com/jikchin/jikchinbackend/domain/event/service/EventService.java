package com.jikchin.jikchinbackend.domain.event.service;

import com.jikchin.jikchinbackend.domain.event.dto.request.EventCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.SportCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.TeamCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.VenueCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.response.CatalogResponse;
import com.jikchin.jikchinbackend.domain.event.dto.response.EventResponse;
import com.jikchin.jikchinbackend.domain.event.entity.Event;
import com.jikchin.jikchinbackend.domain.event.entity.Sport;
import com.jikchin.jikchinbackend.domain.event.entity.Team;
import com.jikchin.jikchinbackend.domain.event.entity.Venue;
import com.jikchin.jikchinbackend.domain.event.repository.EventRepository;
import com.jikchin.jikchinbackend.domain.event.repository.SportRepository;
import com.jikchin.jikchinbackend.domain.event.repository.TeamRepository;
import com.jikchin.jikchinbackend.domain.event.repository.VenueRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {
  private final EventRepository eventRepository;
  private final SportRepository sportRepository;
  private final TeamRepository teamRepository;
  private final VenueRepository venueRepository;

  @Transactional
  public EventResponse create(EventCreateRequest request) {
    Sport sport =
        sportRepository
            .findById(request.sportId())
            .orElseThrow(() -> new EntityNotFoundException("종목을 찾을 수 없습니다."));
    Venue venue =
        venueRepository
            .findById(request.venueId())
            .orElseThrow(() -> new EntityNotFoundException("경기장을 찾을 수 없습니다."));
    Team home =
        teamRepository
            .findById(request.homeTeamId())
            .orElseThrow(() -> new EntityNotFoundException("홈팀을 찾을 수 없습니다."));
    Team away =
        teamRepository
            .findById(request.awayTeamId())
            .orElseThrow(() -> new EntityNotFoundException("원정팀을 찾을 수 없습니다."));
    return EventResponse.from(
        eventRepository.save(
            Event.create(
                sport, venue, home, away, request.leagueName().trim(), request.startsAt())));
  }

  @Transactional
  public CatalogResponse createSport(SportCreateRequest request) {
    Sport sport = sportRepository.save(new Sport(request.code(), request.name().trim()));
    return new CatalogResponse(sport.getId(), sport.getName());
  }

  @Transactional
  public CatalogResponse createTeam(TeamCreateRequest request) {
    Sport sport =
        sportRepository
            .findById(request.sportId())
            .orElseThrow(() -> new EntityNotFoundException("종목을 찾을 수 없습니다."));
    Team team =
        teamRepository.save(
            new Team(sport, request.name().trim(), request.shortName(), request.logoUrl()));
    return new CatalogResponse(team.getId(), team.getName());
  }

  @Transactional
  public CatalogResponse createVenue(VenueCreateRequest request) {
    Venue venue =
        venueRepository.save(
            new Venue(request.name().trim(), request.region().trim(), request.address()));
    return new CatalogResponse(venue.getId(), venue.getName());
  }

  @Transactional(readOnly = true)
  public List<EventResponse> getEvents() {
    return eventRepository.findAllByOrderByStartsAtAsc().stream().map(EventResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<EventResponse> getEventsBySportAndPeriod(
      Long sportId, LocalDateTime from, LocalDateTime to, int size) {
    return eventRepository
        .findBySportIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            sportId, from, to, PageRequest.of(0, size))
        .stream()
        .map(EventResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public EventResponse getEvent(Long eventId) {
    return EventResponse.from(
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("경기를 찾을 수 없습니다.")));
  }
}
