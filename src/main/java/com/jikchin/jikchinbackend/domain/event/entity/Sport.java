package com.jikchin.jikchinbackend.domain.event.entity;

import com.jikchin.jikchinbackend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "sports",
    uniqueConstraints = @UniqueConstraint(name = "uk_sports_code", columnNames = "code"))
public class Sport extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SportCode code;

  @Column(nullable = false, length = 30)
  private String name;

  @Column(nullable = false)
  private boolean active = true;

  public Sport(SportCode code, String name) {
    this.code = code;
    this.name = name;
  }
}
