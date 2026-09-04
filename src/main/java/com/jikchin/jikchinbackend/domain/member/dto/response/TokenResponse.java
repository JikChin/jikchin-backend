package com.jikchin.jikchinbackend.domain.member.dto.response;

import com.jikchin.jikchinbackend.global.security.jwt.TokenPair;
import java.time.Instant;

public record TokenResponse(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt) {

  public static TokenResponse from(TokenPair tokenPair) {
    return new TokenResponse(
        tokenPair.accessToken(),
        tokenPair.accessTokenExpiresAt(),
        tokenPair.refreshToken(),
        tokenPair.refreshTokenExpiresAt());
  }
}
