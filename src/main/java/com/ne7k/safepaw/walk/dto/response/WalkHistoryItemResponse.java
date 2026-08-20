package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.territory.domain.Territory;
import com.ne7k.safepaw.walk.domain.WalkSession;
import com.ne7k.safepaw.walk.service.GeoUtils;

import java.time.OffsetDateTime;

public record WalkHistoryItemResponse(
        Long walkId,
        Long dogId,
        String dogName,
        String status,
        String walkType,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        WalkStats stats,
        WalkHistoryTerritorySummary territory
) {
    public static WalkHistoryItemResponse from(WalkSession w, Territory territoryOrNull) {
        double distance = w.getDistanceMeters() == null ? 0 : w.getDistanceMeters();
        int duration = w.getDurationSeconds() == null ? 0 : w.getDurationSeconds();
        long durationSec = duration;
        double avgSpeed = GeoUtils.speedKmh(distance, durationSec);

        String walkType = territoryOrNull != null ? "TERRITORY" : "NORMAL";

        WalkStats stats = new WalkStats(
                distance,
                duration,
                avgSpeed,
                w.getPointCount(),
                null
        );

        WalkHistoryTerritorySummary territorySummary = territoryOrNull != null
                ? WalkHistoryTerritorySummary.from(territoryOrNull)
                : null;

        return new WalkHistoryItemResponse(
                w.getId(),
                w.getDog().getId(),
                w.getDog().getName(),
                w.getStatus().name(),
                walkType,
                w.getStartedAt(),
                w.getEndedAt(),
                stats,
                territorySummary
        );
    }
}