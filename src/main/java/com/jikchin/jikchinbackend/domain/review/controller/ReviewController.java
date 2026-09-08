package com.jikchin.jikchinbackend.domain.review.controller;

import com.jikchin.jikchinbackend.domain.review.dto.request.ReviewCreateRequest;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewResponse;
import com.jikchin.jikchinbackend.domain.review.service.ReviewService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping("/api/reviews")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ReviewResponse> create(
      @AuthenticationPrincipal UUID memberKey, @Valid @RequestBody ReviewCreateRequest request) {
    return ApiResponse.success(reviewService.create(memberKey, request));
  }

  @GetMapping("/api/members/{memberId}/reviews")
  public ApiResponse<List<ReviewResponse>> getReceivedReviews(@PathVariable Long memberId) {
    return ApiResponse.success(reviewService.getReceivedReviews(memberId));
  }
}
