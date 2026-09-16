package com.jikchin.jikchinbackend.domain.review.service;

import com.jikchin.jikchinbackend.domain.eventactivity.ActivityType;
import com.jikchin.jikchinbackend.domain.eventactivity.EventActivityRecorder;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.review.dto.request.ReviewCreateRequest;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewResponse;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewSliceResponse;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewStatsResponse;
import com.jikchin.jikchinbackend.domain.review.entity.Review;
import com.jikchin.jikchinbackend.domain.review.repository.ReviewRepository;
import com.jikchin.jikchinbackend.domain.review.repository.ReviewStatsRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final EventActivityRecorder activityRecorder;
  private final ReviewRepository reviewRepository;
  private final ReviewStatsRepository reviewStatsRepository;
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
    // 집계 행과 매너 점수를 같은 트랜잭션에서 갱신한다. 정상 경로는 reviews를 다시 읽지 않는다.
    reviewStatsRepository.applyScore(request.revieweeId(), request.score());
    reviewStatsRepository.syncMannerScore(request.revieweeId());
    activityRecorder.record(
        ActivityType.REVIEW_CREATED,
        matePost.getEventId(),
        reviewerId,
        request.revieweeId(),
        matePost.getId(),
        review.getId());
    return ReviewResponse.from(review);
  }

  /**
   * 받은 리뷰를 최신순으로 size건 돌려준다. cursor는 직전 페이지 마지막 리뷰의 id이며, 그 리뷰의 (createdAt, id) 뒤부터 이어 읽는다. size +
   * 1건을 조회해 다음 페이지 유무를 판단한다.
   */
  @Transactional(readOnly = true)
  public ReviewSliceResponse getReceivedReviews(Long memberId, Long cursor, int size) {
    requireMember(memberId);
    PageRequest limit = PageRequest.of(0, size + 1);
    List<Review> fetched;
    if (cursor == null) {
      fetched = reviewRepository.findAllByRevieweeIdOrderByCreatedAtDescIdDesc(memberId, limit);
    } else {
      Review cursorReview =
          reviewRepository
              .findById(cursor)
              .filter(review -> review.getRevieweeId().equals(memberId))
              .orElseThrow(() -> new AppException(ErrorType.REVIEW_CURSOR_INVALID));
      fetched =
          reviewRepository.findReceivedBeforeCursor(
              memberId, cursorReview.getCreatedAt(), cursorReview.getId(), limit);
    }
    return ReviewSliceResponse.of(fetched, size);
  }

  @Transactional(readOnly = true)
  public ReviewStatsResponse getReviewStats(Long memberId) {
    requireMember(memberId);
    // 집계 행이 없으면 아직 백필되지 않은 회원이므로 원본에서 계산한다. 리뷰를 받은 적이 없는 회원도 같은 경로로
    // 빈 통계가 된다.
    return reviewStatsRepository
        .findById(memberId)
        .map(ReviewStatsResponse::from)
        .orElseGet(
            () ->
                ReviewStatsResponse.of(
                    memberId, reviewRepository.countByScoreForReviewee(memberId)));
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
