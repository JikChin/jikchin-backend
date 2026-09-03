package com.jikchin.jikchinbackend.domain.member.service;

import com.jikchin.jikchinbackend.domain.member.dto.request.LoginRequest;
import com.jikchin.jikchinbackend.domain.member.dto.request.SignUpRequest;
import com.jikchin.jikchinbackend.domain.member.dto.response.TokenResponse;
import com.jikchin.jikchinbackend.domain.member.entity.Member;
import com.jikchin.jikchinbackend.domain.member.entity.MemberStatus;
import com.jikchin.jikchinbackend.domain.member.entity.RefreshToken;
import com.jikchin.jikchinbackend.domain.member.entity.Role;
import com.jikchin.jikchinbackend.domain.member.repository.MemberRepository;
import com.jikchin.jikchinbackend.domain.member.repository.RefreshTokenRepository;
import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.error.ErrorType;
import com.jikchin.jikchinbackend.global.security.MemberPrincipal;
import com.jikchin.jikchinbackend.global.security.jwt.JwtTokenProvider;
import com.jikchin.jikchinbackend.global.security.jwt.RefreshTokenInfo;
import com.jikchin.jikchinbackend.global.security.jwt.TokenPair;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final MemberRepository memberRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public TokenResponse signUp(SignUpRequest request) {
    String normalizedEmail = normalizeEmail(request.email());

    if (memberRepository.existsByEmail(normalizedEmail)
        || memberRepository.existsByNickname(request.nickname().trim())) {
      throw new AppException(ErrorType.DUPLICATE_MEMBER);
    }

    Member member =
        Member.create(
            normalizedEmail,
            passwordEncoder.encode(request.password()),
            request.nickname(),
            request.profileImageUrl(),
            request.gender(),
            request.birthDate(),
            request.region());

    Member savedMember = memberRepository.save(member);
    return issueAndStoreRefreshToken(savedMember.getMemberKey(), savedMember.getRole());
  }

  @Transactional
  public TokenResponse login(LoginRequest request) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  normalizeEmail(request.email()), request.password()));

      MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
      return issueAndStoreRefreshToken(principal.getMemberKey(), principal.getRole());
    } catch (AuthenticationException exception) {
      throw new AppException(ErrorType.LOGIN_FAILED);
    }
  }

  @Transactional
  public TokenResponse refresh(String refreshToken) {
    RefreshTokenInfo tokenInfo = jwtTokenProvider.parseRefreshToken(refreshToken);

    RefreshToken savedRefreshToken =
        refreshTokenRepository
            .findByTokenIdAndMemberKey(tokenInfo.tokenId(), tokenInfo.memberKey())
            .orElseThrow(() -> new AppException(ErrorType.REFRESH_TOKEN_NOT_FOUND));

    Member member =
        memberRepository
            .findByMemberKey(tokenInfo.memberKey())
            .orElseThrow(() -> new AppException(ErrorType.MEMBER_NOT_FOUND));

    validateActiveMember(member);
    refreshTokenRepository.delete(savedRefreshToken);

    return issueAndStoreRefreshToken(member.getMemberKey(), member.getRole());
  }

  @Transactional
  public void logout(String refreshToken) {
    RefreshTokenInfo tokenInfo = jwtTokenProvider.parseRefreshToken(refreshToken);

    refreshTokenRepository
        .findByTokenIdAndMemberKey(tokenInfo.tokenId(), tokenInfo.memberKey())
        .ifPresent(refreshTokenRepository::delete);
  }

  private TokenResponse issueAndStoreRefreshToken(UUID memberKey, Role role) {
    TokenPair tokenPair = jwtTokenProvider.issue(memberKey, role);

    refreshTokenRepository.save(
        RefreshToken.create(
            tokenPair.refreshTokenId(), memberKey, tokenPair.refreshTokenExpiresAt()));

    return TokenResponse.from(tokenPair);
  }

  private void validateActiveMember(Member member) {
    if (member.getStatus() != MemberStatus.ACTIVE) {
      throw new AppException(ErrorType.MEMBER_INACTIVE);
    }
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
