package com.jikchin.jikchinbackend.domain.event.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record EventCreateRequest(
    @NotNull Long sportId,
    @NotNull Long venueId,
    @NotNull Long homeTeamId,
    @NotNull Long awayTeamId,
    @NotBlank String leagueName,
    @NotNull @Future LocalDateTime startsAt) {}
