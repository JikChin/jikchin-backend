package com.jikchin.jikchinbackend.domain.member.repository;

import com.jikchin.jikchinbackend.domain.member.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshToken> findByTokenIdAndMemberKey(UUID tokenId, UUID memberKey);
}
