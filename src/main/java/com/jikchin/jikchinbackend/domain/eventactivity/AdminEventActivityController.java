package com.jikchin.jikchinbackend.domain.eventactivity;

import com.jikchin.jikchinbackend.global.response.ApiResponse;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/event-activities")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "activity-log.enabled", havingValue = "true")
public class AdminEventActivityController {
  private final EventActivityQueryService service;

  @GetMapping
  public ApiResponse<EventActivityQueryService.Slice> find(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      @RequestParam(required = false) Long eventId,
      @RequestParam(required = false) ActivityType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime cursorAt,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "50") int size) {
    return ApiResponse.success(service.find(from, to, eventId, type, cursorAt, cursorId, size));
  }

  @GetMapping("/monthly")
  public ApiResponse<List<EventActivityQueryService.MonthlyScore>> monthly(
      @RequestParam String month, @RequestParam(defaultValue = "20") int limit) {
    YearMonth parsed;
    try {
      parsed = YearMonth.parse(month);
    } catch (DateTimeParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month는 yyyy-MM 형식입니다.");
    }
    return ApiResponse.success(service.monthly(parsed, limit));
  }
}
