package com.jikchin.jikchinbackend.domain.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jikchin.jikchinbackend.domain.member.dto.request.SignUpRequest;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.member.repository.RefreshTokenRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import com.jikchin.jikchinbackend.global.security.jwt.JwtTokenProvider;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @InjectMocks private AuthService authService;

  @Mock private AuthenticationManager authenticationManager;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private MemberRepository memberRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;

  @Test
  void 저장_시점의_이메일_유니크_제약_위반을_중복_회원_예외로_변환한다() {
    SignUpRequest request =
        new SignUpRequest("member@example.com", "password1234", "직관러", null, null, null, "서울");
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException(
            "duplicate email",
            new ConstraintViolationException(
                "duplicate email", new SQLException(), Member.EMAIL_UNIQUE_CONSTRAINT));

    when(memberRepository.existsByEmail("member@example.com")).thenReturn(false);
    when(memberRepository.existsByNickname("직관러")).thenReturn(false);
    when(passwordEncoder.encode("password1234")).thenReturn("encoded-password");
    when(memberRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Member.class)))
        .thenThrow(exception);

    assertThatThrownBy(() -> authService.signUp(request))
        .isInstanceOf(AppException.class)
        .extracting(error -> ((AppException) error).getErrorType())
        .isEqualTo(ErrorType.DUPLICATE_MEMBER);
  }
}
