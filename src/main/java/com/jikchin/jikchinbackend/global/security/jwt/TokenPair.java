package com.jikchin.jikchinbackend.global.security.jwt;

import java.time.Instant;
import java.util.UUID;

public record TokenPair(
    String accessToken,
    UUID accessTokenId,
    Instant accessTokenExpiresAt,
    String refreshToken,
    UUID refreshTokenId,
    Instant refreshTokenExpiresAt) {}
