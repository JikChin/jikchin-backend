package com.jikchin.jikchinbackend.domain.review.service;

import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.review.dto.request.ReviewCreateRequest;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewResponse;
import com.jikchin.jikchinbackend.domain.review.entity.Review;
import com.jikchin.jikchinbackend.domain.review.repository.ReviewRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final MatePostRepository matePostRepository;
  private final MateMemberRepository mateMemberRepository;
  private final MemberRepository memberRepository;

  @Transactional
  public ReviewResponse create(UUID reviewerKey, ReviewCreateRequest request) {
    Long reviewerId = getMemberId(reviewerKey);
    if (reviewerId.equals(request.revieweeId())) {
      throw new AppException(ErrorType.REVIEW_SELF_NOT_ALLOWED);
    }

    MatePost matePost =
        matePostRepository
            .findById(request.matePostId())
            .orElseThrow(() -> new AppException(ErrorType.REVIEW_MATE_POST_NOT_FOUND));

    requireActiveMateMember(matePost.getId(), reviewerId, ErrorType.REVIEW_NOT_MATE_MEMBER);
    requireActiveMateMember(
        matePost.getId(), request.revieweeId(), ErrorType.REVIEW_NOT_MATE_MEMBER);

    // TODO: Event 도메인 추가 후 이벤트 종료 이후에만 작성 가능하도록 검증 추가
    if (reviewRepository.existsByMatePost_IdAndReviewerIdAndRevieweeId(
        matePost.getId(), reviewerId, request.revieweeId())) {
      throw new AppException(ErrorType.REVIEW_ALREADY_EXISTS);
    }

    Review review =
        Review.create(
            matePost, reviewerId, request.revieweeId(), request.score(), request.content());
    try {
      reviewRepository.save(review);
      reviewRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw new AppException(ErrorType.REVIEW_ALREADY_EXISTS);
    }
    reviewRepository.updateMannerScore(request.revieweeId());
    return ReviewResponse.from(review);
  }

  @Transactional(readOnly = true)
  public List<ReviewResponse> getReceivedReviews(Long memberId) {
    requireMember(memberId);
    return reviewRepository.findAllByRevieweeIdOrderByCreatedAtDesc(memberId).stream()
        .map(ReviewResponse::from)
        .toList();
  }

  private Long getMemberId(UUID memberKey) {
    return memberRepository
        .findByMemberKey(memberKey)
        .map(Member::getId)
        .orElseThrow(() -> new AppException(ErrorType.MEMBER_NOT_FOUND));
  }

  private void requireMember(Long memberId) {
    if (!memberRepository.existsById(memberId)) {
      throw new AppException(ErrorType.MEMBER_NOT_FOUND);
    }
  }

  private void requireActiveMateMember(Long matePostId, Long userId, ErrorType errorType) {
    if (!mateMemberRepository.existsByMatePost_IdAndUserIdAndStatus(
        matePostId, userId, MateMemberStatus.ACTIVE)) {
      throw new AppException(errorType);
    }
  }
}
