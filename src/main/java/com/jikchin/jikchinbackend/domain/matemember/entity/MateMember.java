package com.jikchin.jikchinbackend.domain.matemember.entity;

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

@Entity
@Table(
    name = "mate_members",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_mate_member",
            columnNames = {"mate_post_id", "user_id"}),
    indexes = @Index(name = "idx_mate_members_user", columnList = "user_id"))
public class MateMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "mate_post_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_mate_members_post"))
  private MatePost matePost;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MateMemberStatus status;

  protected MateMember() {}

  private MateMember(MatePost matePost, Long userId) {
    this.matePost = matePost;
    this.userId = userId;
    this.joinedAt = LocalDateTime.now();
    this.status = MateMemberStatus.ACTIVE;
  }

  public static MateMember join(MatePost matePost, Long userId) {
    return new MateMember(matePost, userId);
  }

  public void leave() {
    if (status == MateMemberStatus.LEFT) {
      throw new IllegalStateException("이미 탈퇴한 메이트 멤버입니다.");
    }
    status = MateMemberStatus.LEFT;
  }

  public boolean isActive() {
    return status == MateMemberStatus.ACTIVE;
  }

  public Long getId() {
    return id;
  }

  public MatePost getMatePost() {
    return matePost;
  }

  public Long getUserId() {
    return userId;
  }

  public LocalDateTime getJoinedAt() {
    return joinedAt;
  }

  public MateMemberStatus getStatus() {
    return status;
  }
}
