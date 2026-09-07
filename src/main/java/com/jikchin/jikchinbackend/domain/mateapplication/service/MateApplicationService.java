package com.jikchin.jikchinbackend.domain.mateapplication.service;

import com.jikchin.jikchinbackend.domain.mateapplication.dto.request.MateApplicationCreateRequest;
import com.jikchin.jikchinbackend.domain.mateapplication.dto.response.MateApplicationResponse;
import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplication;
import com.jikchin.jikchinbackend.domain.mateapplication.repository.MateApplicationRepository;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMember;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePostStatus;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MateApplicationService {

  private final MateApplicationRepository mateApplicationRepository;
  private final MateMemberRepository mateMemberRepository;
  private final MatePostRepository matePostRepository;

  @Transactional
  public MateApplicationResponse apply(
      Long matePostId, Long userId, MateApplicationCreateRequest request) {
    MatePost matePost = getMatePostForUpdate(matePostId);

    if (matePost.getStatus() != MatePostStatus.OPEN) {
      throw new IllegalStateException("모집이 마감된 글에는 신청할 수 없습니다.");
    }
    if (matePost.getUserId().equals(userId)) {
      throw new IllegalStateException("모집자는 자신의 모집글에 신청할 수 없습니다.");
    }
    if (mateMemberRepository.existsByMatePost_IdAndUserId(matePostId, userId)) {
      throw new IllegalStateException("이미 참가 중이거나 참가 이력이 있는 사용자입니다.");
    }
    if (mateApplicationRepository.existsByMatePost_IdAndUserId(matePostId, userId)) {
      throw new IllegalStateException("이미 신청한 모집글입니다.");
    }

    MateApplication application = MateApplication.apply(matePost, userId, request.message());
    return MateApplicationResponse.from(mateApplicationRepository.save(application));
  }

  @Transactional(readOnly = true)
  public List<MateApplicationResponse> getApplications(Long matePostId, Long requesterId) {
    MatePost matePost = getMatePost(matePostId);
    requireOwner(matePost, requesterId);

    return mateApplicationRepository.findAllByMatePost_IdOrderByCreatedAtAsc(matePostId).stream()
        .map(MateApplicationResponse::from)
        .toList();
  }

  @Transactional
  public MateApplicationResponse accept(Long matePostId, Long applicationId, Long requesterId) {
    MatePost matePost = getMatePostForUpdate(matePostId);
    requireOwner(matePost, requesterId);
    MateApplication application = getApplication(matePostId, applicationId);

    if (!application.isPending()) {
      throw new IllegalStateException("이미 처리된 참가 신청입니다.");
    }
    if (mateMemberRepository.existsByMatePost_IdAndUserId(matePostId, application.getUserId())) {
      throw new IllegalStateException("이미 참가 중이거나 참가 이력이 있는 사용자입니다.");
    }

    matePost.addMember();
    application.accept();
    mateMemberRepository.save(MateMember.join(matePost, application.getUserId()));
    return MateApplicationResponse.from(application);
  }

  @Transactional
  public MateApplicationResponse reject(Long matePostId, Long applicationId, Long requesterId) {
    MatePost matePost = getMatePostForUpdate(matePostId);
    requireOwner(matePost, requesterId);
    MateApplication application = getApplication(matePostId, applicationId);
    application.reject();
    return MateApplicationResponse.from(application);
  }

  private MatePost getMatePost(Long matePostId) {
    return matePostRepository
        .findById(matePostId)
        .orElseThrow(() -> new EntityNotFoundException("모집글을 찾을 수 없습니다."));
  }

  private MatePost getMatePostForUpdate(Long matePostId) {
    return matePostRepository
        .findByIdForUpdate(matePostId)
        .orElseThrow(() -> new EntityNotFoundException("모집글을 찾을 수 없습니다."));
  }

  private MateApplication getApplication(Long matePostId, Long applicationId) {
    return mateApplicationRepository
        .findByIdAndMatePost_Id(applicationId, matePostId)
        .orElseThrow(() -> new EntityNotFoundException("참가 신청을 찾을 수 없습니다."));
  }

  private void requireOwner(MatePost matePost, Long requesterId) {
    if (!matePost.getUserId().equals(requesterId)) {
      throw new IllegalStateException("모집자만 참가 신청을 처리할 수 있습니다.");
    }
  }
}
