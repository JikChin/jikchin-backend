package com.jikchin.jikchinbackend.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jikchin.jikchinbackend.domain.member.dto.request.LoginRequest;
import com.jikchin.jikchinbackend.domain.member.dto.request.SignUpRequest;
import com.jikchin.jikchinbackend.domain.member.dto.response.TokenResponse;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.member.repository.RefreshTokenRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthServiceIntegrationTest {

  @Autowired private AuthService authService;

  @Autowired private MemberRepository memberRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    memberRepository.deleteAll();
  }

  @Test
  void 회원가입_후_로그인하고_토큰을_발급한다() {
    authService.signUp(createSignUpRequest());

    TokenResponse tokenResponse =
        authService.login(new LoginRequest("member@example.com", "password1234"));

    assertThat(tokenResponse.accessToken()).isNotBlank();
    assertThat(tokenResponse.refreshToken()).isNotBlank();
    assertThat(refreshTokenRepository.count()).isEqualTo(2);
  }

  @Test
  void 재발급하면_기존_Refresh_Token은_재사용할_수_없다() {
    TokenResponse firstTokenResponse = authService.signUp(createSignUpRequest());

    TokenResponse refreshedTokenResponse = authService.refresh(firstTokenResponse.refreshToken());

    assertThat(refreshedTokenResponse.accessToken()).isNotEqualTo(firstTokenResponse.accessToken());
    assertThatThrownBy(() -> authService.refresh(firstTokenResponse.refreshToken()))
        .isInstanceOf(AppException.class)
        .extracting(exception -> ((AppException) exception).getErrorType())
        .isEqualTo(ErrorType.REFRESH_TOKEN_NOT_FOUND);
  }

  @Test
  void 로그아웃하면_Refresh_Token을_삭제한다() {
    TokenResponse tokenResponse = authService.signUp(createSignUpRequest());

    authService.logout(tokenResponse.refreshToken());

    assertThatThrownBy(() -> authService.refresh(tokenResponse.refreshToken()))
        .isInstanceOf(AppException.class)
        .extracting(exception -> ((AppException) exception).getErrorType())
        .isEqualTo(ErrorType.REFRESH_TOKEN_NOT_FOUND);
  }

  private SignUpRequest createSignUpRequest() {
    return new SignUpRequest("member@example.com", "password1234", "직관러", null, null, null, "서울");
  }
}
