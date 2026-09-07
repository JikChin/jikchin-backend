package com.jikchin.jikchinbackend.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matemember.service.MateMemberService;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.domain.member.entity.Gender;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.report.repository.ReportRepository;
import com.jikchin.jikchinbackend.domain.review.dto.request.ReviewCreateRequest;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewResponse;
import com.jikchin.jikchinbackend.domain.review.repository.ReviewRepository;
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
    reportRepository.deleteAll();
    mateMemberRepository.deleteAll();
    matePostRepository.deleteAll();
    memberRepository.deleteAll();

    reviewer = saveMember("reviewer@jikchin.com", "직관러버");
    reviewee = saveMember("reviewee@jikchin.com", "야구광");
    outsider = saveMember("outsider@jikchin.com", "구경꾼");

    MatePostResponse post = matePostService.create(reviewer.getId(), createPostRequest());
    matePostId = post.id();
    mateMemberService.addMember(matePostId, reviewee.getId());
  }

  @AfterEach
  void tearDown() {
    reviewRepository.deleteAll();
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
