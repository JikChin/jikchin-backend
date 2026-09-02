package com.jikchin.jikchinbackend.domain.matemember.dto.response;

import com.jikchin.jikchinbackend.domain.matemember.entity.MateMember;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import java.time.LocalDateTime;

public record MateMemberResponse(
    Long id, Long matePostId, Long userId, LocalDateTime joinedAt, MateMemberStatus status) {

  public static MateMemberResponse from(MateMember mateMember) {
    return new MateMemberResponse(
        mateMember.getId(),
        mateMember.getMatePost().getId(),
        mateMember.getUserId(),
        mateMember.getJoinedAt(),
        mateMember.getStatus());
  }
}
