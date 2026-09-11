package com.jikchin.jikchinbackend.domain.event.controller;

import com.jikchin.jikchinbackend.domain.event.dto.request.EventCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.SportCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.TeamCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.VenueCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.response.CatalogResponse;
import com.jikchin.jikchinbackend.domain.event.dto.response.EventResponse;
import com.jikchin.jikchinbackend.domain.event.service.EventService;
import com.jikchin.jikchinbackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {
  private final EventService eventService;

  @PostMapping
  public ApiResponse<EventResponse> create(@Valid @RequestBody EventCreateRequest request) {
    return ApiResponse.success(eventService.create(request));
  }

  @PostMapping("/catalog/sports")
  public ApiResponse<CatalogResponse> createSport(@Valid @RequestBody SportCreateRequest request) {
    return ApiResponse.success(eventService.createSport(request));
  }

  @PostMapping("/catalog/teams")
  public ApiResponse<CatalogResponse> createTeam(@Valid @RequestBody TeamCreateRequest request) {
    return ApiResponse.success(eventService.createTeam(request));
  }

  @PostMapping("/catalog/venues")
  public ApiResponse<CatalogResponse> createVenue(@Valid @RequestBody VenueCreateRequest request) {
    return ApiResponse.success(eventService.createVenue(request));
  }
}
