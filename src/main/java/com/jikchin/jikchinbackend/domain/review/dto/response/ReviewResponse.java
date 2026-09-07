package com.jikchin.jikchinbackend.domain.review.dto.response;

import com.jikchin.jikchinbackend.domain.review.entity.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    Long matePostId,
    Long reviewerId,
    Long revieweeId,
    int score,
    String content,
    LocalDateTime createdAt) {

  public static ReviewResponse from(Review review) {
    return new ReviewResponse(
        review.getId(),
        review.getMatePost().getId(),
        review.getReviewerId(),
        review.getRevieweeId(),
        review.getScore(),
        review.getContent(),
        review.getCreatedAt());
  }
}
