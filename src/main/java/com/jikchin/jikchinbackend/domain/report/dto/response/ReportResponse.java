package com.jikchin.jikchinbackend.domain.report.dto.response;

import com.jikchin.jikchinbackend.domain.report.entity.Report;
import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import java.time.LocalDateTime;

public record ReportResponse(
    Long id,
    Long matePostId,
    Long reporterId,
    Long reportedUserId,
    ReportReason reason,
    String detail,
    ReportStatus status,
    LocalDateTime createdAt,
    LocalDateTime processedAt) {

  public static ReportResponse from(Report report) {
    return new ReportResponse(
        report.getId(),
        report.getMatePost().getId(),
        report.getReporterId(),
        report.getReportedUserId(),
        report.getReason(),
        report.getDetail(),
        report.getStatus(),
        report.getCreatedAt(),
        report.getProcessedAt());
  }
}
