package com.jikchin.jikchinbackend.domain.report.service;

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
import com.jikchin.jikchinbackend.domain.report.dto.request.ReportCreateRequest;
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.repository.ReportRepository;
import com.jikchin.jikchinbackend.domain.review.repository.ReviewRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReportServiceIntegrationTest {

  @Autowired private ReportService reportService;
  @Autowired private ReportRepository reportRepository;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private MatePostService matePostService;
  @Autowired private MateMemberService mateMemberService;
  @Autowired private MateMemberRepository mateMemberRepository;
  @Autowired private MatePostRepository matePostRepository;
  @Autowired private MemberRepository memberRepository;

  private Member reporter;
  private Member reported;
  private Member outsider;
  private Long matePostId;

  @BeforeEach
  void setUp() {
    reportRepository.deleteAll();
    reviewRepository.deleteAll();
    mateMemberRepository.deleteAll();
    matePostRepository.deleteAll();
    memberRepository.deleteAll();

    reporter = saveMember("reporter@jikchin.com", "직관러버");
    reported = saveMember("reported@jikchin.com", "노쇼왕");
    outsider = saveMember("outsider@jikchin.com", "구경꾼");

    MatePostResponse post = matePostService.create(reporter.getId(), createPostRequest());
    matePostId = post.id();
    mateMemberService.addMember(matePostId, reported.getId());
  }

  @AfterEach
  void tearDown() {
    reportRepository.deleteAll();
    reviewRepository.deleteAll();
  }

  @Test
  void createsReportWithPendingStatus() {
    ReportResponse response =
        reportService.create(
            reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, "당일에 나타나지 않았습니다."));

    assertThat(response.id()).isNotNull();
    assertThat(response.reporterId()).isEqualTo(reporter.getId());
    assertThat(response.reportedUserId()).isEqualTo(reported.getId());
    assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
    assertThat(response.processedAt()).isEqualTo(response.createdAt());
  }

  @Test
  void rejectsSelfReport() {
    assertThatThrownBy(
            () ->
                reportService.create(
                    reporter.getMemberKey(),
                    new ReportCreateRequest(
                        matePostId, reporter.getId(), ReportReason.ABUSE, null)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REPORT_SELF_NOT_ALLOWED);
  }

  @Test
  void rejectsReportFromNonMateMember() {
    assertThatThrownBy(
            () ->
                reportService.create(
                    outsider.getMemberKey(), createRequest(ReportReason.ABUSE, null)))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REPORT_NOT_MATE_MEMBER);
  }

  @Test
  void rejectsDuplicateReportForSameReason() {
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, null));

    assertThatThrownBy(
            () ->
                reportService.create(
                    reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, "재신고")))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REPORT_ALREADY_EXISTS);
  }

  @Test
  void allowsReportForSameTargetWithDifferentReason() {
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, null));

    ReportResponse response =
        reportService.create(reporter.getMemberKey(), createRequest(ReportReason.ABUSE, null));

    assertThat(response.reason()).isEqualTo(ReportReason.ABUSE);
    assertThat(reportRepository.findAll()).hasSize(2);
  }

  @Test
  void resolvesPendingReportAndUpdatesProcessedAt() {
    ReportResponse created =
        reportService.create(reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, null));

    ReportResponse resolved = reportService.resolve(created.id());

    assertThat(resolved.status()).isEqualTo(ReportStatus.RESOLVED);
    assertThat(resolved.processedAt()).isAfterOrEqualTo(created.processedAt());
  }

  @Test
  void rejectsProcessingAlreadyProcessedReport() {
    ReportResponse created =
        reportService.create(reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, null));
    reportService.resolve(created.id());

    assertThatThrownBy(() -> reportService.reject(created.id()))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REPORT_ALREADY_PROCESSED);
  }

  @Test
  void filtersReportsByStatus() {
    ReportResponse first =
        reportService.create(reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, null));
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.ABUSE, null));
    reportService.resolve(first.id());

    List<ReportResponse> pending = reportService.getReports(ReportStatus.PENDING);
    List<ReportResponse> all = reportService.getReports(null);

    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().reason()).isEqualTo(ReportReason.ABUSE);
    assertThat(all).hasSize(2);
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

  private ReportCreateRequest createRequest(ReportReason reason, String detail) {
    return new ReportCreateRequest(matePostId, reported.getId(), reason, detail);
  }

  private MatePostCreateRequest createPostRequest() {
    return new MatePostCreateRequest(
        10L, "잠실 경기 같이 봐요", "즐겁게 응원할 분을 모집합니다.", 3, "ANY", 20, 40, "1루 네이비석");
  }
}
