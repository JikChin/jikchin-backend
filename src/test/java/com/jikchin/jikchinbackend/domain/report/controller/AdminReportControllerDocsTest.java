package com.jikchin.jikchinbackend.domain.report.controller;

import static com.jikchin.jikchinbackend.domain.report.controller.ReportDocsFields.report;
import static com.jikchin.jikchinbackend.domain.report.controller.ReportDocsFields.reportFields;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportSliceResponse;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.service.ReportService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class AdminReportControllerDocsTest extends RestDocsSupport {

  private static final LocalDateTime PROCESSED_AT = LocalDateTime.of(2026, 9, 15, 10, 0);

  private final ReportService reportService = mock(ReportService.class);

  @Override
  protected Object initController() {
    return new AdminReportController(reportService);
  }

  @Test
  void getReports() throws Exception {
    given(reportService.getReports(ReportStatus.PENDING, 10L, 2))
        .willReturn(
            new ReportSliceResponse(
                List.of(
                    report(11L, ReportStatus.PENDING, null),
                    report(12L, ReportStatus.PENDING, null)),
                true,
                12L));

    mockMvc
        .perform(
            get("/api/admin/reports")
                .header(HttpHeaders.AUTHORIZATION, ADMIN_ACCESS_TOKEN)
                .param("status", "PENDING")
                .param("cursor", "10")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin-report-list",
                adminAuthorizationHeader(),
                queryParameters(
                    parameterWithName("status")
                        .optional()
                        .description("처리 상태 필터: " + enumValues(ReportStatus.class) + ". 생략하면 전체"),
                    parameterWithName("cursor")
                        .optional()
                        .description("이전 응답의 nextCursor. 첫 페이지는 생략"),
                    parameterWithName("size").optional().description("페이지 크기 (1~100, 기본 20)")),
                successResponse(
                        fieldWithPath("reports[]").type(ARRAY).description("신고 목록 (오래된 순)"),
                        fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                        fieldWithPath("nextCursor")
                            .type(NUMBER)
                            .optional()
                            .description("다음 페이지 요청 시 cursor로 넘길 값. 마지막 페이지면 null"))
                    .andWithPrefix("data.reports[].", reportFields())));
  }

  @Test
  void resolve() throws Exception {
    given(reportService.resolve(1L)).willReturn(report(1L, ReportStatus.RESOLVED, PROCESSED_AT));

    mockMvc
        .perform(
            patch("/api/admin/reports/{reportId}/resolve", 1L)
                .header(HttpHeaders.AUTHORIZATION, ADMIN_ACCESS_TOKEN))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin-report-resolve",
                adminAuthorizationHeader(),
                pathParameters(parameterWithName("reportId").description("처리할 신고 ID")),
                successResponse(reportFields())));
  }

  @Test
  void reject() throws Exception {
    given(reportService.reject(1L)).willReturn(report(1L, ReportStatus.REJECTED, PROCESSED_AT));

    mockMvc
        .perform(
            patch("/api/admin/reports/{reportId}/reject", 1L)
                .header(HttpHeaders.AUTHORIZATION, ADMIN_ACCESS_TOKEN))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin-report-reject",
                adminAuthorizationHeader(),
                pathParameters(parameterWithName("reportId").description("반려할 신고 ID")),
                successResponse(reportFields())));
  }
}
