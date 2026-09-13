package com.jikchin.jikchinbackend.domain.review.repository;

import com.jikchin.jikchinbackend.domain.review.entity.ReviewStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReviewStatsRepository extends JpaRepository<ReviewStats, Long> {

  /**
   * 별점 1건을 집계에 더한다. 행이 없으면 만들고, 있으면 제자리에서 증가시킨다.
   *
   * <p>한 문장으로 끝나는 upsert라 같은 피리뷰어에게 동시에 들어오는 리뷰는 이 행의 배타 락에서 직렬화된다. 그것이 이 방식의 비용이며, 대신 통계 조회는 PK 1건
   * 읽기(O(1))가 된다. 어느 점수 칸을 올릴지는 호출부가 0/1로 넘겨 SQL 방언 차이(불리언 산술)를 피한다.
   */
  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO review_stats
            (reviewee_id, total_count, score_sum, count_1, count_2, count_3, count_4, count_5)
          VALUES (:revieweeId, 1, :score, :c1, :c2, :c3, :c4, :c5)
          ON DUPLICATE KEY UPDATE
            total_count = total_count + 1,
            score_sum = score_sum + :score,
            count_1 = count_1 + :c1,
            count_2 = count_2 + :c2,
            count_3 = count_3 + :c3,
            count_4 = count_4 + :c4,
            count_5 = count_5 + :c5
          """,
      nativeQuery = true)
  void applyScore(
      @Param("revieweeId") Long revieweeId,
      @Param("score") int score,
      @Param("c1") int c1,
      @Param("c2") int c2,
      @Param("c3") int c3,
      @Param("c4") int c4,
      @Param("c5") int c5);

  /** 집계 행에서 평균을 계산해 회원의 매너 점수에 반영한다. reviews 테이블은 읽지 않는다. */
  @Transactional
  @Modifying
  @Query(
      value =
          """
          UPDATE members
          SET manner_score = (
            SELECT ROUND(score_sum * 1.0 / total_count, 2)
            FROM review_stats
            WHERE reviewee_id = :memberId
          )
          WHERE id = :memberId
          """,
      nativeQuery = true)
  void syncMannerScore(@Param("memberId") Long memberId);
}
