package com.ne7k.safepaw.walk.dto.response;

import java.time.OffsetDateTime;

public record WalkLiveResponse(
        Long walkId,
        String status,
        double distanceMeters,
        int durationSeconds,
        double currentSpeedKmh,
        double averageSpeedKmh,
        int pointCount,
        double estimatedTerritoryM2,
        double caloriesKcal,
        OffsetDateTime pausedAt,
        int totalPausedSeconds
) {
}
