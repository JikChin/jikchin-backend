package com.jikchin.jikchinbackend.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matemember.service.MateMemberService;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.domain.member.entity.Gender;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.report.repository.ReportRepository;
import com.jikchin.jikchinbackend.domain.review.dto.request.ReviewCreateRequest;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewResponse;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewStatsResponse;
import com.jikchin.jikchinbackend.domain.review.entity.Review;
import com.jikchin.jikchinbackend.domain.review.repository.ReviewRepository;
import com.jikchin.jikchinbackend.domain.review.repository.ReviewStatsRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReviewServiceIntegrationTest {

  @Autowired private ReviewService reviewService;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private ReviewStatsRepository reviewStatsRepository;
  @Autowired private ReportRepository reportRepository;
  @Autowired private MatePostService matePostService;
  @Autowired private MateMemberService mateMemberService;
  @Autowired private MateMemberRepository mateMemberRepository;
  @Autowired private MatePostRepository matePostRepository;
  @Autowired private MemberRepository memberRepository;

  private Member reviewer;
  private Member reviewee;
  private Member outsider;
  private Long matePostId;

  @BeforeEach
  void setUp() {
    reviewRepository.deleteAll();
    reviewStatsRepository.deleteAll();
    reportRepository.deleteAll();
    mateMemberRepository.deleteAll();
    matePostRepository.deleteAll();
    memberRepository.deleteAll();

    reviewer = saveMember("reviewer@jikchin.com", "직관러버");
    reviewee = saveMember("reviewee@jikchin.com", "야구광");
    outsider = saveMember("outsider@jikchin.com", "구경꾼");

    MatePostResponse post = matePostService.create(reviewer.getMemberKey(), createPostRequest());
    matePostId = post.id();
    mateMemberService.addMember(matePostId, reviewee.getId());
  }

  @AfterEach
  void tearDown() {
    reviewRepository.deleteAll();
    reviewStatsRepository.deleteAll();
    reportRepository.deleteAll();
  }

  @Test
  void createsReviewAndRecalculatesMannerScore() {
    ReviewResponse response =
        reviewService.create(
            reviewer.getMemberKey(), createRequest(reviewee.getId(), 4, "시간 약속을 잘 지켜요."));

    assertThat(response.id()).isNotNull();
    assertThat(response.matePostId()).isEqualTo(matePostId);
    assertThat(response.reviewerId()).isEqualTo(reviewer.getId());
    assertThat(response.revieweeId()).isEqualTo(reviewee.getId());
    assertThat(response.score()).isEqualTo(4);

    BigDecimal mannerScore =
        memberRepository.findById(reviewee.getId()).orElseThrow().getMannerScore();
    assertThat(mannerScore).isEqualByComparingTo("4.00");
  }

  @Test
  void createUpdatesReviewStatsInSameTransaction() {
    reviewService.create(reviewer.getMemberKey(), createRequest(reviewee.getId(), 4, null));
    reviewService.create(reviewee.getMemberKey(), createRequest(reviewer.getId(), 2, null));

    ReviewStatsResponse revieweeStats = reviewService.getReviewStats(reviewee.getId());
    ReviewStatsResponse reviewerStats = reviewService.getReviewStats(reviewer.getId());

    assertThat(revieweeStats.totalCount()).isEqualTo(1);
    assertThat(revieweeStats.scoreCounts()).containsEntry(4, 1L).containsEntry(2, 0L);
    assertThat(reviewerStats.totalCount()).isEqualTo(1);
    assertThat(reviewerStats.scoreCounts()).containsEntry(2, 1L).containsEntry(4, 0L);
    assertThat(memberRepository.findById(reviewer.getId()).orElseThrow().getMannerScore())
        .isEqualByComparingTo("2.00");
  }

  @Test
  void rejectsSelfReview() {
    assertThatThrownBy(
            () ->
                reviewService.create(
                    reviewer.getMemberKey(), createRequest(reviewer.getId(), 5, null)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REVIEW_SELF_NOT_ALLOWED);
  }

  @Test
  void rejectsReviewWhenReviewerIsNotMateMember() {
    assertThatThrownBy(
            () ->
                reviewService.create(
                    outsider.getMemberKey(), createRequest(reviewee.getId(), 3, null)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REVIEW_NOT_MATE_MEMBER);
  }

  @Test
  void rejectsReviewWhenRevieweeIsNotMateMember() {
    assertThatThrownBy(
            () ->
                reviewService.create(
                    reviewer.getMemberKey(), createRequest(outsider.getId(), 3, null)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REVIEW_NOT_MATE_MEMBER);
  }

  @Test
  void rejectsDuplicateReview() {
    reviewService.create(reviewer.getMemberKey(), createRequest(reviewee.getId(), 4, null));

    assertThatThrownBy(
            () ->
                reviewService.create(
                    reviewer.getMemberKey(), createRequest(reviewee.getId(), 2, null)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REVIEW_ALREADY_EXISTS);
  }

  @Test
  void rejectsReviewForUnknownMatePost() {
    assertThatThrownBy(
            () ->
                reviewService.create(
                    reviewer.getMemberKey(),
                    new ReviewCreateRequest(999999L, reviewee.getId(), 3, null)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REVIEW_MATE_POST_NOT_FOUND);
  }

  @Test
  void returnsReceivedReviewsInLatestOrder() {
    reviewService.create(reviewer.getMemberKey(), createRequest(reviewee.getId(), 5, "최고의 직관 메이트"));
    reviewService.create(reviewee.getMemberKey(), createRequest(reviewer.getId(), 3, null));

    List<ReviewResponse> received = reviewService.getReceivedReviews(reviewee.getId());

    assertThat(received).hasSize(1);
    assertThat(received.getFirst().reviewerId()).isEqualTo(reviewer.getId());
    assertThat(received.getFirst().content()).isEqualTo("최고의 직관 메이트");
  }

  @Test
  void returnsScoreDistributionWithAverageAndMostFrequentScore() {
    saveReviews(reviewee.getId(), 5, 5, 4, 3, 5, 4);

    ReviewStatsResponse stats = reviewService.getReviewStats(reviewee.getId());

    assertThat(stats.revieweeId()).isEqualTo(reviewee.getId());
    assertThat(stats.totalCount()).isEqualTo(6);
    assertThat(stats.averageScore()).isEqualByComparingTo("4.33");
    assertThat(stats.mostFrequentScore()).isEqualTo(5);
    assertThat(stats.scoreCounts())
        .containsExactly(entry(1, 0L), entry(2, 0L), entry(3, 1L), entry(4, 2L), entry(5, 3L));
  }

  @Test
  void prefersHigherScoreWhenFrequenciesTie() {
    saveReviews(reviewee.getId(), 4, 4, 2, 2);

    ReviewStatsResponse stats = reviewService.getReviewStats(reviewee.getId());

    assertThat(stats.mostFrequentScore()).isEqualTo(4);
  }

  @Test
  void returnsEmptyStatsWhenNoReviewReceived() {
    ReviewStatsResponse stats = reviewService.getReviewStats(reviewee.getId());

    assertThat(stats.totalCount()).isZero();
    assertThat(stats.averageScore()).isNull();
    assertThat(stats.mostFrequentScore()).isNull();
    assertThat(stats.scoreCounts()).hasSize(5).containsValues(0L).doesNotContainValue(1L);
  }

  @Test
  void rejectsStatsForUnknownMember() {
    assertThatThrownBy(() -> reviewService.getReviewStats(999999L))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.MEMBER_NOT_FOUND);
  }

  /**
   * reviewerId는 FK가 아니므로 리뷰어 계정 없이 순번만 달리해 유일 제약을 피한다. 서비스를 거치지 않으므로 집계 행은 서비스가 하는 것과 같은 upsert로 직접
   * 맞춘다.
   */
  private void saveReviews(Long revieweeId, int... scores) {
    MatePost matePost = matePostRepository.findById(matePostId).orElseThrow();
    long reviewerId = 1000L;
    for (int score : scores) {
      reviewRepository.save(Review.create(matePost, reviewerId++, revieweeId, score, null));
      reviewStatsRepository.applyScore(
          revieweeId,
          score,
          score == 1 ? 1 : 0,
          score == 2 ? 1 : 0,
          score == 3 ? 1 : 0,
          score == 4 ? 1 : 0,
          score == 5 ? 1 : 0);
    }
  }

  private Member saveMember(String email, String nickname) {
    return memberRepository.save(
        Member.create(
            email,
            "encoded-password",
            nickname,
            null,
            Gender.MALE,
            LocalDate.of(1995, 3, 1),
            "서울"));
  }

  private ReviewCreateRequest createRequest(Long revieweeId, int score, String content) {
    return new ReviewCreateRequest(matePostId, revieweeId, score, content);
  }

  private MatePostCreateRequest createPostRequest() {
    return new MatePostCreateRequest(
        10L, "잠실 경기 같이 봐요", "즐겁게 응원할 분을 모집합니다.", 3, "ANY", 20, 40, "1루 네이비석");
  }
}
