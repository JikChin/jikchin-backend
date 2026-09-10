package com.jikchin.jikchinbackend.domain.event.dto.request;

import com.jikchin.jikchinbackend.domain.event.entity.SportCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SportCreateRequest(@NotNull SportCode code, @NotBlank String name) {}
