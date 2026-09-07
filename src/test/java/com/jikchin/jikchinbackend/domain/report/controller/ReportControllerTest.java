package com.jikchin.jikchinbackend.domain.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jikchin.jikchinbackend.domain.report.dto.request.ReportCreateRequest;
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.service.ReportService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import com.jikchin.jikchinbackend.global.response.ResultType;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

  @Mock private ReportService reportService;

  private ReportController reportController;

  @BeforeEach
  void setUp() {
    reportController = new ReportController(reportService);
  }

  @Test
  void createsReportWrappedWithApiResponse() {
    UUID memberKey = UUID.randomUUID();
    ReportCreateRequest request =
        new ReportCreateRequest(10L, 2L, ReportReason.NO_SHOW, "당일에 나타나지 않았습니다.");
    ReportResponse report = createResponse(ReportStatus.PENDING);
    when(reportService.create(memberKey, request)).thenReturn(report);

    ApiResponse<ReportResponse> response = reportController.create(memberKey, request);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(report);
    assertThat(response.getError()).isNull();
  }

  private ReportResponse createResponse(ReportStatus status) {
    LocalDateTime now = LocalDateTime.now();
    return new ReportResponse(
        1L, 10L, 1L, 2L, ReportReason.NO_SHOW, "당일에 나타나지 않았습니다.", status, now, now);
  }
}
