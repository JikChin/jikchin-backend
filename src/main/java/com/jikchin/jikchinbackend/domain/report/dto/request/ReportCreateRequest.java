package com.jikchin.jikchinbackend.domain.report.dto.request;

import com.jikchin.jikchinbackend.domain.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
    @NotNull Long matePostId,
    @NotNull Long reportedUserId,
    @NotNull ReportReason reason,
    @Size(max = 1000) String detail) {}
