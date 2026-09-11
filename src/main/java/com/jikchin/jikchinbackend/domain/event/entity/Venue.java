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
    name = "venues",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_venues_name_region",
            columnNames = {"name", "region"}))
public class Venue extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 100)
  private String region;

  @Column(length = 300)
  private String address;

  public Venue(String name, String region, String address) {
    this.name = name;
    this.region = region;
    this.address = address;
  }
}
