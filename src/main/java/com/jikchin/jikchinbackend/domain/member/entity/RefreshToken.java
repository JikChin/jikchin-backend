package com.jikchin.jikchinbackend.domain.member.entity;

import com.jikchin.jikchinbackend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "refresh_tokens",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_refresh_tokens_token_id", columnNames = "token_id"))
public class RefreshToken extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "token_id", nullable = false, updatable = false, length = 36)
  private UUID tokenId;

  @Column(name = "member_key", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
  private UUID memberKey;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  private RefreshToken(UUID tokenId, UUID memberKey, Instant expiresAt) {
    this.tokenId = tokenId;
    this.memberKey = memberKey;
    this.expiresAt = expiresAt;
  }

  public static RefreshToken create(UUID tokenId, UUID memberKey, Instant expiresAt) {
    return new RefreshToken(tokenId, memberKey, expiresAt);
  }
}
