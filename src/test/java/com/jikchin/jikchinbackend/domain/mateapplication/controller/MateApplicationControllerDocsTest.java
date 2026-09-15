package com.jikchin.jikchinbackend.domain.mateapplication.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.mateapplication.dto.request.MateApplicationCreateRequest;
import com.jikchin.jikchinbackend.domain.mateapplication.dto.response.MateApplicationResponse;
import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplicationStatus;
import com.jikchin.jikchinbackend.domain.mateapplication.service.MateApplicationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;

class MateApplicationControllerDocsTest extends RestDocsSupport {

  private static final UUID MEMBER_KEY = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

  private final MateApplicationService mateApplicationService = mock(MateApplicationService.class);

  @Override
  protected Object initController() {
    return new MateApplicationController(mateApplicationService);
  }

  @Test
  void apply() throws Exception {
    given(
            mateApplicationService.apply(
                eq(1L), eq(MEMBER_KEY), any(MateApplicationCreateRequest.class)))
        .willReturn(application(3L, MateApplicationStatus.PENDING));

    mockMvc
        .perform(
            post("/api/mate-posts/{matePostId}/applications", 1L)
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                .with(memberAuth(MEMBER_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"message":"같이 응원해요!"}
                    """))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mate-application-create",
                authorizationHeader(),
                pathParameters(matePostIdParameter()),
                requestFields(
                    fieldWithPath("message")
                        .type(STRING)
                        .optional()
                        .description("모집자에게 남길 메시지 (최대 500자)")),
                successResponse(applicationFields())));
  }

  @Test
  void getApplications() throws Exception {
    given(mateApplicationService.getApplications(1L, MEMBER_KEY))
        .willReturn(
            List.of(
                application(3L, MateApplicationStatus.PENDING),
                application(4L, MateApplicationStatus.REJECTED)));

    mockMvc
        .perform(
            get("/api/mate-posts/{matePostId}/applications", 1L)
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                .with(memberAuth(MEMBER_KEY)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mate-application-list",
                authorizationHeader(),
                pathParameters(matePostIdParameter()),
                successListResponse("참가 신청 목록 (신청순)", applicationFields())));
  }

  @Test
  void accept() throws Exception {
    given(mateApplicationService.accept(1L, 3L, MEMBER_KEY))
        .willReturn(application(3L, MateApplicationStatus.ACCEPTED));

    mockMvc
        .perform(
            patch("/api/mate-posts/{matePostId}/applications/{applicationId}/accept", 1L, 3L)
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                .with(memberAuth(MEMBER_KEY)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mate-application-accept",
                authorizationHeader(),
                pathParameters(matePostIdParameter(), applicationIdParameter()),
                successResponse(applicationFields())));
  }

  @Test
  void reject() throws Exception {
    given(mateApplicationService.reject(1L, 3L, MEMBER_KEY))
        .willReturn(application(3L, MateApplicationStatus.REJECTED));

    mockMvc
        .perform(
            patch("/api/mate-posts/{matePostId}/applications/{applicationId}/reject", 1L, 3L)
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                .with(memberAuth(MEMBER_KEY)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mate-application-reject",
                authorizationHeader(),
                pathParameters(matePostIdParameter(), applicationIdParameter()),
                successResponse(applicationFields())));
  }

  private ParameterDescriptor matePostIdParameter() {
    return parameterWithName("matePostId").description("모집글 ID");
  }

  private ParameterDescriptor applicationIdParameter() {
    return parameterWithName("applicationId").description("참가 신청 ID");
  }

  private FieldDescriptor[] applicationFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(NUMBER).description("참가 신청 ID"),
      fieldWithPath("matePostId").type(NUMBER).description("모집글 ID"),
      fieldWithPath("userId").type(NUMBER).description("신청자 회원 ID"),
      fieldWithPath("status")
          .type(STRING)
          .description("신청 상태: " + enumValues(MateApplicationStatus.class)),
      fieldWithPath("message").type(STRING).optional().description("신청 메시지"),
      fieldWithPath("createdAt").type(STRING).description("신청 시각 (ISO-8601)"),
      fieldWithPath("updatedAt").type(STRING).description("수정 시각 (ISO-8601)")
    };
  }

  private MateApplicationResponse application(Long id, MateApplicationStatus status) {
    return new MateApplicationResponse(
        id,
        1L,
        id + 10,
        status,
        "같이 응원해요!",
        LocalDateTime.of(2026, 9, 14, 20, 0),
        LocalDateTime.of(2026, 9, 14, 21, 0));
  }
}
