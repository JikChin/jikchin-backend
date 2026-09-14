package com.jikchin.jikchinbackend.domain.review.repository;

import com.jikchin.jikchinbackend.domain.review.entity.Review;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByMatePost_IdAndReviewerIdAndRevieweeId(
      Long matePostId, Long reviewerId, Long revieweeId);

  /**
   * 집계 행이 없는 회원(집계 테이블 도입 전에 리뷰를 받았고 아직 백필되지 않은 경우)의 통계를 원본에서 계산한다. 정상 경로는 review_stats PK 조회이며, 이
   * 쿼리는 그 행이 없을 때만 실행된다.
   */
  @Query(
      """
      select r.score as score, count(r) as count
      from Review r
      where r.revieweeId = :revieweeId
      group by r.score
      """)
  List<ScoreCount> countByScoreForReviewee(@Param("revieweeId") Long revieweeId);

  /** 첫 페이지. 최신순이며 같은 시각이면 id가 큰 쪽이 먼저다. */
  List<Review> findAllByRevieweeIdOrderByCreatedAtDescIdDesc(Long revieweeId, Pageable pageable);

  /**
   * 커서 다음 페이지. OFFSET 대신 (created_at, id) 키셋으로 이어 읽으므로 몇 페이지를 넘겨도 건너뛰는 행이 없다. (reviewee_id,
   * created_at) 인덱스가 있으면 정렬 없이 인덱스를 역방향으로 size건만 읽는다.
   */
  @Query(
      """
      select r from Review r
      where r.revieweeId = :revieweeId
        and (r.createdAt < :cursorCreatedAt
          or (r.createdAt = :cursorCreatedAt and r.id < :cursorId))
      order by r.createdAt desc, r.id desc
      """)
  List<Review> findReceivedBeforeCursor(
      @Param("revieweeId") Long revieweeId,
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable);
}
