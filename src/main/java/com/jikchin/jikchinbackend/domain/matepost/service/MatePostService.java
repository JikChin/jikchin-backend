package com.jikchin.jikchinbackend.domain.matepost.service;

import com.jikchin.jikchinbackend.domain.matemember.entity.MateMember;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatePostService {

  private final MatePostRepository matePostRepository;
  private final MateMemberRepository mateMemberRepository;

  @Transactional
  public MatePostResponse create(Long userId, MatePostCreateRequest request) {
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
}
