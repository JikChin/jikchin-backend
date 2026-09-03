package com.jikchin.jikchinbackend.domain.matepost.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "mate_posts",
    indexes = {
      @Index(name = "idx_mate_posts_event_status", columnList = "event_id,status"),
      @Index(name = "idx_mate_posts_created_at", columnList = "created_at")
    })
public class MatePost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "event_id", nullable = false)
  private Long eventId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "max_members", nullable = false)
  private int maxMembers;

  @Column(name = "current_members", nullable = false)
  private int currentMembers;

  @Column(name = "preferred_gender", length = 20)
  private String preferredGender;

  @Column(name = "min_age")
  private Integer minAge;

  @Column(name = "max_age")
  private Integer maxAge;

  @Column(name = "seat_info", length = 100)
  private String seatInfo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MatePostStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  private MatePost(
      Long userId,
      Long eventId,
      String title,
      String content,
      int maxMembers,
      String preferredGender,
      Integer minAge,
      Integer maxAge,
      String seatInfo) {
    validateAgeRange(minAge, maxAge);
    this.userId = userId;
    this.eventId = eventId;
    this.title = title;
    this.content = content;
    this.maxMembers = maxMembers;
    this.currentMembers = 1;
    this.preferredGender = preferredGender;
    this.minAge = minAge;
    this.maxAge = maxAge;
    this.seatInfo = seatInfo;
    this.status = MatePostStatus.OPEN;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  public static MatePost create(
      Long userId,
      Long eventId,
      String title,
      String content,
      int maxMembers,
      String preferredGender,
      Integer minAge,
      Integer maxAge,
      String seatInfo) {
    return new MatePost(
        userId, eventId, title, content, maxMembers, preferredGender, minAge, maxAge, seatInfo);
  }

  public void addMember() {
    if (status != MatePostStatus.OPEN || currentMembers >= maxMembers) {
      throw new IllegalStateException("모집 정원이 마감되었습니다.");
    }
    currentMembers++;
    if (currentMembers == maxMembers) {
      status = MatePostStatus.CLOSED;
    }
    updatedAt = LocalDateTime.now();
  }

  private static void validateAgeRange(Integer minAge, Integer maxAge) {
    if (minAge != null && maxAge != null && minAge > maxAge) {
      throw new IllegalArgumentException("최소 나이는 최대 나이보다 클 수 없습니다.");
    }
  }
}
