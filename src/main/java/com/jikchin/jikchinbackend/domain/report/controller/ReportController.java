package com.jikchin.jikchinbackend.domain.report.controller;

import com.jikchin.jikchinbackend.domain.report.dto.request.ReportCreateRequest;
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.service.ReportService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ReportResponse> create(
      @AuthenticationPrincipal UUID memberKey, @Valid @RequestBody ReportCreateRequest request) {
    return ApiResponse.success(reportService.create(memberKey, request));
  }
}
