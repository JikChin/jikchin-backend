package com.jikchin.jikchinbackend.domain.review.repository;

import com.jikchin.jikchinbackend.domain.review.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByMatePost_IdAndReviewerIdAndRevieweeId(
      Long matePostId, Long reviewerId, Long revieweeId);

  List<Review> findAllByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

  @Modifying
  @Query(
      value =
          "UPDATE members SET manner_score = "
              + "(SELECT ROUND(AVG(r.score), 2) FROM reviews r WHERE r.reviewee_id = :memberId) "
              + "WHERE id = :memberId",
      nativeQuery = true)
  void updateMannerScore(@Param("memberId") Long memberId);
}
