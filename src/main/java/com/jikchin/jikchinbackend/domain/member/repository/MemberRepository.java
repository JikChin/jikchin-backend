package com.jikchin.jikchinbackend.domain.member.repository;

import com.jikchin.jikchinbackend.domain.member.entity.Member;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

  Optional<Member> findByEmail(String email);

  Optional<Member> findByMemberKey(UUID memberKey);

  boolean existsByEmail(String email);

  boolean existsByNickname(String nickname);
}
