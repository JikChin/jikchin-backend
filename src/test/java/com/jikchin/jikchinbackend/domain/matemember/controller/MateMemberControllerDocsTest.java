package com.jikchin.jikchinbackend.domain.matemember.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.matemember.dto.response.MateMemberResponse;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.service.MateMemberService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;

class MateMemberControllerDocsTest extends RestDocsSupport {

  private final MateMemberService mateMemberService = mock(MateMemberService.class);

  @Override
  protected Object initController() {
    return new MateMemberController(mateMemberService);
  }

  @Test
  void getActiveMembers() throws Exception {
    given(mateMemberService.getActiveMembers(1L))
        .willReturn(
            List.of(
                new MateMemberResponse(
                    1L, 1L, 1L, LocalDateTime.of(2026, 9, 14, 19, 0), MateMemberStatus.ACTIVE),
                new MateMemberResponse(
                    2L, 1L, 13L, LocalDateTime.of(2026, 9, 14, 21, 0), MateMemberStatus.ACTIVE)));

    mockMvc
        .perform(
            get("/api/mate-posts/{matePostId}/members", 1L)
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mate-member-list",
                authorizationHeader(),
                pathParameters(parameterWithName("matePostId").description("모집글 ID")),
                successListResponse("참여 중(`ACTIVE`)인 멤버 목록 (참여순)", memberFields())));
  }

  @Test
  void isParticipating() throws Exception {
    given(mateMemberService.isParticipating(1L, 13L)).willReturn(true);

    mockMvc
        .perform(
            get("/api/mate-posts/{matePostId}/members/{userId}/participation", 1L, 13L)
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mate-member-participation",
                authorizationHeader(),
                pathParameters(
                    parameterWithName("matePostId").description("모집글 ID"),
                    parameterWithName("userId").description("확인할 회원 ID")),
                successScalarResponse(BOOLEAN, "`ACTIVE` 상태로 참여 중이면 true")));
  }

  private FieldDescriptor[] memberFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(NUMBER).description("멤버 ID"),
      fieldWithPath("matePostId").type(NUMBER).description("모집글 ID"),
      fieldWithPath("userId").type(NUMBER).description("회원 ID"),
      fieldWithPath("joinedAt").type(STRING).description("참여 시각 (ISO-8601)"),
      fieldWithPath("status")
          .type(STRING)
          .description("멤버 상태: " + enumValues(MateMemberStatus.class))
    };
  }
}
