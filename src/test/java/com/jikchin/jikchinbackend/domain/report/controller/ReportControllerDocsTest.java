package com.jikchin.jikchinbackend.domain.report.controller;

import static com.jikchin.jikchinbackend.domain.report.controller.ReportDocsFields.report;
import static com.jikchin.jikchinbackend.domain.report.controller.ReportDocsFields.reportFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.report.dto.request.ReportCreateRequest;
import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.service.ReportService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ReportControllerDocsTest extends RestDocsSupport {

  private static final UUID MEMBER_KEY = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

  private final ReportService reportService = mock(ReportService.class);

  @Override
  protected Object initController() {
    return new ReportController(reportService);
  }

  @Test
  void createReport() throws Exception {
    // memberKey까지 맞춰 스텁해 @AuthenticationPrincipal 주입이 깨지면 응답 필드 검증에서 실패하게 한다.
    given(reportService.create(eq(MEMBER_KEY), any(ReportCreateRequest.class)))
        .willReturn(report(1L, ReportStatus.PENDING, null));

    mockMvc
        .perform(
            post("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                .with(memberAuth(MEMBER_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"matePostId":10,"reportedUserId":2,"reason":"NO_SHOW",\
                    "detail":"약속 시간에 나타나지 않았어요."}
                    """))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "report-create",
                authorizationHeader(),
                requestFields(
                    fieldWithPath("matePostId").type(NUMBER).description("신고할 모집글 ID"),
                    fieldWithPath("reportedUserId").type(NUMBER).description("신고할 회원 ID"),
                    fieldWithPath("reason")
                        .type(STRING)
                        .description("신고 사유: " + enumValues(ReportReason.class)),
                    fieldWithPath("detail")
                        .type(STRING)
                        .optional()
                        .description("상세 내용 (최대 1000자)")),
                successResponse(reportFields())));
  }
}
