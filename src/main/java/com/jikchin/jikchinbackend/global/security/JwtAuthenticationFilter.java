package com.jikchin.jikchinbackend.global.security;

import com.jikchin.jikchinbackend.global.error.AppException;
import com.jikchin.jikchinbackend.global.security.jwt.JwtAuthenticationInfo;
import com.jikchin.jikchinbackend.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String accessToken = resolveAccessToken(request);

    if (accessToken == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      JwtAuthenticationInfo tokenInfo = jwtTokenProvider.parseAccessToken(accessToken);

      MemberAuthenticationToken authentication =
          new MemberAuthenticationToken(
              tokenInfo.memberKey(), List.of(new SimpleGrantedAuthority(tokenInfo.role().name())));

      SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
      securityContext.setAuthentication(authentication);
      SecurityContextHolder.setContext(securityContext);

      filterChain.doFilter(request, response);
    } catch (AppException exception) {
      SecurityContextHolder.clearContext();
      jwtAuthenticationEntryPoint.writeError(response, exception.getErrorType());
    }
  }

  private String resolveAccessToken(HttpServletRequest request) {
    String authorization = request.getHeader(AUTHORIZATION_HEADER);

    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }

    return authorization.substring(BEARER_PREFIX.length());
  }
}
