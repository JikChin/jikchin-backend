package com.jikchin.jikchinbackend.domain.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.review.dto.request.ReviewCreateRequest;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewResponse;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewSliceResponse;
import com.jikchin.jikchinbackend.domain.review.dto.response.ReviewStatsResponse;
import com.jikchin.jikchinbackend.domain.review.service.ReviewService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

class ReviewControllerDocsTest extends RestDocsSupport {

  private static final UUID MEMBER_KEY = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

  private final ReviewService reviewService = mock(ReviewService.class);

  @Override
  protected Object initController() {
    return new ReviewController(reviewService);
  }

  @Test
  void createReview() throws Exception {
    // memberKey까지 맞춰 스텁해 @AuthenticationPrincipal 주입이 깨지면 응답 필드 검증에서 실패하게 한다.
    given(reviewService.create(eq(MEMBER_KEY), any(ReviewCreateRequest.class)))
        .willReturn(review(1L));

    mockMvc
        .perform(
            post("/api/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer {access-token}")
                .with(memberAuth(MEMBER_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"matePostId":10,"revieweeId":2,"score":4,"content":"시간 약속을 잘 지켜요."}
                    """))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "review-create",
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token")),
                requestFields(
                    fieldWithPath("matePostId").type(NUMBER).description("리뷰를 남길 모집글 ID"),
                    fieldWithPath("revieweeId").type(NUMBER).description("평가받는 회원 ID"),
                    fieldWithPath("score").type(NUMBER).description("별점 (1~5)"),
                    fieldWithPath("content")
                        .type(STRING)
                        .optional()
                        .description("리뷰 내용 (최대 1000자)")),
                successResponse(reviewFields())));
  }

  @Test
  void getReceivedReviews() throws Exception {
    given(reviewService.getReceivedReviews(2L, 10L, 2))
        .willReturn(new ReviewSliceResponse(List.of(review(9L), review(8L)), true, 8L));

    mockMvc
        .perform(
            get("/api/members/{memberId}/reviews", 2L).param("cursor", "10").param("size", "2"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "review-list",
                pathParameters(parameterWithName("memberId").description("리뷰를 받은 회원 ID")),
                queryParameters(
                    parameterWithName("cursor")
                        .optional()
                        .description("이전 응답의 nextCursor. 첫 페이지는 생략"),
                    parameterWithName("size").optional().description("페이지 크기 (1~100, 기본 20)")),
                successResponse(
                        fieldWithPath("reviews[]").type(ARRAY).description("리뷰 목록 (최신순)"),
                        fieldWithPath("hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                        fieldWithPath("nextCursor")
                            .type(NUMBER)
                            .optional()
                            .description("다음 페이지 요청 시 cursor로 넘길 값. 마지막 페이지면 null"))
                    .andWithPrefix("data.reviews[].", reviewFields())));
  }

  @Test
  void getReviewStats() throws Exception {
    Map<Integer, Long> scoreCounts = new TreeMap<>(Map.of(1, 0L, 2, 0L, 3, 1L, 4, 0L, 5, 2L));
    given(reviewService.getReviewStats(2L))
        .willReturn(new ReviewStatsResponse(2L, 3, new BigDecimal("4.33"), 5, scoreCounts));

    mockMvc
        .perform(get("/api/members/{memberId}/reviews/stats", 2L))
        .andExpect(status().isOk())
        .andDo(
            document(
                "review-stats",
                pathParameters(parameterWithName("memberId").description("리뷰를 받은 회원 ID")),
                successResponse(
                    fieldWithPath("revieweeId").type(NUMBER).description("리뷰를 받은 회원 ID"),
                    fieldWithPath("totalCount").type(NUMBER).description("받은 리뷰 수"),
                    fieldWithPath("averageScore")
                        .type(NUMBER)
                        .optional()
                        .description("평균 별점 (소수점 둘째 자리 반올림). 리뷰가 없으면 null"),
                    fieldWithPath("mostFrequentScore")
                        .type(NUMBER)
                        .optional()
                        .description("가장 많이 받은 별점. 동률이면 높은 점수, 리뷰가 없으면 null"),
                    fieldWithPath("scoreCounts")
                        .type(OBJECT)
                        .description("별점별 리뷰 수. 리뷰가 없는 점수도 0으로 항상 포함"),
                    fieldWithPath("scoreCounts.1").type(NUMBER).description("1점 리뷰 수"),
                    fieldWithPath("scoreCounts.2").type(NUMBER).description("2점 리뷰 수"),
                    fieldWithPath("scoreCounts.3").type(NUMBER).description("3점 리뷰 수"),
                    fieldWithPath("scoreCounts.4").type(NUMBER).description("4점 리뷰 수"),
                    fieldWithPath("scoreCounts.5").type(NUMBER).description("5점 리뷰 수"))));
  }

  private FieldDescriptor[] reviewFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(NUMBER).description("리뷰 ID"),
      fieldWithPath("matePostId").type(NUMBER).description("모집글 ID"),
      fieldWithPath("reviewerId").type(NUMBER).description("작성자 회원 ID"),
      fieldWithPath("revieweeId").type(NUMBER).description("평가받는 회원 ID"),
      fieldWithPath("score").type(NUMBER).description("별점 (1~5)"),
      fieldWithPath("content").type(STRING).optional().description("리뷰 내용"),
      fieldWithPath("createdAt").type(STRING).description("작성 시각 (ISO-8601)")
    };
  }

  private ReviewResponse review(Long id) {
    return new ReviewResponse(
        id, 10L, 1L, 2L, 4, "시간 약속을 잘 지켜요.", LocalDateTime.of(2026, 9, 14, 19, 0));
  }
}
