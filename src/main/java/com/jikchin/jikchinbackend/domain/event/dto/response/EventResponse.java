package com.jikchin.jikchinbackend.domain.event.dto.response;

import com.jikchin.jikchinbackend.domain.event.entity.Event;
import com.jikchin.jikchinbackend.domain.event.entity.EventStatus;
import java.time.LocalDateTime;

public record EventResponse(
    Long id,
    String sport,
    String leagueName,
    TeamResponse home,
    TeamResponse away,
    VenueResponse venue,
    LocalDateTime startsAt,
    EventStatus status) {
  public static EventResponse from(Event event) {
    return new EventResponse(
        event.getId(),
        event.getSport().getName(),
        event.getLeagueName(),
        TeamResponse.from(event.getHomeTeam()),
        TeamResponse.from(event.getAwayTeam()),
        VenueResponse.from(event.getVenue()),
        event.getStartsAt(),
        event.getStatus());
  }

  public record TeamResponse(Long id, String name, String shortName, String logoUrl) {
    static TeamResponse from(com.jikchin.jikchinbackend.domain.event.entity.Team team) {
      return new TeamResponse(team.getId(), team.getName(), team.getShortName(), team.getLogoUrl());
    }
  }

  public record VenueResponse(Long id, String name, String region, String address) {
    static VenueResponse from(com.jikchin.jikchinbackend.domain.event.entity.Venue venue) {
      return new VenueResponse(
          venue.getId(), venue.getName(), venue.getRegion(), venue.getAddress());
    }
  }
}
