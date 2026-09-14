package com.jikchin.jikchinbackend.domain.review.repository;

import com.jikchin.jikchinbackend.domain.review.entity.ReviewStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReviewStatsRepository extends JpaRepository<ReviewStats, Long> {

  /**
   * 방금 저장(flush)된 별점 1건을 집계에 더한다.
   *
   * <p>행이 있으면 제자리에서 증가시키고, 행이 없으면 {@code reviews}를 한 번 집계해 만든다. 집계 테이블이 나중에 도입됐으므로 그 전에 받은 리뷰가 있는
   * 회원은 행이 없을 수 있는데, 이때 새 리뷰 1건으로 행을 시작하면 과거 리뷰가 통계와 매너 점수에서 사라진다. INSERT 쪽이 원본에서 재계산하므로 백필을 빠뜨려도 첫
   * 쓰기에서 스스로 맞춰진다 (새 리뷰는 이미 flush됐으므로 재계산에 포함된다). 그 한 번만 O(N)이고 이후는 O(1)이다.
   *
   * <p>한 문장으로 끝나는 upsert라 같은 피리뷰어에게 동시에 들어오는 리뷰는 이 행의 배타 락에서 직렬화된다. 그것이 이 방식의 비용이며, 대신 통계 조회는 PK 1건
   * 읽기가 된다.
   */
  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO review_stats
            (reviewee_id, total_count, score_sum, count_1, count_2, count_3, count_4, count_5)
          SELECT
            :revieweeId,
            COUNT(*),
            COALESCE(SUM(score), 0),
            SUM(CASE WHEN score = 1 THEN 1 ELSE 0 END),
            SUM(CASE WHEN score = 2 THEN 1 ELSE 0 END),
            SUM(CASE WHEN score = 3 THEN 1 ELSE 0 END),
            SUM(CASE WHEN score = 4 THEN 1 ELSE 0 END),
            SUM(CASE WHEN score = 5 THEN 1 ELSE 0 END)
          FROM reviews
          WHERE reviewee_id = :revieweeId
          ON DUPLICATE KEY UPDATE
            total_count = total_count + 1,
            score_sum = score_sum + :score,
            count_1 = count_1 + CASE WHEN :score = 1 THEN 1 ELSE 0 END,
            count_2 = count_2 + CASE WHEN :score = 2 THEN 1 ELSE 0 END,
            count_3 = count_3 + CASE WHEN :score = 3 THEN 1 ELSE 0 END,
            count_4 = count_4 + CASE WHEN :score = 4 THEN 1 ELSE 0 END,
            count_5 = count_5 + CASE WHEN :score = 5 THEN 1 ELSE 0 END
          """,
      nativeQuery = true)
  void applyScore(@Param("revieweeId") Long revieweeId, @Param("score") int score);

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
