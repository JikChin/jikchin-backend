package com.jikchin.jikchinbackend.domain.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jikchin.jikchinbackend.domain.review.dto.request.ReviewCreateRequest;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewResponse;
import com.jikchin.jikchinbackend.domain.review.service.ReviewService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import com.jikchin.jikchinbackend.global.response.ResultType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

  @Mock private ReviewService reviewService;

  private ReviewController reviewController;

  @BeforeEach
  void setUp() {
    reviewController = new ReviewController(reviewService);
  }

  @Test
  void createsReviewWrappedWithApiResponse() {
    UUID memberKey = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(10L, 2L, 4, "시간 약속을 잘 지켜요.");
    ReviewResponse review = createResponse();
    when(reviewService.create(memberKey, request)).thenReturn(review);

    ApiResponse<ReviewResponse> response = reviewController.create(memberKey, request);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(review);
    assertThat(response.getError()).isNull();
  }

  @Test
  void returnsReceivedReviewsWrappedWithApiResponse() {
    List<ReviewResponse> reviews = List.of(createResponse());
    when(reviewService.getReceivedReviews(2L)).thenReturn(reviews);

    ApiResponse<List<ReviewResponse>> response = reviewController.getReceivedReviews(2L);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(reviews);
    assertThat(response.getError()).isNull();
  }

  private ReviewResponse createResponse() {
    return new ReviewResponse(1L, 10L, 1L, 2L, 4, "시간 약속을 잘 지켜요.", LocalDateTime.now());
  }
}
