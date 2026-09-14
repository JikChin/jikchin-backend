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
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportSliceResponse;
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

    MatePostResponse post = matePostService.create(reporter.getMemberKey(), createPostRequest());
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
    assertThat(response.processedAt()).isNull();
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
    assertThat(resolved.processedAt()).isNotNull().isAfterOrEqualTo(created.createdAt());
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

    ReportSliceResponse pending = reportService.getReports(ReportStatus.PENDING, null, 20);
    ReportSliceResponse all = reportService.getReports(null, null, 20);

    assertThat(pending.reports()).hasSize(1);
    assertThat(pending.reports().getFirst().reason()).isEqualTo(ReportReason.ABUSE);
    assertThat(pending.hasNext()).isFalse();
    assertThat(all.reports()).hasSize(2);
  }

  @Test
  void pagesPendingReportsOldestFirstByCursor() {
    for (ReportReason reason : ReportReason.values()) {
      reportService.create(reporter.getMemberKey(), createRequest(reason, null));
    }

    ReportSliceResponse first = reportService.getReports(ReportStatus.PENDING, null, 2);
    ReportSliceResponse second =
        reportService.getReports(ReportStatus.PENDING, first.nextCursor(), 2);
    ReportSliceResponse third =
        reportService.getReports(ReportStatus.PENDING, second.nextCursor(), 2);

    assertThat(first.reports()).hasSize(2);
    assertThat(first.hasNext()).isTrue();
    assertThat(second.reports()).hasSize(2);
    assertThat(third.reports()).hasSize(1);
    assertThat(third.hasNext()).isFalse();
    assertThat(third.nextCursor()).isNull();

    List<Long> ids = new java.util.ArrayList<>();
    for (ReportSliceResponse page : List.of(first, second, third)) {
      page.reports().forEach(report -> ids.add(report.id()));
    }
    assertThat(ids).doesNotHaveDuplicates().hasSize(5).isSorted();
  }

  @Test
  void keepsPagingAfterCursorReportWasProcessed() {
    for (ReportReason reason : ReportReason.values()) {
      reportService.create(reporter.getMemberKey(), createRequest(reason, null));
    }
    ReportSliceResponse first = reportService.getReports(ReportStatus.PENDING, null, 2);
    // 관리자가 페이지 마지막 신고를 처리한 뒤에도 같은 커서로 다음 페이지를 이어 읽을 수 있어야 한다.
    reportService.resolve(first.nextCursor());

    ReportSliceResponse second =
        reportService.getReports(ReportStatus.PENDING, first.nextCursor(), 2);

    assertThat(second.reports()).hasSize(2);
    assertThat(second.reports())
        .allSatisfy(report -> assertThat(report.status()).isEqualTo(ReportStatus.PENDING));
    assertThat(second.reports().getFirst().id()).isGreaterThan(first.nextCursor());
  }

  @Test
  void reportsNoNextPageWhenRemainingRowsExactlyFillThePage() {
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, null));
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.ABUSE, null));
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.SPAM, null));
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.ETC, null));

    ReportSliceResponse first = reportService.getReports(ReportStatus.PENDING, null, 2);
    ReportSliceResponse last =
        reportService.getReports(ReportStatus.PENDING, first.nextCursor(), 2);

    // 남은 행이 정확히 size건이면 마지막 페이지다. size + 1건 조회가 size건만 돌려주므로 hasNext는 false여야 한다.
    assertThat(last.reports()).hasSize(2);
    assertThat(last.hasNext()).isFalse();
    assertThat(last.nextCursor()).isNull();
  }

  @Test
  void pagesAllStatusesByCursorWhenStatusIsOmitted() {
    ReportResponse resolved =
        reportService.create(reporter.getMemberKey(), createRequest(ReportReason.NO_SHOW, null));
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.ABUSE, null));
    reportService.create(reporter.getMemberKey(), createRequest(ReportReason.SPAM, null));
    reportService.resolve(resolved.id());

    ReportSliceResponse first = reportService.getReports(null, null, 2);
    ReportSliceResponse second = reportService.getReports(null, first.nextCursor(), 2);

    assertThat(first.reports())
        .extracting(ReportResponse::status)
        .containsExactly(ReportStatus.RESOLVED, ReportStatus.PENDING);
    assertThat(second.reports()).hasSize(1);
    assertThat(second.reports().getFirst().reason()).isEqualTo(ReportReason.SPAM);
    assertThat(second.hasNext()).isFalse();
  }

  @Test
  void rejectsUnknownCursor() {
    assertThatThrownBy(() -> reportService.getReports(ReportStatus.PENDING, 999999L, 20))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorType())
        .isEqualTo(ErrorType.REPORT_CURSOR_INVALID);
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
