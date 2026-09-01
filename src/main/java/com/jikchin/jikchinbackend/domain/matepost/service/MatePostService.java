package com.jikchin.jikchinbackend.domain.matepost.service;

import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatePostService {

  private final MatePostRepository matePostRepository;

  public MatePostService(MatePostRepository matePostRepository) {
    this.matePostRepository = matePostRepository;
  }

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
    return MatePostResponse.from(matePostRepository.save(matePost));
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
