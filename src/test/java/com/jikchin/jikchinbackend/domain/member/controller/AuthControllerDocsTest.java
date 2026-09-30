package com.jikchin.jikchinbackend.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.member.dto.request.LoginRequest;
import com.jikchin.jikchinbackend.domain.member.dto.request.SignUpRequest;
import com.jikchin.jikchinbackend.domain.member.dto.response.TokenResponse;
import com.jikchin.jikchinbackend.domain.member.entity.Gender;
import com.jikchin.jikchinbackend.domain.member.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

class AuthControllerDocsTest extends RestDocsSupport {

  // 공개 저장소에 남는 문서라 누가 봐도 가짜인 토큰 값을 쓴다.
  private static final String REFRESH_TOKEN = "sample-refresh-token";

  private final AuthService authService = mock(AuthService.class);

  @Override
  protected Object initController() {
    return new AuthController(authService);
  }

  @Test
  void signUp() throws Exception {
    given(authService.signUp(any(SignUpRequest.class))).willReturn(tokens());

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"fan@example.com","password":"password1234","nickname":"직관러",\
                    "profileImageUrl":"https://example.com/profile.png","gender":"FEMALE",\
                    "birthDate":"1998-05-20","region":"서울"}
                    """))
        .andExpect(status().isCreated())
        .andDo(
            document(
                "auth-signup",
                requestFields(
                    fieldWithPath("email").type(STRING).description("이메일 (최대 100자)"),
                    fieldWithPath("password").type(STRING).description("비밀번호 (8~72자)"),
                    fieldWithPath("nickname").type(STRING).description("닉네임 (최대 50자)"),
                    fieldWithPath("profileImageUrl")
                        .type(STRING)
                        .optional()
                        .description("프로필 이미지 URL (최대 500자)"),
                    fieldWithPath("gender")
                        .type(STRING)
                        .optional()
                        .description("성별: " + enumValues(Gender.class)),
                    fieldWithPath("birthDate")
                        .type(STRING)
                        .optional()
                        .description("생년월일 (yyyy-MM-dd, 과거 날짜)"),
                    fieldWithPath("region").type(STRING).optional().description("활동 지역 (최대 100자)")),
                successResponse(tokenFields())));
  }

  @Test
  void login() throws Exception {
    given(authService.login(any(LoginRequest.class))).willReturn(tokens());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"fan@example.com","password":"password1234"}
                    """))
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth-login",
                requestFields(
                    fieldWithPath("email").type(STRING).description("이메일"),
                    fieldWithPath("password").type(STRING).description("비밀번호")),
                successResponse(tokenFields())));
  }

  @Test
  void refresh() throws Exception {
    given(authService.refresh(REFRESH_TOKEN)).willReturn(tokens());

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenBody()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth-refresh",
                requestFields(
                    fieldWithPath("refreshToken")
                        .type(STRING)
                        .description("발급받은 Refresh Token. 사용하면 폐기되고 새 토큰 쌍이 발급된다")),
                successResponse(tokenFields())));
  }

  @Test
  void logout() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenBody()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth-logout",
                requestFields(
                    fieldWithPath("refreshToken").type(STRING).description("폐기할 Refresh Token")),
                successResponseWithoutData()));

    verify(authService).logout(REFRESH_TOKEN);
  }

  private FieldDescriptor[] tokenFields() {
    return new FieldDescriptor[] {
      fieldWithPath("accessToken").type(STRING).description("Access Token"),
      fieldWithPath("accessTokenExpiresAt")
          .type(STRING)
          .description("Access Token 만료 시각 (ISO-8601, UTC)"),
      fieldWithPath("refreshToken").type(STRING).description("Refresh Token"),
      fieldWithPath("refreshTokenExpiresAt")
          .type(STRING)
          .description("Refresh Token 만료 시각 (ISO-8601, UTC)")
    };
  }

  private TokenResponse tokens() {
    return new TokenResponse(
        "sample-access-token",
        Instant.parse("2026-09-15T10:30:00Z"),
        REFRESH_TOKEN,
        Instant.parse("2026-09-29T10:00:00Z"));
  }

  private String refreshTokenBody() {
    return """
        {"refreshToken":"%s"}
        """
        .formatted(REFRESH_TOKEN);
  }
}
