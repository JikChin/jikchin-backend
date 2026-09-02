package com.jikchin.jikchinbackend.domain.mateapplication.entity;

import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "mate_applications",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_mate_application",
            columnNames = {"mate_post_id", "user_id"}),
    indexes =
        @Index(name = "idx_mate_applications_post_status", columnList = "mate_post_id,status"))
public class MateApplication {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "mate_post_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_mate_applications_post"))
  private MatePost matePost;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MateApplicationStatus status;

  @Column(length = 500)
  private String message;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  private MateApplication(MatePost matePost, Long userId, String message) {
    this.matePost = matePost;
    this.userId = userId;
    this.status = MateApplicationStatus.PENDING;
    this.message = message;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  public static MateApplication apply(MatePost matePost, Long userId, String message) {
    return new MateApplication(matePost, userId, message);
  }

  public void accept() {
    requirePending();
    status = MateApplicationStatus.ACCEPTED;
    updatedAt = LocalDateTime.now();
  }

  public void reject() {
    requirePending();
    status = MateApplicationStatus.REJECTED;
    updatedAt = LocalDateTime.now();
  }

  public boolean isPending() {
    return status == MateApplicationStatus.PENDING;
  }

  private void requirePending() {
    if (!isPending()) {
      throw new IllegalStateException("이미 처리된 참가 신청입니다.");
    }
  }
}
