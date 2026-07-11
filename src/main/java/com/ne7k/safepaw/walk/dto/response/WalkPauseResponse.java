package com.ne7k.safepaw.walk.dto.response;

import java.time.OffsetDateTime;

public record WalkPauseResponse(
        Long walkId,
        String status,
        OffsetDateTime pausedAt
) {
}
