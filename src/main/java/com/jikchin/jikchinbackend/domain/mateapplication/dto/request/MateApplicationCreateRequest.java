package com.jikchin.jikchinbackend.domain.mateapplication.dto.request;

import jakarta.validation.constraints.Size;

public record MateApplicationCreateRequest(@Size(max = 500) String message) {}
