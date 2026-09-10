package com.jikchin.jikchinbackend.domain.event.controller;

import com.jikchin.jikchinbackend.domain.event.dto.response.EventResponse;
import com.jikchin.jikchinbackend.domain.event.service.EventService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
  private final EventService eventService;

  @GetMapping
  public ApiResponse<List<EventResponse>> getEvents() {
    return ApiResponse.success(eventService.getEvents());
  }

  @GetMapping("/{eventId}")
  public ApiResponse<EventResponse> getEvent(@PathVariable Long eventId) {
    return ApiResponse.success(eventService.getEvent(eventId));
  }
}
