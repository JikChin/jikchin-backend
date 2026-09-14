package com.jikchin.jikchinbackend.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 회원이 받은 리뷰의 별점 집계. 리뷰 1건이 저장될 때 같은 트랜잭션에서 {@code ReviewStatsRepository.applyScore}가 갱신하므로 항상
 * {@code reviews}와 같은 값이다.
 *
 * <p>읽기 전용 프로젝션이다. 갱신은 원자적 upsert SQL로만 하고, JPA로 이 엔티티를 수정하지 않는다.
 */
@Getter
@Entity
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_stats")
public class ReviewStats {

  @Id
  @Column(name = "reviewee_id")
  private Long revieweeId;

  @Column(name = "total_count", nullable = false)
  private long totalCount;

  @Column(name = "score_sum", nullable = false)
  private long scoreSum;

  @Column(name = "count_1", nullable = false)
  private long count1;

  @Column(name = "count_2", nullable = false)
  private long count2;

  @Column(name = "count_3", nullable = false)
  private long count3;

  @Column(name = "count_4", nullable = false)
  private long count4;

  @Column(name = "count_5", nullable = false)
  private long count5;

  public long countOf(int score) {
    return switch (score) {
      case 1 -> count1;
      case 2 -> count2;
      case 3 -> count3;
      case 4 -> count4;
      case 5 -> count5;
      default -> throw new IllegalArgumentException("별점은 1점부터 5점까지만 가능합니다.");
    };
  }
}
