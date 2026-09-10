package com.jikchin.jikchinbackend.domain.matepost.controller;

import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/mate-posts")
@RequiredArgsConstructor
public class MatePostController {

  private final MatePostService matePostService;

  @PostMapping
  public ApiResponse<MatePostResponse> create(
      @AuthenticationPrincipal UUID memberKey, @Valid @RequestBody MatePostCreateRequest request) {
    MatePostResponse response = matePostService.create(memberKey, request);
    return ApiResponse.success(response);
  }

  @GetMapping("/{matePostId}")
  public ApiResponse<MatePostResponse> getById(@PathVariable Long matePostId) {
    return ApiResponse.success(matePostService.getById(matePostId));
  }
}
