package com.jikchin.jikchinbackend.domain.report.controller;

import static com.jikchin.jikchinbackend.docs.RestDocsSupport.enumValues;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import java.time.LocalDateTime;
import org.springframework.restdocs.payload.FieldDescriptor;

/** ReportController와 AdminReportController 문서가 함께 쓰는 ReportResponse 필드와 픽스처. */
final class ReportDocsFields {

  private ReportDocsFields() {}

  static FieldDescriptor[] reportFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(NUMBER).description("신고 ID"),
      fieldWithPath("matePostId").type(NUMBER).description("모집글 ID"),
      fieldWithPath("reporterId").type(NUMBER).description("신고한 회원 ID"),
      fieldWithPath("reportedUserId").type(NUMBER).description("신고당한 회원 ID"),
      fieldWithPath("reason").type(STRING).description("신고 사유: " + enumValues(ReportReason.class)),
      fieldWithPath("detail").type(STRING).optional().description("상세 내용"),
      fieldWithPath("status").type(STRING).description("처리 상태: " + enumValues(ReportStatus.class)),
      fieldWithPath("createdAt").type(STRING).description("신고 시각 (ISO-8601)"),
      fieldWithPath("processedAt")
          .type(STRING)
          .optional()
          .description("처리 시각 (ISO-8601). 처리 전(PENDING)이면 null")
    };
  }

  static ReportResponse report(Long id, ReportStatus status, LocalDateTime processedAt) {
    return new ReportResponse(
        id,
        10L,
        1L,
        2L,
        ReportReason.NO_SHOW,
        "약속 시간에 나타나지 않았어요.",
        status,
        LocalDateTime.of(2026, 9, 14, 21, 0),
        processedAt);
  }
}
