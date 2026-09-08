package com.jikchin.jikchinbackend.domain.report.service;

import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.repository.MateMemberRepository;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePost;
import com.jikchin.jikchinbackend.domain.matepost.repository.MatePostRepository;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.report.dto.request.ReportCreateRequest;
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.entity.Report;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.repository.ReportRepository;
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

  @Transactional(readOnly = true)
  public List<ReportResponse> getReports(ReportStatus status) {
    List<Report> reports =
        status == null
            ? reportRepository.findAllByOrderByCreatedAtAsc()
            : reportRepository.findAllByStatusOrderByCreatedAtAsc(status);
    return reports.stream().map(ReportResponse::from).toList();
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
