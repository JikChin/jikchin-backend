package com.jikchin.jikchinbackend.domain.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.service.ReportService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import com.jikchin.jikchinbackend.global.response.ResultType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

  @Mock private ReportService reportService;

  private AdminReportController adminReportController;

  @BeforeEach
  void setUp() {
    adminReportController = new AdminReportController(reportService);
  }

  @Test
  void returnsReportsFilteredByStatusWrappedWithApiResponse() {
    List<ReportResponse> reports = List.of(createResponse(ReportStatus.PENDING));
    when(reportService.getReports(ReportStatus.PENDING)).thenReturn(reports);

    ApiResponse<List<ReportResponse>> response =
        adminReportController.getReports(ReportStatus.PENDING);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(reports);
    assertThat(response.getError()).isNull();
  }

  @Test
  void resolvesReportWrappedWithApiResponse() {
    ReportResponse resolved = createResponse(ReportStatus.RESOLVED);
    when(reportService.resolve(1L)).thenReturn(resolved);

    ApiResponse<ReportResponse> response = adminReportController.resolve(1L);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(resolved);
  }

  @Test
  void rejectsReportWrappedWithApiResponse() {
    ReportResponse rejected = createResponse(ReportStatus.REJECTED);
    when(reportService.reject(1L)).thenReturn(rejected);

    ApiResponse<ReportResponse> response = adminReportController.reject(1L);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(rejected);
  }

  private ReportResponse createResponse(ReportStatus status) {
    LocalDateTime now = LocalDateTime.now();
    return new ReportResponse(
        1L, 10L, 1L, 2L, ReportReason.NO_SHOW, "당일에 나타나지 않았습니다.", status, now, now);
  }
}
