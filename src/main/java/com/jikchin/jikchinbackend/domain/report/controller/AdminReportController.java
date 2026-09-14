package com.jikchin.jikchinbackend.domain.report.controller;

import com.jikchin.jikchinbackend.domain.report.dto.response.ReportResponse;
import com.jikchin.jikchinbackend.domain.report.dto.response.ReportSliceResponse;
import com.jikchin.jikchinbackend.domain.report.entity.ReportStatus;
import com.jikchin.jikchinbackend.domain.report.service.ReportService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
  public ApiResponse<ReportSliceResponse> getReports(
      @RequestParam(required = false) ReportStatus status,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ApiResponse.success(reportService.getReports(status, cursor, size));
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
