package com.jikchin.jikchinbackend.domain.matepost.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MatePostCreateRequest(
    @NotNull Long eventId,
    @NotBlank @Size(max = 200) String title,
    @NotBlank String content,
    @Min(2) int maxMembers,
    @Size(max = 20) String preferredGender,
    @Min(0) @Max(150) Integer minAge,
    @Min(0) @Max(150) Integer maxAge,
    @Size(max = 100) String seatInfo) {}
