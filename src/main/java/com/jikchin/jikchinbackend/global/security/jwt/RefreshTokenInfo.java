package com.jikchin.jikchinbackend.global.security.jwt;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenInfo(UUID tokenId, UUID memberKey, Instant expiresAt) {}
