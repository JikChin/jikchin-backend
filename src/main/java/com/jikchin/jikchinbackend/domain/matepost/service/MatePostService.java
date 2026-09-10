package com.jikchin.jikchinbackend.domain.matepost.service;

import com.jikchin.jikchinbackend.domain.matemember.entity.MateMember;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatePostService {

  private final MatePostRepository matePostRepository;
  private final MemberRepository memberRepository;
  private final MateMemberRepository mateMemberRepository;

  @Transactional
  public MatePostResponse create(UUID memberKey, MatePostCreateRequest request) {
    Long userId = getMemberId(memberKey);
    MatePost matePost =
        MatePost.create(
            userId,
            request.eventId(),
            request.title(),
            request.content(),
            request.maxMembers(),
            request.preferredGender(),
            request.minAge(),
            request.maxAge(),
            request.seatInfo());
    MatePost savedMatePost = matePostRepository.save(matePost);
    mateMemberRepository.save(MateMember.join(savedMatePost, userId));
    return MatePostResponse.from(savedMatePost);
  }

  @Transactional(readOnly = true)
  public MatePostResponse getById(Long matePostId) {
    MatePost matePost =
        matePostRepository
            .findById(matePostId)
            .orElseThrow(() -> new EntityNotFoundException("모집글을 찾을 수 없습니다."));
    return MatePostResponse.from(matePost);
  }

  private Long getMemberId(UUID memberKey) {
    return memberRepository
        .findByMemberKey(memberKey)
        .map(Member::getId)
        .orElseThrow(() -> new AppException(ErrorType.MEMBER_NOT_FOUND));
  }
}
