package com.jikchin.jikchinbackend.domain.matepost.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jikchin.jikchinbackend.domain.matepost.dto.request.MatePostCreateRequest;
import com.jikchin.jikchinbackend.domain.matepost.dto.response.MatePostResponse;
import com.jikchin.jikchinbackend.domain.matepost.entity.MatePostStatus;
import com.jikchin.jikchinbackend.domain.matepost.service.MatePostService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import com.jikchin.jikchinbackend.global.response.ResultType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class MatePostControllerTest {

  @Mock private MatePostService matePostService;

  private MatePostController matePostController;

  @BeforeEach
  void setUp() {
    matePostController = new MatePostController(matePostService);
  }

  @Test
  void wrapsCreateResponseWithApiResponse() {
    MatePostCreateRequest request = createRequest();
    MatePostResponse matePost = createResponse();
    when(matePostService.create(1L, request)).thenReturn(matePost);

    ResponseEntity<ApiResponse<MatePostResponse>> response = matePostController.create(1L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasPath("/api/mate-posts/10");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getBody().getData()).isEqualTo(matePost);
    assertThat(response.getBody().getError()).isNull();
  }

  @Test
  void wrapsGetResponseWithApiResponse() {
    MatePostResponse matePost = createResponse();
    when(matePostService.getById(10L)).thenReturn(matePost);

    ApiResponse<MatePostResponse> response = matePostController.getById(10L);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(matePost);
    assertThat(response.getError()).isNull();
  }

  private MatePostCreateRequest createRequest() {
    return new MatePostCreateRequest(
        20L, "잠실 경기 같이 봐요", "즐겁게 응원할 분을 모집합니다.", 3, "ANY", 20, 40, "1루 네이비석");
  }

  private MatePostResponse createResponse() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 9, 1, 12, 0);
    return new MatePostResponse(
        10L,
        1L,
        20L,
        "잠실 경기 같이 봐요",
        "즐겁게 응원할 분을 모집합니다.",
        3,
        1,
        "ANY",
        20,
        40,
        "1루 네이비석",
        MatePostStatus.OPEN,
        createdAt,
        createdAt);
  }
}
