package com.jikchin.jikchinbackend.domain.matepost.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePostStatus;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

class MatePostControllerDocsTest extends RestDocsSupport {

  private static final UUID MEMBER_KEY = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

  private final MatePostService matePostService = mock(MatePostService.class);

  @Override
  protected Object initController() {
    return new MatePostController(matePostService);
  }

  @Test
  void createMatePost() throws Exception {
    given(matePostService.create(eq(MEMBER_KEY), any(MatePostCreateRequest.class)))
        .willReturn(matePost());

    mockMvc
        .perform(
            post("/api/mate-posts")
                .header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN)
                .with(memberAuth(MEMBER_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eventId":1,"title":"잠실 LG vs 두산 같이 봐요",\
                    "content":"1루 응원석에서 같이 응원하실 분 구해요.","maxMembers":4,\
                    "preferredGender":"무관","minAge":20,"maxAge":35,"seatInfo":"1루 응원석 3구역"}
                    """))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mate-post-create",
                authorizationHeader(),
                requestFields(
                    fieldWithPath("eventId").type(NUMBER).description("같이 볼 경기 ID"),
                    fieldWithPath("title").type(STRING).description("제목 (최대 200자)"),
                    fieldWithPath("content").type(STRING).description("본문"),
                    fieldWithPath("maxMembers").type(NUMBER).description("최대 인원 (모집자 포함, 2 이상)"),
                    fieldWithPath("preferredGender")
                        .type(STRING)
                        .optional()
                        .description("선호 성별. enum이 아닌 자유 문자열 (최대 20자)"),
                    fieldWithPath("minAge").type(NUMBER).optional().description("최소 나이 (0~150)"),
                    fieldWithPath("maxAge").type(NUMBER).optional().description("최대 나이 (0~150)"),
                    fieldWithPath("seatInfo")
                        .type(STRING)
                        .optional()
                        .description("좌석 정보 (최대 100자)")),
                successResponse(matePostFields())));
  }

  @Test
  void getMatePost() throws Exception {
    given(matePostService.getById(1L)).willReturn(matePost());

    mockMvc
        .perform(
            get("/api/mate-posts/{matePostId}", 1L).header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk())
        .andDo(
            document(
                "mate-post-get",
                authorizationHeader(),
                pathParameters(parameterWithName("matePostId").description("모집글 ID")),
                successResponse(matePostFields())));
  }

  private FieldDescriptor[] matePostFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(NUMBER).description("모집글 ID"),
      fieldWithPath("userId").type(NUMBER).description("모집자 회원 ID"),
      fieldWithPath("eventId").type(NUMBER).description("경기 ID"),
      fieldWithPath("title").type(STRING).description("제목"),
      fieldWithPath("content").type(STRING).description("본문"),
      fieldWithPath("maxMembers").type(NUMBER).description("최대 인원"),
      fieldWithPath("currentMembers").type(NUMBER).description("현재 인원 (모집자 포함, 작성 직후 1)"),
      fieldWithPath("preferredGender").type(STRING).optional().description("선호 성별 (자유 문자열)"),
      fieldWithPath("minAge").type(NUMBER).optional().description("최소 나이"),
      fieldWithPath("maxAge").type(NUMBER).optional().description("최대 나이"),
      fieldWithPath("seatInfo").type(STRING).optional().description("좌석 정보"),
      fieldWithPath("status")
          .type(STRING)
          .description("모집 상태: " + enumValues(MatePostStatus.class)),
      fieldWithPath("createdAt").type(STRING).description("작성 시각 (ISO-8601)"),
      fieldWithPath("updatedAt").type(STRING).description("수정 시각 (ISO-8601)")
    };
  }

  private MatePostResponse matePost() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 9, 14, 19, 0);
    return new MatePostResponse(
        1L,
        1L,
        1L,
        "잠실 LG vs 두산 같이 봐요",
        "1루 응원석에서 같이 응원하실 분 구해요.",
        4,
        1,
        "무관",
        20,
        35,
        "1루 응원석 3구역",
        MatePostStatus.OPEN,
        createdAt,
        createdAt);
  }
}
