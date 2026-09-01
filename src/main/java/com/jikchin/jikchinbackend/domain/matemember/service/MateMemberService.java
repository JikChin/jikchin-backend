package com.jikchin.jikchinbackend.domain.matemember.service;

import com.jikchin.jikchinbackend.domain.matemember.dto.response.MateMemberResponse;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMember;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MateMemberService {

  private final MateMemberRepository mateMemberRepository;
  private final MatePostRepository matePostRepository;

  @Transactional
  public MateMemberResponse addMember(Long matePostId, Long userId) {
    MatePost matePost =
        matePostRepository
            .findByIdForUpdate(matePostId)
            .orElseThrow(() -> new EntityNotFoundException("모집글을 찾을 수 없습니다."));

    if (mateMemberRepository.existsByMatePost_IdAndUserId(matePostId, userId)) {
      throw new IllegalStateException("이미 참가 중이거나 참가 이력이 있는 사용자입니다.");
    }

    matePost.addMember();
    return MateMemberResponse.from(mateMemberRepository.save(MateMember.join(matePost, userId)));
  }

  @Transactional(readOnly = true)
  public List<MateMemberResponse> getActiveMembers(Long matePostId) {
    requireMatePost(matePostId);
    return mateMemberRepository
        .findAllByMatePost_IdAndStatusOrderByJoinedAtAsc(matePostId, MateMemberStatus.ACTIVE)
        .stream()
        .map(MateMemberResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean isParticipating(Long matePostId, Long userId) {
    requireMatePost(matePostId);
    return mateMemberRepository.existsByMatePost_IdAndUserIdAndStatus(
        matePostId, userId, MateMemberStatus.ACTIVE);
  }

  private void requireMatePost(Long matePostId) {
    if (!matePostRepository.existsById(matePostId)) {
      throw new EntityNotFoundException("모집글을 찾을 수 없습니다.");
    }
  }
}
