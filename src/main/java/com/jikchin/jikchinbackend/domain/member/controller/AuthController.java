package com.jikchin.jikchinbackend.domain.member.controller;

import com.jikchin.jikchinbackend.domain.member.dto.request.LoginRequest;
import com.jikchin.jikchinbackend.domain.member.dto.request.LogoutRequest;
import com.jikchin.jikchinbackend.domain.member.dto.request.RefreshTokenRequest;
import com.jikchin.jikchinbackend.domain.member.dto.request.SignUpRequest;
import com.jikchin.jikchinbackend.domain.member.dto.response.TokenResponse;
import com.jikchin.jikchinbackend.domain.member.service.AuthService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<TokenResponse>> signUp(
      @Valid @RequestBody SignUpRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(authService.signUp(request)));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<TokenResponse>> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.refresh(request.refreshToken())));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request.refreshToken());
    return ResponseEntity.ok(ApiResponse.success());
  }
}
