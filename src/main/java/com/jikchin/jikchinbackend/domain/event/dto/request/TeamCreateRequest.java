package com.jikchin.jikchinbackend.domain.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeamCreateRequest(
    @NotNull Long sportId, @NotBlank String name, String shortName, String logoUrl) {}
