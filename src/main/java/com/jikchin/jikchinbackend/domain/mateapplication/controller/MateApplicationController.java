package com.jikchin.jikchinbackend.domain.mateapplication.controller;

import com.jikchin.jikchinbackend.domain.mateapplication.dto.request.MateApplicationCreateRequest;
import com.jikchin.jikchinbackend.domain.mateapplication.dto.response.MateApplicationResponse;
import com.jikchin.jikchinbackend.domain.mateapplication.service.MateApplicationService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/mate-posts/{matePostId}/applications")
@RequiredArgsConstructor
public class MateApplicationController {

  private static final String USER_ID_HEADER = "X-User-Id";

  private final MateApplicationService mateApplicationService;

  @PostMapping
  public ApiResponse<MateApplicationResponse> apply(
      @PathVariable @Positive Long matePostId,
      @RequestHeader(USER_ID_HEADER) @Positive Long userId,
      @Valid @RequestBody MateApplicationCreateRequest request) {
    return ApiResponse.success(mateApplicationService.apply(matePostId, userId, request));
  }

  @GetMapping
  public ApiResponse<List<MateApplicationResponse>> getApplications(
      @PathVariable @Positive Long matePostId,
      @RequestHeader(USER_ID_HEADER) @Positive Long requesterId) {
    return ApiResponse.success(mateApplicationService.getApplications(matePostId, requesterId));
  }

  @PatchMapping("/{applicationId}/accept")
  public ApiResponse<MateApplicationResponse> accept(
      @PathVariable @Positive Long matePostId,
      @PathVariable @Positive Long applicationId,
      @RequestHeader(USER_ID_HEADER) @Positive Long requesterId) {
    return ApiResponse.success(
        mateApplicationService.accept(matePostId, applicationId, requesterId));
  }

  @PatchMapping("/{applicationId}/reject")
  public ApiResponse<MateApplicationResponse> reject(
      @PathVariable @Positive Long matePostId,
      @PathVariable @Positive Long applicationId,
      @RequestHeader(USER_ID_HEADER) @Positive Long requesterId) {
    return ApiResponse.success(
        mateApplicationService.reject(matePostId, applicationId, requesterId));
  }
}
