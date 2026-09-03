package com.jikchin.jikchinbackend.global.security.jwt;

import com.jikchin.jikchinbackend.domain.member.entity.Role;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final JwtProperties properties;

  public TokenPair issue(UUID memberKey, Role role) {
    Instant issuedAt = Instant.now();
    UUID accessTokenId = UUID.randomUUID();
    UUID refreshTokenId = UUID.randomUUID();
    Instant accessTokenExpiresAt = issuedAt.plus(properties.accessTokenExpiration());
    Instant refreshTokenExpiresAt = issuedAt.plus(properties.refreshTokenExpiration());

    return new TokenPair(
        createToken(
            memberKey, role, TokenType.ACCESS, accessTokenId, issuedAt, accessTokenExpiresAt),
        accessTokenId,
        accessTokenExpiresAt,
        createToken(
            memberKey, role, TokenType.REFRESH, refreshTokenId, issuedAt, refreshTokenExpiresAt),
        refreshTokenId,
        refreshTokenExpiresAt);
  }

  public JwtAuthenticationInfo parseAccessToken(String accessToken) {
    Jwt jwt = decode(accessToken, ErrorType.INVALID_ACCESS_TOKEN);
    validateTokenType(jwt, TokenType.ACCESS, ErrorType.INVALID_ACCESS_TOKEN);

    try {
      return new JwtAuthenticationInfo(
          UUID.fromString(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString("role")));
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorType.INVALID_ACCESS_TOKEN);
    }
  }

  public RefreshTokenInfo parseRefreshToken(String refreshToken) {
    Jwt jwt = decode(refreshToken, ErrorType.INVALID_REFRESH_TOKEN);
    validateTokenType(jwt, TokenType.REFRESH, ErrorType.INVALID_REFRESH_TOKEN);

    try {
      return new RefreshTokenInfo(
          UUID.fromString(jwt.getId()), UUID.fromString(jwt.getSubject()), jwt.getExpiresAt());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorType.INVALID_REFRESH_TOKEN);
    }
  }

  private String createToken(
      UUID memberKey,
      Role role,
      TokenType tokenType,
      UUID tokenId,
      Instant issuedAt,
      Instant expiresAt) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(memberKey.toString())
            .id(tokenId.toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("role", role.name())
            .claim("token_type", tokenType.name())
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private Jwt decode(String token, ErrorType errorType) {
    try {
      return jwtDecoder.decode(token);
    } catch (JwtException exception) {
      throw new AppException(errorType);
    }
  }

  private void validateTokenType(Jwt jwt, TokenType tokenType, ErrorType errorType) {
    if (!tokenType.name().equals(jwt.getClaimAsString("token_type"))) {
      throw new AppException(errorType);
    }
  }
}
