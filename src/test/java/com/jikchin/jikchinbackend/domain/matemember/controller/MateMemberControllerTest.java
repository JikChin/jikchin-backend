package com.jikchin.jikchinbackend.domain.matemember.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jikchin.jikchinbackend.domain.matemember.dto.response.MateMemberResponse;
import com.jikchin.jikchinbackend.domain.matemember.entity.MateMemberStatus;
import com.jikchin.jikchinbackend.domain.matemember.service.MateMemberService;
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
class MateMemberControllerTest {

  @Mock private MateMemberService mateMemberService;

  private MateMemberController mateMemberController;

  @BeforeEach
  void setUp() {
    mateMemberController = new MateMemberController(mateMemberService);
  }

  @Test
  void returnsActiveMembersWrappedWithApiResponse() {
    List<MateMemberResponse> members =
        List.of(new MateMemberResponse(1L, 10L, 2L, LocalDateTime.now(), MateMemberStatus.ACTIVE));
    when(mateMemberService.getActiveMembers(10L)).thenReturn(members);

    ApiResponse<List<MateMemberResponse>> response = mateMemberController.getActiveMembers(10L);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isEqualTo(members);
    assertThat(response.getError()).isNull();
  }

  @Test
  void returnsParticipationStatusWrappedWithApiResponse() {
    when(mateMemberService.isParticipating(10L, 2L)).thenReturn(true);

    ApiResponse<Boolean> response = mateMemberController.isParticipating(10L, 2L);

    assertThat(response.getResultType()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isTrue();
    assertThat(response.getError()).isNull();
  }
}
