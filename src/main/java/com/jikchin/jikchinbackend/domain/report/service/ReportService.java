package com.jikchin.jikchinbackend.domain.report.service;

import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.report.dto.request.ReportCreateRequest;
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportSliceResponse;
import com.jikchin.jikchinbackend.domain.report.entity.Report;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.repository.ReportRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final ReportRepository reportRepository;
  private final MatePostRepository matePostRepository;
  private final MateMemberRepository mateMemberRepository;
  private final MemberRepository memberRepository;

  @Transactional
  public ReportResponse create(UUID reporterKey, ReportCreateRequest request) {
    Long reporterId = getMemberId(reporterKey);
    if (reporterId.equals(request.reportedUserId())) {
      throw new AppException(ErrorType.REPORT_SELF_NOT_ALLOWED);
    }

    MatePost matePost =
        matePostRepository
            .findById(request.matePostId())
            .orElseThrow(() -> new AppException(ErrorType.REPORT_MATE_POST_NOT_FOUND));

    requireActiveMateMember(matePost.getId(), reporterId);
    requireActiveMateMember(matePost.getId(), request.reportedUserId());

    if (reportRepository.existsByMatePost_IdAndReporterIdAndReportedUserIdAndReason(
        matePost.getId(), reporterId, request.reportedUserId(), request.reason())) {
      throw new AppException(ErrorType.REPORT_ALREADY_EXISTS);
    }

    Report report =
        Report.create(
            matePost, reporterId, request.reportedUserId(), request.reason(), request.detail());
    try {
      reportRepository.save(report);
      reportRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw new AppException(ErrorType.REPORT_ALREADY_EXISTS);
    }
    return ReportResponse.from(report);
  }

  /**
   * 신고를 오래된 순으로 size건 돌려준다. cursor는 직전 페이지 마지막 신고의 id이며 그 신고의 (createdAt, id) 뒤부터 이어 읽는다. 커서 신고의
   * 상태는 검사하지 않는다. 관리자가 페이지 마지막 신고를 처리해 PENDING이 아니게 돼도 다음 페이지 요청이 깨지면 안 되기 때문이다.
   */
  @Transactional(readOnly = true)
  public ReportSliceResponse getReports(ReportStatus status, Long cursor, int size) {
    PageRequest limit = PageRequest.of(0, size + 1);
    List<Report> fetched;
    if (cursor == null) {
      fetched =
          status == null
              ? reportRepository.findAllByOrderByCreatedAtAscIdAsc(limit)
              : reportRepository.findAllByStatusOrderByCreatedAtAscIdAsc(status, limit);
    } else {
      Report cursorReport =
          reportRepository
              .findById(cursor)
              .orElseThrow(() -> new AppException(ErrorType.REPORT_CURSOR_INVALID));
      LocalDateTime cursorCreatedAt = cursorReport.getCreatedAt();
      fetched =
          status == null
              ? reportRepository.findAfterCursor(cursorCreatedAt, cursorReport.getId(), limit)
              : reportRepository.findByStatusAfterCursor(
                  status, cursorCreatedAt, cursorReport.getId(), limit);
    }
    return ReportSliceResponse.of(fetched, size);
  }

  @Transactional
  public ReportResponse resolve(Long reportId) {
    Report report = getReport(reportId);
    report.resolve();
    return ReportResponse.from(report);
  }

  @Transactional
  public ReportResponse reject(Long reportId) {
    Report report = getReport(reportId);
    report.reject();
    return ReportResponse.from(report);
  }

  private Report getReport(Long reportId) {
    return reportRepository
        .findById(reportId)
        .orElseThrow(() -> new AppException(ErrorType.REPORT_NOT_FOUND));
  }

  private Long getMemberId(UUID memberKey) {
    return memberRepository
        .findByMemberKey(memberKey)
        .map(Member::getId)
        .orElseThrow(() -> new AppException(ErrorType.MEMBER_NOT_FOUND));
  }

  private void requireActiveMateMember(Long matePostId, Long userId) {
    if (!mateMemberRepository.existsByMatePost_IdAndUserIdAndStatus(
        matePostId, userId, MateMemberStatus.ACTIVE)) {
      throw new AppException(ErrorType.REPORT_NOT_MATE_MEMBER);
    }
  }
}
