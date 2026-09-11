package com.jikchin.jikchinbackend.domain.event.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VenueCreateRequest(@NotBlank String name, @NotBlank String region, String address) {}
