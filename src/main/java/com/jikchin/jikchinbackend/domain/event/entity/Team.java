package com.jikchin.jikchinbackend.domain.event.entity;

import com.jikchin.jikchinbackend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "teams",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_teams_sport_name",
            columnNames = {"sport_id", "name"}))
public class Team extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sport_id", nullable = false)
  private Sport sport;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "short_name", length = 30)
  private String shortName;

  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @Column(nullable = false)
  private boolean active = true;

  public Team(Sport sport, String name, String shortName, String logoUrl) {
    this.sport = sport;
    this.name = name;
    this.shortName = shortName;
    this.logoUrl = logoUrl;
  }
}
