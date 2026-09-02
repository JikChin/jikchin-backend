package com.jikchin.jikchinbackend.domain.mateapplication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jikchin.jikchinbackend.domain.mateapplication.dto.request.MateApplicationCreateRequest;
import com.jikchin.jikchinbackend.domain.mateapplication.dto.response.MateApplicationResponse;
import com.jikchin.jikchinbackend.domain.mateapplication.entity.MateApplicationStatus;
import com.jikchin.jikchinbackend.domain.mateapplication.service.MateApplicationService;
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
class MateApplicationControllerTest {

  @Mock private MateApplicationService mateApplicationService;

  private MateApplicationController mateApplicationController;

  @BeforeEach
  void setUp() {
    mateApplicationController = new MateApplicationController(mateApplicationService);
  }

  @Test
  void returnsApplicationWrappedWithApiResponse() {
    MateApplicationCreateRequest request = new MateApplicationCreateRequest("신청합니다");
    MateApplicationResponse application = createResponse(MateApplicationStatus.PENDING);
    when(mateApplicationService.apply(10L, 2L, request)).thenReturn(application);

    ApiResponse<MateApplicationResponse> response =
        mateApplicationController.apply(10L, 2L, request);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(application);
    assertThat(response.getError()).isNull();
  }

  @Test
  void returnsAcceptedApplicationWrappedWithApiResponse() {
    MateApplicationResponse application = createResponse(MateApplicationStatus.ACCEPTED);
    when(mateApplicationService.accept(10L, 100L, 1L)).thenReturn(application);

    ApiResponse<MateApplicationResponse> response = mateApplicationController.accept(10L, 100L, 1L);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().status()).isEqualTo(MateApplicationStatus.ACCEPTED);
    assertThat(response.getError()).isNull();
  }

  @Test
  void returnsApplicationsWrappedWithApiResponse() {
    List<MateApplicationResponse> applications =
        List.of(createResponse(MateApplicationStatus.PENDING));
    when(mateApplicationService.getApplications(10L, 1L)).thenReturn(applications);

    ApiResponse<List<MateApplicationResponse>> response =
        mateApplicationController.getApplications(10L, 1L);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(applications);
    assertThat(response.getError()).isNull();
  }

  private MateApplicationResponse createResponse(MateApplicationStatus status) {
    LocalDateTime createdAt = LocalDateTime.of(2026, 9, 2, 12, 0);
    return new MateApplicationResponse(100L, 10L, 2L, status, "신청합니다", createdAt, createdAt);
  }
}
