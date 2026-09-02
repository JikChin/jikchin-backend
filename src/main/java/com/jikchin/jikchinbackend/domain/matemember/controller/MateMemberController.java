package com.jikchin.jikchinbackend.domain.matemember.controller;

import com.jikchin.jikchinbackend.domain.matemember.dto.response.MateMemberResponse;
import com.jikchin.jikchinbackend.domain.matemember.service.MateMemberService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mate-posts/{matePostId}/members")
@RequiredArgsConstructor
public class MateMemberController {

  private final MateMemberService mateMemberService;

  @GetMapping
  public ApiResponse<List<MateMemberResponse>> getActiveMembers(@PathVariable Long matePostId) {
    return ApiResponse.success(mateMemberService.getActiveMembers(matePostId));
  }

  @GetMapping("/{userId}/participation")
  public ApiResponse<Boolean> isParticipating(
      @PathVariable Long matePostId, @PathVariable Long userId) {
    return ApiResponse.success(mateMemberService.isParticipating(matePostId, userId));
  }
}
