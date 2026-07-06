package com.ne7k.safepaw.walk.dto.request;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record WalkPointDto(
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng,
        @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")  Double lat,
        @NotNull @PositiveOrZero Float accuracyMeters,
        Float speedKmh,                 // 참고용 — 서버 재계산
        @NotNull OffsetDateTime recordedAt
) {}