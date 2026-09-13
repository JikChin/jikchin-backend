package com.jikchin.jikchinbackend.domain.review.entity;

import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "reviews",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_review_once",
            columnNames = {"mate_post_id", "reviewer_id", "reviewee_id"}),
    indexes = {
      @Index(name = "idx_reviews_reviewee", columnList = "reviewee_id"),
      // 받은 리뷰 목록(최신순 커서 페이징)이 정렬 없이 인덱스를 역방향으로 읽게 한다.
      // idx_reviews_reviewee는 이 인덱스의 왼쪽 접두사와 중복이지만 ddl-auto: update가 기존 인덱스를 지우지
      // 않으므로 선언을 남겨 둔다. 정리는 마이그레이션에서 한다.
      @Index(name = "idx_reviews_reviewee_created", columnList = "reviewee_id, created_at"),
      @Index(name = "fk_reviews_reviewer", columnList = "reviewer_id")
    })
public class Review {

  public static final int MIN_SCORE = 1;
  public static final int MAX_SCORE = 5;
  public static final int CONTENT_MAX_LENGTH = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "mate_post_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_reviews_post"))
  private MatePost matePost;

  @Column(name = "reviewer_id", nullable = false)
  private Long reviewerId;

  @Column(name = "reviewee_id", nullable = false)
  private Long revieweeId;

  @Column(nullable = false, columnDefinition = "TINYINT")
  private int score;

  @Column(length = CONTENT_MAX_LENGTH)
  private String content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private Review(MatePost matePost, Long reviewerId, Long revieweeId, int score, String content) {
    validateScore(score);
    this.matePost = matePost;
    this.reviewerId = reviewerId;
    this.revieweeId = revieweeId;
    this.score = score;
    this.content = content;
    this.createdAt = LocalDateTime.now();
  }

  public static Review create(
      MatePost matePost, Long reviewerId, Long revieweeId, int score, String content) {
    return new Review(matePost, reviewerId, revieweeId, score, content);
  }

  private static void validateScore(int score) {
    if (score < MIN_SCORE || score > MAX_SCORE) {
      throw new IllegalArgumentException("별점은 1점부터 5점까지만 가능합니다.");
    }
  }
}
