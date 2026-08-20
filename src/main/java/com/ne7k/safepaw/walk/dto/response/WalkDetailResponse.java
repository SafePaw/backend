package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.territory.dto.response.GeoJsonLineString;
import com.ne7k.safepaw.walk.domain.WalkSession;

import java.time.OffsetDateTime;
import java.util.List;

public record WalkDetailResponse(
        Long id, Long dogId, String status,
        OffsetDateTime startedAt, OffsetDateTime endedAt,
        WalkStats stats,
        GeoJsonLineString polyline,
        Long territoryId
) {
    public static WalkDetailResponse from(
            WalkSession w,
            List<Object[]> lngLatRows,
            Long territoryId,
            Double caloriesKcal
    ) {
        double distance = w.getDistanceMeters() == null ? 0 : w.getDistanceMeters();
        int duration = w.getDurationSeconds() == null ? 0 : w.getDurationSeconds();
        return new WalkDetailResponse(
                w.getId(), w.getDog().getId(), w.getStatus().name(),
                w.getStartedAt(), w.getEndedAt(),
                WalkStats.of(distance, duration, w.getPointCount(), null, caloriesKcal),
                lngLatRows.isEmpty() ? null : GeoJsonLineString.from(lngLatRows),
                territoryId);
    }
}
