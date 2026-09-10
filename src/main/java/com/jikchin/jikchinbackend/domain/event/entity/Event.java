package com.jikchin.jikchinbackend.domain.event.entity;

import com.jikchin.jikchinbackend.global.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "events",
    indexes = {
      @Index(name = "idx_events_starts_at", columnList = "starts_at"),
      @Index(name = "idx_events_sport_starts_at", columnList = "sport_id,starts_at")
    })
public class Event extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sport_id", nullable = false)
  private Sport sport;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "venue_id", nullable = false)
  private Venue venue;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "home_team_id", nullable = false)
  private Team homeTeam;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "away_team_id", nullable = false)
  private Team awayTeam;

  @Column(name = "league_name", nullable = false, length = 100)
  private String leagueName;

  @Column(name = "starts_at", nullable = false)
  private LocalDateTime startsAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EventStatus status;

  private Event(
      Sport sport,
      Venue venue,
      Team homeTeam,
      Team awayTeam,
      String leagueName,
      LocalDateTime startsAt) {
    if (homeTeam.getId().equals(awayTeam.getId()))
      throw new IllegalArgumentException("홈팀과 원정팀은 달라야 합니다.");
    if (!sport.getId().equals(homeTeam.getSport().getId())
        || !sport.getId().equals(awayTeam.getSport().getId()))
      throw new IllegalArgumentException("경기 종목과 팀 종목이 일치하지 않습니다.");
    this.sport = sport;
    this.venue = venue;
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
    this.leagueName = leagueName;
    this.startsAt = startsAt;
    this.status = EventStatus.SCHEDULED;
  }

  public static Event create(
      Sport sport,
      Venue venue,
      Team homeTeam,
      Team awayTeam,
      String leagueName,
      LocalDateTime startsAt) {
    return new Event(sport, venue, homeTeam, awayTeam, leagueName, startsAt);
  }

  public void changeStatus(EventStatus status) {
    this.status = status;
  }
}
