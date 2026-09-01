package com.jikchin.jikchinbackend.domain.matepost.dto.response;

import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePostStatus;
import java.time.LocalDateTime;

public record MatePostResponse(
    Long id,
    Long userId,
    Long eventId,
    String title,
    String content,
    int maxMembers,
    int currentMembers,
    String preferredGender,
    Integer minAge,
    Integer maxAge,
    String seatInfo,
    MatePostStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static MatePostResponse from(MatePost post) {
    return new MatePostResponse(
        post.getId(),
        post.getUserId(),
        post.getEventId(),
        post.getTitle(),
        post.getContent(),
        post.getMaxMembers(),
        post.getCurrentMembers(),
        post.getPreferredGender(),
        post.getMinAge(),
        post.getMaxAge(),
        post.getSeatInfo(),
        post.getStatus(),
        post.getCreatedAt(),
        post.getUpdatedAt());
  }
}
