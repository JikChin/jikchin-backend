package com.jikchin.jikchinbackend.domain.report.controller;

import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.service.ReportService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

  private final ReportService reportService;

  @GetMapping
  public ApiResponse<List<ReportResponse>> getReports(
      @RequestParam(required = false) ReportStatus status) {
    return ApiResponse.success(reportService.getReports(status));
  }

  @PatchMapping("/{reportId}/resolve")
  public ApiResponse<ReportResponse> resolve(@PathVariable Long reportId) {
    return ApiResponse.success(reportService.resolve(reportId));
  }

  @PatchMapping("/{reportId}/reject")
  public ApiResponse<ReportResponse> reject(@PathVariable Long reportId) {
    return ApiResponse.success(reportService.reject(reportId));
  }
}
