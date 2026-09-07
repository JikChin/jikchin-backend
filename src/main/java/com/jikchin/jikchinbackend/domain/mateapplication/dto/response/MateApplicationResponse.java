package com.jikchin.jikchinbackend.domain.mateapplication.dto.response;

import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplication;
import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplicationStatus;
import java.time.LocalDateTime;

public record MateApplicationResponse(
    Long id,
    Long matePostId,
    Long userId,
    MateApplicationStatus status,
    String message,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static MateApplicationResponse from(MateApplication application) {
    return new MateApplicationResponse(
        application.getId(),
        application.getMatePost().getId(),
        application.getUserId(),
        application.getStatus(),
        application.getMessage(),
        application.getCreatedAt(),
        application.getUpdatedAt());
  }
}
