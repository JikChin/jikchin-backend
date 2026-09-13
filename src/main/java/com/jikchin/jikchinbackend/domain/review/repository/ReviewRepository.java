package com.jikchin.jikchinbackend.domain.review.repository;

import com.jikchin.jikchinbackend.domain.review.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByMatePost_IdAndReviewerIdAndRevieweeId(
      Long matePostId, Long reviewerId, Long revieweeId);

  List<Review> findAllByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);
}
