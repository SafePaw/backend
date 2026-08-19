package com.ne7k.safepaw.walk.dto.response;

import com.ne7k.safepaw.walk.service.GeoUtils;

public record WalkStats(
        double distanceMeters,
        int durationSeconds,
        double averageSpeedKmh,
        int pointCount,
        Double loopGapMeters,
        Double caloriesKcal
) {
    public static WalkStats of(
            double distanceMeters,
            int durationSeconds,
            int pointCount,
            Double loopGapMeters,
            Double caloriesKcal
    ) {
        return new WalkStats(
                distanceMeters,
                durationSeconds,
                GeoUtils.speedKmh(distanceMeters, durationSeconds),
                pointCount,
                loopGapMeters,
                caloriesKcal
        );
    }
}