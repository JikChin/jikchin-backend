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
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "mate_posts",
    indexes = {
      @Index(name = "idx_mate_posts_event_status", columnList = "event_id,status"),
      @Index(name = "idx_mate_posts_created_at", columnList = "created_at")
    })
public class MatePost {

  public static final int INITIAL_MEMBER_COUNT = 1;
  public static final int TITLE_MAX_LENGTH = 200;
  public static final int PREFERRED_GENDER_MAX_LENGTH = 20;
  public static final int SEAT_INFO_MAX_LENGTH = 100;
  public static final int STATUS_MAX_LENGTH = 30;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "event_id", nullable = false)
  private Long eventId;

  @Column(nullable = false, length = TITLE_MAX_LENGTH)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "max_members", nullable = false)
  private int maxMembers;

  @Column(name = "current_members", nullable = false)
  private int currentMembers;

  @Column(name = "preferred_gender", length = PREFERRED_GENDER_MAX_LENGTH)
  private String preferredGender;

  @Column(name = "min_age")
  private Integer minAge;

  @Column(name = "max_age")
  private Integer maxAge;

  @Column(name = "seat_info", length = SEAT_INFO_MAX_LENGTH)
  private String seatInfo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = STATUS_MAX_LENGTH)
  private MatePostStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected MatePost() {}

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
    this.currentMembers = INITIAL_MEMBER_COUNT;
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
