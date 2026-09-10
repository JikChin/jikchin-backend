package com.jikchin.jikchinbackend.domain.event.controller;

import com.jikchin.jikchinbackend.domain.event.dto.response.EventResponse;
import com.jikchin.jikchinbackend.domain.event.service.EventService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
public class EventController {
  private final EventService eventService;

  @GetMapping
  public ApiResponse<List<EventResponse>> getEvents(
      @RequestParam(required = false) Long sportId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    if (sportId != null && from != null && to != null) {
      if (!from.isBefore(to)) {
        throw new IllegalArgumentException("조회 시작 시각은 종료 시각보다 앞서야 합니다.");
      }
      return ApiResponse.success(eventService.getEventsBySportAndPeriod(sportId, from, to, size));
    }
    if (sportId != null || from != null || to != null) {
      throw new IllegalArgumentException("sportId, from, to를 함께 전달해 주세요.");
    }
    return ApiResponse.success(eventService.getEvents());
  }

  @GetMapping("/{eventId}")
  public ApiResponse<EventResponse> getEvent(@PathVariable Long eventId) {
    return ApiResponse.success(eventService.getEvent(eventId));
  }
}
