package com.jikchin.jikchinbackend.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
    @NotNull Long matePostId,
    @NotNull Long revieweeId,
    @Min(1) @Max(5) int score,
    @Size(max = 1000) String content) {}
