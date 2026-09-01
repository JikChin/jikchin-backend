package com.jikchin.jikchinbackend.domain.matemember.repository;

import com.jikchin.jikchinbackend.domain.matemember.entity.MateMember;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateMemberRepository extends JpaRepository<MateMember, Long> {

  boolean existsByMatePost_IdAndUserId(Long matePostId, Long userId);

  boolean existsByMatePost_IdAndUserIdAndStatus(
      Long matePostId, Long userId, MateMemberStatus status);

  Optional<MateMember> findByMatePost_IdAndUserId(Long matePostId, Long userId);

  List<MateMember> findAllByMatePost_IdAndStatusOrderByJoinedAtAsc(
      Long matePostId, MateMemberStatus status);
}
