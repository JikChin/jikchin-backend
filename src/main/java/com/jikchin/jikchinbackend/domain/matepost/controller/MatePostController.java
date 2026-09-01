package com.jikchin.jikchinbackend.domain.matepost.controller;

import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/mate-posts")
@RequiredArgsConstructor
public class MatePostController {

  private static final String USER_ID_HEADER = "X-User-Id";

  private final MatePostService matePostService;

  @PostMapping
  public ApiResponse<MatePostResponse> create(
      @RequestHeader(USER_ID_HEADER) @Positive Long userId,
      @Valid @RequestBody MatePostCreateRequest request) {
    MatePostResponse response = matePostService.create(userId, request);
    return ApiResponse.success(response);
  }

  @GetMapping("/{matePostId}")
  public ApiResponse<MatePostResponse> getById(@PathVariable Long matePostId) {
    return ApiResponse.success(matePostService.getById(matePostId));
  }
}
