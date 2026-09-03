package com.jikchin.jikchinbackend.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {}
